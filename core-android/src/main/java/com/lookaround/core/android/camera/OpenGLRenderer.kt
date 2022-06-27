package com.lookaround.core.android.camera

import android.annotation.SuppressLint
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.opengl.Matrix
import android.os.Process
import android.util.Size
import android.view.Surface
import android.view.ViewStub
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.view.PreviewView.StreamState
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.lookaround.core.android.ext.shouldUseTextureView
import com.lookaround.core.android.model.RoundedRectF
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber

class OpenGLRenderer {
    companion object {
        init {
            System.loadLibrary("opengl_renderer_jni")
        }

        private val RENDERER_COUNT = AtomicInteger(0)

        const val MARKER_RECT_CORNER_RADIUS = 100f
        private const val MARKER_RECTS_MAX_SIZE = 24
        private const val COORDINATES_PER_RECT = 5
    }

    private val executor =
        SingleThreadHandlerExecutor(
            String.format(Locale.US, "GLRenderer-%03d", RENDERER_COUNT.incrementAndGet()),
            Process.THREAD_PRIORITY_DEFAULT
        ) // Use UI thread priority (DEFAULT)

    private var previewResolution: Size? = null
    private var previewTexture: SurfaceTexture? = null
    private val previewTransform = FloatArray(16)
    private var naturalPreviewWidth = 0f
    private var naturalPreviewHeight = 0f

    private var surfaceSize: Size? = null
    private var surfaceRotationDegrees = 0
    private val surfaceTransform = FloatArray(16)

    private val tempVec = FloatArray(8)
    private var nativeContext = 0L
    private var isShutdown = false
    private var numOutstandingSurfaces = 0
    private var frameUpdateListener: Pair<Executor, (Long) -> Unit>? = null

    private val activeStreamStateObserver = AtomicReference<PreviewStreamStateObserver?>()
    private val previewStreamStateFlow = MutableStateFlow(StreamState.IDLE)
    val previewStreamStates: Flow<StreamState>
        get() = previewStreamStateFlow

    private var markerRects: List<RoundedRectF> = emptyList()
        set(value) {
            field = value.take(MARKER_RECTS_MAX_SIZE)
        }
    var otherRects: List<RoundedRectF> = emptyList()
    var markerRectsDisabled: Boolean = false

    private val rectsCoordinates: FloatArray
        get() {
            val coordinates =
                FloatArray((MARKER_RECTS_MAX_SIZE + otherRects.size) * COORDINATES_PER_RECT)
            var index = 0

            fun fillCoordinatesOf(rects: Collection<RoundedRectF>) {
                for (roundedRect in rects) {
                    val (rect, cornerRadius) = roundedRect
                    coordinates[index++] = rect.left
                    coordinates[index++] = rect.bottom
                    coordinates[index++] = rect.width()
                    coordinates[index++] = rect.height()
                    coordinates[index++] = cornerRadius
                }
            }

            fillCoordinatesOf(otherRects)
            if (!markerRectsDisabled) fillCoordinatesOf(markerRects)
            return coordinates
        }

    fun setMarkerRects(rects: Iterable<RectF>) {
        if (markerRectsDisabled) return
        markerRects = rects.map { RoundedRectF(it, MARKER_RECT_CORNER_RADIUS) }
    }

    @MainThread
    fun setBlurEnabled(enabled: Boolean, animated: Boolean) {
        if (isShutdown || nativeContext == 0L) return
        try {
            executor.execute {
                setBlurEnabled(nativeContext, enabled = enabled, animated = animated)
            }
        } catch (e: RejectedExecutionException) {
            Timber.tag("OGL").i("Renderer already shutting down. Ignore.")
        }
    }

    @MainThread
    fun setContrastingColor(red: Int, green: Int, blue: Int) {
        if (isShutdown || nativeContext == 0L) return
        try {
            executor.execute {
                setContrastingColor(
                    nativeContext,
                    red = red.toFloat() / 256f,
                    green = green.toFloat() / 256f,
                    blue = blue.toFloat() / 256f
                )
            }
        } catch (e: RejectedExecutionException) {
            Timber.tag("OGL").i("Renderer already shutting down. Ignore.")
        }
    }

    @SuppressLint("RestrictedApi")
    @MainThread
    fun attachInputPreview(preview: Preview, previewStub: ViewStub) {
        preview.setSurfaceProvider(executor) { surfaceRequest: SurfaceRequest ->
            if (isShutdown) {
                surfaceRequest.willNotProvideSurface()
                return@setSurfaceProvider
            }

            val camera = surfaceRequest.camera
            val renderSurface =
                if (surfaceRequest.shouldUseTextureView()) TextureViewRenderSurface()
                else SurfaceViewRenderSurface()
            previewStub.post { renderSurface.inflateWith(previewStub, this) }
            val streamStateObserver =
                PreviewStreamStateObserver(
                    camera.cameraInfoInternal,
                    previewStreamStateFlow,
                    renderSurface
                )
            camera.cameraState.addObserver(
                ContextCompat.getMainExecutor(previewStub.context),
                streamStateObserver
            )
            activeStreamStateObserver.set(streamStateObserver)

            if (nativeContext == 0L)
                nativeContext =
                    initContext().also {
                        if (it < 0L) throw IllegalStateException()
                    } // TODO: replace this with a callback function or a mutableSharedFlow to
            // signal an error to CameraFragment -> then show camera init failure msg
            // (just throwing an exception does not work because we're in a callback yo) - check if
            // exception can be thrown and caught here as well!!!
            val surfaceTexture = resetPreviewTexture(surfaceRequest.resolution)
            val inputSurface = Surface(surfaceTexture)
            numOutstandingSurfaces++
            surfaceRequest.provideSurface(inputSurface, executor) {
                inputSurface.release()
                surfaceTexture.release()
                if (surfaceTexture === previewTexture) previewTexture = null
                numOutstandingSurfaces--
                doShutdownIfNeeded()
                if (activeStreamStateObserver.compareAndSet(streamStateObserver, null)) {
                    streamStateObserver.updatePreviewStreamState(StreamState.IDLE)
                }
                streamStateObserver.clear()
                camera.cameraState.removeObserver(streamStateObserver)
            }
        }
    }

    fun attachOutputSurface(surface: Surface, surfaceSize: Size, surfaceRotationDegrees: Int) {
        if (isShutdown) return
        try {
            executor.execute {
                if (nativeContext == 0L) nativeContext = initContext()

                if (setWindowSurface(nativeContext, surface)) {
                    this.surfaceRotationDegrees = surfaceRotationDegrees
                    this.surfaceSize = surfaceSize
                } else {
                    this.surfaceSize = null
                }
            }
        } catch (e: RejectedExecutionException) {
            Timber.tag("OGL").i("Renderer already shutting down. Ignore.")
        }
    }

    /**
     * Sets a listener to receive updates when a frame has been drawn to the output [Surface].
     *
     * Frame updates include the timestamp of the latest drawn frame.
     *
     * @param executor Executor used to call the listener.
     * @param listener Listener which receives updates in the form of a timestamp (in nanoseconds).
     */
    fun setFrameUpdateListener(executor: Executor, listener: (Long) -> Unit) {
        try {
            this.executor.execute { frameUpdateListener = Pair(executor, listener) }
        } catch (e: RejectedExecutionException) {
            Timber.tag("OGL").i("Renderer already shutting down. Ignore.")
        }
    }

    fun invalidateSurface(surfaceRotationDegrees: Int) {
        try {
            executor.execute {
                this.surfaceRotationDegrees = surfaceRotationDegrees
                if (previewTexture != null && nativeContext != 0L) renderLatest()
            }
        } catch (e: RejectedExecutionException) {
            Timber.tag("OGL").i("Renderer already shutting down. Ignore.")
        }
    }

    /**
     * Detach the current output surface from the renderer.
     *
     * @return A [ListenableFuture] that signals detach from the renderer. Some devices may not be
     * able to handle the surface being released while still attached to an EGL context. It should
     * be safe to release resources associated with the output surface once this future has
     * completed.
     */
    fun detachOutputSurface(): ListenableFuture<Unit> =
        CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Unit> ->
            try {
                executor.execute {
                    if (nativeContext != 0L) {
                        setWindowSurface(nativeContext, null)
                        surfaceSize = null
                    }
                    completer.set(Unit)
                }
            } catch (e: RejectedExecutionException) {
                // Renderer is shutting down. Can notify that the surface is detached.
                completer.set(Unit)
            }
            "detachOutputSurface [$this]"
        }

    fun shutdown() {
        try {
            executor.execute {
                isShutdown = true
                if (nativeContext != 0L) {
                    closeContext(nativeContext)
                    nativeContext = 0
                }
                doShutdownIfNeeded()
            }
        } catch (e: RejectedExecutionException) {
            Timber.tag("OGL").i("Renderer already shutting down. Ignore.")
        }
    }

    @WorkerThread
    private fun doShutdownIfNeeded() {
        if (isShutdown && numOutstandingSurfaces == 0) {
            frameUpdateListener = null
            executor.shutdown()
        }
    }

    @WorkerThread
    private fun resetPreviewTexture(size: Size): SurfaceTexture {
        previewTexture?.detachFromGLContext()
        return SurfaceTexture(getTexName(nativeContext)).apply {
            previewTexture = this
            setDefaultBufferSize(size.width, size.height)
            setOnFrameAvailableListener(
                { surfaceTexture ->
                    if (surfaceTexture === previewTexture && nativeContext != 0L) {
                        surfaceTexture.updateTexImage()
                        renderLatest()
                    }
                },
                executor.handler
            )
            previewResolution = size
        }
    }

    @WorkerThread
    private fun renderLatest() {
        val previewTexture = requireNotNull(this.previewTexture)
        // Get the timestamp so we can pass it along to the output surface (not strictly necessary)
        val timestampNs = previewTexture.timestamp

        // Get texture transform from surface texture (transform to natural orientation).
        // This will be used to transform texture coordinates in the fragment shader.
        previewTexture.getTransformMatrix(previewTransform)
        if (surfaceSize == null) return

        calculateSurfaceTransform()
        val success =
            renderTexture(
                nativeContext = nativeContext,
                timestampNs = timestampNs,
                vertexTransform = surfaceTransform,
                textureTransform = previewTransform,
                rectsCoordinates = rectsCoordinates,
                allRectsCount = markerRects.size + otherRects.size,
                otherRectsCount = otherRects.size
            )
        if (!success) return

        frameUpdateListener?.let { (executor, listener) ->
            try {
                executor.execute { listener(timestampNs) }
            } catch (e: RejectedExecutionException) {
                Timber.tag("OGL").i("Unable to send frame update. Ignore.")
            }
        }
    }

    /**
     * Calculates the dimensions of the source texture after it has been transformed from the raw
     * sensor texture to an image which is in the device's 'natural' orientation.
     *
     * The required transform is passed along with each texture update and is retrieved from [ ]
     * [SurfaceTexture.getTransformMatrix].
     *
     * <pre>`TEXTURE FROM SENSOR: ^ | | .########### | *********** | ....############## ####. /
     * Sensor may be rotated relative | ################### #( )#. to the device's 'natural' |
     * ############## ###### orientation. | ################### #( )#* | ****############## ####* \
     * | ........... | *########### | +--------------------------------> TRANSFORMED IMAGE: | | ^ |
     * | | . . | | | \\ ........ // Transform matrix from | ##############
     * SurfaceTexture#getTransformMatrix() | ###( )####( )### performs scale/crop/rotate on |
     * #################### image from sensor to produce | ###################### image in 'natural'
     * orientation. | .. ...................... .. | | |#### ###################### #### | +-------\
     * |#### ###################### #### +---------/ |#### ###################### ####
     * +--------------------------------> `</pre> *
     *
     * The transform matrix is a 4x4 affine transform matrix that operates on standard normalized
     * texture coordinates which are in the range of [0,1] for both s and t dimensions. Once the
     * transform is applied, we scale by the width and height of the source texture.
     */
    @WorkerThread
    private fun calculateInputDimensions() {

        // Although the transform is normally used to rotate, it can also handle scale and
        // translation.
        // In order to accommodate for this, we use test vectors representing the boundaries of the
        // input, and run them through the transform to find the boundaries of the output.
        //
        //                                Top Bound (Vt):    Right Bound (Vr):
        //
        //                                ^ (0.5,1)             ^
        //                                |    ^                |
        //                                |    |                |
        //                                |    |                |        (1,0.5)
        //          Texture               |    +                |     +---->
        //          Coordinates:          |                     |
        //          ^                     |                     |
        //          |                     +----------->         +----------->
        //        (0,1)     (1,1)
        //          +---------+           Bottom Bound (Vb):     Left Bound (Vl):
        //          |         |
        //          |         |           ^                     ^
        //          |    +    |           |                     |
        //          |(0.5,0.5)|           |                     |
        //          |         |           |                  (0,0.5)
        //          +------------>        |    +                <----+
        //        (0,0)     (1,0)         |    |                |
        //                                |    |                |
        //                                +----v------>         +----------->
        //                                  (0.5,0)
        //
        // Using the above test vectors, we can calculate the transformed height using transform
        // matrix M as:
        //
        // Voh = |M x (Vt * h) - M x (Vb * h)| = |M x (Vt - Vb) * h| = |M x Vih| = |M x [0 h 0 0]|
        // where:
        // Vih = input, pre-transform height vector,
        // Voh = output transformed height vector,
        //   h = pre-transform texture height,
        //  || denotes element-wise absolute value,
        //   x denotes matrix-vector multiplication, and
        //   * denotes element-wise multiplication.
        //
        // Similarly, the transformed width will be calculated as:
        //
        // Vow = |M x (Vr * w) - M x (Vl * w)| = |M x (Vr - Vl) * w| = |M x Viw| = |M x [w 0 0 0]|
        // where:
        // Vow = output transformed width vector, and w = pre-transform texture width
        //
        // Since the transform matrix can potentially swap width and height, we must hold on to both
        // elements of each output vector. However, since we assume rotations in multiples of 90
        // degrees, and the vectors are orthogonal, we can calculate the final transformed vector
        // as:
        //
        // Vo = |M x Vih| + |M x Viw|

        // Initialize the components we care about for the output vector. This will be
        // accumulated from
        // Voh and Vow.
        naturalPreviewWidth = 0f
        naturalPreviewHeight = 0f

        val previewResolution = requireNotNull(this.previewResolution)

        // Calculate Voh. We use our allocated temporary vector to avoid excessive allocations since
        // this is done per-frame.
        val vih = tempVec
        vih[0] = 0f
        vih[1] = previewResolution.height.toFloat()
        vih[2] = 0f
        vih[3] = 0f

        // Apply the transform. Second half of the array is the result vector Voh.
        Matrix.multiplyMV(
            /*resultVec=*/ tempVec,
            /*resultVecOffset=*/ 4,
            /*lhsMat=*/ previewTransform,
            /*lhsMatOffset=*/ 0,
            /*rhsVec=*/ vih,
            /*rhsVecOffset=*/ 0
        )

        // Accumulate output from Voh.
        naturalPreviewWidth += abs(tempVec[4])
        naturalPreviewHeight += abs(tempVec[5])

        // Calculate Vow.
        val voh = tempVec
        voh[0] = previewResolution.width.toFloat()
        voh[1] = 0f
        voh[2] = 0f
        voh[3] = 0f

        // Apply the transform. Second half of the array is the result vector Vow.
        Matrix.multiplyMV(
            /*resultVec=*/ tempVec,
            /*resultVecOffset=*/ 4,
            /*lhsMat=*/ previewTransform,
            /*lhsMatOffset=*/ 0,
            /*rhsVec=*/ voh,
            /*rhsVecOffset=*/ 0
        )

        // Accumulate output from Vow. This now represents the fully transformed coordinates.
        naturalPreviewWidth += abs(tempVec[4])
        naturalPreviewHeight += abs(tempVec[5])
    }

    /**
     * Calculates the vertex shader transform matrix needed to transform the output from device
     * 'natural' orientation coordinates to a "center-crop" view of the camera viewport.
     *
     * A device's 'natural' orientation is the orientation where the Display rotation is
     * Surface.ROTATION_0. For most phones, this will be a portrait orientation, whereas some
     * tablets may use landscape as their natural orientation. The Surface rotation is always
     * provided relative to the device's 'natural' orientation.
     *
     * Because the camera sensor (or crop of the camera sensor) may have a different aspect ratio
     * than the Surface that is meant to display it, we also want to fit the image from the camera
     * so the entire Surface is filled. This generally requires scaling the input texture and
     * cropping pixels from either the width or height. We call this transform "center-crop" and is
     * equivalent to the ScaleType with the same name in ImageView.
     */
    @WorkerThread
    private fun calculateSurfaceTransform() {
        // Calculate the dimensions of the source texture in the 'natural' orientation of the
        // device.
        calculateInputDimensions()

        // Transform surface width and height to natural orientation
        Matrix.setRotateM(surfaceTransform, 0, -surfaceRotationDegrees.toFloat(), 0f, 0f, 1.0f)

        // Since rotation is a linear transform, we don't need to worry about the affine component
        val surfaceSize = requireNotNull(this.surfaceSize)
        tempVec[0] = surfaceSize.width.toFloat()
        tempVec[1] = surfaceSize.height.toFloat()

        // Apply the transform to surface dimensions
        Matrix.multiplyMV(tempVec, 4, surfaceTransform, 0, tempVec, 0)
        val naturalSurfaceWidth = abs(tempVec[4])
        val naturalSurfaceHeight = abs(tempVec[5])

        // Now that both preview and surface are in the same coordinate system, calculate the ratio
        // of width/height between preview/surface to determine which dimension to scale
        val heightRatio = naturalPreviewHeight / naturalSurfaceHeight
        val widthRatio = naturalPreviewWidth / naturalSurfaceWidth

        // Now that we have calculated scale, we must apply rotation and scale in the correct order
        // such that it will apply to the vertex shader's vertices consistently.
        Matrix.setIdentityM(surfaceTransform, 0)

        // Apply the scale depending on whether the width or the height needs to be scaled to match
        // a "center crop" scale type. Because vertex coordinates are already normalized, we must
        // remove
        // the implicit scaling (through division) before scaling by the opposite dimension.
        if (naturalPreviewWidth * naturalSurfaceHeight > naturalPreviewHeight * naturalSurfaceWidth
        ) {
            Matrix.scaleM(surfaceTransform, 0, heightRatio / widthRatio, 1.0f, 1.0f)
        } else {
            Matrix.scaleM(surfaceTransform, 0, 1.0f, widthRatio / heightRatio, 1.0f)
        }

        // Finally add in rotation. This will be applied to vertices first.
        Matrix.rotateM(surfaceTransform, 0, -surfaceRotationDegrees.toFloat(), 0f, 0f, 1.0f)
    }

    @WorkerThread private external fun initContext(): Long

    @WorkerThread
    private external fun setWindowSurface(nativeContext: Long, surface: Surface?): Boolean

    @WorkerThread private external fun getTexName(nativeContext: Long): Int

    @WorkerThread
    private external fun renderTexture(
        nativeContext: Long,
        timestampNs: Long,
        vertexTransform: FloatArray,
        textureTransform: FloatArray,
        rectsCoordinates: FloatArray,
        allRectsCount: Int,
        otherRectsCount: Int
    ): Boolean

    @WorkerThread private external fun closeContext(nativeContext: Long)

    @WorkerThread
    private external fun setBlurEnabled(nativeContext: Long, enabled: Boolean, animated: Boolean)

    @WorkerThread
    private external fun setContrastingColor(
        nativeContext: Long,
        red: Float,
        green: Float,
        blue: Float
    )
}

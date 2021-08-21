package com.lookaround.core.android.camera;

import android.annotation.SuppressLint;
import android.graphics.SurfaceTexture;
import android.opengl.Matrix;
import android.os.Build;
import android.os.Process;
import android.util.Size;
import android.view.Surface;
import android.view.ViewStub;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.view.PreviewView;
import androidx.camera.view.internal.compat.quirk.DeviceQuirks;
import androidx.camera.view.internal.compat.quirk.SurfaceViewStretchedQuirk;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;
import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import timber.log.Timber;

public final class OpenGLRenderer {
    static {
        System.loadLibrary("opengl_renderer_jni");
    }

    private static final AtomicInteger RENDERER_COUNT = new AtomicInteger(0);
    private final SingleThreadHandlerExecutor executor =
            new SingleThreadHandlerExecutor(
                    String.format(Locale.US, "GLRenderer-%03d", RENDERER_COUNT.incrementAndGet()),
                    Process.THREAD_PRIORITY_DEFAULT); // Use UI thread priority (DEFAULT)

    private Size previewResolution;
    private SurfaceTexture previewTexture;
    private final float[] previewTransform = new float[16];
    private float naturalPreviewWidth = 0;
    private float naturalPreviewHeight = 0;

    private Size surfaceSize = null;
    private int surfaceRotationDegrees = 0;
    private final float[] surfaceTransform = new float[16];

    private final float[] tempVec = new float[8];
    private long nativeContext = 0;

    private boolean isShutdown = false;
    private int numOutstandingSurfaces = 0;

    private Pair<Executor, Consumer<Long>> frameUpdateListener;

    private final AtomicReference<PreviewStreamStateObserver> activeStreamStateObserver =
            new AtomicReference<>();

    @NonNull
    private final MutableLiveData<PreviewView.StreamState> previewStreamStateLiveData =
            new MutableLiveData<>(PreviewView.StreamState.IDLE);

    @NonNull
    public LiveData<PreviewView.StreamState> getPreviewStreamStateLiveData() {
        return previewStreamStateLiveData;
    }

    @MainThread
    public void setBlurEnabled(boolean enabled) {
        try {
            executor.execute(
                    () -> {
                        if (isShutdown || nativeContext == 0) return;
                        setBlurEnabled(nativeContext, enabled);
                    });
        } catch (RejectedExecutionException e) {
            Timber.tag("OGL").i("Renderer already shutting down. Ignore.");
        }
    }

    @SuppressLint("RestrictedApi")
    @SuppressWarnings("WeakerAccess")
    private static boolean shouldUseTextureView(@NonNull SurfaceRequest surfaceRequest) {
        boolean isLegacyDevice = surfaceRequest.getCamera().getCameraInfoInternal()
                .getImplementationType().equals(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2_LEGACY);
        boolean hasSurfaceViewQuirk = DeviceQuirks.get(SurfaceViewStretchedQuirk.class) != null;
        // Force to use TextureView when the device is running android 7.0 and below, legacy
        // level, RGBA8888 is required or SurfaceView has quirks.
        return surfaceRequest.isRGBA8888Required()
                || Build.VERSION.SDK_INT <= 24
                || isLegacyDevice
                || hasSurfaceViewQuirk;
    }

    @SuppressLint("RestrictedApi")
    @MainThread
    public void attachInputPreview(@NonNull Preview preview, @NonNull ViewStub viewFinderStub) {
        preview.setSurfaceProvider(
                executor,
                surfaceRequest -> {
                    if (isShutdown) {
                        surfaceRequest.willNotProvideSurface();
                        return;
                    }

                    CameraInternal camera = surfaceRequest.getCamera();
                    boolean useTextureView = shouldUseTextureView(surfaceRequest);
                    final IRenderSurface renderSurface = useTextureView
                            ? new TextureViewRenderSurface()
                            : new SurfaceViewRenderSurface();

                    viewFinderStub.post(() -> renderSurface.inflateWith(viewFinderStub, this));

                    PreviewStreamStateObserver streamStateObserver =
                            new PreviewStreamStateObserver(camera.getCameraInfoInternal(),
                                    previewStreamStateLiveData, renderSurface);
                    camera.getCameraState().addObserver(
                            ContextCompat.getMainExecutor(viewFinderStub.getContext()), streamStateObserver);
                    activeStreamStateObserver.set(streamStateObserver);

                    if (nativeContext == 0) {
                        nativeContext = initContext();
                    }

                    SurfaceTexture surfaceTexture = resetPreviewTexture(
                            surfaceRequest.getResolution());
                    Surface inputSurface = new Surface(surfaceTexture);
                    numOutstandingSurfaces++;
                    surfaceRequest.provideSurface(
                            inputSurface,
                            executor,
                            result -> {
                                inputSurface.release();
                                surfaceTexture.release();
                                if (surfaceTexture == previewTexture) {
                                    previewTexture = null;
                                }
                                numOutstandingSurfaces--;
                                doShutdownIfNeeded();

                                if (activeStreamStateObserver.compareAndSet(streamStateObserver, null)) {
                                    streamStateObserver.updatePreviewStreamState(PreviewView.StreamState.IDLE);
                                }
                                streamStateObserver.clear();
                                camera.getCameraState().removeObserver(streamStateObserver);
                            });
                });
    }

    public void attachOutputSurface(
            @NonNull Surface surface, @NonNull Size surfaceSize, int surfaceRotationDegrees) {
        try {
            executor.execute(
                    () -> {
                        if (isShutdown) {
                            return;
                        }

                        if (nativeContext == 0) {
                            nativeContext = initContext();
                        }

                        setBlurEnabled(nativeContext, true);

                        if (setWindowSurface(nativeContext, surface)) {
                            this.surfaceRotationDegrees = surfaceRotationDegrees;
                            this.surfaceSize = surfaceSize;
                        } else {
                            this.surfaceSize = null;
                        }

                    });
        } catch (RejectedExecutionException e) {
            Timber.tag("OGL").i("Renderer already shutting down. Ignore.");
        }
    }


    /**
     * Sets a listener to receive updates when a frame has been drawn to the output {@link Surface}.
     *
     * <p>Frame updates include the timestamp of the latest drawn frame.
     *
     * @param executor Executor used to call the listener.
     * @param listener Listener which receives updates in the form of a timestamp (in nanoseconds).
     */
    public void setFrameUpdateListener(@NonNull Executor executor, @NonNull Consumer<Long> listener) {
        try {
            this.executor.execute(() -> frameUpdateListener = new Pair<>(executor, listener));
        } catch (RejectedExecutionException e) {
            Timber.tag("OGL").i("Renderer already shutting down. Ignore.");
        }
    }

    public void invalidateSurface(int surfaceRotationDegrees) {
        try {
            executor.execute(
                    () -> {
                        this.surfaceRotationDegrees = surfaceRotationDegrees;
                        if (previewTexture != null && nativeContext != 0) {
                            renderLatest();
                        }
                    });
        } catch (RejectedExecutionException e) {
            Timber.tag("OGL").i("Renderer already shutting down. Ignore.");
        }
    }

    /**
     * Detach the current output surface from the renderer.
     *
     * @return A {@link ListenableFuture} that signals detach from the renderer. Some devices may
     * not be able to handle the surface being released while still attached to an EGL context.
     * It should be safe to release resources associated with the output surface once this future
     * has completed.
     */
    public ListenableFuture<Void> detachOutputSurface() {
        return CallbackToFutureAdapter.getFuture(completer -> {
            try {
                executor.execute(
                        () -> {
                            if (nativeContext != 0) {
                                setWindowSurface(nativeContext, null);
                                surfaceSize = null;
                            }
                            completer.set(null);
                        });
            } catch (RejectedExecutionException e) {
                // Renderer is shutting down. Can notify that the surface is detached.
                completer.set(null);
            }
            return "detachOutputSurface [" + this + "]";
        });
    }

    public void shutdown() {
        try {
            executor.execute(
                    () -> {
                        isShutdown = true;
                        if (nativeContext != 0) {
                            closeContext(nativeContext);
                            nativeContext = 0;
                        }
                        doShutdownIfNeeded();
                    });
        } catch (RejectedExecutionException e) {
            Timber.tag("OGL").i("Renderer already shutting down. Ignore.");
        }
    }

    @WorkerThread
    private void doShutdownIfNeeded() {
        if (isShutdown && numOutstandingSurfaces == 0) {
            frameUpdateListener = null;
            executor.shutdown();
        }
    }

    @WorkerThread
    @NonNull
    private SurfaceTexture resetPreviewTexture(@NonNull Size size) {
        if (previewTexture != null) {
            previewTexture.detachFromGLContext();
        }

        previewTexture = new SurfaceTexture(getTexName(nativeContext));
        previewTexture.setDefaultBufferSize(size.getWidth(), size.getHeight());
        previewTexture.setOnFrameAvailableListener(
                surfaceTexture -> {
                    if (surfaceTexture == previewTexture && nativeContext != 0) {
                        surfaceTexture.updateTexImage();
                        renderLatest();
                    }
                },
                executor.getHandler());
        previewResolution = size;
        return previewTexture;
    }

    @WorkerThread
    private void renderLatest() {
        // Get the timestamp so we can pass it along to the output surface (not strictly necessary)
        long timestampNs = previewTexture.getTimestamp();

        // Get texture transform from surface texture (transform to natural orientation).
        // This will be used to transform texture coordinates in the fragment shader.
        previewTexture.getTransformMatrix(previewTransform);
        if (surfaceSize != null) {
            calculateSurfaceTransform();
            boolean success = renderTexture(nativeContext, timestampNs, surfaceTransform,
                    previewTransform);
            if (success && frameUpdateListener != null) {
                Executor executor = frameUpdateListener.first;
                Consumer<Long> listener = frameUpdateListener.second;
                try {
                    executor.execute(() -> listener.accept(timestampNs));
                } catch (RejectedExecutionException e) {
                    Timber.tag("OGL").i("Unable to send frame update. Ignore.");
                }
            }
        }
    }

    /**
     * Calculates the dimensions of the source texture after it has been transformed from the raw
     * sensor texture to an image which is in the device's 'natural' orientation.
     *
     * <p>The required transform is passed along with each texture update and is retrieved from
     * {@link
     * SurfaceTexture#getTransformMatrix(float[])}.
     *
     * <pre>{@code
     *        TEXTURE FROM SENSOR:
     * ^
     * |
     * |          .###########
     * |           ***********
     * |   ....############## ####. /           Sensor may be rotated relative
     * |  ################### #( )#.            to the device's 'natural'
     * |       ############## ######            orientation.
     * |  ################### #( )#*
     * |   ****############## ####* \
     * |           ...........
     * |          *###########
     * |
     * +-------------------------------->
     *                                               TRANSFORMED IMAGE:
     *                 | |                   ^
     *                 | |                   |         .            .
     *                 | |                   |         \\ ........ //
     *   Transform matrix from               |         ##############
     *   SurfaceTexture#getTransformMatrix() |       ###(  )####(  )###
     *   performs scale/crop/rotate on       |      ####################
     *   image from sensor to produce        |     ######################
     *   image in 'natural' orientation.     | ..  ......................  ..
     *                 | |                   |#### ###################### ####
     *                 | +-------\           |#### ###################### ####
     *                 +---------/           |#### ###################### ####
     *                                       +-------------------------------->
     * }</pre>
     *
     * <p>The transform matrix is a 4x4 affine transform matrix that operates on standard normalized
     * texture coordinates which are in the range of [0,1] for both s and t dimensions. Once the
     * transform is applied, we scale by the width and height of the source texture.
     */
    @WorkerThread
    private void calculateInputDimensions() {

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
        naturalPreviewWidth = 0;
        naturalPreviewHeight = 0;

        // Calculate Voh. We use our allocated temporary vector to avoid excessive allocations since
        // this is done per-frame.
        float[] vih = tempVec;
        vih[0] = 0;
        vih[1] = previewResolution.getHeight();
        vih[2] = 0;
        vih[3] = 0;

        // Apply the transform. Second half of the array is the result vector Voh.
        Matrix.multiplyMV(
                /*resultVec=*/ tempVec, /*resultVecOffset=*/ 4,
                /*lhsMat=*/ previewTransform, /*lhsMatOffset=*/ 0,
                /*rhsVec=*/ vih, /*rhsVecOffset=*/ 0);

        // Accumulate output from Voh.
        naturalPreviewWidth += Math.abs(tempVec[4]);
        naturalPreviewHeight += Math.abs(tempVec[5]);

        // Calculate Vow.
        float[] voh = tempVec;
        voh[0] = previewResolution.getWidth();
        voh[1] = 0;
        voh[2] = 0;
        voh[3] = 0;

        // Apply the transform. Second half of the array is the result vector Vow.
        Matrix.multiplyMV(
                /*resultVec=*/ tempVec,
                /*resultVecOffset=*/ 4,
                /*lhsMat=*/ previewTransform,
                /*lhsMatOffset=*/ 0,
                /*rhsVec=*/ voh,
                /*rhsVecOffset=*/ 0);

        // Accumulate output from Vow. This now represents the fully transformed coordinates.
        naturalPreviewWidth += Math.abs(tempVec[4]);
        naturalPreviewHeight += Math.abs(tempVec[5]);
    }

    /**
     * Calculates the vertex shader transform matrix needed to transform the output from device
     * 'natural' orientation coordinates to a "center-crop" view of the camera viewport.
     *
     * <p>A device's 'natural' orientation is the orientation where the Display rotation is
     * Surface.ROTATION_0. For most phones, this will be a portrait orientation, whereas some
     * tablets
     * may use landscape as their natural orientation. The Surface rotation is always provided
     * relative to the device's 'natural' orientation.
     *
     * <p>Because the camera sensor (or crop of the camera sensor) may have a different aspect ratio
     * than the Surface that is meant to display it, we also want to fit the image from the
     * camera so
     * the entire Surface is filled. This generally requires scaling the input texture and cropping
     * pixels from either the width or height. We call this transform "center-crop" and is
     * equivalent
     * to the ScaleType with the same name in ImageView.
     */
    @WorkerThread
    private void calculateSurfaceTransform() {
        // Calculate the dimensions of the source texture in the 'natural' orientation of the
        // device.
        calculateInputDimensions();

        // Transform surface width and height to natural orientation
        Matrix.setRotateM(surfaceTransform, 0, -surfaceRotationDegrees, 0, 0, 1.0f);

        // Since rotation is a linear transform, we don't need to worry about the affine component
        tempVec[0] = surfaceSize.getWidth();
        tempVec[1] = surfaceSize.getHeight();

        // Apply the transform to surface dimensions
        Matrix.multiplyMV(tempVec, 4, surfaceTransform, 0, tempVec, 0);

        float naturalSurfaceWidth = Math.abs(tempVec[4]);
        float naturalSurfaceHeight = Math.abs(tempVec[5]);

        // Now that both preview and surface are in the same coordinate system, calculate the ratio
        // of width/height between preview/surface to determine which dimension to scale
        float heightRatio = naturalPreviewHeight / naturalSurfaceHeight;
        float widthRatio = naturalPreviewWidth / naturalSurfaceWidth;

        // Now that we have calculated scale, we must apply rotation and scale in the correct order
        // such that it will apply to the vertex shader's vertices consistently.
        Matrix.setIdentityM(surfaceTransform, 0);

        // Apply the scale depending on whether the width or the height needs to be scaled to match
        // a "center crop" scale type. Because vertex coordinates are already normalized, we must
        // remove
        // the implicit scaling (through division) before scaling by the opposite dimension.
        if (naturalPreviewWidth * naturalSurfaceHeight
                > naturalPreviewHeight * naturalSurfaceWidth) {
            Matrix.scaleM(surfaceTransform, 0, heightRatio / widthRatio, 1.0f, 1.0f);
        } else {
            Matrix.scaleM(surfaceTransform, 0, 1.0f, widthRatio / heightRatio, 1.0f);
        }

        // Finally add in rotation. This will be applied to vertices first.
        Matrix.rotateM(surfaceTransform, 0, -surfaceRotationDegrees, 0, 0, 1.0f);
    }

    @WorkerThread
    private static native long initContext();

    @WorkerThread
    private static native boolean setWindowSurface(long nativeContext, @Nullable Surface surface);

    @WorkerThread
    private static native int getTexName(long nativeContext);

    @WorkerThread
    private static native boolean renderTexture(
            long nativeContext,
            long timestampNs,
            @NonNull float[] vertexTransform,
            @NonNull float[] textureTransform);

    @WorkerThread
    private static native void closeContext(long nativeContext);

    @WorkerThread
    private static native void setBlurEnabled(long nativeContext, boolean enabled);
}

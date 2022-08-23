package com.lookaround.core.android.camera.surface.impl

import android.annotation.SuppressLint
import android.os.Build
import android.util.Size
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewStub
import androidx.camera.core.impl.utils.futures.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.lookaround.core.android.R
import com.lookaround.core.android.camera.OpenGLRenderer
import com.lookaround.core.android.camera.surface.IRenderSurface
import com.lookaround.core.android.camera.surface.Surfaces
import java.util.concurrent.ExecutionException
import timber.log.Timber

internal class SurfaceViewRenderSurface : IRenderSurface {
    /**
     * Inflates a non-blocking [SurfaceView] into the provided [ViewStub] and attaches it to the
     * provided [OpenGLRenderer].
     *
     * WARNING: This type of render surface should only be used on specific devices. A non-blocking
     * [SurfaceView] will not block the main thread when destroying its internal [Surface], which is
     * known to cause race conditions between the main thread and the rendering thread. Some
     * OpenGL/EGL drivers do not support this usage and may crash on the rendering thread.
     *
     * @param viewStub Stub which will be replaced by SurfaceView.
     * @param renderer Renderer which will be used to update the SurfaceView.
     * @return The inflated SurfaceView.
     */
    fun inflateNonBlockingWith(viewStub: ViewStub, renderer: OpenGLRenderer): SurfaceView =
        inflateWith(viewStub, renderer, /*nonBlocking=*/ true)

    /**
     * Inflates a [SurfaceView] into the provided [ViewStub] and attaches it to the provided
     * [OpenGLRenderer].
     *
     * @param viewStub Stub which will be replaced by SurfaceView.
     * @param renderer Renderer which will be used to update the SurfaceView.
     * @return The inflated SurfaceView.
     */
    override fun inflateWith(viewStub: ViewStub, renderer: OpenGLRenderer): SurfaceView =
        inflateWith(viewStub, renderer, /*nonBlocking=*/ false)

    private fun inflateWith(
        viewStub: ViewStub,
        renderer: OpenGLRenderer,
        nonBlocking: Boolean
    ): SurfaceView {
        Timber.d("Inflating SurfaceView into view stub (non-blocking = $nonBlocking).")
        if (nonBlocking) warnOnKnownBuggyNonBlockingDevice()

        viewStub.layoutResource = R.layout.surface_view_render_surface
        val surfaceView = viewStub.inflate() as SurfaceView
        surfaceView.holder.addCallback(
            object : SurfaceHolder.Callback2 {
                override fun surfaceRedrawNeeded(holder: SurfaceHolder) {
                    val surfaceViewDisplay = surfaceView.display
                    if (surfaceViewDisplay != null) {
                        renderer.invalidateSurface(
                            Surfaces.toSurfaceRotationDegrees(surfaceViewDisplay.rotation)
                        )
                    }
                }

                override fun surfaceCreated(holder: SurfaceHolder) = Unit

                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int
                ) {
                    renderer.attachOutputSurface(
                        holder.surface,
                        Size(width, height),
                        Surfaces.toSurfaceRotationDegrees(surfaceView.display.rotation)
                    )
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    // SurfaceView's documentation states that the Surface should only be touched
                    // between surfaceCreated() and surfaceDestroyed(). However, many EGL
                    // implementations will allow it to be touched but may return errors during
                    // drawing operations. Other implementations may crash when those drawing
                    // operations are called. In normal operation, we block the main thread until
                    // the surface has been detached from the renderer. This is safe, but can cause
                    // jank and/or ANRs. In non-blocking mode, we signal to the renderer to detach
                    // but do not wait for a signal that the surface has been detached. This will
                    // work on some devices with more robust EGL implementations. For devices with
                    // crashing EGL implementations TextureView is an alternative which provides
                    // stable non-blocking behavior between the main thread and render thread.
                    val detachFuture = renderer.detachOutputSurface()
                    if (!nonBlocking) {
                        try {
                            detachFuture.get()
                        } catch (e: ExecutionException) {
                            Timber.e(
                                e.cause,
                                "An error occurred while waiting for surface to detach from the renderer"
                            )
                        } catch (e: InterruptedException) {
                            Timber.e(
                                "Interrupted while waiting for surface to detach from the renderer."
                            )
                            Thread.currentThread().interrupt() // Restore the interrupted status
                        }
                    }
                }
            }
        )
        return surfaceView
    }

    @SuppressLint("RestrictedApi")
    override fun waitForNextFrame(): ListenableFuture<Unit> = Futures.immediateFuture(Unit)

    companion object {
        private const val TAG = "SurfaceViewRndrSrfc"

        private fun warnOnKnownBuggyNonBlockingDevice() {
            // Cuttlefish currently uses swiftshader for its OpenGL and EGL implementations.
            // Swiftshader is not thread-safe, and sometimes will crash in OpenGL or EGL calls if
            // the consumer has already been detached. See b/74108717 for more info.
            if (Build.MODEL.contains("Cuttlefish")) {
                Timber.tag(TAG)
                    .w(
                        "Running SurfaceView in non-blocking mode on a device with known buggy EGL implementation: Cuttlefish"
                    )
            }
        }
    }
}

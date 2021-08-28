package com.lookaround.core.android.camera

import android.graphics.SurfaceTexture
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import android.view.ViewStub
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.lookaround.core.android.R
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference

internal class TextureViewRenderSurface : IRenderSurface {
    private val nextFrameCompleter = AtomicReference<CallbackToFutureAdapter.Completer<Unit>?>()

    override fun inflateWith(viewStub: ViewStub, renderer: OpenGLRenderer): TextureView {
        Timber.tag(TAG).d("Inflating TextureView into view stub.")
        viewStub.layoutResource = R.layout.texture_view_render_surface
        val textureView = viewStub.inflate() as TextureView
        textureView.surfaceTextureListener =
            object : SurfaceTextureListener {
                private lateinit var surface: Surface

                override fun onSurfaceTextureAvailable(
                    st: SurfaceTexture,
                    width: Int,
                    height: Int
                ) {
                    surface = Surface(st)
                    renderer.attachOutputSurface(
                        surface,
                        Size(width, height),
                        Surfaces.toSurfaceRotationDegrees(textureView.display.rotation)
                    )
                }

                override fun onSurfaceTextureSizeChanged(
                    st: SurfaceTexture,
                    width: Int,
                    height: Int
                ) {
                    renderer.attachOutputSurface(
                        surface,
                        Size(width, height),
                        Surfaces.toSurfaceRotationDegrees(textureView.display.rotation)
                    )
                }

                override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                    renderer
                        .detachOutputSurface()
                        .addListener(
                            {
                                surface.release()
                                st.release()
                            },
                            ContextCompat.getMainExecutor(textureView.context)
                        )
                    return false
                }

                override fun onSurfaceTextureUpdated(st: SurfaceTexture) {
                    val completer = nextFrameCompleter.getAndSet(null)
                    completer?.set(null)
                }
            }
        return textureView
    }

    override fun waitForNextFrame(): ListenableFuture<Unit> =
        CallbackToFutureAdapter.getFuture { completer ->
            nextFrameCompleter.set(completer)
            "textureViewImpl_waitForNextFrame"
        }

    companion object {
        private const val TAG = "TextureViewRndrSrfc"
    }
}

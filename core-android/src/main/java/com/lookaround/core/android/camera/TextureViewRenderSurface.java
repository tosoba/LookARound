package com.lookaround.core.android.camera;

import android.graphics.SurfaceTexture;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewStub;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.lookaround.core.android.R;

/**
 * Utilities for instantiating a {@link TextureView} and attaching to an {@link OpenGLRenderer}.
 */
public final class TextureViewRenderSurface {
    private static final String TAG = "TextureViewRndrSrfc";

    /**
     * Inflates a {@link TextureView} into the provided {@link ViewStub} and attaches it to the
     * provided {@link OpenGLRenderer}.
     *
     * @param viewStub Stub which will be replaced by TextureView.
     * @param renderer Renderer which will be used to update the TextureView.
     * @return The inflated TextureView.
     */
    @NonNull
    public static TextureView inflateWith(@NonNull ViewStub viewStub,
                                          @NonNull OpenGLRenderer renderer) {
        Log.d(TAG, "Inflating TextureView into view stub.");
        viewStub.setLayoutResource(R.layout.texture_view_render_surface);
        TextureView textureView = (TextureView) viewStub.inflate();
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            private Surface mSurface;

            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture st, int width, int height) {
                mSurface = new Surface(st);
                renderer.attachOutputSurface(mSurface, new Size(width, height),
                        Surfaces.toSurfaceRotationDegrees(textureView.getDisplay().getRotation()));
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture st, int width, int height) {
                renderer.attachOutputSurface(mSurface, new Size(width, height),
                        Surfaces.toSurfaceRotationDegrees(textureView.getDisplay().getRotation()));
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture st) {
                Surface surface = mSurface;
                mSurface = null;
                renderer.detachOutputSurface().addListener(() -> {
                    surface.release();
                    st.release();
                }, ContextCompat.getMainExecutor(textureView.getContext()));
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture st) {
            }
        });

        return textureView;
    }

    private TextureViewRenderSurface() {
    }
}

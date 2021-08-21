/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lookaround.core.android.camera;

import android.graphics.SurfaceTexture;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewStub;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.lookaround.core.android.R;

import java.util.concurrent.atomic.AtomicReference;

import timber.log.Timber;

/**
 * Utilities for instantiating a {@link TextureView} and attaching to an {@link OpenGLRenderer}.
 */
final class TextureViewRenderSurface implements IRenderSurface {
    private static final String TAG = "TextureViewRndrSrfc";

    private final AtomicReference<CallbackToFutureAdapter.Completer<Void>> nextFrameCompleter =
            new AtomicReference<>();

    /**
     * Inflates a {@link TextureView} into the provided {@link ViewStub} and attaches it to the
     * provided {@link OpenGLRenderer}.
     *
     * @param viewStub Stub which will be replaced by TextureView.
     * @param renderer Renderer which will be used to update the TextureView.
     * @return The inflated TextureView.
     */

    @NonNull
    @Override
    public TextureView inflateWith(@NonNull ViewStub viewStub, @NonNull OpenGLRenderer renderer) {
        Timber.tag(TAG).d("Inflating TextureView into view stub.");
        viewStub.setLayoutResource(R.layout.texture_view_render_surface);
        TextureView textureView = (TextureView) viewStub.inflate();
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            private Surface surface;

            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture st, int width, int height) {
                surface = new Surface(st);
                renderer.attachOutputSurface(surface, new Size(width, height),
                        Surfaces.toSurfaceRotationDegrees(textureView.getDisplay().getRotation()));
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture st, int width, int height) {
                renderer.attachOutputSurface(surface, new Size(width, height),
                        Surfaces.toSurfaceRotationDegrees(textureView.getDisplay().getRotation()));
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture st) {
                Surface surface = this.surface;
                this.surface = null;
                renderer.detachOutputSurface().addListener(() -> {
                    surface.release();
                    st.release();
                }, ContextCompat.getMainExecutor(textureView.getContext()));
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture st) {
                CallbackToFutureAdapter.Completer<Void> completer =
                        nextFrameCompleter.getAndSet(null);

                if (completer != null) {
                    completer.set(null);
                }
            }
        });

        return textureView;
    }

    public TextureViewRenderSurface() {
    }

    @NonNull
    @Override
    public ListenableFuture<Void> waitForNextFrame() {
        return CallbackToFutureAdapter.getFuture(
                completer -> {
                    nextFrameCompleter.set(completer);
                    return "textureViewImpl_waitForNextFrame";
                }
        );
    }
}

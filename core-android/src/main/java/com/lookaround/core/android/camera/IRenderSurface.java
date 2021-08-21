package com.lookaround.core.android.camera;

import android.view.View;
import android.view.ViewStub;

import androidx.annotation.NonNull;

import com.google.common.util.concurrent.ListenableFuture;

interface IRenderSurface {
    @NonNull
    ListenableFuture<Void> waitForNextFrame();

    @NonNull
    View inflateWith(ViewStub viewStub, OpenGLRenderer renderer);
}

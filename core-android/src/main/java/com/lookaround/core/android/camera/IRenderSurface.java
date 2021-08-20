package com.lookaround.core.android.camera;

import androidx.annotation.NonNull;

import com.google.common.util.concurrent.ListenableFuture;

public interface IRenderSurface {
    @NonNull
    ListenableFuture<Void> waitForNextFrame();
}

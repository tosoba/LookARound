package com.lookaround.core.android.camera;

import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

final class SingleThreadHandlerExecutor implements Executor {
    private final String mThreadName;
    private final HandlerThread mHandlerThread;
    private final Handler mHandler;

    public SingleThreadHandlerExecutor(@NonNull String threadName, int priority) {
        this.mThreadName = threadName;
        mHandlerThread = new HandlerThread(threadName, priority);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    @NonNull
    public Handler getHandler() {
        return mHandler;
    }

    @Override
    public void execute(@NonNull Runnable command) {
        if (!mHandler.post(command)) {
            throw new RejectedExecutionException(mThreadName + " is shutting down.");
        }
    }

    public boolean shutdown() {
        return mHandlerThread.quitSafely();
    }
}

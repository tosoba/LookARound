package com.lookaround.core.android.camera;

import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

final class SingleThreadHandlerExecutor implements Executor {
    private final String threadName;
    private final HandlerThread handlerThread;
    private final Handler handler;

    public SingleThreadHandlerExecutor(@NonNull String threadName, int priority) {
        this.threadName = threadName;
        handlerThread = new HandlerThread(threadName, priority);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @NonNull
    public Handler getHandler() {
        return handler;
    }

    @Override
    public void execute(@NonNull Runnable command) {
        if (!handler.post(command)) {
            throw new RejectedExecutionException(threadName + " is shutting down.");
        }
    }

    public boolean shutdown() {
        return handlerThread.quitSafely();
    }
}

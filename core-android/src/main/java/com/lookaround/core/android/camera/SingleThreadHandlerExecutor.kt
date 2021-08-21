package com.lookaround.core.android.camera

import android.os.Handler
import android.os.HandlerThread
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException

internal class SingleThreadHandlerExecutor(private val threadName: String, priority: Int) :
    Executor {
    private val handlerThread: HandlerThread = HandlerThread(threadName, priority)
    val handler: Handler

    init {
        handlerThread.start()
        handler = Handler(handlerThread.looper)
    }

    override fun execute(command: Runnable) {
        if (!handler.post(command)) {
            throw RejectedExecutionException("$threadName is shutting down.")
        }
    }

    fun shutdown(): Boolean = handlerThread.quitSafely()
}

package com.lookaround.core.android.ext

import com.mapzen.tangram.MapController
import com.mapzen.tangram.MapView
import com.mapzen.tangram.networking.HttpHandler
import com.mapzen.tangram.viewholder.GLSurfaceViewHolderFactory
import com.mapzen.tangram.viewholder.GLViewHolderFactory
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object GetMapException : Throwable()

suspend fun MapView.init(
    httpHandler: HttpHandler? = null,
    glViewHolderFactory: GLViewHolderFactory = GLSurfaceViewHolderFactory()
): MapController = suspendCoroutine { continuation ->
    getMapAsync(
        { mapController ->
            mapController?.let { continuation.resume(it) }
                ?: continuation.resumeWithException(GetMapException)
        },
        glViewHolderFactory,
        httpHandler
    )
}

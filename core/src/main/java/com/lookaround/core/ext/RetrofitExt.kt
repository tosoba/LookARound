package com.lookaround.core.ext

import com.lookaround.core.exception.EmptyResponseException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

suspend fun <T> Call<T>.await(): T = suspendCoroutine {
    enqueue(object : Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            response.body()?.let(it::resume) ?: it.resumeWithException(EmptyResponseException)
        }

        override fun onFailure(call: Call<T>, t: Throwable) {
            it.resumeWithException(t)
        }
    })
}

package com.lookaround.core.android.camera

import android.annotation.SuppressLint
import androidx.annotation.GuardedBy
import androidx.annotation.MainThread
import androidx.camera.core.CameraInfo
import androidx.camera.core.Logger
import androidx.camera.core.impl.*
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.impl.utils.futures.FutureCallback
import androidx.camera.core.impl.utils.futures.FutureChain
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.view.PreviewView.StreamState
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableStateFlow

@SuppressLint("RestrictedApi")
internal class PreviewStreamStateObserver(
    private val cameraInfoInternal: CameraInfoInternal,
    private val previewStreamStateFlow: MutableStateFlow<StreamState>,
    private val renderSurface: IRenderSurface
) : Observable.Observer<CameraInternal.State?> {
    @GuardedBy("this") private var previewStreamState: StreamState? = null
    private var flowFuture: ListenableFuture<Unit>? = null
    private var hasStartedPreviewStreamFlow = false

    init {
        synchronized(this) { previewStreamState = previewStreamStateFlow.value }
    }

    @MainThread
    override fun onNewData(value: CameraInternal.State?) {
        when (value) {
            CameraInternal.State.CLOSING,
            CameraInternal.State.CLOSED,
            CameraInternal.State.RELEASING,
            CameraInternal.State.RELEASED -> {
                updatePreviewStreamState(StreamState.IDLE)
                if (hasStartedPreviewStreamFlow) {
                    hasStartedPreviewStreamFlow = false
                    cancelFlow()
                }
            }
            CameraInternal.State.OPENING,
            CameraInternal.State.OPEN,
            CameraInternal.State.PENDING_OPEN -> {
                if (!hasStartedPreviewStreamFlow) {
                    startPreviewStreamStateFlow(cameraInfoInternal)
                    hasStartedPreviewStreamFlow = true
                }
            }
        }
    }

    @MainThread
    override fun onError(t: Throwable) {
        clear()
        updatePreviewStreamState(StreamState.IDLE)
    }

    fun clear() {
        cancelFlow()
    }

    private fun cancelFlow() {
        flowFuture?.cancel(false)
        flowFuture = null
    }

    @SuppressLint("RestrictedApi")
    @MainThread
    private fun startPreviewStreamStateFlow(cameraInfo: CameraInfo) {
        updatePreviewStreamState(StreamState.IDLE)
        val callbacksToClear: MutableList<CameraCaptureCallback> = ArrayList()
        FutureChain.from(waitForCaptureResult(cameraInfo, callbacksToClear))
            .transformAsync({ renderSurface.waitForNextFrame() }, CameraXExecutors.directExecutor())
            .transform(
                { updatePreviewStreamState(StreamState.STREAMING) },
                CameraXExecutors.directExecutor()
            )
            .apply {
                flowFuture = this
                Futures.addCallback(
                    this,
                    object : FutureCallback<Unit?> {
                        override fun onSuccess(result: Unit?) {
                            flowFuture = null
                        }

                        override fun onFailure(t: Throwable) {
                            flowFuture = null
                            if (callbacksToClear.isNotEmpty()) {
                                for (callback in callbacksToClear) {
                                    (cameraInfo as CameraInfoInternal).removeSessionCaptureCallback(
                                        callback
                                    )
                                }
                                callbacksToClear.clear()
                            }
                        }
                    },
                    CameraXExecutors.directExecutor()
                )
            }
    }

    @SuppressLint("RestrictedApi")
    fun updatePreviewStreamState(streamState: StreamState) {
        // Prevent from notifying same states.
        synchronized(this) {
            if (previewStreamState == streamState) return
            previewStreamState = streamState
        }
        Logger.d(TAG, "Update Preview stream state to $streamState")
        previewStreamStateFlow.value = streamState
    }

    /**
     * Returns a ListenableFuture which will complete when the session onCaptureCompleted happens.
     * Please note that the future could complete in background thread.
     */
    @SuppressLint("RestrictedApi")
    private fun waitForCaptureResult(
        cameraInfo: CameraInfo,
        callbacksToClear: MutableList<CameraCaptureCallback>
    ): ListenableFuture<Unit> =
        CallbackToFutureAdapter.getFuture { completer ->
            // The callback will be invoked in camera executor thread.
            val callback: CameraCaptureCallback =
                object : CameraCaptureCallback() {
                    override fun onCaptureCompleted(result: CameraCaptureResult) {
                        completer.set(null)
                        (cameraInfo as CameraInfoInternal).removeSessionCaptureCallback(this)
                    }
                }
            callbacksToClear.add(callback)
            (cameraInfo as CameraInfoInternal).addSessionCaptureCallback(
                CameraXExecutors.directExecutor(),
                callback
            )
            "waitForCaptureResult"
        }

    companion object {
        private const val TAG = "StreamStateObserver"
    }
}

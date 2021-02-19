package com.lookaround.core.android.appunta.view

import android.content.Context
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.util.AttributeSet
import android.view.*
import java.io.IOException

class CameraView : SurfaceView, SurfaceHolder.Callback {
    private var camera: Camera? = null
    private var isPreviewRunning = false
    private var surfaceHolder: SurfaceHolder = holder.apply { addCallback(this@CameraView) }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int)
            : super(context, attrs, defStyle)

    override fun surfaceCreated(holder: SurfaceHolder) {
        camera = Camera.open()
        setCameraDisplayOrientation(camera)
        try {
            camera?.setPreviewDisplay(holder)
        } catch (e1: IOException) {
        }
        camera?.startPreview()
    }

    fun stopPreview() {
        camera?.stopPreview()
    }

    fun startPreview() {
        camera?.startPreview()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (isPreviewRunning) {
            camera?.stopPreview()
        }
        setCameraDisplayOrientation(camera)
        previewCamera()
    }

    fun previewCamera() {
        try {
            camera?.setPreviewDisplay(surfaceHolder)
            camera?.startPreview()
            isPreviewRunning = true
        } catch (e: Exception) {
        }
    }

    override fun surfaceDestroyed(arg0: SurfaceHolder) {
        camera?.stopPreview()
        camera?.release()
    }

    private fun setCameraDisplayOrientation(camera: Camera?) {
        val info = CameraInfo()
        Camera.getCameraInfo(0, info)
        val display = (context.getSystemService(
            Context.WINDOW_SERVICE
        ) as WindowManager).defaultDisplay
        val rotation = display.rotation
        var degrees = 0
        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
        }
        var result: Int
        if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360
            result = (360 - result) % 360 // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360
        }
        setDisplayOrientation(camera, result)
    }

    companion object {
        private fun setDisplayOrientation(camera: Camera?, angle: Int) {
            try {
                camera?.javaClass
                    ?.getMethod("setDisplayOrientation", Int::class.javaPrimitiveType)
                    ?.invoke(camera, angle)
            } catch (e1: Exception) {
            }
        }
    }
}
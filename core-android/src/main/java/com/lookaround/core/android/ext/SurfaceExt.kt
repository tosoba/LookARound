package com.lookaround.core.android.ext

import android.annotation.SuppressLint
import android.os.Build
import androidx.camera.core.CameraInfo
import androidx.camera.core.SurfaceRequest
import androidx.camera.view.internal.compat.quirk.DeviceQuirks
import androidx.camera.view.internal.compat.quirk.SurfaceViewStretchedQuirk

@SuppressLint("RestrictedApi")
fun SurfaceRequest.shouldUseTextureView(): Boolean {
    val isLegacyDevice =
        camera.cameraInfoInternal.implementationType ==
            CameraInfo.IMPLEMENTATION_TYPE_CAMERA2_LEGACY
    val hasSurfaceViewQuirk = DeviceQuirks.get(SurfaceViewStretchedQuirk::class.java) != null
    // Force to use TextureView when the device is running android 7.0 and below, legacy
    // level, RGBA8888 is required or SurfaceView has quirks.
    return isRGBA8888Required ||
        Build.VERSION.SDK_INT <= 24 ||
        isLegacyDevice ||
        hasSurfaceViewQuirk
}

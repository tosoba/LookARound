package com.lookaround.core.android.appunta.view;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.io.IOException;
import java.lang.reflect.Method;

public class CameraView extends SurfaceView implements Callback {
    Camera camera;
    SurfaceHolder previewHolder;
    private boolean isPreviewRunning;

    public CameraView(Context ctx) {
        super(ctx);
        init();
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CameraView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    protected static void setDisplayOrientation(Camera camera, int angle) {
        Method downPolymorphic;
        try {
            downPolymorphic = camera.getClass().getMethod(
                    "setDisplayOrientation", int.class);
            if (downPolymorphic != null)
                downPolymorphic.invoke(camera, angle);
        } catch (Exception e1) {
        }
    }

    private void init() {
        previewHolder = this.getHolder();
        previewHolder.addCallback(this);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        camera = Camera.open();
        setCameraDisplayOrientation(camera);
        try {
            camera.setPreviewDisplay(holder);
        } catch (IOException e1) {
        }
        camera.startPreview();
    }

    public void stopPreview() {
        camera.stopPreview();
    }

    public void startPreview() {
        camera.startPreview();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (isPreviewRunning) {
            camera.stopPreview();
        }
        setCameraDisplayOrientation(camera);
        previewCamera();
    }

    public void previewCamera() {
        try {
            camera.setPreviewDisplay(previewHolder);
            camera.startPreview();
            isPreviewRunning = true;
        } catch (Exception e) {
        }
    }

    public void surfaceDestroyed(SurfaceHolder arg0) {
        camera.stopPreview();
        camera.release();
    }

    private void setCameraDisplayOrientation(Camera camera) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(0, info);
        Display display = ((WindowManager) getContext().getSystemService(
                Context.WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        setDisplayOrientation(camera, result);
    }
}

package com.lookaround.core.android.appunta.view;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

import com.lookaround.core.android.appunta.math3d.Math3dUtil;
import com.lookaround.core.android.appunta.math3d.Trig1;
import com.lookaround.core.android.appunta.math3d.Trig3;
import com.lookaround.core.android.appunta.math3d.Vector1;
import com.lookaround.core.android.appunta.math3d.Vector2;
import com.lookaround.core.android.appunta.math3d.Vector3;
import com.lookaround.core.android.appunta.point.Point;

public class EyeView extends AppuntaView {
    private static final int SCREEN_DEPTH = 1;

    private final Vector3 camRot = new Vector3();
    private final Trig3 camTrig = new Trig3();
    private final Vector3 camPos = new Vector3();
    private final Vector3 pointPos = new Vector3();
    private final Vector3 relativePos = new Vector3();
    private final Vector3 relativeRotPos = new Vector3();

    private final Vector3 screenRatio = new Vector3();

    private final Vector2 screenPos = new Vector2();
    private final Vector2 screenSize = new Vector2();

    private final Vector1 screenRot = new Vector1();
    private final Trig1 screenRotTrig = new Trig1();

    public EyeView(Context context) {
        super(context);
        init();
    }

    public EyeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    EyeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        screenRatio.z = SCREEN_DEPTH;
    }

    @Override
    protected void preRender(Canvas canvas) {
        // For the moment we stablish a square as ratio. Size is arithmetic mean of width and height
        screenRatio.y = (float) (getWidth() + getHeight()) / 2;
        screenRatio.x = (float) (getWidth() + getHeight()) / 2;
        // Get the current size of the window
        screenSize.y = getHeight();
        screenSize.x = getWidth();
        //Obtain the current camera rotation and related calculations based on phone orientation and rotation
        Math3dUtil.getCamRotation(getOrientation(), getPhoneRotation(), camRot, camTrig, screenRot, screenRotTrig);
        //Transform current camera location into a position object;
        Math3dUtil.convertLocationToPosition(getLocation(), camPos);
    }

    @Override
    protected void calculatePointCoordinates(Point point) {
        //Transform point Location into a Position object
        Math3dUtil.convertLocationToPosition(point.getLocation(), pointPos);
        //Calculate relative position to the camera. Transforms angles of latitude and longitude into meters of distance.
        Math3dUtil.getRelativeTranslationInMeters(pointPos, camPos, relativePos);
        //Rotates the point around the camera in order to stablish the camera rotation to <0,0,0>
        Math3dUtil.getRelativeRotation(relativePos, camTrig, relativeRotPos);
        //Converts a 3d position into a 2d position on screen
        boolean drawn = Math3dUtil.convert3dTo2d(relativeRotPos, screenSize, screenRatio, screenRotTrig, screenPos);
        //If drawn is false, the point is behind us, so no need to paint
        if (drawn) {
            point.setX((float) screenPos.x);
            point.setY((float) screenPos.y);
        }
        point.setDrawn(drawn);
    }

    @Override
    protected void postRender(Canvas canvas) {
    }
}
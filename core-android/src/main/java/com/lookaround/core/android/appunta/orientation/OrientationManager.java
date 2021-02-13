package com.lookaround.core.android.appunta.orientation;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * This class is responsible for providing the measure of the compass (in the 3
 * axis) everytime it changes and dealing with the service
 */
public class OrientationManager implements SensorEventListener {
    public static final int MODE_COMPASS = 0;
    public static final int MODE_AR = 1;

    private static final float CIRCLE = (float) (Math.PI * 2);
    private static final float SMOOTH_THRESHOLD = CIRCLE / 3;
    private static final float SMOOTH_FACTOR = .03f;

    // <<<< ORIGINAL VALUES: >>>>
//    private static final float SMOOTH_THRESHOLD = CIRCLE / 6;
//    private static final float SMOOTH_FACTOR = SMOOTH_THRESHOLD / 5;
    private final float[] gravs = new float[3];
    private final float[] geoMags = new float[3];
    private final float[] orientationArray = new float[3];
    private final float[] rotationM = new float[9];
    private final float[] remappedRotationM = new float[9];
    private SensorManager sensorManager;
    private Orientation orientation = new Orientation();
    private Orientation oldOrientation;
    private boolean sensorRunning = false;
    private OnOrientationChangedListener onOrientationChangeListener;
    private int axisMode = MODE_COMPASS;
    private int firstAxis = SensorManager.AXIS_Y;
    private int secondAxis = SensorManager.AXIS_MINUS_X;
    private boolean failed;

    /***
     * This constructor will generate and start a Compass Manager
     *
     * @param activity
     * The activity where the service will work
     */
    public OrientationManager(Activity activity) {
        startSensor(activity);
    }

    /***
     * This constructor will generate a Compass Manager, but it will need to be
     * started manually using {@link #startSensor}
     */
    public OrientationManager() {
    }

    public static int getPhoneRotation(Activity activity) {
        return activity.getWindowManager().getDefaultDisplay().getRotation();
    }

    /***
     * This method registers this class as a listener of the Sensor service
     *
     * @param activity
     *            The activity over this will work
     */
    public void startSensor(Activity activity) {
        if (!sensorRunning) {
            sensorManager = (SensorManager) activity
                    .getSystemService(Context.SENSOR_SERVICE);
            sensorManager.registerListener(this,
                    sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                    SensorManager.SENSOR_DELAY_UI);
            sensorManager.registerListener(this,
                    sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_UI);

            sensorRunning = true;
        }
    }

    /***
     * Detects a change in a sensor and warns the appropiate listener.
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                System.arraycopy(event.values, 0, gravs, 0, 3);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(event.values, 0, geoMags, 0, 3);
                break;
            default:
                return;
        }

        if (SensorManager.getRotationMatrix(rotationM, null, gravs, geoMags)) {
            // Rotate to the camera's line of view (Y axis along the camera's axis)
            SensorManager.remapCoordinateSystem(rotationM, firstAxis, secondAxis, remappedRotationM);
            SensorManager.getOrientation(remappedRotationM, orientationArray);
            onSuccess();
        } else {
            failed = true;
        }
    }

    private void onSuccess() {
        if (failed) failed = false;

        // Convert the azimuth to degrees in 0.5 degree resolution.
        float x = orientationArray[1];
        float y = orientationArray[0];
        float z = orientationArray[2];

//        if (axisMode == MODE_AR)y=y+(getPhoneRotation(activity))*CIRCLE/4;

        if (oldOrientation == null) {
            orientation.setX(x);
            orientation.setY(y);
            orientation.setZ(z);
        } else {
            orientation.setX(lowPass(x, oldOrientation.getX()));
            orientation.setY(lowPass(y, oldOrientation.getY()));
            orientation.setZ(lowPass(z, oldOrientation.getZ()));
        }

        oldOrientation = orientation;

        if (getOnCompassChangeListener() != null) {
            getOnCompassChangeListener().onOrientationChanged(orientation);
        }
    }

    /**
     * Applies a lowpass filter to the change in the lecture of the sensor
     *
     * @param newValue the new sensor value
     * @param lowValue the old sensor value
     * @return and intermediate value
     */
    public float lowPass(float newValue, float lowValue) {
        float lowPassValue;
        if (Math.abs(newValue - lowValue) < CIRCLE / 2) {
            if (Math.abs(newValue - lowValue) > SMOOTH_THRESHOLD) {
                lowPassValue = newValue;
            } else {
                lowPassValue = lowValue + SMOOTH_FACTOR * (newValue - lowValue);
            }
        } else {
            if (CIRCLE - Math.abs(newValue - lowValue) > SMOOTH_THRESHOLD) {
                lowPassValue = newValue;
            } else {
                if (lowValue > newValue) {
                    lowPassValue = (lowValue + SMOOTH_FACTOR
                            * ((CIRCLE + newValue - lowValue) % CIRCLE) + CIRCLE)
                            % CIRCLE;
                } else {
                    lowPassValue = (lowValue - SMOOTH_FACTOR
                            * ((CIRCLE - newValue + lowValue) % CIRCLE) + CIRCLE)
                            % CIRCLE;
                }
            }
        }
        return lowPassValue;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Space for rent
    }

    /***
     * We stop "hearing" the sensors
     */
    public void stopSensor() {
        if (sensorRunning) {
            sensorManager.unregisterListener(this);
            sensorRunning = false;
        }
    }

    /***
     * Just in case, we stop the sensor
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        stopSensor();
    }

    // Setters and getter for the three listeners (Bob, Moe and Curly)

    public OnOrientationChangedListener getOnCompassChangeListener() {
        return onOrientationChangeListener;
    }

    public void setOnOrientationChangeListener(OnOrientationChangedListener onOrientationChangeListener) {
        this.onOrientationChangeListener = onOrientationChangeListener;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
    }

    public int getAxisMode() {
        return axisMode;
    }

    public void setAxisMode(int axisMode) {
        this.axisMode = axisMode;
        if (axisMode == MODE_COMPASS) {
            firstAxis = SensorManager.AXIS_Y;
            secondAxis = SensorManager.AXIS_MINUS_X;
        }
        if (axisMode == MODE_AR) {
            firstAxis = SensorManager.AXIS_X;
            secondAxis = SensorManager.AXIS_Z;
        }
    }

    public interface OnOrientationChangedListener {
        /***
         * This method will be invoked when the magnetic orientation of the
         * phone changed
         *
         * @param orientation
         * Orientation on degrees. 360-0 is north.
         */
        void onOrientationChanged(Orientation orientation);
    }
}

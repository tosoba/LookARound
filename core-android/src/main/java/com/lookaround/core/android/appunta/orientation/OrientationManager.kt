package com.lookaround.core.android.appunta.orientation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.WindowManager
import androidx.annotation.MainThread
import kotlin.math.abs

/**
 * This class is responsible for providing the measure of the compass (in the 3
 * axis) everytime it changes and dealing with the service
 */
class OrientationManager : SensorEventListener {
    // <<<< ORIGINAL VALUES: >>>>
    //    private static final float SMOOTH_THRESHOLD = CIRCLE / 6;
    //    private static final float SMOOTH_FACTOR = SMOOTH_THRESHOLD / 5;

    private val gravs = FloatArray(3)
    private val geoMags = FloatArray(3)
    private val orientationArray = FloatArray(3)
    private val rotationM = FloatArray(9)
    private val remappedRotationM = FloatArray(9)

    private var sensorManager: SensorManager? = null
    var orientation = Orientation()
    private var oldOrientation: Orientation? = null
    private var sensorRunning = false

    // Setters and getter for the three listeners (Bob, Moe and Curly)
    var onCompassChangeListener: OnOrientationChangedListener? = null
        private set
    var axisMode = MODE_COMPASS
        set(axisMode) {
            field = axisMode
            if (axisMode == MODE_COMPASS) {
                firstAxis = SensorManager.AXIS_Y
                secondAxis = SensorManager.AXIS_MINUS_X
            }
            if (axisMode == MODE_AR) {
                firstAxis = SensorManager.AXIS_X
                secondAxis = SensorManager.AXIS_Z
            }
        }
    private var firstAxis = SensorManager.AXIS_Y
    private var secondAxis = SensorManager.AXIS_MINUS_X
    private var failed = false

    /***
     * This constructor will generate and start a Compass Manager
     *
     * @param context
     * The context where the service will work
     */
    constructor(context: Context) {
        startSensor(context)
    }

    /***
     * This constructor will generate a Compass Manager, but it will need to be
     * started manually using [.startSensor]
     */
    constructor()

    /***
     * This method registers this class as a listener of the Sensor service
     *
     * @param context
     * The context over this will work
     */
    @MainThread
    fun startSensor(context: Context) {
        if (!sensorRunning) {
            val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            manager.registerListener(
                this,
                manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_UI
            )
            manager.registerListener(
                this,
                manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI
            )
            sensorManager = manager
            sensorRunning = true
        }
    }

    /***
     * Detects a change in a sensor and warns the appropiate listener.
     */
    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> System.arraycopy(event.values, 0, gravs, 0, 3)
            Sensor.TYPE_MAGNETIC_FIELD -> System.arraycopy(event.values, 0, geoMags, 0, 3)
            else -> return
        }

        if (SensorManager.getRotationMatrix(rotationM, null, gravs, geoMags)) {
            // Rotate to the camera's line of view (Y axis along the camera's axis)
            SensorManager.remapCoordinateSystem(rotationM, firstAxis, secondAxis, remappedRotationM)
            SensorManager.getOrientation(remappedRotationM, orientationArray)
            onSuccess()
        } else {
            failed = true
        }
    }

    private fun onSuccess() {
        if (failed) failed = false

        // Convert the azimuth to degrees in 0.5 degree resolution.
        val x = orientationArray[1]
        val y = orientationArray[0]
        val z = orientationArray[2]

        oldOrientation?.let {
            orientation.x = lowPass(x, it.x)
            orientation.y = lowPass(y, it.y)
            orientation.z = lowPass(z, it.z)
        } ?: run {
            orientation.x = x
            orientation.y = y
            orientation.z = z
        }

        oldOrientation = orientation
        onCompassChangeListener?.onOrientationChanged(orientation)
    }

    /**
     * Applies a lowpass filter to the change in the lecture of the sensor
     *
     * @param newValue the new sensor value
     * @param lowValue the old sensor value
     * @return and intermediate value
     */
    fun lowPass(newValue: Float, lowValue: Float): Float = if (abs(newValue - lowValue) < CIRCLE / 2) {
        if (abs(newValue - lowValue) > SMOOTH_THRESHOLD) {
            newValue
        } else {
            lowValue + SMOOTH_FACTOR * (newValue - lowValue)
        }
    } else {
        if (CIRCLE - abs(newValue - lowValue) > SMOOTH_THRESHOLD) {
            newValue
        } else {
            if (lowValue > newValue) {
                ((lowValue + (SMOOTH_FACTOR * ((CIRCLE + newValue - lowValue) % CIRCLE)) + CIRCLE) % CIRCLE)
            } else {
                ((lowValue - SMOOTH_FACTOR * ((CIRCLE - newValue + lowValue) % CIRCLE) + CIRCLE) % CIRCLE)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit

    /***
     * We stop "hearing" the sensors
     */
    @MainThread
    fun stopSensor() {
        if (sensorRunning) {
            sensorManager?.unregisterListener(this)
            sensorRunning = false
        }
    }

    fun setOnOrientationChangeListener(onOrientationChangeListener: OnOrientationChangedListener?) {
        onCompassChangeListener = onOrientationChangeListener
    }

    interface OnOrientationChangedListener {
        /***
         * This method will be invoked when the magnetic orientation of the
         * phone changed
         *
         * @param orientation
         * Orientation on degrees. 360-0 is north.
         */
        fun onOrientationChanged(orientation: Orientation)
    }

    companion object {
        const val MODE_COMPASS = 0
        const val MODE_AR = 1

        private const val CIRCLE = (Math.PI * 2).toFloat()
        private const val SMOOTH_THRESHOLD = CIRCLE / 3
        private const val SMOOTH_FACTOR = .03f

        fun getPhoneRotation(context: Context): Int {
            return (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
        }
    }
}
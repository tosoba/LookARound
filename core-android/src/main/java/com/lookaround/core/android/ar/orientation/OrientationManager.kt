package com.lookaround.core.android.ar.orientation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.annotation.MainThread
import kotlin.math.abs

/**
 * This class is responsible for providing the measure of the compass (in the 3 axis) everytime it
 * changes and dealing with the service
 */
class OrientationManager : SensorEventListener {
    private val gravs = FloatArray(3)
    private val geoMags = FloatArray(3)
    private val orientationArray = FloatArray(3)
    private val rotationM = FloatArray(9)
    private val remappedRotationM = FloatArray(9)

    private var sensorManager: SensorManager? = null
    private var orientation = Orientation()
    private var oldOrientation: Orientation? = null
    private var sensorRunning: Boolean = false
    var smoothFactor: Float = SMOOTH_FACTOR

    var onOrientationChangedListener: OnOrientationChangedListener? = null
    var axisMode: Mode = Mode.COMPASS
        set(value) {
            field = value
            if (value == Mode.COMPASS) {
                firstAxis = SensorManager.AXIS_Y
                secondAxis = SensorManager.AXIS_MINUS_X
            } else {
                firstAxis = SensorManager.AXIS_X
                secondAxis = SensorManager.AXIS_Z
            }
        }
    private var firstAxis: Int = SensorManager.AXIS_Y
    private var secondAxis: Int = SensorManager.AXIS_MINUS_X
    private var failed: Boolean = false

    /**
     * * This constructor will generate and start a Compass Manager
     *
     * @param context The context where the service will work
     */
    constructor(context: Context) {
        startSensor(context)
    }

    /**
     * * This constructor will generate a Compass Manager, but it will need to be started manually
     * using [.startSensor]
     */
    constructor()

    /**
     * * This method registers this class as a listener of the Sensor service
     *
     * @param context The context over this will work
     */
    @MainThread
    fun startSensor(context: Context): Boolean {
        if (sensorRunning) return true
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        if (
            !manager.registerListener(
                this,
                manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_UI
            ) ||
                !manager.registerListener(
                    this,
                    manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_UI
                )
        ) {
            return false
        }
        sensorManager = manager
        sensorRunning = true
        return true
    }

    /** * Detects a change in a sensor and warns the appropiate listener. */
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
        val pitch = orientationArray[1]
        val azimuth = orientationArray[0]
        val roll = orientationArray[2]

        oldOrientation?.let {
            orientation.pitch = lowPass(pitch, it.pitch)
            orientation.azimuth = lowPass(azimuth, it.azimuth)
            orientation.roll = lowPass(roll, it.roll)
        }
            ?: run {
                orientation.pitch = pitch
                orientation.azimuth = azimuth
                orientation.roll = roll
            }

        oldOrientation = orientation
        onOrientationChangedListener?.onOrientationChanged(orientation)
    }

    /**
     * Applies a low pass filter to the change in the lecture of the sensor
     *
     * @param newValue the new sensor value
     * @param oldValue the old sensor value
     * @return and intermediate value
     */
    private fun lowPass(newValue: Float, oldValue: Float): Float =
        if (abs(newValue - oldValue) < CIRCLE / 2) {
            if (abs(newValue - oldValue) > SMOOTH_THRESHOLD) {
                newValue
            } else {
                oldValue + smoothFactor * (newValue - oldValue)
            }
        } else {
            if (CIRCLE - abs(newValue - oldValue) > SMOOTH_THRESHOLD) {
                newValue
            } else {
                if (oldValue > newValue) {
                    ((oldValue +
                        (smoothFactor * ((CIRCLE + newValue - oldValue) % CIRCLE)) +
                        CIRCLE) % CIRCLE)
                } else {
                    ((oldValue - smoothFactor * ((CIRCLE - newValue + oldValue) % CIRCLE) +
                        CIRCLE) % CIRCLE)
                }
            }
        }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit

    /** * We stop "hearing" the sensors */
    @MainThread
    fun stopSensor() {
        if (sensorRunning) {
            sensorManager?.unregisterListener(this)
            sensorRunning = false
        }
    }

    enum class Mode {
        COMPASS,
        AR
    }

    interface OnOrientationChangedListener {
        /**
         * * This method will be invoked when the magnetic orientation of the phone changed
         *
         * @param orientation Orientation on degrees. 360-0 is north.
         */
        fun onOrientationChanged(orientation: Orientation)
    }

    companion object {
        private const val CIRCLE: Float = (Math.PI * 2).toFloat()
        private const val SMOOTH_THRESHOLD: Float = CIRCLE / 3f // originally: CIRCLE / 6
        private const val SMOOTH_FACTOR: Float = .005f // originally: SMOOTH_THRESHOLD / 5
    }
}

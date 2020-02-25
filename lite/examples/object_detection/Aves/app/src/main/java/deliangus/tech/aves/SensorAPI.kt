package deliangus.tech.aves

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

open class SensorAPI(sensorManager: SensorManager) : SensorEventListener {
    private var absolute_Gravity = 9.91f

    private var last_time_gyro: Long = 0L
    private var last_time_acce: Long = 0L
    private var last_time_linear_acce: Long = 0L
    private var delta_time_linear_acce: Float = 0f
    private var last_time_magn: Long = 0L
    private var last_time_grav: Long = 0L

    private var geomag_values: FloatArray = floatArrayOf(0f, 0f, 0f, 0f, 0f)
    private var gyro_values: FloatArray = floatArrayOf(0.0f, 0.0f, 0.0f)
    private var acce_values: FloatArray = floatArrayOf(0.0f, 0.0f, 0.0f)
    private var gravity_values: FloatArray = floatArrayOf(0f, 0f, 0f)
    private var linear_acce_values: FloatArray = floatArrayOf(0f, 0f, 0f)
    private var vector_values: FloatArray = floatArrayOf(0.0f, 0.0f, 0.0f)
    private var velocity_values: FloatArray = floatArrayOf(0.0f, 0.0f, 0.0f)

    private var azimuth = 0.0f
    private var pitch = 0.0f
    private var roll = 0.0f
    private var alpha = 0.0f
    private var beta = 0.0f
    private var mag_values: FloatArray = floatArrayOf(0f, 0f, 0f)

    private var calculated_ori_ins_x = 0.0f
    private var calculated_ori_ins_y = 0.0f
    private var calculated_ori_ins_z = 0.0f
    private var delta_time = 0.0f
    private var kflt_angles: FloatArray = floatArrayOf(.0f, 0.0f, 0f)

    private val gravity_list: FloatArray = FloatArray(20, { 0.0f })
    private val kflt = KalmanFilter()
    private var acceleration: FloatArray = floatArrayOf(0f, 0f, 0f)

    init {
        registerListener(sensorManager)
    }

    fun getLast_time_Linear_acce(): Float = delta_time_linear_acce
    fun kflt_angles(): FloatArray = kflt_angles
    fun geo_mag_rotation_values(): FloatArray = geomag_values

    fun registerListener(sensorManager: SensorManager) {
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR),
            SensorManager.SENSOR_DELAY_FASTEST
        )
        sensorManager.registerListener(
            this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
            SensorManager.SENSOR_DELAY_FASTEST
        )
        sensorManager.registerListener(
            this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_FASTEST
        )
        sensorManager.registerListener(
            this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
            SensorManager.SENSOR_DELAY_FASTEST
        )
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
            SensorManager.SENSOR_DELAY_FASTEST
        )
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
            SensorManager.SENSOR_DELAY_FASTEST
        )
    }

    fun reset() {
        reset(Sensor.TYPE_ACCELEROMETER)
        reset(Sensor.TYPE_GYROSCOPE)
        reset(Sensor.TYPE_MAGNETIC_FIELD)
    }

    fun reset(sensortype: Int) {
        when (sensortype) {
            Sensor.TYPE_ACCELEROMETER -> {
                acce_values = FloatArray(acce_values.size) { 0f }

                vector_values = FloatArray(vector_values.size) { 0f }

                velocity_values = FloatArray(velocity_values.size) { 0f }
            }
            Sensor.TYPE_GYROSCOPE -> {
                gyro_values = FloatArray(gyro_values.size) { 0f }
            }
        }
    }

    fun getAccelerate_values(): FloatArray = acceleration
    fun getVelocity_values(): FloatArray = velocity_values
    fun getVector_values(): FloatArray = vector_values

    fun delta_time(): Float = delta_time
    fun roll(): Float = roll
    fun pitch(): Float = pitch
    fun azimuth(): Float = azimuth


    override fun onSensorChanged(event: SensorEvent) {

        val current_time = event.timestamp

        when (event.sensor.type) {
            Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> {
                geomag_values = event.values.clone()
            }
            Sensor.TYPE_GYROSCOPE -> {
                gyro_values = event.values.clone()
            }
            Sensor.TYPE_GRAVITY -> {
                gravity_values = event.values.clone()
                absolute_Gravity = Math.sqrt(norm_2(event.values).toDouble()).toFloat()
            }
            Sensor.TYPE_ACCELEROMETER -> {
                acce_values = event.values.clone()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                mag_values = event.values.clone()
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                if (last_time_linear_acce == 0L) {
                    last_time_linear_acce = current_time
                    delta_time_linear_acce = 0f
                } else {
                    delta_time_linear_acce = (current_time - last_time_linear_acce) * NS_TO_S
                    last_time_linear_acce = current_time
                }
                var rMatrix: FloatArray = FloatArray(9) { 0f }
                var incline: FloatArray = FloatArray(9) { 0f }

                SensorManager.getRotationMatrix(rMatrix, incline, gravity_values, mag_values)

                acceleration = floatArrayOf(
                    linear_acce_values[0] * rMatrix[0] + linear_acce_values[1] * rMatrix[1] + linear_acce_values[2] * rMatrix[2],
                    linear_acce_values[0] * rMatrix[3] + linear_acce_values[1] * rMatrix[4] + linear_acce_values[2] * rMatrix[5],
                    linear_acce_values[0] * rMatrix[6] + linear_acce_values[1] * rMatrix[7] + linear_acce_values[2] * rMatrix[8]
                )

                vector_values.forEachIndexed { index, value ->
                    vector_values[index] =
                        velocity_values[index] * delta_time_linear_acce + delta_time_linear_acce * delta_time_linear_acce * (acceleration[index]) / 2
                }
                //- 6.48534E-06.toFloat()
                velocity_values.forEachIndexed { index, value ->
                    velocity_values[index] =
                        value + (acceleration[index]) * delta_time_linear_acce
                }

                //linear_acce_values = event.values.clone()
                var mean_error: FloatArray = floatArrayOf(-2.30389e-05f, 6.99e-05f, 3.998e-3f)
                linear_acce_values.forEachIndexed { index, value ->
                    linear_acce_values[index] = (linear_acce_values[index] + event.values[index])/2
                    /*
                    if (abs(linear_acce_values[index]) > abs(mean_error[index])) {
                        linear_acce_values[index] = linear_acce_values[index]-mean_error[index]
                    } else {
                        linear_acce_values[index] = 0f
                    }
                    */
                }
            }
        }
        calculateOrientation()


    }

    fun linear_acce_values(): FloatArray = linear_acce_values
    fun acceleration(): FloatArray = acceleration
    fun calculateOrientation() {

        val result_values = FloatArray(3)
        val r_matrix = FloatArray(9)

        SensorManager.getRotationMatrix(r_matrix, null, gravity_values, mag_values)
        SensorManager.getOrientation(r_matrix, result_values)

        calculated_ori_ins_x = Math.toDegrees(result_values[0].toDouble()).toFloat()
        calculated_ori_ins_y = Math.toDegrees(result_values[1].toDouble()).toFloat()
        calculated_ori_ins_z = Math.toDegrees(result_values[2].toDouble()).toFloat()

        roll = Math.toDegrees(result_values[2].toDouble()).toFloat()


        azimuth = calculated_ori_ins_x
        pitch = calculated_ori_ins_y
        roll = calculated_ori_ins_z
        alpha = azimuth
        beta = Math.toDegrees(
            Math.atan(
                -MathCot(pitch) * MathCot(roll) / Math.sqrt(
                    (MathCot(pitch) * MathCot(pitch) + MathCot(roll) * MathCot(roll)).toDouble()
                )
            )
        ).toFloat()

        if (Math.abs(pitch) > 90 || Math.abs(roll) > 90) {
            beta = Math.abs(beta)
        } else {
            beta = -Math.abs(beta)
        }

        val imu_9 = arrayOf(
            gravity_values,
            gyro_values,
            mag_values
        )

        val am_angle_mat = arrayOf(
            floatArrayOf(Math.toDegrees(gyro_values[0].toDouble()).toFloat(), 0.0f, 0.0f),
            floatArrayOf(0.0f, Math.toDegrees(gyro_values[1].toDouble()).toFloat(), 0.0f),
            floatArrayOf(0.0f, 0.0f, Math.toDegrees(gyro_values[2].toDouble()).toFloat())
        )

        val gyro_angle_mat = arrayOf(
            floatArrayOf(Math.toDegrees(gyro_values[1].toDouble()).toFloat(), 0.0f, 0.0f),
            floatArrayOf(0.0f, Math.toDegrees(-gyro_values[0].toDouble()).toFloat(), 0.0f),
            floatArrayOf(0.0f, 0.0f, Math.toDegrees(-gyro_values[2].toDouble()).toFloat())
        )

        kflt_angles = kflt.filter(am_angle_mat, gyro_angle_mat)

    }

    fun calculateAdjustedAcce(): FloatArray {
        val pitch_cos = Math.cos(Math.toRadians(this.pitch.toDouble()))
        val pitch_sin = Math.sin(Math.toRadians(this.pitch.toDouble()))
        val roll_cos = Math.cos(Math.toRadians(this.roll.toDouble()))
        val roll_sin = Math.sin(Math.toRadians(this.roll.toDouble()))

        val p = 1 / Math.sqrt(pitch_cos * pitch_cos / (roll_cos * roll_cos) + pitch_sin * pitch_sin)
        val r = pitch_cos * p / roll_cos
        val x = roll_sin * r
        val y = pitch_sin * p
        val z = roll_cos * r

        val gravity = absolute_Gravity
        val adjust_x = gravity * roll_sin
        val adjust_y = gravity * pitch_sin
        val adjust_z = Math.sqrt(
            Math.pow(gravity.toDouble(), 2.0) - Math.pow(adjust_x, 2.0) - Math.pow(
                adjust_y,
                2.0
            )
        )

        return floatArrayOf(
            (acce_values[0] + adjust_x).toFloat(),
            (acce_values[1] + adjust_y).toFloat(),
            (acce_values[2] - adjust_z).toFloat()
        )
    }

    fun MathCot(degree: Float): Float {
        return 1.0f / Math.tan(Math.toRadians(degree.toDouble())).toFloat()
    }

    fun norm_2(values: FloatArray): Float {
        var result = 0.0f
        for (i in values) {
            result += i * i
        }
        return result
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

    }

    companion object {
        val NS_TO_S = 10e-9f
    }

}

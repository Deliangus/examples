package org.tensorflow.lite.examples.detection;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class SensorAPI implements SensorEventListener {

    public static final float NS_TO_S = 10e-9f;
    public double absolute_Gravity = 9.91;
    public double gyro_instant_x = 0.0;
    public double gyro_instant_y = 0.0;
    public double gyro_instant_z = 0.0;
    public double gyro_ac_x = 0.0;
    public double gyro_ac_y = 0.0;
    public double gyro_ac_z = 0.0;
    public double gyro_ac_v_x = 0.0;
    public double gyro_ac_v_y = 0.0;
    public double gyro_ac_v_z = 0.0;
    public double acce_instant_x = 0.0;
    public double acce_instant_y = 0.0;
    public double acce_instant_z = 0.0;
    public double acce_ac_x = 0.0;
    public double acce_ac_y = 0.0;
    public double acce_ac_z = 0.0;
    public double acce_ac_v_x = 0.0;
    public double acce_ac_v_y = 0.0;
    public double acce_ac_v_z = 0.0;
    public double azimuth = 0.0;
    public double pitch = 0.0;
    public double roll = 0.0;
    public double alpha = 0.0;
    public double beta = 0.0;
    public double ori_instant_x = 0.0;
    public double ori_instant_y = 0.0;
    public double ori_instant_z = 0.0;
    public double mag_instant_x = 0.0;
    public double mag_instant_y = 0.0;
    public double mag_instant_z = 0.0;

    public double calculated_ori_ins_x = 0.0;
    public double calculated_ori_ins_y = 0.0;
    public double calculated_ori_ins_z = 0.0;
    public double delta_time = 0.0;
    long last_time = -1;
    double ave_delta = 0.0;

    public SensorAPI(SensorManager sensorManager) {
        super();
        registerListener(sensorManager);
    }

    public void registerListener(SensorManager sensorManager) {

        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_GAME);
    }

    public void reset() {
        reset(Sensor.TYPE_ACCELEROMETER);
        reset(Sensor.TYPE_GYROSCOPE);
        reset(Sensor.TYPE_MAGNETIC_FIELD);
    }

    public void reset(int sensortype) {
        switch (sensortype) {
            case Sensor.TYPE_ACCELEROMETER: {
                acce_instant_x = 0.0;
                acce_instant_y = 0.0;
                acce_instant_z = 0.0;

                acce_ac_x = 0.0;
                acce_ac_y = 0.0;
                acce_ac_z = 0.0;

                acce_ac_v_x = 0.0;
                acce_ac_v_y = 0.0;
                acce_ac_v_z = 0.0;
                break;
            }
            case Sensor.TYPE_GYROSCOPE: {
                gyro_instant_x = 0.0;
                gyro_instant_y = 0.0;
                gyro_instant_z = 0.0;

                gyro_ac_v_x = 0.0;
                gyro_ac_v_y = 0.0;
                gyro_ac_v_z = 0.0;

                gyro_ac_x = 0.0;
                gyro_ac_y = 0.0;
                gyro_ac_z = 0.0;
                break;
            }

        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        long current_time = event.timestamp;


        if (last_time < 0) {
            if (last_time > -100) {
                last_time = current_time;
                return;
            }

            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                absolute_Gravity = Math.sqrt(norm_2(event.values));
            }

        } else {
            delta_time = (current_time - last_time) * NS_TO_S;
            last_time = current_time;
        }


        switch (event.sensor.getType()) {
            case Sensor.TYPE_GYROSCOPE: {
                if (norm_2(event.values) > 0) {
                    double x = Math.toDegrees(event.values[0]);
                    double y = Math.toDegrees(event.values[1]);
                    double z = Math.toDegrees(event.values[2]);

                    gyro_instant_x = x;
                    gyro_instant_y = y;
                    gyro_instant_z = z;

                    gyro_ac_v_x += x * delta_time;
                    gyro_ac_v_y += y * delta_time;
                    gyro_ac_v_z += z * delta_time;

                    gyro_ac_x += gyro_ac_v_x * delta_time;
                    gyro_ac_y += gyro_ac_v_y * delta_time;
                    gyro_ac_z += gyro_ac_v_z * delta_time;
                }
                break;
            }
            case Sensor.TYPE_ACCELEROMETER: {
                if (norm_2(event.values) > 0) {
                    double x = event.values[0];
                    double y = event.values[1];
                    double z = event.values[2];

                    acce_instant_x = x;
                    acce_instant_y = y;
                    acce_instant_z = z;

                    double[] adjust = calculateAdjustedAcce();

                    acce_ac_v_x += adjust[0] * delta_time;
                    acce_ac_v_y += adjust[1] * delta_time;
                    acce_ac_v_z += adjust[2] * delta_time;

                    acce_ac_x += acce_ac_v_x * delta_time;
                    acce_ac_y += acce_ac_v_y * delta_time;
                    acce_ac_z += acce_ac_v_z * delta_time;
                }
                break;

            }
            case Sensor.TYPE_MAGNETIC_FIELD: {

                mag_instant_x = event.values[0];
                mag_instant_y = event.values[1];
                mag_instant_z = event.values[2];

                /*
                float[] result_values = new float[3];
                float[] r_matrix = new float[9];

                float[] acce_instant = new float[]{(float) acce_instant_x, (float) acce_instant_y,
                        (float) acce_instant_z};
                SensorManager.getRotationMatrix(r_matrix, null, acce_instant, event.values);
                SensorManager.getOrientation(r_matrix, result_values);

                //azimuth = Math.toDegrees(result_values[0]) + 180;
                //pitch = Math.toDegrees(result_values[1]);
                roll = Math.toDegrees(result_values[2]);

                //alpha = azimuth;
                //beta = Math.toDegrees(Math.atan(-MathCot(pitch) * MathCot(roll) / Math.sqrt(MathCot(pitch) * MathCot(pitch) + MathCot(roll) * MathCot(roll))));
                 */
                break;
            }
            case Sensor.TYPE_ORIENTATION: {

                ori_instant_x = event.values[0];
                ori_instant_y = event.values[1];
                ori_instant_z = event.values[2];
                /*
                azimuth = event.values[0];
                pitch = event.values[1];
                //roll = event.values[2];

                alpha = azimuth;
                //beta = Math.toDegrees(Math.atan(-MathCot(Math.abs(pitch)) * MathCot(roll) / Math.sqrt(MathCot(pitch) * MathCot(pitch) + MathCot(roll) * MathCot(roll))));
                beta = Math.toDegrees(Math.atan(-MathCot(pitch) * MathCot(roll) / Math.sqrt(MathCot(pitch) * MathCot(pitch) + MathCot(roll) * MathCot(roll))));
                //beta = Math.toDegrees(Math.atan(-MathCot(Math.abs(pitch)) * MathCot(Math.abs(roll)) / Math.sqrt(MathCot(pitch) * MathCot(pitch) + MathCot(roll) * MathCot(roll))));

                */
                break;
            }


        }
        calculateOrientation();
    }

    public void calculateOrientation() {

        float[] result_values = new float[3];
        float[] r_matrix = new float[9];

        float[] acce_instant = new float[]{(float) acce_instant_x, (float) acce_instant_y,
                (float) acce_instant_z};
        float[] mag_instant = new float[]{(float) mag_instant_x, (float) mag_instant_y, (float) mag_instant_z};
        SensorManager.getRotationMatrix(r_matrix, null, acce_instant, mag_instant);
        SensorManager.getOrientation(r_matrix, result_values);

        calculated_ori_ins_x = result_values[0];
        calculated_ori_ins_y = Math.toDegrees(result_values[1]);
        calculated_ori_ins_z = Math.toDegrees(result_values[2]);

        //azimuth = Math.toDegrees(result_values[0]) + 180;
        //pitch = Math.toDegrees(result_values[1]);
        roll = Math.toDegrees(result_values[2]);

        //alpha = azimuth;
        //beta = Math.toDegrees(Math.atan(-MathCot(pitch) * MathCot(roll) / Math.sqrt(MathCot(pitch) * MathCot(pitch) + MathCot(roll) * MathCot(roll))));


        azimuth = ori_instant_x;
        //pitch = ori_instant_y;
        //roll = event.values[2];
        pitch = calculated_ori_ins_y;
        roll = calculated_ori_ins_z;

        if (Math.abs(calculated_ori_ins_z) + Math.abs(calculated_ori_ins_y) <= 180) {
            pitch = calculated_ori_ins_y;
            roll = calculated_ori_ins_z;
        } else {
            pitch = ori_instant_y;
            roll = ori_instant_z;
        }

        alpha = azimuth;
        //beta = Math.toDegrees(Math.atan(-MathCot(Math.abs(pitch)) * MathCot(roll) / Math.sqrt(MathCot(pitch) * MathCot(pitch) + MathCot(roll) * MathCot(roll))));
        beta = Math.toDegrees(Math.atan(-MathCot(pitch) * MathCot(roll) / Math.sqrt(MathCot(pitch) * MathCot(pitch) + MathCot(roll) * MathCot(roll))));
        //beta = Math.toDegrees(Math.atan(-MathCot(Math.abs(pitch)) * MathCot(Math.abs(roll)) / Math.sqrt(MathCot(pitch) * MathCot(pitch) + MathCot(roll) * MathCot(roll))));

        if (Math.abs(pitch) > 90 || Math.abs(roll) > 90) {
            beta = Math.abs(beta);
        } else {
            beta = -Math.abs(beta);
        }

    }

    public double[] calculateAdjustedAcce() {
        double pitch_cos = Math.cos(Math.toRadians(this.pitch));
        double pitch_sin = Math.sin(Math.toRadians(this.pitch));
        double roll_cos = Math.cos(Math.toRadians(this.roll));
        double roll_sin = Math.sin(Math.toRadians(this.roll));

        double p = 1 / Math.sqrt(pitch_cos * pitch_cos / (roll_cos * roll_cos) + pitch_sin * pitch_sin);
        double r = pitch_cos * p / roll_cos;
        double x = roll_sin * r;
        double y = pitch_sin * p;
        double z = roll_cos * r;

        double gravity = absolute_Gravity;
        double adjust_x = gravity * roll_sin;
        double adjust_y = gravity * pitch_sin;
        double adjust_z = Math.sqrt(Math.pow(gravity, 2) - Math.pow(adjust_x, 2) - Math.pow(adjust_y, 2));

        return new double[]{acce_instant_x + adjust_x, acce_instant_y + adjust_y, acce_instant_z - adjust_z};
    }

    public double MathCot(double degree) {
        return 1.0 / Math.tan(Math.toRadians(degree));
    }

    public double norm_2(float[] values) {
        double result = 0.0;
        for (double i : values) {
            result += i * i;
        }
        return result;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}

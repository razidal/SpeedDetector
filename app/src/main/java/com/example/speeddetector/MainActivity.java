package com.example.speeddetector;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private TextView speedTextView;

    private float[] gravity = new float[3];
    private float[] linear_acceleration = new float[3];
    private float velocity = 0;
    private long lastUpdate = 0;

    private KalmanFilter kalmanFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        speedTextView = findViewById(R.id.speedTextView);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager == null) {
            Log.e("MainActivity", "SensorManager is null");
            return;
        }

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer == null) {
            Log.e("MainActivity", "Accelerometer sensor not available");
            return;
        }

        kalmanFilter = new KalmanFilter(0.0f);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (accelerometer != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long currentTime = System.currentTimeMillis();
            long timeDifference = currentTime - lastUpdate;

            if (lastUpdate == 0) {
                lastUpdate = currentTime;
                return;
            }

            if (timeDifference > 100) {
                lastUpdate = currentTime;

                final float alpha = 0.8f;

                // Isolate the force of gravity with the low-pass filter
                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

                // Remove the gravity contribution with the high-pass filter
                linear_acceleration[0] = event.values[0] - gravity[0];
                linear_acceleration[1] = event.values[1] - gravity[1];
                linear_acceleration[2] = event.values[2] - gravity[2];

                // Calculate the linear acceleration magnitude
                float acceleration = (float) Math.sqrt(linear_acceleration[0] * linear_acceleration[0]
                        + linear_acceleration[1] * linear_acceleration[1]
                        + linear_acceleration[2] * linear_acceleration[2]);

                // Apply a noise threshold
                if (acceleration < 0.1) {
                    acceleration = 0;
                }

                // Integrate acceleration to get velocity
                velocity += acceleration * (timeDifference / 1000.0f); // Convert ms to seconds

                // Apply damping to velocity to simulate friction
                velocity *= 0.5;

                // Reset velocity if no significant movement
                if (acceleration == 0 && velocity < 0.1) {
                    velocity = 0;
                }

                // Use Kalman filter to smooth the velocity
                velocity = kalmanFilter.update(velocity);

                updateSpeed(velocity);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed for accelerometer
    }

    private void updateSpeed(float speed) {
        runOnUiThread(() -> speedTextView.setText("Speed: " + String.format("%.2f", speed) + " m/s"));
    }
}

class KalmanFilter {
    private float estimate;
    private float errorCovariance;
    private float processNoise;
    private float measurementNoise;
    private float kalmanGain;

    public KalmanFilter(float initialEstimate) {
        this.estimate = initialEstimate;
        this.errorCovariance = 1.0f;
        this.processNoise = 0.1f;
        this.measurementNoise = 0.5f;
    }

    public float update(float measurement) {
        kalmanGain = errorCovariance / (errorCovariance + measurementNoise);
        estimate = estimate + kalmanGain * (measurement - estimate);
        errorCovariance = (1 - kalmanGain) * errorCovariance + processNoise;
        return estimate;
    }
}

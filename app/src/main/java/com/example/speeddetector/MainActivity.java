package com.example.speeddetector;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;



public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private TextView msTextView;
    private TextView knotTextView;
    private static final int ALPHA = (int) 0.1; // Low-pass filter constant
    private int lastAcceleration = 0; // Last acceleration value
    private long lastUpdate = 0; // Last update timestamp

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        msTextView = findViewById(R.id.speedTextView);

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
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

            if (timeDifference > 100) {
                lastUpdate = currentTime;

                int x = (int) event.values[0];
                int y = (int) event.values[1];
                int z = (int) event.values[2];

                int acceleration = Math.abs(lowPass((int) Math.sqrt(x * x + y * y + z * z), lastAcceleration));
                lastAcceleration = acceleration;

                int speed = calculateSpeed(acceleration);
                updateSpeed(speed);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed for accelerometer
    }

    private int lowPass(int current, int last) {
        return last + (ALPHA * (current - last));
    }

    private int calculateSpeed(int acceleration) {
        // Adjust the conversion formula based on your application's requirements
        int speed = (int) (acceleration * (int)(0.277)); // Convert from m/s^2 to km/h (example)
        return speed;
    }

    private void updateSpeed(int speed) {
        runOnUiThread(() -> msTextView.setText("Speed: " + speed + " m/s"));

    }
}

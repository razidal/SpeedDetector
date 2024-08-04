package com.example.speeddetector;

import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private TextView speedTextView;
    private Switch themeSwitch;
    private Button buttonMs, buttonKnots, buttonKmH;

    private float[] gravity = new float[3];
    private float[] linear_acceleration = new float[3];
    private float velocity = 0;
    private long lastUpdate = 0;

    private KalmanFilter kalmanFilter;
    private boolean displayInMs;
    private boolean displayInKnots;
    private boolean displayInKmH;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        speedTextView = findViewById(R.id.speedTextView);
        themeSwitch = findViewById(R.id.themeSwitch);
        buttonMs = findViewById(R.id.buttonMs);
        buttonKnots = findViewById(R.id.buttonKnots);
        buttonKmH = findViewById(R.id.buttonKmH);

        SharedPreferences preferences = getSharedPreferences("settings", MODE_PRIVATE);
        boolean isDarkMode = preferences.getBoolean("isDarkMode", false);
        String unitPreference = preferences.getString("unit", "m/s");

        applyTheme(isDarkMode);

        themeSwitch.setChecked(isDarkMode);

        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("isDarkMode", isChecked);
            editor.apply();
            applyTheme(isChecked);
        });

        switch (unitPreference) {
            case "knots":
                displayInMs = false;
                displayInKnots = true;
                displayInKmH = false;
                break;
            case "km/h":
                displayInMs = false;
                displayInKnots = false;
                displayInKmH = true;
                break;
            default:
                displayInMs = true;
                displayInKnots = false;
                displayInKmH = false;
                break;
        }

        buttonMs.setOnClickListener(v -> {
            setUnitPreference("m/s");
            displayInMs = true;
            displayInKnots = false;
            displayInKmH = false;
            updateSpeed(velocity);
        });

        buttonKnots.setOnClickListener(v -> {
            setUnitPreference("knots");
            displayInMs = false;
            displayInKnots = true;
            displayInKmH = false;
            updateSpeed(velocity);
        });

        buttonKmH.setOnClickListener(v -> {
            setUnitPreference("km/h");
            displayInMs = false;
            displayInKnots = false;
            displayInKmH = true;
            updateSpeed(velocity);
        });

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager == null) {
            return;
        }

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer == null) {
            return;
        }

        kalmanFilter = new KalmanFilter(0.0f);
    }

    private void applyTheme(boolean isDarkMode) {
        int textColor = isDarkMode ? getResources().getColor(R.color.white) : getResources().getColor(R.color.black);
        int backgroundColor = isDarkMode ? getResources().getColor(R.color.black) : getResources().getColor(R.color.white);

        speedTextView.setTextColor(textColor);
        themeSwitch.setTextColor(textColor);
        buttonMs.setTextColor(textColor);
        buttonKnots.setTextColor(textColor);
        buttonKmH.setTextColor(textColor);
        findViewById(R.id.main_layout).setBackgroundColor(backgroundColor);
    }

    private void setUnitPreference(String unit) {
        SharedPreferences preferences = getSharedPreferences("settings", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("unit", unit);
        editor.apply();
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

                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

                linear_acceleration[0] = event.values[0] - gravity[0];
                linear_acceleration[1] = event.values[1] - gravity[1];
                linear_acceleration[2] = event.values[2] - gravity[2];

                float acceleration = (float) Math.sqrt(linear_acceleration[0] * linear_acceleration[0]
                        + linear_acceleration[1] * linear_acceleration[1]
                        + linear_acceleration[2] * linear_acceleration[2]);

                if (acceleration < 0.1) {
                    acceleration = 0;
                }

                velocity += acceleration * (timeDifference / 1000.0f);

                velocity *= 0.5;

                if (acceleration == 0 && velocity < 0.1) {
                    velocity = 0;
                }

                velocity = kalmanFilter.update(velocity);

                updateSpeed(velocity);
            }
        }
    }

    //not for use in this app
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void updateSpeed(float speed) {
        float displaySpeed = speed;
        String unit = " m/s";

        if (displayInKnots) {
            displaySpeed = speed * 1.94384f;
            unit = " knots";
        } else if (displayInKmH) {
            displaySpeed = speed * 3.6f;
            unit = " km/h";
        }

        speedTextView.setText(String.format("Speed: %.2f%s", displaySpeed, unit));
    }

    private static class KalmanFilter {
        private float estimate;
        private final float processNoise = 0.1f;
        private final float measurementNoise = 0.1f;
        private float errorEstimate = 1.0f;

        public KalmanFilter(float initialEstimate) {
            estimate = initialEstimate;
        }

        public float update(float measurement) {
            float kalmanGain = errorEstimate / (errorEstimate + measurementNoise);
            estimate = estimate + kalmanGain * (measurement - estimate);
            errorEstimate = (1 - kalmanGain) * errorEstimate + Math.abs(estimate) * processNoise;
            return estimate;
        }
    }
}

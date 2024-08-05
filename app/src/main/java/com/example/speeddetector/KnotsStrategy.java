package com.example.speeddetector;

public class KnotsStrategy implements SpeedUnitStrategy {
    @Override
    public float convertSpeed(float speed) {
        return speed * 1.94384f;
    }

    @Override
    public String getUnit() {
        return " knots";
    }
}

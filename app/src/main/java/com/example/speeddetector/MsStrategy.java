package com.example.speeddetector;

public class MsStrategy implements SpeedUnitStrategy {
    @Override
    public float convertSpeed(float speed) {
        return speed;
    }

    @Override
    public String getUnit() {
        return " m/s";
    }
}

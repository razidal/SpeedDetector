package com.example.speeddetector;

public class KmHStrategy implements SpeedUnitStrategy {
    @Override
    public float convertSpeed(float speed) {
        return speed * 3.6f;
    }

    @Override
    public String getUnit() {
        return " km/h";
    }
}

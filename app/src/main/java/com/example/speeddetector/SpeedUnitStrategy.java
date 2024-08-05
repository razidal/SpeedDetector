package com.example.speeddetector;

public interface SpeedUnitStrategy {
    float convertSpeed(float speed);
    String getUnit();
}

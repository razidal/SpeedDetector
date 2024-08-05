package com.example.speeddetector;

public class SpeedUnitFactory {

    public static SpeedUnitStrategy getSpeedUnitStrategy(String unitPreference) {
        switch (unitPreference) {
            case "knots":
                return new KnotsStrategy();
            case "km/h":
                return new KmHStrategy();
            default:
                return new MsStrategy();
        }
    }
}
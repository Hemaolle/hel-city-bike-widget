package com.leppaaho.oskari.citybikewidget;

class BikeStation {
    private static final String COUNT_UNKNOWN = "?";
    private static final String LOW_COUNT_WARNING = "  !";
    private static final String ZERO_COUNT_WARNING = " !!";
    private static final int LOW_COUNT_WARNING_LIMIT = 3;

    // Filled by deserialization from Gson.
    @SuppressWarnings("unused")
    public final String name;
    private final int bikesAvailable;

    // Required to fix final field not initialized error.
    @SuppressWarnings("unused")
    BikeStation(String name, int bikesAvailable) {
        this.name = name;
        this.bikesAvailable = bikesAvailable;
    }

    public static String getBikeCountString(BikeStation station) {
        if (station == null) {
            return COUNT_UNKNOWN;
        }
        return Integer.toString(station.bikesAvailable) + getWarning(station.bikesAvailable);
    }

    private static String getWarning(int bikeCount) {
        if (0 < bikeCount && bikeCount <= LOW_COUNT_WARNING_LIMIT) {
            return LOW_COUNT_WARNING;
        }
        if (bikeCount == 0) {
            return ZERO_COUNT_WARNING;
        }
        return "";
    }
}

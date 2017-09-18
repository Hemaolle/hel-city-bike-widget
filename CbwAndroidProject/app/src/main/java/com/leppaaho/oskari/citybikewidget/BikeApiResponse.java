package com.leppaaho.oskari.citybikewidget;

import com.google.gson.annotations.SerializedName;

class BikeApiResponse {
    @SuppressWarnings("unused")
    @SerializedName("data")
    public final BikeStations bikeStations;

    BikeApiResponse(BikeStations bikeStations) {
        this.bikeStations = bikeStations;
    }
}

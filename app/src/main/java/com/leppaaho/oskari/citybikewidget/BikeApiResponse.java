package com.leppaaho.oskari.citybikewidget;

import com.google.gson.annotations.SerializedName;

public class BikeApiResponse {
    @SerializedName("data")
    public BikeStations bikeStations;
}

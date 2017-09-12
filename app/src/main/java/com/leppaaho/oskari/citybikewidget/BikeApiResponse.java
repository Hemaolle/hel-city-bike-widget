package com.leppaaho.oskari.citybikewidget;

import com.google.gson.annotations.SerializedName;

public class BikeApiResponse extends Object {
    @SuppressWarnings("unused")
    @SerializedName("data")
    public BikeStations bikeStations;
}

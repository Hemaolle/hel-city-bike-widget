package com.leppaaho.oskari.citybikewidget;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class BikeApiData {

    @SerializedName("bikeRentalStations")
    public List<BikeStation> stations;

    public BikeApiData() {
        stations = new ArrayList<>();
    }
}

package com.leppaaho.oskari.citybikewidget;

import java.util.ArrayList;
import java.util.List;

public class BikeApiResponse {

    public List<BikeStation> stations;

    public BikeApiResponse() {
        stations = new ArrayList<>();
    }
}

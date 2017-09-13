package com.leppaaho.oskari.citybikewidget;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

class BikeStations implements Iterable<BikeStation>{

    @SerializedName("bikeRentalStations")
    private List<BikeStation> stations;

    public BikeStations() {
        stations = new ArrayList<>();
    }

    @Override
    public Iterator<BikeStation> iterator() {
        return stations.iterator();
    }

    public BikeStation find(String name) {
        for(BikeStation station : stations) {
            if(station.name.equals(name)) {
                return station;
            }
        }

        return null;
    }

    public List<String> getNames() {
        List<String> names = new ArrayList<>();
        for (BikeStation s : stations) {
            names.add(s.name);
        }
        return names;
    }

    public void sort() {
        Collections.sort(stations, new Comparator<BikeStation>() {
            @Override
            public int compare(BikeStation s1, BikeStation s2) {
                return s1.name.compareTo(s2.name);
            }
        });
    }
}

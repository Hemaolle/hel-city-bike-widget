package com.leppaaho.oskari.citybikewidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;

/**
 * Provides bike station data. Stores the last seen data locally and falls back to that if the
 * BikeApiClient fails.
 */
public class BikeDataProvider {

    public interface BikeStationsListener {
        void onResponse(BikeStations stations);
    }

    public static void requestBikeData(final Context context, final BikeStationsListener listener) {
        BikeApiClient.getStations(context, new BikeApiClient.BikeApiResponseListener() {

            public void onResponse(BikeStations stations) {
                storeStations(stations, context);
                listener.onResponse(stations);
            }

            public void onError(String error) {
                listener.onResponse(retrieveStoredStations(context));
            }
        });
    }

    private static void storeStations(BikeStations stations, Context context) {

        // It's not that nice to have to re-serialize the stations since we have them as a string
        // in GsonRequest and actually Volley also caches the previous response. It might be
        // possible to be able to find a nicer solution.
        Gson gson = new Gson();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove("last_bike_station_data");
        editor.putString("last_bike_station_data", gson.toJson(stations));
        editor.apply();
    }

    private static BikeStations retrieveStoredStations(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String storedData = preferences.getString("last_bike_station_data", "");
        if (storedData.equals("")) {
            return new BikeStations();
        }
        return new Gson().fromJson(storedData, BikeStations.class);
    }
}

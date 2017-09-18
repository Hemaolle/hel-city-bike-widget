package com.leppaaho.oskari.citybikewidget;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;

/**
 * Provides bike station data. Stores the last seen data locally and falls back to that if the
 * BikeApiClient fails.
 */
class BikeDataProvider {
    private static final String TAG = BikeDataProvider.class.getSimpleName();

    public interface BikeStationsListener {
        void onResponse(BikeStations stations);
    }

    private static class BikeDataRequest {
        private Request<BikeApiResponse> request;

        public void request(final Context context, final BikeStationsListener listener) {
            request = BikeApiClient.getStations(context, new BikeApiClient.BikeApiResponseListener() {

                public void onResponse(BikeStations stations) {
                    listener.onResponse(stations);
                }

                public void onError(String error) {
                    Log.i(TAG, "Bike data request failed, use the cached entry");
                    if (request.getCacheEntry() == null)
                    {
                        listener.onResponse(new BikeStations());
                    }
                    String cachedString = null;
                    try {
                        cachedString = new String(request.getCacheEntry().data, "utf-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    BikeApiResponse response = new Gson().fromJson(cachedString, BikeApiResponse.class);
                    listener.onResponse(response.bikeStations);
                }
            });
        }
    }

    public static void requestBikeData(final Context context, final BikeStationsListener listener) {
        new BikeDataRequest().request(context, listener);
    }
}

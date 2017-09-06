package com.leppaaho.oskari.citybikewidget;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Comparator;

public class BikeApi {

    private static final String TAG = ConfigurationActivity.class.getName();

    public interface BikeApiResponseListener {
        void onResponse(BikeApiResponse response);
        void onError(String error);
    }

    public static void getStations(Context context, final BikeApiResponseListener listener) {
        RequestQueue queue = Volley.newRequestQueue(context);
        final String bikeApi = context.getString(R.string.bikeApi);
        JSONObject data = null;

        try {
            data = new JSONObject(
                    "{\"query\": \"{ bikeRentalStations { name bikesAvailable } }\" }"
            );
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.i(TAG, data.toString());

        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, bikeApi, data,
                new Response.Listener<JSONObject>()
                {
                    @Override
                    public void onResponse(JSONObject response) {
                        // response
                        Log.d(TAG, "Response: " + response.toString());

                        JSONArray stations = null;

                        try {
                            stations = response.getJSONObject("data")
                                    .getJSONArray("bikeRentalStations");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        final BikeApiResponse bikeApiResponse = new BikeApiResponse();
                        for (int i = 0; i < stations.length(); ++i) {
                            try {
                                BikeStation station = new BikeStation();
                                JSONObject jsonStation = stations.getJSONObject(i);
                                station.name = jsonStation.getString("name");
                                station.bikesAvailable = jsonStation.getInt("bikesAvailable");
                                bikeApiResponse.stations.add(station);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        Collections.sort(bikeApiResponse.stations, new Comparator<BikeStation>() {
                            @Override
                            public int compare(BikeStation s1, BikeStation s2) {
                                return s1.name.compareTo(s2.name);
                            }
                        });

                        listener.onResponse(bikeApiResponse);
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        listener.onError(error.toString());
                    }
                }
        );
        queue.add(postRequest);
    }
}

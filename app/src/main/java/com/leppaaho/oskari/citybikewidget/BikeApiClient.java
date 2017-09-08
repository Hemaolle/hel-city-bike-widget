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
import java.util.List;

public class BikeApiClient {

    private static final String TAG = ConfigurationActivity.class.getName();

    public interface BikeApiResponseListener {
        void onResponse(List<BikeStation > stations);
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

        GsonRequest<BikeApiResponse> postRequest = new GsonRequest<>(
                Request.Method.POST, bikeApi, data, BikeApiResponse.class,
                new Response.Listener<BikeApiResponse>()
                {
                    @Override
                    public void onResponse(BikeApiResponse response) {
                        Collections.sort(response.data.stations, new Comparator<BikeStation>() {
                            @Override
                            public int compare(BikeStation s1, BikeStation s2) {
                                return s1.name.compareTo(s2.name);
                            }
                        });

                        listener.onResponse(response.data.stations);
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

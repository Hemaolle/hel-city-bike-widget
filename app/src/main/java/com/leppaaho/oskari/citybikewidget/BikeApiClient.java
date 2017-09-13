package com.leppaaho.oskari.citybikewidget;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class BikeApiClient {

    private static final String TAG = BikeApiClient.class.getName();

    public interface BikeApiResponseListener {
        void onResponse(BikeStations stations);
        void onError(String error);
    }

    public static Request<BikeApiResponse> getStations(
            Context context, final BikeApiResponseListener listener)
    {
        RequestQueue queue = Volley.newRequestQueue(context);
        final String bikeApi = context.getString(R.string.bikeApi);
        JSONObject data = null;

        try {
            data = new JSONObject(context.getString(R.string.bikeNamesAndAvailabilityQuery));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.i(TAG, data.toString());

        GsonRequest<BikeApiResponse> postRequest = new GsonRequest<>(
                bikeApi, data, BikeApiResponse.class,
                new Response.Listener<BikeApiResponse>()
                {
                    @Override
                    public void onResponse(BikeApiResponse response) {
                        response.bikeStations.sort();
                        listener.onResponse(response.bikeStations);
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
        return postRequest;
    }
}

package com.leppaaho.oskari.citybikewidget;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * An interface to the city bike REST api.
 */
class BikeApiClient {
    public interface BikeApiResponseListener {
        void onResponse(BikeStations stations);

        void onError(String error);
    }

    public static Request<BikeApiResponse> getStations(
            Context context, final BikeApiResponseListener listener) {

        // Using Volley for simpler HTTP requests.
        RequestQueue queue = Volley.newRequestQueue(context);
        final String bikeApi = context.getString(R.string.bike_api);
        JSONObject query = null;

        try {
            query = new JSONObject(context.getString(R.string.bike_names_and_availability_query));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        GsonRequest<BikeApiResponse> postRequest = new GsonRequest<>(
                bikeApi, query, BikeApiResponse.class,
                new Response.Listener<BikeApiResponse>() {
                    @Override
                    public void onResponse(BikeApiResponse response) {
                        response.bikeStations.sort();
                        listener.onResponse(response.bikeStations);
                    }
                },
                new Response.ErrorListener() {
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

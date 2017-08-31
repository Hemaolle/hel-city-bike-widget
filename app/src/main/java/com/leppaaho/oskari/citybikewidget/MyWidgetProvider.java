package com.leppaaho.oskari.citybikewidget;

import java.util.HashSet;
import java.util.Set;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MyWidgetProvider extends AppWidgetProvider {

    private static final String ACTION_CLICK = "ACTION_CLICK";
    AppWidgetManager appWidgetManager;
    int[] allWidgetIds;
    Set<String> selectedStationNames;
    Context context;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
        this.context = context;
        this.appWidgetManager = appWidgetManager;
        Log.i("INFO", "updating widget");

        // Get all ids
        ComponentName thisWidget = new ComponentName(context,
                MyWidgetProvider.class);
        allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        // TODO: Make the order fixed.
        selectedStationNames = preferences.getStringSet("selected_stations", new HashSet<String>());

        requestBikeCount(context, selectedStationNames);

        for (int widgetId : allWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                    R.layout.widget_layout);

            // Register an onClickListener
            Intent intent = new Intent(context, MyWidgetProvider.class);

            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                    0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.update, pendingIntent);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }

    public void requestBikeCount(final Context context, final Set<String> selectedStationNames) {
        RequestQueue queue = Volley.newRequestQueue(context);
        String url = "http://api.digitransit.fi/routing/v1/routers/hsl/index/graphql";
        JSONObject data = null;

        try {
            data = new JSONObject(
                    "{ \"query\": \"{ bikeRentalStations { name id bikesAvailable } }\" }\""
            );
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.i("INFO", data.toString());

        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, data,
                new Response.Listener<JSONObject>()
                {
                    @Override
                    public void onResponse(JSONObject response) {
                        onBikeDataReceived(response);
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub
                        Log.d("ERROR","error => "+error.toString());
                    }
                }
        );
        queue.add(postRequest);
    }

    public void onBikeDataReceived(JSONObject response) {
        // response
        Log.d("Response", response.toString());
        int availableBikes = 0;
        String stationsString = "";
        try {
            JSONArray stations =
                    response.getJSONObject("data")
                            .getJSONArray("bikeRentalStations");
            Log.d("Stations length", "" + stations.length());
            for (int i = 0; i < stations.length(); i++) {
                JSONObject station = stations.getJSONObject(i);
                for (String selectedStationName: selectedStationNames) {
                    if (station.getString("name").equals(selectedStationName)) {
                        stationsString += selectedStationName + ": " + station.getInt("bikesAvailable") + "\n";
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        for (int widgetId : allWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                    R.layout.widget_layout);

            // Set the text
            remoteViews.setTextViewText(R.id.update, stationsString);

            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }

    }
}
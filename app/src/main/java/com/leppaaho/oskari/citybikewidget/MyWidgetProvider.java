package com.leppaaho.oskari.citybikewidget;

import java.util.HashMap;
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

    private static final String TAG = MyWidgetProvider.class.getName();

    AppWidgetManager appWidgetManager;
    int[] allWidgetIds;
    Set<String> selectedStationNames;
    Context context;
    HashMap<String, Integer> bikeCounts = new HashMap<>();

    private void reloadFromCache(Context context, AppWidgetManager appWidgetManager,
                                 int[] appWidgetIds) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        for (int widgetId : appWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                    R.layout.widget_layout);

            Log.i(TAG, "get preferences for widget id: " + widgetId);

            String targetStation = preferences.getString(Integer.toString(widgetId), "");
            int cachedBikeCount = preferences.getInt(Integer.toString(widgetId) + "_cached_count", 0);

            Log.i(TAG, "wigget " + widgetId + " target station: " + targetStation);

            updateStationInfo(remoteViews, targetStation, cachedBikeCount);

            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
        this.context = context;
        this.appWidgetManager = appWidgetManager;
        Log.i(TAG, "updating widget");

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
            remoteViews.setOnClickPendingIntent(R.id.layout, pendingIntent);
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

        Log.i(TAG, data.toString());

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

                        // Looks like we may get an error response when rebooting the device (PIN
                        // not entered yet). The widget update seems to be called earlier than
                        // getting a BOOT_COMPLETE Intent though (resulting in this error then),
                        // so loading the data from the cache here should handle updating the UI
                        // on device reboot quicker. And no need to listen to a BOOT_COMPLETE Intent.

                        // TODO: Might make sense to listen to CONNECTIVITY_ACTION and update the
                        // data once we have network on reboot.

                        Log.i(TAG, "Bike status request failed, loading from cache. Error: => "+error.toString());

                        reloadFromCache(context, appWidgetManager, allWidgetIds);
                    }
                }
        );
        queue.add(postRequest);
    }

    public void onBikeDataReceived(JSONObject response) {
        // response
        Log.d(TAG, "Response: " + response.toString());

        try {
            JSONArray stations =
                    response.getJSONObject("data")
                            .getJSONArray("bikeRentalStations");
            Log.d(TAG, "Stations length: " + stations.length());
            for (int i = 0; i < stations.length(); i++) {
                JSONObject station = stations.getJSONObject(i);
                bikeCounts.put(station.getString("name"), station.getInt("bikesAvailable"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        for (int widgetId : allWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                    R.layout.widget_layout);

            Log.i(TAG, "get preferences for widget id: " + widgetId);

            String targetStation = preferences.getString(Integer.toString(widgetId), "");

            Log.i(TAG, "wigget " + widgetId + " target station: " + targetStation);

            // TODO: if should not be needed
            if (bikeCounts.containsKey(targetStation)) {
                int bikeCount = bikeCounts.get(targetStation);
                storeCount(preferences, widgetId, bikeCount);
                updateStationInfo(remoteViews, targetStation, bikeCount);
            }
            else {
                remoteViews.setTextViewText(R.id.stationName, "No target station selected");
            }

            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }

    }

    private void storeCount(SharedPreferences preferences, int widgetId, int bikeCount) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(Integer.toString(widgetId) + "_cached_count");
        editor.putInt(Integer.toString(widgetId) + "_cached_count", bikeCount);
        editor.apply();
    }

    private void updateStationInfo(RemoteViews remoteViews, String targetStation, int bikeCount) {
        // Set the text
        remoteViews.setTextViewText(R.id.stationName, targetStation);

        String warning = "";
        if (0 < bikeCount && bikeCount < 4) {
            warning = " !";
        }
        if (bikeCount == 0) {
            warning = " !!";
        }
        remoteViews.setTextViewText(
                R.id.bikeCount, ": " + Integer.toString(bikeCount) + warning);
    }
}
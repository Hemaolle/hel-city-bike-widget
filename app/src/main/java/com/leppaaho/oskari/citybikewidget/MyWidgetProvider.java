package com.leppaaho.oskari.citybikewidget;

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

public class MyWidgetProvider extends AppWidgetProvider {

    private static final String TAG = MyWidgetProvider.class.getName();

    AppWidgetManager appWidgetManager;
    int[] allWidgetIds;
    Context context;
    SharedPreferences preferences;
    RemoteViews remoteViews;

    // Note that onUpdate will be run already before the configuration activity finishes. This is
    // a bug originally from 2009: https://issuetracker.google.com/issues/36908882 (won't fix).
    // Should be ok though, just have to handle a station not having been selected gracefully.
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
        Log.i(TAG, "Updating all widgets");

        this.context = context;
        this.appWidgetManager = appWidgetManager;
        remoteViews = new RemoteViews(context.getPackageName(),
                R.layout.widget_layout);
        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        // Always update all the widgets to keep them in sync.
        ComponentName thisWidget = new ComponentName(context,
                MyWidgetProvider.class);
        allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        requestBikeCount();
    }

    public void requestBikeCount() {
        BikeApiClient.getStations(context, new BikeApiClient.BikeApiResponseListener() {
            @Override
            public void onResponse(BikeStations stations) {
                for (int widgetId : allWidgetIds) {
                    String stationName = getTargetStationName(widgetId);
                    BikeStation station = stations.find(stationName);
                    String bikeCount = BikeStation.getBikeCountString(station);
                    storeCount(widgetId, bikeCount);
                    logWidgetUpdate("Update widget", widgetId, stationName, bikeCount);
                    updateAppWidget(widgetId, stationName, bikeCount);
                }
            }

            @Override
            public void onError(String error) {
                // Looks like we may get an error response when rebooting the device (PIN
                // not entered yet). The widget update seems to be called earlier than
                // getting a BOOT_COMPLETE Intent though (resulting in this error then),
                // so loading the bikeStations from the cache here should handle updating the UI
                // on device reboot quicker than listening to a BOOT_COMPLETE Intent.

                // TODO: Might make sense to listen to CONNECTIVITY_ACTION and update the
                // bikeStations once we have network on reboot.

                Log.i(TAG, "Bike status request failed, loading from cache. Error: => "
                        + error.toString());

                reloadFromCache(context, appWidgetManager);
            }
        });
    }

    private String getTargetStationName(int widgetId) {
        return preferences.getString(Integer.toString(widgetId), "");
    }

    private void storeCount(int widgetId, String bikeCount) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(Integer.toString(widgetId) + "_cached_count");
        editor.putString(Integer.toString(widgetId) + "_cached_count", bikeCount);
        editor.apply();
    }

    private void updateAppWidget(int widgetId, String stationName, String bikeCount) {
        updateUI(stationName, bikeCount);
        updateAppWidgetOnClick(context, remoteViews);
        appWidgetManager.updateAppWidget(widgetId, remoteViews);
    }

    private void logWidgetUpdate(String message, int widgetId, String stationName, String bikeCount) {
        Log.i(TAG, message +
                ": widgetId: " + widgetId +
                ", target station: " + stationName +
                ", bike count: " + bikeCount);
    }

    private void updateUI(String targetStation, String bikeCountString) {
        remoteViews.setTextViewText(R.id.stationName, targetStation);
        remoteViews.setTextViewText(R.id.bikeCount, ": " + bikeCountString);
    }

    private void updateAppWidgetOnClick(Context context, RemoteViews remoteViews) {
        Intent intent = new Intent(context, MyWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        remoteViews.setOnClickPendingIntent(R.id.widget_root_layout, pendingIntent);
    }

    private void reloadFromCache(Context context, AppWidgetManager appWidgetManager) {
        for (int widgetId : allWidgetIds) {
            String stationName = getTargetStationName(widgetId);
            String cachedBikeCount = getCachedBikeCount(widgetId);
            logWidgetUpdate("Reload widget from cache", widgetId, stationName, cachedBikeCount);
            updateAppWidget(widgetId, stationName, cachedBikeCount);
        }
    }

    private String getCachedBikeCount(int widgetId) {
        return preferences.getString(Integer.toString(widgetId) + "_cached_count",
                BikeStation.COUNT_UNKNOWN);
    }
}
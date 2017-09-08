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
                    if (station != null) {
                        Log.i(TAG, "Update widget: widgetId: " + widgetId +
                                ", target station: " + stationName +
                                ", bike count: " + station.bikesAvailable);

                        storeCount(widgetId, station.bikesAvailable);
                        updateUI(stationName, station.bikesAvailable);
                    }
                    else {
                        Log.e(TAG, "No target station selected");
                        remoteViews.setTextViewText(R.id.stationName, "No target station selected");
                    }

                    updateAppWidgetOnClick(context, remoteViews);

                    appWidgetManager.updateAppWidget(widgetId, remoteViews);
                }
            }

            private String getTargetStationName(int widgetId) {
                return preferences.getString(Integer.toString(widgetId), "");
            }

            @Override
            public void onError(String error) {
                // Looks like we may get an error response when rebooting the device (PIN
                // not entered yet). The widget update seems to be called earlier than
                // getting a BOOT_COMPLETE Intent though (resulting in this error then),
                // so loading the bikeStations from the cache here should handle updating the UI
                // on device reboot quicker. And no need to listen to a BOOT_COMPLETE Intent.

                // TODO: Might make sense to listen to CONNECTIVITY_ACTION and update the
                // bikeStations once we have network on reboot.

                Log.i(TAG, "Bike status request failed, loading from cache. Error: => " + error.toString());

                reloadFromCache(context, appWidgetManager, allWidgetIds);
            }
        });
    }

    private void storeCount(int widgetId, int bikeCount) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(Integer.toString(widgetId) + "_cached_count");
        editor.putInt(Integer.toString(widgetId) + "_cached_count", bikeCount);
        editor.apply();
    }

    private void updateUI(String targetStation, int bikeCount) {
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

    private void updateAppWidgetOnClick(Context context, RemoteViews remoteViews) {
        Intent intent = new Intent(context, MyWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        remoteViews.setOnClickPendingIntent(R.id.widget_root_layout, pendingIntent);
    }

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

            updateUI(targetStation, cachedBikeCount);

            updateAppWidgetOnClick(context, remoteViews);

            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }
}
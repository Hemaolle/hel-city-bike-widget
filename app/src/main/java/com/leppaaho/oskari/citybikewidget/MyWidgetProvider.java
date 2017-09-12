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
        BikeDataProvider.requestBikeData(context, new BikeDataProvider.BikeStationsListener() {
            @Override
            public void onResponse(BikeStations stations) {
                for (int widgetId : allWidgetIds) {
                    String stationName = getTargetStationName(widgetId);
                    BikeStation station = stations.find(stationName);
                    String bikeCount = BikeStation.getBikeCountString(station);
                    logWidgetUpdate("Update widget", widgetId, stationName, bikeCount);
                    updateAppWidget(widgetId, stationName, bikeCount);
                }
            }
        });
    }

    private String getTargetStationName(int widgetId) {
        return preferences.getString(Integer.toString(widgetId), "");
    }

    private void logWidgetUpdate(String message, int widgetId, String stationName, String bikeCount) {
        Log.i(TAG, message +
                ": widgetId: " + widgetId +
                ", target station: " + stationName +
                ", bike count: " + bikeCount);
    }

    private void updateAppWidget(int widgetId, String stationName, String bikeCount) {
        updateUI(stationName, bikeCount);
        updateAppWidgetOnClick(context, remoteViews);
        appWidgetManager.updateAppWidget(widgetId, remoteViews);
    }

    private void updateUI(String targetStation, String bikeCountString) {
        // TODO: Finnish hyphenation would be nice for long station names.
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
}
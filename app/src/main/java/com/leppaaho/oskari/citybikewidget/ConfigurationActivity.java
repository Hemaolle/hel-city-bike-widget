package com.leppaaho.oskari.citybikewidget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.List;

public class ConfigurationActivity extends AppCompatActivity {

    private static final String TAG = ConfigurationActivity.class.getName();

    private ListView allStationsListView;
    private Context applicationContext;
    private SharedPreferences sharedPreferences;
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Ensures that the widget is not created if the user cancels the configuration.
        setResult(RESULT_CANCELED);

        setContentView(R.layout.activity_configuration);

        applicationContext = getApplicationContext();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        allStationsListView = (ListView) findViewById(R.id.listview);

        allStationsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
                String selectedStation = adapter.getItemAtPosition(position).toString();

                Log.i(TAG, "target station for widget " + appWidgetId + " selected: " + selectedStation);

                saveAppWidgetStation(selectedStation, appWidgetId);
                sendAppWidgetUpdateIntent(appWidgetId);
                setResultOk();
                finish();
            }

            private void saveAppWidgetStation(String stationName, int appWidgetId) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.remove(Integer.toString(appWidgetId));
                editor.putString(Integer.toString(appWidgetId), stationName);
                editor.apply();
            }

            // Triggers the onUpdate method in the WidgetProvider.
            private void sendAppWidgetUpdateIntent(int appWidgetId) {
                Intent intent = new Intent(applicationContext, MyWidgetProvider.class);
                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

                // Use an array and EXTRA_APPWIDGET_IDS instead of AppWidgetManager.EXTRA_APPWIDGET_ID,
                // since it seems the onUpdate() is only fired like this.
                int[] ids = {appWidgetId};

                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                sendBroadcast(intent);
            }

            // AppWidgetHost (the home screen) will read the result. RESULT_OK from a configuration
            // activity should result in displaying the widget after finishing the configuration.
            private void setResultOk() {
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                setResult(RESULT_OK, resultValue);
            }
        });

        requestAllStations();
    }

    private void requestAllStations() {
        final Activity main = this;

        BikeApiClient.getStations(this, new BikeApiClient.BikeApiResponseListener() {
            @Override
            public void onResponse(BikeStations stations) {
                List<String> stationNames = stations.getNames();
                final ArrayAdapter<String> adapter = new ArrayAdapter<>(main,
                        android.R.layout.simple_selectable_list_item,  stationNames);
                allStationsListView.setAdapter(adapter);
                allStationsListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            }

            @Override
            public void onError(String error) {
                Log.d(TAG, "error => " + error);

                ErrorDialog.show(ConfigurationActivity.this, new ErrorDialog.ResultListener() {
                    @Override
                    public void onCancel() {
                        finish();
                    }

                    @Override
                    public void onRetry() {
                        requestAllStations();
                    }
                });
            }
        });
    }
}

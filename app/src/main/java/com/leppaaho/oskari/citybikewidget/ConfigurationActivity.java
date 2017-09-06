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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class ConfigurationActivity extends AppCompatActivity {

    private static final String TAG = ConfigurationActivity.class.getName();

    ListView allStationsListView;
    HashMap<String, String> stationNamesToIds = new HashMap<>();
    Context applicationContext;
    SharedPreferences sharedPreferences;
    AppWidgetManager widgetManager;
    RemoteViews remoteViews;
    int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Ensures that the widget is not created if the user cancels the configuration.
        setResult(RESULT_CANCELED);

        setContentView(R.layout.activity_configuration);

        applicationContext = getApplicationContext();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        widgetManager = AppWidgetManager.getInstance(this);
        remoteViews = new RemoteViews(this.getPackageName(), R.layout.widget_layout);

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
                // since it seems the onUpdate() is only fired on that.
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

        requestWithSomeHttpHeaders();
    }

        public void requestWithSomeHttpHeaders() {
            RequestQueue queue = Volley.newRequestQueue(this);
            String url = "http://api.digitransit.fi/routing/v1/routers/hsl/index/graphql";
            JSONObject data = null;

            try {
                data = new JSONObject(
                        "{\"query\": \"{ bikeRentalStations { name stationId } }\" }"
                );
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Log.i(TAG, data.toString());

            final Activity main = this;

            JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, data,
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

                            final ArrayList<String> list = new ArrayList<String>();
                            for (int i = 0; i < stations.length(); ++i) {
                                try {
                                    String stationName = stations.getJSONObject(i).getString("name");
                                    String stationId = stations.getJSONObject(i).getString("stationId");
                                    list.add(stationName);
                                    stationNamesToIds.put(stationName, stationId);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                            Collections.sort(list);

                            final ArrayAdapter<String> adapter = new ArrayAdapter<String>(main,
                                    android.R.layout.simple_selectable_list_item, list);
                            allStationsListView.setAdapter(adapter);
                            allStationsListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                        }
                    },
                    new Response.ErrorListener()
                    {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // TODO Auto-generated method stub
                            Log.d(TAG, "error => "+error.toString());
                        }
                    }
            );
            queue.add(postRequest);

        }
}

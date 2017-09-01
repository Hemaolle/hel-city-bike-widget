package com.leppaaho.oskari.citybikewidget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
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
import java.util.HashSet;
import java.util.Set;

public class ConfigurationActivity extends AppCompatActivity {

    ListView listview = null;
    HashMap<String, String> stationNamesToIds = new HashMap<String, String>();
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
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

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

        listview = (ListView) findViewById(R.id.listview);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                CheckedTextView checkedTextView = (CheckedTextView) view;
                String targetStation = checkedTextView.getText().toString();
                boolean checked = checkedTextView.isChecked();

                SharedPreferences.Editor editor = sharedPreferences.edit();

                Log.i("INFO", "target station for widget " + appWidgetId + " selected: " + targetStation);

                editor.remove(Integer.toString(appWidgetId));
                editor.putString(Integer.toString(appWidgetId), targetStation);
                editor.apply();

                Intent intent = new Intent(applicationContext, MyWidgetProvider.class);
                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                // Use an array and EXTRA_APPWIDGET_IDS instead of AppWidgetManager.EXTRA_APPWIDGET_ID,
                // since it seems the onUpdate() is only fired on that:
                int[] ids = {appWidgetId};

                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                sendBroadcast(intent);

                Intent resultValue = new Intent();
                // Set the results as expected from a 'configure activity'.
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                setResult(RESULT_OK, resultValue);

                finish();
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

            Log.i("INFO", data.toString());

            final Activity main = this;

            JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, data,
                    new Response.Listener<JSONObject>()
                    {
                        @Override
                        public void onResponse(JSONObject response) {
                            // response
                            Log.d("Response", response.toString());

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
                            listview.setAdapter(adapter);
                            listview.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_configuration, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

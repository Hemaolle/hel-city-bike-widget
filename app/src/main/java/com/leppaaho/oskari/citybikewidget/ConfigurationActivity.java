package com.leppaaho.oskari.citybikewidget;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class ConfigurationActivity extends AppCompatActivity {

    ListView listview = null;
    HashMap<String, String> stationNamesToIds;
    Context applicationContext;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        sharedPreferences = applicationContext.getSharedPreferences("prefs", MODE_PRIVATE);

        listview = (ListView) findViewById(R.id.listview);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                CheckedTextView checkedTextView = (CheckedTextView) view;
                String targetStation = checkedTextView.getText().toString();
                boolean checked = checkedTextView.isChecked();

                Set<String> selectedStations =
                        sharedPreferences.getStringSet("selected_stations", new HashSet<String>());
                if (checked)  {
                    Log.i("INFO", "add " + targetStation);
                    selectedStations.add(targetStation);
                }
                else {
                    selectedStations.remove(targetStation);
                }
                SharedPreferences.Editor editor = sharedPreferences.edit();

                editor.putStringSet("selected_stations", selectedStations);

                editor.apply();
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
                                    list.add(stations.getJSONObject(i).getString("name"));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                            Collections.sort(list);

                            final ArrayAdapter<String> adapter = new ArrayAdapter<String>(main,
                                    android.R.layout.simple_list_item_multiple_choice, list);
                            listview.setAdapter(adapter);
                            listview.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
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

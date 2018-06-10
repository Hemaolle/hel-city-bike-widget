package com.leppaaho.oskari.citybikewidget;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WebView instructionsView = (WebView) findViewById(R.id.instructions_web_view);
        instructionsView.loadUrl("file:///android_res/raw/instructions.html");
    }
}

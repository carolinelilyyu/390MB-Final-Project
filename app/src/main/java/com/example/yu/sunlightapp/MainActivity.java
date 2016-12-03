package com.example.yu.sunlightapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.widget.TextView;

import com.example.yu.sunlightapp.services.AccelerometerService;

public class MainActivity extends Activity {
    AccelerometerService accelerometerService = new AccelerometerService();
    TextView acceleration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AccelerometerService acc = new AccelerometerService();
        acc.init(this);
    }
}

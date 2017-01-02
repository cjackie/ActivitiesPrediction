package com.example.luyu.activity_recognition_gyroscope;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;


import com.example.luyu.activity_recognition_gyroscope.sensordata.GyroscopeDataItem;

public class MainActivity extends Activity implements ServiceConnection {

    final static String TAG = "ACTIVITY_RECOGNITION_W";

    private TextView mTextView;
    private Boolean mSensorDataCollectingServiceConnected;
    private SensorDataCollectingService mSensorDataCollectingService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
                mTextView.setText("Service for collecting sensor data has started.");
            }
        });
        startService(new Intent(this, SensorDataCollectingService.class));

        mSensorDataCollectingServiceConnected = false;
        bindService(new Intent(this, SensorDataCollectingService.class), this, BIND_ABOVE_CLIENT);

    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        mSensorDataCollectingServiceConnected = true;
        mSensorDataCollectingService =
                ((SensorDataCollectingService.SensorDataCollectingServiceLocalBinder)iBinder).getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        mSensorDataCollectingServiceConnected = false;
        mSensorDataCollectingService = null;
    }
}
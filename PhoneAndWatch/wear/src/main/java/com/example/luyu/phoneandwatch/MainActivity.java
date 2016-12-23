package com.example.luyu.phoneandwatch;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

public class MainActivity extends Activity implements SensorEventListener {

    final static String TAG = "ACTIVITY_RECOGNITION";

    private GoogleApiClient mGoogleApiClient;
    private TextView mTextView;
    private SensorManager mSensorManager;
    private Sensor mAccelerameter;
    private static final String SENSOR_DATA_PATH = "/sensor-data";
    private TextView textX, textY, textZ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerameter = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
                textX = (TextView)stub.findViewById(R.id.textX);
                textY = (TextView)stub.findViewById(R.id.textY);
                textZ = (TextView)stub.findViewById(R.id.textZ);
            }
        });
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "on resume");
        super.onResume();
        mGoogleApiClient.connect();
        mSensorManager.registerListener(this, mAccelerameter, mSensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "execute on pause ");
        super.onPause();
        mGoogleApiClient.disconnect();
        mSensorManager.unregisterListener(this);

    }

    public void onSensorChanged(SensorEvent event) {
        String key = event.sensor.getName();
        Log.d(TAG, "New reading for sensor: "+key);

        float[] values = new float[3];

        values[0] = event.values[0];
        values[1] = event.values[1];
        values[2] = event.values[2];


//        Log.d(TAG, "get values: "+values[0]+" "+values[1]+" "+values[2]);

        textX.setText(" "+values[0]);
        textY.setText(" "+values[1]);
        textZ.setText(" "+values[2]);

        PutDataMapRequest sensorData = PutDataMapRequest.create(SENSOR_DATA_PATH);
        sensorData.getDataMap().putFloatArray("accelerometer", values);
        sensorData.getDataMap().putInt(key + " Accuracy", event.accuracy);


        PutDataRequest request = sensorData.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleApiClient, request);


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        return;
    }


}





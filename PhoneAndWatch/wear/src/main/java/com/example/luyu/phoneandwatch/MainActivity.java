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

public class MainActivity extends Activity implements SensorEventListener{

    private GoogleApiClient mGoogleApiClient;
    private TextView mTextView;
    private SensorManager sensorManager;
    private Sensor gyro;
    private PutDataMapRequest sensorData;
    private static final String SENSOR_DATA_PATH = "/sensor-data";
    private TextView textX, textY, textZ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
                textX = (TextView)stub.findViewById(R.id.textX);
                textY = (TextView)stub.findViewById(R.id.textY);
                textZ = (TextView)stub.findViewById(R.id.textZ);
                getSensorData();
            }
        });
        Log.d("MainActivity", "execute on create ");

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();

    }

    public void getSensorData() {

        Log.d("MainActivity", "execute on resume ");
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        gyro = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, gyro,
                SensorManager.SENSOR_DELAY_NORMAL);

    }

    protected void onPause() {
        Log.d("MainActivity", "execute on pause ");
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    public void onSensorChanged(SensorEvent event) {
        String key = event.sensor.getName();
        Log.d("MainActivity", "New reading for sensor: "+key);

        float[] values = new float[3];

        values[0] = event.values[0];
        values[1] = event.values[1];
        values[2] = event.values[2];


        Log.d("MainActivity", "get values: "+values[0]+" "+values[1]+" "+values[2]);

        textX.setText(" "+values[0]);
        textY.setText(" "+values[1]);
        textZ.setText(" "+values[2]);

        sensorData = PutDataMapRequest.create(SENSOR_DATA_PATH);
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





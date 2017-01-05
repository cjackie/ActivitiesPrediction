package com.example.luyu.activity_recognition_all;

import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.luyu.activity_recognition_all.sensordata.AccelerometerDataItem;
import com.example.luyu.activity_recognition_all.sensordata.AmbientLightDataItem;
import com.example.luyu.activity_recognition_all.sensordata.GyroscopeDataItem;
import com.example.luyu.activity_recognition_all.sensordata.HeartRateDataItem;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;


import com.example.luyu.activity_recognition_all.sensordata.SensorLocalStorage;

public class MainActivity extends AppCompatActivity implements
        DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    public final static String TAG = "ACTIVITY_RECOGNITION_P";

    private TextView mTextX, mTextY, mTextZ;
    private GoogleApiClient mGoogleApiClient;
    private SensorLocalStorage mStorage;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextX = (TextView)findViewById(R.id.textX);
        mTextY = (TextView)findViewById(R.id.textY);
        mTextZ = (TextView)findViewById(R.id.textZ);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.registerConnectionCallbacks(this);
        mGoogleApiClient.registerConnectionFailedListener(this);

        mStorage = new SensorLocalStorage(this);
    }

    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
        mTextX.setText("waiting");
        mTextY.setText("waiting");
        mTextZ.setText("waiting");
    }


    @Override
    public void onConnected(Bundle bundle) {
        Wearable.DataApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        throw new RuntimeException("connection suspended.");
    }

    @Override
    protected void onPause() {
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
        super.onPause();
    }


    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
//        Log.d(TAG, "onDataChanged: " + dataEvents);

        for (DataEvent event : dataEvents) {
            DataItem item = event.getDataItem();

            if (item.getUri().getPath().compareTo(AccelerometerDataItem.PATH) == 0) {
                AccelerometerDataItem accelData = new AccelerometerDataItem();
                accelData.setData(item.getData());
                mTextX.setText(accelData.getX()+"");
                mTextY.setText(accelData.getY()+"");
                mTextZ.setText(accelData.getZ()+"");
                mStorage.save(accelData);
            }

             if (item.getUri().getPath().compareTo(GyroscopeDataItem.PATH) == 0) {
                GyroscopeDataItem GyroData = new GyroscopeDataItem();
                GyroData.setData(item.getData());
                mTextX.setText(GyroData.getX()+"");
                mTextY.setText(GyroData.getY()+"");
                mTextZ.setText(GyroData.getZ()+"");
                mStorage.save(GyroData);
            }

            if (item.getUri().getPath().compareTo(AmbientLightDataItem.PATH) == 0) {
                AmbientLightDataItem AmbientLightData = new AmbientLightDataItem();
                AmbientLightData.setData(item.getData());
                mTextX.setText(AmbientLightData.getX()+"");
                mTextY.setText("xxx");
                mTextZ.setText("xxx");
                mStorage.save(AmbientLightData);
            }

            if (item.getUri().getPath().compareTo(HeartRateDataItem.PATH) == 0) {
                HeartRateDataItem HeartRateData = new HeartRateDataItem();
                HeartRateData.setData(item.getData());
                mTextX.setText(HeartRateData.getX()+"");
                mTextY.setText("xxx");
                mTextZ.setText("xxx");
                mStorage.save(HeartRateData);
            }
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        throw new RuntimeException("connection fail.");

    }


}

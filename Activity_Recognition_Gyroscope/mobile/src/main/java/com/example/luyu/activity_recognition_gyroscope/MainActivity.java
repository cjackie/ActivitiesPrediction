package com.example.luyu.activity_recognition_gyroscope;

import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Set;

import com.example.luyu.activity_recognition_gyroscope.sensordata.GyroscopeDataItem;
import com.example.luyu.activity_recognition_gyroscope.sensordata.SensorLocalStorage;

public class MainActivity extends AppCompatActivity implements
        DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    public final static String TAG = "ACTIVITY_RECOGNITION_G";

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
            if (item.getUri().getPath().compareTo(GyroscopeDataItem.PATH) == 0) {
                GyroscopeDataItem GyroData = new GyroscopeDataItem();
                GyroData.setData(item.getData());
                mTextX.setText(GyroData.getX()+"");
                mTextY.setText(GyroData.getY()+"");
                mTextZ.setText(GyroData.getZ()+"");
                mStorage.save(GyroData);
            }
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        throw new RuntimeException("connection fail.");

    }


}
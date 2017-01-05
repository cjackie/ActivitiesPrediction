package com.example.luyu.activity_recognition_all;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;


import com.example.luyu.activity_recognition_all.sensordata.AmbientLightDataItem;
import com.example.luyu.activity_recognition_all.sensordata.HeartRateDataItem;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import com.example.luyu.activity_recognition_all.sensordata.AccelerometerDataItem;
import com.example.luyu.activity_recognition_all.sensordata.GyroscopeDataItem;

public class SensorDataCollectingService extends Service implements SensorEventListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    final static String TAG = "ACTIVITY_RECOGNITION_S";

    private GoogleApiClient mGoogleApiClient;
    private SensorManager mSensorManager;

    private Sensor mAccelerameter;
    private Sensor mAmbientLightSensor;
    private Sensor mGyroscope;
    private Sensor mHeartRate;

    private Boolean mStarted;

    public SensorDataCollectingService() { }

    @Override
    public void onCreate() {
        super.onCreate();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.registerConnectionCallbacks(this);
        mGoogleApiClient.registerConnectionFailedListener(this);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        mAccelerameter = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mAmbientLightSensor=mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mHeartRate=mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

        mStarted = false;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {


        mSensorManager.registerListener(this, mGyroscope, mSensorManager.SENSOR_DELAY_NORMAL);

        mSensorManager.registerListener(this, mAccelerameter, mSensorManager.SENSOR_DELAY_NORMAL);

        mSensorManager.registerListener(this,  mAmbientLightSensor, mSensorManager.SENSOR_DELAY_NORMAL);

        mSensorManager.registerListener(this,  mHeartRate,  mSensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.w(TAG, "google api connection suspended.");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "connection to google api failed.");
        throw new RuntimeException("google api connection fail...");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                Log.e(TAG, "To get Accelerometer data");
                AccelerometerDataItem accItem = new AccelerometerDataItem(event.values[0],
                        event.values[1], event.values[2], event.timestamp);
                PutDataRequest accRequest = PutDataRequest.create(AccelerometerDataItem.PATH);
                accRequest.setData(accItem.getData());
                Wearable.DataApi.putDataItem(mGoogleApiClient, accRequest);
                break;

            case Sensor.TYPE_GYROSCOPE:
                Log.e(TAG, "To get Gyroscope data");
                GyroscopeDataItem gyroItem = new GyroscopeDataItem(event.values[0],
                        event.values[1], event.values[2], event.timestamp);
                PutDataRequest gyroRequest = PutDataRequest.create(GyroscopeDataItem.PATH);
                gyroRequest.setData(gyroItem.getData());
                Wearable.DataApi.putDataItem(mGoogleApiClient, gyroRequest);
                break;

            case Sensor.TYPE_LIGHT:
                Log.e(TAG, "To get Ambient Light data");
                AmbientLightDataItem ambientItem = new AmbientLightDataItem(event.values[0],
                        event.timestamp);
                PutDataRequest ambientRequest = PutDataRequest.create(AmbientLightDataItem.PATH);
                ambientRequest.setData(ambientItem.getData());
                Wearable.DataApi.putDataItem(mGoogleApiClient, ambientRequest);
                break;

            case Sensor.TYPE_HEART_RATE:
                Log.e(TAG, "To get Heart data");
                HeartRateDataItem heartRateItem = new HeartRateDataItem(event.values[0],
                        event.timestamp);
                PutDataRequest heartRateRequest = PutDataRequest.create(HeartRateDataItem.PATH);
                heartRateRequest.setData(heartRateItem.getData());
                Wearable.DataApi.putDataItem(mGoogleApiClient, heartRateRequest);
                break;

            default:
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    // Because this service runs on the same process, other threads can access it:
    //  ((SensorDataCollectingServiceLocalBinder) mBinder).getService() to get the
    //  reference to this object.
    public class SensorDataCollectingServiceLocalBinder extends Binder {
        SensorDataCollectingService getService() {
            return SensorDataCollectingService.this;
        }
    }
    private IBinder mBinder = new SensorDataCollectingServiceLocalBinder();
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        synchronized (mStarted) {
            if (mStarted == true) {
                return START_STICKY;
            }
        }

        mGoogleApiClient.connect();
        mStarted = true;

        return START_STICKY;
    }

}
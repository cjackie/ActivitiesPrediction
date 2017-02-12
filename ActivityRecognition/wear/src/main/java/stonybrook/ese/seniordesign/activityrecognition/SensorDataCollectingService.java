package stonybrook.ese.seniordesign.activityrecognition;

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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import stonybrook.ese.seniordesign.activityrecognition.sensordata.AccelerometerDataItem;
import stonybrook.ese.seniordesign.activityrecognition.sensordata.GyroscopeDataItem;

public class SensorDataCollectingService extends Service implements SensorEventListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    final static String TAG = "ACTIVITY_RECOGNITION_S";

    public enum SendingState {
        UNKNOWN,
        SENDING,
        NOT_SENDING,
    }

    private GoogleApiClient mGoogleApiClient;
    private SensorManager mSensorManager;
    private Sensor mAccelerameter;
    private Sensor mGyroscope;
    private Boolean mStarted;
    private Boolean mGoogleApiConnected;
    private SendingState mState;

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
        mStarted = false;
        mState = SendingState.NOT_SENDING;
        mGoogleApiConnected = false;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mSensorManager.registerListener(this, mAccelerameter, mSensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mGyroscope, mSensorManager.SENSOR_DELAY_FASTEST);
        mState = SendingState.SENDING;
        mGoogleApiConnected = true;
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.w(TAG, "google api connection suspended.");
        mGoogleApiConnected = false;
        mState = SendingState.NOT_SENDING;
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
                AccelerometerDataItem accelItem = new AccelerometerDataItem(event.values[0],
                        event.values[1], event.values[2], event.timestamp);
                PutDataRequest accelRequest = PutDataRequest.create(AccelerometerDataItem.PATH);
                accelRequest.setData(accelItem.getData());
                Wearable.DataApi.putDataItem(mGoogleApiClient, accelRequest);
                break;
            case Sensor.TYPE_GYROSCOPE:
                GyroscopeDataItem gyroItem = new GyroscopeDataItem(event.values[0],
                        event.values[1], event.values[2], event.timestamp);
                PutDataRequest gyroRequest = PutDataRequest.create(GyroscopeDataItem.PATH);
                gyroRequest.setData(gyroItem.getData());
                Wearable.DataApi.putDataItem(mGoogleApiClient, gyroRequest);
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
                return START_NOT_STICKY;
            }
        }

        mGoogleApiClient.connect();
        mStarted = true;

        return START_NOT_STICKY;
    }

    public boolean isSendingSensorData() {
        if (mState == SendingState.SENDING) {
            return true;
        } else if (mState == SendingState.NOT_SENDING){
            return false;
        } else {
            throw new RuntimeException("unknow state??");
        }
    }

    // @return. indicate the action is successful or not.
    public boolean pauseSending() {
        if (mGoogleApiConnected && mState == SendingState.SENDING) {
            mSensorManager.unregisterListener(this);
            mState = SendingState.NOT_SENDING;
            return true;
        } else {
            return false;
        }
    }

    public boolean resumeSending() {
        if (mGoogleApiConnected && mState == SendingState.NOT_SENDING) {
            mSensorManager.registerListener(this, mAccelerameter, mSensorManager.SENSOR_DELAY_NORMAL);
            mSensorManager.registerListener(this, mGyroscope, mSensorManager.SENSOR_DELAY_NORMAL);
            mState = SendingState.SENDING;
            return true;
        } else {
            return false;
        }
    }

}

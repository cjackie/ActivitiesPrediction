package stonybrook.ese.seniordesign.activityrecognition;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.internal.BinderWrapper;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.Wearable;

import stonybrook.ese.seniordesign.activityrecognition.sensordata.AccelerometerDataItem;
import stonybrook.ese.seniordesign.activityrecognition.sensordata.GyroscopeDataItem;
import stonybrook.ese.seniordesign.activityrecognition.sensordata.SensorDataItem;
import stonybrook.ese.seniordesign.activityrecognition.sensordata.SensorLocalStorage;

public class SensorDataStoringService extends Service implements
        DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;
    private SensorLocalStorage mStorage;
    private State mState;
    private boolean mStarted;
    private StringBuilder mCurrentAccelData;
    private StringBuilder mCurrentGyroData;

    public enum State {
        NOT_STARTED,
        STORING_DATA,
        DONE_COLLECING
    }

    public SensorDataStoringService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.registerConnectionCallbacks(this);
        mGoogleApiClient.registerConnectionFailedListener(this);
        mGoogleApiClient.connect();

        mStorage = new SensorLocalStorage(this.getBaseContext());
        mState = State.NOT_STARTED;
        mStarted = false;

        mCurrentAccelData = new StringBuilder();
        mCurrentGyroData = new StringBuilder();
    }

    public class SensorDataStoringServiceLocalBinder extends Binder {
        SensorDataStoringService getService() {
            return SensorDataStoringService.this;
        }
    }
    private IBinder mBinder = new SensorDataStoringServiceLocalBinder();
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (State.STORING_DATA == mState) {
            mStorage.finishSaving();
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            DataItem item = event.getDataItem();
            if (item.getUri().getPath().compareTo(AccelerometerDataItem.PATH) == 0) {
                AccelerometerDataItem accelData = new AccelerometerDataItem();
                accelData.setData(item.getData());
                mCurrentAccelData.append(String.format("%1$f,%2$f,%3$f,%4$f\n", accelData.getTime(),
                        accelData.getX(), accelData.getY(), accelData.getZ()));
                mStorage.save(accelData);
            } else if (item.getUri().getPath().compareTo(GyroscopeDataItem.PATH) == 0) {
                GyroscopeDataItem gyroData = new GyroscopeDataItem();
                gyroData.setData(item.getData());
                mCurrentAccelData.append(String.format("%1$f,%2$f,%3$f,%4$f\n", gyroData.getTime(),
                        gyroData.getX(), gyroData.getY(), gyroData.getZ()));
                mStorage.save(gyroData);
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Wearable.DataApi.addListener(mGoogleApiClient, this);
    }

    /** APIs **/
    public void startNewRecord(String label) {
        mCurrentAccelData = new StringBuilder();
        mCurrentGyroData = new StringBuilder();

        if (State.STORING_DATA == mState) {
            mStorage.finishSaving();
        }
        mStorage.startSaving(label);
        mState = State.STORING_DATA;
    }

    public void finishStoring() {
        if (State.STORING_DATA == mState) {
            mStorage.finishSaving();
        }
        mState = State.DONE_COLLECING;
    }

    public String getAccelRecord() {
        return mCurrentAccelData.toString();
    }

    public String getGyroRecord() {
        return mCurrentGyroData.toString();
    }

    public State getState() {
        return mState;
    }

    @Override
    public void onConnectionSuspended(int i) {
        throw new RuntimeException("connection suspended.");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        throw new RuntimeException("connection fail.");
    }
}

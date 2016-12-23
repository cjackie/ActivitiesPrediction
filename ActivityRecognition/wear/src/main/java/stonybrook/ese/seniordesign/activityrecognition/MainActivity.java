package stonybrook.ese.seniordesign.activityrecognition;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
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


import stonybrook.ese.seniordesign.activityrecognition.sensordata.AccelerometerDataItem;

public class MainActivity extends Activity implements SensorEventListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    final static String TAG = "ACTIVITY_RECOGNITION_W";

    private GoogleApiClient mGoogleApiClient;
    private AccelerometerDataItem mAccelData;
    private TextView mTextView;
    private SensorManager mSensorManager;
    private Sensor mAccelerameter;
    private TextView mTextX, mTextY, mTextZ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.registerConnectionCallbacks(this);
        mGoogleApiClient.registerConnectionFailedListener(this);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerameter = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccelData = null;

        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
                mTextX = (TextView)stub.findViewById(R.id.textX);
                mTextY = (TextView)stub.findViewById(R.id.textY);
                mTextZ = (TextView)stub.findViewById(R.id.textZ);
            }
        });

    }

    @Override
    protected void onResume() {
        Log.d(TAG, "on resume");
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "execute on pause ");
        super.onPause();
        mSensorManager.unregisterListener(this);
    }


    public void onSensorChanged(SensorEvent event) {

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                // temp
                mTextX.setText(" " + event.values[0]);
                mTextY.setText(" " + event.values[1]);
                mTextZ.setText(" " + event.values[2]);
                // end of temp

                AccelerometerDataItem item = new AccelerometerDataItem(event.values[0],
                        event.values[1], event.values[2], event.timestamp);
                PutDataRequest request = PutDataRequest.create(AccelerometerDataItem.PATH);
                request.setData(item.getData());
                Wearable.DataApi.putDataItem(mGoogleApiClient, request);
                break;
            default:
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        return;
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "google connection succeeded.");
        mSensorManager.registerListener(this, mAccelerameter, mSensorManager.SENSOR_DELAY_NORMAL);
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

}





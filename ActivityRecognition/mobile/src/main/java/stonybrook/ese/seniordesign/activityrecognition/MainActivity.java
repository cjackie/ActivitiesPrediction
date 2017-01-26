package stonybrook.ese.seniordesign.activityrecognition;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.Wearable;


import stonybrook.ese.seniordesign.activityrecognition.sensordata.AccelerometerDataItem;
import stonybrook.ese.seniordesign.activityrecognition.sensordata.SensorLocalStorage;



public class MainActivity extends AppCompatActivity implements
        DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    public final static String TAG = "ACTIVITY_RECOGNITION_P";

    private enum SavingState {
        STARTED,
        NOT_STARTED
    }

    private TextView mTextX;
    private EditText mLabelText;
    private Button mStartBtn;
    private Button mDoneBtn;
    private GoogleApiClient mGoogleApiClient;
    private SensorLocalStorage mStorage;
    private SavingState mSavingState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextX = (TextView)findViewById(R.id.textX);
        mStartBtn = (Button)findViewById(R.id.startCollect);
        mDoneBtn = (Button)findViewById(R.id.finishCollect);
        mLabelText = (EditText)findViewById(R.id.labelText);
        mSavingState = SavingState.NOT_STARTED;

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.registerConnectionCallbacks(this);
        mGoogleApiClient.registerConnectionFailedListener(this);

        mStorage = new SensorLocalStorage(this);
        mStartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String label = mLabelText.getText().toString();
                mStorage.startSaving(label);
                mSavingState = SavingState.STARTED;
            }
        });
        mDoneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mStorage.finishSaving();
                mSavingState = SavingState.NOT_STARTED;
            }
        });
    }

    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
        mTextX.setText("waiting");
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
        mStorage.flushAll();
        super.onPause();
    }


    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
//        Log.d(TAG, "onDataChanged: " + dataEvents);
        if (SavingState.STARTED != mSavingState) {
            // ignore all incoming data.
            return ;
        }

        for (DataEvent event : dataEvents) {
            DataItem item = event.getDataItem();
            if (item.getUri().getPath().compareTo(AccelerometerDataItem.PATH) == 0) {
                AccelerometerDataItem accelData = new AccelerometerDataItem();
                accelData.setData(item.getData());
                mTextX.setText(accelData.getX()+"");
                mStorage.save(accelData);
            }
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        throw new RuntimeException("connection fail.");

    }


}

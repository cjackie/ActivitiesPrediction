package stonybrook.ese.seniordesign.activityrecognition;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends Activity implements ServiceConnection {

    final static String TAG = "ACTIVITY_RECOGNITION_W";

    private TextView mStateText;
    private TextView mMessageText;
    private Button mtoggleCollect;
    private boolean mLayoutInflated;
    private Boolean mSensorDataCollectingServiceConnected;
    private SensorDataCollectingService mSensorDataCollectingService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSensorDataCollectingServiceConnected = false;
        mLayoutInflated = false;

        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mStateText = (TextView) stub.findViewById(R.id.state);
                mMessageText = (TextView) stub.findViewById(R.id.message);
                mtoggleCollect = (Button) stub.findViewById(R.id.toggleCollect);

                mStateText.setText("State: unkwown.");
                mtoggleCollect.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!mSensorDataCollectingServiceConnected) {
                            mMessageText.setText("service not connected yet with this activity.");
                            return ;
                        }

                        if (mSensorDataCollectingService.isSendingSensorData()) {
                            mSensorDataCollectingService.pauseSending();
                        } else {
                            mSensorDataCollectingService.resumeSending();
                        }
                        updateStateText();
                    }
                });

                mLayoutInflated = true;
            }
        });
        startService(new Intent(this, SensorDataCollectingService.class));
        bindService(new Intent(this, SensorDataCollectingService.class), this, BIND_ABOVE_CLIENT);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mLayoutInflated){
            updateStateText();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, SensorDataCollectingService.class));
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        mSensorDataCollectingServiceConnected = true;
        mSensorDataCollectingService =
                ((SensorDataCollectingService.SensorDataCollectingServiceLocalBinder)iBinder).getService();
        updateStateText();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        mSensorDataCollectingServiceConnected = false;
        mSensorDataCollectingService = null;
    }

    private void updateStateText() {
        if (!mSensorDataCollectingServiceConnected) {
            mStateText.setText("State: unknown");
        } else if (mSensorDataCollectingService.isSendingSensorData()) {
            mStateText.setText("State: sending");
        } else {
            mStateText.setText("State: not sending");
        }
    }
}





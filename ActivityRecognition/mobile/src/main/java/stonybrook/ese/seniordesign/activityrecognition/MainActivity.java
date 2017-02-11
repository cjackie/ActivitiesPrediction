package stonybrook.ese.seniordesign.activityrecognition;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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


import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;

import stonybrook.ese.seniordesign.activityrecognition.sensordata.AccelerometerDataItem;
import stonybrook.ese.seniordesign.activityrecognition.sensordata.GyroscopeDataItem;
import stonybrook.ese.seniordesign.activityrecognition.sensordata.SensorLocalStorage;



public class MainActivity extends AppCompatActivity implements ServiceConnection {
    public final static String SERVER_ADDR = "www.kbumsik.net";
    public final static int SERVER_PORT = 9999;
    public final static String TAG = "ACTIVITY_RECOGNITION_P";

    private TextView mStateText;
    private EditText mLabelText;
    private Button mStartBtn;
    private Button mDoneBtn;
    private SensorDataStoringService mServcie;

    // UI for communication
    private TextView mCommStateText;
    private EditText mCommSendMsgText;
    private Button mCommSendBtn;
    private TextView mCommRecvMsgText;
    CommTask commTask;

    // Communication component
    Socket comm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setting Recording components
        mStateText = (TextView)findViewById(R.id.stateText);
        mStartBtn = (Button)findViewById(R.id.startCollect);
        mDoneBtn = (Button)findViewById(R.id.finishCollect);
        mLabelText = (EditText)findViewById(R.id.labelText);

        mStartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String label = mLabelText.getText().toString();
                if (null != mServcie) {
                    if (SensorDataStoringService.State.NOT_STARTED == mServcie.getState() ||
                            SensorDataStoringService.State.DONE_COLLECING == mServcie.getState()) {
                        mServcie.startNewRecord(label);
                        mStateText.setText("storing data");
                    }
                }
            }
        });
        mDoneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            if (null != mServcie) {
                if (SensorDataStoringService.State.STORING_DATA == mServcie.getState()) {
                    mServcie.finishStoring();
                    mStateText.setText("done storing. you can start another one.");
                }
            }
            }
        });

        mStateText.setText("service is not bound yet");
        mServcie = null;
        startService(new Intent(this, SensorDataStoringService.class));
        bindService(new Intent(this, SensorDataStoringService.class), this, BIND_ABOVE_CLIENT);

        // Setting Comm UI components

        mCommStateText = (TextView)findViewById(R.id.CommStateText);
        mCommSendMsgText = (EditText) findViewById(R.id.CommSendMsgText);
        mCommSendBtn = (Button)findViewById(R.id.CommSendMsgBtn);
        mCommRecvMsgText = (TextView)findViewById(R.id.CommRecvMsgText);

        // Setting Communication components

        mCommSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                commTask = new CommTask(SERVER_ADDR, SERVER_PORT, mCommStateText, mCommRecvMsgText);
                byte[] buffer = mCommSendMsgText.getText().toString().getBytes();
                commTask.execute(buffer);
            }
        });
    }

    protected void onResume() {
        super.onResume();
    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, SensorDataStoringService.class));
    }


    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        mStateText.setText("service is bound.");
        mServcie = ((SensorDataStoringService.SensorDataStoringServiceLocalBinder)iBinder).getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        Log.w(TAG, "service disconnected.");
    }
}

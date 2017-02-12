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


public class MainActivity extends AppCompatActivity implements ServiceConnection {

    public final static String TAG = "ACTIVITY_RECOGNITION_P";

    private TextView mStateText;
    private EditText mLabelText;
    private Button mStartBtn;
    private Button mDoneBtn;
    private SensorDataStoringService mServcie;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

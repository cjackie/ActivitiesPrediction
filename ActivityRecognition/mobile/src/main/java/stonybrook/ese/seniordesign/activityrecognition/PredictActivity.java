package stonybrook.ese.seniordesign.activityrecognition;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

import stonybrook.ese.seniordesign.activityrecognition.sensordata.BackendComm;

public class PredictActivity extends AppCompatActivity {
    public static final String TAG = "PredictActivity";

    private enum PredictState {
        PREDICTING,
        NOT_STARTED
    }
    private PredictState predictState;

    private TextView predictionText;
    private Spinner timeFrames;
    private Button predictBtn;

    private int timeFrameSelectedI;
    private Timer timer;

    private ServiceConnection serviceConn;
    private SensorDataStoringService dataService;
    private BackendComm comm;
    private Handler uiHandler;
    private final int UI_HANDLER_UPDATE_LABEL = 0;
    private final int UI_HANDLER_ENABLE_BUTTON = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.predict_view);
        predictBtn = (Button) findViewById(R.id.predictBtn);

        predictionText = (TextView) findViewById(R.id.predictText);
        String instruction = "Select a time during, then click predict";
        predictionText.setText(instruction);

        timeFrames = (Spinner) findViewById(R.id.timeFrames);
        ArrayAdapter<CharSequence> timeFramesAdapter = ArrayAdapter.createFromResource(
                this, R.array.timeFrames, android.R.layout.simple_spinner_item
        );
        timeFrames.setAdapter(timeFramesAdapter);
        timeFrameSelectedI = 0;

        predictState = PredictState.NOT_STARTED;
        timer = new Timer();

        initComm();
        initUiHanlder();
        getDataService();
        setUpListeners();
        restoreFromPref();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedState) {
        super.onRestoreInstanceState(savedState);
        if (savedState != null) {
            savedState.putInt("timeFrameSelectedI", timeFrameSelectedI);
            savedState.putString("predictionText", predictionText.getText().toString());
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (state != null) {
            timeFrameSelectedI = state.getInt("timeFrameSelectedI", 0);
            predictionText.setText(state.getString("predictionText", "??"));

            if (predictState == PredictState.PREDICTING) {
                dataService.finishStoring();

            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        savedToPref();
        this.unbindService(serviceConn);
    }

//    @Override
//    public void onConfigurationChanged(Configuration newConfig) {
//        // with android:configChanges="orientation|screenSize" to disable rotate.
//    }

    private void initComm() {
        comm = new BackendComm();
        comm.start();
    }

    private void initUiHanlder() {
        uiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                Log.d(TAG, "received a message");
                super.handleMessage(msg);
                if (msg.what == UI_HANDLER_UPDATE_LABEL) {
                    String label = msg.getData().getString("label", "??");
                    predictionText.setText("label predicted: " + label);
                } else if (msg.what == UI_HANDLER_ENABLE_BUTTON) {
                    String uiText = msg.getData().getString("uiText", "??");
                    predictionText.setText(uiText);
                    predictBtn.setEnabled(true);
                }
            }
        };
    }

    private void setUpListeners() {
        timeFrames.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                timeFrameSelectedI = i;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                timeFrameSelectedI = 0;
            }
        });

        predictBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (dataService == null) {
                    return ;
                }

                if (predictState == PredictState.PREDICTING
                        && dataService.getState() == SensorDataStoringService.State.STORING_DATA) {
                    dataService.finishStoring();
                }

                predictBtn.setEnabled(false);

                int collectTime; // in milliseconds
                int times[] = {1000, 2000, 4000, 10000, 30000};
                collectTime = times[timeFrameSelectedI];

                dataService.startNewRecord("NA");
                predictionText.setText("Collecting data for prediction.");
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        String data = dataService.getAccelRecord();

                        Bundle uiBundle = new Bundle();
                        uiBundle.putString("uiText", "Done collecting, waiting for result");
                        Message msg = new Message();
                        msg.what = UI_HANDLER_ENABLE_BUTTON;
                        msg.setData(uiBundle);
                        msg.setTarget(uiHandler);
                        msg.sendToTarget();

                        comm.predict(data, new BackendComm.PredictionReadyCallback() {
                            @Override
                            public void callback(String label) {
                                Bundle labelBundle = new Bundle();
                                labelBundle.putString("label", label);
                                Message msg = new Message();
                                msg.what = UI_HANDLER_UPDATE_LABEL;
                                msg.setData(labelBundle);
                                msg.setTarget(uiHandler);
                                msg.sendToTarget();
                            }
                        });
                    }
                }, collectTime);

                predictState = PredictState.PREDICTING;
            }
        });
    }

    private void getDataService() {
        serviceConn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                dataService = ((SensorDataStoringService.SensorDataStoringServiceLocalBinder)iBinder).getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                Log.w(TAG, "service disconnected.");
            }
        };
        bindService(new Intent(this, SensorDataStoringService.class), serviceConn, BIND_ABOVE_CLIENT);
    }

    private void restoreFromPref() {
        SharedPreferences pref = getSharedPreferences(getClass().getName(), MODE_PRIVATE);
        timeFrameSelectedI = pref.getInt("timeFrameSelectedI", 0);
    }

    private void savedToPref() {
        SharedPreferences pref = getSharedPreferences(getClass().getName(), MODE_PRIVATE);
        pref.edit().putInt("timeFrameSelectedI", timeFrameSelectedI).commit();
    }
}

package stonybrook.ese.seniordesign.activityrecognition;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import stonybrook.ese.seniordesign.activityrecognition.sensordata.BackendComm;

public class CollectActivity extends AppCompatActivity {

    public final String TAG = "collect_activity";

    private enum CollectState {
        COLLECTING,
        NOT_STARTED,
    }

    private TextView collectTextView;
    private Spinner labelsDropdown;
    private Button collectBtn;
    private Switch enableUploadSwitch;

    private CollectState state;
    private String labelSelected;
    private boolean enableUpload;

    private SensorDataStoringService dataService;
    private ServiceConnection serviceConn;

    private BackendComm comm;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.collect_view);

        collectTextView = (TextView) findViewById(R.id.collectText);
        labelsDropdown = (Spinner) findViewById(R.id.labels);
        collectBtn = (Button) findViewById(R.id.startCollectBtn);
        enableUploadSwitch = (Switch) findViewById(R.id.enableUpload);

        collectBtn.setText("Collect");
        state = CollectState.NOT_STARTED;
        labelSelected = null;
        enableUploadSwitch.setChecked(false);
        enableUpload = false;

        // set dropdowns
        ArrayAdapter<CharSequence> labelsAdapter = ArrayAdapter.createFromResource(this,
                R.array.labels, android.R.layout.simple_spinner_item);
        labelsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        labelsDropdown.setAdapter(labelsAdapter);

        dataService = null;

        getDataService();
        setUpListners();
        restoreFromPref();
        initComm();
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

    private void setUpListners() {
        // enableUploadSwitch
        enableUploadSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                enableUpload = isChecked;
            }
        });

        labelsDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                labelSelected = (String) adapterView.getItemAtPosition(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                labelSelected = null;
            }
        });

        // set collect btn
        collectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (labelSelected == null) {
                    collectTextView.setText("Select a label before collecting.");
                    return ;
                }

                if (state == CollectState.NOT_STARTED) {
                    startCollect();
                } else {
                    stopCollect();
                }
            }
        });

    }

    private void startCollect() {
        if (dataService == null) {
            collectTextView.setText("service is not ready yet. try again later.");
            return ;
        }

        if (labelSelected == null) {
            collectTextView.setText("Select a label before collecting.");
            return ;
        }

        if (dataService.getState() == SensorDataStoringService.State.NOT_STARTED ||
                dataService.getState() == SensorDataStoringService.State.DONE_COLLECING &&
                        state == CollectState.NOT_STARTED) {
            dataService.startNewRecord(labelSelected);
            collectTextView.setText("collecting.");
            collectBtn.setText("Stop");
            state = CollectState.COLLECTING;
        } else {
            collectTextView.setText("??");
            Log.w(TAG, "started already?");
        }
    }

    private void stopCollect() {
        if (dataService == null) {
            collectTextView.setText("service is not ready yet. try again later.");
            return ;
        }

        if (dataService.getState() == SensorDataStoringService.State.STORING_DATA &&
                state == CollectState.COLLECTING) {
            dataService.finishStoring();
            collectTextView.setText("stopped collecting. saved.");
            collectBtn.setText("Collect");
            state = CollectState.NOT_STARTED;

            // decide if upload is needed
            if (enableUpload) {
                comm.send(dataService.getAccelRecord(), labelSelected);
            }
        } else {
            collectTextView.setText("???");
            Log.w(TAG, "?");
        }
    }

    private void initComm() {
        comm = new BackendComm();
        comm.start();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedState) {
        super.onRestoreInstanceState(savedState);
        if (savedState != null) {
            labelSelected = savedState.getString("labelSelected", null);
            labelsDropdown.setSelection(savedState.getInt("labelsDropdown", 0));
            enableUpload = savedState.getBoolean("enableUpload", false);
            enableUploadSwitch.setChecked(enableUpload);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("labelSelected", labelSelected);
        state.putInt("labelsDropdown", labelsDropdown.getSelectedItemPosition());
        state.putBoolean("enableUpload", enableUpload);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();

        if (dataService != null && dataService.getState() == SensorDataStoringService.State.STORING_DATA) {
            dataService.finishStoring();
        }
        this.unbindService(serviceConn);

        saveToPref();
    }

    private void restoreFromPref() {
        SharedPreferences pref = getSharedPreferences(CollectActivity.class.getName(), MODE_PRIVATE);

        // restore the state activity... It is not good...... highly breakable and hard to scale
        // in implementation complexity.
        labelSelected = pref.getString("labelSelected", null);
        labelsDropdown.setSelection(pref.getInt("labelsDropdown", 0));
        enableUpload = pref.getBoolean("enableUpload", false);
        enableUploadSwitch.setChecked(enableUpload);
    }

    private void saveToPref() {
        SharedPreferences pref = getSharedPreferences(CollectActivity.class.getName(), MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        editor.putString("labelSelected", labelSelected)
                .putInt("labelsDropdown", labelsDropdown.getSelectedItemPosition())
                .putBoolean("enableUpload", enableUpload)
                .apply();
    }


}

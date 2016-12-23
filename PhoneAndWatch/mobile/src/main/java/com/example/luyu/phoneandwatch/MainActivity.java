package com.example.luyu.phoneandwatch;

import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements
        DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;
    private TextView textX, textY, textZ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textX = (TextView)findViewById(R.id.textX);
        textY = (TextView)findViewById(R.id.textY);
        textZ = (TextView)findViewById(R.id.textZ);


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();



        Log.d("Main Activity", "onCreate");
    }

    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
        Log.d("Main Activity", "onResume");
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("Main Activity", "onConnected");
        Wearable.DataApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("Main Activity", "onConnectionSuspended");
        return;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }


    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d("Main Activity", "onDataChanged: " + dataEvents);

        for (DataEvent event : dataEvents) {

            Log.d("Main Activity", "Data event: " + DataEvent.TYPE_CHANGED+"  "+event.getType());
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                DataItem item = event.getDataItem();
                Log.d("Main Activity", "Comparisionsss: " + item.getUri().getPath());
                Log.d("Main Activity", "Comparision: " + item.getUri().getPath().compareTo("/sensor-data"));
                if (item.getUri().getPath().compareTo("/sensor-data") == 0) {

                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                   int c= dataMap.getInt("count");
                    float[] values = dataMap.getFloatArray("accelerometer");

                    Log.d("Main Activity", "receive sensor data: " + values[0]+" "+values[1]+" "+values[2]);
                    textX.setText(" "+values[0]);
                    textY.setText(" "+values[1]);
                    textZ.setText(" "+values[2]);

                    byte[] rawData = event.getDataItem().getData();
                    DataMap sensorData = DataMap.fromByteArray(rawData);
                    saveData(sensorData);

                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                Log.d("Main Activity", "Type Deleted ");
                // DataItem deleted
            }
        }
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void saveData(DataMap data) {
        if (!isExternalStorageWritable()) {
            Log.d("MainActivity", "External Storage Not Writable");
            return;
        }

        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);

       if(!directory.exists()) {
           directory.mkdirs();
           Log.d("MainActivity", "mkdirs");
       }

        File file = new File(directory, "wearable_data.txt");

        Log.d("MainActivity", "Try creating directory "+directory.getAbsolutePath()+" "+directory.canWrite());

        String dataJSON = dataMapAsJSONObject(data).toString() + "\n";
        try {
            Log.d("MainActivity", "Try writing ");
            FileOutputStream stream = new FileOutputStream(file, true);
            OutputStreamWriter writer = new OutputStreamWriter(stream);
            writer.write(dataJSON);
            writer.close();

        } catch (Exception e) {
            Log.d("MainActivity", "Error Saving");
            e.printStackTrace();
        }
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }


    private JSONObject dataMapAsJSONObject(DataMap data) {
        Bundle bundle = data.toBundle();
        JSONObject json = new JSONObject();
        Set<String> keys = bundle.keySet();
        for (String key : keys) {
            try {
                // json.put(key, bundle.get(key)); see edit below
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    json.put(key, JSONObject.wrap(bundle.get(key)));
                    Log.d("MainActivity", "JSON put");
                }
            } catch(JSONException e) {
                //Handle exception here
                Log.d("MainActivity", "JSON exception");
            }
        }
        return json;
    }






}

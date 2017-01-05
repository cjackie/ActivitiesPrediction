package com.example.luyu.activity_recognition_all.sensordata;

/**
 * Created by luyu on 1/3/2017.
 */
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.support.v4.util.LogWriter;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by chaojiewang on 12/23/16.
 */

public class SensorLocalStorage {
    static public final String TAG = "SensorLocalStorage";
    private final String STORAGE_DIR = "ActivityRecognition/";

    private final String ACCELEROMETER_FILE_NAME = "accelerometer.csv";
    private final String GYROSCOPE_FILE_NAME = "gyroscope.csv";
    private final String AMBIENTLIGHT_FILE_NAME = "ambientlight.csv";
    private final String HEARTRATE_FILE_NAME = "heartrate.csv";


    private FileOutputStream accelerometerFileStream;
    private FileOutputStream gyroscopeFileStream;
    private FileOutputStream ambientlightFileStream;
    private FileOutputStream heartrateFileStream;

    public SensorLocalStorage(Context context) {
        File sdcard = context.getExternalFilesDir(null);
        File dataDir = new File(sdcard.getAbsolutePath(), STORAGE_DIR);
        String sdcardState = null;

        sdcardState = Environment.getExternalStorageState(dataDir);

        if (!sdcardState.equals(Environment.MEDIA_MOUNTED)) {
            Log.w(TAG,"the media is not mounted and readable");
        }
        if (!dataDir.mkdirs()) {
            Log.w(TAG, "failed to create dir");
        }

        File accelerometerFile = new File(dataDir, ACCELEROMETER_FILE_NAME);
        File gyroscopeFile = new File(dataDir, GYROSCOPE_FILE_NAME);
        File ambientlightFile = new File(dataDir, AMBIENTLIGHT_FILE_NAME);
        File heartrateFile = new File(dataDir, HEARTRATE_FILE_NAME);


        try {
            if (!accelerometerFile.exists()) {
                accelerometerFile.createNewFile();
                accelerometerFileStream = new FileOutputStream(accelerometerFile);
                String header = "x,y,z,time\n";
                accelerometerFileStream.write(header.getBytes(StandardCharsets.UTF_8));

            } else {
                accelerometerFileStream = new FileOutputStream(accelerometerFile, true);
            }

        } catch (Exception error) {
            Log.e(TAG, "fail to open file for accelerometer");
            throw new RuntimeException(error.toString());
        }


        try {
            if (!gyroscopeFile.exists()) {
                gyroscopeFile.createNewFile();
                gyroscopeFileStream = new FileOutputStream(gyroscopeFile);
                String header = "x,y,z,time\n";
                gyroscopeFileStream.write(header.getBytes(StandardCharsets.UTF_8));

            } else {
                gyroscopeFileStream = new FileOutputStream(gyroscopeFile, true);
            }

        } catch (Exception error) {
            Log.e(TAG, "fail to open file for gyroscope");
            throw new RuntimeException(error.toString());
        }


        try {
            if (!ambientlightFile.exists()) {
                ambientlightFile.createNewFile();
                ambientlightFileStream = new FileOutputStream(ambientlightFile);
                String header = "x,time\n";
                ambientlightFileStream.write(header.getBytes(StandardCharsets.UTF_8));

            } else {
                ambientlightFileStream = new FileOutputStream(ambientlightFile, true);
            }

        } catch (Exception error) {
            Log.e(TAG, "fail to open file for ambient light sensor");
            throw new RuntimeException(error.toString());
        }

        try {
            if (!heartrateFile.exists()) {
                heartrateFile.createNewFile();
                heartrateFileStream = new FileOutputStream(heartrateFile);
                String header = "x,time\n";
                heartrateFileStream.write(header.getBytes(StandardCharsets.UTF_8));

            } else {
                heartrateFileStream = new FileOutputStream(heartrateFile, true);
            }

        } catch (Exception error) {
            Log.e(TAG, "fail to open file for heart rate sensor");
            throw new RuntimeException(error.toString());
        }

    }

    public void save(AccelerometerDataItem item) {
        String entry = String.format("%1$f,%2$f,%3$f,%4$f\n",
                item.getX(), item.getY(), item.getZ(), item.getTime());
        try {
            accelerometerFileStream.write(entry.getBytes(StandardCharsets.UTF_8));
        } catch (IOException error) {
            Log.w(TAG, "failed to save acceleromenter data.");
        }
    }

    public void save(GyroscopeDataItem item) {
        String entry = String.format("%1$f,%2$f,%3$f,%4$f\n",
                item.getX(), item.getY(), item.getZ(), item.getTime());
        try {
            gyroscopeFileStream.write(entry.getBytes(StandardCharsets.UTF_8));
        } catch (IOException error) {
            Log.w(TAG, "failed to save gyroscope data.");
        }
    }

    public void save(AmbientLightDataItem item) {
        String entry = String.format("%1$f,%2$f\n",
                item.getX(), item.getTime());
        try {
            ambientlightFileStream.write(entry.getBytes(StandardCharsets.UTF_8));
        } catch (IOException error) {
            Log.w(TAG, "failed to save ambient light data.");
        }
    }


    public void save(HeartRateDataItem item) {
        String entry = String.format("%1$f,%2$f\n",
                item.getX(), item.getTime());
        try {
            heartrateFileStream.write(entry.getBytes(StandardCharsets.UTF_8));
        } catch (IOException error) {
            Log.w(TAG, "failed to save heart rate data.");
        }
    }


}
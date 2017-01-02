package com.example.luyu.activity_recognition_gyroscope.sensordata;

/**
 * Created by luyu on 1/1/2017.
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
    private final String STORAGE_DIR = "activity_recognition_gyroscope/";
    private final String GYROSCOPE_FILE_NAME = "gyroscope.csv";

    private FileOutputStream gyroscopeFileStream;

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

        File gyroscopeFile = new File(dataDir, GYROSCOPE_FILE_NAME);

        try {
            if (!gyroscopeFile.exists()) {
                gyroscopeFile.createNewFile();
                gyroscopeFileStream = new FileOutputStream(gyroscopeFile);
                String header = "x,y,z,time\n";
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    gyroscopeFileStream.write(header.getBytes(StandardCharsets.UTF_8));
                }
            } else {
                gyroscopeFileStream = new FileOutputStream(gyroscopeFile, true);
            }

        } catch (Exception error) {
            Log.e(TAG, "fail to open file");
            throw new RuntimeException(error.toString());
        }
    }

    public void save(GyroscopeDataItem item) {
        String entry = String.format("%1$f,%2$f,%3$f,%4$f\n",
                item.getX(), item.getY(), item.getZ(), item.getTime());
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                gyroscopeFileStream.write(entry.getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException error) {
            Log.w(TAG, "failed to save gyroscope data.");
        }
    }
}

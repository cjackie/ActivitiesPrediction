package stonybrook.ese.seniordesign.activityrecognition.sensordata;

import android.content.Context;
import android.content.SharedPreferences;
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
    private final String ACCELEROMETER_FILE_NAME_PREFIX = "accelerometer";
    private final String GYROSCOPE_FILE_NAME_PREFIX = "gyroscope";
    private final String LABELS_FILE_NAME = "labels.txt";
    private final String PREF_NAME = "SENSOR_LOCAL_STORAGE";
    private final String PREF_COUNTER_NAME = "counter";

    private SharedPreferences pref;
    private File fileDir;
    private FileOutputStream labelsFileStream;
    private int fileCounter;
    private FileOutputStream currentAccelerometerFileStream;
    private FileOutputStream currentGyroscopeFileStream;

    public SensorLocalStorage(Context context) {
        File sdcard = context.getExternalFilesDir(null);
        fileDir = new File(sdcard.getAbsolutePath(), STORAGE_DIR);
        String sdcardState = Environment.getExternalStorageState(fileDir);
        if (!sdcardState.equals(Environment.MEDIA_MOUNTED)) {
            Log.w(TAG,"the media is not mounted and readable");
            throw new RuntimeException("the media is not mounted and readable");
        }
        if (!fileDir.mkdirs()) {
            Log.w(TAG, "failed to create dir");
        }

        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        fileCounter = pref.getInt(PREF_COUNTER_NAME, 0);;
        // create label file
        File f = new File(fileDir, LABELS_FILE_NAME);
        try {
            if (!f.exists()) {
                f.createNewFile();
            }
            labelsFileStream = new FileOutputStream(f, true);
        } catch (IOException e) {
            throw new RuntimeException("??");
        }

        currentAccelerometerFileStream = null;
        currentGyroscopeFileStream = null;
    }

    // @label. one line, with no new line("/n") at the end.
    public void startSaving(String label) {

        String accelFilename = ACCELEROMETER_FILE_NAME_PREFIX + "_" + String.valueOf(fileCounter) + ".csv";
        String gyroFilename = GYROSCOPE_FILE_NAME_PREFIX + "_" + String.valueOf(fileCounter) + ".csv";

        File accelFile = new File(fileDir, accelFilename);
        File gyroFile = new File(fileDir, gyroFilename);
        try {
            if (currentAccelerometerFileStream != null) {
                // gracefully close it.
                // "currentAccelerometerFileStream is null" implies other streams are also null
                finishSaving();
            }

            accelFile.createNewFile();
            gyroFile.createNewFile();
            currentAccelerometerFileStream = new FileOutputStream(accelFile);
            currentGyroscopeFileStream = new FileOutputStream(gyroFile);

            String header = "x,y,z,time\n";
            currentAccelerometerFileStream.write(header.getBytes(StandardCharsets.UTF_8));
            currentGyroscopeFileStream.write(header.getBytes(StandardCharsets.UTF_8));

            // writing labels.
            String fileToLabel = "\n" + String.valueOf(fileCounter) + " => " + label;
            labelsFileStream.write(fileToLabel.getBytes(StandardCharsets.UTF_8));

            fileCounter++;
        } catch (Exception error) {
            Log.e(TAG, "fail1");
            throw new RuntimeException(error.toString());
        }

    }

    public void finishSaving() {
        if (currentAccelerometerFileStream == null) {
            // increase robustness.
            return ;
        }

        try {
            currentAccelerometerFileStream.close();
            currentGyroscopeFileStream.close();
            currentAccelerometerFileStream = null;
            currentGyroscopeFileStream = null;
            pref.edit().putInt(PREF_COUNTER_NAME, fileCounter).apply();
        } catch (Exception error) {
            Log.e(TAG, "fail2");
            throw new RuntimeException(error.toString());
        }
    }

    // call this after start saving... it will return false;
    public boolean save(AccelerometerDataItem item) {
        if (currentAccelerometerFileStream == null) {
            return false;
        }

        String entry = String.format("%1$f,%2$f,%3$f,%4$f\n",
                item.getTime(), item.getX(), item.getY(), item.getZ());
        try {
            currentAccelerometerFileStream.write(entry.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (IOException error) {
            Log.w(TAG, "failed to save acceleromenter data.");
            return false;
        }
    }

    public boolean save(GyroscopeDataItem item) {
        if (currentGyroscopeFileStream == null) {
            return false;
        }

        String entry = String.format("%1$f,%2$f,%3$f,%4$f\n",
                item.getTime(), item.getX(), item.getY(), item.getZ());
        try {
            currentGyroscopeFileStream.write(entry.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (IOException error) {
            Log.w(TAG, "failed to save gyroscope data.");
            return false;
        }
    }

    public void flushAll(){
        try {
            if (null != currentAccelerometerFileStream) {
                currentAccelerometerFileStream.flush();
                currentGyroscopeFileStream.flush();
            }
            labelsFileStream.flush();
        } catch (IOException error) {
            Log.w(TAG, "failed to save flush data.");
            throw new RuntimeException("failed to save flush data.");
        }
    }
}

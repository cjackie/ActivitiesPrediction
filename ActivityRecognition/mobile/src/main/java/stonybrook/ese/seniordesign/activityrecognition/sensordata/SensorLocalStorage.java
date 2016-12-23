package stonybrook.ese.seniordesign.activityrecognition.sensordata;

import android.content.Context;
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

    private FileOutputStream accelerometerFileStream;

    public SensorLocalStorage(Context context) {
        File sdcard = context.getExternalFilesDir(null);
        File dataDir = new File(sdcard.getAbsolutePath(), STORAGE_DIR);
        String sdcardState = Environment.getExternalStorageState(dataDir);
        if (!sdcardState.equals(Environment.MEDIA_MOUNTED)) {
            Log.w(TAG,"the media is not mounted and readable");
        }
        if (!dataDir.mkdirs()) {
            Log.w(TAG, "failed to create dir");
        }

        File accelerometerFile = new File(dataDir, ACCELEROMETER_FILE_NAME);

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
            Log.e(TAG, "fail to open file");
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
}

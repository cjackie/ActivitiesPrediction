package stonybrook.ese.seniordesign.activityrecognition.sensordata;


import android.util.ArrayMap;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import stonybrook.ese.seniordesign.activityrecognition.sensordata.SensorDataItem;

public class AccelerometerDataItem implements SensorDataItem {

    final public static String PATH = "/activity_recognition/accelerometer";

    private double x;
    private double y;
    private double z;
    private double time;


    public AccelerometerDataItem() {
        x = 0;
        y = 0;
        z = 0;
        time = 0;
    }

    public AccelerometerDataItem(double x, double y, double z, double time) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.time = time;
    }


    @Override
    public byte[] getData() {
        // encode information.
        // format:
        // x1,y1,z1,time1
        return String.format("%1$f,%2$f,%3$f,%4$f", x, y, z, time).getBytes();
    }

    public AccelerometerDataItem setData(byte[] bytes) {
        String encodedDataStr = new String(bytes, Charset.defaultCharset());
        String tokens[] = encodedDataStr.split(",");
        this.x = Double.parseDouble(tokens[0]);
        this.y = Double.parseDouble(tokens[1]);
        this.z = Double.parseDouble(tokens[2]);
        this.time = Double.parseDouble(tokens[3]);

        return this;
    }



    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }

}
package com.example.luyu.activity_recognition_all.sensordata;

import java.nio.charset.Charset;
/**
 * Created by luyu on 1/4/2017.
 */
public class AmbientLightDataItem implements SensorDataItem{

    final public static String PATH = "/activity_recognition/ambient_light";

    private double x;
    private double time;

    public AmbientLightDataItem(){

        x = 0;
        time = 0;
    }

    public AmbientLightDataItem (double x, double time) {
        this.x = x;
        this.time = time;
    }

    @Override
    public byte[] getData() {
        // encode information.
        // format:
        // x1,time1
        return String.format("%1$f,%2$f", x,  time).getBytes();
    }

    public AmbientLightDataItem setData(byte[] bytes) {
        String encodedDataStr = new String(bytes, Charset.defaultCharset());
        String tokens[] = encodedDataStr.split(",");
        this.x = Double.parseDouble(tokens[0]);
        this.time = Double.parseDouble(tokens[1]);

        return this;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }

}

package com.example.luyu.phoneandwatch.SensorData;

import android.net.Uri;

import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemAsset;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

class AccelerometerDataItem implements DataItem {

    private class AccelerometerSingleData {
        public double x;
        public double y;
        public double z;
        public double time;
        public AccelerometerSingleData(double x, double y, double z, double time) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.time = time;
        }
    }

    private boolean freezed;
    private List<AccelerometerSingleData> data;
    private int sizeCap;
    private String label;

    static public boolean isThisType(DataItem item) {
        Uri uri = item.getUri();
        if (uri.getAuthority() == "activity_prediction" &&
                uri.getQueryParameter("type") != null &&
                uri.getQueryParameter("type") == AccelerometerDataItem.class.getName()) {
            return true;
        } else {
            return false;
        }
    }

    public AccelerometerDataItem(int size, String label) {
        sizeCap = size;
        List<AccelerometerSingleData> data = new ArrayList<>();
        freezed = false;
        this.label = label;
    }

    public AccelerometerDataItem addData(double x, double y, double z, double time) {
        if (freezed) {
            throw new RuntimeException("can't modify freezed data.");
        }

        if (isFull()) {
            throw new RuntimeException("full already");
        }

        AccelerometerSingleData d = new AccelerometerSingleData(x, y, z, time);
        data.add(d);
        return this;
    }

    public boolean isFull() {
        if (data.size() > sizeCap) {
            throw new RuntimeException("overflow of data.");
        }

        if (data.size() ==  sizeCap) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Uri getUri() {
        new Uri.Builder().scheme("wear").authority("activity_prediction")
                .appendQueryParameter("type", AccelerometerDataItem.class.getName());
        return null;
    }

    @Override
    public byte[] getData() {
        if (freezed) {
            throw new RuntimeException("mutate freezed data.");
        }

        if (!isFull()) {
            throw new RuntimeException("the data is not mature");
        }

        // encode information.
        // format:
        // label
        // x1,y1,z1,time1
        // x2,y2,z2,time2
        // .
        // .
        // xn,yn,zn,timen

        StringBuilder encodedData = new StringBuilder();
        for (AccelerometerSingleData d : data) {
            String line = String.format("%1$,%2$,%3$,%4$\n", d.x, d.y, d.z, d.time);
            encodedData.append(line);
        }
        return encodedData.toString().getBytes(Charset.defaultCharset());
    }

    /*
    assume @bytes is a thing return from @this.getData()
     */
    @Override
    public DataItem setData(byte[] bytes) {
        if (freezed) {
            throw new RuntimeException("mutate freezed data.");
        }

        this.data.clear();

        String encodedDataStr = new String(bytes, Charset.defaultCharset());
        String lines[] = encodedDataStr.split("\n");
        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            if (lineIndex == 0) {
                this.label = lines[lineIndex];
            } else {
                String tokens[] = lines[lineIndex].split(",");
                double x = Double.parseDouble(tokens[0]);
                double y = Double.parseDouble(tokens[1]);
                double z = Double.parseDouble(tokens[2]);
                double time = Double.parseDouble(tokens[3]);
                this.data.add(new AccelerometerSingleData(x,y,z,time));
            }
        }

        return this;
    }

    @Override
    public Map<String, DataItemAsset> getAssets() {
        return null;
    }

    @Override
    public DataItem freeze() {
        freezed = true;
        return this;
    }

    @Override
    public boolean isDataValid() {
        return isFull();
    }
}
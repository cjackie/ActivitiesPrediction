package stonybrook.ese.seniordesign.activityrecognition.sensordata;

/**
 * Created by chaojiewang on 12/23/16.
 */
public interface SensorDataItem {
    byte[] getData();
    SensorDataItem setData(byte[] data);
}

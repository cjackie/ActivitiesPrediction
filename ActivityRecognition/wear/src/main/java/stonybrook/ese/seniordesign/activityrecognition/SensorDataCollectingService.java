package stonybrook.ese.seniordesign.activityrecognition;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.WearableListenerService;

public class SensorDataCollectingService extends WearableListenerService {


    public SensorDataCollectingService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        super.onDataChanged(dataEventBuffer);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
    }

    @Override
    public void onPeerConnected(Node node) {
        super.onPeerConnected(node);
    }

    @Override
    public void onPeerDisconnected(Node node) {
        super.onPeerDisconnected(node);
    }
}

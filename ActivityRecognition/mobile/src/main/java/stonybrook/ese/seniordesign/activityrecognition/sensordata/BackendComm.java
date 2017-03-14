package stonybrook.ese.seniordesign.activityrecognition.sensordata;

import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by chaojiewang on 3/2/17.
 */

public class BackendComm extends HandlerThread {
    public static final String SERVER_ADDR = "192.168.0.106"; // TODO
    public static final int SERVER_PORT = 8000;     // TODO

    public interface PredictionReadyCallback {
        void callback(String label);
    }

    private Handler handler;

    public final String TAG = "BackendComm";
    private ArrayDeque<byte[]> accelDataLabeled;
    // global variable for @predict method.. not good....
    private String accelDataForPrediction;
    PredictionReadyCallback predictionCallback;
    private String predictedLabel;

    public BackendComm() {
        super("BackendComm");
        accelDataLabeled = new ArrayDeque<>();
    }

    public void send(String accelData, String label) {
        String headerStr = String.format("SEND %d %s \n", accelData.length(), label);
        String data = headerStr + accelData;
        sendBytes(data.getBytes(StandardCharsets.US_ASCII));
    }

    @Override
    protected void onLooperPrepared() {
        handler = new Handler(this.getLooper()) {};
    }

    // TODO
    // @return, null on result not available.
    synchronized public void predict(String accelData, PredictionReadyCallback cb) {
        accelDataForPrediction = accelData;
        predictionCallback = cb;
        handler.post(new Runnable() {
            @Override
            public void run() {
                // TODO
                String label = "NA";

                String data = String.format("PREDICT %d \n", accelDataForPrediction.length());
                data = data + accelDataForPrediction;
                byte[] formattedData = data.getBytes(StandardCharsets.US_ASCII);
                Socket sock = null;
                try {
                    // Connect
                    InetAddress inet = InetAddress.getByName(SERVER_ADDR);
                    sock = new Socket(inet, SERVER_PORT);

                    // get stream
                    OutputStream outputStream = sock.getOutputStream();
                    InputStream inputStream = sock.getInputStream();

                    // Send
                    outputStream.write(formattedData);

                    // Read
                    byte[] inBuffer = new byte[1024];
                    int byteLength = inputStream.read(inBuffer);
                    String inData = new String(inBuffer, 0, byteLength, StandardCharsets.US_ASCII);
                    Pattern labelPattern = Pattern.compile("(LABEL) (.+)");
                    Matcher matcher = labelPattern.matcher(inData);
                    if (matcher.matches()) {
                        if (matcher.groupCount() >= 2) {
                            predictionCallback.callback(matcher.group(2));
                        } else {
                            Log.w(TAG, "unexpected group counts???");
                            predictionCallback.callback("NA");
                        }
                    } else {
                        Log.w(TAG, "does not recognize the prediction result from the server: " + inData);
                        predictionCallback.callback("NA");
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (sock != null) {
                        try {
                            sock.close();
                        } catch (IOException e) {
                            e.printStackTrace();    // Auto-generated.
                        }
                    }
                }
            }
        });
    }

    private void sendBytes(byte[] data) {
        synchronized (accelDataLabeled) {
            accelDataLabeled.addLast(data);
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                byte[] bytes = null;
                synchronized (accelDataLabeled) {
                    if (accelDataLabeled.isEmpty()) {
                        Log.e(TAG, "run: ???");
                        return ;
                    }
                    bytes = accelDataLabeled.pollFirst();
                }

                long result = 0;
                byte[] input = new byte[1024];
                Socket sock = null;
                try {
                    // Connect
                    InetAddress inet = InetAddress.getByName(SERVER_ADDR);
                    sock = new Socket(inet, SERVER_PORT);

                    // get stream
                    OutputStream outputStream = sock.getOutputStream();

                    // Send
                    outputStream.write(bytes);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (sock != null) {
                        try {
                            sock.close();
                        } catch (IOException e) {
                            e.printStackTrace();    // Auto-generated.
                        }
                    }
                }
            }
        });
    }

}

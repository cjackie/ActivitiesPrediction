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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.FutureTask;

/**
 * Created by chaojiewang on 3/2/17.
 */

public class BackendComm extends HandlerThread {
    private Handler handler;

    public final String TAG = "BackendComm";
    private String hostName;
    private int port;
    // global variable for @predict method.. not good....
    private String accelDataForPrediction;
    private ArrayDeque<byte[]> accelDataLabeled;

    public BackendComm(String hostName, int port) {
        super("BackendComm");
        this.hostName = hostName;
        this.port = port;
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
    synchronized public String predict(String accelData) {
        accelDataForPrediction = accelData;
        Future<String> label = new FutureTask<String>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                byte[] bytes = accelDataForPrediction.getBytes();
                long result = 0;
                byte[] input = new byte[1024];
                Socket sock = null;
                try {
                    // Connect
                    InetAddress inet = InetAddress.getByName(hostName);
                    sock = new Socket(inet, port);

                    // get stream
                    OutputStream outputStream = sock.getOutputStream();
                    InputStream inputStream = sock.getInputStream();

                    // Send
                    outputStream.write(bytes);

                    // wait for return
                    inputStream.read(input);


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
                return null;
            }
        });

        try {
            return label.get(5, TimeUnit.SECONDS);
        } catch (Exception err) {
            return null;
        }
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
                    InetAddress inet = InetAddress.getByName(hostName);
                    sock = new Socket(inet, port);

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

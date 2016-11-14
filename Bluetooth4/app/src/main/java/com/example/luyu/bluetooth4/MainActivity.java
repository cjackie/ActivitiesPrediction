package com.example.luyu.bluetooth4;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import android.widget.EditText;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private ConnectedThread mConnectedThread;
    private BluetoothDevice mDevice;
    private ConnectThread mConnectThread;

    private static final String TAG = "BluetoothChatService";
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*Setting Up Bluetooth
        Get the BluetoothAdapter
        The BluetoothAdapter is required for any and all Bluetooth activity. To get the BluetoothAdapter,
        call the static getDefaultAdapter() method. This returns a BluetoothAdapter that represents
        the device's own Bluetooth adapter (the Bluetooth radio). There's one
        Bluetooth adapter for the entire system, and your application can interact with
        it using this object. If getDefaultAdapter() returns null, then the device does not
        support Bluetooth and your story ends here.*/

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


       /*Enable Bluetooth
         Next, you need to ensure that Bluetooth is enabled. Call isEnabled() to
         check whether Bluetooth is currently enable. If this method returns false,
         then Bluetooth is disabled. To request that Bluetooth be enabled,
         call startActivityForResult() with the ACTION_REQUEST_ENABLE action Intent.
         This will issue a request to enable Bluetooth through the system settings (
         without stopping your application). */

        if (mBluetoothAdapter == null) {
        // Device does not support Bluetooth
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        /*Querying paired devices
          Before performing device discovery, its worth querying the set of paired
          devices to see if the desired device is already known. To do so,
          call getBondedDevices(). This will return a Set of BluetoothDevices
          representing paired devices */

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                mDevice = device;
            }
        }


        /*  To use this ConnectThread, add the code
        mConnectThread = new ConnectThread(mDevice);
        mConnectThread.start();

        The code will now connect ANOTHER Android’s Bluetooth module with the Android.*/
        mConnectThread = new ConnectThread(mDevice);
        mConnectThread.start();

    }


    /*Connecting as a client
   1. Using the BluetoothDevice, get a BluetoothSocket by calling createRfcommSocketToServiceRecord(UUID).
    This initializes a BluetoothSocket that will connect to the BluetoothDevice. The UUID passed
     here must match the UUID used by the server device when it opened its BluetoothServerSocket
     (with listenUsingRfcommWithServiceRecord(String, UUID)). Using the same UUID is simply a matter of
     hard-coding the UUID string into your application and then referencing it from both the server and client code.

   2. Initiate the connection by calling connect().
    Upon this call, the system will perform an SDP lookup on the remote device in order to match the UUID.
    If the lookup is successful and the remote device accepts the connection, it will share the RFCOMM channel to
    use during the connection and connect() will return. This method is a blocking call. If, for any reason, the connection
    fails or the connect() method times out (after about 12 seconds), then it will throw an exception.
    Because connect() is a blocking call, this connection procedure should always be performed in a thread separate
    from the main activity thread.
     */

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
        boolean fail = false;

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mmDevice = device;
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) { }
            mmSocket = tmp;
        }
        public void run() {
            mBluetoothAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
                Log.d("111", "has connected");
            } catch (IOException connectException) {
                try {
                    fail = true;
                    mmSocket.close();
                    Log.d("222", "not connected");

                } catch (IOException closeException) { }
                return;
            }

            if(fail == false) {

                mConnectedThread = new ConnectedThread(mmSocket);
                mConnectedThread.start();
                Log.d("rogerlin", "thread started");


                /*You can decide to send anything to the server here inside the write method
                mConnectedThread.write("XXXX".getBytes());
                 */
                mConnectedThread.write("rogerlin".getBytes());
                Log.d("linhuang", "has written");


            }
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    /*Managing a Connection
    When you have successfully connected two (or more) devices, each one will have a connected BluetoothSocket. This is where the fun begins because you can share data between devices. Using the BluetoothSocket, the general procedure to transfer arbitrary data is simple:

    1.Get the InputStream and OutputStream that handle transmissions through the socket, via
    getInputStream() and getOutputStream(), respectively.
    2.Read and write data to the streams with read(byte[]) and write(byte[]).

     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        public void run() {
            byte[] buffer = new byte[1024];
            int begin = 0;
            int bytes = 0;

            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI Activity

                    mHandler.obtainMessage(1, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);

                    break;
                }
            }
        }
        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }


  /* NOT USED IN THIS CASE*/
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            byte[] writeBuf = (byte[]) msg.obj;
            int begin = (int)msg.arg1;
            int end = (int)msg.arg2;

            switch(msg.what) {
                case 1:
                    break;

            }
        }
    };





}

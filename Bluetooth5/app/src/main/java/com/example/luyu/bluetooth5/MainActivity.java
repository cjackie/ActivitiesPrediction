package com.example.luyu.bluetooth5;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import android.widget.TextView;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private ConnectedThread mConnectedThread;
    private BluetoothDevice mDevice;
    private AcceptThread mAcceptThread;
    private TextView mBluetoothStatus;

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
        mBluetoothStatus = (TextView)findViewById(R.id.bluetoothStatus);


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
        mAcceptThread = new AcceptThread();
        mAcceptThread.start();

        The android device will now accept ANOTHER Android device's connection*/

        mAcceptThread = new AcceptThread();
        mAcceptThread.start();

    }

    /*Here's the basic procedure to set up a server socket and accept a connection:

    1. Get a BluetoothServerSocket by calling the listenUsingRfcommWithServiceRecord(String, UUID).
    The string is an identifiable name of your service, which the system will automatically write to a new Service Discovery Protocol (SDP)
    database entry on the device (the name is arbitrary and can simply be your application name). The UUID is also included in the SDP entry
    and will be the basis for the connection agreement with the client device. That is, when the client attempts to connect with this device,
    it will carry a UUID that uniquely identifies the service with which it wants to connect. These UUIDs must match in order for the connection to be accepted (in the next step).

    2. Start listening for connection requests by calling accept().
    This is a blocking call. It will return when either a connection has been accepted or
    an exception has occurred. A connection is accepted only when a remote device has sent a
    connection request with a UUID matching the one registered with this listening server socket.
     When successful, accept() will return a connected BluetoothSocket.

    3.Unless you want to accept additional connections, call close().
     This releases the server socket and all its resources, but does not close the connected
     BluetoothSocket that's been returned by accept(). Unlike TCP/IP, RFCOMM only allows one
     connected client per channel at a time, so in most cases it makes sense to call close() on
     the BluetoothServerSocket immediately after accepting a connected socket.

     The accept() call should not be executed in the main activity UI thread because it is a blocking
     call and will prevent any other interaction with the application. It usually makes sense to do
     all work with a BluetoothServerSocket or BluetoothSocket in a new thread managed by your application.
     To abort a blocked call such as accept(), call close() on the BluetoothServerSocket (or BluetoothSocket)
     from another thread and the blocked call will immediately return. Note that all methods on a BluetoothServerSocket or BluetoothSocket are thread-safe.
    */

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;
        private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
        String NAME ="bluetoothcom";
        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) { }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    socket = mmServerSocket.accept();

                } catch (IOException e) {

                    break;
                }
                // If a connection was accepted
                if (socket != null) {


                    mConnectedThread = new ConnectedThread(socket);
                    mConnectedThread.start();
                    Log.d("222", "thread start");
                    // / Do work to manage the connection (in a separate thread)

                    break;
                }
            }
        }

        /** Will cancel the listening socket, and cause the thread to finish */
        public void cancel() {
            try {
                mmServerSocket.close();
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
                    Log.d("333", "read bytes");
                    // Send the obtained bytes to the UI activity
                    mHandler.obtainMessage(1, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    /* The handler will receive message from ConnectedThread and then write the
     message on the screen
     */
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            byte[] writeBuf = (byte[]) msg.obj;
            int begin = (int)msg.arg1;
            int end = (int)msg.arg2;

            switch(msg.what) {
                case 1:
                    Log.d("44444", "read bytes in the handler");
                    String writeMessage = new String(writeBuf);
                    mBluetoothStatus.setText(writeMessage);
                    break;

            }
        }
    };





}


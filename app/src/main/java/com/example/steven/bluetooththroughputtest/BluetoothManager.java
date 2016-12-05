package com.example.steven.bluetooththroughputtest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.Set;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/*
 * Created by Steven on 12/1/2016.
 */

class BluetoothManager {

    //CONSTANT STRINGS
    private static final UUID MY_UUID = new UUID(72000001, 9);
    private static final String APP_BT_NAME = "BluetoothThroughputTest";

    private BluetoothAdapter mBtAdapter;
    private Set<BluetoothDevice> mBtDevices;
    private boolean mIsServer = true;
    private Handler mHandler;

    BluetoothManager(BluetoothAdapter pBtAdapter, Handler pHandler) {
        mHandler = pHandler;

        //Save bluetooth adapter as class member
        mBtAdapter = pBtAdapter;

        QueryPairedDevices();
    }

    void SetMasterOrSlave(boolean pIsMaster) {
        mIsServer = pIsMaster;
    }

    Set<BluetoothDevice> GetPairedDevices() {
        if(mBtDevices.size() == 0) {
            QueryPairedDevices();
        }
        return mBtDevices;
    }

    void EstablishConnection() {

        if(mIsServer) {
            AcceptThread lAcceptThread = new AcceptThread();
            lAcceptThread.run();
        }
        else {

            Log.d("BluetoothThroughputTest", "Client Mode");

            for (BluetoothDevice device : mBtDevices) {
                ConnectThread lConnectThread = new ConnectThread(device);
                lConnectThread.run();
            }

        }

    }

    public void manageConnectedSocket(BluetoothSocket socket, BluetoothDevice
            device) {

        //ConnectedThread lConnectedThread = new ConnectedThread(socket);
        //lConnectedThread.start();
        //TODO: manage connection


    }

    //PRIVATE:
    private void QueryPairedDevices() {

        //Get set (no repeats, max 1 null value) of paired devices
        mBtDevices = mBtAdapter.getBondedDevices();

        //Should only work with paired devices, throw error if there are none
        if(mBtDevices.size() <= 0) {
            System.err.println("Please pair at least one device.");
        }
    }

    //From developer.android.com Bluetooth Guide
    private class AcceptThread extends Thread {

        private final BluetoothServerSocket mServerSocket;

        //Create thread to establish server
        AcceptThread() {
            BluetoothServerSocket lSocket = null;

            try {
                lSocket = mBtAdapter.listenUsingRfcommWithServiceRecord(APP_BT_NAME, MY_UUID);
                Log.d("BluetoothThroughputTest", "Trying to create socket.");

            } catch (IOException e) {
                Log.e(APP_BT_NAME, "Socket creation failed.", e);
            }

            mServerSocket = lSocket;
        }

        public void run() {

            BluetoothSocket socket;

            Log.d("BluetoothThroughputTest", "Trying to establish connections.");

            //Wait for incoming connection request
            while(true) {
                try {
                    socket = mServerSocket.accept();
                }
                catch (IOException e) {
                    e.printStackTrace();
                    break;
                }

                //Check if connection has been established
                if(socket != null) {

                    Log.d("BluetoothThroughputTest", socket.getRemoteDevice().getName());

                    try {
                        mServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    break;
                }
            }
        }

        public void cancel() {

            try {
                mServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    private class ConnectThread extends Thread {

        private final BluetoothSocket mSocket;
        private final BluetoothDevice mDevice;

        ConnectThread(BluetoothDevice pServerDevice) {

            //Server device should come from list of paired devices. See https://developer.android.com/guide/topics/connectivity/bluetooth.html#ConnectingDevices for help

            BluetoothSocket tmp = null;
            mDevice = pServerDevice;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = mDevice.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) { }
            mSocket = tmp;
        }

        public void run() {

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mSocket.close();
                } catch (IOException closeException) { }
                return;
            }


            // Do work to manage the connection (in a separate thread)
            //TODO: Create process to manage the connection
            manageConnectedSocket(mSocket, mDevice);
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) { }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    //TODO: Figure out how to pass information to main activity. Define handler and Main Activiity might need to implement Handler.handleMessage()
                    //http://stackoverflow.com/questions/24837548/how-to-write-a-correct-handler-with-obtainmessage
                    //In this case, 1 is an arbitrary value taht indicates a message was successfully read
                    //Also this could be useful too: https://github.com/googlesamples/android-BluetoothChat/blob/master/Application/src/main/java/com/example/android/bluetoothchat/BluetoothChatService.java
                    mHandler.obtainMessage(1, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
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

}

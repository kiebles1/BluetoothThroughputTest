package com.example.steven.bluetooththroughputtest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.util.TimingLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    private int mClientNumber = 1;
    private Handler mHandler;
    private long mDataGatherStartTime;
    private long mDataGatherEndTime;
    private List<byte[]> mMasterData = new ArrayList<byte[]>();
    private TimingLogger mTimer;

    BluetoothManager(BluetoothAdapter pBtAdapter, Handler pHandler) {
        mHandler = pHandler;

        //Save bluetooth adapter as class member
        mBtAdapter = pBtAdapter;

        QueryPairedDevices();
    }

    void SetMasterOrSlave(boolean pIsMaster) {
        mIsServer = pIsMaster;
    }

    void SetClientNumber(int pClientNumber) {
        mClientNumber = pClientNumber;
    }

    Set<BluetoothDevice> GetPairedDevices() {
        if(mBtDevices.size() == 0) {
            QueryPairedDevices();
        }
        return mBtDevices;
    }

    void AssembleData(byte[] data) {
        //put together data into a centralized structure, then retransmit
        mMasterData.add(data);

        if (mMasterData.size() == 3) {
            mDataGatherEndTime = SystemClock.elapsedRealtime();

            Bundle lBundle = new Bundle();
            lBundle.putLong("Start Time", mDataGatherStartTime);
            lBundle.putLong("End Time", mDataGatherEndTime);

            int messageType = 3;

            Message msg = mHandler.obtainMessage(messageType);
            msg.setData(lBundle);
            msg.sendToTarget();
        }
    }

    void EstablishConnection() {

        if(mIsServer) {

            AcceptThread lAcceptThread = new AcceptThread();
            lAcceptThread.start();
        }
        else {

            Log.d("BluetoothThroughputTest", "Client Mode");

            for (BluetoothDevice device : mBtDevices) {
                String deviceName = device.getName();
                Log.d("BluetoothThroughputTest", "Attempting to connect with paired devices");
                if (deviceName.compareTo("Nexus 6P") == 0 || deviceName.compareTo("MotoG3") == 0 || deviceName.compareTo("Muad'Dib") == 0) {
                    Log.d("BluetoothThroughputTest", "Connected to " + deviceName);
                    ConnectThread lConnectThread = new ConnectThread(device);
                    lConnectThread.start();
                }
            }

        }

    }

    private byte[] createData() {
        byte[] data = new byte[4];
        if(mIsServer) {
            for(int i = 0; i < 4; i++) {
                data[i] = ((byte)(i*3));
            }
            //If it's the server, send this to it's own internal copy of data.
            Bundle lBundle = new Bundle();
            lBundle.putByteArray("Device Name", (Arrays.copyOfRange(data, 0, data.length)));

            int messageType = 2;
            Message msg = mHandler.obtainMessage(messageType);
            msg.setData(lBundle);
            msg.sendToTarget();
        }
        else {
            for(int i = 0; i < 4; i++) {
                data[i] = ((byte)(i*3+mClientNumber));
            }
        }
        return data;
    }

    //PRIVATE:
    private void manageConnectedSockets(BluetoothSocket[] sockets) {

        Log.d("Thread ID", String.valueOf(android.os.Process.getThreadPriority(android.os.Process.myTid())));

        mDataGatherStartTime = SystemClock.elapsedRealtime();

        byte[] writeData = createData();
        byte[] blankData = new byte[4];

            // 0 is read, 1 is write
            for(BluetoothSocket socket : sockets) {
                ConnectedThread lConnectedThread = new ConnectedThread(socket, 0, blankData);
                lConnectedThread.start();
                ConnectedThread lConnectedThreadWrite = new ConnectedThread(socket, 1, writeData);
                lConnectedThreadWrite.start();
            }
    }

    private void manageConnectedSockets(BluetoothSocket socket) {
        byte[] writeData = createData();
        byte[] blankData = new byte[4];

        // 0 is read, 1 is write
        ConnectedThread lConnectedThread = new ConnectedThread(socket, 0, blankData);
        lConnectedThread.start();
        ConnectedThread lConnectedThreadWrite = new ConnectedThread(socket, 1, writeData);
        lConnectedThreadWrite.start();
    }

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

            BluetoothSocket[] sockets = new BluetoothSocket[2];
            int socketIndex = 0;

            //Wait for incoming connection request
            while(true) {
                try {
                    sockets[socketIndex] = mServerSocket.accept();
                }
                catch (IOException e) {
                    e.printStackTrace();
                    break;
                }

                //Check if connection has been established
                if(sockets[socketIndex] != null) {

                    Log.d("BluetoothThroughputTest", sockets[socketIndex].getRemoteDevice().getName());

                    socketIndex++;

                    if(socketIndex > 1) {

                        manageConnectedSockets(sockets);

                        try {
                            mServerSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        break;
                    }
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
            } catch (IOException e) {
                e.printStackTrace();
            }
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
                } catch (IOException closeException) {
                    closeException.printStackTrace();
                }
                return;
            }

            manageConnectedSockets(mSocket);
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final int mType; //0 means read, 1 means write
        private byte[] mData;

         ConnectedThread(BluetoothSocket socket, int type, byte[] pData) {
            mmSocket = socket;
            mType = type;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
             mData = pData;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            if (mType == 0) {
                byte[] buffer = new byte[1024];  // buffer store for the stream
                int bufferLength;

                // Keep listening to the InputStream until an exception occurs
                while (true) {
                    try {
                        // Read from the InputStream
                        bufferLength = mmInStream.read(buffer);

                        Bundle lBundle = new Bundle();
                        lBundle.putByteArray("Device Name", (Arrays.copyOfRange(buffer, 0, bufferLength-1)));

                        int messageType = 1;
                        if (mIsServer) {
                            messageType = 2;
                        }
                        //if master and if read, we need to assemble all the data and retransmit from the server
                        //Passing a 2 tells the main thread to send the data back to master.
                        Message msg = mHandler.obtainMessage(messageType);
                        msg.setData(lBundle);
                        msg.sendToTarget();
                    } catch (IOException e) {
                        break;
                    }
                }


            } else {
                write(mData);
            }
        }

        /* Call this from the main activity to send data to the remote device */
        void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}

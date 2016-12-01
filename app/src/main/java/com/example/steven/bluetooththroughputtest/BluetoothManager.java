package com.example.steven.bluetooththroughputtest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;

import java.util.Set;

/**
 * Created by Steven on 12/1/2016.
 */

public class BluetoothManager {

    private BluetoothAdapter mBtAdapter;
    private Set<BluetoothDevice> mBtDevices;

    public BluetoothManager(BluetoothAdapter pBtAdapter) {

        //Save bluetooth adapter as class member
        mBtAdapter = pBtAdapter;

        QueryPairedDevices();
    }

    private void QueryPairedDevices() {

        //Get set (no repeats, max 1 null value) of paired devices
        mBtDevices = mBtAdapter.getBondedDevices();

        //Should only work with paired devices, throw error if there are none
        if(mBtDevices.size() <= 0) {
            System.err.println("Please pair at least one device.");
        }
    }

}

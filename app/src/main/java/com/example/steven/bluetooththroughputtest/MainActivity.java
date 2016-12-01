package com.example.steven.bluetooththroughputtest;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.example.steven.bluetooththroughputtest.R.layout.activity_main);
        TextView mTextView = (TextView) findViewById(R.id.MainTest);
        BluetoothManager lBtManager = SetupBluetoothConnection();
    }

    public BluetoothManager SetupBluetoothConnection() {

        //Get device bluetooth adapter and save as class member
        BluetoothAdapter lBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        int lBluetoothSuccess = 0;

        //IF bluetooth is not enabled, see if the user wants to enable it
        if(!lBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, lBluetoothSuccess);
        }

        return new BluetoothManager(lBluetoothAdapter);

    }
}
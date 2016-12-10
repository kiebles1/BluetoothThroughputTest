package com.example.steven.bluetooththroughputtest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private final int BLUETOOTH_REQUEST_CODE = 1;
    BluetoothManager mBtManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.example.steven.bluetooththroughputtest.R.layout.activity_main);
        SetupBluetoothConnection();
    }

    public void SetupBluetoothConnection() {

        //Get device bluetooth adapter and save as class member
        BluetoothAdapter lBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //If bluetooth is not enabled, see if the user wants to enable it
        if (!lBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, BLUETOOTH_REQUEST_CODE);
            findViewById(R.id.radioButton1).setEnabled(false);
            findViewById(R.id.radioButton2).setEnabled(false);
            findViewById(R.id.pair_button).setEnabled(false);
        }

        mBtManager = new BluetoothManager(lBluetoothAdapter, mHandler);

    }

    public void onButtonClick(View v) {
        switch(v.getId()) {
            case R.id.pair_button:

                Set<BluetoothDevice> lPairedDeviceSet = mBtManager.GetPairedDevices();
                TextView lConsoleTextView = (TextView) findViewById(R.id.androidConsoleTV);
                String lPairedDeviceNames = "";
                if(lPairedDeviceSet.size() > 0) {
                    for (BluetoothDevice device : lPairedDeviceSet) {
                        lPairedDeviceNames = lPairedDeviceNames.concat(device.getName() + "\n");
                    }
                }
                else {
                    lPairedDeviceNames = "No paired devices.";
                }
                lConsoleTextView.setText(lPairedDeviceNames);
                break;

            case R.id.connection_button:

                mBtManager.EstablishConnection();

        }
    }

    public void onRadioButton(View v) {

        switch (v.getId()) {
            case R.id.radioButton1:

                mBtManager.SetMasterOrSlave(true);
                break;

            case R.id.radioButton2:

                mBtManager.SetMasterOrSlave(false);
                mBtManager.SetClientNumber(1);
                break;

            case R.id.radioButton3:

                mBtManager.SetMasterOrSlave(false);
                mBtManager.SetClientNumber(2);
                break;

        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == BLUETOOTH_REQUEST_CODE) {
            if(resultCode == RESULT_OK) {
                findViewById(R.id.radioButton1).setEnabled(true);
                findViewById(R.id.radioButton2).setEnabled(true);
                findViewById(R.id.radioButton3).setEnabled(true);
                findViewById(R.id.pair_button).setEnabled(true);
            }
        }
    }

    //PRIVATE:

    private ArrayList<Integer> ByteArrayToIntList(byte[] bytes) {

        ArrayList<Integer> intList = new ArrayList<Integer>();

        for(byte b : bytes) {
            intList.add((int)(b));
        }

        return intList;
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            byte[] integerBytes;
            int lengthRead = 0;
            TextView androidConsoleTextView = ((TextView) findViewById(R.id.androidConsoleTV));
            TextView androidLabelTextView = ((TextView) findViewById(R.id.androidLabelTV));
            switch(msg.what) {
                case 1:
                    androidLabelTextView.append("Client Read Number, ");
                    lengthRead = msg.getData().getInt("Device Name");
                    androidConsoleTextView.append(Integer.toString(lengthRead));
                    break;
                case 2:
                    androidLabelTextView.append("Server Sent Number, ");
                    integerBytes = msg.getData().getByteArray("Device Name");
                    mBtManager.AssembleData(integerBytes);
                    lengthRead = integerBytes.length;
                    if (androidConsoleTextView.getText().toString().compareTo("Console") == 0) {
                        androidConsoleTextView.setText("");
                    }
                    androidConsoleTextView.append(Integer.toString(lengthRead));
                    break;
                case 3:
                    androidLabelTextView.append("Time, ");
                    long startTime = msg.getData().getLong("Start Time");
                    long endTime = msg.getData().getLong("End Time");

                    androidConsoleTextView.append(String.valueOf(endTime - startTime));
                    break;
                case 4:
                    androidLabelTextView.append("Server Read Number, ");
                    integerBytes = msg.getData().getByteArray("Device Name");
                    mBtManager.AssembleData(integerBytes);
                    lengthRead = integerBytes.length;
                    if (androidConsoleTextView.getText().toString().compareTo("Console") == 0) {
                        androidConsoleTextView.setText("");
                    }
                    androidConsoleTextView.append(Integer.toString(lengthRead));

                    break;
            }
        }
    };
}
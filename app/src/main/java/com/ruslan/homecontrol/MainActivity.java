package com.ruslan.homecontrol;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends Activity {

    private static String HISTORY = "HISTORY";
    private static String LIVING_ROOM = "Living Room";

    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private Button toggleBtn;
    private TextView toggleName;
    private boolean state = false;
    private DeviceControl mDeviceControl;
    private HashMap<String, String> deviceNames;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 15000;
    public static int REQUEST_ENABLE_BT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(mDeviceControl == null) {
            mDeviceControl = new DeviceControl(this);
        }
        mHandler = new Handler();
        deviceNames = new HashMap<>();
        /*
        SharedPreferences historyPrefs = getSharedPreferences(HISTORY, MODE_PRIVATE);
        for(String name : historyPrefs.getAll().keySet()){
            String val = historyPrefs.getString(name, null);
            if(val != null){
                deviceNames.put(name, val);

            }

        }*/

        toggleName = findViewById(R.id.toggle_name);
        toggleBtn = findViewById(R.id.toggle_btn);
        toggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = state ? "0" : "1";
                mDeviceControl.mBluetoothLeService.writeCharacteristic(String.valueOf(message), deviceNames.get(toggleName.getText()));
            }
        });

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

    }

    public void onDataReceived(final String data){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (data.equals(BluetoothLeService.ON)){
                    toggleBtn.setText("Turn Off");
                    state = true;
                } else if (data.equals(BluetoothLeService.OFF)){
                    toggleBtn.setText("Turn On");
                    state = false;
                }
            }
        });

    }

    public void onDataReceived(final String data, final String address){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (data.equals(BluetoothLeService.NAME_LIVING_ROOM)){
                    toggleName.setText(LIVING_ROOM);
                    toggleBtn.setVisibility(View.VISIBLE);
                    deviceNames.put(LIVING_ROOM, address);
                }
            }
        });

    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mDeviceControl.setDevice(device);
                        }
                    });
                }
            };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    public void addDeviceName(String name, String address){
        deviceNames.put(name, address);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mDeviceControl == null){
            mDeviceControl = new DeviceControl(this);
        }
        registerReceiver(mDeviceControl.mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mDeviceControl.mBluetoothLeService != null) {
            final boolean result = mDeviceControl.mBluetoothLeService.connect(mDeviceControl.mDeviceAddress);
            Log.d("TAG", "Connect request result=" + result);
        }
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        // Initializes list view adapter.
        scanLeDevice(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mDeviceControl != null) {
            unregisterReceiver(mDeviceControl.mGattUpdateReceiver);
            if(deviceNames.isEmpty()) return;
            /*
            SharedPreferences.Editor preferences = getSharedPreferences(HISTORY, MODE_PRIVATE).edit();
            preferences.clear();
            for(String name : deviceNames.keySet()){
                preferences.putString(name, deviceNames.get(name));
            }
            preferences.apply();*/
        }
        scanLeDevice(false);
    }
}

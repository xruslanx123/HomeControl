package com.ruslan.homecontrol;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * Created by cambiumteam on 24/10/2017.
 */

public class DeviceControl {

    private final static String TAG = DeviceControl.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private MainActivity mActivity;
    private HashMap<String, BluetoothDevice> mDevices;
    private HashMap<String, Boolean> boundDevices;
    public BluetoothLeService mBluetoothLeService;
    public String mDeviceAddress, mDeviceName;
    private boolean mConnected;

    public final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    public final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                //displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                String data = String.valueOf(intent.getStringExtra(BluetoothLeService.EXTRA_DATA).charAt(0));
                if (Integer.valueOf(data) < 5) {
                    mActivity.onDataReceived(data);
                }
                if(!intent.getExtras().isEmpty() && intent.hasExtra(EXTRAS_DEVICE_ADDRESS)){
                    String address = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
                    if (Integer.valueOf(data) > 4) {
                        mActivity.onDataReceived(data, address);
                        mBluetoothLeService.writeCharacteristic(BluetoothLeService.STATE_REQUEST, address);
                    }
                    if(!boundDevices.containsKey(address) || !boundDevices.get(address)) {
                        mBluetoothLeService.setCharacteristicNotification(SampleGattAttributes.CUSTOM_SERVICE, SampleGattAttributes.CUSTOM_CHARACTERISTIC, address);
                        boundDevices.put(address, true);
                    }

                }


            }
        }
    };

    public DeviceControl(MainActivity activity){
        mActivity = activity;
        mDevices = new HashMap<>();
        boundDevices = new HashMap<>();
    }

    public void setDevice(BluetoothDevice device){
        if (device == null) return;
        mDevices.put(device.getAddress(), device);
        mDeviceName = device.getName();
        mDeviceAddress = device.getAddress();
        Intent gattServiceIntent = new Intent(mActivity, BluetoothLeService.class);
        gattServiceIntent.putExtra(EXTRAS_DEVICE_ADDRESS, device.getAddress());
        mActivity.bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

    }
}

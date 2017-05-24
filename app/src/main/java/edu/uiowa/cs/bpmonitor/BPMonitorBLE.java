package edu.uiowa.cs.bpmonitor;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;

/**
 * Created by Syed Shabih Hasan on 5/20/17.
 *
 * The process is quite straightforward:
 * 1) Get the BT manager and the adapter
 * 2) Setup the callbacks
 * 3) Get the device information
 * 4) Use the device information to connect to the device
 * 5) Once device's connection status has changed, onConnectionStateChanged will be called, if the
 * device is connected, discover the services it offers
 * 6) Once the services have been discovered, onServicesDiscovered will be called, enable the
 * notification
 * 7) Rediscover services, write the start command to the characteristic to start measurement
 * 8) onCharacteristicChanged will be called as the BP is being measured, check if it is the
 * BP_MEASUREMENT characteristic. Read the values.
 * Byte 0 (first byte) is just control indicator bits. Pulse (bit 2 LSB -> MSB) becomes 1 when the
 * measurement is over.
 * Bytes 1, 2 are systolic measurements. They are SFLOATS. mmHg
 * Bytes 3, 4 are diastolic measurements. They are SFLOATS. mmHg
 * Bytes 8, 9 are the pulse measurement which will become available when Byte 0's bit 2
 * (3rd bit) is 1
 *
 * As we get the measurements, update the UI
 *
 *
 */

public class BPMonitorBLE {
    private static final String TAG = "BPMonitorBLE";

    private String deviceAddress;
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothManager bluetoothManager;
    private BluetoothGatt bluetoothGattStack;
    private BluetoothGattCallback bluetoothGattCallback;
    private BluetoothDevice bpMonitor;
    private boolean enabledNotification = false;
    private TextViewUpdateCallback tvCallback;
    private Activity tempAc;


    public BPMonitorBLE(String deviceAddress, Context context, TextViewUpdateCallback tvCallback){
        Log.d(TAG, "inside BPMonitorBLE constructor");
        this.context = context;
        this.deviceAddress = deviceAddress;
        this.tvCallback = tvCallback;
        // we need the activity to update the textview on the UI
        tempAc = (Activity) context;
        enabledNotification = false;
        initializeBLE();
    }

    public void closeConnection(){
        enabledNotification = false;
        if(bluetoothManager != null) {
            try {
                bluetoothGattStack.disconnect();
            }catch(Exception e){
                Log.e(TAG, "error while disconnecting: \n"+e.toString());
            }
            bluetoothGattStack = null;
            bluetoothAdapter = null;
            bluetoothManager = null;
        }
    }

    /**
     * This function initializes the bluetooth components
     * */
    private void initializeBLE(){
        Log.d(TAG, "inside initialize BLE");
        // Step 1: get the Manager and the Adapter
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Step 2: setup callback
        bluetoothGattCallback = setupCallback();

        // Step 3: get the device information
        bpMonitor = bluetoothAdapter.getRemoteDevice(deviceAddress);

        // Step 4: connect to the device and define the callback
        bluetoothGattStack = bpMonitor.connectGatt(context, false, bluetoothGattCallback);
    }

    /**
     * Return the callback with the apt process
     * */
    private BluetoothGattCallback setupCallback(){
        BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                Log.d(TAG, "inside onServicesDiscovered");
                Log.d(TAG, "services discovered");
                /* this seems like a hack... once I update the characteristic to notify me,
                * I have to rediscover services before I can successfully ask the BP Monitor to give
                * stuff
                * */
                if(!enabledNotification){
                    enableBPNotifications();
                    Log.d(TAG, "written notification and description values");
                    enabledNotification = true;
                    bluetoothGattStack.discoverServices();
                }else{
                    Log.d(TAG, "notifications already enabled");
                    getBPValues();
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
                Log.d(TAG, "inside onCharacteristicRead");
                Log.d(TAG, "Char: "+characteristic.getUuid().toString() + ", status: "+status);
            }

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                Log.d(TAG, "inside onConnectionStateChange, newState: "+newState);
                if(newState == BluetoothGatt.STATE_CONNECTED){
                    Log.d(TAG, "connected to device");
                    bluetoothGattStack.discoverServices();
                }
                else if(newState == BluetoothGatt.STATE_DISCONNECTED){
                    Log.d(TAG, "disconnected");
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic){
                super.onCharacteristicChanged(gatt, characteristic);
                Log.d(TAG, "inside onCharacteristicChanged");
                if(characteristic.getUuid().equals(InternalConstants.BP_MEASUREMENT)) {
                    byte[] temp = characteristic.getValue();
                    Float systolic = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, 1);
                    Float diastolic = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, 3);
                    final String toDisplay = "Sys.: " + systolic.toString() + ", Dia.: " + diastolic.toString() + ", Pulse: NA";
                    tempAc.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvCallback.updateTextView(toDisplay);
                        }
                    });
                    Log.d(TAG, toDisplay);
                    // if pulse if present then the readings have been issued
                    if((temp[0] & 4) != 0){
                        Float pulse = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, 7);
                        final String toDisplay1 = "Systolic: " + systolic.toString() + ", Diastolic: " +
                                diastolic.toString() + ", Pulse: " + pulse.toString();
                        Log.e(TAG, toDisplay1);
                        tempAc.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvCallback.updateTextView(toDisplay1);
                            }
                        });
                    }
                }
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
                super.onCharacteristicWrite(gatt, characteristic, status);
                Log.d(TAG, "onCharacteristicWrite");
                if(characteristic.getUuid().equals(InternalConstants.BP_DEVICE_CONTROL)){
                    Log.d(TAG, "BP_DEVICE_CONTROL characteristic written: " + (status == BluetoothGatt.GATT_SUCCESS));
                    if(status == BluetoothGatt.GATT_SUCCESS) {
                        Log.d(TAG, "SUCCESS");
                    }
                }
            }
        };

        return gattCallback;
    }

    private void getBPValues(){
        Log.d(TAG, "inside getBPValues");
        BluetoothGattCharacteristic gattCharacteristic =
                bluetoothGattStack.getService(InternalConstants.BP_SERVICE).
                        getCharacteristic(InternalConstants.BP_DEVICE_CONTROL);
        if(gattCharacteristic == null){
            Log.e(TAG, "could not find BP_DEVICE_CONTROL");
            return;
        }
        gattCharacteristic.setValue(InternalConstants.START_BP_MEASUREMENT);
        boolean t1 = bluetoothGattStack.writeCharacteristic(gattCharacteristic);
        Log.d(TAG, "t1: "+t1);
    }

    private void enableBPNotifications(){
        Log.d(TAG, "inside enableBPNotifications");
        BluetoothGattCharacteristic bp_char = bluetoothGattStack.getService(InternalConstants.BP_SERVICE).getCharacteristic(InternalConstants.BP_MEASUREMENT);
        if(bp_char == null){
            return;
        }
        boolean t1 = bluetoothGattStack.setCharacteristicNotification(bp_char, true);
        Log.d(TAG, "notification: "+t1);
        if(!t1){
            Log.e(TAG, "notification: false");
            return;
        }
        BluetoothGattDescriptor bp_notification_desc = bp_char.getDescriptor(InternalConstants.CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID);
        if(bp_notification_desc == null){
            Log.e(TAG, "descriptor null");
            return;
        }
        bp_notification_desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        boolean t2 = bluetoothGattStack.writeDescriptor(bp_notification_desc);
        Log.d(TAG, "descriptor: "+t2);
        // THIS IS THE STRANGEST OF THINGS.... >:(
        try{
            Thread.sleep(3000);
        }catch (Exception e){
            Log.e(TAG, "Exception: \n"+e.toString());
        }
    }
}

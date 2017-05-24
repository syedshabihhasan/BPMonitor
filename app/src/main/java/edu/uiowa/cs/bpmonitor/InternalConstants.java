package edu.uiowa.cs.bpmonitor;

import java.util.UUID;

/**
 * Created by Syed Shabih Hasan on 5/20/17.
 */

public class InternalConstants {

    /**
     * The UUID for the standard BLE Service related BP Measurements
     * */
    public static final UUID BP_SERVICE = UUID.fromString("00001810-0000-1000-8000-00805f9b34fb");

    /**
     * Qardio's custom characteristic (within BP_SERVICE) to which the start and stop commands
     * are written to control the device
     * */
    public static final UUID BP_DEVICE_CONTROL = UUID.fromString("583cb5b3-875d-40ed-9098-c39eb0c1983d");

    /**
     * Main BLE SIG's BP Measurement Characteristic
     * */
    public static final UUID BP_MEASUREMENT = UUID.fromString("00002a35-0000-1000-8000-00805f9b34fb");

    /**
     * UUID of descriptor which needs to be updated for notifications
     * */
    public static final UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    /**
     * UUID of another characteristic within the BP Monitor. I am not sure what this does...
     * */
    public static final UUID BP_UNKNOWN_CHAR = UUID.fromString("107efd5d-de10-4f30-8c1f-3730687fd3ef");

    /**
     * Byte array with command to start measurements
     * */
    public static final byte[] START_BP_MEASUREMENT = new byte[]{(byte) 0xf1, (byte) 0x01};

    /**
     * Byte array with command to stop measurements
     * */
    public static final byte[] STOP_BP_MEASUREMENT = new byte[]{(byte) 0xf1, (byte) 0x02};

}

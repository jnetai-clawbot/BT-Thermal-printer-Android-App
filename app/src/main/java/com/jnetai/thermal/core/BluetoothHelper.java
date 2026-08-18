package com.jnetai.thermal.core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Build;
import com.jnetai.thermal.diagnostics.Diagnostics;
import com.jnetai.thermal.diagnostics.ErrorCodes;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BluetoothHelper {
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String COMPONENT = "BluetoothHelper";

    private Context context;
    private BluetoothSocket socket;
    private OutputStream outputStream;
    private BluetoothDevice currentDevice;

    public static class DeviceInfo {
        public final String name;
        public final String address;

        public DeviceInfo(String name, String address) {
            this.name = name;
            this.address = address;
        }

        @Override
        public String toString() {
            return (name == null || name.isEmpty() ? "Unknown Printer" : name) + "  (" + address + ")";
        }
    }

    public BluetoothHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean isSupported() {
        BluetoothManager bm = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bm == null) {
            Diagnostics.log(ErrorCodes.BT_001, COMPONENT, "isSupported", "Bluetooth service unavailable");
            return false;
        }
        BluetoothAdapter adapter = bm.getAdapter();
        if (adapter == null) {
            Diagnostics.log(ErrorCodes.BT_001, COMPONENT, "isSupported", "No Bluetooth adapter found");
            return false;
        }
        return true;
    }

    public BluetoothAdapter getAdapter() {
        BluetoothManager bm = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bm == null) return null;
        return bm.getAdapter();
    }

    public boolean isEnabled() {
        BluetoothAdapter a = getAdapter();
        return a != null && a.isEnabled();
    }

    public List<DeviceInfo> getBondedDevices() {
        List<DeviceInfo> list = new ArrayList<>();
        BluetoothAdapter adapter = getAdapter();
        if (adapter == null) return list;
        Set<BluetoothDevice> bonded = adapter.getBondedDevices();
        if (bonded == null) return list;
        for (BluetoothDevice d : bonded) {
            list.add(new DeviceInfo(d.getName(), d.getAddress()));
        }
        return list;
    }

    public boolean connect(String address) {
        BluetoothAdapter adapter = getAdapter();
        if (adapter == null) {
            Diagnostics.log(ErrorCodes.BT_001, COMPONENT, "connect", "Bluetooth unavailable, address=" + address);
            return false;
        }
        if (!adapter.isEnabled()) {
            Diagnostics.log(ErrorCodes.BT_001, COMPONENT, "connect", "Bluetooth disabled");
            return false;
        }
        BluetoothDevice device = adapter.getRemoteDevice(address);
        if (device == null) {
            Diagnostics.log(ErrorCodes.BT_006, COMPONENT, "connect", "Device not found: " + address);
            return false;
        }
        close();
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                socket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID);
            } else if (Build.VERSION.SDK_INT >= 31) {
                socket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID);
            } else {
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
            }
            socket.connect();
            outputStream = socket.getOutputStream();
            currentDevice = device;
            Diagnostics.info(COMPONENT, "connect", "Connected to " + device.getName() + " " + address);
            return true;
        } catch (IOException e) {
            Diagnostics.log(ErrorCodes.BT_004, COMPONENT, "connect", e, "Address=" + address);
            close();
            return false;
        } catch (SecurityException e) {
            Diagnostics.log(ErrorCodes.BT_002, COMPONENT, "connect", e, "BLUETOOTH_CONNECT permission denied");
            return false;
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && outputStream != null;
    }

    public String getCurrentDeviceName() {
        return currentDevice != null ? currentDevice.getName() : null;
    }

    public String getCurrentDeviceAddress() {
        return currentDevice != null ? currentDevice.getAddress() : null;
    }

    public boolean write(byte[] data) {
        if (!isConnected()) {
            Diagnostics.log(ErrorCodes.PR_002, COMPONENT, "write", "Not connected, bytes=" + data.length);
            return false;
        }
        try {
            outputStream.write(data);
            outputStream.flush();
            Diagnostics.info(COMPONENT, "write", "Wrote " + data.length + " bytes");
            return true;
        } catch (IOException e) {
            Diagnostics.log(ErrorCodes.BT_005, COMPONENT, "write", e, "IO during write, bytes=" + data.length);
            close();
            return false;
        } catch (SecurityException e) {
            Diagnostics.log(ErrorCodes.BT_002, COMPONENT, "write", e, "BLUETOOTH_CONNECT permission denied during write");
            return false;
        }
    }

    public void close() {
        try {
            if (outputStream != null) { outputStream.close(); }
        } catch (IOException e) {
            Diagnostics.info(COMPONENT, "close", "OutputStream close exception: " + e.getMessage());
        }
        try {
            if (socket != null) { socket.close(); }
        } catch (IOException e) {
            Diagnostics.info(COMPONENT, "close", "Socket close exception: " + e.getMessage());
        }
        outputStream = null;
        socket = null;
        currentDevice = null;
    }
}
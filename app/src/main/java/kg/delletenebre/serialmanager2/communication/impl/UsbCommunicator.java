package kg.delletenebre.serialmanager2.communication.impl;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;

import com.felhr.usbserial.UsbSerialDevice;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import kg.delletenebre.serialmanager2.App;
import kg.delletenebre.serialmanager2.communication.BaseCommunicatorImpl;
import kg.delletenebre.serialmanager2.communication.CommunicatorType;

import static com.felhr.usbserial.UsbSerialDevice.createUsbSerialDevice;

public class UsbCommunicator extends BaseCommunicatorImpl {
    private final UsbManager usbManager;
    private final Map<String, UsbSerialDevice> connectedDevices;

    public UsbCommunicator(Context context) {
        super(context, CommunicatorType.USB);
        connectedDevices = new HashMap<>();
        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
    }

    @Override
    public void open() {
        Set<String> selectedDevices = PreferenceManager.getDefaultSharedPreferences(context).getStringSet("usb_device", null);
        for (Map.Entry<String, UsbDevice> device : usbManager.getDeviceList().entrySet()) {
            if (isSelectedDevice(device.getValue(), selectedDevices)
                    && !connectedDevices.containsKey(device.getValue().getDeviceName())) {

                if (!usbManager.hasPermission(device.getValue())) {
                    PendingIntent mPermissionIntent = PendingIntent.getBroadcast(App.getInstance(), 0, new Intent(), 0);
                    usbManager.requestPermission(device.getValue(), mPermissionIntent);

                    localBroadcastManager.sendBroadcast(
                            new Intent(App.LOCAL_ACTION_CONNECTION_FAILED)
                                    .putExtra("type", "usb")
                                    .putExtra("name", "(Permission denied) " + device.getValue().getDeviceName()));
                    continue;
                }

                UsbSerialDevice serialDevice = openSpecific(device.getValue());

                if (serialDevice != null) {
                    connectedDevices.put(device.getValue().getDeviceName(), serialDevice);
                    String path = device.getValue().getDeviceName();
                    int vid = device.getValue().getVendorId();
                    int pid = device.getValue().getProductId();
                    App.log("USB Connection-Connected to USB-device: " + path + " VID: " + vid + " PID: " + pid);
                }
            }
        }

        if (isConnectionStateMessageUse() && !connectedDevices.isEmpty()) {
            write(App.ACTION_CONNECTION_ESTABLISHED);
        }
    }

    @Override
    public void close() {
        if (connectedDevices.isEmpty()) {
            return;
        }

        if (isConnectionStateMessageUse()) {
            write(App.ACTION_CONNECTION_LOST);
        }

        for (UsbSerialDevice connectedDevice : connectedDevices.values()) {
            connectedDevice.close();
        }
        connectedDevices.clear();

        localBroadcastManager.sendBroadcast(
                new Intent(App.LOCAL_ACTION_CONNECTION_CLOSED)
                        .putExtra("type", "usb")
                        .putExtra("name", "usb"));
    }

    public void close(UsbDevice device) {
        if (!connectedDevices.containsKey(device.getDeviceName())) {
            return;
        }

        UsbSerialDevice serialDevice = connectedDevices.get(device.getDeviceName());
        serialDevice.close();
        connectedDevices.remove(device.getDeviceName());
        localBroadcastManager.sendBroadcast(
                new Intent(App.LOCAL_ACTION_CONNECTION_CLOSED)
                        .putExtra("type", "usb")
                        .putExtra("name", device.getDeviceName()));
    }

    @Override
    public void write(@NonNull String data) {
        for (UsbSerialDevice connectedDevice : connectedDevices.values()) {
            connectedDevice.write(data.getBytes());
        }
    }

    @Override
    public boolean isConnected() {
        return connectedDevices.size() > 0;
    }

    @Override
    public Integer getConnectionsCount() {
        return connectedDevices.size();
    }

    private UsbSerialDevice openSpecific(UsbDevice device) {
        UsbDeviceConnection connection = usbManager.openDevice(device);
        String deviceName = device.getDeviceName();

        try {
            UsbSerialDevice serialDevice = createUsbSerialDevice(device, connection);
            if (serialDevice.open()) {
                serialDevice.setBaudRate(App.getInstance().getIntPreference("usb_baud_rate"));
                serialDevice.setDataBits(App.getInstance().getIntPreference("usb_data_bits"));
                serialDevice.setStopBits(App.getInstance().getIntPreference("usb_stop_bits"));
                serialDevice.setParity(App.getInstance().getIntPreference("usb_parity"));
                serialDevice.setFlowControl(App.getInstance().getIntPreference("usb_flow_control"));

                serialDevice.read(data -> {
                    if (data.length > 0) {
                        String strData = new String(data, StandardCharsets.UTF_8);
                        localBroadcastManager.sendBroadcast(
                                new Intent(App.LOCAL_ACTION_COMMAND_RECEIVED)
                                        .putExtra("from", "usb")
                                        .putExtra("command", strData));
                    }
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });

                Thread.sleep(2000);

                localBroadcastManager.sendBroadcast(
                        new Intent(App.LOCAL_ACTION_CONNECTION_ESTABLISHED)
                                .putExtra("type", "usb")
                                .putExtra("name", deviceName));
                return serialDevice;
            }
        } catch (Exception e) {
            App.logError("USB Connection failed-" + e.getMessage());
        }
        localBroadcastManager.sendBroadcast(
                new Intent(App.LOCAL_ACTION_CONNECTION_FAILED)
                        .putExtra("type", "usb")
                        .putExtra("name", device.getDeviceName()));
        return null;
    }

    //devicePath:pid:vid
    private boolean isSelectedDevice(UsbDevice device, Set<String> selectedDevices) {
        String identifier = device.getDeviceName() + ":" + device.getProductId() + ":" + device.getVendorId();
        return selectedDevices.contains(identifier);
    }
}

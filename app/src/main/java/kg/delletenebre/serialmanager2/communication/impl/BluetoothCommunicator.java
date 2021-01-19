package kg.delletenebre.serialmanager2.communication.impl;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import androidx.annotation.NonNull;

import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothClassicService;
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothConfiguration;
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothService;
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothStatus;
import com.github.douglasjunior.bluetoothlowenergylibrary.BluetoothLeService;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import kg.delletenebre.serialmanager2.App;
import kg.delletenebre.serialmanager2.R;
import kg.delletenebre.serialmanager2.communication.BaseCommunicatorImpl;
import kg.delletenebre.serialmanager2.communication.CommunicatorType;

import static com.github.douglasjunior.bluetoothclassiclibrary.BluetoothStatus.CONNECTED;

public class BluetoothCommunicator extends BaseCommunicatorImpl {
    private BluetoothService bluetoothService;
    private BluetoothStatus lastStatus;

    public BluetoothCommunicator(Context context) {
        super(context, CommunicatorType.BLUETOOTH);
    }

    @Override
    public void open() {
        bluetoothService = initBluetooth();
        if (bluetoothService == null) {
            return;
        }

        bluetoothService.setOnEventCallback(new BluetoothService.OnBluetoothEventCallback() {
            @Override
            public void onDataRead(byte[] buffer, int length) {
                String data = new String(buffer, StandardCharsets.UTF_8);
                App.log(String.format("Bluetooth-Data read: %s", data));

                localBroadcastManager.sendBroadcast(
                        new Intent(App.LOCAL_ACTION_COMMAND_RECEIVED)
                                .putExtra("from", "bluetooth")
                                .putExtra("command", data));
            }

            @Override
            public void onStatusChange(BluetoothStatus status) {
                App.log(String.format("Bluetooth-Status: %s", status.toString()));
//                communicationService.updateNotificationText()

                switch (status) {
                    case CONNECTED: {
                        localBroadcastManager.sendBroadcast(
                                new Intent(App.LOCAL_ACTION_CONNECTION_ESTABLISHED)
                                        .putExtra("type", "bluetooth")
                                        .putExtra("name", bluetoothService.getConfiguration().deviceName));
                        if (isConnectionStateMessageUse()) {
                            write(App.ACTION_CONNECTION_ESTABLISHED);
                        }
                        break;
                    }
                    case NONE:
                        if (CONNECTED == lastStatus && isConnectionStateMessageUse()) {
                            write(App.ACTION_CONNECTION_LOST);
                        }

                        int delay = 2;
                        App.log(String.format("Bluetooth-Reconnect after %d seconds", delay));
                        new Handler().postDelayed(() -> connect(), delay * 1000);
                        break;
                    default:
                }
                lastStatus = status;
            }

            @Override
            public void onDeviceName(String deviceName) {

            }

            @Override
            public void onToast(String message) {

            }

            @Override
            public void onDataWrite(byte[] buffer) {
                String data = new String(buffer, StandardCharsets.UTF_8);
                App.log(String.format("Bluetooth-Data write: %s", data));
            }
        });

        connect();
    }

    @Override
    public void close() {
        if (bluetoothService != null) {
            if (isConnectionStateMessageUse()) {
                write(App.ACTION_CONNECTION_LOST);
            }

            bluetoothService.disconnect();
            localBroadcastManager.sendBroadcast(
                    new Intent(App.LOCAL_ACTION_CONNECTION_CLOSED)
                            .putExtra("type", "bluetooth")
                            .putExtra("name", "bluetooth"));
        }
    }

    @Override
    public void write(@NonNull String data) {
        if (bluetoothService != null) {
            bluetoothService.write(data.getBytes());
        }
    }

    @Override
    public boolean isConnected() {
        if (bluetoothService != null) {
            return CONNECTED == bluetoothService.getStatus();
        }
        return false;
    }

    @Override
    public Integer getConnectionsCount() {
        return isConnected() ? 1 : 0;
    }

    private BluetoothService initBluetooth() {
        if (BluetoothAdapter.getDefaultAdapter() != null) {
            if (!BluetoothAdapter.getDefaultAdapter().isEnabled()
                    && App.getInstance().getBooleanPreference("bluetooth_adapter_turn_on")) {
                BluetoothAdapter.getDefaultAdapter().enable();
            }

            BluetoothConfiguration configuration = new BluetoothConfiguration();
            configuration.context = context;
            configuration.bufferSize = 1024;
            configuration.characterDelimiter = '\n';
            configuration.deviceName = context.getString(R.string.app_name);
            configuration.callListenersInMainThread = true;
            // config.transport = BluetoothDevice.TRANSPORT_LE; // Only for dual-mode devices

            if (App.getInstance().getBooleanPreference("bluetooth_le_protocol")) {
                // Bluetooth Low Energy
                configuration.bluetoothServiceClass = BluetoothLeService.class;
                configuration.uuidService = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
                configuration.uuidCharacteristic = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

            } else {
                // Bluetooth Classic
                configuration.bluetoothServiceClass = BluetoothClassicService.class;
                configuration.uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
            }

            BluetoothService.init(configuration);
            return BluetoothService.getDefaultInstance();
        }
        return null;
    }

    private void connect() {
        if (bluetoothService == null) {
            return;
        }

        String address = App.getInstance().getStringPreference("bluetooth_device");
        if (address != null && !address.isEmpty()) {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null) {
                BluetoothDevice bluetoothDevice = adapter.getRemoteDevice(address);
                if (bluetoothDevice != null) {
                    bluetoothService.connect(bluetoothDevice);
                }
            }
        }
    }
}

package kg.delletenebre.serialmanager2;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import kg.delletenebre.serialmanager2.utils.Utils;

public class CommunicationService extends Service implements SensorEventListener {
    private final static int NOTIFICATION_ID = 109;
    public final static String LINE_SEPARATOR = System.getProperty("line.separator");

    public final static String EXTRA_BLUETOOTH_ENABLED = "bluetooth_enabled";
    public final static String EXTRA_UPDATE_USB_CONNECTION = "update_usb";
    public final static String EXTRA_UPDATE_BLUETOOTH_CONNECTION = "update_bluetooth";


    private UsbManager mUsbManager;
    private BroadcastReceiver mBroadcastReceiver;
    private SharedPreferences mPrefs;

    // **** USB **** //
    private HashMap<String,UsbSerialDevice> mConnectedUsbSerialDevices;
    private HashMap<String,ByteArrayOutputStream> mUsbSerialReadBuffers;

    // **** BLUETOOTH **** //
    private BluetoothConnection mBluetothConnection;

    // **** WEBSERVER **** //
    private AsyncHttpServer mWebServer;
    private List<WebSocket> mWebSockets;

    private SensorManager mSensorManager;

    private NotificationCompat.Builder mNotification;
    private RemoteViews mNotificationLayout;


    public CommunicationService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getBooleanExtra(EXTRA_UPDATE_USB_CONNECTION, false)) {
            startUsbCommunication();
        }

        if (intent.getBooleanExtra(EXTRA_BLUETOOTH_ENABLED, false)
                || intent.getBooleanExtra(EXTRA_UPDATE_BLUETOOTH_CONNECTION, false)) {
            startBluetoothCommunication();
        }

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        mPrefs = App.getInstance().getPrefs();
        initializeNotification();

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mConnectedUsbSerialDevices = new HashMap<>();
        mUsbSerialReadBuffers = new HashMap<>();

        mBluetothConnection = new BluetoothConnection();
        if (mPrefs.getBoolean("bluetooth_adapter_turn_on",
                getResources().getBoolean(R.bool.pref_default_bluetooth_adapter_turn_on))) {
            mBluetothConnection.getBluetoothAdapter().enable();
        }

        if (isCommunicationTypeEnabled("web_socket")) {
            startWebServer();
        }

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                switch (action) {
                    case UsbManager.ACTION_USB_DEVICE_DETACHED:
                        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if (mConnectedUsbSerialDevices.containsKey(device.getDeviceName())) {
                            mConnectedUsbSerialDevices.get(device.getDeviceName()).close();
                            mConnectedUsbSerialDevices.remove(device.getDeviceName());
                            updateNotificationText();
                        }
                        break;

                    case BluetoothAdapter.ACTION_STATE_CHANGED:
                        final int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                                BluetoothAdapter.ERROR);
                        if (bluetoothState == BluetoothAdapter.STATE_TURNING_OFF) {
                            stopBluetoothCommunication();
                        }
                        break;

                    case App.ACTION_SEND_DATA:
                        String data = intent.getStringExtra("data");
                        if (data != null) {
                            sendData(data);
                        }
                        break;

                    case App.LOCAL_ACTION_SETTINGS_UPDATED:
                        if (isCommunicationTypeEnabled("usb")) {
                            if (mConnectedUsbSerialDevices.isEmpty()) {
                                startUsbCommunication();
                            }
                        } else {
                            stopUsbCommunication();
                        }
                        if (isCommunicationTypeEnabled("bluetooth")) {
                            if (mBluetothConnection == null) {
                                startBluetoothCommunication();
                            }
                        } else {
                            stopBluetoothCommunication();
                        }
                        if (isCommunicationTypeEnabled("web_socket")) {
                            if (mWebServer == null) {
                                startWebServer();
                            }
                        } else {
                            stopWebServer();
                        }
                        updateNotificationText();
                        break;
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(App.ACTION_SEND_DATA);
        intentFilter.addAction(App.LOCAL_ACTION_SETTINGS_UPDATED);
        intentFilter.addAction(App.ACTION_EXTERNAL_COMMAND);
        registerReceiver(mBroadcastReceiver, intentFilter);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor sensorLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (sensorLight != null) {
            mSensorManager.registerListener(this, sensorLight, SensorManager.SENSOR_DELAY_NORMAL);
        }

        sendBroadcast(new Intent(App.ACTION_SERVICE_STARTED));

        updateNotificationText();
    }

    @Override
    public void onDestroy() {
        if (isConnectionStateMessageEnabled()) {
            sendData(App.ACTION_CONNECTION_LOST);
        }

        unregisterReceiver(mBroadcastReceiver);
        stopUsbCommunication();
        mConnectedUsbSerialDevices = null;
        mUsbSerialReadBuffers = null;
        stopBluetoothCommunication();
        stopWebServer();

//        if (mPrefs.getBoolean("bluetooth_adapter_turn_on",
//                getResources().getBoolean(R.bool.pref_default_bluetooth_adapter_turn_on))) {
//            mBluetothConnection.getBluetoothAdapter().disable();
//        }

        mSensorManager.unregisterListener(this);

        sendBroadcast(new Intent(App.ACTION_SERVICE_STOPPED));

        super.onDestroy();
    }


    private boolean isCommunicationTypeEnabled(String type) {
        return mPrefs.getBoolean(type + "_communication_enabled",
                getResources().getBoolean(Utils.getBooleanIdentifier(this,
                        "pref_default_" + type + "_communication_enabled")));
    }


    private int getNotificationTextColor(int style) {
        TextView tempTextView = new TextView(this);
        tempTextView = Utils.setTextAppearence(tempTextView, style);
        return tempTextView.getCurrentTextColor();
    }

    private void initializeNotification() {
        mNotificationLayout = new RemoteViews(getPackageName(), R.layout.layout_notification);

        int textColor = getNotificationTextColor(
                android.support.v7.appcompat.R.style.TextAppearance_AppCompat_Notification_Info);

        Bitmap appIcon = getNotificationInfoIcon(R.drawable.notification_icon, textColor);
        mNotificationLayout.setImageViewBitmap(R.id.app_icon, appIcon);

        textColor = getNotificationTextColor(
                android.support.v7.appcompat.R.style.TextAppearance_AppCompat_Notification_Title);

        Bitmap usbIcon = getNotificationInfoIcon(R.drawable.ic_usb, textColor);
        mNotificationLayout.setImageViewBitmap(R.id.usb_connections_icon, usbIcon);

        Bitmap bluetoothIcon = getNotificationInfoIcon(R.drawable.ic_bluetooth_black_24dp, textColor);
        mNotificationLayout.setImageViewBitmap(R.id.bluetooth_connections_icon, bluetoothIcon);

        Bitmap websocketIcon = getNotificationInfoIcon(R.drawable.ic_language_black_24dp, textColor);
        mNotificationLayout.setImageViewBitmap(R.id.web_socket_connections_icon, websocketIcon);

        mNotification =
                (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                        .setOnlyAlertOnce(true)
                        .setSmallIcon(R.drawable.notification_icon)
                        .setContent(mNotificationLayout)
                        .setContentIntent(
                                PendingIntent.getActivity(this, 0,
                                        new Intent(this, MainActivity.class), 0));
        startForeground(NOTIFICATION_ID, mNotification.build());
    }

    private void updateNotificationText() {
        if (isCommunicationTypeEnabled("usb")) {
            if (mNotificationLayout != null) {
                String usbCountText = "0";
                if (mConnectedUsbSerialDevices != null) {
                    usbCountText = String.valueOf(mConnectedUsbSerialDevices.size());
                }
                mNotificationLayout.setTextViewText(R.id.usb_connections_count, usbCountText);
                setNotificationInfoVisibility("usb", View.VISIBLE);
            }
        } else {
            setNotificationInfoVisibility("usb", View.GONE);
        }

        if (isCommunicationTypeEnabled("bluetooth")) {
            if (mNotificationLayout != null) {
                int bluetoothIconId = R.drawable.ic_bluetooth_black_24dp;
                if (mBluetothConnection != null && mBluetothConnection.isConnected()) {
                    bluetoothIconId = R.drawable.ic_bluetooth_connected_black_24dp;
                }
                int textColor = getNotificationTextColor(
                        android.support.v7.appcompat.R.style.TextAppearance_AppCompat_Notification_Title);
                Bitmap bluetoothIcon = getNotificationInfoIcon(bluetoothIconId, textColor);
                mNotificationLayout.setImageViewBitmap(R.id.bluetooth_connections_icon, bluetoothIcon);
            }
            setNotificationInfoVisibility("bluetooth", View.VISIBLE);
        } else {
            setNotificationInfoVisibility("bluetooth", View.GONE);
        }

        if (isCommunicationTypeEnabled("web_socket")) {
            if (mNotificationLayout != null) {
                String webSocketCountText = "0";
                if (mWebSockets != null) {
                    webSocketCountText = String.valueOf(mWebSockets.size());
                }
                mNotificationLayout.setTextViewText(R.id.web_socket_connections_count,
                        webSocketCountText);
                setNotificationInfoVisibility("web_socket", View.VISIBLE);
            }
            mNotificationLayout.setTextViewText(R.id.ip_address,
                    Utils.getIpAddress() + ":" + App.getInstance().getIntPreference("web_server_port",
                            getString(R.string.pref_default_web_server_port)));
            mNotificationLayout.setViewVisibility(R.id.ip_address, View.VISIBLE);
        } else {
            setNotificationInfoVisibility("web_socket", View.GONE);
            mNotificationLayout.setViewVisibility(R.id.ip_address, View.GONE);
        }

        mNotification.setContent(mNotificationLayout);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, mNotification.build());
    }

    private void setNotificationInfoVisibility(String type, int visibility) {
        mNotificationLayout.setViewVisibility(
                Utils.getIdIdentifier(this, type + "_connections_icon"), visibility);
        mNotificationLayout.setViewVisibility(
                Utils.getIdIdentifier(this, type + "_connections_count"), visibility);
    }

    private Bitmap getNotificationInfoIcon(int iconResId, int color) {
        Bitmap usbIcon = Utils.getBitmapFromVectorDrawable(this, iconResId);
        return Utils.tintBitmap(usbIcon, color);
    }


    private void startUsbCommunication() {
        for (UsbDevice device : mUsbManager.getDeviceList().values()) {
            if (mUsbManager.hasPermission(device) &&
                    !mConnectedUsbSerialDevices.containsKey(device.getDeviceName())) {
                if (setupUsbSerialConnection(device)) {
                    String path = device.getDeviceName();
                    String vid = String.valueOf(device.getVendorId());
                    String pid = String.valueOf(device.getProductId());
                    App.log("Connected usb device: " + path + " | VID:" + vid + " | PID: " + pid);
                }
            }
        }
    }
    private void stopUsbCommunication() {
        if (!mConnectedUsbSerialDevices.isEmpty()) {
            for (Iterator<Map.Entry<String, UsbSerialDevice>> iterator =
                 mConnectedUsbSerialDevices.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry<String, UsbSerialDevice> entry = iterator.next();
                entry.getValue().close(); // close usb serial port
                iterator.remove();
            }
        }
    }
    private boolean setupUsbSerialConnection(UsbDevice device) {
        UsbDeviceConnection connection = mUsbManager.openDevice(device);
        UsbSerialDevice serialDevice = null;

        try {
            serialDevice = UsbSerialDevice.createUsbSerialDevice(device, connection);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (serialDevice != null && serialDevice.open()) {
            serialDevice.setBaudRate(App.getInstance()
                    .getIntPreference("usb_baud_rate", getString(R.string.pref_default_usb_baud_rate)));
//            serialDevice.setDataBits(UsbSerialInterface.DATA_BITS_8);
//            serialDevice.setStopBits(UsbSerialInterface.STOP_BITS_1);
//            serialDevice.setParity(UsbSerialInterface.PARITY_NONE);

            final String deviceName = device.getDeviceName();
            mConnectedUsbSerialDevices.put(deviceName, serialDevice);
            mUsbSerialReadBuffers.put(deviceName, new ByteArrayOutputStream());
            serialDevice.read(new UsbSerialInterface.UsbReadCallback() {
                @Override
                public void onReceivedData(byte[] arg0) {
                    try {
                        ByteArrayOutputStream buffer = mUsbSerialReadBuffers.get(deviceName);
                        buffer.write(arg0);
                        String data = buffer.toString("UTF-8");
                        if (data.contains(LINE_SEPARATOR)) {
                            String[] dataParts = data.split(LINE_SEPARATOR);
                            App.getInstance().detectCommand(dataParts[0].replaceAll("\r", "").replaceAll("\n", ""));
                            buffer.reset();
                            if (dataParts.length == 2) {
                                buffer.write(dataParts[1].getBytes("UTF-8"));
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            //
            // Some Arduinos would need some sleep because firmware wait some time to know whether
            // a new sketch is going to be uploaded or not
//            try {
//                Thread.sleep(2000); // sleep some. YMMV with different chips.
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
            if (isConnectionStateMessageEnabled()) {
                serialDevice.write((App.ACTION_CONNECTION_ESTABLISHED + "\n").getBytes());
            }
            updateNotificationText();

            return true;
        }

        return false;
    }
    private void usbSend(String message) {
        if (!mConnectedUsbSerialDevices.isEmpty()) {
            for (Map.Entry<String, UsbSerialDevice> entry : mConnectedUsbSerialDevices.entrySet()) {
                UsbSerialDevice serial = entry.getValue();
                serial.write(message.getBytes());
            }
        }
    }


    private void startBluetoothCommunication() {
        if (isCommunicationTypeEnabled("bluetooth")) {
            if (mBluetothConnection == null) {
                mBluetothConnection = new BluetoothConnection();
            }
            mBluetothConnection.autoConnectTo(mPrefs.getString("bluetooth_device", ""));
            mBluetothConnection.setConnectionListener(new BluetoothConnection.ConnectionListener() {
                @Override
                public void onDeviceConnected(String name, String address) {
                    if (isConnectionStateMessageEnabled()) {
                        bluetoothSend(App.ACTION_CONNECTION_ESTABLISHED);
                    }
                    updateNotificationText();
                }

                @Override
                public void onDeviceConnectionFailed() {
                    updateNotificationText();
                    if (mBluetothConnection != null) {
                        mBluetothConnection.autoConnectTo(mPrefs.getString("bluetooth_device", ""));
                    }
                }
            });
            mBluetothConnection.setOnDataReceivedListener(new BluetoothConnection.OnDataReceivedListener() {
                public void onDataReceived(String message) {
                    App.getInstance().detectCommand(message);
                }
            });
        }
    }
    private void stopBluetoothCommunication() {
        if (mBluetothConnection != null) {
            mBluetothConnection.stop();
            mBluetothConnection = null;
            updateNotificationText();
        }
    }
    private void bluetoothSend(String message) {
        if (mBluetothConnection != null) {
            mBluetothConnection.write(message, true);
        }
    }


    public void startWebServer() {
        mWebServer = new AsyncHttpServer();
        mWebSockets = new ArrayList<>();
        final int port = App.getInstance().getIntPreference("web_server_port",
                getString(R.string.pref_default_web_server_port));
        final String ipAddress = Utils.getIpAddress();

        mWebServer.get("/", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                String webSocketInfo = String.format(getString(R.string.web_socket_info),
                        ipAddress, port);

                response.send("<!DOCTYPE html><head><title>" + getString(R.string.app_name) + "</title><meta charset=\"utf-8\" /></head><body><h1>" + getString(R.string.app_name) + "</h1><i>version: <b>" + App.getInstance().getVersion() + "</b></i><br><br>" + webSocketInfo + "<br><br><a href=\"/test-websocket\">WebSocket test</a></body></html>");
            }
        });
        mWebServer.get("/test-websocket", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                AssetManager assetManager = getAssets();

                String html = "<h1>404 Not found</h1>";
                InputStream input;
                try {
                    input = assetManager.open("websoket_test.html");
                    int size = input.available();
                    byte[] buffer = new byte[size];
                    //noinspection ResultOfMethodCallIgnored
                    input.read(buffer);
                    input.close();

                    html = new String(buffer);
                    html = html.replace("{{address}}", ipAddress + ":" + port + "/ws");
                } catch(IOException e) {
                    e.printStackTrace();
                }

                response.send(html);
            }
        });
        mWebServer.listen(port);
        mWebServer.websocket("/ws", new AsyncHttpServer.WebSocketRequestCallback() {
            @Override
            public void onConnected(final WebSocket webSocket, AsyncHttpServerRequest request) {
                App.log("New WebSocket client connected");
                webSocket.setClosedCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception e) {
                        try {
                            if (e != null) {
                                e.printStackTrace();
                            }
                        } finally {
                            mWebSockets.remove(webSocket);
                            updateNotificationText();
                        }
                    }
                });
                webSocket.setStringCallback(new WebSocket.StringCallback() {
                    @Override
                    public void onStringAvailable(String message) {
                        App.log("Received from WebSocket: " + message);
                        App.getInstance().detectCommand(message);
                    }
                });
                mWebSockets.add(webSocket);
                if (isConnectionStateMessageEnabled()) {
                    webSocket.send(App.ACTION_CONNECTION_ESTABLISHED);
                }
                updateNotificationText();
            }
        });
    }
    private void stopWebServer() {
        if (mWebSockets != null) {
            for (WebSocket socket : mWebSockets) {
                socket.close();
            }
            mWebSockets = null;
        }

        if (mWebServer != null) {
            mWebServer.stop();
            mWebServer = null;
        }
    }
    public void webSocketSend(String message) {
        if (mWebSockets != null) {
            for (WebSocket socket : mWebSockets) {
                socket.send(message);
            }
        }
    }


    private void sendData(String message) {
        usbSend(message);
        bluetoothSend(message);
        webSocketSend(message);
    }

    private int mLastLightSensorMode = 0;
    private long mLastLightSensorMillis = 0;
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float value = event.values[0];
            int mode = 0;

            if (value >= SensorManager.LIGHT_SUNLIGHT_MAX) {
                mode = 7;
            } else if (value >= SensorManager.LIGHT_SUNLIGHT) {
                mode = 6;
            } else if (value >= SensorManager.LIGHT_SHADE) {
                mode = 5;
            } else if (value >= SensorManager.LIGHT_OVERCAST) {
                mode = 4;
            } else if (value >= SensorManager.LIGHT_SUNRISE) {
                mode = 3;
            } else if (value >= SensorManager.LIGHT_CLOUDY) {
                mode = 2;
            } else if (value >= SensorManager.LIGHT_FULLMOON) {
                mode = 1;
            }

            if (mLastLightSensorMode != mode
                    && System.currentTimeMillis() - mLastLightSensorMillis > 3000) {
                mLastLightSensorMillis = System.currentTimeMillis();
                mLastLightSensorMode = mode;

                if (mPrefs != null && mPrefs.getBoolean("send_light_sensor_data",
                        getResources().getBoolean(R.bool.pref_default_send_light_sensor_data))) {
                    sendData(String.format(Locale.getDefault(),
                            getString(R.string.send_data_to_controller_format),
                            "light_sensor_value", String.valueOf(value)));
                    sendData(String.format(Locale.getDefault(),
                            getString(R.string.send_data_to_controller_format),
                            "light_sensor_mode", String.valueOf(mode)));
                }
            }
        }
    }


    private boolean isConnectionStateMessageEnabled() {
        return mPrefs != null && mPrefs.getBoolean("send_connection_state",
                getResources().getBoolean(R.bool.pref_default_send_connection_state));
    }
}

package kg.delletenebre.serialmanager2.communication;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import kg.delletenebre.serialmanager2.App;
import kg.delletenebre.serialmanager2.MainActivity;
import kg.delletenebre.serialmanager2.R;
import kg.delletenebre.serialmanager2.communication.impl.BluetoothCommunicator;
import kg.delletenebre.serialmanager2.communication.impl.SerialCommunicator;
import kg.delletenebre.serialmanager2.communication.impl.UsbCommunicator;
import kg.delletenebre.serialmanager2.communication.impl.WebServerCommunicator;
import kg.delletenebre.serialmanager2.utils.Utils;

public class CommunicationService extends Service implements SensorEventListener {
    private final static int NOTIFICATION_ID = 109;

    public final static String EXTRA_BLUETOOTH_ENABLED = "bluetooth_enabled";
    public final static String EXTRA_UPDATE_USB_CONNECTION = "update_usb";
    public final static String EXTRA_UPDATE_BLUETOOTH_CONNECTION = "update_bluetooth";

    private BroadcastReceiver mBroadcastReceiver;
    private BroadcastReceiver mLocalBroadcastReceiver;
    private LocalBroadcastManager mLocalBroadcastManager;
    private SharedPreferences mPrefs;

    private Map<CommunicatorType, BaseCommunicator> communicators;
    private SensorManager mSensorManager;

    private Notification.Builder mNotificationBuilder;
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
        App.log("CommunicationService-onStartCommand");

        for (BaseCommunicator communicator : communicators.values()) {
            communicator.openOrClose(
                    isCommunicationTypeEnabled(communicator.getCommunicatorType().getTypeCode()));
        }
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        communicators = new HashMap<>();
        for (BaseCommunicator communicator : Arrays.asList(
                new SerialCommunicator(this),
                new UsbCommunicator(this),
                new BluetoothCommunicator(this),
                new WebServerCommunicator(this))) {
            communicators.put(communicator.getCommunicatorType(), communicator);
        }

        mPrefs = App.getInstance().getPrefs();
        initializeNotification();

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (action != null) {
                    switch (action) {
                        case UsbManager.ACTION_USB_DEVICE_DETACHED:
                            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                            ((UsbCommunicator) communicators.get(CommunicatorType.USB)).close(device);
                            break;

                        case BluetoothAdapter.ACTION_STATE_CHANGED:
                            final int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                                    BluetoothAdapter.ERROR);
                            if (BluetoothAdapter.STATE_TURNING_OFF == bluetoothState
                                    || BluetoothAdapter.ERROR == bluetoothState) {
                                communicators.get(CommunicatorType.BLUETOOTH).close();
                            }
                            break;

                        case App.ACTION_SEND_DATA:
                            Bundle extra = intent.getExtras();
                            if (extra != null && extra.containsKey("data")) {
                                String dataStr = String.valueOf(extra.get("data"));
                                String data = App.getInstance().compileFormulas(dataStr);

                                sendData(data);

                                if (extra.containsKey("id")) {
                                    sendActionComplete(String.valueOf(extra.get("id")));
                                }
                            }
                            break;
                    }
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(App.ACTION_SEND_DATA);
        intentFilter.addAction(App.ACTION_EXTERNAL_COMMAND);
        registerReceiver(mBroadcastReceiver, intentFilter);


        mLocalBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                App.log("CommunicationService-action" + action);
                if (action != null) {

                    switch (action) {
                        case App.LOCAL_ACTION_CONNECTION_ESTABLISHED:
                        case App.LOCAL_ACTION_CONNECTION_CLOSED:
                        case App.LOCAL_ACTION_CONNECTION_FAILED:
                            updateNotificationText();
                            break;

                        case App.LOCAL_ACTION_COMMAND_RECEIVED:
                            App.getInstance().detectCommand(intent.getStringExtra("command"));
                            break;

                        case App.LOCAL_ACTION_SEND_DATA:
                            sendData(intent.getStringExtra("data"));
                            break;

                        case App.LOCAL_ACTION_SETTINGS_UPDATED:
                            for (BaseCommunicator communicator : communicators.values()) {
                                communicator.openOrClose(false);
                            }
                            for (BaseCommunicator communicator : communicators.values()) {
                                communicator.openOrClose(
                                        isCommunicationTypeEnabled(communicator.getCommunicatorType().getTypeCode()));
                            }
                            updateNotificationText();
                            break;
                    }
                }
            }
        };
        IntentFilter localIntentFilter = new IntentFilter();
        localIntentFilter.addAction(App.LOCAL_ACTION_CONNECTION_ESTABLISHED);
        localIntentFilter.addAction(App.LOCAL_ACTION_CONNECTION_CLOSED);
        localIntentFilter.addAction(App.LOCAL_ACTION_CONNECTION_FAILED);
        localIntentFilter.addAction(App.LOCAL_ACTION_COMMAND_RECEIVED);
        localIntentFilter.addAction(App.LOCAL_ACTION_SETTINGS_UPDATED);
        localIntentFilter.addAction(App.LOCAL_ACTION_SEND_DATA);
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mLocalBroadcastManager.registerReceiver(mLocalBroadcastReceiver, localIntentFilter);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor sensorLight = mSensorManager != null ? mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) : null;
        if (sensorLight != null) {
            mSensorManager.registerListener(this, sensorLight, SensorManager.SENSOR_DELAY_NORMAL);
        }

        sendBroadcast(new Intent(App.ACTION_SERVICE_STARTED));

        updateNotificationText();
    }

    @Override
    public void onDestroy() {
        App.log("CommunicationService - destroy");

        for (BaseCommunicator communicator : communicators.values()) {
            communicator.openOrClose(false);
        }

        mSensorManager.unregisterListener(this);
        mSensorManager = null;

        unregisterReceiver(mBroadcastReceiver);
        mBroadcastReceiver = null;

        sendBroadcast(new Intent(App.ACTION_SERVICE_STOPPED));

        mLocalBroadcastManager.unregisterReceiver(mLocalBroadcastReceiver);
        mLocalBroadcastManager = null;
        mLocalBroadcastReceiver = null;

        super.onDestroy();
    }

    public LocalBroadcastManager getLocalBroadcastManager() {
        return mLocalBroadcastManager;
    }

    private boolean isCommunicationTypeEnabled(String type) {
        return App.getInstance().getBooleanPreference(type + "_communication_enabled");
    }


    private int getNotificationTextColor(int style) {
        TextView tempTextView = new TextView(this);
        tempTextView = Utils.setTextAppearence(tempTextView, style);
        return tempTextView.getCurrentTextColor();
    }

    private void initializeNotification() {
        mNotificationLayout = new RemoteViews(getPackageName(), R.layout.layout_notification);

        int infoTextColor = getNotificationTextColor(
                R.style.TextAppearance_Compat_Notification_Info);

        int titleTextColor = getNotificationTextColor(
                R.style.TextAppearance_Compat_Notification_Title);

        Bitmap appIcon = getNotificationInfoIcon(R.drawable.notification_icon, infoTextColor);
        mNotificationLayout.setImageViewBitmap(R.id.app_icon, appIcon);

        for (BaseCommunicator communicator : communicators.values()) {
            Bitmap icon = getNotificationInfoIcon(communicator.getCommunicatorType().getIconId(), titleTextColor);
            mNotificationLayout.setImageViewBitmap(communicator.getCommunicatorType().getImageViewId(), icon);
        }

        mNotificationBuilder = new Notification.Builder(this)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.notification_icon)
                .setContent(mNotificationLayout)
                .setContentIntent(
                        PendingIntent.getActivity(this, 0,
                                new Intent(this, MainActivity.class), 0));

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(
                    "serial.manager.v2", "notification_service", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(notificationChannel);
                mNotificationBuilder.setChannelId(notificationChannel.getId());
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= 16) {
            startForeground(NOTIFICATION_ID, mNotificationBuilder.build());
        } else {
            startForeground(NOTIFICATION_ID, mNotificationBuilder.getNotification());
        }
    }

    public void updateNotificationText() {
        App.log("CommunicationService-updateNotificationText");
        for (BaseCommunicator communicator : communicators.values()) {
            updateNotificationTextItem(communicator);
        }

        mNotificationBuilder.setContent(mNotificationLayout);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null && mNotificationBuilder != null) {
            notificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
        }
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

    private void sendData(String message) {
        if (App.getInstance().getBooleanPreference("crlf")) {
            message += App.CRLF;
        }

        for (BaseCommunicator communicator : communicators.values()) {
            communicator.write(message);
        }

        LocalBroadcastManager.getInstance(this)
                .sendBroadcast(new Intent(App.LOCAL_ACTION_DATA_SENT).putExtra("data", message));
    }

    private void sendActionComplete(String id) {
        Intent intent = new Intent(App.ACTION_SEND_DATA_COMPLETE);
        intent.putExtra("id", id);
        sendBroadcast(intent);
    }

    private int mLastLightSensorMode = 0;
    private long mLastLightSensorMillis = 0;

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float value = event.values[0];
            int mode = 0;

            if (value >= SensorManager.LIGHT_FULLMOON) {
                mode++;
            }
            if (value >= SensorManager.LIGHT_CLOUDY) {
                mode++;
            }
            if (value >= SensorManager.LIGHT_SUNRISE) {
                mode++;
            }
            if (value >= SensorManager.LIGHT_OVERCAST) {
                mode++;
            }
            if (value >= SensorManager.LIGHT_SHADE) {
                mode++;
            }
            if (value >= SensorManager.LIGHT_SUNLIGHT) {
                mode++;
            }
            if (value >= SensorManager.LIGHT_SUNLIGHT_MAX) {
                mode++;
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

    private void updateNotificationTextItem(BaseCommunicator communicator) {
        if (mNotificationLayout == null) {
            return;
        }

        String communicatorTypeCode = communicator.getCommunicatorType().getTypeCode();
        mNotificationLayout.setTextViewText(
                communicator.getCommunicatorType().getCountTextViewId(),
                String.valueOf(communicator.getConnectionsCount()));

        setNotificationInfoVisibility(communicatorTypeCode, isCommunicationTypeEnabled(communicatorTypeCode) ? View.VISIBLE : View.GONE);
        App.log(communicator.getCommunicatorType().getTypeCode() + "-" + (isCommunicationTypeEnabled(communicatorTypeCode) ? "VISIBLE" : "GONE"));

        if (CommunicatorType.WEB == communicator.getCommunicatorType()) {
            mNotificationLayout.setTextViewText(R.id.ip_address,
                    Utils.getIpAddress() + ":" + App.getInstance().getIntPreference("web_server_port",
                            getString(R.string.pref_default_web_server_port)));
            mNotificationLayout.setViewVisibility(R.id.ip_address, isCommunicationTypeEnabled(communicatorTypeCode) ? View.VISIBLE : View.GONE);
        }
    }

    public boolean isConnectionStateMessageEnabled() {
        return App.getInstance().getBooleanPreference("send_connection_state");
    }
}

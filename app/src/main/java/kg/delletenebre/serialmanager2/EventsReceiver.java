package kg.delletenebre.serialmanager2;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Handler;
import android.os.SystemClock;

import java.util.Locale;

public class EventsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();
        final App app = App.getInstance();
        SharedPreferences prefs = App.getInstance().getPrefs();
        Resources resources = app.getResources();
        final boolean debug = prefs.getBoolean("debugging",
                resources.getBoolean(R.bool.pref_default_debugging));
        boolean sendScreenState = prefs.getBoolean("send_screen_state",
                resources.getBoolean(R.bool.pref_default_send_screen_state));
        final boolean startOnBoot = prefs.getBoolean("start_on_boot",
                app.getResources().getBoolean(R.bool.pref_default_start_on_boot));
        final boolean startWhenScreenOn = prefs.getBoolean("start_when_screen_on",
                app.getResources().getBoolean(R.bool.pref_default_start_when_screen_on));
        final boolean stopWhenScreenOff = prefs.getBoolean("stop_when_screen_off",
                app.getResources().getBoolean(R.bool.pref_default_stop_when_screen_off));
        final int startOnBootDelay = app.getIntPreference("start_on_boot_delay") * 1000;
        Intent sendIntent = new Intent(App.ACTION_SEND_DATA);


        if (action != null) {
            switch (action) {
                case Intent.ACTION_BOOT_COMPLETED:
                    if (debug) {
                        App.log("Boot completed");
                    }
                    app.setBootedMillis(SystemClock.uptimeMillis());
                    if (startOnBoot) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (!stopWhenScreenOff || App.getInstance().isScreenOn()) {
                                    Intent startIntent = new Intent(App.getInstance(), CommunicationService.class);
                                    startIntent.putExtra(CommunicationService.EXTRA_UPDATE_USB_CONNECTION, true);
                                    startIntent.putExtra(CommunicationService.EXTRA_UPDATE_BLUETOOTH_CONNECTION, true);
                                    App.getInstance().startService(startIntent);
                                }
                            }
                        }, startOnBootDelay);
                    }
                    break;


                case Intent.ACTION_SCREEN_ON:
                    App.logStatus("Screen state", "ON");

                    if (sendScreenState) {
                        sendIntent.putExtra("data", String.format(Locale.getDefault(),
                                context.getString(R.string.send_data_to_controller_format),
                                "screen", "on"));
                        context.sendBroadcast(sendIntent);
                    }

                    if (startWhenScreenOn && (!startOnBoot
                            || SystemClock.uptimeMillis() > 150000
                            || SystemClock.uptimeMillis() - app.getBootedMillis() - startOnBootDelay > 0)) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (App.getInstance().isScreenOn()) {
                                    Intent startIntent = new Intent(App.getInstance(), CommunicationService.class);
                                    startIntent.putExtra(CommunicationService.EXTRA_UPDATE_USB_CONNECTION, true);
                                    startIntent.putExtra(CommunicationService.EXTRA_UPDATE_BLUETOOTH_CONNECTION, true);
                                    App.getInstance().startService(startIntent);
                                }
                            }
                        }, App.getInstance().getIntPreference("start_when_screen_on_delay") * 1000);
                    }
                    break;


                case Intent.ACTION_SCREEN_OFF:
                    App.logStatus("Screen state", "OFF");

                    if (sendScreenState) {
                        sendIntent.putExtra("data", String.format(Locale.getDefault(),
                                context.getString(R.string.send_data_to_controller_format),
                                "screen", "off"));
                        context.sendBroadcast(sendIntent);
                    }

                    if (stopWhenScreenOff) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (App.getInstance().isScreenOff()) {
                                    App.getInstance().stopService(new Intent(context, CommunicationService.class));
                                }
                            }
                        }, app.getIntPreference("stop_when_screen_off_delay") * 1000);
                    }
                    break;


                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    App.log("**** BluetoothAdapter.ACTION_STATE_CHANGED ****");

                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR);

                    Intent bluetoothIntent = new Intent(context, CommunicationService.class);

                    switch (state) {
                        case BluetoothAdapter.STATE_ON:
                            bluetoothIntent.putExtra(CommunicationService.EXTRA_BLUETOOTH_ENABLED, true);
                            context.startService(bluetoothIntent);
                            break;
                    }

                    break;
            }
        }
    }
}

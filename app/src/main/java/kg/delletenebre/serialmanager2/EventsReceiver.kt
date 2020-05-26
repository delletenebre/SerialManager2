package kg.delletenebre.serialmanager2

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.SystemClock
import java.util.*
import androidx.core.content.ContextCompat.startForegroundService
import android.os.Build



class EventsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val app = App.getInstance()
        val prefs = App.getInstance().prefs
        val resources = app.resources
        val debug = prefs.getBoolean("debugging",
                resources.getBoolean(R.bool.pref_default_debugging))
        val sendScreenState = prefs.getBoolean("send_screen_state",
                resources.getBoolean(R.bool.pref_default_send_screen_state))
        val startOnBoot = prefs.getBoolean("start_on_boot",
                app.resources.getBoolean(R.bool.pref_default_start_on_boot))
        val startWhenScreenOn = prefs.getBoolean("start_when_screen_on",
                app.resources.getBoolean(R.bool.pref_default_start_when_screen_on))
        val stopWhenScreenOff = prefs.getBoolean("stop_when_screen_off",
                app.resources.getBoolean(R.bool.pref_default_stop_when_screen_off))
        val startOnBootDelay = app.getIntPreference("start_on_boot_delay") * 1000
        val sendIntent = Intent(App.ACTION_SEND_DATA)


        if (action != null) {
            when (action) {
                Intent.ACTION_BOOT_COMPLETED -> {
                    if (debug) {
                        App.log("Boot completed")
                    }
                    app.bootedMillis = SystemClock.uptimeMillis()
                    if (startOnBoot) {
                        Handler().postDelayed({
                            if (!stopWhenScreenOff || App.getInstance().isScreenOn) {
                                val startIntent = Intent(App.getInstance(), CommunicationService::class.java)
                                startIntent.putExtra(CommunicationService.EXTRA_UPDATE_USB_CONNECTION, true)
                                startIntent.putExtra(CommunicationService.EXTRA_UPDATE_BLUETOOTH_CONNECTION, true)

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(startIntent)
                                } else {
                                    context.startService(startIntent)
                                }
                            }
                        }, startOnBootDelay.toLong())
                    }
                }


                Intent.ACTION_SCREEN_ON -> {
                    App.logStatus("Screen state", "ON")

                    if (sendScreenState) {
                        sendIntent.putExtra("data", String.format(Locale.getDefault(),
                                context.getString(R.string.send_data_to_controller_format),
                                "screen", "on"))
                        context.sendBroadcast(sendIntent)
                    }

                    if (startWhenScreenOn && (!startOnBoot
                                    || SystemClock.uptimeMillis() > 150000
                                    || SystemClock.uptimeMillis() - app.bootedMillis - startOnBootDelay.toLong() > 0)) {
                        Handler().postDelayed({
                            if (App.getInstance().isScreenOn) {
                                val startIntent = Intent(App.getInstance(), CommunicationService::class.java)
                                startIntent.putExtra(CommunicationService.EXTRA_UPDATE_USB_CONNECTION, true)
                                startIntent.putExtra(CommunicationService.EXTRA_UPDATE_BLUETOOTH_CONNECTION, true)

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(startIntent)
                                } else {
                                    context.startService(startIntent)
                                }
                            }
                        }, (App.getInstance().getIntPreference("start_when_screen_on_delay") * 1000).toLong())
                    }
                }


                Intent.ACTION_SCREEN_OFF -> {
                    App.logStatus("Screen state", "OFF")

                    if (sendScreenState) {
                        sendIntent.putExtra("data", String.format(Locale.getDefault(),
                                context.getString(R.string.send_data_to_controller_format),
                                "screen", "off"))
                        context.sendBroadcast(sendIntent)
                    }

                    if (stopWhenScreenOff) {
                        Handler().postDelayed({
                            if (App.getInstance().isScreenOff) {
                                App.getInstance().stopService(Intent(context, CommunicationService::class.java))
                            }
                        }, (app.getIntPreference("stop_when_screen_off_delay") * 1000).toLong())
                    }
                }


                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    App.log("**** BluetoothAdapter.ACTION_STATE_CHANGED ****")

                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR)

                    val bluetoothIntent = Intent(context, CommunicationService::class.java)

                    when (state) {
                        BluetoothAdapter.STATE_ON -> {
                            bluetoothIntent.putExtra(CommunicationService.EXTRA_BLUETOOTH_ENABLED, true)
                            context.startService(bluetoothIntent)
                        }
                    }
                }
            }
        }
    }
}
package kg.delletenebre.serialmanager2.bluetooth

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.os.Handler
import com.github.douglasjunior.bluetoothclassiclibrary.*
import com.github.douglasjunior.bluetoothlowenergylibrary.BluetoothLeService
import kg.delletenebre.serialmanager2.App
import kg.delletenebre.serialmanager2.CommunicationService
import kg.delletenebre.serialmanager2.R
import java.nio.charset.Charset
import java.util.*


class BluetoothConnection {
    var mBluetoothStatus: BluetoothStatus = BluetoothStatus.NONE
    var mBluetoothService: BluetoothService? = null

    fun enableAdapter() {
        BluetoothAdapter.getDefaultAdapter()?.enable()
    }

    fun isConnected(): Boolean {
        return mBluetoothStatus == BluetoothStatus.CONNECTED
    }

    fun start(communicationService: CommunicationService?) {
        disconnect()
        if (communicationService != null) {
            init(communicationService.applicationContext)

            mBluetoothService?.setOnEventCallback(object : BluetoothService.OnBluetoothEventCallback {
                override fun onDataRead(buffer: ByteArray, length: Int) {
                    val data = String(buffer, Charset.forName("UTF-8"))
                    App.log(String.format("bluetooth data read: %s", data))

                    communicationService.localBroadcastManager?.sendBroadcast(
                            Intent(App.LOCAL_ACTION_COMMAND_RECEIVED)
                                    .putExtra("from", "bluetooth")
                                    .putExtra("command", data))
                }

                override fun onStatusChange(status: BluetoothStatus) {
                    App.log(String.format("bluetooth status: %s", status.toString()))
                    mBluetoothStatus = status
                    communicationService?.updateNotificationText()

                    when (status) {
                        BluetoothStatus.CONNECTED -> {
                            if (communicationService?.isConnectionStateMessageEnabled) {
                                writeln(App.ACTION_CONNECTION_ESTABLISHED)
                            }
                            communicationService?.localBroadcastManager?.sendBroadcast(
                                    Intent(App.LOCAL_ACTION_CONNECTION_ESTABLISHED)
                                            .putExtra("type", "bluetooth")
                                            .putExtra("name", mBluetoothService?.configuration?.deviceName))
                        }
                        BluetoothStatus.CONNECTING -> {
                            // nothing
                        }
                        BluetoothStatus.NONE -> {
                            val delay = 2
                            App.log(String.format("bluetooth reconnect after %d seconds", delay))
                            Handler().postDelayed({ connect() }, delay.toLong() * 1000)

                        }
                    }
                }

                override fun onDeviceName(deviceName: String) {}

                override fun onToast(message: String) {}

                override fun onDataWrite(buffer: ByteArray) {
                    val data = String(buffer, Charset.forName("UTF-8"))
                    App.log(String.format("bluetooth data write: %s", data))
                }
            })

            connect()
        }
    }

    fun stop() {
        disconnect()
        mBluetoothService?.setOnEventCallback(null)
        mBluetoothService = null
    }

    private fun getWriter() : BluetoothWriter? {
        return if (mBluetoothService != null) {
            BluetoothWriter(mBluetoothService)
        } else {
            null
        }
    }

    fun write(message: String) {
        getWriter()?.write(message)
    }

    fun writeln(message: String) {
        getWriter()?.writeln(message)
    }

    private fun init(context: Context) {
        val config = BluetoothConfiguration()
        config.context = context
        config.bufferSize = 1024
        config.characterDelimiter = '\n'
        config.deviceName = context.getString(R.string.app_name)
        config.callListenersInMainThread = true
        // config.transport = BluetoothDevice.TRANSPORT_LE; // Only for dual-mode devices

        if (App.getInstance().getBooleanPreference("bluetooth_le_protocol")) {
            // Bluetooth Low Energy
            config.bluetoothServiceClass = BluetoothLeService::class.java
            config.uuidService = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
            config.uuidCharacteristic = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")

        } else {
            // Bluetooth Classic
            config.bluetoothServiceClass = BluetoothClassicService::class.java
            config.uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        }

        BluetoothService.init(config)
        mBluetoothService = BluetoothService.getDefaultInstance()
    }

    fun connect() {
        val address = App.getInstance().getStringPreference("bluetooth_device")
        if (!address.isNullOrEmpty()) {
            val device = BluetoothAdapter.getDefaultAdapter()?.getRemoteDevice(address)
            mBluetoothService?.connect(device)
        }
    }

    private fun disconnect() {
        mBluetoothService?.disconnect()
    }


}
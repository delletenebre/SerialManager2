package kg.delletenebre.serialmanager2

import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.support.v4.content.LocalBroadcastManager
import android.util.Log.d
import com.felhr.usbserial.UsbSerialDevice
import java.io.ByteArrayOutputStream
import java.util.*



class UsbConnection(context: Context, baudRate: Int) {
    companion object {
        val sConnectedDevices = HashMap<String, UsbSerialDevice>()
        val sBuffers = HashMap<String, ByteArrayOutputStream>()
    }
    private var mUsbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var mLocalBroadcastManager: LocalBroadcastManager = LocalBroadcastManager.getInstance(context)
    private var mBaudRate: Int = baudRate

    fun findConnectedDevices() {
        for (device in mUsbManager.deviceList.values) {
            if (mUsbManager.hasPermission(device) && !sConnectedDevices.containsKey(device.deviceName)) {
                if (open(device)) {
                    val path = device.deviceName
                    val vid = device.vendorId.toString()
                    val pid = device.productId.toString()
                    d("USB Connection", "Connected to USB-device: $path | VID:$vid | PID: $pid")
                }
            }
        }
    }

    private fun open(device: UsbDevice): Boolean {
        val connection = mUsbManager.openDevice(device)
        val serialDevice: UsbSerialDevice

        try {
            serialDevice = UsbSerialDevice.createUsbSerialDevice(device, connection)
        } catch (e: Exception) {
            return false
        }

        if (serialDevice.open()) {
            serialDevice.setBaudRate(mBaudRate)
//            serialDevice.setDataBits(UsbSerialInterface.DATA_BITS_8)
//            serialDevice.setStopBits(UsbSerialInterface.STOP_BITS_1)
//            serialDevice.setParity(UsbSerialInterface.PARITY_NONE)

            val deviceName = device.deviceName
            sConnectedDevices.put(deviceName, serialDevice)
            sBuffers.put(deviceName, ByteArrayOutputStream())
            serialDevice.read { bytes ->
                if (bytes.isNotEmpty()) {
                    val buffer = sBuffers[deviceName]
                    if (buffer != null) {
                        buffer.write(bytes)
                        if (buffer.toByteArray().contains(0x0A)) {
                            val data = buffer.toString("UTF-8")
                            val dataParts = data.split("\n".toRegex(), 2)
                            val command = dataParts[0]
                                    .replace("\r", "")
                            mLocalBroadcastManager.sendBroadcast(
                                    Intent(App.LOCAL_ACTION_COMMAND_RECEIVED)
                                            .putExtra("from", "usb")
                                            .putExtra("command", command))
                            buffer.reset()
                            buffer.write(dataParts[1].toByteArray(Charsets.UTF_8))
                        }
                    }
                }
            }

            // Some Arduinos would need some sleep because firmware wait some time to know whether
            // a new sketch is going to be uploaded or not
            Thread.sleep(2000)

            if (App.getInstance().getBooleanPreference("send_connection_state")) {
                serialDevice.write((App.ACTION_CONNECTION_ESTABLISHED + "\n").toByteArray())
            }
            mLocalBroadcastManager.sendBroadcast(
                    Intent(App.LOCAL_ACTION_CONNECTION_ESTABLISHED)
                            .putExtra("type", "usb")
                            .putExtra("name", deviceName))
            return true
        }

        return false
    }

    fun send(message: String) {
        sConnectedDevices.entries
                .map { it.value }
                .forEach { it.write(message.toByteArray()) }
    }

    fun close(deviceName: String) {
        if (sConnectedDevices.containsKey(deviceName)) {
            sConnectedDevices[deviceName]?.close()
            sConnectedDevices.remove(deviceName)
            sBuffers.remove(deviceName)
            mLocalBroadcastManager.sendBroadcast(
                    Intent(App.LOCAL_ACTION_CONNECTION_CLOSED)
                            .putExtra("type", "usb")
                            .putExtra("name", deviceName))
        }
    }

    fun closeAll() {
        if (!sConnectedDevices.isEmpty()) {
            sConnectedDevices.values.forEach { it.close() }
            sConnectedDevices.clear()
            sBuffers.clear()
        }
    }

    fun hasOpened(): Boolean {
        return !sConnectedDevices.isEmpty()
    }

    fun count(): Int {
        return sConnectedDevices.count()
    }
}
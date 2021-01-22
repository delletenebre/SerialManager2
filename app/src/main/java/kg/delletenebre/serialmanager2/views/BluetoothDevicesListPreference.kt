package kg.delletenebre.serialmanager2.views

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.preference.ListPreference
import android.util.AttributeSet
import kg.delletenebre.serialmanager2.App
import kg.delletenebre.serialmanager2.R
import java.util.*


class BluetoothDevicesListPreference : ListPreference {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        val entriesList = ArrayList<CharSequence>()
        val entryValuesList = ArrayList<CharSequence>()

        val resources = context.resources
        entriesList.add(resources.getString(R.string.no_device))
        entryValuesList.add("")

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val pairedDevices = bluetoothAdapter?.bondedDevices
        if (pairedDevices != null) {
            for (device in pairedDevices) {
                val majorClass = device.bluetoothClass.majorDeviceClass
                val name = device.name
                val address = device.address
                App.log(String.format("bluetooth device [ %s (%s)] major class: %d",
                        name, address, majorClass))

                //if (majorClass == BluetoothClass.Device.Major.UNCATEGORIZED) {
                    entriesList.add(String.format(
                            resources.getString(R.string.pref_template_bluetooth_device_list_item),
                            name, address))
                    entryValuesList.add(address)
                //}
            }
        }

        entries = entriesList.toTypedArray()
        entryValues = entryValuesList.toTypedArray()
    }

}

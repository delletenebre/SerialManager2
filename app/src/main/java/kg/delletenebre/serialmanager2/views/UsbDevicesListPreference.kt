package kg.delletenebre.serialmanager2.views

import android.content.Context
import android.hardware.usb.UsbManager
import android.preference.MultiSelectListPreference
import android.util.AttributeSet
import kg.delletenebre.serialmanager2.R
import java.util.*


class UsbDevicesListPreference : MultiSelectListPreference {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        val entriesList = ArrayList<CharSequence>()
        val entryValuesList = ArrayList<CharSequence>()

        val resources = context.resources
        entriesList.add(resources.getString(R.string.no_device))
        entryValuesList.add("")

        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        //usb serial interfaceClass == 255 ?
        for (device in usbManager.deviceList.values.filter { device -> device.getInterface(0).interfaceClass == 255 }) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                entriesList.add(String.format(
                        resources.getString(R.string.pref_template_usb_device_list_item),
                        device.deviceName,
                        device.productId,
                        device.vendorId,
                        if (device.productName != null) device.productName else "",
                        if (device.manufacturerName != null) device.manufacturerName else ""))
            } else {
                entriesList.add(String.format(
                        resources.getString(R.string.pref_template_usb_device_list_item_old),
                        device.deviceName,
                        device.productId,
                        device.vendorId))
            }
            //devicePath:pid:vid
            val entryValue = StringBuilder()
                    .append(device.deviceName)
                    .append(":")
                    .append(device.productId)
                    .append(":")
                    .append(device.vendorId)
            entryValuesList.add(entryValue.toString())
        }

        entries = entriesList.toTypedArray()
        entryValues = entryValuesList.toTypedArray()
    }

    override fun setValues(values: MutableSet<String>?) {
        val newValues: MutableSet<String> = HashSet()
        newValues.addAll(values!!)
        super.setValues(values)
    }
}

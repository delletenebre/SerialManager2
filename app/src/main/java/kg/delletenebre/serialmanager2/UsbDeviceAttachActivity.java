package kg.delletenebre.serialmanager2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;

import kg.delletenebre.serialmanager2.communication.CommunicationService;

public class UsbDeviceAttachActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        Intent intent = new Intent(getApplicationContext(), CommunicationService.class);

        UsbDevice usbDevice = getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (usbDevice != null && usbManager != null && usbManager.hasPermission(usbDevice)) {
            intent.putExtra(CommunicationService.EXTRA_UPDATE_USB_CONNECTION, true);
        }

//        UsbAccessory usbAccessory = getIntent().getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
//        if (usbAccessory != null && usbManager.hasPermission(usbAccessory)) {
//            intent.putExtra(CommunicationService.ACTION_TYPE,
//                    CommunicationService.ACTION_USB_ACCESSORY);
//        }

        startService(intent);

        finish();
    }
}

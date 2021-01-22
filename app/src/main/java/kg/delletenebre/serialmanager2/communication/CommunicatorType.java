package kg.delletenebre.serialmanager2.communication;

import kg.delletenebre.serialmanager2.R;

public enum CommunicatorType {
    USB("usb", R.id.usb_connections_count, R.id.usb_connections_icon, R.drawable.ic_usb),
    SERIAL("serial", R.id.serial_connections_count, R.id.serial_connections_icon, R.drawable.ic_vga),
    WEB("web_socket", R.id.web_socket_connections_count, R.id.web_socket_connections_icon, R.drawable.ic_language_black_24dp),
    BLUETOOTH("bluetooth", R.id.bluetooth_connections_count, R.id.bluetooth_connections_icon, R.drawable.ic_bluetooth_black_24dp);

    private final String typeCode;
    private final int countTextViewId;
    private final int imageViewId;
    private final int iconId;

    CommunicatorType(String typeCode, int countTextViewId, int imageViewId, int iconId) {
        this.typeCode = typeCode;
        this.countTextViewId = countTextViewId;
        this.imageViewId = imageViewId;
        this.iconId = iconId;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public int getCountTextViewId() {
        return countTextViewId;
    }

    public int getImageViewId() {
        return imageViewId;
    }

    public int getIconId() {
        return iconId;
    }
}

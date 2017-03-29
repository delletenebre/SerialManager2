package kg.delletenebre.serialmanager2.utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

import kg.delletenebre.serialmanager2.App;

public class VirtualKeyboard {
    static {
        System.loadLibrary("virtual-keyboard");
    }
    private native int create(int[] codes);
    private native void destroy(int fd);
    private native void sendEvent(int fd, int code);
    private native void sendEvents(int fd, int code1, int code2);
    private native void sendKeyDown(int fd, int code);
    private native void sendKeyUp(int fd, int code);

    private int mFileDescriptor;
    private JSONArray mKeymap;
    private long mKeyAltTabTabTimer = 0;

    public VirtualKeyboard() {
        try {
            mKeymap = new JSONArray(Utils.getAssetsFileContent("virtual-keyboard-codes.json"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean createKeyboard() {
        mFileDescriptor = create(getLinuxKeyCodes());
        return mFileDescriptor > 0;
    }

    public void destroyKeyboard() {
        if (mFileDescriptor > 0) {
            destroy(mFileDescriptor);
        }
    }

    public void sendKeyPress(int keycode) {
        if (mFileDescriptor > 0) {
            sendEvent(mFileDescriptor, keycode);
        }
    }

    private JSONArray getKeymap() {
        return mKeymap;
    }

    public String[] getNames() {
        int size = mKeymap.length();
        String[] names = new String[size];

        try {
            for (int i = 0; i < size; i++) {
                names[i] = mKeymap.getJSONObject(i).getString("name");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return names;
    }

    private int[] getLinuxKeyCodes() {
        int size = mKeymap.length();
        int[] linuxCodes = new int[size];

        try {
            for (int i = 0; i < size; i++) {
                linuxCodes[i] = mKeymap.getJSONObject(i).getInt("linuxCode");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return linuxCodes;
    }

    public String getNameById(int id) {
        try {
            return mKeymap.getJSONObject(id).getString("name");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "[ null ]";
    }

    public void emulateKey(final int keyId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject key = mKeymap.getJSONObject(keyId);
                    String name = key.getString("name");
                    int linuxCode = key.getInt("linuxCode");
                    int androidCode = key.getInt("androidCode");

                    if (linuxCode > 0) {
                        // Emulated key in fast mode
                        sendEvent(mFileDescriptor, linuxCode);
                    } else if (androidCode > 0) {
                        // Emulated key in slow mode
                        try {
                            Runtime.getRuntime().exec("input keyevent " + androidCode);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (linuxCode == -2) {
                        if (name.equals("Alt + Tab")) {
                            sendKeyDown(mFileDescriptor, 56);
                            sendKeyDown(mFileDescriptor, 15);
                            sendKeyUp(mFileDescriptor, 15);
                            sendKeyUp(mFileDescriptor, 56);
                        } else if (name.equals("Alt + Tab .. Tab ...")) {
                            if (System.currentTimeMillis() - mKeyAltTabTabTimer >= 1500) {
                                sendKeyDown(mFileDescriptor, 56);
                            }
                            sendKeyDown(mFileDescriptor, 15);
                            sendKeyUp(mFileDescriptor, 15);
                            mKeyAltTabTabTimer = System.currentTimeMillis();
                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    try {
                                        if (System.currentTimeMillis() - mKeyAltTabTabTimer >= 1500) {
                                            sendKeyUp(mFileDescriptor, 56);
                                        }
                                    } catch(Exception e) {
                                        App.log(e.getLocalizedMessage());
                                    }
                                }
                            }, 1500);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}

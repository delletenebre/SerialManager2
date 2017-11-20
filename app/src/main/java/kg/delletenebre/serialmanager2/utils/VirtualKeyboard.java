package kg.delletenebre.serialmanager2.utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
        final String path = "/system/usr/keylayout/";
        final String name = "uinput-serialmanager.kl";
        final String filePath = path + name;

        Process suProcess;
        DataOutputStream os;
        int sleepTime = 50;
        try {
            //Get Root
            suProcess = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(suProcess.getOutputStream());

            //Remount writable FS within the root process
            os.writeBytes("mount -o rw,remount,rw /system\n");
            os.flush();
            Thread.sleep(sleepTime);

            os.writeBytes("[ -fe " + filePath + " ] && rm " + filePath + "\n");
            os.flush();
            Thread.sleep(sleepTime);

            os.writeBytes("touch " + filePath + "\n");
            os.flush();
            Thread.sleep(sleepTime);

            try {
                InputStream file = App.getInstance().getAssets().open(name);
                BufferedReader reader = new BufferedReader(new InputStreamReader(file));
                String line = reader.readLine();
                while (line != null) {
                    os.writeBytes("echo '" + line + "' >> " + filePath + "\n");
                    os.flush();
                    Thread.sleep(5);
                    line = reader.readLine();
                }

                file.close();
                reader.close();
            } catch(IOException ioe) {
                App.logError(ioe.getLocalizedMessage());
            }

            os.writeBytes("chmod 644 " + filePath + "\n");
            os.flush();
            Thread.sleep(sleepTime);

            //Remount Read-Only
            os.writeBytes("mount -o ro,remount,ro /system\n");
            os.flush();
            Thread.sleep(sleepTime);

            //End process
            os.writeBytes("exit\n");
            os.flush();
            Thread.sleep(sleepTime);

            os.close();
            suProcess.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }

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

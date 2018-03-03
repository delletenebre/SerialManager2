package kg.delletenebre.serialmanager2;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.udojava.evalex.Expression;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.fabric.sdk.android.Fabric;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import kg.delletenebre.serialmanager2.commands.CommandModel;
import kg.delletenebre.serialmanager2.utils.RealmMigration;
import kg.delletenebre.serialmanager2.utils.Utils;
import kg.delletenebre.serialmanager2.utils.VirtualKeyboard;
import kg.delletenebre.serialmanager2.views.AppChooserView;


public class App extends Application implements Application.ActivityLifecycleCallbacks {
    private static App sSelf;
    public static App getInstance() {
        return sSelf;
    }

    public static final String TAG = "kg.serial.manager";
    public static final String LOCAL_ACTION_COMMAND_RECEIVED = "local.command_received";
    public static final String LOCAL_ACTION_COMMAND_DETECTED = "local.command_detected";
    public static final String LOCAL_ACTION_SEND_DATA = "local.send_data";
    public static final String LOCAL_ACTION_DATA_SENT = "local.data_sent";
    public static final String LOCAL_ACTION_SETTINGS_UPDATED = "local.settings_updated";
    public static final String LOCAL_ACTION_CONNECTION_ESTABLISHED = "local.connection_established";
    public static final String LOCAL_ACTION_CONNECTION_CLOSED = "local.connection_closed";



    public static final String ACTION_COMMAND_RECEIVED = "kg.serial.manager.command_received";
    public static final String ACTION_SEND_DATA = "kg.serial.manager.send";
    public static final String ACTION_SEND_DATA_COMPLETE = "kg.serial.manager.send.complete";
    public static final String ACTION_EXTERNAL_COMMAND = "kg.serial.manager.new_command";
    public static final String ACTION_APP_STARTED = "kg.serial.manager.app_started";
    public static final String ACTION_SERVICE_STARTED = "kg.serial.manager.started";
    public static final String ACTION_SERVICE_STOPPED = "kg.serial.manager.stopped";
    public static final String ACTION_CONNECTION_ESTABLISHED = "kg.serial.manager.connection_established";
    public static final String ACTION_CONNECTION_LOST = "kg.serial.manager.connection_lost";

    public static final String EXTRA_APPWIDGET_EDIT = "kg.serial.manager.extra.appWidgetEdit";
    public static final String EXTRA_APPWIDGET_KEY = "kg.serial.manager.extra.appWidgetKey";
    public static final String EXTRA_APPWIDGET_VALUE = "kg.serial.manager.extra.appWidgetValue";
    public static final String EXTRA_APPWIDGET_ACTION_ID = "kg.serial.manager.extra.appWidgetActionId";
    public static final String EXTRA_SELECTED_ACTION_CHOSEN_APP = "kg.serial.manager.extra.selected_action.chosen_app";
    public static final String EXTRA_SELECTED_ACTION_EMULATE_KEY = "kg.serial.manager.extra.selected_action.emulateed_key_id";
    public static final String EXTRA_SELECTED_ACTION_SHELL_COMMAND = "kg.serial.manager.extra.selected_action.shell_command";
    public static final String EXTRA_SELECTED_ACTION_SEND_DATA = "kg.serial.manager.extra.selected_action.send_data";

    public static final Map<String, String> ICONS;
    static
    {
        ICONS = new HashMap<>();
        ICONS.put("controller", "\ue800");
        ICONS.put("usb", "\ue801");
        ICONS.put("bluetooth", "\ue802");
        ICONS.put("web", "\ue803");
        ICONS.put("receive", "\ue804");
        ICONS.put("send", "\ue805");
        ICONS.put("ok", "\ue806");
        ICONS.put("cancel", "\ue807");
    }

    public final static char CR  = (char) 0x0D;
    public final static char LF  = (char) 0x0A;
    public final static String CRLF  = "" + CR + LF;

    private static boolean sDebugEnabled = false;
    public static boolean isDebugEnabled() {
        return sDebugEnabled;
    }
    public static void setDebugEnabled(boolean debugEnabled) {
        sDebugEnabled = debugEnabled;
    }
    public static void log(String message) {
        if (isDebugEnabled()) {
            Log.d(TAG, message);
        }
    }
    public static void logError(String message) {
        if (isDebugEnabled()) {
            Log.e(TAG, message);
        }
    }
    public static void logStatus(String message, String state) {
        if (isDebugEnabled()) {
            Log.d(TAG, message + " ........ " + state);
        }
    }

    private Realm mRealm;
    private RealmConfiguration mRealmConfig;
    private SharedPreferences mPrefs;
    private WindowManager mWindowManager;
    private VirtualKeyboard mVirtualKeyboard;
    private AudioManager mAudioManager;
    private boolean mIsFullscreen = false;
    private boolean mVisibleActivityStatus = false;
    private long mBootedMillis;
    private int mHelperOverlayFirstHeight = 0;


    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        sSelf = this;
        registerActivityLifecycleCallbacks(this);
        Realm.init(this);

        setBootedMillis(android.os.SystemClock.uptimeMillis());

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mRealmConfig = new RealmConfiguration.Builder()
                .schemaVersion(7)
                .migration(new RealmMigration())
                //.deleteRealmIfMigrationNeeded()
                .build();
        mRealm = getNewRealmInstance();

        setDebugEnabled(getBooleanPreference("debugging"));

        if (isSystemOverlaysPermissionGranted()) {
            createHelperOverlay();
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(new EventsReceiver(), intentFilter);

        mVirtualKeyboard = new VirtualKeyboard();
        if (mVirtualKeyboard.createKeyboard()) {
            logStatus("VirtualKeyboard", "OK");
        } else {
            logStatus("VirtualKeyboard", "FAIL");
        }

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        sendBroadcast(new Intent(App.ACTION_APP_STARTED));
    }

    public Realm getNewRealmInstance() {
        return Realm.getInstance(mRealmConfig);
    }

    public SharedPreferences getPrefs() {
        return mPrefs;
    }
    public Realm getRealm() {
        if (mRealm.isClosed()) {
            mRealm = getNewRealmInstance();
        }

        return mRealm;
    }

    private void createHelperOverlay() {
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            params.type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
        }
        params.gravity = Gravity.END | Gravity.TOP;
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        params.width = 1;
        params.height = WindowManager.LayoutParams.MATCH_PARENT;
        params.format = PixelFormat.TRANSPARENT;
        final View helperOverlay = new View(this);

        mWindowManager.addView(helperOverlay, params);
        final ViewTreeObserver vto = helperOverlay.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (mHelperOverlayFirstHeight == 0) {
                    mHelperOverlayFirstHeight = helperOverlay.getHeight();
                }
                if (mWindowManager != null) {
                    DisplayMetrics metrics = new DisplayMetrics();
                    mWindowManager.getDefaultDisplay().getMetrics(metrics);
                    mIsFullscreen = helperOverlay.getHeight() != mHelperOverlayFirstHeight;

                    log("helperOverlay.getHeight() = " + helperOverlay.getHeight());
                    log("mHelperOverlayFirstHeight  = " + mHelperOverlayFirstHeight);
                    logStatus("Fullscreen", String.valueOf(mIsFullscreen));
                }
            }
        });
    }


    Activity mVisibleActivity;
    public boolean isActivityVisible() {
        return mVisibleActivityStatus;
    }
    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {}
    @Override
    public void onActivityStarted(Activity activity) {}
    @Override
    public void onActivityResumed(Activity activity) {
        mVisibleActivityStatus = true;
        mVisibleActivity = activity;
    }
    @Override
    public void onActivityPaused(Activity activity) {
        mVisibleActivityStatus = false;
        mVisibleActivity = null;
    }
    @Override
    public void onActivityStopped(Activity activity) {}
    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {}
    @Override
    public void onActivityDestroyed(Activity activity) {}

    public long getBootedMillis() {
        return mBootedMillis;
    }
    public void setBootedMillis(long bootedMillis) {
        mBootedMillis = bootedMillis;
    }

    public String getVersion() {
        PackageInfo packageInfo;
        String version = "undefined";
        try {
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return version;
    }

    public VirtualKeyboard getVirtualKeyboard() {
        return mVirtualKeyboard;
    }

    public void detectCommand(String incomingString) {
        log("Receive " + incomingString);
        Pattern pattern = Pattern.compile(getStringPreference("command_format_regex"));
        Matcher matcher = pattern.matcher(incomingString);
        if (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2);

            Intent detectedCommandIntent = new Intent(LOCAL_ACTION_COMMAND_DETECTED);
            detectedCommandIntent.putExtra("key", key);
            detectedCommandIntent.putExtra("value", value);
            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
            localBroadcastManager.sendBroadcast(detectedCommandIntent);

            if (isActivityVisible() && !getBooleanPreference("always_execute_command_action")) {
                showSnackbar(mVisibleActivity, incomingString);

                Intent receivedCommandIntent = new Intent(ACTION_COMMAND_RECEIVED);
                receivedCommandIntent.putExtra("key", key);
                receivedCommandIntent.putExtra("value", value);
                sendBroadcast(receivedCommandIntent);


            } else {
                String intentValue = value;

                Realm realm = getNewRealmInstance();
                RealmResults<CommandModel> commands = realm.where(CommandModel.class)
                        .equalTo("key", key)
                        .findAll();

                for (CommandModel command : commands) {
                    boolean inRange = false;
                    if (Utils.isNumber(command.getValue()) && Utils.isNumber(value)) {
                        float commandValue = Float.parseFloat(command.getValue());
                        float receivedValue = Float.parseFloat(value);

                        inRange = commandValue - command.getScatter() <= receivedValue
                                && receivedValue <= commandValue + command.getScatter();
                    }

                    if (command.getValue().isEmpty() ||
                            (command.getValue().equals(value) || inRange)) {
                        if (!command.getNotyMessage().isEmpty()) {
                            if (!mIsFullscreen) {
                                String text = compileFormulas(
                                        replaceKeywords(command.getNotyMessage(), key, value));

                                new NotyOverlay(this, command.getPositionZ())
                                    .setPosition(command.getPositionX(), command.getPositionY(),
                                        command.getOffsetX(), command.getOffsetY())
                                    .setTextSize(command.getNotyTextSize())
                                    .setTextColor(Color.parseColor(command.getNotyTextColor()))
                                    .setBackgroundColor(Color.parseColor(command.getNotyBackgroundColor()))
                                    .show(text, command.getNotyDurationInMillis());
                            }
                        }

                        executeCommandAction(command.getActionId(), command.getChosenApp(),
                                command.getEmulatedKeyId(), command.getShellCommand(),
                                command.getSendData(), key, value);

                        intentValue = replaceKeywords(command.getIntentValueExtra(), key, value);
                    }
                }

                if (!intentValue.isEmpty()) {
                    Intent intent = new Intent(ACTION_COMMAND_RECEIVED);
                    intent.putExtra("key", key);
                    intent.putExtra("value", intentValue);
                    sendBroadcast(intent);
                }

                realm.close();
            }
        }
    }

    public int getIntPreference(String key, String defaultValue) {
        int result = Integer.parseInt(defaultValue);
        try {
            result = Integer.parseInt(mPrefs.getString(key, defaultValue));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return result;
    }

    public int getIntPreference(String key) {
        return getIntPreference(key,
                getString(Utils.getStringIdentifier(this, "pref_default_" + key)));
    }


    public boolean getBooleanPreference(String key, boolean defaultValue) {
        return mPrefs.getBoolean(key, defaultValue);
    }

    public boolean getBooleanPreference(String key) {
        return getBooleanPreference(key,
                getResources().getBoolean(Utils.getBooleanIdentifier(this, "pref_default_" + key)));
    }

    public String getStringPreference(String key, String defaultValue) {
        return mPrefs.getString(key, defaultValue);
    }

    public String getStringPreference(String key) {
        return getStringPreference(key,
                getResources().getString(Utils.getStringIdentifier(this, "pref_default_" + key)));
    }

    public boolean isScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            DisplayManager dm = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
            boolean screenOn = false;
            for (Display display : dm.getDisplays()) {
                if (display.getState() != Display.STATE_OFF) {
                    screenOn = true;
                }
            }
            return screenOn;
        } else {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            return powerManager.isScreenOn();
        }
    }
    public boolean isScreenOff() {
        return !isScreenOn();
    }
    public int getScreenBrightness() {
        if (isScreenBrightnessModeManual()) {
            return Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, -1);
        }
        return -1;
    }
    public void setScreenBrightness(Object value) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(this)) {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return;
            }
        }

        if (!isScreenBrightnessModeManual()) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        }

        boolean isPercent = false;
        float level;
        String stringValue = String.valueOf(value);

        if (!stringValue.isEmpty()) {
            if (stringValue.charAt(stringValue.length() - 1) == '%') {
                stringValue = stringValue.substring(0, stringValue.length() - 1);
                isPercent = true;
            }

            try {
                level = Float.parseFloat(stringValue);
            } catch (Exception ex) {
                logError("Error while setting brightness level: " + ex.getLocalizedMessage());
                return;
            }

            if (isPercent) {
                level = 255 / 100.0f * level;
            }

            if (level > 255) {
                level = 255;
            } else if (level < 5) {
                level = 5;
            }

            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, (int) level);
            log("Screen brightness set to " + (int) level);
        }
    }
    public boolean isScreenBrightnessModeManual() {
        return (Settings.System.getInt(getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
                == Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
    }


    public String replaceKeywords(String str, String key, String value) {
        if (str != null) {
            return str.replaceAll("(%key)", key)
                    .replaceAll("(%k)", key)
                    .replaceAll("(%value)", value)
                    .replaceAll("(%v)", value);
        }

        return "";
    }

    public String compileFormulas(String str) {
        StringBuffer stringBuffer;
        Pattern pattern;
        Matcher matcher;

        for (int i = 0; i < 3; i++) {
            // **** hex2dec **** //
            pattern = Pattern.compile("hex2dec\\(([x0-9a-f]+?)\\)", Pattern.CASE_INSENSITIVE);
            matcher = pattern.matcher(str);
            stringBuffer = new StringBuffer();
            while (matcher.find()) {
                try {
                    matcher.appendReplacement(stringBuffer,
                            String.valueOf(Long.parseLong(matcher.group(1).toLowerCase().replaceAll("0x", ""), 16)));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
            matcher.appendTail(stringBuffer);
            str = stringBuffer.toString();


            // **** dec2hex **** //
            pattern = Pattern.compile("dec2hex\\((\\d+?)\\)", Pattern.CASE_INSENSITIVE);
            matcher = pattern.matcher(str);
            stringBuffer = new StringBuffer();
            while (matcher.find()) {
                try {
                    matcher.appendReplacement(stringBuffer, Long.toHexString(Long.valueOf(matcher.group(1))));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
            matcher.appendTail(stringBuffer);
            str = stringBuffer.toString();


            // **** bin2dec **** //
            pattern = Pattern.compile("bin2dec\\(([01]+?)\\)");
            matcher = pattern.matcher(str);
            stringBuffer = new StringBuffer();
            while (matcher.find()) {
                try {
                    matcher.appendReplacement(stringBuffer,
                            String.valueOf(Long.parseLong(matcher.group(1), 2)));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
            matcher.appendTail(stringBuffer);
            str = stringBuffer.toString();


            // **** dec2bin **** //
            pattern = Pattern.compile("dec2bin\\((\\d+?)\\)");
            matcher = pattern.matcher(str);
            stringBuffer = new StringBuffer();
            while (matcher.find()) {
                try {
                    matcher.appendReplacement(stringBuffer,
                            Long.toBinaryString(Long.valueOf(matcher.group(1))));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
            matcher.appendTail(stringBuffer);
            str = stringBuffer.toString();
        }


        // **** EvalEx **** //
        pattern = Pattern.compile("%\\{(.+?)\\}");
        matcher = pattern.matcher(str);
        stringBuffer = new StringBuffer();
        while (matcher.find()) {
            try {
                matcher.appendReplacement(stringBuffer,
                        String.valueOf(new Expression(matcher.group(1)).eval()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        matcher.appendTail(stringBuffer);

        return stringBuffer.toString();
    }


    public void emulateMediaButton(int buttonCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && mAudioManager != null) {
            mAudioManager.dispatchMediaKeyEvent(
                    new KeyEvent(KeyEvent.ACTION_DOWN, buttonCode));
            mAudioManager.dispatchMediaKeyEvent(
                    new KeyEvent(KeyEvent.ACTION_UP, buttonCode));
        } else {
            Intent downIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            downIntent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, buttonCode));
            sendBroadcast(downIntent);

            Intent upIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            upIntent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, buttonCode));
            sendBroadcast(upIntent);
        }
    }



    public void showSnackbar(Activity activity, String message, int length, Integer textColor) {
        if (activity != null) {
            Snackbar snackbar = Snackbar.make(
                    activity.findViewById(android.R.id.content),
                    message,
                    length);

            if (textColor != null) {
                View snackbarView = snackbar.getView();
                TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
                textView.setTextColor(textColor);
            }

            snackbar.show();
        }
    }
    public void showSnackbar(Activity activity, String message, int length) {
        showSnackbar(activity, message, length, null);
    }
    public void showSnackbar(Activity activity, String message) {
        showSnackbar(activity, message, Snackbar.LENGTH_LONG, null);
    }
    public void showSnackbarSuccess(Activity activity, String message) {
        showSnackbar(activity, message, Snackbar.LENGTH_LONG, Color.parseColor("#4CAF50"));
    }
    public void showSnackbarError(Activity activity, String message) {
        showSnackbar(activity, message, Snackbar.LENGTH_LONG, Color.parseColor("#F44336"));
    }



    public boolean isSystemOverlaysPermissionGranted() {
        return (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this));
    }

    public void requestSystemOverlaysPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK
                    | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            startActivity(intent);
        }

    }


    public void executeCommandAction(int actionId, String chosenApp, int emulatedKeyId,
                                     String shellCommand, String sendData,
                                     String key, String value) {
        switch (actionId) {
            case CommandModel.ACTION_RUN_APPLICATION: {
                Intent intent = AppChooserView.getIntentValue(chosenApp, null);
                if (intent == null) {
//                    Toaster.toast(getString(R.string.app_chooser_toast_app_not_found,
//                            AppChooserView.getLabelByValue(this, chosenApp)));
                } else {
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
                break;
            }

            case CommandModel.ACTION_EMULATE_KEY: {
                if (mVirtualKeyboard != null) {
                    mVirtualKeyboard.emulateKey(emulatedKeyId);
                }
                break;
            }

            case CommandModel.ACTION_SHELL_COMMAND: {
                try {
                    Runtime.getRuntime().exec(shellCommand);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }

            case CommandModel.ACTION_SEND_DATA: {
                Intent intent = new Intent(ACTION_SEND_DATA);
                intent.putExtra("data", App.getInstance().replaceKeywords(sendData, key, value));
                sendBroadcast(intent);
                break;
            }
        }
    }
}

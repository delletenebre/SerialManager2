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
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.TextView;

import com.udojava.evalex.Expression;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import kg.delletenebre.serialmanager2.commands.Command;
import kg.delletenebre.serialmanager2.commands.Migration;
import kg.delletenebre.serialmanager2.utils.Utils;
import kg.delletenebre.serialmanager2.utils.VirtualKeyboard;
import kg.delletenebre.serialmanager2.views.AppChooserView;
import xdroid.toaster.Toaster;

@ReportsCrashes(
        mailTo = "delletenebre@gmail.com",
        formUri = "https://collector.tracepot.com/f61f59df",
        customReportContent = {
                ReportField.USER_COMMENT,
                ReportField.APP_VERSION_CODE,
                ReportField.APP_VERSION_NAME,
                ReportField.ANDROID_VERSION,
                ReportField.PHONE_MODEL,
                ReportField.STACK_TRACE,
                ReportField.LOGCAT,
        },
        logcatArguments = { "-t", "200", "-v", "threadtime" },
        mode = ReportingInteractionMode.DIALOG,
        resToastText = R.string.crash_toast_text, // optional, displayed as soon as the crash occurs, before collecting data which can take a few seconds
        resDialogText = R.string.crash_dialog_text,
        resDialogIcon = android.R.drawable.ic_dialog_info, //optional. default is a warning sign
        resDialogTitle = R.string.crash_dialog_title, // optional. default is your application name
        resDialogCommentPrompt = R.string.crash_dialog_comment_prompt, // optional. When defined, adds a user text field input with this text resource as a label
        //resDialogEmailPrompt = R.string.crash_user_email_label, // optional. When defined, adds a user email text entry with this text resource as label. The email address will be populated from SharedPreferences and will be provided as an ACRA field if configured.
        resDialogOkToast = R.string.crash_dialog_ok_toast // optional. displays a Toast message when the user accepts to send a report.
)
public class App extends Application implements Application.ActivityLifecycleCallbacks {
    private static App sSelf;
    public static App getInstance() {
        return sSelf;
    }

    public static final String TAG = "kg.serial.manager";
    public static final String LOCAL_ACTION_COMMAND_RECEIVED = "kg.serial.manager.local.new_data";
    public static final String LOCAL_ACTION_SETTINGS_UPDATED = "kg.serial.manager.local.settings_updated";

    public static final String ACTION_COMMAND_RECEIVED = "kg.serial.manager.command_received";
    public static final String ACTION_SEND_DATA = "kg.serial.manager.send";
    public static final String ACTION_SEND_DATA_COMPLETE = "kg.serial.manager.send.complete";
    public static final String ACTION_EXTERNAL_COMMAND = "kg.serial.manager.new_command";
    public static final String ACTION_APP_STARTED = "kg.serial.manager.app_started";
    public static final String ACTION_SERVICE_STARTED = "kg.serial.manager.started";
    public static final String ACTION_SERVICE_STOPPED = "kg.serial.manager.stopped";
    public static final String ACTION_CONNECTION_ESTABLISHED = "kg.serial.manager.connection_established";
    public static final String ACTION_CONNECTION_LOST = "kg.serial.manager.connection_lost";

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
        sSelf = this;
        registerActivityLifecycleCallbacks(this);
        Realm.init(this);

        setBootedMillis(android.os.SystemClock.uptimeMillis());

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mRealmConfig = new RealmConfiguration.Builder()
                .schemaVersion(3)
                .migration(new Migration())
                .build();
        mRealm = getNewRealmInstance();

        sDebugEnabled = mPrefs.getBoolean("debugging", false);

        if (isSystemOverlaysPermissionGranted()) {
            createHelperOverlay();
//            mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
//            LayoutInflater layoutInflater = (LayoutInflater) getSystemService(
//                    Context.LAYOUT_INFLATER_SERVICE);
//            View checkFullscreenView = layoutInflater.inflate(R.layout.check_fullscreen_overlay, null);
//            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
//                    0, ViewGroup.LayoutParams.MATCH_PARENT,
//                    WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
//                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
//                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
//                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
//                    PixelFormat.TRANSLUCENT);
//            checkFullscreenView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
//                @Override
//                public void onLayoutChange(View view, int left, int top, int right, int bottom,
//                                           int oldLeft, int oldTop, int oldRight, int oldBottom) {
//                    DisplayMetrics metrics = new DisplayMetrics();
//                    mWindowManager.getDefaultDisplay().getMetrics(metrics);
//                    mIsFullscreen = bottom >= metrics.heightPixels;
//
//                    logStatus("Fullscreen", String.valueOf(mIsFullscreen));
//                }
//            });
//            mWindowManager.addView(checkFullscreenView, layoutParams);
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

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        ACRA.init(this);
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
        final WindowManager.LayoutParams p = new WindowManager.LayoutParams();
        p.type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
        p.gravity = Gravity.END | Gravity.TOP;
        p.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        p.width = 1;
        p.height = WindowManager.LayoutParams.MATCH_PARENT;
        p.format = PixelFormat.TRANSPARENT;
        final View helperOverlay = new View(this);

        mWindowManager.addView(helperOverlay, p);
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
        Pattern pattern = Pattern.compile("^<(.+?):(.+?)>$");
        Matcher matcher = pattern.matcher(incomingString);
        if (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2);
            log("Receive | key:" + key + " / value:" + value);


            if (isActivityVisible()) {
                // Toaster.toast(incomingString);
                showSnackbar(mVisibleActivity, incomingString);

                Intent intent = new Intent(LOCAL_ACTION_COMMAND_RECEIVED);
                intent.putExtra("key", key);
                intent.putExtra("value", value);
                sendBroadcast(intent);
            } else {
                String intentValue = value;

                Realm realm = getNewRealmInstance();
                RealmResults<Command> commands = realm.where(Command.class)
                        .equalTo("key", key)
                        .findAll();

                for (Command command : commands) {
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
                                    .setBackgroundColor(Color.parseColor(command.getNotyBgColor()))
                                    .show(text, command.getNotyDurationInMillis());
                            }
                        }

                        switch (command.getActionId()) {
                            case Command.ACTION_RUN_APPLICATION:
                                if (!command.getChosenApp().isEmpty()) {
                                    Intent intent = AppChooserView.getIntentValue(
                                            command.getChosenApp(), null);
                                    if (intent == null) {
                                        Toaster.toast(getString(R.string.app_chooser_toast_app_not_found,
                                                AppChooserView.getLabelByValue(this, command.getChosenApp())));
                                    } else {
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                    }
                                }
                                break;

                            case Command.ACTION_EMULATE_KEY:
                                if (mVirtualKeyboard != null) {
                                    mVirtualKeyboard.emulateKey(command.getEmulatedKeyId());
                                }
                                break;

                            case Command.ACTION_SHELL_COMMAND:
                                try {
                                    // Executes the command.
                                    Process process = Runtime.getRuntime()
                                            .exec(command.getShellCommand());

                                    // Reads stdout.
                                    // NOTE: You can write to stdin of the command using
                                    //       process.getOutputStream().
//                                    BufferedReader reader = new BufferedReader(
//                                            new InputStreamReader(process.getInputStream()));
//                                    int read;
//                                    char[] buffer = new char[4096];
//                                    StringBuffer output = new StringBuffer();
//                                    while ((read = reader.read(buffer)) > 0) {
//                                        output.append(buffer, 0, read);
//                                    }
//                                    reader.close();
//
//                                    // Waits for the command to finish.
//                                    process.waitFor();
//
//                                    return output.toString();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                break;

                            case Command.ACTION_SEND_DATA:
                                Intent intent = new Intent(ACTION_SEND_DATA);
                                intent.putExtra("data", compileFormulas(
                                        replaceKeywords(command.getSendData(), key, value)));
                                sendBroadcast(intent);
                                break;
                        }

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
        return str.replaceAll("(%key)", key)
                .replaceAll("(%value)", value);
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
        showSnackbar(activity, message, Snackbar.LENGTH_SHORT, null);
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


}

package kg.delletenebre.serialmanager2;


import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

class Updater extends AsyncTask<MainActivity, Void, Void> {
    static final String PREF_NAME_LAST_IGNORED_VERSION = "last_ignored_update_version";

    private String mVersionName = "";
    private boolean mShowDialogIfNoUpdates = false;

    Updater(boolean showDialogIfNoUpdates) {
        mShowDialogIfNoUpdates = showDialogIfNoUpdates;
    }

    protected Void doInBackground(MainActivity... activity) {
        checkUpdates(activity[0]);
        return null;
    }

    private Integer getLastAppVersion() {
        int versionCode = -1;
        try {
            // Create a URL for the desired page
            URL url = new URL("https://raw.githubusercontent.com/delletenebre/SerialManager2/master/app/build.gradle");
            // Read all the text returned by the server
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String str;

            while ((str = in.readLine()) != null) {
                int f = str.indexOf("versionCode");
                if (f != -1) {
                    str = str.substring(f + ("versionCode").length()).trim();
                    versionCode = Integer.parseInt(str);
                }

                f = str.indexOf("versionName");
                if (f != -1) {
                    str = str.substring(f + ("versionName").length() + 2, str.length() - 1).trim();
                    mVersionName = str;
                    break;
                }
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d("*****", "versionCode: " + versionCode);
        Log.d("*****", "versionName: " + mVersionName);
        return versionCode;
    }

    private void checkUpdates(final MainActivity activity) {
        final Integer lastAppVersion = getLastAppVersion();
        if (lastAppVersion <= BuildConfig.VERSION_CODE) {
            if (!mShowDialogIfNoUpdates) {
                return;
            } else {
                activity.showNoUpdatesAvailable();
            }
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        String lastIgnoredUpdateVersion = prefs.getString(PREF_NAME_LAST_IGNORED_VERSION, "");
        if (!mShowDialogIfNoUpdates && lastIgnoredUpdateVersion.equals(mVersionName)) {
            return;
        }

        activity.update(mVersionName);
    }
}
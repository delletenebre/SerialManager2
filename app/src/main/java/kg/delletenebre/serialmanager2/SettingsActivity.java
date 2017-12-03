package kg.delletenebre.serialmanager2;


import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatDelegate;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import kg.delletenebre.serialmanager2.utils.RealmBackupRestore;

public class SettingsActivity extends PreferenceActivity {

    private AppCompatDelegate mDelegate;

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else {
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    private static boolean isLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    private static boolean isLandscape(Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    private static void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getDelegate().onPostCreate(savedInstanceState);
    }

    @NonNull
    @Override
    public MenuInflater getMenuInflater() {
        return getDelegate().getMenuInflater();
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        getDelegate().setContentView(layoutResID);
    }

    @Override
    public void setContentView(View view) {
        getDelegate().setContentView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().setContentView(view, params);
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().addContentView(view, params);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        getDelegate().onPostResume();
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        getDelegate().setTitle(title);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getDelegate().onConfigurationChanged(newConfig);
    }

    @Override
    protected void onStop() {
        super.onStop();
        getDelegate().onStop();
        sendBroadcast(new Intent(App.LOCAL_ACTION_SETTINGS_UPDATED));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getDelegate().onDestroy();
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                NavUtils.navigateUpFromSameTask(this);
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this) || (isLargeTablet(this) && isLandscape(this));
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private AppCompatDelegate getDelegate() {
        if (mDelegate == null) {
            mDelegate = AppCompatDelegate.create(this, null);
        }
        return mDelegate;
    }

    private void setupActionBar() {
        ActionBar actionBar = getDelegate().getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                || UsbPreferenceFragment.class.getName().equals(fragmentName)
                || BluetoothPreferenceFragment.class.getName().equals(fragmentName)
                || WebSocketPreferenceFragment.class.getName().equals(fragmentName)
                || SerialPreferenceFragment.class.getName().equals(fragmentName)
                || ControllerPreferenceFragment.class.getName().equals(fragmentName)
                || BackupRestorePreferenceFragment.class.getName().equals(fragmentName);
    }

    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_fragment_general);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference("start_on_boot_delay"));
            bindPreferenceSummaryToValue(findPreference("start_when_screen_on_delay"));
            bindPreferenceSummaryToValue(findPreference("stop_when_screen_off_delay"));
            bindPreferenceSummaryToValue(findPreference("command_format_regex"));
        }
    }

    public static class UsbPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_fragment_usb_communication);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference("usb_baud_rate"));
        }
    }

    public static class BluetoothPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_fragment_bluetooth_communication);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference("bluetooth_device"));
        }
    }

    public static class WebSocketPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_fragment_web_socket_communication);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference("web_server_port"));
        }
    }

    public static class SerialPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_fragment_serial_communication);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference("serial_path"));
            bindPreferenceSummaryToValue(findPreference("serial_baud_rate"));
        }
    }

    public static class ControllerPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_fragment_controller_communication);
            setHasOptionsMenu(true);

            //bindPreferenceSummaryToValue(findPreference("web_server_port"));
        }
    }

    public static class BackupRestorePreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_fragment_backup_restore);
            setHasOptionsMenu(true);

            final RealmBackupRestore realmBackupRestore = new RealmBackupRestore(getActivity());
            Preference backup = findPreference("db_backup");
            backup.setSummary(String.format(
                    getString(R.string.pref_summary_commands_backup), RealmBackupRestore.JSON_FILE_PATH));
            backup.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    realmBackupRestore.backupToJson();
                    return true;
                }
            });

            Preference restore = findPreference("db_restore");
            restore.setSummary(String.format(
                    getString(R.string.pref_summary_commands_restore), RealmBackupRestore.JSON_FILE_PATH));
            restore.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    realmBackupRestore.restoreFromJson();
                    return true;
                }
            });
        }
    }
}

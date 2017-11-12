package kg.delletenebre.serialmanager2.utils;


import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import io.realm.Realm;
import kg.delletenebre.serialmanager2.App;
import kg.delletenebre.serialmanager2.R;
import kg.delletenebre.serialmanager2.commands.Command;

public class RealmBackupRestore {
    private static final File EXPORT_REALM_FOLDER = Environment.getExternalStorageDirectory();
    private static  String JSON_FILE_NAME = "serial_manager_backup.json";
    public static  String JSON_FILE_PATH = EXPORT_REALM_FOLDER + "/" + JSON_FILE_NAME;

    private Activity mActivity;
    private Realm mRealm;

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public RealmBackupRestore(Activity activity) {
        mRealm = App.getInstance().getRealm();
        mActivity = activity;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            PERMISSIONS_STORAGE = new String[] {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        }
    }

    private boolean checkStoragePermissions(Activity activity) {
        // Check if we have write permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int writePermission = ActivityCompat.checkSelfPermission(activity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int readPermission = ActivityCompat.checkSelfPermission(activity,
                    Manifest.permission.READ_EXTERNAL_STORAGE);

            if (writePermission != PackageManager.PERMISSION_GRANTED ||
                    readPermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        activity,
                        PERMISSIONS_STORAGE,
                        REQUEST_EXTERNAL_STORAGE
                );
            }
        }

        return true;
    }

    public void restoreFromJson() {
        if (checkStoragePermissions(mActivity)) {

            mRealm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    try {
                        InputStream stream = new FileInputStream(new File(JSON_FILE_PATH));
                        mRealm.createAllFromJson(Command.class, stream);
                        List<Command> items = mRealm.where(Command.class).findAll().sort("index");
                        if (items.size() > 0) {
                            int index = 0;
                            for (Command command : items) {
                                command.setIndex(index);
                                index++;
                            }
                        }

                        App.getInstance().showSnackbarSuccess(mActivity,
                                mActivity.getString(R.string.pref_toast_commands_restore_success));

                        stream.close();
                    } catch (java.io.IOException ioe) {
                        App.logError(ioe.getLocalizedMessage());
                        App.getInstance().showSnackbarError(mActivity,
                                mActivity.getString(R.string.pref_toast_commands_restore_error));
                    }
                }
            });
        }
    }

    public void backupToJson() {
        if (checkStoragePermissions(mActivity)) {
            try {
                List<Command> commands = mRealm.where(Command.class).findAll();
                if (commands.size() > 0) {
                    Gson gson = new Gson();
                    File file = new File(JSON_FILE_PATH);
                    if (file.exists()) {
                        file.delete(); // Delete any previous recording
                    }
                    file.createNewFile();
                    FileOutputStream stream = new FileOutputStream(file);
                    try {
                        stream.write(gson.toJson(mRealm.copyFromRealm(commands)).getBytes());
                    } finally {
                        stream.close();
                    }

                    App.getInstance().showSnackbarSuccess(mActivity,
                            mActivity.getString(R.string.pref_toast_commands_backup_success));
                }
            } catch (Exception e) {
                e.printStackTrace();
                App.logError(e.getLocalizedMessage());
                App.getInstance().showSnackbarError(mActivity,
                        mActivity.getString(R.string.pref_toast_commands_backup_error));
            }
        }
    }

}

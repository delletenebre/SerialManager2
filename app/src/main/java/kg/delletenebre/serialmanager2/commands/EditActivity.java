package kg.delletenebre.serialmanager2.commands;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.jrummyapps.android.colorpicker.ColorPickerDialog;
import com.jrummyapps.android.colorpicker.ColorPickerDialogListener;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemSelected;
import io.realm.Realm;
import kg.delletenebre.serialmanager2.App;
import kg.delletenebre.serialmanager2.R;
import kg.delletenebre.serialmanager2.views.AppChooserView;

public class EditActivity extends AppCompatActivity implements ColorPickerDialogListener {
    private static final String TAG = "EditActivity";

    private static final int DIALOG_ID_NOTY_BACKGROUND_COLOR = 0;
    private static final int DIALOG_ID_NOTY_TEXT_COLOR = 1;

    private Realm mRealm;
    private Command mCommand;
    private int mCommandIndex;

    @BindView(R.id.main_layout) LinearLayout mMainLayout;
    @BindView(R.id.autoset_key_and_value) CheckBox mCheckboxAutoset;
    @BindView(R.id.key) EditText mEditTextKey;
    @BindView(R.id.value) EditText mEditTextValue;
    @BindView(R.id.scatter) EditText mEditTextScatter;
    @BindView(R.id.intent_value_extra) EditText mEditTextIntentValueExtra;
    @BindView(R.id.action) Spinner mSpinnerAction;
    @BindView(R.id.app_chooser_parent) TextInputLayout mAppChooserViewParent;
    @BindView(R.id.app_chooser) AppChooserView mAppChooserView;
    @BindView(R.id.emulate_key_layout) LinearLayout mEmulateKeyLayout;
    @BindView(R.id.emulate_key) Spinner mEmulateKeySpinner;
    @BindView(R.id.shell_command_layout) TextInputLayout mShellCommandLayout;
    @BindView(R.id.shell_command) EditText mEditTextShellCommand;
    @BindView(R.id.send_data_layout) TextInputLayout mSendDataLayout;
    @BindView(R.id.send_data) EditText mEditTextSendData;
    @BindView(R.id.noty_message) EditText mEditTextNotyMessage;
    @BindView(R.id.noty_duration) EditText mEditTextNotyDuration;
    @BindView(R.id.noty_text_size) EditText mEditTextNotyTextSize;
    @BindView(R.id.noty_bg_color_helper) TextView mTextViewNotyBgColor;
    @BindView(R.id.noty_text_color_helper) TextView mTextViewNotyTextColor;
    @BindView(R.id.noty_horiz_position) Spinner mSpinnerPositionX;
    @BindView(R.id.noty_vert_position) Spinner mSpinnerPositionY;
    @BindView(R.id.noty_horiz_offset) EditText mEditTextNotyOffsetX;
    @BindView(R.id.noty_vert_offset) EditText mEditTextNotyOffsetY;

    @OnClick(R.id.noty_bg_color) void onNotyBgColorClicked () {
        showColorDialog(DIALOG_ID_NOTY_BACKGROUND_COLOR);
    }
    @OnClick(R.id.noty_text_color) void onNotyTextColorClicked () {
        showColorDialog(DIALOG_ID_NOTY_TEXT_COLOR);
    }

    @OnItemSelected(R.id.action) void onActionSelected(int position) {
        showViewForSelectedAction(position);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        setContentView(R.layout.activity_command_edit);


        mRealm = App.getInstance().getRealm();

        Intent intent = getIntent();
        if (intent == null || !intent.hasExtra("CommandIndex")) {
            finish();
        } else {
            ButterKnife.bind(this);

            mCommandIndex = intent.getIntExtra("CommandIndex", 0);
            boolean isNew = intent.hasExtra("isNew");

            mMainLayout.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if (hasFocus) {
                        hideKeyboard(EditActivity.this);
                    }
                }
            });

            ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_item,
                    App.getInstance().getVirtualKeyboard().getNames());
            spinnerArrayAdapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item);
            mEmulateKeySpinner.setAdapter(spinnerArrayAdapter);

            if (!isNew) {
                mCommand = mRealm.where(Command.class)
                        .equalTo("index", mCommandIndex)
                        .findFirst();

                mEditTextKey.setText(mCommand.getKey());
                mEditTextValue.setText(mCommand.getValue());
                mEditTextScatter.setText(String.valueOf(mCommand.getScatter()));
                mEditTextIntentValueExtra.setText(mCommand.getIntentValueExtra());
                mSpinnerAction.setSelection(mCommand.getActionId());
                mAppChooserView.setValue(String.valueOf(mCommand.getChosenApp()));
                mAppChooserView.setLabel(String.valueOf(mCommand.getChosenAppLabel()));
                mEmulateKeySpinner.setSelection(mCommand.getEmulatedKeyId());
                mEditTextShellCommand.setText(mCommand.getShellCommand());
                mEditTextSendData.setText(mCommand.getSendData());
                mEditTextNotyMessage.setText(mCommand.getNotyMessage());
                mEditTextNotyDuration.setText(String.valueOf(mCommand.getNotyDuration()));
                mEditTextNotyTextSize.setText(String.valueOf(mCommand.getNotyTextSize()));
                mTextViewNotyBgColor.setText(mCommand.getNotyBgColor());
                mTextViewNotyTextColor.setText(mCommand.getNotyTextColor());
                mSpinnerPositionX.setSelection(mCommand.getPositionX());
                mSpinnerPositionY.setSelection(mCommand.getPositionY());
                mEditTextNotyOffsetX.setText(String.valueOf(mCommand.getOffsetX()));
                mEditTextNotyOffsetY.setText(String.valueOf(mCommand.getOffsetY()));

            } else {
//                mEditTextKey.setText("");
//                mEditTextValue.setText("");
//                mEditTextScatter.setText(getString(R.string.default__command_scatter));
                mEditTextIntentValueExtra.setText(
                        getString(R.string.default__command_intent_value_extra));
                mSpinnerAction.setSelection(0);
//                mAppChooserView.setValue("");
                mEmulateKeySpinner.setSelection(0);
//                mEditTextShellCommand.setText("");
//                mEditTextSendData.setText("");
//                mEditTextNotyMessage.setText("");
                mEditTextNotyDuration.setText(getString(R.string.default__command_noty_duration));
                mEditTextNotyTextSize.setText(getString(R.string.default__command_noty_text_size));
                mTextViewNotyBgColor.setText(getString(R.string.default__command_noty_bg_color));
                mTextViewNotyTextColor.setText(getString(R.string.default__command_noty_text_color));
                mSpinnerPositionX.setSelection(0);
                mSpinnerPositionY.setSelection(0);
                mEditTextNotyOffsetX.setText(getString(R.string.default__command_noty_offset));
                mEditTextNotyOffsetY.setText(getString(R.string.default__command_noty_offset));
            }

            registerReceiver(mNewDataReceiver,
                    new IntentFilter(App.LOCAL_ACTION_COMMAND_RECEIVED));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mNewDataReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_command, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                if (mEditTextKey.getText().toString().isEmpty()) {
                    new AlertDialog.Builder(EditActivity.this)
                            .setIconAttribute(android.R.attr.alertDialogIcon)
                            .setTitle(getString(R.string.error))
                            .setMessage(getString(R.string.command_alert_empty_key_message))
                            .setPositiveButton("OK", null)
                            .show();
                    mEditTextKey.requestFocus();
                } else {
                    saveCommand();
                    NavUtils.navigateUpFromSameTask(this);
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }

    private BroadcastReceiver mNewDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(App.LOCAL_ACTION_COMMAND_RECEIVED)) {
                if (mCheckboxAutoset.isChecked()) {
                    mEditTextKey.setText(intent.getStringExtra("key"));
                    mEditTextValue.setText(intent.getStringExtra("value"));
                }
            }
        }
    };

    private void showViewForSelectedAction(int actionId) {
        mAppChooserViewParent.setVisibility(View.GONE);
        mEmulateKeyLayout.setVisibility(View.GONE);
        mShellCommandLayout.setVisibility(View.GONE);
        mSendDataLayout.setVisibility(View.GONE);

        switch (actionId) {
            case Command.ACTION_RUN_APPLICATION:
                mAppChooserViewParent.setVisibility(View.VISIBLE);
                break;
            case Command.ACTION_EMULATE_KEY:
                mEmulateKeyLayout.setVisibility(View.VISIBLE);
                break;
            case Command.ACTION_SHELL_COMMAND:
                mShellCommandLayout.setVisibility(View.VISIBLE);
                break;
            case Command.ACTION_SEND_DATA:
                mSendDataLayout.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    private void saveCommand() {
        mRealm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                if (mCommand == null) {
                    mCommand = realm.createObject(Command.class);
                    mCommand.setIndex(mCommandIndex);
                }

                mCommand.setKey(mEditTextKey.getText().toString());
                mCommand.setValue(mEditTextValue.getText().toString());

                float scatter = 0.0f;
                try {
                    scatter = Float.parseFloat(mEditTextScatter.getText().toString());
                } catch (Exception e) {
                    App.logError(e.getLocalizedMessage());
                }
                mCommand.setScatter(scatter);

                mCommand.setIntentValueExtra(mEditTextIntentValueExtra.getText().toString());
                mCommand.setActionId(mSpinnerAction.getSelectedItemPosition());
                mCommand.setChosenApp(mAppChooserView.getValue());
                mCommand.setChosenAppLabel(mAppChooserView.getLabel());
                mCommand.setEmulatedKeyId(mEmulateKeySpinner.getSelectedItemPosition());
                mCommand.setShellCommand(mEditTextShellCommand.getText().toString());
                mCommand.setSendData(mEditTextSendData.getText().toString());

                mCommand.setNotyMessage(mEditTextNotyMessage.getText().toString());

                float duration = 0;
                try {
                    duration = Float.parseFloat(mEditTextNotyDuration.getText().toString());
                } catch (Exception e) {
                    App.logError(e.getLocalizedMessage());
                }
                mCommand.setNotyDuration(duration);

                int textSize = 0;
                try {
                    textSize = Integer.parseInt(mEditTextNotyTextSize.getText().toString());
                } catch (Exception e) {
                    App.logError(e.getLocalizedMessage());
                }
                mCommand.setNotyTextSize(textSize);

                mCommand.setNotyBgColor(mTextViewNotyBgColor.getText().toString());
                mCommand.setNotyTextColor(mTextViewNotyTextColor.getText().toString());

                mCommand.setPositionX(mSpinnerPositionX.getSelectedItemPosition());
                mCommand.setPositionY(mSpinnerPositionY.getSelectedItemPosition());

                int offsetX = 0;
                try {
                    offsetX = Integer.parseInt(mEditTextNotyOffsetX.getText().toString());
                } catch (Exception e) {
                    App.logError(e.getLocalizedMessage());
                }
                mCommand.setOffsetX(offsetX);

                int offsetY = 0;
                try {
                    offsetY = Integer.parseInt(mEditTextNotyOffsetY.getText().toString());
                } catch (Exception e) {
                    App.logError(e.getLocalizedMessage());
                }
                mCommand.setOffsetY(offsetY);
            }
        });
    }

    public void hideKeyboard(Activity context) {
        InputMethodManager inputMethodManager = (InputMethodManager)
                context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View focusedView = context.getCurrentFocus();
        if (focusedView != null) {
            inputMethodManager.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
        }
    }

    private void showColorDialog(int dialogId) {
        String hexColor;
        switch (dialogId) {
            case DIALOG_ID_NOTY_BACKGROUND_COLOR:
                hexColor = mTextViewNotyBgColor.getText().toString();
                break;
            case DIALOG_ID_NOTY_TEXT_COLOR:
                hexColor = mTextViewNotyTextColor.getText().toString();
                break;
            default:
                hexColor = "#ffff0000";
        }

        ColorPickerDialog.newBuilder()
                .setColor(Color.parseColor(hexColor))
                .setShowAlphaSlider(true)
                .setDialogId(dialogId)
                .show(this);
    }

    @Override
    public void onColorSelected(int dialogId, @ColorInt int color) {
        String hexColor = String.format("#%08X", color);

        switch (dialogId) {
            case DIALOG_ID_NOTY_BACKGROUND_COLOR:
                mTextViewNotyBgColor.setText(hexColor);
                break;
            case DIALOG_ID_NOTY_TEXT_COLOR:
                mTextViewNotyTextColor.setText(hexColor);
                break;
        }
    }

    @Override
    public void onDialogDismissed(int dialogId) {}
}

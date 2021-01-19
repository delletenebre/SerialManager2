package kg.delletenebre.serialmanager2.commands

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NavUtils
import com.jrummyapps.android.colorpicker.ColorPickerDialog
import com.jrummyapps.android.colorpicker.ColorPickerDialogListener
import io.realm.Realm
import kg.delletenebre.serialmanager2.App
import kg.delletenebre.serialmanager2.R
import kotlinx.android.synthetic.main.command_edit_activity.*


class CommandEditActivity : AppCompatActivity(), ColorPickerDialogListener {
    companion object {
        private const val DIALOG_ID_NOTY_BACKGROUND_COLOR: Int = 0
        private const val DIALOG_ID_NOTY_TEXT_COLOR: Int = 1
    }

    private lateinit var mRealm: Realm
    private lateinit var mLocalBroadcastManager: androidx.localbroadcastmanager.content.LocalBroadcastManager
    private lateinit var mLocalBroadcastReceiver: BroadcastReceiver
    private var mCommand: CommandModel? = null
    private var mCommandIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        setContentView(R.layout.command_edit_activity)

        mRealm = App.getInstance().realm

        if (intent == null || !intent.hasExtra("CommandIndex")) {
            finish()
        }

        mCommandIndex = intent.getIntExtra("CommandIndex", 0)
        val isNew = intent.hasExtra("isNew")

        xMainLayout.onFocusChangeListener = View.OnFocusChangeListener {_, hasFocus ->
            if (hasFocus) {
                hideKeyboard(this)
            }
        }

        val spinnerArrayAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                App.getInstance().virtualKeyboard.names)
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        xEmulateKey.adapter = spinnerArrayAdapter

        if (!isNew) {
            mCommand = mRealm.where(CommandModel::class.java)
                    .equalTo("index", mCommandIndex)
                    .findFirst()

            xKey.setText(mCommand?.key)
            xValue.setText(mCommand?.value)
            xScatter.setText(mCommand?.scatter.toString())
            xIntentValueExtra.setText(mCommand?.intentValueExtra)
            mCommand?.actionId?.let { xAction.setSelection(it) }
            xAppChooser.value = mCommand?.chosenApp
            xAppChooser.label = mCommand?.chosenAppLabel
            mCommand?.emulatedKeyId?.let { xEmulateKey.setSelection(it) }
            xShellCommand.setText(mCommand?.shellCommand)
            xSendData.setText(mCommand?.sendData)
            xNotyMessage.setText(mCommand?.notyMessage)
            xNotyDuration.setText(mCommand?.notyDuration.toString())
            xNotyTextSize.setText(mCommand?.notyTextSize.toString())
            xNotyBackgroundColorHelper.text = mCommand?.notyBackgroundColor
            xNotyTextColorHelper.text = mCommand?.notyTextColor
            mCommand?.positionZ?.let { xNotyPositionZ.setSelection(it) }
            mCommand?.positionX?.let { xNotyPositionX.setSelection(it) }
            mCommand?.positionY?.let { xNotyPositionY.setSelection(it) }
            xNotyOffsetX.setText(mCommand?.offsetX.toString())
            xNotyOffsetY.setText(mCommand?.offsetY.toString())
            mCommand?.systemActionId?.let { xSystemAction.setSelection(it) }

        } else {
            xIntentValueExtra.setText(
                    getString(R.string.default__command_intent_value_extra))
            xAction.setSelection(0)
            xEmulateKey.setSelection(0)
            xNotyDuration.setText(getString(R.string.default__command_noty_duration))
            xNotyTextSize.setText(getString(R.string.default__command_noty_text_size))
            xNotyBackgroundColorHelper.text = getString(R.string.default__command_noty_bg_color)
            xNotyTextColorHelper.text = getString(R.string.default__command_noty_text_color)
            xNotyPositionZ.setSelection(0)
            xNotyPositionX.setSelection(0)
            xNotyPositionY.setSelection(0)
            xNotyOffsetX.setText(getString(R.string.default__command_noty_offset))
            xNotyOffsetY.setText(getString(R.string.default__command_noty_offset))
        }

        xNotyBackgroundColor.setOnClickListener { showColorDialog(DIALOG_ID_NOTY_BACKGROUND_COLOR) }
        xNotyTextColor.setOnClickListener { showColorDialog(DIALOG_ID_NOTY_TEXT_COLOR) }
        xAction.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                showViewForSelectedAction(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>) { }
        }


        mLocalBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                if (action != null && action == App.LOCAL_ACTION_COMMAND_DETECTED) {
                    if (xDetectKeyAndValue.isChecked) {
                        xKey.setText(intent.getStringExtra("key"))
                        xValue.setText(intent.getStringExtra("value"))
                    }
                }
            }
        }
        val localIntentFilter = IntentFilter()
        localIntentFilter.addAction(App.LOCAL_ACTION_COMMAND_DETECTED)
        mLocalBroadcastManager = androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this)
        mLocalBroadcastManager.registerReceiver(mLocalBroadcastReceiver, localIntentFilter)
    }

    public override fun onDestroy() {
        super.onDestroy()
        mLocalBroadcastManager.unregisterReceiver(mLocalBroadcastReceiver)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.edit_command, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_save -> {
                if (xKey.text.toString().isEmpty()) {
                    AlertDialog.Builder(this)
                            .setIconAttribute(android.R.attr.alertDialogIcon)
                            .setTitle(getString(R.string.error))
                            .setMessage(getString(R.string.command_alert_empty_key_message))
                            .setPositiveButton("OK", null)
                            .show()
                    xKey.requestFocus()
                } else {
                    saveCommand()
                    NavUtils.navigateUpFromSameTask(this)
                }
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onColorSelected(dialogId: Int, @ColorInt color: Int) {
        val hexColor = String.format("#%08X", color)

        when (dialogId) {
            DIALOG_ID_NOTY_BACKGROUND_COLOR -> xNotyBackgroundColorHelper.text = hexColor
            DIALOG_ID_NOTY_TEXT_COLOR -> xNotyTextColorHelper.text = hexColor
        }
    }

    override fun onDialogDismissed(dialogId: Int) {}

    private fun hideKeyboard(context: Activity) {
        val inputMethodManager =
                context.getSystemService(Activity.INPUT_METHOD_SERVICE)as InputMethodManager
        val focusedView = context.currentFocus
        if (focusedView != null) {
            inputMethodManager.hideSoftInputFromWindow(focusedView.windowToken, 0)
        }
    }

    private fun showViewForSelectedAction(actionId: Int) {
        xAppChooserLayout.visibility = View.GONE
        xEmulateKeyLayout.visibility = View.GONE
        xShellCommandLayout.visibility = View.GONE
        xSendDataLayout.visibility = View.GONE
        xSystemActionLayout.visibility = View.GONE

        when (actionId) {
            CommandModel.ACTION_RUN_APPLICATION -> xAppChooserLayout.visibility = View.VISIBLE
            CommandModel.ACTION_EMULATE_KEY -> xEmulateKeyLayout.visibility = View.VISIBLE
            CommandModel.ACTION_SHELL_COMMAND -> xShellCommandLayout.visibility = View.VISIBLE
            CommandModel.ACTION_SEND_DATA -> xSendDataLayout.visibility = View.VISIBLE
            CommandModel.ACTION_SYSTEM -> xSystemActionLayout.visibility = View.VISIBLE
            else -> {
            }
        }
    }

    private fun saveCommand() {
        mRealm.executeTransaction { realm ->
            if (mCommand == null) {
                mCommand = realm.createObject(CommandModel::class.java)
                mCommand?.index = mCommandIndex
            }

            mCommand?.key = xKey.text.toString()
            mCommand?.value = xValue.text.toString()

            var scatter = 0.0f
            try {
                scatter = java.lang.Float.parseFloat(xScatter.text.toString())
            } catch (e: Exception) {
                App.logError(e.localizedMessage)
            }

            mCommand?.scatter = scatter

            mCommand?.intentValueExtra = xIntentValueExtra.text.toString()
            mCommand?.actionId = xAction.selectedItemPosition
            mCommand?.chosenApp = xAppChooser.value
            mCommand?.chosenAppLabel = xAppChooser.label
            mCommand?.emulatedKeyId = xEmulateKey.selectedItemPosition
            mCommand?.shellCommand = xShellCommand.text.toString()
            mCommand?.sendData = xSendData.text.toString()
            mCommand?.systemActionId = xSystemAction.selectedItemPosition

            mCommand?.notyMessage = xNotyMessage.text.toString()

            var duration = 0f
            try {
                duration = java.lang.Float.parseFloat(xNotyDuration.text.toString())
            } catch (e: Exception) {
                App.logError(e.localizedMessage)
            }

            mCommand?.notyDuration = duration

            var textSize = 0
            try {
                textSize = Integer.parseInt(xNotyTextSize.text.toString())
            } catch (e: Exception) {
                App.logError(e.localizedMessage)
            }

            mCommand?.notyTextSize = textSize

            mCommand?.notyBackgroundColor = xNotyBackgroundColorHelper.text.toString()
            mCommand?.notyTextColor = xNotyTextColorHelper.text.toString()

            mCommand?.positionZ = xNotyPositionZ.selectedItemPosition
            mCommand?.positionX = xNotyPositionX.selectedItemPosition
            mCommand?.positionY = xNotyPositionY.selectedItemPosition

            var offsetX = 0
            try {
                offsetX = Integer.parseInt(xNotyOffsetX.text.toString())
            } catch (e: Exception) {
                App.logError(e.localizedMessage)
            }

            mCommand?.offsetX = offsetX

            var offsetY = 0
            try {
                offsetY = Integer.parseInt(xNotyOffsetY.text.toString())
            } catch (e: Exception) {
                App.logError(e.localizedMessage)
            }

            mCommand?.offsetY = offsetY
        }
    }

    private fun showColorDialog(dialogId: Int) {
        val hexColor: String = when (dialogId) {
            DIALOG_ID_NOTY_BACKGROUND_COLOR -> xNotyBackgroundColorHelper.text.toString()
            DIALOG_ID_NOTY_TEXT_COLOR -> xNotyTextColorHelper.text.toString()
            else -> "#ffff0000"
        }

        ColorPickerDialog.newBuilder()
                .setColor(Color.parseColor(hexColor))
                .setShowAlphaSlider(true)
                .setDialogId(dialogId)
                .show(this)
    }
}
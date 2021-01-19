package kg.delletenebre.serialmanager2.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import com.jrummyapps.android.colorpicker.ColorPickerDialog
import com.jrummyapps.android.colorpicker.ColorPickerDialogListener
import io.realm.Realm
import kg.delletenebre.serialmanager2.App
import kg.delletenebre.serialmanager2.R
import kg.delletenebre.serialmanager2.commands.CommandModel
import kotlinx.android.synthetic.main.widget_simple_activity.*


class WidgetSimpleSettingsActivity : AppCompatActivity(), ColorPickerDialogListener {
    private val DIALOGID_BACKGROUND_COLOR = 0
    private val DIALOGID_TEXT_COLOR = 1

    private var mWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private var mWidget: WidgetSimpleModel? = null
    private var mRealm: Realm? = null
    private var mIsWidgetCreated = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.widget_simple_activity)

        mWidgetId = intent.extras!!.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID)
        if (mWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
        }


        xTextColorButton.setOnClickListener { showColorDialog(DIALOGID_TEXT_COLOR) }
        xBackgroundColorButton.setOnClickListener { showColorDialog(DIALOGID_BACKGROUND_COLOR) }
        xAction.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(arg0: AdapterView<*>, arg1: View, position: Int, id: Long) {
                showViewForSelectedAction(position)
            }

            override fun onNothingSelected(arg0: AdapterView<*>) {}
        }
        val spinnerArrayAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                App.getInstance().virtualKeyboard.names)
        spinnerArrayAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item)
        xEmulateKey.adapter = spinnerArrayAdapter


        mRealm = App.getInstance().realm
        if (!intent.extras!!.getBoolean(App.EXTRA_APPWIDGET_EDIT, false)) {
            mRealm?.executeTransaction {
                mWidget = mRealm?.createObject(WidgetSimpleModel::class.java, mWidgetId)
                mWidget?.text = getString(R.string.widget_simple_default_text)
                mWidget?.position = mRealm?.where(WidgetSimpleModel::class.java)?.count() ?: 0
            }
        } else {
            mWidget = App.getInstance().realm.where(WidgetSimpleModel::class.java)
                    .equalTo("id", mWidgetId).findFirst()
            mIsWidgetCreated = true
        }

        setWidgetDataToViews()

        setResult(Activity.RESULT_CANCELED,
                (Intent()).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mWidgetId))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_widget_activity, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_save -> {
            saveWidgetSettings()

            val resultIntent = Intent()
            resultIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mWidgetId)
            setResult(Activity.RESULT_OK, resultIntent)

            mIsWidgetCreated = true

            finish()
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        if (!mIsWidgetCreated && mRealm != null && !mRealm!!.isClosed) {
            mRealm?.executeTransaction {
                mWidget?.deleteFromRealm()
            }
        }
        mRealm?.close()

        super.onDestroy()
    }

    private fun setWidgetDataToViews() {
        xKey.setText(mWidget?.key)
        xText.setText(mWidget?.text)
        xTextSize.setText(mWidget?.textSize.toString())
        xTextColor.text = mWidget?.textColor
        xBackgroundColor.text = mWidget?.backgroundColor
        mWidget?.textVerticalPositionId?.let { xTextVerticalPosition.setSelection(it) }
        mWidget?.textAlignmentId?.let { xTextAlignment.setSelection(it) }

        mWidget?.actionId?.let { xAction.setSelection(it) }
        xAppChooser.value = mWidget?.chosenApp
        xAppChooser.label = mWidget?.chosenAppLabel
        mWidget?.emulatedKeyId?.let { xEmulateKey.setSelection(it) }
        xShellCommand.setText(mWidget?.shellCommand)
        xSendData.setText(mWidget?.sendData)
        mWidget?.systemActionId?.let { xSystemAction.setSelection(it) }
    }

    private fun saveWidgetSettings() {
        mRealm?.executeTransaction {
            mWidget?.key = xKey.text.toString()
            mWidget?.text = xText.text.toString()
            mWidget?.textSize = xTextSize.text.toString().toInt()
            mWidget?.textColor = xTextColor.text.toString()
            mWidget?.backgroundColor = xBackgroundColor.text.toString()
            mWidget?.textVerticalPositionId = xTextVerticalPosition.selectedItemPosition
            mWidget?.textAlignmentId = xTextAlignment.selectedItemPosition

            mWidget?.actionId = xAction.selectedItemPosition
            mWidget?.chosenApp = xAppChooser.value
            mWidget?.chosenAppLabel = xAppChooser.label
            mWidget?.emulatedKeyId = xEmulateKey.selectedItemPosition
            mWidget?.shellCommand = xShellCommand.text.toString()
            mWidget?.sendData = xSendData.text.toString()
            mWidget?.systemActionId = xSystemAction.selectedItemPosition
        }

        val intent = Intent(this, WidgetSimple::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(mWidgetId))
        sendBroadcast(intent)
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
        }
    }


    private fun showColorDialog(dialogId: Int) {
        val hexColor: String = when (dialogId) {
            DIALOGID_TEXT_COLOR -> xTextColor.text.toString()
            DIALOGID_BACKGROUND_COLOR -> xBackgroundColor.text.toString()
            else -> "#ffff0000"
        }

        ColorPickerDialog.newBuilder()
                .setColor(Color.parseColor(hexColor))
                .setShowAlphaSlider(true)
                .setDialogId(dialogId)
                .show(this)
    }

    override fun onColorSelected(dialogId: Int, @ColorInt color: Int) {
        val hexColor = String.format("#%08X", color)

        when (dialogId) {
            DIALOGID_TEXT_COLOR -> xTextColor.text = hexColor
            DIALOGID_BACKGROUND_COLOR -> xBackgroundColor.text = hexColor
        }
    }

    override fun onDialogDismissed(dialogId: Int) {}
}
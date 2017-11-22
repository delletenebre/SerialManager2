package kg.delletenebre.serialmanager2.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import io.realm.Realm
import kg.delletenebre.serialmanager2.App
import kg.delletenebre.serialmanager2.R
import kotlinx.android.synthetic.main.widget_simple_activity.*


class WidgetReceiveActivity : AppCompatActivity() {
    private var mWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    private var mWidget: WidgetSimpleModel? = null
    lateinit private var mRealm: Realm


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.widget_simple_activity)

        mWidgetId = intent.extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID)
        if (mWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
        }

//
//        mSpinnerAction = findViewById(R.id.action)
//        mAppChooserViewParent = findViewById(R.id.app_chooser_parent)
////
        val spinnerArrayAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                App.getInstance().virtualKeyboard.names)
        spinnerArrayAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item)
        xEmulateKey.adapter = spinnerArrayAdapter



        mRealm = App.getInstance().realm
        if (!intent.extras.getBoolean(App.EXTRA_APPWIDGET_EDIT, false)) {
            mRealm.executeTransaction {
                mWidget = mRealm.createObject(WidgetSimpleModel::class.java, mWidgetId)
                mWidget?.text = getString(R.string.widget_receive_default_text)
            }
        } else {
            mWidget = App.getInstance().realm.where(WidgetSimpleModel::class.java)
                    .equalTo("id", mWidgetId).findFirst()
        }

        setWidgetDataToViews()

        val resultIntent = Intent()
        resultIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mWidgetId)
        setResult(Activity.RESULT_CANCELED, resultIntent)
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
            finish()
            true
        }

        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mRealm.close()
    }

    private fun setWidgetDataToViews() {
        xKey.setText(mWidget?.key)
        xText.setText(mWidget?.text)
        xTextSize.setText(mWidget?.textSize.toString())
        mWidget?.textVerticalPositionId?.let { xTextVerticalPosition.setSelection(it) }
        mWidget?.textAlignmentId?.let { xTextAlignment.setSelection(it) }

        mWidget?.actionId?.let { xAction.setSelection(it) }
    }

    private fun saveWidgetSettings() {
        mRealm.executeTransaction {
            mWidget?.key = xKey.text.toString()
            mWidget?.text = xText.text.toString()
            mWidget?.textSize = xTextSize.text.toString().toInt()
            mWidget?.textVerticalPositionId = xTextVerticalPosition.selectedItemPosition
            mWidget?.textAlignmentId = xTextAlignment.selectedItemPosition

            mWidget?.actionId = xAction.selectedItemPosition
        }

        val intent = Intent(this, WidgetSimple::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(mWidgetId))
        sendBroadcast(intent)
    }
}
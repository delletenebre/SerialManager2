package kg.delletenebre.serialmanager2.widgets

import android.app.Activity
import android.os.Bundle
import kg.delletenebre.serialmanager2.App
import kg.delletenebre.serialmanager2.commands.Command

class WidgetActionActivity: Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val actionId = intent.getIntExtra(App.EXTRA_APPWIDGET_ACTION_ID, Command.ACTION_NONE)
        val chosenApp = intent.getStringExtra(App.EXTRA_SELECTED_ACTION_CHOSEN_APP)
        val emulatedKeyId = intent.getIntExtra(App.EXTRA_SELECTED_ACTION_EMULATE_KEY, 0)
        val shellCommand = intent.getStringExtra(App.EXTRA_SELECTED_ACTION_SHELL_COMMAND)
        val sendData = intent.getStringExtra(App.EXTRA_SELECTED_ACTION_SEND_DATA)
        val key = intent.getStringExtra(App.EXTRA_APPWIDGET_KEY)
        val value = intent.getStringExtra(App.EXTRA_APPWIDGET_VALUE)

        App.getInstance().executeCommandAction(actionId, chosenApp, emulatedKeyId, shellCommand,
                sendData, key, value)

        setResult(RESULT_OK)
        finish()
    }
}
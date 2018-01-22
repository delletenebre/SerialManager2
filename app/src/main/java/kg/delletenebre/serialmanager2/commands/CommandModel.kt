package kg.delletenebre.serialmanager2.commands

import android.content.Context
import android.support.annotation.Keep
import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.PrimaryKey
import kg.delletenebre.serialmanager2.App
import kg.delletenebre.serialmanager2.R
import kg.delletenebre.serialmanager2.utils.Utils

@Keep
open class CommandModel : RealmObject() {
    companion object {
        @Ignore
        const val ACTION_NONE = 0
        @Ignore
        const val ACTION_RUN_APPLICATION = 1
        @Ignore
        const val ACTION_EMULATE_KEY = 2
        @Ignore
        const val ACTION_SHELL_COMMAND = 3
        @Ignore
        const val ACTION_SEND_DATA = 4
    }

    @PrimaryKey
    var index: Int = 0

    var folder: Boolean = false
    var key: String = ""
    var value: String = ""
    var scatter: Float = 0.0f
    var intentValueExtra: String = ""
    var actionId: Int = 0
    var chosenApp: String = ""
    var chosenAppLabel: String = ""
    var emulatedKeyId: Int = 0
    var shellCommand: String = ""
    var sendData: String = ""
    var notyMessage: String = ""
    var notyDuration: Float = 0.0f
    var notyTextSize: Int = 0
    var notyBackgroundColor: String = "#88000000"
    var notyTextColor: String = "#ffffffff"
    var positionZ: Int = 0
    var positionX: Int = 0
    var positionY: Int = 0
    var offsetX: Int = 0
    var offsetY: Int = 0

    fun getNotyDurationInMillis(): Int {
        return (notyDuration * 1000).toInt()
    }

    fun getTitleLabel(context: Context): String {
        var strValue = value
        if (value.isEmpty()) {
            strValue = "*"
        } else if (Utils.isNumber(value) && scatter != 0.0f) {
            strValue += " ± " + scatter
        }
        return context.getString(R.string.command_title, key, strValue)

    }

    fun getSubtitleLabel(context: Context): String {
        val actions = context.resources.getStringArray(R.array.array_command_actions)

        when (actionId) {
            ACTION_RUN_APPLICATION -> return chosenAppLabel

            ACTION_EMULATE_KEY -> return context.getString(R.string.command_subtitle,
                    actions[actionId],
                    App.getInstance().virtualKeyboard.getNameById(emulatedKeyId))

            ACTION_SHELL_COMMAND -> return context.getString(R.string.command_subtitle,
                    actions[actionId],
                    shellCommand)

            ACTION_SEND_DATA -> return context.getString(R.string.command_subtitle,
                    actions[actionId],
                    sendData)

            else ->
                // ACTION_NONE
                return actions[actionId]
        }
    }
}
package kg.delletenebre.serialmanager2.widgets

import android.content.Context
import androidx.annotation.Keep
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import kg.delletenebre.serialmanager2.App
import kg.delletenebre.serialmanager2.R
import kg.delletenebre.serialmanager2.commands.CommandModel


@Keep
open class WidgetSimpleModel : RealmObject() {
    @PrimaryKey var id: Int = 0
    var position: Long = 0
    var key: String = ""
    var text: String = ""
    var textSize: Int = 14
    var textColor: String = "#ffffffff"
    var backgroundColor: String = "#88000000"
    var backgroundImage: String = ""
    var textVerticalPositionId: Int = 1
    var textAlignmentId: Int = 1

    var actionId: Int = 0
    var chosenApp: String = ""
    var chosenAppLabel: String = ""
    var emulatedKeyId: Int = 0
    var shellCommand: String = ""
    var sendData: String = ""

    fun getTitleLabel(context: Context): String {
        return context.getString(R.string.widgets_list_item_title, id, key)
    }

    fun getSubtitleLabel(context: Context): String {
        val actions = context.resources.getStringArray(R.array.array_command_actions)

        when (actionId) {
            CommandModel.ACTION_RUN_APPLICATION -> return chosenAppLabel

            CommandModel.ACTION_EMULATE_KEY -> return context.getString(R.string.command_subtitle,
                    actions[actionId],
                    App.getInstance().virtualKeyboard.getNameById(emulatedKeyId))

            CommandModel.ACTION_SHELL_COMMAND -> return context.getString(R.string.command_subtitle,
                    actions[actionId],
                    shellCommand)

            CommandModel.ACTION_SEND_DATA -> return context.getString(R.string.command_subtitle,
                    actions[actionId],
                    sendData)

            else ->
                // ACTION_NONE
                return actions[actionId]
        }
    }
}
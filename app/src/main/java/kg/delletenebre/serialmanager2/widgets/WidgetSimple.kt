package kg.delletenebre.serialmanager2.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import kg.delletenebre.serialmanager2.App
import kg.delletenebre.serialmanager2.R
import java.util.*


class WidgetSimple : AppWidgetProvider() {
    private val sTextViewIds = intArrayOf(
            R.id.text_top_left, R.id.text_top_center, R.id.text_top_right,
            R.id.text_middle_left, R.id.text_middle_center, R.id.text_middle_right,
            R.id.text_bottom_left, R.id.text_bottom_center, R.id.text_bottom_right)

    override fun onUpdate(context: Context, widgetManager: AppWidgetManager, widgetIds: IntArray) {
        widgetIds.forEach { widgetId ->
            update(context, widgetManager, widgetId, "---")
        }
        super.onUpdate(context, widgetManager, widgetIds)
    }

    override fun onDeleted(context: Context, widgetIds: IntArray) {
        val realm = App.getInstance().realm
        val widgets = realm.where(WidgetSimpleModel::class.java)
                .`in`("id", widgetIds.toTypedArray()).findAll()
        realm.executeTransaction {
            widgets.deleteAllFromRealm()
        }
        realm.close()
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        var key = ""
        var value = ""
        if (intent.hasExtra("key") && intent.hasExtra("value")) {
            key = intent.getStringExtra("key")
            value = intent.getStringExtra("value")
        }

        if (!key.isEmpty()) {
            // val appContext = context.applicationContext
            val widgetManager = AppWidgetManager.getInstance(context)

            val realm = App.getInstance().realm
            val widgets = realm.where(WidgetSimpleModel::class.java)
                    .equalTo("key", key).findAll()!!
            for (widget in widgets) {
                update(context, widgetManager, widget.id, value)
            }
        }
    }

    private fun update(context: Context, widgetManager: AppWidgetManager,
                       widgetId: Int, value: String) {
        val widgetViews = RemoteViews(context.packageName, R.layout.widget_simple)
        val widget = App.getInstance().realm.where(WidgetSimpleModel::class.java)
                .equalTo("id", widgetId).findFirst()
        if (widget != null) {
            val app = App.getInstance()
            val position = widget.textAlignmentId + widget.textVerticalPositionId * 3
            val visibleTextViewId = sTextViewIds[position]
            val text = app.compileFormulas(app.replaceKeywords(widget.text, widget.key, value))
            val textSize = widget.textSize.toFloat()
            val textColor = Color.parseColor(widget.textColor)
            val backgroundColor = Color.parseColor(widget.backgroundColor)

            widgetViews.setTextViewText(visibleTextViewId, text)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                widgetViews.setTextViewTextSize(visibleTextViewId,
                        TypedValue.COMPLEX_UNIT_SP, textSize)
            }
            widgetViews.setTextColor(visibleTextViewId, textColor)
            widgetViews.setInt(R.id.widget_container, "setBackgroundColor", backgroundColor)

            for (viewId in sTextViewIds) {
                widgetViews.setViewVisibility(viewId, View.GONE)
            }
            widgetViews.setViewVisibility(visibleTextViewId, View.VISIBLE)

            val intent = Intent(context, WidgetActionActivity::class.java)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            intent.putExtra(App.EXTRA_APPWIDGET_ACTION_ID, widget.actionId)
            intent.putExtra(App.EXTRA_APPWIDGET_KEY, widget.key)
            intent.putExtra(App.EXTRA_APPWIDGET_VALUE, value)
            intent.putExtra(App.EXTRA_SELECTED_ACTION_CHOSEN_APP, widget.chosenApp)
            intent.putExtra(App.EXTRA_SELECTED_ACTION_EMULATE_KEY, widget.emulatedKeyId)
            intent.putExtra(App.EXTRA_SELECTED_ACTION_SHELL_COMMAND, widget.shellCommand)
            intent.putExtra(App.EXTRA_SELECTED_ACTION_SEND_DATA, widget.sendData)
            intent.putExtra(App.EXTRA_SELECTED_ACTION_SYSTEM_ACTION_ID, widget.systemActionId)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.data = Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME))
            val pendingIntent = PendingIntent.getActivity(context, (Random()).nextInt(),
                    intent, PendingIntent.FLAG_CANCEL_CURRENT)
            widgetViews.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
        }

        widgetManager.updateAppWidget(widgetId, widgetViews)
    }

}


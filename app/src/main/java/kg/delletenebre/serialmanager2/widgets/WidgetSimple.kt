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


class WidgetSimple : AppWidgetProvider() {
    private val sTextViewIds = intArrayOf(
            R.id.text_top_left, R.id.text_top_center, R.id.text_top_right,
            R.id.text_middle_left, R.id.text_middle_center, R.id.text_middle_right,
            R.id.text_bottom_left, R.id.text_bottom_center, R.id.text_bottom_right)

    override fun onUpdate(context: Context, widgetManager: AppWidgetManager, widgetIds: IntArray) {
        for (widgetId in widgetIds) {
            update(context, widgetManager, widgetId, "---")
        }
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

        if (intent.action == App.ACTION_COMMAND_RECEIVED) {
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
            val position = widget.textAlignmentId + widget.textVerticalPositionId * 3
            val visibleTextViewId = sTextViewIds[position]
            val text = widget.text
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

            val intent = Intent(context, WidgetSimpleActivity::class.java)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            intent.putExtra(App.EXTRA_APPWIDGET_EDIT, true)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.data = Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME))
            val pendingIntent = PendingIntent.getActivity(context, 0,
                    intent, PendingIntent.FLAG_UPDATE_CURRENT)
            widgetViews.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

//            widgetViews.setOnClickFillInIntent()
//            when (widget.actionId) {
//                Command.ACTION_RUN_APPLICATION -> if (widget.chosenApp.none()) {
//                    val intent = AppChooserView.getIntentValue(widget.chosenApp, null)
//                    if (intent == null) {
//                        Toaster.toast(context.getString(R.string.app_chooser_toast_app_not_found,
//                                AppChooserView.getLabelByValue(context, widget.chosenApp)))
//                    } else {
//                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                        context.startActivity(intent)
//                    }
//                }
//
//                Command.ACTION_EMULATE_KEY -> {}
//
//                Command.ACTION_SHELL_COMMAND -> try {
//                    Runtime.getRuntime().exec(widget.shellCommand)
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
//
//                Command.ACTION_SEND_DATA -> {
//                    val intent = Intent(App.ACTION_SEND_DATA)
//                    intent.putExtra("data", App.getInstance().compileFormulas(
//                            App.getInstance().replaceKeywords(widget.sendData, widget.key, value)))
//                    context.sendBroadcast(intent)
//                }
//            }
        }

        widgetManager.updateAppWidget(widgetId, widgetViews)
    }

}


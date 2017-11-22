package kg.delletenebre.serialmanager2.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
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
            val backgroundColor = Color.parseColor(widget.backgroundColor)

            widgetViews.setInt(R.id.container, "setBackgroundColor", backgroundColor)
            widgetViews.setTextColor(visibleTextViewId, Color.WHITE)
            widgetViews.setTextViewText(visibleTextViewId, text)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                widgetViews.setTextViewTextSize(visibleTextViewId,
                        TypedValue.COMPLEX_UNIT_SP, textSize)
            }

            for (viewId in sTextViewIds) {
                widgetViews.setViewVisibility(viewId, View.GONE)
            }
            widgetViews.setViewVisibility(visibleTextViewId, View.VISIBLE)
        }

        widgetManager.updateAppWidget(widgetId, widgetViews)
    }

//    companion object {
//
//        private val sImageViewIds = intArrayOf(R.id.image_text_top_left, R.id.image_text_top_center, R.id.image_text_top_right, R.id.image_text_middle_left, R.id.image_text_middle_center, R.id.image_text_middle_right, R.id.image_text_bottom_left, R.id.image_text_bottom_center, R.id.image_text_bottom_right)
//
//        internal fun updateWidget(context: Context, widgetManager: AppWidgetManager,
//                                  widgetId: Int, value: String) {
//
//            val prefs = getPrefs(context, widgetId)
//
//            val position = Integer.parseInt(prefs.getString("position", "4"))
//
//            val imageViewId = sImageViewIds[position]
//
//            val backgroundColor = prefs.getInt("backgroundColor", Color.parseColor("#88000000"))
//
//            val views = RemoteViews(context.packageName, R.layout.widget_simple)
//            views.setInt(R.id.container, "setBackgroundColor", backgroundColor)
//
//            for (id in sImageViewIds) {
//                views.setViewVisibility(id, View.GONE)
//            }
//            views.setViewVisibility(imageViewId, View.VISIBLE)
//            views.setImageViewBitmap(imageViewId, getFontBitmap(context, prefs, value))
//
//
//            // onClick open configure activity
//            val intent = Intent(context, WidgetReceiveConfigureActivity::class.java)
//            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
//            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
//            intent.data = Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME))
//            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
//            views.setOnClickPendingIntent(R.id.container, pendingIntent)
//
//            widgetManager.updateAppWidget(widgetId, views)
//        }
//
//
//        internal fun getPreferenceName(widgetId: Int): String {
//            return WidgetReceiveConfigureActivity.PREF_PREFIX_KEY + widgetId
//        }
//
//        internal fun getPrefs(context: Context, widgetId: Int): SharedPreferences {
//            return context.getSharedPreferences(getPreferenceName(widgetId), Context.MODE_PRIVATE)
//        }
//
//        internal fun getFontBitmap(context: Context, prefs: SharedPreferences, value: String): Bitmap {
//
//            var text = prefs.getString("text",
//                    context.getString(R.string.pref_default_widget_receive_text))
//
//            text = StringEscapeUtils.unescapeJava(
//                    App.getInstance().replaceKeywords(text, prefs.getString("key", ""), value))
//            val textLines = text!!.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
//
//            val fontSize = Integer.parseInt(prefs.getString("fontSize", "24"))
//            val fontColor = prefs.getInt("fontColor", Color.WHITE)
//
//            val fontSizePX = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, fontSize.toFloat(),
//                    context.resources.displayMetrics).toInt()
//            val textPadding = fontSizePX / 5
//            val paint = Paint()
//            var typeface = Typeface.createFromAsset(context.assets, "fontawesome.ttf")
//            if (prefs.getBoolean("useCustomFont", false)) {
//                try {
//                    typeface = Typeface.createFromFile(prefs.getString("fontFile", ""))
//                } catch (re: RuntimeException) {
//                    App.logError(re.message)
//                }
//
//            }
//
//            paint.isAntiAlias = true
//            paint.typeface = typeface
//            paint.color = fontColor
//            paint.textSize = fontSizePX.toFloat()
//
//            val textWidth = getLongestStringMeasure(paint, textLines) + textPadding * 2
//
//            val lineCount = if (textLines.size > 0) textLines.size else 1
//            val height = (lineCount * fontSizePX / 0.85).toInt()
//            val bitmap = Bitmap.createBitmap(textWidth, height, Bitmap.Config.ARGB_4444)
//            val canvas = Canvas(bitmap)
//
//            var textAlign: Paint.Align = Paint.Align.CENTER
//            var textX = canvas.width / 2
//            when (prefs.getString("textAlign", "0")) {
//                "0" -> {
//                    textAlign = Paint.Align.LEFT
//                    textX = textPadding
//                }
//
//                "2" -> {
//                    textAlign = Paint.Align.RIGHT
//                    textX = canvas.width - textPadding
//                }
//            }
//            paint.textAlign = textAlign
//
//            var textY = fontSizePX
//            for (line in textLines) {
//                canvas.drawText(line, textX.toFloat(), textY.toFloat(), paint)
//                textY += (-paint.ascent() + paint.descent()).toInt()
//            }
//
//            return bitmap
//        }
//
//        fun getLongestStringMeasure(paint: Paint, array: Array<String>): Int {
//            var maxWidth = 0f
//            for (s in array) {
//                val textWidth = paint.measureText(s)
//                if (textWidth > maxWidth) {
//                    maxWidth = textWidth
//                }
//            }
//
//            return maxWidth.toInt()
//        }
//    }


}


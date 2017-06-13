package kg.delletenebre.serialmanager2.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import org.apache.commons.lang3.StringEscapeUtils;

import kg.delletenebre.serialmanager2.App;
import kg.delletenebre.serialmanager2.R;

public class WidgetReceive extends AppWidgetProvider {

    private static final int[] sImageViewIds = {
            R.id.image_text_top_left,
            R.id.image_text_top_center,
            R.id.image_text_top_right,
            R.id.image_text_middle_left,
            R.id.image_text_middle_center,
            R.id.image_text_middle_right,
            R.id.image_text_bottom_left,
            R.id.image_text_bottom_center,
            R.id.image_text_bottom_right
    };

    static void updateWidget(Context context, AppWidgetManager widgetManager,
                                int widgetId, String value) {

        SharedPreferences prefs = getPrefs(context, widgetId);

        int position = Integer.parseInt(prefs.getString("position", "4"));

        int imageViewId = sImageViewIds[position];

        int backgroundColor = prefs.getInt("backgroundColor", Color.parseColor("#88000000"));

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_receive);
        views.setInt(R.id.container, "setBackgroundColor", backgroundColor);

        for (int id : sImageViewIds) {
            views.setViewVisibility(id, View.GONE);
        }
        views.setViewVisibility(imageViewId, View.VISIBLE);
        views.setImageViewBitmap(imageViewId, getFontBitmap(context, prefs, value));


        // onClick open configure activity
        Intent intent = new Intent(context, WidgetReceiveConfigureActivity.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.container, pendingIntent);

        widgetManager.updateAppWidget(widgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager widgetManager, int[] widgetIds) {
        for (int widgetId : widgetIds) {
            updateWidget(context, widgetManager, widgetId, "---");
        }
    }

    @Override
    public void onDeleted(Context context, int[] widgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        for (int widgetId : widgetIds) {
            WidgetReceiveConfigureActivity.deleteTitlePref(context, widgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        String key = "";
        String val = "";

        if (intent.getAction().equals(App.ACTION_COMMAND_RECEIVED)) {
            key = intent.getStringExtra("key");
            val = intent.getStringExtra("value");
        }

        if (!key.isEmpty()) {
            context = context.getApplicationContext();
            AppWidgetManager widgetManager =
                    AppWidgetManager.getInstance(context);

            ComponentName thisWidget = new ComponentName(context, WidgetReceive.class);

            int[] widgetIds = widgetManager.getAppWidgetIds(thisWidget);
            for (int widgetId : widgetIds) {
                SharedPreferences prefs = getPrefs(context, widgetId);
                if (prefs.getString("key", "").equals(key)) {
                    updateWidget(context, widgetManager, widgetId, val);
                }
            }
        }
    }



    static String getPreferenceName(int widgetId) {
        return WidgetReceiveConfigureActivity.PREF_PREFIX_KEY + widgetId;
    }

    static SharedPreferences getPrefs(Context context, int widgetId) {
        return context.getSharedPreferences(getPreferenceName(widgetId), Context.MODE_PRIVATE);
    }

    static Bitmap getFontBitmap(Context context, SharedPreferences prefs, String value) {

        String text = prefs.getString("text",
                context.getString(R.string.pref_default_widget_receive_text));

        text = StringEscapeUtils.unescapeJava(
                App.getInstance().replaceKeywords(text, prefs.getString("key", ""), value));
        String[] textLines = text.split("\n");

        int fontSize = Integer.parseInt(prefs.getString("fontSize", "24"));
        int fontColor = prefs.getInt("fontColor", Color.WHITE);

        int fontSizePX = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, fontSize,
                context.getResources().getDisplayMetrics());
        int textPadding = (fontSizePX / 5);
        Paint paint = new Paint();
        Typeface typeface = Typeface.createFromAsset(context.getAssets(), "fontawesome.ttf");
        if (prefs.getBoolean("useCustomFont", false)) {
            try {
                typeface = Typeface.createFromFile(prefs.getString("fontFile", ""));
            } catch (RuntimeException re) {
                App.logError(re.getMessage());
            }
        }

        paint.setAntiAlias(true);
        paint.setTypeface(typeface);
        paint.setColor(fontColor);
        paint.setTextSize(fontSizePX);

        int textWidth = getLongestStringMeasure(paint, textLines) + textPadding * 2;

        int lineCount = textLines.length > 0 ? textLines.length : 1;
        int height = (int) (lineCount * fontSizePX / 0.85);
        Bitmap bitmap = Bitmap.createBitmap(textWidth, height, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(bitmap);

        Paint.Align textAlign = Paint.Align.CENTER;
        int textX = canvas.getWidth() / 2;
        switch (prefs.getString("textAlign", "0")) {
            case "0":
                textAlign = Paint.Align.LEFT;
                textX = textPadding;
                break;

            case "2":
                textAlign = Paint.Align.RIGHT;
                textX = canvas.getWidth() - textPadding;
                break;
        }
        paint.setTextAlign(textAlign);

        int textY = fontSizePX;
        for (String line: textLines) {
            canvas.drawText(line, textX, textY, paint);
            textY += -paint.ascent() + paint.descent();
        }

        return bitmap;
    }

    public static int getLongestStringMeasure(Paint paint, String[] array) {
        float maxWidth = 0;
        for (String s : array) {
            float textWidth = paint.measureText(s);
            if (textWidth > maxWidth) {
                maxWidth = textWidth;
            }
        }

        return (int) maxWidth;
    }


}


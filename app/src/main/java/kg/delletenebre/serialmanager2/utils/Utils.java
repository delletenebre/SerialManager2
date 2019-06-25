package kg.delletenebre.serialmanager2.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatDrawableManager;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import kg.delletenebre.serialmanager2.App;

public class Utils {
    private static final String TAG = "SerialManager2";

    static String getAssetsFileContent(String path) {
        String json = "";
        try {
            InputStream is = App.getInstance().getAssets().open(path);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return json;
    }

    public static boolean isNumber(String string) {
        return string.matches("-?\\d+(\\.\\d+)?");
    }

    public static boolean hasPermission(Context context, String permission) {
        return ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static int getStatusBarHeight() {
        int result = 0;
        int resourceId = Resources.getSystem()
                .getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = Resources.getSystem().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static int getNavigationBarHeight() {
        int result = 0;
        int resourceId = Resources.getSystem()
                .getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = Resources.getSystem().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static int[] getScreenSize(WindowManager windowManager) {
        Display display = windowManager.getDefaultDisplay();
        int realWidth;
        int realHeight;

        if (Build.VERSION.SDK_INT >= 17){
            //new pleasant way to get real metrics
            DisplayMetrics realMetrics = new DisplayMetrics();
            display.getRealMetrics(realMetrics);
            realWidth = realMetrics.widthPixels;
            realHeight = realMetrics.heightPixels;

        } else if (Build.VERSION.SDK_INT >= 14) {
            //reflection for this weird in-between time
            try {
                Method mGetRawH = Display.class.getMethod("getRawHeight");
                Method mGetRawW = Display.class.getMethod("getRawWidth");
                realWidth = (Integer) mGetRawW.invoke(display);
                realHeight = (Integer) mGetRawH.invoke(display);
            } catch (Exception e) {
                //this may not be 100% accurate, but it's all we've got
                realWidth = display.getWidth();
                realHeight = display.getHeight();
            }

        } else {
            //This should be close, as lower API devices should not have window navigation bars
            realWidth = display.getWidth();
            realHeight = display.getHeight();
        }

        return new int[]{realWidth, realHeight};
    }

    public static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
        Drawable drawable = AppCompatDrawableManager.get().getDrawable(context, drawableId);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            drawable = (DrawableCompat.wrap(drawable)).mutate();
        }
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public static Bitmap tintBitmap(Bitmap bitmap, int color) {
        Paint paint = new Paint();
        paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
        Bitmap bitmapResult = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapResult);
        canvas.drawBitmap(bitmap, 0, 0, paint);

        return bitmapResult;
    }

    public static TextView setTextAppearence(TextView textView, int resId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            textView.setTextAppearance(resId);
        } else {
            textView.setTextAppearance(textView.getContext(), resId);
        }

        return textView;
    }

    public static String getIpAddress() {
        String type = "wlan"; // wlan, eth, sit, lo
        try {
            for (Enumeration en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface anInterface = (NetworkInterface) en.nextElement();

                if (anInterface.getName().startsWith(type)) {
                    for (Enumeration enumIpAddress = anInterface.getInetAddresses();
                         enumIpAddress.hasMoreElements();) {
                        InetAddress inetAddress = (InetAddress) enumIpAddress.nextElement();
                        if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                            return inetAddress.getHostAddress();
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e(TAG, "Socket exception in GetIP Address of Utilities" + ex.toString());
        }

        return "0.0.0.0";
    }

    public static int getStringIdentifier(Context context, String name) {
        return context.getResources().getIdentifier(name, "string", context.getPackageName());
    }

    public static int getResourceId(Context context, String name, String type) {
        return context.getResources().getIdentifier(name, type, context.getPackageName());
    }

    public static int getBooleanIdentifier(Context context, String name) {
        return context.getResources().getIdentifier(name, "bool", context.getPackageName());
    }
    public static int getIdIdentifier(Context context, String name) {
        return context.getResources().getIdentifier(name, "id", context.getPackageName());
    }
}

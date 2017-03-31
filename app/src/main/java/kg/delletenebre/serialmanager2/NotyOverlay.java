package kg.delletenebre.serialmanager2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.WindowManager;
import android.widget.TextView;

class NotyOverlay {

    private WindowManager mWindowManager;
    private View mNotificationLayout;
    private TextView mMessageView;
    private Context mContext;
    private WindowManager.LayoutParams mNotificationLayoutParams;
    private int mShowDuration = 5000;


    NotyOverlay(Context context) {
        mContext = context;
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);

        LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        mNotificationLayout = layoutInflater.inflate(R.layout.noty_overlay, null);

        mNotificationLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,

                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,//WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,

                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,

                PixelFormat.TRANSLUCENT);

        mMessageView = (TextView) mNotificationLayout.findViewById(R.id.message);
    }

    public NotyOverlay setTextSize(int size) {
        mMessageView.setTextSize(size);
        return this;
    }

    public NotyOverlay setTextColor(int color) {
        mMessageView.setTextColor(color);
        return this;
    }

    NotyOverlay setBackgroundColor(int color) {
        mNotificationLayout.setBackgroundColor(color);
        return this;
    }

    NotyOverlay setPosition(int positionX, int positionY,
                                   int offsetX, int offsetY) {
        mNotificationLayoutParams.gravity = getGravity(positionX, positionY);
        mNotificationLayoutParams.x = offsetX;
        mNotificationLayoutParams.y = offsetY;
        return this;
    }

    void show(String message, int duration) {
        if (Build.VERSION.SDK_INT < 23
                || (Build.VERSION.SDK_INT >= 23 && Settings.canDrawOverlays(mContext))) {

            if (mNotificationLayout.getWindowToken() != null) {
                mWindowManager.removeView(mNotificationLayout);
            }

            mShowDuration = duration;

//            DisplayMetrics displayMetrics = new DisplayMetrics();
//            mWindowManager.getDefaultDisplay().getMetrics(displayMetrics);

//            int screenWidth = displayMetrics.widthPixels;
//            int screenHeight = displayMetrics.heightPixels;

            mMessageView.setText(message);

            mNotificationLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    remove();
                }
            });

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    mNotificationLayout.setAlpha(0.0f);
                    mWindowManager.addView(mNotificationLayout, mNotificationLayoutParams);
                    mNotificationLayout.post(mShowRunnable);
                    mNotificationLayout.postDelayed(mHideRunnable, mShowDuration);
                }
            });
        } else if (Build.VERSION.SDK_INT >= 23) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + mContext.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK
                    | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            mContext.startActivity(intent);
        }
    }

    private Runnable mShowRunnable = new Runnable() {
        @Override
        public void run() {
            mNotificationLayout.animate().alpha(1.0f);
        }
    };
    private Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            if (mNotificationLayout != null) {
                ViewPropertyAnimator animator = mNotificationLayout.animate().alpha(0.0f);
                animator.setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        remove();
                    }
                });
            }
        }
    };

    public void remove() {
        if (mNotificationLayout != null) {
            mNotificationLayout.removeCallbacks(mShowRunnable);
            mNotificationLayout.removeCallbacks(mHideRunnable);
            mWindowManager.removeView(mNotificationLayout);
            mNotificationLayout = null;
        }
    }

    private int getStatusBarHeight(Resources resources) {
        int result = 0;
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId);
        }
        return result;
    }

    private int getGravity(String vert, String horiz) {
        return getVertGravity(vert) | getHorizGravity(horiz);
    }

    private int getGravity(int x, int y) {
        String[] positionsX = {"left", "center", "right"};
        String[] positionsY = {"top", "center", "bottom"};

        return getVertGravity(positionsY[y]) | getHorizGravity(positionsX[x]);
    }

    private int getVertGravity(String vertPosition) {
        switch (vertPosition) {
            case "top":
                return Gravity.TOP;

            case "center":
                return Gravity.CENTER_VERTICAL;

            case "bottom":
                return Gravity.BOTTOM;

            default:
                return Gravity.TOP;
        }
    }

    private int getHorizGravity(String horizPosition) {
        switch (horizPosition) {
            case "left":
                return Gravity.LEFT;

            case "center":
                return Gravity.CENTER_HORIZONTAL;

            case "right":
                return Gravity.RIGHT;

            default:
                return Gravity.CENTER_HORIZONTAL;
        }
    }
}

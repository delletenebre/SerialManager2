package kg.delletenebre.serialmanager2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
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
    private WindowManager.LayoutParams mNotificationLayoutParams;
    private int mShowDuration = 5000;


    NotyOverlay(Context context, int positionZ) {
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        mNotificationLayout = layoutInflater.inflate(R.layout.noty_overlay, null);

        int layoutType = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR; //HIGH
        if (positionZ == 1) {
            layoutType = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT; //LOW
        }

        mNotificationLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,

                layoutType,

                // Keeps the button presses from going to the background window
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        // Enables the notification to recieve touch events
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        // Draws over status bar
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,

                PixelFormat.TRANSLUCENT);

        mMessageView = mNotificationLayout.findViewById(R.id.message);
    }

    NotyOverlay setTextSize(int size) {
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
        if (App.getInstance().isSystemOverlaysPermissionGranted()) {
            if (mNotificationLayout.getWindowToken() != null) {
                mWindowManager.removeView(mNotificationLayout);
            }

            mShowDuration = duration;
            mMessageView.setText(message);

            mNotificationLayout.setOnClickListener(view -> remove());

            new Handler(Looper.getMainLooper()).post(() -> {
                mNotificationLayout.setAlpha(0.0f);
                mWindowManager.addView(mNotificationLayout, mNotificationLayoutParams);
                mNotificationLayout.post(mShowRunnable);
                mNotificationLayout.postDelayed(mHideRunnable, mShowDuration);
            });
        } else if (Build.VERSION.SDK_INT >= 23) {
            App.logError("SYSTEM_ALERT_WINDOW permission is not granted");
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

    private int getGravity(int x, int y) {
        String[] positionsX = {"left", "center", "right"};
        String[] positionsY = {"top", "center", "bottom"};

        return getVerticalGravity(positionsY[y]) | getHorizontalGravity(positionsX[x]);
    }

    private int getVerticalGravity(String positionY) {
        switch (positionY) {
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

    @SuppressLint("RtlHardcoded")
    private int getHorizontalGravity(String positionX) {
        switch (positionX) {
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

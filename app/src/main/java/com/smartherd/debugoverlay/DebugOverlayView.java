package com.smartherd.debugoverlay;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;

/**
 * Floating view responsible for displaying the statistics and handling drag movement.
 */
public class DebugOverlayView extends FrameLayout implements DebugStatsCollector.StatsUpdateListener {

    private final WindowManager windowManager;
    private final WindowManager.LayoutParams params;
    private float initialTouchX, initialTouchY;
    private int initialX, initialY;
    private final TextView statsTextView;

    public DebugOverlayView(@NonNull Context context) {
        super(context);

        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        // --- Setup Content (TextView) ---
        statsTextView = new TextView(context);
        statsTextView.setBackgroundColor(Color.argb(210, 30, 30, 30)); // Dark semi-transparent
        statsTextView.setTextColor(Color.GREEN);
        statsTextView.setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6));
        statsTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        statsTextView.setText("Overlay Starting...");

        // Use WRAP_CONTENT for the view itself
        addView(statsTextView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));

        // --- Setup Window Manager Parameters ---
        // Use TYPE_APPLICATION_OVERLAY for modern Android versions
        int layoutType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                // FLAG_NOT_FOCUSABLE: allows touching through to the app below
                // FLAG_NOT_TOUCH_MODAL: essential for dragging and passing touches
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = dpToPx(100);
    }

    public WindowManager.LayoutParams getLayoutParams() {
        return params;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    /**
     * Updates the TextView with the latest performance metrics.
     */
    @Override
    public void onStatsUpdated(DebugStatsCollector.StatsData data) {
        String networkStatus = data.networkCallCount > 0 ?
                String.format("%dms (Total: %d)", data.lastRequestLatencyMs, data.networkCallCount) :
                "None";

        // This correctly formats the double cpuUsage to one decimal place.
        String statsText = String.format(
                "FPS: %d\nMEM: %d MB\nCPU: %.1f %%\nNET: %s",
                data.fps,
                data.usedMemoryMB,
                data.cpuUsage,
                networkStatus
        );
        statsTextView.setText(statsText);
    }

    // --- Dragging functionality ---
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialX = params.x;
                initialY = params.y;
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                return true;

            case MotionEvent.ACTION_MOVE:
                int deltaX = (int) (event.getRawX() - initialTouchX);
                int deltaY = (int) (event.getRawY() - initialTouchY);

                params.x = initialX + deltaX;
                params.y = initialY + deltaY;

                windowManager.updateViewLayout(this, params);
                return true;

            case MotionEvent.ACTION_UP:
                return true;
        }
        return super.onTouchEvent(event);
    }
}

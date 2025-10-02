package com.smartherd.debugoverlay;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

/**
 * Custom View responsible for rendering the debug overlay and handling drag gestures.
 * It implements the StatsUpdateListener to receive and display real-time data.
 * Now loads its design from an XML layout file (debug_overlay.xml).
 */
public class DebugOverlayView extends LinearLayout implements DebugStatsCollector.StatsUpdateListener {

    private final WindowManager windowManager;
    private WindowManager.LayoutParams params;

    // --- Design Elements (IDs match XML) ---
    private TextView fpsTextView;
    private TextView memoryTextView;
    private TextView cpuTextView;
    private TextView networkTextView;

    // --- Drag State ---
    private float initialTouchX;
    private float initialTouchY;
    private int initialX;
    private int initialY;
    private long touchStartTime;
    private static final int CLICK_ACTION_THRESHOLD = 200; // ms

    public DebugOverlayView(Context context) {
        super(context);
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        initView(context);
    }

    // Kept dpToPx for drag implementation's click threshold
    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }

    private void initView(Context context) {
        // *** Inflate the XML Layout ***
        // Inflates res/layout/debug_overlay.xml into this LinearLayout
        // NOTE: R.layout.debug_overlay must be available in your project resources
        try {
            LayoutInflater.from(context).inflate(context.getResources().getIdentifier("debug_overlay", "layout", context.getPackageName()), this, true);
        } catch (Exception e) {
            // Fallback or error handling if R.layout.debug_overlay is not found
            // In a standard Android project, this should use R.layout.debug_overlay
            // For environments where R.layout is generated dynamically, this tries to find it.
            LayoutInflater.from(context).inflate(com.smartherd.debugoverlay.R.layout.debug_overlay, this, true);
        }

        // --- Retrieve Views by ID ---
        fpsTextView = findViewById(com.smartherd.debugoverlay.R.id.fps_text);
        memoryTextView = findViewById(com.smartherd.debugoverlay.R.id.memory_text);
        cpuTextView = findViewById(com.smartherd.debugoverlay.R.id.cpu_text);
        networkTextView = findViewById(com.smartherd.debugoverlay.R.id.network_text);
    }

    /**
     * Defines the WindowManager layout parameters for the overlay.
     */
    public WindowManager.LayoutParams getLayoutParams() {
        // Define the WindowManager layout parameters
        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Use TYPE_APPLICATION_OVERLAY for modern Android versions (API 26+)
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            // Deprecated, but necessary for older Android versions (API < 26)
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                // Add FLAG_NOT_FOCUSABLE so the overlay doesn't steal focus from the app
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );

        // Default position: Top-Right
        params.gravity = Gravity.TOP | Gravity.END;
        params.x = dpToPx(10); // Margin from the edge
        params.y = dpToPx(100); // Offset from the top

        return params;
    }

    // --- Data Update Listener Implementation ---

    @Override
    public void onStatsUpdated(DebugStatsCollector.StatsData data) {
        // Must update UI elements on the main thread
        post(new Runnable() {
            @Override
            public void run() {
                if (fpsTextView != null) {
                    fpsTextView.setText(String.format("FPS: %d", data.fps));
                }
                if (memoryTextView != null) {
                    memoryTextView.setText(String.format("Memory: %d MB", data.usedMemoryMB));
                }
                if (cpuTextView != null) {
                    cpuTextView.setText(String.format("CPU: %.1f%%", data.cpuUsage));
                }
                if (networkTextView != null) {
                    String netText = data.networkCallCount > 0 ?
                            String.format("Net: %dms (%d calls)", data.lastRequestLatencyMs, data.networkCallCount) :
                            String.format("Net: N/A (0 calls)");
                    networkTextView.setText(netText);
                }
            }
        });
    }

    // --- Drag Implementation ---

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 1. Record initial state for drag and click detection
                initialX = params.x;
                initialY = params.y;
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                touchStartTime = SystemClock.elapsedRealtime();
                return true;

            case MotionEvent.ACTION_MOVE:
                // 2. Calculate the difference from the initial touch point
                int deltaX = (int) (event.getRawX() - initialTouchX);
                int deltaY = (int) (event.getRawY() - initialTouchY);

                // 3. Update the parameters and reposition the view
                // FIX APPLIED HERE: Subtract deltaX instead of adding it.
                // When gravity is Gravity.END, moving right (positive deltaX) must decrease params.x (margin from right).
                params.x = initialX - deltaX;
                params.y = initialY + deltaY;
                windowManager.updateViewLayout(this, params);
                return true;

            case MotionEvent.ACTION_UP:
                long elapsedTime = SystemClock.elapsedRealtime() - touchStartTime;

                // If the touch was quick and didn't move far, treat it as a click/tap
                if (elapsedTime < CLICK_ACTION_THRESHOLD &&
                        Math.abs(event.getRawX() - initialTouchX) < dpToPx(5) &&
                        Math.abs(event.getRawY() - initialTouchY) < dpToPx(5)) {
                    // This was a click, not a drag.
                    return true;
                }

                // It was a successful drag
                return true;
        }
        return super.onTouchEvent(event);
    }
}

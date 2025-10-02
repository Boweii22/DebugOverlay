package com.smartherd.debugoverlay;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

class OverlayView extends FrameLayout {
    private final StatsCollector stats;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final TextView fpsTv, memTv, threadTv;
    private final LinearLayout logContainer;
    private final ScrollView logScroll;
    private final TextView toggleBtn;
    private boolean logsVisible = false;

    private OverlayView(Context context,
                        boolean showFps,
                        boolean showMemory,
                        boolean showThreads,
                        boolean showNetwork) {
        super(context);
        stats = new StatsCollector();

        setBackgroundColor(0xCC222222);
        setPadding(16, 16, 16, 16);
        setElevation(20);

        // Vertical container
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setLayoutParams(new LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
        ));

        fpsTv = makeText("FPS: --");
        memTv = makeText("MEM: --");
        threadTv = makeText("THR: --");

        if (showFps) container.addView(fpsTv);
        if (showMemory) container.addView(memTv);
        if (showThreads) container.addView(threadTv);

        // Toggle button
        toggleBtn = makeText("▼ Show Logs");
        toggleBtn.setTextColor(Color.CYAN);
        toggleBtn.setPadding(8, 8, 8, 8);
        toggleBtn.setOnClickListener(v -> toggleLogs());
        if (showNetwork) container.addView(toggleBtn);

        // Logs container
        logScroll = new ScrollView(context);
        logScroll.setVisibility(View.GONE);
        logScroll.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                dpToPx(150)
        ));

        logContainer = new LinearLayout(context);
        logContainer.setOrientation(LinearLayout.VERTICAL);
        logScroll.addView(logContainer);

        container.addView(logScroll);

        addView(container);

        // Make draggable
        setOnTouchListener(new OnTouchListener() {
            float dX, dY;
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = v.getX() - e.getRawX();
                        dY = v.getY() - e.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        v.setX(e.getRawX() + dX);
                        v.setY(e.getRawY() + dY);
                        return true;
                }
                return false;
            }
        });

        main.post(update);
    }

    private void toggleLogs() {
        logsVisible = !logsVisible;
        logScroll.setVisibility(logsVisible ? View.VISIBLE : View.GONE);
        toggleBtn.setText(logsVisible ? "▲ Hide Logs" : "▼ Show Logs");
    }

    private TextView makeText(String text) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        return tv;
    }

    private final Runnable update = new Runnable() {
        @Override
        public void run() {
            fpsTv.setText("FPS: " + stats.getFps());
            memTv.setText("MEM: " + stats.getMemoryMb() + "MB");
            threadTv.setText("THR: " + stats.getThreadCount());
            main.postDelayed(this, 500);
        }
    };

    static void attachToActivity(Activity activity,
                                 boolean showFps,
                                 boolean showMemory,
                                 boolean showThreads,
                                 boolean showNetwork) {
        ViewGroup root = activity.findViewById(android.R.id.content);
        OverlayView v = new OverlayView(activity, showFps, showMemory, showThreads, showNetwork);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        root.addView(v, lp);
        StatsCollector.registerOverlay(v);
    }

    void addNetworkLog(String log) {
        TextView tv = makeText(log);
        tv.setTextColor(Color.GREEN);
        logContainer.addView(tv);

        // Auto-scroll to bottom
        main.post(() -> logScroll.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}

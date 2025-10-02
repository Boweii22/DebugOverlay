package com.smartherd.debugoverlay;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;
import android.view.Choreographer;

import androidx.annotation.Nullable;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

/**
 * Collects FPS, Memory, and CPU usage statistics in a background thread using official Android APIs.
 * It uses the StatsUpdateListener interface to deliver data back to the UI (DebugOverlayView).
 * * CPU monitoring now uses the universally available android.os.Process.getElapsedCpuTime()
 * to bypass file permission and API resolution issues.
 */
public class DebugStatsCollector implements Choreographer.FrameCallback {

    private static final String TAG = "DebugStatsCollector";
    private static final long UPDATE_INTERVAL_MS = 1000; // Update metrics every 1 second

    // --- State & Handlers ---
    private final Context context;
    private Handler handler;
    private volatile boolean isRunning = false;
    private final ActivityManager activityManager;

    // --- CPU Usage Tracking (Universal API Implementation) ---
    // Total time this process has actively spent on the CPU (in milliseconds).
    private long processCpuTimeBefore = 0L;
    // Wall-clock time elapsed since boot (in milliseconds).
    private long systemTimeBefore = 0L;

    // --- FPS Tracking ---
    private long lastFrameTimeNanos = 0;
    private final LinkedList<Long> frameIntervalsNs = new LinkedList<>();
    private static final int MAX_FRAME_SAMPLES = 30;

    // --- Data Storage ---
    private final StatsData currentStatsData = new StatsData();

    // --- Listener ---
    @Nullable
    private StatsUpdateListener listener;

    /**
     * Interface for components that wish to receive performance updates.
     */
    public interface StatsUpdateListener {
        void onStatsUpdated(StatsData data);
    }

    /**
     * Data structure to hold the collected statistics.
     */
    public static class StatsData {
        public int fps = 0;
        public int usedMemoryMB = 0;
        public double cpuUsage = 0.0;
        public long lastRequestLatencyMs = 0;
        public int networkCallCount = 0;
    }

    public DebugStatsCollector(Context context, @Nullable StatsUpdateListener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        this.activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    }

    public void setListener(@Nullable StatsUpdateListener listener) {
        this.listener = listener;
    }

    /**
     * Starts the data collection thread (CPU/MEM/NET reporting) and FPS tracking.
     */
    public void start() {
        if (!isRunning) {
            isRunning = true;

            // Capture initial times before starting the periodic update
            // We use the universal API here, so no Build checks are needed.
            processCpuTimeBefore = Process.getElapsedCpuTime();
            systemTimeBefore = SystemClock.elapsedRealtime();

            handler = new Handler(Looper.getMainLooper());
            handler.post(statsRunnable);

            // START REAL FPS TRACKING
            Choreographer.getInstance().postFrameCallback(this);

            Log.d(TAG, "DebugStatsCollector started.");
        }
    }

    /**
     * Stops the data collection thread and FPS tracking.
     */
    public void stop() {
        isRunning = false;
        if (handler != null) {
            handler.removeCallbacks(statsRunnable);
            handler = null;
        }

        // STOP REAL FPS TRACKING
        Choreographer.getInstance().removeFrameCallback(this);
        frameIntervalsNs.clear();
        lastFrameTimeNanos = 0;

        Log.d(TAG, "DebugStatsCollector stopped.");
    }

    // --- FPS Implementation (Unchanged) ---

    @Override
    public void doFrame(long frameTimeNanos) {
        if (!isRunning) return;

        if (lastFrameTimeNanos > 0) {
            long interval = frameTimeNanos - lastFrameTimeNanos;
            frameIntervalsNs.add(interval);
            if (frameIntervalsNs.size() > MAX_FRAME_SAMPLES) {
                frameIntervalsNs.removeFirst();
            }
        }
        lastFrameTimeNanos = frameTimeNanos;

        Choreographer.getInstance().postFrameCallback(this);
    }

    private void calculateFps() {
        if (frameIntervalsNs.isEmpty()) {
            currentStatsData.fps = 0;
            return;
        }

        long totalIntervalNs = 0;
        for (long interval : frameIntervalsNs) {
            totalIntervalNs += interval;
        }

        long averageIntervalNs = totalIntervalNs / frameIntervalsNs.size();
        int calculatedFps = (int) (TimeUnit.SECONDS.toNanos(1) / averageIntervalNs);
        currentStatsData.fps = Math.min(60, calculatedFps);
    }

    // --- Core Data Reporting Runnable ---

    private final Runnable statsRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isRunning) return;

            // Collect all real data sources
            collectMemoryStats();
            collectCpuStats(); // Using universal API
            calculateFps();

            // Report the latest data to the listener (DebugOverlayView)
            if (listener != null) {
                listener.onStatsUpdated(currentStatsData);
            }

            // Schedule the next run
            handler.postDelayed(this, UPDATE_INTERVAL_MS);
        }
    };

    /**
     * Uses ActivityManager to get current process memory usage (Unchanged).
     */
    private void collectMemoryStats() {
        try {
            android.os.Debug.MemoryInfo[] memoryInfos = activityManager.getProcessMemoryInfo(new int[]{Process.myPid()});
            if (memoryInfos.length > 0) {
                int pssMb = memoryInfos[0].getTotalPss() / 1024;
                currentStatsData.usedMemoryMB = pssMb;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to collect memory stats.", e);
        }
    }

    /**
     * Calculates the process CPU usage percentage using the universal Android Process API (API 1+).
     */
    private void collectCpuStats() {
        try {
            // Get current CPU usage time for THIS process (in milliseconds)
            long processCpuTimeAfter = Process.getElapsedCpuTime();
            // Get wall-clock time elapsed since boot (in milliseconds)
            long systemTimeAfter = SystemClock.elapsedRealtime();

            // Check if initial values were captured
            if (processCpuTimeBefore == 0L || systemTimeBefore == 0L) {
                // Initialize the 'before' values on the first run
                processCpuTimeBefore = processCpuTimeAfter;
                systemTimeBefore = systemTimeAfter;
                currentStatsData.cpuUsage = 0.0;
                return;
            }

            // Calculate deltas, working entirely in milliseconds
            long processDelta = processCpuTimeAfter - processCpuTimeBefore; // in milliseconds
            long systemDeltaMs = systemTimeAfter - systemTimeBefore; // in milliseconds

            if (systemDeltaMs > 0) {
                // CPU Usage = (Process CPU Time Delta / System Wall Clock Time Delta) * 100
                double cpuUsage = (double) processDelta * 100.0 / systemDeltaMs;

                // Clamping to a maximum of 100%
                currentStatsData.cpuUsage = Math.min(100.0, cpuUsage);
            } else {
                currentStatsData.cpuUsage = 0.0;
            }

            // Update 'before' values for the next iteration
            processCpuTimeBefore = processCpuTimeAfter;
            systemTimeBefore = systemTimeAfter;

        } catch (Exception e) {
            Log.e(TAG, "Failed to collect CPU stats using universal Process API.", e);
            currentStatsData.cpuUsage = 0.0;
        }
    }

    // --- Network API implementation (Unchanged) ---

    public void updateNetworkStats(long durationMs) {
        currentStatsData.lastRequestLatencyMs = durationMs;
        currentStatsData.networkCallCount++;
        Log.d(TAG, String.format("Network request finished in %dms. Total calls: %d", durationMs, currentStatsData.networkCallCount));
    }
}

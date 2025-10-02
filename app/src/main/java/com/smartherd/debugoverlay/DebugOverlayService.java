package com.smartherd.debugoverlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.view.WindowManager;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class DebugOverlayService extends Service {
    private static final String TAG = "DebugOverlayService";
    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_STOP = "ACTION_STOP";

    private WindowManager windowManager;
    private DebugOverlayView debugOverlayView;
    private DebugStatsCollector statsCollector;

    // Required for Android O+ Foreground Service Notification
    private static final String CHANNEL_ID = "DebugOverlayChannel";
    private static final int NOTIFICATION_ID = 1001; // Must be a unique positive integer

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate started.");

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // Retrieve the global collector instance from the Application class
        if (getApplication() instanceof DebugOverlayApp) {
            statsCollector = ((DebugOverlayApp) getApplication()).getDebugStatsCollector();
            Log.d(TAG, "StatsCollector retrieved from DebugOverlayApp.");
        } else {
            Log.e(TAG, "FATAL: Application is NOT DebugOverlayApp! Service cannot find global collector.");
            // Fallback: create a new one, but network integration will fail.
            statsCollector = new DebugStatsCollector(this, null);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && statsCollector != null) {
            String action = intent.getAction();
            Log.d(TAG, "onStartCommand received action: " + action);

            if (ACTION_START.equals(action)) {
                // FIX: Call startForeground() immediately to satisfy Android O+ requirement
                startForeground(NOTIFICATION_ID, buildForegroundNotification());
                startOverlay();
            } else if (ACTION_STOP.equals(action)) {
                stopOverlay();
            }
        } else {
            Log.e(TAG, "onStartCommand failed. Intent was null or statsCollector is null.");
        }
        return START_STICKY;
    }

    private void startOverlay() {
        if (debugOverlayView == null) {
            Log.d(TAG, "Starting Debug Overlay components: Creating View and attempting to add to WindowManager.");

            // 1. Initialize View and set it as the collector's listener
            debugOverlayView = new DebugOverlayView(this);
            statsCollector.setListener(debugOverlayView);

            try {
                // 2. Add the View to the WindowManager
                windowManager.addView(debugOverlayView, debugOverlayView.getLayoutParams());

                // 3. Start data collection (FPS/Memory/CPU)
                statsCollector.start();
                Log.i(TAG, "Debug Overlay SHOWN successfully and collection started.");
            } catch (WindowManager.BadTokenException e) {
                // This is the common failure point if permission check was bypassed or failed.
                Log.e(TAG, "FATAL ERROR: Failed to add overlay view. Permission denied or invalid token. Check Manifest and user permissions.", e);
            } catch (Exception e) {
                Log.e(TAG, "FATAL ERROR: Unexpected exception during view setup.", e);
            }
        } else {
            Log.d(TAG, "Debug Overlay already running. Skipping start.");
        }
    }

    private void stopOverlay() {
        if (debugOverlayView != null) {
            Log.d(TAG, "Stopping Debug Overlay components.");

            // 1. Stop data collection
            statsCollector.stop();
            // 2. Clear the listener from the collector
            statsCollector.setListener(null);

            // 3. Remove the View from the WindowManager
            windowManager.removeView(debugOverlayView);
            debugOverlayView = null;
        }

        // Stop foreground state and service
        stopForeground(true); // true removes the notification
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service onDestroy.");
        if (debugOverlayView != null) {
            windowManager.removeView(debugOverlayView);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Builds the persistent notification required for the Foreground Service.
     */
    private Notification buildForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }

        // NOTE: Uses a placeholder system icon for simplicity.
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Debug Overlay Running")
                .setContentText("Collecting performance metrics in the background.")
                .setSmallIcon(android.R.drawable.stat_sys_warning) // Placeholder icon
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true) // Makes the notification non-dismissible
                .build();
    }

    /**
     * Creates the required Notification Channel for Android O (API 26) and above.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Debug Overlay Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}

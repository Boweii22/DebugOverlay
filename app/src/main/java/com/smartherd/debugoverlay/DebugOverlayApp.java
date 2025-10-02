package com.smartherd.debugoverlay;

import android.app.Application;
import android.util.Log;

/**
 * Custom Application class used to hold the singleton instance of DebugStatsCollector.
 * MUST be registered in the AndroidManifest.xml using android:name.
 */
public class DebugOverlayApp extends Application {

    private DebugStatsCollector debugStatsCollector;
    private static final String TAG = "DebugOverlayApp";

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize the collector early in the application lifecycle.
        // We pass null for the listener here, as the listener (DebugOverlayView)
        // is created later by the DebugOverlayService when the overlay starts.
        debugStatsCollector = new DebugStatsCollector(this, null);
        Log.d(TAG, "DebugStatsCollector initialized.");
    }

    /**
     * Provides the global instance of the collector to other components (Service, Network Client).
     */
    public DebugStatsCollector getDebugStatsCollector() {
        return debugStatsCollector;
    }
}

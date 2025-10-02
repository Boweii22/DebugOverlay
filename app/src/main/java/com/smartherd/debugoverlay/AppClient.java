package com.smartherd.debugoverlay;

import android.content.Context;
import okhttp3.OkHttpClient;

/**
 * Utility class demonstrating how to instantiate an OkHttpClient
 * and conditionally inject the DebugOverlay's Network Interceptor.
 * * Usage: Replace your standard OkHttpClient.Builder().build() with
 * AppClient.getClient(context).
 */
public class AppClient {
    private static OkHttpClient instance;

    public static OkHttpClient getClient(Context context) {
        if (instance == null) {
            synchronized (AppClient.class) {
                if (instance == null) {
                    OkHttpClient.Builder builder = new OkHttpClient.Builder();

                    // --- Network Integration Point ---
                    // Only add the interceptor if the application is marked as debuggable.
                    if (isDebugBuild(context)) {
                        DebugStatsCollector collector = getCollector(context);
                        if (collector != null) {
                            builder.addInterceptor(new NetworkMonitorInterceptor(collector));
                        }
                    }

                    instance = builder.build();
                }
            }
        }
        return instance;
    }

    // --- Helper Methods ---

    private static DebugStatsCollector getCollector(Context context) {
        Context appContext = context.getApplicationContext();
        if (appContext instanceof DebugOverlayApp) {
            return ((DebugOverlayApp) appContext).getDebugStatsCollector();
        }
        return null;
    }

    private static boolean isDebugBuild(Context context) {
        // Robust check for debug build status
        return (context.getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }
}

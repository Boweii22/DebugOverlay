package com.smartherd.debugoverlay;


import android.app.Application;

import okhttp3.Interceptor;

public final class DebugOverlay {
    private static DebugOverlay instance;
    private OverlayManager overlayManager;

    private DebugOverlay() { }

    public static DebugOverlay init(Application app) {
        if (instance == null) {
            instance = new DebugOverlay();
            instance.overlayManager = new OverlayManager(app);
        }
        return instance;
    }

    public DebugOverlay showFps(boolean show) {
        overlayManager.setShowFps(show);
        return this;
    }

    public DebugOverlay showMemory(boolean show) {
        overlayManager.setShowMemory(show);
        return this;
    }

    public DebugOverlay showThreads(boolean show) {
        overlayManager.setShowThreads(show);
        return this;
    }

    public DebugOverlay showNetwork(boolean show) {
        overlayManager.setShowNetwork(show);
        return this;
    }

    public static Interceptor getNetworkInterceptor() {
        return new NetworkInterceptor();
    }
}

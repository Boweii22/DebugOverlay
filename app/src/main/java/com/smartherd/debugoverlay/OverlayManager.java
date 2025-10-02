package com.smartherd.debugoverlay;


import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class OverlayManager implements Application.ActivityLifecycleCallbacks {

    private final Application app;
    private boolean showFps = true;
    private boolean showMemory = true;
    private boolean showThreads = true;
    private boolean showNetwork = true;

    OverlayManager(Application app) {
        this.app = app;
        app.registerActivityLifecycleCallbacks(this);
    }

    void setShowFps(boolean v) { showFps = v; }
    void setShowMemory(boolean v) { showMemory = v; }
    void setShowThreads(boolean v) { showThreads = v; }
    void setShowNetwork(boolean v) { showNetwork = v; }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        OverlayView.attachToActivity(activity, showFps, showMemory, showThreads, showNetwork);
    }

    @Override public void onActivityStarted(@NonNull Activity activity) {}
    @Override public void onActivityResumed(@NonNull Activity activity) {}
    @Override public void onActivityPaused(@NonNull Activity activity) {}
    @Override public void onActivityStopped(@NonNull Activity activity) {}
    @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
    @Override public void onActivityDestroyed(@NonNull Activity activity) {}
}

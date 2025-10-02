package com.smartherd.debugoverlay;


import android.app.Application;

import okhttp3.OkHttpClient;

public class myApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize overlay
        DebugOverlay.init(this)
                .showFps(true)
                .showMemory(true)
                .showThreads(true)
                .showNetwork(true);

        // Example OkHttp client (not strictly required if you don’t use OkHttp)
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(DebugOverlay.getNetworkInterceptor())
                .build();
    }
}

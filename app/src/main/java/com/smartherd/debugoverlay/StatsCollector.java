package com.smartherd.debugoverlay;

import android.os.Debug;
import android.view.Choreographer;

import java.lang.ref.WeakReference;

class StatsCollector {
    private static WeakReference<OverlayView> overlayRef = new WeakReference<>(null);
    private int fps = 0, frames = 0;
    private long lastTime = System.nanoTime();

    StatsCollector() {
        Choreographer.getInstance().postFrameCallback(frameCallback);
    }

    private final Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            frames++;
            long now = System.nanoTime();
            if (now - lastTime >= 1_000_000_000L) {
                fps = frames;
                frames = 0;
                lastTime = now;
            }
            Choreographer.getInstance().postFrameCallback(this);
        }
    };

    int getFps() { return fps; }
    int getThreadCount() { return Thread.getAllStackTraces().keySet().size(); }
    int getMemoryMb() { return (int)(Debug.getNativeHeapAllocatedSize() / 1024 / 1024); }

    static void registerOverlay(OverlayView v) {
        overlayRef = new WeakReference<>(v);
    }

    static void logNetworkEvent(String s) {
        OverlayView v = overlayRef.get();
        if (v != null) {
            v.post(() -> v.addNetworkLog(s));
        }
    }
}

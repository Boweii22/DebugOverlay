package com.smartherd.debugoverlay;
import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class NetworkInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request r = chain.request();
        long start = System.nanoTime();
        try {
            Response res = chain.proceed(r);
            long tookMs = (System.nanoTime() - start) / 1_000_000;

            String msg = r.method() + " " + r.url()
                    + " → " + res.code()
                    + " (" + tookMs + "ms)";

            // 👇 send it to overlay
            StatsCollector.logNetworkEvent(msg);

            return res;
        } catch (IOException e) {
            String msg = r.method() + " " + r.url()
                    + " → ERROR " + e.getMessage();

            StatsCollector.logNetworkEvent(msg);
            throw e;
        }
    }
}


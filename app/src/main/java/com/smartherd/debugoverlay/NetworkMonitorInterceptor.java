package com.smartherd.debugoverlay;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * An OkHttp Interceptor that captures the duration of a network request
 * and reports it back to the DebugStatsCollector.
 *
 * NOTE: This requires the OkHttp library to be included in your project dependencies
 * and must be added to your OkHttpClient instance.
 */
public class NetworkMonitorInterceptor implements Interceptor {

    private final DebugStatsCollector collector;

    public NetworkMonitorInterceptor(DebugStatsCollector collector) {
        this.collector = collector;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        long startTime = System.currentTimeMillis();

        Request request = chain.request();
        Response response;

        try {
            response = chain.proceed(request);
        } catch (IOException e) {
            // Propagate the exception if the request fails
            throw e;
        } finally {
            long durationMs = System.currentTimeMillis() - startTime;

            // Push the latency data to the collector for real-time update
            if (collector != null) {
                collector.updateNetworkStats(durationMs);
            }
        }

        return response;
    }
}

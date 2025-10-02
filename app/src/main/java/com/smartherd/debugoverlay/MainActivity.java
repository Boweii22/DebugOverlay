package com.smartherd.debugoverlay;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.os.Handler;

/**
 * Main Activity responsible for:
 * 1. Requesting the SYSTEM_ALERT_WINDOW permission.
 * 2. Providing a button to start/stop the DebugOverlayService.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_OVERLAY_PERMISSION = 101;

    private Button toggleButton;
    private TextView permissionStatusText;
    private ExecutorService executorService;
    private Handler mainHandler;
    private boolean isCpuTestRunning = false;
    Button cpuTestButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Assuming a simple layout file exists

        // Example layout references (replace with your actual layout IDs)
        toggleButton = findViewById(R.id.toggle_debug_overlay_button);
        permissionStatusText = findViewById(R.id.permission_status_text);

        // Initialize threading components for CPU test
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        toggleButton.setOnClickListener(v -> toggleOverlay());
        cpuTestButton = findViewById(R.id.btn_cpu_test);
        cpuTestButton.setOnClickListener(v -> {
            startCpuTest();
        });

        checkOverlayPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check permissions again when returning from settings
        checkOverlayPermission();
    }

    // Logic for the CPU Test button
    private void startCpuTest() {
        if (isCpuTestRunning) {
            // Test is already running
            return;
        }

        // 1. Update UI to show the test is active
        cpuTestButton.setText("CPU Test Running...");
        cpuTestButton.setEnabled(false);
        isCpuTestRunning = true;

        // 2. Submit the heavy task to the background executor
        executorService.submit(cpuTestRunnable);
    }

    // Runnable that contains the heavy, blocking calculation
    private final Runnable cpuTestRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d("CPUTest", "Starting heavy calculation...");

            long startTime = System.currentTimeMillis();
            long durationMs = 5000; // Run for 5 seconds

            // Intensive, non-IO bound calculation loop
            double result = 0;
            while (System.currentTimeMillis() - startTime < durationMs) {
                // Complex math operations to keep the CPU busy
//                result += Math.sin(Math.sqrt(Math.cos(result))) * Math.log(System.currentTimeMillis());
                //more complex, stable cpu-intensive calculation or something
                result += Math.sin(result) * Math.cos(result) / Math.PI;
                // Note: Use 'result' to prevent the compiler from optimizing the entire loop away
            }

            Log.d("CPUTest", "Calculation finished. Last result: " + result);

            // 3. Post back to the main thread to update the UI safely
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    Button cpuTestButton = findViewById(R.id.btn_cpu_test);
                    cpuTestButton.setText("Start CPU Test");
                    cpuTestButton.setEnabled(true);
                    isCpuTestRunning = false;
                }
            });
        }
    };

    /**
     * Checks if the required permission is granted and updates the UI.
     */
    private void checkOverlayPermission() {
        // Permission check is only needed for Android M (API 23) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                // Permission not granted, show request prompt
                permissionStatusText.setText("Permission: DENIED (Tap to Request)");
                permissionStatusText.setVisibility(View.VISIBLE);
                permissionStatusText.setOnClickListener(v -> requestOverlayPermission());
                toggleButton.setEnabled(false);
                stopDebugService(); // Ensure service is stopped if permission is lost
            } else {
                // Permission granted
                permissionStatusText.setText("Permission: GRANTED");
                permissionStatusText.setOnClickListener(null);
                toggleButton.setEnabled(true);
            }
        } else {
            // Pre-Android M, permission is granted via Manifest
            permissionStatusText.setVisibility(View.GONE);
            toggleButton.setEnabled(true);
        }
    }

    /**
     * Sends the user to the system settings page to grant overlay permission.
     */
    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            // Check permission immediately upon returning from settings
            checkOverlayPermission();
        }
    }

    /**
     * Toggles the debug service state.
     */
    private void toggleOverlay() {
        if (DebugOverlayServiceRunning()) {
            stopDebugService();
            toggleButton.setText("START OVERLAY");
        } else {
            startDebugService();
            toggleButton.setText("STOP OVERLAY");
        }
    }

    private void startDebugService() {
        Intent intent = new Intent(this, DebugOverlayService.class);
        intent.setAction(DebugOverlayService.ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Use startForegroundService for higher APIs
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        Log.i(TAG, "DebugOverlayService START requested.");
        Toast.makeText(this, "Debug Overlay Started", Toast.LENGTH_SHORT).show();
    }

    private void stopDebugService() {
        Intent intent = new Intent(this, DebugOverlayService.class);
        intent.setAction(DebugOverlayService.ACTION_STOP);
        stopService(intent);
        Log.i(TAG, "DebugOverlayService STOP requested.");
        Toast.makeText(this, "Debug Overlay Stopped", Toast.LENGTH_SHORT).show();
    }

    // Simplified way to check if service is 'running' based on UI state (can be improved)
    private boolean DebugOverlayServiceRunning() {
        // In a real app, you would check ActivityManager, but for simplicity,
        // we'll rely on the button text state in this example.
        return toggleButton.getText().equals("STOP OVERLAY");
    }
}

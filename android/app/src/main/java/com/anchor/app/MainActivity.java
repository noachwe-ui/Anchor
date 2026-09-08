package com.anchor.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    public static final String PREFS = "anchor_prefs";
    public static final String KEY_MODE = "bubble_mode";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
                ));
            }
        }

        startService(new Intent(this, FloatingBubbleService.class));
        handleClipIntent(getIntent());

        // After web loads, sync mode from localStorage
        getBridge().getWebView().postDelayed(this::syncModeFromWeb, 1200);
    }

    @Override
    public void onResume() {
        super.onResume();
        getBridge().getWebView().postDelayed(this::syncModeFromWeb, 500);
    }

    private void syncModeFromWeb() {
        try {
            getBridge().getWebView().evaluateJavascript(
                "localStorage.getItem('anchor-bubble-mode')",
                value -> {
                    if (value == null || value.equals("null")) return;
                    String mode = value.replace("\"", "").trim();
                    if (mode.isEmpty()) return;
                    SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
                    prefs.edit().putString(KEY_MODE, mode).apply();

                    Intent i = new Intent(this, FloatingBubbleService.class);
                    i.setAction(FloatingBubbleService.ACTION_APPLY_MODE);
                    i.putExtra("mode", mode);
                    startService(i);
                }
            );
        } catch (Exception ignored) {}
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleClipIntent(intent);
    }

    private void handleClipIntent(Intent intent) {
        if (intent == null || !intent.getBooleanExtra("open_clip", false)) return;
        getBridge().getWebView().postDelayed(() -> {
            try {
                getBridge().getWebView().evaluateJavascript(
                    "document.getElementById('clip-btn') && document.getElementById('clip-btn').click();",
                    null
                );
            } catch (Exception ignored) {}
        }, 800);
    }
}

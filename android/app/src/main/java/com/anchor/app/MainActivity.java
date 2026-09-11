package com.anchor.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
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
        seedWidgetUrls();
        handleClipIntent(getIntent());

        // Lets script.js push a bubble-mode change to native the instant
        // Save is tapped, instead of waiting for the next timed poll below
        // (which only runs right after launch/resume) — this is what was
        // making the in-app bubble settings menu appear to do nothing.
        getBridge().getWebView().addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void setBubbleMode(String mode) {
                runOnUiThread(() -> applyBubbleMode(mode));
            }
            @JavascriptInterface
            public void setWidgetColor(String hex) {
                runOnUiThread(() -> AnchorWidgetProvider.setBackgroundColor(MainActivity.this, hex));
            }
            @JavascriptInterface
            public void setWidgetAlpha(int alpha) {
                runOnUiThread(() -> AnchorWidgetProvider.setBackgroundAlpha(MainActivity.this, alpha));
            }
        }, "AnchorNative");

        // After web loads, sync mode from localStorage. script.js may not
        // have finished running yet on a slow/cold launch, in which case
        // this silently no-ops — retry a few times with backoff instead
        // of gambling on one fixed delay.
        getBridge().getWebView().postDelayed(() -> syncModeFromWeb(0), 800);
    }

    @Override
    public void onResume() {
        super.onResume();
        getBridge().getWebView().postDelayed(() -> syncModeFromWeb(0), 400);

        Intent fg = new Intent(this, FloatingBubbleService.class);
        fg.setAction(FloatingBubbleService.ACTION_APP_FOREGROUND);
        startService(fg);
        // TEMPORARY DEBUG — remove once the persistence bug is confirmed fixed.
        android.widget.Toast.makeText(this, "DEBUG: sent APP_FOREGROUND", android.widget.Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPause() {
        super.onPause();
        Intent bg = new Intent(this, FloatingBubbleService.class);
        bg.setAction(FloatingBubbleService.ACTION_APP_BACKGROUND);
        startService(bg);
    }

    private static final int[] SYNC_RETRY_DELAYS_MS = { 400, 800, 1500 };

    private void syncModeFromWeb(int attempt) {
        try {
            getBridge().getWebView().evaluateJavascript(
                "localStorage.getItem('anchor-bubble-mode')",
                value -> {
                    boolean gotValue = value != null && !value.equals("null");
                    if (!gotValue) {
                        if (attempt < SYNC_RETRY_DELAYS_MS.length) {
                            getBridge().getWebView().postDelayed(
                                () -> syncModeFromWeb(attempt + 1),
                                SYNC_RETRY_DELAYS_MS[attempt]);
                        }
                        return;
                    }
                    applyBubbleMode(value.replace("\"", "").trim());
                }
            );
        } catch (Exception ignored) {}
    }

    private void applyBubbleMode(String mode) {
        if (mode == null || mode.isEmpty()) return;
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        prefs.edit().putString(KEY_MODE, mode).apply();

        Intent i = new Intent(this, FloatingBubbleService.class);
        i.setAction(FloatingBubbleService.ACTION_APPLY_MODE);
        i.putExtra("mode", mode);
        startService(i);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleClipIntent(intent);
    }

    
    private void seedWidgetUrls() {
        // Only seed each cache the first time it's empty. Previously this
        // ran unconditionally on every launch, which would silently
        // overwrite any future per-widget URL customization on next open.
        SharedPreferences widgetPrefs = getSharedPreferences(
            AnchorWidgetProvider.PREFS, MODE_PRIVATE);
        if (widgetPrefs.getString(AnchorWidgetProvider.KEY_URLS, "").isEmpty()) {
            seedAssetToWidget("public/urls.json", true, false, false);
        }
        if (widgetPrefs.getString(AnchorWidgetProvider.KEY_DAILY, "").isEmpty()) {
            seedAssetToWidget("public/daily_dose.json", false, true, false);
        }
        if (widgetPrefs.getString(AnchorWidgetProvider.KEY_HILLEL, "").isEmpty()) {
            seedAssetToWidget("public/hillel_eisenberg.json", false, false, true);
        }
    }

    private void seedAssetToWidget(String path, boolean urls, boolean daily, boolean hillel) {
        try {
            java.io.InputStream is = getAssets().open(path);
            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            String json = sb.toString();
            if (urls) AnchorWidgetProvider.saveClipUrls(this, json);
            if (daily) AnchorWidgetProvider.saveDailyUrls(this, json);
            if (hillel) AnchorWidgetProvider.saveHillelUrls(this, json);
        } catch (Exception ignored) {}
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

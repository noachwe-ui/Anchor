package com.anchor.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.webkit.JavascriptInterface;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    public static final String PREFS = "anchor_prefs";
    public static final String KEY_MODE = "bubble_mode";

    // TEMPORARY DEBUG — same log file FloatingBubbleService writes to
    // (both are the same app, same internal storage). Remove this method
    // and all its call sites once the persistence bug is fixed.
    private void debugLog(String msg) {
        try {
            java.io.File f = new java.io.File(getFilesDir(), "anchor_debug.log");
            java.io.FileWriter fw = new java.io.FileWriter(f, true);
            fw.write(new java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US)
                .format(new java.util.Date()) + " [Main] " + msg + "\n");
            fw.close();
        } catch (Exception ignored) {}
    }

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
            @JavascriptInterface
            public void setWidgetTextColor(String hex) {
                runOnUiThread(() -> AnchorWidgetProvider.setTextColor(MainActivity.this, hex));
            }
            @JavascriptInterface
            public void setBubbleColor(String hex) {
                runOnUiThread(() -> {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                        .putString(FloatingBubbleService.KEY_BUBBLE_COLOR, hex).apply();
                    notifyBubbleAppearanceChanged();
                });
            }
            @JavascriptInterface
            public void setBubbleAlpha(int alpha) {
                runOnUiThread(() -> {
                    int clamped = Math.max(0, Math.min(255, alpha));
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                        .putInt(FloatingBubbleService.KEY_BUBBLE_ALPHA, clamped).apply();
                    notifyBubbleAppearanceChanged();
                });
            }
            @JavascriptInterface
            public void setBubbleCorner(String corner) {
                runOnUiThread(() -> {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                        .putString(FloatingBubbleService.KEY_BUBBLE_CORNER, corner).apply();
                    // Position only resets on the next full show, by design —
                    // no need to notify the service immediately here.
                });
            }
            @JavascriptInterface
            public String getDebugLog() {
                try {
                    java.io.File f = new java.io.File(getFilesDir(), "anchor_debug.log");
                    if (!f.exists()) return "(no log yet)";
                    java.io.FileInputStream fis = new java.io.FileInputStream(f);
                    java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = fis.read(buf)) != -1) bos.write(buf, 0, n);
                    fis.close();
                    return bos.toString("UTF-8");
                } catch (Exception e) {
                    return "(error reading log: " + e + ")";
                }
            }
            @JavascriptInterface
            public void clearDebugLog() {
                try {
                    java.io.File f = new java.io.File(getFilesDir(), "anchor_debug.log");
                    if (f.exists()) f.delete();
                } catch (Exception ignored) {}
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
        debugLog("onResume: sent APP_FOREGROUND");
        // TEMPORARY DEBUG — remove once the persistence bug is confirmed fixed.
        android.widget.Toast.makeText(this, "DEBUG: sent APP_FOREGROUND", android.widget.Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            Intent bg = new Intent(this, FloatingBubbleService.class);
            bg.setAction(FloatingBubbleService.ACTION_APP_BACKGROUND);
            startService(bg);
            debugLog("onPause: sent APP_BACKGROUND");
        } catch (Exception e) {
            debugLog("onPause: startService FAILED: " + Log.getStackTraceString(e));
            // TEMPORARY DEBUG — remove once the persistence bug is confirmed fixed.
            android.widget.Toast.makeText(this,
                "DEBUG: onPause startService FAILED: " + e,
                android.widget.Toast.LENGTH_LONG).show();
        }
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

    private void notifyBubbleAppearanceChanged() {
        Intent i = new Intent(this, FloatingBubbleService.class);
        i.setAction(FloatingBubbleService.ACTION_APPLY_BUBBLE_APPEARANCE);
        startService(i);
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

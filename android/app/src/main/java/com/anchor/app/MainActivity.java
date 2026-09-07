package com.anchor.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
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

        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (Exception ignored) {}

        startService(new Intent(this, FloatingBubbleService.class));
        handleClipIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleClipIntent(intent);
    }

    private void handleClipIntent(Intent intent) {
        if (intent == null || !intent.getBooleanExtra("open_clip", false)) return;
        // Click the web "Watch a Clip" button after the page loads
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

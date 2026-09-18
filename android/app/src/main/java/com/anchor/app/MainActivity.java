package com.anchor.app;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    public static final String PREFS = "anchor_prefs";
    public static final String KEY_MODE = "bubble_mode";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Configure the Capacitor WebView for inline media playback
        try {
            WebView webView = this.bridge.getWebView();
            if (webView != null) {
                WebSettings settings = webView.getSettings();
                settings.setJavaScriptEnabled(true);
                settings.setDomStorageEnabled(true);
                settings.setAllowFileAccess(true);
                settings.setMediaPlaybackRequiresUserGesture(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

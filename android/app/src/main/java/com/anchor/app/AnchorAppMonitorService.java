package com.anchor.app;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AnchorAppMonitorService extends AccessibilityService {

    private static final Set<String> TARGETS = new HashSet<>(Arrays.asList(
        "com.android.chrome", "com.chrome.beta", "com.chrome.dev",
        "com.sec.android.app.sbrowser", "org.mozilla.firefox",
        "org.mozilla.firefox_beta", "com.opera.browser", "com.brave.browser",
        "com.microsoft.emmx", "com.duckduckgo.mobile.android",
        "com.google.android.youtube",
        "com.whatsapp", "com.whatsapp.w4b",
        "com.instagram.android", "com.facebook.katana", "com.facebook.lite",
        "com.facebook.orca", "com.facebook.mlite",
        "com.zhiliaoapp.musically", "com.ss.android.ugc.trill",
        "com.twitter.android", "com.snapchat.android", "com.reddit.frontpage",
        "org.telegram.messenger", "org.telegram.messenger.web",
        "com.discord", "com.pinterest", "com.linkedin.android"
    ));

    private String lastPkg = "";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;

        CharSequence pkgCs = event.getPackageName();
        if (pkgCs == null) return;
        String pkg = pkgCs.toString();

        // ignore noise
        if (pkg.contains("systemui") || pkg.contains("inputmethod") ||
            pkg.contains("keyboard") || pkg.contains("honeyboard") ||
            pkg.equals(getPackageName())) {
            return;
        }

        if (pkg.equals(lastPkg)) return;
        lastPkg = pkg;

        boolean onTarget = TARGETS.contains(pkg) ||
                pkg.contains("chrome") || pkg.contains("whatsapp");

        Intent i = new Intent(this, FloatingBubbleService.class);
        i.setAction(onTarget ? FloatingBubbleService.ACTION_SHOW
                             : FloatingBubbleService.ACTION_HIDE);
        startService(i);
    }

    @Override
    public void onInterrupt() {}
}

package com.anchor.app;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class AnchorAppMonitorService extends AccessibilityService {

    private static final String TAG = "AnchorAppMonitor";
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

        boolean onTarget = Constants.TARGETS.contains(pkg);

        Intent i = new Intent(this, FloatingBubbleService.class);
        i.setAction(onTarget ? FloatingBubbleService.ACTION_SHOW
                             : FloatingBubbleService.ACTION_HIDE);
        try {
            startService(i);
        } catch (Exception e) {
            Log.w(TAG, "Failed to dispatch bubble action for " + pkg, e);
        }
    }

    @Override
    public void onInterrupt() {}
}

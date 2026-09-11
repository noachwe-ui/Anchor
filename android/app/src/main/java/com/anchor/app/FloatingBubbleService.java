package com.anchor.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

public class FloatingBubbleService extends Service {
    public static final String ACTION_SHOW = "com.anchor.app.SHOW_BUBBLE";
    public static final String ACTION_HIDE = "com.anchor.app.HIDE_BUBBLE";
    public static final String ACTION_APPLY_MODE = "com.anchor.app.APPLY_MODE";
    // Sent by MainActivity itself on resume/pause. These are honored
    // regardless of the current bubble mode — the bubble should never
    // float over Anchor's own screens, and reopening the app should
    // always clear a prior manual "drag to remove" dismissal.
    public static final String ACTION_APP_FOREGROUND = "com.anchor.app.APP_FOREGROUND";
    public static final String ACTION_APP_BACKGROUND = "com.anchor.app.APP_BACKGROUND";
    private static final String TAG = "FloatingBubble";

    // "targets" mode polls UsageStatsManager on this cadence when the
    // Accessibility path isn't being used. Widened from 500ms/2500ms to
    // cut battery/CPU churn from a tight polling loop.
    private static final long POLL_INTERVAL_MS = 1500;
    private static final long POLL_WINDOW_MS = 4000;

    private WindowManager wm;
    private View bubble;
    private View removeZone;
    private WindowManager.LayoutParams bubbleParams;
    private WindowManager.LayoutParams removeParams;
    private Handler handler;
    private boolean visible = false;
    private boolean removeVisible = false;
    private boolean userHidden = false;
    private boolean appInForeground = false;
    private boolean isDragging = false;
    private int screenHeight;
    private String mode = "always";
    private int missCount = 0;

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        startAsForeground();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        screenHeight = dm.heightPixels;
        makeBubble();
        makeRemoveZone();
        handler = new Handler(Looper.getMainLooper());
        mode = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE)
            .getString(MainActivity.KEY_MODE, "always");
        applyMode();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            if (ACTION_APPLY_MODE.equals(action)) {
                String m = intent.getStringExtra("mode");
                if (m != null && !m.isEmpty()) {
                    mode = m;
                    userHidden = false;
                    applyMode();
                }
            } else if (ACTION_SHOW.equals(action)) {
                if ("accessibility".equals(mode) && !userHidden) showBubble();
            } else if (ACTION_HIDE.equals(action)) {
                if ("accessibility".equals(mode)) {
                    userHidden = false;
                    hideBubble();
                }
            } else if (ACTION_APP_FOREGROUND.equals(action)) {
                // Anchor's own app just came to the front: never float the
                // bubble over our own screens, and treat this as a fresh
                // start — clear any earlier manual "drag to remove".
                appInForeground = true;
                userHidden = false;
                hideBubble();
                // TEMPORARY DEBUG — remove once the persistence bug is confirmed fixed.
                Toast.makeText(this, "DEBUG: FBS received APP_FOREGROUND, hid bubble", Toast.LENGTH_SHORT).show();
            } else if (ACTION_APP_BACKGROUND.equals(action)) {
                appInForeground = false;
                applyMode();
            }
        }
        return START_STICKY;
    }

    private void applyMode() {
        // TEMPORARY DEBUG — remove once the persistence bug is confirmed fixed.
        Toast.makeText(this, "DEBUG: applyMode mode=" + mode
            + " userHidden=" + userHidden + " appInForeground=" + appInForeground,
            Toast.LENGTH_LONG).show();
        if (handler != null) handler.removeCallbacksAndMessages(null);
        switch (mode) {
            case "off":
                hideBubble();
                break;
            case "always":
                if (!userHidden) showBubble();
                break;
            case "targets":
                handler.post(targetsRunnable);
                break;
            case "accessibility":
                // wait for Accessibility SHOW/HIDE
                break;
            default:
                if (!userHidden) showBubble();
        }
    }

    private final Runnable targetsRunnable = new Runnable() {
        @Override public void run() {
            if (!"targets".equals(mode)) return;
            if (isDragging) {
                handler.postDelayed(this, POLL_INTERVAL_MS);
                return;
            }
            String fg = getForegroundApp();
            boolean onTarget = fg != null && Constants.TARGETS.contains(fg);
            if (onTarget) {
                missCount = 0;
                if (!userHidden) showBubble();
            } else {
                missCount++;
                if (missCount >= 5) {
                    userHidden = false;
                    hideBubble();
                }
            }
            handler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };

    private String getForegroundApp() {
        try {
            UsageStatsManager usm = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
            long end = System.currentTimeMillis();
            List<android.app.usage.UsageStats> stats =
                usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, end - POLL_WINDOW_MS, end);
            String bestTarget = null; long bestTargetTime = 0;
            String bestAny = null; long bestAnyTime = 0;
            if (stats != null) {
                for (android.app.usage.UsageStats s : stats) {
                    String pkg = s.getPackageName();
                    long t = s.getLastTimeUsed();
                    if (t < end - POLL_WINDOW_MS) continue;
                    if (pkg.contains("systemui") || pkg.contains("inputmethod")
                        || pkg.contains("keyboard") || pkg.contains("launcher")) continue;
                    if (t > bestAnyTime) { bestAnyTime = t; bestAny = pkg; }
                    if (Constants.TARGETS.contains(pkg) && t > bestTargetTime) {
                        bestTargetTime = t; bestTarget = pkg;
                    }
                }
            }
            if (bestTarget != null) return bestTarget;
            UsageEvents events = usm.queryEvents(end - POLL_WINDOW_MS, end);
            UsageEvents.Event ev = new UsageEvents.Event();
            String last = null;
            while (events.hasNextEvent()) {
                events.getNextEvent(ev);
                if (ev.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    String pkg = ev.getPackageName();
                    if (pkg != null && !pkg.contains("systemui") && !pkg.contains("inputmethod"))
                        last = pkg;
                }
            }
            if (last != null && Constants.TARGETS.contains(last)) return last;
            return bestAny != null ? bestAny : last;
        } catch (Exception e) {
            Log.w(TAG, "getForegroundApp failed", e);
            return null;
        }
    }

    private void startAsForeground() {
        String id = "anchor_bubble";
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                id, "Anchor Bubble", NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ?
            new Notification.Builder(this, id) : new Notification.Builder(this);
        startForeground(1, b.setContentTitle("Anchor")
            .setContentText("Bubble is active")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true).build());
    }

    private int overlayType() {
        return Build.VERSION.SDK_INT >= 26 ?
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
            WindowManager.LayoutParams.TYPE_PHONE;
    }

    private void makeBubble() {
        TextView tv = new TextView(this);
        tv.setText("⚓");
        tv.setTextSize(22);
        tv.setPadding(28, 28, 28, 28);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#FF9F7A"));
        bg.setCornerRadius(80);
        tv.setBackground(bg);
        tv.setTextColor(Color.WHITE);
        bubble = tv;

        bubbleParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT);
        bubbleParams.gravity = Gravity.TOP | Gravity.START;
        bubbleParams.x = 50;
        bubbleParams.y = 350;

        bubble.setOnTouchListener(new View.OnTouchListener() {
            int ix, iy; float tx, ty; boolean moved;
            public boolean onTouch(View v, MotionEvent e) {
                int a = e.getActionMasked();
                if (a == MotionEvent.ACTION_DOWN) {
                    isDragging = true; moved = false;
                    ix = bubbleParams.x; iy = bubbleParams.y;
                    tx = e.getRawX(); ty = e.getRawY();
                    return true;
                }
                if (a == MotionEvent.ACTION_MOVE) {
                    int dx = (int)(e.getRawX() - tx);
                    int dy = (int)(e.getRawY() - ty);
                    if (Math.abs(dx) > 8 || Math.abs(dy) > 8) {
                        moved = true; showRemoveZone();
                    }
                    bubbleParams.x = ix + dx;
                    bubbleParams.y = iy + dy;
                    try { wm.updateViewLayout(bubble, bubbleParams); } catch (Exception ex) {
                        Log.w(TAG, "updateViewLayout during drag failed", ex);
                    }
                    highlightRemoveZone(e.getRawY());
                    return true;
                }
                if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
                    isDragging = false;
                    boolean overRemove = removeVisible && e.getRawY() > screenHeight * 0.78f;
                    hideRemoveZone();
                    if (overRemove) {
                        userHidden = true;
                        hideBubble();
                        return true;
                    }
                    if (!moved) {
                        Intent i = new Intent(FloatingBubbleService.this, MainActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(i);
                    }
                    return true;
                }
                return false;
            }
        });
    }

    private void makeRemoveZone() {
        TextView tv = new TextView(this);
        tv.setText("✕  Remove");
        tv.setTextSize(16);
        tv.setTextColor(Color.WHITE);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(40, 36, 40, 36);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#E53935"));
        bg.setCornerRadius(40);
        tv.setBackground(bg);
        FrameLayout wrap = new FrameLayout(this);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        lp.bottomMargin = 48;
        wrap.addView(tv, lp);
        removeZone = wrap;
        removeParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT);
        removeParams.gravity = Gravity.BOTTOM;
        removeParams.y = 24;
    }

    private void showRemoveZone() {
        if (removeVisible) return;
        try {
            wm.addView(removeZone, removeParams);
            removeVisible = true;
        } catch (Exception e) {
            Log.w(TAG, "Failed to show remove zone", e);
        }
    }

    private void hideRemoveZone() {
        if (!removeVisible) return;
        try {
            wm.removeView(removeZone);
        } catch (Exception e) {
            Log.w(TAG, "Failed to hide remove zone", e);
        }
        removeVisible = false;
    }

    private void highlightRemoveZone(float rawY) {
        if (!removeVisible || !(removeZone instanceof FrameLayout)) return;
        View inner = ((FrameLayout) removeZone).getChildAt(0);
        if (inner == null) return;
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(40);
        if (rawY > screenHeight * 0.78f) {
            bg.setColor(Color.parseColor("#B71C1C"));
            if (inner instanceof TextView) ((TextView) inner).setText("✕  Release to remove");
        } else {
            bg.setColor(Color.parseColor("#E53935"));
            if (inner instanceof TextView) ((TextView) inner).setText("✕  Remove");
        }
        inner.setBackground(bg);
    }

    private void showBubble() {
        if (bubble == null || bubbleParams == null) return;
        if ("off".equals(mode)) return;
        if (appInForeground) {
            // TEMPORARY DEBUG — remove once the persistence bug is confirmed fixed.
            Toast.makeText(this, "DEBUG: show blocked (appInForeground)", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            if (!bubble.isAttachedToWindow()) {
                wm.addView(bubble, bubbleParams);
            } else {
                bubble.setVisibility(View.VISIBLE);
                wm.updateViewLayout(bubble, bubbleParams);
            }
            visible = true;
        } catch (Exception e) {
            Log.w(TAG, "showBubble failed, retrying once", e);
            // The view may be in an inconsistent attached state — force a
            // clean detach before retrying, rather than trusting whatever
            // state we thought we were in.
            try {
                if (bubble.isAttachedToWindow()) wm.removeView(bubble);
            } catch (Exception ignored2) {}
            try {
                wm.addView(bubble, bubbleParams);
                visible = true;
            } catch (Exception e2) {
                Log.e(TAG, "showBubble retry also failed", e2);
                // TEMPORARY DEBUG — remove once the persistence bug is confirmed fixed.
                Toast.makeText(this, "DEBUG: showBubble FAILED: " + e2, Toast.LENGTH_LONG).show();
                visible = false;
            }
        }
    }

    private void hideBubble() {
        hideRemoveZone();
        if (bubble == null) return;
        try {
            bubble.setVisibility(View.GONE);
        } catch (Exception e) {
            Log.w(TAG, "setVisibility(GONE) on bubble failed", e);
        }
        try {
            if (bubble.isAttachedToWindow()) wm.removeView(bubble);
        } catch (Exception e) {
            Log.w(TAG, "removeView on bubble failed", e);
        }
        // Always resync regardless of whether removeView above succeeded —
        // this is what was getting stuck permanently true if removeView
        // ever threw, silently blocking every future showBubble() call.
        visible = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null) handler.removeCallbacksAndMessages(null);
        hideRemoveZone();
        if (bubble != null && bubble.isAttachedToWindow()) {
            try {
                wm.removeView(bubble);
            } catch (Exception e) {
                Log.w(TAG, "removeView in onDestroy failed", e);
            }
        }
    }
}

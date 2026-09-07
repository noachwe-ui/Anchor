package com.anchor.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class FloatingBubbleService extends Service {
    private WindowManager wm;
    private View bubble;
    private View removeZone;
    private WindowManager.LayoutParams bubbleParams;
    private WindowManager.LayoutParams removeParams;
    private Handler handler;
    private boolean visible = false;
    private boolean removeVisible = false;
    private boolean userHidden = false;
    private boolean isDragging = false;
    private int screenHeight;

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
        handler.post(checkRunnable);
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
                    isDragging = true;
                    moved = false;
                    ix = bubbleParams.x; iy = bubbleParams.y;
                    tx = e.getRawX(); ty = e.getRawY();
                    return true;
                }
                if (a == MotionEvent.ACTION_MOVE) {
                    int dx = (int)(e.getRawX() - tx);
                    int dy = (int)(e.getRawY() - ty);
                    if (Math.abs(dx) > 8 || Math.abs(dy) > 8) {
                        moved = true;
                        showRemoveZone();
                    }
                    bubbleParams.x = ix + dx;
                    bubbleParams.y = iy + dy;
                    try { wm.updateViewLayout(bubble, bubbleParams); } catch (Exception ignored) {}
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
        } catch (Exception ignored) {}
    }

    private void hideRemoveZone() {
        if (!removeVisible) return;
        try { wm.removeView(removeZone); } catch (Exception ignored) {}
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
        if (bubble == null) return;
        try {
            if (!visible) {
                wm.addView(bubble, bubbleParams);
                visible = true;
            } else {
                bubble.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            try { wm.addView(bubble, bubbleParams); visible = true; } catch (Exception ignored) {}
        }
    }

    private void hideBubble() {
        hideRemoveZone();
        if (bubble == null) return;
        try { bubble.setVisibility(View.GONE); } catch (Exception ignored) {}
        try {
            if (visible) {
                wm.removeView(bubble);
                visible = false;
            }
        } catch (Exception ignored) {}
    }

    private final Runnable checkRunnable = new Runnable() {
        @Override public void run() {
            if (isDragging) {
                handler.postDelayed(this, 500);
                return;
            }
            String fg = getForegroundApp();
            boolean onTarget = fg != null && TARGETS.contains(fg);

            if (onTarget) {
                if (!userHidden) showBubble();
            } else {
                // Left target app (or unknown/launcher). Hide right away.
                userHidden = false;
                hideBubble();
            }
            handler.postDelayed(this, 500);
        }
    };

    private String getForegroundApp() {
        try {
            UsageStatsManager usm = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
            long end = System.currentTimeMillis();

            // Prefer recent usage stats (more reliable on many phones)
            java.util.List<android.app.usage.UsageStats> stats =
                usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, end - 3000, end);
            String best = null;
            long bestTime = 0;
            if (stats != null) {
                for (android.app.usage.UsageStats s : stats) {
                    long t = s.getLastTimeUsed();
                    if (t > bestTime) {
                        bestTime = t;
                        best = s.getPackageName();
                    }
                }
            }
            if (best != null && bestTime >= end - 3000) {
                return best;
            }

            // Fallback to events
            UsageEvents events = usm.queryEvents(end - 3000, end);
            UsageEvents.Event ev = new UsageEvents.Event();
            String last = null;
            while (events.hasNextEvent()) {
                events.getNextEvent(ev);
                if (ev.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    last = ev.getPackageName();
                }
            }
            return last;
        } catch (Exception e) {
            return null;
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null) handler.removeCallbacksAndMessages(null);
        hideRemoveZone();
        if (visible && bubble != null) {
            try { wm.removeView(bubble); } catch (Exception ignored) {}
        }
    }
}

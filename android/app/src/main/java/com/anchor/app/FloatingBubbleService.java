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
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class FloatingBubbleService extends Service {
    private WindowManager wm;
    private View bubble;
    private WindowManager.LayoutParams params;
    private Handler handler;
    private boolean visible = false;
    private boolean isDragging = false;
    private int hideCountdown = 0;
    private static final int HIDE_DELAY_TICKS = 4; // keep visible \~3 seconds after brief glitches

    private static final Set<String> TARGETS = new HashSet<>(Arrays.asList(
        "com.android.chrome",
        "com.chrome.beta",
        "com.chrome.dev",
        "com.sec.android.app.sbrowser",
        "org.mozilla.firefox",
        "org.mozilla.firefox_beta",
        "com.opera.browser",
        "com.opera.mini.native",
        "com.brave.browser",
        "com.microsoft.emmx",
        "com.duckduckgo.mobile.android",
        "com.vivaldi.browser",
        "com.google.android.youtube",
        "com.whatsapp",
        "com.whatsapp.w4b",
        "com.instagram.android",
        "com.facebook.katana",
        "com.facebook.lite",
        "com.facebook.orca",
        "com.facebook.mlite",
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",
        "com.ss.android.ugc.aweme",
        "com.twitter.android",
        "com.snapchat.android",
        "com.reddit.frontpage",
        "org.telegram.messenger",
        "org.telegram.messenger.web",
        "com.discord",
        "com.pinterest",
        "com.linkedin.android"
    ));

    @Override
    public IBinder onBind(Intent i) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        startAsForeground();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        makeBubble();
        handler = new Handler(Looper.getMainLooper());
        handler.post(checkRunnable);
    }

    private void startAsForeground() {
        String id = "anchor_bubble";
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                id, "Anchor Bubble", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(ch);
        }
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ?
            new Notification.Builder(this, id) :
            new Notification.Builder(this);
        Notification n = b.setContentTitle("Anchor")
            .setContentText("Bubble is ready")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build();
        startForeground(1, n);
    }

    private void makeBubble() {
        TextView tv = new TextView(this);
        tv.setText("⚓");
        tv.setTextSize(22);
        tv.setPadding(28, 28, 28, 28);
        tv.setBackgroundColor(Color.parseColor("#FF9F7A"));
        tv.setTextColor(Color.WHITE);
        bubble = tv;

        int type = Build.VERSION.SDK_INT >= 26 ?
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
            WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 50;
        params.y = 350;

        bubble.setOnTouchListener(new View.OnTouchListener() {
            int ix, iy; float tx, ty;
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    isDragging = true;
                    ix = params.x; iy = params.y;
                    tx = e.getRawX(); ty = e.getRawY();
                    return true;
                }
                if (e.getAction() == MotionEvent.ACTION_MOVE) {
                    params.x = ix + (int)(e.getRawX() - tx);
                    params.y = iy + (int)(e.getRawY() - ty);
                    if (visible) wm.updateViewLayout(bubble, params);
                    return true;
                }
                if (e.getAction() == MotionEvent.ACTION_UP) {
                    if (Math.abs(e.getRawX() - tx) < 10 && Math.abs(e.getRawY() - ty) < 10) {
                        Intent i = new Intent(FloatingBubbleService.this, MainActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                    }
                    return true;
                }
                return false;
            }
        });
    }

    private final Runnable checkRunnable = new Runnable() {
        @Override
        public void run() {
            boolean shouldShow = TARGETS.contains(getForegroundApp());
            if (shouldShow && !visible) {
                try { wm.addView(bubble, params); visible = true; } catch (Exception ignored) {}
            } else if (!shouldShow && visible) {
                try { wm.removeView(bubble); visible = false; } catch (Exception ignored) {}
            }
            handler.postDelayed(this, 700);
        }
    };

    private String getForegroundApp() {
        try {
            UsageStatsManager usm = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
            long end = System.currentTimeMillis();
            UsageEvents events = usm.queryEvents(end - 5000, end);
            UsageEvents.Event ev = new UsageEvents.Event();
            String last = "";
            while (events.hasNextEvent()) {
                events.getNextEvent(ev);
                if (ev.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    last = ev.getPackageName();
                }
            }
            return last;
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null) handler.removeCallbacksAndMessages(null);
        if (visible && bubble != null) {
            try { wm.removeView(bubble); } catch (Exception ignored) {}
        }
    }
}

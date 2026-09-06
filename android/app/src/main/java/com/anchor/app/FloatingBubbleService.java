package com.anchor.app;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class FloatingBubbleService extends Service {
    private WindowManager wm;
    private View bubble;

    @Override
    public IBinder onBind(Intent i) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

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

        final WindowManager.LayoutParams p = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.START;
        p.x = 50;
        p.y = 350;

        bubble.setOnTouchListener(new View.OnTouchListener() {
            int ix, iy;
            float tx, ty;
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    ix = p.x; iy = p.y;
                    tx = e.getRawX(); ty = e.getRawY();
                    return true;
                }
                if (e.getAction() == MotionEvent.ACTION_MOVE) {
                    p.x = ix + (int)(e.getRawX() - tx);
                    p.y = iy + (int)(e.getRawY() - ty);
                    wm.updateViewLayout(bubble, p);
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
        wm.addView(bubble, p);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bubble != null) wm.removeView(bubble);
    }
}

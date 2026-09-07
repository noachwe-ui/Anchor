package com.anchor.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.widget.RemoteViews;
import java.util.Random;

public class AnchorWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_OPEN_APP = "com.anchor.app.WIDGET_OPEN_APP";
    public static final String ACTION_OPEN_CLIP = "com.anchor.app.WIDGET_OPEN_CLIP";
    public static final String PREFS = "anchor_widget_prefs";
    public static final String KEY_BG = "bg_color";

    private static final String[] MESSAGES = {
        "Take a breath. You're doing great.",
        "This moment will pass.",
        "One gentle step is still progress.",
        "You are allowed to pause.",
        "Return to the present."
    };

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) updateWidget(context, manager, id);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (ACTION_OPEN_APP.equals(action)) {
            Intent i = new Intent(context, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivity(i);
        } else if (ACTION_OPEN_CLIP.equals(action)) {
            Intent i = new Intent(context, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            i.putExtra("open_clip", true);
            context.startActivity(i);
        }
    }

    static void updateWidget(Context context, AppWidgetManager manager, int id) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_anchor);

        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String bg = prefs.getString(KEY_BG, "#FAF8F5");
        try {
            views.setInt(R.id.widget_root, "setBackgroundColor", Color.parseColor(bg));
        } catch (Exception ignored) {}

        views.setTextViewText(R.id.widget_message, MESSAGES[new Random().nextInt(MESSAGES.length)]);

        // Tap body -> open app
        Intent openApp = new Intent(context, AnchorWidgetProvider.class);
        openApp.setAction(ACTION_OPEN_APP);
        PendingIntent piApp = PendingIntent.getBroadcast(
            context, 1, openApp,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_title, piApp);
        views.setOnClickPendingIntent(R.id.widget_message, piApp);

        // Clip button
        Intent openClip = new Intent(context, AnchorWidgetProvider.class);
        openClip.setAction(ACTION_OPEN_CLIP);
        PendingIntent piClip = PendingIntent.getBroadcast(
            context, 2, openClip,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_clip_btn, piClip);

        manager.updateAppWidget(id, views);
    }

    // Call this from the app to change widget background, e.g. "#FFF3E8"
    public static void setBackgroundColor(Context context, String hexColor) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_BG, hexColor).apply();
        Intent i = new Intent(context, AnchorWidgetProvider.class);
        i.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        context.sendBroadcast(i);
    }
}

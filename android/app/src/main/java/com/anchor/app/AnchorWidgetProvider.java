package com.anchor.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import java.util.Random;

public class AnchorWidgetProvider extends AppWidgetProvider {

    private static final String[] MESSAGES = {
        "Take a breath. You're doing great.",
        "This moment will pass.",
        "One gentle step is still progress.",
        "You are allowed to pause.",
        "Return to the present."
    };

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) {
            updateWidget(context, manager, id);
        }
    }

    static void updateWidget(Context context, AppWidgetManager manager, int id) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_anchor);

        String msg = MESSAGES[new Random().nextInt(MESSAGES.length)];
        views.setTextViewText(R.id.widget_message, msg);

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_root, pi);

        manager.updateAppWidget(id, views);
    }
}

package com.anchor.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.widget.RemoteViews;
import android.widget.Toast;
import org.json.JSONArray;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AnchorWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_CLICK = "com.anchor.app.WIDGET_CLICK";
    public static final String PREFS = "anchor_widget_prefs";
    public static final String KEY_BG = "bg_color";
    public static final String KEY_URLS = "clip_urls";
    public static final String KEY_DAILY = "daily_urls";
    public static final String KEY_HILLEL = "hillel_urls";

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
        if (!ACTION_CLICK.equals(intent.getAction())) {
            if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) {
                AppWidgetManager manager = AppWidgetManager.getInstance(context);
                int[] ids = manager.getAppWidgetIds(new ComponentName(context, AnchorWidgetProvider.class));
                onUpdate(context, manager, ids);
            }
            return;
        }

        int id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID);
        String action = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("widget_action_" + id,
                "vayimaen");

        if ("open_app".equals(action)) {
            Intent i = new Intent(context, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivity(i);
            return;
        }

        List<String> urls;
        String emptyMsg;
        if ("daily".equals(action)) {
            urls = loadList(context, KEY_DAILY, "public/daily_dose.json");
            emptyMsg = "No Daily Dose links yet. Open Anchor online once.";
        } else if ("hillel".equals(action)) {
            urls = loadList(context, KEY_HILLEL, "public/hillel_eisenberg.json");
            emptyMsg = "No Hillel links yet. Open Anchor online once.";
        } else {
            urls = loadList(context, KEY_URLS, "public/urls.json");
            emptyMsg = "No Vayimaen links yet. Open Anchor online once.";
        }

        if (urls.isEmpty()) {
            Toast.makeText(context, emptyMsg, Toast.LENGTH_SHORT).show();
            return;
        }
        String link = urls.get(new Random().nextInt(urls.size()));
        try {
            Intent view = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
            view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(view);
        } catch (Exception e) {
            Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show();
        }
    }

    private List<String> loadList(Context context, String prefKey, String assetPath) {
        List<String> urls = new ArrayList<>();
        try {
            String json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(prefKey, "");
            if (json != null && !json.isEmpty()) {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    String u = arr.optString(i, "").trim();
                    if (u.startsWith("http")) urls.add(u);
                }
            }
        } catch (Exception ignored) {}
        if (!urls.isEmpty()) return urls;
        try {
            InputStream is = context.getAssets().open(assetPath);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                String u = arr.optString(i, "").trim();
                if (u.startsWith("http")) urls.add(u);
            }
        } catch (Exception ignored) {}
        return urls;
    }

    static void updateWidget(Context context, AppWidgetManager manager, int id) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_anchor);
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        try {
            views.setInt(R.id.widget_root, "setBackgroundColor",
                Color.parseColor(prefs.getString(KEY_BG, "#FAF8F5")));
        } catch (Exception ignored) {}

        views.setTextViewText(R.id.widget_message,
            MESSAGES[new Random().nextInt(MESSAGES.length)]);

        String action = prefs.getString(
            "widget_action_" + id,
            "vayimaen");
        String label =
            "daily".equals(action) ? "Daily Dose" :
            "hillel".equals(action) ? "Rabbi Hillel Eisenberg" :
            "open_app".equals(action) ? "Open Anchor" :
            "Vayimaen";
        views.setTextViewText(R.id.widget_clip_btn, label);

        Intent click = new Intent(context, AnchorWidgetProvider.class);
        click.setAction(ACTION_CLICK);
        click.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        PendingIntent pi = PendingIntent.getBroadcast(context, id, click,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_clip_btn, pi);

        // Title/message still open app
        Intent openApp = new Intent(context, MainActivity.class);
        openApp.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent piApp = PendingIntent.getActivity(context, id + 1000, openApp,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_title, piApp);
        views.setOnClickPendingIntent(R.id.widget_message, piApp);

        manager.updateAppWidget(id, views);
    }

    public static void saveClipUrls(Context context, String jsonArray) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_URLS, jsonArray).apply();
    }

    public static void saveDailyUrls(Context context, String jsonArray) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_DAILY, jsonArray).apply();
    }

    public static void saveHillelUrls(Context context, String jsonArray) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_HILLEL, jsonArray).apply();
    }

    public static void setBackgroundColor(Context context, String hexColor) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_BG, hexColor).apply();
        Intent i = new Intent(context, AnchorWidgetProvider.class);
        i.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        context.sendBroadcast(i);
    }
}

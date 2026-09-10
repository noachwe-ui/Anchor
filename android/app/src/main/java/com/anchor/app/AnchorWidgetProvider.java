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
    public static final String ACTION_CYCLE = "com.anchor.app.WIDGET_CYCLE";
    public static final String PREFS = "anchor_widget_prefs";
    public static final String KEY_BG = "bg_color";
    public static final String KEY_URLS = "clip_urls";
    public static final String KEY_DAILY = "daily_urls";
    public static final String KEY_HILLEL = "hillel_urls";

    // Same key prefix AnchorWidgetConfigureActivity already defines —
    // referencing it here instead of a second copy of the literal string.
    private static final String KEY_ACTION_PREFIX = AnchorWidgetConfigureActivity.KEY_ACTION_PREFIX;

    // Order the cycle button steps through. Same four actions the
    // configure screen already offered — cycling just means you no
    // longer have to delete and re-add the widget to change this.
    private static final String[] CYCLE_ORDER = { "vayimaen", "daily", "hillel", "open_app" };

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
        String act = intent.getAction();

        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(act)) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = manager.getAppWidgetIds(new ComponentName(context, AnchorWidgetProvider.class));
            onUpdate(context, manager, ids);
            return;
        }

        int id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID);
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return;

        if (ACTION_CYCLE.equals(act)) {
            cycleAction(context, id);
            return;
        }

        if (!ACTION_CLICK.equals(act)) return;

        String action = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ACTION_PREFIX + id, "vayimaen");

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

    private void cycleAction(Context context, int id) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String current = prefs.getString(KEY_ACTION_PREFIX + id, "vayimaen");
        int idx = 0;
        for (int i = 0; i < CYCLE_ORDER.length; i++) {
            if (CYCLE_ORDER[i].equals(current)) { idx = i; break; }
        }
        String next = CYCLE_ORDER[(idx + 1) % CYCLE_ORDER.length];
        prefs.edit().putString(KEY_ACTION_PREFIX + id, next).apply();
        updateWidget(context, AppWidgetManager.getInstance(context), id);
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

    private static String labelFor(String action) {
        switch (action) {
            case "daily": return "Daily Dose";
            case "hillel": return "Rabbi Hillel Eisenberg";
            case "open_app": return "Open Anchor";
            default: return "Vayimaen";
        }
    }

    static void updateWidget(Context context, AppWidgetManager manager, int id) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_anchor);
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        // Tint the pill background via colorFilter, not setBackgroundColor —
        // this preserves the rounded corners from widget_pill_shape.xml
        // instead of flattening them into a plain rectangle.
        try {
            views.setInt(R.id.widget_pill_bg, "setColorFilter",
                Color.parseColor(prefs.getString(KEY_BG, "#FFFFFF")));
        } catch (Exception ignored) {}

        String action = prefs.getString(KEY_ACTION_PREFIX + id, "vayimaen");
        views.setTextViewText(R.id.widget_label, labelFor(action));

        // Main row: performs the current action.
        Intent click = new Intent(context, AnchorWidgetProvider.class);
        click.setAction(ACTION_CLICK);
        click.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        PendingIntent piClick = PendingIntent.getBroadcast(context, id, click,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_row, piClick);

        // Cycle button: separate tap target, switches the action in place.
        Intent cycle = new Intent(context, AnchorWidgetProvider.class);
        cycle.setAction(ACTION_CYCLE);
        cycle.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        // Distinct requestCode (id + 2000) so this PendingIntent doesn't
        // collide with piClick's (id) or MainActivity's (id + 1000).
        PendingIntent piCycle = PendingIntent.getBroadcast(context, id + 2000, cycle,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_cycle_btn, piCycle);

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

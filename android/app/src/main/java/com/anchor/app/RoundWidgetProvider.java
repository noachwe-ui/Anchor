package com.anchor.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

public class RoundWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_CLICK = "com.anchor.app.ROUND_WIDGET_CLICK";

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) updateWidget(context, manager, id);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (!ACTION_CLICK.equals(intent.getAction())) return;
        int id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID);
        String action = context.getSharedPreferences(
            RoundWidgetConfigureActivity.PREFS, Context.MODE_PRIVATE)
            .getString(RoundWidgetConfigureActivity.KEY_ACTION_PREFIX + id,
                RoundWidgetConfigureActivity.ACTION_OPEN_APP);

        if (RoundWidgetConfigureActivity.ACTION_OPEN_CLIP.equals(action)) {
            openRandomClip(context);
        } else {
            Intent i = new Intent(context, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivity(i);
        }
    }

    private void openRandomClip(Context context) {
        List<String> urls = loadUrls(context);
        if (urls.isEmpty()) {
            Toast.makeText(context, "No clips available yet. Open Anchor once first.", Toast.LENGTH_SHORT).show();
            return;
        }
        String link = urls.get(new Random().nextInt(urls.size()));
        try {
            Intent view = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
            view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(view);
        } catch (Exception e) {
            Toast.makeText(context, "Could not open clip", Toast.LENGTH_SHORT).show();
        }
    }

    private List<String> loadUrls(Context context) {
        List<String> urls = new ArrayList<>();
        try {
            String json = context.getSharedPreferences(
                AnchorWidgetProvider.PREFS, Context.MODE_PRIVATE)
                .getString(AnchorWidgetProvider.KEY_URLS, "");
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
            InputStream is = context.getAssets().open("public/urls.json");
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
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_round);
        Intent click = new Intent(context, RoundWidgetProvider.class);
        click.setAction(ACTION_CLICK);
        click.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        PendingIntent pi = PendingIntent.getBroadcast(context, id, click,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.round_btn, pi);
        manager.updateAppWidget(id, views);
    }

    @Override
    public void onDeleted(Context context, int[] ids) {
        SharedPreferences.Editor ed = context.getSharedPreferences(
            RoundWidgetConfigureActivity.PREFS, Context.MODE_PRIVATE).edit();
        for (int id : ids) ed.remove(RoundWidgetConfigureActivity.KEY_ACTION_PREFIX + id);
        ed.apply();
    }
}

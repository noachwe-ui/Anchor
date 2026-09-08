package com.anchor.app;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

public class RoundWidgetConfigureActivity extends Activity {
    public static final String PREFS = "round_widget_prefs";
    public static final String KEY_ACTION_PREFIX = "action_"; // + appWidgetId

    public static final String ACTION_OPEN_APP = "open_app";
    public static final String ACTION_OPEN_CLIP = "open_clip";

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        setContentView(R.layout.activity_round_widget_configure);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            );
        }
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        Button openApp = findViewById(R.id.btn_open_app);
        Button openClip = findViewById(R.id.btn_open_clip);

        openApp.setOnClickListener(v -> finishWithAction(ACTION_OPEN_APP));
        openClip.setOnClickListener(v -> finishWithAction(ACTION_OPEN_CLIP));
    }

    private void finishWithAction(String action) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        prefs.edit().putString(KEY_ACTION_PREFIX + appWidgetId, action).apply();

        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        RoundWidgetProvider.updateWidget(this, manager, appWidgetId);

        Intent result = new Intent();
        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, result);
        finish();
    }
}

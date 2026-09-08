package com.anchor.app;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;

public class AnchorWidgetConfigureActivity extends Activity {
    public static final String PREFS = "anchor_widget_prefs";
    public static final String KEY_ACTION_PREFIX = "widget_action_";

    public static final String ACTION_VAYIMAEN = "vayimaen";
    public static final String ACTION_DAILY = "daily";
    public static final String ACTION_HILLEL = "hillel";
    public static final String ACTION_OPEN_APP = "open_app";

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        setContentView(R.layout.activity_anchor_widget_configure);

        Bundle extras = getIntent().getExtras();
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

        findViewById(R.id.btn_vayimaen).setOnClickListener(v -> finishWith(ACTION_VAYIMAEN));
        findViewById(R.id.btn_daily).setOnClickListener(v -> finishWith(ACTION_DAILY));
        findViewById(R.id.btn_hillel).setOnClickListener(v -> finishWith(ACTION_HILLEL));
        findViewById(R.id.btn_open_app).setOnClickListener(v -> finishWith(ACTION_OPEN_APP));
    }

    private void finishWith(String action) {
        getSharedPreferences(PREFS, MODE_PRIVATE)
            .edit()
            .putString(KEY_ACTION_PREFIX + appWidgetId, action)
            .apply();

        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        AnchorWidgetProvider.updateWidget(this, manager, appWidgetId);

        Intent result = new Intent();
        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, result);
        finish();
    }
}

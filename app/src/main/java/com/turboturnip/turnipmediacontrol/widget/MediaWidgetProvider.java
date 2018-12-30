package com.turboturnip.turnipmediacontrol.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

import com.turboturnip.turnipmediacontrol.LogHelper;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link MediaWidgetConfigureActivity MediaWidgetConfigureActivity}
 */
public class MediaWidgetProvider extends AppWidgetProvider {
    private static final String TAG = LogHelper.getTag(MediaWidgetProvider.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        if (MediaWidgetData.WIDGET_ACTION.equals(intent.getAction())){
            LogHelper.v(TAG, "Received intent " + intent.getAction() + " " + intent.getData());
            if (intent.getData() == null) {
                LogHelper.e(TAG, "Intent had null data, can't execute");
            } else {
                int appWidgetId = Integer.parseInt(intent.getData().getQueryParameter(MediaWidgetData.WIDGET_ID));
                MediaWidgetSet.instance.updateContext(context);
                MediaWidgetSet.instance.performAction(appWidgetId, intent.getData().getPath());
            }
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        MediaWidgetSet.instance.updateContext(context);
        MediaWidgetSet.instance.discoverNewWidgets(appWidgetIds);
        MediaWidgetSet.instance.updateKnownWidgets();
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        LogHelper.v(TAG, "onDeleted");
        // When the user deletes the widget, delete the preference associated with it.
        MediaWidgetSet.instance.updateContext(context);
        for (int appWidgetId : appWidgetIds) {
            //MediaWidgetConfigureActivity.deleteTitlePref(context, appWidgetId);
            MediaWidgetSet.instance.removeWidget(appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);

        // Enter relevant functionality for when the first widget is created
        LogHelper.v(TAG, "onEnabled");
        MediaWidgetSet.instance.updateContext(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        // Enter relevant functionality for when the last widget is disabled
        LogHelper.v(TAG, "onDisabled");
    }
}


package com.turboturnip.turnipmediacontrol.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.RemoteViews;

import com.turboturnip.turnipmediacontrol.MediaNotificationFinderService;
import com.turboturnip.turnipmediacontrol.R;

import java.lang.reflect.Array;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link MediaWidgetConfigureActivity MediaWidgetConfigureActivity}
 */
public class MediaWidgetProvider extends AppWidgetProvider {



    Handler updateHandler = new Handler();
    long updateDelay = 250; // ms

    private void Loge(Object msg) {
        Log.e("turnipmediawidget", "MediaWidgetProvider@" + System.identityHashCode(this) + " - " + msg);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        //
        Loge("Received intent " + intent.getAction() + " " + intent.getData());
        if (MediaWidgetData.WIDGET_ACTION.equals(intent.getAction())){
            Loge("Received intent " + intent.getAction() + " " + intent.getData());
            if (intent.getData() == null) {
                Loge("Intent had null data, can't execute");
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
        Loge("onDeleted");
        // When the user deletes the widget, delete the preference associated with it.
        MediaWidgetSet.instance.updateContext(context);
        for (int appWidgetId : appWidgetIds) {
            MediaWidgetConfigureActivity.deleteTitlePref(context, appWidgetId);
            MediaWidgetSet.instance.removeWidget(appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);

        // Enter relevant functionality for when the first widget is created
        Loge("onEnabled");
        MediaWidgetSet.instance.updateContext(context);

    }

    /*void attachToNotificationFinder(Context context){
        if (!attached) {
            MediaNotificationFinderService.attachInterface(notificationWatcher);
            AppWidgetManager appWidgetManager = context.getSystemService(AppWidgetManager.class);
            for (int appWidgetId : appWidgetManager.getAppWidgetIds(new ComponentName(context, getClass()))) {
                if (!widgetIdToData.containsKey(appWidgetId)) {
                    Loge("New Widget " + appWidgetId);
                    MediaWidgetData data = new MediaWidgetData(appWidgetId);
                    widgetIdToData.put(appWidgetId, data);
                    data.changeActiveNotification(context, appWidgetManager, orderedNotifications.size() > 0 ? orderedNotifications.get(0) : null);
                }
            }
            attached = true;
        }
    }*/

    /*private void queueUpdate(final Context context) {
        Loge("queueUpdate");

        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        updateHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (orderChangedSinceLastUpdate || stateChangedSinceLastUpdate) {
                    immediateUpdate(context, appWidgetManager);
                }
                if (shouldUpdate)
                    updateHandler.postDelayed(this, updateDelay);
            }
        }, updateDelay);
    }*/



    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        // Enter relevant functionality for when the last widget is disabled
        Loge("onDisabled");
        /*MediaNotificationFinderService.detachInterface(notificationWatcher);
        attached = false;
        shouldUpdate = false;*/
    }
}


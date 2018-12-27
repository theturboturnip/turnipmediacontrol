package com.turboturnip.turnipmediacontrol.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
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

    static final String WIDGET_NOTIFICATION_ACTION = "com.turboturnip.turnipmediacontrol.WIDGET_NOTIFICATION_ACTION";
    static final String WIDGET_ACTION = "com.turboturnip.turnipmediacontrol.WIDGET_ACTION";
    static final String TARGET_NOTIFICATION_ID = "com.turboturnip.turnipmediacontrol.TARGET_NOTIFICATION_ID";
    static final String WIDGET_ID = "com.turboturnip.turnipmediacontrol.WIDGET_ID";
    static final String ACTION_PLAY = "play";
    static final String ACTION_PAUSE = "pause";
    static final String ACTION_SKIP_NEXT = "skipnext";
    static final String ACTION_SKIP_PREVIOUS = "skipprev";
    static final String WIDGET_SELECT_LEFT = "selectleft";
    static final String WIDGET_SELECT_RIGHT = "selectright";

    static List<MediaNotificationFinderService.MediaNotification> orderedNotifications = new ArrayList<>();
    static List<MediaNotificationFinderService.MediaNotification> previousOrderedNotifications = new ArrayList<>();
    static Map<Integer, MediaWidgetData> widgetIdToData = new HashMap<>();
    static MediaNotificationFinderService.Interface notificationWatcher = new MediaNotificationFinderService.Interface() {
        @Override
        public void onUpdateOrder(MediaNotificationFinderService.MediaNotificationSet notificationSet) {
            Log.e("turnipmediawidget","Got " + notificationSet.orderedMediaNotifications.size() + " Notifications from onUpdateOrder");
            orderedNotifications = notificationSet.orderedMediaNotifications;
            orderChangedSinceLastUpdate = true;
        }

        // We don't care about state changes, those will be handled by MediaWidgetData
        @Override
        public void onUpdateState(MediaNotificationFinderService.MediaNotificationSet notificationSet) {
            orderedNotifications = notificationSet.orderedMediaNotifications;
            stateChangedSinceLastUpdate = true;
        }
    };

    static boolean attached = false;
    static boolean shouldUpdate = false;
    static boolean orderChangedSinceLastUpdate = true;
    static boolean stateChangedSinceLastUpdate = false;
    Handler updateHandler = new Handler();
    long updateDelay = 250; // ms

    private void Loge(Object msg) {
        Log.e("turnipmediawidget", "MediaWidgetProvider@" + System.identityHashCode(this) + " - " + msg);
    }

    void setAppWidgetPlayPending(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        if (appWidgetId == -1) return;
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.media_widget);
        views.setViewVisibility(R.id.play_pending, View.VISIBLE);
        views.setImageViewResource(R.id.play_button, android.R.id.empty);
        views.setOnClickPendingIntent(R.id.play_button, null);

        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        //
        Loge("Received intent " + intent.getAction() + " " + intent.getData());
        if (WIDGET_NOTIFICATION_ACTION.equals(intent.getAction())){
            Loge("Received intent " + intent.getAction() + " " + intent.getData());
            try {
                int notificationId = Integer.parseInt(intent.getData().getQueryParameter(TARGET_NOTIFICATION_ID));
                MediaController controller = null;
                for (MediaNotificationFinderService.MediaNotification notification : orderedNotifications) {
                    if (notification.notification.getId() != notificationId)
                        continue;
                    controller = notification.controller;
                    break;
                }
                if (controller != null) {
                    switch (intent.getData().getPath()) {
                        case ACTION_PLAY:
                            if (controller.getPlaybackState().getState() != PlaybackState.STATE_PLAYING) {
                                controller.getTransportControls().play();
                            }
                            break;
                        case ACTION_PAUSE:
                            if (controller.getPlaybackState().getState() != PlaybackState.STATE_PAUSED) {
                                controller.getTransportControls().pause();
                            }
                            break;
                        case ACTION_SKIP_NEXT:
                            controller.getTransportControls().skipToNext();
                            break;
                        case ACTION_SKIP_PREVIOUS:
                            controller.getTransportControls().skipToPrevious();
                            break;
                        default:
                            Loge("Incorrect WIDGET_NOTIFICATION_ACTION path " + intent.getData().getPath());
                    }
                } else {
                    Loge("Null Controller");
                }
            } catch (NullPointerException e) {
                Loge("Failed to process a WIDGET_NOTIFICATION_ACTION with URI " + intent.getData());
            }
        } else if (WIDGET_ACTION.equals(intent.getAction())) {
            int appWidgetId = -1;//intent.getIntExtra(WIDGET_ID, -1);
            Uri intentData = intent.getData();
            if (intentData != null)
                appWidgetId = Integer.parseInt(intentData.getQueryParameter(WIDGET_ID));
            MediaWidgetData data = widgetIdToData.get(appWidgetId);
            Loge("Widget action on " + appWidgetId + ", " + data);
            if (data != null && intentData != null && intentData.getPath() != null) {
                int widgetNotificationIndex = indexOfNotificationWithId(data.selectedNotification.notification.getId());
                Loge("Changing notification index, start: " + widgetNotificationIndex);
                switch (intentData.getPath()) {
                    case WIDGET_SELECT_LEFT:
                        if (widgetNotificationIndex > 0)
                            widgetNotificationIndex--;
                        break;
                    case WIDGET_SELECT_RIGHT:
                        if (widgetNotificationIndex < orderedNotifications.size() - 1)
                            widgetNotificationIndex++;
                        break;
                    default:
                        Loge("Incorrect WIDGET_ACTION path " + intent.getData());
                }
                Loge("Changing notification index, end: " + widgetNotificationIndex);
                AppWidgetManager appWidgetManager = context.getSystemService(AppWidgetManager.class);
                MediaNotificationFinderService.MediaNotification newNotification = orderedNotifications.get(widgetNotificationIndex);
                data.changeActiveNotification(context, appWidgetManager, newNotification);
            }
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        //Log.e("turnipmediawidget", "onUpdate");
        // There may be multiple widgets active, so update all of them
        ArrayList<Integer> appWidgetIdsList = new ArrayList<>(appWidgetIds.length);
        for (int i : appWidgetIds) appWidgetIdsList.add(i);
        immediateUpdate(context, appWidgetManager, appWidgetIdsList);
        /*for (int appWidgetId : appWidgetIds) {
            updateSingleWidget(context, appWidgetManager, appWidgetId);
        }*/

        attachToNotificationFinder();
        if (!shouldUpdate) {
            shouldUpdate = true;
            queueUpdate(context);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        Loge("onDeleted");
        // When the user deletes the widget, delete the preference associated with it.
        for (int appWidgetId : appWidgetIds) {
            MediaWidgetConfigureActivity.deleteTitlePref(context, appWidgetId);
            MediaWidgetData associatedData = widgetIdToData.get(appWidgetId);
            if (associatedData != null)
                associatedData.onDestroy();
            widgetIdToData.remove(appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);

        // Enter relevant functionality for when the first widget is created
        Loge("onEnabled");
        attachToNotificationFinder();
        if (!shouldUpdate) {
            shouldUpdate = true;
            queueUpdate(context);
        }
    }

    void attachToNotificationFinder(){
        if (!attached) {
            MediaNotificationFinderService.attachInterface(notificationWatcher);
            attached = true;
        }
    }

    private void queueUpdate(final Context context) {
        Loge("queueUpdate");

        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        updateHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //Loge("update changed:" + changedSinceLastUpdate);
                if (orderChangedSinceLastUpdate || stateChangedSinceLastUpdate) {
                    immediateUpdate(context, appWidgetManager);
                    //shouldUpdate = true;
                }
                if (shouldUpdate)
                    updateHandler.postDelayed(this, updateDelay);
            }
        }, updateDelay);
    }

    private int indexOfNotificationWithId(int id) {
        for (int i = 0; i < orderedNotifications.size(); i++) {
            if (orderedNotifications.get(i).notification.getId() == id)
                return i;
        }
        return -1;
    }
    private void updateSingleWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        MediaWidgetData data = widgetIdToData.get(appWidgetId);
        if (data == null) {
            Loge("New Widget " + appWidgetId);
            data = new MediaWidgetData(appWidgetId);
            widgetIdToData.put(appWidgetId, data);
            data.changeActiveNotification(context, appWidgetManager, orderedNotifications.size() > 0 ? orderedNotifications.get(0) : null);
        }

        if (orderChangedSinceLastUpdate) {
            // If this widget was on the previous highest priority notification
            if (previousOrderedNotifications.size() > 0 &&
                    MediaNotificationFinderService.MediaNotification.notificationsEqual(data.selectedNotification, previousOrderedNotifications.get(0))) {
                // Swap to the new one
                data.changeActiveNotification(context, appWidgetManager, orderedNotifications.size() > 0 ? orderedNotifications.get(0) : null);
            } else if (data.selectedNotification != null) {
                int indexInNew = indexOfNotificationWithId(data.selectedNotification.notification.getId());
                if (indexInNew >= 0)
                    data.updateActiveNotification(context, appWidgetManager, orderedNotifications.get(indexInNew), true);
                else
                    data.changeActiveNotification(context, appWidgetManager, orderedNotifications.size() > 0 ? orderedNotifications.get(0) : null);
            } else if (orderedNotifications.size() > 0) {
                data.changeActiveNotification(context, appWidgetManager, orderedNotifications.get(0));
            }
        }else if (stateChangedSinceLastUpdate) {
            data.updateActiveNotification(context, appWidgetManager, orderedNotifications.get(indexOfNotificationWithId(data.selectedNotification.notification.getId())), false);
        }
    }

    private void immediateUpdate(Context context, AppWidgetManager appWidgetManager, Iterable<Integer> widgetIds) {
        /*Intent updateWidget = new Intent(context, MediaWidgetProvider.class).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        updateWidget.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, MediaWidgetProvider.class)));
        context.sendBroadcast(updateWidget);*/
        //Loge("immediateUpdate");s

        for (Integer appWidgetId : widgetIds) {
            updateSingleWidget(context, appWidgetManager, appWidgetId);
        }
        previousOrderedNotifications = orderedNotifications;
        orderChangedSinceLastUpdate = false;
        stateChangedSinceLastUpdate = false;
    }
    private void immediateUpdate(Context context, AppWidgetManager appWidgetManager) {
        immediateUpdate(context, appWidgetManager, widgetIdToData.keySet());
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        // Enter relevant functionality for when the last widget is disabled
        Loge("onDisabled");
        MediaNotificationFinderService.detachInterface(notificationWatcher);
        attached = false;
        shouldUpdate = false;
    }
}


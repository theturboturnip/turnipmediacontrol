package com.turboturnip.turnipmediacontrol;

import android.app.AlarmManager;
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
import android.support.v4.app.AlarmManagerCompat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link MediaWidgetConfigureActivity MediaWidgetConfigureActivity}
 */
public class MediaWidget extends AppWidgetProvider {

    static final String WIDGET_ACTION = "com.turboturnip.turnipmediacontrol.WIDGET_ACTION";
    static final String TARGET_NOTIFICATION_ID = "com.turboturnip.turnipmediacontrol.TARGET_NOTIFICATION_ID";
    static final String WIDGET_ID = "com.turboturnip.turnipmediacontrol.WIDGET_ID";
    static final String ACTION_PLAY = "play";
    static final String ACTION_PAUSE = "pause";
    static final String ACTION_SKIP_NEXT = "skipnext";
    static final String ACTION_SKIP_PREVIOUS = "skipprev";

    private static class WidgetData {
        //RemoteViews views;
        int selectedNotificationIndex = 0;
    }

    List<MediaNotificationFinderService.MediaNotification> orderedNotifications = new ArrayList<>();
    Map<Integer, WidgetData> widgetIdToData = new HashMap<>();
    MediaNotificationFinderService.Interface notificationWatcher = new MediaNotificationFinderService.Interface() {
        @Override
        public void updateNotificationSet(MediaNotificationFinderService.MediaNotificationSet notificationSet) {
            Log.e("turnipmediawidget","Got " + notificationSet.orderedMediaNotifications.size() + " Notifications");
            orderedNotifications = notificationSet.orderedMediaNotifications;
            changedSinceLastUpdate = true;
        }
    };

    boolean attached = false;
    boolean shouldUpdate = false;
    boolean changedSinceLastUpdate = true;
    Handler updateHandler = new Handler();
    long updateDelay = 250; // ms

    void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        Log.e("turnipmedia", "Updating widget");

        WidgetData widgetData = widgetIdToData.get(appWidgetId);
        if (widgetData == null) {
            widgetData = new WidgetData();
            widgetData.selectedNotificationIndex = 0;
            widgetIdToData.put(appWidgetId, widgetData);
        }

        RemoteViews views = null;
        if (orderedNotifications.size() > 0) {
            Log.e("turnipmedia", "Updating widget as notificatoin");
            views = new RemoteViews(context.getPackageName(), R.layout.media_widget);

            MediaNotificationFinderService.MediaNotification selectedNotification = orderedNotifications.get(widgetData.selectedNotificationIndex);

            {
                MediaMetadata metadata = selectedNotification.controller.getMetadata();
                views.setImageViewBitmap(R.id.album_art, metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART));
                views.setTextViewText(R.id.title_text, metadata.getText(MediaMetadata.METADATA_KEY_TITLE));
                //views.setBoolean(R.id.title_text, "setSelected", true);
                views.setTextViewText(R.id.artist_text, metadata.getString(MediaMetadata.METADATA_KEY_ARTIST));
                views.setTextViewText(R.id.album_text, metadata.getString(MediaMetadata.METADATA_KEY_ALBUM));
            }

            {
                boolean playing = selectedNotification.controller.getPlaybackState().getState() == PlaybackState.STATE_PLAYING;
                views.setViewVisibility(R.id.play_button, View.VISIBLE);
                views.setImageViewResource(R.id.play_button, playing ? R.drawable.ic_pause_36dp : R.drawable.ic_play_arrow_36dp);
                views.setOnClickPendingIntent(R.id.play_button, generatePlayPausePendingIntent(context, playing, selectedNotification, appWidgetId));

                views.setImageViewResource(R.id.skip_next_button, R.drawable.ic_skip_next_36dp);
                views.setOnClickPendingIntent(R.id.skip_next_button, generateSkipNextPendingIntent(context, selectedNotification, appWidgetId));

                views.setImageViewResource(R.id.skip_previous_button, R.drawable.ic_skip_previous_36dp);
                views.setOnClickPendingIntent(R.id.skip_previous_button, generateSkipPreviousPendingIntent(context, selectedNotification, appWidgetId));

                views.setViewVisibility(R.id.play_pending, View.GONE);
            }

            {
                views.setViewVisibility(R.id.nav_left, (widgetData.selectedNotificationIndex > 0) ? View.VISIBLE : View.GONE);
                views.setImageViewResource(R.id.nav_left, R.drawable.ic_chevron_left_24dp);

                views.setViewVisibility(R.id.nav_right, (widgetData.selectedNotificationIndex < orderedNotifications.size() - 1) ? View.VISIBLE : View.GONE);
                views.setImageViewResource(R.id.nav_right, R.drawable.ic_chevron_right_24dp);
            }
        } else {
            // Handle switching from some notification to no notification
            views = new RemoteViews(context.getPackageName(), R.layout.speed_dial_widget);
            views.setOnClickPendingIntent(R.id.refresh_button,  generateUpdateWidgetPendingIntent(context, appWidgetId));
        }

        //Log.e("turnipmediawidget", "Found Album Art View " + views.)

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
    void setAppWidgetPlayPending(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        if (appWidgetId == -1) return;
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.media_widget);
        views.setViewVisibility(R.id.play_pending, View.VISIBLE);
        views.setImageViewResource(R.id.play_button, android.R.id.empty);
        views.setOnClickPendingIntent(R.id.play_button, null);

        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views);
    }
    PendingIntent generateUpdateWidgetPendingIntent(Context context, int appWidgetId) {
        Intent updateWidget = new Intent(context, MediaWidget.class).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        updateWidget.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{appWidgetId});
        return PendingIntent.getBroadcast(context, 0, updateWidget, 0);
    }
    private PendingIntent generatePlayPausePendingIntent(Context context, boolean playing, MediaNotificationFinderService.MediaNotification notification, int appWidgetId) {
        Intent intent = generateActionIntent(context, playing ? ACTION_PAUSE : ACTION_PLAY, notification.notification.getId(), appWidgetId);
        PendingIntent newPendingIntent =  PendingIntent.getBroadcast(context,
                0,
                intent,
                0);
        //Log.e("turnipmediawidget", "Generated pendingintent for notification: " + newPendingIntent + " data: " + intent.getData());
        return newPendingIntent;
    }
    private PendingIntent generateSkipNextPendingIntent(Context context, MediaNotificationFinderService.MediaNotification notification, int appWidgetId) {
        return PendingIntent.getBroadcast(context,
                0,
                generateActionIntent(context, ACTION_SKIP_NEXT, notification.notification.getId(), appWidgetId),
                0);
    }
    private PendingIntent generateSkipPreviousPendingIntent(Context context, MediaNotificationFinderService.MediaNotification notification, int appWidgetId) {
        return PendingIntent.getBroadcast(context,
                0,
                generateActionIntent(context, ACTION_SKIP_PREVIOUS, notification.notification.getId(), appWidgetId),
                0);
    }
    private static Intent generateActionIntent(Context context, String action, int notificationId, int appWidgetId) {
        // Generates an Intent that will perform the specified action for the
        // controller paired to a notification with the given id
        Intent controllerActionIntent = new Intent(context, MediaWidget.class).setAction(WIDGET_ACTION);
        controllerActionIntent.setData(new Uri.Builder().path(action).appendQueryParameter(TARGET_NOTIFICATION_ID, ""+notificationId).build());
        controllerActionIntent.putExtra(WIDGET_ID, appWidgetId);
        return controllerActionIntent;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        //
        Log.e("turnipmediawidget", "Received intent " + intent.getAction() + " " + intent.getData());
        if (WIDGET_ACTION.equals(intent.getAction())){
            Log.e("turnipmediawidget", "Received intent " + intent.getAction() + " " + intent.getData());
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
                    // TODO: Updating the widget shouldn't really be done here, it should be hooked into its controller directly and get updates asap?
                    AppWidgetManager appWidgetManager = context.getSystemService(AppWidgetManager.class);
                    switch (intent.getData().getPath()) {
                        case ACTION_PLAY:
                            if (controller.getPlaybackState().getState() != PlaybackState.STATE_PLAYING) {
                                controller.getTransportControls().play();
                                setAppWidgetPlayPending(context, appWidgetManager, intent.getIntExtra(WIDGET_ID, -1));
                            }
                            break;
                        case ACTION_PAUSE:
                            if (controller.getPlaybackState().getState() != PlaybackState.STATE_PAUSED) {
                                controller.getTransportControls().pause();
                                setAppWidgetPlayPending(context, appWidgetManager, intent.getIntExtra(WIDGET_ID, -1));
                            }
                            break;
                        case ACTION_SKIP_NEXT:
                            controller.getTransportControls().skipToNext();
                            break;
                        case ACTION_SKIP_PREVIOUS:
                            controller.getTransportControls().skipToPrevious();
                            break;
                        default:
                            Log.e("turnipmediawidget", "Incorrect WIDGET_ACTION path " + intent.getData().getPath());
                    }
                }
            } catch (NullPointerException e) {
                Log.e("turnipmediawidget", "Failed to process a WIDGET_ACTION with URI " + intent.getData());
            }
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        //Log.e("turnipmediawidget", "onUpdate");
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }

        if (!shouldUpdate) {
            shouldUpdate = true;
            queueUpdate(context);
        }
        changedSinceLastUpdate = false;
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        Log.e("turnipmediawidget", "onDeleted");
        // When the user deletes the widget, delete the preference associated with it.
        for (int appWidgetId : appWidgetIds) {
            MediaWidgetConfigureActivity.deleteTitlePref(context, appWidgetId);
            widgetIdToData.remove(appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);

        // Enter relevant functionality for when the first widget is created
        Log.e("turnipmediawidget", "onEnabled");

        if (!shouldUpdate) {
            shouldUpdate = true;
            queueUpdate(context);
        }
    }

    private void queueUpdate(final Context context) {
        Log.e("turnipmediawidget", "queueUpdate");

        if (!attached) {
            MediaNotificationFinderService.attachInterface(notificationWatcher);
            attached = true;
        }

        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        updateHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (changedSinceLastUpdate) {
                    /*Intent updateWidget = new Intent(context, MediaWidget.class).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                    updateWidget.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, MediaWidget.class)));
                    context.sendBroadcast(updateWidget);*/
                    immediateUpdate(context, appWidgetManager);
                    changedSinceLastUpdate = false;
                    //shouldUpdate = true;
                }
                if (shouldUpdate)
                    updateHandler.postDelayed(this, updateDelay);
            }
        }, updateDelay);
    }

    private void immediateUpdate(Context context, AppWidgetManager appWidgetManager) {
        /*Intent updateWidget = new Intent(context, MediaWidget.class).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        updateWidget.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, MediaWidget.class)));
        context.sendBroadcast(updateWidget);*/
        for (Integer appWidgetId : widgetIdToData.keySet()) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        // Enter relevant functionality for when the last widget is disabled
        Log.e("turnipmediawidget", "onDisabled");
        MediaNotificationFinderService.detachInterface(notificationWatcher);
        attached = false;
        shouldUpdate = false;
    }
}


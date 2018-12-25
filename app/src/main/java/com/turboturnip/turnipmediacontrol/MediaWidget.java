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
    static final String ACTION_PLAY = "play";
    static final String ACTION_PAUSE = "pause";
    static final String ACTION_SKIP_NEXT = "skipnext";
    static final String ACTION_SKIP_PREVIOUS = "skipprev";

    static List<MediaNotificationFinderService.MediaNotification> orderedNotifications = new ArrayList<>();
    static Map<Integer, Integer> widgetIdToSelectedNotificationIndex = new HashMap<>();
    static MediaNotificationFinderService.Interface notificationWatcher = new MediaNotificationFinderService.Interface() {
        @Override
        public void updateNotificationSet(MediaNotificationFinderService.MediaNotificationSet notificationSet) {
            Log.e("turnipmediawidget","Got " + notificationSet.orderedMediaNotifications.size() + " Notifications");
            orderedNotifications = notificationSet.orderedMediaNotifications;
            changedSinceLastUpdate = true;
        }
    };

    static boolean shouldUpdate = false;
    static boolean changedSinceLastUpdate = true;
    Handler updateHandler = new Handler();
    long updateDelay = 500; // ms

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        //Log.e("turnipmedia", "Updating widget");

        CharSequence widgetText = MediaWidgetConfigureActivity.loadTitlePref(context, appWidgetId);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.media_widget);
        //views.setTextViewText(R.id.appwidget_text, widgetText);

        if (orderedNotifications.size() > 0) {
            //Log.e("turnipmediawidget", "Adding src for notification");
            MediaNotificationFinderService.MediaNotification selectedNotification = orderedNotifications.get(0);
            MediaMetadata metadata = selectedNotification.controller.getMetadata();
            views.setImageViewBitmap(R.id.album_art, metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART));
            views.setTextViewText(R.id.title_text, metadata.getText(MediaMetadata.METADATA_KEY_TITLE));
            views.setTextViewText(R.id.subtitle_text, metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) + "\n" + metadata.getString(MediaMetadata.METADATA_KEY_ALBUM));

            boolean playing = selectedNotification.controller.getPlaybackState().getState() == PlaybackState.STATE_PLAYING;
            views.setImageViewResource(R.id.play_button, playing ? R.drawable.ic_pause_36dp : R.drawable.ic_play_arrow_36dp);
            views.setOnClickPendingIntent(R.id.play_button, generatePlayPausePendingIntent(context, playing, selectedNotification));

            views.setImageViewResource(R.id.skip_next_button, R.drawable.ic_skip_next_36dp);
            views.setOnClickPendingIntent(R.id.skip_next_button, generateSkipNextPendingIntent(context, selectedNotification));

            views.setImageViewResource(R.id.skip_previous_button, R.drawable.ic_skip_previous_36dp);
            views.setOnClickPendingIntent(R.id.skip_previous_button, generateSkipPreviousPendingIntent(context, selectedNotification));
        }

        //Log.e("turnipmediawidget", "Found Album Art View " + views.)

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
    private static PendingIntent generatePlayPausePendingIntent(Context context, boolean playing, MediaNotificationFinderService.MediaNotification notification) {
        Intent intent = generateActionIntent(context, playing ? ACTION_PAUSE : ACTION_PLAY, notification.notification.getId());
        PendingIntent newPendingIntent =  PendingIntent.getBroadcast(context,
                0,
                intent,
                0);
        Log.e("turnipmediawidget", "Generated pendingintent for notification: " + newPendingIntent + " data: " + intent.getData());
        return newPendingIntent;
    }
    private static PendingIntent generateSkipNextPendingIntent(Context context, MediaNotificationFinderService.MediaNotification notification) {
        return PendingIntent.getBroadcast(context,
                0,
                generateActionIntent(context, ACTION_SKIP_NEXT, notification.notification.getId()),
                0);
    }
    private static PendingIntent generateSkipPreviousPendingIntent(Context context, MediaNotificationFinderService.MediaNotification notification) {
        return PendingIntent.getBroadcast(context,
                0,
                generateActionIntent(context, ACTION_SKIP_PREVIOUS, notification.notification.getId()),
                0);
    }
    private static Intent generateActionIntent(Context context, String action, int notificationId) {
        // Generates an Intent that will perform the specified action for the
        // controller paired to a notification with the given id
        Intent controllerActionIntent = new Intent(context, MediaWidget.class).setAction(WIDGET_ACTION);
        controllerActionIntent.setData(new Uri.Builder().path(action).appendQueryParameter(TARGET_NOTIFICATION_ID, ""+notificationId).build());
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
                    switch (intent.getData().getPath()) {
                        case ACTION_PLAY:
                            controller.getTransportControls().play();
                            break;
                        case ACTION_PAUSE:
                            controller.getTransportControls().pause();
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
                    immediateUpdate(context);
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
        }
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);

        // Enter relevant functionality for when the first widget is created
        Log.e("turnipmediawidget", "onEnabled");
        MediaNotificationFinderService.attachInterface(notificationWatcher);

        if (!shouldUpdate) {
            shouldUpdate = true;
            queueUpdate(context);
        }
    }

    private void queueUpdate(final Context context) {
        Log.e("turnipmediawidget", "queueUpdate");
        updateHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (changedSinceLastUpdate) {
                    immediateUpdate(context);
                }
                if (shouldUpdate)
                    updateHandler.postDelayed(this, updateDelay);
            }
        }, updateDelay);
    }

    private void immediateUpdate(Context context) {
        Intent updateWidget = new Intent(context, MediaWidget.class).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        updateWidget.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, MediaWidget.class)));
        context.sendBroadcast(updateWidget);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        // Enter relevant functionality for when the last widget is disabled
        Log.e("turnipmediawidget", "onDisabled");
        MediaNotificationFinderService.detachInterface(notificationWatcher);

        shouldUpdate = false;
    }
}


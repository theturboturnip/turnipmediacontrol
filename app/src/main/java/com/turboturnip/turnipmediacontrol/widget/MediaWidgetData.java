package com.turboturnip.turnipmediacontrol.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.turboturnip.turnipmediacontrol.MediaNotificationFinderService;
import com.turboturnip.turnipmediacontrol.R;

import static com.turboturnip.turnipmediacontrol.widget.MediaWidgetProvider.*;

public class MediaWidgetData {
    final int appWidgetId;
    boolean hasSetNotification = false;
    MediaNotificationFinderService.MediaNotification selectedNotification;

    private Context context;
    private AppWidgetManager appWidgetManager;
    private MediaController.Callback controllerCallback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(@Nullable PlaybackState state) {
            super.onPlaybackStateChanged(state);
            if (appWidgetManager != null)
                appWidgetManager.partiallyUpdateAppWidget(appWidgetId, generateViews(true, false));
        }

        @Override
        public void onMetadataChanged(@Nullable MediaMetadata metadata) {
            super.onMetadataChanged(metadata);
            if (appWidgetManager != null)
                appWidgetManager.partiallyUpdateAppWidget(appWidgetId, generateViews(false, true));
        }
    };
    //private MediaNotificationFinderService.MediaNotification oldPrimaryNotification = null;
    /*private MediaNotificationFinderService.Interface notificationFinderInterface  = new MediaNotificationFinderService.Interface() {
        @Override
        public void updateNotificationSet(MediaNotificationFinderService.MediaNotificationSet notificationSet) {
            if (notificationSet.orderedMediaNotifications.size() > 0) {
                if (MediaNotificationFinderService.MediaNotification.notificationsEqual(selectedNotification, oldPrimaryNotification)
                        && context != null && appWidgetManager != null) {
                    changeActiveNotification(context, appWidgetManager, notificationSet.orderedMediaNotifications.get(0));
                }
                oldPrimaryNotification = notificationSet.orderedMediaNotifications.get(0);
            }else{
                oldPrimaryNotification = null;
            }
        }
    };*/

    public MediaWidgetData(int appWidgetId) {
        this.appWidgetId = appWidgetId;
        //MediaNotificationFinderService.attachInterface(notificationFinderInterface);
    }
    public void onDestroy() {
        //MediaNotificationFinderService.detachInterface(notificationFinderInterface);
    }

    public void changeActiveNotification(Context context, AppWidgetManager appWidgetManager, MediaNotificationFinderService.MediaNotification newSelectedNotification) {
        if (hasSetNotification && MediaNotificationFinderService.MediaNotification.notificationsEqual(selectedNotification, newSelectedNotification))
            return;

        Log.e("turnipmediawidget", "Change Active Notification From " + selectedNotification + " To " + newSelectedNotification);
        if (selectedNotification != null)
            selectedNotification.controller.unregisterCallback(controllerCallback);
        selectedNotification = newSelectedNotification;
        if (newSelectedNotification != null)
            newSelectedNotification.controller.registerCallback(controllerCallback);

        manualUpdate(context, appWidgetManager);
        hasSetNotification = true;
    }

    public void manualUpdate(Context context, AppWidgetManager appWidgetManager) {
        this.context = context;
        this.appWidgetManager = appWidgetManager;

        Log.e("turnipmediawidget", "Full Update");

        appWidgetManager.updateAppWidget(appWidgetId, generateViews(true, true));
    }

    private RemoteViews generateViews(boolean updatePlayback, boolean updateMetadata) {
        RemoteViews views;
        if (selectedNotification != null) {
            //MediaWidgetProvider.Loge("Updating widget as notificatoin");
            views = new RemoteViews(context.getPackageName(), R.layout.media_widget);

            int selectedNotificationIndex;
            for (selectedNotificationIndex = 0; selectedNotificationIndex < orderedNotifications.size(); selectedNotificationIndex++) {
                if (orderedNotifications.get(selectedNotificationIndex).notification.getId() == selectedNotification.notification.getId()){
                    break;
                }
            }

            if (updateMetadata){
                Log.e("turnipmediawidget", "updating metadataa");
                MediaMetadata metadata = selectedNotification.controller.getMetadata();
                views.setImageViewBitmap(R.id.album_art, metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART));
                views.setTextViewText(R.id.title_text, metadata.getText(MediaMetadata.METADATA_KEY_TITLE));
                //views.setBoolean(R.id.title_text, "setSelected", true);
                views.setTextViewText(R.id.artist_text, metadata.getString(MediaMetadata.METADATA_KEY_ARTIST));
                views.setTextViewText(R.id.album_text, metadata.getString(MediaMetadata.METADATA_KEY_ALBUM));
            }

            if (updatePlayback){
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

            if (selectedNotificationIndex != orderedNotifications.size()){
                if (selectedNotificationIndex > 0) {
                    views.setViewVisibility(R.id.nav_left, View.VISIBLE);
                    views.setOnClickPendingIntent(R.id.nav_left, generateWidgetActionIntent(context, WIDGET_SELECT_LEFT, appWidgetId));
                    views.setImageViewResource(R.id.nav_left, R.drawable.ic_chevron_left_24dp);
                } else {
                    views.setViewVisibility(R.id.nav_left, View.GONE);
                }

                if (selectedNotificationIndex < orderedNotifications.size() - 1) {
                    views.setViewVisibility(R.id.nav_right, View.VISIBLE);
                    views.setOnClickPendingIntent(R.id.nav_right, generateWidgetActionIntent(context, WIDGET_SELECT_RIGHT, appWidgetId));
                    views.setImageViewResource(R.id.nav_right, R.drawable.ic_chevron_right_24dp);
                } else {
                    views.setViewVisibility(R.id.nav_right, View.GONE);
                }
            }
        } else {
            // Handle switching from some notification to no notification
            views = new RemoteViews(context.getPackageName(), R.layout.speed_dial_widget);
            views.setOnClickPendingIntent(R.id.refresh_button, generateUpdateWidgetPendingIntent(context, appWidgetId));
        }
        return views;
    }
    PendingIntent generateUpdateWidgetPendingIntent(Context context, int appWidgetId) {
        Intent updateWidget = new Intent(context, MediaWidgetProvider.class).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        updateWidget.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{appWidgetId});
        return PendingIntent.getBroadcast(context, 1, updateWidget, 0);
    }
    private PendingIntent generatePlayPausePendingIntent(Context context, boolean playing, MediaNotificationFinderService.MediaNotification notification, int appWidgetId) {
        Intent intent = generateNotificationActionIntent(context, playing ? ACTION_PAUSE : ACTION_PLAY, notification.notification.getId(), appWidgetId);
        PendingIntent newPendingIntent =  PendingIntent.getBroadcast(context,
                0,
                intent,
                0);
        //Loge("turnipmediawidget", "Generated pendingintent for notification: " + newPendingIntent + " data: " + intent.getData());
        return newPendingIntent;
    }
    private PendingIntent generateSkipNextPendingIntent(Context context, MediaNotificationFinderService.MediaNotification notification, int appWidgetId) {
        return PendingIntent.getBroadcast(context,
                0,
                generateNotificationActionIntent(context, ACTION_SKIP_NEXT, notification.notification.getId(), appWidgetId),
                0);
    }
    private PendingIntent generateSkipPreviousPendingIntent(Context context, MediaNotificationFinderService.MediaNotification notification, int appWidgetId) {
        return PendingIntent.getBroadcast(context,
                0,
                generateNotificationActionIntent(context, ACTION_SKIP_PREVIOUS, notification.notification.getId(), appWidgetId),
                0);
    }
    private static Intent generateNotificationActionIntent(Context context, String action, int notificationId, int appWidgetId) {
        // Generates an Intent that will perform the specified action for the
        // controller paired to a notification with the given id
        Intent controllerActionIntent = new Intent(context, MediaWidgetProvider.class).setAction(WIDGET_NOTIFICATION_ACTION);
        controllerActionIntent.setData(new Uri.Builder().path(action).appendQueryParameter(TARGET_NOTIFICATION_ID, ""+notificationId).build());
        controllerActionIntent.putExtra(WIDGET_ID, appWidgetId);
        return controllerActionIntent;
    }
    private static PendingIntent generateWidgetActionIntent(Context context, String action, int appWidgetId) {
        // Generates an Intent that will perform the specified action for the
        // widget with the given id
        Intent controllerActionIntent = new Intent(context, MediaWidgetProvider.class).setAction(WIDGET_ACTION);
        controllerActionIntent.setData(new Uri.Builder().path(action).build());
        controllerActionIntent.putExtra(WIDGET_ID, appWidgetId);
        return PendingIntent.getBroadcast(context, 0, controllerActionIntent, 0);
    }
}

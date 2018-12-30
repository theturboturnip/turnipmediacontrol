package com.turboturnip.turnipmediacontrol.widget;

import android.app.Notification;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.RemoteViews;

import com.turboturnip.turnipmediacontrol.LogHelper;
import com.turboturnip.turnipmediacontrol.MediaNotificationFinderService;
import com.turboturnip.turnipmediacontrol.R;
import com.turboturnip.turnipmediacontrol.SettingsActivity;
import com.turboturnip.turnipmediacontrol.Util;

import java.util.Set;

public class MediaWidgetData {
    private static final String TAG = LogHelper.getTag(MediaWidgetData.class);

    private final int appWidgetId;
    private boolean hasSetNotification = false;
    MediaNotificationFinderService.MediaNotification selectedNotification;

    private static final String[] notificationTextPriority = new String[] {
            Notification.EXTRA_TITLE_BIG,
            Notification.EXTRA_TITLE,
            Notification.EXTRA_BIG_TEXT,
            Notification.EXTRA_TEXT,
            Notification.EXTRA_SUB_TEXT,
            Notification.EXTRA_INFO_TEXT
    };

    private MediaController.Callback controllerCallback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(@Nullable PlaybackState state) {
            super.onPlaybackStateChanged(state);
            if (MediaWidgetSet.instance.context.get() != null)
                generateViews(MediaWidgetSet.instance.context.get(), MediaWidgetSet.instance.appWidgetManager);
        }

        @Override
        public void onMetadataChanged(@Nullable MediaMetadata metadata) {
            super.onMetadataChanged(metadata);
            if (MediaWidgetSet.instance.context.get() != null)
                generateViews(MediaWidgetSet.instance.context.get(), MediaWidgetSet.instance.appWidgetManager, true);
        }
    };

    private static class ViewState {
        static class Metadata {
            Bitmap albumArtBitmap = null;
            Icon albumArtIcon = null;
            CharSequence title = null, artist = null, album = null;

            @Override
            public boolean equals(@Nullable Object obj) {
                if (!(obj instanceof Metadata))
                    return false;

                Metadata other = (Metadata)obj;
                //if (!Util.objectsEqual(albumArtBitmap, other.albumArtBitmap))
                //    return false;
                //if (!Util.objectsEqual(albumArtIcon, other.albumArtIcon))
                //    return false;
                if (!Util.objectsEqual(title, other.title))
                    return false;
                if (!Util.objectsEqual(artist, other.artist))
                    return false;
                if (!Util.objectsEqual(album, other.album))
                    return false;
                return true;
            }
        }
        boolean hasSong = true;

        final Metadata metadata = new Metadata();

        boolean playing = false;
        boolean navLeft = false;
        boolean navRight = false;

        MediaWidgetTheme theme;

        @Override
        public boolean equals(@Nullable Object obj) {
            if (!(obj instanceof ViewState))
                return false;

            ViewState other = (ViewState)obj;
            if (!Util.objectsEqual(metadata, other.metadata))
                return false;
            if (playing != other.playing)
                return false;
            if (navLeft != other.navLeft)
                return false;
            if (navRight != other.navRight)
                return false;
            if (!Util.objectsEqual(theme, other.theme))
                return false;
            return true;
        }

        @NonNull
        @Override
        public String toString() {
            return "[ViewState "+ metadata.title + " " + metadata.artist + " " + metadata.album + "]";
        }
    }
    private ViewState currentViewState = null;

    //private MediaWidgetTheme theme =
    //        new MediaWidgetTheme(Color.argb(128, 0,0,0));
            //new MediaWidgetTheme(Color.rgb(255,255,255));
            //new MediaWidgetTheme(Color.rgb(255,0,0));

    static final String WIDGET_ACTION = "com.turboturnip.turnipmediacontrol.WIDGET_ACTION";
    static final String WIDGET_ID = "com.turboturnip.turnipmediacontrol.WIDGET_ID";
    static final String ACTION_PLAY = "play";
    static final String ACTION_PAUSE = "pause";
    static final String ACTION_SKIP_NEXT = "skipnext";
    static final String ACTION_SKIP_PREVIOUS = "skipprev";
    static final String ACTION_SELECT_LEFT = "selectleft";
    static final String ACTION_SELECT_RIGHT = "selectright";


    MediaWidgetData(int appWidgetId) {
        this.appWidgetId = appWidgetId;
    }

    void changeActiveNotification(Context context, AppWidgetManager appWidgetManager, MediaNotificationFinderService.MediaNotification newSelectedNotification) {
        if (hasSetNotification && MediaNotificationFinderService.MediaNotification.notificationsEqual(selectedNotification, newSelectedNotification))
            return;

        LogHelper.d("turnipmediawidget", "Change Active Notification From " + selectedNotification + " To " + newSelectedNotification);
        if (selectedNotification != null)
            selectedNotification.controller.unregisterCallback(controllerCallback);
        selectedNotification = newSelectedNotification;
        if (newSelectedNotification != null)
            newSelectedNotification.controller.registerCallback(controllerCallback);

        manualUpdate(context, appWidgetManager);
        hasSetNotification = true;
    }

    void updateActiveNotification(Context context, AppWidgetManager appWidgetManager, MediaNotificationFinderService.MediaNotification newSelectedNotification) {
        if (selectedNotification == null) return;
        if (!hasSetNotification) return;
        if (!MediaNotificationFinderService.MediaNotification.notificationsEqual(selectedNotification, newSelectedNotification)) {
            throw new RuntimeException("Tried to updateActiveNotification with nonmatching notification");
        }

        selectedNotification = new MediaNotificationFinderService.MediaNotification(newSelectedNotification.notification, selectedNotification.controller);

        manualUpdate(context, appWidgetManager);
    }

    private void manualUpdate(Context context, AppWidgetManager appWidgetManager) {
        LogHelper.d("turnipmediawidget", "Full Update");

        generateViews(context, appWidgetManager);
    }

    private ViewState generateViewState(Context context) {
        ViewState resultViewState = new ViewState();

        SettingsActivity.MediaWidgetThemeType themeType = SettingsActivity.getSelectedTheme(context);
        resultViewState.theme = SettingsActivity.decodeExistingThemeType(context, themeType);

        if (selectedNotification == null) {
            resultViewState.hasSong = false;
            return resultViewState;
        }
        resultViewState.hasSong = true;

        MediaMetadata metadata = selectedNotification.controller.getMetadata();
        if (metadata != null) {
            resultViewState.metadata.albumArtBitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
            resultViewState.metadata.title = metadata.getText(MediaMetadata.METADATA_KEY_TITLE);
            resultViewState.metadata.artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
            resultViewState.metadata.album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM);
        }
        if (resultViewState.metadata.title == null || resultViewState.metadata.artist == null || resultViewState.metadata.album == null) {
            Bundle extras = selectedNotification.notification.getNotification().extras;
            CharSequence[] info = new CharSequence[]{ "NO METADATA", "NO METADATA", "NO METADATA" };
            int currentInfoIndex = 0;
            for(String potentialExtra : notificationTextPriority) {
                CharSequence extraValue = extras.getCharSequence(potentialExtra);
                if (extraValue != null && extraValue.length() > 0) {
                    info[currentInfoIndex++] = extras.getCharSequence(potentialExtra);
                }
            }
            resultViewState.metadata.title = (resultViewState.metadata.title == null) ? info[0] : resultViewState.metadata.title;
            resultViewState.metadata.artist = (resultViewState.metadata.artist == null) ? info[1] : resultViewState.metadata.artist;
            resultViewState.metadata.album = (resultViewState.metadata.album == null) ? info[2] : resultViewState.metadata.album;
        }

        if (resultViewState.metadata.albumArtBitmap == null) {
            resultViewState.metadata.albumArtIcon = selectedNotification.notification.getNotification().getLargeIcon();
            if (resultViewState.metadata.albumArtIcon == null)
                resultViewState.metadata.albumArtIcon = selectedNotification.notification.getNotification().getSmallIcon();
        }

        resultViewState.playing = selectedNotification.controller.getPlaybackState().getState() == PlaybackState.STATE_PLAYING;

        int selectedNotificationIndex = MediaWidgetSet.instance.indexOfMatchingNotification(selectedNotification);
        resultViewState.navLeft = selectedNotificationIndex > 0;
        resultViewState.navRight = selectedNotificationIndex < MediaWidgetSet.instance.orderedNotifications.size() - 1;

        return resultViewState;
    }

    public void generateViews(Context context, AppWidgetManager appWidgetManager){
        generateViews(context, appWidgetManager, false);
    }
    private void generateViews(Context context, AppWidgetManager appWidgetManager, boolean forceUpdateMetadata) {
        ViewState newViewState = generateViewState(context);

        boolean shouldPushFullUpdate = currentViewState == null || currentViewState.hasSong != newViewState.hasSong || !Util.objectsEqual(currentViewState.theme, newViewState.theme);
        boolean shouldPushPartialUpdate = false;

        RemoteViews views;
        if (selectedNotification != null) {
            views = new RemoteViews(context.getPackageName(), R.layout.media_widget);

            boolean updateMetadata = shouldPushFullUpdate || forceUpdateMetadata || !Util.objectsEqual(currentViewState.metadata, newViewState.metadata);
            boolean updatePlayback = shouldPushFullUpdate || currentViewState.playing != newViewState.playing;
            boolean updateNav = shouldPushFullUpdate || currentViewState.navLeft != newViewState.navLeft || currentViewState.navRight != newViewState.navRight;

            shouldPushPartialUpdate = updateMetadata || updatePlayback || updateNav;

            views.setInt(R.id.content, "setBackgroundColor", newViewState.theme.backgroundColor);

            if (updateMetadata){
                if (newViewState.metadata.albumArtBitmap != null)
                    views.setImageViewBitmap(R.id.album_art, newViewState.metadata.albumArtBitmap);
                else
                    views.setImageViewIcon(R.id.album_art, newViewState.metadata.albumArtIcon);

                views.setTextViewText(R.id.title_text, newViewState.metadata.title);
                views.setTextColor(R.id.title_text, newViewState.theme.standoutColor);
                views.setTextViewText(R.id.artist_text, newViewState.metadata.artist);
                views.setTextColor(R.id.artist_text, newViewState.theme.standoutColor);
                views.setTextViewText(R.id.album_text, newViewState.metadata.album);
                views.setTextColor(R.id.album_text, newViewState.theme.standoutColor);
            }

            if (updatePlayback){
                views.setViewVisibility(R.id.play_button, View.VISIBLE);
                views.setImageViewResource(R.id.play_button, newViewState.playing ? R.drawable.ic_pause_36dp : R.drawable.ic_play_arrow_36dp);
                views.setOnClickPendingIntent(R.id.play_button, generatePlayPausePendingIntent(context, newViewState.playing));
                views.setInt(R.id.play_button, "setColorFilter", newViewState.theme.standoutColor);

                views.setImageViewResource(R.id.skip_next_button, R.drawable.ic_skip_next_36dp);
                views.setOnClickPendingIntent(R.id.skip_next_button, generateSkipNextPendingIntent(context));
                views.setInt(R.id.skip_next_button, "setColorFilter", newViewState.theme.standoutColor);

                views.setImageViewResource(R.id.skip_previous_button, R.drawable.ic_skip_previous_36dp);
                views.setOnClickPendingIntent(R.id.skip_previous_button, generateSkipPreviousPendingIntent(context));
                views.setInt(R.id.skip_previous_button, "setColorFilter", newViewState.theme.standoutColor);
            }

            if (updateNav){
                if (newViewState.navLeft) {
                    views.setViewVisibility(R.id.nav_left, View.VISIBLE);
                    views.setOnClickPendingIntent(R.id.nav_left, generateActionIntent(context, ACTION_SELECT_LEFT));
                    views.setImageViewResource(R.id.nav_left, R.drawable.ic_chevron_left_24dp);
                    views.setInt(R.id.nav_left, "setColorFilter", newViewState.theme.standoutColor);
                    views.setInt(R.id.nav_left, "setBackgroundColor", newViewState.theme.mutedBackgroundColor);
                } else {
                    views.setViewVisibility(R.id.nav_left, View.GONE);
                }

                if (newViewState.navRight) {
                    views.setViewVisibility(R.id.nav_right, View.VISIBLE);
                    views.setOnClickPendingIntent(R.id.nav_right, generateActionIntent(context, ACTION_SELECT_RIGHT));
                    views.setImageViewResource(R.id.nav_right, R.drawable.ic_chevron_right_24dp);
                    views.setInt(R.id.nav_right, "setColorFilter", newViewState.theme.standoutColor);
                    views.setInt(R.id.nav_right, "setBackgroundColor", newViewState.theme.mutedBackgroundColor);
                } else {
                    views.setViewVisibility(R.id.nav_right, View.GONE);
                }
            }
        } else {
            // Handle switching from some notification to no notification
            views = new RemoteViews(context.getPackageName(), R.layout.speed_dial_widget);
            if (shouldPushFullUpdate) {
                views.setInt(R.id.content, "setBackgroundColor", newViewState.theme.backgroundColor);

                views.setOnClickPendingIntent(R.id.settings_button, generateOpenSettingsIntent(context));
                views.setImageViewResource(R.id.settings_button, R.drawable.ic_settings_24dp);
                views.setInt(R.id.settings_button, "setColorFilter", newViewState.theme.standoutColor);
                views.setInt(R.id.settings_button, "setBackgroundColor", newViewState.theme.mutedBackgroundColor);
            }
        }

        if (shouldPushFullUpdate) {
            appWidgetManager.updateAppWidget(appWidgetId, views);
        } else if (shouldPushPartialUpdate) {
            appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views);
        }
        currentViewState = newViewState;
    }

    private PendingIntent generatePlayPausePendingIntent(Context context, boolean playing) {
        return generateActionIntent(context, playing ? ACTION_PAUSE : ACTION_PLAY);
    }
    private PendingIntent generateSkipNextPendingIntent(Context context) {
        return generateActionIntent(context, ACTION_SKIP_NEXT);
    }
    private PendingIntent generateSkipPreviousPendingIntent(Context context) {
        return generateActionIntent(context, ACTION_SKIP_PREVIOUS);
    }
    private PendingIntent generateActionIntent(Context context, String action) {
        // Generates an Intent that will perform the specified action for the
        // controller paired to a notification with the given id
        Intent actionIntent = new Intent(context, MediaWidgetProvider.class);
        actionIntent.setAction(WIDGET_ACTION);
        actionIntent.setData(new Uri.Builder().path(action).appendQueryParameter(WIDGET_ID, ""+appWidgetId).build());
        return PendingIntent.getBroadcast(context,
                0,
                actionIntent,
                0);
    }
    private PendingIntent generateOpenSettingsIntent(Context context) {
        Intent openSettingsIntent = new Intent(context, SettingsActivity.class);
        // Add a distinction to make sure this can exist for each widget in the system
        openSettingsIntent.setAction("" + appWidgetId);
        openSettingsIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        return PendingIntent.getActivity(context,
                0,
                openSettingsIntent,
                0);
    }
}

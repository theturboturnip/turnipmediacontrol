package com.turboturnip.turnipmediacontrol;

import android.app.Notification;
import android.app.Service;
import android.content.AsyncQueryHandler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.drm.DrmStore;
import android.media.MediaDataSource;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.annotation.Nullable;
import android.util.JsonWriter;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MediaNotificationFinderService extends NotificationListenerService implements MediaSessionManager.OnActiveSessionsChangedListener {

    public static final int MSG_REQUEST_NOTIFICATION_LIST = 1;
    public static final int MSG_RECEIVE_NOTIFICATION_LIST = 2;

    private Collection<Messenger> inputMessengers = new ArrayList<>();

    // This class holds notification clones, so they won't change unexpectedly
    public static class MediaNotification {
        public final StatusBarNotification notification;
        public final MediaController controller;

        MediaNotification(StatusBarNotification notification,
                          MediaController controller) {
            this.notification = notification;
            this.controller = controller;
        }

        /*@Override
        public boolean equals(@Nullable Object obj) {
            if (!(obj instanceof MediaNotification))
                return false;

            return notification.getId() == ((MediaNotification)obj).notification.getId();
        }

        @Override
        public int hashCode() {
            return notification.getId();
        }*/
    }
    public static class MediaNotificationSet {
        List<MediaNotification> orderedMediaNotifications = new ArrayList<>();
    }
    private static MediaNotificationSet currentSet = new MediaNotificationSet();

    private static Set<StatusBarNotification> mediaNotifications = new HashSet<StatusBarNotification>(){
        @Override
        public boolean add(StatusBarNotification sbn) {
            remove(sbn);
            return super.add(sbn);
        }

        @Override
        public boolean remove(Object o) {
            if (!(o instanceof StatusBarNotification))
                return false;

            return removeIf(otherNotification -> otherNotification.getId() == ((StatusBarNotification)o).getId());
        }
    };

    public interface Interface {
        void updateNotificationSet(MediaNotificationSet notificationSet);
    }
    private static List<Interface> interfaces = new ArrayList<>();

    private static MediaSessionManager sessionManager;
    private static ComponentName componentName;

    @Override
    public void onCreate() {
        super.onCreate();
        if (sessionManager == null)
            sessionManager = getSystemService(MediaSessionManager.class);
        if (componentName == null)
            componentName = new ComponentName(this, MediaNotificationFinderService.class);
        sessionManager.addOnActiveSessionsChangedListener(this, componentName);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sessionManager != null)
            sessionManager.removeOnActiveSessionsChangedListener(this);
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Log.e("turnipmediacontrol", "Listener Connected");
        for (StatusBarNotification sbn : getActiveNotifications()){
            if (!notificationIsMedia(sbn)) continue;
            Log.e("turnipmediacontrol", "Found Notification: " + sbn.getPackageName());
            mediaNotifications.add(sbn);
        }
        queueReprioritize();
    }

    public static boolean reprioritize() {
        MediaNotificationSet set = new MediaNotificationSet();

        // This is sorted by priority
        List<MediaController> existingControllers = (sessionManager != null) ? sessionManager.getActiveSessions(componentName) : new ArrayList<>();

        Map<MediaSession.Token, StatusBarNotification> tokenMap = new HashMap<>();
        for (StatusBarNotification sbn : mediaNotifications) {
            MediaSession.Token mediaSessionToken = ((MediaSession.Token)sbn.getNotification().extras.get(Notification.EXTRA_MEDIA_SESSION));
            if (mediaSessionToken != null)
                tokenMap.put(mediaSessionToken, sbn);
        }

        for (MediaController controller : existingControllers) {
            StatusBarNotification associatedNotification = tokenMap.get(controller.getSessionToken());
            if (associatedNotification != null)
                set.orderedMediaNotifications.add(new MediaNotification(associatedNotification, controller));
        }

        //if (mediaNotifications.contains(currentSet.primaryMediaNotification))
        //    set.primaryMediaNotification = currentSet.primaryMediaNotification;

        /*for (StatusBarNotification mediaNotification : mediaNotifications) {
            // Check the notification has a MediaSession and a PlaybackState
            if (mediaSessionToken == null) continue;
            MediaController controller = tokenMap.get(mediaSessionToken);
            if (controller == null) continue;
            PlaybackState playbackState = controller.getPlaybackState();
            if (playbackState == null) continue;

            switch (playbackState.getState()) {
                case PlaybackState.STATE_NONE:
                case PlaybackState.STATE_STOPPED:
                case PlaybackState.STATE_ERROR:
                    continue;
                default:
                    break;
            }

            set.primaryMediaNotification = mediaNotification;
        }
        set.otherMediaNotifications = new HashSet<>();
        for (StatusBarNotification mediaNotification : mediaNotifications) {
            if (mediaNotification != set.primaryMediaNotification)
                set.otherMediaNotifications.add(mediaNotification);
        }*/

        boolean shouldUpdateListeners = (currentSet.orderedMediaNotifications != set.orderedMediaNotifications);
        currentSet = set;
        return shouldUpdateListeners;
    }

    private static void queueReprioritize(){
        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... voids) {
                if (reprioritize()) {
                    for (Interface i : interfaces) {
                        i.updateNotificationSet(currentSet);
                    }
                }
                return null;
            }
        }.doInBackground();
    }

    private void discoverMediaNotification(StatusBarNotification sbn){
        mediaNotifications.add(sbn);
        queueReprioritize();
    }
    private void removeMediaNotification(StatusBarNotification sbn){
        mediaNotifications.remove(sbn);
        queueReprioritize();
    }
    private boolean notificationIsMedia(StatusBarNotification sbn){
        return Notification.CATEGORY_TRANSPORT.equals(sbn.getNotification().category);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);

        if (notificationIsMedia(sbn))
            discoverMediaNotification(sbn);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);

        if (notificationIsMedia(sbn))
            removeMediaNotification(sbn);
    }

    @Override
    public void onActiveSessionsChanged(@Nullable List<MediaController> controllers) {
        queueReprioritize();
    }

    public static void attachInterface(Interface i) {
        if (!interfaces.contains(i))
            interfaces.add(i);
        i.updateNotificationSet(currentSet);
    }
    public static void detachInterface(Interface i) {
        interfaces.remove(i);
    }
}

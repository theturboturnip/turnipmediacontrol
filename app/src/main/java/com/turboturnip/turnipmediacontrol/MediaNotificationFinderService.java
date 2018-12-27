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
import android.media.MediaMetadata;
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
import android.provider.MediaStore;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.JsonWriter;
import android.util.Log;

import com.turboturnip.turnipmediacontrol.widget.MediaWidgetData;

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

        public MediaNotification(StatusBarNotification notification,
                          MediaController controller) {
            this.notification = notification;
            this.controller = controller;
        }

        public static boolean notificationsEqual(MediaNotification a, MediaNotification b) {
            if (a == null && b == null) return true;
            if (a == null || b == null) return false;
            return (a.notification.getId() == b.notification.getId());
        }

        /*@Override
        public boolean equals(@Nullable Object obj) {
            if (!(obj instanceof MediaNotification))
                return false;

            return notification.describeContents() == ((MediaNotification)obj).notification.describeContents();
        }

        @Override
        public int hashCode() {
            return notification.describeContents();
        }*/


    }
    public static class MediaNotificationSet {
        public List<MediaNotification> orderedMediaNotifications = new ArrayList<>();

        @NonNull
        @Override
        public String toString() {
            return "[MediaNotificationSet size:" + orderedMediaNotifications.size() + "]";
        }
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
        void onUpdateOrder(MediaNotificationSet notificationSet);
        void onUpdateState(MediaNotificationSet notificationSet);
    }
    private static Set<Interface> interfaces = new HashSet<>();

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
        //Log.e("turnipmediacontrol", "Listener Connected");
        for (StatusBarNotification sbn : getActiveNotifications()){
            if (!notificationIsMedia(sbn)) continue;
            //Log.e("turnipmediacontrol", "Found Notification: " + sbn.getPackageName());
            mediaNotifications.add(sbn);
        }
        queueReprioritize();
    }

    private static final int POSITIONS_CHANGED = 1 << 0;
    private static final int STATE_CHANGED = 1 << 1;
    private static int reprioritize() {
        MediaNotificationSet set = new MediaNotificationSet();

        // This is sorted by priority
        List<MediaController> existingControllers = (sessionManager != null) ? sessionManager.getActiveSessions(componentName) : new ArrayList<>();

        Map<MediaSession.Token, StatusBarNotification> tokenMap = new HashMap<>();
        for (StatusBarNotification sbn : mediaNotifications) {
            MediaSession.Token mediaSessionToken = ((MediaSession.Token)sbn.getNotification().extras.get(Notification.EXTRA_MEDIA_SESSION));
            if (mediaSessionToken != null)
                tokenMap.put(mediaSessionToken, sbn);
            else
                Log.e("turnipmedia", "notification from" + sbn.getPackageName() + " has no mediasessiontoken");
            if (sbn.getPackageName().equals("com.simplemobiletools.musicplayer")) {
                Log.e("turnipmedia", "session token for simple player = " + mediaSessionToken);
            }
        }

        for (MediaController controller : existingControllers) {
            StatusBarNotification associatedNotification = tokenMap.get(controller.getSessionToken());
            if (associatedNotification != null &&
                    controller.getMetadata() != null)
                set.orderedMediaNotifications.add(new MediaNotification(associatedNotification, controller));
            Log.e("turnipmedia", "mediasessiontoken " + controller.getSessionToken() + " on package " + controller.getPackageName());
        }

        Log.e("turnipmedia", "[");
        for (MediaNotification notification : set.orderedMediaNotifications) {
            Log.e("turnipmedia", "\t" + notification.notification.getPackageName() + " - " + notification.controller.getSessionToken());
        }
        Log.e("turnipmedia", "]");

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

        int shouldUpdateListeners = 0;
        if (currentSet == null)
            shouldUpdateListeners = POSITIONS_CHANGED | STATE_CHANGED;
        else if (set.orderedMediaNotifications.size() != currentSet.orderedMediaNotifications.size())
            shouldUpdateListeners = POSITIONS_CHANGED | STATE_CHANGED;
        else {
            for (int i = 0; i < set.orderedMediaNotifications.size(); i++) {
                MediaNotification newNotification = set.orderedMediaNotifications.get(i);
                MediaNotification oldNotification = currentSet.orderedMediaNotifications.get(i);
                if (oldNotification.notification.getId() != newNotification.notification.getId()){
                    shouldUpdateListeners = shouldUpdateListeners | POSITIONS_CHANGED;
                }

                newNotification = null;
                for (MediaNotification notification : currentSet.orderedMediaNotifications) {
                    if (notification.notification.getId() == oldNotification.notification.getId()) {
                        newNotification = notification;
                        break;
                    }
                }
                if (newNotification == null) continue;

                if (!Util.objectsEqual(newNotification.controller.getMetadata(), oldNotification.controller.getMetadata())) {
                    shouldUpdateListeners = shouldUpdateListeners | STATE_CHANGED;
                    break;
                }

                if (!Util.objectsEqual(newNotification.notification.getNotification().extras, oldNotification.notification.getNotification().extras)){
                    shouldUpdateListeners = shouldUpdateListeners | STATE_CHANGED;
                    break;
                }

                PlaybackState oldPlaybackState = oldNotification.controller.getPlaybackState();
                PlaybackState newPlaybackState = newNotification.controller.getPlaybackState();
                if (oldPlaybackState == null && newPlaybackState == null) continue;
                if (oldPlaybackState == null || newPlaybackState == null) {
                    shouldUpdateListeners = shouldUpdateListeners | STATE_CHANGED;
                    break;
                }
                if (newPlaybackState.getState() !=oldPlaybackState.getState()) {
                    shouldUpdateListeners = shouldUpdateListeners | STATE_CHANGED;
                    break;
                }
            }
        }
        Log.i("turnipmedia", "New: " + set + " Current: " + currentSet + " Will Update Listeners: " + shouldUpdateListeners);

        currentSet = set;
        return shouldUpdateListeners;
    }

    private static void queueReprioritize(){
        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... voids) {
                int reprioritizeResult = reprioritize();
                if (reprioritizeResult != 0) {
                    for (Interface i : interfaces) {
                        if ((reprioritizeResult & POSITIONS_CHANGED) != 0)
                            i.onUpdateOrder(currentSet);
                        if ((reprioritizeResult & STATE_CHANGED) != 0)
                            i.onUpdateState(currentSet);
                    }
                }
                return null;
            }
        }.doInBackground();
    }

    private void discoverMediaNotification(StatusBarNotification sbn){
        mediaNotifications.add(sbn);
        Log.e("turnipmedia", "Discovered notification from " + sbn.getPackageName());
        queueReprioritize();
    }
    private void removeMediaNotification(StatusBarNotification sbn){
        mediaNotifications.remove(sbn);
        Log.e("turnipmedia", "Threw away notification from " + sbn.getPackageName());
        queueReprioritize();
    }
    private boolean notificationIsMedia(StatusBarNotification sbn){
        return sbn.getNotification().extras.get(Notification.EXTRA_MEDIA_SESSION) != null;
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
        interfaces.add(i);
        i.onUpdateOrder(currentSet);
        i.onUpdateState(currentSet);
    }
    public static void detachInterface(Interface i) {
        interfaces.remove(i);
    }
}

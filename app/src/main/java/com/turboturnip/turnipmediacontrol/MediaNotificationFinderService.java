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

public class MediaNotificationFinderService extends NotificationListenerService {

    public static final int MSG_REQUEST_NOTIFICATION_LIST = 1;
    public static final int MSG_RECEIVE_NOTIFICATION_LIST = 2;

    private Collection<Messenger> inputMessengers = new ArrayList<>();

    // This class holds notification clones, so they won't change unexpectedly
    public static class MediaNotificationSet {
        StatusBarNotification primaryMediaNotification;
        Set<StatusBarNotification> otherMediaNotifications;
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

    // TO USE:
    // Call MediaNotificationFinderService.addListener or something

    /*public interface NotificationsProvider {
        Set<String> getAppsWithNotifications();
    }

    static class IncomingMessageHandler extends Handler {
        private final NotificationsProvider provider;
        IncomingMessageHandler(NotificationsProvider provider){
            this.provider = provider;
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.replyTo == null || msg.what != MSG_REQUEST_NOTIFICATION_LIST) {
                super.handleMessage(msg);
                return;
            }

            // Pack notification data into message
            String jsonNotificationData = "";
            try{
                JSONStringer stringer = new JSONStringer();
                stringer.object();
                stringer.key("ARRAY");
                stringer.array();
                for (String appWithNotification : provider.getAppsWithNotifications()){
                    stringer.value(appWithNotification);
                }
                stringer.endArray();
                stringer.endObject();
                jsonNotificationData = stringer.toString();
            }catch (JSONException e){
                e.printStackTrace();
            }
            Message toSend = Message.obtain(this, MSG_RECEIVE_NOTIFICATION_LIST, jsonNotificationData);

            try {
                msg.replyTo.send(toSend);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }*/

    public interface Interface {
        void updateNotificationSet(MediaNotificationSet notificationSet);
    }
    private static List<Interface> interfaces = new ArrayList<>();

    /*public static abstract class Interface {
        private Messenger serviceOutputConnection;
        private Messenger serviceInputConnection;
        public boolean isBound = false;

        private ServiceConnection mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder binder) {
                // This is called when the connection with the service has been
                // established, giving us the object we can use to
                // interact with the service.  We are communicating with the
                // service using a Messenger, so here we get a client-side
                // representation of that from the raw IBinder object.
                serviceInputConnection = new Messenger(binder);
                isBound = true;
            }

            public void onServiceDisconnected(ComponentName className) {
                // This is called when the connection with the service has been
                // unexpectedly disconnected -- that is, its process crashed.
                serviceInputConnection = null;
                isBound = false;
            }
        };

        private static class NotificationReceiver extends Handler {
            private final Interface owner;
            NotificationReceiver(Interface owner) {
                this.owner = owner;
            }

            @Override
            public void handleMessage(Message msg) {
                if (msg.what != MSG_RECEIVE_NOTIFICATION_LIST) {
                    super.handleMessage(msg);
                    return;
                }

                // Extract names from msg
                List<String> notificationNames = new ArrayList<>();
                try {
                    JSONObject object = new JSONObject((String)msg.obj);
                    JSONArray array = object.getJSONArray("ARRAY");
                    for (int i = 0; i < array.length(); i++) {
                        notificationNames.add(array.getString(i));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                owner.getNotificationNames(notificationNames);
            }
        }

        public Interface() {
            serviceOutputConnection = new Messenger(new NotificationReceiver(this));
        }

        abstract void getNotificationNames(Collection<String> names);

        public void onStart(Context context) {
            context.bindService(new Intent(context, MediaNotificationFinderService.class), mConnection,
                    Context.BIND_AUTO_CREATE);
        }
        public void onStop(Context context) {
            if (isBound) {
                context.unbindService(mConnection);
                isBound = false;
            }
        }

        public void requestNotificationList() {
            if (!isBound) return;*/

            /*Message toSend = Message.obtain(null, MSG_REQUEST_NOTIFICATION_LIST);
            toSend.replyTo = serviceOutputConnection;
            try {
                serviceInputConnection.send(toSend);
            } catch (RemoteException e) {
                e.printStackTrace();
            }*/
    //    }
    //}

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Log.e("turnipmediacontrol", "Listener Connected");
        for (StatusBarNotification sbn : getActiveNotifications()){
            if (!notificationIsMedia(sbn)) continue;
            Log.e("turnipmediacontrol", "Found Notification: " + sbn.getPackageName());
            discoverMediaNotification(sbn);
        }
    }

    public static void updateCurrentSet(Context toUse) {
        MediaNotificationSet set = new MediaNotificationSet();

        List<MediaController> existingControllers = toUse.getSystemService(MediaSessionManager.class).getActiveSessions(new ComponentName(toUse, MediaNotificationFinderService.class));
        Map<MediaSession.Token, MediaController> tokenMap = new HashMap<>();
        for (MediaController controller : existingControllers) {
            tokenMap.put(controller.getSessionToken(), controller);
        }

        if (mediaNotifications.contains(currentSet.primaryMediaNotification))
            set.primaryMediaNotification = currentSet.primaryMediaNotification;

        for (StatusBarNotification mediaNotification : mediaNotifications) {
            // Check the notification has a MediaSession and a PlaybackState
            MediaSession.Token mediaSessionToken = ((MediaSession.Token)mediaNotification.getNotification().extras.get(Notification.EXTRA_MEDIA_SESSION));
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
        }

        boolean shouldUpdateListeners = (currentSet.primaryMediaNotification != set.primaryMediaNotification)
                ||(currentSet.otherMediaNotifications != set.otherMediaNotifications);
        currentSet = set;
        if (shouldUpdateListeners)
            updateListeners();
    }

    private static void updateListeners(){
        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... voids) {
                for (Interface i : interfaces){
                    i.updateNotificationSet(currentSet);
                }
                return null;
            }
        }.doInBackground();
    }

    private void discoverMediaNotification(StatusBarNotification sbn){
        mediaNotifications.add(sbn);
        updateCurrentSet(this);
    }
    private void removeMediaNotification(StatusBarNotification sbn){
        mediaNotifications.remove(sbn);
        updateCurrentSet(this);
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


    public static void attachInterface(Interface i) {
        if (!interfaces.contains(i))
            interfaces.add(i);
        i.updateNotificationSet(currentSet);
    }
    public static void detachInterface(Interface i) {
        interfaces.remove(i);
    }
}

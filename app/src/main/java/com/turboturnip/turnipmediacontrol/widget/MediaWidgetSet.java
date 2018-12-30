package com.turboturnip.turnipmediacontrol.widget;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.media.session.MediaController;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.turboturnip.turnipmediacontrol.LogHelper;
import com.turboturnip.turnipmediacontrol.MediaNotificationFinderService;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Singleton class, holds all MediaWidgetData instances and manipulates them based on requests
public class MediaWidgetSet {
    private static final String TAG = LogHelper.getTag(MediaWidgetSet.class);
    public static final MediaWidgetSet instance = new MediaWidgetSet();

    public List<MediaNotificationFinderService.MediaNotification> orderedNotifications = new ArrayList<>();
    private List<MediaNotificationFinderService.MediaNotification> previousOrderedNotifications = new ArrayList<>();
    private Map<Integer, MediaWidgetData> widgetIdToData = new HashMap<>();
    private MediaNotificationFinderService.Interface notificationWatcher = new MediaNotificationFinderService.Interface() {
        @Override
        public void onUpdateOrder(MediaNotificationFinderService.MediaNotificationSet notificationSet) {
            LogHelper.i(TAG, "Got " + notificationSet.orderedMediaNotifications.size() + " Notifications from onUpdateOrder");
            orderedNotifications = notificationSet.orderedMediaNotifications;
            orderChangedSinceLastUpdate = true;
            queueUpdate();
        }

        // We don't care about state changes, those will be handled by MediaWidgetData
        @Override
        public void onUpdateState(MediaNotificationFinderService.MediaNotificationSet notificationSet) {
            orderedNotifications = notificationSet.orderedMediaNotifications;
            stateChangedSinceLastUpdate = true;
            queueUpdate();
        }
    };

    private boolean attached = false;
    private boolean shouldUpdate = false;
    private boolean orderChangedSinceLastUpdate = true;
    private boolean stateChangedSinceLastUpdate = false;

    WeakReference<Context> context = new WeakReference<>(null);
    AppWidgetManager appWidgetManager = null;
    ComponentName providerComponentName = null;

    private MediaWidgetSet() {
        MediaNotificationFinderService.attachInterface(notificationWatcher);
    }

    private boolean updateQueued = false;
    private static final long queuedUpdateDelay = 100;

    @SuppressLint("StaticFieldLeak") // The Context won't be leaked by the AsyncTask, because it's a WeakReference
    private void queueUpdate(){
        if (updateQueued) return;
        updateQueued = true;
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    Thread.sleep(queuedUpdateDelay);
                    if (context.get() != null)
                        updateKnownWidgets();
                    updateQueued = false;
                    return null;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.doInBackground();
    }

    public void updateContext(@NonNull Context context) {
        MediaNotificationFinderService.attachInterface(notificationWatcher);
        if (this.context.get() == null) {
            this.context = new WeakReference<>(context);
            if (appWidgetManager == null) {
                appWidgetManager = context.getSystemService(AppWidgetManager.class);
            }
            providerComponentName = new ComponentName(context, MediaWidgetProvider.class);
        }
        discoverNewWidgets(appWidgetManager.getAppWidgetIds(providerComponentName));
    }
    private MediaWidgetData getWidgetData(int appWidgetId) {
        if (context.get() == null)
            throw new IllegalStateException();

        MediaWidgetData data = widgetIdToData.get(appWidgetId);
        if (data == null){
            data = new MediaWidgetData(appWidgetId);
            widgetIdToData.put(appWidgetId, data);
            data.changeActiveNotification(context.get(), appWidgetManager, orderedNotifications.size() > 0 ? orderedNotifications.get(0) : null);
        }
        return data;
    }
    public void performAction(int appWidgetId, String action) {
        if (context.get() == null)
            return;

        MediaWidgetData widgetData = getWidgetData(appWidgetId);

        switch(action) {
            case MediaWidgetData.ACTION_SELECT_LEFT:
            {
                int notificationIndex = indexOfMatchingNotification(widgetData.selectedNotification);
                if (notificationIndex > 0)
                    widgetData.changeActiveNotification(context.get(), appWidgetManager, orderedNotifications.get(notificationIndex - 1));
                return;
            }
            case MediaWidgetData.ACTION_SELECT_RIGHT:
                int notificationIndex = indexOfMatchingNotification(widgetData.selectedNotification);
                if (notificationIndex < orderedNotifications.size() - 1)
                    widgetData.changeActiveNotification(context.get(), appWidgetManager, orderedNotifications.get(notificationIndex + 1));
                return;
        }

        if (widgetData.selectedNotification == null)
            return;
        MediaController controller = widgetData.selectedNotification.controller;
        if (controller == null)
            return;
        switch(action) {
            case MediaWidgetData.ACTION_PAUSE:
                controller.getTransportControls().pause();
                break;
            case MediaWidgetData.ACTION_PLAY:
                controller.getTransportControls().play();
                break;
            case MediaWidgetData.ACTION_SKIP_NEXT:
                controller.getTransportControls().skipToNext();
                break;
            case MediaWidgetData.ACTION_SKIP_PREVIOUS:
                controller.getTransportControls().skipToPrevious();
                break;
            default:
                throw new UnsupportedOperationException();
        }

    }
    public void discoverNewWidgets(int[] possiblyNewAppWidgetIds) {
        if (appWidgetManager == null) return;

        // Make sure all existing widgets are mapped
        for (int appWidgetId : possiblyNewAppWidgetIds) {
            getWidgetData(appWidgetId);
        }
    }
    public void removeWidget(int appWidgetId) {
        widgetIdToData.remove(appWidgetId);
    }
    public void updateKnownWidgets() {
        for (MediaWidgetData data : widgetIdToData.values()) {
            updateWidget(data);
        }
        previousOrderedNotifications = orderedNotifications;
        orderChangedSinceLastUpdate = false;
        stateChangedSinceLastUpdate = false;
    }

    int indexOfMatchingNotification(MediaNotificationFinderService.MediaNotification notificationToMatch) {
        if (notificationToMatch == null)
            return -1;
        return indexOfNotificationWithId(notificationToMatch.notification.getId());
    }
    private int indexOfNotificationWithId(int id) {
        for (int i = 0; i < orderedNotifications.size(); i++) {
            if (orderedNotifications.get(i).notification.getId() == id)
                return i;
        }
        return -1;
    }
    private void updateWidget(MediaWidgetData data) {
        if (orderChangedSinceLastUpdate) {
            // If this widget was on the previous highest priority notification
            if (previousOrderedNotifications.size() > 0 &&
                    MediaNotificationFinderService.MediaNotification.notificationsEqual(data.selectedNotification, previousOrderedNotifications.get(0))) {
                // Swap to the new one
                data.changeActiveNotification(context.get(), appWidgetManager, orderedNotifications.size() > 0 ? orderedNotifications.get(0) : null);
            } else if (data.selectedNotification != null) {
                int indexInNew = indexOfNotificationWithId(data.selectedNotification.notification.getId());
                if (indexInNew >= 0)
                    data.updateActiveNotification(context.get(), appWidgetManager, orderedNotifications.get(indexInNew));
                else
                    data.changeActiveNotification(context.get(), appWidgetManager, orderedNotifications.size() > 0 ? orderedNotifications.get(0) : null);
            } else if (orderedNotifications.size() > 0) {
                data.changeActiveNotification(context.get(), appWidgetManager, orderedNotifications.get(0));
            }
        }else if (stateChangedSinceLastUpdate && data.selectedNotification != null) {
            data.updateActiveNotification(context.get(), appWidgetManager, orderedNotifications.get(indexOfMatchingNotification(data.selectedNotification)));
        } else {
            data.generateViews(context.get(), appWidgetManager);
        }
    }
}

package com.turboturnip.turnipmediacontrol;

import android.content.pm.PermissionInfo;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.service.notification.StatusBarNotification;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.security.Permission;
import java.security.Permissions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ListNotificationsActivity extends AppCompatActivity {

    MediaNotificationFinderService.Interface notificationInterface = new MediaNotificationFinderService.Interface() {
        @Override
        public void onUpdateOrder(MediaNotificationFinderService.MediaNotificationSet notificationSet) {
            updateNames(notificationSet);
        }
        @Override
        public void onUpdateState(MediaNotificationFinderService.MediaNotificationSet notificationSet) {
            updateNames(notificationSet);
        }
    };

    TextView notificationListText;
    MediaSessionManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_notifications);
        notificationListText = findViewById(R.id.media_notification_text);
        findViewById(R.id.update_list_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //MediaNotificationFinderService.updateCurrentSet(ListNotificationsActivity.this);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        MediaNotificationFinderService.attachInterface(notificationInterface);
    }

    @Override
    protected void onStop() {
        super.onStop();
        MediaNotificationFinderService.detachInterface(notificationInterface);
    }

    void updateNames(MediaNotificationFinderService.MediaNotificationSet notificationSet){
        if (notificationListText == null)
            return;

        //Log.i("turnipmediacontrol", "Received Notifications!");
        StringBuilder namesBuilder = new StringBuilder();


        for (MediaNotificationFinderService.MediaNotification secondaryNotification : notificationSet.orderedMediaNotifications) {
            namesBuilder.append(secondaryNotification.notification.getPackageName()).append(':');
            if (secondaryNotification.controller.getMetadata() != null)
                namesBuilder.append(secondaryNotification.controller.getMetadata().getString(MediaMetadata.METADATA_KEY_TITLE));
            namesBuilder.append('\n');
        }

        notificationListText.setText(namesBuilder.toString());
    }

    /*@Override
    public void onActiveSessionsChanged(@Nullable List<MediaController> controllers) {
        if (notificationListText == null)
            return;

        List<MediaController> filteredControllers = new ArrayList<>();
        for (MediaController controller : controllers){
            //if (controller.getMetadata().)
            filteredControllers.add(controller);
        }

        Log.i("turnipmediacontrol", "Received Notifications!");
        StringBuilder namesBuilder = new StringBuilder();

        //namesBuilder.append("Primary: ");
        //if (filteredControllers.size() > 0)
        //    namesBuilder.append(filteredControllers.get(0).getPackageName());
        //else
        //    namesBuilder.append("null");
        //if (notificationSet.otherMediaNotifications != null) {
        //    namesBuilder.append("\nOthers: ");
            for (MediaController controller : filteredControllers) {
                namesBuilder.append(controller.getPackageName()).append('\n');
            }
        //}

        notificationListText.setText(namesBuilder.toString());
    }*/
}

package com.example.rongcloud_android_imdemo_import;

import android.content.Context;

import io.rong.push.PushType;
import io.rong.push.notification.PushMessageReceiver;
import io.rong.push.notification.PushNotificationMessage;
import io.rong.push.notification.RongNotificationInterface;

public class MyPushMessageReceiver extends PushMessageReceiver {
    @Override
    public boolean onNotificationMessageArrived(Context context, PushType pushType, PushNotificationMessage message) {

        if (message != null) {
            MyRongNotificationInterface.sendNotification(context, message, 0);
        }
        return true;
    }

    @Override
    public boolean onNotificationMessageClicked(Context context, PushType pushType, PushNotificationMessage pushNotificationMessage) {

        String notificationId = pushNotificationMessage.getExtra();
        RongNotificationInterface.removeNotification(context, Integer.parseInt(notificationId));
        
        return false;
    }
}

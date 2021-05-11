package com.example.rongcloud_android_imdemo_import;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import io.rong.push.RongPushClient;
import io.rong.push.common.PushCacheHelper;
import io.rong.push.common.PushConst;
import io.rong.push.common.RLog;
import io.rong.push.notification.PushNotificationMessage;

public class MyRongNotificationInterface {

    private static final String TAG = "RongNotificationInterface";
    private static HashMap<String, List<PushNotificationMessage>> messageCache = new HashMap<>();
    private static int NOTIFICATION_ID = 1000;  //落地消息通知id.
    private static int PUSH_SERVICE_NOTIFICATION_ID = 2000;  // 不落地消息通知id. 即消息类型为 PUSH_SERVICE/SYSTEM 的情况。
    private static int VOIP_NOTIFICATION_ID = 3000; //VoIP类型的通知消息。
    private static final int NEW_NOTIFICATION_LEVEL = 11;
    private static final int PUSH_REQUEST_CODE = 200;
    private static final int NEGLECT_TIME = 3000;
    private static long lastNotificationTimestamp;
    private static Uri mSound;
    private static boolean recallUpdate = false;

    /**
     * 发送通知。推送通知和后台通知最终都是通过这个方法发送notification。
     *
     * @param context 上下文
     * @param message 通知信息
     */
    public static void sendNotification(Context context, PushNotificationMessage message) {
        sendNotification(context, message, 0);
    }


    /**
     * 发送通知。推送通知和后台通知最终都是通过这个方法发送notification。
     *
     * @param context 上下文
     * @param message 通知信息
     */
    public static void sendNotification(Context context, PushNotificationMessage message, int left) {
        if (messageCache == null) {
            messageCache = new HashMap<>();
        }

        RongPushClient.ConversationType conversationType = message.getConversationType();
        String objName = message.getObjectName();
        String title;
        String content = "";
        int notificationId;
        boolean isMulti = false;
        int requestCode = 200;
        SoundType soundType = SoundType.DEFAULT;

        RLog.i(TAG, "sendNotification() messageType: " + message.getConversationType()
                + " messagePushContent: " + message.getPushContent()
                + " messageObjectName: " + message.getObjectName());
        if (TextUtils.isEmpty(objName) || conversationType == null) {
            return;
        }

        // 弹通知间隔太小时，静音处理
        long now = System.currentTimeMillis();
        if (now - lastNotificationTimestamp < NEGLECT_TIME) {
            soundType = SoundType.SILENT;
        } else {
            lastNotificationTimestamp = now;
        }

        if (conversationType != null && ((conversationType.equals(RongPushClient.ConversationType.SYSTEM)
                || conversationType.equals(RongPushClient.ConversationType.PUSH_SERVICE)))) {
            title = message.getPushTitle();
            if (TextUtils.isEmpty(title)) {
                title = (String) context.getPackageManager().getApplicationLabel(context.getApplicationInfo());
            }
            content = message.getPushContent();
            notificationId = PUSH_SERVICE_NOTIFICATION_ID;
            requestCode = 300;
            PUSH_SERVICE_NOTIFICATION_ID++;
        } else if (objName != null && (objName.equals("RC:VCInvite") || objName.equals("RC:VCModifyMem") || objName.equals("RC:VCHangup"))) {
            if (objName.equals("RC:VCHangup")) {
                removeNotification(context, VOIP_NOTIFICATION_ID);
                return;
            }
            notificationId = VOIP_NOTIFICATION_ID;
            soundType = SoundType.VOIP;
            requestCode = 400;
            title = (String) context.getPackageManager().getApplicationLabel(context.getApplicationInfo());
            content = message.getPushContent();
        } else {
            //缓存离线消息。
            List<PushNotificationMessage> messages = messageCache.get(message.getTargetId());
            if (messages == null) {
                messages = new ArrayList<>();
                messages.add(message);
                messageCache.put(message.getTargetId(), messages);
            } else {
                if (objName.equals("RC:RcNtf")) {
                    for (int i = messages.size() - 1; i >= 0; i--) {
                        if (messages.get(i) != null && messages.get(i).getPushId() != null && messages.get(i).getPushId().equals(message.getPushId())) {
                            messages.remove(messages.get(i));
                            break;
                        }
                    }
                    if (messages.size() == 0) {
                        if (messageCache.size() == 1) {
                            messages.add(message);
                        } else {
                            messageCache.remove(message.getTargetId());
                            if (messageCache.size() == 1) {
                                recallUpdate = true;
                            }
                        }
                    }
                } else {
                    if (messages.get(messages.size() - 1).getObjectName().equals("RC:RcNtf")) {
                        messages.remove(messages.size() - 1);
                    }
                    messages.add(message);
                }
            }
            if (messageCache.size() > 1) {
                isMulti = true;
            }
            title = getNotificationTitle(context);
            notificationId = NOTIFICATION_ID;


        }
        if (left > 0) {
            return;
        }

        String extra = message.getExtra();
        String extraWithNotificationId = extra + notificationId;
        message.setExtra(extraWithNotificationId);

        PendingIntent intent;
        if (recallUpdate) {
            intent = updateRecallPendingIntent(context, requestCode, isMulti);
        } else {
            intent = createPendingIntent(context, message, requestCode, isMulti);
        }
        Notification notification = createNotification(context, title, intent, content, soundType);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            String channelName = context.getResources().getString(context.getResources().getIdentifier("rc_notification_channel_name", "string", context.getPackageName()));
            NotificationChannel notificationChannel = new NotificationChannel("rc_notification_id", channelName, importance);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.GREEN);
            if (notification != null && notification.sound != null) {
                notificationChannel.setSound(notification.sound, null);
            }
            nm.createNotificationChannel(notificationChannel);
        }
        if (notification != null) {
            RLog.i(TAG, "sendNotification() real notify! notificationId: " + notificationId +
                    " notification: " + notification.toString());
            nm.notify(notificationId, notification);
        }
    }

    private static PendingIntent updateRecallPendingIntent(Context context, int requestCode, boolean isMulti) {
        Collection<List<PushNotificationMessage>> collection = messageCache.values();
        List<PushNotificationMessage> msg = collection.iterator().next();
        PushNotificationMessage notificationMessage = msg.get(0);

        Intent intent = new Intent();
        intent.setAction(PushConst.ACTION_NOTIFICATION_MESSAGE_CLICKED);
        intent.putExtra("message", notificationMessage);
        intent.putExtra("isMulti", isMulti);
        intent.setPackage(context.getPackageName());
        return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * 清除应用的所有推送通知。
     *
     * @param context 上下文。
     */
    public static void removeAllNotification(Context context) {
        messageCache.clear();
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        try {
            nm.cancelAll();
        } catch (Exception e) {
            RLog.e(TAG, "removeAllNotification" + e.getMessage());
        }
        NOTIFICATION_ID = 1000;
    }

    /**
     * 清除所有离线消息的推送通知。也就是说，如果是从开发者后台发送推送服务的通知，仍然会保留，不会清除。
     *
     * @param context 上下文。
     */
    public static void removeAllPushNotification(Context context) {
        messageCache.clear();
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATION_ID);
        nm.cancel(VOIP_NOTIFICATION_ID);
    }

    /**
     * 清除所有后台推送服务的推送通知。后台推送服务，是指开发者后台的广播推送服务。
     *
     * @param context 上下文。
     */
    public static void removeAllPushServiceNotification(Context context) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        for (int i = PUSH_SERVICE_NOTIFICATION_ID; i >= 1000; i--) {
            nm.cancel(i);
        }
        PUSH_SERVICE_NOTIFICATION_ID = 2000;
    }

    public static void removeNotification(Context context, int notificationId) {
        if (notificationId < 0) {
            return;
        }
        if (notificationId >= NOTIFICATION_ID && notificationId < PUSH_SERVICE_NOTIFICATION_ID) {
            messageCache.clear();
        }

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(notificationId);
    }

    private static PendingIntent createPendingIntent(Context context, PushNotificationMessage message, int requestCode, boolean isMulti) {
        Intent intent = new Intent();
        intent.setAction(PushConst.ACTION_NOTIFICATION_MESSAGE_CLICKED);
        intent.putExtra("message", message);
        intent.putExtra(PushConst.IS_MULTI, isMulti);
        intent.setPackage(context.getPackageName());
        return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static String getNotificationContent(Context context) {
        String content;
        String rc_notification_new_msg = context.getResources().getString(context.getResources().getIdentifier("rc_notification_new_msg", "string", context.getPackageName()));
        String rc_notification_new_plural_msg = context.getResources().getString(context.getResources().getIdentifier("rc_notification_new_plural_msg", "string", context.getPackageName()));

        if (messageCache.size() == 1) {
            Collection<List<PushNotificationMessage>> collection = messageCache.values();
            List<PushNotificationMessage> msg = collection.iterator().next();
            PushNotificationMessage notificationMessage = msg.get(0);

            if (msg.size() == 1) {
                content = notificationMessage.getPushContent();
            } else {
                if (msg.get(msg.size() - 1).getObjectName().equals("RC:RcNtf")) {
                    notificationMessage = msg.get(msg.size() - 1);
                    content = notificationMessage.getPushContent();
                } else {
                    content = String.format(rc_notification_new_msg, notificationMessage.getTargetUserName(), msg.size());
                }
            }
        } else {
            int count = 0;
            Collection<List<PushNotificationMessage>> collection = messageCache.values();
            for (List<PushNotificationMessage> msg : collection) {
                count += msg.size();
            }
            content = String.format(rc_notification_new_plural_msg, messageCache.size(), count);
        }
        return content;
    }

    private static String getNotificationTitle(Context context) {
        String title;
        if (messageCache.size() == 1) {
            Collection<List<PushNotificationMessage>> collection = messageCache.values();
            List<PushNotificationMessage> msg = collection.iterator().next();
            PushNotificationMessage notificationMessage = msg.get(0);
            title = notificationMessage.getTargetUserName();
        } else {
            title = (String) context.getPackageManager().getApplicationLabel(context.getApplicationInfo());
        }
        return title;
    }

    public static Notification createNotification(Context context, String title, PendingIntent pendingIntent, String content, SoundType soundType) {
        String tickerText = context.getResources().getString(context.getResources().getIdentifier("rc_notification_ticker_text", "string", context.getPackageName()));
        Notification notification;
        if (TextUtils.isEmpty(content)) {
            content = getNotificationContent(context);
        }
        if (Build.VERSION.SDK_INT < NEW_NOTIFICATION_LEVEL) {
            try {
                Method method;
                notification = new Notification(context.getApplicationInfo().icon, tickerText, System.currentTimeMillis());

                Class<?> classType = Notification.class;
                method = classType.getMethod("setLatestEventInfo", new Class[]{Context.class, CharSequence.class, CharSequence.class, PendingIntent.class});
                method.invoke(notification, new Object[]{context, title, content, pendingIntent});

                notification.flags = Notification.FLAG_AUTO_CANCEL;
                notification.defaults = Notification.DEFAULT_ALL;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        } else {
            boolean isLollipop = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
            int smallIcon = context.getResources().getIdentifier("notification_small_icon", "drawable", context.getPackageName());

            if (smallIcon <= 0 || !isLollipop) {
                smallIcon = context.getApplicationInfo().icon;
            }

            int defaults = Notification.DEFAULT_SOUND;
            Uri sound = null;
            if (soundType.equals(SoundType.SILENT)) {
                defaults = Notification.DEFAULT_LIGHTS;
            } else if (soundType.equals(SoundType.VOIP)) {
                defaults = Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE;
                sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            } else {
                sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            Drawable loadIcon = context.getApplicationInfo().loadIcon((context.getPackageManager()));
            Bitmap appIcon = null;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && loadIcon instanceof AdaptiveIconDrawable) {
                    appIcon = Bitmap.createBitmap(loadIcon.getIntrinsicWidth(), loadIcon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                    final Canvas canvas = new Canvas(appIcon);
                    loadIcon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                    loadIcon.draw(canvas);
                } else {
                    appIcon = ((BitmapDrawable) loadIcon).getBitmap();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Notification.Builder builder = new Notification.Builder(context);
            builder.setLargeIcon(appIcon);
            if (!soundType.equals(SoundType.SILENT)) {
                builder.setVibrate(new long[]{0, 200, 250, 200});
            }
            builder.setSmallIcon(smallIcon);
            builder.setTicker(tickerText);
            if (PushCacheHelper.getInstance().getPushContentShowStatus(context)) {
                builder.setContentTitle(title);
                builder.setContentText(content);
            } else {
                PackageManager pm = context.getPackageManager();
                String name;
                try {
                    name = pm.getApplicationLabel(pm.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA)).toString();
                } catch (PackageManager.NameNotFoundException e) {
                    name = "";
                }
                builder.setContentTitle(name);
                builder.setContentText(tickerText);
            }
            builder.setContentIntent(pendingIntent);
            builder.setLights(Color.GREEN, 3000, 3000);
            builder.setAutoCancel(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setChannelId("rc_notification_id");
            }
            if (mSound == null || TextUtils.isEmpty(mSound.toString())) {
                builder.setSound(sound);
                builder.setDefaults(defaults);
            } else {
                builder.setSound(mSound);
            }
            notification = builder.build();
            notification.flags = Notification.FLAG_SHOW_LIGHTS;
        }
        return notification;
    }

    public static void setNotificationSound(Uri uri) {
        mSound = uri;
    }

    public enum SoundType {
        DEFAULT(0),
        SILENT(1),
        VOIP(2);

        int value;

        SoundType(int v) {
            value = v;
        }
    }
}

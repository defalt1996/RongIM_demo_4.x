package io.rong.imkit.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;

import java.lang.reflect.Method;

import io.rong.common.RLog;

/**
 * Created by jiangecho on 2016/11/29.
 */

public class NotificationUtil {

    private static final String TAG = NotificationUtil.class.getSimpleName();
    private static final String CHANNEL_ID = "rc_notification_id";

    /**
     * @param context 上下文
     * @param title 标题
     * @param content 内容
     * @param intent PendingIntent
     * @param notificationId  通知 id
     * @param defaults       控制通知属性， 对应public Builder setDefaults(int defaults)
     */
    public static void showNotification(Context context, String title, String content, PendingIntent intent, int notificationId, int defaults) {
        Notification notification = createNotification(context, title, content, intent, defaults);
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            String channelName = context.getResources().getString(context.getResources().getIdentifier("rc_notification_channel_name", "string", context.getPackageName()));
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, channelName, importance);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.GREEN);
            if (notification != null && notification.sound != null) {
                notificationChannel.setSound(notification.sound, null);
            }
            nm.createNotificationChannel(notificationChannel);
        }
        if (notification != null) {
            nm.notify(notificationId, notification);
        }
    }

    public static void showNotification(Context context, String title, String content, PendingIntent intent, int notificationId) {
        showNotification(context, title, content, intent, notificationId, Notification.DEFAULT_ALL);
    }

    public static void clearNotification(Context context, int notificationId) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(notificationId);
    }

    private static Notification createNotification(Context context, String title, String content, PendingIntent pendingIntent, int defaults) {
        String tickerText = context.getResources().getString(context.getResources().getIdentifier("rc_notification_ticker_text", "string", context.getPackageName()));
        Notification notification;
        if (Build.VERSION.SDK_INT < 11) {
            try {
                Method method;
                notification = new Notification(context.getApplicationInfo().icon, tickerText, System.currentTimeMillis());

                Class<?> classType = Notification.class;
                method = classType.getMethod("setLatestEventInfo", new Class[]{Context.class, CharSequence.class, CharSequence.class, PendingIntent.class});
                method.invoke(notification, new Object[]{context, title, content, pendingIntent});

                notification.flags = Notification.FLAG_AUTO_CANCEL | Notification.FLAG_NO_CLEAR;
                notification.defaults = Notification.DEFAULT_ALL;
            } catch (Exception e) {
                RLog.e(TAG, "createNotification", e);
                return null;
            }
        } else {
            boolean isLollipop = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
            int smallIcon = context.getResources().getIdentifier("notification_small_icon", "drawable", context.getPackageName());

            if (smallIcon <= 0 || !isLollipop) {
                smallIcon = context.getApplicationInfo().icon;
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
                RLog.e(TAG, "createNotification", e);
            }

            Notification.Builder builder = new Notification.Builder(context);
            builder.setLargeIcon(appIcon);
            builder.setSmallIcon(smallIcon);
            builder.setTicker(tickerText);
            builder.setContentTitle(title);
            builder.setContentText(content);
            builder.setContentIntent(pendingIntent);
            builder.setAutoCancel(true);
            builder.setOngoing(true);
            builder.setDefaults(defaults);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setChannelId(CHANNEL_ID);
            }
            notification = builder.getNotification();
        }
        return notification;
    }

    public static int getRingerMode(Context context) {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return audio.getRingerMode();
    }
}

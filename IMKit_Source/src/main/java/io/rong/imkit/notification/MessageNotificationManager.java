package io.rong.imkit.notification;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.text.TextUtils;

import java.util.Calendar;
import java.util.List;

import io.rong.common.RLog;
import io.rong.imkit.R;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.RongNotificationManager;
import io.rong.imkit.model.ConversationInfo;
import io.rong.imkit.utils.NotificationUtil;
import io.rong.imkit.utils.SystemUtils;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.MentionedInfo;
import io.rong.imlib.model.Message;
import io.rong.message.RecallNotificationMessage;


/**
 * 控制弹通知和消息音
 * <p/>
 * 1、应用是否在后台
 * 2、新消息提醒设置
 * 3、安静时间设置
 */
public class MessageNotificationManager {
    private final static String TAG = "MessageNotificationManager";

    //应用在前台，如果没有在会话界面，收消息时每间隔 3s 一次响铃、震动。
    //收离线消息时，left 为 0 才会做震动、响铃。
    private final static int SOUND_INTERVAL = 3000;
    private long lastSoundTime = 0;

    private String startTime;
    private int spanTime;

    public String getNotificationQuietHoursStartTime() {
        return startTime;
    }

    public int getNotificationQuietHoursSpanTime() {
        return spanTime;
    }

    /**
     * 创建单实例。
     */
    private static class SingletonHolder {
        static final MessageNotificationManager instance = new MessageNotificationManager();
    }

    public static MessageNotificationManager getInstance() {
        return SingletonHolder.instance;
    }

    public void setNotificationQuietHours(String startTime, int spanTime) {
        this.spanTime = spanTime;
        this.startTime = startTime;
    }

    public void clearNotificationQuietHours() {
        this.startTime = null;
        this.spanTime = 0;
    }

    /**
     * 是否设置了消息免打扰，新消息提醒是否关闭？
     *
     * @param context 上下文
     * @param message 要通知的消息
     * @param left    剩余的消息
     */
    public void notifyIfNeed(final Context context, final Message message, final int left) {
        // @消息按最高优先级处理
        if (message.getContent().getMentionedInfo() != null) {
            MentionedInfo mentionedInfo = message.getContent().getMentionedInfo();
            if (mentionedInfo.getType().equals(MentionedInfo.MentionedType.ALL)
                    || (mentionedInfo.getType().equals(MentionedInfo.MentionedType.PART)
                    && mentionedInfo.getMentionedUserIdList() != null
                    && mentionedInfo.getMentionedUserIdList().contains(RongIMClient.getInstance().getCurrentUserId()))) {
                notify(context, message, left);
                return;
            }
        }

        boolean quiet = isInQuietTime();
        if (quiet) {
            RLog.d(TAG, "in quiet time, don't notify.");
            return;
        }

        if (message.getConversationType().equals(Conversation.ConversationType.ENCRYPTED)) {
            MessageNotificationManager.getInstance().notify(context, message, left);
        } else {
            RongIM.getInstance().getConversationNotificationStatus(message.getConversationType(), message.getTargetId(), new RongIMClient.ResultCallback<Conversation.ConversationNotificationStatus>() {
                @Override
                public void onSuccess(Conversation.ConversationNotificationStatus conversationNotificationStatus) {
                    if (conversationNotificationStatus.equals(Conversation.ConversationNotificationStatus.NOTIFY)) {
                        MessageNotificationManager.getInstance().notify(context, message, left);
                    }
                }

                @Override
                public void onError(RongIMClient.ErrorCode e) {

                }
            });
        }
    }

    private void notify(Context context, Message message, int left) {
        boolean isInBackground = SystemUtils.isInBackground(context);
        RLog.d(TAG, "isInBackground:" + isInBackground);
        if (message.getConversationType() == Conversation.ConversationType.CHATROOM) {
            return;
        }

        if (isInBackground) {
            RongNotificationManager.getInstance().onReceiveMessageFromApp(message, left);
        } else if (!isInConversationPager(message.getTargetId(), message.getConversationType())
                && left == 0
                && System.currentTimeMillis() - lastSoundTime > SOUND_INTERVAL) {
            if ((message.getContent()) instanceof RecallNotificationMessage
                    || message.getObjectName().equals("RCJrmf:RpOpendMsg")) {
                return;
            }

            if (context.getResources().getBoolean(R.bool.rc_sound_in_foreground)) {
                lastSoundTime = System.currentTimeMillis();
                int ringerMode = NotificationUtil.getRingerMode(context);
                if (ringerMode != AudioManager.RINGER_MODE_SILENT) {
                    if (ringerMode != AudioManager.RINGER_MODE_VIBRATE) {
                        sound();
                    }
                    vibrate(context);
                }
            } else {
                RLog.d(TAG, "message sound is disabled in rc_config.xml");
            }
        }
    }

    public boolean isInQuietTime() {
        int hour = -1;
        int minute = -1;
        int second = -1;

        if (!TextUtils.isEmpty(startTime) && startTime.contains(":")) {
            String[] time = startTime.split(":");

            try {
                if (time.length >= 3) {
                    hour = Integer.parseInt(time[0]);
                    minute = Integer.parseInt(time[1]);
                    second = Integer.parseInt(time[2]);
                }
            } catch (NumberFormatException e) {
                RLog.e(TAG, "getConversationNotificationStatus NumberFormatException");
            }
        }

        if (hour == -1 || minute == -1 || second == -1) {
            return false;
        }

        Calendar startCalendar = Calendar.getInstance();
        startCalendar.set(Calendar.HOUR_OF_DAY, hour);
        startCalendar.set(Calendar.MINUTE, minute);
        startCalendar.set(Calendar.SECOND, second);

        long startTime = startCalendar.getTimeInMillis();

        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTimeInMillis(startTime + spanTime * 60 * 1000);

        Calendar currentCalendar = Calendar.getInstance();
        //免打扰时段分为 不跨天（比如12：00--14：00）和 跨天（比如22：00 -- 第二天07：00）两种情况，不跨天走if里的逻辑，跨天走else里的逻辑
        if (currentCalendar.get(Calendar.DAY_OF_MONTH) == endCalendar.get(Calendar.DAY_OF_MONTH)) {

            return currentCalendar.after(startCalendar) && currentCalendar.before(endCalendar);
        } else {

            //跨天 且 currentCalendar 在 startCalendar 之前 ，需要判断 currentCalendar 是否在00：00到 endCalendar 之间
            if (currentCalendar.before(startCalendar)) {

                endCalendar.add(Calendar.DAY_OF_MONTH, -1);        //将endCalendar日期-1 ，再与currentCalendar比较

                return currentCalendar.before(endCalendar);
            } else {
                //跨天 且 currentCalendar 在 startCalendar 之后，则当前时间一定在免打扰时段，return true
                return true;
            }
        }
    }

    private boolean isInConversationPager(String id, Conversation.ConversationType type) {
        List<ConversationInfo> list = RongContext.getInstance().getCurrentConversationList();
        //如果处于所在会话界面，不响铃。
        for (ConversationInfo conversationInfo : list) {
            boolean isInConversationPage = id.equals(conversationInfo.getTargetId()) && type == conversationInfo.getConversationType();
            if (isInConversationPage) {
                return true;
            }
        }
        return false;
    }

    MediaPlayer mediaPlayer;

    private void sound() {
        Uri res = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        if (RongContext.getInstance().getNotificationSound() != null
                && !TextUtils.isEmpty(RongContext.getInstance().getNotificationSound().toString())) {
            res = RongContext.getInstance().getNotificationSound();
        }
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.reset();
                mediaPlayer.release();
                mediaPlayer = null;
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (mp != null) {
                        try {
                            mp.stop();
                            mp.reset();
                            mp.release();
                        } catch (Exception e) {
                            RLog.e(TAG, "sound", e);
                        }
                    }
                    if (mediaPlayer != null) {
                        mediaPlayer = null;
                    }
                }
            });
            //设置 STREAM_RING 模式：当系统设置震动时，使用系统设置方式是否播放收消息铃声。
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_RING);
            mediaPlayer.setDataSource(RongIM.getInstance().getApplicationContext(), res);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            RLog.e(TAG, "sound", e);
            if (mediaPlayer != null) {
                mediaPlayer = null;
            }
        }
    }

    private void vibrate(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(new long[]{0, 200, 250, 200}, -1);
        }
    }
}

package io.rong.imkit.destruct;

import java.util.HashMap;
import java.util.Map;

import io.rong.eventbus.EventBus;
import io.rong.imkit.model.Event;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.destruct.DestructionTaskManager;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;

/**
 * Created by Android Studio.
 * User: lvhongzhen
 * Date: 2019-09-11
 * Time: 10:26
 */
public class DestructManager {

    private Map<String, Map<String, RongIMClient.DestructCountDownTimerListener>> mMap;
    private Map<String, String> mUnFinishTimes;

    private DestructManager() {
        mMap = new HashMap<>();
        mUnFinishTimes = new HashMap<>();
    }

    public void addListener(String pUId, RongIMClient.DestructCountDownTimerListener pDestructListener, String pTag) {
        if (mMap.containsKey(pUId)) {
            Map<String, RongIMClient.DestructCountDownTimerListener> map = mMap.get(pUId);
            if (map != null) {
                map.put(pTag, pDestructListener);
            }
        } else {
            HashMap<String, RongIMClient.DestructCountDownTimerListener> map = new HashMap<>();
            map.put(pTag, pDestructListener);
            mMap.put(pUId, map);
        }
    }

    public void deleteMessage(Message pMessage) {
        DestructionTaskManager.getInstance().deleteMessage(pMessage);
    }

    public void deleteMessages(Conversation.ConversationType pConversationType, String pTargetId, Message[] pDeleteMessages) {
        DestructionTaskManager.getInstance().deleteMessages(pConversationType, pTargetId, pDeleteMessages);
    }

    private static class DestructManagerHolder {
        private static DestructManager instance = new DestructManager();
    }

    public static DestructManager getInstance() {
        return DestructManager.DestructManagerHolder.instance;
    }

    public String getUnFinishTime(String pMessageId) {
        return mUnFinishTimes.get(pMessageId);
    }

    public void startDestruct(final Message pMessage) {
        RongIMClient.getInstance().beginDestructMessage(pMessage, new RongIMClient.DestructCountDownTimerListener() {
            @Override
            public void onTick(final long untilFinished, final String messageId) {
                if (mMap.containsKey(messageId)) {
                    Map<String, RongIMClient.DestructCountDownTimerListener> map = mMap.get(messageId);
                    if (map != null) {
                        for (String key : map.keySet()) {
                            RongIMClient.DestructCountDownTimerListener destructCountDownTimerListener = map.get(key);
                            if (destructCountDownTimerListener != null) {
                                destructCountDownTimerListener.onTick(untilFinished, messageId);
                            }
                        }
                    }
                    if (untilFinished == 0) {
                        if (map != null) {
                            map.clear();
                        }
                        mMap.remove(messageId);
                        mUnFinishTimes.remove(messageId);
                        EventBus.getDefault().post(new Event.MessageDeleteEvent(pMessage.getMessageId()));
                    } else {
                        mUnFinishTimes.put(messageId, String.valueOf(untilFinished));
                    }
                }

            }

            @Override
            public void onStop(final String messageId) {
                if (mMap.containsKey(messageId)) {
                    Map<String, RongIMClient.DestructCountDownTimerListener> map = mMap.get(messageId);
                    if (map != null) {
                        for (String key : map.keySet()) {
                            RongIMClient.DestructCountDownTimerListener destructCountDownTimerListener = map.get(key);
                            if (destructCountDownTimerListener != null) {
                                destructCountDownTimerListener.onStop(messageId);
                            }
                        }
                    }
                    mUnFinishTimes.remove(messageId);
                }
            }
        });
    }

    public void stopDestruct(Message pMessage) {
        RongIMClient.getInstance().stopDestructMessage(pMessage);
    }

}

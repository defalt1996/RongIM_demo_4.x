package io.rong.imkit.manager;

import java.util.ArrayList;
import java.util.List;

import io.rong.common.RLog;
import io.rong.eventbus.EventBus;
import io.rong.imkit.model.Event;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;

/**
 * Created by yuejunhong on 16/9/13.
 */
public class UnReadMessageManager {
    private final static String TAG = "UnReadMessageManager";
    private final List<MultiConversationUnreadMsgInfo> mMultiConversationUnreadInfos;
    private int left;

    private UnReadMessageManager() {
        mMultiConversationUnreadInfos = new ArrayList<>();
        EventBus.getDefault().register(this);
    }

    private static class SingletonHolder {
        static UnReadMessageManager sInstance = new UnReadMessageManager();
    }

    public static UnReadMessageManager getInstance() {
        return SingletonHolder.sInstance;
    }

    public void onEventMainThread(final Event.OnReceiveMessageEvent event) {
        left = event.getLeft();
        if (event.getLeft() == 0) {
            syncUnreadCount(event.getMessage(), event.getLeft());
        }
    }

    public void onEventMainThread(final Event.MessageLeftEvent event) {
        RLog.d(TAG, "MessageLeftEvent " + event.left);
        // 此处不进行成员 left  数的更新，MessageLeftEvent 事件会在全收到消息后仍会抛出，导致 left 数不准确
        syncUnreadCount(null, event.left);
    }

    public void onEventMainThread(final Event.ConnectEvent event) {
        syncUnreadCount(null, 0);
    }

    private void syncUnreadCount(Message message, int left) {
        for (final MultiConversationUnreadMsgInfo msgInfo : mMultiConversationUnreadInfos) {
            /*
             * 判断当前消息数或成员变量的剩余消息数两者有一个为 0 时就进行刷新，防止当消息数为 0 时，
             * 如撤回消息等消息造成 left 因为是收到消息时的 left ，当所有消息全收到后此 left 还会为当时的剩余消息数，导致不刷新
             */
            if (left == 0 || this.left == 0) {
                RongIMClient.getInstance().getUnreadCount(msgInfo.conversationTypes, new RongIMClient.ResultCallback<Integer>() {
                    @Override
                    public void onSuccess(Integer integer) {
                        RLog.d(TAG, "get result: " + integer);
                        msgInfo.count = integer;
                        msgInfo.observer.onCountChanged(integer);
                    }

                    @Override
                    public void onError(RongIMClient.ErrorCode e) {

                    }
                });
            }
        }
    }

    public void onEventMainThread(final Event.ConversationRemoveEvent removeEvent) {
        Conversation.ConversationType conversationType = removeEvent.getType();
        for (final MultiConversationUnreadMsgInfo msgInfo : mMultiConversationUnreadInfos) {
            for (Conversation.ConversationType ct : msgInfo.conversationTypes) {
                if (ct.equals(conversationType)) {
                    RongIMClient.getInstance().getUnreadCount(msgInfo.conversationTypes, new RongIMClient.ResultCallback<Integer>() {
                        @Override
                        public void onSuccess(Integer integer) {
                            msgInfo.count = integer;
                            msgInfo.observer.onCountChanged(integer);
                        }

                        @Override
                        public void onError(RongIMClient.ErrorCode e) {

                        }
                    });
                    break;
                }
            }
        }
    }

    public void onEventMainThread(Message message) {
        syncUnreadCount(message, 0);
    }

    public void onEventMainThread(final Event.ConversationUnreadEvent unreadEvent) {
        Conversation.ConversationType conversationType = unreadEvent.getType();
        for (final MultiConversationUnreadMsgInfo msgInfo : mMultiConversationUnreadInfos) {
            for (Conversation.ConversationType ct : msgInfo.conversationTypes) {
                if (ct.equals(conversationType)) {
                    RongIMClient.getInstance().getUnreadCount(msgInfo.conversationTypes, new RongIMClient.ResultCallback<Integer>() {
                        @Override
                        public void onSuccess(Integer integer) {
                            msgInfo.count = integer;
                            msgInfo.observer.onCountChanged(integer);
                        }

                        @Override
                        public void onError(RongIMClient.ErrorCode e) {

                        }
                    });
                    break;
                }
            }
        }
    }

    public void addObserver(Conversation.ConversationType[] conversationTypes, final IUnReadMessageObserver observer) {
        synchronized (mMultiConversationUnreadInfos) {
            final MultiConversationUnreadMsgInfo msgInfo = new MultiConversationUnreadMsgInfo();
            msgInfo.conversationTypes = conversationTypes;
            msgInfo.observer = observer;
            mMultiConversationUnreadInfos.add(msgInfo);
            RongIMClient.getInstance().getUnreadCount(conversationTypes, new RongIMClient.ResultCallback<Integer>() {
                @Override
                public void onSuccess(Integer integer) {
                    msgInfo.count = integer;
                    msgInfo.observer.onCountChanged(integer);
                }

                @Override
                public void onError(RongIMClient.ErrorCode e) {

                }
            });
        }

    }

    public void removeObserver(final IUnReadMessageObserver observer) {
        synchronized (mMultiConversationUnreadInfos) {
            MultiConversationUnreadMsgInfo result = null;
            for (final MultiConversationUnreadMsgInfo msgInfo : mMultiConversationUnreadInfos) {
                if (msgInfo.observer == observer) {
                    result = msgInfo;
                    break;
                }
            }
            if (result != null) {
                mMultiConversationUnreadInfos.remove(result);
            }
        }
    }

    public void clearObserver() {
        synchronized (mMultiConversationUnreadInfos) {
            mMultiConversationUnreadInfos.clear();
        }
    }

    public void onMessageReceivedStatusChanged() {
        syncUnreadCount(null, 0);
    }

    public void onEventMainThread(Event.SyncReadStatusEvent event) {
        RLog.d(TAG, "SyncReadStatusEvent " + left);
        if (left == 0) {
            Conversation.ConversationType conversationType = event.getConversationType();
            for (final MultiConversationUnreadMsgInfo msgInfo : mMultiConversationUnreadInfos) {
                for (Conversation.ConversationType ct : msgInfo.conversationTypes) {
                    if (ct.equals(conversationType)) {
                        RongIMClient.getInstance().getUnreadCount(msgInfo.conversationTypes, new RongIMClient.ResultCallback<Integer>() {
                            @Override
                            public void onSuccess(Integer integer) {
                                msgInfo.count = integer;
                                msgInfo.observer.onCountChanged(integer);
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode e) {

                            }
                        });
                        break;
                    }
                }
            }
        }
    }

    public void onEventMainThread(Event.RemoteMessageRecallEvent event) {
        RLog.d(TAG, "SyncReadStatusEvent " + left);
        if (left == 0) {
            Conversation.ConversationType conversationType = event.getConversationType();
            for (final MultiConversationUnreadMsgInfo msgInfo : mMultiConversationUnreadInfos) {
                for (Conversation.ConversationType ct : msgInfo.conversationTypes) {
                    if (ct.equals(conversationType)) {
                        RongIMClient.getInstance().getUnreadCount(msgInfo.conversationTypes, new RongIMClient.ResultCallback<Integer>() {
                            @Override
                            public void onSuccess(Integer integer) {
                                msgInfo.count = integer;
                                msgInfo.observer.onCountChanged(integer);
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode e) {

                            }
                        });
                        break;
                    }
                }
            }
        }
    }

    private class MultiConversationUnreadMsgInfo {
        Conversation.ConversationType[] conversationTypes;
        int count;
        IUnReadMessageObserver observer;
    }
}

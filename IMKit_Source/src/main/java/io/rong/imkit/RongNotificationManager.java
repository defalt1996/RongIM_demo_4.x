package io.rong.imkit;

import android.content.Context;
import android.text.Spannable;
import android.text.TextUtils;

import java.util.concurrent.ConcurrentHashMap;

import io.rong.common.RLog;
import io.rong.imkit.model.ConversationKey;
import io.rong.imkit.model.GroupUserInfo;
import io.rong.imkit.userInfoCache.RongUserInfoManager;
import io.rong.imkit.widget.provider.IContainerItemProvider;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Discussion;
import io.rong.imlib.model.Group;
import io.rong.imlib.model.MentionedInfo;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessagePushConfig;
import io.rong.imlib.model.PublicServiceProfile;
import io.rong.imlib.model.UserInfo;
import io.rong.message.RecallNotificationMessage;
import io.rong.push.RongPushClient;
import io.rong.push.common.PushCacheHelper;
import io.rong.push.notification.PushNotificationMessage;

public class RongNotificationManager {
    private final static String TAG = "RongNotificationManager";
    private static RongNotificationManager sS;
    Context mContext;
    ConcurrentHashMap<String, Message> messageMap = new ConcurrentHashMap<>();

    static {
        sS = new RongNotificationManager();
    }

    private RongNotificationManager() {
    }

    public void init(Context context) {
        mContext = context;
        messageMap.clear();
        if (!RongContext.getInstance().getEventBus().isRegistered(this)) {
            RongContext.getInstance().getEventBus().register(this);
        }
    }

    public static RongNotificationManager getInstance() {
        if (sS == null) {
            sS = new RongNotificationManager();
        }
        return sS;
    }

    public void onReceiveMessageFromApp(Message message) {
        onReceiveMessageFromApp(message, 0);
    }

    public void onReceiveMessageFromApp(Message message, int left) {
        Conversation.ConversationType type = message.getConversationType();
        String targetName = null;
        String userName = "";
        PushNotificationMessage pushMsg;

        IContainerItemProvider.MessageProvider provider = RongContext.getInstance().getMessageTemplate(message.getContent().getClass());
        if (provider == null)
            return;

        Spannable content = provider.getContentSummary(mContext, message.getContent());
        ConversationKey targetKey = ConversationKey.obtain(message.getTargetId(), message.getConversationType());
        if (targetKey == null) {
            RLog.e(TAG, "onReceiveMessageFromApp targetKey is null");
        }
        RLog.i(TAG, "onReceiveMessageFromApp. conversationType:" + type);

        if (content == null) {
            RLog.e(TAG, "onReceiveMessageFromApp Content is null. Return directly.");
            return;
        }

        if (type.equals(Conversation.ConversationType.PRIVATE) || type.equals(Conversation.ConversationType.CUSTOMER_SERVICE)
                || type.equals(Conversation.ConversationType.CHATROOM) || type.equals(Conversation.ConversationType.SYSTEM)) {
            UserInfo userInfo = RongUserInfoManager.getInstance().getUserInfo(message.getTargetId());
            if (message.getMessagePushConfig() != null && !TextUtils.isEmpty(message.getMessagePushConfig().getPushTitle())) {
                // 设置了 title 默认能发送
                targetName = message.getMessagePushConfig().getPushTitle();
            } else if (userInfo != null) {
                targetName = userInfo.getName();
            }
            if (!TextUtils.isEmpty(targetName)) {
                pushMsg = transformToPushMessage(message, content.toString(), targetName, targetName);
                RongPushClient.sendNotification(mContext, pushMsg, left);
            } else {
                if (targetKey != null) {
                    messageMap.put(targetKey.getKey(), message);
                }
                RLog.e(TAG, "No popup notification cause of the sender name is null, please set UserInfoProvider");
            }
        } else if (type.equals(Conversation.ConversationType.GROUP)) {
            Group groupInfo = RongUserInfoManager.getInstance().getGroupInfo(message.getTargetId());
            UserInfo userInfo = RongUserInfoManager.getInstance().getUserInfo(message.getSenderUserId());
            GroupUserInfo groupUserInfo = RongUserInfoManager.getInstance().getGroupUserInfo(message.getTargetId(), message.getSenderUserId());
            if (groupInfo != null) {
                targetName = groupInfo.getName();
            }
            if (groupUserInfo != null) {
                userName = groupUserInfo.getNickname();
            }
            if (TextUtils.isEmpty(userName) && userInfo != null) {
                userName = userInfo.getName();
                RLog.d(TAG, "onReceiveMessageFromApp the nickName of group user is null");
            }

            if (!TextUtils.isEmpty(targetName) && !TextUtils.isEmpty(userName)) {
                String notificationContent;
                if (isMentionedMessage(message)) {
                    if (TextUtils.isEmpty(message.getContent().getMentionedInfo().getMentionedContent())) {
                        notificationContent = mContext.getString(R.string.rc_message_content_mentioned) + userName + " : " + content.toString();
                    } else {
                        notificationContent = message.getContent().getMentionedInfo().getMentionedContent();
                    }
                } else if (message.getContent() instanceof RecallNotificationMessage) {
                    notificationContent = content.toString();
                } else {
                    notificationContent = userName + " : " + content.toString();
                }
                pushMsg = transformToPushMessage(message, notificationContent, targetName, "");
                RongPushClient.sendNotification(mContext, pushMsg, left);
            } else {
                if (TextUtils.isEmpty(targetName)) {
                    if (targetKey != null) {
                        messageMap.put(targetKey.getKey(), message);
                    }
                }
                if (TextUtils.isEmpty(userName)) {
                    ConversationKey senderKey = ConversationKey.obtain(message.getSenderUserId(), type);
                    if (senderKey != null) {
                        messageMap.put(senderKey.getKey(), message);
                    } else {
                        RLog.e(TAG, "onReceiveMessageFromApp senderKey is null");
                    }
                }
                RLog.e(TAG, "No popup notification cause of the sender name is null, please set UserInfoProvider");
            }
        } else if (type.equals(Conversation.ConversationType.DISCUSSION)) {
            Discussion discussionInfo = RongUserInfoManager.getInstance().getDiscussionInfo(message.getTargetId());
            UserInfo userInfo = RongUserInfoManager.getInstance().getUserInfo(message.getSenderUserId());

            if (discussionInfo != null) {
                targetName = discussionInfo.getName();
            }
            if (userInfo != null) {
                userName = userInfo.getName();
            }
            if (!TextUtils.isEmpty(targetName) && !TextUtils.isEmpty(userName)) {
                String notificationContent;
                if (isMentionedMessage(message)) {
                    if (TextUtils.isEmpty(message.getContent().getMentionedInfo().getMentionedContent())) {
                        notificationContent = mContext.getString(R.string.rc_message_content_mentioned) + userName + " : " + content.toString();
                    } else {
                        notificationContent = message.getContent().getMentionedInfo().getMentionedContent();
                    }
                } else {
                    notificationContent = userName + " : " + content.toString();
                }
                pushMsg = transformToPushMessage(message, notificationContent, targetName, "");
                RongPushClient.sendNotification(mContext, pushMsg, left);
            } else {
                if (TextUtils.isEmpty(targetName)) {
                    if (targetKey != null) {
                        messageMap.put(targetKey.getKey(), message);
                    }
                }
                if (TextUtils.isEmpty(userName)) {
                    ConversationKey senderKey = ConversationKey.obtain(message.getSenderUserId(), type);
                    if (senderKey != null) {
                        messageMap.put(senderKey.getKey(), message);
                    } else {
                        RLog.e(TAG, "onReceiveMessageFromApp senderKey is null");
                    }
                }
                RLog.e(TAG, "No popup notification cause of the sender name is null, please set UserInfoProvider");
            }
        } else if (type.equals(Conversation.ConversationType.PUBLIC_SERVICE) ||
                type.getName().equals(Conversation.PublicServiceType.APP_PUBLIC_SERVICE.getName())) {
            if (targetKey != null) {
                PublicServiceProfile info = RongContext.getInstance().getPublicServiceInfoFromCache(targetKey.getKey());
                if (info != null) {
                    targetName = info.getName();
                }
            }
            if (!TextUtils.isEmpty(targetName)) {
                pushMsg = transformToPushMessage(message, content.toString(), targetName, "");
                RongPushClient.sendNotification(mContext, pushMsg, left);
            } else {
                if (targetKey != null) {
                    messageMap.put(targetKey.getKey(), message);
                }
                RLog.e(TAG, "No popup notification cause of the sender name is null, please set UserInfoProvider");
            }
        } else if (type.equals(Conversation.ConversationType.ENCRYPTED)) {
            String[] ids = message.getTargetId().split(";;;");
            if (ids.length < 2) {
                RLog.e(TAG, "Error targetId for encrypted conversation.");
                return;
            }
            String realId = ids[1];
            UserInfo userInfo = RongUserInfoManager.getInstance().getUserInfo(realId);
            if (userInfo != null && !TextUtils.isEmpty(userInfo.getName())) {
                pushMsg = transformToPushMessage(message, mContext.getString(R.string.rc_receive_new_message), userInfo.getName(), userInfo.getName());
                RongPushClient.sendNotification(mContext, pushMsg, left);
            } else {
                targetKey = ConversationKey.obtain(realId, message.getConversationType());
                if (targetKey != null) {
                    messageMap.put(targetKey.getKey(), message);
                }
                RLog.e(TAG, "No popup notification cause of the sender name is null, please set UserInfoProvider");
            }
        }
    }

    public void onEventMainThread(UserInfo userInfo) {
        Message message;
        PushNotificationMessage pushMsg;

        Conversation.ConversationType[] types = new Conversation.ConversationType[]{
                Conversation.ConversationType.PRIVATE, Conversation.ConversationType.GROUP,
                Conversation.ConversationType.DISCUSSION, Conversation.ConversationType.CUSTOMER_SERVICE,
                Conversation.ConversationType.CHATROOM, Conversation.ConversationType.SYSTEM
        };

        RLog.i(TAG, "onEventMainThread. userInfo" + userInfo);
        for (Conversation.ConversationType type : types) {
            String key = ConversationKey.obtain(userInfo.getUserId(), type).getKey();

            if (messageMap.containsKey(key)) {
                message = messageMap.get(key);
                String targetName = "";
                String notificationContent = "";
                Spannable content = RongContext.getInstance().getMessageTemplate(message.getContent().getClass())
                        .getContentSummary(mContext, message.getContent());

                messageMap.remove(key);

                if (type.equals(Conversation.ConversationType.GROUP)) {
                    Group groupInfo = RongUserInfoManager.getInstance().getGroupInfo(message.getTargetId());
                    GroupUserInfo groupUserInfo = RongUserInfoManager.getInstance().getGroupUserInfo(message.getTargetId(), message.getSenderUserId());
                    String userName = "";

                    if (groupInfo != null) {
                        targetName = groupInfo.getName();
                    } else {
                        RLog.e(TAG, "onEventMainThread userInfo : groupInfo is null, return directly");
                        return;
                    }

                    if (groupUserInfo != null) {
                        userName = groupUserInfo.getNickname();
                    }
                    if (TextUtils.isEmpty(userName) && userInfo != null) {
                        userName = userInfo.getName();
                        RLog.d(TAG, "onReceiveMessageFromApp the nickName of group user is null");
                    }


                    if (isMentionedMessage(message)) {
                        if (TextUtils.isEmpty(message.getContent().getMentionedInfo().getMentionedContent())) {
                            notificationContent = mContext.getString(R.string.rc_message_content_mentioned) + userName + " : " + content.toString();
                        } else {
                            notificationContent = message.getContent().getMentionedInfo().getMentionedContent();
                        }
                    } else {
                        notificationContent = userName + " : " + content.toString();
                    }
                } else if (type.equals(Conversation.ConversationType.DISCUSSION)) {
                    Discussion discussion = RongUserInfoManager.getInstance().getDiscussionInfo(message.getTargetId());
                    if (discussion != null) {
                        targetName = discussion.getName();
                    }
                    if (isMentionedMessage(message)) {
                        if (TextUtils.isEmpty(message.getContent().getMentionedInfo().getMentionedContent())) {
                            notificationContent = mContext.getString(R.string.rc_message_content_mentioned) + userInfo.getName() + " : " + content.toString();
                        } else {
                            notificationContent = message.getContent().getMentionedInfo().getMentionedContent();
                        }
                    } else {
                        notificationContent = userInfo.getName() + " : " + content.toString();
                    }
                } else if (type.equals(Conversation.ConversationType.ENCRYPTED)) {
                    targetName = userInfo.getName();
                    notificationContent = mContext.getString(R.string.rc_receive_new_message);
                } else {
                    targetName = userInfo.getName();
                    notificationContent = content.toString();
                }
                if (TextUtils.isEmpty(targetName))
                    return;
                pushMsg = transformToPushMessage(message, notificationContent, targetName, "");
                RongPushClient.sendNotification(mContext, pushMsg);
            }
        }
    }

    public void onEventMainThread(Group groupInfo) {
        Message message;
        PushNotificationMessage pushMsg;
        String key = ConversationKey.obtain(groupInfo.getId(), Conversation.ConversationType.GROUP).getKey();
        RLog.i(TAG, "onEventMainThread. groupInfo" + groupInfo);
        if (messageMap.containsKey(key)) {
            message = messageMap.get(key);
            String userName = "";
            Spannable content = RongContext.getInstance().getMessageTemplate(message.getContent().getClass())
                    .getContentSummary(mContext, message.getContent());

            messageMap.remove(key);

            UserInfo userInfo = RongUserInfoManager.getInstance().getUserInfo(message.getSenderUserId());
            if (userInfo != null) {
                userName = userInfo.getName();
                if (TextUtils.isEmpty(userName)) {
                    RLog.e(TAG, "onEventMainThread Group : userName is empty, return directly");
                    return;
                }
            } else {
                RLog.e(TAG, "onEventMainThread Group : userInfo is null, return directly");
                return;
            }
            String pushContent = userName + " : " + content.toString();
            pushMsg = transformToPushMessage(message, pushContent, groupInfo.getName(), "");
            RongPushClient.sendNotification(mContext, pushMsg);
        }

    }

    public void onEventMainThread(Discussion discussion) {
        Message message;
        PushNotificationMessage pushMsg;
        String key = ConversationKey.obtain(discussion.getId(), Conversation.ConversationType.DISCUSSION).getKey();
        if (messageMap.containsKey(key)) {
            String userName = "";
            message = messageMap.get(key);
            Spannable content = RongContext.getInstance().getMessageTemplate(message.getContent().getClass())
                    .getContentSummary(mContext, message.getContent());

            messageMap.remove(key);

            UserInfo userInfo = RongUserInfoManager.getInstance().getUserInfo(message.getSenderUserId());
            if (userInfo != null) {
                userName = userInfo.getName();
                if (TextUtils.isEmpty(userName))
                    return;
            }
            String pushContent = userName + " : " + content.toString();
            pushMsg = transformToPushMessage(message, pushContent, discussion.getName(), "");
            RongPushClient.sendNotification(mContext, pushMsg);
        }
    }

    public void onEventMainThread(PublicServiceProfile info) {
        Message message;
        PushNotificationMessage pushMsg;
        String key = ConversationKey.obtain(info.getTargetId(), info.getConversationType()).getKey();

        if (messageMap.containsKey(key)) {
            message = messageMap.get(key);
            Spannable content = RongContext.getInstance().getMessageTemplate(message.getContent().getClass())
                    .getContentSummary(mContext, message.getContent());
            pushMsg = transformToPushMessage(message, content.toString(), info.getName(), "");
            RongPushClient.sendNotification(mContext, pushMsg);
            messageMap.remove(key);
        }
    }

    private boolean isMentionedMessage(Message message) {
        MentionedInfo mentionedInfo = message.getContent().getMentionedInfo();
        if (mentionedInfo != null && (mentionedInfo.getType().equals(MentionedInfo.MentionedType.ALL)
                || (mentionedInfo.getType().equals(MentionedInfo.MentionedType.PART)
                && mentionedInfo.getMentionedUserIdList() != null
                && mentionedInfo.getMentionedUserIdList().contains(RongIMClient.getInstance().getCurrentUserId())))) {
            return true;
        }
        return false;
    }

    private PushNotificationMessage transformToPushMessage(Message message, String content, String targetUserName, String senderName) {
        PushNotificationMessage pushMsg = new PushNotificationMessage();
        MessagePushConfig messagePushConfig = message.getMessagePushConfig();
        boolean isShowDetail = true;

        if (messagePushConfig != null) {
            if (!messagePushConfig.isForceShowDetailContent() && !PushCacheHelper.getInstance().getPushContentShowStatus(mContext)) {
                pushMsg.setPushContent(mContext.getString(R.string.rc_receive_new_message));
                isShowDetail = false;
            } else {
                if (!TextUtils.isEmpty(messagePushConfig.getPushContent())) {
                    pushMsg.setPushContent(messagePushConfig.getPushContent());
                } else {
                    if (message.getContent() instanceof RecallNotificationMessage) {
                        if (message.getConversationType().equals(Conversation.ConversationType.PRIVATE)) {
                            content = (mContext.getString(R.string.rc_recalled_a_message)).trim();
                        }
                    }
                    pushMsg.setPushContent(content);
                }
            }
            if (messagePushConfig.getAndroidConfig() != null) {
                String notificationId = messagePushConfig.getAndroidConfig().getNotificationId();
                if (!TextUtils.isEmpty(notificationId)) {
                    pushMsg.setNotificationId(notificationId);
                }
                pushMsg.setChannelIdHW(messagePushConfig.getAndroidConfig().getChannelIdHW());
                pushMsg.setChannelIdMi(messagePushConfig.getAndroidConfig().getChannelIdMi());
                pushMsg.setChannelIdOPPO(messagePushConfig.getAndroidConfig().getChannelIdOPPO());
            }
            if (!TextUtils.isEmpty(messagePushConfig.getPushTitle())) {
                pushMsg.setPushTitle(messagePushConfig.getPushTitle());
            }
            if (!TextUtils.isEmpty(messagePushConfig.getPushData())) {
                pushMsg.setPushData(messagePushConfig.getPushData());
            }
        } else {
            if (!PushCacheHelper.getInstance().getPushContentShowStatus(mContext)) {
                pushMsg.setPushContent(mContext.getString(R.string.rc_receive_new_message));
                isShowDetail = false;
            } else {
                if (message.getContent() instanceof RecallNotificationMessage) {
                    if (message.getConversationType().equals(Conversation.ConversationType.PRIVATE)) {
                        content = (mContext.getString(R.string.rc_recalled_a_message)).trim();
                    }
                }
                pushMsg.setPushContent(content);
            }
        }

        if (message.getContent() instanceof RecallNotificationMessage) {
            pushMsg.setObjectName("RC:RcNtf");
        } else {
            pushMsg.setObjectName(message.getObjectName());
        }
        pushMsg.setShowDetail(isShowDetail);
        pushMsg.setConversationType(RongPushClient.ConversationType.setValue(message.getConversationType().getValue()));
        pushMsg.setTargetId(message.getTargetId());
        pushMsg.setTargetUserName(targetUserName);
        pushMsg.setSenderId(message.getSenderUserId());
        pushMsg.setSenderName(senderName);
        pushMsg.setPushFlag("false");
        pushMsg.setToId(RongIMClient.getInstance().getCurrentUserId());
        pushMsg.setSourceType(PushNotificationMessage.PushSourceType.LOCAL_MESSAGE);
        pushMsg.setPushId(message.getUId());
        return pushMsg;
    }
}
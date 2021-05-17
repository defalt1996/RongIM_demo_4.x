package io.rong.imkit.model;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.rong.imkit.R;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.mention.DraftHelper;
import io.rong.imkit.userInfoCache.RongUserInfoManager;
import io.rong.imkit.widget.provider.IContainerItemProvider;
import io.rong.imlib.MessageTag;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Discussion;
import io.rong.imlib.model.Group;
import io.rong.imlib.model.MentionedInfo;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.PublicServiceProfile;
import io.rong.imlib.model.UserInfo;
import io.rong.message.VoiceMessage;

public class UIConversation implements Parcelable {

    private String conversationTitle;
    private Uri portrait;
    private Spannable conversationContent;
    private MessageContent messageContent;
    private long conversationTime;
    private int unReadMessageCount;
    private boolean isTop;
    private Conversation.ConversationType conversationType;
    private Message.SentStatus sentStatus;
    private Message.ReceivedStatus receivedStatus;
    private String targetId;
    private String senderId;
    private boolean isGathered;
    private Map<String, Integer> gatheredConversations;
    private Conversation.ConversationNotificationStatus notificationBlockStatus;
    private String draft;
    private int latestMessageId;
    private boolean extraFlag;
    private boolean isMentioned;
    private UserInfo senderUserInfo;
    private String extra;
    private Context mContext;
    private long syncReadReceiptTime;

    public void setSyncReadReceiptTime(long syncReadReceiptTime) {
        this.syncReadReceiptTime = syncReadReceiptTime;
    }

    public long getSyncReadReceiptTime() {
        return syncReadReceiptTime;
    }

    public static UIConversation obtain(Context context, Message message, boolean isGathered) {
        UIConversation uiConversation = new UIConversation(context);
        Conversation.ConversationType conversationType = message.getConversationType();
        String targetId = message.getTargetId();

        String title;
        Uri portrait = null;
        if (isGathered) {
            title = RongContext.getInstance().getGatheredConversationTitle(context, conversationType);
        } else {
            title = RongContext.getInstance()
                    .getConversationTemplate(conversationType.getName())
                    .getTitle(targetId);
            portrait = RongContext.getInstance()
                    .getConversationTemplate(conversationType.getName())
                    .getPortraitUri(targetId);
        }
        ConversationKey key = ConversationKey.obtain(targetId, conversationType);
        Conversation.ConversationNotificationStatus notificationStatus = RongContext.getInstance().getConversationNotifyStatusFromCache(key);
        uiConversation.updateConversation(context, message, isGathered);
        uiConversation.conversationTitle = title;
        uiConversation.notificationBlockStatus = notificationStatus == null ? Conversation.ConversationNotificationStatus.NOTIFY : notificationStatus;
        uiConversation.portrait = portrait;

        if (conversationType.equals(Conversation.ConversationType.GROUP)) {
            GroupUserInfo groupUserInfo = RongUserInfoManager.getInstance().getGroupUserInfo(message.getTargetId(), message.getSenderUserId());
            UserInfo userInfo = RongUserInfoManager.getInstance().getUserInfo(message.getSenderUserId());
            if (groupUserInfo != null && userInfo != null) {
                uiConversation.senderUserInfo = new UserInfo(message.getSenderUserId(), groupUserInfo.getNickname(), userInfo.getPortraitUri());
                uiConversation.nicknameIds.add(message.getSenderUserId());
            } else {
                uiConversation.senderUserInfo = RongUserInfoManager.getInstance().getUserInfo(message.getSenderUserId());
            }
        } else if ((conversationType.equals(Conversation.ConversationType.APP_PUBLIC_SERVICE) ||
                conversationType.equals(Conversation.ConversationType.PUBLIC_SERVICE)) && targetId.equals(message.getSenderUserId())) {
            ConversationKey mKey = ConversationKey.obtain(targetId, conversationType);
            PublicServiceProfile publicServiceProfile = null;
            if (mKey != null) {
                publicServiceProfile = RongContext.getInstance().getPublicServiceInfoFromCache(mKey.getKey());
            }
            if (publicServiceProfile != null) {
                uiConversation.senderUserInfo = new UserInfo(targetId, publicServiceProfile.getName(), publicServiceProfile.getPortraitUri());
            }
        } else {
            uiConversation.senderUserInfo = RongUserInfoManager.getInstance().getUserInfo(message.getSenderUserId());
        }

        uiConversation.senderId = message.getSenderUserId();
        uiConversation.gatheredConversations.put(message.getConversationType().getName() + message.getTargetId(), 0);

        return uiConversation;
    }

    public void updateConversation(Context context, Message message, boolean isGathered) {
        if (context == null) {
            return;
        }
        MessageTag msgTag = message.getContent().getClass().getAnnotation(MessageTag.class);
        MentionedInfo mentionedInfo = message.getContent().getMentionedInfo();
        boolean mentioned = mentionedInfo != null
                && ((mentionedInfo.getType().equals(MentionedInfo.MentionedType.ALL) && !message.getSenderUserId().equals(RongIMClient.getInstance().getCurrentUserId()))
                || (mentionedInfo.getType().equals(MentionedInfo.MentionedType.PART) && mentionedInfo.getMentionedUserIdList() != null
                && mentionedInfo.getMentionedUserIdList().contains(RongIMClient.getInstance().getCurrentUserId())));

        this.conversationType = message.getConversationType();
        this.targetId = message.getTargetId();
        this.receivedStatus = message.getReceivedStatus();
        if (message.getSentTime() > syncReadReceiptTime) {
            this.sentStatus = message.getSentStatus();
        }
        this.conversationTime = message.getSentTime();
        this.isGathered = isGathered;
        this.messageContent = message.getContent();
        this.latestMessageId = message.getMessageId();
        this.isMentioned = !isGathered && (this.isMentioned || mentioned);
        boolean isCount = (msgTag != null && (msgTag.flag() & MessageTag.ISCOUNTED) == MessageTag.ISCOUNTED) && message.getMessageDirection().equals(Message.MessageDirection.RECEIVE);
        if (isCount && !message.getReceivedStatus().isRead()) {
            this.unReadMessageCount = unReadMessageCount + 1;
        }
        if (isGathered && isCount) {
            String key = conversationType.getName() + targetId;
            Set<String> set = gatheredConversations.keySet();
            if (set.contains(key)) {
                Integer count = gatheredConversations.get(key);
                if (count != null) {
                    gatheredConversations.put(key, count + 1);
                }
            } else {
                gatheredConversations.put(key, 1);
            }
        }
        if (!message.getSenderUserId().equals(this.senderId)) {
            if (conversationType.equals(Conversation.ConversationType.GROUP)) {
                GroupUserInfo groupUserInfo = RongUserInfoManager.getInstance().getGroupUserInfo(message.getTargetId(), message.getSenderUserId());
                UserInfo userInfo = RongUserInfoManager.getInstance().getUserInfo(message.getSenderUserId());
                if (groupUserInfo != null && userInfo != null) {
                    this.senderUserInfo = new UserInfo(message.getSenderUserId(), groupUserInfo.getNickname(), userInfo.getPortraitUri());
                    this.nicknameIds.add(message.getSenderUserId());
                } else {
                    this.senderUserInfo = RongUserInfoManager.getInstance().getUserInfo(message.getSenderUserId());
                }
            } else if ((conversationType.equals(Conversation.ConversationType.APP_PUBLIC_SERVICE) ||
                    conversationType.equals(Conversation.ConversationType.PUBLIC_SERVICE)) && targetId.equals(message.getSenderUserId())) {
                ConversationKey mKey = ConversationKey.obtain(targetId, conversationType);
                PublicServiceProfile publicServiceProfile = null;
                if (mKey != null) {
                    publicServiceProfile = RongContext.getInstance().getPublicServiceInfoFromCache(mKey.getKey());
                }
                if (publicServiceProfile != null) {
                    this.senderUserInfo = new UserInfo(targetId, publicServiceProfile.getName(), publicServiceProfile.getPortraitUri());
                }
            } else {
                this.senderUserInfo = RongUserInfoManager.getInstance().getUserInfo(message.getSenderUserId());
            }
            this.senderId = message.getSenderUserId();
        }
        this.buildContent(context, senderUserInfo);
    }

    public static UIConversation obtain(Context context, Conversation conversation, boolean isGathered) {
        UIConversation uiConversation = new UIConversation(context);
        uiConversation.gatheredConversations.put(conversation.getConversationType().getName() + conversation.getTargetId(), 0);
        uiConversation.updateConversation(context, conversation, isGathered);
        return uiConversation;
    }

    public void updateConversation(Context context, Conversation conversation, boolean isGathered) {
        if (context == null) {
            return;
        }
        String title;
        String portrait = null;
        if (conversation.getSentTime() >= conversationTime) {
            if (isGathered) {
                title = RongContext.getInstance().getGatheredConversationTitle(context, conversation.getConversationType());
            } else {
                title = conversationTitle == null ? conversation.getConversationTitle() : conversationTitle;
                if (TextUtils.isEmpty(title)) {
                    title = RongContext
                            .getInstance()
                            .getConversationTemplate(conversation.getConversationType().getName())
                            .getTitle(conversation.getTargetId());
                }
                portrait = this.portrait != null ? this.portrait.toString() : filterConversationPortrait(conversation.getPortraitUrl());
                if (TextUtils.isEmpty(portrait)) {
                    Uri url = RongContext
                            .getInstance()
                            .getConversationTemplate(conversation.getConversationType().getName())
                            .getPortraitUri(conversation.getTargetId());
                    portrait = url != null ? url.toString() : null;
                }
            }

            this.conversationType = conversation.getConversationType();
            this.targetId = conversation.getTargetId();
            this.conversationTitle = title;
            this.portrait = portrait != null ? Uri.parse(portrait) : null;
            this.receivedStatus = conversation.getReceivedStatus();
            this.sentStatus = conversation.getSentStatus();
            this.conversationTime = conversation.getSentTime();
            this.notificationBlockStatus = conversation.getNotificationStatus() == null ? Conversation.ConversationNotificationStatus.NOTIFY : conversation.getNotificationStatus();
            this.draft = isGathered ? null : DraftHelper.getDraftContent(conversation.getDraft());
            this.isGathered = isGathered;
            this.isTop = !isGathered && conversation.isTop();
            this.messageContent = conversation.getLatestMessage();
            this.latestMessageId = conversation.getLatestMessageId();
            this.senderId = conversation.getSenderUserId();
            this.isMentioned = !isGathered && conversation.getMentionedCount() > 0;

            if (conversationType.equals(Conversation.ConversationType.GROUP)) {
                if (latestMessageId >= 0) {
                    GroupUserInfo groupUserInfo = RongUserInfoManager.getInstance().getGroupUserInfo(targetId, senderId);
                    UserInfo userInfo = RongUserInfoManager.getInstance().getUserInfo(senderId);
                    if (groupUserInfo != null && userInfo != null) {
                        this.senderUserInfo = new UserInfo(senderId, groupUserInfo.getNickname(), userInfo.getPortraitUri());
                        this.nicknameIds.add(senderId);
                    } else {
                        this.senderUserInfo = RongUserInfoManager.getInstance().getUserInfo(senderId);
                    }
                }
            } else if ((conversationType.equals(Conversation.ConversationType.APP_PUBLIC_SERVICE) ||
                    conversationType.equals(Conversation.ConversationType.PUBLIC_SERVICE)) && targetId.equals(senderId)) {
                ConversationKey mKey = ConversationKey.obtain(targetId, conversationType);
                PublicServiceProfile publicServiceProfile = null;
                if (mKey != null) {
                    publicServiceProfile = RongContext.getInstance().getPublicServiceInfoFromCache(mKey.getKey());
                }
                if (publicServiceProfile != null) {
                    this.senderUserInfo = new UserInfo(targetId, publicServiceProfile.getName(), publicServiceProfile.getPortraitUri());
                }
            } else {
                this.senderUserInfo = RongUserInfoManager.getInstance().getUserInfo(senderId);
            }

            this.buildContent(context, senderUserInfo);
        }
        if (isGathered) {
            String gatheredKey = conversation.getConversationType().getName() + conversation.getTargetId();
            this.gatheredConversations.put(gatheredKey, conversation.getUnreadMessageCount());
            int newUnReadMessageCount = 0;
            Collection<Integer> collection = gatheredConversations.values();
            for (int count : collection) {
                newUnReadMessageCount += count;
            }
            unReadMessageCount = newUnReadMessageCount;
        } else {
            this.unReadMessageCount = conversation.getUnreadMessageCount();
        }
    }

    private void buildContent(Context context, UserInfo userInfo) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        if (messageContent == null) {
            builder.append("");
        } else {
            ProviderTag providerTag = RongContext.getInstance().getMessageProviderTag(messageContent.getClass());
            IContainerItemProvider.MessageProvider messageProvider = RongContext.getInstance().getMessageTemplate(messageContent.getClass());
            if (providerTag == null || messageProvider == null) {
                builder.append("");
            } else {
                Spannable summary = null;
                if (mContext != null) {
                    summary = messageProvider.getContentSummary(mContext, messageContent);
                    if (summary == null) {
                        Message message = Message.obtain(targetId, conversationType, messageContent);
                        UIMessage uiMessage = UIMessage.obtain(message);
                        summary = messageProvider.getSummary(uiMessage);
                    }
                }
                boolean isShowName = providerTag.showSummaryWithName();
                String curUserId = RongIM.getInstance().getCurrentUserId();
                if (summary == null) {
                    builder.append("");
                } else {
                    if (messageContent instanceof VoiceMessage) {
                        boolean isListened = receivedStatus.isListened();
                        if (senderId.equals(curUserId) || isListened) {
                            summary.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.rc_text_color_secondary)), 0, summary.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        } else {
                            summary.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.rc_voice_color)), 0, summary.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }
                    if (isGathered) {
                        String name = RongContext
                                .getInstance()
                                .getConversationTemplate(conversationType.getName())
                                .getTitle(targetId);
                        builder.append(String.format("%s: ", name)).append(summary);
                    } else {
                        if (!TextUtils.isEmpty(draft) && !isMentioned) {
                            builder.append(draft);
                        } else if (senderId.equals(curUserId)) {
                            builder.append(summary);
                        } else {
                            String name = userInfo != null ? userInfo.getName() : senderId;
                            if ((conversationType.equals(Conversation.ConversationType.GROUP)
                                    || conversationType.equals(Conversation.ConversationType.DISCUSSION))
                                    && isShowName) {
                                if (conversationType.equals(Conversation.ConversationType.GROUP)) {
                                    GroupUserInfo groupUserInfo = RongUserInfoManager.getInstance().getGroupUserInfo(targetId, senderId);
                                    if (groupUserInfo != null && !TextUtils.isEmpty(groupUserInfo.getNickname())) {
                                        name = groupUserInfo.getNickname();
                                    }
                                }
                                builder.append(String.format("%s: ", name)).append(summary);
                            } else {
                                builder.append(summary);
                            }
                        }
                    }
                }
            }
        }
        conversationContent = builder;
    }

    private String filterConversationPortrait(String portrait) {
        if (!TextUtils.isEmpty(portrait)) {
            if (portrait.toLowerCase().startsWith("file")) {
                File file = new File(portrait.substring(7));
                if (file.exists()) {
                    return portrait;
                }
            } else if (portrait.toLowerCase().startsWith("http")) {
                return portrait;
            }
        }
        return "";
    }

    public void clearLastMessage() {
        messageContent = null;
        latestMessageId = 0;
        conversationContent = null;
        unReadMessageCount = 0;
        isMentioned = false;
        sentStatus = Message.SentStatus.DESTROYED;
    }

    public void clearUnRead(Conversation.ConversationType conversationType, String targetId) {
        if (isGathered) {
            String key = conversationType.getName() + targetId;
            gatheredConversations.put(key, 0);
            unReadMessageCount = 0;
            Collection<Integer> collection = gatheredConversations.values();
            for (int count : collection) {
                unReadMessageCount += count;
            }
        } else {
            unReadMessageCount = 0;
            isMentioned = false;
        }
    }

    /**
     * 根据用户信息，分别更新
     *
     * @param userInfo 用户信息
     */
    public void updateConversation(Context context, UserInfo userInfo) {
        if (context == null) {
            return;
        }
        if (isGathered) {
            conversationTitle = RongContext.getInstance().getGatheredConversationTitle(context, conversationType);
            buildContent(context, userInfo);
        } else {
            if (conversationType.equals(Conversation.ConversationType.GROUP)
                    || conversationType.equals(Conversation.ConversationType.DISCUSSION)) {
                if (senderId != null && userInfo.getUserId().equals(senderId)) {
                    senderUserInfo = userInfo;
                    buildContent(context, userInfo);
                }
            } else if (conversationType == Conversation.ConversationType.ENCRYPTED) {
                if (targetId.endsWith(userInfo.getUserId())) {
                    conversationTitle = userInfo.getName();
                    buildContent(context, userInfo);
                }
            } else {
                if (userInfo.getUserId().equals(targetId)) {
                    conversationTitle = userInfo.getName();
                    portrait = userInfo.getPortraitUri();
                    buildContent(context, userInfo);
                }
            }
        }
    }

    public void updateConversation(Context context, Group group) {
        if (context == null) {
            return;
        }
        if (isGathered) {
            conversationTitle = RongContext.getInstance().getGatheredConversationTitle(context, conversationType);
            buildContent(context, senderUserInfo);
        } else {
            if (conversationType.equals(Conversation.ConversationType.GROUP)
                    && group.getId().equals(targetId)) {
                conversationTitle = group.getName();
                portrait = group.getPortraitUri();
            }
        }
    }

    public void updateConversation(Context context, Discussion discussion) {
        if (context == null) {
            return;
        }
        if (isGathered) {
            conversationTitle = RongContext.getInstance().getGatheredConversationTitle(context, conversationType);
            buildContent(context, senderUserInfo);
        } else {
            if (conversationType.equals(Conversation.ConversationType.DISCUSSION)
                    && discussion.getId().equals(targetId)) {
                conversationTitle = discussion.getName();
            }
        }
    }

    public void updateConversation(Context context, GroupUserInfo groupUserInfo) {
        if (context == null) {
            return;
        }
        UserInfo userInfo = new UserInfo(groupUserInfo.getUserId(), groupUserInfo.getNickname(), portrait);
        addNickname(groupUserInfo.getUserId());
        senderUserInfo = new UserInfo(groupUserInfo.getUserId(), groupUserInfo.getNickname(), null);
        buildContent(context, userInfo);
    }

    public boolean getExtraFlag() {
        return extraFlag;
    }

    public void setExtraFlag(boolean extraFlag) {
        this.extraFlag = extraFlag;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    private Set<String> nicknameIds;

    private UIConversation(Context context) {
        this.mContext = context;
        nicknameIds = new HashSet<>();
        gatheredConversations = new HashMap<>();
    }

    private UIConversation() {
        nicknameIds = new HashSet<>();
        gatheredConversations = new HashMap<>();
    }


    public void setUIConversationTitle(String title) {
        conversationTitle = title;
    }

    public String getUIConversationTitle() {
        return conversationTitle;
    }

    public void setIconUrl(Uri iconUrl) {
        portrait = iconUrl;
    }

    public Uri getIconUrl() {
        return portrait;
    }

    public void setConversationContent(Spannable content) {
        conversationContent = content;
    }

    public Spannable getConversationContent() {
        return conversationContent;
    }

    public void setMessageContent(MessageContent content) {
        messageContent = content;
    }

    public MessageContent getMessageContent() {
        return messageContent;
    }

    public void setUIConversationTime(long time) {
        conversationTime = time;
    }

    public long getUIConversationTime() {
        return conversationTime;
    }

    public void setUnReadMessageCount(int count) {
        unReadMessageCount = count;
    }

    public int getUnReadMessageCount() {
        return unReadMessageCount;
    }

    public void setTop(boolean value) {
        isTop = value;
    }

    public boolean isTop() {
        return isTop;
    }

    public void setConversationType(Conversation.ConversationType type) {
        conversationType = type;
    }

    public Conversation.ConversationType getConversationType() {
        return conversationType;
    }

    public void setSentStatus(Message.SentStatus status) {
        sentStatus = status;
    }

    public Message.SentStatus getSentStatus() {
        return sentStatus;
    }

    public void setConversationTargetId(String id) {
        targetId = id;
    }

    public String getConversationTargetId() {
        return targetId;
    }

    public void setConversationSenderId(String id) {
        senderId = id;
    }

    public String getConversationSenderId() {
        return senderId;
    }

    public void setConversationGatherState(boolean state) {
        isGathered = state;
    }

    public boolean getConversationGatherState() {
        return isGathered;
    }

    public void setNotificationStatus(Conversation.ConversationNotificationStatus status) {
        notificationBlockStatus = status;
    }

    public Conversation.ConversationNotificationStatus getNotificationStatus() {
        return notificationBlockStatus;
    }

    public void setDraft(String content) {
        draft = content;
    }

    public String getDraft() {
        return draft;
    }

    public void setLatestMessageId(int id) {
        this.latestMessageId = id;
    }

    public int getLatestMessageId() {
        return latestMessageId;
    }

    public void addNickname(String userId) {
        nicknameIds.add(userId);
    }

    public void removeNickName(String userId) {
        nicknameIds.remove(userId);
    }

    public boolean hasNickname(String userId) {
        return nicknameIds.contains(userId);
    }

    public void setMentionedFlag(boolean flag) {
        this.isMentioned = flag;
    }

    public boolean getMentionedFlag() {
        return isMentioned;
    }

    private UnreadRemindType mUnreadType = UnreadRemindType.REMIND_WITH_COUNTING;

    public void setUnreadType(UnreadRemindType type) {
        this.mUnreadType = type;
    }

    public UnreadRemindType getUnReadType() {
        return mUnreadType;
    }

    public enum UnreadRemindType {
        /**
         * 无未读提示
         */
        NO_REMIND,
        /**
         * 提示，但无计数
         */
        REMIND_ONLY,
        /**
         * 带计数的提示
         */
        REMIND_WITH_COUNTING
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }

    public static final Creator<UIConversation> CREATOR = new Creator<UIConversation>() {

        @Override
        public UIConversation createFromParcel(Parcel source) {
            return new UIConversation();
        }

        @Override
        public UIConversation[] newArray(int size) {
            return new UIConversation[size];
        }
    };
}

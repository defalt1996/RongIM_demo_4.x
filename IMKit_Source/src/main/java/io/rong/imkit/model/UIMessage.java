package io.rong.imkit.model;

import android.text.SpannableStringBuilder;
import android.text.style.URLSpan;
import android.text.util.Linkify;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import io.rong.imkit.emoticon.AndroidEmoji;
import io.rong.imkit.widget.AutoLinkTextView;
import io.rong.imlib.CustomServiceConfig;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.ReadReceiptInfo;
import io.rong.imlib.model.UserInfo;
import io.rong.message.ReferenceMessage;
import io.rong.message.TextMessage;

public class UIMessage {

    /**
     * TextMessage 和 ReferenceMessage 的 content 字段
     */
    private SpannableStringBuilder contentSpannable;
    /**
     * ReferenceMessage 的 referMsg 为 TextMessage 时 的 content 字段
     */
    private SpannableStringBuilder referenceContentSpannable;
    private UserInfo mUserInfo;
    private int mProgress;
    private boolean evaluated = false;
    private boolean isHistoryMessage = true;
    private Message mMessage;
    private boolean mNickName;
    private boolean isListening;
    public boolean continuePlayAudio;
    private CustomServiceConfig csConfig;
    private boolean isChecked;
    //显示阅后即焚的剩余时间;
    private String unDestructTime;

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public boolean isListening() {
        return isListening;
    }

    public void setListening(boolean listening) {
        isListening = listening;
    }

    public boolean isNickName() {
        return mNickName;
    }

    public void setNickName(boolean nickName) {
        this.mNickName = nickName;
    }

    public Message getMessage() {
        return mMessage;
    }

    public void setMessage(Message message) {
        mMessage = message;
    }

    public void setReceivedStatus(Message.ReceivedStatus receivedStatus) {
        mMessage.setReceivedStatus(receivedStatus);
    }

    public void setSentStatus(Message.SentStatus sentStatus) {
        mMessage.setSentStatus(sentStatus);
    }

    public void setReceivedTime(long receivedTime) {
        mMessage.setReceivedTime(receivedTime);
    }

    public void setSentTime(long sentTime) {
        mMessage.setSentTime(sentTime);
    }

    public void setContent(MessageContent content) {
        mMessage.setContent(content);
    }

    public void setExtra(String extra) {
        mMessage.setExtra(extra);
    }

    public void setSenderUserId(String senderUserId) {
        mMessage.setSenderUserId(senderUserId);
    }

    public void setCsConfig(CustomServiceConfig csConfig) {
        this.csConfig = csConfig;
    }


    public String getUId() {
        return mMessage.getUId();
    }

    public Conversation.ConversationType getConversationType() {
        return mMessage.getConversationType();
    }

    public String getTargetId() {
        return mMessage.getTargetId();
    }

    public int getMessageId() {
        return mMessage.getMessageId();
    }

    public Message.MessageDirection getMessageDirection() {
        return mMessage.getMessageDirection();
    }

    public String getSenderUserId() {
        return mMessage.getSenderUserId();
    }

    public Message.ReceivedStatus getReceivedStatus() {
        return mMessage.getReceivedStatus();
    }

    public Message.SentStatus getSentStatus() {
        return mMessage.getSentStatus();
    }

    public long getReceivedTime() {
        return mMessage.getReceivedTime();
    }

    public long getSentTime() {
        return mMessage.getSentTime();
    }

    public String getObjectName() {
        return mMessage.getObjectName();
    }

    public MessageContent getContent() {
        return mMessage.getContent();
    }

    public String getExtra() {
        return mMessage.getExtra();
    }

    public CustomServiceConfig getCsConfig() {
        return csConfig;
    }

    public static UIMessage obtain(Message message) {
        UIMessage uiMessage = new UIMessage();
        uiMessage.mMessage = message;
        uiMessage.continuePlayAudio = false;
        return uiMessage;
    }


    public SpannableStringBuilder getContentSpannable() {
        return contentSpannable;
    }

    public SpannableStringBuilder getReferenceContentSpannable() {
        return referenceContentSpannable;
    }



    public ReadReceiptInfo getReadReceiptInfo() {
        return mMessage.getReadReceiptInfo();
    }

    public void setReadReceiptInfo(ReadReceiptInfo info) {
        mMessage.setReadReceiptInfo(info);
    }

    public void setContentSpannable(SpannableStringBuilder contentSpannable) {
        this.contentSpannable = contentSpannable;
    }

    public UserInfo getUserInfo() {
        return mUserInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        if (userInfo != null && mMessage != null && mMessage.getSenderUserId() != null) {
            if (mMessage.getSenderUserId().equals(userInfo.getUserId())) {
                mUserInfo = userInfo;
            }
        } else {
            mUserInfo = userInfo;
        }
    }

    public void setProgress(int progress) {
        mProgress = progress;
    }

    public int getProgress() {
        return mProgress;
    }

    public void setEvaluated(boolean evaluated) {
        this.evaluated = evaluated;
    }

    public boolean getEvaluated() {
        return evaluated;
    }

    public void setIsHistoryMessage(boolean isHistoryMessage) {
        this.isHistoryMessage = isHistoryMessage;
    }

    public boolean getIsHistoryMessage() {
        return isHistoryMessage;
    }

    public String getUnDestructTime() {
        return unDestructTime;
    }

    public void setUnDestructTime(String pUnDestructTime) {
        unDestructTime = pUnDestructTime;
    }




    public void setReferenceContentSpannable(SpannableStringBuilder referenceContentSpannable) {
        this.referenceContentSpannable = referenceContentSpannable;
    }

}

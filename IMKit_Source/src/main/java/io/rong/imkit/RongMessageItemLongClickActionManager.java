package io.rong.imkit;

import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.rong.common.RLog;
import io.rong.imkit.manager.AudioPlayManager;
import io.rong.imkit.model.UIMessage;
import io.rong.imkit.userInfoCache.RongUserInfoManager;
import io.rong.imkit.utilities.OptionsPopupDialog;
import io.rong.imkit.widget.provider.MessageItemLongClickAction;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.location.message.RealTimeLocationStartMessage;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.UnknownMessage;
import io.rong.imlib.model.UserInfo;
import io.rong.message.HandshakeMessage;
import io.rong.message.NotificationMessage;
import io.rong.message.PublicServiceMultiRichContentMessage;
import io.rong.message.PublicServiceRichContentMessage;
import io.rong.message.RecallNotificationMessage;
import io.rong.message.ReferenceMessage;
import io.rong.message.TextMessage;
import io.rong.message.VoiceMessage;

import static android.widget.Toast.makeText;

/**
 * Created by jiangecho on 2017/3/17.
 */

public class RongMessageItemLongClickActionManager {

    private static final String TAG = RongMessageItemLongClickActionManager.class.getSimpleName();

    private OptionsPopupDialog mDialog;
    private Message mLongClickMessage;

    private static class Holder {
        static RongMessageItemLongClickActionManager instance = new RongMessageItemLongClickActionManager();
    }

    public static RongMessageItemLongClickActionManager getInstance() {
        return Holder.instance;
    }

    private RongMessageItemLongClickActionManager() {
        messageItemLongClickActions = new ArrayList<>();
    }

    private List<MessageItemLongClickAction> messageItemLongClickActions;

    public void init() {
        messageItemLongClickActions.clear();
        initCommonMessageItemLongClickActions();
    }

    public void setLongClickMessage(Message message) {
        mLongClickMessage = message;
    }

    public Message getLongClickMessage() {
        return mLongClickMessage;
    }

    public void setLongClickDialog(OptionsPopupDialog dialog) {
        mDialog = dialog;
    }

    public OptionsPopupDialog getLongClickDialog() {
        return mDialog;
    }

    public List<MessageItemLongClickAction> getMessageItemLongClickActions() {
        return messageItemLongClickActions;
    }

    public void addMessageItemLongClickAction(MessageItemLongClickAction action) {
        addMessageItemLongClickAction(action, -1);
    }

    public void addMessageItemLongClickAction(MessageItemLongClickAction action, int index) {
        if (messageItemLongClickActions.contains(action)) {
            messageItemLongClickActions.remove(action);
        }
        if (index < 0) {
            messageItemLongClickActions.add(action);
        } else {
            messageItemLongClickActions.add(index, action);
        }
    }

    public void removeMessageItemLongClickAction(MessageItemLongClickAction action) {
        messageItemLongClickActions.remove(action);
    }

    /**
     * 本方法应当只能被ItemProvider调用, 如果想修改默认的长按弹出菜单，请调用getMessageItemLongClickActions()
     *
     * @param uiMessage
     * @return
     */
    public List<MessageItemLongClickAction> getMessageItemLongClickActions(UIMessage uiMessage) {
        List<MessageItemLongClickAction> actions = new ArrayList<>();
        for (MessageItemLongClickAction action : messageItemLongClickActions) {
            if (action.filter(uiMessage)) {
                actions.add(action);
            }
        }
        Collections.sort(actions, new Comparator<MessageItemLongClickAction>() {
            @Override
            public int compare(MessageItemLongClickAction t1, MessageItemLongClickAction t2) {
                if (t1.priority > t2.priority) {
                    return 1;
                }
                if (t1.priority == t2.priority) {
                    return 0;
                }
                return -1;
            }
        });
        return actions;
    }

    // T5051: 会话界面中，优化消息长按后的功能顺序为：复制、删除、撤回、引用、更多
    private void initCommonMessageItemLongClickActions() {
        MessageItemLongClickAction messageItemLongClickAction = new MessageItemLongClickAction.Builder()
                .titleResId(R.string.rc_dialog_item_message_copy)
                .actionListener(new MessageItemLongClickAction.MessageItemLongClickListener() {
                    @Override
                    public boolean onMessageItemLongClick(Context context, UIMessage message) {
                        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                        if (message.getContent() instanceof RecallNotificationMessage) {
                            return false;
                        } else {
                            if (message.getContent() instanceof TextMessage) {
                                if (clipboard != null) {
                                    clipboard.setText(((TextMessage) message.getContent()).getContent());
                                }
                            } else if (message.getContent() instanceof ReferenceMessage) {
                                ReferenceMessage referenceMessage = (ReferenceMessage) message.getContent();
                                if (referenceMessage == null) {
                                    return false;
                                }
                                if (clipboard != null) {
                                    clipboard.setText(referenceMessage.getEditSendText());
                                }
                            }
                            return true;
                        }
                    }
                })
                .showFilter(new MessageItemLongClickAction.Filter() {
                    @Override
                    public boolean filter(UIMessage message) {
                        return (message.getContent() instanceof TextMessage || message.getContent() instanceof ReferenceMessage) &&
                                !message.getConversationType().equals(Conversation.ConversationType.ENCRYPTED) && !message.getContent().isDestruct();
                    }
                })
                .build();
        addMessageItemLongClickAction(messageItemLongClickAction);

        messageItemLongClickAction = new MessageItemLongClickAction.Builder()
                .titleResId(R.string.rc_dialog_item_message_delete)
                .actionListener(new MessageItemLongClickAction.MessageItemLongClickListener() {
                    @Override
                    public boolean onMessageItemLongClick(Context context, UIMessage message) {
                        if (message.getMessage().getContent() instanceof VoiceMessage) {
                            Uri uri = ((VoiceMessage) message.getMessage().getContent()).getUri();
                            Uri playingUri = AudioPlayManager.getInstance().getPlayingUri();
                            if (playingUri != null && playingUri == uri) {
                                AudioPlayManager.getInstance().stopPlay();
                            }
                        }

                        RongIM.getInstance().deleteMessages(new int[]{message.getMessageId()}, null);
                        return true;
                    }
                }).build();
        addMessageItemLongClickAction(messageItemLongClickAction);

        messageItemLongClickAction = new MessageItemLongClickAction.Builder()
                .titleResId(R.string.rc_dialog_item_message_recall)
                .actionListener(new MessageItemLongClickAction.MessageItemLongClickListener() {
                    @Override
                    public boolean onMessageItemLongClick(Context context, UIMessage message) {
                        if (RongIM.getInstance().getCurrentConnectionStatus() == RongIMClient.ConnectionStatusListener.ConnectionStatus.NETWORK_UNAVAILABLE) {
                            makeText(context, context.getResources().getString(R.string.rc_recall_failed_for_network_unavailable), Toast.LENGTH_SHORT).show();
                            return true;
                        }
                        long deltaTime = RongIM.getInstance().getDeltaTime();
                        long normalTime = System.currentTimeMillis() - deltaTime;
                        int messageRecallInterval = -1;
                        boolean needRecall = false;
                        try {
                            messageRecallInterval = context.getResources().getInteger(R.integer.rc_message_recall_interval);
                        } catch (Resources.NotFoundException e) {
                            RLog.e(TAG, "rc_message_recall_interval not configure in rc_config.xml");
                            e.printStackTrace();
                        }
                        needRecall = (normalTime - message.getSentTime()) <= messageRecallInterval * 1000;
                        if (needRecall) {
                            RongIM.getInstance().recallMessage(message.getMessage(), null);
                        } else {
                            new AlertDialog.Builder(context)
                                    .setMessage(R.string.rc_recall_overtime)
                                    .setPositiveButton(R.string.rc_confirm, null)
                                    .setCancelable(false)
                                    .create()
                                    .show();
                            RLog.e(TAG, "撤回消息失败");
                        }
                        return true;
                    }
                })
                .showFilter(new MessageItemLongClickAction.Filter() {
                    @Override
                    public boolean filter(UIMessage message) {
                        if (message.getContent() instanceof NotificationMessage
                                || message.getContent() instanceof HandshakeMessage
                                || message.getContent() instanceof PublicServiceRichContentMessage
                                || message.getContent() instanceof RealTimeLocationStartMessage
                                || message.getContent() instanceof UnknownMessage
                                || message.getContent() instanceof PublicServiceMultiRichContentMessage
                                || message.getSentStatus().equals(Message.SentStatus.CANCELED) ||
                                message.getConversationType().equals(Conversation.ConversationType.ENCRYPTED)) {
                            return false;
                        }

                        long deltaTime = RongIM.getInstance().getDeltaTime();
                        long normalTime = System.currentTimeMillis() - deltaTime;
                        boolean enableMessageRecall = false;
                        int messageRecallInterval = -1;
                        boolean hasSent = (!message.getSentStatus().equals(Message.SentStatus.SENDING)) && (!message.getSentStatus().equals(Message.SentStatus.FAILED));

                        try {
                            enableMessageRecall = RongIM.getInstance().getApplicationContext().getResources().getBoolean(R.bool.rc_enable_message_recall);
                            messageRecallInterval = RongIM.getInstance().getApplicationContext().getResources().getInteger(R.integer.rc_message_recall_interval);
                        } catch (Exception e) {
                            RLog.e(TAG, "rc_message_recall_interval not configure in rc_config.xml");
                            e.printStackTrace();
                        }
                        return hasSent
                                && enableMessageRecall
                                && (normalTime - message.getSentTime()) <= messageRecallInterval * 1000
                                && message.getSenderUserId().equals(RongIM.getInstance().getCurrentUserId())
                                && !message.getConversationType().equals(Conversation.ConversationType.CUSTOMER_SERVICE)
                                && !message.getConversationType().equals(Conversation.ConversationType.APP_PUBLIC_SERVICE)
                                && !message.getConversationType().equals(Conversation.ConversationType.PUBLIC_SERVICE)
                                && !message.getConversationType().equals(Conversation.ConversationType.SYSTEM)
                                && !message.getConversationType().equals(Conversation.ConversationType.CHATROOM);
                    }
                })
                .build();
        addMessageItemLongClickAction(messageItemLongClickAction);

    }

    // 这个方法不应当在这
    private String getPushContent(Context context, UIMessage message) {
        String userName = "";
        UserInfo userInfo = RongUserInfoManager.getInstance().getUserInfo(message.getSenderUserId());
        if (userInfo != null) {
            userName = userInfo.getName();
        }
        return context.getString(R.string.rc_user_recalled_message, userName);
    }
}

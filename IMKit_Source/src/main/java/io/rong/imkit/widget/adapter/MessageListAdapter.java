package io.rong.imkit.widget.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import io.rong.common.RLog;
import io.rong.eventbus.EventBus;
import io.rong.imkit.R;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.destruct.DestructManager;
import io.rong.imkit.dialog.BurnHintDialog;
import io.rong.imkit.mention.RongMentionManager;
import io.rong.imkit.model.ConversationKey;
import io.rong.imkit.model.Event;
import io.rong.imkit.model.GroupUserInfo;
import io.rong.imkit.model.ProviderTag;
import io.rong.imkit.model.UIMessage;
import io.rong.imkit.resend.ResendManager;
import io.rong.imkit.userInfoCache.RongUserInfoManager;
import io.rong.imkit.utilities.RongUtils;
import io.rong.imkit.utils.RongDateUtils;
import io.rong.imkit.widget.AsyncImageView;
import io.rong.imkit.widget.DebouncedOnClickListener;
import io.rong.imkit.widget.ProviderContainerView;
import io.rong.imkit.widget.provider.IContainerItemProvider;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.destruct.DestructionTaskManager;
import io.rong.imlib.location.message.RealTimeLocationJoinMessage;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.PublicServiceProfile;
import io.rong.imlib.model.ReadReceiptInfo;
import io.rong.imlib.model.UnknownMessage;
import io.rong.imlib.model.UserInfo;
import io.rong.message.GIFMessage;
import io.rong.message.GroupNotificationMessage;
import io.rong.message.HistoryDividerMessage;
import io.rong.message.ImageMessage;
import io.rong.message.InformationNotificationMessage;
import io.rong.message.RecallNotificationMessage;
import io.rong.message.SightMessage;
import io.rong.message.TextMessage;


public class MessageListAdapter extends BaseAdapter<UIMessage> {
    private static final String TAG = "MessageListAdapter";
    private static long readReceiptRequestInterval = 120;
    private LayoutInflater mInflater;
    private Context mContext;
    private OnItemHandlerListener mOnItemHandlerListener;
    boolean evaForRobot = false;
    boolean robotMode = true;
    protected boolean timeGone = false;

    private boolean isShowCheckbox; //是否处于多选状态
    private int maxMessageSelectedCount = -1;
    private OnMessageCheckedChanged messageCheckedChanged;
    private OnSelectedCountDidExceed selectedCountDidExceed;

    protected class ViewHolder {
        public AsyncImageView leftIconView;
        public AsyncImageView rightIconView;
        public TextView nameView;
        public ProviderContainerView contentView;
        public ProgressBar progressBar;
        public ImageView warning;
        public TextView readReceipt;
        public TextView readReceiptRequest;
        public TextView readReceiptStatus;
        public ViewGroup layout;
        public TextView time;
        public TextView sentStatus;
        public RelativeLayout layoutItem;
        public CheckBox message_check;
        public LinearLayout checkboxLayout;

    }

    public MessageListAdapter(Context context) {
        super();
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        try {
            if (RongContext.getInstance() != null) {
                readReceiptRequestInterval = context.getResources().getInteger(R.integer.rc_read_receipt_request_interval);
            } else {
                RLog.e(TAG, "SDK isn't init, use default readReceiptRequestInterval. Please refer to http://support.rongcloud.cn/kb/Mjc2 about how to init.");
            }
        } catch (Resources.NotFoundException e) {
            RLog.e(TAG, "MessageListAdapter rc_read_receipt_request_interval not configure in rc_config.xml", e);
        }
    }

    public void setMaxMessageSelectedCount(int maxMessageSelectedCount) {
        this.maxMessageSelectedCount = maxMessageSelectedCount;
    }

    public void setSelectedCountDidExceed(OnSelectedCountDidExceed selectedCountDidExceed) {
        this.selectedCountDidExceed = selectedCountDidExceed;
    }

    public void setOnItemHandlerListener(OnItemHandlerListener onItemHandlerListener) {
        this.mOnItemHandlerListener = onItemHandlerListener;
    }

    protected OnItemHandlerListener getItemHandlerListener() {
        return this.mOnItemHandlerListener;
    }

    public boolean isShowCheckbox() {
        return isShowCheckbox;
    }

    public void setShowCheckbox(boolean showCheckbox) {
        isShowCheckbox = showCheckbox;
    }

    public void setMessageCheckedChanged(OnMessageCheckedChanged messageCheckedChanged) {
        this.messageCheckedChanged = messageCheckedChanged;
    }

    public interface OnSelectedCountDidExceed {
        void onSelectedCountDidExceed();
    }

    public interface OnMessageCheckedChanged {
        void onCheckedEnable(boolean enable);
    }

    public interface OnItemHandlerListener {
        boolean onWarningViewClick(int position, Message data, View v);

        void onReadReceiptStateClick(Message message);

        void onMessageClick(int position, Message data, View v);
    }

    @Override
    public long getItemId(int position) {
        UIMessage message = getItem(position);
        if (message == null)
            return -1;
        return message.getMessageId();
    }

    public int getPositionBySendTime(long sentTime) {
        for (int i = 0; i < getCount(); i++) {
            UIMessage message = getItem(i);
            if (message.getSentTime() > sentTime) {
                return i;
            }
        }
        return getCount();
    }

    @Override
    protected View newView(final Context context, final int position, ViewGroup group) {
        View result = mInflater.inflate(R.layout.rc_item_message, null);

        final ViewHolder holder = new ViewHolder();
        holder.leftIconView = findViewById(result, R.id.rc_left);
        holder.rightIconView = findViewById(result, R.id.rc_right);
        holder.nameView = findViewById(result, R.id.rc_title);
        holder.contentView = findViewById(result, R.id.rc_content);
        holder.layout = findViewById(result, R.id.rc_layout);
        holder.progressBar = findViewById(result, R.id.rc_progress);
        holder.warning = findViewById(result, R.id.rc_warning);
        holder.readReceipt = findViewById(result, R.id.rc_read_receipt);
        holder.readReceiptRequest = findViewById(result, R.id.rc_read_receipt_request);
        holder.readReceiptStatus = findViewById(result, R.id.rc_read_receipt_status);
        holder.message_check = findViewById(result, R.id.message_check);
        holder.checkboxLayout = findViewById(result, R.id.ll_message_check);

        holder.time = findViewById(result, R.id.rc_time);
        holder.sentStatus = findViewById(result, R.id.rc_sent_status);
        holder.layoutItem = findViewById(result, R.id.rc_layout_item_message);
        timeGone = holder.time.getVisibility() == View.GONE;
        result.setTag(holder);
        return result;
    }

    protected boolean getNeedEvaluate(UIMessage data) {
        String extra;
        String robotEva = "";
        String sid = "";
        if (data != null && data.getConversationType() != null && data.getConversationType().equals(Conversation.ConversationType.CUSTOMER_SERVICE)) {
            if (data.getContent() instanceof TextMessage) {
                extra = ((TextMessage) data.getContent()).getExtra();
                if (TextUtils.isEmpty(extra))
                    return false;
                try {
                    JSONObject jsonObj = new JSONObject(extra);
                    robotEva = jsonObj.optString("robotEva");
                    sid = jsonObj.optString("sid");
                } catch (JSONException e) {
                }
            }
            return data.getMessageDirection() == Message.MessageDirection.RECEIVE
                    && data.getContent() instanceof TextMessage
                    && evaForRobot
                    && robotMode
                    && !TextUtils.isEmpty(robotEva)
                    && !TextUtils.isEmpty(sid)
                    && !data.getIsHistoryMessage();
        }
        return false;
    }

    public List<Message> getCheckedMessage() {
        List<Message> checkedMessage = new ArrayList<>();
        for (int i = 0; i < getCount(); ++i) {
            UIMessage uiMessage = getItem(i);
            if (uiMessage.isChecked()) {
                checkedMessage.add(uiMessage.getMessage());
            }
        }
        return checkedMessage;
    }

    /**
     * 绑定点击事件
     *
     * @param convertView View
     * @param contentView View
     * @param position    位置
     * @param data        UIMessage
     */
    private void bindViewClickEvent(final View convertView, final View contentView, final int position, final UIMessage data) {
        View.OnClickListener viewClickListener;
        View.OnTouchListener viewTouchListener;
        View.OnClickListener contentClickListener;
        View.OnLongClickListener contentLongClickListener;
        View.OnClickListener iconClickListener;
        View.OnLongClickListener iconLongClickListener;
        final ViewHolder holder = (ViewHolder) convertView.getTag();
        viewClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isShowCheckbox()) {
                    boolean checked = !data.isChecked();
                    if (maxMessageSelectedCount != -1 && getCheckedMessage().size() >= maxMessageSelectedCount && checked) {
                        if (selectedCountDidExceed != null) {
                            selectedCountDidExceed.onSelectedCountDidExceed();
                        }
                        return;
                    }
                    data.setChecked(checked);
                    holder.message_check.setChecked(checked);
                    if (messageCheckedChanged != null) {
                        messageCheckedChanged.onCheckedEnable(getCheckedMessage().size() > 0);
                    }
                }
            }
        };

        viewTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isShowCheckbox() && event.getAction() == MotionEvent.ACTION_UP) {
                    boolean checked = !data.isChecked();
                    if (maxMessageSelectedCount != -1 && getCheckedMessage().size() >= maxMessageSelectedCount && checked) {
                        if (selectedCountDidExceed != null) {
                            selectedCountDidExceed.onSelectedCountDidExceed();
                        }
                        return true;
                    }
                    data.setChecked(checked);
                    holder.message_check.setChecked(checked);
                    if (messageCheckedChanged != null) {
                        messageCheckedChanged.onCheckedEnable(getCheckedMessage().size() > 0);
                    }
                    return true;
                }
                return false;
            }
        };

        contentClickListener = new NoDoubleClickListener() {
            @Override
            public void onNoDoubleClick(View v) {
                if (RongContext.getInstance().getConversationBehaviorListener() != null) {
                    if (RongContext.getInstance().getConversationBehaviorListener().onMessageClick(mContext, v, data.getMessage())) {
                        return;
                    }
                } else if (RongContext.getInstance().getConversationClickListener() != null) {
                    if (RongContext.getInstance().getConversationClickListener().onMessageClick(mContext, v, data.getMessage())) {
                        return;
                    }
                }

                if (mOnItemHandlerListener != null) {
                    mOnItemHandlerListener.onMessageClick(position, data.getMessage(), v);
                }

                IContainerItemProvider.MessageProvider provider;//= RongContext.getInstance().getMessageTemplate(data.getContent().getClass());
                if (getNeedEvaluate(data))
                    provider = RongContext.getInstance().getEvaluateProvider();
                else
                    provider = RongContext.getInstance().getMessageTemplate(data.getContent().getClass());
                if (provider != null) {
                    if (data != null && data.getContent() != null && data.getContent().isDestruct() && data.getMessageDirection().equals(Message.MessageDirection.RECEIVE)) {
                        if (!BurnHintDialog.isFirstClick(v.getContext())) {
                            EventBus.getDefault().post(new Event.ShowDurnDialogEvent());
                            return;
                        }
                    }
                    provider.onItemClick(v, position, data != null ? data.getContent() : null, data);
                }
            }

            @Override
            public void onClick(View v) {
                long currentTime = Calendar.getInstance().getTimeInMillis();
                if (currentTime - lastClickTime > MIN_CLICK_DELAY_TIME) {
                    lastClickTime = currentTime;
                    onNoDoubleClick(v);
                }
            }
        };
        contentLongClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (isShowCheckbox()) {
                    return true;
                }
                if (RongContext.getInstance().getConversationBehaviorListener() != null) {
                    if (RongContext.getInstance().getConversationBehaviorListener().onMessageLongClick(mContext, v, data.getMessage())) {
                        return true;
                    }
                } else if (RongContext.getInstance().getConversationClickListener() != null) {
                    if (RongContext.getInstance().getConversationClickListener().onMessageLongClick(mContext, v, data.getMessage())) {
                        return true;
                    }
                }

                IContainerItemProvider.MessageProvider provider;//= RongContext.getInstance().getMessageTemplate(data.getContent().getClass());
                if (getNeedEvaluate(data))
                    provider = RongContext.getInstance().getEvaluateProvider();
                else
                    provider = RongContext.getInstance().getMessageTemplate(data.getContent().getClass());
                if (provider != null)
                    provider.onItemLongClick(v, position, data.getContent(), data);
                return true;
            }
        };

        iconClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UserInfo userInfo = data.getUserInfo();
                if (!TextUtils.isEmpty(data.getSenderUserId())) {
                    if (userInfo == null) {
                        userInfo = RongUserInfoManager.getInstance().getUserInfo(data.getSenderUserId());
                    }
                    userInfo = userInfo == null ? (new UserInfo(data.getSenderUserId(), null, null)) : userInfo;
                }
                if (RongContext.getInstance().getConversationBehaviorListener() != null) {
                    RongContext.getInstance().getConversationBehaviorListener().onUserPortraitClick(mContext, data.getConversationType(), userInfo);
                } else if (RongContext.getInstance().getConversationClickListener() != null) {
                    RongContext.getInstance().getConversationClickListener().onUserPortraitClick(mContext, data.getConversationType(), userInfo, data.getTargetId());
                }
            }
        };

        iconLongClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                UserInfo userInfo = data.getUserInfo();
                if (!TextUtils.isEmpty(data.getSenderUserId())) {
                    if (userInfo == null) {
                        userInfo = RongUserInfoManager.getInstance().getUserInfo(data.getSenderUserId());
                    }
                    userInfo = userInfo == null ? (new UserInfo(data.getSenderUserId(), null, null)) : userInfo;
                }
                if (data.getConversationType().equals(Conversation.ConversationType.GROUP)) {
                    GroupUserInfo groupUserInfo = RongUserInfoManager.getInstance().getGroupUserInfo(data.getTargetId(), data.getSenderUserId());
                    if (groupUserInfo != null && !TextUtils.isEmpty(groupUserInfo.getNickname()) && userInfo != null) {
                        userInfo.setName(groupUserInfo.getNickname());
                    }
                }

                if (data.getMessageDirection().equals(Message.MessageDirection.SEND)) {
                    if (RongContext.getInstance().getConversationBehaviorListener() != null) {
                        return RongContext.getInstance().getConversationBehaviorListener().onUserPortraitLongClick(mContext, data.getConversationType(), userInfo);
                    } else if (RongContext.getInstance().getConversationClickListener() != null) {
                        return RongContext.getInstance().getConversationClickListener().onUserPortraitLongClick(mContext, data.getConversationType(), userInfo, data.getTargetId());
                    }
                } else {
                    Object conversationListener = RongContext.getInstance().getConversationListener();
                    if (conversationListener != null) {
                        if (((conversationListener instanceof RongIM.ConversationClickListener)
                                && ((RongIM.ConversationClickListener) conversationListener).onUserPortraitLongClick(mContext, data.getConversationType(), userInfo, data.getTargetId()))
                                || ((conversationListener instanceof RongIM.ConversationBehaviorListener)
                                && ((RongIM.ConversationBehaviorListener) conversationListener).onUserPortraitLongClick(mContext, data.getConversationType(), userInfo))) {
                            return true;
                        }
                    }
                    if (convertView.getContext().getResources().getBoolean(R.bool.rc_enable_mentioned_message)
                            && (data.getConversationType().equals(Conversation.ConversationType.GROUP)
                            || data.getConversationType().equals(Conversation.ConversationType.DISCUSSION))) {
                        RongMentionManager.getInstance().mentionMember(data.getConversationType(), data.getTargetId(), data.getSenderUserId());
                        return true;
                    }
                }
                return true;
            }
        };

        if (isShowCheckbox() && allowShowCheckButton(data.getMessage())) {
            convertView.setOnClickListener(viewClickListener);
            contentView.setOnTouchListener(viewTouchListener);
            holder.rightIconView.setOnClickListener(viewClickListener);
            holder.leftIconView.setOnClickListener(viewClickListener);
        } else {
            contentView.setOnClickListener(contentClickListener);
            contentView.setOnLongClickListener(contentLongClickListener);
            holder.rightIconView.setOnClickListener(iconClickListener);
            holder.leftIconView.setOnClickListener(iconClickListener);
            holder.rightIconView.setOnLongClickListener(iconLongClickListener);
            holder.leftIconView.setOnLongClickListener(iconLongClickListener);
        }

        holder.warning.setOnClickListener(new DebouncedOnClickListener() {
            @Override
            public void onDebouncedClick(View view) {
                if (getItemHandlerListener() != null) {
                    getItemHandlerListener().onWarningViewClick(position, data.getMessage(), view);
                }
            }
        });
    }

    @Override
    protected void bindView(View v, final int position, final UIMessage data) {
        if (data == null) {
            return;
        }

        final ViewHolder holder = (ViewHolder) v.getTag();
        IContainerItemProvider provider;
        ProviderTag tag;

        if (holder == null) {
            RLog.e("MessageListAdapter", "view holder is null !");
            return;
        }
        if (getNeedEvaluate(data)) {
            provider = RongContext.getInstance().getEvaluateProvider();
            tag = RongContext.getInstance().getMessageProviderTag(data.getContent().getClass());
        } else if (RongContext.getInstance() != null && data.getContent() != null) {
            provider = RongContext.getInstance().getMessageTemplate(data.getContent().getClass());
            if (provider == null) {
                provider = RongContext.getInstance().getMessageTemplate(UnknownMessage.class);
                tag = RongContext.getInstance().getMessageProviderTag(UnknownMessage.class);
            } else {
                tag = RongContext.getInstance().getMessageProviderTag(data.getContent().getClass());
            }
            if (provider == null) {
                RLog.e("MessageListAdapter", data.getObjectName() + " message provider not found !");
                return;
            }
        } else {
            RLog.e("MessageListAdapter", "Message is null !");
            return;
        }
        //先判断是否是阅后即焚消息，如果是发送方，直接在列表中移除，并删除本地和远端消息，如果是接收方，倒计时结束删除本地和远端消息
        if (data.getContent() != null &&
                data.getContent().isDestruct()) {
            if (data.getMessage() != null &&
                    data.getMessage().getReadTime() > 0 &&
                    data.getMessage().getContent() != null) {
                if (data.getMessageDirection() == Message.MessageDirection.SEND) {
                    DestructManager.getInstance().deleteMessage(data.getMessage());
                    remove(position);
                    notifyDataSetChanged();
                    return;
                } else {
                    long readTime = data.getMessage().getReadTime();
                    MessageContent messageContent = data.getMessage().getContent();
                    long delay = (System.currentTimeMillis() - readTime) / 1000;
                    if (delay >= messageContent.getDestructTime()) {
                        DestructionTaskManager.getInstance().deleteMessage(data.getMessage());
                        remove(position);
                        notifyDataSetChanged();
                        return;
                    }
                }
            }
        }
        View v1;
        try {
            v1 = holder.contentView.inflate(provider);
        } catch (Exception e) {
            RLog.e(TAG, "bindView contentView inflate error", e);
            provider = RongContext.getInstance().getMessageTemplate(UnknownMessage.class);
            tag = RongContext.getInstance().getMessageProviderTag(UnknownMessage.class);
            v1 = holder.contentView.inflate(provider);
        }
        final View view = v1;

        provider.bindView(view, position, data);

        if (tag == null) {
            RLog.e("MessageListAdapter", "Can not find ProviderTag for " + data.getObjectName());
            return;
        }

        if (tag.hide()) {
            holder.contentView.setVisibility(View.GONE);
            holder.time.setVisibility(View.GONE);
            holder.nameView.setVisibility(View.GONE);
            holder.leftIconView.setVisibility(View.GONE);
            holder.rightIconView.setVisibility(View.GONE);
            holder.layoutItem.setVisibility(View.GONE);
            holder.layoutItem.setPadding(0, 0, 0, 0);
        } else {
            holder.contentView.setVisibility(View.VISIBLE);
            holder.layoutItem.setVisibility(View.VISIBLE);
            holder.layoutItem.setPadding(RongUtils.dip2px(8),
                    RongUtils.dip2px(6),
                    RongUtils.dip2px(8),
                    RongUtils.dip2px(6));
        }

        if (data.getMessageDirection() == Message.MessageDirection.SEND) {

            if (tag.showPortrait()) {
                holder.rightIconView.setVisibility(View.VISIBLE);
                holder.leftIconView.setVisibility(View.GONE);
            } else {
                holder.leftIconView.setVisibility(View.GONE);
                holder.rightIconView.setVisibility(View.GONE);
            }

            if (!tag.centerInHorizontal()) {
                setGravity(holder.layout, Gravity.RIGHT);
                holder.contentView.containerViewRight();
                holder.nameView.setGravity(Gravity.RIGHT);
            } else {
                setGravity(holder.layout, Gravity.CENTER);
                holder.contentView.containerViewCenter();
                holder.nameView.setGravity(Gravity.CENTER_HORIZONTAL);
                holder.contentView.setBackgroundColor(Color.TRANSPARENT);
            }

            //readRec 是否显示已读回执
            boolean readRec = false;
            try {
                readRec = mContext.getResources().getBoolean(R.bool.rc_read_receipt);
            } catch (Resources.NotFoundException e) {
                RLog.e(TAG, "bindView rc_read_receipt not configure in rc_config.xml", e);
            }
            // 如果在需要重发队列里面，显示为发送中。。。的状态
            if (data.getSentStatus() == Message.SentStatus.SENDING) {
                if (tag.showProgress())
                    holder.progressBar.setVisibility(View.VISIBLE);
                else
                    holder.progressBar.setVisibility(View.GONE);

                holder.warning.setVisibility(View.GONE);
                holder.readReceipt.setVisibility(View.GONE);
            } else if (data.getSentStatus() == Message.SentStatus.FAILED) {
                if (ResendManager.getInstance().needResend(data.getMessageId())) {
                    // 图片消息，动图消息，小视频消息不显示等待
                    if (data.getMessage().getContent() instanceof ImageMessage
                            || data.getMessage().getContent() instanceof GIFMessage
                            || data.getMessage().getContent() instanceof SightMessage) {
                        holder.progressBar.setVisibility(View.GONE);
                    } else {
                        holder.progressBar.setVisibility(View.VISIBLE);
                    }
                    holder.warning.setVisibility(View.GONE);
                } else {
                    holder.progressBar.setVisibility(View.GONE);
                    holder.warning.setVisibility(View.VISIBLE);
                }
                holder.readReceipt.setVisibility(View.GONE);
            } else if (data.getSentStatus() == Message.SentStatus.SENT) {
                holder.progressBar.setVisibility(View.GONE);
                holder.warning.setVisibility(View.GONE);
                holder.readReceipt.setVisibility(View.GONE);
            } else if (readRec && data.getSentStatus() == Message.SentStatus.READ) {
                holder.progressBar.setVisibility(View.GONE);
                holder.warning.setVisibility(View.GONE);
                if (data.getConversationType().equals(Conversation.ConversationType.PRIVATE) && tag.showReadState()) {
                    holder.readReceipt.setVisibility(View.VISIBLE);
                } else {
                    holder.readReceipt.setVisibility(View.GONE);
                }
            } else {
                holder.progressBar.setVisibility(View.GONE);
                holder.warning.setVisibility(View.GONE);
                holder.readReceipt.setVisibility(View.GONE);
            }

            holder.readReceiptRequest.setVisibility(View.GONE);
            holder.readReceiptStatus.setVisibility(View.GONE);
            if (readRec && RongContext.getInstance().isReadReceiptConversationType(data.getConversationType())
                    && (data.getConversationType().equals(Conversation.ConversationType.GROUP) || data.getConversationType().equals(Conversation.ConversationType.DISCUSSION))) {
                if (allowReadReceiptRequest(data.getMessage())
                        && !TextUtils.isEmpty(data.getUId())) {
                    boolean isLastSentMessage = true;
                    for (int i = position + 1; i < getCount(); i++) {
                        if (getItem(i).getMessageDirection() == Message.MessageDirection.SEND) {
                            isLastSentMessage = false;
                            break;
                        }
                    }

                    long serverTime = System.currentTimeMillis() - RongIMClient.getInstance().getDeltaTime();

                    if ((serverTime - data.getSentTime() < readReceiptRequestInterval * 1000)
                            && isLastSentMessage
                            && (data.getReadReceiptInfo() == null || !data.getReadReceiptInfo().isReadReceiptMessage())) {
                        holder.readReceiptRequest.setVisibility(View.VISIBLE);
                    }
                }
                if (allowReadReceiptRequest(data.getMessage())
                        && data.getReadReceiptInfo() != null
                        && data.getReadReceiptInfo().isReadReceiptMessage()) {
                    if (data.getReadReceiptInfo().getRespondUserIdList() != null) {
                        holder.readReceiptStatus.setText(String.format(view.getResources().getString(R.string.rc_read_receipt_status), data.getReadReceiptInfo().getRespondUserIdList().size()));
                    } else {
                        holder.readReceiptStatus.setText(String.format(view.getResources().getString(R.string.rc_read_receipt_status), 0));
                    }
                    holder.readReceiptStatus.setVisibility(View.VISIBLE);
                }
            }

            holder.nameView.setVisibility(View.GONE);

            holder.readReceiptRequest.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    RongIMClient.getInstance().sendReadReceiptRequest(data.getMessage(), new RongIMClient.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            ReadReceiptInfo readReceiptInfo = data.getReadReceiptInfo();
                            if (readReceiptInfo == null) {
                                readReceiptInfo = new ReadReceiptInfo();
                                data.setReadReceiptInfo(readReceiptInfo);
                            }
                            readReceiptInfo.setIsReadReceiptMessage(true);
                            holder.readReceiptStatus.setText(String.format(view.getResources().getString(R.string.rc_read_receipt_status), 0));
                            holder.readReceiptRequest.setVisibility(View.GONE);
                            holder.readReceiptStatus.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onError(RongIMClient.ErrorCode errorCode) {
                            RLog.e(TAG, "sendReadReceiptRequest failed, errorCode = " + errorCode);
                        }
                    });
                }
            });

            holder.readReceiptStatus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnItemHandlerListener != null) {
                        mOnItemHandlerListener.onReadReceiptStateClick(data.getMessage());
                    }
                }
            });

            if (!tag.showWarning())
                holder.warning.setVisibility(View.GONE);

//            holder.sentStatus.setVisibility(View.VISIBLE);

        } else {
            if (tag.showPortrait()) {
                holder.rightIconView.setVisibility(View.GONE);
                holder.leftIconView.setVisibility(View.VISIBLE);
            } else {
                holder.leftIconView.setVisibility(View.GONE);
                holder.rightIconView.setVisibility(View.GONE);
            }

            if (!tag.centerInHorizontal()) {
                setGravity(holder.layout, Gravity.LEFT);
                holder.contentView.containerViewLeft();
                holder.nameView.setGravity(Gravity.LEFT);

            } else {
                setGravity(holder.layout, Gravity.CENTER);
                holder.contentView.containerViewCenter();
                holder.nameView.setGravity(Gravity.CENTER_HORIZONTAL);
                holder.contentView.setBackgroundColor(Color.TRANSPARENT);
            }

            holder.progressBar.setVisibility(View.GONE);
            holder.warning.setVisibility(View.GONE);
            holder.readReceipt.setVisibility(View.GONE);
            holder.readReceiptRequest.setVisibility(View.GONE);
            holder.readReceiptStatus.setVisibility(View.GONE);

            holder.nameView.setVisibility(View.VISIBLE);

            if (data.getConversationType() == Conversation.ConversationType.PRIVATE
                    || !tag.showSummaryWithName()
                    || data.getConversationType() == Conversation.ConversationType.PUBLIC_SERVICE
                    || data.getConversationType() == Conversation.ConversationType.APP_PUBLIC_SERVICE) {

                holder.nameView.setVisibility(View.GONE);
            } else {
                UserInfo userInfo = data.getUserInfo();
                if (data.getConversationType().equals(Conversation.ConversationType.CUSTOMER_SERVICE)
                        && data.getMessageDirection().equals(Message.MessageDirection.RECEIVE)) {

                    if (userInfo == null) {
                        if (data.getMessage() != null && data.getMessage().getContent() != null) {
                            userInfo = data.getMessage().getContent().getUserInfo();
                        }
                    }
                    if (userInfo != null) {
                        holder.nameView.setText(userInfo.getName());
                    } else {
                        holder.nameView.setText(data.getSenderUserId());
                    }
                } else if (data.getConversationType() == Conversation.ConversationType.GROUP) {
                    GroupUserInfo groupUserInfo = RongUserInfoManager.getInstance().getGroupUserInfo(data.getTargetId(), data.getSenderUserId());
                    if (groupUserInfo != null && !TextUtils.isEmpty(groupUserInfo.getNickname())) {
                        holder.nameView.setText(groupUserInfo.getNickname());
                    } else {
                        if (userInfo == null) {
                            userInfo = RongUserInfoManager.getInstance().getUserInfo(data.getSenderUserId());
                        }
                        if (userInfo == null)
                            holder.nameView.setText(data.getSenderUserId());
                        else
                            holder.nameView.setText(userInfo.getName());
                    }
                } else {
                    if (userInfo == null) {
                        userInfo = RongUserInfoManager.getInstance().getUserInfo(data.getSenderUserId());
                    }
                    if (userInfo == null)
                        holder.nameView.setText(data.getSenderUserId());
                    else
                        holder.nameView.setText(userInfo.getName());
                }
            }

        }

        if (holder.rightIconView.getVisibility() == View.VISIBLE) {
            UserInfo userInfo = data.getUserInfo();
            Uri portrait = null;
            if (data.getConversationType().equals(Conversation.ConversationType.CUSTOMER_SERVICE)
                    && data.getUserInfo() != null && data.getMessageDirection().equals(Message.MessageDirection.RECEIVE)) {
                if (userInfo != null) {
                    portrait = userInfo.getPortraitUri();
                }
                holder.rightIconView.setAvatar(portrait != null ? portrait.toString() : null, 0);
            } else if ((data.getConversationType().equals(Conversation.ConversationType.PUBLIC_SERVICE)
                    || data.getConversationType().equals(Conversation.ConversationType.APP_PUBLIC_SERVICE))
                    && data.getMessageDirection().equals(Message.MessageDirection.RECEIVE)) {
                if (userInfo != null) {
                    portrait = userInfo.getPortraitUri();
                    holder.rightIconView.setAvatar(portrait != null ? portrait.toString() : null, 0);
                } else {
                    PublicServiceProfile publicServiceProfile;

                    ConversationKey mKey = ConversationKey.obtain(data.getTargetId(), data.getConversationType());
                    publicServiceProfile = RongContext.getInstance().getPublicServiceInfoFromCache(mKey.getKey());
                    portrait = publicServiceProfile.getPortraitUri();

                    holder.rightIconView.setAvatar(portrait != null ? portrait.toString() : null, 0);
                }
            } else if (!TextUtils.isEmpty(data.getSenderUserId())) {
                if (userInfo == null) {
                    userInfo = RongUserInfoManager.getInstance().getUserInfo(data.getSenderUserId());
                }

                if (userInfo != null && userInfo.getPortraitUri() != null) {
                    holder.rightIconView.setAvatar(userInfo.getPortraitUri().toString(), 0);
                } else {
                    holder.rightIconView.setAvatar(null, 0);
                }
            }
        } else if (holder.leftIconView.getVisibility() == View.VISIBLE) {
            UserInfo userInfo = data.getUserInfo();
            Uri portrait;
            if (data.getConversationType().equals(Conversation.ConversationType.CUSTOMER_SERVICE)
                    && data.getMessageDirection().equals(Message.MessageDirection.RECEIVE)) {
                if (userInfo == null && data.getMessage() != null && data.getMessage().getContent() != null) {
                    userInfo = data.getMessage().getContent().getUserInfo();
                }

                if (userInfo != null) {
                    portrait = userInfo.getPortraitUri();
                    holder.leftIconView.setAvatar(portrait != null ? portrait.toString() : null, R.drawable.rc_cs_default_portrait);
                }
            } else if ((data.getConversationType().equals(Conversation.ConversationType.PUBLIC_SERVICE)
                    || data.getConversationType().equals(Conversation.ConversationType.APP_PUBLIC_SERVICE))
                    && data.getMessageDirection().equals(Message.MessageDirection.RECEIVE)) {
                if (userInfo != null) {
                    portrait = userInfo.getPortraitUri();
                    holder.leftIconView.setAvatar(portrait != null ? portrait.toString() : null, 0);
                } else {
                    PublicServiceProfile publicServiceProfile = null;
                    ConversationKey mKey = ConversationKey.obtain(data.getTargetId(), data.getConversationType());
                    if (mKey != null) {
                        publicServiceProfile = RongContext.getInstance().getPublicServiceInfoFromCache(mKey.getKey());
                    }
                    if (publicServiceProfile != null && publicServiceProfile.getPortraitUri() != null) {
                        holder.leftIconView.setAvatar(publicServiceProfile.getPortraitUri().toString(), 0);
                    } else {
                        holder.leftIconView.setAvatar(null, 0);
                    }
                }
            } else if (!TextUtils.isEmpty(data.getSenderUserId())) {
                if (userInfo == null) {
                    userInfo = RongUserInfoManager.getInstance().getUserInfo(data.getSenderUserId());
                }
                if (userInfo != null && userInfo.getPortraitUri() != null) {
                    holder.leftIconView.setAvatar(userInfo.getPortraitUri().toString(), 0);
                } else {
                    holder.leftIconView.setAvatar(null, 0);
                }
            }
        }

        bindViewClickEvent(v, view, position, data);
        if (tag.hide()) {
            holder.time.setVisibility(View.GONE);
            return;
        }

        if (!timeGone) {
            String time = RongDateUtils.getConversationFormatDate(data.getSentTime(), view.getContext());
            holder.time.setText(time);
            if (position == 0) {
                if (data.getMessage() != null && data.getMessage().getContent() != null
                        && data.getMessage().getContent() instanceof HistoryDividerMessage) {
                    holder.time.setVisibility(View.GONE);
                } else {
                    holder.time.setVisibility(View.VISIBLE);
                }
            } else {
                UIMessage pre = getItem(position - 1);
                if (RongDateUtils.isShowChatTime(data.getSentTime(), pre.getSentTime(), 180)) {
                    holder.time.setVisibility(View.VISIBLE);
                } else {
                    holder.time.setVisibility(View.GONE);
                }
            }
        }

        if (isShowCheckbox() && allowShowCheckButton(data.getMessage())) {
            holder.checkboxLayout.setVisibility(View.VISIBLE);
            holder.message_check.setFocusable(false);
            holder.message_check.setClickable(false);
            holder.message_check.setChecked(data.isChecked());
        } else {
            holder.checkboxLayout.setVisibility(View.GONE);
            data.setChecked(false);
        }
        if (messageCheckedChanged != null) {
            messageCheckedChanged.onCheckedEnable(getCheckedMessage().size() > 0);
        }
    }


    protected void setGravity(View view, int gravity) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
        params.gravity = gravity;
    }

    public void setEvaluateForRobot(boolean needEvaluate) {
        evaForRobot = needEvaluate;
    }

    public void setRobotMode(boolean robotMode) {
        this.robotMode = robotMode;
    }

    /**
     * 群组讨论组里特定消息类型的消息是否允许已读回执
     * 默认只有文本消息有已读回执，开发者可以重写此方法加入更多有已读回执功能的消息类型
     *
     * @param message 消息
     * @return boolean 是否有已读回执功能
     */
    public boolean allowReadReceiptRequest(Message message) {
        return message != null && message.getContent() != null &&
                (message.getContent() instanceof TextMessage);

    }

    /**
     * 多选状态时是否显示checkBox，开发者需要重写此方法，根据消息类型判断是否显示。
     *
     * @param message 消息类型
     * @return true 显示，false 不显示
     */
    protected boolean allowShowCheckButton(Message message) {
        if (message != null) {
            MessageContent messageContent = message.getContent();
            if (messageContent != null) {
                return !(messageContent instanceof InformationNotificationMessage) &&
                        !(messageContent instanceof UnknownMessage) &&
                        !(messageContent instanceof GroupNotificationMessage) &&
                        !(messageContent instanceof RecallNotificationMessage) &&
                        !(messageContent instanceof RealTimeLocationJoinMessage);
            }
        }
        return true;
    }

    /**
     * 防止快速连击时打开两个预览页面
     */
    private abstract class NoDoubleClickListener implements View.OnClickListener {

        public static final int MIN_CLICK_DELAY_TIME = 500;
        public long lastClickTime = 0;

        public abstract void onNoDoubleClick(View v);
    }

}

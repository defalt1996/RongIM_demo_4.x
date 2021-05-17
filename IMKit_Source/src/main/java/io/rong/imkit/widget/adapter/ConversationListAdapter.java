package io.rong.imkit.widget.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import io.rong.common.RLog;
import io.rong.eventbus.EventBus;
import io.rong.imkit.R;
import io.rong.imkit.RongContext;
import io.rong.imkit.model.ConversationProviderTag;
import io.rong.imkit.model.Event;
import io.rong.imkit.model.UIConversation;
import io.rong.imkit.widget.AsyncImageView;
import io.rong.imkit.widget.ProviderContainerView;
import io.rong.imkit.widget.provider.IContainerItemProvider;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;

public class ConversationListAdapter extends BaseAdapter<UIConversation> {
    private final static String TAG = "ConversationListAdapter";
    LayoutInflater mInflater;
    Context mContext;

    @Override
    public long getItemId(int position) {
        UIConversation conversation = getItem(position);
        if (conversation == null)
            return 0;
        return conversation.hashCode();
    }

    protected class ViewHolder {
        public View layout;
        public View leftImageLayout;
        public View rightImageLayout;
        public View leftUnReadView;
        public View rightUnReadView;
        public AsyncImageView leftImageView;
        public TextView unReadMsgCount;
        public ImageView unReadMsgCountIcon;
        public AsyncImageView rightImageView;
        public TextView unReadMsgCountRight;
        public ImageView unReadMsgCountRightIcon;
        public ProviderContainerView contentView;

    }

    public ConversationListAdapter(Context context) {
        super();
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
    }

    public int findGatheredItem(Conversation.ConversationType type) {
        int index = getCount();
        int position = -1;
        while ((index-- > 0)) {
            UIConversation uiConversation = getItem(index);
            if (uiConversation.getConversationType().equals(type)) {
                position = index;
                break;
            }
        }
        return position;
    }

    public int findPosition(Conversation.ConversationType type, String targetId) {
        int index = getCount();
        int position = -1;
        while (index-- > 0) {
            if (getItem(index).getConversationType().equals(type)
                    && getItem(index).getConversationTargetId().equals(targetId)) {
                position = index;
                break;
            }
        }
        return position;
    }

    @Override
    protected View newView(Context context, int position, ViewGroup group) {
        View result = mInflater.inflate(R.layout.rc_item_conversation, null);

        ViewHolder holder = new ViewHolder();
        holder.layout = findViewById(result, R.id.rc_item_conversation);
        holder.leftImageLayout = findViewById(result, R.id.rc_item1);
        holder.rightImageLayout = findViewById(result, R.id.rc_item2);
        holder.leftUnReadView = findViewById(result, R.id.rc_unread_view_left);
        holder.rightUnReadView = findViewById(result, R.id.rc_unread_view_right);
        holder.leftImageView = findViewById(result, R.id.rc_left);
        holder.rightImageView = findViewById(result, R.id.rc_right);
        holder.contentView = findViewById(result, R.id.rc_content);
        holder.unReadMsgCount = findViewById(result, R.id.rc_unread_message);
        holder.unReadMsgCountRight = findViewById(result, R.id.rc_unread_message_right);
        holder.unReadMsgCountIcon = findViewById(result, R.id.rc_unread_message_icon);
        holder.unReadMsgCountRightIcon = findViewById(result, R.id.rc_unread_message_icon_right);
        result.setTag(holder);
        return result;
    }

    @Override
    protected void bindView(View v, int position, final UIConversation data) {
        ViewHolder holder = (ViewHolder) v.getTag();

        if (data == null) {
            return;
        }
        /*通过会话类型，获得对应的会话provider.ex: PrivateConversationProvider*/
        IContainerItemProvider provider = RongContext.getInstance().getConversationTemplate(data.getConversationType().getName());
        if (provider == null) {
            RLog.e(TAG, "provider is null");
            return;
        }

        View view = holder.contentView.inflate(provider);
        provider.bindView(view, position, data);

        //设置背景色
        if (data.isTop())
            holder.layout.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.rc_item_top_list_selector));
        else
            holder.layout.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.rc_item_list_selector));


        ConversationProviderTag tag = RongContext.getInstance().getConversationProviderTag(data.getConversationType().getName());

        int defaultId;
        if (data.getConversationType().equals(Conversation.ConversationType.GROUP)) {
            defaultId = R.drawable.rc_default_group_portrait;
        } else if (data.getConversationType().equals(Conversation.ConversationType.DISCUSSION)) {
            defaultId = R.drawable.rc_default_discussion_portrait;
        } else {
            defaultId = R.drawable.rc_default_portrait;
        }
        // 1:图像靠左显示。2：图像靠右显示。3：不显示图像。
        if (tag.portraitPosition() == 1) {
            holder.leftImageLayout.setVisibility(View.VISIBLE);

            holder.leftImageLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnPortraitItemClick != null)
                        mOnPortraitItemClick.onPortraitItemClick(v, data);
                }
            });
            holder.leftImageLayout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (mOnPortraitItemClick != null)
                        mOnPortraitItemClick.onPortraitItemLongClick(v, data);
                    return true;
                }
            });
            if (data.getConversationGatherState()) {
                holder.leftImageView.setAvatar(null, defaultId);
            } else {
                if (data.getIconUrl() != null) {
                    holder.leftImageView.setAvatar(data.getIconUrl().toString(), defaultId);
                } else {
                    holder.leftImageView.setAvatar(null, defaultId);
                }
            }

            if (data.getUnReadMessageCount() > 0) {
                holder.unReadMsgCountIcon.setVisibility(View.VISIBLE);
                setUnReadViewLayoutParams(holder.leftUnReadView, data.getUnReadType());
                if (data.getUnReadType().equals(UIConversation.UnreadRemindType.REMIND_WITH_COUNTING)) {
                    if (data.getUnReadMessageCount() > 99) {
                        holder.unReadMsgCount.setText(mContext.getResources().getString(R.string.rc_message_unread_count));
                    } else {
                        holder.unReadMsgCount.setText(Integer.toString(data.getUnReadMessageCount()));
                    }
                    holder.unReadMsgCount.setVisibility(View.VISIBLE);
                    holder.unReadMsgCountIcon.setImageResource(R.drawable.rc_unread_count_bg);
                } else {
                    holder.unReadMsgCount.setVisibility(View.GONE);
                    holder.unReadMsgCountIcon.setImageResource(R.drawable.rc_unread_remind_list_count);
                }
            } else {
                holder.unReadMsgCountIcon.setVisibility(View.GONE);
                holder.unReadMsgCount.setVisibility(View.GONE);
            }

            holder.rightImageLayout.setVisibility(View.GONE);
        } else if (tag.portraitPosition() == 2) {
            holder.rightImageLayout.setVisibility(View.VISIBLE);

            holder.rightImageLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnPortraitItemClick != null)
                        mOnPortraitItemClick.onPortraitItemClick(v, data);
                }
            });
            holder.rightImageLayout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (mOnPortraitItemClick != null)
                        mOnPortraitItemClick.onPortraitItemLongClick(v, data);
                    return true;
                }
            });

            if (data.getConversationGatherState()) {
                holder.rightImageView.setAvatar(null, defaultId);
            } else {
                if (data.getIconUrl() != null) {
                    holder.rightImageView.setAvatar(data.getIconUrl().toString(), defaultId);
                } else {
                    holder.rightImageView.setAvatar(null, defaultId);
                }
            }

            if (data.getUnReadMessageCount() > 0) {
                holder.unReadMsgCountRightIcon.setVisibility(View.VISIBLE);
                setUnReadViewLayoutParams(holder.rightUnReadView, data.getUnReadType());
                if (data.getUnReadType().equals(UIConversation.UnreadRemindType.REMIND_WITH_COUNTING)) {
                    holder.unReadMsgCount.setVisibility(View.VISIBLE);
                    if (data.getUnReadMessageCount() > 99) {
                        holder.unReadMsgCountRight.setText(mContext.getResources().getString(R.string.rc_message_unread_count));
                    } else {
                        holder.unReadMsgCountRight.setText(Integer.toString(data.getUnReadMessageCount()));
                    }
                    holder.unReadMsgCountRightIcon.setImageResource(R.drawable.rc_unread_count_bg);
                } else {
                    holder.unReadMsgCount.setVisibility(View.GONE);
                    holder.unReadMsgCountRightIcon.setImageResource(R.drawable.rc_unread_remind_without_count);
                }
            } else {
                holder.unReadMsgCountIcon.setVisibility(View.GONE);
                holder.unReadMsgCount.setVisibility(View.GONE);
            }

            holder.leftImageLayout.setVisibility(View.GONE);
        } else if (tag.portraitPosition() == 3) {
            holder.rightImageLayout.setVisibility(View.GONE);
            holder.leftImageLayout.setVisibility(View.GONE);
        } else {
            throw new IllegalArgumentException("the portrait position is wrong!");
        }
        MessageContent content = data.getMessageContent();
        if (content != null && content.isDestruct()) {
            RongIMClient.getInstance().getMessage(data.getLatestMessageId(), new RongIMClient.ResultCallback<Message>() {

                @Override
                public void onSuccess(Message message) {
                    if (message == null) {
                        EventBus.getDefault().post(new Event.MessageDeleteEvent(data.getLatestMessageId()));
                    }
                }

                @Override
                public void onError(RongIMClient.ErrorCode e) {

                }
            });
        }
    }

    protected void setUnReadViewLayoutParams(View view, UIConversation.UnreadRemindType type) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        Context context = view.getContext();
        if (type == UIConversation.UnreadRemindType.REMIND_WITH_COUNTING) {
            params.width = (int) context.getResources().getDimension(R.dimen.rc_dimen_size_18);
            params.height = (int) context.getResources().getDimension(R.dimen.rc_dimen_size_18);
            params.leftMargin = (int) mContext.getResources().getDimension(R.dimen.rc_dimen_size_44);
            params.topMargin = (int) context.getResources().getDimension(R.dimen.rc_dimen_size_5);
        } else {
            params.width = (int) context.getResources().getDimension(R.dimen.rc_dimen_size_9);
            params.height = (int) context.getResources().getDimension(R.dimen.rc_dimen_size_9);
            params.leftMargin = (int) context.getResources().getDimension(R.dimen.rc_dimen_size_50);
            params.topMargin = (int) context.getResources().getDimension(R.dimen.rc_dimen_size_7);
        }
        view.setLayoutParams(params);
    }

    private OnPortraitItemClick mOnPortraitItemClick;

    public interface OnPortraitItemClick {
        void onPortraitItemClick(View v, UIConversation data);

        boolean onPortraitItemLongClick(View v, UIConversation data);
    }

    public void setOnPortraitItemClick(OnPortraitItemClick onPortraitItemClick) {
        this.mOnPortraitItemClick = onPortraitItemClick;
    }
}

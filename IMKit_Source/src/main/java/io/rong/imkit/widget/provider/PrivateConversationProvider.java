package io.rong.imkit.widget.provider;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import io.rong.common.RLog;
import io.rong.imkit.R;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.emoticon.AndroidEmoji;
import io.rong.imkit.model.ConversationProviderTag;
import io.rong.imkit.model.ProviderTag;
import io.rong.imkit.model.UIConversation;
import io.rong.imkit.resend.ResendManager;
import io.rong.imkit.userInfoCache.RongUserInfoManager;
import io.rong.imkit.utils.RongDateUtils;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.UserInfo;
import io.rong.message.RecallNotificationMessage;

@ConversationProviderTag(conversationType = "private", portraitPosition = 1)
public class PrivateConversationProvider implements IContainerItemProvider.ConversationProvider<UIConversation> {
    private static final String TAG = "PrivateConversationProvider";

    protected class ViewHolder {
        public TextView title;
        public TextView time;
        public TextView content;
        public ImageView notificationBlockImage;
        public ImageView readStatus;
    }

    public View newView(Context context, ViewGroup viewGroup) {
        View result = LayoutInflater.from(context).inflate(R.layout.rc_item_base_conversation, null);

        ViewHolder holder = new ViewHolder();
        holder.title = result.findViewById(R.id.rc_conversation_title);
        holder.time = result.findViewById(R.id.rc_conversation_time);
        holder.content = result.findViewById(R.id.rc_conversation_content);
        holder.notificationBlockImage = result.findViewById(R.id.rc_conversation_msg_block);
        holder.readStatus = result.findViewById(R.id.rc_conversation_status);
        result.setTag(holder);

        return result;
    }

    private void handleMentionedContent(final ViewHolder holder, final View view, final UIConversation data) {
        final int empirical_value = 60;//经验值，部分机型上不做调整无法打点。
        final SpannableStringBuilder builder = new SpannableStringBuilder();
        SpannableString string;
        final String preStr = view.getContext().getString(R.string.rc_message_content_mentioned);
        if (holder.content.getWidth() > empirical_value) {
            CharSequence cutStr = TextUtils.ellipsize(preStr + " " + data.getConversationContent(), holder.content.getPaint(), holder.content.getWidth() - empirical_value, TextUtils.TruncateAt.END);
            string = new SpannableString(cutStr);
            int colorSpanLength = preStr.length();
            if (colorSpanLength > cutStr.length()) {
                colorSpanLength = cutStr.length();
            }
            string.setSpan(new ForegroundColorSpan(view.getContext().getResources().getColor(R.color.rc_mentioned_color)), 0, colorSpanLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.append(string);

            AndroidEmoji.ensure(builder);
            holder.content.setText(builder, TextView.BufferType.SPANNABLE);
        } else {
            holder.content.post(new Runnable() {
                @Override
                public void run() {
                    if (holder.content.getWidth() > empirical_value) {
                        CharSequence cutStr = TextUtils.ellipsize(preStr + " " + data.getConversationContent(), holder.content.getPaint(), holder.content.getWidth() - 40, TextUtils.TruncateAt.END);
                        SpannableString str = new SpannableString(cutStr);
                        int colorSpanLength = preStr.length();
                        if (colorSpanLength > cutStr.length()) {
                            colorSpanLength = cutStr.length();
                        }
                        str.setSpan(new ForegroundColorSpan(view.getContext().getResources().getColor(R.color.rc_mentioned_color)), 0, colorSpanLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        builder.append(str);
                    } else {
                        String oriStr = preStr + " " + data.getConversationContent();
                        SpannableString str = new SpannableString(oriStr);
                        str.setSpan(new ForegroundColorSpan(view.getContext().getResources().getColor(R.color.rc_mentioned_color)), 0, preStr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        builder.append(str);
                    }
                    AndroidEmoji.ensure(builder);
                    holder.content.setText(builder, TextView.BufferType.SPANNABLE);
                }
            });
        }
    }


    private void handleDraftContent(final ViewHolder holder, final View view, final UIConversation data) {
        final int empirical_value = 60;//经验值，部分机型上不做调整无法打点。
        final SpannableStringBuilder builder = new SpannableStringBuilder();
        SpannableString string;
        final String preStr = view.getContext().getString(R.string.rc_message_content_draft);
        if (holder.content.getWidth() > empirical_value) {
            CharSequence cutStr = TextUtils.ellipsize(preStr + " " + data.getDraft(), holder.content.getPaint(), holder.content.getWidth() - empirical_value, TextUtils.TruncateAt.END);
            string = new SpannableString(cutStr);
            int colorSpanLength = preStr.length();
            if (colorSpanLength > cutStr.length()) {
                colorSpanLength = cutStr.length();
            }
            string.setSpan(new ForegroundColorSpan(view.getContext().getResources().getColor(R.color.rc_draft_color)), 0, colorSpanLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.append(string);
            AndroidEmoji.ensure(builder);
            holder.content.setText(builder, TextView.BufferType.SPANNABLE);
        } else {
            holder.content.post(new Runnable() {
                @Override
                public void run() {
                    if (holder.content.getWidth() > empirical_value) {
                        CharSequence cutStr = TextUtils.ellipsize(preStr + " " + data.getDraft(), holder.content.getPaint(), holder.content.getWidth() - empirical_value, TextUtils.TruncateAt.END);
                        SpannableString str = new SpannableString(cutStr);
                        int colorSpanLength = preStr.length();
                        if (colorSpanLength > cutStr.length()) {
                            colorSpanLength = cutStr.length();
                        }
                        str.setSpan(new ForegroundColorSpan(view.getContext().getResources().getColor(R.color.rc_draft_color)), 0, colorSpanLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        builder.append(str);
                    } else {
                        String oriStr = preStr + " " + data.getDraft();
                        SpannableString str = new SpannableString(oriStr);
                        str.setSpan(new ForegroundColorSpan(view.getContext().getResources().getColor(R.color.rc_draft_color)), 0, preStr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        builder.append(str);
                    }
                    AndroidEmoji.ensure(builder);
                    holder.content.setText(builder, TextView.BufferType.SPANNABLE);
                }
            });
        }
    }

    private void handleCommonContent(final ViewHolder holder, final UIConversation data) {
        final CharSequence cutStr;
        //清除聊天记录后，会话内容为空
        //解决表情显示不全
        final int empirical_value = 60;//经验值，部分机型上不做调整无法打点。
        if (holder.content.getWidth() > empirical_value && data.getConversationContent() != null) {
            cutStr = TextUtils.ellipsize(data.getConversationContent(), holder.content.getPaint(),
                    holder.content.getWidth() - empirical_value, TextUtils.TruncateAt.END);
            holder.content.setText(cutStr, TextView.BufferType.SPANNABLE);
        } else {
            cutStr = data.getConversationContent();
            holder.content.post(new Runnable() {
                @Override
                public void run() {
                    if (holder.content.getWidth() > empirical_value && cutStr != null) {
                        CharSequence str = TextUtils.ellipsize(cutStr, holder.content.getPaint(),
                                holder.content.getWidth() - empirical_value, TextUtils.TruncateAt.END);
                        holder.content.setText(str, TextView.BufferType.SPANNABLE);
                    } else {
                        holder.content.setText(cutStr);
                    }
                }
            });
        }
    }

    public void bindView(final View view, int position, final UIConversation data) {
        final ViewHolder holder = (ViewHolder) view.getTag();
        ProviderTag tag = null;
        if (data == null) {
            holder.title.setText(null);
            holder.time.setText(null);
            holder.content.setText(null);
        } else {
            //设置会话标题
            holder.title.setText(data.getUIConversationTitle());
            //设置会话时间
            String time = RongDateUtils.getConversationListFormatDate(data.getUIConversationTime(), view.getContext());
            holder.time.setText(time);

            //设置内容
            if (!TextUtils.isEmpty(data.getDraft()) || data.getMentionedFlag()) {
                if (data.getMentionedFlag()) {
                    handleMentionedContent(holder, view, data);
                } else {
                    handleDraftContent(holder, view, data);
                }
                holder.readStatus.setVisibility(View.GONE);
            } else {
                //设置已读
                //readRec 是否显示已读回执
                boolean readRec = false;
                try {
                    readRec = view.getResources().getBoolean(R.bool.rc_read_receipt);
                } catch (Resources.NotFoundException e) {
                    RLog.e(TAG, "rc_read_receipt not configure in rc_config.xml");
                    e.printStackTrace();
                }

                if (readRec) {
                    if (data.getSentStatus() == Message.SentStatus.READ
                            && data.getConversationSenderId().equals(RongIM.getInstance().getCurrentUserId())
                            && !(data.getMessageContent() instanceof RecallNotificationMessage)) {
                        holder.readStatus.setVisibility(View.VISIBLE);
                    } else {
                        holder.readStatus.setVisibility(View.GONE);
                    }
                }
                handleCommonContent(holder, data);
            }

            if (RongContext.getInstance() != null && data.getMessageContent() != null)
                tag = RongContext.getInstance().getMessageProviderTag(data.getMessageContent().getClass());

            if (data.getSentStatus() != null && (data.getSentStatus() == Message.SentStatus.FAILED
                    || data.getSentStatus() == Message.SentStatus.SENDING) && tag != null && tag.showWarning()
                    && data.getConversationSenderId() != null && data.getConversationSenderId().equals(RongIM.getInstance().getCurrentUserId())
                    && TextUtils.isEmpty(data.getDraft())) {
                Bitmap bitmap = BitmapFactory.decodeResource(view.getResources(), R.drawable.rc_conversation_list_msg_send_failure);
                int width = bitmap.getWidth();
                Drawable drawable = null;
                if (data.getSentStatus() == Message.SentStatus.FAILED && TextUtils.isEmpty(data.getDraft())) {
                    if (ResendManager.getInstance().needResend(data.getLatestMessageId())) {
                        drawable = view.getContext().getResources().getDrawable(R.drawable.rc_conversation_list_msg_sending);
                    } else {
                        drawable = view.getContext().getResources().getDrawable(R.drawable.rc_conversation_list_msg_send_failure);
                    }
                } else if (data.getSentStatus() == Message.SentStatus.SENDING && TextUtils.isEmpty(data.getDraft()))
                    drawable = view.getContext().getResources().getDrawable(R.drawable.rc_conversation_list_msg_sending);
                if (drawable != null) {
                    drawable.setBounds(0, 0, width, width);
                    holder.content.setCompoundDrawablePadding(10);
                    holder.content.setCompoundDrawables(drawable, null, null, null);
                }
            } else {
                holder.content.setCompoundDrawables(null, null, null, null);
            }

            Conversation.ConversationNotificationStatus status = data.getNotificationStatus();
            if (status != null && status.equals(Conversation.ConversationNotificationStatus.DO_NOT_DISTURB))
                holder.notificationBlockImage.setVisibility(View.VISIBLE);
            else
                holder.notificationBlockImage.setVisibility(View.GONE);
        }
    }


    public Spannable getSummary(UIConversation data) {
        return null;
    }

    public String getTitle(String userId) {
        UserInfo userInfo = RongUserInfoManager.getInstance().getUserInfo(userId);
        return userInfo == null ? userId : userInfo.getName();
    }

    @Override
    public Uri getPortraitUri(String userId) {
        UserInfo userInfo = RongUserInfoManager.getInstance().getUserInfo(userId);
        return userInfo == null ? null : userInfo.getPortraitUri();
    }

}

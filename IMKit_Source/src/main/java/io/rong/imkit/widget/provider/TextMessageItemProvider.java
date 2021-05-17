package io.rong.imkit.widget.provider;

import android.content.Context;
import android.content.Intent;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import io.rong.imkit.R;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.RongKitIntent;
import io.rong.imkit.destruct.DestructManager;
import io.rong.imkit.emoticon.AndroidEmoji;
import io.rong.imkit.model.ProviderTag;
import io.rong.imkit.model.UIMessage;
import io.rong.imkit.utils.TextViewUtils;
import io.rong.imkit.widget.ILinkClickListener;
import io.rong.imkit.widget.LinkTextViewMovementMethod;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Message;
import io.rong.message.TextMessage;

@ProviderTag(messageContent = TextMessage.class, showReadState = true)
public class TextMessageItemProvider extends IContainerItemProvider.MessageProvider<TextMessage> {
    private static final String TAG = "TextMessageItemProvider";
    private static final int TAG_MESSAGE_ID = -1;

    private static class ViewHolder {
        TextView message;
        TextView unRead;
        FrameLayout sendFire;
        FrameLayout receiverFire;
        ImageView receiverFireImg;
        TextView receiverFireText;
    }

    @Override
    public View newView(Context context, ViewGroup group) {
        View view = LayoutInflater.from(context).inflate(R.layout.rc_item_destruct_text_message, null);
        ViewHolder holder = new ViewHolder();
        holder.message = view.findViewById(android.R.id.text1);
        holder.unRead = view.findViewById(R.id.tv_unread);
        holder.sendFire = view.findViewById(R.id.fl_send_fire);
        holder.receiverFire = view.findViewById(R.id.fl_receiver_fire);
        holder.receiverFireImg = view.findViewById(R.id.iv_receiver_fire);
        holder.receiverFireText = view.findViewById(R.id.tv_receiver_fire);
        view.setTag(holder);
        return view;
    }

    @Override
    public Spannable getContentSummary(TextMessage data) {
        return null;
    }

    @Override
    public Spannable getContentSummary(Context context, TextMessage data) {
        if (data == null)
            return null;
        if (data.isDestruct()) {
            return new SpannableString(context.getString(R.string.rc_message_content_burn));
        }
        String content = data.getContent();
        if (content != null) {
            if (content.length() > 100) {
                content = content.substring(0, 100);
            }
            return new SpannableString(AndroidEmoji.ensure(content));
        }
        return null;
    }

    @Override
    public void onItemClick(View view, int position, TextMessage content, UIMessage message) {
        ViewHolder holder = (ViewHolder) view.getTag();
        if (content != null && content.isDestruct() && !(message.getMessage().getReadTime() > 0)) {
            holder.unRead.setVisibility(View.GONE);
            holder.message.setVisibility(View.VISIBLE);
            holder.receiverFireText.setVisibility(View.VISIBLE);
            holder.receiverFireImg.setVisibility(View.GONE);
            processTextView(view, position, content, message, holder.message);
            DestructManager.getInstance().startDestruct(message.getMessage());
        }
    }

    @Override
    public void bindView(final View v, int position, TextMessage content, final UIMessage data) {
        ViewHolder holder = (ViewHolder) v.getTag();
        holder.receiverFire.setTag(data.getUId());
        if (data.getMessageDirection() == Message.MessageDirection.SEND) {
            holder.message.setBackgroundResource(R.drawable.rc_ic_bubble_right);
        } else {
            holder.message.setBackgroundResource(R.drawable.rc_ic_bubble_left);
        }
        if (content.isDestruct()) {
            bindFireView(v, position, content, data);
        } else {
            holder.sendFire.setVisibility(View.GONE);
            holder.receiverFire.setVisibility(View.GONE);
            holder.unRead.setVisibility(View.GONE);
            holder.message.setVisibility(View.VISIBLE);
            final TextView textView = holder.message;
            processTextView(v, position, content, data, textView);
        }
    }

    private void processTextView(final View v, final int position, final TextMessage content, final UIMessage data, final TextView pTextView) {
        pTextView.setTag(data.getMessageId());
        if (data.getContentSpannable() == null) {
            SpannableStringBuilder spannable = TextViewUtils.getSpannable(content.getContent(), new TextViewUtils.RegularCallBack() {
                @Override
                public void finish(final SpannableStringBuilder spannable) {
                    pTextView.post(new Runnable() {
                        @Override
                        public void run() {
                            data.setContentSpannable(spannable);
                            if (pTextView.getTag().equals(data.getMessageId())) {
                                pTextView.setText(data.getContentSpannable());
                            }
                        }
                    });
                }
            });
            data.setContentSpannable(spannable);
        }
        pTextView.setText(data.getContentSpannable());
        pTextView.setMovementMethod(new LinkTextViewMovementMethod(new ILinkClickListener() {
            @Override
            public boolean onLinkClick(String link) {
                RongIM.ConversationBehaviorListener listener = RongContext.getInstance().getConversationBehaviorListener();
                RongIM.ConversationClickListener clickListener = RongContext.getInstance().getConversationClickListener();
                boolean result = false;
                if (listener != null) {
                    result = listener.onMessageLinkClick(v.getContext(), link);
                } else if (clickListener != null) {
                    result = clickListener.onMessageLinkClick(v.getContext(), link, data.getMessage());
                }
                if ((listener == null && clickListener == null) || !result) {
                    String str = link.toLowerCase();
                    if (str.startsWith("http") || str.startsWith("https")) {
                        Intent intent = new Intent(RongKitIntent.RONG_INTENT_ACTION_WEBVIEW);
                        intent.setPackage(v.getContext().getPackageName());
                        intent.putExtra("url", link);
                        v.getContext().startActivity(intent);
                        result = true;
                    }
                }
                return result;
            }
        }));
        pTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                v.performClick();
            }
        });
        pTextView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return v.performLongClick();
            }
        });
    }

    private void bindFireView(View pV, int pPosition, TextMessage pContent, final UIMessage pData) {
        ViewHolder holder = (ViewHolder) pV.getTag();
        if (pData.getMessageDirection() == Message.MessageDirection.SEND) {
            holder.sendFire.setVisibility(View.VISIBLE);
            holder.receiverFire.setVisibility(View.GONE);
            holder.unRead.setVisibility(View.GONE);
            holder.message.setVisibility(View.VISIBLE);
            processTextView(pV, pPosition, pContent, pData, holder.message);
        } else {
            holder.sendFire.setVisibility(View.GONE);
            holder.receiverFire.setVisibility(View.VISIBLE);
            DestructManager.getInstance().addListener(pData.getUId(), new DestructListener(holder, pData), TAG);
            //getReadTime>0，证明已读，开始倒计时
            if (pData.getMessage().getReadTime() > 0) {
                holder.unRead.setVisibility(View.GONE);
                holder.message.setVisibility(View.VISIBLE);
                holder.receiverFireText.setVisibility(View.VISIBLE);
                String unFinishTime;
                if (TextUtils.isEmpty(pData.getUnDestructTime())) {
                    unFinishTime = DestructManager.getInstance().getUnFinishTime(pData.getUId());
                } else {
                    unFinishTime = pData.getUnDestructTime();
                }
                holder.receiverFireText.setText(unFinishTime);
                holder.receiverFireImg.setVisibility(View.GONE);
                processTextView(pV, pPosition, pContent, pData, holder.message);
                DestructManager.getInstance().startDestruct(pData.getMessage());
            } else {
                holder.unRead.setVisibility(View.VISIBLE);
                holder.message.setVisibility(View.GONE);
                holder.receiverFireText.setVisibility(View.GONE);
                holder.receiverFireImg.setVisibility(View.VISIBLE);
            }
        }
    }


    private static class DestructListener implements RongIMClient.DestructCountDownTimerListener {
        private WeakReference<ViewHolder> mHolder;
        private UIMessage mUIMessage;

        public DestructListener(ViewHolder pHolder, UIMessage pUIMessage) {
            mHolder = new WeakReference<>(pHolder);
            mUIMessage = pUIMessage;
        }

        @Override
        public void onTick(long millisUntilFinished, String messageId) {
            if (mUIMessage.getUId().equals(messageId)) {
                ViewHolder viewHolder = mHolder.get();
                if (viewHolder != null && messageId.equals(viewHolder.receiverFire.getTag())) {
                    viewHolder.receiverFireText.setVisibility(View.VISIBLE);
                    viewHolder.receiverFireImg.setVisibility(View.GONE);
                    String unDestructTime = String.valueOf(Math.max(millisUntilFinished, 1));
                    viewHolder.receiverFireText.setText(unDestructTime);
                    mUIMessage.setUnDestructTime(unDestructTime);
                }
            }
        }

        @Override
        public void onStop(String messageId) {
            if (mUIMessage.getUId().equals(messageId)) {
                ViewHolder viewHolder = mHolder.get();
                if (viewHolder != null && messageId.equals(viewHolder.receiverFire.getTag())) {
                    viewHolder.receiverFireText.setVisibility(View.GONE);
                    viewHolder.receiverFireImg.setVisibility(View.VISIBLE);
                    mUIMessage.setUnDestructTime(null);
                }
            }
        }

        public void setHolder(ViewHolder pHolder) {
            mHolder = new WeakReference<>(pHolder);
        }

        public void setUIMessage(UIMessage pUIMessage) {
            mUIMessage = pUIMessage;
        }
    }
}

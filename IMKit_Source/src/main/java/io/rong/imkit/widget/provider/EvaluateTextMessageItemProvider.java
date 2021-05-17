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
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import io.rong.common.RLog;
import io.rong.imkit.R;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.RongKitIntent;
import io.rong.imkit.emoticon.AndroidEmoji;
import io.rong.imkit.model.UIMessage;
import io.rong.imkit.utils.TextViewUtils;
import io.rong.imkit.widget.AutoLinkTextView;
import io.rong.imkit.widget.ILinkClickListener;
import io.rong.imkit.widget.LinkTextViewMovementMethod;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Message;
import io.rong.message.TextMessage;

public class EvaluateTextMessageItemProvider extends IContainerItemProvider.MessageProvider<TextMessage> {

    private static final String TAG = "EvaluateTextMessageItemProvider";

    private static class ViewHolder {
        TextView message;
        TextView tv_prompt;
        ImageView iv_yes;
        ImageView iv_no;
        ImageView iv_complete;
        RelativeLayout layout_praise;
        boolean longClick;
    }

    @Override
    public View newView(Context context, ViewGroup group) {
        View view = LayoutInflater.from(context).inflate(R.layout.rc_item_text_message_evaluate, null);

        ViewHolder holder = new ViewHolder();
        holder.message = view.findViewById(R.id.evaluate_text);
        holder.tv_prompt = view.findViewById(R.id.tv_prompt);
        holder.iv_yes = view.findViewById(R.id.iv_yes);
        holder.iv_no = view.findViewById(R.id.iv_no);
        holder.iv_complete = view.findViewById(R.id.iv_complete);
        holder.layout_praise = view.findViewById(R.id.layout_praise);
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

    }

    @Override
    public void bindView(final View v, final int position, final TextMessage content, final UIMessage data) {
        final ViewHolder holder = (ViewHolder) v.getTag();
        if (data.getMessageDirection() == Message.MessageDirection.SEND) {
            v.setBackgroundResource(R.drawable.rc_ic_bubble_right);
        } else {
            v.setBackgroundResource(R.drawable.rc_ic_bubble_left);
        }
        if (data.getEvaluated()) {
            holder.iv_yes.setVisibility(View.GONE);
            holder.iv_no.setVisibility(View.GONE);
            holder.iv_complete.setVisibility(View.VISIBLE);
            holder.tv_prompt.setText("感谢您的评价");
        } else {
            holder.iv_yes.setVisibility(View.VISIBLE);
            holder.iv_no.setVisibility(View.VISIBLE);
            holder.iv_complete.setVisibility(View.GONE);
            holder.tv_prompt.setText("您对我的回答");
        }
        holder.iv_yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String extra = ((TextMessage) data.getContent()).getExtra();
                String knowledgeId = "";
                if (!TextUtils.isEmpty(extra)) {
                    try {
                        JSONObject jsonObj = new JSONObject(extra);
                        knowledgeId = jsonObj.optString("sid");
                    } catch (JSONException e) {
                        RLog.e(TAG, "bindView", e);
                    }
                }
                RongIMClient.getInstance().evaluateCustomService(data.getSenderUserId(), true, knowledgeId);
                holder.iv_complete.setVisibility(View.VISIBLE);
                holder.iv_yes.setVisibility(View.GONE);
                holder.iv_no.setVisibility(View.GONE);
                holder.tv_prompt.setText("感谢您的评价");
                data.setEvaluated(true);
            }
        });

        holder.iv_no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String extra = ((TextMessage) data.getContent()).getExtra();
                String knowledgeId = "";
                if (!TextUtils.isEmpty(extra)) {
                    try {
                        JSONObject jsonObj = new JSONObject(extra);
                        knowledgeId = jsonObj.optString("sid");
                    } catch (JSONException e) {
                        RLog.e(TAG, "bindView", e);
                    }
                }
                RongIMClient.getInstance().evaluateCustomService(data.getSenderUserId(), false, knowledgeId);
                holder.iv_complete.setVisibility(View.VISIBLE);
                holder.iv_yes.setVisibility(View.GONE);
                holder.iv_no.setVisibility(View.GONE);
                holder.tv_prompt.setText("感谢您的评价");
                data.setEvaluated(true);
            }
        });
        final TextView textView = holder.message;
        if (data.getContentSpannable() == null) {
            SpannableStringBuilder spannable = TextViewUtils.getSpannable(content.getContent(), new TextViewUtils.RegularCallBack() {
                @Override
                public void finish(SpannableStringBuilder spannable) {
                    data.setContentSpannable(spannable);
                    if (textView.getTag().equals(data.getMessageId())) {
                        textView.post(new Runnable() {
                            @Override
                            public void run() {
                                textView.setText(data.getContentSpannable());
                            }
                        });
                    }
                }
            });
            data.setContentSpannable(spannable);
        }
        textView.setText(data.getContentSpannable());

        holder.message.setClickable(true);
        holder.message.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
        holder.message.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v1) {
                onItemLongClick(v, position, content, data);
                return false;
            }
        });

        holder.message.setMovementMethod(new LinkTextViewMovementMethod(new ILinkClickListener() {
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
    }
}

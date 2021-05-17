package io.rong.imkit.reference;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.net.Uri;
import androidx.core.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.rong.common.RLog;
import io.rong.imkit.R;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.RongKitIntent;
import io.rong.imkit.activity.FilePreviewActivity;
import io.rong.imkit.activity.PicturePagerActivity;
import io.rong.imkit.emoticon.AndroidEmoji;
import io.rong.imkit.model.GroupUserInfo;
import io.rong.imkit.model.ProviderTag;
import io.rong.imkit.model.UIMessage;
import io.rong.imkit.tools.RongWebviewActivity;
import io.rong.imkit.userInfoCache.RongUserInfoManager;
import io.rong.imkit.utils.TextViewUtils;
import io.rong.imkit.widget.AsyncImageView;
import io.rong.imkit.widget.ILinkClickListener;
import io.rong.imkit.widget.LinkTextViewMovementMethod;
import io.rong.imkit.widget.provider.IContainerItemProvider;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.UserInfo;
import io.rong.message.FileMessage;
import io.rong.message.ImageMessage;
import io.rong.message.ReferenceMessage;
import io.rong.message.RichContentMessage;
import io.rong.message.TextMessage;

/**
 * 引用消息提供者
 * Created by JL on 2018/3/26.
 */
@ProviderTag(messageContent = ReferenceMessage.class, showProgress = false, showReadState = true)
public class ReferenceMessageItemProvider extends IContainerItemProvider.MessageProvider<ReferenceMessage> {

    private static final String TAG = "ReferenceMessageItemProvider";

    @Override
    public View newView(Context context, ViewGroup viewGroup) {
        View view = LayoutInflater.from(context).inflate(R.layout.rc_message_reference, null);
        ViewHolder viewHolder = new ViewHolder();
        viewHolder.containerView = (LinearLayout) view;
        viewHolder.referenceName = view.findViewById(R.id.rc_msg_tv_reference_name);
        viewHolder.referenceFileName = view.findViewById(R.id.rc_msg_tv_reference_file_name);
        viewHolder.referenceContent = view.findViewById(R.id.rc_msg_tv_reference_content);
        viewHolder.referenceSendContent = view.findViewById(R.id.rc_msg_tv_reference_send_content);
        viewHolder.referenceImg = view.findViewById(R.id.rc_msg_iv_reference);
        viewHolder.referenceVerticalMark = view.findViewById(R.id.rc_reference_vertical_mark);
        viewHolder.referenceHorizontalMark = view.findViewById(R.id.rc_reference_horizontal_mark);
        view.setTag(viewHolder);
        return view;
    }

    @Override
    public void bindView(final View view, int position, ReferenceMessage referenceMessage, final UIMessage uiMessage) {
        ViewHolder holder = (ViewHolder) view.getTag();
        holder.referenceName.setText("");
        holder.referenceContent.setText("");
        holder.referenceSendContent.setText("");
        holder.referenceSendContent.setVisibility(View.VISIBLE);
        if (uiMessage.getMessageDirection() == Message.MessageDirection.SEND) {
            holder.containerView.setBackgroundResource(R.drawable.rc_ic_bubble_right);
            Drawable drawable = ContextCompat
                    .getDrawable(view.getContext(), R.drawable.rc_shape_bg_vertical_bar);
            if (drawable == null) {
                return;
            }
            drawable.mutate();
            int color = ContextCompat
                    .getColor(view.getContext(), R.color.rc_reference_sent_mark_bg);
            if (drawable instanceof ShapeDrawable) {
                ((ShapeDrawable) drawable).getPaint().setColor(color);
            } else if (drawable instanceof GradientDrawable) {
                ((GradientDrawable) drawable).setColor(color);
            }
            holder.referenceVerticalMark.setBackground(drawable);
            holder.referenceHorizontalMark.setBackgroundColor(
                    view.getResources().getColor(R.color.rc_reference_sent_mark_bg));
            holder.referenceName.setTextColor(
                    ContextCompat.getColor(view.getContext(), R.color.rc_reference_sent_title));
        } else {
            holder.containerView.setBackgroundResource(R.drawable.rc_ic_bubble_left);
            holder.referenceVerticalMark.setBackgroundResource(R.drawable.rc_shape_bg_vertical_bar);
            holder.referenceHorizontalMark.setBackgroundColor(
                    view.getResources().getColor(R.color.rc_reference_received_mark_bg));
            holder.referenceName.setTextColor(
                    ContextCompat.getColor(view.getContext(), R.color.rc_reference_received_title));
        }
        if (referenceMessage.getUserId() != null) {
            holder.referenceName.setText(getDisplayName(uiMessage, referenceMessage.getUserId()));
        }
        if (referenceMessage.getEditSendText() != null) {
            setTextContent(holder.referenceSendContent, uiMessage, referenceMessage.getEditSendText(), true);
            setMovementMethod(view, uiMessage, holder.referenceSendContent);
        }
        if (referenceMessage.getReferenceContent() == null) {
            return;
        }
        if (referenceMessage.getReferenceContent() instanceof TextMessage) {
            setTextType(view, holder, position, referenceMessage, uiMessage);
            holder.referenceContent.setVisibility(View.VISIBLE);
            holder.referenceContent.setMaxLines(Integer.MAX_VALUE);
            holder.referenceImg.setVisibility(View.GONE);
            holder.referenceFileName.setVisibility(View.GONE);
            setMovementMethod(view, uiMessage, holder.referenceContent);
        } else if (referenceMessage.getReferenceContent() instanceof ImageMessage) {
            setImageType(view, holder, position, referenceMessage, uiMessage);
            holder.referenceContent.setVisibility(View.GONE);
            holder.referenceImg.setVisibility(View.VISIBLE);
            holder.referenceFileName.setVisibility(View.GONE);
        } else if (referenceMessage.getReferenceContent() instanceof FileMessage) {
            setFileType(view, holder, position, referenceMessage, uiMessage);
            holder.referenceContent.setVisibility(View.GONE);
            holder.referenceImg.setVisibility(View.GONE);
            holder.referenceFileName.setVisibility(View.VISIBLE);
        } else if (referenceMessage.getReferenceContent() instanceof RichContentMessage) {
            setRichType(view, holder, position, referenceMessage, uiMessage);
            holder.referenceContent.setVisibility(View.VISIBLE);
            holder.referenceContent.setMaxLines(3);
            holder.referenceContent.setEllipsize(TextUtils.TruncateAt.END);
            holder.referenceImg.setVisibility(View.GONE);
            holder.referenceFileName.setVisibility(View.GONE);
        } else if (referenceMessage.getReferenceContent() instanceof ReferenceMessage) {
            setReferenceType(view, holder, position, referenceMessage, uiMessage);
            holder.referenceContent.setVisibility(View.VISIBLE);
            holder.referenceContent.setMaxLines(Integer.MAX_VALUE);
            holder.referenceImg.setVisibility(View.GONE);
            holder.referenceFileName.setVisibility(View.GONE);
        } else {
            holder.referenceContent.setVisibility(View.VISIBLE);
            holder.referenceContent.setText(R.string.rc_message_unknown);
            holder.referenceImg.setVisibility(View.GONE);
            holder.referenceFileName.setVisibility(View.GONE);
        }
    }

    private void setMovementMethod(final View view, final UIMessage uiMessage, TextView referenceContent) {
        referenceContent.setMovementMethod(new LinkTextViewMovementMethod(new ILinkClickListener() {
            @Override
            public boolean onLinkClick(String link) {
                RongIM.ConversationBehaviorListener listener = RongContext.getInstance().getConversationBehaviorListener();
                RongIM.ConversationClickListener clickListener = RongContext.getInstance().getConversationClickListener();
                boolean result = false;
                if (listener != null) {
                    result = listener.onMessageLinkClick(view.getContext(), link);
                } else if (clickListener != null) {
                    result = clickListener.onMessageLinkClick(view.getContext(), link, uiMessage.getMessage());
                }
                if ((listener == null && clickListener == null) || !result) {
                    String str = link.toLowerCase();
                    if (str.startsWith("http") || str.startsWith("https")) {
                        Intent intent = new Intent(RongKitIntent.RONG_INTENT_ACTION_WEBVIEW);
                        intent.setPackage(view.getContext().getPackageName());
                        intent.putExtra("url", link);
                        view.getContext().startActivity(intent);
                        result = true;
                    }
                }
                return result;
            }
        }));

    }

    private void setRichType(final View view, ViewHolder holder, final int position, final ReferenceMessage referenceMessage, final UIMessage uiMessage) {
        holder.referenceContent.setOnClickListener(null);
        if (referenceMessage == null || referenceMessage.getReferenceContent() == null) {
            return;
        }
        holder.referenceContent.setOnClickListener(null);
        final RichContentMessage content = (RichContentMessage) referenceMessage.getReferenceContent();
        String string = view.getContext().getString(R.string.rc_reference_link) +
                ' ' + content.getTitle();
        SpannableStringBuilder ssb = new SpannableStringBuilder(string);
        ssb.setSpan(new ForegroundColorSpan(view.getContext().getResources().getColor(R.color.rc_reference_text_link_color)),
                0,
                string.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        AndroidEmoji.ensure(ssb);
        holder.referenceContent.setText(ssb);
        holder.referenceContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {

                    Intent intent = new Intent(view.getContext(), RongWebviewActivity.class);
                    intent.setPackage(view.getContext().getPackageName());
                    intent.putExtra("url", content.getUrl());
                    view.getContext().startActivity(intent);
                } catch (Exception e) {
                    RLog.e(TAG, "exception: " + e);
                }
            }
        });
        holder.referenceContent.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return view.performLongClick();
            }
        });
        holder.referenceSendContent.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return view.performLongClick();
            }
        });
    }

    private void setFileType(final View view, final ViewHolder holder, final int position, final ReferenceMessage referenceMessage, final UIMessage uiMessage) {
        holder.referenceContent.setOnClickListener(null);
        if (referenceMessage == null || referenceMessage.getReferenceContent() == null) {
            return;
        }
        holder.referenceContent.setOnClickListener(null);
        final FileMessage content = (FileMessage) referenceMessage.getReferenceContent();
        String string = view.getContext().getString(R.string.rc_search_file_prefix) +
                ' ' + content.getName();

        final SpannableStringBuilder ssb = new SpannableStringBuilder(string);
        ssb.setSpan(new ForegroundColorSpan(view.getContext().getResources().getColor(R.color.rc_reference_text_link_color)),
                0,
                string.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        AndroidEmoji.ensure(ssb);
        holder.referenceFileName.setText(ssb);
        holder.referenceFileName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Intent intent = new Intent();
                    intent.setClass(view.getContext(), FilePreviewActivity.class);
                    intent.setPackage(view.getContext().getPackageName());
                    ReferenceMessage referenceMessage = (ReferenceMessage) uiMessage.getMessage().getContent();
                    FileMessage fileMessage = (FileMessage) referenceMessage.getReferenceContent();
                    intent.putExtra("FileMessage", fileMessage);
                    intent.putExtra("Message", uiMessage.getMessage());
                    intent.putExtra("Progress", uiMessage.getProgress());
                    view.getContext().startActivity(intent);
                } catch (Exception e) {
                    RLog.e(TAG, "exception: " + e);
                }
            }
        });
        holder.referenceFileName.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return view.performLongClick();
            }
        });
        holder.referenceSendContent.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return view.performLongClick();
            }
        });
    }

    private void setImageType(final View view, ViewHolder holder, final int position, final ReferenceMessage referenceMessage, final UIMessage uiMessage) {
        holder.referenceContent.setOnClickListener(null);
        if (referenceMessage == null || referenceMessage.getReferenceContent() == null) {
            return;
        }
        ImageMessage content = (ImageMessage) referenceMessage.getReferenceContent();
        Uri imageUri = null;
        if (content.getThumUri() != null) {
            imageUri = content.getThumUri();
        } else if (content.getLocalPath() != null) {
            imageUri = content.getLocalPath();
        } else if (content.getMediaUrl() != null) {
            imageUri = content.getMediaUrl();
        }
        if (imageUri != null) {
            holder.referenceImg.setResource(content.getThumUri());
        }
        holder.referenceImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Intent intent = new Intent(view.getContext(), PicturePagerActivity.class);
                    intent.setPackage(view.getContext().getPackageName());
                    intent.putExtra("message", uiMessage.getMessage());
                    view.getContext().startActivity(intent);
                } catch (Exception e) {
                    RLog.e(TAG, "setImageType", e);
                }
            }
        });

        holder.referenceImg.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return view.performLongClick();
            }
        });

        holder.referenceSendContent.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return view.performLongClick();
            }
        });
    }

    private void setTextType(final View view, ViewHolder holder, final int position, final ReferenceMessage referenceMessage, final UIMessage uiMessage) {
        holder.referenceContent.setOnClickListener(null);
        if (referenceMessage == null || referenceMessage.getReferenceContent() == null) {
            return;
        }
        if (referenceMessage.getReferenceContent() instanceof TextMessage) {
            setTextContent(holder.referenceContent, uiMessage,((TextMessage) referenceMessage.getReferenceContent()).getContent() , false);
            setReferenceContentAction(view, holder, position, referenceMessage, uiMessage);
        }
    }

    private void setReferenceContentAction(final View view, ViewHolder holder, final int position, final ReferenceMessage referenceMessage, final UIMessage uiMessage) {

        holder.referenceContent.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return view.performLongClick();
            }
        });

        holder.referenceSendContent.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return view.performLongClick();
            }
        });

        holder.referenceContent.setMovementMethod(new LinkTextViewMovementMethod(new ILinkClickListener() {
            @Override
            public boolean onLinkClick(String link) {
                boolean result = false;
                String str = link.toLowerCase();
                if (str.startsWith("http") || str.startsWith("https")) {
                    Intent intent = new Intent(RongKitIntent.RONG_INTENT_ACTION_WEBVIEW);
                    intent.setPackage(view.getContext().getPackageName());
                    intent.putExtra("url", link);
                    view.getContext().startActivity(intent);
                    result = true;
                }
                return result;
            }
        }));
    }


    private void setReferenceType(final View view, ViewHolder holder, final int position, final ReferenceMessage referenceMessage, final UIMessage uiMessage) {
        holder.referenceContent.setOnClickListener(null);
        if (referenceMessage == null || referenceMessage.getReferenceContent() == null) {
            return;
        }
        if (referenceMessage.getReferenceContent() instanceof TextMessage) {
            setTextContent(holder.referenceContent, uiMessage,((TextMessage) referenceMessage.getReferenceContent()).getContent() , false);
            setReferenceContentAction(view, holder, position, referenceMessage, uiMessage);
        }
    }

    @Override
    public Spannable getContentSummary(ReferenceMessage referenceMessage) {
        return new SpannableString(AndroidEmoji.ensure(referenceMessage.getEditSendText()));
    }

    @Override
    public void onItemClick(View view, int i, ReferenceMessage referenceMessage, UIMessage uiMessage) {
    }


    private void setTextContent(final TextView textView, final UIMessage data, String content, boolean isSendContent) {
        textView.setTag(data.getMessageId());
        if (isSendContent) {
            if (data.getContentSpannable() == null) {
                SpannableStringBuilder spannable = TextViewUtils.getSpannable(content, new TextViewUtils.RegularCallBack() {
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
        } else {
            if (data.getReferenceContentSpannable() == null) {
                SpannableStringBuilder spannable = TextViewUtils.getSpannable(content, new TextViewUtils.RegularCallBack() {
                    @Override
                    public void finish(SpannableStringBuilder spannable) {
                        data.setReferenceContentSpannable(spannable);
                        if (textView.getTag().equals(data.getMessageId())) {
                            textView.post(new Runnable() {
                                @Override
                                public void run() {
                                    textView.setText(data.getReferenceContentSpannable());
                                }
                            });
                        }
                    }
                });
                data.setReferenceContentSpannable(spannable);
            }
            textView.setText(data.getReferenceContentSpannable());
        }
    }

    private String getDisplayName(UIMessage uiMessage, String referenceUserId) {
        if (uiMessage.getSenderUserId() != null) {
            if (uiMessage.getConversationType().equals(Conversation.ConversationType.GROUP)) {
                GroupUserInfo groupUserInfo = RongUserInfoManager.getInstance().getGroupUserInfo(uiMessage.getTargetId(), referenceUserId);
                if (groupUserInfo != null) {
                    return groupUserInfo.getNickname();
                }
            }
            UserInfo userInfo = RongUserInfoManager.getInstance().getUserInfo(referenceUserId);
            if (userInfo != null) {
                return userInfo.getName();
            }
        }
        return null;
    }

    private static class ViewHolder {
        LinearLayout containerView;
        TextView referenceName;
        TextView referenceFileName;
        TextView referenceContent;
        TextView referenceSendContent;
        AsyncImageView referenceImg;
        View referenceVerticalMark;
        View referenceHorizontalMark;
    }
}

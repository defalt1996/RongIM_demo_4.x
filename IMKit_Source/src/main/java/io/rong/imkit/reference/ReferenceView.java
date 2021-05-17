package io.rong.imkit.reference;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import androidx.core.content.ContextCompat;
import android.text.SpannableStringBuilder;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import io.rong.common.RLog;
import io.rong.imkit.R;
import io.rong.imkit.activity.FilePreviewActivity;
import io.rong.imkit.activity.PicturePagerActivity;
import io.rong.imkit.emoticon.AndroidEmoji;
import io.rong.imkit.model.GroupUserInfo;
import io.rong.imkit.model.UIMessage;
import io.rong.imkit.userInfoCache.RongUserInfoManager;
import io.rong.imkit.widget.AsyncImageView;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.UserInfo;
import io.rong.message.FileMessage;
import io.rong.message.ImageMessage;
import io.rong.message.ReferenceMessage;
import io.rong.message.RichContentMessage;
import io.rong.message.TextMessage;

/**
 * 引用控件
 * Created by JL on 2018/3/28.
 */
public class ReferenceView extends FrameLayout {

    private static final String TAG = "ConversationFragment";
    private static final int DEFAULT_MAX_HEIGHT = 238; // 最大高度设置
    private int maxHeight = DEFAULT_MAX_HEIGHT;
    private TextView tvReferenceName;
    private TextView tvReferenceContent;
    private AsyncImageView imgReferenceContent;
    private CancelListener cancelListener;

    public ReferenceView(Context context) {
        super(context);
        init();
    }

    public ReferenceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(context, attrs);
        init();
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.ReferenceView);
        final int count = a.getIndexCount();
        for (int i = 0; i < count; ++i) {
            int attr = a.getIndex(i);
            if (attr == R.styleable.ReferenceView_RCMaxHeight) {
                maxHeight = a.getDimensionPixelOffset(attr, DEFAULT_MAX_HEIGHT);
            }
        }
        a.recycle();
    }

    private void init() {
        inflate(getContext(), R.layout.rc_view_reference, this);
        ImageButton imgCancel = findViewById(R.id.rc_view_iv_reference_cancel);
        tvReferenceName = findViewById(R.id.rc_view_tv_reference_name);
        tvReferenceContent = findViewById(R.id.rc_view_tv_reference_content);
        imgReferenceContent = findViewById(R.id.rc_view_iv_reference_content);

        imgCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cancelListener != null) {
                    cancelListener.cancelClick();
                }
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        maxHeight = (int) getResources().getDimension(R.dimen.rc_reference_dimen_size_103);
        if (heightMode == MeasureSpec.EXACTLY) {
            heightSize = heightSize <= maxHeight ? heightSize : maxHeight;
        }
        if (heightMode == MeasureSpec.UNSPECIFIED) {
            heightSize = heightSize <= maxHeight ? heightSize : maxHeight;
        }
        if (heightMode == MeasureSpec.AT_MOST) {
            heightSize = heightSize <= maxHeight ? heightSize : maxHeight;
        }
        int maxHeightMeasureSpec = MeasureSpec.makeMeasureSpec(heightSize,
                heightMode);
        super.onMeasure(widthMeasureSpec, maxHeightMeasureSpec);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;    // 消费掉触摸事件，以防传回到父页面
    }

    /**
     * 设置消息内容 UI
     *
     * @param uiMessage 消息
     * @return ReferenceMessage
     */
    public ReferenceMessage setMessageContent(UIMessage uiMessage) {
        if (tvReferenceContent != null) {
            tvReferenceContent.setOnClickListener(null);
        }
        if (uiMessage != null) {
            ReferenceMessage referenceMessage = null;
            if (uiMessage.getContent() instanceof TextMessage) {
                //文本消息
                referenceMessage = makeTextMessage(uiMessage);
            } else if (uiMessage.getContent() instanceof ImageMessage) {
                //图片消息
                referenceMessage = makeImageMessage(uiMessage);
            } else if (uiMessage.getContent() instanceof FileMessage) {
                //文件消息
                referenceMessage = makeFileMessage(uiMessage);
            } else if (uiMessage.getContent() instanceof RichContentMessage) {
                //图文消息
                referenceMessage = makeRichContentMessage(uiMessage);
            } else if (uiMessage.getContent() instanceof ReferenceMessage) {
                //引用消息
                referenceMessage = makeReferenceMessage(uiMessage);
            }
            return referenceMessage;
        }
        return null;
    }

    /**
     * 创建引用消息
     *
     * @param uiMessage 消息
     */
    private ReferenceMessage makeReferenceMessage(UIMessage uiMessage) {
        ReferenceMessage message = (ReferenceMessage) uiMessage.getContent();
        if (uiMessage.getSenderUserId() != null) {
            tvReferenceName.setText(getDisplayName(uiMessage));
            SpannableStringBuilder span = createSpan(message.getEditSendText());
            setTextContent(tvReferenceContent, span);
            tvReferenceContent.setTextColor(ContextCompat.getColor(getContext(), R.color.rc_reference_normal_text_color));
            tvReferenceContent.setVisibility(VISIBLE);
            imgReferenceContent.setVisibility(GONE);
            TextMessage obtain = TextMessage.obtain(message.getEditSendText());//引用类型 使用的是 TextMessage 进行发送
            return ReferenceMessage.obtainMessage(uiMessage.getSenderUserId(), obtain);
        }
        return null;
    }

    /**
     * 创建图文消息
     *
     * @param uiMessage 消息
     */
    private ReferenceMessage makeRichContentMessage(UIMessage uiMessage) {
        RichContentMessage richContentMessage = (RichContentMessage) uiMessage.getContent();
        if (uiMessage.getSenderUserId() != null) {
            if (richContentMessage.getTitle() != null && richContentMessage.getUrl() != null) {
                tvReferenceName.setText(getDisplayName(uiMessage));

                String string = getResources().getString(R.string.rc_reference_link) +
                        ' ' + richContentMessage.getTitle();
                SpannableStringBuilder ssb = new SpannableStringBuilder(string);
                AndroidEmoji.ensure(ssb);

                tvReferenceContent.setText(ssb);
                tvReferenceContent.setTextColor(ContextCompat.getColor(getContext(), R.color.rc_reference_text_link_color));
                tvReferenceContent.setVisibility(VISIBLE);
                imgReferenceContent.setVisibility(GONE);
                return ReferenceMessage.obtainMessage(uiMessage.getSenderUserId(), uiMessage.getContent());
            }
        }
        return null;
    }

    /**
     * 创建文件消息
     *
     * @param uiMessage 消息
     * @return ReferenceMessage
     */
    private ReferenceMessage makeFileMessage(final UIMessage uiMessage) {
        final FileMessage fileMessage = (FileMessage) uiMessage.getContent();
        if (uiMessage.getSenderUserId() != null) {
            if (fileMessage.getFileUrl() != null) {
                tvReferenceName.setText(getDisplayName(uiMessage));

                String string = getResources().getString(R.string.rc_reference_file) +
                        ' ' + fileMessage.getName();
                SpannableStringBuilder ssb = new SpannableStringBuilder(string);
                AndroidEmoji.ensure(ssb);

                tvReferenceContent.setText(ssb);
                tvReferenceContent.setTextColor(ContextCompat.getColor(getContext(), R.color.rc_reference_text_link_color));
                tvReferenceContent.setVisibility(VISIBLE);
                imgReferenceContent.setVisibility(GONE);
                tvReferenceContent.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            Intent intent = new Intent();
                            intent.setClass(view.getContext(), FilePreviewActivity.class);
                            intent.setPackage(view.getContext().getPackageName());
                            intent.putExtra("FileMessage", fileMessage);
                            intent.putExtra("Message", uiMessage.getMessage());
                            intent.putExtra("Progress", uiMessage.getProgress());
                            view.getContext().startActivity(intent);
                        } catch (Exception e) {
                            RLog.e(TAG, "exception: " + e);
                        }
                    }
                });
                return ReferenceMessage.obtainMessage(uiMessage.getSenderUserId(), uiMessage.getContent());
            }
        }

        return null;
    }


    /**
     * 创建图片消息
     *
     * @param uiMessage 消息
     * @return ReferenceMessage
     */
    private ReferenceMessage makeImageMessage(final UIMessage uiMessage) {
        ImageMessage imageMessage = (ImageMessage) uiMessage.getContent();
        if (uiMessage.getSenderUserId() != null) {
            if (imageMessage.getRemoteUri() != null) {
                tvReferenceName.setText(getDisplayName(uiMessage));
                tvReferenceContent.setVisibility(GONE);
                imgReferenceContent.setVisibility(VISIBLE);
                imgReferenceContent.setResource(imageMessage.getThumUri());
                imgReferenceContent.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            Intent intent = new Intent(view.getContext(), PicturePagerActivity.class);
                            intent.setPackage(view.getContext().getPackageName());
                            intent.putExtra("message", uiMessage.getMessage());
                            view.getContext().startActivity(intent);
                        } catch (Exception e) {
                            RLog.e(TAG, "exception: " + e);
                        }
                    }
                });
                return ReferenceMessage.obtainMessage(uiMessage.getSenderUserId(), uiMessage.getContent());
            }
        }
        return null;
    }

    /**
     * 创建文本消息
     *
     * @param uiMessage 消息
     * @return ReferenceMessage
     */
    private ReferenceMessage makeTextMessage(final UIMessage uiMessage) {
        TextMessage textMessage = (TextMessage) uiMessage.getContent();
        if (uiMessage.getSenderUserId() != null && textMessage.getContent() != null) {
            tvReferenceName.setText(getDisplayName(uiMessage));
            SpannableStringBuilder span = createSpan(textMessage.getContent());
            setTextContent(tvReferenceContent, span);
            tvReferenceContent.setTextColor(ContextCompat.getColor(getContext(), R.color.rc_reference_normal_text_color));
            tvReferenceContent.setVisibility(VISIBLE);
            imgReferenceContent.setVisibility(GONE);
            return ReferenceMessage.obtainMessage(uiMessage.getSenderUserId(), uiMessage.getContent());
        }
        return null;
    }

    private SpannableStringBuilder createSpan(String content) {
        SpannableStringBuilder spannable = new SpannableStringBuilder(content);
        AndroidEmoji.ensure(spannable);
        return spannable;
    }

    private void setTextContent(final TextView textView, final SpannableStringBuilder spannable) {
        int len = spannable.length();
        if (getHandler() != null && len > 500) {
            getHandler().postDelayed(new Runnable() {
                public void run() {
                    textView.setText(spannable);
                }
            }, 50L);
        } else {
            textView.setText(spannable);
        }
    }

    /**
     * 关闭引用 View 调用
     */
    public void clearReference() {
        tvReferenceName.setText("");
        tvReferenceContent.setText("");
        imgReferenceContent.setImageDrawable(null);
        this.setVisibility(GONE);
    }


    public interface CancelListener {
        void cancelClick();
    }

    public void setCancelListener(CancelListener cancelListener) {
        this.cancelListener = cancelListener;
    }

    private String getDisplayName(UIMessage uiMessage) {
        if (uiMessage.getSenderUserId() != null) {
            if (uiMessage.getConversationType().equals(Conversation.ConversationType.GROUP)) {
                GroupUserInfo groupUserInfo = RongUserInfoManager.getInstance().getGroupUserInfo(uiMessage.getTargetId(), uiMessage.getSenderUserId());
                if (groupUserInfo != null) {
                    return groupUserInfo.getNickname();
                }
            }
            UserInfo userInfo = RongUserInfoManager.getInstance().getUserInfo(uiMessage.getSenderUserId());
            if (userInfo != null) {
                return userInfo.getName();
            }
        }
        return null;
    }

}

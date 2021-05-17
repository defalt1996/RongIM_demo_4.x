package io.rong.imkit.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import androidx.annotation.NonNull;
import android.text.Layout;
import android.text.Spannable;
import android.text.TextPaint;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.widget.TextView;

import io.rong.imkit.R;

/**
 * Created by jiangecho on 2016/10/31.
 */

public class AutoLinkTextView extends TextView {
    private int mMaxWidth;

    public AutoLinkTextView(Context context) {
        super(context);
    }

    public AutoLinkTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initWidth(context,attrs);
        setAutoLinkMask(Linkify.WEB_URLS|Linkify.EMAIL_ADDRESSES|Linkify.PHONE_NUMBERS);
    }

    public AutoLinkTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initWidth(context,attrs);
        setAutoLinkMask(Linkify.WEB_URLS|Linkify.EMAIL_ADDRESSES|Linkify.PHONE_NUMBERS);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public AutoLinkTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initWidth(context,attrs);
        setAutoLinkMask(Linkify.WEB_URLS|Linkify.EMAIL_ADDRESSES|Linkify.PHONE_NUMBERS);
    }

    private void initWidth(Context context,AttributeSet attrs){
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.AutoLinkTextView);
        mMaxWidth = array.getDimensionPixelSize(R.styleable.AutoLinkTextView_RCMaxWidth, 0);
        setMaxWidth(mMaxWidth);
        array.recycle();
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Layout layout = getLayout();
        float width = 0;
        for (int i = 0; i < layout.getLineCount(); i++) {
            width = Math.max(width, layout.getLineWidth(i));
        }
        width += getCompoundPaddingLeft() + getCompoundPaddingRight();
        if (getBackground() != null) {
            width = Math.max(width, getBackground().getIntrinsicWidth());
        }
        if (mMaxWidth != 0) {
            width = Math.min(width, mMaxWidth);
        }
        setMeasuredDimension((int) Math.ceil(width), getMeasuredHeight());
    }

    /**
     * 取消文本中超链接的下划线
     * 需要每次在setText之后调用，因为在setAutoLinkMask后，每次setText会重新替换成原始的UrlSpan导致下划线再次生成
     * setMovementMethod内部也会调用setText，所以也要在setMovementMethod之后调用此方法
     */
    public void stripUnderlines() {
        TextView textView = this;
        if (textView.getText() instanceof Spannable) {
            Spannable s = (Spannable) textView.getText();
            URLSpan[] spans = s.getSpans(0, s.length(), URLSpan.class);
            for (URLSpan span : spans) {
                int start = s.getSpanStart(span);
                int end = s.getSpanEnd(span);
                s.removeSpan(span);
                span = new URLSpanNoUnderline(span.getURL());
                s.setSpan(span, start, end, 0);
            }
        }
    }

    /**
     * 取消超链接下划线的UrlSpan
     */
    public static class URLSpanNoUnderline extends URLSpan {
        public URLSpanNoUnderline(String url) {
            super(url);
        }

        @Override
        public void updateDrawState(@NonNull TextPaint ds) {
            super.updateDrawState(ds);
            ds.setUnderlineText(false);
        }
    }
}

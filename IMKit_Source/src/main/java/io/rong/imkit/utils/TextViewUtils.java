package io.rong.imkit.utils;

import android.text.SpannableStringBuilder;
import android.text.style.URLSpan;
import android.text.util.Linkify;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import io.rong.imkit.emoticon.AndroidEmoji;
import io.rong.imkit.widget.AutoLinkTextView;

/**
 * 处理 textMessage 和 ReferenceMessage 的文本内容
 */
public class TextViewUtils {

    /**
     * 字符串校验限制，大于等于 150 字符开线程校验，否则在当前线程校验
     */
    private static final int CONTENT_LIMIT_LENGTH = 150;
    private static RegularExecutor regularExecutor = new RegularExecutor();


    /**
     * @param content 替换内容
     * @param callBack 异步请求回调
     * @return 内容的 spannable
     */
    public static SpannableStringBuilder getSpannable(String content, final RegularCallBack callBack) {
        if (content == null) {
            return new SpannableStringBuilder("");
        }
        SpannableStringBuilder spannable = new SpannableStringBuilder(content);
        final SpannableStringBuilder emojiSpannable = AndroidEmoji.replaceEmojiWithText(spannable);
        AndroidEmoji.ensure(emojiSpannable);
        if (spannable.length() < CONTENT_LIMIT_LENGTH) {
            regularContent(emojiSpannable);
        } else {
            regularExecutor.execute(new Runnable() {
                                        @Override
                                        public void run() {
                                            regularContent(emojiSpannable);
                                            callBack.finish(emojiSpannable);
                                        }
                                    }
            );
        }
        return emojiSpannable;
    }


    private static void regularContent(SpannableStringBuilder spannable) {
        Linkify.addLinks(spannable, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES | Linkify.PHONE_NUMBERS);
        URLSpan[] spans = spannable.getSpans(0, spannable.length(), URLSpan.class);
        for (URLSpan span : spans) {
            int start = spannable.getSpanStart(span);
            int end = spannable.getSpanEnd(span);
            spannable.removeSpan(span);
            span = new AutoLinkTextView.URLSpanNoUnderline(span.getURL());
            spannable.setSpan(span, start, end, 0);
        }
    }

    /**
     * 无核心线程，使用后 60 秒自动释放
     */
    private static class RegularExecutor implements Executor {
        private final Executor mCompressExecutor;

        public RegularExecutor() {
            mCompressExecutor = Executors.newCachedThreadPool();
        }

        @Override
        public void execute(Runnable command) {
            mCompressExecutor.execute(command);
        }
    }

    public interface RegularCallBack {
        void finish(SpannableStringBuilder spannable);
    }

}

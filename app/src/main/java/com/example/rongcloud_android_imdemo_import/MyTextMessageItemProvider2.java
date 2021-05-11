package com.example.rongcloud_android_imdemo_import;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;

import io.rong.imkit.emoticon.AndroidEmoji;
import io.rong.imkit.model.ProviderTag;
import io.rong.imkit.widget.provider.TextMessageItemProvider;
import io.rong.message.TextMessage;

@ProviderTag(
        messageContent = TextMessage.class,
        showReadState = true
)
public class MyTextMessageItemProvider2 extends TextMessageItemProvider {

    @Override
    public Spannable getContentSummary(Context context, TextMessage data) {
        if (data == null) {
            return null;
        }
        if (data.isDestruct()) {
            return new SpannableString(context.getString(R.string.rc_message_content_burn));
        }
        String content = data.getContent();
        if (content != null) {
            if (content.length() > 100) {
                content = content.substring(0, 100);
            }
            content = content + "test getContentSummary";
            return new SpannableString(AndroidEmoji.ensure(content));
        }
        return null;
    }
}

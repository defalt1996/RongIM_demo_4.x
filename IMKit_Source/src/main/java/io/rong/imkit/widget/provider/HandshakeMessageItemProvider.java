package io.rong.imkit.widget.provider;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.View;
import android.view.ViewGroup;

import io.rong.imkit.emoticon.AndroidEmoji;
import io.rong.imkit.model.ProviderTag;
import io.rong.imkit.model.UIMessage;
import io.rong.message.HandshakeMessage;

@ProviderTag(messageContent = HandshakeMessage.class , showPortrait = false, centerInHorizontal = true, hide = true)
public class HandshakeMessageItemProvider extends IContainerItemProvider.MessageProvider<HandshakeMessage> {


    @Override
    public View newView(Context context, ViewGroup group) {
        return null;
    }

    @Override
    public Spannable getContentSummary(HandshakeMessage data) {
        return null;
    }

    @Override
    public Spannable getContentSummary(Context context, HandshakeMessage data) {
        if (data != null && data.getContent() != null)
            return new SpannableString(AndroidEmoji.ensure(data.getContent()));
        return null;
    }

    @Override
    public void onItemClick(View view, int position, HandshakeMessage content, UIMessage message) {

    }

    @Override
    public void bindView(View v, int position, HandshakeMessage content, UIMessage data) {

    }
}

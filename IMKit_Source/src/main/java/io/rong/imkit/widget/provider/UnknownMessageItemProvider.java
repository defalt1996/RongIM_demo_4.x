package io.rong.imkit.widget.provider;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import io.rong.imkit.R;
import io.rong.imkit.model.ProviderTag;
import io.rong.imkit.model.UIMessage;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.UnknownMessage;

@ProviderTag(messageContent = UnknownMessage.class, showPortrait = false, showWarning = false, centerInHorizontal = true, showSummaryWithName = false)
public class UnknownMessageItemProvider extends IContainerItemProvider.MessageProvider<MessageContent> {

    @Override
    public void bindView(View v, int position, MessageContent content, UIMessage message) {
        ViewHolder viewHolder = (ViewHolder) v.getTag();
        viewHolder.contentTextView.setText(R.string.rc_message_unknown);
    }

    @Override
    public Spannable getContentSummary(MessageContent data) {
        return null;
    }

    @Override
    public Spannable getContentSummary(Context context, MessageContent data) {
        return new SpannableString(context.getResources().getString(R.string.rc_message_unknown));
    }

    @Override
    public void onItemClick(View view, int position, MessageContent
            content, UIMessage message) {
    }

    @Override
    public View newView(Context context, ViewGroup group) {
        View view = LayoutInflater.from(context).inflate(R.layout.rc_item_information_notification_message, null);
        ViewHolder viewHolder = new ViewHolder();
        viewHolder.contentTextView = view.findViewById(R.id.rc_msg);
        viewHolder.contentTextView.setMovementMethod(LinkMovementMethod.getInstance());
        view.setTag(viewHolder);

        return view;
    }


    private static class ViewHolder {
        TextView contentTextView;
    }

    @Override
    public void onItemLongClick(final View view, final int position, final MessageContent content, final UIMessage message) {
        // 未知消息不支持长按弹窗
    }
}
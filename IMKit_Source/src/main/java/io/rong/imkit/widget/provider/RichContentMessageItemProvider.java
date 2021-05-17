package io.rong.imkit.widget.provider;

import android.content.Context;
import android.content.Intent;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import io.rong.imkit.R;
import io.rong.imkit.RongKitIntent;
import io.rong.imkit.model.ProviderTag;
import io.rong.imkit.model.UIMessage;
import io.rong.imkit.widget.AsyncImageView;
import io.rong.imlib.model.Message;
import io.rong.message.RichContentMessage;

@ProviderTag(messageContent = RichContentMessage.class, showReadState = true)
public class RichContentMessageItemProvider extends IContainerItemProvider.MessageProvider<RichContentMessage> {

    private static class ViewHolder {
        AsyncImageView img;
        TextView title;
        TextView content;
        RelativeLayout mLayout;
    }

    @Override
    public View newView(Context context, ViewGroup group) {
        View view = LayoutInflater.from(context).inflate(R.layout.rc_item_rich_content_message, null);
        ViewHolder holder = new ViewHolder();
        holder.title = view.findViewById(R.id.rc_title);
        holder.content = view.findViewById(R.id.rc_content);
        holder.img = view.findViewById(R.id.rc_img);
        holder.mLayout = view.findViewById(R.id.rc_layout);
        view.setTag(holder);
        return view;
    }

    @Override
    public void onItemClick(View view, int position, RichContentMessage content, UIMessage message) {

        String action = RongKitIntent.RONG_INTENT_ACTION_WEBVIEW;
        Intent intent = new Intent(action);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("url", content.getUrl());
        intent.setPackage(view.getContext().getPackageName());
        view.getContext().startActivity(intent);
    }

    @Override
    public void bindView(View v, int position, RichContentMessage content, UIMessage message) {
        ViewHolder holder = (ViewHolder) v.getTag();
        holder.title.setText(content.getTitle());
        holder.content.setText(content.getContent());
        if (content.getImgUrl() != null) {
            holder.img.setResource(content.getImgUrl(), 0);
        }

        if (message.getMessageDirection() == Message.MessageDirection.SEND) {
            holder.mLayout.setBackgroundResource(R.drawable.rc_ic_bubble_right_file);
        } else {
            holder.mLayout.setBackgroundResource(R.drawable.rc_ic_bubble_left_file);
        }
    }

    @Override
    public Spannable getContentSummary(RichContentMessage data) {
        return null;
    }

    @Override
    public Spannable getContentSummary(Context context, RichContentMessage data) {
        String text = context.getResources().getString(R.string.rc_message_content_rich_text);
        return new SpannableString(text);
    }
}

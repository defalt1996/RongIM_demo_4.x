package io.rong.imkit.widget.provider;

import android.content.Context;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import io.rong.imkit.R;
import io.rong.imkit.model.ProviderTag;
import io.rong.imkit.model.UIMessage;
import io.rong.message.HistoryDividerMessage;

@ProviderTag(messageContent = HistoryDividerMessage.class, showPortrait = false, centerInHorizontal = true, showProgress = false, showSummaryWithName = false)
public class HistoryDividerMessageProvider extends IContainerItemProvider.MessageProvider<HistoryDividerMessage> {
    @Override
    public void bindView(View view, int i, HistoryDividerMessage newMessageDivider, UIMessage uiMessage) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        viewHolder.contentTextView.setText(newMessageDivider.getContent());

    }

    @Override
    public Spannable getContentSummary(HistoryDividerMessage historyDividerMessage) {
        return null;
    }

    @Override
    public void onItemClick(View view, int i, HistoryDividerMessage historyDividerMessage, UIMessage uiMessage) {

    }

    @Override
    public void onItemLongClick(View view, int i, HistoryDividerMessage historyDividerMessage, UIMessage uiMessage) {

    }

    @Override
    public View newView(Context context, ViewGroup viewGroup) {
        View view = LayoutInflater.from(context).inflate(R.layout.rc_item_new_message_divider, null);
        ViewHolder viewHolder = new ViewHolder();
        viewHolder.contentTextView = view.findViewById(R.id.tv_divider_message);
        viewHolder.contentTextView.setMovementMethod(LinkMovementMethod.getInstance());
        view.setTag(viewHolder);
        return view;
    }

    private static class ViewHolder {
        TextView contentTextView;
    }

}

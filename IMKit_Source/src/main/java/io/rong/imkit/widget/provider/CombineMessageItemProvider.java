package io.rong.imkit.widget.provider;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.List;

import io.rong.common.FileUtils;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.RongKitIntent;
import io.rong.imkit.model.ProviderTag;
import io.rong.imkit.model.UIMessage;
import io.rong.imkit.tools.CombineWebViewActivity;
import io.rong.imkit.utilities.RongUtils;
import io.rong.imkit.utils.CombineMessageUtils;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;

import io.rong.imkit.R;
import io.rong.imkit.message.CombineMessage;


@ProviderTag(messageContent = CombineMessage.class, showReadState = true)
public class CombineMessageItemProvider extends IContainerItemProvider.MessageProvider<CombineMessage> {

    private static class ViewHolder {
        LinearLayout message;
        TextView title;
        TextView summary;
    }

    @Override
    public View newView(Context context, ViewGroup group) {
        View view = LayoutInflater.from(context).inflate(R.layout.rc_item_combine_message, null);
        ViewHolder holder = new ViewHolder();
        holder.message = view.findViewById(R.id.rc_message);
        holder.title = view.findViewById(R.id.title);
        holder.summary = view.findViewById(R.id.summary);
        view.setTag(holder);
        return view;
    }

    @Override
    public void bindView(View v, int position, CombineMessage content, UIMessage message) {
        final ViewHolder holder = (ViewHolder) v.getTag();
        if (message.getMessageDirection() == Message.MessageDirection.SEND) {
            holder.message.setBackgroundResource(R.drawable.rc_ic_bubble_right);
        } else {
            holder.message.setBackgroundResource(R.drawable.rc_ic_bubble_left);
        }

        String title = getTitle(content);
        content.setTitle(title);
        holder.title.setText(content.getTitle());

        StringBuilder summary = new StringBuilder();
        List<String> summarys = content.getSummaryList();
        for (int i = 0; i < summarys.size() && i < 4; i++) {
            if (i == 0) {
                summary = new StringBuilder(summarys.get(i));
            } else {
                summary.append("\n").append(summarys.get(i));
            }
        }
        holder.summary.setText(summary.toString());
    }

    private String getTitle(CombineMessage content) {
        String title = "";
        Context context = RongIM.getInstance().getApplicationContext();
        if (Conversation.ConversationType.GROUP.equals(content.getConversationType())) {
            title = context.getString(R.string.rc_combine_group_chat);
        } else {
            List<String> nameList = content.getNameList();
            if (nameList == null) return title;

            if (nameList.size() == 1) {
                title = String.format(context.getString(R.string.rc_combine_the_group_chat_of), nameList.get(0));
            } else if (nameList.size() == 2) {
                title = String.format(context.getString(R.string.rc_combine_the_group_chat_of),
                        nameList.get(0) + " " + context.getString(R.string.rc_combine_and) + " " + nameList.get(1));
            }
        }
        if (TextUtils.isEmpty(title)) {
            title = context.getString(R.string.rc_combine_chat_history);
        }
        return title;
    }

    @Override
    public Spannable getContentSummary(CombineMessage data) {
        return null;
    }

    @Override
    public Spannable getContentSummary(Context context, CombineMessage data) {
        return new SpannableString(context.getString(R.string.rc_message_content_combine));
    }

    @Override
    public void onItemClick(View view, int position, CombineMessage content, UIMessage message) {

        String type = CombineWebViewActivity.TYPE_LOCAL;
        Uri uri = content.getLocalPath();
        if ((uri == null || !new File(uri.toString().substring(7)).exists())
                && content.getMediaUrl() != null) {
            String filePath = CombineMessageUtils.getInstance().getCombineFilePath(content.getMediaUrl().toString());
            if (new File(filePath).exists()) {
                uri = Uri.parse("file://" + filePath);
            } else {
                uri = content.getMediaUrl();
                type = CombineWebViewActivity.TYPE_MEDIA;
            }
        }

        if (uri == null) {
            Context context = view.getContext();
            new AlertDialog.Builder(context)
                    .setMessage(context.getString(R.string.rc_combine_history_deleted))
                    .setPositiveButton(context.getString(R.string.rc_dialog_ok), null)
                    .show();
            return;
        }

        Intent intent = new Intent(RongKitIntent.RONG_INTENT_ACTION_COMBINEWEBVIEW);
        intent.setPackage(view.getContext().getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("messageId", message.getMessageId());
        intent.putExtra("uri", uri.toString());
        intent.putExtra("type", type);
        intent.putExtra("title", content.getTitle());
        view.getContext().startActivity(intent);
    }
}

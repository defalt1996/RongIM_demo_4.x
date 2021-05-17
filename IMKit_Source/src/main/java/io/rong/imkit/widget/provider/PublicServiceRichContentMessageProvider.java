package io.rong.imkit.widget.provider;

import android.content.Context;
import android.content.Intent;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

import io.rong.common.RLog;
import io.rong.imkit.R;
import io.rong.imkit.RongKitIntent;
import io.rong.imkit.model.ProviderTag;
import io.rong.imkit.model.UIMessage;
import io.rong.imkit.utilities.RongUtils;
import io.rong.imkit.widget.AsyncImageView;
import io.rong.message.PublicServiceRichContentMessage;
import io.rong.message.RichContentItem;

/**
 * Created by weiqinxiao on 15/4/18.
 */
@ProviderTag(messageContent = PublicServiceRichContentMessage.class, showPortrait = false, centerInHorizontal = true, showSummaryWithName = false)
public class PublicServiceRichContentMessageProvider extends IContainerItemProvider.MessageProvider<PublicServiceRichContentMessage> {
    private final String TAG = getClass().getSimpleName();

    @Override
    public View newView(Context context, ViewGroup group) {
        ViewHolder holder = new ViewHolder();
        View view = LayoutInflater.from(context).inflate(R.layout.rc_item_public_service_rich_content_message, null);

        holder.title = view.findViewById(R.id.rc_title);
        holder.time = view.findViewById(R.id.rc_time);
        holder.description = view.findViewById(R.id.rc_content);
        holder.imageView = view.findViewById(R.id.rc_img);

        int w = RongUtils.getScreenWidth() - RongUtils.dip2px(32);
        view.setLayoutParams(new ViewGroup.LayoutParams(w, LinearLayout.LayoutParams.WRAP_CONTENT));
        view.setTag(holder);
        return view;
    }

    @Override
    public void bindView(View v, int position, PublicServiceRichContentMessage content, UIMessage message) {
        ViewHolder holder = (ViewHolder) v.getTag();

        PublicServiceRichContentMessage msg = (PublicServiceRichContentMessage) message.getContent();

        if (msg.getMessage() != null) {
            holder.title.setText(msg.getMessage().getTitle());
            holder.description.setText(msg.getMessage().getDigest());
            holder.imageView.setResource(msg.getMessage().getImageUrl(), 0);
        }
        String time = formatDate(message.getReceivedTime(), "MM月dd日 HH:mm");
        holder.time.setText(time);
    }

    private String formatDate(long timeMillis, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(new Date(timeMillis));
    }

    @Override
    public Spannable getContentSummary(PublicServiceRichContentMessage data) {
        return null;
    }

    @Override
    public Spannable getContentSummary(Context context, PublicServiceRichContentMessage data) {
        if (data != null && data.getMessage() != null) {
            return new SpannableString(data.getMessage().getTitle());
        } else {
            RLog.e(TAG, "The content of the message is null! Check your message content!");
            return new SpannableString("");
        }
    }

    @Override
    public void onItemClick(View view, int position, PublicServiceRichContentMessage content, UIMessage message) {
        String url = "";
        RichContentItem richContentItem = content.getMessage();
        if (richContentItem != null) {
            url = richContentItem.getUrl();
        } else {
            RLog.e(TAG, "onItemClick RichContentItem is Null");
        }
        String action = RongKitIntent.RONG_INTENT_ACTION_WEBVIEW;
        Intent intent = new Intent(action);
        intent.setPackage(view.getContext().getPackageName());
        intent.putExtra("url", url);
        view.getContext().startActivity(intent);
    }

    public static class ViewHolder {
        public TextView title;
        public AsyncImageView imageView;
        public TextView time;
        public TextView description;
    }

}

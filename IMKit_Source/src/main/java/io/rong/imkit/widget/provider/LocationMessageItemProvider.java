package io.rong.imkit.widget.provider;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import io.rong.common.RLog;
import io.rong.imkit.R;
import io.rong.imkit.RongContext;
import io.rong.imkit.model.ProviderTag;
import io.rong.imkit.model.UIMessage;
import io.rong.imkit.utilities.RongUtils;
import io.rong.imkit.widget.AsyncImageView;
import io.rong.imlib.model.Message;
import io.rong.message.LocationMessage;

@ProviderTag(messageContent = LocationMessage.class, showReadState = true)
public class LocationMessageItemProvider extends IContainerItemProvider.MessageProvider<LocationMessage> {
    private final static String TAG = "LocationMessageItemProvider";
    private static int THUMB_WIDTH = 408;
    private static int THUMB_HEIGHT = 240;

    private static class ViewHolder {
        AsyncImageView img;
        TextView title;
        FrameLayout mLayout;
    }

    @Override
    public View newView(Context context, ViewGroup group) {
        View view = LayoutInflater.from(context).inflate(R.layout.rc_item_location_message, null);

        ViewHolder holder = new ViewHolder();

        holder.img = view.findViewById(R.id.rc_img);
        holder.title = view.findViewById(R.id.rc_content);
        holder.mLayout = view.findViewById(R.id.rc_layout);

        Resources resources = context.getResources();
        try {
            THUMB_WIDTH = resources.getInteger(resources.getIdentifier("rc_location_thumb_width", "integer", context.getPackageName()));
            THUMB_HEIGHT = resources.getInteger(resources.getIdentifier("rc_location_thumb_height", "integer", context.getPackageName()));
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
        }

        view.setTag(holder);
        return view;
    }

    @Override
    public void onItemClick(View view, int position, LocationMessage content, UIMessage message) {
        try {
            String clsName = "com.amap.api.netlocation.AMapNetworkLocationClient";
            Class<?> locationCls = Class.forName(clsName);
            Intent intent;
            if (view.getContext().getResources().getBoolean(R.bool.rc_location_2D)) {
                intent = new Intent(view.getContext(), io.rong.imkit.plugin.location.AMapPreviewActivity2D.class);
            } else {
                intent = new Intent(view.getContext(), io.rong.imkit.plugin.location.AMapPreviewActivity.class);
            }
            intent.putExtra("location", message.getContent());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            view.getContext().startActivity(intent);
        } catch (Exception e) {
            RLog.i(TAG, "Not default AMap Location");
            RLog.e(TAG, "onItemClick", e);
        }
    }

    @Override
    public void bindView(View v, int position, final LocationMessage content, final UIMessage uiMsg) {
        ViewHolder holder = (ViewHolder) v.getTag();
        final Uri uri = content.getImgUri();
        RLog.d(TAG, "uri = " + uri);
        if (uri == null || !("file").equals(uri.getScheme())) {
            holder.img.setDefaultDrawable();
        } else {
            holder.img.setResource(uri);
        }
        holder.img.setLayoutParam(RongUtils.dip2px(THUMB_WIDTH / 2), RongUtils.dip2px(THUMB_HEIGHT / 2));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(holder.title.getLayoutParams().width, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.BOTTOM | Gravity.RIGHT;
        params.width = RongUtils.dip2px(THUMB_WIDTH / 2 - 7);

        if (uiMsg.getMessageDirection() == Message.MessageDirection.SEND) {
            holder.mLayout.setBackgroundResource(R.drawable.rc_ic_bubble_no_right);
            params.leftMargin = 0;
            params.rightMargin = (int) (7 * v.getResources().getDisplayMetrics().density);
            holder.title.setLayoutParams(params);
        } else {
            params.leftMargin = (int) (6 * v.getResources().getDisplayMetrics().density);
            params.rightMargin = 0;
            holder.title.setLayoutParams(params);
            holder.mLayout.setBackgroundResource(R.drawable.rc_ic_bubble_no_left);
        }
        holder.title.setText(content.getPoi());
    }

    @Override
    public Spannable getContentSummary(LocationMessage data) {
        return null;
    }

    @Override
    public Spannable getContentSummary(Context context, LocationMessage data) {
        String text = context.getResources().getString(R.string.rc_message_content_location);
        return new SpannableString(text);
    }
}
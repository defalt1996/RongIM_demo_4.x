package io.rong.imkit.widget.provider;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import io.rong.imkit.R;
import io.rong.imkit.RongContext;
import io.rong.imkit.model.ProviderTag;
import io.rong.imkit.model.UIMessage;
import io.rong.imkit.utilities.PromptPopupDialog;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.location.message.RealTimeLocationStartMessage;
import io.rong.imlib.model.Message;

/**
 * Created by weiqinxiao on 15/8/14.
 */
@ProviderTag(messageContent = RealTimeLocationStartMessage.class)
public class RealTimeLocationMessageProvider extends IContainerItemProvider.MessageProvider<RealTimeLocationStartMessage> {

    private static class ViewHolder {
        TextView message;
        boolean longClick;
    }

    @Override
    public View newView(Context context, ViewGroup group) {
        View view = LayoutInflater.from(context).inflate(R.layout.rc_share_location_message, null);

        ViewHolder holder = new ViewHolder();
        holder.message = view.findViewById(android.R.id.text1);
        view.setTag(holder);
        return view;
    }

    @Override
    public Spannable getContentSummary(RealTimeLocationStartMessage data) {
        return null;
    }

    @Override
    public Spannable getContentSummary(Context context, RealTimeLocationStartMessage data) {
        if (data != null && data.getContent() != null)
            return new SpannableString(context.getResources().getString(R.string.rc_real_time_location_start));
        return null;
    }

    @Override
    public void onItemClick(final View view, int position, RealTimeLocationStartMessage content, final UIMessage message) {

        if (message.getMessageDirection() == Message.MessageDirection.SEND) {
            joinMap(view, message);
        } else {
            PromptPopupDialog dialog = PromptPopupDialog.newInstance(view.getContext(), "",
                    view.getContext().getResources().getString(R.string.rc_real_time_join_notification));
            dialog.setPromptButtonClickedListener(new PromptPopupDialog.OnPromptButtonClickedListener() {
                @Override
                public void onPositiveButtonClicked() {
                    joinMap(view, message);
                }
            });
            dialog.show();
        }
    }

    private void joinMap(final View view, UIMessage message) {
        List<String> mLocationShareParticipants = RongIMClient.getInstance()
                .getRealTimeLocationParticipants(message.getConversationType(), message.getTargetId());
        //int result = LocationManager.getInstance().joinLocationSharing();
        int result;
        if (view.getContext().getResources().getBoolean(R.bool.rc_location_2D)) {
            result = io.rong.imkit.plugin.location.LocationManager2D.getInstance().joinLocationSharing();
        } else {
            result = io.rong.imkit.plugin.location.LocationManager.getInstance().joinLocationSharing();
        }

        if (result == 0) {
            Intent intent;
            if (view.getContext().getResources().getBoolean(R.bool.rc_location_2D)) {
                intent = new Intent(view.getContext(), io.rong.imkit.plugin.location.AMapRealTimeActivity2D.class);
            } else {
                intent = new Intent(view.getContext(), io.rong.imkit.plugin.location.AMapRealTimeActivity.class);
            }
            if (mLocationShareParticipants != null) {
                intent.putStringArrayListExtra("participants", (ArrayList<String>) mLocationShareParticipants);
            }
            view.getContext().startActivity(intent);
        } else if (result == 1) {
            Toast.makeText(view.getContext(), R.string.rc_network_exception, Toast.LENGTH_SHORT).show();
        } else if ((result == 2)) {
            Toast.makeText(view.getContext(), R.string.rc_location_sharing_exceed_max, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onItemLongClick(final View view, int position, final RealTimeLocationStartMessage content, final UIMessage message) {
        super.onItemLongClick(view, position, content, message);
        ViewHolder holder = (ViewHolder) view.getTag();
        holder.longClick = true;
        if (view instanceof TextView) {
            CharSequence text = ((TextView) view).getText();
            if (text instanceof Spannable)
                Selection.removeSelection((Spannable) text);
        }
    }

    @Override
    public void bindView(View v, int position, final RealTimeLocationStartMessage content, final UIMessage data) {
        ViewHolder holder = (ViewHolder) v.getTag();

        if (data.getMessageDirection() == Message.MessageDirection.SEND) {
            Drawable drawable = holder.message.getResources().getDrawable(R.drawable.rc_icon_rt_message_right);
            drawable.setBounds(0, 0, 29, 41);
            holder.message.setBackgroundResource(R.drawable.rc_ic_bubble_right);
            holder.message.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null);
            holder.message.setText(v.getContext()
                    .getResources()
                    .getString(R.string.rc_real_time_location_sharing));
        } else {
            Drawable drawable = holder.message.getResources().getDrawable(R.drawable.rc_icon_rt_message_left);
            drawable.setBounds(0, 0, 29, 41);
            holder.message.setBackgroundResource(R.drawable.rc_ic_bubble_left);
            holder.message.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
            holder.message.setText(v.getContext()
                    .getResources()
                    .getString(R.string.rc_real_time_location_sharing));
        }
    }
}

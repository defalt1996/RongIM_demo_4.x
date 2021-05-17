package io.rong.imkit.widget.provider;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import io.rong.imkit.R;
import io.rong.imkit.RongKitIntent;
import io.rong.imkit.destruct.DestructManager;
import io.rong.imkit.model.ProviderTag;
import io.rong.imkit.model.UIMessage;
import io.rong.imkit.resend.ResendManager;
import io.rong.imkit.utilities.RongUtils;
import io.rong.imkit.widget.AsyncImageView;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Message;
import io.rong.message.ImageMessage;

/**
 * Created by DragonJ on 14-8-2.
 */
@ProviderTag(messageContent = ImageMessage.class, showProgress = false, showReadState = true)
public class ImageMessageItemProvider extends IContainerItemProvider.MessageProvider<ImageMessage> {

    private static final String TAG = "ImageMessageItemProvider";

    private static class ViewHolder {
        AsyncImageView img;
        TextView message;
        FrameLayout fireView;
        FrameLayout sendFire;
        FrameLayout receiverFire;
        ImageView receiverFireImg;
        TextView receiverFireText;
        TextView clickHint;
    }

    @Override
    public View newView(Context context, ViewGroup group) {
        View view = LayoutInflater.from(context).inflate(R.layout.rc_item_image_message, null);

        ViewHolder holder = new ViewHolder();

        holder.message = view.findViewById(R.id.rc_msg);
        holder.img = view.findViewById(R.id.rc_img);
        holder.fireView = view.findViewById(R.id.rc_destruct_click);
        holder.sendFire = view.findViewById(R.id.fl_send_fire);
        holder.receiverFire = view.findViewById(R.id.fl_receiver_fire);
        holder.receiverFireImg = view.findViewById(R.id.iv_receiver_fire);
        holder.receiverFireText = view.findViewById(R.id.tv_receiver_fire);
        holder.clickHint = view.findViewById(R.id.rc_destruct_click_hint);

        view.setTag(holder);
        return view;
    }

    @Override
    public void onItemClick(View view, int position, ImageMessage content, UIMessage message) {
        if (content != null) {
            Intent intent = new Intent(RongKitIntent.RONG_INTENT_ACTION_PICTUREPAGERVIEW);
            intent.setPackage(view.getContext().getPackageName());
            intent.putExtra("message", message.getMessage());
            view.getContext().startActivity(intent);
        }
    }

    @Override
    public void bindView(View v, int position, ImageMessage content, UIMessage message) {
        final ViewHolder holder = (ViewHolder) v.getTag();
        holder.receiverFire.setTag(message.getUId());
        if (content.isDestruct()) {
            bindFireView(v, position, content, message);
            return;
        }
        if (message.getMessageDirection() == Message.MessageDirection.SEND) {
            v.setBackgroundResource(R.drawable.rc_ic_bubble_no_right);
        } else {
            v.setBackgroundResource(R.drawable.rc_ic_bubble_no_left);
        }
        holder.sendFire.setVisibility(View.GONE);
        holder.receiverFire.setVisibility(View.GONE);
        holder.fireView.setVisibility(View.GONE);
        holder.img.setVisibility(View.VISIBLE);
        holder.img.setResource(content.getThumUri());
        int progress = message.getProgress();

        Message.SentStatus status = message.getSentStatus();

        if (status != null
                && (status.equals(Message.SentStatus.SENDING) && progress < 100
                || (status.equals(Message.SentStatus.FAILED) && ResendManager.getInstance().needResend(message.getMessageId())))) {
            holder.message.setText(progress + "%");
            holder.message.setVisibility(View.VISIBLE);
        } else {
            holder.message.setVisibility(View.GONE);
        }
    }

    private void bindFireView(View v, int position, ImageMessage content, UIMessage message) {
        final ViewHolder holder = (ViewHolder) v.getTag();
        holder.img.setVisibility(View.GONE);
        holder.fireView.setVisibility(View.VISIBLE);
        if (message.getMessageDirection() == Message.MessageDirection.SEND) {
            holder.sendFire.setVisibility(View.VISIBLE);
            holder.receiverFire.setVisibility(View.GONE);
            holder.fireView.setBackgroundResource(R.drawable.rc_ic_bubble_no_right);
            Drawable drawable = v.getContext().getResources().getDrawable(R.drawable.rc_fire_sender_album);
            drawable.setBounds(0, 0, RongUtils.dip2px(31), RongUtils.dip2px(26));
            holder.clickHint.setCompoundDrawables(null, drawable, null, null);
            holder.clickHint.setTextColor(Color.parseColor("#FFFFFF"));
        } else {
            holder.sendFire.setVisibility(View.GONE);
            holder.receiverFire.setVisibility(View.VISIBLE);
            holder.fireView.setBackgroundResource(R.drawable.rc_ic_bubble_no_left);
            Drawable drawable = v.getContext().getResources().getDrawable(R.drawable.rc_fire_receiver_album);
            drawable.setBounds(0, 0, RongUtils.dip2px(31), RongUtils.dip2px(26));
            holder.clickHint.setCompoundDrawables(null, drawable, null, null);
            holder.clickHint.setTextColor(Color.parseColor("#F4B50B"));
            DestructManager.getInstance().addListener(message.getUId(), new DestructListener(holder, message), TAG);
            if (message.getMessage().getReadTime() > 0) {
                holder.receiverFireText.setVisibility(View.VISIBLE);
                holder.receiverFireImg.setVisibility(View.GONE);
                String unFinishTime;
                if (TextUtils.isEmpty(message.getUnDestructTime())) {
                    unFinishTime = DestructManager.getInstance().getUnFinishTime(message.getUId());
                } else {
                    unFinishTime = message.getUnDestructTime();
                }
                holder.receiverFireText.setText(unFinishTime);
                DestructManager.getInstance().startDestruct(message.getMessage());
            } else {
                holder.receiverFireText.setVisibility(View.GONE);
                holder.receiverFireImg.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public Spannable getContentSummary(ImageMessage data) {
        return null;
    }

    @Override
    public Spannable getContentSummary(Context context, ImageMessage data) {
        if (data.isDestruct()) {
            return new SpannableString(context.getString(R.string.rc_message_content_burn));
        }
        return new SpannableString(context.getString(R.string.rc_message_content_image));
    }

    private static class DestructListener implements RongIMClient.DestructCountDownTimerListener {
        private WeakReference<ViewHolder> mHolder;
        private UIMessage mUIMessage;

        public DestructListener(ImageMessageItemProvider.ViewHolder pHolder, UIMessage pUIMessage) {
            mHolder = new WeakReference<>(pHolder);
            mUIMessage = pUIMessage;
        }

        @Override
        public void onTick(long millisUntilFinished, String messageId) {
            if (mUIMessage.getUId().equals(messageId)) {
                ImageMessageItemProvider.ViewHolder viewHolder = mHolder.get();
                if (viewHolder != null && messageId.equals(viewHolder.receiverFire.getTag())) {
                    viewHolder.receiverFireText.setVisibility(View.VISIBLE);
                    viewHolder.receiverFireImg.setVisibility(View.GONE);
                    String unDestructTime = String.valueOf(Math.max(millisUntilFinished, 1));
                    viewHolder.receiverFireText.setText(unDestructTime);
                    mUIMessage.setUnDestructTime(unDestructTime);
                }
            }
        }

        @Override
        public void onStop(String messageId) {
            if (mUIMessage.getUId().equals(messageId)) {
                ImageMessageItemProvider.ViewHolder viewHolder = mHolder.get();
                if (viewHolder != null && messageId.equals(viewHolder.receiverFire.getTag())) {
                    viewHolder.receiverFireText.setVisibility(View.GONE);
                    viewHolder.receiverFireImg.setVisibility(View.VISIBLE);
                    mUIMessage.setUnDestructTime(null);
                }
            }
        }
    }
}

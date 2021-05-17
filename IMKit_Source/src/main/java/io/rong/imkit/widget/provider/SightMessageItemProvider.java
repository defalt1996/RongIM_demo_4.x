package io.rong.imkit.widget.provider;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;

import io.rong.imkit.R;
import io.rong.imkit.RongIM;
import io.rong.imkit.destruct.DestructManager;
import io.rong.imkit.manager.SendMediaManager;
import io.rong.imkit.model.ProviderTag;
import io.rong.imkit.model.UIMessage;
import io.rong.imkit.resend.ResendManager;
import io.rong.imkit.utilities.OptionsPopupDialog;
import io.rong.imkit.utilities.PermissionCheckUtil;
import io.rong.imkit.utilities.RongUtils;
import io.rong.imkit.utils.RongOperationPermissionUtils;
import io.rong.imkit.widget.AsyncImageView;
import io.rong.imkit.widget.CircleProgressView;
import io.rong.imlib.IRongCallback;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Message;
import io.rong.message.SightMessage;

@ProviderTag(messageContent = SightMessage.class, showProgress = false, showReadState = true)
public class SightMessageItemProvider extends IContainerItemProvider.MessageProvider<SightMessage> {

    private static final String TAG = "Sight-SightMessageItemProvider";

    private static class ViewHolder {
        RelativeLayout operationButton;
        ImageView operationIcon;
        FrameLayout message;
        AsyncImageView thumbImg;
        ImageView tagImg;
        ProgressBar compressProgress;
        CircleProgressView loadingProgress;
        TextView duration;
        FrameLayout fireView;
        FrameLayout sendFire;
        FrameLayout receiverFire;
        ImageView receiverFireImg;
        TextView receiverFireText;
        TextView clickHint;
    }

    @Override
    public View newView(Context context, ViewGroup group) {
        View view = LayoutInflater.from(context).inflate(R.layout.rc_item_sight_message, null);

        ViewHolder holder = new ViewHolder();
        holder.operationButton = view.findViewById(R.id.rc_sight_operation);
        holder.operationIcon = view.findViewById(R.id.rc_sight_operation_icon);
        holder.message = view.findViewById(R.id.rc_message);
        holder.compressProgress = view.findViewById(R.id.compressVideoBar);
        holder.loadingProgress = view.findViewById(R.id.rc_sight_progress);
        holder.thumbImg = view.findViewById(R.id.rc_sight_thumb);
        holder.tagImg = view.findViewById(R.id.rc_sight_tag);
        holder.duration = view.findViewById(R.id.rc_sight_duration);
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
    public void onItemClick(final View view, int position, SightMessage content, final UIMessage uiMessage) {
        if (content != null) {
            if (!RongOperationPermissionUtils.isMediaOperationPermit(view.getContext())) {
                return;
            }
            String[] permissions = {
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            if (!PermissionCheckUtil.checkPermissions(view.getContext(), permissions)) {
                Activity activity = (Activity) view.getContext();
                PermissionCheckUtil.requestPermissions(activity, permissions, 100);
                return;
            }

            Uri.Builder builder = new Uri.Builder();
            builder.scheme("rong")
                    .authority(view.getContext().getPackageName())
                    .appendPath("sight")
                    .appendPath("player");
            String intentUrl = builder.build().toString();
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(intentUrl));
            intent.setPackage(view.getContext().getPackageName());
            intent.putExtra("SightMessage", content);
            intent.putExtra("Message", uiMessage.getMessage());
            intent.putExtra("Progress", uiMessage.getProgress());
            if (intent.resolveActivity(view.getContext().getPackageManager()) != null) {
                view.getContext().startActivity(intent);
            } else {
                Toast.makeText(view.getContext(), "Sight Module does not exist.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void bindView(View v, int position, SightMessage content, final UIMessage message) {
        final ViewHolder holder = (ViewHolder) v.getTag();
        holder.receiverFire.setTag(message.getUId());
        if (message.getMessageDirection() == Message.MessageDirection.SEND) {
            holder.message.setBackgroundResource(R.drawable.rc_ic_bubble_no_right);
            int padding = (int) v.getContext().getResources().getDimension(R.dimen.rc_dimen_size_12);
            holder.duration.setPadding(0, 0, padding, 0);
        } else {
            holder.message.setBackgroundResource(R.drawable.rc_ic_bubble_no_left);
            int padding = (int) v.getContext().getResources().getDimension(R.dimen.rc_dimen_size_6);
            holder.duration.setPadding(0, 0, padding, 0);
        }

        int progress = message.getProgress();
        final Message.SentStatus status = message.getSentStatus();
        if (content.isDestruct()) {
            holder.fireView.setVisibility(View.VISIBLE);
            holder.thumbImg.setVisibility(View.GONE);
            if (message.getMessageDirection() == Message.MessageDirection.SEND) {
                holder.sendFire.setVisibility(View.VISIBLE);
                holder.receiverFire.setVisibility(View.GONE);
                holder.fireView.setBackgroundResource(R.drawable.rc_ic_bubble_no_right);
                Drawable drawable = v.getContext().getResources().getDrawable(R.drawable.rc_destruct_video_play);
                drawable.setBounds(0, 0, RongUtils.dip2px(22), RongUtils.dip2px(22));
                holder.clickHint.setCompoundDrawables(null, drawable, null, null);
                holder.clickHint.setTextColor(Color.parseColor("#FFFFFF"));
                holder.duration.setTextColor(Color.parseColor("#FFFFFF"));
            } else {
                holder.sendFire.setVisibility(View.GONE);
                holder.receiverFire.setVisibility(View.VISIBLE);
                holder.fireView.setBackgroundResource(R.drawable.rc_ic_bubble_no_left);
                Drawable drawable = v.getContext().getResources().getDrawable(R.drawable.rc_icon_fire_video_play);
                drawable.setBounds(0, 0, RongUtils.dip2px(22), RongUtils.dip2px(22));
                holder.clickHint.setCompoundDrawables(null, drawable, null, null);
                holder.clickHint.setTextColor(Color.parseColor("#F4B50B"));
                holder.duration.setTextColor(Color.parseColor("#F4B50B"));
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
        } else {
            holder.sendFire.setVisibility(View.GONE);
            holder.receiverFire.setVisibility(View.GONE);
            holder.fireView.setVisibility(View.GONE);
            holder.thumbImg.setVisibility(View.VISIBLE);
            holder.thumbImg.setResource(content.getThumbUri());
            holder.duration.setTextColor(Color.parseColor("#FFFFFF"));
        }
        holder.duration.setText(getSightDuration(content.getDuration()));
        if (progress > 0 && progress < 100) {
            holder.loadingProgress.setProgress(progress, true);
            holder.tagImg.setVisibility(View.GONE);
            holder.loadingProgress.setVisibility(View.VISIBLE);
            holder.compressProgress.setVisibility(View.GONE);
            //发送小视频时取消发送，这个功能暂时未打开
            //handleSendingView(message, holder);
        } else if (status.equals(Message.SentStatus.SENDING)) {
            holder.tagImg.setVisibility(View.GONE);
            holder.loadingProgress.setVisibility(View.GONE);
            holder.compressProgress.setVisibility(View.VISIBLE);
        } else if (status.equals(Message.SentStatus.FAILED) && ResendManager.getInstance().needResend(message.getMessageId())) {
            holder.tagImg.setVisibility(View.GONE);
            holder.loadingProgress.setVisibility(View.GONE);
            holder.compressProgress.setVisibility(View.VISIBLE);
        } else {
            holder.tagImg.setVisibility(View.VISIBLE);
            holder.loadingProgress.setVisibility(View.GONE);
            holder.compressProgress.setVisibility(View.GONE);
            //handleSendingView(message, holder);
        }
    }

    @Override
    public Spannable getContentSummary(SightMessage data) {
        return null;
    }

    @Override
    public Spannable getContentSummary(Context context, SightMessage data) {
        if (data.isDestruct()) {
            return new SpannableString(context.getString(R.string.rc_message_content_burn));
        }
        return new SpannableString(context.getResources().getString(R.string.rc_message_content_sight));
    }

    @Override
    public void onItemLongClick(View view, int position, SightMessage content, final UIMessage message) {
        if (message.getMessage().getSentStatus().getValue() < Message.SentStatus.SENT.getValue()) {
            String[] items = new String[]{view.getContext().getResources().getString(R.string.rc_dialog_item_message_delete)};
            OptionsPopupDialog.newInstance(view.getContext(), items).setOptionsPopupDialogListener(new OptionsPopupDialog.OnOptionsItemClickedListener() {
                @Override
                public void onOptionsItemClicked(int which) {
                    if (which == 0) {
                        SendMediaManager.getInstance().cancelSendingMedia(message.getConversationType(), message.getTargetId(), message.getMessageId());
                        RongIM.getInstance().cancelSendMediaMessage(message.getMessage(), null);
                        RongIM.getInstance().deleteMessages(new int[]{message.getMessageId()}, null);
                    }
                }
            }).show();
        } else {
            super.onItemLongClick(view, position, content, message);
        }
    }

    private boolean isSightDownloaded(SightMessage sightMessage) {
        if (sightMessage.getLocalPath() != null && !TextUtils.isEmpty(sightMessage.getLocalPath().toString())) {
            String path = sightMessage.getLocalPath().toString();
            if (path.startsWith("file://")) {
                path = path.substring(7);
            }
            File file = new File(path);
            return file.exists();
        } else {
            return false;
        }
    }

    private void handleSendingView(final UIMessage message, final ViewHolder holder) {
        final Message.SentStatus status = message.getSentStatus();
        if (status.equals(Message.SentStatus.SENDING)) {
            holder.operationButton.setVisibility(View.VISIBLE);
            holder.operationIcon.setImageResource(io.rong.imkit.R.drawable.rc_file_icon_cancel);
        } else {
            if (status.equals(Message.SentStatus.CANCELED)) {
                holder.operationButton.setVisibility(View.VISIBLE);
                holder.operationIcon.setImageResource(io.rong.imkit.R.drawable.rc_ic_warning);
            } else {
                holder.operationButton.setVisibility(View.GONE);
            }
        }
        holder.operationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (status.equals(Message.SentStatus.SENDING)) {
                    RongIM.getInstance().cancelSendMediaMessage(message.getMessage(), new RongIMClient.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            holder.operationButton.setVisibility(View.VISIBLE);
                            holder.operationIcon.setImageResource(io.rong.imkit.R.drawable.rc_ic_warning);
                            holder.tagImg.setVisibility(View.VISIBLE);
                            holder.loadingProgress.setVisibility(View.GONE);
                            holder.compressProgress.setVisibility(View.GONE);
                        }

                        @Override
                        public void onError(RongIMClient.ErrorCode errorCode) {

                        }
                    });
                } else if (message.getSentStatus().equals(Message.SentStatus.CANCELED)) {
                    RongIM.getInstance().deleteMessages(new int[]{message.getMessageId()}, new RongIMClient.ResultCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean aBoolean) {
                            if (aBoolean) {
                                message.getMessage().setMessageId(0);
                                RongIM.getInstance().sendMediaMessage(message.getMessage(), null, null, (IRongCallback.ISendMediaMessageCallback) null);
                            }
                        }

                        @Override
                        public void onError(RongIMClient.ErrorCode e) {

                        }
                    });
                }
            }
        });
    }

    private String getSightDuration(int time) {
        String recordTime;
        int hour, minute, second;
        if (time <= 0)
            return "00:00";
        else {
            minute = time / 60;
            if (minute < 60) {
                second = time % 60;
                recordTime = unitFormat(minute) + ":" + unitFormat(second);
            } else {
                hour = minute / 60;
                if (hour > 99)
                    return "99:59:59";
                minute = minute % 60;
                second = time - hour * 3600 - minute * 60;
                recordTime = unitFormat(hour) + ":" + unitFormat(minute) + ":" + unitFormat(second);
            }
        }
        return recordTime;
    }

    private String unitFormat(int time) {
        String formatTime;
        if (time >= 0 && time < 10)
            formatTime = "0" + time;
        else
            formatTime = "" + time;
        return formatTime;
    }

    private static class DestructListener implements RongIMClient.DestructCountDownTimerListener {
        private WeakReference<SightMessageItemProvider.ViewHolder> mHolder;
        private UIMessage mUIMessage;

        public DestructListener(SightMessageItemProvider.ViewHolder pHolder, UIMessage pUIMessage) {
            mHolder = new WeakReference<>(pHolder);
            mUIMessage = pUIMessage;
        }

        @Override
        public void onTick(long millisUntilFinished, String messageId) {
            if (mUIMessage.getUId().equals(messageId)) {
                SightMessageItemProvider.ViewHolder viewHolder = mHolder.get();
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
                SightMessageItemProvider.ViewHolder viewHolder = mHolder.get();
                if (viewHolder != null && messageId.equals(viewHolder.receiverFire.getTag())) {
                    viewHolder.receiverFireText.setVisibility(View.GONE);
                    viewHolder.receiverFireImg.setVisibility(View.VISIBLE);
                    mUIMessage.setUnDestructTime(null);
                }
            }
        }
    }

}

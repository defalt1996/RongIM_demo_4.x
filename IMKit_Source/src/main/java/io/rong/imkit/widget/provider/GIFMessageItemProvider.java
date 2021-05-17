package io.rong.imkit.widget.provider;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import androidx.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.lang.ref.WeakReference;

import io.rong.imkit.R;
import io.rong.imkit.RongIM;
import io.rong.imkit.RongKitIntent;
import io.rong.imkit.destruct.DestructManager;
import io.rong.imkit.model.ProviderTag;
import io.rong.imkit.model.UIMessage;
import io.rong.imkit.resend.ResendManager;
import io.rong.imkit.utilities.PermissionCheckUtil;
import io.rong.imkit.utilities.RongUtils;
import io.rong.imkit.widget.AsyncImageView;
import io.rong.imkit.widget.CircleProgressView;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Message;
import io.rong.message.GIFMessage;

@ProviderTag(messageContent = GIFMessage.class, showProgress = false, showReadState = true)
public class GIFMessageItemProvider extends IContainerItemProvider.MessageProvider<GIFMessage> {

    private static final String TAG = "GIFMessageItemProvider";

    private static class ViewHolder {
        AsyncImageView img;
        ProgressBar preProgress;
        CircleProgressView loadingProgress;
        ImageView startDownLoad;
        ImageView downLoadFailed;
        TextView length;
        FrameLayout fireView;
        FrameLayout sendFire;
        FrameLayout receiverFire;
        ImageView receiverFireImg;
        TextView receiverFireText;
        TextView clickHint;
    }

    @Override
    public View newView(Context context, ViewGroup group) {
        View view = LayoutInflater.from(context).inflate(R.layout.rc_item_gif_message, null);

        ViewHolder holder = new ViewHolder();

        holder.img = view.findViewById(R.id.rc_img);
        holder.preProgress = view.findViewById(R.id.rc_pre_progress);
        holder.loadingProgress = view.findViewById(R.id.rc_gif_progress);
        holder.startDownLoad = view.findViewById(R.id.rc_start_download);
        holder.downLoadFailed = view.findViewById(R.id.rc_download_failed);
        holder.length = view.findViewById(R.id.rc_length);
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
    public void onItemClick(View view, int position, GIFMessage content, UIMessage message) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        if (holder.startDownLoad.getVisibility() == View.VISIBLE) {
            holder.startDownLoad.setVisibility(View.GONE);
            if (checkPermission(view.getContext())) {
                downLoad(message.getMessage(), holder);
            } else {
                holder.downLoadFailed.setVisibility(View.VISIBLE);
                holder.length.setVisibility(View.VISIBLE);
                holder.length.setText(formatSize(content.getGifDataSize()));
                Toast.makeText(view.getContext(), R.string.rc_ac_file_download_request_permission, Toast.LENGTH_SHORT).show();
            }
        } else if (holder.downLoadFailed.getVisibility() == View.VISIBLE) {
            holder.downLoadFailed.setVisibility(View.GONE);
            if (checkPermission(view.getContext())) {
                downLoad(message.getMessage(), holder);
            } else {
                // 显示图片下载的界面
                holder.downLoadFailed.setVisibility(View.VISIBLE);
                holder.length.setVisibility(View.VISIBLE);
                holder.length.setText(formatSize(content.getGifDataSize()));
                Toast.makeText(view.getContext(), R.string.rc_ac_file_download_request_permission, Toast.LENGTH_SHORT).show();
            }
        } else if (holder.preProgress.getVisibility() != View.VISIBLE && holder.loadingProgress.getVisibility() != View.VISIBLE) {
            if (content != null) {
                Intent intent = new Intent(RongKitIntent.RONG_INTENT_ACTION_GIF_VIEW);
                intent.setPackage(view.getContext().getPackageName());
                intent.putExtra("message", message.getMessage());
                view.getContext().startActivity(intent);
            }
        }
    }

    @Override
    public void bindView(View v, int position, GIFMessage content, UIMessage message) {

        final ViewHolder holder = (ViewHolder) v.getTag();
        holder.receiverFire.setTag(message.getUId());
        holder.startDownLoad.setVisibility(View.GONE);
        holder.downLoadFailed.setVisibility(View.GONE);
        holder.preProgress.setVisibility(View.GONE);
        holder.loadingProgress.setVisibility(View.GONE);
        holder.length.setVisibility(View.GONE);

        final int[] paramsValue = getParamsValue(v.getContext(), content.getWidth(), content.getHeight());
        holder.img.setLayoutParam(paramsValue[0], paramsValue[1]);

        holder.img.setImageDrawable(v.getContext().getResources().getDrawable(R.drawable.def_gif_bg));

        int progress = message.getProgress();
        if (message.getMessageDirection() == Message.MessageDirection.SEND) {
            Message.SentStatus status = message.getSentStatus();
            if (progress > 0 && progress < 100
                    || (status.equals(Message.SentStatus.FAILED) && ResendManager.getInstance().needResend(message.getMessageId()))) {
                holder.loadingProgress.setProgress(progress, true);
                holder.loadingProgress.setVisibility(View.VISIBLE);
                holder.preProgress.setVisibility(View.GONE);
            } else if (status.equals(Message.SentStatus.SENDING)) {
                holder.loadingProgress.setVisibility(View.GONE);
            } else if (progress == -1) {
                holder.loadingProgress.setVisibility(View.GONE);
                holder.preProgress.setVisibility(View.GONE);
                holder.downLoadFailed.setVisibility(View.VISIBLE);
                holder.length.setVisibility(View.VISIBLE);
            } else {
                holder.loadingProgress.setVisibility(View.GONE);
                holder.preProgress.setVisibility(View.GONE);
            }
        } else {
            if (message.getReceivedStatus().isDownload()) {
                if (progress > 0 && progress < 100) {
                    holder.loadingProgress.setProgress(progress, true);
                    holder.loadingProgress.setVisibility(View.VISIBLE);
                    holder.preProgress.setVisibility(View.GONE);
                    holder.startDownLoad.setVisibility(View.GONE);
                } else if (progress == 100) {
                    holder.loadingProgress.setVisibility(View.GONE);
                    holder.preProgress.setVisibility(View.GONE);
                    holder.length.setVisibility(View.GONE);
                    holder.startDownLoad.setVisibility(View.GONE);
                } else if (progress == -1) {
                    holder.loadingProgress.setVisibility(View.GONE);
                    holder.preProgress.setVisibility(View.GONE);
                    holder.downLoadFailed.setVisibility(View.VISIBLE);
                    holder.length.setVisibility(View.VISIBLE);
                    holder.startDownLoad.setVisibility(View.GONE);
                } else {
                    holder.loadingProgress.setVisibility(View.GONE);
                    holder.preProgress.setVisibility(View.VISIBLE);
                    holder.length.setVisibility(View.VISIBLE);
                    holder.startDownLoad.setVisibility(View.GONE);
                }
            } else {

                holder.loadingProgress.setVisibility(View.GONE);
                holder.preProgress.setVisibility(View.GONE);
                holder.length.setVisibility(View.GONE);
                holder.startDownLoad.setVisibility(View.GONE);

                // 下载失败
                if (progress == -1) {
                    holder.downLoadFailed.setVisibility(View.VISIBLE);
                    holder.length.setVisibility(View.VISIBLE);
                    holder.length.setText(formatSize(content.getGifDataSize()));
                }
            }
        }
        if (content.isDestruct()) {
            if (message.getMessageDirection() == Message.MessageDirection.SEND) {
                holder.sendFire.setVisibility(View.VISIBLE);
                holder.receiverFire.setVisibility(View.GONE);
                holder.fireView.setBackgroundResource(R.drawable.rc_ic_bubble_no_right);
                Drawable drawable = v.getContext().getResources().getDrawable(R.drawable.rc_fire_sender_album);
                drawable.setBounds(0, 0, RongUtils.dip2px(40), RongUtils.dip2px(34));
                holder.clickHint.setCompoundDrawables(null, drawable, null, null);
                holder.clickHint.setTextColor(Color.parseColor("#FFFFFF"));
            } else {
                holder.sendFire.setVisibility(View.GONE);
                holder.receiverFire.setVisibility(View.VISIBLE);
                holder.fireView.setBackgroundResource(R.drawable.rc_ic_bubble_no_left);
                Drawable drawable = v.getContext().getResources().getDrawable(R.drawable.rc_fire_receiver_album);
                drawable.setBounds(0, 0, RongUtils.dip2px(40), RongUtils.dip2px(34));
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
        } else {
            holder.receiverFire.setVisibility(View.GONE);
            holder.sendFire.setVisibility(View.GONE);
        }
        if (content.getLocalPath() != null) {
            if (content.isDestruct()) {
                holder.fireView.setVisibility(View.VISIBLE);
                holder.img.setVisibility(View.GONE);
            } else {
                holder.fireView.setVisibility(View.GONE);
                holder.img.setVisibility(View.VISIBLE);
                loadGif(v, content.getLocalUri(), holder);
            }
        } else {
            final int size = v.getContext().getResources().getInteger(R.integer.rc_gifmsg_auto_download_size);
            if (content.getGifDataSize() <= size * 1024) {
                if (checkPermission(v.getContext())) {
                    if (!message.getReceivedStatus().isDownload()) {
                        message.getReceivedStatus().setDownload();
                        downLoad(message.getMessage(), holder);
                    }
                } else {
                    if (progress != -1) {
                        // 显示图片下载的界面
                        holder.startDownLoad.setVisibility(View.VISIBLE);
                        holder.length.setVisibility(View.VISIBLE);
                        holder.length.setText(formatSize(content.getGifDataSize()));
                    }

                }
            } else {
                // 下载的时候
                if (progress > 0 && progress < 100) {
                    holder.startDownLoad.setVisibility(View.GONE);
                    holder.length.setVisibility(View.VISIBLE);
                    holder.length.setText(formatSize(content.getGifDataSize()));
                } else if (progress != -1) {
                    // 显示图片下载的界面
                    holder.startDownLoad.setVisibility(View.VISIBLE);
                    holder.preProgress.setVisibility(View.GONE);
                    holder.loadingProgress.setVisibility(View.GONE);
                    holder.downLoadFailed.setVisibility(View.GONE);
                    holder.length.setVisibility(View.VISIBLE);
                    holder.length.setText(formatSize(content.getGifDataSize()));
                }

            }

        }

    }

    @Override
    public Spannable getContentSummary(GIFMessage data) {
        return null;
    }

    @Override
    public Spannable getContentSummary(Context context, GIFMessage data) {
        if (data.isDestruct()) {
            return new SpannableString(context.getString(R.string.rc_message_content_burn));
        }
        return new SpannableString(context.getString(R.string.rc_message_content_image));
    }

    private void downLoad(final Message downloadMsg, final ViewHolder holder) {
        holder.preProgress.setVisibility(View.VISIBLE);
        RongIM.getInstance().downloadMediaMessage(downloadMsg, null);
    }


    private void loadGif(View v, Uri uri, ViewHolder holder) {
        Glide.with(v.getContext())
                .asGif()
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .load(uri.getPath())
                .listener(new RequestListener<GifDrawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<GifDrawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GifDrawable resource, Object model, Target<GifDrawable> target, DataSource dataSource, boolean isFirstResource) {
                        return false;
                    }
                })
                .into(holder.img);
    }

    private String formatSize(long length) {
        if (length > 1024 * 1024) { // M
            float size = Math.round(length / (1024f * 1024f) * 100) / 100f;
            return size + "M";
        } else if (length > 1024) {
            float size = Math.round(length / (1024f) * 100) / 100f;
            return size + "KB";
        } else {
            return length + "B";
        }
    }


    private int[] getParamsValue(Context context, int width, int height) {
        // 宽度最大 120 dp， 最小 80 dp， 长度最小 80dp
        final int maxWidth = dip2px((context), 120);
        final int minValue = dip2px(context, 80);
        int finalWidth;
        int finalHeight;

        if (width > maxWidth) {
            finalWidth = maxWidth;
            float scale = (float) width / maxWidth;
            finalHeight = Math.round(height / scale);
            if (finalHeight < minValue) {
                finalHeight = minValue;
            }
        } else if (width < minValue) {
            finalWidth = minValue;
            float scale = (float) width / minValue;
            finalHeight = Math.round(height * scale);
            if (finalHeight < minValue) {
                finalHeight = minValue;
            }

        } else {
            finalWidth = Math.round(height);
            finalHeight = Math.round(width);
        }
        int[] params = new int[2];
        params[0] = finalWidth;
        params[1] = finalHeight;
        return params;
    }


    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);

    }


    private boolean checkPermission(Context context) {
        String[] permission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        return PermissionCheckUtil.checkPermissions(context, permission);
    }

    private static class DestructListener implements RongIMClient.DestructCountDownTimerListener {
        private WeakReference<GIFMessageItemProvider.ViewHolder> mHolder;
        private UIMessage mUIMessage;

        public DestructListener(GIFMessageItemProvider.ViewHolder pHolder, UIMessage pUIMessage) {
            mHolder = new WeakReference<>(pHolder);
            mUIMessage = pUIMessage;
        }

        @Override
        public void onTick(long millisUntilFinished, String messageId) {
            if (mUIMessage.getUId().equals(messageId)) {
                GIFMessageItemProvider.ViewHolder viewHolder = mHolder.get();
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
                GIFMessageItemProvider.ViewHolder viewHolder = mHolder.get();
                if (viewHolder != null && messageId.equals(viewHolder.receiverFire.getTag())) {
                    viewHolder.receiverFireText.setVisibility(View.GONE);
                    viewHolder.receiverFireImg.setVisibility(View.VISIBLE);
                    mUIMessage.setUnDestructTime(null);
                }
            }
        }
    }

    @Override
    public void onItemLongClick(View view, int position, GIFMessage content, UIMessage message) {
        final ViewHolder holder = (ViewHolder) view.getTag();
        if (holder.startDownLoad.getVisibility() == View.VISIBLE
                || holder.downLoadFailed.getVisibility() == View.VISIBLE
                || holder.loadingProgress.getVisibility() == View.VISIBLE) {
            return;
        }
        super.onItemLongClick(view, position, content, message);
    }
}

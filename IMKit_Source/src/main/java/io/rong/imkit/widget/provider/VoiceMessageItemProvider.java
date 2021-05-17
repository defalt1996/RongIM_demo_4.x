package io.rong.imkit.widget.provider;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import io.rong.common.RLog;
import io.rong.eventbus.EventBus;
import io.rong.imkit.R;
import io.rong.imkit.destruct.DestructManager;
import io.rong.imkit.manager.AudioPlayManager;
import io.rong.imkit.manager.AudioRecordManager;
import io.rong.imkit.manager.IAudioPlayListener;
import io.rong.imkit.model.Event;
import io.rong.imkit.model.ProviderTag;
import io.rong.imkit.model.UIMessage;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Message;
import io.rong.message.VoiceMessage;

@ProviderTag(messageContent = VoiceMessage.class, showReadState = true)
public class VoiceMessageItemProvider extends IContainerItemProvider.MessageProvider<VoiceMessage> {
    private final static String TAG = "VoiceMessageItemProvider";

    private static class ViewHolder {
        ImageView img;
        TextView left;
        TextView right;
        ImageView unread;
        FrameLayout sendFire;
        FrameLayout receiverFire;
        ImageView receiverFireImg;
        TextView receiverFireText;
    }

    public VoiceMessageItemProvider(Context context) {
    }

    @Override
    public View newView(Context context, ViewGroup group) {
        View view = LayoutInflater.from(context).inflate(R.layout.rc_item_voice_message, null);
        ViewHolder holder = new ViewHolder();
        holder.left = view.findViewById(R.id.rc_left);
        holder.right = view.findViewById(R.id.rc_right);
        holder.img = view.findViewById(R.id.rc_img);
        holder.unread = view.findViewById(R.id.rc_voice_unread);
        holder.sendFire = view.findViewById(R.id.fl_send_fire);
        holder.receiverFire = view.findViewById(R.id.fl_receiver_fire);
        holder.receiverFireImg = view.findViewById(R.id.iv_receiver_fire);
        holder.receiverFireText = view.findViewById(R.id.tv_receiver_fire);
        view.setTag(holder);
        return view;
    }

    @Override
    public void bindView(final View v, int position, final VoiceMessage content, final UIMessage message) {
        final ViewHolder holder = (ViewHolder) v.getTag();
        holder.receiverFire.setTag(message.getUId());
        if (content.isDestruct()) {
            if (message.getMessageDirection() == Message.MessageDirection.SEND) {
                holder.sendFire.setVisibility(View.VISIBLE);
                holder.receiverFire.setVisibility(View.GONE);
            } else {
                holder.sendFire.setVisibility(View.GONE);
                holder.receiverFire.setVisibility(View.VISIBLE);
                DestructManager.getInstance().addListener(message.getUId(), new VoiceMessageItemProvider.DestructListener(holder, message), TAG);
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
        }
        if (message.continuePlayAudio) {
            Uri playingUri = AudioPlayManager.getInstance().getPlayingUri();
            if (playingUri == null || !playingUri.equals(content.getUri())) {
                final boolean listened = message.getMessage().getReceivedStatus().isListened();
                //sendDestructReceiptMessage(message);
                AudioPlayManager.getInstance().startPlay(v.getContext(), content.getUri(), new VoiceMessagePlayListener(v.getContext(), message, holder, listened));
            }
        } else {
            Uri playingUri = AudioPlayManager.getInstance().getPlayingUri();
            if (playingUri != null && playingUri.equals(content.getUri())) {
                setLayout(v.getContext(), holder, message, true);
                final boolean listened = message.getMessage().getReceivedStatus().isListened();
                AudioPlayManager.getInstance().setPlayListener(new VoiceMessagePlayListener(v.getContext(), message, holder, listened));
            } else {
                setLayout(v.getContext(), holder, message, false);
            }
        }
    }


    @Override
    public void onItemClick(final View view, int position, final VoiceMessage content, final UIMessage message) {
        RLog.d(TAG, "Item index:" + position);
        if (content == null) return;
        final ViewHolder holder = (ViewHolder) view.getTag();
        if (AudioPlayManager.getInstance().isPlaying()) {
            //正在播放，停止播放开始倒计时
            //DestructManager.getInstance().startDestruct(message.getMessage());
            if (AudioPlayManager.getInstance().getPlayingUri().equals(content.getUri())) {
                AudioPlayManager.getInstance().stopPlay();
                return;
            }
            AudioPlayManager.getInstance().stopPlay();
        }
        if (!AudioPlayManager.getInstance().isInNormalMode(view.getContext()) && AudioPlayManager.getInstance().isInVOIPMode(view.getContext())) {
            Toast.makeText(view.getContext(), view.getContext().getString(R.string
                    .rc_voip_occupying), Toast.LENGTH_SHORT).show();
            return;
        }
        holder.unread.setVisibility(View.GONE);
        final boolean listened = message.getMessage().getReceivedStatus().isListened();
        AudioPlayManager.getInstance().startPlay(view.getContext(), content.getUri(), new
                VoiceMessagePlayListener(view.getContext(), message, holder, listened));
    }

    private void setLayout(Context context, ViewHolder holder, UIMessage message, boolean playing) {
        VoiceMessage content = (VoiceMessage) message.getContent();
        int minWidth = 70, maxWidth = 204;
        float scale = context.getResources().getDisplayMetrics().density;
        minWidth = (int) (minWidth * scale + 0.5f);
        maxWidth = (int) (maxWidth * scale + 0.5f);
        int duration = AudioRecordManager.getInstance().getMaxVoiceDuration();
        holder.img.getLayoutParams().width = minWidth + (maxWidth - minWidth) / duration * content.getDuration();

        if (message.getMessageDirection() == Message.MessageDirection.SEND) {
            holder.left.setText(String.format("%s\"", content.getDuration()));
            holder.left.setVisibility(View.VISIBLE);
            holder.right.setVisibility(View.GONE);
            holder.unread.setVisibility(View.GONE);
            holder.img.setScaleType(ImageView.ScaleType.FIT_END);
            holder.img.setBackgroundResource(R.drawable.rc_ic_bubble_right);
            AnimationDrawable animationDrawable = (AnimationDrawable) context.getResources().getDrawable(R.drawable.rc_an_voice_sent);
            if (playing) {
                holder.img.setImageDrawable(animationDrawable);
                if (animationDrawable != null)
                    animationDrawable.start();
            } else {
                holder.img.setImageDrawable(holder.img.getResources().getDrawable(R.drawable.rc_ic_voice_sent));
                if (animationDrawable != null)
                    animationDrawable.stop();
            }
        } else {
            holder.right.setText(String.format("%s\"", content.getDuration()));
            holder.right.setVisibility(View.VISIBLE);
            holder.left.setVisibility(View.GONE);
            if (!message.getReceivedStatus().isListened())
                holder.unread.setVisibility(View.VISIBLE);
            else
                holder.unread.setVisibility(View.GONE);
            holder.img.setBackgroundResource(R.drawable.rc_ic_bubble_left);
            AnimationDrawable animationDrawable = (AnimationDrawable) context.getResources().getDrawable(R.drawable.rc_an_voice_receive);
            if (playing) {
                holder.img.setImageDrawable(animationDrawable);
                if (animationDrawable != null)
                    animationDrawable.start();
            } else {
                holder.img.setImageDrawable(holder.img.getResources().getDrawable(R.drawable.rc_ic_voice_receive));
                if (animationDrawable != null)
                    animationDrawable.stop();
            }
            holder.img.setScaleType(ImageView.ScaleType.FIT_START);
        }
    }

    @Override
    public Spannable getContentSummary(VoiceMessage data) {
        return null;
    }

    @Override
    public Spannable getContentSummary(Context context, VoiceMessage data) {
        if (data.isDestruct()) {
            return new SpannableString(context.getString(R.string.rc_message_content_burn));
        }
        return new SpannableString(context.getString(R.string.rc_message_content_voice));
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    private boolean muteAudioFocus(Context context, boolean bMute) {
        if (context == null) {
            RLog.d(TAG, "muteAudioFocus context is null.");
            return false;
        }
        if (Build.VERSION.SDK_INT < 8) {
            // 2.1以下的版本不支持下面的API：requestAudioFocus和abandonAudioFocus
            RLog.d(TAG, "muteAudioFocus Android 2.1 and below can not stop music");
            return false;
        }
        boolean bool = false;
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (bMute) {
            int result = am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            bool = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        } else {
            int result = am.abandonAudioFocus(null);
            bool = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
        RLog.d(TAG, "muteAudioFocus pauseMusic bMute=" + bMute + " result=" + bool);
        return bool;
    }

    private class VoiceMessagePlayListener implements IAudioPlayListener {
        private Context context;
        private UIMessage message;
        private ViewHolder holder;
        private boolean listened;

        public VoiceMessagePlayListener(Context context, UIMessage message, ViewHolder holder, boolean listened) {
            this.context = context;
            this.message = message;
            this.holder = holder;
            this.listened = listened;
        }

        @Override
        public void onStart(Uri uri) {
            message.continuePlayAudio = false;
            message.setListening(true);
            message.getReceivedStatus().setListened();
            RongIMClient.getInstance().setMessageReceivedStatus(message.getMessageId(), message.getReceivedStatus(), null);
            setLayout(context, holder, message, true);
            EventBus.getDefault().post(new Event.AudioListenedEvent(message.getMessage()));
            if (message.getContent().isDestruct() && message.getMessageDirection().equals(Message.MessageDirection.RECEIVE)) {
                DestructManager.getInstance().stopDestruct(message.getMessage());
            }
        }

        @Override
        public void onStop(Uri uri) {
            if (message.getContent() instanceof VoiceMessage) {
                message.setListening(false);
                setLayout(context, holder, message, false);
                if (message.getContent().isDestruct() && message.getMessageDirection().equals(Message.MessageDirection.RECEIVE)) {
                    DestructManager.getInstance().startDestruct(message.getMessage());
                }
            }

        }

        @Override
        public void onComplete(Uri uri) {
            Event.PlayAudioEvent event = Event.PlayAudioEvent.obtain();
            event.messageId = message.getMessageId();
            //判断是否未听语音消息，在决定是否连续播放。
            if (message.isListening() && message.getMessageDirection().equals(Message.MessageDirection.RECEIVE)) {
                try {
                    event.continuously = context.getResources().getBoolean(R.bool.rc_play_audio_continuous);
                } catch (Resources.NotFoundException e) {
                    e.printStackTrace();
                }
            }
            if (event.continuously && !message.getContent().isDestruct()) {
                EventBus.getDefault().post(event);
            }
            message.setListening(false);
            setLayout(context, holder, message, false);
            if (message.getContent().isDestruct() && message.getMessageDirection().equals(Message.MessageDirection.RECEIVE)) {
                DestructManager.getInstance().startDestruct(message.getMessage());
            }
        }

    }

    private static class DestructListener implements RongIMClient.DestructCountDownTimerListener {
        private WeakReference<VoiceMessageItemProvider.ViewHolder> mHolder;
        private UIMessage mUIMessage;

        public DestructListener(VoiceMessageItemProvider.ViewHolder pHolder, UIMessage pUIMessage) {
            mHolder = new WeakReference<>(pHolder);
            mUIMessage = pUIMessage;
        }

        @Override
        public void onTick(long millisUntilFinished, String messageId) {
            if (mUIMessage.getUId().equals(messageId)) {
                VoiceMessageItemProvider.ViewHolder viewHolder = mHolder.get();
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
                VoiceMessageItemProvider.ViewHolder viewHolder = mHolder.get();
                if (viewHolder != null && messageId.equals(viewHolder.receiverFire.getTag())) {
                    viewHolder.receiverFireText.setVisibility(View.GONE);
                    viewHolder.receiverFireImg.setVisibility(View.VISIBLE);
                    mUIMessage.setUnDestructTime(null);
                }
            }
        }
    }
}
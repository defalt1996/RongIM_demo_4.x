package io.rong.imkit.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.io.File;
import java.lang.ref.WeakReference;

import io.rong.eventbus.EventBus;
import io.rong.imkit.R;
import io.rong.imkit.RongBaseNoActionbarActivity;
import io.rong.imkit.RongContext;
import io.rong.imkit.destruct.DestructManager;
import io.rong.imkit.model.Event;
import io.rong.imkit.utilities.OptionsPopupDialog;
import io.rong.imkit.utilities.PermissionCheckUtil;
import io.rong.imkit.utilities.KitStorageUtils;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Message;
import io.rong.message.GIFMessage;

public class GIFPreviewActivity extends RongBaseNoActionbarActivity {
    TextView mCountDownView;
    Message currentMessage;
    private static final String TAG = "GIFPreviewActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rc_gif_preview);
        mCountDownView = findViewById(R.id.rc_count_down);
        ImageView gifPreview = findViewById(R.id.rc_gif_preview);
        currentMessage = getIntent().getParcelableExtra("message");
        RongContext.getInstance().getEventBus().register(this);
        if (currentMessage == null || currentMessage.getContent() == null || !(currentMessage.getContent() instanceof GIFMessage)) {
            finish();
            return;
        }

        gifPreview.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                GIFMessage gifMessage = (GIFMessage) currentMessage.getContent();
                if (!gifMessage.isDestruct()) {
                    saveGif(gifMessage);
                }
                return false;
            }
        });

        gifPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Window window = GIFPreviewActivity.this.getWindow();
                if (window != null) {
                    window.setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN, WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                }
                finish();
            }
        });

        GIFMessage gifMessage = (GIFMessage) currentMessage.getContent();
        if (gifMessage.isDestruct() && currentMessage.getMessageDirection().equals(Message.MessageDirection.RECEIVE)) {
            DestructManager.getInstance().addListener(currentMessage.getUId(), new DestructListener(mCountDownView, currentMessage.getUId()), TAG);
        }
        if (gifMessage.getLocalUri() != null) {
            Glide.with(this).asGif().addListener(new RequestListener<GifDrawable>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<GifDrawable> target, boolean isFirstResource) {
                    return false;
                }

                @Override
                public boolean onResourceReady(GifDrawable resource, Object model, Target<GifDrawable> target, DataSource dataSource, boolean isFirstResource) {
                    if (currentMessage.getContent().isDestruct()
                            && currentMessage.getMessageDirection() == Message.MessageDirection.RECEIVE
                            && currentMessage.getReadTime() <= 0
                            && !TextUtils.isEmpty(currentMessage.getUId())) {
                        DestructManager.getInstance().startDestruct(currentMessage);
                        EventBus.getDefault().post(new Event.changeDestructionReadTimeEvent(currentMessage));
                    }
                    return false;
                }
            }).load(gifMessage.getLocalUri().getPath()).into(gifPreview);
        }
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    private void saveGif(GIFMessage message) {
        String path = message.getLocalUri().getPath();
        final File file = new File(path);
        if (!file.exists()) {
            return;
        }
        String[] items = new String[]{getString(R.string.rc_save_picture)};
        OptionsPopupDialog.newInstance(GIFPreviewActivity.this, items).setOptionsPopupDialogListener(new OptionsPopupDialog.OnOptionsItemClickedListener() {
            @Override
            public void onOptionsItemClicked(int which) {
                if (which == 0) {
                    String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                    if (!PermissionCheckUtil.requestPermissions(GIFPreviewActivity.this, permissions)) {
                        return;
                    }

                    if (file != null && file.exists()) {
                        String name = "rong_" + System.currentTimeMillis() + ".gif";
                        KitStorageUtils.saveMediaToPublicDir(GIFPreviewActivity.this, file, KitStorageUtils.MediaType.IMAGE);
                        Toast.makeText(GIFPreviewActivity.this, GIFPreviewActivity.this.getString(R.string.rc_save_picture_at), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(GIFPreviewActivity.this, getString(R.string.rc_src_file_not_found), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }).show();
    }

    private static class DestructListener implements RongIMClient.DestructCountDownTimerListener {
        private WeakReference<TextView> mCountDownView;
        private String mMessageId;

        public DestructListener(TextView pCountDownView, String pMessageId) {
            mCountDownView = new WeakReference<>(pCountDownView);
            mMessageId = pMessageId;
        }

        @Override
        public void onTick(long millisUntilFinished, String pMessageId) {
            if (mMessageId.equals(pMessageId)) {
                TextView countDownView = mCountDownView.get();
                if (countDownView != null) {
                    countDownView.setVisibility(View.VISIBLE);
                    countDownView.setText(String.valueOf(Math.max(millisUntilFinished, 1)));
                }
            }
        }


        @Override
        public void onStop(String messageId) {
            if (mMessageId.equals(messageId)) {
                TextView countDownView = mCountDownView.get();
                if (countDownView != null) {
                    countDownView.setVisibility(View.GONE);
                }
            }
        }
    }

    public void onEventMainThread(Event.MessageDeleteEvent deleteEvent) {
        if (deleteEvent.getMessageIds() != null && currentMessage != null) {
            for (int messageId : deleteEvent.getMessageIds()) {
                if (messageId == currentMessage.getMessageId()) {
                    finish();
                    break;
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public void onEventMainThread(Event.RemoteMessageRecallEvent event) {
        if (currentMessage == null) {
            return;
        }
        int messageId = currentMessage.getMessageId();
        if (messageId == event.getMessageId()) {
            new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                    .setMessage(getString(R.string.rc_recall_success))
                    .setPositiveButton(getString(R.string.rc_dialog_ok), new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setCancelable(false)
                    .show();
        }
    }

}

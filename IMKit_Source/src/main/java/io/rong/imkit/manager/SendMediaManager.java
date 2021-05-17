package io.rong.imkit.manager;

import android.net.Uri;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.rong.common.FileUtils;
import io.rong.common.RLog;
import io.rong.imkit.R;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.model.Event;
import io.rong.imkit.utilities.KitStorageUtils;
import io.rong.imkit.utilities.RongUtils;
import io.rong.imkit.utilities.videocompressor.VideoCompress;
import io.rong.imlib.IRongCallback;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.message.SightMessage;

public class SendMediaManager {
    private final static String TAG = SendMediaManager.class.getSimpleName();

    private ExecutorService executorService;
    private UploadController uploadController;

    static class SingletonHolder {
        static SendMediaManager sInstance = new SendMediaManager();
    }

    public static SendMediaManager getInstance() {
        return SingletonHolder.sInstance;
    }

    private SendMediaManager() {
        executorService = getExecutorService();
        uploadController = new UploadController();
    }

    public void sendMedia(Conversation.ConversationType conversationType, String targetId, List<Uri> mediaList, boolean isFull) {
        sendMedia(conversationType, targetId, mediaList, isFull, false, 0);
    }

    public void sendMedia(Conversation.ConversationType conversationType, String targetId, List<Uri> mediaList, boolean isFull, boolean isDestruct, long destructTime) {
        RLog.d(TAG, "The size of media is " + mediaList.size());
        if (RongIM.getInstance().getApplicationContext() == null) {
            return;
        }
        for (Uri mediaUri : mediaList) {
            if (!TextUtils.isEmpty(mediaUri.toString())) {
                if (!FileUtils.isFileExistsWithUri(RongIM.getInstance().getApplicationContext(), mediaUri))
                    continue;
                int mediaDuration = RongUtils.getVideoDuration(RongIM.getInstance().getApplicationContext(), mediaUri.toString());
                SightMessage sightMessage = SightMessage.obtain(mediaUri, mediaDuration / 1000);
                if (isDestruct) {
                    sightMessage.setDestructTime(destructTime);
                }
                RongIM.OnSendMessageListener listener = RongContext.getInstance().getOnSendMessageListener();
                if (listener != null) {
                    Message message = listener.onSend(Message.obtain(targetId, conversationType, sightMessage));
                    if (message != null) {
                        RongIMClient.getInstance().insertOutgoingMessage(conversationType,
                                targetId,
                                null,
                                message.getContent(),
                                new RongIMClient.ResultCallback<Message>() {
                                    @Override
                                    public void onSuccess(Message message) {
                                        message.setSentStatus(Message.SentStatus.SENDING);
                                        RongIMClient.getInstance().setMessageSentStatus(message, null);
                                        RongContext.getInstance().getEventBus().post(message);
                                        uploadController.execute(message);
                                    }

                                    @Override
                                    public void onError(RongIMClient.ErrorCode e) {

                                    }
                                });
                    }
                } else {
                    RongIMClient.getInstance().insertOutgoingMessage(conversationType,
                            targetId,
                            null,
                            sightMessage,
                            new RongIMClient.ResultCallback<Message>() {
                                @Override
                                public void onSuccess(Message message) {
                                    message.setSentStatus(Message.SentStatus.SENDING);
                                    RongIMClient.getInstance().setMessageSentStatus(message, null);
                                    RongContext.getInstance().getEventBus().post(message);
                                    uploadController.execute(message);
                                }

                                @Override
                                public void onError(RongIMClient.ErrorCode e) {

                                }
                            });
                }
            }
        }
    }

    public void cancelSendingMedia(Conversation.ConversationType conversationType, String targetId) {
        RLog.d(TAG, "cancel Sending media");
        if (conversationType != null && targetId != null && uploadController != null)
            uploadController.cancel(conversationType, targetId);
    }

    public void cancelSendingMedia(Conversation.ConversationType conversationType, String targetId, int messageId) {
        RLog.d(TAG, "cancel Sending media");
        if (conversationType != null && targetId != null && uploadController != null && messageId > 0)
            uploadController.cancel(conversationType, targetId, messageId);
    }

    public void reset() {
        uploadController.reset();
    }

    private class UploadController implements Runnable {
        final List<Message> pendingMessages;
        Message executingMessage;

        public UploadController() {
            this.pendingMessages = new ArrayList<>();
        }

        public void execute(Message message) {
            synchronized (pendingMessages) {
                pendingMessages.add(message);
                if (executingMessage == null) {
                    executingMessage = pendingMessages.remove(0);
                    executorService.submit(this);
                }
            }
        }

        public void reset() {
            RLog.w(TAG, "Reset Sending media.");
            synchronized (pendingMessages) {
                for (Message message : pendingMessages) {
                    message.setSentStatus(Message.SentStatus.FAILED);
                    RongContext.getInstance().getEventBus().post(message);
                }
                pendingMessages.clear();
            }
            if (executingMessage != null) {
                executingMessage.setSentStatus(Message.SentStatus.FAILED);
                RongContext.getInstance().getEventBus().post(executingMessage);
                executingMessage = null;
            }
        }

        public void cancel(Conversation.ConversationType conversationType, String targetId) {
            synchronized (pendingMessages) {
                int count = pendingMessages.size();
                for (int i = 0; i < count; i++) {
                    Message msg = pendingMessages.get(i);
                    if (msg.getConversationType().equals(conversationType) && msg.getTargetId().equals(targetId)) {
                        pendingMessages.remove(msg);
                    }
                }
                if (pendingMessages.size() == 0)
                    executingMessage = null;
            }
        }

        public void cancel(Conversation.ConversationType conversationType, String targetId, int messageId) {
            synchronized (pendingMessages) {
                int count = pendingMessages.size();
                for (int i = 0; i < count; i++) {
                    Message msg = pendingMessages.get(i);
                    if (msg.getConversationType().equals(conversationType)
                            && msg.getTargetId().equals(targetId)
                            && msg.getMessageId() == messageId) {
                        pendingMessages.remove(msg);
                        break;
                    }
                }
                if (pendingMessages.size() == 0)
                    executingMessage = null;
            }
        }

        private void polling() {
            synchronized (pendingMessages) {
                RLog.d(TAG, "polling " + pendingMessages.size());
                if (pendingMessages.size() > 0) {
                    executingMessage = pendingMessages.remove(0);
                    executorService.submit(this);
                } else {
                    executingMessage = null;
                }
            }
        }

        @Override
        public void run() {
            final Event.OnReceiveMessageProgressEvent result = new Event.OnReceiveMessageProgressEvent();
            result.setMessage(executingMessage);
            final String originLocalPath = ((SightMessage) executingMessage.getContent()).getLocalPath().toString().substring(7);
            final String compressPath = KitStorageUtils.getImageSavePath(RongIM.getInstance().getApplicationContext()) + File.separator
                    + "VID_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(new Date()) + ".mp4";
            VideoCompress.compressVideo(originLocalPath, compressPath, new VideoCompress.CompressListener() {
                @Override
                public void onStart() {
                    RongContext.getInstance().getEventBus().post(result);
                    RLog.d(TAG, "Compressing video file starts.");
                }

                @Override
                public void onSuccess() {
                    RLog.d(TAG, "Compressing video file successes.");
                    if (executingMessage == null) {
                        return;
                    }
                    ((SightMessage) executingMessage.getContent()).setLocalPath(Uri.parse("file://" + compressPath));
                    boolean isDestruct = false;
                    if (executingMessage.getContent() != null)
                        isDestruct = executingMessage.getContent().isDestruct();
                    String filePath = Uri.parse("file://" + compressPath).toString().substring(7);
                    File file = new File(filePath);
                    ((SightMessage) executingMessage.getContent()).setSize(file.length());
                    RongIM.getInstance().sendMediaMessage(executingMessage, isDestruct ? RongIM.getInstance().getApplicationContext().getString(R.string.rc_message_content_burn) : null,
                            null, new IRongCallback.ISendMediaMessageCallback() {
                                @Override
                                public void onAttached(Message message) {

                                }

                                @Override
                                public void onSuccess(Message message) {
                                    polling();
                                }

                                @Override
                                public void onError(Message message, RongIMClient.ErrorCode errorCode) {
                                    //FileUtils.removeFile(compressPath);
                                    polling();
                                }

                                @Override
                                public void onProgress(Message message, int progress) {

                                }

                                @Override
                                public void onCanceled(Message message) {

                                }
                            });
                    RLog.d(TAG, "Compressing video file successes.");
                }

                @Override
                public void onFail() {
                    Toast.makeText(RongIM.getInstance().getApplicationContext(), RongIM.getInstance().getApplicationContext().getString(R.string.rc_picsel_video_corrupted),
                            Toast.LENGTH_SHORT).show();
                    polling();
                    RLog.d(TAG, "Compressing video file failed.");
                }

                @Override
                public void onProgress(float percent) {
                    RLog.d(TAG, "The progress of compressing video file is " + percent);
                }
            });
        }
    }

    private ExecutorService getExecutorService() {
        if (executorService == null) {
            executorService = new ThreadPoolExecutor(1,
                    Integer.MAX_VALUE,
                    60,
                    TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(),
                    threadFactory());
        }
        return executorService;
    }

    private ThreadFactory threadFactory() {
        return new ThreadFactory() {
            @Override
            public Thread newThread(@Nullable Runnable runnable) {
                Thread result = new Thread(runnable, "Rong SendMediaManager");
                result.setDaemon(false);
                return result;
            }
        };
    }
}

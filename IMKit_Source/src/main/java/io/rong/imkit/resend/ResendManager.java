package io.rong.imkit.resend;

import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;

import java.util.Hashtable;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.rong.common.RLog;
import io.rong.imkit.RongIM;
import io.rong.imlib.IRongCallback;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Message;
import io.rong.message.ImageMessage;
import io.rong.message.LocationMessage;
import io.rong.message.MediaMessageContent;
import io.rong.message.ReferenceMessage;

/**
 * 用于管理重新发送
 */
public class ResendManager {

    private final String TAG = "ResendManager";
    // 发送消息间隔
    private static final int TIME_DELAY = 300;
    private volatile boolean mIsProcessing = false;

    private Hashtable<Integer, Message> mMessageMap;
    private ConcurrentLinkedQueue<Integer> mMessageQueue;
    private Handler mResendHandler;


    private ResendManager() {
        mMessageMap = new Hashtable<>();
        mMessageQueue = new ConcurrentLinkedQueue<>();
        HandlerThread resendThread = new HandlerThread("RESEND_WORK");
        resendThread.start();
        mResendHandler = new Handler(resendThread.getLooper());
    }

    private static class ResendManagerHolder {
        private static ResendManager instance = new ResendManager();
    }

    public static ResendManager getInstance() {
        return ResendManager.ResendManagerHolder.instance;
    }

    public void addResendMessage(final Message message, final RongIMClient.ErrorCode errorCode, final AddResendMessageCallBack callBack) {
        mResendHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isResendErrorCode(errorCode)) {
                    if (!mMessageMap.containsKey(message.getMessageId())) {
                        RLog.d(TAG, "addResendMessage : id=" + message.getMessageId());
                        if (mMessageMap != null && mMessageQueue != null) {
                            mMessageMap.put(message.getMessageId(), message);
                            mMessageQueue.add(message.getMessageId());
                            beginResend();
                            message.setSentStatus(Message.SentStatus.SENDING);
                        }
                    }
                }
                callBack.onComplete(message, errorCode);
            }
        });
    }

    public void removeResendMessage(final int messageId) {
        mResendHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mMessageMap != null) {
                    mMessageMap.remove(messageId);
                    mMessageQueue.remove(messageId);
                }
            }
        });
    }

    public void removeAllResendMessage() {
        mResendHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mMessageMap != null) {
                    mMessageMap.clear();
                    mMessageQueue.clear();
                    mIsProcessing = false;
                }
            }
        });
    }

    /**
     * 是否为需要重发处理的错误码
     *
     * @param errorCode 发送消息的失败错误码
     * @return 是否重发，true 会重发， false 不需要重发处理。
     */
    public boolean isResendErrorCode(RongIMClient.ErrorCode errorCode) {
        return (errorCode.equals(RongIMClient.ErrorCode.RC_NET_CHANNEL_INVALID)
                || errorCode.equals(RongIMClient.ErrorCode.RC_NET_UNAVAILABLE)
                || errorCode.equals(RongIMClient.ErrorCode.RC_MSG_RESP_TIMEOUT));
    }

    public boolean needResend(int messageId) {
        if (mMessageMap == null) {
            return false;
        }
        return mMessageMap.containsKey(messageId);
    }

    public void beginResend() {
        mResendHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mMessageMap == null || mMessageMap.size() == 0) {
                    RLog.i(TAG, "beginResend onChanged no message need resend");
                    mIsProcessing = false;
                    return;
                }
                if (mIsProcessing) {
                    RLog.i(TAG, "beginResend ConnectionStatus is resending");
                    return;
                }
                mIsProcessing = true;
                loopResendMessage();
            }
        });
    }

    private void loopResendMessage() {
        mResendHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                final Integer idInteger = mMessageQueue.peek();
                RLog.d(TAG, "beginResend: messageId = " + idInteger);
                if (idInteger == null
                        || RongIM.getInstance().getCurrentConnectionStatus() != RongIMClient.ConnectionStatusListener.ConnectionStatus.CONNECTED) {
                    mIsProcessing = false;
                    return;
                }
                resendMessage(mMessageMap.get(idInteger), new IRongCallback.ISendMessageCallback() {
                    @Override
                    public void onAttached(Message message) {

                    }

                    @Override
                    public void onSuccess(Message message) {
                        RLog.i(TAG, "resendMessage success messageId = " + message.getMessageId());
                        removeResendMessage(idInteger);
                        loopResendMessage();
                    }

                    @Override
                    public void onError(Message message, RongIMClient.ErrorCode errorCode) {
                        RLog.i(TAG, "resendMessage error messageId = " + message.getMessageId());
                        if (isResendErrorCode(errorCode)) {
                            loopResendMessage();
                        } else {
                            removeResendMessage(idInteger);
                            loopResendMessage();
                        }
                    }
                });
            }
        }, TIME_DELAY);
    }

    /**
     * 重发洗消息
     *
     * @param message 消息
     */
    private void resendMessage(Message message, final IRongCallback.ISendMessageCallback callback) {
        if (message == null) {
            RLog.i(TAG, "resendMessage: Message is Null");
            return;
        }
        if (TextUtils.isEmpty(message.getTargetId()) || message.getContent() == null) {
            RLog.e(TAG, "targetId or messageContent is Null");
            removeResendMessage(message.getMessageId());
            return;
        }
        if (message.getContent() instanceof ImageMessage) {
            ImageMessage imageMessage = (ImageMessage) message.getContent();
            if (imageMessage.getRemoteUri() != null && !imageMessage.getRemoteUri().toString().startsWith("file")) {
                RongIM.getInstance().sendMessage(message, null, null, callback);
            } else {
                RongIM.getInstance().sendImageMessage(message, null, null, new RongIMClient.SendImageMessageCallback() {
                    @Override
                    public void onAttached(Message message) {

                    }

                    @Override
                    public void onError(Message message, RongIMClient.ErrorCode code) {
                        callback.onError(message, code);
                    }

                    @Override
                    public void onSuccess(Message message) {
                        callback.onSuccess(message);
                    }

                    @Override
                    public void onProgress(Message message, int progress) {

                    }
                });
            }
        } else if (message.getContent() instanceof LocationMessage) {
            RongIM.getInstance().sendLocationMessage(message, null, null, callback);
        } else if (message.getContent() instanceof ReferenceMessage) {
            RongIM.getInstance().sendMessage(message, null, null, callback);
        } else if (message.getContent() instanceof MediaMessageContent) {
            MediaMessageContent mediaMessageContent = (MediaMessageContent) message.getContent();
            if (mediaMessageContent.getMediaUrl() != null) {
                RongIM.getInstance().sendMessage(message, null, null, callback);
            } else {
                RongIM.getInstance().sendMediaMessage(message, null, null, new IRongCallback.ISendMediaMessageCallback() {
                    @Override
                    public void onProgress(Message message, int progress) {

                    }

                    @Override
                    public void onCanceled(Message message) {

                    }

                    @Override
                    public void onAttached(Message message) {

                    }

                    @Override
                    public void onSuccess(Message message) {
                        callback.onSuccess(message);
                    }

                    @Override
                    public void onError(Message message, RongIMClient.ErrorCode errorCode) {
                        callback.onError(message, errorCode);
                    }
                });
            }
        } else {
            RongIM.getInstance().sendMessage(message, null, null, callback);
        }
    }

    public interface AddResendMessageCallBack {
        void onComplete(Message message, RongIMClient.ErrorCode errorCode);
    }
}

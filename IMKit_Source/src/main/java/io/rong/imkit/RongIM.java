package io.rong.imkit;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.view.View;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import io.rong.common.RLog;
import io.rong.common.SystemUtils;
import io.rong.eventbus.EventBus;
import io.rong.imageloader.core.ImageLoader;
import io.rong.imageloader.core.assist.FailReason;
import io.rong.imageloader.core.listener.ImageLoadingListener;
import io.rong.imageloader.core.listener.ImageLoadingProgressListener;
import io.rong.imkit.manager.AudioRecordManager;
import io.rong.imkit.manager.IUnReadMessageObserver;
import io.rong.imkit.manager.InternalModuleManager;
import io.rong.imkit.manager.SendImageManager;
import io.rong.imkit.manager.SendMediaManager;
import io.rong.imkit.manager.UnReadMessageManager;
import io.rong.imkit.mention.RongMentionManager;
import io.rong.imkit.message.CombineMessage;
import io.rong.imkit.model.ConversationKey;
import io.rong.imkit.model.Event;
import io.rong.imkit.model.GroupUserInfo;
import io.rong.imkit.model.UIConversation;
import io.rong.imkit.notification.MessageNotificationManager;
import io.rong.imkit.plugin.image.AlbumBitmapCacheHelper;
import io.rong.imkit.reference.ReferenceMessageItemProvider;
import io.rong.imkit.resend.ResendManager;
import io.rong.imkit.userInfoCache.RongUserInfoManager;
import io.rong.imkit.utilities.KitCommonDefine;
import io.rong.imkit.utils.ImageDownloadManager;
import io.rong.imkit.voiceMessageDownload.HQVoiceMsgDownloadManager;
import io.rong.imkit.widget.provider.CSPullLeaveMsgItemProvider;
import io.rong.imkit.widget.provider.CombineMessageItemProvider;
import io.rong.imkit.widget.provider.DiscussionNotificationMessageItemProvider;
import io.rong.imkit.widget.provider.FileMessageItemProvider;
import io.rong.imkit.widget.provider.GIFMessageItemProvider;
import io.rong.imkit.widget.provider.GroupNotificationMessageItemProvider;
import io.rong.imkit.widget.provider.HQVoiceMessageItemProvider;
import io.rong.imkit.widget.provider.HandshakeMessageItemProvider;
import io.rong.imkit.widget.provider.HistoryDividerMessageProvider;
import io.rong.imkit.widget.provider.IContainerItemProvider;
import io.rong.imkit.widget.provider.ImageMessageItemProvider;
import io.rong.imkit.widget.provider.InfoNotificationMsgItemProvider;
import io.rong.imkit.widget.provider.LocationMessageItemProvider;
import io.rong.imkit.widget.provider.PublicServiceMultiRichContentMessageProvider;
import io.rong.imkit.widget.provider.PublicServiceRichContentMessageProvider;
import io.rong.imkit.widget.provider.RealTimeLocationMessageProvider;
import io.rong.imkit.widget.provider.RecallMessageItemProvider;
import io.rong.imkit.widget.provider.RichContentMessageItemProvider;
import io.rong.imkit.widget.provider.TextMessageItemProvider;
import io.rong.imkit.widget.provider.UnknownMessageItemProvider;
import io.rong.imkit.widget.provider.VoiceMessageItemProvider;
import io.rong.imlib.AnnotationNotFoundException;
import io.rong.imlib.IRongCallback;
import io.rong.imlib.MessageTag;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.common.SharedPreferencesUtils;
import io.rong.imlib.model.CSCustomServiceInfo;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.ConversationStatus;
import io.rong.imlib.model.Discussion;
import io.rong.imlib.model.Group;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageConfig;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.PublicServiceProfile;
import io.rong.imlib.model.PublicServiceProfileList;
import io.rong.imlib.model.RCEncryptedSession;
import io.rong.imlib.model.RemoteHistoryMsgOption;
import io.rong.imlib.model.SendMessageOption;
import io.rong.imlib.model.UserInfo;
import io.rong.message.FileMessage;
import io.rong.message.GIFMessage;
import io.rong.message.ImageMessage;
import io.rong.message.InformationNotificationMessage;
import io.rong.message.LocationMessage;
import io.rong.message.MediaMessageContent;
import io.rong.message.RecallNotificationMessage;
import io.rong.message.TextMessage;
import io.rong.push.RongPushClient;

/**
 * IM 界面组件核心类。
 * <p/>
 * 所有 IM 相关界面、功能都由此调用和设置。
 */
public class RongIM {

    private static final String TAG = RongIM.class.getSimpleName();
    public static final int ON_SUCCESS_CALLBACK = 100;
    public static final int ON_PROGRESS_CALLBACK = 101;
    public static final int ON_CANCEL_CALLBACK = 102;
    public static final int ON_ERROR_CALLBACK = 103;

    private Context mApplicationContext;
    private Handler mWorkHandler;
    static RongIMClient.OnReceiveMessageListener sMessageListener;
    static RongIMClient.ConnectionStatusListener sConnectionStatusListener;
    private static MessageInterceptor messageInterceptor;
    private static boolean notificationQuiteHoursConfigured;
    private List<RongIMClient.EncryptedSessionConnectionListener> mEncSessionListeners;

    /**
     * 实例化客户端核心类 RongIM，同时初始化 SDK。
     */
    private RongIM() {
        mEncSessionListeners = new LinkedList<>();

        HandlerThread workThread = new HandlerThread("KIT_WORK");
        workThread.start();
        mWorkHandler = new Handler(workThread.getLooper());

        RongIMClient.getInstance().setEncryptedSessionConnectionListener(
                new RongIMClient.EncryptedSessionConnectionListener() {

                    @Override
                    public void onEncryptedSessionRequest(String targetId, boolean isSuccess) {
                        RLog.d(TAG, "发送加密会话请求");
                        if (mEncSessionListeners != null) {
                            for (int i = 0; i < mEncSessionListeners.size(); i++) {
                                if (mEncSessionListeners.get(i) != null) {
                                    mEncSessionListeners.get(i).onEncryptedSessionRequest(targetId, isSuccess);
                                }
                            }
                        }
                    }

                    @Override
                    public void onEncryptedSessionResponse(String targetId) {
                        RLog.d(TAG, "发送加密会话响应");
                        if (mEncSessionListeners != null) {
                            for (int i = 0; i < mEncSessionListeners.size(); i++) {
                                if (mEncSessionListeners.get(i) != null) {
                                    mEncSessionListeners.get(i).onEncryptedSessionResponse(targetId);
                                }
                            }
                        }
                    }

                    @Override
                    public void onEncryptedSessionEstablished(String targetId) {
                        RLog.d(TAG, "加密会话建立");
                        // get userId from remoteEncId (targetId)
                        if (targetId == null || targetId.length() == 0) {
                            RLog.e(TAG, "encrypted session establish failed because the targetId is null or empty.");
                            return;
                        }
                        if (mEncSessionListeners != null) {
                            for (int i = 0; i < mEncSessionListeners.size(); i++) {
                                if (mEncSessionListeners.get(i) != null) {
                                    mEncSessionListeners.get(i).onEncryptedSessionEstablished(targetId);
                                }
                            }
                        }
                    }

                    @Override
                    public void onEncryptedSessionCanceled(String targetId) {
                        // 请求取消
                        RLog.d(TAG, "取消加密会话请求");
                        if (mEncSessionListeners != null) {
                            for (int i = 0; i < mEncSessionListeners.size(); i++) {
                                if (mEncSessionListeners.get(i) != null) {
                                    mEncSessionListeners.get(i).onEncryptedSessionCanceled(targetId);
                                }
                            }
                        }
                    }

                    @Override
                    public void onEncryptedSessionTerminated(String targetId) {
                        // 会话终结
                        RLog.d(TAG, "结束加密会话请求");
                        if (mEncSessionListeners != null) {
                            for (int i = 0; i < mEncSessionListeners.size(); i++) {
                                if (mEncSessionListeners.get(i) != null) {
                                    mEncSessionListeners.get(i).onEncryptedSessionTerminated(targetId);
                                }
                            }
                        }
                    }
                }
        );

    }

    /**
     * 注册listener
     *
     * @param listener 接口对象
     */
    public void registerEncSessionListener(RongIMClient.EncryptedSessionConnectionListener listener) {
        if (listener != null && !mEncSessionListeners.contains(listener)) {
            mEncSessionListeners.add(listener);
        }
    }

    /**
     * 解注listener
     *
     * @param listener 接口对象
     */
    public void unRegisterEncSessionListener(RongIMClient.EncryptedSessionConnectionListener listener) {
        if (listener != null) {
            mEncSessionListeners.remove(listener);
        }
    }

    /**
     * 判断listener是否被注册
     *
     * @param listener 待判断的接口对象
     * @return 已经注册则返回true，否则返回false。
     */
    public boolean isRegisted(RongIMClient.EncryptedSessionConnectionListener listener) {
        return mEncSessionListeners != null && mEncSessionListeners.contains(listener);
    }

    private void saveToken(String token) {
        SharedPreferences preferences = SharedPreferencesUtils.get(mApplicationContext, KitCommonDefine.RONG_KIT_SP_CONFIG, Context.MODE_PRIVATE);
        Editor editor = preferences.edit();
        editor.putString("token", token);
        editor.commit();// 提交数据到背后的xml文件中
    }

    static class SingletonHolder {
        static RongIM sRongIM = new RongIM();
    }

    /**
     * 初始化 SDK，在整个应用程序全局，只需要调用一次。
     *
     * @param context 应用上下文。
     */
    private void initSDK(Context context, String appKey, boolean enablePush) {
        mApplicationContext = context;
        String current = io.rong.common.SystemUtils.getCurrentProcessName(context);
        String mainProcessName = context.getPackageName();
        if (!TextUtils.isEmpty(current) && !mainProcessName.equals(current)) {
            RLog.w(TAG, "Init. Current process : " + current);
            return;
        }
        RLog.i(TAG, "init : " + current);
        RongContext.getInstance().initRegister();
        RongConfigurationManager.init(context);
        RongMessageItemLongClickActionManager.getInstance().init();
        initListener();
        if (TextUtils.isEmpty(appKey)) {
            try {
                ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
                appKey = applicationInfo.metaData.getString("RONG_CLOUD_APP_KEY");
                if (TextUtils.isEmpty(appKey) || !SystemUtils.isValidAppKey(appKey)) {
                    throw new IllegalArgumentException("can't find RONG_CLOUD_APP_KEY in AndroidManifest.xml.");
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                throw new ExceptionInInitializerError("can't find packageName!");
            }
        }
        RongUserInfoManager.getInstance().init(mApplicationContext, appKey, new RongUserCacheListener());
        RongIMClient.init(context, appKey, enablePush);

        registerMessageTemplate(new TextMessageItemProvider());
        registerMessageTemplate(new ImageMessageItemProvider());
        registerMessageTemplate(new GIFMessageItemProvider());
        registerMessageTemplate(new LocationMessageItemProvider());
        registerMessageTemplate(new VoiceMessageItemProvider(context));
        registerMessageTemplate(new HQVoiceMessageItemProvider());
        registerMessageTemplate(new DiscussionNotificationMessageItemProvider());
        registerMessageTemplate(new InfoNotificationMsgItemProvider());
        registerMessageTemplate(new RichContentMessageItemProvider());
        registerMessageTemplate(new PublicServiceMultiRichContentMessageProvider());
        registerMessageTemplate(new PublicServiceRichContentMessageProvider());
        registerMessageTemplate(new HandshakeMessageItemProvider());
        registerMessageTemplate(new RecallMessageItemProvider());
        registerMessageTemplate(new FileMessageItemProvider());
        registerMessageTemplate(new GroupNotificationMessageItemProvider());
        registerMessageTemplate(new RealTimeLocationMessageProvider());
        registerMessageTemplate(new UnknownMessageItemProvider());
        registerMessageTemplate(new CSPullLeaveMsgItemProvider());
        registerMessageTemplate(new HistoryDividerMessageProvider());
        registerMessageTemplate(new CombineMessageItemProvider());
        registerMessageTemplate(new ReferenceMessageItemProvider());

        registerMessageType(CombineMessage.class);


        RongExtensionManager.init(context, appKey);
        RongExtensionManager.getInstance().registerExtensionModule(new DefaultExtensionModule(context));

        InternalModuleManager.init(context);
        InternalModuleManager.getInstance().onInitialized(appKey);
        AlbumBitmapCacheHelper.init(context);
        ImageDownloadManager.init(context);
        HQVoiceMsgDownloadManager.getInstance().init(context);
        RongNotificationManager.getInstance().init(mApplicationContext);
        ImageLoader.getInstance().init(RongContext.getInstance().getDefaultConfig(mApplicationContext));
    }

    /**
     * 初始化 SDK，在整个应用程序全局只需要调用一次, 建议在 Application 继承类中调用。
     * <p>
     * 初始化后，SDK 会注册 activity 生命周期，用于判断应用处于前、后台，根据前后台只能调整链接心跳
     *
     * @param application 传入 Application 的类。
     */
    public static void init(Application application, String appKey) {
        SingletonHolder.sRongIM.initSDK(application, appKey, true);
    }

    /**
     * 初始化 SDK，在整个应用程序全局，只需要调用一次。建议在 Application 继承类中调用。
     *
     * @param appKey  应用的app key.
     * @param context 应用上下文。
     */
    public static void init(Context context, String appKey) {
        SingletonHolder.sRongIM.initSDK(context, appKey, true);
    }

    /**
     * <p>初始化 SDK，在整个应用程序全局只需要调用一次, 建议在 Application 继承类中调用。
     * 调用此接口传入 AppKey 与在 AndroidManifest.xml 里写入 RONG_CLOUD_APP_KEY 是同样效果，二选一即可。</p>
     *
     * @param context    传入Application类的Context。
     * @param appKey     融云注册应用的AppKey。
     * @param enablePush 是否使用推送功能。false 代表不使用推送相关功能, SDK 里将不会携带推送相关文件。
     */
    public static void init(Context context, String appKey, boolean enablePush) {
        SingletonHolder.sRongIM.initSDK(context, appKey, enablePush);
    }

    /**
     * 初始化 SDK，在整个应用程序全局，只需要调用一次。建议在 Application 继承类中调用。
     *
     * @param context 应用上下文。
     */
    public static void init(Context context) {
        SingletonHolder.sRongIM.initSDK(context, null, true);
    }

    /**
     * 异步初始化 SDK，在整个应用程序全局，只需要调用一次。建议在 Application 继承类中调用。
     * 此初始化会切换至工作线程中进行,减少初始化 SDK 所占用的时间。
     * 通过 {@link AsyncInitListener} 接口，可以异步去执行需要在 SDK 初始化之前和之后跟 SDK 相关的业务。
     *
     * @param context           传入Application类的Context。
     * @param appKey            融云注册应用的AppKey。
     * @param enablePush        是否使用推送功能。false 代表不使用推送相关功能, SDK 里将不会携带推送相关文件。
     * @param asyncInitListener 异步初始化时执行初始化前后事件的监听
     */
    public static void initAsync(final Context context, final String appKey, final boolean enablePush, final AsyncInitListener asyncInitListener) {
        SingletonHolder.sRongIM.mWorkHandler.post(new Runnable() {
            @Override
            public void run() {
                if (asyncInitListener != null) {
                    asyncInitListener.doBeforeInit();
                    SingletonHolder.sRongIM.initSDK(context, appKey, enablePush);
                    asyncInitListener.doAfterInit();
                } else {
                    SingletonHolder.sRongIM.initSDK(context, appKey, enablePush);
                }
            }
        });
    }

    /**
     * 异步初始化 SDK 时，回调初始化前后的事件。
     * 通过实现此监听可以将在初始化 SDK 前后的关于 SDK 相关的事件进行异步处理。
     */
    public interface AsyncInitListener {
        /**
         * 在此回调中执行初始化 SDK 之前的操作<BR/>
         * 如：
         * 配置 融云 IM 消息推送： {@link RongPushClient#setPushConfig}<BR/>
         * 具体哪些方法需要在初始化 SDK 之前进行，可参考官方文档对该方法的描述。
         */
        void doBeforeInit();

        /**
         * 在此回调中执行初始化 SDK 之后的操作<BR/>
         * 如：
         * 初始化自定义消息：{@link RongIM#registerMessageType(Class)}<BR/>
         * 具体哪些方法需要在初始化 SDK 之后进行，可参考官方文档对该方法的描述。
         */
        void doAfterInit();
    }

    /**
     * 注册消息类型，如果不对消息类型进行扩展，可以忽略此方法。
     *
     * @param messageContentClass 消息类型，必须要继承自 io.rong.imlib.model.MessageContent。
     */
    public static void registerMessageType(Class<? extends MessageContent> messageContentClass) {
        if (RongContext.getInstance() != null) {
            RongIMClient.registerMessageType(messageContentClass);
        }
    }

    /**
     * 注册消息模板。
     *
     * @param provider 模板类型。
     */
    public static void registerMessageTemplate(IContainerItemProvider.MessageProvider provider) {
        if (RongContext.getInstance() != null) {
            RongContext.getInstance().registerMessageTemplate(provider);
        }
    }

    /**
     * 设置当前用户信息。
     * 如果开发者没有实现用户信息提供者，而是使用消息携带用户信息，需要使用这个方法设置当前用户的信息，
     * 然后在{@link #init(Context)}之后调用{@link #setMessageAttachedUserInfo(boolean)}，
     * 这样可以在每条消息中携带当前用户的信息，IMKit会在接收到消息的时候取出用户信息并刷新到界面上。
     *
     * @param userInfo 当前用户信息。
     */
    public void setCurrentUserInfo(UserInfo userInfo) {
        if (RongContext.getInstance() != null) {
            RongContext.getInstance().setCurrentUserInfo(userInfo);
        }
    }

    /**
     * 连接服务器，在整个应用程序全局，只需要调用一次。
     *
     * @param token           从服务端获取的 <a
     *                        href="http://docs.rongcloud.cn/android#token">用户身份令牌（
     *                        Token）</a>。
     * @param connectCallback 连接服务器的回调扩展类，新增打开数据库的回调，用户可以在此回调中执行拉取会话列表操作。
     * @return RongIM IM 客户端核心类的实例。
     * @discussion 调用该接口，SDK 会在连接失败之后尝试重连，将出现以下两种情况：
     * 第一、连接成功，回调 onSuccess(userId)。
     * 第二、出现 SDK 无法处理的错误，回调 onError(errorCode)（如 token 非法），并不再重连。
     * <p>
     * 如果您不想一直进行重连，可以使用 connect(String,int,ConnectCallback) 接口并设置连接超时时间 timeLimit。
     * @discussion 连接成功后，SDK 将接管所有的重连处理。当因为网络原因断线的情况下，SDK 会不停重连直到连接成功为止，不需要您做额外的连接操作。
     */
    public static RongIM connect(final String token, final RongIMClient.ConnectCallback connectCallback) {
        return connect(token, -1, connectCallback);
    }


    /**
     * 连接服务器，在整个应用程序全局，只需要调用一次。
     *
     * @param token           从服务端获取的 <a
     *                        href="http://docs.rongcloud.cn/android#token">用户身份令牌（
     *                        Token）</a>。
     * @param timeLimit       连接超时时间，单位：秒。timeLimit <= 0，则 IM 将一直连接，直到连接成功或者无法连接（如 token 非法）
     *                        timeLimit > 0 ,则 IM 将最多连接 timeLimit 秒：
     *                        如果在 timeLimit 秒内连接成功，后面再发生了网络变化或前后台切换，SDK 会自动重连；
     *                        如果在 timeLimit 秒无法连接成功则不再进行重连，通过 onError 告知连接超时，您需要再自行调用 connect 接口
     * @param connectCallback 连接服务器的回调扩展类，新增打开数据库的回调，用户可以在此回调中执行拉取会话列表操作。
     * @return RongIM IM 客户端核心类的实例。
     * @discussion 调用该接口，SDK 会在 timeLimit 秒内尝试重连，直到出现下面三种情况之一：
     * 第一、连接成功，回调 onSuccess(userId)。
     * 第二、超时，回调 onError(RC_CONNECT_TIMEOUT)，并不再重连。
     * 第三、出现 SDK 无法处理的错误，回调 onError(errorCode)（如 token 非法），并不再重连。
     * @discussion 连接成功后，SDK 将接管所有的重连处理。当因为网络原因断线的情况下，SDK 会不停重连直到连接成功为止，不需要您做额外的连接操作。
     */
    public static RongIM connect(final String token, final int timeLimit, final RongIMClient.ConnectCallback connectCallback) {
        RongIMClient.connect(token, timeLimit, new RongIMClient.ConnectCallback() {
            @Override
            public void onSuccess(String userId) {
                RongIM.getInstance().saveToken(token);
                if (connectCallback != null) {
                    connectCallback.onSuccess(userId);
                }
                RongContext.getInstance().getEventBus().post(Event.ConnectEvent.obtain(true));
                RongExtensionManager.getInstance().connect(token);
                InternalModuleManager.getInstance().onConnected(token);
                if (!notificationQuiteHoursConfigured) {
                    SingletonHolder.sRongIM.getNotificationQuietHours(null);
                }
            }

            @Override
            public void onError(RongIMClient.ConnectionErrorCode e) {
                if (connectCallback != null) {
                    connectCallback.onError(e);
                }
                RongExtensionManager.getInstance().connect(token);
                InternalModuleManager.getInstance().onConnected(token);
                if (RongContext.getInstance() == null) {
                    RLog.d(TAG, "connect - onError. RongIM SDK not init, ConnectEvent do not post.");
                } else {
                    RongContext.getInstance().getEventBus().post(Event.ConnectEvent.obtain(false));
                }
            }

            @Override
            public void onDatabaseOpened(RongIMClient.DatabaseOpenStatus code) {
                if (connectCallback != null) {
                    connectCallback.onDatabaseOpened(code);
                }
            }
        });
        return SingletonHolder.sRongIM;
    }

    private static RongIMClient.ConnectionStatusListener mConnectionStatusListener = new RongIMClient.ConnectionStatusListener() {

        @Override
        public void onChanged(ConnectionStatus status) {
            if (status != null) {
                RLog.d(TAG, "ConnectionStatusListener onChanged : " + status.toString());
                if (sConnectionStatusListener != null)
                    sConnectionStatusListener.onChanged(status);

                //如果 ipc 进程崩溃，会导致发送中的图片状态错误
                if (status.equals(ConnectionStatus.UNCONNECTED)) {
                    SendImageManager.getInstance().reset();
                    SendMediaManager.getInstance().reset();
                }
                if (status.equals(ConnectionStatus.CONNECTED)) {
                    // 开始发送缓存队列因发送失败需要重发的消息
                    ResendManager.getInstance().beginResend();
                    if (!notificationQuiteHoursConfigured) {
                        RLog.d(TAG, "ConnectionStatusListener not get notificationQuietHours, get again");
                        SingletonHolder.sRongIM.getNotificationQuietHours(null);
                    }
                }
                if (status.equals(ConnectionStatus.SIGN_OUT)) {
                    ResendManager.getInstance().removeAllResendMessage();
                }
                //todo 异常情况清空列表
                RongContext.getInstance().getEventBus().post(status);
            }
        }
    };

    private void initListener() {

        RongIMClient.setOnReceiveMessageListener(new RongIMClient.OnReceiveMessageWrapperListener() {
            @Override
            public boolean onReceived(final Message message, final int left, boolean hasPackage, boolean offline) {
                boolean isProcess = false;

                if (sMessageListener != null) {
                    if (sMessageListener instanceof RongIMClient.OnReceiveMessageWrapperListener) {
                        isProcess = ((RongIMClient.OnReceiveMessageWrapperListener) sMessageListener).onReceived(message, left, hasPackage, offline);
                    } else {
                        isProcess = sMessageListener.onReceived(message, left); //首先透传给用户处理。
                    }
                }

                final MessageTag msgTag = message.getContent().getClass().getAnnotation(MessageTag.class);
                //如果该条消息是计数的或者存到历史记录的，则post到相应界面显示或响铃，否则直接返回（VoIP消息除外）。
                if (msgTag != null && (msgTag.flag() == MessageTag.ISCOUNTED || msgTag.flag() == MessageTag.ISPERSISTED)) {
                    if (messageInterceptor != null) {
                        boolean msgRemove = messageInterceptor.intercept(message);
                        if (msgRemove) {
                            return true;
                        }
                    }
                    RongContext.getInstance().getEventBus().post(new Event.OnReceiveMessageEvent(message, left, hasPackage, offline));

                    //如果消息中附带了用户信息，则通知界面刷新此用户信息。
                    if (message.getContent() != null && message.getContent().getUserInfo() != null) {
                        RongUserInfoManager.getInstance().setUserInfo(message.getContent().getUserInfo());
                    }

                    //如果用户自己处理铃声和后台通知，或者是web端自己发送的消息，则直接返回。
                    if (isProcess || message.getSenderUserId().equals(RongIM.getInstance().getCurrentUserId())) {
                        return true;
                    }
                    MessageConfig messageConfig = message.getMessageConfig();
                    boolean isDisableNotification = false;
                    if (messageConfig != null) {
                        isDisableNotification = messageConfig.isDisableNotification();
                    }
                    if (left == 0 && !hasPackage && !isDisableNotification) { // 拉取离线消息过程没有铃声以及后台通知，并且收到的消息是允许发送通知的
                        MessageNotificationManager.getInstance().notifyIfNeed(mApplicationContext, message, left);
                        if (!notificationQuiteHoursConfigured) {
                            RLog.d(TAG, "OnReceiveMessageListener not get notificationQuietHours get again");
                            SingletonHolder.sRongIM.getNotificationQuietHours(null);
                        }
                    }
                } else {
                    //未知的消息类型：UnknownMessage
                    if (!isProcess) {
                        if (message.getMessageId() > 0) {
                            RongContext.getInstance().getEventBus().post(new Event.OnReceiveMessageEvent(message, left, hasPackage, offline));
                        } else {
                            RongContext.getInstance().getEventBus().post(new Event.MessageLeftEvent(left));
                        }
                    }
                }
                RongExtensionManager.getInstance().onReceivedMessage(message);

                return false;
            }
        });

        //消息回执监听
        boolean readRec = false;
        try {
            readRec = mApplicationContext.getResources().getBoolean(R.bool.rc_read_receipt);
        } catch (Resources.NotFoundException e) {
            RLog.e(TAG, "rc_read_receipt not configure in rc_config.xml");
            e.printStackTrace();
        }

        if (readRec) {
            RongIMClient.setReadReceiptListener(new RongIMClient.ReadReceiptListener() {
                @Override
                public void onReadReceiptReceived(final Message message) {
                    RongContext.getInstance().getEventBus().post(new Event.ReadReceiptEvent(message));
                }

                @Override
                public void onMessageReceiptRequest(Conversation.ConversationType type, String targetId, String messageUId) {
                    RongContext.getInstance().getEventBus().post(new Event.ReadReceiptRequestEvent(type, targetId, messageUId));
                }

                @Override
                public void onMessageReceiptResponse(Conversation.ConversationType type, String targetId, String messageUId, HashMap<String, Long> respondUserIdList) {
                    RongContext.getInstance().getEventBus().post(new Event.ReadReceiptResponseEvent(type, targetId, messageUId, respondUserIdList));
                }
            });
        }

        boolean syncReadStatus = false;
        try {
            syncReadStatus = mApplicationContext.getResources().getBoolean(R.bool.rc_enable_sync_read_status);
        } catch (Resources.NotFoundException e) {
            RLog.e(TAG, "rc_enable_sync_read_status not configure in rc_config.xml");
            e.printStackTrace();
        }
        if (syncReadStatus) {
            RongIMClient.getInstance().setSyncConversationReadStatusListener(new RongIMClient.SyncConversationReadStatusListener() {
                @Override
                public void onSyncConversationReadStatus(Conversation.ConversationType type, String targetId) {
                    RongContext.getInstance().getEventBus().post(new Event.SyncReadStatusEvent(type, targetId));
                }
            });
        }

        //撤回消息监听
        RongIMClient.setOnRecallMessageListener((new RongIMClient.OnRecallMessageListener() {
            @Override
            public boolean onMessageRecalled(Message message, RecallNotificationMessage recallNotificationMessage) {

                RongContext.getInstance().getEventBus().post(new Event.RemoteMessageRecallEvent(message.getMessageId(),
                        message.getConversationType(), recallNotificationMessage, true, message.getTargetId()));

                if (recallNotificationMessage == null) {
                    RLog.i(TAG, "Delete the recall message, recallNotificationMessage is null");
                    return true;
                }

                final MessageTag msgTag = recallNotificationMessage.getClass().getAnnotation(MessageTag.class);
                if (msgTag != null && (msgTag.flag() == MessageTag.ISCOUNTED || msgTag.flag() == MessageTag.ISPERSISTED)) {
                    if (notificationQuiteHoursConfigured) {
                        MessageNotificationManager.getInstance().notifyIfNeed(mApplicationContext, message, 0);
                    }
                }
                return true;
            }
        }));

        RongIMClient.getInstance().setOnReceiveDestructionMessageListener(new RongIMClient.OnReceiveDestructionMessageListener() {

            @Override
            public void onReceive(final Message message) {
                EventBus.getDefault().post(new Event.MessageDeleteEvent(message.getMessageId()));
            }
        });
        RongIMClient.setConnectionStatusListener(mConnectionStatusListener);

        RongIMClient.getInstance().setConversationStatusListener(new RongIMClient.ConversationStatusListener() {
            @Override
            public void onStatusChanged(ConversationStatus[] conversationStatus) {
                if (conversationStatus.length == 0) {
                    RLog.i(TAG, "length is 0");
                    return;
                }
                if (conversationStatus.length == 1) {
                    String conversationId = conversationStatus[0].getTargetId();
                    Conversation.ConversationType conversationType = conversationStatus[0].getConversationType();
                    RongContext.getInstance().setConversationNotifyStatusToCache(ConversationKey.obtain(conversationId, conversationType),
                            conversationStatus[0].getNotifyStatus());
                }
                EventBus.getDefault().post(conversationStatus);
            }
        });
    }


    /**
     * 设置接收消息的监听器。
     * <p/>
     * 所有接收到的消息、通知、状态都经由此处设置的监听器处理。包括私聊消息、讨论组消息、群组消息、聊天室消息以及各种状态。
     *
     * @param listener 接收消息的监听器。
     */
    public static void setOnReceiveMessageListener(RongIMClient.OnReceiveMessageListener listener) {
        RLog.i(TAG, "RongIM setOnReceiveMessageListener");
        sMessageListener = listener;
    }

    /**
     * 设置连接状态变化的监听器。
     * <p>
     * <strong>
     * 当回调状态为{@link io.rong.imlib.RongIMClient.ConnectionStatusListener.ConnectionStatus#TOKEN_INCORRECT},
     * 需要获取正确的token, 并主动调用{@link io.rong.imkit.RongIM#connect(String, RongIMClient.ConnectCallback)}
     * </strong>
     *
     * @param listener 连接状态变化的监听器。
     */
    public static void setConnectionStatusListener(final RongIMClient.ConnectionStatusListener listener) {
        sConnectionStatusListener = listener;
    }

    /**
     * 断开连接或注销当前登录。
     *
     * @param isReceivePush 断开后是否接收 push。
     * @deprecated 该方法废弃，请使用{@link #disconnect()}或者{@link #logout()}方法。
     */
    @Deprecated
    public void disconnect(boolean isReceivePush) {
        RongIMClient.getInstance().disconnect(isReceivePush);
    }

    /**
     * 注销当前登录，执行该方法后不会再收到 push 消息。
     */
    public void logout() {
        if (mApplicationContext == null) {
            return;
        }
        String current = SystemUtils.getCurrentProcessName(mApplicationContext);
        String mainProcessName = mApplicationContext.getPackageName();

        if (!mainProcessName.equals(current)) {
            RLog.w(TAG, "only can logout in main progress! current process is:" + current);
            return;
        }
        if (RongContext.getInstance() == null) {
            RLog.e(TAG, "logout. RongIM SDK not init, please do after init.");
        } else {
            RongContext.getInstance().clearConversationNotifyStatusCache();
        }
        RongIMClient.getInstance().logout();
        RongUserInfoManager.getInstance().uninit();
        UnReadMessageManager.getInstance().clearObserver();
        RongExtensionManager.getInstance().disconnect();
        notificationQuiteHoursConfigured = false;
        MessageNotificationManager.getInstance().clearNotificationQuietHours();
    }

    /**
     * 设置群组成员提供者。
     * <p/>
     * '@' 功能和VoIP功能在选人界面,需要知道群组内成员信息,开发者需要设置该提供者。 开发者需要在回调中获取到群成员信息
     * 并通过{@link IGroupMemberCallback}中的方法设置到 sdk 中
     * <p/>
     *
     * @param groupMembersProvider 群组成员提供者。
     */
    public void setGroupMembersProvider(IGroupMembersProvider groupMembersProvider) {
        RongMentionManager.getInstance().setGroupMembersProvider(groupMembersProvider);
    }

    public interface IGroupMembersProvider {
        void getGroupMembers(String groupId, IGroupMemberCallback callback);
    }

    public interface IGroupMemberCallback {
        void onGetGroupMembersResult(List<UserInfo> members);
    }

    /**
     * 位置信息的提供者，实现后获取用户位置信息。
     */
    public interface LocationProvider {
        void onStartLocation(Context context, LocationCallback callback);

        interface LocationCallback {
            void onSuccess(LocationMessage message);

            void onFailure(String msg);
        }
    }

    /**
     * 设置位置信息的提供者。
     *
     * @param locationProvider 位置信息提供者。
     */
    public static void setLocationProvider(LocationProvider locationProvider) {

        if (RongContext.getInstance() != null)
            RongContext.getInstance().setLocationProvider(locationProvider);
    }

    /**
     * 断开连接(断开后继续接收 Push 消息)。
     */
    public void disconnect() {
        RongIMClient.getInstance().disconnect();
        RongExtensionManager.getInstance().disconnect();
    }

    /**
     * 获取 IMKit RongIM 实例，需在执行 init 方法初始化 SDK 后获取否则返回值为 NULL。
     *
     * @return RongIM IM 客户端核心类的实例。
     */
    public static RongIM getInstance() {
        return SingletonHolder.sRongIM;
    }


    /**
     * 启动会话列表界面。
     *
     * @param context 应用上下文。
     * @deprecated 废弃该方法，请使用 {@link #startConversationList(Context, Map)}
     */
    @Deprecated
    public void startConversationList(Context context) {

        if (context == null) {
            RLog.e(TAG, "startConversationList. context can not be empty!!!");
            return;
        }

        if (RongContext.getInstance() == null) {
            RLog.e(TAG, "startConversationList. RongIM SDK not init, please do after init.");
            return;
        }

        String packageName = context.getApplicationInfo().packageName;
        Uri uri = Uri.parse("rong://" + packageName).buildUpon()
                .appendPath("conversationlist").build();
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage(packageName);
        context.startActivity(intent);
    }

    /**
     * 启动会话列表界面。
     *
     * @param context               应用上下文。
     * @param supportedConversation 定义会话列表支持显示的会话类型，及对应的会话类型是否聚合显示。
     *                              例如：supportedConversation.put(Conversation.ConversationType.PRIVATE.getName(), false) 非聚合式显示 private 类型的会话。
     */
    public void startConversationList(Context context, Map<String, Boolean> supportedConversation) {

        if (context == null) {
            RLog.e(TAG, "startConversationList. context can not be empty!!!");
            return;
        }

        if (RongContext.getInstance() == null) {
            RLog.e(TAG, "startConversationList. RongIM SDK not init, please do after init.");
            return;
        }

        String packageName = context.getApplicationInfo().packageName;
        Uri.Builder builder = Uri.parse("rong://" + packageName).buildUpon().appendPath("conversationlist");
        if (supportedConversation != null && supportedConversation.size() > 0) {
            Set<String> keys = supportedConversation.keySet();
            for (String key : keys) {
                builder.appendQueryParameter(key, supportedConversation.get(key) ? "true" : "false");
            }
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        intent.setPackage(packageName);
        context.startActivity(intent);
    }

    /**
     * 启动聚合后的某类型的会话列表。<br> 例如：如果设置了单聊会话为聚合，则通过该方法可以打开包含所有的单聊会话的列表。
     *
     * @param context          应用上下文。
     * @param conversationType 会话类型。
     */
    public void startSubConversationList(Context context, Conversation.ConversationType conversationType) {

        if (context == null) {
            RLog.e(TAG, "startSubConversationList. context can not be empty!!!");
            return;
        }

        if (RongContext.getInstance() == null) {
            RLog.e(TAG, "startSubConversationList. RongIM SDK not init, please do after init.");
            return;
        }

        String packageName = context.getApplicationInfo().packageName;
        Uri uri = Uri.parse("rong://" + packageName).buildUpon()
                .appendPath("subconversationlist")
                .appendQueryParameter("type", conversationType.getName())
                .build();
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage(packageName);
        context.startActivity(intent);
    }

    /**
     * 设置会话界面操作的监听器。
     *
     * @param listener 会话界面操作的监听器。
     * @deprecated 请使用 {@link #setConversationClickListener(ConversationClickListener)}
     */
    public static void setConversationBehaviorListener(ConversationBehaviorListener listener) {
        if (RongContext.getInstance() != null) {
            RongContext.getInstance().setConversationBehaviorListener(listener);
        }
    }

    /**
     * 设置会话界面操作的监听器。
     *
     * @param listener 会话界面操作的监听器。
     */
    public static void setConversationClickListener(ConversationClickListener listener) {
        if (RongContext.getInstance() != null) {
            RongContext.getInstance().setConversationClickListener(listener);
        }
    }

    /**
     * 设置会话列表界面操作的监听器。
     *
     * @param listener 会话列表界面操作的监听器。
     */
    public static void setConversationListBehaviorListener(ConversationListBehaviorListener listener) {
        if (RongContext.getInstance() != null) {
            RongContext.getInstance().setConversationListBehaviorListener(listener);
        }
    }

    /**
     * 设置公众号界面操作的监听器。
     *
     * @param listener 会话公众号界面操作的监听器。
     */
    public static void setPublicServiceBehaviorListener(PublicServiceBehaviorListener listener) {
        if (RongContext.getInstance() != null) {
            RongContext.getInstance().setPublicServiceBehaviorListener(listener);
        }
    }

    /**
     * 公众号界面操作的监听器
     */
    public interface PublicServiceBehaviorListener {
        /**
         * 当点击关注后执行。
         *
         * @param context 上下文。
         * @param info    公众号信息。
         * @return 如果用户自己处理了点击后的逻辑处理，则返回 true，否则返回 false，false 走融云默认处理方式。
         */
        boolean onFollowClick(Context context, PublicServiceProfile info);

        /**
         * 当点击取消关注后执行。
         *
         * @param context 上下文。
         * @param info    公众号信息。
         * @return 如果用户自己处理了点击后的逻辑处理，则返回 true，否则返回 false，false 走融云默认处理方式。
         */
        boolean onUnFollowClick(Context context, PublicServiceProfile info);

        /**
         * 当点击进入进入会话后执行。
         *
         * @param context 上下文。
         * @param info    公众号信息。
         * @return 如果用户自己处理了点击后的逻辑处理，则返回 true，否则返回 false，false 走融云默认处理方式。
         */
        boolean onEnterConversationClick(Context context, PublicServiceProfile info);
    }


    /**
     * 启动单聊界面。
     *
     * @param context      应用上下文。
     * @param targetUserId 要与之聊天的用户 Id。
     * @param title        聊天的标题。开发者需要在聊天界面通过intent.getData().getQueryParameter("title")获取该值, 再手动设置为聊天界面的标题。
     */
    public void startPrivateChat(Context context, String targetUserId, String title) {

        if (context == null || TextUtils.isEmpty(targetUserId)) {
            RLog.e(TAG, "startPrivateChat. context or targetUserId can not be empty!!!");
            return;
        }

        if (RongContext.getInstance() == null) {
            RLog.e(TAG, "startPrivateChat. RongIM SDK not init, please do after init.");
            return;
        }

        String packageName = context.getApplicationInfo().packageName;
        Uri uri = Uri.parse("rong://" + packageName).buildUpon()
                .appendPath("conversation").appendPath(Conversation.ConversationType.PRIVATE.getName().toLowerCase(Locale.US))
                .appendQueryParameter("targetId", targetUserId).appendQueryParameter("title", title).build();
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage(packageName);
        context.startActivity(intent);
    }

    /**
     * 开启加密会话，如果是新开启的一个会话则会重新发送信令消息。如果是之前开启但为得到响应或确认的会话则不会重新创建。
     *
     * @param context  应用上下文
     * @param targetId 根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param title    聊天的标题。开发者需要在聊天界面通过intent.getData().getQueryParameter("title")获取该值, 再手动设置为聊天界面的标题。
     * @see RongIMClient.EncryptedSessionConnectionListener
     */
    public void startEncryptedSession(Context context, String targetId, String title) {
        RongIMClient.getInstance().startEncryptedSession(targetId);
    }

    /**
     * 删除加密会话
     *
     * @param targetId //targetId 由 encId + ";;;" + remote userId 组成
     */
    public void quitEncryptedSession(String targetId) {
        RongIMClient.getInstance().quitEncryptedSession(targetId);
    }


    /**
     * 退出所有加密会话
     */
    public void clearEncryptedConversations() {
        RongIMClient.getInstance().clearEncryptedConversations();
    }


    /**
     * 获取私密会话状态
     *
     * @param context
     * @param targetId //targetId 由 encId + ";;;" + remote userId 组成
     * @return 私密会话状态 RCEncryptedSessionStatus
     */
    public int getEncryptedSessionStatus(Context context, String targetId) {
        return RongIMClient.getInstance().getEncryptedSessionStatus(targetId);
    }

    /**
     * 获取所有加密会话
     *
     * @return 本地所有加密会话列表
     */
    public List<RCEncryptedSession> getAllEncryptedConversations() {
        return RongIMClient.getInstance().getAllEncryptedConversations();
    }

    /**
     * <p>启动会话界面。</p>
     * <p>使用时，可以传入多种会话类型 {@link io.rong.imlib.model.Conversation.ConversationType} 对应不同的会话类型，开启不同的会话界面。
     * 如果传入的是 {@link io.rong.imlib.model.Conversation.ConversationType#CHATROOM}，sdk 会默认调用
     * {@link RongIMClient#joinChatRoom(String, int, RongIMClient.OperationCallback)} 加入聊天室。
     * 如果你的逻辑是，只允许加入已存在的聊天室，请使用接口 {@link #startChatRoomChat(Context, String, boolean)} 并且第三个参数为 true</p>
     *
     * @param context          应用上下文。
     * @param conversationType 会话类型。
     * @param targetId         根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param title            聊天的标题。开发者需要在聊天界面通过intent.getData().getQueryParameter("title")获取该值, 再手动设置为聊天界面的标题。
     */
    public void startConversation(Context context, Conversation.ConversationType conversationType, String targetId, String title) {
        if (context == null || TextUtils.isEmpty(targetId) || conversationType == null) {
            RLog.e(TAG, "startConversation. context, targetId or conversationType can not be empty!!!");
            return;
        }
        String packageName = context.getApplicationInfo().packageName;
        Uri uri = Uri.parse("rong://" + packageName).buildUpon()
                .appendPath("conversation").appendPath(conversationType.getName().toLowerCase(Locale.US))
                .appendQueryParameter("targetId", targetId).appendQueryParameter("title", title).build();
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        if (!(context instanceof Activity)) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        intent.setPackage(packageName);
        context.startActivity(intent);
    }

    /**
     * <p>启动会话界面。</p>
     * <p>使用时，可以传入多种会话类型 {@link io.rong.imlib.model.Conversation.ConversationType} 对应不同的会话类型，开启不同的会话界面。
     * 如果传入的是 {@link io.rong.imlib.model.Conversation.ConversationType#CHATROOM}，sdk 会默认调用
     * {@link RongIMClient#joinChatRoom(String, int, RongIMClient.OperationCallback)} 加入聊天室。
     * 如果你的逻辑是，只允许加入已存在的聊天室，请使用接口 {@link #startChatRoomChat(Context, String, boolean)} 并且第三个参数为 true</p>
     *
     * @param context          应用上下文。
     * @param conversationType 会话类型。
     * @param targetId         根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param title            聊天的标题。开发者需要在聊天界面通过intent.getData().getQueryParameter("title")获取该值, 再手动设置为聊天界面的标题。
     * @param bundle           参数传递 bundle
     */
    public void startConversation(Context context, Conversation.ConversationType conversationType, String targetId, String title, Bundle bundle) {
        if (context == null || TextUtils.isEmpty(targetId) || conversationType == null) {
            RLog.e(TAG, "startConversation. context, targetId or conversationType can not be empty!!!");
            return;
        }
        String packageName = context.getApplicationInfo().packageName;
        Uri uri = Uri.parse("rong://" + packageName).buildUpon()
                .appendPath("conversation").appendPath(conversationType.getName().toLowerCase(Locale.US))
                .appendQueryParameter("targetId", targetId).appendQueryParameter("title", title).build();

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        if (!(context instanceof Activity)) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        intent.setPackage(packageName);
        context.startActivity(intent);
    }

    /**
     * <p>启动会话界面，并跳转到指定的消息位置</p>
     * <p>使用时，可以传入多种会话类型 {@link io.rong.imlib.model.Conversation.ConversationType} 对应不同的会话类型，开启不同的会话界面。
     * 如果传入的是 {@link io.rong.imlib.model.Conversation.ConversationType#CHATROOM}，sdk 会默认调用
     * {@link RongIMClient#joinChatRoom(String, int, RongIMClient.OperationCallback)} 加入聊天室。
     * 如果你的逻辑是，只允许加入已存在的聊天室，请使用接口 {@link #startChatRoomChat(Context, String, boolean)} 并且第三个参数为 true</p>
     *
     * @param context          应用上下文。
     * @param conversationType 会话类型。
     * @param targetId         根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param title            聊天的标题。开发者需要在聊天界面通过intent.getData().getQueryParameter("title")获取该值, 再手动设置为聊天界面的标题。
     * @param fixedMsgSentTime 需要定位的消息发送时间
     */
    public void startConversation(Context context, Conversation.ConversationType conversationType, String targetId, String title, long fixedMsgSentTime) {
        if (context == null || TextUtils.isEmpty(targetId) || conversationType == null) {
            RLog.e(TAG, "startConversation. context, targetId or conversationType can not be empty!!!");
            return;
        }
        String packageName = context.getApplicationInfo().packageName;
        Uri uri = Uri.parse("rong://" + packageName).buildUpon()
                .appendPath("conversation").appendPath(conversationType.getName().toLowerCase(Locale.US))
                .appendQueryParameter("targetId", targetId).appendQueryParameter("title", title).build();

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.putExtra("indexMessageTime", fixedMsgSentTime);
        intent.setPackage(packageName);
        if (!(context instanceof Activity)) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    /**
     * 创建讨论组会话并进入会话界面。
     * <p>该方法会同时根据传入的 userId 创建讨论组，但无法获取奥讨论组创建结果。
     * 如果想要获取到讨论组创建结果，请使用 {@link #createDiscussion(String, List, RongIMClient.CreateDiscussionCallback)}</p>
     *
     * @param context       应用上下文。
     * @param targetUserIds 要与之聊天的讨论组用户 Id 列表。
     * @param title         聊天的标题，如果传入空值，则默认显示与之聊天的用户名称。
     */
    public void createDiscussionChat(final Context context, final List<String> targetUserIds, final String title) {

        if (context == null || targetUserIds == null || targetUserIds.size() == 0) {
            RLog.e(TAG, "createDiscussionChat. context, targetUserIds or targetUserIds.size can not be empty!!!");
            return;
        }
        RongIMClient.getInstance().createDiscussion(title, targetUserIds, new RongIMClient.CreateDiscussionCallback() {
            @Override
            public void onSuccess(String targetId) {
                String packageName = context.getApplicationInfo().packageName;
                Uri uri = Uri.parse("rong://" + packageName).buildUpon()
                        .appendPath("conversation").appendPath(Conversation.ConversationType.DISCUSSION.getName().toLowerCase(Locale.US))
                        .appendQueryParameter("targetIds", TextUtils.join(",", targetUserIds)).appendQueryParameter("delimiter", ",")
                        .appendQueryParameter("targetId", targetId)
                        .appendQueryParameter("title", title).build();
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.setPackage(packageName);
                context.startActivity(intent);
            }

            @Override
            public void onError(RongIMClient.ErrorCode e) {
                RLog.d(TAG, "createDiscussionChat createDiscussion not success." + e);
            }
        });
    }

    /**
     * 创建讨论组会话并进入会话界面。
     * <p>该方法会同时根据传入的 userId 创建讨论组，并通过{@link io.rong.imlib.RongIMClient.CreateDiscussionCallback} 返回讨论组创建结果。</p>
     *
     * @param context       应用上下文。
     * @param targetUserIds 要与之聊天的讨论组用户 Id 列表。
     * @param title         聊天的标题，如果传入空值，则默认显示与之聊天的用户名称。
     * @param callback      讨论组回调，成功时，返回讨论组 id。
     */
    public void createDiscussionChat(final Context context, final List<String> targetUserIds, final String title, final RongIMClient.CreateDiscussionCallback callback) {

        if (context == null || targetUserIds == null || targetUserIds.size() == 0) {
            RLog.e(TAG, "createDiscussionChat. context, targetUserIds or targetUserIds.size can not be empty!!!");
            return;
        }

        RongIMClient.getInstance().createDiscussion(title, targetUserIds, new RongIMClient.CreateDiscussionCallback() {
            @Override
            public void onSuccess(String targetId) {
                String packageName = context.getApplicationInfo().packageName;
                Uri uri = Uri.parse("rong://" + packageName).buildUpon()
                        .appendPath("conversation").appendPath(Conversation.ConversationType.DISCUSSION.getName().toLowerCase(Locale.US))
                        .appendQueryParameter("targetIds", TextUtils.join(",", targetUserIds)).appendQueryParameter("delimiter", ",")
                        .appendQueryParameter("targetId", targetId)
                        .appendQueryParameter("title", title).build();
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.setPackage(packageName);
                context.startActivity(intent);
                if (callback != null)
                    callback.onSuccess(targetId);
            }

            @Override
            public void onError(RongIMClient.ErrorCode e) {
                RLog.d(TAG, "createDiscussionChat createDiscussion not success." + e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }

    /**
     * 启动讨论组聊天界面。
     *
     * @param context            应用上下文。
     * @param targetDiscussionId 要聊天的讨论组 Id。
     * @param title              聊天的标题。开发者需要在聊天界面通过intent.getData().getQueryParameter("title")获取该值, 再手动设置为聊天界面的标题。
     */
    public void startDiscussionChat(Context context, String targetDiscussionId, String title) {

        if (context == null || TextUtils.isEmpty(targetDiscussionId)) {
            RLog.e(TAG, "startDiscussionChat. context or targetDiscussionId can not be empty!!!");
            return;
        }

        String packageName = context.getApplicationInfo().packageName;
        Uri uri = Uri.parse("rong://" + packageName).buildUpon()
                .appendPath("conversation").appendPath(Conversation.ConversationType.DISCUSSION.getName().toLowerCase(Locale.US))
                .appendQueryParameter("targetId", targetDiscussionId).appendQueryParameter("title", title).build();

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage(packageName);
        context.startActivity(intent);
    }

    /**
     * 启动群组聊天界面。
     *
     * @param context       应用上下文。
     * @param targetGroupId 要聊天的群组 Id。
     * @param title         聊天的标题。开发者需要在聊天界面通过intent.getData().getQueryParameter("title")获取该值, 再手动设置为聊天界面的标题。
     */
    public void startGroupChat(Context context, String targetGroupId, String title) {
        if (context == null || TextUtils.isEmpty(targetGroupId)) {
            RLog.e(TAG, "startGroupChat. context or targetGroupId can not be empty!!!");
            return;
        }
        if (RongContext.getInstance() == null) {
            RLog.e(TAG, "startGroupChat. RongIM SDK not init, please do after init.");
            return;
        }

        String packageName = context.getApplicationInfo().packageName;
        Uri uri = Uri.parse("rong://" + packageName).buildUpon()
                .appendPath("conversation").appendPath(Conversation.ConversationType.GROUP.getName().toLowerCase(Locale.US))
                .appendQueryParameter("targetId", targetGroupId).appendQueryParameter("title", title).build();

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage(packageName);
        context.startActivity(intent);
    }

    /**
     * <p>启动聊天室会话。</p>
     * <p>设置参数 createIfNotExist 为 true，对应到 kit 中调用的接口是
     * {@link RongIMClient#joinChatRoom(String, int, RongIMClient.OperationCallback)}.
     * 如果聊天室不存在，则自动创建并加入，如果回调失败，则弹出 warning。</p>
     * <p>设置参数 createIfNotExist 为 false，对应到 kit 中调用的接口是
     * {@link RongIMClient#joinExistChatRoom(String, int, RongIMClient.OperationCallback)}.
     * 如果聊天室不存在，则返回错误 {@link io.rong.imlib.RongIMClient.ErrorCode#RC_CHATROOM_NOT_EXIST}，并且会话界面会弹出 warning.
     * </p>
     *
     * @param context          应用上下文。
     * @param chatRoomId       聊天室 id。
     * @param createIfNotExist 如果聊天室不存在，是否创建。
     */
    public void startChatRoomChat(Context context, String chatRoomId, boolean createIfNotExist) {
        if (context == null || TextUtils.isEmpty(chatRoomId)) {
            RLog.e(TAG, "startChatRoomChat. context or chatRoomId can not be empty!!!");
            return;
        }
        if (RongContext.getInstance() == null) {
            RLog.e(TAG, "startChatRoomChat. RongIM SDK not init, please do after init.");
            return;
        }

        String packageName = context.getApplicationInfo().packageName;
        Uri uri = Uri.parse("rong://" + packageName).buildUpon()
                .appendPath("conversation").appendPath(Conversation.ConversationType.CHATROOM.getName().toLowerCase(Locale.US))
                .appendQueryParameter("targetId", chatRoomId).build();

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage(packageName);
        intent.putExtra("createIfNotExist", createIfNotExist);
        context.startActivity(intent);
    }


    /**
     * 启动客户服聊天界面。
     *
     * @param context           应用上下文。
     * @param customerServiceId 要与之聊天的客服 Id。
     * @param title             聊天的标题。开发者需要在聊天界面通过intent.getData().getQueryParameter("title")获取该值, 再手动设置为聊天界面的标题。
     * @param customServiceInfo 当前使用客服者的用户信息。{@link io.rong.imlib.model.CSCustomServiceInfo}
     */
    public void startCustomerServiceChat(Context context, String customerServiceId, String title, CSCustomServiceInfo customServiceInfo) {
        if (context == null || TextUtils.isEmpty(customerServiceId)) {
            RLog.e(TAG, "startCustomerServiceChat. context or customerServiceId can not be empty!!!");
            return;
        }
        if (RongContext.getInstance() == null) {
            RLog.e(TAG, "startCustomerServiceChat. RongIM SDK not init, please do after init.");
            return;
        }

        String packageName = context.getApplicationInfo().packageName;
        Uri uri = Uri.parse("rong://" + packageName).buildUpon()
                .appendPath("conversation").appendPath(Conversation.ConversationType.CUSTOMER_SERVICE.getName().toLowerCase(Locale.US))
                .appendQueryParameter("targetId", customerServiceId).appendQueryParameter("title", title)
                .build();
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage(packageName);
        intent.putExtra("customServiceInfo", customServiceInfo);
        context.startActivity(intent);
    }


    /**
     * <p>
     * 设置用户信息的提供者，供 RongIM 调用获取用户名称和头像信息。
     * 设置后，当 sdk 界面展示用户信息时，会回调 {@link io.rong.imkit.RongIM.UserInfoProvider#getUserInfo(String)}
     * 使用者只需要根据对应的 userId 提供对应的用户信息。
     * 如果需要异步从服务器获取用户信息，使用者可以在此方法中发起异步请求，然后返回 null 信息。
     * 在异步请求结果返回后，根据返回的结果调用 {@link #refreshUserInfoCache(UserInfo)} 刷新用户信息。
     * </p>
     *
     * @param userInfoProvider 用户信息提供者 {@link io.rong.imkit.RongIM.UserInfoProvider}。
     * @param isCacheUserInfo  设置是否由 IMKit 来缓存用户信息。<br>
     *                         如果 App 提供的 UserInfoProvider。
     *                         每次都需要通过网络请求用户数据，而不是将用户数据缓存到本地，会影响用户信息的加载速度；<br>
     *                         此时最好将本参数设置为 true，由 IMKit 来缓存用户信息。
     */
    public static void setUserInfoProvider(UserInfoProvider userInfoProvider, boolean isCacheUserInfo) {
        if (RongContext.getInstance() != null) {
            RongContext.getInstance().setGetUserInfoProvider(userInfoProvider, isCacheUserInfo);
        }
    }

    /**
     * <p>
     * 设置公众服务账号信息的提供者，供 RongIM 调用获公众服务账号名称，头像信息和公众服务号菜单。
     * 目前 sdk 默认的公众号服务不需要开发者设置，这个接口提供了另外一种从 app 层设置公众服务账号信息的方式
     * 设置后，当 sdk 界面展示用户信息时，会回调 {@link io.rong.imkit.RongIM.PublicServiceProfileProvider#getPublicServiceProfile(Conversation.PublicServiceType, String)}
     * 使用者只需要根据对应的publicServiceType, publicServiceId 提供对应的公众服务账号信息。
     * 如果需要异步从服务器获取公众服务账号信息，使用者可以在此方法中发起异步请求，然后返回 null 信息。
     * 在异步请求结果返回后，根据返回的结果调用 {@link #refreshPublicServiceProfile(PublicServiceProfile)} 刷新公众号信息。
     * </p>
     *
     * @param publicServiceProfileProvider 公众服务账号信息的提供者 {@link io.rong.imkit.RongIM.PublicServiceProfileProvider}。
     */
    public static void setPublicServiceProfileProvider(PublicServiceProfileProvider publicServiceProfileProvider) {
        if (RongContext.getInstance() != null) {
            RongContext.getInstance().setPublicServiceProfileProvider(publicServiceProfileProvider);
        }
    }

    /**
     * <p>设置GroupUserInfo提供者，供RongIM 调用获取GroupUserInfo</p>
     * <p>可以使用此方法，修改群组中用户昵称</p>
     * <p>设置后，当 sdk 界面展示用户信息时，会回调 {@link io.rong.imkit.RongIM.GroupUserInfoProvider#getGroupUserInfo(String, String)}
     * 使用者只需要根据对应的 groupId, userId 提供对应的用户信息 {@link GroupUserInfo}。
     * 如果需要异步从服务器获取用户信息，使用者可以在此方法中发起异步请求，然后返回 null 信息。
     * 在异步请求结果返回后，根据返回的结果调用 {@link #refreshGroupUserInfoCache(GroupUserInfo)} 刷新信息。</p>
     *
     * @param userInfoProvider 群组用户信息提供者。
     * @param isCacheUserInfo  设置是否由 IMKit 来缓存 GroupUserInfo。<br>
     *                         如果 App 提供的 GroupUserInfoProvider。
     *                         每次都需要通过网络请求数据，而不是将数据缓存到本地，会影响信息的加载速度；<br>
     *                         此时最好将本参数设置为 true，由 IMKit 来缓存信息。
     */
    public static void setGroupUserInfoProvider(GroupUserInfoProvider userInfoProvider, boolean isCacheUserInfo) {
        if (RongContext.getInstance() != null) {
            RongContext.getInstance().setGroupUserInfoProvider(userInfoProvider, isCacheUserInfo);
        }
    }

    /**
     * <p>设置群组信息的提供者。</p>
     * <p>设置后，当 sdk 界面展示群组信息时，会回调 {@link io.rong.imkit.RongIM.GroupInfoProvider#getGroupInfo(String)}
     * 使用者只需要根据对应的 groupId 提供对应的群组信息。
     * 如果需要异步从服务器获取群组信息，使用者可以在此方法中发起异步请求，然后返回 null 信息。
     * 在异步请求结果返回后，根据返回的结果调用 {@link #refreshGroupInfoCache(Group)} 刷新信息。</p>
     *
     * @param groupInfoProvider 群组信息提供者。
     * @param isCacheGroupInfo  设置是否由 IMKit 来缓存用户信息。<br>
     *                          如果 App 提供的 GroupInfoProvider。
     *                          每次都需要通过网络请求群组数据，而不是将群组数据缓存到本地，会影响群组信息的加载速度；<br>
     *                          此时最好将本参数设置为 true，由 IMKit 来缓存群组信息。
     */
    public static void setGroupInfoProvider(GroupInfoProvider groupInfoProvider, boolean isCacheGroupInfo) {
        if (RongContext.getInstance() != null) {
            RongContext.getInstance().setGetGroupInfoProvider(groupInfoProvider, isCacheGroupInfo);
        }
    }

    /**
     * 刷新讨论组缓存数据，可用于讨论组修改名称后刷新讨论组内其他人员的缓存数据。
     *
     * @param discussion 需要更新的讨论组缓存数据。
     */
    public void refreshDiscussionCache(Discussion discussion) {

        if (discussion == null)
            return;

        RongUserInfoManager.getInstance().setDiscussionInfo(discussion);
    }

    /**
     * 刷新用户缓存数据。
     *
     * @param userInfo 需要更新的用户缓存数据。
     */
    public void refreshUserInfoCache(UserInfo userInfo) {

        if (userInfo == null)
            return;

        RongUserInfoManager.getInstance().setUserInfo(userInfo);
        if (RongContext.getInstance() == null) {
            RLog.e(TAG, "refreshUserInfoCache. RongIM SDK not init, please do after init.");
            return;
        }
        UserInfo currentUserInfo = RongContext.getInstance().getCurrentUserInfo();
        if (currentUserInfo != null && currentUserInfo.getUserId().equals(userInfo.getUserId())) {
            RongContext.getInstance().setCurrentUserInfo(userInfo);
        }
    }

    /**
     * 刷新、更改群组用户缓存数据。
     *
     * @param groupUserInfo 需要更新的群组用户缓存数据。
     */
    public void refreshGroupUserInfoCache(GroupUserInfo groupUserInfo) {

        if (groupUserInfo == null)
            return;

        RongUserInfoManager.getInstance().setGroupUserInfo(groupUserInfo);
    }

    /**
     * 刷新群组缓存数据。
     *
     * @param group 需要更新的群组缓存数据。
     */
    public void refreshGroupInfoCache(Group group) {
        if (group == null)
            return;

        RongUserInfoManager.getInstance().setGroupInfo(group);
    }

    /**
     * 刷新公众服务账号缓存数据。
     *
     * @param publicServiceProfile 需要更新的公众服务账号缓存数据。
     */
    public void refreshPublicServiceProfile(PublicServiceProfile publicServiceProfile) {
        if (publicServiceProfile == null)
            return;

        RongUserInfoManager.getInstance().setPublicServiceProfile(publicServiceProfile);
    }

    /**
     * 设置发送消息的监听。
     *
     * @param listener 发送消息的监听。
     */
    public void setSendMessageListener(OnSendMessageListener listener) {
        if (RongContext.getInstance() != null) {
            RongContext.getInstance().setOnSendMessageListener(listener);
        }
    }

    /**
     * 会话界面操作的监听器。
     *
     * @deprecated 此监听废弃，部分回调无法获得 targetId
     * 请使用 {@link ConversationClickListener}
     */
    public interface ConversationBehaviorListener {


        /**
         * 当点击用户头像后执行。
         *
         * @param context          上下文。
         * @param conversationType 会话类型。
         * @param user             被点击的用户的信息。
         * @return 如果用户自己处理了点击后的逻辑处理，则返回 true，否则返回 false，false 走融云默认处理方式。
         */
        boolean onUserPortraitClick(Context context, Conversation.ConversationType conversationType, UserInfo user);

        /**
         * 当长按用户头像后执行。
         *
         * @param context          上下文。
         * @param conversationType 会话类型。
         * @param user             被点击的用户的信息。
         * @return 如果用户自己处理了点击后的逻辑处理，则返回 true，否则返回 false，false 走融云默认处理方式。
         */
        boolean onUserPortraitLongClick(Context context, Conversation.ConversationType conversationType, UserInfo user);

        /**
         * 当点击消息时执行。
         *
         * @param context 上下文。
         * @param view    触发点击的 View。
         * @param message 被点击的消息的实体信息。
         * @return 如果用户自己处理了点击后的逻辑处理，则返回 true， 否则返回 false, false 走融云默认处理方式。
         */
        boolean onMessageClick(Context context, View view, Message message);

        /**
         * 当点击链接消息时执行。
         *
         * @param context 上下文。
         * @param link    被点击的链接。
         * @return 如果用户自己处理了点击后的逻辑处理，则返回 true， 否则返回 false, false 走融云默认处理方式。
         */
        boolean onMessageLinkClick(Context context, String link);

        /**
         * 当长按消息时执行。
         *
         * @param context 上下文。
         * @param view    触发点击的 View。
         * @param message 被长按的消息的实体信息。
         * @return 如果用户自己处理了长按后的逻辑处理，则返回 true，否则返回 false，false 走融云默认处理方式。
         */
        boolean onMessageLongClick(Context context, View view, Message message);

    }


    /**
     * 会话界面操作的监听器。
     */
    public interface ConversationClickListener {


        /**
         * 当点击用户头像后执行。
         *
         * @param context          上下文。
         * @param conversationType 会话类型。
         * @param user             被点击的用户的信息。
         * @param targetId         会话 id
         * @return 如果用户自己处理了点击后的逻辑处理，则返回 true，否则返回 false，false 走融云默认处理方式。
         */
        boolean onUserPortraitClick(Context context, Conversation.ConversationType conversationType, UserInfo user, String targetId);

        /**
         * 当长按用户头像后执行。
         *
         * @param context          上下文。
         * @param conversationType 会话类型。
         * @param user             被点击的用户的信息。
         * @param targetId         会话 id
         * @return 如果用户自己处理了点击后的逻辑处理，则返回 true，否则返回 false，false 走融云默认处理方式。
         */
        boolean onUserPortraitLongClick(Context context, Conversation.ConversationType conversationType, UserInfo user, String targetId);

        /**
         * 当点击消息时执行。
         *
         * @param context 上下文。
         * @param view    触发点击的 View。
         * @param message 被点击的消息的实体信息。
         * @return 如果用户自己处理了点击后的逻辑处理，则返回 true， 否则返回 false, false 走融云默认处理方式。
         */
        boolean onMessageClick(Context context, View view, Message message);

        /**
         * 当点击链接消息时执行。
         *
         * @param context 上下文。
         * @param link    被点击的链接。
         * @param message 被点击的消息的实体信息
         * @return 如果用户自己处理了点击后的逻辑处理，则返回 true， 否则返回 false, false 走融云默认处理方式。
         */
        boolean onMessageLinkClick(Context context, String link, Message message);

        /**
         * 当长按消息时执行。
         *
         * @param context 上下文。
         * @param view    触发点击的 View。
         * @param message 被长按的消息的实体信息。
         * @return 如果用户自己处理了长按后的逻辑处理，则返回 true，否则返回 false，false 走融云默认处理方式。
         */
        boolean onMessageLongClick(Context context, View view, Message message);

    }

    /**
     * 会话列表界面操作的监听器。
     */
    public interface ConversationListBehaviorListener {
        /**
         * 当点击会话头像后执行。
         *
         * @param context          上下文。
         * @param conversationType 会话类型。
         * @param targetId         被点击的用户id。
         * @return 如果用户自己处理了点击后的逻辑处理，则返回 true，否则返回 false，false 走融云默认处理方式。
         */
        boolean onConversationPortraitClick(Context context, Conversation.ConversationType conversationType, String targetId);

        /**
         * 当长按会话头像后执行。
         *
         * @param context          上下文。
         * @param conversationType 会话类型。
         * @param targetId         被点击的用户id。
         * @return 如果用户自己处理了点击后的逻辑处理，则返回 true，否则返回 false，false 走融云默认处理方式。
         */
        boolean onConversationPortraitLongClick(Context context, Conversation.ConversationType conversationType, String targetId);

        /**
         * 长按会话列表中的 item 时执行。
         *
         * @param context      上下文。
         * @param view         触发点击的 View。
         * @param conversation 长按时的会话条目。
         * @return 如果用户自己处理了长按会话后的逻辑处理，则返回 true， 否则返回 false，false 走融云默认处理方式。
         */
        boolean onConversationLongClick(Context context, View view, UIConversation conversation);

        /**
         * 点击会话列表中的 item 时执行。
         *
         * @param context      上下文。
         * @param view         触发点击的 View。
         * @param conversation 会话条目。
         * @return 如果用户自己处理了点击会话后的逻辑处理，则返回 true， 否则返回 false，false 走融云默认处理方式。
         */
        boolean onConversationClick(Context context, View view, UIConversation conversation);
    }

    /**
     * 用户信息的提供者。
     * <p/>
     * 如果在聊天中遇到的聊天对象是没有登录过的用户（即没有通过融云服务器鉴权过的），RongIM 是不知道用户信息的，RongIM 将调用此
     * Provider 获取用户信息。
     */
    public interface UserInfoProvider {
        /**
         * 获取用户信息。
         *
         * @param userId 用户 Id。
         * @return 用户信息。
         */
        UserInfo getUserInfo(String userId);
    }

    /**
     * GroupUserInfo提供者。
     */
    public interface GroupUserInfoProvider {
        /**
         * 获取GroupUserInfo。
         *
         * @param groupId 群组id。
         * @param userId  用户id。
         * @return GroupUserInfo。
         */
        GroupUserInfo getGroupUserInfo(String groupId, String userId);
    }


    /**
     * 群组信息的提供者。
     * <p/>
     * RongIM 本身不保存群组信息，如果在聊天中需要使用群组信息，RongIM 将调用此 Provider 获取群组信息。
     */
    public interface GroupInfoProvider {
        /**
         * 获取群组信息。
         *
         * @param groupId 群组 Id.
         * @return 群组信息。
         */
        Group getGroupInfo(String groupId);
    }

    /**
     * PublicServiceProfile提供者。
     */
    public interface PublicServiceProfileProvider {
        /**
         * 获取PublicServiceProfile。
         *
         * @param publicServiceType 公众服务类型。
         * @param id                公众服务 id。
         * @return PublicServiceProfile。
         */
        PublicServiceProfile getPublicServiceProfile(Conversation.PublicServiceType publicServiceType, String id);
    }

    /**
     * 启动好友选择界面的监听器
     */
    public interface OnSelectMemberListener {
        /**
         * 启动好友选择界面的接口。
         *
         * @param context          上下文。
         * @param conversationType 会话类型：PRIVATE / DISCUSSION.
         * @param targetId         该会话对应的 Id，私聊时为发送者 Id，讨论组时为讨论组 Id。
         */

        void startSelectMember(Context context, Conversation.ConversationType conversationType, String targetId);
    }

    /**
     * 获取自己发出的消息监听器。
     */
    public interface OnSendMessageListener {

        /**
         * 消息发送前监听器处理接口（是否发送成功可以从SentStatus属性获取）
         * 可以通过这个方法，过滤，修改发送出的消息。
         *
         * @param message 发送的消息实例。
         * @return 处理后的消息实例，注意：可以通过 return 的返回值，过滤消息
         * 当 return null 时，该消息不发送，界面也无显示
         * 也可以更改 message 内的消息内容，发送出的消息，就是更改后的。
         */
        Message onSend(Message message);


        /**
         * 消息发送后回调接口。
         *
         * @param message              消息实例。
         * @param sentMessageErrorCode 发送消息失败的状态码，消息发送成功 SentMessageErrorCode 为 null。
         */
        boolean onSent(Message message, SentMessageErrorCode sentMessageErrorCode);

    }

    /**
     * 发出的消息错误码。
     */
    public enum SentMessageErrorCode {

        /**
         * 未知错误。
         */
        UNKNOWN(-1, "Unknown error."),

        /**
         * 不在讨论组。
         */
        NOT_IN_DISCUSSION(21406, "not_in_discussion"),
        /**
         * 不在群组。
         */
        NOT_IN_GROUP(22406, "not_in_group"),
        /**
         * 群组禁言
         */
        FORBIDDEN_IN_GROUP(22408, "forbidden_in_group"),
        /**
         * 不在聊天室。
         */
        NOT_IN_CHATROOM(23406, "not_in_chatroom"),

        /**
         * 在黑名单中。
         */
        REJECTED_BY_BLACKLIST(405, "rejected by blacklist"),
        /**
         * 发送的消息中包含敏感词 （发送方发送失败，接收方不会收到消息）
         */
        RC_MSG_BLOCKED_SENSITIVE_WORD(21501, "word is blocked"),
        /**
         * 消息中敏感词已经被替换 （接收方可以收到被替换之后的消息）
         */
        RC_MSG_REPLACED_SENSITIVE_WORD(21502, "word is replaced"),
        /**
         * 未关注此公众号
         */
        RC_APP_PUBLICSERVICE_UNFOLLOW(29106, "not followed");


        private int code;
        private String msg;

        /**
         * 构造函数。
         *
         * @param code 错误代码。
         * @param msg  错误消息。
         */
        SentMessageErrorCode(int code, String msg) {
            this.code = code;
            this.msg = msg;
        }

        /**
         * 获取错误代码值。
         *
         * @return 错误代码值。
         */
        public int getValue() {
            return this.code;
        }

        /**
         * 获取错误消息。
         *
         * @return 错误消息。
         */
        public String getMessage() {
            return this.msg;
        }

        /**
         * 设置错误代码值。
         *
         * @param code 错误代码。
         * @return 错误代码枚举。
         */
        public static SentMessageErrorCode setValue(int code) {
            for (SentMessageErrorCode c : SentMessageErrorCode.values()) {
                if (code == c.getValue()) {
                    return c;
                }
            }

            RLog.d("RongIMClient", "SentMessageErrorCode---ErrorCode---code:" + code);

            return UNKNOWN;
        }
    }

    /**
     * 设置消息体内是否携带用户信息。
     *
     * @param state 是否携带用户信息，true 携带，false 不携带。
     */
    public void setMessageAttachedUserInfo(boolean state) {
        if (RongContext.getInstance() != null) {
            RongContext.getInstance().setUserInfoAttachedState(state);
        }
    }

    /**
     * 接收未读消息的监听器。
     */
    public interface OnReceiveUnreadCountChangedListener {
        void onMessageIncreased(int count);
    }

    /**
     * 设置接收未读消息的监听器。
     *
     * @param listener          接收未读消息消息的监听器。
     * @param conversationTypes 接收未读消息的会话类型。
     * @deprecated 该方法已废弃, 会造成内存泄漏, 请使用 {@link #addUnReadMessageCountChangedObserver(IUnReadMessageObserver, Conversation.ConversationType...)}
     * 和 {@link #removeUnReadMessageCountChangedObserver(IUnReadMessageObserver)}
     */
    @Deprecated
    public void setOnReceiveUnreadCountChangedListener(final OnReceiveUnreadCountChangedListener listener, Conversation.ConversationType... conversationTypes) {
        if (listener == null || conversationTypes == null || conversationTypes.length == 0) {
            RLog.w(TAG, "setOnReceiveUnreadCountChangedListener Illegal argument");
            return;
        }
        UnReadMessageManager.getInstance().addObserver(conversationTypes, new IUnReadMessageObserver() {
            @Override
            public void onCountChanged(int count) {
                listener.onMessageIncreased(count);
            }
        });
    }

    /**
     * 设置未读消息数变化监听器。
     * 注意:如果是在 activity 中设置,那么要在 activity 销毁时,调用 {@link #removeUnReadMessageCountChangedObserver(IUnReadMessageObserver)}
     * 否则会造成内存泄漏。
     *
     * @param observer          接收未读消息消息的监听器。
     * @param conversationTypes 接收未读消息的会话类型。
     */
    public void addUnReadMessageCountChangedObserver(final IUnReadMessageObserver observer, Conversation.ConversationType... conversationTypes) {
        if (observer == null || conversationTypes == null || conversationTypes.length == 0) {
            RLog.w(TAG, "addOnReceiveUnreadCountChangedListener Illegal argument");
            throw new IllegalArgumentException("observer must not be null and must include at least one conversationType");
        }
        UnReadMessageManager.getInstance().addObserver(conversationTypes, observer);
    }

    /**
     * 注销已注册的未读消息数变化监听器。
     *
     * @param observer 接收未读消息消息的监听器。
     */
    public void removeUnReadMessageCountChangedObserver(final IUnReadMessageObserver observer) {
        if (observer == null) {
            RLog.w(TAG, "removeOnReceiveUnreadCountChangedListener Illegal argument");
            return;
        }
        UnReadMessageManager.getInstance().removeObserver(observer);
    }

    /**
     * 启动公众号信息界面。
     *
     * @param context          应用上下文。
     * @param conversationType 会话类型。
     * @param targetId         目标 Id。
     */
    public void startPublicServiceProfile(Context context, Conversation.ConversationType conversationType, String targetId) {
        if (context == null || conversationType == null || TextUtils.isEmpty(targetId)) {
            RLog.e(TAG, "startPublicServiceProfile. context, conversationType or targetId can not be empty!!!");
            return;
        }
        if (RongContext.getInstance() == null) {
            RLog.e(TAG, "startPublicServiceProfile. RongIM SDK not init, please do after init.");
            return;
        }

        String packageName = context.getApplicationInfo().packageName;
        Uri uri = Uri.parse("rong://" + packageName).buildUpon()
                .appendPath("publicServiceProfile").appendPath(conversationType.getName().toLowerCase(Locale.US)).appendQueryParameter("targetId", targetId).build();

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setPackage(packageName);
        context.startActivity(intent);
    }

    /**
     * 注册会话列表消息模板提供者。
     *
     * @param provider 会话列表模板提供者。
     */
    public void registerConversationTemplate(IContainerItemProvider.ConversationProvider provider) {
        if (RongContext.getInstance() != null) {
            if (provider == null)
                throw new IllegalArgumentException();

            RongContext.getInstance().registerConversationTemplate(provider);
        }
    }


    /**
     * 设置会话界面未读新消息是否展示 注:未读新消息大于1条即展示
     *
     * @param state true 展示，false 不展示。
     */
    public void enableNewComingMessageIcon(boolean state) {
        if (RongContext.getInstance() != null) {
            RongContext.getInstance().showNewMessageIcon(state);
        }
    }

    /**
     * 设置会话界面历史消息是否展示 注:历史消息大于10条即展示
     *
     * @param state true 展示，false 不展示。
     */
    public void enableUnreadMessageIcon(boolean state) {
        if (RongContext.getInstance() != null) {
            RongContext.getInstance().showUnreadMessageIcon(state);
        }
    }

    /**
     * 设置语音消息的最大长度
     *
     * @param sec 默认值是60s，有效值为不小于5秒，不大于60秒
     */
    public void setMaxVoiceDurationg(int sec) {
        AudioRecordManager.getInstance().setMaxVoiceDuration(sec);
    }

    /**
     * 获取连接状态。
     *
     * @return 连接状态枚举。
     */
    public RongIMClient.ConnectionStatusListener.ConnectionStatus getCurrentConnectionStatus() {
        return RongIMClient.getInstance().getCurrentConnectionStatus();
    }

    /**
     * 获取会话列表。
     *
     * @param callback 会话列表数据回调。
     *                 Conversation。
     */
    public void getConversationList(RongIMClient.ResultCallback<List<Conversation>> callback) {
        RongIMClient.getInstance().getConversationList(callback);
    }

    /**
     * 根据会话类型，回调方式获取会话列表。
     *
     * @param callback 获取会话列表的回调。
     * @param types    会话类型。
     */
    public void getConversationList(RongIMClient.ResultCallback<List<Conversation>> callback, Conversation.ConversationType... types) {
        RongIMClient.getInstance().getConversationList(callback, types);
    }

    /**
     * 根据不同会话类型的目标Id，回调方式获取某一会话信息。
     *
     * @param type     会话类型。
     * @param targetId 目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param callback 获取会话信息的回调。
     */
    public void getConversation(Conversation.ConversationType type, String targetId, RongIMClient.ResultCallback<Conversation> callback) {
        RongIMClient.getInstance().getConversation(type, targetId, callback);
    }

    /**
     * 从会话列表中移除某一会话，但是不删除会话内的消息。
     * <p/>
     * 如果此会话中有新的消息，该会话将重新在会话列表中显示，并显示最近的历史消息。
     *
     * @param type     会话类型。
     * @param targetId 目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param callback 移除会话是否成功的回调。
     */
    public void removeConversation(final Conversation.ConversationType type, final String targetId, final RongIMClient.ResultCallback<Boolean> callback) {
        RongIMClient.getInstance().removeConversation(type, targetId, new RongIMClient.ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean bool) {
                if (callback != null) {
                    callback.onSuccess(bool);
                }
                if (bool) {
                    RongContext.getInstance().getEventBus().post(new Event.ConversationRemoveEvent(type, targetId));
                }
            }

            @Override
            public void onError(RongIMClient.ErrorCode e) {
                if (callback != null)
                    callback.onError(e);
            }
        });
    }

    /**
     * 设置某一会话为置顶或者取消置顶，回调方式获取设置是否成功。
     *
     * @param type     会话类型。
     * @param id       目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param isTop    是否置顶。
     * @param callback 设置置顶或取消置顶是否成功的回调。
     */
    public void setConversationToTop(final Conversation.ConversationType type, final String id, final boolean isTop, final RongIMClient.ResultCallback<Boolean> callback) {
        RongIMClient.getInstance().setConversationToTop(type, id, isTop, new RongIMClient.ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean bool) {
                if (callback != null)
                    callback.onSuccess(bool);

                if (bool)
                    RongContext.getInstance().getEventBus().post(new Event.ConversationTopEvent(type, id, isTop));
            }

            @Override
            public void onError(RongIMClient.ErrorCode e) {
                if (callback != null)
                    callback.onError(e);
            }
        });
    }

    /**
     * 设置某一会话为置顶或者取消置顶，回调方式获取设置是否成功。
     *
     * @param type       会话类型。
     * @param id         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param isTop      是否置顶。
     * @param needCreate 会话不存在时，是否创建会话。
     * @param callback   设置置顶或取消置顶是否成功的回调。
     */
    public void setConversationToTop(final Conversation.ConversationType type, final String id, final boolean isTop, final boolean needCreate, final RongIMClient.ResultCallback<Boolean> callback) {
        RongIMClient.getInstance().setConversationToTop(type, id, isTop, needCreate, new RongIMClient.ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean bool) {
                if (callback != null)
                    callback.onSuccess(bool);

                if (bool && (!isTop || needCreate))
                    RongContext.getInstance().getEventBus().post(new Event.ConversationTopEvent(type, id, isTop));
            }

            @Override
            public void onError(RongIMClient.ErrorCode e) {
                if (callback != null)
                    callback.onError(e);
            }
        });
    }

    /**
     * 通过回调方式，获取所有未读消息数。
     *
     * @param callback 消息数的回调。
     */
    public void getTotalUnreadCount(final RongIMClient.ResultCallback<Integer> callback) {
        RongIMClient.getInstance().getTotalUnreadCount(new RongIMClient.ResultCallback<Integer>() {
            @Override
            public void onSuccess(Integer integer) {
                if (callback != null)
                    callback.onSuccess(integer);
            }

            @Override
            public void onError(RongIMClient.ErrorCode e) {
                if (callback != null)
                    callback.onError(e);
            }
        });
    }

    /**
     * 根据会话类型的目标 Id,回调方式获取来自某用户（某会话）的未读消息数。
     *
     * @param conversationType 会话类型。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @param callback         未读消息数的回调
     */
    public void getUnreadCount(Conversation.ConversationType conversationType, String targetId, RongIMClient.ResultCallback<Integer> callback) {
        RongIMClient.getInstance().getUnreadCount(conversationType, targetId, callback);
    }

    /**
     * 回调方式获取某会话类型的未读消息数。
     *
     * @param callback          未读消息数的回调。
     * @param conversationTypes 会话类型。
     */
    public void getUnreadCount(RongIMClient.ResultCallback<Integer> callback, Conversation.ConversationType... conversationTypes) {
        RongIMClient.getInstance().getUnreadCount(callback, conversationTypes);
    }


    /**
     * 回调方式获取某会话类型的未读消息数。可选择包含或者不包含消息免打扰的未读消息数。
     *
     * @param conversationTypes 会话类型。
     * @param containBlocked    是否包含消息免打扰的未读消息数，true 包含， false 不包含
     * @param callback          未读消息数的回调。
     */
    public void getUnreadCount(Conversation.ConversationType[] conversationTypes, boolean containBlocked, RongIMClient.ResultCallback<Integer> callback) {
        RongIMClient.getInstance().getUnreadCount(conversationTypes, containBlocked, callback);
    }


    /**
     * 根据会话类型数组，回调方式获取某会话类型的未读消息数。
     *
     * @param conversationTypes 会话类型。
     * @param callback          未读消息数的回调。
     */
    public void getUnreadCount(Conversation.ConversationType[] conversationTypes, RongIMClient.ResultCallback<Integer> callback) {
        RongIMClient.getInstance().getUnreadCount(conversationTypes, callback);
    }


    /**
     * 根据会话类型的目标 Id，回调方式获取最新的 N 条消息记录。
     *
     * @param conversationType 会话类型。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param count            要获取的消息数量。
     * @param callback         获取最新消息记录的回调，按照时间顺序从新到旧排列。
     */
    public void getLatestMessages(Conversation.ConversationType conversationType, String targetId, int count, RongIMClient.ResultCallback<List<Message>> callback) {
        RongIMClient.getInstance().getLatestMessages(conversationType, targetId, count, callback);
    }

    /**
     * 获取历史消息记录。
     *
     * @param conversationType 会话类型。不支持传入 ConversationType.CHATROOM。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @param oldestMessageId  最后一条消息的 Id，获取此消息之前的 count 条消息，没有消息第一次调用应设置为:-1。
     * @param count            要获取的消息数量。
     * @return 历史消息记录，按照时间顺序从新到旧排列。
     * @deprecated 此方法废弃，请使用{@link #getHistoryMessages(Conversation.ConversationType, String, int, int, RongIMClient.ResultCallback)}。
     */
    @Deprecated
    public List<Message> getHistoryMessages(Conversation.ConversationType conversationType, String targetId, int oldestMessageId, int count) {
        return RongIMClient.getInstance().getHistoryMessages(conversationType, targetId, oldestMessageId, count);
    }

    /**
     * 获取历史消息记录。
     *
     * @param conversationType 会话类型。不支持传入 ConversationType.CHATROOM。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @param objectName       消息类型标识。
     * @param oldestMessageId  最后一条消息的 Id，获取此消息之前的 count 条消息,没有消息第一次调用应设置为:-1。
     * @param count            要获取的消息数量。
     * @return 历史消息记录，按照时间顺序从新到旧排列。
     * @deprecated 此方法废弃，请使用{@link #getHistoryMessages(Conversation.ConversationType, String, String, int, int, RongIMClient.ResultCallback)}。
     */
    @Deprecated
    public List<Message> getHistoryMessages(Conversation.ConversationType conversationType, String targetId, String objectName, int oldestMessageId, int count) {
        return RongIMClient.getInstance().getHistoryMessages(conversationType, targetId, objectName, oldestMessageId, count);
    }

    /**
     * 根据会话类型的目标 Id，回调方式获取某消息类型标识的N条历史消息记录。
     *
     * @param conversationType 会话类型。不支持传入 ConversationType.CHATROOM。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 。
     * @param objectName       消息类型标识。
     * @param oldestMessageId  最后一条消息的 Id，获取此消息之前的 count 条消息,没有消息第一次调用应设置为:-1。
     * @param count            要获取的消息数量。
     * @param callback         获取历史消息记录的回调，按照时间顺序从新到旧排列。
     */
    public void getHistoryMessages(Conversation.ConversationType conversationType, String targetId, String objectName, int oldestMessageId, int count, RongIMClient.ResultCallback<List<Message>> callback) {
        RongIMClient.getInstance().getHistoryMessages(conversationType, targetId, objectName, oldestMessageId, count, callback);
    }

    /**
     * 根据会话类型的目标 Id，回调方式获取N条历史消息记录。
     *
     * @param conversationType 会话类型。不支持传入 ConversationType.CHATROOM。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @param oldestMessageId  最后一条消息的 Id，获取此消息之前的 count 条消息，没有消息第一次调用应设置为:-1。
     * @param count            要获取的消息数量。
     * @param callback         获取历史消息记录的回调，按照时间顺序从新到旧排列。
     */
    public void getHistoryMessages(Conversation.ConversationType conversationType, String targetId, int oldestMessageId, int count, RongIMClient.ResultCallback<List<Message>> callback) {
        RongIMClient.getInstance().getHistoryMessages(conversationType, targetId, oldestMessageId, count, callback);
    }


    /**
     * 根据会话类型和目标 Id，获取 N 条远端历史消息记录。
     * <p>该方法只支持拉取指定时间之前的远端历史消息</p>
     *
     * @param conversationType 会话类型。不支持传入 ConversationType.CHATROOM。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @param dataTime         从该时间点开始获取消息。即：消息中的 sendTime；第一次可传 0，获取最新 count 条。
     * @param count            要获取的消息数量，最多 20 条。
     * @param callback         获取历史消息记录的回调，按照时间顺序从新到旧排列。
     */
    public void getRemoteHistoryMessages(Conversation.ConversationType conversationType, String targetId, long dataTime, int count, RongIMClient.ResultCallback<List<Message>> callback) {
        RongIMClient.getInstance().getRemoteHistoryMessages(conversationType, targetId, dataTime, count, callback);
    }

    /**
     * 根据会话类型和目标 Id，拉取某时间戳之前或之后的 N 条远端历史消息记录。
     * <p>该方法支持拉取指定时间之前或之后的远端历史消息</p>
     *
     * @param conversationType       会话类型。不支持传入 ConversationType.CHATROOM。
     * @param targetId               目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @param remoteHistoryMsgOption {@link RemoteHistoryMsgOption}
     * @param callback               获取历史消息记录的回调，按照时间顺序从新到旧排列。
     */
    public void getRemoteHistoryMessages(Conversation.ConversationType conversationType, String targetId, RemoteHistoryMsgOption remoteHistoryMsgOption, RongIMClient.ResultCallback<List<Message>> callback) {
        RongIMClient.getInstance().getRemoteHistoryMessages(conversationType, targetId, remoteHistoryMsgOption, callback);
    }

    /**
     * 删除指定的一条或者一组消息，回调方式获取是否删除成功。
     *
     * @param messageIds 要删除的消息 Id 数组。
     * @param callback   是否删除成功的回调。
     */
    public void deleteMessages(final int[] messageIds, final RongIMClient.ResultCallback<Boolean> callback) {
        RongIMClient.getInstance().deleteMessages(messageIds, new RongIMClient.ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean bool) {
                if (bool)
                    RongContext.getInstance().getEventBus().post(new Event.MessageDeleteEvent(messageIds));
                if (callback != null)
                    callback.onSuccess(bool);
            }

            @Override
            public void onError(RongIMClient.ErrorCode e) {
                if (callback != null)
                    callback.onError(e);
            }
        });
    }

    /**
     * <p>清除指定会话的消息</p>。
     * <p>此接口会删除指定会话中数据库的所有消息，同时，会清理数据库空间。
     * 如果数据库特别大，超过几百 M，调用该接口会有少许耗时。</p>
     *
     * @param conversationType 要删除的消息 Id 数组。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @param callback         是否删除成功的回调。
     */
    public void deleteMessages(final Conversation.ConversationType conversationType, final String targetId, final RongIMClient.ResultCallback<Boolean> callback) {
        RongIMClient.getInstance().deleteMessages(conversationType, targetId, new RongIMClient.ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean bool) {
                if (bool)
                    RongContext.getInstance().getEventBus().post(new Event.MessagesClearEvent(conversationType, targetId));
                if (callback != null)
                    callback.onSuccess(bool);
            }

            @Override
            public void onError(RongIMClient.ErrorCode e) {
                if (callback != null)
                    callback.onError(e);
            }
        });
    }

    /**
     * 删除指定的一条或者一组消息。会同时删除本地和远端消息。
     * <p>请注意，此方法会删除远端消息，请慎重使用</p>
     *
     * @param conversationType 会话类型。暂时不支持聊天室
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、客服 Id。
     * @param messages         要删除的消息数组, 数组大小不能超过100条。
     * @param callback         是否删除成功的回调。
     */
    public void deleteRemoteMessages(Conversation.ConversationType conversationType, String targetId, final Message[] messages, final RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance().deleteRemoteMessages(conversationType, targetId, messages, new RongIMClient.OperationCallback() {
            @Override
            public void onSuccess() {
                int[] messageIds = new int[messages.length];
                for (int i = 0; i < messages.length; i++) {
                    messageIds[i] = messages[i].getMessageId();
                }
                RongContext.getInstance().getEventBus().post(new Event.MessageDeleteEvent(messageIds));
                if (callback != null) {
                    callback.onSuccess();
                }
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                if (callback != null) {
                    callback.onError(errorCode);
                }
            }
        });
    }

    /**
     * 根据会话类型，清空某一会话的所有聊天消息记录,回调方式获取清空是否成功。
     *
     * @param conversationType 会话类型。不支持传入 ConversationType.CHATROOM。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @param callback         清空是否成功的回调。
     */
    public void clearMessages(final Conversation.ConversationType conversationType, final String targetId, final RongIMClient.ResultCallback<Boolean> callback) {
        RongIMClient.getInstance().clearMessages(conversationType, targetId, new RongIMClient.ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean bool) {
                if (bool)
                    RongContext.getInstance().getEventBus().post(new Event.MessagesClearEvent(conversationType, targetId));

                if (callback != null)
                    callback.onSuccess(bool);
            }

            @Override
            public void onError(RongIMClient.ErrorCode e) {
                if (callback != null)
                    callback.onError(e);
            }
        });
    }

    /**
     * 根据会话类型，清除目标 Id 的消息未读状态，回调方式获取清除是否成功。
     *
     * @param conversationType 会话类型。不支持传入 ConversationType.CHATROOM。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @param callback         清除是否成功的回调。
     */
    public void clearMessagesUnreadStatus(final Conversation.ConversationType conversationType, final String targetId, final RongIMClient.ResultCallback<Boolean> callback) {
        RongIMClient.getInstance().clearMessagesUnreadStatus(conversationType, targetId, new RongIMClient.ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean bool) {
                if (callback != null)
                    callback.onSuccess(bool);
                RongContext.getInstance().getEventBus().post(new Event.ConversationUnreadEvent(conversationType, targetId));
            }

            @Override
            public void onError(RongIMClient.ErrorCode e) {
                if (callback != null)
                    callback.onError(e);
            }
        });
    }

    /**
     * 设置消息的附加信息，此信息只保存在本地，回调方式获取设置是否成功。
     *
     * @param messageId 消息 Id。
     * @param value     消息附加信息，最大 1024 字节。
     * @param callback  是否设置成功的回调。
     */
    public void setMessageExtra(int messageId, String value, RongIMClient.ResultCallback<Boolean> callback) {
        RongIMClient.getInstance().setMessageExtra(messageId, value, callback);
    }

    /**
     * 根据消息 Id，设置接收到的消息状态，回调方式获取设置是否成功。
     *
     * @param messageId      消息 Id。
     * @param receivedStatus 接收到的消息状态。
     * @param callback       是否设置成功的回调。
     */
    public void setMessageReceivedStatus(int messageId, Message.ReceivedStatus receivedStatus, RongIMClient.ResultCallback<Boolean> callback) {
        RongIMClient.getInstance().setMessageReceivedStatus(messageId, receivedStatus, callback);
    }

    /**
     * 根据消息 Message 设置消息状态，回调方式获取设置是否成功。
     *
     * @param message  消息实体。要设置的发送状态包含在 message 中
     * @param callback 是否设置成功的回调。
     */
    public void setMessageSentStatus(final Message message, final RongIMClient.ResultCallback<Boolean> callback) {
        if (message == null || message.getMessageId() <= 0) {
            RLog.e(TAG, "setMessageSentStatus message is null or messageId <= 0");
            return;
        }
        RongIMClient.getInstance().setMessageSentStatus(message, new RongIMClient.ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean bool) {
                if (callback != null)
                    callback.onSuccess(bool);

                if (bool)
                    RongContext.getInstance().getEventBus().post(new Event.MessageSentStatusUpdateEvent(message, message.getSentStatus()));
            }

            @Override
            public void onError(RongIMClient.ErrorCode e) {
                if (callback != null)
                    callback.onError(e);
            }
        });
    }

    /**
     * 根据会话类型，获取某一会话的文字消息草稿。
     *
     * @param conversationType 会话类型。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param callback         获取草稿文字内容的回调。
     */
    public void getTextMessageDraft(Conversation.ConversationType conversationType, String targetId, RongIMClient.ResultCallback<String> callback) {
        RongIMClient.getInstance().getTextMessageDraft(conversationType, targetId, callback);
    }

    /**
     * 保存文字消息草稿，回调方式获取保存是否成功。
     *
     * @param conversationType 会话类型。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param content          草稿的文字内容。
     * @param callback         是否保存成功的回调。
     */
    public void saveTextMessageDraft(Conversation.ConversationType conversationType, String targetId, String content, RongIMClient.ResultCallback<Boolean> callback) {
        RongIMClient.getInstance().saveTextMessageDraft(conversationType, targetId, content, callback);
    }

    /**
     * 清除某一会话的文字消息草稿，回调方式获取清除是否成功。
     *
     * @param conversationType 会话类型。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param callback         是否清除成功的回调。
     */
    public void clearTextMessageDraft(Conversation.ConversationType conversationType, String targetId, RongIMClient.ResultCallback<Boolean> callback) {
        RongIMClient.getInstance().clearTextMessageDraft(conversationType, targetId, callback);
    }

    /**
     * 获取讨论组信息和设置。
     *
     * @param discussionId 讨论组 Id。
     * @param callback     获取讨论组的回调。
     */
    public void getDiscussion(String discussionId, RongIMClient.ResultCallback<Discussion> callback) {
        RongIMClient.getInstance().getDiscussion(discussionId, callback);
    }

    /**
     * 设置讨论组名称。
     *
     * @param discussionId 讨论组 Id。
     * @param name         讨论组名称。
     * @param callback     设置讨论组的回调。
     */
    public void setDiscussionName(final String discussionId, final String name, final RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance().setDiscussionName(discussionId, name, new RongIMClient.OperationCallback() {

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                if (callback != null) {
                    callback.onError(errorCode);
                }
            }

            @Override
            public void onSuccess() {
                if (callback != null) {
                    RongUserInfoManager.getInstance().setDiscussionInfo(new Discussion(discussionId, name));
                    callback.onSuccess();
                }
            }
        });
    }

    /**
     * 创建讨论组。
     *
     * @param name       讨论组名称，如：当前所有成员的名字的组合。
     * @param userIdList 讨论组成员 Id 列表。
     * @param callback   创建讨论组成功后的回调。
     */
    public void createDiscussion(final String name, final List<String> userIdList, final RongIMClient.CreateDiscussionCallback callback) {
        RongIMClient.getInstance().createDiscussion(name, userIdList, new RongIMClient.CreateDiscussionCallback() {
            @Override
            public void onSuccess(String discussionId) {
                RongContext.getInstance().getEventBus().post(new Event.CreateDiscussionEvent(discussionId, name, userIdList));
                if (callback != null)
                    callback.onSuccess(discussionId);
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                if (callback != null)
                    callback.onError(errorCode);
            }
        });
    }

    /**
     * 添加一名或者一组用户加入讨论组。
     *
     * @param discussionId 讨论组 Id。
     * @param userIdList   邀请的用户 Id 列表。
     * @param callback     执行操作的回调。
     */
    public void addMemberToDiscussion(final String discussionId, final List<String> userIdList, final RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance().addMemberToDiscussion(discussionId, userIdList, new RongIMClient.OperationCallback() {
            @Override
            public void onSuccess() {
                RongContext.getInstance().getEventBus().post(new Event.AddMemberToDiscussionEvent(discussionId, userIdList));

                if (callback != null)
                    callback.onSuccess();

            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                if (callback != null)
                    callback.onError(errorCode);
            }
        });
    }

    /**
     * <p>供创建者将某用户移出讨论组。</p>
     * <p>
     * 如果当前登陆用户不是此讨论组的创建者并且此讨论组没有开放加人权限，则会返回错误 {@link RongIMClient.ErrorCode}。
     * 不能使用此接口将自己移除，否则会返回错误 {@link RongIMClient.ErrorCode}。
     * 如果您需要退出该讨论组，可以使用 {@link #quitDiscussion(String, RongIMClient.OperationCallback)} 方法。
     * </p>
     *
     * @param discussionId 讨论组 Id。
     * @param userId       用户 Id。
     * @param callback     执行操作的回调。{@link io.rong.imlib.RongIMClient.OperationCallback}。
     */
    public void removeMemberFromDiscussion(final String discussionId, final String userId, final RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance().removeMemberFromDiscussion(discussionId, userId, new RongIMClient.OperationCallback() {
            @Override
            public void onSuccess() {
                RongContext.getInstance().getEventBus().post(new Event.RemoveMemberFromDiscussionEvent(discussionId, userId));

                if (callback != null)
                    callback.onSuccess();
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                if (callback != null)
                    callback.onError(errorCode);
            }
        });
    }

    /**
     * 退出当前用户所在的某讨论组。
     *
     * @param discussionId 讨论组 Id。
     * @param callback     执行操作的回调。
     */
    public void quitDiscussion(final String discussionId, final RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance().quitDiscussion(discussionId, new RongIMClient.OperationCallback() {
            @Override
            public void onSuccess() {
                RongContext.getInstance().getEventBus().post(new Event.QuitDiscussionEvent(discussionId));

                if (callback != null)
                    callback.onSuccess();
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {

                if (callback != null)
                    callback.onError(errorCode);
            }
        });
    }

    /**
     * 向本地会话中插入一条消息，方向为接收。这条消息只是插入本地会话，不会实际发送给服务器和对方。
     * 插入消息需为入库消息，即 {@link MessageTag.ISPERSISTED}，否者会回调 {@link RongIMClient.ErrorCode.PARAMETER_ERROR}
     *
     * @param type           会话类型。
     * @param targetId       目标会话Id。比如私人会话时，是对方的id； 群组会话时，是群id; 讨论组会话时，则为该讨论,组的id.
     * @param senderUserId   发送方 Id
     * @param receivedStatus 接收状态 @see {@link Message.ReceivedStatus}
     * @param content        消息内容。如{@link TextMessage} {@link ImageMessage}等。
     * @param callback       获得消息发送实体的回调。
     */
    public void insertIncomingMessage(Conversation.ConversationType type, String targetId,
                                      String senderUserId, Message.ReceivedStatus receivedStatus,
                                      MessageContent content, RongIMClient.ResultCallback<Message> callback) {
        insertIncomingMessage(type, targetId, senderUserId, receivedStatus, content, System.currentTimeMillis(), callback);
    }

    /**
     * 向本地会话中插入一条消息，方向为接收。这条消息只是插入本地会话，不会实际发送给服务器和对方。
     * 插入消息需为入库消息，即 {@link MessageTag.ISPERSISTED}，否者会回调 {@link RongIMClient.ErrorCode.PARAMETER_ERROR}
     *
     * @param type           会话类型。
     * @param targetId       目标会话Id。比如私人会话时，是对方的id； 群组会话时，是群id; 讨论组会话时，则为该讨论,组的id.
     * @param senderUserId   发送方 Id
     * @param receivedStatus 接收状态 @see {@link Message.ReceivedStatus}
     * @param content        消息内容。如{@link TextMessage} {@link ImageMessage}等。
     * @param callback       获得消息发送实体的回调。
     */
    public void insertIncomingMessage(Conversation.ConversationType type, String targetId,
                                      String senderUserId, Message.ReceivedStatus receivedStatus,
                                      MessageContent content, long sentTime,
                                      final RongIMClient.ResultCallback<Message> callback) {
        final MessageTag tag = content.getClass().getAnnotation(MessageTag.class);

        if (tag != null && (tag.flag() & MessageTag.ISPERSISTED) == MessageTag.ISPERSISTED) {

            RongIMClient.getInstance().insertIncomingMessage(type, targetId, senderUserId, receivedStatus, content, sentTime, new RongIMClient.ResultCallback<Message>() {
                @Override
                public void onSuccess(Message message) {
                    if (callback != null) {
                        callback.onSuccess(message);
                    }
                    RongContext.getInstance().getEventBus().post(message);
                }

                @Override
                public void onError(RongIMClient.ErrorCode e) {
                    if (callback != null) {
                        callback.onError(e);
                    }
                    if (RongContext.getInstance() == null) {
                        RLog.e(TAG, "insertIncomingMessage. RongIM SDK not init, please do after init.");
                    } else {
                        RongContext.getInstance().getEventBus().post(e);
                    }
                }
            });
        } else {
            if (callback != null) {
                callback.onError(RongIMClient.ErrorCode.PARAMETER_ERROR);
            }
            RLog.e(TAG, "insertMessage Message is missing MessageTag.ISPERSISTED");
        }
    }

    /**
     * 向本地会话中插入一条消息，方向为发送。这条消息只是插入本地会话，不会实际发送给服务器和对方。
     * 插入消息需为入库消息，即 {@link MessageTag#ISPERSISTED}，否者会回调 {@link RongIMClient.ErrorCode#PARAMETER_ERROR}
     *
     * @param type       会话类型。
     * @param targetId   目标会话Id。比如私人会话时，是对方的id； 群组会话时，是群id; 讨论组会话时，则为该讨论,组的id.
     * @param sentStatus 接收状态 @see {@link Message.ReceivedStatus}
     * @param content    消息内容。如{@link TextMessage} {@link ImageMessage}等。
     * @param callback   获得消息发送实体的回调。
     */
    public void insertOutgoingMessage(Conversation.ConversationType type, String targetId,
                                      Message.SentStatus sentStatus, MessageContent content,
                                      RongIMClient.ResultCallback<Message> callback) {
        insertOutgoingMessage(type, targetId, sentStatus, content, System.currentTimeMillis(), callback);
    }

    /**
     * 向本地会话中插入一条消息，方向为发送。这条消息只是插入本地会话，不会实际发送给服务器和对方。
     * 插入消息需为入库消息，即 {@link MessageTag.ISPERSISTED}，否者会回调 {@link RongIMClient.ErrorCode.PARAMETER_ERROR}
     *
     * @param type       会话类型。
     * @param targetId   目标会话Id。比如私人会话时，是对方的id； 群组会话时，是群id; 讨论组会话时，则为该讨论,组的id.
     * @param sentStatus 发送状态 @see {@link Message.SentStatus}
     * @param content    消息内容。如{@link TextMessage} {@link ImageMessage}等。
     * @param callback   获得消息发送实体的回调。
     */
    public void insertOutgoingMessage(Conversation.ConversationType type, String targetId,
                                      Message.SentStatus sentStatus, MessageContent content,
                                      long sentTime, final RongIMClient.ResultCallback<Message> callback) {
        final MessageTag tag = content.getClass().getAnnotation(MessageTag.class);

        if (tag != null && (tag.flag() & MessageTag.ISPERSISTED) == MessageTag.ISPERSISTED) {

            RongIMClient.getInstance().insertOutgoingMessage(type, targetId, sentStatus, content, sentTime, new RongIMClient.ResultCallback<Message>() {
                @Override
                public void onSuccess(Message message) {
                    if (callback != null) {
                        callback.onSuccess(message);
                    }
                    RongContext.getInstance().getEventBus().post(message);
                }

                @Override
                public void onError(RongIMClient.ErrorCode e) {
                    if (callback != null) {
                        callback.onError(e);
                    }
                    if (RongContext.getInstance() == null) {
                        RLog.e(TAG, "insertOutgoingMessage. RongIM SDK not init, please do after init.");
                    } else {
                        RongContext.getInstance().getEventBus().post(e);
                    }
                }
            });
        } else {
            if (callback != null) {
                callback.onError(RongIMClient.ErrorCode.PARAMETER_ERROR);
            }
            RLog.e(TAG, "insertMessage Message is missing MessageTag.ISPERSISTED");
        }
    }

    /**
     * <p>发送消息。
     * 通过 {@link io.rong.imlib.IRongCallback.ISendMessageCallback}
     * 中的方法回调发送的消息状态及消息体。</p>
     *
     * @param message     将要发送的消息体。
     * @param pushContent 当下发 push 消息时，在通知栏里会显示这个字段。
     *                    如果发送的是自定义消息，该字段必须填写，否则无法收到 push 消息。
     *                    如果发送 sdk 中默认的消息类型，例如 RC:TxtMsg, RC:VcMsg, RC:ImgMsg，则不需要填写，默认已经指定。
     * @param pushData    push 附加信息。如果设置该字段，用户在收到 push 消息时，能通过 {@link io.rong.push.notification.PushNotificationMessage#getPushData()} 方法获取。
     * @param callback    发送消息的回调，参考 {@link io.rong.imlib.IRongCallback.ISendMessageCallback}。
     */
    public void sendMessage(Message message, String pushContent, final String pushData, final IRongCallback.ISendMessageCallback callback) {
        final Message filterMsg = filterSendMessage(message);
        if (filterMsg == null) {
            RLog.w(TAG, "sendMessage: 因在 onSend 中消息被过滤为 null，取消发送。");
            return;
        }
        if (filterMsg != message) {
            message = filterMsg;
        }
        message.setContent(setMessageAttachedUserInfo(message.getContent()));
        RongIMClient.getInstance().sendMessage(message, pushContent, pushData, new IRongCallback.ISendMessageCallback() {
            @Override
            public void onAttached(Message message) {
                MessageTag tag = message.getContent().getClass().getAnnotation(MessageTag.class);
                if (tag != null && (tag.flag() & MessageTag.ISPERSISTED) == MessageTag.ISPERSISTED) {
                    RongContext.getInstance().getEventBus().post(message);
                }

                if (callback != null) {
                    callback.onAttached(message);
                }
            }

            @Override
            public void onSuccess(Message message) {
                filterSentMessage(message, null);
                if (callback != null) {
                    callback.onSuccess(message);
                }
            }

            @Override
            public void onError(Message message, RongIMClient.ErrorCode errorCode) {
                filterSentMessage(message, errorCode);
                if (callback != null) {
                    if (errorCode.equals(RongIMClient.ErrorCode.RC_MSG_REPLACED_SENSITIVE_WORD)) {
                        callback.onSuccess(message);
                    } else {
                        callback.onError(message, errorCode);
                    }
                }
            }
        });
    }

    /**
     * <p>发送消息。
     * 通过 {@link io.rong.imlib.IRongCallback.ISendMessageCallback}
     * 中的方法回调发送的消息状态及消息体。</p>
     *
     * @param message     将要发送的消息体。
     * @param pushContent 当下发 push 消息时，在通知栏里会显示这个字段。
     *                    如果发送的是自定义消息，该字段必须填写，否则无法收到 push 消息。
     *                    如果发送 sdk 中默认的消息类型，例如 RC:TxtMsg, RC:VcMsg, RC:ImgMsg，则不需要填写，默认已经指定。
     * @param pushData    push 附加信息。如果设置该字段，用户在收到 push 消息时，能通过 {@link io.rong.push.notification.PushNotificationMessage#getPushData()} 方法获取。
     * @param option      发送消息附加选项，目前仅支持设置 isVoIPPush，如果对端设备是 iOS,设置 isVoIPPush 为 True，会走 VoIP 通道推送 Push.
     * @param callback    发送消息的回调，参考 {@link io.rong.imlib.IRongCallback.ISendMessageCallback}。
     */
    public void sendMessage(Message message, String pushContent, final String pushData, SendMessageOption option, final IRongCallback.ISendMessageCallback callback) {
        final Message filterMsg = filterSendMessage(message);
        if (filterMsg == null) {
            RLog.w(TAG, "sendMessage: 因在 onSend 中消息被过滤为 null，取消发送。");
            return;
        }
        if (filterMsg != message) {
            message = filterMsg;
        }
        message.setContent(setMessageAttachedUserInfo(message.getContent()));
        RongIMClient.getInstance().sendMessage(message, pushContent, pushData, option, new IRongCallback.ISendMessageCallback() {
            @Override
            public void onAttached(Message message) {
                MessageTag tag = message.getContent().getClass().getAnnotation(MessageTag.class);
                if (tag != null && (tag.flag() & MessageTag.ISPERSISTED) == MessageTag.ISPERSISTED) {
                    RongContext.getInstance().getEventBus().post(message);
                }

                if (callback != null) {
                    callback.onAttached(message);
                }
            }

            @Override
            public void onSuccess(Message message) {
                filterSentMessage(message, null);
                if (callback != null) {
                    callback.onSuccess(message);
                }
            }

            @Override
            public void onError(Message message, RongIMClient.ErrorCode errorCode) {
                filterSentMessage(message, errorCode);
                if (callback != null) {
                    if (errorCode.equals(RongIMClient.ErrorCode.RC_MSG_REPLACED_SENSITIVE_WORD)) {
                        callback.onSuccess(message);
                    } else {
                        callback.onError(message, errorCode);
                    }
                }
            }
        });
    }

    /**
     * 根据会话类型，发送消息。
     * <p>
     * 通过 {@link io.rong.imlib.IRongCallback.ISendMessageCallback} 中的方法回调发送的消息状态及消息体。<br>
     * <strong>注意：1 秒钟发送消息不能超过 5 条。 </p>
     *
     * @param type        会话类型。
     * @param targetId    会话 id。根据不同的 conversationType，可能是用户 id、讨论组 id、群组 id 或聊天室 id。
     * @param content     消息内容，例如 {@link TextMessage}, {@link ImageMessage}。
     * @param pushContent 当下发远程推送消息时，在通知栏里会显示这个字段。
     *                    如果发送的是自定义消息，该字段必须填写，否则无法收到远程推送消息。
     *                    如果发送 SDK 中默认的消息类型，例如 RC:TxtMsg, RC:VcMsg, RC:ImgMsg，则不需要填写，默认已经指定。
     * @param pushData    远程推送附加信息。如果设置该字段，用户在收到 push 消息时，能通过 {@link io.rong.push.notification.PushNotificationMessage#getPushData()} 方法获取。
     * @param callback    发送消息的回调。参考 {@link io.rong.imlib.IRongCallback.ISendMessageCallback}。
     * @group 消息操作
     */
    public void sendMessage(final Conversation.ConversationType type, final String targetId, final MessageContent content, final String pushContent, final String pushData, final IRongCallback.ISendMessageCallback callback) {
        Message message = Message.obtain(targetId, type, content);
        sendMessage(message, pushContent, pushData, callback);
    }

    /**
     * <p>发送地理位置消息。并同时更新界面。</p>
     * <p>发送前构造 {@link Message} 消息实体，消息实体中的 content 必须为 {@link LocationMessage}, 否则返回失败。</p>
     * <p>其中的缩略图地址 scheme 只支持 file:// 和 http:// 其他暂不支持。</p>
     *
     * @param message             消息实体。
     * @param pushContent         当下发 push 消息时，在通知栏里会显示这个字段。
     *                            如果发送的是自定义消息，该字段必须填写，否则无法收到 push 消息。
     *                            如果发送 sdk 中默认的消息类型，例如 RC:TxtMsg, RC:VcMsg, RC:ImgMsg，则不需要填写，默认已经指定。
     * @param pushData            push 附加信息。如果设置该字段，用户在收到 push 消息时，能通过 {@link io.rong.push.notification.PushNotificationMessage#getPushData()} 方法获取。
     * @param sendMessageCallback 发送消息的回调，参考 {@link io.rong.imlib.IRongCallback.ISendMessageCallback}。
     */
    public void sendLocationMessage(Message message, String pushContent, final String pushData, final IRongCallback.ISendMessageCallback sendMessageCallback) {
        final Message filterMsg = filterSendMessage(message);
        if (filterMsg == null) {
            RLog.w(TAG, "sendLocationMessage: 因在 onSend 中消息被过滤为 null，取消发送。");
            return;
        }
        if (filterMsg != message) {
            message = filterMsg;
        }
        message.setContent(setMessageAttachedUserInfo(message.getContent()));
        RongIMClient.getInstance().sendLocationMessage(message, pushContent, pushData, new IRongCallback.ISendMessageCallback() {
            @Override
            public void onAttached(Message message) {
                MessageTag tag = message.getContent().getClass().getAnnotation(MessageTag.class);
                if (tag != null && (tag.flag() & MessageTag.ISPERSISTED) == MessageTag.ISPERSISTED) {
                    RongContext.getInstance().getEventBus().post(message);
                }

                if (sendMessageCallback != null) {
                    sendMessageCallback.onAttached(message);
                }
            }

            @Override
            public void onSuccess(Message message) {
                filterSentMessage(message, null);
                if (sendMessageCallback != null) {
                    sendMessageCallback.onSuccess(message);
                }
            }

            @Override
            public void onError(Message message, RongIMClient.ErrorCode errorCode) {
                filterSentMessage(message, errorCode);
                if (sendMessageCallback != null) {
                    sendMessageCallback.onError(message, errorCode);
                }
            }
        });
    }

    /**
     * <p>根据会话类型，发送图片消息。</p>
     *
     * @param type        会话类型。
     * @param targetId    目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param content     消息内容，例如 {@link TextMessage}, {@link ImageMessage}。
     * @param pushContent 当下发 push 消息时，在通知栏里会显示这个字段。
     *                    如果发送的是自定义消息，该字段必须填写，否则无法收到 push 消息。
     *                    如果发送 sdk 中默认的消息类型，例如 RC:TxtMsg, RC:VcMsg, RC:ImgMsg，则不需要填写，默认已经指定。
     * @param pushData    push 附加信息。如果设置该字段，用户在收到 push 消息时，能通过 {@link io.rong.push.notification.PushNotificationMessage#getPushData()} 方法获取。
     * @param callback    发送消息的回调。
     */
    public void sendImageMessage(Conversation.ConversationType type, String
            targetId, MessageContent content, String pushContent, String pushData,
                                 final RongIMClient.SendImageMessageCallback callback) {

        Message message = Message.obtain(targetId, type, content);

        Message temp = filterSendMessage(message);
        if (temp == null)
            return;

        if (temp != message)
            message = temp;

        content = message.getContent();

        content = setMessageAttachedUserInfo(content);

        final RongIMClient.ResultCallback.Result<Event.OnReceiveMessageProgressEvent> result = new RongIMClient.ResultCallback.Result<>();
        result.t = new Event.OnReceiveMessageProgressEvent();

        RongIMClient.SendImageMessageCallback sendMessageCallback = new RongIMClient.SendImageMessageCallback() {

            @Override
            public void onAttached(Message message) {

                RongContext.getInstance().getEventBus().post(message);

                if (callback != null)
                    callback.onAttached(message);
            }

            @Override
            public void onProgress(Message message, int progress) {
                if (result.t == null)
                    return;
                result.t.setMessage(message);
                result.t.setProgress(progress);
                RongContext.getInstance().getEventBus().post(result.t);

                if (callback != null)
                    callback.onProgress(message, progress);
            }

            @Override
            public void onError(Message message, RongIMClient.ErrorCode errorCode) {

                filterSentMessage(message, errorCode);

                if (callback != null)
                    callback.onError(message, errorCode);
            }

            @Override
            public void onSuccess(Message message) {

                filterSentMessage(message, null);

                if (callback != null)
                    callback.onSuccess(message);
            }
        };

        RongIMClient.getInstance().sendImageMessage(type, targetId, content, pushContent, pushData, sendMessageCallback);
    }

    /**
     * <p>发送图片消息</p>
     *
     * @param message     发送消息的实体。
     * @param pushContent 当下发 push 消息时，在通知栏里会显示这个字段。
     *                    如果发送的是自定义消息，该字段必须填写，否则无法收到 push 消息。
     *                    如果发送 sdk 中默认的消息类型，例如 RC:TxtMsg, RC:VcMsg, RC:ImgMsg，则不需要填写，默认已经指定。
     * @param pushData    push 附加信息。如果设置该字段，用户在收到 push 消息时，能通过 {@link io.rong.push.notification.PushNotificationMessage#getPushData()} 方法获取。
     * @param filter      是否过滤消息，false 则不会回调{@link OnSendMessageListener},true 则会回调onSend方法，用户可以处理这条消息。
     * @param callback    发送消息的回调 {@link io.rong.imlib.RongIMClient.SendImageMessageCallback}。
     */
    public void sendImageMessage(Message message, String pushContent, String pushData, boolean filter, final RongIMClient.SendImageMessageCallback callback) {

        if (filter) {
            Message temp = filterSendMessage(message);

            if (temp == null)
                return;

            if (temp != message)
                message = temp;
        }

        setMessageAttachedUserInfo(message.getContent());

        final RongIMClient.ResultCallback.Result<Event.OnReceiveMessageProgressEvent> result = new RongIMClient.ResultCallback.Result<>();
        result.t = new Event.OnReceiveMessageProgressEvent();

        RongIMClient.SendImageMessageCallback sendMessageCallback = new RongIMClient.SendImageMessageCallback() {
            @Override
            public void onAttached(Message message) {
                RongContext.getInstance().getEventBus().post(message);

                if (callback != null)
                    callback.onAttached(message);
            }

            @Override
            public void onProgress(Message message, int progress) {
                if (result.t == null)
                    return;
                result.t.setMessage(message);
                result.t.setProgress(progress);
                RongContext.getInstance().getEventBus().post(result.t);

                if (callback != null)
                    callback.onProgress(message, progress);
            }

            @Override
            public void onError(Message message, RongIMClient.ErrorCode errorCode) {

                filterSentMessage(message, errorCode);

                if (callback != null)
                    callback.onError(message, errorCode);
            }

            @Override
            public void onSuccess(Message message) {

                filterSentMessage(message, null);

                if (callback != null)
                    callback.onSuccess(message);
            }
        };

        RongIMClient.getInstance().sendImageMessage(message, pushContent, pushData, sendMessageCallback);
    }

    /**
     * <p>发送图片消息</p>
     *
     * @param message     发送消息的实体。
     * @param pushContent 当下发 push 消息时，在通知栏里会显示这个字段。
     *                    如果发送的是自定义消息，该字段必须填写，否则无法收到 push 消息。
     *                    如果发送 sdk 中默认的消息类型，例如 RC:TxtMsg, RC:VcMsg, RC:ImgMsg，则不需要填写，默认已经指定。
     * @param pushData    push 附加信息。如果设置该字段，用户在收到 push 消息时，能通过 {@link io.rong.push.notification.PushNotificationMessage#getPushData()} 方法获取。
     * @param callback    发送消息的回调 {@link io.rong.imlib.RongIMClient.SendImageMessageCallback}。
     */
    public void sendImageMessage(Message message, String pushContent,
                                 final String pushData, final RongIMClient.SendImageMessageCallback callback) {
        sendImageMessage(message, pushContent, pushData, true, callback);
    }

    /**
     * <p>发送图片消息，可以使用该方法将图片上传到自己的服务器发送，同时更新图片状态。</p>
     * <p>使用该方法在上传图片时，会回调 {@link io.rong.imlib.RongIMClient.SendImageMessageWithUploadListenerCallback}
     * 此回调中会携带 {@link RongIMClient.UploadImageStatusListener} 对象，使用者只需要调用其中的
     * {@link RongIMClient.UploadImageStatusListener#update(int)} 更新进度
     * {@link RongIMClient.UploadImageStatusListener#success(Uri)} 更新成功状态，并告知上传成功后的图片地址
     * {@link RongIMClient.UploadImageStatusListener#error()} 更新失败状态 </p>
     *
     * @param message     发送消息的实体。
     * @param pushContent 当下发 push 消息时，在通知栏里会显示这个字段。
     *                    如果发送的是自定义消息，该字段必须填写，否则无法收到 push 消息。
     *                    如果发送 sdk 中默认的消息类型，例如 RC:TxtMsg, RC:VcMsg, RC:ImgMsg，则不需要填写，默认已经指定。
     * @param pushData    push 附加信息。如果设置该字段，用户在收到 push 消息时，能通过 {@link io.rong.push.notification.PushNotificationMessage#getPushData()} 方法获取。
     * @param callback    发送消息的回调，回调中携带 {@link RongIMClient.UploadImageStatusListener} 对象，用户调用该对象中的方法更新状态。
     *                    {@link #sendImageMessage(Message, String, String, RongIMClient.SendImageMessageCallback)}
     */
    public void sendImageMessage(Message message, String pushContent,
                                 final String pushData,
                                 final RongIMClient.SendImageMessageWithUploadListenerCallback callback) {

        Message temp = filterSendMessage(message);

        if (temp == null)
            return;

        if (temp != message)
            message = temp;

        final RongIMClient.ResultCallback.Result<Event.OnReceiveMessageProgressEvent> result = new RongIMClient.ResultCallback.Result<>();
        result.t = new Event.OnReceiveMessageProgressEvent();

        RongIMClient.SendImageMessageWithUploadListenerCallback sendMessageCallback = new RongIMClient.SendImageMessageWithUploadListenerCallback() {

            @Override
            public void onAttached(Message message, RongIMClient.UploadImageStatusListener listener) {
                RongContext.getInstance().getEventBus().post(message);

                if (callback != null)
                    callback.onAttached(message, listener);
            }

            @Override
            public void onProgress(Message message, int progress) {
                if (result.t == null)
                    return;
                result.t.setMessage(message);
                result.t.setProgress(progress);
                RongContext.getInstance().getEventBus().post(result.t);

                if (callback != null)
                    callback.onProgress(message, progress);
            }

            @Override
            public void onError(Message message, RongIMClient.ErrorCode errorCode) {

                filterSentMessage(message, errorCode);

                if (callback != null)
                    callback.onError(message, errorCode);
            }

            @Override
            public void onSuccess(Message message) {

                filterSentMessage(message, null);

                if (callback != null)
                    callback.onSuccess(message);
            }
        };

        RongIMClient.getInstance().sendImageMessage(message, pushContent, pushData, sendMessageCallback);
    }

    /**
     * 下载文件。
     * <p/>
     * 用来获取媒体原文件时调用。如果本地缓存中包含此文件，则从本地缓存中直接获取，否则将从服务器端下载。
     *
     * @param conversationType 会话类型。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param mediaType        文件类型。
     * @param imageUrl         文件的 URL 地址。
     * @param callback         下载文件的回调。
     */
    public void downloadMedia(Conversation.ConversationType conversationType, String targetId, RongIMClient.MediaType mediaType, String imageUrl, final RongIMClient.DownloadMediaCallback callback) {
        RongIMClient.getInstance().downloadMedia(conversationType, targetId, mediaType, imageUrl, callback);
    }

    /**
     * 下载文件。和{@link #downloadMedia(Conversation.ConversationType, String, RongIMClient.MediaType, String, RongIMClient.DownloadMediaCallback)}的区别是，该方法支持取消操作。
     * <p/>
     * 用来获取媒体原文件时调用。如果本地缓存中包含此文件，则从本地缓存中直接获取，否则将从服务器端下载。
     *
     * @param message  文件消息。
     * @param callback 下载文件的回调。
     */
    public void downloadMediaMessage(Message message, final IRongCallback.IDownloadMediaMessageCallback callback) {
        RongIMClient.getInstance().downloadMediaMessage(message, new IRongCallback.IDownloadMediaMessageCallback() {
            @Override
            public void onSuccess(Message message) {

                // 进度事件
                final Event.OnReceiveMessageProgressEvent result = new Event.OnReceiveMessageProgressEvent();
                message.getReceivedStatus().setDownload();
                result.setMessage(message);
                result.setProgress(100);
                EventBus.getDefault().post(result);

                EventBus.getDefault().post(new Event.FileMessageEvent(message, 100, ON_SUCCESS_CALLBACK, null));
                if (callback != null) {
                    callback.onSuccess(message);
                }
            }

            @Override
            public void onProgress(Message message, int progress) {
                // 进度事件
                final Event.OnReceiveMessageProgressEvent result = new Event.OnReceiveMessageProgressEvent();
                message.getReceivedStatus().setDownload();
                result.setMessage(message);
                result.setProgress(progress);
                EventBus.getDefault().post(result);

                EventBus.getDefault().post(new Event.FileMessageEvent(message, progress, ON_PROGRESS_CALLBACK, null));
                if (callback != null) {
                    callback.onProgress(message, progress);
                }
            }

            @Override
            public void onError(Message message, RongIMClient.ErrorCode code) {

                final Event.OnReceiveMessageProgressEvent result = new Event.OnReceiveMessageProgressEvent();
                result.setMessage(message);
                result.setProgress(-1);
                message.getReceivedStatus().isDownload();
                EventBus.getDefault().post(result);

                if (message.getContent() instanceof GIFMessage) {
                    EventBus.getDefault().post(new Event.FileMessageEvent(message, -1, ON_ERROR_CALLBACK, code));
                } else {
                    EventBus.getDefault().post(new Event.FileMessageEvent(message, 0, ON_ERROR_CALLBACK, code));
                }

                if (callback != null) {
                    callback.onError(message, code);
                }
            }

            @Override
            public void onCanceled(Message message) {
                EventBus.getDefault().post(new Event.FileMessageEvent(message, 0, ON_CANCEL_CALLBACK, null));
                if (callback != null) {
                    callback.onCanceled(message);
                }
            }
        });
    }

    /**
     * 下载文件
     * 支持断点续传
     *
     * @param uid      文件唯一标识
     * @param fileUrl  文件下载地址
     * @param fileName 文件名
     * @param path     文件下载保存目录，如果是 targetVersion 29 为目标，由于访问权限原因，建议使用 context.getExternalFilesDir() 方法保存到私有目录
     * @param callback 回调
     */
    public void downloadMediaFile(final String uid, String fileUrl, String fileName, String path, final IRongCallback.IDownloadMediaFileCallback callback) {
        RongIMClient.getInstance().downloadMediaFile(uid, fileUrl, fileName, path, new IRongCallback.IDownloadMediaFileCallback() {
            @Override
            public void onFileNameChanged(String newFileName) {
                if (callback != null) {
                    callback.onFileNameChanged(newFileName);
                }
            }

            @Override
            public void onSuccess() {
                EventBus.getDefault().post(new Event.MediaFileEvent(uid, 100, ON_SUCCESS_CALLBACK, null));
                if (callback != null) {
                    callback.onSuccess();
                }
            }

            @Override
            public void onProgress(int progress) {
                EventBus.getDefault().post(new Event.MediaFileEvent(uid, progress, ON_PROGRESS_CALLBACK, null));
                if (callback != null) {
                    callback.onProgress(progress);
                }
            }

            @Override
            public void onError(RongIMClient.ErrorCode code) {
                EventBus.getDefault().post(new Event.MediaFileEvent(uid, 0, ON_ERROR_CALLBACK, code));
                if (callback != null) {
                    callback.onError(code);
                }
            }

            @Override
            public void onCanceled() {
                EventBus.getDefault().post(new Event.MediaFileEvent(uid, 0, ON_CANCEL_CALLBACK, null));
                if (callback != null) {
                    callback.onCanceled();
                }
            }
        });
    }

    /**
     * 下载文件。
     *
     * @param imageUrl 文件的 URL 地址。
     * @param callback 下载文件的回调。
     */
    public void downloadMedia(String imageUrl, final RongIMClient.DownloadMediaCallback callback) {

        ImageLoader.getInstance().loadImage(imageUrl, null, null, new ImageLoadingListener() {
            @Override
            public void onLoadingStarted(String imageUri, View view) {

            }

            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                if (callback != null)
                    callback.onError(RongIMClient.ErrorCode.RC_NET_UNAVAILABLE);
            }

            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                if (callback != null)
                    callback.onSuccess(imageUri);
            }

            @Override
            public void onLoadingCancelled(String imageUri, View view) {

            }
        }, new ImageLoadingProgressListener() {
            @Override
            public void onProgressUpdate(String imageUri, View view, int current, int total) {
                if (callback != null) {
                    callback.onProgress((current * 100) / total);
                }
            }
        });
    }

    /**
     * 获取会话消息提醒状态。
     *
     * @param conversationType 会话类型。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @param callback         获取状态的回调。
     */
    public void getConversationNotificationStatus(final Conversation.ConversationType conversationType, final String targetId, final RongIMClient.ResultCallback<Conversation.ConversationNotificationStatus> callback) {
        if (RongContext.getInstance() == null) {
            RLog.e(TAG, "getConversationNotificationStatus. RongIM SDK not init, please do after init.");
            if (callback != null) {
                callback.onError(RongIMClient.ErrorCode.IPC_DISCONNECT);
            }
            return;
        }
        Conversation.ConversationNotificationStatus status = RongContext.getInstance().getConversationNotifyStatusFromCache(ConversationKey.obtain(targetId, conversationType));
        if (status != null) {
            callback.onSuccess(status);
        } else {
            RongIMClient.getInstance().getConversationNotificationStatus(conversationType, targetId, new RongIMClient.ResultCallback<Conversation.ConversationNotificationStatus>() {
                @Override
                public void onSuccess(Conversation.ConversationNotificationStatus status) {

                    RongContext.getInstance().setConversationNotifyStatusToCache(ConversationKey.obtain(targetId, conversationType), status);

                    if (callback != null) {
                        callback.onSuccess(status);
                    }
                }

                @Override
                public void onError(RongIMClient.ErrorCode e) {

                    if (callback != null) {
                        callback.onError(e);
                    }
                }
            });
        }
    }

    /**
     * 设置会话消息提醒状态。
     *
     * @param conversationType   会话类型。
     * @param targetId           目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @param notificationStatus 是否屏蔽。
     * @param callback           设置状态的回调。
     */
    public void setConversationNotificationStatus(final Conversation.ConversationType conversationType, final String targetId, final Conversation.ConversationNotificationStatus notificationStatus, final RongIMClient.ResultCallback<Conversation.ConversationNotificationStatus> callback) {
        RongIMClient.getInstance().setConversationNotificationStatus(conversationType, targetId, notificationStatus, new RongIMClient.ResultCallback<Conversation.ConversationNotificationStatus>() {
            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                if (callback != null)
                    callback.onError(errorCode);
            }

            @Override
            public void onSuccess(Conversation.ConversationNotificationStatus status) {
                RongContext.getInstance().setConversationNotifyStatusToCache(ConversationKey.obtain(targetId, conversationType), status);
                RongContext.getInstance().getEventBus().post(new Event.ConversationNotificationEvent(targetId, conversationType, notificationStatus));

                if (callback != null)
                    callback.onSuccess(status);
            }
        });
    }

    /**
     * 设置讨论组成员邀请权限。
     *
     * @param discussionId 讨论组 id。
     * @param status       邀请状态，默认为开放。
     * @param callback     设置权限的回调。
     */
    public void setDiscussionInviteStatus(final String discussionId, final RongIMClient.DiscussionInviteStatus status, final RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance().setDiscussionInviteStatus(discussionId, status, new RongIMClient.OperationCallback() {

            @Override
            public void onSuccess() {
                RongContext.getInstance().getEventBus().post(new Event.DiscussionInviteStatusEvent(discussionId, status));

                if (callback != null)
                    callback.onSuccess();
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                if (callback != null)
                    callback.onError(errorCode);
            }
        });
    }

    /**
     * 获取当前连接用户的信息。
     *
     * @return 当前连接用户的信息。
     */
    public String getCurrentUserId() {
        return RongIMClient.getInstance().getCurrentUserId();
    }

    /**
     * 获取本地时间与服务器时间的差值。
     * 消息发送成功后，sdk 会与服务器同步时间，消息所在数据库中存储的时间就是服务器时间。
     *
     * @return 本地时间与服务器时间的差值。
     */
    public long getDeltaTime() {
        return RongIMClient.getInstance().getDeltaTime();
    }

    /**
     * 加入聊天室。
     * <p>如果聊天室不存在，sdk 会创建聊天室并加入，如果已存在，则直接加入</p>
     * <p>加入聊天室时，可以选择拉取聊天室消息数目。</p>
     *
     * @param chatroomId      聊天室 Id。
     * @param defMessageCount 进入聊天室拉取消息数目，-1 时不拉取任何消息，0 时拉取 10 条消息，最多只能拉取 50 条。（加入聊天室时会传本地最后一条消息的时间戳，拉取的是这个时间戳之后的消息。比如：这个时间戳之后有3条消息，defMessageCount传10，也只能拉到3条消息。）
     * @param callback        状态回调。
     */
    public void joinChatRoom(final String chatroomId, final int defMessageCount, final RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance().joinChatRoom(chatroomId, defMessageCount, new RongIMClient.OperationCallback() {

            @Override
            public void onSuccess() {
                RongContext.getInstance().getEventBus().post(new Event.JoinChatRoomEvent(chatroomId, defMessageCount));
                if (callback != null)
                    callback.onSuccess();
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                if (callback != null)
                    callback.onError(errorCode);
            }
        });
    }

    /**
     * 加入已存在的聊天室。
     * <p>如果聊天室不存在，则加入失败</p>
     * <p>加入聊天室时，可以选择拉取聊天室消息数目。</p>
     *
     * @param chatroomId      聊天室 Id。
     * @param defMessageCount 进入聊天室拉取消息数目，-1 时不拉取任何消息，0 时拉取 10 条消息，最多只能拉取 40 条。
     * @param callback        状态回调。
     */
    public void joinExistChatRoom(final String chatroomId, final int defMessageCount, final RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance().joinExistChatRoom(chatroomId, defMessageCount, new RongIMClient.OperationCallback() {

            @Override
            public void onSuccess() {
                RongContext.getInstance().getEventBus().post(new Event.JoinChatRoomEvent(chatroomId, defMessageCount));

                if (callback != null)
                    callback.onSuccess();
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {

                if (callback != null)
                    callback.onError(errorCode);
            }
        });
    }

    /**
     * 退出聊天室。
     *
     * @param chatroomId 聊天室 Id。
     * @param callback   状态回调。
     */
    public void quitChatRoom(final String chatroomId, final RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance().quitChatRoom(chatroomId, new RongIMClient.OperationCallback() {

            @Override
            public void onSuccess() {
                RongContext.getInstance().getEventBus().post(new Event.QuitChatRoomEvent(chatroomId));

                if (callback != null)
                    callback.onSuccess();
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {

                if (callback != null)
                    callback.onError(errorCode);
            }
        });
    }

    /**
     * 清空所有会话及会话消息，回调方式通知是否清空成功。
     *
     * @param callback          是否清空成功的回调。
     * @param conversationTypes 会话类型。
     */
    public void clearConversations(final RongIMClient.ResultCallback callback, final Conversation.ConversationType... conversationTypes) {
        RongIMClient.getInstance().clearConversations(new RongIMClient.ResultCallback() {
            @Override
            public void onSuccess(Object o) {
                RongContext.getInstance().getEventBus().post(Event.ClearConversationEvent.obtain(conversationTypes));
                if (callback != null)
                    callback.onSuccess(o);
            }

            @Override
            public void onError(RongIMClient.ErrorCode e) {
                if (callback != null)
                    callback.onError(e);
            }
        }, conversationTypes);
    }

    /**
     * 将某个用户加到黑名单中。
     * <p>当你把对方加入黑名单后，对方再发消息时，就会提示“已被加入黑名单，消息发送失败”。
     * 但你依然可以发消息个对方。</p>
     *
     * @param userId   用户 Id。
     * @param callback 加到黑名单回调。
     */
    public void addToBlacklist(final String userId, final RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance().addToBlacklist(userId, new RongIMClient.OperationCallback() {

            @Override
            public void onSuccess() {
                RongContext.getInstance().getEventBus().post(new Event.AddToBlacklistEvent(userId));

                if (callback != null)
                    callback.onSuccess();
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {

                if (callback != null)
                    callback.onError(errorCode);
            }
        });
    }

    /**
     * 将个某用户从黑名单中移出。
     *
     * @param userId   用户 Id。
     * @param callback 移除黑名单回调。
     */
    public void removeFromBlacklist(final String userId, final RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance().removeFromBlacklist(userId, new RongIMClient.OperationCallback() {

            @Override
            public void onSuccess() {
                RongContext.getInstance().getEventBus().post(new Event.RemoveFromBlacklistEvent(userId));

                if (callback != null)
                    callback.onSuccess();
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {

                if (callback != null)
                    callback.onError(errorCode);
            }
        });
    }

    /**
     * 获取某用户是否在黑名单中。
     *
     * @param userId   用户 Id。
     * @param callback 获取用户是否在黑名单回调。
     */
    public void getBlacklistStatus(String userId, RongIMClient.ResultCallback<RongIMClient.BlacklistStatus> callback) {
        RongIMClient.getInstance().getBlacklistStatus(userId, callback);
    }

    /**
     * 获取当前用户的黑名单列表。
     *
     * @param callback 获取黑名单回调。
     */
    public void getBlacklist(RongIMClient.GetBlacklistCallback callback) {
        RongIMClient.getInstance().getBlacklist(callback);
    }

    /**
     * 设置会话通知免打扰时间。
     *
     * @param startTime   起始时间 格式 HH:MM:SS。
     * @param spanMinutes 间隔分钟数大于 0 小于 1440。
     * @param callback    设置会话通知免打扰时间回调。
     */
    public void setNotificationQuietHours(final String startTime, final int spanMinutes, final RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance().setNotificationQuietHours(startTime, spanMinutes, new RongIMClient.OperationCallback() {
            @Override
            public void onSuccess() {
                MessageNotificationManager.getInstance().setNotificationQuietHours(startTime, spanMinutes);
                if (callback != null) {
                    callback.onSuccess();
                }
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                if (callback != null) {
                    callback.onError(errorCode);
                }
            }
        });
    }

    /**
     * 移除会话通知免打扰时间。
     *
     * @param callback 移除会话通知免打扰时间回调。
     */
    public void removeNotificationQuietHours(final RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance().removeNotificationQuietHours(new RongIMClient.OperationCallback() {
            @Override
            public void onSuccess() {
                MessageNotificationManager.getInstance().setNotificationQuietHours(null, 0);
                if (callback != null) {
                    callback.onSuccess();
                }
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                if (callback != null) {
                    callback.onError(errorCode);
                }
            }
        });
    }

    /**
     * 获取会话通知免打扰时间。
     *
     * @param callback 获取会话通知免打扰时间回调。
     */
    public void getNotificationQuietHours(final RongIMClient.GetNotificationQuietHoursCallback callback) {
        RongIMClient.getInstance().getNotificationQuietHours(new RongIMClient.GetNotificationQuietHoursCallback() {
            @Override
            public void onSuccess(String startTime, int spanMinutes) {
                MessageNotificationManager.getInstance().setNotificationQuietHours(startTime, spanMinutes);
                notificationQuiteHoursConfigured = true;
                if (callback != null) {
                    callback.onSuccess(startTime, spanMinutes);
                }
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                if (callback != null) {
                    callback.onError(errorCode);
                }
            }
        });
    }

    /**
     * 获取公众服务信息。
     *
     * @param publicServiceType 会话类型，APP_PUBLIC_SERVICE 或者 PUBLIC_SERVICE。
     * @param publicServiceId   公众服务 Id。
     * @param callback          获取公众号信息回调。
     */
    public void getPublicServiceProfile(Conversation.PublicServiceType publicServiceType, String publicServiceId, RongIMClient.ResultCallback<PublicServiceProfile> callback) {
        RongIMClient.getInstance().getPublicServiceProfile(publicServiceType, publicServiceId, callback);
    }

    /**
     * 搜索公众服务。
     *
     * @param searchType 搜索类型枚举。
     * @param keywords   搜索关键字。
     * @param callback   搜索结果回调。
     */
    public void searchPublicService(RongIMClient.SearchType searchType, String keywords, RongIMClient.ResultCallback<PublicServiceProfileList> callback) {
        RongIMClient.getInstance().searchPublicService(searchType, keywords, callback);
    }

    /**
     * 按公众服务类型搜索公众服务。
     *
     * @param publicServiceType 公众服务类型。
     * @param searchType        搜索类型枚举。
     * @param keywords          搜索关键字。
     * @param callback          搜索结果回调。
     */
    public void searchPublicServiceByType(Conversation.PublicServiceType publicServiceType, RongIMClient.SearchType searchType, final String keywords, final RongIMClient.ResultCallback<PublicServiceProfileList> callback) {
        RongIMClient.getInstance().searchPublicServiceByType(publicServiceType, searchType, keywords, callback);
    }

    /**
     * 订阅公众号。
     *
     * @param publicServiceId   公共服务 Id。
     * @param publicServiceType 公众服务类型枚举。
     * @param callback          订阅公众号回调。
     */
    public void subscribePublicService(Conversation.PublicServiceType publicServiceType, String publicServiceId, RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance().subscribePublicService(publicServiceType, publicServiceId, callback);
    }

    /**
     * 取消订阅公众号。
     *
     * @param publicServiceId   公共服务 Id。
     * @param publicServiceType 公众服务类型枚举。
     * @param callback          取消订阅公众号回调。
     */
    public void unsubscribePublicService(Conversation.PublicServiceType publicServiceType, String publicServiceId, RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance().unsubscribePublicService(publicServiceType, publicServiceId, callback);
    }

    /**
     * 获取己关注公共账号列表。
     *
     * @param callback 获取己关注公共账号列表回调。
     */
    public void getPublicServiceList(RongIMClient.ResultCallback<PublicServiceProfileList> callback) {
        RongIMClient.getInstance().getPublicServiceList(callback);
    }

    /**
     * 设置请求权限的监听器。在Android 6.0 以上系统时，如果融云内部需要请求某些权限，会通过这个监听器像用户请求对应权限。
     * 用户可以在该权限监听器里调用Android 6.0相关权限请求api,进行权限处理。
     *
     * @param listener 权限监听器。
     **/
    @Deprecated
    public void setRequestPermissionListener(RequestPermissionsListener listener) {
        if (RongContext.getInstance() == null) {
            RLog.e(TAG, "setRequestPermissionListener. RongIM SDK not init, please do after init.");
        } else {
            RongContext.getInstance().setRequestPermissionListener(listener);
        }
    }

    /**
     * <p>记录在开发者后台使用后台推送功能时，对应的推送通知的点击事件。开发者后台的推送打开率既根据客户端上传的该事件进行相应统计和计算。
     * 2.6.0之前版本，推送打开率的使用请在知识库里搜索标签push，有相关说明。
     * 2.6.0之后版本，如果用户使用的SDK内置的通知实现，则不需要调用该方法来统计推送打开率，SDK内部已经帮用户做了统计。
     * 但是如果用户自己定义了推送时通知栏的显示，则需要在点击通知时，调用此方法，来向服务器上传推送打开事件。</p>
     *
     * @param pushId push通知的id。只有当后台广播消息和后台推送时，pushId才会有值，其余非后台情况下都为空。
     */
    public void recordNotificationEvent(String pushId) {
        RongPushClient.recordNotificationEvent(pushId);
    }

    /**
     * 请求权限监听器。
     **/
    @Deprecated
    public interface RequestPermissionsListener {
        @Deprecated
        void onPermissionRequest(String[] permissions, int requestCode);
    }

    private MessageContent setMessageAttachedUserInfo(MessageContent content) {
        if (RongContext.getInstance() == null) {
            RLog.e(TAG, "setMessageAttachedUserInfo. RongIM SDK not init, please do after init.");
            return content;
        }

        if (RongContext.getInstance().getUserInfoAttachedState()) {

            if (content.getUserInfo() == null) {
                String userId = RongIM.getInstance().getCurrentUserId();

                UserInfo info = RongContext.getInstance().getCurrentUserInfo();

                if (info == null)
                    info = RongUserInfoManager.getInstance().getUserInfo(userId);

                if (info != null)
                    content.setUserInfo(info);
            }
        }

        return content;
    }

    /**
     * 对UI已发送消息进行过滤。
     *
     * @param conversationType 会话类型
     * @param targetId         会话id
     * @param messageContent   消息内容
     * @return 消息
     */
    private Message filterSendMessage(Conversation.ConversationType conversationType, String targetId, MessageContent messageContent) {
        Message message = new Message();
        message.setConversationType(conversationType);
        message.setTargetId(targetId);
        message.setContent(messageContent);

        if (RongContext.getInstance() == null) {
            RLog.e(TAG, "filterSendMessage. RongIM SDK not init, please do after init.");
            return message;
        }

        if (RongContext.getInstance().getOnSendMessageListener() != null) {
            message = RongContext.getInstance().getOnSendMessageListener().onSend(message);
        }

        return message;
    }

    /**
     * 对 UI 已发送消息进行过滤。
     *
     * @param message 消息
     * @return 消息
     */
    private Message filterSendMessage(Message message) {
        if (RongContext.getInstance() == null) {
            RLog.e(TAG, "filterSendMessage. RongIM SDK not init, please do after init.");
            return message;
        }

        if (RongContext.getInstance().getOnSendMessageListener() != null) {
            message = RongContext.getInstance().getOnSendMessageListener().onSend(message);
        }

        return message;
    }

    private void filterSentMessage(Message message, RongIMClient.ErrorCode errorCode) {

        SentMessageErrorCode sentMessageErrorCode = null;
        boolean isExecute = false;

        if (RongContext.getInstance() == null) {
            RLog.e(TAG, "filterSendMessage. RongIM SDK not init, please do after init.");
        } else if (RongContext.getInstance().getOnSendMessageListener() != null) {

            if (errorCode != null) {
                sentMessageErrorCode = SentMessageErrorCode.setValue(errorCode.getValue());
            }

            isExecute = RongContext.getInstance().getOnSendMessageListener().onSent(message, sentMessageErrorCode);
        }

        if (errorCode != null && !isExecute && errorCode != RongIMClient.ErrorCode.RC_MSG_REPLACED_SENSITIVE_WORD) {

            if (errorCode.equals(RongIMClient.ErrorCode.NOT_IN_DISCUSSION) || errorCode.equals(RongIMClient.ErrorCode.NOT_IN_GROUP)
                    || errorCode.equals(RongIMClient.ErrorCode.NOT_IN_CHATROOM) || errorCode.equals(RongIMClient.ErrorCode.REJECTED_BY_BLACKLIST) || errorCode.equals(RongIMClient.ErrorCode.FORBIDDEN_IN_GROUP)
                    || errorCode.equals(RongIMClient.ErrorCode.FORBIDDEN_IN_CHATROOM) || errorCode.equals(RongIMClient.ErrorCode.KICKED_FROM_CHATROOM)) {

                InformationNotificationMessage informationMessage = null;

                if (errorCode.equals(RongIMClient.ErrorCode.NOT_IN_DISCUSSION)) {
                    informationMessage = InformationNotificationMessage.obtain(mApplicationContext.getString(R.string.rc_info_not_in_discussion));
                } else if (errorCode.equals(RongIMClient.ErrorCode.NOT_IN_GROUP)) {
                    informationMessage = InformationNotificationMessage.obtain(mApplicationContext.getString(R.string.rc_info_not_in_group));
                } else if (errorCode.equals(RongIMClient.ErrorCode.NOT_IN_CHATROOM)) {
                    informationMessage = InformationNotificationMessage.obtain(mApplicationContext.getString(R.string.rc_info_not_in_chatroom));
                } else if (errorCode.equals(RongIMClient.ErrorCode.REJECTED_BY_BLACKLIST)) {
                    informationMessage = InformationNotificationMessage.obtain(mApplicationContext.getString(R.string.rc_rejected_by_blacklist_prompt));
                } else if (errorCode.equals(RongIMClient.ErrorCode.FORBIDDEN_IN_GROUP)) {
                    informationMessage = InformationNotificationMessage.obtain(mApplicationContext.getString(R.string.rc_info_forbidden_to_talk));
                } else if (errorCode.equals(RongIMClient.ErrorCode.FORBIDDEN_IN_CHATROOM)) {
                    informationMessage = InformationNotificationMessage.obtain(mApplicationContext.getString(R.string.rc_forbidden_in_chatroom));
                } else if (errorCode.equals(RongIMClient.ErrorCode.KICKED_FROM_CHATROOM)) {
                    informationMessage = InformationNotificationMessage.obtain(mApplicationContext.getString(R.string.rc_kicked_from_chatroom));
                }

                insertOutgoingMessage(message.getConversationType(), message.getTargetId(), null, informationMessage, null);
            }

            MessageContent content = message.getContent();
            if (content == null) {
                RLog.e(TAG, "filterSentMessage content is null");
                return;
            }
            MessageTag tag = content.getClass().getAnnotation(MessageTag.class);

            if (ResendManager.getInstance().isResendErrorCode(errorCode)
                    && tag != null && (tag.flag() & MessageTag.ISPERSISTED) == MessageTag.ISPERSISTED) {
                // 发送失败的消息存入重发列表
                ResendManager.getInstance().addResendMessage(message, errorCode, new ResendManager.AddResendMessageCallBack() {
                    @Override
                    public void onComplete(Message message, RongIMClient.ErrorCode errorCode) {
                        RongContext.getInstance().getEventBus().post(new Event.OnMessageSendErrorEvent(message, errorCode));
                    }
                });
            } else {
                RongContext.getInstance().getEventBus().post(new Event.OnMessageSendErrorEvent(message, errorCode));
            }
        } else {//发消息成功 onSuccess()或onProgress()
            if (message != null) {
                MessageContent content = message.getContent();

                MessageTag tag = content.getClass().getAnnotation(MessageTag.class);

                if (RongContext.getInstance() != null && tag != null && (tag.flag() & MessageTag.ISPERSISTED) == MessageTag.ISPERSISTED &&
                        message.getSentStatus() != null && message.getSentStatus().getValue() != io.rong.imlib.model.Message.SentStatus.CANCELED.getValue()) {
                    RongContext.getInstance().getEventBus().post(message);
                }
            }
        }
    }

    /**
     * 设置私有部署的导航服务器和媒体服务器地址。
     * 此方法要在 {@link #init(Context, String)} 前使用
     * 支持传入多个导航, 多个导航地址之间须以分号 ; 分隔
     *
     * @param naviServer 私有部署的导航服务器地址。
     * @param fileServer 私有部署的媒体服务器地址，即文件和图片的上传地址。使用私有云时必须填写。
     */
    public static void setServerInfo(final String naviServer, final String fileServer) {
        if (TextUtils.isEmpty(naviServer)) {
            RLog.e(TAG, "setServerInfo naviServer should not be null.");
            throw new IllegalArgumentException("naviServer should not be null.");
        }
        RongIMClient.setServerInfo(naviServer, fileServer);
    }

    /**
     * 设置数据上传服务器地址。
     * 可以支持设置 http://cn.xxx.com 或者 https://cn.xxx.com 或者 cn.xxx.com
     * 如果设置成 cn.xxx.com，sdk 会组装成并仅支持 http:// 协议格式。
     *
     * @param domain 域名
     */
    public static void setStatisticDomain(String domain) {
        RongIMClient.setStatisticDomain(domain);
    }

    /**
     * 设置公众服务菜单点击监听。
     * 建议使用方法：在进入对应公众服务会话时，设置监听。当退出会话时，重置监听为 null，这样可以防止内存泄露。
     *
     * @param menuClickListener 监听。
     */
    public void setPublicServiceMenuClickListener(IPublicServiceMenuClickListener menuClickListener) {
        if (RongContext.getInstance() != null) {
            RongContext.getInstance().setPublicServiceMenuClickListener(menuClickListener);
        }
    }

    /**
     * 撤回消息
     *
     * @param message     将被撤回的消息
     * @param pushContent 被撤回时，通知栏显示的信息
     */
    public void recallMessage(final Message message, String pushContent) {
        RongIMClient.getInstance().recallMessage(message, pushContent, new RongIMClient.ResultCallback<RecallNotificationMessage>() {
            @Override
            public void onSuccess(RecallNotificationMessage recallNotificationMessage) {
                RongContext.getInstance().getEventBus().post(new Event.MessageRecallEvent(message.getMessageId(), recallNotificationMessage, true));
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                RLog.d(TAG, "recallMessage errorCode = " + errorCode.getValue());
            }
        });
    }

    /**
     * <p>发送多媒体消息</p>
     * <p>发送前构造 {@link Message} 消息实体，消息实体中的 content 必须为 {@link FileMessage}, 否则返回失败。</p>
     *
     * @param message     发送消息的实体。
     * @param pushContent 当下发 push 消息时，在通知栏里会显示这个字段。
     *                    发送文件消息时，此字段必须填写，否则会收不到 push 推送。
     * @param pushData    push 附加信息。如果设置该字段，用户在收到 push 消息时，能通过 {@link io.rong.push.notification.PushNotificationMessage#getPushData()} 方法获取。
     * @param callback    发送消息的回调 {@link io.rong.imlib.RongIMClient.SendMediaMessageCallback}。
     */
    public void sendMediaMessage(Message message, String pushContent,
                                 final String pushData, final IRongCallback.ISendMediaMessageCallback callback) {

        Message temp = filterSendMessage(message);

        if (temp == null) {
            return;
        }

        if (temp != message)
            message = temp;

        setMessageAttachedUserInfo(message.getContent());

        final RongIMClient.ResultCallback.Result<Event.OnReceiveMessageProgressEvent> result = new RongIMClient.ResultCallback.Result<>();
        result.t = new Event.OnReceiveMessageProgressEvent();

        IRongCallback.ISendMediaMessageCallback sendMessageCallback = new IRongCallback.ISendMediaMessageCallback() {
            @Override
            public void onProgress(Message message, int progress) {
                if (result.t == null)
                    return;
                result.t.setMessage(message);
                result.t.setProgress(progress);
                RongContext.getInstance().getEventBus().post(result.t);

                if (callback != null)
                    callback.onProgress(message, progress);
            }

            @Override
            public void onAttached(Message message) {
                RongContext.getInstance().getEventBus().post(message);

                if (callback != null)
                    callback.onAttached(message);
            }

            @Override
            public void onSuccess(Message message) {
                filterSentMessage(message, null);

                if (callback != null)
                    callback.onSuccess(message);
            }

            @Override
            public void onError(Message message, RongIMClient.ErrorCode errorCode) {
                filterSentMessage(message, errorCode);

                if (callback != null)
                    callback.onError(message, errorCode);
            }

            @Override
            public void onCanceled(Message message) {
                filterSentMessage(message, null);
                if (callback != null) {
                    callback.onCanceled(message);
                }
            }
        };

        RongIMClient.getInstance().sendMediaMessage(message, pushContent, pushData, sendMessageCallback);
    }

    /**
     * <p>发送定向多媒体消息</p> 向会话中特定的某些用户发送消息，会话中其他用户将不会收到此消息。
     * <p>发送前构造 {@link Message} 消息实体，消息实体中的 content 必须为多媒体消息，如 {@link ImageMessage} {@link FileMessage} </p>
     * <p>或者其他继承自 {@link MediaMessageContent} 的消息</p>
     *
     * @param message     发送消息的实体。
     * @param userIds     定向接收者 id 数组
     * @param pushContent 当下发 push 消息时，在通知栏里会显示这个字段。
     *                    发送文件消息时，此字段必须填写，否则会收不到 push 推送。
     * @param pushData    push 附加信息。如果设置该字段，用户在收到 push 消息时，能通过 {@link io.rong.push.notification.PushNotificationMessage#getPushData()} 方法获取。
     * @param callback    发送消息的回调 {@link io.rong.imlib.RongIMClient.SendMediaMessageCallback}。
     */
    public void sendDirectionalMediaMessage(Message message, String[] userIds, String pushContent,
                                            final String pushData, final IRongCallback.ISendMediaMessageCallback callback) {

        Message temp = filterSendMessage(message);

        if (temp == null) {
            return;
        }

        if (temp != message)
            message = temp;

        setMessageAttachedUserInfo(message.getContent());

        final RongIMClient.ResultCallback.Result<Event.OnReceiveMessageProgressEvent> result = new RongIMClient.ResultCallback.Result<>();
        result.t = new Event.OnReceiveMessageProgressEvent();

        IRongCallback.ISendMediaMessageCallback sendMessageCallback = new IRongCallback.ISendMediaMessageCallback() {
            @Override
            public void onProgress(Message message, int progress) {
                if (result.t == null)
                    return;
                result.t.setMessage(message);
                result.t.setProgress(progress);
                RongContext.getInstance().getEventBus().post(result.t);

                if (callback != null)
                    callback.onProgress(message, progress);
            }

            @Override
            public void onAttached(Message message) {
                RongContext.getInstance().getEventBus().post(message);

                if (callback != null)
                    callback.onAttached(message);
            }

            @Override
            public void onSuccess(Message message) {
                filterSentMessage(message, null);

                if (callback != null)
                    callback.onSuccess(message);
            }

            @Override
            public void onError(Message message, RongIMClient.ErrorCode errorCode) {
                filterSentMessage(message, errorCode);

                if (callback != null)
                    callback.onError(message, errorCode);
            }

            @Override
            public void onCanceled(Message message) {
                filterSentMessage(message, null);
                if (callback != null) {
                    callback.onCanceled(message);
                }
            }
        };

        RongIMClient.getInstance().sendDirectionalMediaMessage(message, userIds, pushContent, pushData, sendMessageCallback);
    }


    /**
     * <p>发送多媒体消息，可以使用该方法将多媒体文件上传到自己的服务器。
     * 使用该方法在上传多媒体文件时，会回调 {@link io.rong.imlib.IRongCallback.ISendMediaMessageCallbackWithUploader#onAttached(Message, IRongCallback.MediaMessageUploader)}
     * 此回调中会携带 {@link IRongCallback.MediaMessageUploader} 对象，使用者只需要调用此对象中的
     * {@link IRongCallback.MediaMessageUploader#update(int)} 更新进度
     * {@link IRongCallback.MediaMessageUploader#success(Uri)} 更新成功状态，并告知上传成功后的文件地址
     * {@link IRongCallback.MediaMessageUploader#error()} 更新失败状态
     * </p>
     *
     * @param message     发送消息的实体。
     * @param pushContent 当下发 push 消息时，在通知栏里会显示这个字段。
     *                    如果发送的是自定义消息，该字段必须填写，否则无法收到 push 消息。
     *                    如果发送 sdk 中默认的消息类型，例如 RC:TxtMsg, RC:VcMsg, RC:ImgMsg, RC:FileMsg，则不需要填写，默认已经指定。
     * @param pushData    push 附加信息。如果设置该字段，用户在收到 push 消息时，能通过 {@link io.rong.push.notification.PushNotificationMessage#getPushData()} 方法获取。
     * @param callback    发送消息的回调，回调中携带 {@link IRongCallback.MediaMessageUploader} 对象，用户调用该对象中的方法更新状态。
     */
    public void sendMediaMessage(Message message, String pushContent, final String pushData, final IRongCallback.ISendMediaMessageCallbackWithUploader callback) {

        Message temp = filterSendMessage(message);
        if (temp == null) {
            return;
        }
        if (temp != message) {
            message = temp;
        }
        setMessageAttachedUserInfo(message.getContent());

        IRongCallback.ISendMediaMessageCallbackWithUploader sendMediaMessageCallbackWithUploader = new IRongCallback.ISendMediaMessageCallbackWithUploader() {
            @Override
            public void onAttached(Message message, IRongCallback.MediaMessageUploader uploader) {
                MessageTag tag = message.getContent().getClass().getAnnotation(MessageTag.class);
                if (tag != null && (tag.flag() & MessageTag.ISPERSISTED) == MessageTag.ISPERSISTED) {
                    RongContext.getInstance().getEventBus().post(message);
                }
                if (callback != null) {
                    callback.onAttached(message, uploader);
                }
            }

            @Override
            public void onProgress(Message message, int progress) {
                final RongIMClient.ResultCallback.Result<Event.OnReceiveMessageProgressEvent> result = new RongIMClient.ResultCallback.Result<>();
                result.t = new Event.OnReceiveMessageProgressEvent();
                result.t.setMessage(message);
                result.t.setProgress(progress);
                RongContext.getInstance().getEventBus().post(result.t);
                if (callback != null) {
                    callback.onProgress(message, progress);
                }
            }

            @Override
            public void onSuccess(Message message) {
                filterSentMessage(message, null);
                if (callback != null) {
                    callback.onSuccess(message);
                }
            }

            @Override
            public void onError(Message message, RongIMClient.ErrorCode errorCode) {
                filterSentMessage(message, errorCode);
                if (callback != null) {
                    callback.onError(message, errorCode);
                }
            }

            @Override
            public void onCanceled(Message message) {
                filterSentMessage(message, null);
                if (callback != null) {
                    callback.onCanceled(message);
                }
            }
        };

        RongIMClient.getInstance().sendMediaMessage(message, pushContent, pushData, sendMediaMessageCallbackWithUploader);
    }

    /**
     * 取消下载多媒体文件。
     *
     * @param message  包含多媒体文件的消息，即{@link MessageContent}为 FileMessage, ImageMessage 等。
     * @param callback 取消下载多媒体文件时的回调。
     */
    public void cancelDownloadMediaMessage(Message message, RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance().cancelDownloadMediaMessage(message, callback);
    }

    /**
     * 暂停下载多媒体文件
     *
     * @param message  包含多媒体文件的消息，即{@link MessageContent}为 FileMessage, ImageMessage 等。
     * @param callback 暂停下载多媒体文件时的回调
     */
    public void pauseDownloadMediaMessage(Message message, RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance().pauseDownloadMediaMessage(message, callback);
    }

    /**
     * 取消发送多媒体文件。
     *
     * @param message  包含多媒体文件的消息，即{@link MessageContent}为 FileMessage, ImageMessage 等。
     * @param callback 取消发送多媒体文件时的回调。
     */
    public void cancelSendMediaMessage(Message message, RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance().cancelSendMediaMessage(message, callback);
    }

    /**
     * 设置发送消息回执的会话类型。目前只支持私聊，群组和讨论组。
     * 默认支持私聊。
     *
     * @param types 包含在types里的会话类型中将会发送消息回执。
     */
    public void setReadReceiptConversationTypeList(Conversation.ConversationType... types) {
        if (RongContext.getInstance() != null) {
            RongContext.getInstance().setReadReceiptConversationTypeList(types);
        }
    }

    /**
     * <p>发送定向消息。向会话中特定的某些用户发送消息，会话中其他用户将不会收到此消息。
     * 通过 {@link io.rong.imlib.IRongCallback.ISendMessageCallback} 中的方法回调发送的消息状态及消息体。</p>
     * 此方法只能发送非多媒体消息，多媒体消息如{@link ImageMessage} {@link FileMessage} ，或者继承自{@link MediaMessageContent}的消息须调用
     * {@link #sendDirectionalMediaMessage(Message, String[], String, String, IRongCallback.ISendMediaMessageCallback)}。
     *
     * @param type        会话类型。
     * @param targetId    目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param content     消息内容，例如 {@link TextMessage}
     * @param pushContent 当下发 push 消息时，在通知栏里会显示这个字段。
     *                    如果发送的是自定义消息，该字段必须填写，否则无法收到 push 消息。
     *                    如果发送 sdk 中默认的消息类型，例如 RC:TxtMsg, RC:VcMsg, RC:ImgMsg，则不需要填写，默认已经指定。
     * @param pushData    push 附加信息。如果设置该字段，用户在收到 push 消息时，能通过 {@link io.rong.push.notification.PushNotificationMessage#getPushData()} 方法获取。
     * @param userIds     会话中将会接收到此消息的用户列表。
     * @param callback    发送消息的回调，参考 {@link io.rong.imlib.IRongCallback.ISendMessageCallback}。
     */
    public void sendDirectionalMessage(Conversation.ConversationType type, String targetId, MessageContent content, final String[] userIds, String pushContent, final String pushData, final IRongCallback.ISendMessageCallback callback) {
        Message message = Message.obtain(targetId, type, content);
        final Message filterMsg = filterSendMessage(message);
        if (filterMsg == null) {
            RLog.w(TAG, "sendDirectionalMessage: 因在 onSend 中消息被过滤为 null，取消发送。");
            return;
        }
        if (filterMsg != message) {
            message = filterMsg;
        }
        message.setContent(setMessageAttachedUserInfo(message.getContent()));
        RongIMClient.getInstance().sendDirectionalMessage(type, targetId, content, userIds, pushContent, pushData, new IRongCallback.ISendMessageCallback() {
            @Override
            public void onAttached(Message message) {
                MessageTag tag = message.getContent().getClass().getAnnotation(MessageTag.class);
                if (tag != null && (tag.flag() & MessageTag.ISPERSISTED) == MessageTag.ISPERSISTED) {
                    RongContext.getInstance().getEventBus().post(message);
                }

                if (callback != null) {
                    callback.onAttached(message);
                }
            }

            @Override
            public void onSuccess(Message message) {
                filterSentMessage(message, null);
                if (callback != null) {
                    callback.onSuccess(message);
                }
            }

            @Override
            public void onError(Message message, RongIMClient.ErrorCode errorCode) {
                filterSentMessage(message, errorCode);
                if (callback != null) {
                    callback.onError(message, errorCode);
                }
            }
        });
    }

    /**
     * 设置是否已经配置会话通知免打扰时间。
     * SDK在{@link #connect(String, RongIMClient.ConnectCallback)}之后会调用
     * {@link #getNotificationQuietHours(RongIMClient.GetNotificationQuietHoursCallback)}从服务器读取免打扰时间。
     * 开发者可以在{@link #init(Context)}之后{@link #connect(String, RongIMClient.ConnectCallback)}之前
     * 调用MessageNotificationManager.getInstance().setNotificationQuietHours(String, int)配置会话通知免打扰时间，
     * 并调用此接口告知SDK已经配置了会话通知免打扰时间
     *
     * @param notificationQuiteHoursConfigured 是否已经配置会话通知免打扰时间。
     * @deprecated 此方法废弃。
     */
    @Deprecated
    public void setNotificationQuiteHoursConfigured(boolean notificationQuiteHoursConfigured) {
        SingletonHolder.sRongIM.notificationQuiteHoursConfigured = notificationQuiteHoursConfigured;
    }

    public boolean isNotificationQuiteHoursConfigured() {
        return notificationQuiteHoursConfigured;
    }

    /**
     * 接收消息的拦截器
     */
    public interface MessageInterceptor {
        /**
         * 收到消息处理的回调
         *
         * @param message
         * @return true: 拦截, 不显示  false: 不拦截, 显示此消息。
         * 此处只处理实时收到消息时，在界面上是否显示此消息。
         * 在重新加载会话页面时，不受此处逻辑控制。
         * 若要永久不显示此消息，需要从数据库删除该消息，在回调处理中调用 {@link #deleteMessages(int[], RongIMClient.ResultCallback)},
         * 否则在重新加载会话时会将此消息重新加载出来。
         */
        boolean intercept(Message message);
    }

    /**
     * 设置接收消息时的拦截器
     *
     * @param messageInterceptor 拦截器
     */
    public void setMessageInterceptor(MessageInterceptor messageInterceptor) {
        this.messageInterceptor = messageInterceptor;
    }

    public void supportResumeBrokenTransfer(String url, final RongIMClient.ResultCallback<Boolean> callback) {
        RongIMClient.getInstance().supportResumeBrokenTransfer(url, new RongIMClient.ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean aBoolean) {
                if (callback != null) {
                    callback.onSuccess(aBoolean);
                }
            }

            @Override
            public void onError(RongIMClient.ErrorCode e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }

    /**
     * 语音消息采样率
     */
    public enum SamplingRate {
        /**
         * 8KHz
         */
        RC_SAMPLE_RATE_8000(8000),

        /**
         * 16KHz
         */
        RC_SAMPLE_RATE_16000(16000);

        SamplingRate(int sampleRate) {
            this.value = sampleRate;
        }

        private int value;

        public int getValue() {
            return this.value;
        }
    }

    private SamplingRate sampleRate = SamplingRate.RC_SAMPLE_RATE_8000;

    /**
     * 设置语音消息采样率
     *
     * @param sampleRate 消息采样率{@link SamplingRate}
     */
    public void setSamplingRate(SamplingRate sampleRate) {
        this.sampleRate = sampleRate;
    }

    /**
     * 语音消息采样率
     *
     * @return 当前设置的语音采样率
     */
    public int getSamplingRate() {
        return sampleRate.getValue();
    }

    /**
     * 语音消息类型
     */
    public enum VoiceMessageType {
        /**
         * 普通音质语音消息
         */
        Ordinary,
        /**
         * 高音质语音消息
         */
        HighQuality
    }

    private VoiceMessageType voiceMessageType = VoiceMessageType.Ordinary;

    /**
     * 语音消息类型
     *
     * @return 当前设置的语音消息类型
     */
    public VoiceMessageType getVoiceMessageType() {
        return voiceMessageType;
    }

    /**
     * 设置语音消息类型
     *
     * @param voiceMessageType 消息类型{@link VoiceMessageType}
     */
    public void setVoiceMessageType(VoiceMessageType voiceMessageType) {
        this.voiceMessageType = voiceMessageType;
    }

    public Context getApplicationContext() {
        return mApplicationContext;
    }
}
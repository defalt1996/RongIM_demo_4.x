package com.example.rongcloud_android_imdemo_import;

import android.app.Application;
import android.net.Uri;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import io.rong.imkit.RongIM;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.UserInfo;

public class RongApplication extends Application {

    private static final String TAG = "RongApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化. 建议在 Application 中进行初始化.
//        String appKey = "pkfcgjstp8g28";
        String appKey = "p5tvi9dspq3y4";
//        String appKey = "pkfcgjstp5e58";



//        String appKey = "vnroth0kvabso";
        RongIM.init(this, appKey);
        RongIM.registerMessageTemplate(new MyTextMessageItemProvider2());

        //My token
        String token = "pniGLSXhzq+IUY8F/G5rNB/kOM2lGBObZXedyq1fNWQ=@3deq.cn.rongnav.com;3deq.cn.rongcfg.com";
//        String token = "pQLLlmAYMjmIUY8F/G5rNOrZwE2JdY/VZxRYlObC4No=@3deq.cn.rongnav.com;3deq.cn.rongcfg.com";

        //other token
//        String token = "pQLLlmAYMjmIUY8F/G5rNLoI5qnBjTio5I0jLKNwb8Y=@3deq.cn.rongnav.com;3deq.cn.rongcfg.com";
//        String token = "u2fQbK2BP+wNkYrB3djcFm8Dp4OlQ32becpvzPMKF14=@53kp.cn.rongnav.com;53kp.cn.rongcfg.com";
//        String token = "ZZiGRs9vPWc6uzGDHEP3pazERLwQzw0hl/uNLRpQk9N2bEHFjEB5n6kcaovdYUFi7F2hn7EzcWcyFJxBSSjCDA==@c01g.cn.rongnav.com;c01g.cn.rongcfg.com";
//        String token = "nkXL5YD983aJkq445xT/Kwhm/t/oYx3HN373IaEka1VYpZo1nMegdwRo6tq+g1Y0UGghJmQdeVYhad9uWhIdMQ==@cicg.cn.rongnav.com;cicg.cn.rongcfg.com";

//        RongIM.connect(token, new RongIMClient.ConnectCallback() {
//            @Override
//            public void onTokenIncorrect() {
//
//            }
//
//            @Override
//            public void onSuccess(String s) {
//                SavePathUtils.setSavePath(Environment.getExternalStorageDirectory().toString()+"/Download");
//            }
//
//            @Override
//            public void onError(RongIMClient.ErrorCode errorCode) {
//
//            }
//        });

        RongIM.connect(token, new RongIMClient.ConnectCallback() {
            @Override
            public void onDatabaseOpened(RongIMClient.DatabaseOpenStatus code) {
                //消息数据库打开，可以进入到主页面
                Log.d(TAG, "onDatabaseOpened: " + code);

                // 是否缓存用户信息. true 缓存, false 不缓存
                // 1. <span style="color:red">当设置 true 后, 优先从缓存中获取用户信息.
                // 2. 更新用户信息, 需调用 RongIM.getInstance().refreshUserInfoCache(userInfo)
                boolean isCacheUserInfo = true;
                RongIM.setUserInfoProvider(new RongIM.UserInfoProvider() {

                    /**
                     * 获取设置用户信息. 通过返回的 userId 来封装生产用户信息.
                     * @param userId 用户 ID
                     */
                    @Override
                    public UserInfo getUserInfo(String userId) {
                        UserInfo userInfo = new UserInfo(userId, "名称"+userId, Uri.parse("userId 对应的头像地址"));
                        return userInfo;
                    }

                }, isCacheUserInfo);


                //连接成功后， 跳转到会话列表界面
                Map<String, Boolean> supportedConversation = new HashMap<>();
                supportedConversation.put(Conversation.ConversationType.PRIVATE.getName(), false);
                supportedConversation.put(Conversation.ConversationType.GROUP.getName(), false);
                supportedConversation.put(Conversation.ConversationType.SYSTEM.getName(), true);
                RongIM.getInstance().startConversationList(getApplicationContext(), supportedConversation);

            }

            @Override
            public void onSuccess(String s) {
                //连接成功
                Log.d(TAG, "onSuccess: " + s);

            }

            @Override
            public void onError(RongIMClient.ConnectionErrorCode errorCode) {
                Log.d(TAG, "onError: " + errorCode);

                if (errorCode.equals(RongIMClient.ConnectionErrorCode.RC_CONN_TOKEN_INCORRECT)) {
                    //从 APP 服务获取新 token，并重连
                } else {
                    //无法连接 IM 服务器，请根据相应的错误码作出对应处理
                }
            }
        });

        RongIM.getInstance().setSendMessageListener(new RongIM.OnSendMessageListener() {
            @Override
            public Message onSend(Message message) {
                Log.d(TAG, "onSend: ");
                return message;
            }

            @Override
            public boolean onSent(Message message, RongIM.SentMessageErrorCode sentMessageErrorCode) {
                Log.d(TAG, "onSent: ");
                return false;
            }
        });


        RongIM.setOnReceiveMessageListener(new RongIMClient.OnReceiveMessageListener() {
            @Override
            public boolean onReceived(Message message, int i) {
                Log.d(TAG, "onReceived: ");


                return false;
            }
        });



//        RongIM.connect(token, new RongIMClient.ConnectCallback() {
//            @Override
//            public void onSuccess(String s) {
//
//            }
//
//            @Override
//            public void onError(RongIMClient.ConnectionErrorCode connectionErrorCode) {
//
//            }
//
//            @Override
//            public void onDatabaseOpened(RongIMClient.DatabaseOpenStatus databaseOpenStatus) {
//
//            }
//        });


    }



}

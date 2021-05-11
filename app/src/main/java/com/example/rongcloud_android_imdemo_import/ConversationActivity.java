package com.example.rongcloud_android_imdemo_import;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.util.Log;

import io.rong.imkit.RongIM;
import io.rong.imkit.fragment.ConversationFragment;
import io.rong.imlib.RongIMClient;

public class ConversationActivity extends FragmentActivity {

    private static final String TAG = "ConversationActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);
        MyConversationFragment conversationFragment=new MyConversationFragment();
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.replace(R.id.container, conversationFragment);
        transaction.commit();

        RongIM.getInstance().joinChatRoom("123", 50, new RongIMClient.OperationCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "onSuccess: join chatroom successful");
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {

            }
        });

    }
}
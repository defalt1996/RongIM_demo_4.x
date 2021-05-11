package com.example.rongcloud_android_imdemo_import;

import android.content.Context;

import io.rong.imkit.fragment.ConversationFragment;

public class MyConversationFragment extends ConversationFragment {

    @Override
    public MyMessageListAdapter onResolveAdapter(Context context) {
        return new MyMessageListAdapter(context);
    }
}

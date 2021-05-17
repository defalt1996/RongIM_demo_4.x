package io.rong.imkit.fragment;

import android.os.Message;
import android.text.TextUtils;
import android.view.View;

import io.rong.common.RLog;
import io.rong.imkit.R;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.model.Event;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;

/**
 * Created by zhjchen on 3/20/15.
 */
public class SetConversationNotificationFragment extends BaseSettingFragment {
    private final static String TAG = "SetConversationNotificationFragment";
    public static SetConversationNotificationFragment newInstance() {
        return new SetConversationNotificationFragment();
    }


    @Override
    protected void initData() {

        if (RongContext.getInstance() != null)
            RongContext.getInstance().getEventBus().register(this);

        RongIM.getInstance().getConversationNotificationStatus(getConversationType(), getTargetId(), new RongIMClient.ResultCallback<Conversation.ConversationNotificationStatus>() {

            @Override
            public void onSuccess(final Conversation.ConversationNotificationStatus notificationStatus) {

                if (notificationStatus != null) {
                    setSwitchBtnStatus(notificationStatus != Conversation.ConversationNotificationStatus.DO_NOT_DISTURB);
                }
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {

                setSwitchBtnStatus(!getSwitchBtnStatus());
            }
        });
    }


    @Override
    protected boolean setSwitchButtonEnabled() {
        return true;
    }

    @Override
    protected String setTitle() {
        return getString(R.string.rc_setting_conversation_notify);
    }

    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }

    @Override
    protected void onSettingItemClick(View v) {
        RLog.i(TAG, "onSettingItemClick, " + v.toString());
    }


    @Override
    protected int setSwitchBtnVisibility() {
        return View.VISIBLE;
    }

    @Override
    protected void toggleSwitch(boolean toggle) {

        Conversation.ConversationNotificationStatus status;

        if (toggle) {
            status = Conversation.ConversationNotificationStatus.NOTIFY;
        } else {
            status = Conversation.ConversationNotificationStatus.DO_NOT_DISTURB;
        }

        if (getConversationType() != null && !TextUtils.isEmpty(getTargetId())) {
            RongIM.getInstance().setConversationNotificationStatus(getConversationType(), getTargetId(), status, new RongIMClient.ResultCallback<Conversation.ConversationNotificationStatus>() {

                @Override
                public void onSuccess(Conversation.ConversationNotificationStatus status) {
                    RLog.i(TAG, "SetConversationNotificationFragment onSuccess--");
                }

                @Override
                public void onError(RongIMClient.ErrorCode errorCode) {
                    setSwitchBtnStatus(!getSwitchBtnStatus());
                    //Toast.makeText(getActivity(), getString(R.string.rc_setting_conversation_notify_fail), Toast.LENGTH_SHORT).show();
                }
            });

        } else {
            RLog.e(TAG, "SetConversationNotificationFragment Arguments is null");
        }

    }

    public void onEventMainThread(Event.ConversationNotificationEvent event) {
        if (event != null && event.getTargetId().equals(getTargetId()) && event.getConversationType().getValue() == getConversationType().getValue()) {
            setSwitchBtnStatus(event.getStatus() == Conversation.ConversationNotificationStatus.NOTIFY);
        }
    }

    @Override
    public void onDestroy() {

        if (RongContext.getInstance() != null)
            RongContext.getInstance().getEventBus().unregister(this);

        super.onDestroy();

    }
}

package io.rong.imkit.widget.provider;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import io.rong.common.RLog;
import io.rong.eventbus.EventBus;
import io.rong.imkit.R;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.model.Event;
import io.rong.imkit.model.ProviderTag;
import io.rong.imkit.model.UIMessage;
import io.rong.imkit.recallEdit.RecallEditCountDownCallBack;
import io.rong.imkit.recallEdit.RecallEditManager;
import io.rong.imkit.userInfoCache.RongUserInfoManager;
import io.rong.imlib.model.UserInfo;
import io.rong.message.RecallNotificationMessage;

@ProviderTag(messageContent = RecallNotificationMessage.class, showPortrait = false, showProgress = false, showWarning = false, centerInHorizontal = true,
        showSummaryWithName = false)
public class RecallMessageItemProvider extends IContainerItemProvider.MessageProvider<RecallNotificationMessage> {
    public static final String TAG = RecallMessageItemProvider.class.getSimpleName();

    @Override
    public void onItemClick(View view, int position, RecallNotificationMessage content, UIMessage message) {

    }

    @Override
    public void bindView(View v, int position, final RecallNotificationMessage content, final UIMessage message) {
        Object tag = v.getTag();
        if (tag instanceof ViewHolder && content != null) {
            final ViewHolder viewHolder = (ViewHolder) tag;
            viewHolder.contentTextView.setText(getInformation(content));
            long validTime = v.getContext().getResources().getInteger(R.integer.rc_message_recall_edit_interval);
            long countDownTime = System.currentTimeMillis() - content.getRecallActionTime();
            // 判断被复用了，取消上一个 item 的倒计时
            if (!TextUtils.isEmpty(viewHolder.messageId)) {
                RecallEditManager.getInstance().cancelCountDown(viewHolder.messageId);
            }
            viewHolder.messageId = String.valueOf(message.getMessageId());
            if (content.getRecallActionTime() > 0 && countDownTime < validTime * 1000) {
                viewHolder.editTextView.setVisibility(View.VISIBLE);
                RecallEditManager.getInstance().startCountDown(message.getMessage(), validTime * 1000 - countDownTime, new RecallEditCountDownListener(viewHolder));
                viewHolder.editTextView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        EventBus.getDefault().post(new Event.RecallMessageEditClickEvent(message.getMessage()));
                    }
                });
            } else {
                viewHolder.editTextView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onItemLongClick(View view, int position, RecallNotificationMessage content, UIMessage message) {

    }

    @Override
    public Spannable getContentSummary(RecallNotificationMessage data) {
        return null;
    }

    @Override
    public Spannable getContentSummary(Context context, RecallNotificationMessage data) {
        if (data != null) {
            return new SpannableString(getInformation(data));
        }
        return null;
    }

    @Override
    public View newView(Context context, ViewGroup group) {
        View view = LayoutInflater.from(context).inflate(R.layout.rc_item_information_notification_message, null);
        ViewHolder viewHolder = new ViewHolder();
        viewHolder.contentTextView = view.findViewById(R.id.rc_msg);
        viewHolder.contentTextView.setMovementMethod(LinkMovementMethod.getInstance());
        viewHolder.editTextView = (TextView) view.findViewById(R.id.rc_edit);
        view.setTag(viewHolder);

        return view;
    }

    private static class ViewHolder {
        TextView contentTextView;
        TextView editTextView;
        String messageId;
    }

    private String getInformation(RecallNotificationMessage content) {
        String information;
        String operatorId = content.getOperatorId();

        if (TextUtils.isEmpty(operatorId)) {
            RLog.e(TAG, "RecallMessageItemProvider bindView - operatorId is empty");
            information = RongIM.getInstance().getApplicationContext().getString(R.string.rc_recalled_a_message);
        } else if (content.isAdmin()) {
            information = RongIM.getInstance().getApplicationContext().getString(R.string.rc_admin_recalled_message);
        } else if (operatorId.equals(RongIM.getInstance().getCurrentUserId())) {
            information = RongIM.getInstance().getApplicationContext().getString(R.string.rc_you_recalled_a_message);
        } else {
            UserInfo userInfo = RongUserInfoManager.getInstance().getUserInfo(operatorId);
            if (userInfo != null && userInfo.getName() != null) {
                information = userInfo.getName() + RongIM.getInstance().getApplicationContext().getString(R.string.rc_recalled_a_message);
            } else {
                information = operatorId + RongIM.getInstance().getApplicationContext().getString(R.string.rc_recalled_a_message);
            }
        }

        return information;
    }

    private static class RecallEditCountDownListener implements RecallEditCountDownCallBack {
        private WeakReference<RecallMessageItemProvider.ViewHolder> mHolder;

        public RecallEditCountDownListener(ViewHolder holder) {
            mHolder = new WeakReference<>(holder);
        }

        @Override
        public void onFinish(String messageId) {
            RecallMessageItemProvider.ViewHolder viewHolder = mHolder.get();
            if (viewHolder != null && messageId.equals(viewHolder.messageId)) {
                viewHolder.editTextView.setVisibility(View.GONE);
            }
        }
    }
}

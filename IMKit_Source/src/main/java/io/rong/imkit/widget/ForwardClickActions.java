package io.rong.imkit.widget;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import androidx.fragment.app.Fragment;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import io.rong.common.RLog;
import io.rong.imkit.actions.IClickActions;
import io.rong.imkit.R;
import io.rong.imkit.fragment.ConversationFragment;
import io.rong.imkit.utils.ForwardManager;
import io.rong.imlib.model.Message;

public class ForwardClickActions implements IClickActions {
    private static final String TAG = ForwardClickActions.class.getSimpleName();
    private BottomMenuDialog dialog;

    @Override
    public Drawable obtainDrawable(Context context) {
        return context.getResources().getDrawable(R.drawable.rc_selector_multi_forward);
    }

    @Override
    public void onClick(final Fragment fragment) {
        final Activity activity = fragment.getActivity();

        if (activity == null || activity.isFinishing()) {
            RLog.e(TAG, "onClick activity is null or finishing.");
            return;
        }

        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        dialog = new BottomMenuDialog(activity);
        dialog.setConfirmListener(new View.OnClickListener() {
            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onClick(View arg0) {
                if (dialog != null) {
                    dialog.dismiss();
                }
                startSelectConversationActivity(fragment, 0);
            }
        });
        dialog.setMiddleListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (dialog != null) {
                    dialog.dismiss();
                }
                startSelectConversationActivity(fragment, 1);
            }
        });
        dialog.setCancelListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });
        dialog.show();
    }

    // index: 0:逐步转发  1:合并转发
    private void startSelectConversationActivity(Fragment pFragment, int index) {
        final ConversationFragment fragment = (ConversationFragment) pFragment;
        List<Message> messages = ForwardManager.filterMessagesList(
                fragment.getContext(), fragment.getCheckedMessages(), index);
        if (messages.size() == 0) {
            RLog.e(TAG, "startSelectConversationActivity the size of messages is 0!");
            return;
        }
        ArrayList<Integer> messageIds = new ArrayList<>();
        for (Message msg : messages) {
            messageIds.add(msg.getMessageId());
        }

        Intent intent = fragment.getSelectIntentForForward();
        intent.putExtra("index", index);
        intent.putIntegerArrayListExtra("messageIds", messageIds);
        fragment.startActivityForResult(intent, ConversationFragment.REQUEST_CODE_FORWARD);
    }
}

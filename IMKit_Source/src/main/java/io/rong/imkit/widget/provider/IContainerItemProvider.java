package io.rong.imkit.widget.provider;

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Parcelable;
import android.text.Spannable;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.rong.imkit.R;
import io.rong.imkit.RongMessageItemLongClickActionManager;
import io.rong.imkit.model.UIMessage;
import io.rong.imkit.userInfoCache.RongUserInfoManager;
import io.rong.imkit.utilities.OptionsPopupDialog;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.UserInfo;

public interface IContainerItemProvider<T> {

    /**
     * 创建新View。
     *
     * @param context 当前上下文。
     * @param group   创建的新View所附属的父View。
     * @return 需要创建的新View。
     */
    View newView(Context context, ViewGroup group);

    /**
     * 为View绑定数据。
     *
     * @param v        需要绑定数据的View。
     * @param position 绑定的数据位置。
     * @param data     绑定的数据。
     */
    void bindView(View v, int position, T data);

    /**
     * 消息内容适配器。
     */
    abstract class MessageProvider<K extends MessageContent> implements
            IContainerItemProvider<UIMessage>,
            Cloneable {

        /**
         * 为View绑定数据。
         *
         * @param v        需要绑定数据的View。
         * @param position 绑定的数据位置。
         * @param data     绑定的消息。
         */
        @Override
        public final void bindView(View v, int position, UIMessage data) {
            bindView(v, position, (K) data.getContent(), data);
        }

        /**
         * 为View绑定数据。
         *
         * @param v        需要绑定数据的View。
         * @param position 绑定的数据位置。
         * @param content  绑定的消息内容。
         * @param message  绑定的消息。
         */
        public abstract void bindView(View v, int position, K content, UIMessage message);

        /**
         * 当前数据的简单描述。
         *
         * @param data 当前需要绑定的数据
         * @return 数据的描述。
         */
        public Spannable getContentSummary(Context context, K data) {
            return getContentSummary(data);
        }

        /**
         * 当前数据的简单描述。
         *
         * @param data 当前需要绑定的数据
         * @return 数据的描述。
         * 若要通过此方法处理数据，须在 {@link #getContentSummary(Context context, K data)} 中 return null，否则此方法不回调。
         */
        public Spannable getSummary(UIMessage data) {
            return getContentSummary((K) data.getContent());
        }

        /**
         * 当前数据的简单描述。
         *
         * @param data 当前需要绑定的数据
         * @return 数据的描述。
         * @deprecated
         */
        public abstract Spannable getContentSummary(K data);

        /**
         * View的点击事件。
         *
         * @param view     所点击的View。
         * @param position 点击的位置。
         * @param content  点击的消息内容。
         * @param message  点击的消息。
         */
        public abstract void onItemClick(View view, int position, K content, UIMessage message);

        /**
         * View的长按事件。
         *
         * @param view     所长按的View。
         * @param position 长按的位置。
         * @param content  长按的消息内容。
         * @param message  长按的消息。
         */
        public void onItemLongClick(final View view, final int position, final K content, final UIMessage message) {
            final List<MessageItemLongClickAction> messageItemLongClickActions = RongMessageItemLongClickActionManager.getInstance().getMessageItemLongClickActions(message);

            Collections.sort(messageItemLongClickActions, new Comparator<MessageItemLongClickAction>() {
                @Override
                public int compare(MessageItemLongClickAction lhs, MessageItemLongClickAction rhs) {
                    // desc sort
                    return rhs.priority - lhs.priority;
                }
            });
            List<String> titles = new ArrayList<>();
            for (MessageItemLongClickAction action : messageItemLongClickActions) {
                titles.add(action.getTitle(view.getContext()));
            }

            int size = titles.size();
            OptionsPopupDialog dialog = OptionsPopupDialog.newInstance(view.getContext(), titles.toArray(new String[size]))
                    .setOptionsPopupDialogListener(new OptionsPopupDialog.OnOptionsItemClickedListener() {
                        @Override
                        public void onOptionsItemClicked(int which) {
                            if (!messageItemLongClickActions.get(which).listener.onMessageItemLongClick(view.getContext(), message)) {
                                onItemLongClickAction(view, position, message);
                            }
                        }
                    });
            RongMessageItemLongClickActionManager.getInstance().setLongClickDialog(dialog);
            RongMessageItemLongClickActionManager.getInstance().setLongClickMessage(message.getMessage());
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    RongMessageItemLongClickActionManager.getInstance().setLongClickDialog(null);
                    RongMessageItemLongClickActionManager.getInstance().setLongClickMessage(null);
                }
            });
            dialog.show();
        }

        /**
         * 当需要处理view上下文相关的item长按弹出菜单时，可在{@link  io.rong.imkit.widget.provider.MessageItemLongClickAction.MessageItemLongClickListener#MessageItemLongClickAction}中不做任何处理,
         * 直接返回false
         * 比如：微信的长按--更多
         *
         * @param view     View
         * @param position 位置
         * @param message  UIMessage
         */
        public void onItemLongClickAction(View view, int position, UIMessage message) {

        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        /**
         * 消息被撤回是，通知栏显示的信息
         *
         * @param context 上下文
         * @param message 消息
         * @return 通知栏显示的信息
         */
        public String getPushContent(Context context, UIMessage message) {
            String userName = "";
            UserInfo userInfo = RongUserInfoManager.getInstance().getUserInfo(message.getSenderUserId());
            if (userInfo != null) {
                userName = userInfo.getName();
            }
            return context.getString(R.string.rc_user_recalled_message, userName);
        }
    }

    /**
     * 会话适配器。
     */
    interface ConversationProvider<T extends Parcelable> extends IContainerItemProvider<T> {
        /**
         * 绑定标题内容。
         *
         * @param id 需要绑定标题的Id。
         * @return 绑定标题内容。
         */
        String getTitle(String id);

        /**
         * 绑定头像Uri。
         *
         * @param id 需要显示头像的Id。
         * @return 当前头像Uri。
         */
        Uri getPortraitUri(String id);
    }
}

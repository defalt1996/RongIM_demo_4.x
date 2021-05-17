package io.rong.imkit.fragment;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.rong.common.RLog;
import io.rong.eventbus.EventBus;
import io.rong.imkit.R;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.manager.InternalModuleManager;
import io.rong.imkit.model.Event;
import io.rong.imkit.model.GroupUserInfo;
import io.rong.imkit.model.UIConversation;
import io.rong.imkit.utilities.OptionsPopupDialog;
import io.rong.imkit.voiceMessageDownload.AutoDownloadEntry;
import io.rong.imkit.voiceMessageDownload.HQVoiceMsgDownloadManager;
import io.rong.imkit.widget.RongSwipeRefreshLayout;
import io.rong.imkit.widget.adapter.ConversationListAdapter;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Conversation.ConversationType;
import io.rong.imlib.model.ConversationStatus;
import io.rong.imlib.model.Discussion;
import io.rong.imlib.model.Group;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.PublicServiceProfile;
import io.rong.imlib.model.UserInfo;
import io.rong.message.ReadReceiptMessage;
import io.rong.push.RongPushClient;

public class ConversationListFragment extends UriFragment implements
        AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener,
        ConversationListAdapter.OnPortraitItemClick, RongSwipeRefreshLayout.OnLoadListener, RongSwipeRefreshLayout.OnFlushListener {

    public static final String TAG = ConversationListFragment.class.getSimpleName();

    private List<ConversationConfig> mConversationsConfig;
    private ConversationListFragment mThis;

    private ConversationListAdapter mAdapter;
    private ListView mList;
    private RongSwipeRefreshLayout mRefreshLayout;
    private View netWorkBar;
    private View headerNetWorkView;
    private ImageView headerNetWorkImage;
    private TextView headerNetWorkText;
    private boolean isShowWithoutConnected = false;
    private int leftOfflineMsg = 0;
    private boolean enableAutomaticDownloadMsg;

    private long timestamp = 0;
    private int pageSize = 100;
    private static final int REQUEST_MSG_DOWNLOAD_PERMISSION = 1001;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mThis = this;
        mConversationsConfig = new ArrayList<>();
        EventBus.getDefault().register(this);
        InternalModuleManager.getInstance().onLoaded();
    }

    @Override
    protected void initFragment(Uri uri) {
        RLog.d(TAG, "initFragment " + uri);
        if (mConversationsConfig == null) {
            mConversationsConfig = new ArrayList<>();
        }
        Conversation.ConversationType[] defConversationType = {
                Conversation.ConversationType.PRIVATE,
                Conversation.ConversationType.GROUP,
                Conversation.ConversationType.DISCUSSION,
                Conversation.ConversationType.SYSTEM,
                Conversation.ConversationType.CUSTOMER_SERVICE,
                Conversation.ConversationType.CHATROOM,
                Conversation.ConversationType.PUBLIC_SERVICE,
                Conversation.ConversationType.APP_PUBLIC_SERVICE,
                ConversationType.ENCRYPTED
        };

        timestamp = 0;
        isShowWithoutConnected = false;
        leftOfflineMsg = 0;
        mConversationsConfig.clear();
        //ConversationListFragment config
        for (Conversation.ConversationType conversationType : defConversationType) {
            if (uri.getQueryParameter(conversationType.getName()) != null) {
                ConversationConfig config = new ConversationConfig();
                config.conversationType = conversationType;
                config.isGathered = ("true").equals(uri.getQueryParameter(conversationType.getName()));
                mConversationsConfig.add(config);
            }
        }

        //SubConversationListFragment config
        if (mConversationsConfig.size() == 0) {
            String type = uri.getQueryParameter("type");
            for (Conversation.ConversationType conversationType : defConversationType) {
                if (conversationType.getName().equals(type)) {
                    ConversationConfig config = new ConversationConfig();
                    config.conversationType = conversationType;
                    config.isGathered = false;
                    mConversationsConfig.add(config);
                    break;
                }
            }
        }

        mAdapter.clear();

        if (RongIMClient.getInstance().getCurrentConnectionStatus().equals(RongIMClient.ConnectionStatusListener.ConnectionStatus.UNCONNECTED)) {
            RLog.d(TAG, "RongCloud haven't been connected yet, so the conversation list display blank !!!");
            isShowWithoutConnected = true;
            return;
        }
        getConversationList(getConfigConversationTypes(), false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.rc_fr_conversationlist, container, false);
        View emptyView = findViewById(view, R.id.rc_conversation_list_empty_layout);
        TextView emptyText = findViewById(view, R.id.rc_empty_tv);
        if (getActivity() != null) {
            emptyText.setText(getActivity().getResources().getString(R.string.rc_conversation_list_empty_prompt));
        }

        mList = findViewById(view, R.id.rc_list);
        mRefreshLayout = findViewById(view, R.id.rc_refresh);
        mList.setEmptyView(emptyView);
        inflateHeaderView();
        mList.setOnItemClickListener(this);
        mList.setOnItemLongClickListener(this);

        if (mAdapter == null) {
            mAdapter = onResolveAdapter(getActivity());
        }
        mAdapter.setOnPortraitItemClick(this);
        mRefreshLayout.setCanRefresh(false);
        mRefreshLayout.setCanLoading(true);
        mRefreshLayout.setOnLoadListener(this);
        mRefreshLayout.setOnFlushListener(this);
        mList.setAdapter(mAdapter);

        if (getContext() != null) {
            Resources resources = getContext().getResources();
            enableAutomaticDownloadMsg = resources.getBoolean(R.bool.rc_enable_automatic_download_voice_msg);
        }
        headerNetWorkView = findViewById(view, R.id.rc_status_bar);
        headerNetWorkImage = findViewById(view, R.id.rc_status_bar_image);
        headerNetWorkText = findViewById(view, R.id.rc_status_bar_text);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        RLog.d(TAG, "onResume " + RongIM.getInstance().getCurrentConnectionStatus());
        if (getResources().getBoolean(R.bool.rc_wipe_out_notification_message)) {
            RongPushClient.clearAllNotifications(getActivity());
        }
        setNotificationBarVisibility(RongIM.getInstance().getCurrentConnectionStatus());
    }

    private void inflateHeaderView() {
        List<View> headerViews = onAddHeaderView();
        if (headerViews != null && headerViews.size() > 0) {
            for (View headerView : headerViews) {
                mList.addHeaderView(headerView);
            }
        }
    }

    private void getConversationList(Conversation.ConversationType[] conversationTypes, final boolean isLoadMore) {
        getConversationList(conversationTypes, new IHistoryDataResultCallback<List<Conversation>>() {
            @Override
            public void onResult(List<Conversation> data) {
                if (data != null && data.size() > 0) {
                    makeUiConversationList(data);
                    RLog.d(TAG, "getConversationList : listSize = " + data.size());
                    mAdapter.notifyDataSetChanged();
                    onUnreadCountChanged();
                    updateConversationReadReceipt(cacheEventList);
                } else {
                    isShowWithoutConnected = true;
                }
                onFinishLoadConversationList(leftOfflineMsg);
                if (!isLoadMore) {
                    mRefreshLayout.setRefreshing(false);
                    return;
                }
                if (data == null) {
                    mRefreshLayout.setLoadMoreFinish(false);
                } else if (data.size() > 0 && data.size() <= pageSize) {
                    mRefreshLayout.setLoadMoreFinish(false);
                } else if (data.size() == 0) {
                    mRefreshLayout.setLoadMoreFinish(false);
                    mRefreshLayout.setCanLoading(false);
                } else {
                    mRefreshLayout.setLoadMoreFinish(false);
                }
            }

            @Override
            public void onError() {
                RLog.e(TAG, "getConversationList Error");
                onFinishLoadConversationList(leftOfflineMsg);
                isShowWithoutConnected = true;
                mRefreshLayout.setLoadMoreFinish(false);
            }
        }, isLoadMore);
    }

    /**
     * 开发者可以重写此方法，来填充自定义数据到会话列表界面。
     * 如果需要同时显示 sdk 中默认会话列表数据，在重写时可使用 super.getConversationList()。反之，不需要调用 super 方法。
     * <p>
     * 注意：通过 callback 返回的数据要保证在 UI 线程返回。
     *
     * @param conversationTypes 当前会话列表已配置显示的会话类型。
     */
    public void getConversationList(ConversationType[] conversationTypes, final IHistoryDataResultCallback<List<Conversation>> callback, boolean isLoadMore) {
        long lTimestamp = isLoadMore ? timestamp : 0;
        RongIMClient.getInstance().getConversationListByPage(new RongIMClient.ResultCallback<List<Conversation>>() {
            @Override
            public void onSuccess(List<Conversation> conversations) {
                if (getActivity() == null || getActivity().isFinishing()) {
                    return;
                }
                if (callback != null) {
                    List<Conversation> resultConversations = new ArrayList<>();
                    if (conversations != null) {
                        timestamp = conversations.get(conversations.size() - 1).getSentTime();
                        for (Conversation conversation : conversations) {
                            if (!shouldFilterConversation(conversation.getConversationType(), conversation.getTargetId())) {
                                resultConversations.add(conversation);
                            }

                        }
                    }
                    callback.onResult(resultConversations);
                }
            }

            @Override
            public void onError(RongIMClient.ErrorCode e) {
                if (callback != null) {
                    callback.onError();
                }
            }

        }, lTimestamp, pageSize, conversationTypes);
    }

    /**
     * 定位会话列表中的某一条未读会话。
     * 如果有多条未读会话,每调用一次此接口,就会从上往下逐个未读会话定位。
     */
    public void focusUnreadItem() {
        if (mList == null || mAdapter == null) {
            return;
        }
        int first = mList.getFirstVisiblePosition();
        int last = mList.getLastVisiblePosition();
        int count = mAdapter.getCount();
        first = Math.max(0, first - mList.getHeaderViewsCount());
        last -= mList.getHeaderViewsCount();
        last = Math.min(count - 1, last);
        int visibleCount = last - first + 1;
        if (visibleCount < count) {
            int index;
            if (last < count - 1) {
                index = first + 1;
            } else {
                index = 0;
            }

            if (!selectNextUnReadItem(index, count)) {
                selectNextUnReadItem(0, count);
            }
        }
    }

    private boolean selectNextUnReadItem(int startIndex, int totalCount) {
        int index = -1;
        for (int i = startIndex; i < totalCount; i++) {
            UIConversation uiConversation = mAdapter.getItem(i);
            if (uiConversation == null || uiConversation.getUnReadMessageCount() > 0) {
                index = i;
                break;
            }
        }
        if (index >= 0 && index < totalCount) {
            mList.setSelection(index + mList.getHeaderViewsCount());
            return true;
        }
        return false;
    }

    private void setNotificationBarVisibility(RongIMClient.ConnectionStatusListener.ConnectionStatus status) {
        if (!getResources().getBoolean(R.bool.rc_is_show_warning_notification)) {
            RLog.e(TAG, "rc_is_show_warning_notification is disabled.");
            return;
        }

        String content = null;
        if (status.equals(RongIMClient.ConnectionStatusListener.ConnectionStatus.NETWORK_UNAVAILABLE)) {
            content = getResources().getString(R.string.rc_notice_network_unavailable);
        } else if (status.equals(RongIMClient.ConnectionStatusListener.ConnectionStatus.KICKED_OFFLINE_BY_OTHER_CLIENT)) {
            content = getResources().getString(R.string.rc_notice_tick);
        } else if (status.equals(RongIMClient.ConnectionStatusListener.ConnectionStatus.CONNECTED)) {
            headerNetWorkView.setVisibility(View.GONE);
        } else if (status.equals(RongIMClient.ConnectionStatusListener.ConnectionStatus.UNCONNECTED)) {
            content = getResources().getString(R.string.rc_notice_disconnect);
        } else if (status.equals(RongIMClient.ConnectionStatusListener.ConnectionStatus.CONNECTING)
                || status.equals(RongIMClient.ConnectionStatusListener.ConnectionStatus.SUSPEND)) {
            content = getResources().getString(R.string.rc_notice_connecting);
        }
        if (content != null && headerNetWorkView != null) {
            if (headerNetWorkView.getVisibility() == View.GONE) {
                final String text = content;
                getHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!RongIMClient.getInstance().getCurrentConnectionStatus().equals(RongIMClient.ConnectionStatusListener.ConnectionStatus.CONNECTED)) {
                            headerNetWorkView.setVisibility(View.VISIBLE);
                            headerNetWorkText.setText(text);
                            if (RongIMClient.getInstance().getCurrentConnectionStatus().equals(RongIMClient.ConnectionStatusListener.ConnectionStatus.CONNECTING)
                                    || RongIMClient.getInstance().getCurrentConnectionStatus().equals(RongIMClient.ConnectionStatusListener.ConnectionStatus.SUSPEND)) {
                                headerNetWorkImage.setImageResource(R.drawable.rc_notification_connecting_animated);
                            } else {
                                headerNetWorkImage.setImageResource(R.drawable.rc_notification_network_available);
                            }
                        }
                    }
                }, 4000);
            } else {
                headerNetWorkText.setText(content);
                if (RongIMClient.getInstance().getCurrentConnectionStatus().equals(RongIMClient.ConnectionStatusListener.ConnectionStatus.CONNECTING)
                        || RongIMClient.getInstance().getCurrentConnectionStatus().equals(RongIMClient.ConnectionStatusListener.ConnectionStatus.SUSPEND)) {
                    headerNetWorkImage.setImageResource(R.drawable.rc_notification_connecting_animated);
                } else {
                    headerNetWorkImage.setImageResource(R.drawable.rc_notification_network_available);
                }
            }
        }
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    /**
     * 会话列表添加头部
     *
     * @return 头部view
     */
    protected List<View> onAddHeaderView() {
        List<View> headerViews = new ArrayList<>();
        return headerViews;
    }

    /**
     * 设置 ListView 的 Adapter 适配器。
     *
     * @param adapter 适配器
     * @deprecated 此方法已经废弃，可以使用 {@link #onResolveAdapter(Context)} 代替
     */
    @Deprecated
    public void setAdapter(ConversationListAdapter adapter) {
        mAdapter = adapter;
        if (mList != null) {
            mList.setAdapter(adapter);
        }
    }

    /**
     * 提供 ListView 的 Adapter 适配器。
     * 使用时，需要继承 {@link ConversationListFragment} 并重写此方法。
     * 注意：提供的适配器，要继承自 {@link ConversationListAdapter}
     *
     * @return 适配器
     */
    public ConversationListAdapter onResolveAdapter(Context context) {
        mAdapter = new ConversationListAdapter(context);
        return mAdapter;
    }

    public void onEventMainThread(Event.SyncReadStatusEvent event) {
        ConversationType conversationType = event.getConversationType();
        String targetId = event.getTargetId();
        RLog.d(TAG, "SyncReadStatusEvent " + conversationType + " " + targetId);

        int position;
        if (getGatherState(conversationType)) {
            position = mAdapter.findGatheredItem(conversationType);
        } else {
            position = mAdapter.findPosition(conversationType, targetId);
        }
        if (position >= 0) {
            UIConversation uiConversation = mAdapter.getItem(position);
            uiConversation.clearUnRead(conversationType, targetId);
            mAdapter.notifyDataSetChanged();
        }
        onUnreadCountChanged();
    }

    /**
     * 仅处理非聚合状态，显示已读回执标志。
     *
     * @param event 已读回执事件
     */
    public void onEventMainThread(final Event.ReadReceiptEvent event) {
        ConversationType conversationType = event.getMessage().getConversationType();
        String targetId = event.getMessage().getTargetId();
        int originalIndex = mAdapter.findPosition(conversationType, targetId);
        boolean gatherState = getGatherState(conversationType);
        RLog.d(TAG, "ReadReceiptEvent. targetId:" + event.getMessage().getTargetId() + ";originalIndex:" + originalIndex);
        if (!gatherState) {
            if (originalIndex >= 0) {
                UIConversation conversation = mAdapter.getItem(originalIndex);
                ReadReceiptMessage content = (ReadReceiptMessage) event.getMessage().getContent();
                if (content.getLastMessageSendTime() >= conversation.getSyncReadReceiptTime()
                        && conversation.getConversationSenderId().equals(RongIMClient.getInstance().getCurrentUserId())) {
                    conversation.setSentStatus(Message.SentStatus.READ);
                    conversation.setSyncReadReceiptTime(event.getMessage().getSentTime());
                    mAdapter.getView(originalIndex, mList.getChildAt(originalIndex - mList.getFirstVisiblePosition() + mList.getHeaderViewsCount()), mList);
                    return;
                }
            }
            cacheEventList.add(event.getMessage());
        }
    }

    private ArrayList<Message> cacheEventList = new ArrayList<>();

    /**
     * cacheEventList 以及 updateConversationReadReceipt 为了解决 ReadReceiptMessage 消息下发的时机大于 adapter 的 UI 更新时机而
     * 设置的缓存机制
     */
    private void updateConversationReadReceipt(ArrayList<Message> cacheEventList) {
        Iterator<Message> iterator = cacheEventList.iterator();
        while (iterator.hasNext()) {
            Message message = iterator.next();
            ConversationType conversationType = message.getConversationType();
            String targetId = message.getTargetId();
            int originalIndex = mAdapter.findPosition(conversationType, targetId);
            boolean gatherState = getGatherState(conversationType);
            if (!gatherState && originalIndex >= 0) {
                UIConversation conversation = mAdapter.getItem(originalIndex);
                ReadReceiptMessage content = (ReadReceiptMessage) message.getContent();
                if (content.getLastMessageSendTime() >= conversation.getSyncReadReceiptTime()
                        && conversation.getConversationSenderId().equals(RongIMClient.getInstance().getCurrentUserId())) {
                    conversation.setSentStatus(Message.SentStatus.READ);
                    conversation.setSyncReadReceiptTime(content.getLastMessageSendTime());
                    mAdapter.getView(originalIndex, mList.getChildAt(originalIndex - mList.getFirstVisiblePosition() + mList.getHeaderViewsCount()), mList);
                    iterator.remove();
                } else if (content.getLastMessageSendTime() < conversation.getUIConversationTime()) {
                    RLog.d(TAG, "remove cache event. id:" + message.getTargetId());
                    iterator.remove();
                }
            }
        }
    }

    public void onEventMainThread(Event.AudioListenedEvent event) {
        Message message = event.getMessage();
        ConversationType conversationType = message.getConversationType();
        String targetId = message.getTargetId();
        RLog.d(TAG, "Message: " + message.getObjectName() + " " + conversationType + " " + message.getSentStatus());

        if (isConfigured(conversationType)) {
            boolean gathered = getGatherState(conversationType);
            int position = gathered ? mAdapter.findGatheredItem(conversationType) : mAdapter.findPosition(conversationType, targetId);
            if (position >= 0) {
                UIConversation uiConversation = mAdapter.getItem(position);
                if (message.getMessageId() == uiConversation.getLatestMessageId()) {
                    uiConversation.updateConversation(getContext(), message, gathered);
                    mAdapter.getView(position, mList.getChildAt(position - mList.getFirstVisiblePosition() + mList.getHeaderViewsCount()), mList);
                }
            }
        }
    }

    /**
     * 接收到消息，先调用此方法，检查是否可以更新消息对应的会话。
     * 如果可以更新，则返回 true，否则返回 false。
     * 注意：开发者可以重写此方法，来控制是否更新对应的会话。
     *
     * @param message 接收到的消息体。
     * @param left    剩余的消息数量。
     * @return 根据返回值确定是否更新对应会话信息。
     */
    public boolean shouldUpdateConversation(Message message, int left) {
        return true;
    }

    /**
     * 会话列表界面是否过滤某一会话，过滤之后，用户往该会话发送消息，也不会在会话列表展示
     *
     * @param type     会话类型
     * @param targetId 会话的目标Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @return
     */
    public boolean shouldFilterConversation(ConversationType type, String targetId) {
        return false;
    }

    /**
     * 会话列表未读数发生变化时回调此方法，告诉未读数变化的时机
     * 可以重写此方法，当未读数发生变化时，通过 adapter 中的 item 数据计算未读数量
     */
    public void onUnreadCountChanged() {

    }

    /**
     * 当会话列表加载完成时回调此方法，告诉加载完成的时机
     *
     * @param leftOfflineMsg 剩余离线消息数量
     */
    public void onFinishLoadConversationList(int leftOfflineMsg) {

    }

    /**
     * UIConversation 被创建成功，adapter 会根据 UIConversation 中的数据展示在 UI 上
     *
     * @param uiConversation
     */
    public void onUIConversationCreated(UIConversation uiConversation) {

    }

    /**
     * 根据指定的 uiconversation 刷新 list view 中 item
     *
     * @param uiConversation 指定的 uiconversation
     */
    public void updateListItem(UIConversation uiConversation) {
        int position = mAdapter.findPosition(uiConversation.getConversationType(), uiConversation.getConversationTargetId());
        if (position >= 0) {
            mAdapter.getView(position, mList.getChildAt(position - mList.getFirstVisiblePosition() + mList.getHeaderViewsCount()), mList);
        }
    }

    public void onEventMainThread(final Event.OnReceiveMessageEvent event) {
        if (event.isOffline()) {
            if (event.getLeft() == 0) {
                getConversationList(getConfigConversationTypes(), false);
            }
            return;
        }
        leftOfflineMsg = event.getLeft();
        Message message = event.getMessage();
        String targetId = message.getTargetId();
        ConversationType conversationType = message.getConversationType();

        if (shouldFilterConversation(conversationType, targetId)) {
            return;
        }
        if (event.getLeft() == 0 && !event.hasPackage() && enableAutomaticDownloadMsg) {
            HQVoiceMsgDownloadManager.getInstance().enqueue(ConversationListFragment.this, new AutoDownloadEntry(message, AutoDownloadEntry.DownloadPriority.NORMAL));
        }
        RLog.d(TAG, "OnReceiveMessageEvent: " + message.getObjectName() + " " + event.getLeft() + " " + conversationType + " " + targetId);
        if (isConfigured(message.getConversationType()) && shouldUpdateConversation(event.getMessage(), event.getLeft())) {
            if (message.getMessageId() > 0) {
                int position;
                boolean gathered = getGatherState(conversationType);
                if (gathered) {
                    position = mAdapter.findGatheredItem(conversationType);
                } else {
                    position = mAdapter.findPosition(conversationType, targetId);
                }
                UIConversation uiConversation;
                if (position < 0) {
                    uiConversation = UIConversation.obtain(getActivity(), message, gathered);
                    onUIConversationCreated(uiConversation);
                    int index = getPosition(uiConversation);
                    mAdapter.add(uiConversation, index);
                    mAdapter.notifyDataSetChanged();
                } else {
                    uiConversation = mAdapter.getItem(position);
                    if (event.getMessage().getSentTime() > uiConversation.getUIConversationTime()) {
                        uiConversation.updateConversation(getContext(), message, gathered);

                        mAdapter.remove(position);
                        int index = getPosition(uiConversation);
                        mAdapter.add(uiConversation, index);
                        mAdapter.notifyDataSetChanged();
                    } else {
                        RLog.i(TAG, "ignore update message " + event.getMessage().getObjectName());
                    }
                }
                RLog.i(TAG, "conversation unread count : " + uiConversation.getUnReadMessageCount() + " " + conversationType + " " + targetId);
            }
            if (event.getLeft() == 0) {
                syncUnreadCount();
            }

            updateConversationReadReceipt(cacheEventList);

        }
    }

    public void onEventMainThread(Event.MessageLeftEvent event) {
        if (event.left == 0) {
            syncUnreadCount();
            RLog.d(TAG, "reload list by left event ");
            getConversationList(getConfigConversationTypes(), false);
        }
    }

    private void syncUnreadCount() {
        if (mAdapter.getCount() > 0) {
            for (int i = 0; i < mAdapter.getCount(); i++) {
                final UIConversation uiConversation = mAdapter.getItem(i);
                ConversationType conversationType = uiConversation.getConversationType();
                String targetId = uiConversation.getConversationTargetId();
                if (getGatherState(conversationType)) {
                    final int position = mAdapter.findGatheredItem(conversationType);
                    RongIMClient.getInstance().getUnreadCount(new RongIMClient.ResultCallback<Integer>() {
                        @Override
                        public void onSuccess(Integer integer) {
                            uiConversation.setUnReadMessageCount(integer);
                            mAdapter.notifyDataSetChanged();
                            onUnreadCountChanged();
                        }

                        @Override
                        public void onError(RongIMClient.ErrorCode e) {

                        }
                    }, conversationType);
                } else {
                    final int position = mAdapter.findPosition(conversationType, targetId);
                    RongIMClient.getInstance().getUnreadCount(conversationType, targetId, new RongIMClient.ResultCallback<Integer>() {
                        @Override
                        public void onSuccess(Integer integer) {
                            uiConversation.setUnReadMessageCount(integer);
                            mAdapter.notifyDataSetChanged();
                            onUnreadCountChanged();
                        }

                        @Override
                        public void onError(RongIMClient.ErrorCode e) {

                        }
                    });
                }
            }
        }
    }


    public void onEventMainThread(Event.MessageRecallEvent event) {
        RLog.d(TAG, "MessageRecallEvent");

        int count = mAdapter.getCount();
        for (int i = 0; i < count; i++) {
            UIConversation uiConversation = mAdapter.getItem(i);
            if (event.getMessageId() == uiConversation.getLatestMessageId()) {
                final boolean gatherState = mAdapter.getItem(i).getConversationGatherState();
                final int index = i;
                final String targetId = mAdapter.getItem(i).getConversationTargetId();
                if (gatherState) {
                    RongIM.getInstance().getConversationList(new RongIMClient.ResultCallback<List<Conversation>>() {
                        @Override
                        public void onSuccess(List<Conversation> conversationList) {
                            if (getActivity() == null || getActivity().isFinishing()) {
                                return;
                            }
                            if (conversationList != null && conversationList.size() > 0) {
                                UIConversation uiConversation = makeUIConversation(conversationList);
                                int oldPos = mAdapter.findPosition(uiConversation.getConversationType(), targetId);
                                if (oldPos >= 0) {
                                    UIConversation originalConversation = mAdapter.getItem(oldPos);
                                    uiConversation.setExtra(originalConversation.getExtra());
                                    mAdapter.remove(oldPos);
                                }
                                int newIndex = getPosition(uiConversation);
                                mAdapter.add(uiConversation, newIndex);
                                mAdapter.notifyDataSetChanged();
                            }
                        }

                        @Override
                        public void onError(RongIMClient.ErrorCode e) {

                        }
                    }, uiConversation.getConversationType());

                } else {
                    RongIM.getInstance().getConversation(uiConversation.getConversationType(),
                            uiConversation.getConversationTargetId(),
                            new RongIMClient.ResultCallback<Conversation>() {
                                @Override
                                public void onSuccess(Conversation conversation) {
                                    if (conversation != null) {
                                        UIConversation uiConversation = UIConversation.obtain(getActivity(), conversation, false);
                                        int pos = mAdapter.findPosition(conversation.getConversationType(), conversation.getTargetId());
                                        if (pos >= 0) {
                                            UIConversation originalConversation = mAdapter.getItem(pos);
                                            uiConversation.setExtra(originalConversation.getExtra());
                                            mAdapter.remove(pos);
                                        }
                                        int newPosition = getPosition(uiConversation);
                                        mAdapter.add(uiConversation, newPosition);
                                        mAdapter.notifyDataSetChanged();
                                    }
                                }

                                @Override
                                public void onError(RongIMClient.ErrorCode e) {
                                }
                            });
                }
                break;
            }
        }
    }

    public void onEventMainThread(Event.RemoteMessageRecallEvent event) {
        RLog.d(TAG, "RemoteMessageRecallEvent");

        ConversationType conversationType = event.getConversationType();
        final String targetId = event.getTargetId();
        int position = mAdapter.findPosition(conversationType, targetId);
        if (position == -1) {
            RLog.d(TAG, "ConversationListFragment UI unprepared!");
            return;
        }

        UIConversation uiConversation = mAdapter.getItem(position);
        final boolean gatherState = uiConversation.getConversationGatherState();

        if (gatherState) {
            RongIM.getInstance().getConversationList(new RongIMClient.ResultCallback<List<Conversation>>() {
                @Override
                public void onSuccess(List<Conversation> conversationList) {
                    if (getActivity() == null || getActivity().isFinishing()) {
                        return;
                    }
                    if (conversationList != null && conversationList.size() > 0) {
                        UIConversation uiConversation = makeUIConversation(conversationList);
                        int oldPos = mAdapter.findPosition(uiConversation.getConversationType(), targetId);
                        if (oldPos >= 0) {
                            UIConversation originalConversation = mAdapter.getItem(oldPos);
                            uiConversation.setExtra(originalConversation.getExtra());
                            mAdapter.remove(oldPos);
                        }
                        int newIndex = getPosition(uiConversation);
                        mAdapter.add(uiConversation, newIndex);
                        mAdapter.notifyDataSetChanged();
                        onUnreadCountChanged();
                    }
                }

                @Override
                public void onError(RongIMClient.ErrorCode e) {

                }
            }, mAdapter.getItem(position).getConversationType());
        } else {
            RongIM.getInstance().getConversation(uiConversation.getConversationType(),
                    uiConversation.getConversationTargetId(),
                    new RongIMClient.ResultCallback<Conversation>() {
                        @Override
                        public void onSuccess(Conversation conversation) {
                            if (conversation != null) {
                                UIConversation newConversation = UIConversation.obtain(getActivity(), conversation, false);
                                int pos = mAdapter.findPosition(conversation.getConversationType(), conversation.getTargetId());
                                if (pos >= 0) {
                                    UIConversation originalConversation = mAdapter.getItem(pos);
                                    newConversation.setExtra(originalConversation.getExtra());
                                    mAdapter.remove(pos);
                                }
                                int newPosition = getPosition(newConversation);
                                mAdapter.add(newConversation, newPosition);
                                mAdapter.notifyDataSetChanged();
                                onUnreadCountChanged();
                            }
                        }

                        @Override
                        public void onError(RongIMClient.ErrorCode e) {
                        }
                    });
        }

    }

    public void onEventMainThread(Message message) {
        ConversationType conversationType = message.getConversationType();
        String targetId = message.getTargetId();
        RLog.d(TAG, "Message: " + message.getObjectName() + " " + message.getMessageId() + " " + conversationType + " " + message.getSentStatus());
        if (shouldFilterConversation(conversationType, targetId)) {
            return;
        }

        boolean gathered = getGatherState(conversationType);
        if (isConfigured(conversationType) && message.getMessageId() > 0) {
            int position = gathered ? mAdapter.findGatheredItem(conversationType) : mAdapter.findPosition(conversationType, targetId);
            UIConversation uiConversation;
            if (position < 0) {
                uiConversation = UIConversation.obtain(getActivity(), message, gathered);
                onUIConversationCreated(uiConversation);
                int index = getPosition(uiConversation);
                mAdapter.add(uiConversation, index);
                mAdapter.notifyDataSetChanged();
            } else {
                uiConversation = mAdapter.getItem(position);
                //如果当前消息的时间早于会话时间，则不更新。但如果 lastMessageId < 0 表明当前会话消息为空，此时应更新会话和 adapter
                long covTime = uiConversation.getUIConversationTime();
                if (uiConversation.getLatestMessageId() == message.getMessageId()
                        && uiConversation.getSentStatus() == Message.SentStatus.SENDING
                        && message.getSentStatus() == Message.SentStatus.SENT
                        && message.getMessageDirection() == Message.MessageDirection.SEND) {
                    covTime = covTime - RongIMClient.getInstance().getDeltaTime();
                }
                if (covTime <= message.getSentTime() || uiConversation.getLatestMessageId() < 0) {
                    mAdapter.remove(position);
                    uiConversation.updateConversation(getContext(), message, gathered);
                    int index = getPosition(uiConversation);
                    mAdapter.add(uiConversation, index);
                    if (position == index) {
                        mAdapter.getView(index, mList.getChildAt(index - mList.getFirstVisiblePosition() + mList.getHeaderViewsCount()), mList);
                    } else {
                        mAdapter.notifyDataSetChanged();
                    }
                }
            }
        }
    }

    public void onEventMainThread(Event.MessageSentStatusUpdateEvent event) {
        Message message = event.getMessage();
        if (message == null || message.getMessageDirection().equals(Message.MessageDirection.RECEIVE)) {
            RLog.e(TAG, "MessageSentStatusUpdateEvent message is null or direction is RECEIVE");
            return;
        }
        ConversationType conversationType = message.getConversationType();
        String targetId = message.getTargetId();
        RLog.d(TAG, "MessageSentStatusUpdateEvent: " + event.getMessage().getTargetId() + " "
                + conversationType + " " + event.getSentStatus());
        boolean gathered = getGatherState(conversationType);
        if (gathered) {
            return;
        }

        if (isConfigured(conversationType) && message.getMessageId() > 0) {
            int position = mAdapter.findPosition(conversationType, targetId);
            UIConversation uiConversation;
            uiConversation = mAdapter.getItem(position);

            if (message.getMessageId() == uiConversation.getLatestMessageId()) {
                mAdapter.remove(position);
                uiConversation.updateConversation(getContext(), message, gathered);
                int index = getPosition(uiConversation);
                mAdapter.add(uiConversation, index);
                if (position == index) {
                    mAdapter.getView(index, mList.getChildAt(index - mList.getFirstVisiblePosition() + mList.getHeaderViewsCount()), mList);
                } else {
                    mAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    public void onEventMainThread(final RongIMClient.ConnectionStatusListener.ConnectionStatus status) {
        RLog.d(TAG, "ConnectionStatus, " + status.toString());

        setNotificationBarVisibility(status);
        if (status.equals(RongIMClient.ConnectionStatusListener.ConnectionStatus.CONNECTED) && isShowWithoutConnected) {
            getConversationList(getConfigConversationTypes(), false);
            isShowWithoutConnected = false;
        }
    }

    public void onEventMainThread(Event.ConnectEvent event) {
        RLog.d(TAG, "ConnectEvent :" + RongIMClient.getInstance().getCurrentConnectionStatus());
        if (isShowWithoutConnected) {
            getConversationList(getConfigConversationTypes(), false);
            isShowWithoutConnected = false;
        }
    }

    public void onEventMainThread(final Event.CreateDiscussionEvent createDiscussionEvent) {
        RLog.d(TAG, "createDiscussionEvent");
        final String targetId = createDiscussionEvent.getDiscussionId();
        if (isConfigured(ConversationType.DISCUSSION)) {
            RongIMClient.getInstance().getConversation(ConversationType.DISCUSSION, targetId, new RongIMClient.ResultCallback<Conversation>() {
                @Override
                public void onSuccess(Conversation conversation) {
                    if (conversation != null) {
                        UIConversation uiConversation;
                        int position;
                        if (getGatherState(ConversationType.DISCUSSION)) {
                            position = mAdapter.findGatheredItem(ConversationType.DISCUSSION);
                        } else {
                            position = mAdapter.findPosition(ConversationType.DISCUSSION, targetId);
                        }
                        conversation.setConversationTitle(createDiscussionEvent.getDiscussionName());
                        if (position < 0) {
                            uiConversation = UIConversation.obtain(getActivity(), conversation, getGatherState(ConversationType.DISCUSSION));
                            onUIConversationCreated(uiConversation);
                            int index = getPosition(uiConversation);
                            mAdapter.add(uiConversation, index);
                            mAdapter.notifyDataSetChanged();
                        } else {
                            uiConversation = mAdapter.getItem(position);
                            uiConversation.updateConversation(getContext(), conversation, getGatherState(ConversationType.DISCUSSION));
                            mAdapter.getView(position, mList.getChildAt(position - mList.getFirstVisiblePosition() + mList.getHeaderViewsCount()), mList);
                        }
                    }
                }

                @Override
                public void onError(RongIMClient.ErrorCode e) {

                }
            });
        }
    }

    public void onEventMainThread(final Event.DraftEvent draft) {
        final ConversationType conversationType = draft.getConversationType();
        final String targetId = draft.getTargetId();
        RLog.i(TAG, "Draft : " + conversationType);
        RongIMClient.getInstance().getConversation(draft.getConversationType(), draft.getTargetId(),
                new RongIMClient.ResultCallback<Conversation>() {
                    @Override
                    public void onSuccess(Conversation conversation) {
                        int position = mAdapter.findPosition(conversationType, targetId);
                        if (position >= 0 && !getGatherState(conversationType)) {
                            mAdapter.remove(position);
                            UIConversation newUiConversation = UIConversation.obtain(getActivity(), conversation, false);
                            int index = getPosition(newUiConversation);
                            mAdapter.add(newUiConversation, index);
                            if (index == position) {
                                mAdapter.getView(index, mList.getChildAt(index - mList.getFirstVisiblePosition() + mList.getHeaderViewsCount()), mList);
                            } else {
                                mAdapter.notifyDataSetChanged();
                            }
                        }
                    }

                    @Override
                    public void onError(RongIMClient.ErrorCode e) {

                    }
                });
    }

    public void onEventMainThread(Group groupInfo) {
        RLog.d(TAG, "Group: " + groupInfo.getName() + " " + groupInfo.getId());

        int count = mAdapter.getCount();
        if (groupInfo.getName() == null) {
            return;
        }
        for (int i = 0; i < count; i++) {
            UIConversation uiConversation = mAdapter.getItem(i);
            uiConversation.updateConversation(getContext(), groupInfo);
        }
        mAdapter.notifyDataSetChanged();
    }

    public void onEventMainThread(Discussion discussion) {
        RLog.d(TAG, "Discussion: " + discussion.getName() + " " + discussion.getId());

        if (isConfigured(ConversationType.DISCUSSION)) {
            int position;
            if (getGatherState(ConversationType.DISCUSSION)) {
                position = mAdapter.findGatheredItem(ConversationType.DISCUSSION);
            } else {
                position = mAdapter.findPosition(ConversationType.DISCUSSION, discussion.getId());
            }
            if (position >= 0) {
                for (int i = 0; i <= position; i++) {
                    UIConversation uiConversation = mAdapter.getItem(i);
                    uiConversation.updateConversation(getContext(), discussion);
                }
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    public void onEventMainThread(GroupUserInfo groupUserInfo) {
        RLog.d(TAG, "GroupUserInfo " + groupUserInfo.getGroupId() + " " + groupUserInfo.getUserId() + " " + groupUserInfo.getNickname());
        if (groupUserInfo.getNickname() == null || groupUserInfo.getGroupId() == null) {
            return;
        }
        int count = mAdapter.getCount();
        for (int i = 0; i < count; i++) {
            UIConversation uiConversation = mAdapter.getItem(i);
            if (!getGatherState(ConversationType.GROUP)
                    && uiConversation.getConversationTargetId().equals(groupUserInfo.getGroupId())
                    && uiConversation.getConversationSenderId().equals(groupUserInfo.getUserId())) {
                uiConversation.updateConversation(getContext(), groupUserInfo);
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    public void onEventMainThread(UserInfo userInfo) {
        RLog.i(TAG, "UserInfo " + userInfo.getUserId() + " " + userInfo.getName());

        int count = mAdapter.getCount();
        for (int i = 0; i < count && userInfo.getName() != null; i++) {
            UIConversation uiConversation = mAdapter.getItem(i);
            if (uiConversation.hasNickname(userInfo.getUserId())) {
                RLog.i(TAG, "has nick name");
                continue;
            }
            uiConversation.updateConversation(getContext(), userInfo);
        }
        mAdapter.notifyDataSetChanged();
    }

    public void onEventMainThread(PublicServiceProfile profile) {
        RLog.d(TAG, "PublicServiceProfile");
        int count = mAdapter.getCount();
        boolean gatherState = getGatherState(profile.getConversationType());
        for (int i = 0; i < count; i++) {
            UIConversation uiConversation = mAdapter.getItem(i);
            if (uiConversation.getConversationType().equals(profile.getConversationType())
                    && uiConversation.getConversationTargetId().equals(profile.getTargetId())
                    && !gatherState) {
                uiConversation.setUIConversationTitle(profile.getName());
                uiConversation.setIconUrl(profile.getPortraitUri());
                mAdapter.getView(i, mList.getChildAt(i - mList.getFirstVisiblePosition() + mList.getHeaderViewsCount()), mList);
                break;
            }
        }

    }

    public void onEventMainThread(Event.PublicServiceFollowableEvent event) {
        RLog.d(TAG, "PublicServiceFollowableEvent");
        if (!event.isFollow()) {
            int originalIndex = mAdapter.findPosition(event.getConversationType(), event.getTargetId());
            if (originalIndex >= 0) {
                mAdapter.remove(originalIndex);
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    public void onEventMainThread(final Event.ConversationUnreadEvent unreadEvent) {
        RLog.d(TAG, "ConversationUnreadEvent");

        ConversationType conversationType = unreadEvent.getType();
        String targetId = unreadEvent.getTargetId();
        int position = getGatherState(conversationType) ? mAdapter.findGatheredItem(conversationType) : mAdapter.findPosition(conversationType, targetId);
        if (position >= 0) {
            UIConversation uiConversation = mAdapter.getItem(position);
            uiConversation.clearUnRead(conversationType, targetId);
            mAdapter.notifyDataSetChanged();
        }
        onUnreadCountChanged();
    }

    /**
     * 不处理聚合情况下的置顶事件，保持按照时间排序规则。
     *
     * @param setTopEvent 置顶事件。
     */
    public void onEventMainThread(final Event.ConversationTopEvent setTopEvent) {
        RLog.d(TAG, "ConversationTopEvent");
        ConversationType conversationType = setTopEvent.getConversationType();
        String targetId = setTopEvent.getTargetId();
        int position = mAdapter.findPosition(conversationType, targetId);
        if (position >= 0 && !getGatherState(conversationType)) {
            UIConversation uiConversation = mAdapter.getItem(position);
            if (uiConversation.isTop() != setTopEvent.isTop()) {
                uiConversation.setTop(!uiConversation.isTop());
                mAdapter.remove(position);
                int index = getPosition(uiConversation);
                mAdapter.add(uiConversation, index);
                if (index == position) {
                    mAdapter.getView(index, mList.getChildAt(index - mList.getFirstVisiblePosition() + mList.getHeaderViewsCount()), mList);
                } else {
                    mAdapter.notifyDataSetChanged();
                }
            }
        }
    }


    public void onEventMainThread(final Event.ConversationRemoveEvent removeEvent) {
        RLog.d(TAG, "ConversationRemoveEvent");

        ConversationType conversationType = removeEvent.getType();
        removeConversation(conversationType, removeEvent.getTargetId());
    }

    public void onEventMainThread(final Event.ClearConversationEvent clearConversationEvent) {
        RLog.d(TAG, "ClearConversationEvent");

        List<Conversation.ConversationType> typeList = clearConversationEvent.getTypes();
        for (int i = mAdapter.getCount() - 1; i >= 0; i--) {
            if (typeList.indexOf(mAdapter.getItem(i).getConversationType()) >= 0) {
                mAdapter.remove(i);
            }
        }
        mAdapter.notifyDataSetChanged();
        onUnreadCountChanged();
    }


    public void onEventMainThread(Event.MessageDeleteEvent event) {
        RLog.d(TAG, "MessageDeleteEvent");

        int count = mAdapter.getCount();
        for (int i = 0; i < count; i++) {
            if (event.getMessageIds().contains(mAdapter.getItem(i).getLatestMessageId())) {
                final boolean gatherState = mAdapter.getItem(i).getConversationGatherState();
                final String targetId = mAdapter.getItem(i).getConversationTargetId();
                if (gatherState) {
                    RongIM.getInstance().getConversationList(new RongIMClient.ResultCallback<List<Conversation>>() {
                        @Override
                        public void onSuccess(List<Conversation> conversationList) {
                            if (getActivity() == null || getActivity().isFinishing()) {
                                return;
                            }
                            if (conversationList == null || conversationList.size() == 0)
                                return;
                            UIConversation uiConversation = makeUIConversation(conversationList);
                            int oldPos = mAdapter.findPosition(uiConversation.getConversationType(), targetId);
                            if (oldPos >= 0) {
                                UIConversation originalConversation = mAdapter.getItem(oldPos);
                                uiConversation.setExtra(originalConversation.getExtra());
                                mAdapter.remove(oldPos);
                            }
                            int newIndex = getPosition(uiConversation);
                            mAdapter.add(uiConversation, newIndex);
                            mAdapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onError(RongIMClient.ErrorCode e) {

                        }
                    }, mAdapter.getItem(i).getConversationType());

                } else {
                    RongIM.getInstance().getConversation(mAdapter.getItem(i).getConversationType(), mAdapter.getItem(i).getConversationTargetId(),
                            new RongIMClient.ResultCallback<Conversation>() {
                                @Override
                                public void onSuccess(Conversation conversation) {
                                    if (conversation == null) {
                                        RLog.d(TAG, "onEventMainThread getConversation : onSuccess, conversation = null");
                                        return;
                                    }
                                    UIConversation uiConversation = UIConversation.obtain(getActivity(), conversation, false);
                                    int pos = mAdapter.findPosition(conversation.getConversationType(), conversation.getTargetId());
                                    if (pos >= 0) {
                                        UIConversation originalConversation = mAdapter.getItem(pos);
                                        uiConversation.setExtra(originalConversation.getExtra());
                                        mAdapter.remove(pos);
                                    }
                                    int newIndex = getPosition(uiConversation);
                                    mAdapter.add(uiConversation, newIndex);
                                    mAdapter.notifyDataSetChanged();
                                }

                                @Override
                                public void onError(RongIMClient.ErrorCode e) {
                                }
                            });
                }
                break;
            }
        }
    }

    public void onEventMainThread(Event.ConversationNotificationEvent notificationEvent) {
        int originalIndex = mAdapter.findPosition(notificationEvent.getConversationType(), notificationEvent.getTargetId());
        if (originalIndex >= 0) {
            UIConversation uiConversation = mAdapter.getItem(originalIndex);
            if (!uiConversation.getNotificationStatus().equals(notificationEvent.getStatus())) {
                uiConversation.setNotificationStatus(notificationEvent.getStatus());
                mAdapter.getView(originalIndex, mList.getChildAt(originalIndex - mList.getFirstVisiblePosition() + mList.getHeaderViewsCount()), mList);
            }
            onUnreadCountChanged();
        }
    }


    /**
     * 清除消息后，会话时间不会改变，依然跟随上一条消息的时间。
     * 那么会话所在的顺序也不会改变，仅需要清除会话列表内容
     *
     * @param clearMessagesEvent 清除消息事件。
     */
    public void onEventMainThread(Event.MessagesClearEvent clearMessagesEvent) {
        RLog.d(TAG, "MessagesClearEvent");
        ConversationType conversationType = clearMessagesEvent.getType();
        String targetId = clearMessagesEvent.getTargetId();
        int position = getGatherState(conversationType) ? mAdapter.findGatheredItem(conversationType) : mAdapter.findPosition(conversationType, targetId);
        if (position >= 0) {
            UIConversation uiConversation = mAdapter.getItem(position);
            uiConversation.clearLastMessage();
            mAdapter.getView(position, mList.getChildAt(position - mList.getFirstVisiblePosition() + mList.getHeaderViewsCount()), mList);
        }
    }

    public void onEventMainThread(Event.OnMessageSendErrorEvent sendErrorEvent) {
        Message message = sendErrorEvent.getMessage();
        ConversationType conversationType = message.getConversationType();
        String targetId = message.getTargetId();
        if (isConfigured(conversationType)) {
            boolean gathered = getGatherState(conversationType);
            int index = gathered ? mAdapter.findGatheredItem(conversationType) : mAdapter.findPosition(conversationType, targetId);
            if (index >= 0) {
                UIConversation uiConversation = mAdapter.getItem(index);
                message.setSentStatus(Message.SentStatus.FAILED);
                uiConversation.updateConversation(getContext(), message, gathered);
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    public void onEventMainThread(Event.QuitDiscussionEvent event) {
        RLog.d(TAG, "QuitDiscussionEvent");
        removeConversation(ConversationType.DISCUSSION, event.getDiscussionId());
    }

    public void onEventMainThread(Event.QuitGroupEvent event) {
        RLog.d(TAG, "QuitGroupEvent");
        removeConversation(ConversationType.GROUP, event.getGroupId());
    }

    private void removeConversation(final ConversationType conversationType, String targetId) {
        boolean gathered = getGatherState(conversationType);
        if (gathered) {
            int index = mAdapter.findGatheredItem(conversationType);
            if (index >= 0) {
                RongIM.getInstance().getConversationList(new RongIMClient.ResultCallback<List<Conversation>>() {
                    @Override
                    public void onSuccess(List<Conversation> conversationList) {
                        if (getActivity() == null || getActivity().isFinishing()) {
                            return;
                        }
                        int oldPos = mAdapter.findGatheredItem(conversationType);
                        if (oldPos >= 0) {
                            mAdapter.remove(oldPos);
                            if (conversationList != null && conversationList.size() > 0) {
                                UIConversation uiConversation = makeUIConversation(conversationList);
                                int newIndex = getPosition(uiConversation);
                                mAdapter.add(uiConversation, newIndex);
                            }
                            mAdapter.notifyDataSetChanged();
                            onUnreadCountChanged();
                        }
                    }

                    @Override
                    public void onError(RongIMClient.ErrorCode e) {

                    }
                }, conversationType);
            }
        } else {
            int index = mAdapter.findPosition(conversationType, targetId);
            if (index >= 0) {
                mAdapter.remove(index);
                mAdapter.notifyDataSetChanged();
                onUnreadCountChanged();
            }
        }
    }

    @Override
    public void onPortraitItemClick(View v, UIConversation data) {
        ConversationType type = data.getConversationType();
        if (getGatherState(type)) {
            RongIM.getInstance().startSubConversationList(getActivity(), type);
        } else {
            if (RongContext.getInstance().getConversationListBehaviorListener() != null) {
                boolean isDefault = RongContext.getInstance().getConversationListBehaviorListener().onConversationPortraitClick(getActivity(), type, data.getConversationTargetId());
                if (isDefault)
                    return;
            }
            data.setUnReadMessageCount(0);
            RongIM.getInstance().startConversation(getActivity(), type, data.getConversationTargetId(), data.getUIConversationTitle());
        }

    }

    @Override
    public boolean onPortraitItemLongClick(View v, UIConversation data) {
        ConversationType type = data.getConversationType();

        if (RongContext.getInstance().getConversationListBehaviorListener() != null) {
            boolean isDealt = RongContext.getInstance().getConversationListBehaviorListener().onConversationPortraitLongClick(getActivity(), type, data.getConversationTargetId());
            if (isDealt)
                return true;
        }
        if (!getGatherState(type)) {
            buildMultiDialog(data);
            return true;
        } else {
            buildSingleDialog(data);
            return true;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Long previousClickTimestamp = (Long) view.getTag(R.id.rc_debounceClick_last_timestamp);
        long currentTimestamp = SystemClock.uptimeMillis();
        view.setTag(R.id.rc_debounceClick_last_timestamp, currentTimestamp);
        if (previousClickTimestamp == null
                || Math.abs(currentTimestamp - previousClickTimestamp) > 1000) { //防止过快点击出现多次会话界面
            int realPosition = position - mList.getHeaderViewsCount();
            if (realPosition >= 0 && realPosition < mAdapter.getCount()) {
                UIConversation uiConversation = mAdapter.getItem(realPosition);
                ConversationType conversationType = uiConversation.getConversationType();
                if (getGatherState(conversationType)) {
                    RongIM.getInstance().startSubConversationList(getActivity(), conversationType);
                } else {
                    if (RongContext.getInstance().getConversationListBehaviorListener() != null
                            && RongContext.getInstance().getConversationListBehaviorListener().onConversationClick(getActivity(), view, uiConversation)) {
                        return;
                    }
                    uiConversation.setUnReadMessageCount(0);
                    RongIM.getInstance().startConversation(getActivity(), conversationType, uiConversation.getConversationTargetId(), uiConversation.getUIConversationTitle());
                }
            }
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        int realPosition = position - mList.getHeaderViewsCount();
        if (realPosition >= 0 && realPosition < mAdapter.getCount()) {
            UIConversation uiConversation = mAdapter.getItem(realPosition);

            if (RongContext.getInstance().getConversationListBehaviorListener() != null) {
                boolean isDealt = RongContext.getInstance().getConversationListBehaviorListener().onConversationLongClick(getActivity(), view, uiConversation);
                if (isDealt)
                    return true;
            }
            if (!getGatherState(uiConversation.getConversationType())) {
                buildMultiDialog(uiConversation);
                return true;
            } else {
                buildSingleDialog(uiConversation);
                return true;
            }
        }
        return false;
    }

    private void buildMultiDialog(final UIConversation uiConversation) {
        final Context context = getContext();
        if (context == null) {
            return;
        }
        String[] items = new String[2];
        if (uiConversation.isTop())
            items[0] = context.getString(R.string.rc_conversation_list_dialog_cancel_top);
        else
            items[0] = context.getString(R.string.rc_conversation_list_dialog_set_top);

        items[1] = context.getString(R.string.rc_conversation_list_dialog_remove);

        OptionsPopupDialog.newInstance(getActivity(), items).setOptionsPopupDialogListener(new OptionsPopupDialog.OnOptionsItemClickedListener() {
            @Override
            public void onOptionsItemClicked(int which) {
                if (which == 0) {
                    RongIM.getInstance().setConversationToTop(
                            uiConversation.getConversationType(),
                            uiConversation.getConversationTargetId(),
                            !uiConversation.isTop(),
                            new RongIMClient.ResultCallback<Boolean>() {
                                @Override
                                public void onSuccess(Boolean aBoolean) {
                                    if (uiConversation.isTop()) {
                                        Toast.makeText(context, getString(R.string.rc_conversation_list_popup_cancel_top), Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(context, getString(R.string.rc_conversation_list_dialog_set_top), Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onError(RongIMClient.ErrorCode e) {

                                }
                            });
                } else if (which == 1) {
                    RongIM.getInstance().removeConversation(uiConversation.getConversationType(), uiConversation.getConversationTargetId(), null);
                }
            }
        }).show();
    }

    private void buildSingleDialog(final UIConversation uiConversation) {
        final Context context = getContext();
        if (context == null) {
            return;
        }
        String[] items = new String[1];
        items[0] = context.getString(R.string.rc_conversation_list_dialog_remove);

        OptionsPopupDialog.newInstance(getActivity(), items).setOptionsPopupDialogListener(new OptionsPopupDialog.OnOptionsItemClickedListener() {
            @Override
            public void onOptionsItemClicked(int which) {
                RongIM.getInstance().getConversationList(new RongIMClient.ResultCallback<List<Conversation>>() {
                    @Override
                    public void onSuccess(List<Conversation> conversations) {
                        if (conversations != null && conversations.size() > 0) {
                            for (Conversation conversation : conversations) {
                                RongIM.getInstance().removeConversation(conversation.getConversationType(), conversation.getTargetId(), null);
                            }
                        }
                    }

                    @Override
                    public void onError(RongIMClient.ErrorCode errorCode) {
                    }

                }, uiConversation.getConversationType());

                int position = mAdapter.findGatheredItem(uiConversation.getConversationType());
                mAdapter.remove(position);
                mAdapter.notifyDataSetChanged();
            }
        }).show();
    }

    // conversationList排序规律：
    // 1. 首先是top会话，按时间顺序排列。
    // 2. 然后非top会话也是按时间排列。
    private void makeUiConversationList(List<Conversation> conversationList) {
        UIConversation uiConversation;
        for (Conversation conversation : conversationList) {
            ConversationType conversationType = conversation.getConversationType();
            String targetId = conversation.getTargetId();
            boolean gatherState = getGatherState(conversationType);
            int originalIndex;
            if (gatherState) {
                originalIndex = mAdapter.findGatheredItem(conversationType);
                if (originalIndex >= 0) {
                    uiConversation = mAdapter.getItem(originalIndex);
                    uiConversation.updateConversation(getContext(), conversation, true);
                } else {
                    uiConversation = UIConversation.obtain(getActivity(), conversation, true);
                    onUIConversationCreated(uiConversation);
                    mAdapter.add(uiConversation);
                }
            } else {
                originalIndex = mAdapter.findPosition(conversationType, targetId);
                if (originalIndex < 0) {
                    uiConversation = UIConversation.obtain(getActivity(), conversation, false);
                    onUIConversationCreated(uiConversation);
                    int index = getPosition(uiConversation);
                    mAdapter.add(uiConversation, index);
                } else {
                    uiConversation = mAdapter.getItem(originalIndex);
                    if (uiConversation.getUIConversationTime() <= conversation.getSentTime()) {
                        mAdapter.remove(originalIndex);
                        uiConversation.updateConversation(getContext(), conversation, false);
                        int index = getPosition(uiConversation);
                        mAdapter.add(uiConversation, index);
                    } else {
                        uiConversation.setUnReadMessageCount(conversation.getUnreadMessageCount());
                    }
                }
            }
        }
    }

    /**
     * 根据conversations列表，构建新的会话。如：聚合情况下，删掉某条子会话时，根据剩余会话构建新的UI会话。
     */
    private UIConversation makeUIConversation(List<Conversation> conversations) {
        int unreadCount = 0;
        boolean isMentioned = false;
        Conversation newest = conversations.get(0);

        for (Conversation conversation : conversations) {
            if (newest.isTop()) {
                if (conversation.isTop() && conversation.getSentTime() > newest.getSentTime()) {
                    newest = conversation;
                }
            } else {
                if (conversation.isTop() || conversation.getSentTime() > newest.getSentTime()) {
                    newest = conversation;
                }
            }
            if (conversation.getMentionedCount() > 0) {
                isMentioned = true;
            }

            unreadCount = unreadCount + conversation.getUnreadMessageCount();
        }

        UIConversation uiConversation = UIConversation.obtain(getActivity(), newest, getGatherState(newest.getConversationType()));
        uiConversation.setUnReadMessageCount(unreadCount);
        //聚合模式，才会调用此方法，top 为 false。
        uiConversation.setTop(false);
        uiConversation.setMentionedFlag(isMentioned);
        return uiConversation;
    }

    private int getPosition(UIConversation uiConversation) {
        int count = mAdapter.getCount();
        int i, position = 0;

        for (i = 0; i < count; i++) {
            if (uiConversation.isTop()) {
                if (mAdapter.getItem(i).isTop() && mAdapter.getItem(i).getUIConversationTime() > uiConversation.getUIConversationTime())
                    position++;
                else
                    break;
            } else {
                if (mAdapter.getItem(i).isTop() || mAdapter.getItem(i).getUIConversationTime() > uiConversation.getUIConversationTime())
                    position++;
                else
                    break;
            }
        }

        return position;
    }

    private boolean isConfigured(ConversationType conversationType) {
        for (int i = 0; i < mConversationsConfig.size(); i++) {
            if (conversationType.equals(mConversationsConfig.get(i).conversationType)) {
                return true;
            }
        }
        return false;
    }

    public boolean getGatherState(Conversation.ConversationType conversationType) {
        for (ConversationConfig config : mConversationsConfig) {
            if (config.conversationType.equals(conversationType)) {
                return config.isGathered;
            }
        }
        return false;
    }

    private Conversation.ConversationType[] getConfigConversationTypes() {
        Conversation.ConversationType[] conversationTypes = new Conversation.ConversationType[mConversationsConfig.size()];
        for (int i = 0; i < mConversationsConfig.size(); i++) {
            conversationTypes[i] = mConversationsConfig.get(i).conversationType;
        }
        return conversationTypes;
    }


    @Override
    public void onLoad() {
        getConversationList(getConfigConversationTypes(), true);
    }

    @Override
    public void onFlush() {
        getConversationList(getConfigConversationTypes(), false);
    }

    private class ConversationConfig {
        Conversation.ConversationType conversationType;
        boolean isGathered;
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(mThis);
        cacheEventList.clear();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_MSG_DOWNLOAD_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            HQVoiceMsgDownloadManager.getInstance().resumeDownloadService();
        }
    }

    public void onEventMainThread(final ConversationStatus[] conversationStatus) {
        RLog.i(TAG, "onEventMainThread conversationStatus " + conversationStatus.length);
        if (conversationStatus.length == 1) {
            String conversationId = conversationStatus[0].getTargetId();
            Conversation.ConversationType conversationType = conversationStatus[0].getConversationType();
            int position = mAdapter.findPosition(conversationType, conversationId);
            if (!getGatherState(conversationType)) {
                if (position >= 0) {
                    final UIConversation uiConversation = mAdapter.getItem(position);
                    uiConversation.setNotificationStatus(conversationStatus[0].getNotifyStatus());
                    onEventMainThread(new Event.ConversationTopEvent(conversationType,
                            conversationId, conversationStatus[0].isTop()));
                    updateListItem(uiConversation);
                } else {
                    getConversationList(getConfigConversationTypes(), false);
                }
            }
        } else {
            getConversationList(getConfigConversationTypes(), false);
        }
    }

}

package io.rong.imkit.fragment;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.Parcelable;
import android.provider.MediaStore;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.rong.common.RLog;
import io.rong.eventbus.EventBus;
import io.rong.imkit.DeleteClickActions;
import io.rong.imkit.IExtensionClickListener;
import io.rong.imkit.IExtensionModule;
import io.rong.imkit.IPublicServiceMenuClickListener;
import io.rong.imkit.InputMenu;
import io.rong.imkit.R;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongExtension;
import io.rong.imkit.RongIM;
import io.rong.imkit.RongKitIntent;
import io.rong.imkit.RongKitReceiver;
import io.rong.imkit.RongMessageItemLongClickActionManager;
import io.rong.imkit.actions.IClickActions;
import io.rong.imkit.actions.OnMoreActionStateListener;
import io.rong.imkit.activity.SelectConversationActivity;
import io.rong.imkit.destruct.DestructManager;
import io.rong.imkit.dialog.BurnHintDialog;
import io.rong.imkit.manager.AudioPlayManager;
import io.rong.imkit.manager.AudioRecordManager;
import io.rong.imkit.manager.InternalModuleManager;
import io.rong.imkit.manager.SendImageManager;
import io.rong.imkit.manager.SendMediaManager;
import io.rong.imkit.manager.UnReadMessageManager;
import io.rong.imkit.mention.DraftHelper;
import io.rong.imkit.mention.IAddMentionedMemberListener;
import io.rong.imkit.mention.RongMentionManager;
import io.rong.imkit.model.ConversationInfo;
import io.rong.imkit.model.Event;
import io.rong.imkit.model.GroupUserInfo;
import io.rong.imkit.model.UIMessage;
import io.rong.imkit.plugin.DefaultLocationPlugin;
import io.rong.imkit.plugin.IPluginModule;
import io.rong.imkit.plugin.location.IRealTimeLocationStateListener;
import io.rong.imkit.plugin.location.IUserInfoProvider;
import io.rong.imkit.recallEdit.RecallEditManager;
import io.rong.imkit.reference.ReferenceView;
import io.rong.imkit.resend.ResendManager;
import io.rong.imkit.userInfoCache.RongUserInfoManager;
import io.rong.imkit.utilities.KitCommonDefine;
import io.rong.imkit.utilities.PermissionCheckUtil;
import io.rong.imkit.utilities.PromptPopupDialog;
import io.rong.imkit.utils.ForwardManager;
import io.rong.imkit.utils.SystemUtils;
import io.rong.imkit.voiceMessageDownload.AutoDownloadEntry;
import io.rong.imkit.voiceMessageDownload.HQVoiceMsgDownloadManager;
import io.rong.imkit.widget.AutoRefreshListView;
import io.rong.imkit.widget.CSEvaluateDialog;
import io.rong.imkit.widget.ForwardClickActions;
import io.rong.imkit.widget.SingleChoiceDialog;
import io.rong.imkit.widget.adapter.MessageListAdapter;
import io.rong.imkit.widget.provider.EvaluatePlugin;
import io.rong.imkit.widget.provider.MessageItemLongClickAction;
import io.rong.imlib.CustomServiceConfig;
import io.rong.imlib.DestructionTag;
import io.rong.imlib.ICustomServiceListener;
import io.rong.imlib.IMLibExtensionModuleManager;
import io.rong.imlib.IRongCallback;
import io.rong.imlib.MessageTag;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.common.DeviceUtils;
import io.rong.imlib.common.SharedPreferencesUtils;
import io.rong.imlib.destruct.MessageBufferPool;
import io.rong.imlib.location.RealTimeLocationConstant;
import io.rong.imlib.location.message.RealTimeLocationStartMessage;
import io.rong.imlib.model.CSCustomServiceInfo;
import io.rong.imlib.model.CSGroupItem;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.CustomServiceMode;
import io.rong.imlib.model.HardwareResource;
import io.rong.imlib.model.MentionedInfo;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.PublicServiceMenu;
import io.rong.imlib.model.PublicServiceMenuItem;
import io.rong.imlib.model.PublicServiceProfile;
import io.rong.imlib.model.ReadReceiptInfo;
import io.rong.imlib.model.UserInfo;
import io.rong.message.CSPullLeaveMessage;
import io.rong.message.DestructionCmdMessage;
import io.rong.message.FileMessage;
import io.rong.message.GIFMessage;
import io.rong.message.HQVoiceMessage;
import io.rong.message.HistoryDividerMessage;
import io.rong.message.ImageMessage;
import io.rong.message.InformationNotificationMessage;
import io.rong.message.LocationMessage;
import io.rong.message.MediaMessageContent;
import io.rong.message.PublicServiceCommandMessage;
import io.rong.message.ReadReceiptMessage;
import io.rong.message.RecallNotificationMessage;
import io.rong.message.ReferenceMessage;
import io.rong.message.RichContentMessage;
import io.rong.message.SightMessage;
import io.rong.message.TextMessage;
import io.rong.message.VoiceMessage;
import io.rong.push.RongPushClient;


public class ConversationFragment extends UriFragment implements
        AbsListView.OnScrollListener,
        IExtensionClickListener,
        IUserInfoProvider,
        CSEvaluateDialog.EvaluateClickListener, View.OnClickListener {
    public static final String TAG = "ConversationFragment";
    protected PublicServiceProfile mPublicServiceProfile;

    private RongExtension mRongExtension;
    private boolean mEnableMention;
    private float mLastTouchY;
    private boolean mUpDirection;
    private float mOffsetLimit;
    //当 activity 被 pause 后，未 resume 前，就 finish 销毁 fragment，onPause()方法中的销毁流程不会执行。
    //增加此方法，解决 destroy 任何时候都能执行到。
    private boolean finishing = false;

    private CSCustomServiceInfo mCustomUserInfo;
    private ConversationInfo mCurrentConversationInfo;
    private String mDraft;
    private static final int REQUEST_CODE_ASK_PERMISSIONS = 100;
    private static final int REQUEST_CODE_LOCATION_SHARE = 101;
    private static final int REQUEST_CS_LEAVEL_MESSAGE = 102;
    private static final int REQUEST_CODE_PERMISSION = 103;
    // 开启合并转发的选择会话界面
    public static final int REQUEST_CODE_FORWARD = 104;

    private static final int REQUEST_MSG_DOWNLOAD_PERMISSION = 1001;
    public final static int SCROLL_MODE_NORMAL = 1;
    public final static int SCROLL_MODE_TOP = 2;
    public final static int SCROLL_MODE_BOTTOM = 3;

    private static int DEFAULT_HISTORY_MESSAGE_COUNT = 10;
    private static int DEFAULT_REMOTE_MESSAGE_COUNT = 10;
    private final static int TIP_DEFAULT_MESSAGE_COUNT = 1;//提示新消息数时，需要列表底部消息的不可见数（当值为1时代表底部有1条消息不可见时，显示新消息数）
    private static int SHOW_UNREAD_MESSAGE_COUNT = 10;


    // 旋转屏幕后需要恢复的数据
    // 保存未读消息数量
    private final static String UN_READ_COUNT = "unReadCount";
    // 保存列表滚动位置
    private final static String LIST_STATE = "listState";
    // 新消息数量
    private final static String NEW_MESSAGE_COUNT = "newMessageCount";

    private String mTargetId;
    private Conversation.ConversationType mConversationType;
    private long indexMessageTime;

    private boolean mReadRec;
    private boolean mSyncReadStatus;
    private int mNewMessageCount;
    private int mUnReadCount;

    private AutoRefreshListView mList;
    private LinearLayout mUnreadMsgLayout;
    private LinearLayout mMentionMsgLayout;
    private TextView mUnreadMsgCountTv;
    private TextView mMentionMsgCountTv;
    private ImageButton mNewMessageBtn;
    private TextView mNewMessageTextView;
    private MessageListAdapter mListAdapter;
    private View mMsgListView;
    private LinearLayout mNotificationContainer;

    private boolean mHasMoreLocalMessagesUp = true;
    private boolean mHasMoreLocalMessagesDown = true;
    private long mSyncReadStatusMsgTime;
    private boolean mCSNeedToQuit = false;

    /**
     * 是否应该将 onReceived 方法接收到的新消息加入到消息列表中
     */
    private boolean mShouldInsertMsg = true;

    private List<String> mLocationShareParticipants;
    private CustomServiceConfig mCustomServiceConfig;
    private CSEvaluateDialog mEvaluateDialg;

    private RongKitReceiver mKitReceiver;

    private MessageItemLongClickAction clickAction;
    private OnMoreActionStateListener moreActionStateListener;

    private Bundle mSavedInstanceState;
    private Parcelable mListViewState;

    private final int CS_HUMAN_MODE_CUSTOMER_EXPIRE = 0;
    private final int CS_HUMAN_MODE_SEAT_EXPIRE = 1;
    private io.rong.imlib.model.Message firstUnreadMessage;

    private int lastItemBottomOffset = 0;//消息列表最后一项的底部与列表底部的
    private int lastItemPosition = -1; //消息列表最后一项的下标
    private int lastItemHeight = 0; //消息列表最后一项的高度
    private int lastListHeight = 0; //消息列表高度
    private boolean isShowTipMessageCountInBackground = true; //在后台时是否记录并显示消息数提示
    private Conversation mConversation = null;
    private boolean mEnableUnreadMentionMessage;
    // 需要熟悉界面
    private boolean isNeedRefresh;

    // Reference Message
    private ReferenceView referenceView;
    protected ReferenceMessage referenceMessage;
    private MessageItemLongClickAction clickActionReference;
    private View mVoiceInputToggle;
    private ImageView mVoiceToggle;
    private IAddMentionedMemberListener mAddMentionedMemberListener;
    private View contentView;
    //@ 未读消息
    private List<io.rong.imlib.model.Message> mMentionMessages = new ArrayList<>();

    // 当界面显示了 "x条新消息" 时，需要添加该 listener
    private AbsListView.OnScrollListener mOnScrollListener = new AbsListView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {

        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            // 当 ListView 滚动到最上方的一条未读消息时，隐藏 "x条新消息"
            if (mUnreadMsgLayout != null && mUnreadMsgLayout.getVisibility() == View.VISIBLE && firstUnreadMessage != null) {
                int firstPosition = mListAdapter.findPosition(firstUnreadMessage.getMessageId());
                if (firstVisibleItem <= firstPosition) {
                    TranslateAnimation animation = new TranslateAnimation(0, 700, 0, 0);
                    animation.setDuration(700);
                    animation.setFillAfter(true);
                    mUnreadMsgLayout.startAnimation(animation);
                    mUnreadMsgLayout.setClickable(false);
                    mList.removeCurrentOnScrollListener();
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSavedInstanceState = savedInstanceState;
        if (savedInstanceState != null) {
            mNewMessageCount = savedInstanceState.getInt(NEW_MESSAGE_COUNT);
            mListViewState = savedInstanceState.getParcelable(LIST_STATE);
        }
        RLog.i(TAG, "onCreate");
        InternalModuleManager.getInstance().onLoaded();
        try {
            if (getActivity() != null) {
                mEnableMention = getActivity().getResources().getBoolean(R.bool.rc_enable_mentioned_message);
            }
        } catch (Resources.NotFoundException e) {
            RLog.e(TAG, "rc_enable_mentioned_message not found in rc_config.xml");
        }

        try {
            mReadRec = getResources().getBoolean(R.bool.rc_read_receipt);
            mSyncReadStatus = getResources().getBoolean(R.bool.rc_enable_sync_read_status);
            mEnableUnreadMentionMessage = getResources().getBoolean(R.bool.rc_enable_unread_mention);
            DEFAULT_HISTORY_MESSAGE_COUNT = getResources().getInteger(R.integer.rc_conversation_history_message_count);
            DEFAULT_REMOTE_MESSAGE_COUNT = getResources().getInteger(R.integer.rc_conversation_remote_message_count);
            SHOW_UNREAD_MESSAGE_COUNT = getResources().getInteger(R.integer.rc_conversation_show_unread_message_count);
        } catch (Resources.NotFoundException e) {
            RLog.e(TAG, "onCreate rc_read_receipt not found in rc_config.xml", e);
        }

        // 最大值限制为 100
        if (DEFAULT_HISTORY_MESSAGE_COUNT > 100) {
            DEFAULT_HISTORY_MESSAGE_COUNT = 100;
        }

        if (DEFAULT_REMOTE_MESSAGE_COUNT > 100) {
            DEFAULT_REMOTE_MESSAGE_COUNT = 100;
        }

        if (SHOW_UNREAD_MESSAGE_COUNT > 100) {
            SHOW_UNREAD_MESSAGE_COUNT = 100;
        }

        mKitReceiver = new RongKitReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PHONE_STATE");
        try {
            if (getActivity() != null) {
                getActivity().registerReceiver(mKitReceiver, filter);
            }
        } catch (Exception e) {
            RLog.e(TAG, "onCreate", e);
        }
        mAddMentionedMemberListener = new IAddMentionedMemberListener() {
            @Override
            public boolean onAddMentionedMember(UserInfo userInfo, int from) {
                RLog.i("onAddMentionedMember", "from=" + from);
                //@某人的时候 from 等于0
                if (from == 0 && mRongExtension != null) {
                    mRongExtension.showSoftInput();
                }
                return false;
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        contentView = inflater.inflate(R.layout.rc_fr_conversation, container, false);
        mRongExtension = contentView.findViewById(R.id.rc_extension);
        mRongExtension.setFragment(this);
        mVoiceInputToggle = mRongExtension.findViewById(R.id.rc_audio_input_toggle);
        mVoiceToggle = mRongExtension.findViewById(R.id.rc_voice_toggle);

        referenceView = contentView.findViewById(R.id.rc_reference);

        referenceView.setCancelListener(new ReferenceView.CancelListener() {
            @Override
            public void cancelClick() {
                hideReferenceView();
            }
        });

        mOffsetLimit = 70 * getActivity().getResources().getDisplayMetrics().density;

        mMsgListView = findViewById(contentView, R.id.rc_layout_msg_list);
        mList = findViewById(mMsgListView, R.id.rc_list);
        mList.requestDisallowInterceptTouchEvent(true);
        mList.setMode(AutoRefreshListView.Mode.BOTH);
        mListAdapter = onResolveAdapter(getActivity());
        mListAdapter.setMaxMessageSelectedCount(getResources().getInteger(R.integer.rc_max_message_selected_count));
        mList.setAdapter(mListAdapter);

        mList.setOnRefreshListener(new AutoRefreshListView.OnRefreshListener() {
            @Override
            public void onRefreshFromStart() {
                if (mHasMoreLocalMessagesUp) {
                    getHistoryMessage(mConversationType, mTargetId, DEFAULT_HISTORY_MESSAGE_COUNT, AutoRefreshListView.Mode.START, SCROLL_MODE_NORMAL, -1, false);
                } else {
                    getRemoteHistoryMessages(mConversationType, mTargetId, DEFAULT_REMOTE_MESSAGE_COUNT);
                }
            }

            @Override
            public void onRefreshFromEnd() {
                if (mHasMoreLocalMessagesDown && indexMessageTime > 0) {
                    getHistoryMessage(mConversationType, mTargetId, DEFAULT_HISTORY_MESSAGE_COUNT, AutoRefreshListView.Mode.END, SCROLL_MODE_NORMAL, -1, false);
                } else {
                    mList.onRefreshComplete(0, 0, false);
                }
            }
        });

        mList.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //当前会话中无消息，下拉获取远端历史消息时，调用这段逻辑
                if (event.getAction() == MotionEvent.ACTION_MOVE &&
                        mList.getCount() - mList.getHeaderViewsCount() - mList.getFooterViewsCount() == 0) {
                    if (mHasMoreLocalMessagesUp) {
                        getHistoryMessage(mConversationType, mTargetId, DEFAULT_HISTORY_MESSAGE_COUNT, AutoRefreshListView.Mode.START, SCROLL_MODE_NORMAL, -1, false);
                    } else {
                        if (mList.getRefreshState() != AutoRefreshListView.State.REFRESHING) {
                            getRemoteHistoryMessages(mConversationType, mTargetId, DEFAULT_REMOTE_MESSAGE_COUNT);
                        }
                    }
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN && mRongExtension != null) {
                    mRongExtension.collapseExtension();
                }
                return false;
            }
        });

        RongContext rongContext = RongContext.getInstance();
        if (rongContext != null && rongContext.getNewMessageState()) {
            mNewMessageTextView = findViewById(contentView, R.id.rc_new_message_number);
            mNewMessageBtn = findViewById(contentView, R.id.rc_new_message_count);
            mNewMessageBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mList.setSelection(mList.getCount());
                    mNewMessageBtn.setVisibility(View.GONE);
                    mNewMessageTextView.setVisibility(View.GONE);
                    mNewMessageCount = 0;
                    mShouldInsertMsg = true;
                    mListAdapter.clear();
                    AutoRefreshListView.Mode mode = AutoRefreshListView.Mode.START;
                    int scrollMode = SCROLL_MODE_BOTTOM;
                    getHistoryMessage(mConversationType, mTargetId, DEFAULT_HISTORY_MESSAGE_COUNT, mode, scrollMode, -1, false);
                }
            });
        }
        if (rongContext != null && rongContext.getUnreadMessageState()) {
            mUnreadMsgLayout = findViewById(mMsgListView, R.id.rc_unread_message_layout);
            mUnreadMsgCountTv = findViewById(mMsgListView, R.id.rc_unread_message_count);
        }
        mMentionMsgLayout = findViewById(mMsgListView, R.id.rc_mention_message_layout);
        mMentionMsgCountTv = findViewById(mMsgListView, R.id.rc_mention_message_count);
        mList.addOnScrollListener(this);
        mMentionMsgLayout.setOnClickListener(this);
        mListAdapter.setOnItemHandlerListener(new MessageListAdapter.OnItemHandlerListener() {

            @Override
            public boolean onWarningViewClick(final int position, final io.rong.imlib.model.Message data, final View v) {
                RongIMClient.getInstance().deleteMessages(new int[]{data.getMessageId()}, new RongIMClient.ResultCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean aBoolean) {
                        if (aBoolean) {
                            if (data != null) {
                                ResendManager.getInstance().removeResendMessage(data.getMessageId());
                                int mPosition = mListAdapter.findPosition(data.getMessageId());
                                if (mPosition >= 0) {
                                    mListAdapter.remove(mPosition);
                                }
                                data.setMessageId(0);
                                onResendItemClick(data);
                            }
                        }
                    }

                    @Override
                    public void onError(RongIMClient.ErrorCode e) {

                    }
                });
                return true;
            }

            @Override
            public void onReadReceiptStateClick(io.rong.imlib.model.Message message) {
                ConversationFragment.this.onReadReceiptStateClick(message);
            }

            @Override
            public void onMessageClick(int position, io.rong.imlib.model.Message data, View v) {
                if (mRongExtension != null) {
                    mRongExtension.collapseExtension();
                }
            }
        });

        showNewMessage();

        contentView.getViewTreeObserver().addOnGlobalLayoutListener(globalLayoutListener);

        /*
         * 以下逻辑为了处理当消息列表大小发生变化时（输入法弹出，或进入其他全屏界面后再返回）
         * 导致会话列表最后一条消息会被覆盖一部分的情况（假设全屏时显示100%，切回非全屏时只能显示95%，所以最后一条不能全显示）
         */
        mList.getViewTreeObserver().addOnGlobalLayoutListener(listViewLayoutListener);
        return contentView;
    }

    private ViewTreeObserver.OnGlobalLayoutListener globalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {

            Rect r = new Rect();
            contentView.getWindowVisibleDisplayFrame(r);
            int screenHeight = contentView.getRootView().getHeight();

            // r.bottom is the position above soft keypad or device button.
            // if keypad is shown, the r.bottom is smaller than that before.
            int keypadHeight = screenHeight - r.bottom;
            if (keypadHeight > screenHeight * 0.15) { // 0.15 ratio is perhaps enough to determine keypad height.
                contentView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                if (mNewMessageCount > 0 && mNewMessageBtn != null) {
                    mNewMessageCount = 0;
                    mNewMessageBtn.setVisibility(View.GONE);
                    mNewMessageTextView.setVisibility(View.GONE);
                }
            }
        }
    };

    private ViewTreeObserver.OnGlobalLayoutListener listViewLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            if (mList == null) return;

            int height = mList.getHeight();
            // 当列表高度发生变化时，根据原最后一项的下标，高度，底部偏移量进行位置还原
            if (lastListHeight != height) {
                if (lastItemPosition != -1) {
                    mList.setSelectionFromTop(lastItemPosition, height - lastItemHeight + lastItemBottomOffset);
                }
                lastListHeight = height;
            }
        }
    };

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        addReferenceLongClickAction();
        if (showMoreClickItem()) {
            clickAction = new MessageItemLongClickAction.Builder()
                    .title(getResources().getString(R.string.rc_dialog_item_message_more))
                    .actionListener(new MessageItemLongClickAction.MessageItemLongClickListener() {
                        @Override
                        public boolean onMessageItemLongClick(Context context, UIMessage message) {
                            hideReferenceView();
                            setMoreActionState(message);
                            return true;
                        }
                    }).build();
            RongMessageItemLongClickActionManager.getInstance().addMessageItemLongClickAction(clickAction);
        }
    }

    private void addReferenceLongClickAction() {
        clickActionReference = new MessageItemLongClickAction.Builder()
                .titleResId(R.string.rc_reference)
                .actionListener(new MessageItemLongClickAction.MessageItemLongClickListener() {
                    @Override
                    public boolean onMessageItemLongClick(Context context, UIMessage message) {
                        referenceMessage = referenceView.setMessageContent(message);
                        if (referenceMessage != null) {
                            if (mRongExtension != null) {
                                mRongExtension.collapseExtension();
                            }
                            int visibility = mVoiceInputToggle.getVisibility();
                            if (visibility == View.VISIBLE) {
                                mVoiceToggle.performClick();
                            }

                            if (mList != null) {
                                mList.setSelection(mList.getCount());
                            }

                            referenceView.getHandler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (mRongExtension != null) {
                                        mRongExtension.showSoftInput();
                                    }
                                    referenceView.setVisibility(View.VISIBLE);
                                }
                            }, 200);
                        }
                        return true;
                    }
                }).showFilter(new MessageItemLongClickAction.Filter() {
                    @Override
                    public boolean filter(UIMessage message) {
                        //过滤失败消息
                        boolean isSuccess = message.getMessage().getSentStatus() != io.rong.imlib.model.Message.SentStatus.FAILED;
                        boolean forbidConversationType = message.getConversationType().equals(Conversation.ConversationType.ENCRYPTED)
                                || message.getConversationType().equals(Conversation.ConversationType.APP_PUBLIC_SERVICE)
                                || message.getConversationType().equals(Conversation.ConversationType.PUBLIC_SERVICE)
                                || message.getConversationType().equals(Conversation.ConversationType.CUSTOMER_SERVICE);
                        boolean isFireMsg = message.getContent().isDestruct();
                        boolean isFireMode = mRongExtension != null && mRongExtension.isFireStatus();
                        boolean isEnableReferenceMsg = false;
                        if (getContext() != null) {
                            isEnableReferenceMsg = getContext().getResources().getBoolean(R.bool.rc_enable_reference_message);
                        }
                        boolean isInstanceOf = (message.getContent() instanceof TextMessage)
                                || (message.getContent() instanceof ImageMessage)
                                || (message.getContent() instanceof FileMessage)
                                || (message.getContent() instanceof RichContentMessage)
                                || (message.getContent() instanceof ReferenceMessage);
                        return isSuccess && isEnableReferenceMsg && isInstanceOf && !forbidConversationType && !isFireMsg & !isFireMode;
                    }
                })
                .build();
        RongMessageItemLongClickActionManager.getInstance().addMessageItemLongClickAction(clickActionReference);
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollState == SCROLL_STATE_TOUCH_SCROLL) {
            if (mRongExtension != null) mRongExtension.collapseExtension();
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (mList == null) return;

        if (mList.getHeight() == lastListHeight) {
            int childCount = mList.getChildCount();
            if (childCount != 0) {
                /*
                 * 记录消息列表中最后一条消息的底部偏移量，消息项的高度，该消息在列表中的下标
                 * 用于当消息列表高度发生变化时正确的还原最后一条消息的位置
                 */
                View lastView = mList.getChildAt(childCount - 1);
                lastItemBottomOffset = lastView.getBottom() - lastListHeight;
                lastItemHeight = lastView.getHeight();
                lastItemPosition = firstVisibleItem + visibleItemCount - 1;
            }
        }
        if (mEnableUnreadMentionMessage && mMentionMsgLayout != null && mMentionMessages != null && mMentionMessages.size() > 0 && mListAdapter.getCount() > 0) {
            int firstPosition = firstVisibleItem - mList.getHeaderViewsCount();
            int lastPosition = firstPosition + visibleItemCount;
            UIMessage firstMessage = mListAdapter.getItem(Math.max(firstPosition, 0));
            UIMessage lastMessage = mListAdapter.getItem(lastPosition < mListAdapter.getCount() ? lastPosition : mListAdapter.getCount() - 1);
            long topTime = firstMessage.getSentTime();
            long bottomTime = lastMessage.getSentTime();
            Iterator<io.rong.imlib.model.Message> iterator = mMentionMessages.iterator();
            while (iterator.hasNext()) {
                io.rong.imlib.model.Message next = iterator.next();
                if (next.getSentTime() >= topTime && next.getSentTime() <= bottomTime) {
                    iterator.remove();
                }
            }
        }
        processMentionLayout();
        int last = mList.getLastVisiblePosition();
        if (mNewMessageBtn != null && last >= mList.getCount() - 1) {
            mNewMessageCount = 0;
            mNewMessageBtn.setVisibility(View.GONE);
            mNewMessageTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(UN_READ_COUNT, mUnReadCount);
        outState.putInt(NEW_MESSAGE_COUNT, mNewMessageCount);
        outState.putParcelable(LIST_STATE, mList.onSaveInstanceState());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        if (!getActivity().isFinishing() && mRongExtension != null) {
            RLog.d(TAG, "onResume when back from other activity.");
            mRongExtension.resetEditTextLayoutDrawnStatus();

            SharedPreferences sp = SharedPreferencesUtils.get(getContext(), KitCommonDefine.RONG_KIT_SP_CONFIG, Context.MODE_PRIVATE);
            long sendReadReceiptTime = sp.getLong(getSavedReadReceiptTimeName(), 0);
            if (sendReadReceiptTime > 0) {
                RongIMClient.getInstance().sendReadReceiptMessage(mConversationType, mTargetId, sendReadReceiptTime, null);
            }
        }
        if (getResources().getBoolean(R.bool.rc_wipe_out_notification_message)) {
            RongPushClient.clearAllNotifications(getActivity());
        }

        if (isNeedRefresh && mListAdapter != null) {
            isNeedRefresh = false;
            mListAdapter.notifyDataSetChanged();
        }
        super.onResume();
    }

    @Override
    final public void getUserInfo(String userId, UserInfoCallback callback) {
        UserInfo userInfo = RongUserInfoManager.getInstance().getUserInfo(userId);
        if (userInfo != null) {
            callback.onGotUserInfo(userInfo);
        }
    }

    public void setMoreActionStateListener(OnMoreActionStateListener moreActionStateListener) {
        this.moreActionStateListener = moreActionStateListener;
    }

    /**
     * 显示多选状态
     *
     * @param message UIMessage
     */
    public void setMoreActionState(UIMessage message) {
        for (int i = 0; i < mListAdapter.getCount(); ++i) {
            mListAdapter.getItem(i).setChecked(false);
        }
        mListAdapter.setMessageCheckedChanged(new MessageListAdapter.OnMessageCheckedChanged() {
            @Override
            public void onCheckedEnable(boolean enable) {
                if (mRongExtension != null) {
                    mRongExtension.setMoreActionEnable(enable);
                }
            }
        });
        mListAdapter.setSelectedCountDidExceed(new MessageListAdapter.OnSelectedCountDidExceed() {
            @Override
            public void onSelectedCountDidExceed() {
                messageSelectedCountDidExceed();
            }
        });
        mRongExtension.showMoreActionLayout(getMoreClickActions());
        mListAdapter.setShowCheckbox(true);
        message.setChecked(true);
        mListAdapter.notifyDataSetChanged();
        if (moreActionStateListener != null) {
            moreActionStateListener.onShownMoreActionLayout();
        }
    }

    /**
     * 重置多选状态，隐藏多选框，隐藏底部点击事件
     */
    public void resetMoreActionState() {
        mRongExtension.hideMoreActionLayout();
        mListAdapter.setShowCheckbox(false);
        mListAdapter.notifyDataSetChanged();
        if (moreActionStateListener != null) {
            moreActionStateListener.onHiddenMoreActionLayout();
        }
    }

    /**
     * 获取点击更多时，底部的动作
     * 开发者重写此方法，返回自定义的点击动作
     *
     * @return 点击动作
     */
    public List<IClickActions> getMoreClickActions() {
        List<IClickActions> actions = new ArrayList<>();

        // 只有群聊和单聊支持转发
        if ((mConversationType == Conversation.ConversationType.GROUP
                || mConversationType == Conversation.ConversationType.PRIVATE)
                && getContext() != null
                && getContext().getResources().getBoolean(R.bool.rc_enable_send_combine_message)) {
            actions.add(new ForwardClickActions());
        }

        actions.add(new DeleteClickActions());
        return actions;
    }

    /**
     * 获取选中的消息
     *
     * @return 选中的消息列表
     */
    public List<io.rong.imlib.model.Message> getCheckedMessages() {
        if (mListAdapter != null) {
            return mListAdapter.getCheckedMessage();
        }
        return null;
    }


    /**
     * 设置多选时，最大可选消息数量，默认都可选，如果设置了改值，超过改值，则会回调{@link #messageSelectedCountDidExceed()}
     * ,需要在{@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}之后调用
     *
     * @param maxMessageSelectedCount 最大可选数。
     */
    public void setMaxMessageSelectedCount(int maxMessageSelectedCount) {
        if (mListAdapter != null) {
            mListAdapter.setMaxMessageSelectedCount(maxMessageSelectedCount);
        }
    }

    /**
     * 设置多选时，超过最大可选数量回调。
     */
    protected void messageSelectedCountDidExceed() {
        Toast.makeText(getActivity(), R.string.rc_exceeded_max_limit_100, Toast.LENGTH_SHORT).show();
    }

    /**
     * 是否在长按时显示更多选项
     *
     * @return true 显示，false 不显示。
     */
    public boolean showMoreClickItem() {
        return false;
    }

    /**
     * 是否显示"以上是历史消息"
     *
     * @return true 显示，false 不显示。
     */
    public boolean showAboveIsHistoryMessage() {
        return true;
    }

    /**
     * 提供 ListView 的 Adapter 适配器。
     * 使用时，需要继承 {@link ConversationFragment} 并重写此方法。
     * 注意：提供的适配器，要继承自 {@link MessageListAdapter}
     *
     * @return 适配器
     */
    public MessageListAdapter onResolveAdapter(Context context) {
        return new MessageListAdapter(context);
    }

    @Override
    protected void initFragment(final Uri uri) {
        indexMessageTime = getActivity().getIntent().getLongExtra("indexMessageTime", 0);
        RLog.d(TAG, "initFragment : " + uri + ",this=" + this + ", time = " + indexMessageTime);
        if (uri != null) {
            String typeStr = uri.getLastPathSegment().toUpperCase(Locale.US);
            mConversationType = Conversation.ConversationType.valueOf(typeStr);
            mTargetId = uri.getQueryParameter("targetId");

            //优先设置 Extension 会话属性
            mRongExtension.setConversation(mConversationType, mTargetId);
            RongIMClient.getInstance().getTextMessageDraft(mConversationType, mTargetId, new RongIMClient.ResultCallback<String>() {
                @Override
                public void onSuccess(String s) {
                    DraftHelper draftHelper = new DraftHelper(s);
                    mDraft = draftHelper.decode();
                    if (mRongExtension != null) {
                        if (!TextUtils.isEmpty(mDraft)) {
                            EditText editText = mRongExtension.getInputEditText();
                            editText.setText(mDraft);
                            draftHelper.restoreMentionInfo();
                            editText.setSelection(editText.length());
                            editText.requestFocus();
                        }
                        mRongExtension.setExtensionClickListener(ConversationFragment.this);
                    }
                }

                @Override
                public void onError(RongIMClient.ErrorCode e) {
                    if (mRongExtension != null) {
                        mRongExtension.setExtensionClickListener(ConversationFragment.this);
                    }
                }
            });

            mCurrentConversationInfo = ConversationInfo.obtain(mConversationType, mTargetId);
            RongContext.getInstance().registerConversationInfo(mCurrentConversationInfo);
            mNotificationContainer = mMsgListView.findViewById(R.id.rc_notification_container);

            if (mConversationType.equals(Conversation.ConversationType.CUSTOMER_SERVICE)
                    && getActivity() != null
                    && getActivity().getIntent() != null
                    && getActivity().getIntent().getData() != null) {
                mCustomUserInfo = getActivity().getIntent().getParcelableExtra("customServiceInfo");
            }
            Method method1;
            Method method2;
            Method method3;
            Object obj;
            try {
                Class<?> cls;
                if (getActivity() != null && getActivity().getResources().getBoolean(R.bool.rc_location_2D)) {
                    cls = Class.forName("io.rong.imkit.plugin.location.LocationManager2D");
                    obj = io.rong.imkit.plugin.location.LocationManager2D.getInstance();
                } else {
                    cls = Class.forName("io.rong.imkit.plugin.location.LocationManager");
                    obj = io.rong.imkit.plugin.location.LocationManager.getInstance();
                }
                method1 = cls.getMethod("bindConversation", new Class[]{Context.class, Conversation.ConversationType.class, String.class});
                method2 = cls.getMethod("setUserInfoProvider", new Class[]{IUserInfoProvider.class});
                method3 = cls.getMethod("setParticipantChangedListener", new Class[]{IRealTimeLocationStateListener.class});

                method1.invoke(obj, getActivity(), mConversationType, mTargetId);
                method2.invoke(obj, this);
                method3.invoke(obj, new IRealTimeLocationStateListener() {

                    private View mRealTimeBar;
                    private TextView mRealTimeText;

                    @Override
                    public void onParticipantChanged(List<String> userIdList) {
                        if (ConversationFragment.this.isDetached()) {
                            return;
                        }
                        if (mRealTimeBar == null) {
                            mRealTimeBar = inflateNotificationView(R.layout.rc_notification_realtime_location);
                            mRealTimeText = mRealTimeBar.findViewById(R.id.real_time_location_text);
                            mRealTimeBar.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {

                                    String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};

                                    if (!PermissionCheckUtil.checkPermissions(getContext(), permissions)) {
                                        PermissionCheckUtil.requestPermissions(ConversationFragment.this.getActivity(), permissions, REQUEST_CODE_ASK_PERMISSIONS);
                                        return;
                                    }

                                    RealTimeLocationConstant.RealTimeLocationStatus status = RongIMClient.getInstance().getRealTimeLocationCurrentState(mConversationType, mTargetId);
                                    if (status == RealTimeLocationConstant.RealTimeLocationStatus.RC_REAL_TIME_LOCATION_STATUS_INCOMING) {
                                        PromptPopupDialog dialog = PromptPopupDialog.newInstance(ConversationFragment.this.getActivity(), "", getResources().getString(R.string.rc_real_time_join_notification));
                                        dialog.setPromptButtonClickedListener(new PromptPopupDialog.OnPromptButtonClickedListener() {
                                            @Override
                                            public void onPositiveButtonClicked() {
                                                int result;
                                                if (getActivity() != null && getActivity().getResources().getBoolean(R.bool.rc_location_2D)) {
                                                    result = io.rong.imkit.plugin.location.LocationManager2D.getInstance().joinLocationSharing();
                                                } else {
                                                    result = io.rong.imkit.plugin.location.LocationManager.getInstance().joinLocationSharing();
                                                }
                                                if (result == 0) {
                                                    Intent intent;
                                                    if (getActivity() != null && getActivity().getResources().getBoolean(R.bool.rc_location_2D)) {
                                                        intent = new Intent(ConversationFragment.this.getActivity(), io.rong.imkit.plugin.location.AMapRealTimeActivity2D.class);
                                                    } else {
                                                        intent = new Intent(ConversationFragment.this.getActivity(), io.rong.imkit.plugin.location.AMapRealTimeActivity.class);
                                                    }
                                                    if (mLocationShareParticipants != null) {
                                                        intent.putStringArrayListExtra("participants", (ArrayList<String>) mLocationShareParticipants);
                                                    }
                                                    startActivity(intent);
                                                } else if (result == 1) {
                                                    Toast.makeText(getActivity(), R.string.rc_network_exception, Toast.LENGTH_SHORT).show();
                                                } else if ((result == 2)) {
                                                    Toast.makeText(getActivity(), R.string.rc_location_sharing_exceed_max, Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                                        dialog.show();
                                    } else {
                                        //Intent intent = new Intent(ConversationFragment.this.getActivity(), AMapRealTimeActivity.class);
                                        Intent intent;
                                        if (getActivity() != null && getActivity().getResources().getBoolean(R.bool.rc_location_2D)) {
                                            intent = new Intent(ConversationFragment.this.getActivity(), io.rong.imkit.plugin.location.AMapRealTimeActivity2D.class);
                                        } else {
                                            intent = new Intent(ConversationFragment.this.getActivity(), io.rong.imkit.plugin.location.AMapRealTimeActivity.class);
                                        }
                                        if (mLocationShareParticipants != null) {
                                            intent.putStringArrayListExtra("participants", (ArrayList<String>) mLocationShareParticipants);
                                        }
                                        startActivity(intent);
                                    }
                                }
                            });
                        }
                        mLocationShareParticipants = userIdList;
                        if (userIdList != null) {
                            if (userIdList.size() == 0) {
                                hideNotificationView(mRealTimeBar);
                            } else {
                                if (userIdList.size() == 1 && userIdList.contains(RongIM.getInstance().getCurrentUserId())) {
                                    mRealTimeText.setText(getResources().getString(R.string.rc_you_are_sharing_location));
                                } else if (userIdList.size() == 1 && !userIdList.contains(RongIM.getInstance().getCurrentUserId())) {
                                    mRealTimeText.setText(String.format(getResources().getString(R.string.rc_other_is_sharing_location), getNameFromCache(userIdList.get(0))));
                                } else {
                                    mRealTimeText.setText(String.format(getResources().getString(R.string.rc_others_are_sharing_location), userIdList.size()));
                                }
                                showNotificationView(mRealTimeBar);
                            }
                        } else {
                            hideNotificationView(mRealTimeBar);
                        }
                    }

                    @Override
                    public void onErrorException() {
                        if (!ConversationFragment.this.isDetached()) {
                            hideNotificationView(mRealTimeBar);
                            if (mLocationShareParticipants != null) {
                                mLocationShareParticipants.clear();
                                mLocationShareParticipants = null;
                            }
                        }
                    }
                });
            } catch (Exception e) {
                RLog.e(TAG, "Exception :", e);
            } catch (Throwable throwable) {
                RLog.e(TAG, "Throwable :", throwable);
            }

            if (mConversationType.equals(Conversation.ConversationType.CHATROOM)) {
                boolean createIfNotExist = isActivityExist() && getActivity().getIntent().getBooleanExtra("createIfNotExist", true);
                int pullCount = getResources().getInteger(R.integer.rc_chatroom_first_pull_message_count);
                if (createIfNotExist)
                    RongIMClient.getInstance().joinChatRoom(mTargetId, pullCount, new RongIMClient.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            RLog.i(TAG, "joinChatRoom onSuccess : " + mTargetId);
                        }

                        @Override
                        public void onError(RongIMClient.ErrorCode errorCode) {
                            RLog.e(TAG, "joinChatRoom onError : " + errorCode);
                            if (isActivityExist()) {
                                if (errorCode == RongIMClient.ErrorCode.RC_NET_UNAVAILABLE || errorCode == RongIMClient.ErrorCode.RC_NET_CHANNEL_INVALID) {
                                    onWarningDialog(getString(R.string.rc_notice_network_unavailable));
                                } else {
                                    onWarningDialog(getString(R.string.rc_join_chatroom_failure));
                                }
                            }
                        }
                    });
                else
                    RongIMClient.getInstance().joinExistChatRoom(mTargetId, pullCount, new RongIMClient.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            RLog.i(TAG, "joinExistChatRoom onSuccess : " + mTargetId);
                        }

                        @Override
                        public void onError(RongIMClient.ErrorCode errorCode) {
                            RLog.e(TAG, "joinExistChatRoom onError : " + errorCode);
                            if (isActivityExist()) {
                                if (errorCode == RongIMClient.ErrorCode.RC_NET_UNAVAILABLE || errorCode == RongIMClient.ErrorCode.RC_NET_CHANNEL_INVALID) {
                                    onWarningDialog(getString(R.string.rc_notice_network_unavailable));
                                } else {
                                    onWarningDialog(getString(R.string.rc_join_chatroom_failure));
                                }
                            }
                        }
                    });
            } else if (mConversationType == Conversation.ConversationType.APP_PUBLIC_SERVICE ||
                    mConversationType == Conversation.ConversationType.PUBLIC_SERVICE) {
                PublicServiceCommandMessage msg = new PublicServiceCommandMessage();
                msg.setCommand(PublicServiceMenu.PublicServiceMenuItemType.Entry.getMessage());
                io.rong.imlib.model.Message message = io.rong.imlib.model.Message.obtain(mTargetId, mConversationType, msg);
                RongIMClient.getInstance().sendMessage(message, null, null, new IRongCallback.ISendMessageCallback() {
                    @Override
                    public void onAttached(io.rong.imlib.model.Message message) {

                    }

                    @Override
                    public void onSuccess(io.rong.imlib.model.Message message) {

                    }

                    @Override
                    public void onError(io.rong.imlib.model.Message message, RongIMClient.ErrorCode errorCode) {

                    }
                });
                Conversation.PublicServiceType publicServiceType;
                if (mConversationType == Conversation.ConversationType.PUBLIC_SERVICE) {
                    publicServiceType = Conversation.PublicServiceType.PUBLIC_SERVICE;
                } else {
                    publicServiceType = Conversation.PublicServiceType.APP_PUBLIC_SERVICE;
                }
                getPublicServiceProfile(publicServiceType, mTargetId);
            } else if (mConversationType.equals(Conversation.ConversationType.CUSTOMER_SERVICE)) {
                onStartCustomService(mTargetId);
            } else if (mEnableMention
                    && (mConversationType.equals(Conversation.ConversationType.DISCUSSION)
                    || mConversationType.equals(Conversation.ConversationType.GROUP))) {
                RongMentionManager.getInstance().createInstance(mConversationType, mTargetId, mRongExtension.getInputEditText());
                RongMentionManager.getInstance().setAddMentionedMemberListener(mAddMentionedMemberListener);
            }
        }
        RongIMClient.getInstance().getConversation(mConversationType, mTargetId, new RongIMClient.ResultCallback<Conversation>() {
            @Override
            public void onSuccess(final Conversation conversation) {
                mConversation = conversation;
                if (conversation != null && isActivityExist()) {
                    if (mSavedInstanceState != null) { // 旋转屏幕后，读取旋转屏幕前的数据
                        mUnReadCount = mSavedInstanceState.getInt(UN_READ_COUNT);
                    } else {
                        mUnReadCount = conversation.getUnreadMessageCount();
                    }
                    conversationUnreadCount = mUnReadCount;
                    boolean sendReadReceiptFailed = false;
                    if (getActivity() != null && isAdded()) {
                        SharedPreferences sp = SharedPreferencesUtils.get(getContext(), KitCommonDefine.RONG_KIT_SP_CONFIG, Context.MODE_PRIVATE);
                        sendReadReceiptFailed = sp.getBoolean(getSavedReadReceiptStatusName(), false);
                    }

                    if (mUnReadCount > 0 || sendReadReceiptFailed) {
                        boolean isPrivate = mConversationType.equals(Conversation.ConversationType.PRIVATE)
                                && RongContext.getInstance().isReadReceiptConversationType(Conversation.ConversationType.PRIVATE);
                        boolean isEncrypted = mConversationType.equals(Conversation.ConversationType.ENCRYPTED)
                                && RongContext.getInstance().isReadReceiptConversationType(Conversation.ConversationType.ENCRYPTED);
                        if (mReadRec && (isPrivate || isEncrypted)) {
                            RongIMClient.getInstance().sendReadReceiptMessage(mConversationType, mTargetId,
                                    conversation.getSentTime(), new IRongCallback.ISendMessageCallback() {
                                        @Override
                                        public void onAttached(io.rong.imlib.model.Message message) {

                                        }

                                        @Override
                                        public void onSuccess(io.rong.imlib.model.Message message) {
                                            removeSendReadReceiptStatusToSp();
                                        }

                                        @Override
                                        public void onError(io.rong.imlib.model.Message message, RongIMClient.ErrorCode errorCode) {
                                            RLog.e(TAG, "sendReadReceiptMessage errorCode = " + errorCode.getValue());
                                            saveSendReadReceiptStatusToSp(true, conversation.getSentTime());
                                        }
                                    });
                        }

                        if (mSyncReadStatus && ((!mReadRec && mConversationType == Conversation.ConversationType.PRIVATE)
                                || mConversationType == Conversation.ConversationType.GROUP
                                || mConversationType == Conversation.ConversationType.DISCUSSION
                                || mConversationType == Conversation.ConversationType.PUBLIC_SERVICE)) {
                            RongIMClient.getInstance().syncConversationReadStatus(mConversationType, mTargetId, conversation.getSentTime(), null);
                        }
                    }
                    RongIMClient.getInstance().getTheFirstUnreadMessage(mConversationType, mTargetId, new RongIMClient.ResultCallback<io.rong.imlib.model.Message>() {
                        @Override
                        public void onSuccess(io.rong.imlib.model.Message message) {
                            firstUnreadMessage = message;
                            if (mUnReadCount > SHOW_UNREAD_MESSAGE_COUNT && mUnreadMsgLayout != null
                                    && firstUnreadMessage != null) {
                                showUnreadMsgLayout();
                            }
                            refreshUnreadUI();
                            if (conversation.getMentionedCount() > 0) {
                                getMentionedMessage(mConversationType, mTargetId);
                            } else {
                                RongIM.getInstance().clearMessagesUnreadStatus(mConversationType, mTargetId, null);
                            }
                        }

                        @Override
                        public void onError(RongIMClient.ErrorCode e) {
                            RongIM.getInstance().clearMessagesUnreadStatus(mConversationType, mTargetId, null);
                        }
                    });

                    if (mUnReadCount > SHOW_UNREAD_MESSAGE_COUNT && mUnreadMsgLayout != null) {
                        if (mUnReadCount > 99) {
                            mUnreadMsgCountTv.setText(String.format("%s%s", "99+", getActivity().getResources().getString(R.string.rc_new_messages)));
                        } else {
                            mUnreadMsgCountTv.setText(String.format("%s%s", mUnReadCount, getActivity().getResources().getString(R.string.rc_new_messages)));
                        }
                        mUnreadMsgLayout.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mUnreadMsgLayout.setClickable(false);
                                mList.removeOnScrollListener(mOnScrollListener);
                                TranslateAnimation animation = new TranslateAnimation(0, 500, 0, 0);
                                animation.setDuration(500);
                                mUnreadMsgLayout.startAnimation(animation);
                                animation.setFillAfter(true);
                                animation.setAnimationListener(new Animation.AnimationListener() {
                                    @Override
                                    public void onAnimationStart(Animation animation) {

                                    }

                                    @Override
                                    public void onAnimationEnd(Animation animation) {
                                        int position;
                                        if (firstUnreadMessage == null) {
                                            RLog.e(TAG, "firstUnreadMessage is null");
                                            return;
                                        }
                                        indexMessageTime = firstUnreadMessage.getSentTime();
                                        position = mListAdapter.findPosition(firstUnreadMessage.getMessageId());
                                        if (position == 0) {
                                            mList.setSelection(position);
                                        } else if (position > 0) {
                                            mList.setSelection(position - 1);
                                        } else {
                                            isClickUnread = true;
                                            mListAdapter.clear();
                                            mShouldInsertMsg = false;
                                            getHistoryMessage(mConversationType, mTargetId, DEFAULT_HISTORY_MESSAGE_COUNT,
                                                    AutoRefreshListView.Mode.END, SCROLL_MODE_TOP, firstUnreadMessage.getMessageId(), false);
                                        }
                                    }

                                    @Override
                                    public void onAnimationRepeat(Animation animation) {

                                    }
                                });
                            }
                        });
                    }
                }
            }

            @Override
            public void onError(RongIMClient.ErrorCode e) {

            }
        });
        AutoRefreshListView.Mode mode = indexMessageTime > 0 ? AutoRefreshListView.Mode.END : AutoRefreshListView.Mode.START;
        int scrollMode = indexMessageTime > 0 ? SCROLL_MODE_NORMAL : SCROLL_MODE_BOTTOM;
        getHistoryMessage(mConversationType, mTargetId, DEFAULT_HISTORY_MESSAGE_COUNT, mode, scrollMode, -1, false);
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    private void showUnreadMsgLayout() {
        TranslateAnimation translateAnimation = new TranslateAnimation(300, 0, 0, 0);
        AlphaAnimation alphaAnimation = new AlphaAnimation(0, 1);
        translateAnimation.setDuration(1000);
        alphaAnimation.setDuration(2000);
        AnimationSet set = new AnimationSet(true);
        set.addAnimation(translateAnimation);
        set.addAnimation(alphaAnimation);
        if (mUnreadMsgLayout != null) {
            mUnreadMsgLayout.setVisibility(View.VISIBLE);
            mUnreadMsgLayout.startAnimation(set);
        }
    }

    private void processMentionLayout() {
        if (mEnableUnreadMentionMessage && mMentionMsgLayout != null) {
            if (mMentionMessages.size() > 0) {
                mMentionMsgLayout.setVisibility(View.VISIBLE);
                mMentionMsgCountTv.setText(getContext().getResources().getString(R.string.rc_mention_messages, mMentionMessages.size()));
            } else {
                mMentionMsgLayout.setVisibility(View.GONE);
            }
        }
    }

    /**
     * 公众服务账号名称，头像信息和公众服务号菜单
     *
     * @param publicServiceType 公众服务账号类型
     * @param publicServiceId   公众服务账号id
     */
    public void getPublicServiceProfile(Conversation.PublicServiceType publicServiceType, String publicServiceId) {
        RongIM.getInstance().getPublicServiceProfile(publicServiceType, mTargetId, new RongIMClient.ResultCallback<PublicServiceProfile>() {
            @Override
            public void onSuccess(PublicServiceProfile publicServiceProfile) {
                updatePublicServiceMenu(publicServiceProfile);
            }

            @Override
            public void onError(RongIMClient.ErrorCode e) {

            }
        });
    }

    protected void updatePublicServiceMenu(PublicServiceProfile publicServiceProfile) {
        if (publicServiceProfile == null) {
            RLog.e(TAG, "updatePublicServiceMenu publicServiceProfile is null!");
            return;
        }
        List<InputMenu> inputMenuList = new ArrayList<>();
        PublicServiceMenu menu = publicServiceProfile.getMenu();
        List<PublicServiceMenuItem> items = menu != null ? menu.getMenuItems() : null;
        if (items != null && mRongExtension != null) {
            mPublicServiceProfile = publicServiceProfile;
            for (PublicServiceMenuItem item : items) {
                InputMenu inputMenu = new InputMenu();
                inputMenu.title = item.getName();
                inputMenu.subMenuList = new ArrayList<>();
                for (PublicServiceMenuItem i : item.getSubMenuItems()) {
                    inputMenu.subMenuList.add(i.getName());
                }
                inputMenuList.add(inputMenu);
            }
            mRongExtension.setInputMenu(inputMenuList, true);
        }
    }

    /**
     * 隐藏调用showNotificationView所显示的通知view
     *
     * @param notificationView 通知栏 view
     */
    public void hideNotificationView(View notificationView) {
        if (notificationView == null) {
            return;
        }
        View view = mNotificationContainer.findViewById(notificationView.getId());
        if (view != null) {
            mNotificationContainer.removeView(view);
            if (mNotificationContainer.getChildCount() == 0) {
                mNotificationContainer.setVisibility(View.GONE);
            }
        }
    }

    /**
     * 在通知区域显示一个view
     */
    public void showNotificationView(View notificationView) {
        if (notificationView == null) {
            return;
        }
        View view = mNotificationContainer.findViewById(notificationView.getId());
        if (view != null) {
            // do nothing, we already add the view, and the view would update automatically
            return;
        }
        mNotificationContainer.addView(notificationView);
        mNotificationContainer.setVisibility(View.VISIBLE);
    }

    /**
     * 用来生成需要显示在会话界面的通知view
     *
     * @return
     */
    public View inflateNotificationView(@LayoutRes int layout) {
        return LayoutInflater.from(getActivity()).inflate(layout, mNotificationContainer, false);
    }

    /**
     * 点击重发按钮重新发送消息
     * 如果图片消息或文件消息需要上传到开发者指定的服务器，可以通过集成ConversaitonFragment重写此方法
     *
     * @param message 消息
     */
    public void onResendItemClick(io.rong.imlib.model.Message message) {
        if (message.getContent() instanceof ImageMessage) {
            ImageMessage imageMessage = (ImageMessage) message.getContent();
            if (imageMessage.getRemoteUri() != null && !imageMessage.getRemoteUri().toString().startsWith("file")) {
                RongIM.getInstance().sendMessage(message, null, null, (IRongCallback.ISendMediaMessageCallback) null);
            } else {
                RongIM.getInstance().sendImageMessage(message, null, null, (RongIMClient.SendImageMessageCallback) null);
            }
        } else if (message.getContent() instanceof LocationMessage) {
            RongIM.getInstance().sendLocationMessage(message, null, null, null);
        } else if (message.getContent() instanceof ReferenceMessage) {
            RongIM.getInstance().sendMessage(message, null, null, (IRongCallback.ISendMessageCallback) null);
        } else if (message.getContent() instanceof MediaMessageContent) {
            MediaMessageContent mediaMessageContent = (MediaMessageContent) message.getContent();
            if (mediaMessageContent.getMediaUrl() != null) {
                RongIM.getInstance().sendMessage(message, null, null, (IRongCallback.ISendMediaMessageCallback) null);
            } else {
                RongIM.getInstance().sendMediaMessage(message, null, null, (IRongCallback.ISendMediaMessageCallback) null);
            }
        } else {
            RongIM.getInstance().sendMessage(message, null, null, (IRongCallback.ISendMessageCallback) null);
        }
    }

    /**
     * 回执详情按钮点击事件.
     * 用户可以通过集成ConversaitonFragment然后重写此方法的方式自定义
     *
     * @param message 消息
     */
    public void onReadReceiptStateClick(io.rong.imlib.model.Message message) {

    }

    /**
     * 如果客服后台有分组,会弹出此对话框选择分组
     * 可以通过自定义类继承自 ConversationFragment 并重写此方法来自定义弹窗
     */
    public void onSelectCustomerServiceGroup(final List<CSGroupItem> groupList) {
        if (!isActivityExist()) {
            RLog.w(TAG, "onSelectCustomerServiceGroup Activity has finished");
            return;
        }
        if (getActivity() == null) {
            return;
        }
        final SingleChoiceDialog singleChoiceDialog;
        List<String> singleDataList = new ArrayList<>();
        singleDataList.clear();
        for (int i = 0; i < groupList.size(); i++) {
            if (groupList.get(i).getOnline()) {
                singleDataList.add(groupList.get(i).getName());
            }
        }
        if (singleDataList.size() == 0) {
            RongIMClient.getInstance().selectCustomServiceGroup(mTargetId, null);
            return;
        }
        singleChoiceDialog = new SingleChoiceDialog(getActivity(), singleDataList);
        singleChoiceDialog.setTitle(getActivity().getResources().getString(R.string.rc_cs_select_group));
        singleChoiceDialog.setOnOKButtonListener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int selItem = singleChoiceDialog.getSelectItem();
                RongIMClient.getInstance().selectCustomServiceGroup(mTargetId, groupList.get(selItem).getId());
            }

        });
        singleChoiceDialog.setOnCancelButtonListener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                RongIMClient.getInstance().selectCustomServiceGroup(mTargetId, null);
            }
        });
        singleChoiceDialog.show();
    }

    private boolean robotType = true;
    private long csEnterTime;
    private boolean csEvaluate = true;


    ICustomServiceListener customServiceListener = new ICustomServiceListener() {
        @Override
        public void onSuccess(CustomServiceConfig config) {
            mCustomServiceConfig = config;

            if (config.isBlack) {
                onCustomServiceWarning(getString(R.string.rc_blacklist_prompt), false, robotType);
            }
            if (config.robotSessionNoEva) {
                csEvaluate = false;
                mListAdapter.setEvaluateForRobot(true);
            }

            if (mRongExtension != null) {
                if (config.evaEntryPoint.equals(CustomServiceConfig.CSEvaEntryPoint.EVA_EXTENSION) && mCustomServiceConfig != null) {
                    mRongExtension.addPlugin(new EvaluatePlugin(mCustomServiceConfig.isReportResolveStatus));
                }

                if (config.isDisableLocation) {
                    List<IPluginModule> defaultPlugins = mRongExtension.getPluginModules();
                    IPluginModule location = null;
                    for (int i = 0; i < defaultPlugins.size(); i++) {
                        if (defaultPlugins.get(i) instanceof DefaultLocationPlugin) {
                            location = defaultPlugins.get(i);
                        }
                    }
                    if (location != null) {
                        mRongExtension.removePlugin(location);
                    }
                }
            }

            if (config.quitSuspendType.equals(CustomServiceConfig.CSQuitSuspendType.NONE) && getActivity() != null) {
                try {
                    mCSNeedToQuit = getActivity().getResources().getBoolean(R.bool.rc_stop_custom_service_when_quit);
                } catch (Exception e) {
                    RLog.e(TAG, "customServiceListener onSuccess", e);
                }
            } else {
                mCSNeedToQuit = config.quitSuspendType.equals(CustomServiceConfig.CSQuitSuspendType.SUSPEND);
            }

            for (int i = 0; i < mListAdapter.getCount(); i++) {
                UIMessage uiMessage = mListAdapter.getItem(i);
                if (uiMessage.getContent() instanceof CSPullLeaveMessage) {
                    uiMessage.setCsConfig(config);
                }
            }
            mListAdapter.notifyDataSetChanged();
            if (!TextUtils.isEmpty(config.announceMsg)) {
                onShowAnnounceView(config.announceMsg, config.announceClickUrl);
            }
        }

        @Override
        public void onError(int code, String msg) {
            onCustomServiceWarning(msg, false, robotType);
        }

        @Override
        public void onModeChanged(CustomServiceMode mode) {
            if (mRongExtension == null || !isActivityExist()) {
                return;
            }
            mRongExtension.setExtensionBarMode(mode);
            if (mode.equals(CustomServiceMode.CUSTOM_SERVICE_MODE_HUMAN)
                    || mode.equals(CustomServiceMode.CUSTOM_SERVICE_MODE_HUMAN_FIRST)) {
                if (mCustomServiceConfig != null && mCustomServiceConfig.userTipTime > 0 && !TextUtils.isEmpty(mCustomServiceConfig.userTipWord)) {
                    startTimer(CS_HUMAN_MODE_CUSTOMER_EXPIRE, mCustomServiceConfig.userTipTime * 60 * 1000);
                }
                if (mCustomServiceConfig != null && mCustomServiceConfig.adminTipTime > 0 && !TextUtils.isEmpty(mCustomServiceConfig.adminTipWord)) {
                    startTimer(CS_HUMAN_MODE_SEAT_EXPIRE, mCustomServiceConfig.adminTipTime * 60 * 1000);
                }

                robotType = false;
                csEvaluate = true;
            } else if (mode.equals(CustomServiceMode.CUSTOM_SERVICE_MODE_NO_SERVICE)) {
                csEvaluate = false;
            }
        }

        @Override
        public void onQuit(String msg) {
            RLog.i(TAG, "CustomService onQuit.");
            if (getActivity() == null) {
                return;
            }
            if (mCustomServiceConfig != null && mCustomServiceConfig.evaEntryPoint.equals(CustomServiceConfig.CSEvaEntryPoint.EVA_END)
                    && !robotType) {
                csQuitEvaluate(msg);
            } else {
                csQuit(msg);
            }
        }

        @Override
        public void onPullEvaluation(String dialogId) {
            if (mEvaluateDialg == null) {
                onCustomServiceEvaluation(true, dialogId, robotType, csEvaluate);
            }
        }

        @Override
        public void onSelectGroup(List<CSGroupItem> groups) {
            onSelectCustomerServiceGroup(groups);
        }
    };

    private void csQuit(String msg) {
        if (getHandler() != null) {
            getHandler().removeCallbacksAndMessages(null);
        }
        if (mEvaluateDialg == null) {
            if (mCustomServiceConfig != null) {
                onCustomServiceWarning(msg, mCustomServiceConfig.quitSuspendType == CustomServiceConfig.CSQuitSuspendType.NONE, robotType);
            }
        } else {
            mEvaluateDialg.destroy();
        }

        if (mCustomServiceConfig != null && !mCustomServiceConfig.quitSuspendType.equals(CustomServiceConfig.CSQuitSuspendType.NONE)) {
            RongContext.getInstance().getEventBus().post(new Event.CSTerminateEvent(getActivity(), msg));
        }
    }

    private void csQuitEvaluateButtonClick(String msg) {
        if (mEvaluateDialg != null) {
            mEvaluateDialg.destroy();
            mEvaluateDialg = null;
        }
        if (getHandler() != null) {
            getHandler().removeCallbacksAndMessages(null);
        }

        if (mEvaluateDialg == null) {
            onCustomServiceWarning(msg, false, robotType);
        } else {
            mEvaluateDialg.destroy();
        }

        if (mCustomServiceConfig != null && !mCustomServiceConfig.quitSuspendType.equals(CustomServiceConfig.CSQuitSuspendType.NONE)) {
            RongContext.getInstance().getEventBus().post(new Event.CSTerminateEvent(getActivity(), msg));
        }
    }

    private void csQuitEvaluate(final String msg) {
        if (mEvaluateDialg == null) {
            mEvaluateDialg = new CSEvaluateDialog(getActivity(), mTargetId);
            mEvaluateDialg.setClickListener(new CSEvaluateDialog.EvaluateClickListener() {
                @Override
                public void onEvaluateSubmit() {
                    csQuitEvaluateButtonClick(msg);
                }

                @Override
                public void onEvaluateCanceled() {
                    csQuitEvaluateButtonClick(msg);
                }
            });
            mEvaluateDialg.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    if (mEvaluateDialg != null) {
                        mEvaluateDialg = null;
                    }
                }
            });
            mEvaluateDialg.showStar("");
        }
    }

    /**
     * 当收取大量离线消息时，{@link #onDestroy()} 会被延迟调用，{@link RongExtension} 中
     * {@link IExtensionModule#onDetachedFromExtension()} 也会被延迟。频繁进入会话界面，
     * {@link IExtensionModule#onAttachedToExtension(RongExtension)} 两者时序错乱。
     * 通过在 {@link Activity#isFinishing()} 判断 Activity 是否将要结束，决定 RongExtension 的销毁。
     */
    @Override
    public void onPause() {
        if (getActivity() != null) {
            finishing = getActivity().isFinishing();
        }
        if (finishing) {
            destroy();
        } else {
            stopAudioThingsDependsOnVoipMode();
        }
        /*
         * 通过判断当前是否该显示提示消息数，来记录是否在后台时也进行提示。
         * 因为应用切到后台时，消息可见度判断由于列表无法滚动导致而判断不准确，
         * 所以需要在切到后台前记录下来状态。
         */
        if (mList != null) {
            isShowTipMessageCountInBackground = !mList.isLastItemVisible(TIP_DEFAULT_MESSAGE_COUNT);
        }

        super.onPause();
    }

    /**
     * 展示客服通告栏。
     * 此方法带回通告栏的展示内容及点击链接，须 App 自己来实现。
     *
     * @param announceMsg 通告栏展示内容
     * @param announceUrl 通告栏点击链接地址
     */
    public void onShowAnnounceView(String announceMsg, String announceUrl) {

    }

    private void destroy() {
        RongIM.getInstance().clearMessagesUnreadStatus(mConversationType, mTargetId, null);
        if (getHandler() != null) {
            getHandler().removeCallbacksAndMessages(null);
        }
        if (mConversationType.equals(Conversation.ConversationType.CHATROOM)) {
            SendImageManager.getInstance().cancelSendingImages(mConversationType, mTargetId);
            SendMediaManager.getInstance().cancelSendingMedia(mConversationType, mTargetId);
            RongIM.getInstance().quitChatRoom(mTargetId, null);
        }
        if (mConversationType.equals(Conversation.ConversationType.CUSTOMER_SERVICE) && mCSNeedToQuit) {
            onStopCustomService(mTargetId);
        }
        if (mSyncReadStatus
                && mSyncReadStatusMsgTime > 0
                && ((mConversationType.equals(Conversation.ConversationType.DISCUSSION))
                || mConversationType.equals(Conversation.ConversationType.GROUP))) {
            RongIMClient.getInstance().syncConversationReadStatus(mConversationType, mTargetId, mSyncReadStatusMsgTime, null);
        }

        EventBus.getDefault().unregister(this);
        stopAudioThingsDependsOnVoipMode();
        try {
            if (mKitReceiver != null) {
                if (getActivity() != null) {
                    getActivity().unregisterReceiver(mKitReceiver);
                }
            }
        } catch (Exception e) {
            RLog.e(TAG, "destroy", e);
        }

        RongContext.getInstance().unregisterConversationInfo(mCurrentConversationInfo);
        if (getActivity() != null && getActivity().getResources().getBoolean(R.bool.rc_location_2D)) {
            io.rong.imkit.plugin.location.LocationManager2D.getInstance().quitLocationSharing();
            io.rong.imkit.plugin.location.LocationManager2D.getInstance().setParticipantChangedListener(null);
            io.rong.imkit.plugin.location.LocationManager2D.getInstance().setUserInfoProvider(null);
            io.rong.imkit.plugin.location.LocationManager2D.getInstance().unBindConversation();
        } else {
            io.rong.imkit.plugin.location.LocationManager.getInstance().quitLocationSharing();
            io.rong.imkit.plugin.location.LocationManager.getInstance().setParticipantChangedListener(null);
            io.rong.imkit.plugin.location.LocationManager.getInstance().setUserInfoProvider(null);
            io.rong.imkit.plugin.location.LocationManager.getInstance().unBindConversation();
        }
        destroyExtension();
    }

    private void stopAudioThingsDependsOnVoipMode() {
        if (!AudioPlayManager.getInstance().isInVOIPMode(this.getActivity())) {
            AudioPlayManager.getInstance().stopPlay();
        }
        AudioRecordManager.getInstance().destroyRecord();
    }

    private void destroyExtension() {
        if (mRongExtension != null) {
            String text = mRongExtension.getInputEditText().getText().toString();
            String mentionInfo = RongMentionManager.getInstance().getMentionBlockInfo();
            String saveDraft = DraftHelper.encode(text, mentionInfo);
            if ((TextUtils.isEmpty(text) && !TextUtils.isEmpty(mDraft))
                    || (!TextUtils.isEmpty(text) && TextUtils.isEmpty(mDraft))
                    || (!TextUtils.isEmpty(text) && !TextUtils.isEmpty(mDraft) && !text.equals(mDraft))) {
                RongIMClient.getInstance().saveTextMessageDraft(mConversationType, mTargetId, saveDraft, null);
                Event.DraftEvent draft = new Event.DraftEvent(mConversationType, mTargetId, text);
                RongContext.getInstance().getEventBus().post(draft);
            }
            mRongExtension.onDestroy();
            mRongExtension = null;
        }

        if (mEnableMention
                && (mConversationType.equals(Conversation.ConversationType.DISCUSSION)
                || (mConversationType.equals(Conversation.ConversationType.GROUP)))) {
            RongMentionManager.getInstance().destroyInstance(mConversationType, mTargetId);
        }
    }

    @Override
    public void onDestroy() {
        mAddMentionedMemberListener = null;
        RongMentionManager.getInstance().setAddMentionedMemberListener(null);
        RongMessageItemLongClickActionManager
                .getInstance().removeMessageItemLongClickAction(clickAction);
        RongMessageItemLongClickActionManager.getInstance().removeMessageItemLongClickAction(clickActionReference);
        if (!finishing) {
            destroy();
        }

        if (mList != null) {
            mList.getViewTreeObserver().removeOnGlobalLayoutListener(listViewLayoutListener);
        }
        if (contentView != null) {
            contentView.getViewTreeObserver().removeOnGlobalLayoutListener(globalLayoutListener);
        }
        RecallEditManager.getInstance().cancelCountDownInConversation(mConversationType.getName() + mTargetId);
        super.onDestroy();
    }

    public boolean isLocationSharing() {
        if (getContext() != null && getContext().getResources().getBoolean(R.bool.rc_location_2D)) {
            return io.rong.imkit.plugin.location.LocationManager2D.getInstance().isSharing();
        } else {
            return io.rong.imkit.plugin.location.LocationManager.getInstance().isSharing();
        }
    }

    public void showQuitLocationSharingDialog(final Activity activity) {
        PromptPopupDialog.newInstance(activity, getString(R.string.rc_ext_warning), getString(R.string.rc_real_time_exit_notification), getString(R.string.rc_action_bar_ok))
                .setPromptButtonClickedListener(new PromptPopupDialog.OnPromptButtonClickedListener() {
                    @Override
                    public void onPositiveButtonClicked() {
                        activity.finish();
                    }
                }).show();
    }

    @Override
    public boolean onBackPressed() {
        hideReferenceView();
        if (mRongExtension != null && mRongExtension.isExtensionExpanded()) {
            mRongExtension.collapseExtension();
            return true;
        }
        if (mCustomServiceConfig != null && (Conversation.ConversationType.CUSTOMER_SERVICE).equals(mConversationType) &&
                (CustomServiceConfig.CSQuitSuspendType.NONE).equals(mCustomServiceConfig.quitSuspendType)) {
            return onCustomServiceEvaluation(false, "", robotType, csEvaluate);
        }

        if (mRongExtension != null && mRongExtension.isMoreActionShown()) {
            resetMoreActionState();
            return true;
        } else {
            return false;
        }
    }

    protected void hideReferenceView() {
        if (referenceView != null) {
            referenceView.clearReference();
        }
        referenceMessage = null;
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case CS_HUMAN_MODE_CUSTOMER_EXPIRE: {
                if (!isActivityExist() || mCustomServiceConfig == null) {
                    return true;
                }
                InformationNotificationMessage info = new InformationNotificationMessage(mCustomServiceConfig.userTipWord);
                RongIM.getInstance().insertIncomingMessage(Conversation.ConversationType.CUSTOMER_SERVICE, mTargetId, mTargetId, null, info, System.currentTimeMillis(), null);
                return true;
            }
            case CS_HUMAN_MODE_SEAT_EXPIRE: {
                if (!isActivityExist() || mCustomServiceConfig == null) {
                    return true;
                }
                InformationNotificationMessage info = new InformationNotificationMessage(mCustomServiceConfig.adminTipWord);
                RongIM.getInstance().insertIncomingMessage(Conversation.ConversationType.CUSTOMER_SERVICE, mTargetId, mTargetId, null, info, System.currentTimeMillis(), null);
                return true;
            }
        }

        return false;
    }

    private boolean isActivityExist() {
        return getActivity() != null && !getActivity().isFinishing();
    }

    /**
     * 提示dialog.
     * 例如"加入聊天室失败"的dialog
     * 用户自定义此dialog的步骤:
     * 1.定义一个类继承自 ConversationFragment
     * 2.重写 onWarningDialog
     *
     * @param msg dialog 提示
     */
    public void onWarningDialog(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCancelable(false);
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
        Window window = alertDialog.getWindow();
        if (window == null) {
            return;
        }
        window.setContentView(R.layout.rc_cs_alert_warning);
        TextView tv = window.findViewById(R.id.rc_cs_msg);
        tv.setText(msg);

        window.findViewById(R.id.rc_btn_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
                FragmentManager fm = getChildFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack();
                } else {
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                }
            }
        });
    }

    /**
     * <p>弹出客服提示信息</p>
     * 通过重写此方法可以自定义弹出提示的窗口
     *
     * @param msg       提示的内容
     * @param evaluate  是否需要评价. true 表示还需要弹出评价窗口进行评价, false 表示仅需要提示不需要评价
     * @param robotType 是否是机器人模式
     */
    public void onCustomServiceWarning(String msg, final boolean evaluate, final boolean robotType) {
        if (!isActivityExist()) {
            RLog.w(TAG, "Activity has finished");
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCancelable(false);
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
        Window window = alertDialog.getWindow();
        if (window == null) {
            return;
        }
        window.setContentView(R.layout.rc_cs_alert_warning);
        TextView tv = window.findViewById(R.id.rc_cs_msg);
        tv.setText(msg);

        window.findViewById(R.id.rc_btn_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                alertDialog.dismiss();
                if (evaluate) {
                    onCustomServiceEvaluation(false, "", robotType, evaluate);
                } else {
                    FragmentManager fm = getChildFragmentManager();
                    if (fm.getBackStackEntryCount() > 0) {
                        fm.popBackStack();
                    } else {
                        if (getActivity() != null) {
                            getActivity().finish();
                        }
                    }
                }
            }
        });
    }

    /**
     * <p>客服弹出评价并提交评价</p>
     * 通过重写此方法 App 可以自定义评价的弹出窗口和评价的提交
     *
     * @param isPullEva 是否是客服后台主动拉评价，如果为 false,则需要判断在客服界面停留的时间是否超过60秒,
     *                  超过这个时间则弹出评价窗口,否则不弹; 如果为 true,则不论停留时间为多少都要弹出评价窗口
     * @param dialogId  会话 Id. 客服后台主动拉评价的时候这个参数有效
     * @param robotType 是否为机器人模式,true 表示机器人模式,false 表示人工模式
     * @param evaluate  是否需要评价. true 表示需要弹出评价窗口进行评价, false 不需要弹出评价窗口
     *                  例如有些客服不需要针对整个会话评价,只需要针对每条回复评价,这个时候 evaluate 为 false
     * @return true: 已弹出评价, false:未弹出评价
     */
    public boolean onCustomServiceEvaluation(boolean isPullEva, final String dialogId, final boolean robotType, boolean evaluate) {
        if (getActivity() == null) {
            return false;
        }
        if (isActivityExist()) {
            if (evaluate) {
                long currentTime = System.currentTimeMillis();
                int interval = 60;
                try {
                    interval = getActivity().getResources().getInteger(R.integer.rc_custom_service_evaluation_interval);
                } catch (Resources.NotFoundException e) {
                    RLog.e(TAG, "onCustomServiceEvaluation", e);
                }
                if ((currentTime - csEnterTime < interval * 1000) && !isPullEva) {
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null && imm.isActive() && getActivity().getCurrentFocus() != null) {
                        if (getActivity().getCurrentFocus().getWindowToken() != null) {
                            imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                        }
                    }
                    FragmentManager fm = getChildFragmentManager();
                    if (fm.getBackStackEntryCount() > 0) {
                        fm.popBackStack();
                    } else {
                        getActivity().finish();
                    }
                    return false;
                } else {
                    mEvaluateDialg = new CSEvaluateDialog(getActivity(), mTargetId);
                    mEvaluateDialg.setClickListener(this);
                    mEvaluateDialg.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            if (mEvaluateDialg != null) {
                                mEvaluateDialg = null;
                            }
                        }
                    });
                    if (mCustomServiceConfig != null && mCustomServiceConfig.evaluateType.equals(CustomServiceConfig.CSEvaType.EVA_UNIFIED)) {
                        mEvaluateDialg.showStarMessage(mCustomServiceConfig.isReportResolveStatus);
                    } else if (robotType) {
                        mEvaluateDialg.showRobot(true);
                    } else {
                        onShowStarAndTabletDialog(dialogId);
                    }
                }
            } else {
                FragmentManager fm = getChildFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack();
                } else {
                    getActivity().finish();
                }
            }
        }
        return true;
    }

    /**
     * 显示评价星级及标签的方法
     * App可以继承ConversationFragment类并重写此方法来自定义弹出的评价窗口
     * 评价所需数据可以通过设置监听{@Link CustomServiceManager.OnHumanEvaluateListener}来获取
     *
     * @param dialogId 对话框 id
     */
    public void onShowStarAndTabletDialog(String dialogId) {
        mEvaluateDialg.showStar(dialogId);
    }

    @Override
    public void onSendToggleClick(View v, String text) {
        if (TextUtils.isEmpty(text) || TextUtils.isEmpty(text.trim())) {
            RLog.e(TAG, "text content must not be null");
            return;
        }

        if (isSendReferenceMsg(text)) return;
        TextMessage textMessage = TextMessage.obtain(text);
        if (mRongExtension.isFireStatus()) {
            int length = text.length();
            long time;
            if (length <= 20) {
                time = 10;
            } else {
                time = Math.round((length - 20) * 0.5 + 10);
            }
            textMessage.setDestructTime(time);
        }
        MentionedInfo mentionedInfo = RongMentionManager.getInstance().onSendButtonClick();
        if (mentionedInfo != null) {
            textMessage.setMentionedInfo(mentionedInfo);
        }
        io.rong.imlib.model.Message message = io.rong.imlib.model.Message.obtain(mTargetId, mConversationType, textMessage);
        RongIM.getInstance().sendMessage(message, mRongExtension.isFireStatus() ? getContext().getString(R.string.rc_message_content_burn) : null, null, (IRongCallback.ISendMessageCallback) null);
    }

    protected boolean isSendReferenceMsg(String text) {
        if (referenceMessage != null) {
            ReferenceMessage rMessage = this.referenceMessage.buildSendText(text);
            MentionedInfo mentionedInfo = RongMentionManager.getInstance().onSendButtonClick();
            if (mentionedInfo != null) {
                rMessage.setMentionedInfo(mentionedInfo);
            }
            io.rong.imlib.model.Message message = io.rong.imlib.model.Message.obtain(getTargetId(), getConversationType(), rMessage);
            RongIM.getInstance().sendMessage(message, null, null, (IRongCallback.ISendMessageCallback) null);
            hideReferenceView();
            mRongExtension.collapseExtension();
            return true;
        }
        return false;
    }

    @Override
    public void onImageResult(LinkedHashMap<String, Integer> selectedMedias, boolean origin) {
        boolean fireStatus = mRongExtension.isFireStatus();
        for (Map.Entry<String, Integer> media : selectedMedias.entrySet()) {
            int mediaType = media.getValue();
            final String mediaUri = media.getKey();
            switch (mediaType) {
                case MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO:
                    SendMediaManager.getInstance().sendMedia(mConversationType, mTargetId, Collections.singletonList(Uri.parse(mediaUri)), origin, fireStatus, 10);
                    if (mConversationType.equals(Conversation.ConversationType.PRIVATE)) {
                        RongIMClient.getInstance().sendTypingStatus(mConversationType, mTargetId, "RC:SightMsg");
                    }
                    break;
                case MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE:
                    SendImageManager.getInstance().sendImages(mConversationType, mTargetId, Collections.singletonList(Uri.parse(mediaUri)), origin, fireStatus, 30);
                    if (mConversationType.equals(Conversation.ConversationType.PRIVATE)) {
                        RongIMClient.getInstance().sendTypingStatus(mConversationType, mTargetId, "RC:ImgMsg");
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onEditTextClick(EditText editText) {

    }

    @Override
    public void onLocationResult(double lat, double lng, String poi, Uri thumb) {
        LocationMessage locationMessage = LocationMessage.obtain(lat, lng, poi, thumb);
        io.rong.imlib.model.Message message = io.rong.imlib.model.Message.obtain(mTargetId, mConversationType, locationMessage);
        RongIM.getInstance().sendLocationMessage(message, null, null, null);
        if (mConversationType.equals(Conversation.ConversationType.PRIVATE)) {
            RongIMClient.getInstance().sendTypingStatus(mConversationType, mTargetId, "RC:LBSMsg");
        }
    }

    @Override
    public void onSwitchToggleClick(View v, ViewGroup inputBoard) {
        if (robotType) {
            RongIMClient.getInstance().switchToHumanMode(mTargetId);
        }
        if (mVoiceInputToggle.getVisibility() != View.VISIBLE) {
            hideReferenceView();
        }
    }

    @Override
    public void onVoiceInputToggleTouch(View v, MotionEvent event) {
        String[] permissions = {Manifest.permission.RECORD_AUDIO};
        if (getActivity() == null) {
            return;
        }

        if (!PermissionCheckUtil.checkPermissions(getActivity(), permissions) && event.getAction() == MotionEvent.ACTION_DOWN) {
            PermissionCheckUtil.requestPermissions(this, permissions, REQUEST_CODE_ASK_PERMISSIONS);
            return;
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (AudioPlayManager.getInstance().isPlaying()) {
                AudioPlayManager.getInstance().stopPlay();
            }
            //判断正在视频通话和语音通话中不能进行语音消息发送
            if (IMLibExtensionModuleManager.getInstance().onRequestHardwareResource(HardwareResource.ResourceType.VIDEO)
                    || IMLibExtensionModuleManager.getInstance().onRequestHardwareResource(HardwareResource.ResourceType.AUDIO)) {
                Toast.makeText(getActivity(), getActivity().getString(R.string.rc_voip_occupying),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            AudioRecordManager.getInstance().startRecord(v.getRootView(), mConversationType, mTargetId, mRongExtension.isFireStatus(), mRongExtension.isFireStatus() ? 10 : 0);
            mLastTouchY = event.getY();
            mUpDirection = false;
            ((Button) v).setText(R.string.rc_audio_input_hover);
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (mLastTouchY - event.getY() > mOffsetLimit && !mUpDirection) {
                AudioRecordManager.getInstance().willCancelRecord();
                mUpDirection = true;
                ((Button) v).setText(R.string.rc_audio_input);
            } else if (event.getY() - mLastTouchY > -mOffsetLimit && mUpDirection) {
                AudioRecordManager.getInstance().continueRecord();
                mUpDirection = false;
                ((Button) v).setText(R.string.rc_audio_input_hover);
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            AudioRecordManager.getInstance().stopRecord();
            ((Button) v).setText(R.string.rc_audio_input);
        }
        if (mConversationType.equals(Conversation.ConversationType.PRIVATE)) {
            RongIMClient.getInstance().sendTypingStatus(mConversationType, mTargetId, "RC:VcMsg");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_MSG_DOWNLOAD_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                HQVoiceMsgDownloadManager.getInstance().resumeDownloadService();
            } else {
                mRongExtension.showRequestPermissionFailedAlter(getResources().getString(R.string.rc_permission_grant_needed));
            }
            return;
        }
        if (requestCode == REQUEST_CODE_ASK_PERMISSIONS && grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            mRongExtension.showRequestPermissionFailedAlter(getResources().getString(R.string.rc_permission_grant_needed));
        } else {
            mRongExtension.onRequestPermissionResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onEmoticonToggleClick(View v, ViewGroup extensionBoard) {

    }

    @Override
    public void onPluginToggleClick(View v, ViewGroup extensionBoard) {
        hideReferenceView();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        int cursor, offset;
        if (count == 0) {
            cursor = start + before;
            offset = -before;
        } else {
            cursor = start;
            offset = count;
        }
        if (mConversationType.equals(Conversation.ConversationType.GROUP) || mConversationType.equals(Conversation.ConversationType.DISCUSSION)) {
            RongMentionManager.getInstance().onTextEdit(mConversationType, mTargetId, cursor, offset, s.toString());
        } else if (mConversationType.equals(Conversation.ConversationType.PRIVATE) && offset != 0) {
            RongIMClient.getInstance().sendTypingStatus(mConversationType, mTargetId, "RC:TxtMsg");
        }
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
            EditText editText = (EditText) v;
            int cursorPos = editText.getSelectionStart();
            RongMentionManager.getInstance().onDeleteClick(mConversationType, mTargetId, editText, cursorPos);
        }
        return false;
    }

    @Override
    public void onMenuClick(int root, int sub) {
        if (getActivity() == null) {
            return;
        }
        if (mPublicServiceProfile != null) {
            PublicServiceMenuItem item = mPublicServiceProfile.getMenu().getMenuItems().get(root);
            if (sub >= 0) {
                item = item.getSubMenuItems().get(sub);
            }
            if (item.getType().equals(PublicServiceMenu.PublicServiceMenuItemType.View)) {
                IPublicServiceMenuClickListener menuClickListener = RongContext.getInstance().getPublicServiceMenuClickListener();
                if (menuClickListener == null || !menuClickListener.onClick(mConversationType, mTargetId, item)) {
                    String action = RongKitIntent.RONG_INTENT_ACTION_WEBVIEW;
                    Intent intent = new Intent(action);
                    intent.setPackage(getActivity().getPackageName());
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("url", item.getUrl());
                    getActivity().startActivity(intent);
                }
            }

            PublicServiceCommandMessage msg = PublicServiceCommandMessage.obtain(item);
            RongIMClient.getInstance().sendMessage(mConversationType, mTargetId, msg, null, null, new IRongCallback.ISendMessageCallback() {
                @Override
                public void onAttached(io.rong.imlib.model.Message message) {

                }

                @Override
                public void onSuccess(io.rong.imlib.model.Message message) {

                }

                @Override
                public void onError(io.rong.imlib.model.Message message, RongIMClient.ErrorCode errorCode) {

                }
            });

        }
    }

    @Override
    public void onPluginClicked(IPluginModule pluginModule, int position) {

    }

    /**
     * 短语条目被点击的回调
     *
     * @param phrases  短语
     * @param position 被点击的短语position
     */
    @Override
    public void onPhrasesClicked(String phrases, int position) {
        if (TextUtils.isEmpty(phrases) || TextUtils.isEmpty(phrases.trim())) {
            RLog.e(TAG, "text content must not be null");
            return;
        }

        TextMessage textMessage = TextMessage.obtain(phrases);
        io.rong.imlib.model.Message message = io.rong.imlib.model.Message.obtain(mTargetId, mConversationType, textMessage);
        RongIM.getInstance().sendMessage(message, null, null, (IRongCallback.ISendMessageCallback) null);
        mRongExtension.collapseExtension();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_FORWARD) {
            forwardMessage(data);
            return;
        }

        // 如果结束界面是客服留言界面，则同步结束会话界面
        if (requestCode == REQUEST_CS_LEAVEL_MESSAGE) {
            if (getActivity() != null) {
                getActivity().finish();
            }
        } else {
            mRongExtension.onActivityPluginResult(requestCode, resultCode, data);
        }
    }

    /**
     * 用户重写该接口,可以自定义一个用于转发的选择联系人界面
     *
     * @return 返回一个用于转发的选择会话界面的intent
     */
    public Intent getSelectIntentForForward() {
        return new Intent(getActivity(), SelectConversationActivity.class);
    }

    private void forwardMessage(Intent data) {
        if (data == null) return;
        ForwardManager.getInstance().forwardMessages(
                data.getIntExtra("index", 0),
                data.<Conversation>getParcelableArrayListExtra("conversations"),
                data.getIntegerArrayListExtra("messageIds"),
                getCheckedMessages());
        resetMoreActionState();
    }

    private String getNameFromCache(String targetId) {
        UserInfo info = RongContext.getInstance().getUserInfoFromCache(targetId);
        return info == null ? targetId : info.getName();
    }

    public void onEventMainThread(Event.ReadReceiptRequestEvent event) {
        RLog.d(TAG, "ReadReceiptRequestEvent");

        if (mConversationType.equals(Conversation.ConversationType.GROUP) || mConversationType.equals(Conversation.ConversationType.DISCUSSION)) {
            if (RongContext.getInstance().isReadReceiptConversationType(event.getConversationType())) {
                if (event.getConversationType().equals(mConversationType) && event.getTargetId().equals(mTargetId)) {
                    for (int i = 0; i < mListAdapter.getCount(); i++) {
                        if (mListAdapter.getItem(i).getUId().equals(event.getMessageUId())) {
                            final UIMessage uiMessage = mListAdapter.getItem(i);
                            ReadReceiptInfo readReceiptInfo = uiMessage.getReadReceiptInfo();
                            if (readReceiptInfo == null) {
                                readReceiptInfo = new ReadReceiptInfo();
                                uiMessage.setReadReceiptInfo(readReceiptInfo);
                            }
                            if (readReceiptInfo.isReadReceiptMessage() && readReceiptInfo.hasRespond()) {
                                return;
                            }
                            readReceiptInfo.setIsReadReceiptMessage(true);
                            readReceiptInfo.setHasRespond(false);
                            List<io.rong.imlib.model.Message> messageList = new ArrayList<>();
                            messageList.add((mListAdapter.getItem(i)).getMessage());
                            RongIMClient.getInstance().sendReadReceiptResponse(event.getConversationType(), event.getTargetId(), messageList, new RongIMClient.OperationCallback() {
                                @Override
                                public void onSuccess() {
                                    uiMessage.getReadReceiptInfo().setHasRespond(true);
                                }

                                @Override
                                public void onError(RongIMClient.ErrorCode errorCode) {
                                    RLog.e(TAG, "sendReadReceiptResponse failed, errorCode = " + errorCode);
                                }
                            });
                            break;
                        }
                    }
                }
            }
        }
    }

    public void onEventMainThread(Event.ReadReceiptResponseEvent event) {
        RLog.d(TAG, "ReadReceiptResponseEvent");

        if (mConversationType.equals(Conversation.ConversationType.GROUP) || mConversationType.equals(Conversation.ConversationType.DISCUSSION)) {
            if (RongContext.getInstance().isReadReceiptConversationType(event.getConversationType()) &&
                    event.getConversationType().equals(mConversationType) &&
                    event.getTargetId().equals(mTargetId)) {
                for (int i = 0; i < mListAdapter.getCount(); i++) {
                    if (mListAdapter.getItem(i).getUId().equals(event.getMessageUId())) {
                        UIMessage uiMessage = mListAdapter.getItem(i);
                        ReadReceiptInfo readReceiptInfo = uiMessage.getReadReceiptInfo();
                        if (readReceiptInfo == null) {
                            readReceiptInfo = new ReadReceiptInfo();
                            readReceiptInfo.setIsReadReceiptMessage(true);
                            uiMessage.setReadReceiptInfo(readReceiptInfo);
                        }
                        readReceiptInfo.setRespondUserIdList(event.getResponseUserIdList());
                        int first = mList.getFirstVisiblePosition();
                        int last = mList.getLastVisiblePosition();
                        int position = getPositionInListView(i);
                        if (position >= first && position <= last) {
                            mListAdapter.getView(i, getListViewChildAt(i), mList);
                        }
                        break;
                    }
                }
            }
        }
    }


    public void onEventMainThread(Event.MessageDeleteEvent deleteEvent) {
        RLog.d(TAG, "MessageDeleteEvent");
        if (deleteEvent.getMessageIds() != null) {
            for (int messageId : deleteEvent.getMessageIds()) {
                int position = mListAdapter.findPosition(messageId);
                if (position >= 0) {
                    UIMessage message = mListAdapter.getItem(position);
                    if (message.getContent() instanceof VoiceMessage
                            && AudioPlayManager.getInstance().isPlaying()) {
                        VoiceMessage voiceMessage = (VoiceMessage) message.getContent();
                        if (voiceMessage.getUri().equals(AudioPlayManager.getInstance().getPlayingUri())) {
                            AudioPlayManager.getInstance().stopPlay();
                        }
                    }

                    if (message.getContent() instanceof HQVoiceMessage
                            && AudioPlayManager.getInstance().isPlaying()) {
                        HQVoiceMessage voiceMessage = (HQVoiceMessage) message.getContent();
                        if (voiceMessage.getLocalPath().equals(AudioPlayManager.getInstance().getPlayingUri())) {
                            AudioPlayManager.getInstance().stopPlay();
                        }
                    }
                    mListAdapter.remove(position);
                }
            }
            mListAdapter.notifyDataSetChanged();
        }
    }

    public void onEventMainThread(Event.PublicServiceFollowableEvent event) {
        RLog.d(TAG, "PublicServiceFollowableEvent");

        if (event != null && !event.isFollow()) {
            if (getActivity() != null) {
                getActivity().finish();
            }
        }
    }

    public void onEventMainThread(Event.MessagesClearEvent clearEvent) {
        RLog.d(TAG, "MessagesClearEvent");
        if (clearEvent.getTargetId().equals(mTargetId) && clearEvent.getType().equals(mConversationType)) {
            mListAdapter.clear();
            mListAdapter.notifyDataSetChanged();
        }
    }

    public void onEventMainThread(Event.MessageRecallEvent event) {
        RLog.d(TAG, "MessageRecallEvent");

        if (event.isRecallSuccess()) {
            RecallNotificationMessage recallNotificationMessage = event.getRecallNotificationMessage();
            int position = mListAdapter.findPosition(event.getMessageId());
            if (position != -1) {
                UIMessage uiMessage = mListAdapter.getItem(position);
                if (uiMessage.getMessage().getContent() instanceof VoiceMessage
                        || uiMessage.getMessage().getContent() instanceof HQVoiceMessage) {
                    AudioPlayManager.getInstance().stopPlay();
                }
                if (uiMessage.getMessage().getContent() instanceof FileMessage) {
                    RongIM.getInstance().cancelDownloadMediaMessage(uiMessage.getMessage(), null);
                }
                mListAdapter.getItem(position).setContent(recallNotificationMessage);
                int first = mList.getFirstVisiblePosition();
                int last = mList.getLastVisiblePosition();
                int listPos = getPositionInListView(position);
                if (listPos >= first && listPos <= last) {
                    mListAdapter.getView(position, getListViewChildAt(position), mList);
                }
            }
            if (referenceMessage == null) {
                return;
            }
            hideReferenceView();
        } else {
            Toast.makeText(getActivity(), R.string.rc_recall_failed, Toast.LENGTH_SHORT).show();
        }
    }


    public void onEventMainThread(Event.RemoteMessageRecallEvent event) {
        RLog.d(TAG, "RemoteMessageRecallEvent");

        //遍历 @消息未读列表，如果存在撤回消息则移除，刷新 @ Bar
        Iterator<io.rong.imlib.model.Message> iterator = mMentionMessages.iterator();
        boolean needRefresh = false;
        while (iterator.hasNext()) {
            io.rong.imlib.model.Message message = iterator.next();
            if (message.getMessageId() == event.getMessageId()) {
                iterator.remove();
                needRefresh = true;
                break;
            }
        }
        if (needRefresh) {
            processMentionLayout();
        }
        int position = mListAdapter.findPosition(event.getMessageId());
        int first = mList.getFirstVisiblePosition();
        int last = mList.getLastVisiblePosition();
        if (position >= 0) {
            updateNewMessageCountIfNeed(mListAdapter.getItem(position).getMessage(), false);
            if (event.getRecallNotificationMessage() == null) {
                mListAdapter.remove(position);
                mListAdapter.notifyDataSetChanged();
                return;
            }
            UIMessage uiMessage = mListAdapter.getItem(position);
            MessageContent content = uiMessage.getMessage().getContent();
            if (content instanceof VoiceMessage || content instanceof HQVoiceMessage) {
                AudioPlayManager.getInstance().stopPlay();
            } else if (content instanceof FileMessage || content instanceof GIFMessage || content instanceof SightMessage) {
                RongIM.getInstance().cancelDownloadMediaMessage(uiMessage.getMessage(), null);
            }
            uiMessage.setContent(event.getRecallNotificationMessage());
            int listPos = getPositionInListView(position);
            if (listPos >= first && listPos <= last) {
                mListAdapter.getView(position, getListViewChildAt(position), mList);
            }
            if (referenceMessage == null) {
                return;
            }
            hideReferenceView();

        }
    }

    public void onEventMainThread(io.rong.imlib.model.Message msg) {
        RLog.d(TAG, "Event message : " + msg.getMessageId() + ", " + msg.getObjectName() + ", " + msg.getSentStatus());

        if (mTargetId.equals(msg.getTargetId())
                && mConversationType.equals(msg.getConversationType())
                && msg.getMessageId() > 0) {
            int position = mListAdapter.findPosition(msg.getMessageId());
            if (position >= 0) {
                //如果发送失败,也需要把时间校正为服务器时间。发送成功时不需要校正,默认携带的是服务器返回的标准时间。
                if (msg.getSentStatus().equals(io.rong.imlib.model.Message.SentStatus.FAILED)) {
                    long serverTime = msg.getSentTime() - RongIMClient.getInstance().getDeltaTime();
                    msg.setSentTime(serverTime);
                }
                mListAdapter.getItem(position).setMessage(msg);
                mListAdapter.getView(position, getListViewChildAt(position), mList);
            } else {
                UIMessage uiMessage = UIMessage.obtain(msg);
                if (msg.getContent() instanceof CSPullLeaveMessage) {
                    uiMessage.setCsConfig(mCustomServiceConfig);
                }
                long sentTime = uiMessage.getSentTime();
                if (uiMessage.getMessageDirection() == io.rong.imlib.model.Message.MessageDirection.SEND
                        && uiMessage.getSentStatus() == io.rong.imlib.model.Message.SentStatus.SENDING
                        || uiMessage.getContent() instanceof RealTimeLocationStartMessage) {
                    sentTime = uiMessage.getSentTime() - RongIMClient.getInstance().getDeltaTime();
                    uiMessage.setSentTime(sentTime);//更新成服务器时间
                }
                int insertPosition = mListAdapter.getPositionBySendTime(sentTime);
                if (mShouldInsertMsg) {
                    mListAdapter.add(uiMessage, insertPosition);
                    mListAdapter.notifyDataSetChanged();
                }
            }
//            if (msg.getSenderUserId() != null && msg.getSenderUserId().equals(RongIM.getInstance().getCurrentUserId())
//                    && mList.getLastVisiblePosition() - 1 != mList.getCount()) {
//                mList.setSelection(mListAdapter.getCount());
//            }
            MessageTag msgTag = msg.getContent().getClass().getAnnotation(MessageTag.class);
            if (mNewMessageCount <= 0
                    && (msgTag != null && msgTag.flag() == MessageTag.ISCOUNTED || (mList.getLastVisiblePosition() == mList.getCount() - mList.getHeaderViewsCount() - 1)
                    || isSelfSendMessage(msg))) {
                mList.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
                mList.post(new Runnable() {
                    @Override
                    public void run() {
                        if (getActivity() != null && mList != null) {
                            mList.setSelection(mList.getCount());
                        }
                    }
                });
                mList.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_DISABLED);
            }
            if (mConversationType.equals(Conversation.ConversationType.CUSTOMER_SERVICE)
                    && msg.getMessageDirection() == io.rong.imlib.model.Message.MessageDirection.SEND
                    && !robotType
                    && mCustomServiceConfig != null
                    && mCustomServiceConfig.userTipTime > 0
                    && !TextUtils.isEmpty(mCustomServiceConfig.userTipWord)) {
                startTimer(CS_HUMAN_MODE_CUSTOMER_EXPIRE, mCustomServiceConfig.userTipTime * 60 * 1000);
            }
        }
    }

    /**
     * 判断是否是自己发送的消息
     *
     * @param msg
     * @return 是否是自己发送的消息
     */
    private boolean isSelfSendMessage(io.rong.imlib.model.Message msg) {
        String currentUserId = RongIM.getInstance().getCurrentUserId();
        String sendUserId = msg.getSenderUserId();
        if (currentUserId == null || sendUserId == null) {
            return false;
        }
        //判断 sendUserId 是否与当前用户 id 相等
        return currentUserId.equals(sendUserId) && mTargetId.equals(msg.getTargetId());
    }

    public void onEventMainThread(final RongIMClient.ConnectionStatusListener.ConnectionStatus status) {
        RLog.d(TAG, "ConnectionStatus, " + status.toString());
        if (getActivity() != null && isAdded()) {
            SharedPreferences sp = SharedPreferencesUtils.get(getContext(), KitCommonDefine.RONG_KIT_SP_CONFIG, Context.MODE_PRIVATE);
            boolean sendReadReceiptFailed = sp.getBoolean(getSavedReadReceiptStatusName(), false);
            long sendReadReceiptTime = sp.getLong(getSavedReadReceiptTimeName(), 0);
            if (status.equals(RongIMClient.ConnectionStatusListener.ConnectionStatus.CONNECTED) && sendReadReceiptFailed) {
                RongIMClient.getInstance().sendReadReceiptMessage(mConversationType, mTargetId, sendReadReceiptTime, null);
                removeSendReadReceiptStatusToSp();
            }
        }
    }

    public void onEventMainThread(Event.MessageSentStatusUpdateEvent event) {
        io.rong.imlib.model.Message message = event.getMessage();
        if (message == null || message.getMessageDirection().equals(io.rong.imlib.model.Message.MessageDirection.RECEIVE)) {
            RLog.e(TAG, "MessageSentStatusUpdateEvent message is null or direction is RECEIVE");
            return;
        }
        RLog.d(TAG, "MessageSentStatusEvent event : " + event.getMessage().getMessageId() + ", " + event.getSentStatus());
        int position = mListAdapter.findPosition(message.getMessageId());
        if (position >= 0) {
            mListAdapter.getItem(position).setSentStatus(event.getSentStatus());
            mListAdapter.getView(position, getListViewChildAt(position), mList);
        }
    }

    public void onEventMainThread(Event.FileMessageEvent event) {
        io.rong.imlib.model.Message msg = event.getMessage();
        RLog.d(TAG, "FileMessageEvent message : " + msg.getMessageId() + ", " + msg.getObjectName() + ", " + msg.getSentStatus());

        if (mTargetId.equals(msg.getTargetId())
                && mConversationType.equals(msg.getConversationType())
                && msg.getMessageId() > 0
                && msg.getContent() instanceof MediaMessageContent) {
            int position = mListAdapter.findPosition(msg.getMessageId());
            if (position >= 0) {
                UIMessage uiMessage = mListAdapter.getItem(position);
                uiMessage.setMessage(msg);
                uiMessage.setProgress(event.getProgress());
                if (msg.getContent() instanceof FileMessage) {
                    ((FileMessage) msg.getContent()).progress = event.getProgress();
                }
                mListAdapter.getItem(position).setMessage(msg);
                int first = mList.getFirstVisiblePosition();
                int last = mList.getLastVisiblePosition();
                if (position >= first && position <= last) {
                    mListAdapter.getView(position, getListViewChildAt(position), mList);
                }
            }
        }
    }

    public void onEventMainThread(GroupUserInfo groupUserInfo) {
        RLog.d(TAG, "GroupUserInfoEvent " + groupUserInfo.getGroupId() + " " + groupUserInfo.getUserId() + " " + groupUserInfo.getNickname());
        if (groupUserInfo.getNickname() == null || groupUserInfo.getGroupId() == null) {
            return;
        }
        int count = mListAdapter.getCount();
        int first = mList.getFirstVisiblePosition();
        int last = mList.getLastVisiblePosition();
        for (int i = 0; i < count; i++) {
            UIMessage uiMessage = mListAdapter.getItem(i);
            if (uiMessage.getSenderUserId().equals(groupUserInfo.getUserId())) {
                uiMessage.setNickName(true);
                UserInfo userInfo = uiMessage.getUserInfo();
                if (userInfo != null) {
                    userInfo.setName(groupUserInfo.getNickname());
                    uiMessage.setUserInfo(userInfo);
                }
                int pos = getPositionInListView(i);
                if (pos >= first && pos <= last) {
                    mListAdapter.getView(i, getListViewChildAt(i), mList);
                }
            }
        }
    }

    private View getListViewChildAt(int adapterIndex) {
        int header = mList.getHeaderViewsCount();
        int first = mList.getFirstVisiblePosition();
        return mList.getChildAt(adapterIndex + header - first);
    }

    private int getPositionInListView(int adapterIndex) {
        int header = mList.getHeaderViewsCount();
        return adapterIndex + header;
    }

    private int getPositionInAdapter(int listIndex) {
        int header = mList.getHeaderViewsCount();
        return listIndex <= 0 ? 0 : listIndex - header;
    }

    /**
     * 显示屏幕右下角新消息数量气泡
     */
    private void showNewMessage() {
        RongContext rongContext = RongContext.getInstance();
        if (rongContext == null || !rongContext.getNewMessageState() || mNewMessageCount < 0) {
            return;
        }
        if (mNewMessageCount == 0) {
            mNewMessageBtn.setVisibility(View.GONE);
            mNewMessageTextView.setVisibility(View.GONE);
            return;
        }
        mNewMessageBtn.setVisibility(View.VISIBLE);
        mNewMessageTextView.setVisibility(View.VISIBLE);
        mNewMessageTextView.setText(mNewMessageCount > 99 ? "99+" : Integer.toString(mNewMessageCount));
    }

    public void onEventMainThread(Event.OnMessageSendErrorEvent event) {
        onEventMainThread(event.getMessage());
    }

    public void onEventMainThread(Event.MessageLeftEvent event) {
        int count = mListAdapter.getCount();
        if (event.left == 0 && count >= 1) {
            UIMessage uiMessage = mListAdapter.getItem(count - 1);
            if (uiMessage != null && uiMessage.getMessage() != null) {
                handleEventAfterAllMessageLoaded(uiMessage.getMessage());
            }
        }
    }

    public void onEventMainThread(Event.OnReceiveMessageEvent event) {
        io.rong.imlib.model.Message message = event.getMessage();
        RLog.i(TAG, "OnReceiveMessageEvent, " + message.getMessageId() + ", " + message.getObjectName() + ", " + message.getReceivedStatus().toString());
        Conversation.ConversationType conversationType = message.getConversationType();
        String targetId = message.getTargetId();

        if (mConversationType.equals(conversationType)
                && mTargetId.equals(targetId)
                && shouldUpdateMessage(message, event.getLeft())) {
            //离线消息拉取完毕后，处理已读回执
            if (event.getLeft() == 0 && !event.hasPackage()) {
                handleEventAfterAllMessageLoaded(message);
            }
            if (mSyncReadStatus) {
                mSyncReadStatusMsgTime = message.getSentTime();
            }
            if (message.getMessageId() > 0) {
                if (!SystemUtils.isInBackground(getActivity())) {
                    message.getReceivedStatus().setRead();
                    RongIMClient.getInstance().setMessageReceivedStatus(message.getMessageId(), message.getReceivedStatus(), null);
                    if (message.getMessageDirection().equals(io.rong.imlib.model.Message.MessageDirection.RECEIVE)) {
                        UnReadMessageManager.getInstance().onMessageReceivedStatusChanged();
                    }
                }
                if (mConversationType.equals(Conversation.ConversationType.CUSTOMER_SERVICE)
                        && !robotType
                        && mCustomServiceConfig != null
                        && mCustomServiceConfig.adminTipTime > 0
                        && !TextUtils.isEmpty(mCustomServiceConfig.adminTipWord)) {
                    startTimer(CS_HUMAN_MODE_SEAT_EXPIRE, mCustomServiceConfig.adminTipTime * 60 * 1000);
                }
            }
            RLog.d(TAG, "mList.getCount(): " + mList.getCount() + " getLastVisiblePosition:" + mList.getLastVisiblePosition());

            increaseNewMessageCountIfNeed(message);

            onEventMainThread(event.getMessage());
        }
    }

    public void onEventMainThread(Event.changeDestructionReadTimeEvent event) {
        io.rong.imlib.model.Message message = event.message;
        if (message != null && mConversationType == Conversation.ConversationType.PRIVATE
                && event.message.getContent().isDestruct()) {
            int messagePosition = mListAdapter.findPosition(message.getMessageId());
            mListAdapter.getItem(messagePosition).getMessage().setReadTime(message.getReadTime());
            mListAdapter.notifyDataSetChanged();
        }
    }

    private void handleEventAfterAllMessageLoaded(io.rong.imlib.model.Message message) {
        HQVoiceMsgDownloadManager.getInstance().enqueue(ConversationFragment.this, new AutoDownloadEntry(message, AutoDownloadEntry.DownloadPriority.HIGH));

        boolean isPrivate = message.getConversationType().equals(Conversation.ConversationType.PRIVATE)
                && RongContext.getInstance().isReadReceiptConversationType(Conversation.ConversationType.PRIVATE);
        boolean isEncrypted = message.getConversationType().equals(Conversation.ConversationType.ENCRYPTED)
                && RongContext.getInstance().isReadReceiptConversationType(Conversation.ConversationType.ENCRYPTED);
        if ((isPrivate || isEncrypted)
                && message.getMessageDirection().equals(io.rong.imlib.model.Message.MessageDirection.RECEIVE)) {
            if (mReadRec && !TextUtils.isEmpty(message.getUId())) {
                if ((RongIMClient.getInstance().getTopForegroundActivity() != null
                        && RongIMClient.getInstance().getTopForegroundActivity().equals(getActivity()))) {//本地插入的消息(Uid 为空)不需要发送已读回执
                    RongIMClient.getInstance().sendReadReceiptMessage(message.getConversationType(), message.getTargetId(),
                            message.getSentTime(), new IRongCallback.ISendMessageCallback() {
                                @Override
                                public void onAttached(io.rong.imlib.model.Message message) {

                                }

                                @Override
                                public void onSuccess(io.rong.imlib.model.Message message) {
                                    removeSendReadReceiptStatusToSp();
                                }

                                @Override
                                public void onError(io.rong.imlib.model.Message message, RongIMClient.ErrorCode errorCode) {
                                    RLog.e(TAG, "sendReadReceiptMessage errorCode = " + errorCode.getValue());
                                    saveSendReadReceiptStatusToSp(true, message.getSentTime());
                                }
                            });
                } else {
                    saveSendReadReceiptStatusToSp(true, message.getSentTime());
                }
            }
            /**
             * 只在单聊中同步多端未读数，是为了减少群组和讨论组中的消息量。会在会话页面销毁的时候同步未读消息数
             */
            if (!mReadRec && mSyncReadStatus) {
                RongIMClient.getInstance().syncConversationReadStatus(message.getConversationType(), message.getTargetId(), message.getSentTime(), null);
            }
        }
    }


    private void removeSendReadReceiptStatusToSp() {
        if (getActivity() != null && isAdded()) {
            SharedPreferences preferences = SharedPreferencesUtils.get(getContext(), KitCommonDefine.RONG_KIT_SP_CONFIG, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.remove(getSavedReadReceiptStatusName());
            editor.remove(getSavedReadReceiptTimeName());
            editor.commit();
        }
    }

    private void saveSendReadReceiptStatusToSp(boolean status, long time) {
        if (getActivity() != null && isAdded()) {
            SharedPreferences preferences = SharedPreferencesUtils.get(getContext(), KitCommonDefine.RONG_KIT_SP_CONFIG, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(getSavedReadReceiptStatusName(), status);
            editor.putLong(getSavedReadReceiptTimeName(), time);
            editor.commit();
        }
    }

    private String getSavedReadReceiptStatusName() {
        if (!TextUtils.isEmpty(mTargetId) && mConversationType != null) {
            String savedId = DeviceUtils.ShortMD5(Base64.DEFAULT, RongIM.getInstance().getCurrentUserId(), mTargetId, mConversationType.getName());
            return "ReadReceipt" + savedId + "Status";
        }
        return "";
    }

    private String getSavedReadReceiptTimeName() {
        if (!TextUtils.isEmpty(mTargetId) && mConversationType != null) {
            String savedId = DeviceUtils.ShortMD5(Base64.DEFAULT, RongIM.getInstance().getCurrentUserId(), mTargetId, mConversationType.getName());
            return "ReadReceipt" + savedId + "Time";
        }
        return "";
    }

    protected void increaseNewMessageCountIfNeed(io.rong.imlib.model.Message message) {
        updateNewMessageCountIfNeed(message, true);
        updateMentionMessage(message);
    }

    private void updateMentionMessage(io.rong.imlib.model.Message message) {
        if (mEnableUnreadMentionMessage && mMentionMsgLayout != null && message != null && message.getContent() != null && message.getContent().getMentionedInfo() != null) {
            MentionedInfo mentionedInfo = message.getContent().getMentionedInfo();
            MentionedInfo.MentionedType type = mentionedInfo.getType();
            if (type == MentionedInfo.MentionedType.ALL) {
                mMentionMessages.add(message);
            } else if (type == MentionedInfo.MentionedType.PART &&
                    mentionedInfo.getMentionedUserIdList() != null &&
                    mentionedInfo.getMentionedUserIdList().contains(RongIMClient.getInstance().getCurrentUserId())) {
                mMentionMessages.add(message);
            }
            processMentionLayout();
        }
    }

    // increase true:气泡消息+1  false：气泡消息-1
    protected void updateNewMessageCountIfNeed(io.rong.imlib.model.Message message, boolean increase) {
        if (mNewMessageBtn != null
                && getContext() != null
                && (SystemUtils.isInBackground(getContext()) ? isShowTipMessageCountInBackground    //判断是否在后台，如果在后台则判断当前是否该显示消息数提示
                : (!mList.isLastItemVisible(TIP_DEFAULT_MESSAGE_COUNT) || !mShouldInsertMsg)) //判断列表底部的不可见项数是否达到了显示消息提示所需要数量
                && message != null
                && message.getMessageDirection() != io.rong.imlib.model.Message.MessageDirection.SEND
                && message.getConversationType() != Conversation.ConversationType.CHATROOM
                && message.getConversationType() != Conversation.ConversationType.CUSTOMER_SERVICE
                && message.getConversationType() != Conversation.ConversationType.APP_PUBLIC_SERVICE
                && message.getConversationType() != Conversation.ConversationType.PUBLIC_SERVICE) {

            if (increase) {
                mNewMessageCount++;
            } else {
                mNewMessageCount--;
            }
            showNewMessage();
        }
    }

    // 解决连续播放，如果不切换线程，会导致递归调用
    public void onEventBackgroundThread(final Event.PlayAudioEvent event) {
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                handleAudioPlayEvent(event);
            }
        });
    }

    private void handleAudioPlayEvent(Event.PlayAudioEvent event) {
        RLog.i(TAG, "PlayAudioEvent");

        int first = mList.getFirstVisiblePosition();
        int last = mList.getLastVisiblePosition();
        int position = mListAdapter.findPosition(event.messageId);
        if (event.continuously && position >= 0) {
            while (first <= last) {
                position++;
                first++;
                UIMessage uiMessage = mListAdapter.getItem(position);
                if (uiMessage != null &&
                        (uiMessage.getContent() instanceof VoiceMessage || uiMessage.getMessage().getContent() instanceof HQVoiceMessage) &&
                        (uiMessage.getMessageDirection().equals(io.rong.imlib.model.Message.MessageDirection.RECEIVE) &&
                                !uiMessage.getReceivedStatus().isListened()) &&
                        !uiMessage.getContent().isDestruct()) {
                    uiMessage.continuePlayAudio = true;
                    mListAdapter.getView(position, getListViewChildAt(position), mList);
                    break;
                }
            }
        }
    }

    public void onEventMainThread(Event.OnReceiveMessageProgressEvent event) {

        if (mList != null) {
            boolean isInBackground = SystemUtils.isInBackground(getContext());
            if (isInBackground && (event.getProgress() >= 100 || event.getProgress() < 0)) {
                isNeedRefresh = true;
            } else {
                isNeedRefresh = false;
            }

            int first = mList.getFirstVisiblePosition();
            int last = mList.getLastVisiblePosition();
            while (first <= last) {
                int position = getPositionInAdapter(first);
                UIMessage uiMessage = mListAdapter.getItem(position);
                // 为了防止数值一样重复刷新, 则除了 100 完成, 其他 小于 100 的 , 值如果相同也不会去刷新
                if (uiMessage != null && uiMessage.getMessageId() == event.getMessage().getMessageId() && (event.getProgress() != uiMessage.getProgress() || event.getProgress() == 100)) {
                    uiMessage.setProgress(event.getProgress());
                    if (isResumed()) {
                        mListAdapter.getView(position, getListViewChildAt(position), mList);
                    }
                    break;
                }
                first++;
            }
        }
    }

    public void onEventMainThread(Event.ConnectEvent event) {
        RLog.i(TAG, "ConnectEvent : " + event.getConnectStatus());
        if (mListAdapter.getCount() == 0) {
            AutoRefreshListView.Mode mode = indexMessageTime > 0 ? AutoRefreshListView.Mode.END : AutoRefreshListView.Mode.START;
            int scrollMode = indexMessageTime > 0 ? SCROLL_MODE_NORMAL : SCROLL_MODE_BOTTOM;
            getHistoryMessage(mConversationType, mTargetId, DEFAULT_HISTORY_MESSAGE_COUNT, mode, scrollMode, -1, false);
        }
    }

    public void onEventMainThread(UserInfo userInfo) {
        RLog.i(TAG, "userInfo " + userInfo.getUserId());
        int first = mList.getFirstVisiblePosition();
        int last = mList.getLastVisiblePosition();

        for (int i = 0; i < mListAdapter.getCount(); i++) {
            UIMessage uiMessage = mListAdapter.getItem(i);
            if (userInfo.getUserId().equals(uiMessage.getSenderUserId())
                    && !uiMessage.isNickName()) {
                if (uiMessage.getConversationType().equals(Conversation.ConversationType.CUSTOMER_SERVICE)
                        && uiMessage.getMessage() != null
                        && uiMessage.getMessage().getContent() != null
                        && uiMessage.getMessage().getContent().getUserInfo() != null) {
                    uiMessage.setUserInfo(uiMessage.getMessage().getContent().getUserInfo());
                } else {
                    uiMessage.setUserInfo(userInfo);
                }
                int position = getPositionInListView(i);
                if (position >= first && position <= last) {
                    mListAdapter.getView(i, getListViewChildAt(i), mList);
                }
            }
        }
    }

    public void onEventMainThread(PublicServiceProfile publicServiceProfile) {
        RLog.i(TAG, "publicServiceProfile");
        if (publicServiceProfile != null && mConversationType.equals(publicServiceProfile.getConversationType()) && mTargetId.equals(publicServiceProfile.getTargetId())) {
            int first = mList.getFirstVisiblePosition();
            int last = mList.getLastVisiblePosition();
            while (first <= last) {
                int position = getPositionInAdapter(first);
                UIMessage message = mListAdapter.getItem(position);
                if (message != null && (TextUtils.isEmpty(message.getTargetId())
                        || publicServiceProfile.getTargetId().equals(message.getTargetId()))) {
                    mListAdapter.getView(position, getListViewChildAt(position), mList);
                }
                first++;
            }
            updatePublicServiceMenu(publicServiceProfile);
        }
    }

    public void onEventMainThread(final Event.ReadReceiptEvent event) {
        RLog.i(TAG, "ReadReceiptEvent");
        if (RongContext.getInstance().isReadReceiptConversationType(event.getMessage().getConversationType())) {
            if (mTargetId.equals(event.getMessage().getTargetId())
                    && mConversationType.equals(event.getMessage().getConversationType())
                    && event.getMessage().getMessageDirection().equals(io.rong.imlib.model.Message.MessageDirection.RECEIVE)) {
                ReadReceiptMessage content = (ReadReceiptMessage) event.getMessage().getContent();
                long ntfTime = content.getLastMessageSendTime();
                for (int i = mListAdapter.getCount() - 1; i >= 0; i--) {
                    UIMessage uiMessage = mListAdapter.getItem(i);
                    if (uiMessage.getMessageDirection().equals(io.rong.imlib.model.Message.MessageDirection.SEND)
                            && (uiMessage.getSentStatus() == io.rong.imlib.model.Message.SentStatus.SENT)
                            && ntfTime >= uiMessage.getSentTime()) {
                        uiMessage.setSentStatus(io.rong.imlib.model.Message.SentStatus.READ);
                        int first = mList.getFirstVisiblePosition();
                        int last = mList.getLastVisiblePosition();
                        int position = getPositionInListView(i);
                        if (position >= first && position <= last) {
                            mListAdapter.getView(i, getListViewChildAt(i), mList);
                        }
                    }
                }
            }
        }
    }

    public void onEventMainThread(final Event.ShowDurnDialogEvent event) {
        new BurnHintDialog().show(this.getFragmentManager());
    }

    /**
     * 撤回消息点击重新编辑的事件
     *
     * @param event 事件
     */
    public void onEventMainThread(final Event.RecallMessageEditClickEvent event) {
        io.rong.imlib.model.Message message = event.getMessage();
        if (message != null && message.getConversationType() == mConversationType
                && mTargetId.equals(message.getTargetId())) {
            MessageContent messageContent = message.getContent();
            if (messageContent instanceof RecallNotificationMessage) {
                String content = ((RecallNotificationMessage) messageContent).getRecallContent();
                if (!TextUtils.isEmpty(content)) {
                    insertToEditText(content, mRongExtension.getInputEditText());
                    if (mVoiceInputToggle != null && mVoiceToggle != null && mRongExtension != null) {
                        int visibility = mVoiceInputToggle.getVisibility();
                        if (visibility == View.VISIBLE) {
                            mVoiceToggle.performClick();
                        } else {
                            mRongExtension.showSoftInput();
                        }

                    }

                }
            }
        }
    }

    private void insertToEditText(String content, EditText editText) {
        int len = content.length();
        int cursorPos = editText.getSelectionStart();
        editText.getEditableText().insert(cursorPos, content);
        editText.setSelection(cursorPos + len);
    }


    /**
     * 获取会话界面消息展示适配器。
     *
     * @return 消息适配器
     */
    public MessageListAdapter getMessageAdapter() {
        return mListAdapter;
    }

    /**
     * 接收到消息，先调用此方法，检查是否可以更新该消息。
     * 如果可以更新，则返回 true，否则返回 false。
     * 注意：开发者可以重写此方法，来控制是否更新。
     *
     * @param message 接收到的消息体。
     * @param left    剩余的消息数量。
     * @return 根据返回值确定是否更新对应会话信息。
     */
    public boolean shouldUpdateMessage(io.rong.imlib.model.Message message, int left) {
        return true;
    }

    /**
     * 加载本地历史消息。
     * 开发者可以通过重写此方法来加入自己需要在界面上展示的消息数据。
     * 重写方式: 1. 自定义类(如: MyConversationFragment) 继承自 ConversationFragment
     * 2. 集成自定义类(MyConversationFragment), 集成方式同 ConversationFragment
     * 3. 在自定义类(MyConversationFragment)中重写此方法。
     * 重写此方法后，如果需要同时显示 sdk 中的消息，必须执行 super.getHistoryMessage().
     * <p/>
     * 注意：通过 callback 返回的数据要保证在 UI 线程返回
     *
     * @param conversationType 会话类型
     * @param targetId         会话 id
     * @param lastMessageId    最后一条消息 id
     * @param reqCount         加载数量
     * @param callback         数据加载后，通过回调返回数据
     *                         <p>上拉加载时，返回数据中包含当前消息；例：当前消息 id 为 500， before = 0 after = 10，返回
     *                         510 - 500 ，按消息 id 倒序。</p>
     */
    public void getHistoryMessage(final Conversation.ConversationType conversationType, final String targetId, int lastMessageId, final int reqCount, LoadMessageDirection direction, final IHistoryDataResultCallback<List<io.rong.imlib.model.Message>> callback, boolean isNewMentionMessage) {
        if (direction == LoadMessageDirection.UP) {
            RongIMClient.getInstance().getHistoryMessages(conversationType, targetId, lastMessageId, reqCount, new RongIMClient.ResultCallback<List<io.rong.imlib.model.Message>>() {
                @Override
                public void onSuccess(List<io.rong.imlib.model.Message> messages) {
                    if (callback != null) {
                        callback.onResult(messages);
                    }
                }

                @Override
                public void onError(RongIMClient.ErrorCode e) {
                    RLog.e(TAG, "getHistoryMessages " + e);
                    if (callback != null) {
                        callback.onResult(null);
                    }
                }
            });
        } else {
            int before = DEFAULT_HISTORY_MESSAGE_COUNT, after = DEFAULT_HISTORY_MESSAGE_COUNT;
            //如果不是新增的 @ 消息
            if (!isNewMentionMessage) {
                if (mListAdapter.getCount() > 0 || indexMessageTime != 0 || isClickUnread) {
                    after = DEFAULT_HISTORY_MESSAGE_COUNT;
                    before = 0;
                }
            }
            RongIMClient.getInstance().getHistoryMessages(conversationType, targetId, indexMessageTime, before, after, new RongIMClient.ResultCallback<List<io.rong.imlib.model.Message>>() {
                @Override
                public void onSuccess(List<io.rong.imlib.model.Message> messages) {
                    if (callback != null) {
                        callback.onResult(filterDestructionMessage(conversationType, targetId, messages));
                    }
                    if (messages != null && messages.size() > 0 && mHasMoreLocalMessagesDown) {
                        indexMessageTime = messages.get(0).getSentTime();
                    } else {
                        indexMessageTime = 0;
                    }
                }

                @Override
                public void onError(RongIMClient.ErrorCode e) {
                    RLog.e(TAG, "getHistoryMessages " + e);
                    if (callback != null) {
                        callback.onResult(null);
                    }
                    indexMessageTime = 0;
                }
            });
        }
    }

    private List<io.rong.imlib.model.Message> filterDestructionMessage(Conversation.ConversationType conversationType, String targetId, List<io.rong.imlib.model.Message> messages) {
        if (messages == null) {
            return null;
        }
        List<io.rong.imlib.model.Message> messageList = new ArrayList<>();
        List<io.rong.imlib.model.Message> destructionMessages = new ArrayList<>();
        for (io.rong.imlib.model.Message message : messages) {
            if (message.getContent().isDestruct() && message.getReadTime() > 0) {
                //发送方超时删除
                if (message.getMessageDirection().equals(io.rong.imlib.model.Message.MessageDirection.RECEIVE)) {
                    long delay = (System.currentTimeMillis() - RongIMClient.getInstance().getDeltaTime() - message.getReadTime()) / 1000;
                    if (delay >= message.getContent().getDestructTime()) {
                        destructionMessages.add(message);
                        continue;
                    }
                } else {
                    //接受方已读直接删除
                    destructionMessages.add(message);
                    continue;
                }
            }
            messageList.add(message);
        }
        if (destructionMessages.size() > 0) {
            io.rong.imlib.model.Message[] deleteMessages = new io.rong.imlib.model.Message[destructionMessages.size()];
            destructionMessages.toArray(deleteMessages);
            DestructManager.getInstance().deleteMessages(conversationType, targetId, deleteMessages);
        }
        return messageList;
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.rc_mention_message_layout) {
            if (mMentionMessages.size() > 0) {
                io.rong.imlib.model.Message message = mMentionMessages.get(0);
                int position = mListAdapter.findPosition(message.getMessageId());
                if (position >= 0) {
                    mList.setSelection(position + 1);
                } else {
                    boolean isNewMentionMessage = false;
                    if (mListAdapter.getCount() > 0) {
                        UIMessage lastMessage = mListAdapter.getItem(mListAdapter.getCount() - 1);
                        isNewMentionMessage = message.getSentTime() > lastMessage.getSentTime();
                    }
                    mListAdapter.clear();
                    indexMessageTime = message.getSentTime();
                    if (isNewMentionMessage) {
                        getHistoryMessage(mConversationType, mTargetId, DEFAULT_HISTORY_MESSAGE_COUNT, AutoRefreshListView.Mode.END, SCROLL_MODE_NORMAL, message.getMessageId(), isNewMentionMessage);
                    } else {

                        getHistoryMessage(mConversationType, mTargetId, DEFAULT_HISTORY_MESSAGE_COUNT,
                                AutoRefreshListView.Mode.END, SCROLL_MODE_TOP, message.getMessageId(), isNewMentionMessage);
                    }
                }
            }
            processMentionLayout();
        }
    }

    protected enum LoadMessageDirection {
        DOWN, UP
    }

    private int conversationUnreadCount;
    private boolean isClickUnread = false;

    /**
     * @param conversationType    会话类型
     * @param targetId            目标id
     * @param reqCount            下拉加载条目数
     * @param mode                start，end ，both 模式
     * @param scrollMode          滚动模式
     * @param messageId           消息id
     * @param isNewMentionMessage 是否是新增的 @ 消息，false:拉取10条消息，并且定位到列表最后一条，true，拉取20条消息，并定位到 @ 消息
     */
    private void getHistoryMessage(final Conversation.ConversationType conversationType, final String targetId,
                                   final int reqCount, AutoRefreshListView.Mode mode,
                                   final int scrollMode, int messageId, final boolean isNewMentionMessage) {
        mList.onRefreshStart(mode);
        final int getHistoryCount;
        if (conversationType.equals(Conversation.ConversationType.CHATROOM)) {

            int count = getResources().getInteger(R.integer.rc_chatroom_first_pull_message_count);
            if (count == 0) {
                //等于0取默认值
                getHistoryCount = 10;
            } else if (count == -1) {
                //不拉取历史消息直接返回
                mList.onRefreshComplete(0, 0, false);
                return;
            } else {
                getHistoryCount = count;
            }

        } else {
            getHistoryCount = reqCount;
        }
        int fromMsgId = messageId;
        if (messageId < 0) {
            if (mListAdapter.getCount() == 0) {
                fromMsgId = -1;
            } else {
                if (mListAdapter.getItem(0).getMessage().getContent() instanceof HistoryDividerMessage) {
                    fromMsgId = firstUnreadMessage.getMessageId();
                } else {
                    fromMsgId = mListAdapter.getItem(0).getMessageId();
                }
            }
        }
        final int last = fromMsgId;
        final LoadMessageDirection direction = mode == AutoRefreshListView.Mode.START ? LoadMessageDirection.UP : LoadMessageDirection.DOWN;

        getHistoryMessage(conversationType, targetId, last, getHistoryCount, direction, new IHistoryDataResultCallback<List<io.rong.imlib.model.Message>>() {
            @Override
            public void onResult(final List<io.rong.imlib.model.Message> messages) {
                if (mConversation == null && messages != null && messages.size() > 0) {
                    RongIMClient.getInstance().sendReadReceiptMessage(mConversationType, mTargetId,
                            System.currentTimeMillis() - RongIMClient.getInstance().getDeltaTime(), null);
                }
                int msgCount = messages == null ? 0 : messages.size();
                RLog.i(TAG, "getHistoryMessage " + msgCount);
                if (direction == LoadMessageDirection.DOWN) {
                    mList.onRefreshComplete(msgCount > 1 ? msgCount : 0, msgCount, false);
                    mHasMoreLocalMessagesDown = msgCount > 1;
                    if (isNewMentionMessage) {
                        mShouldInsertMsg = msgCount < DEFAULT_HISTORY_MESSAGE_COUNT * 2 + 1;
                    } else {
                        mShouldInsertMsg = msgCount < reqCount + 1;
                    }
                } else {
                    mList.onRefreshComplete(msgCount, getHistoryCount, false);
                    mHasMoreLocalMessagesUp = msgCount == getHistoryCount;
                }
                if (messages != null && messages.size() > 0) {
                    int index = 0;
                    if (direction == LoadMessageDirection.DOWN) {
                        index = mListAdapter.getCount();
                    }
                    boolean needRefresh = false;
                    DestructionCmdMessage destructionCmdMessage = new DestructionCmdMessage();
                    for (io.rong.imlib.model.Message message : messages) {

                        HQVoiceMsgDownloadManager.getInstance().enqueue(ConversationFragment.this,
                                new AutoDownloadEntry(message, AutoDownloadEntry.DownloadPriority.HIGH)
                        );

                        if (message.getMessageDirection() == io.rong.imlib.model.Message.MessageDirection.RECEIVE
                                && message.getContent().isDestruct()
                                && !TextUtils.isEmpty(message.getUId())
                                && message.getReadTime() <= 0) {
                            DestructionTag destructionTag = message.getContent()
                                    .getClass().getAnnotation(DestructionTag.class);

                            if (destructionTag != null
                                    && destructionTag.destructionFlag() == DestructionTag.FLAG_COUNT_DOWN_WHEN_VISIBLE) {
                                destructionCmdMessage.getBurnMessageUIds().add(message.getUId());
                                long serverTime = System.currentTimeMillis() - RongIMClient.getInstance().getDeltaTime();
                                RongIMClient.getInstance()
                                        .setMessageReadTime(message.getMessageId(), serverTime, null);
                                message.setReadTime(serverTime);
                            }
                        }

                        boolean contains = false;
                        for (int i = 0; i < mListAdapter.getCount(); i++) {
                            contains = mListAdapter.getItem(i).getMessageId() == message.getMessageId();
                            if (contains) break;
                        }
                        if (!contains) {
                            UIMessage uiMessage = UIMessage.obtain(message);
                            if (message.getContent() != null && message.getContent().getUserInfo() != null) {
                                uiMessage.setUserInfo(message.getContent().getUserInfo());
                            }
                            if (message.getContent() instanceof CSPullLeaveMessage) {
                                uiMessage.setCsConfig(mCustomServiceConfig);
                            }
                            mListAdapter.add(uiMessage, index);
                            needRefresh = true;
                        }
                    }

                    if (firstUnreadMessage != null) {
                        refreshUnreadUI();
                    }
                    refreshUI(messages, needRefresh, scrollMode, last, direction, isNewMentionMessage);
                    if (destructionCmdMessage.getBurnMessageUIds().size() > 0) {
                        MessageBufferPool.getInstance().putMessageInBuffer(
                                io.rong.imlib.model.Message.obtain(mTargetId, mConversationType, destructionCmdMessage));
                    }
                }
            }

            @Override
            public void onError() {
                mList.onRefreshComplete(getHistoryCount, getHistoryCount, false);
            }
        }, isNewMentionMessage);
    }

    private void refreshUnreadUI() {
        if (!showAboveIsHistoryMessage()) {
            return;
        }
        int insertPosition;
        boolean historyDividerInserted = false;
        if (firstUnreadMessage == null) {
            return;
        }
        if (conversationUnreadCount > SHOW_UNREAD_MESSAGE_COUNT) {
            io.rong.imlib.model.Message hisMessage = io.rong.imlib.model.Message.obtain(mTargetId, mConversationType,
                    HistoryDividerMessage.obtain(getResources().getString(R.string.rc_new_message_divider_content)));
            UIMessage hisUIMessage = UIMessage.obtain(hisMessage);
            insertPosition = mListAdapter.findPosition(firstUnreadMessage.getMessageId());
            if (insertPosition == 0) {//如果正好30条未读消息，那么取前一条消息的sendTime作为插入"以上是历史消息"的时间，保证排列顺序
                List<io.rong.imlib.model.Message> msgs = RongIM.getInstance().getHistoryMessages(mConversationType,
                        mTargetId, firstUnreadMessage.getMessageId(), 1);
                if (msgs != null && msgs.size() == 1) {
                    hisUIMessage.setSentTime(msgs.get(0).getSentTime());
                    mListAdapter.add(hisUIMessage, insertPosition);
                    historyDividerInserted = true;
                }
                conversationUnreadCount = 0;
            } else if (insertPosition > 0) {
                hisUIMessage.setSentTime(mListAdapter.getItem(insertPosition - 1).getSentTime());
                mListAdapter.add(hisUIMessage, insertPosition);
                historyDividerInserted = true;
                conversationUnreadCount = 0;
            }

        }

        if (isClickUnread) {
            isClickUnread = false;
            if (!historyDividerInserted && getActivity() != null && isAdded()) {
                io.rong.imlib.model.Message hisMessage = io.rong.imlib.model.Message.obtain(mTargetId, mConversationType,
                        HistoryDividerMessage.obtain(getActivity().getResources().getString(R.string.rc_new_message_divider_content)));
                UIMessage hisUIMessage = UIMessage.obtain(hisMessage);
                insertPosition = mListAdapter.findPosition(firstUnreadMessage.getMessageId());
                if (insertPosition == 0) {
                    List<io.rong.imlib.model.Message> msgList = RongIM.getInstance().getHistoryMessages(mConversationType, mTargetId,
                            firstUnreadMessage.getMessageId(), 1);
                    long sentTime = 0;
                    if (msgList != null && msgList.size() == 1) {
                        sentTime = msgList.get(0).getSentTime();
                    }
                    if (sentTime > 0) {
                        hisUIMessage.setSentTime(sentTime);
                        mListAdapter.add(hisUIMessage, insertPosition);
                    }
                } else if (insertPosition > 0) {
                    hisUIMessage.setSentTime(mListAdapter.getItem(insertPosition - 1).getSentTime());
                    mListAdapter.add(hisUIMessage, insertPosition);
                }
            }
            conversationUnreadCount = 0;
        }
    }

    private void refreshUI(List<io.rong.imlib.model.Message> messages, boolean needRefresh, int scrollMode, int last, LoadMessageDirection direction, boolean isNewMentionMessage) {
        if (needRefresh) {
            mListAdapter.notifyDataSetChanged();
            if (SCROLL_MODE_TOP == scrollMode) {
                mList.setSelection(0);
            } else if (scrollMode == SCROLL_MODE_BOTTOM) {
                if (last != -1 || mSavedInstanceState == null) {
                    mList.setSelection(mList.getCount());
                } else { // 旋转屏幕后，列表滚动到旋转前的位置
                    mList.onRestoreInstanceState(mListViewState);
                }
            } else {
                if (direction == LoadMessageDirection.DOWN) {
                    if (!isNewMentionMessage) {
                        int selected = mList.getSelectedItemPosition();
                        if (selected <= 0) {
                            for (int i = 0; i < mListAdapter.getCount(); i++) {
                                if (mListAdapter.getItem(i).getSentTime() == indexMessageTime) {
                                    mList.setSelection(i);
                                    break;
                                }
                            }
                        } else {
                            mList.setSelection(mListAdapter.getCount() - messages.size());
                        }
                    } else {
                        if (mMentionMessages.size() > 0) {
                            io.rong.imlib.model.Message message = mMentionMessages.get(0);
                            int position = mListAdapter.findPosition(message.getMessageId());
                            if (position >= 0) {
                                mList.setSelection(position + 1);
                            }

                        }
                    }
                } else {
                    mList.setSelection(messages.size() + 1);
                }
            }
        }
        sendReadReceiptResponseIfNeeded(messages);
        if (RongContext.getInstance().getUnreadMessageState()
                && last == -1 && mUnReadCount > SHOW_UNREAD_MESSAGE_COUNT) {
            mList.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mList.addOnScrollListener(mOnScrollListener);
                }
            }, 100);

        }
    }

    /**
     * 加载服务器远端历史消息。
     * 此功能需要开通 “历史消息云存储” 服务后，才可以使用。
     * 开发者可以通过重写此方法，加入自己需要在界面上要展示的消息数据。
     * 重写此方法后，如果不显示 sdk 中的消息，可以通过不执行 super.getRemoteHistoryMessages()。
     * <p/>
     * 注意：通过 callback 返回的数据要保证在 UI 线程返回。
     *
     * @param conversationType 会话类型
     * @param targetId         会话 id
     * @param dateTime         从该时间点开始获取消息。即：消息中的 sentTime；第一次可传 0，获取最新 count 条。
     * @param reqCount         加载数量
     * @param callback         数据加载后，通过回调返回数据
     */
    public void getRemoteHistoryMessages(final Conversation.ConversationType conversationType, final String targetId, final long dateTime, final int reqCount, final IHistoryDataResultCallback<List<io.rong.imlib.model.Message>> callback) {
        RongIMClient.getInstance().getRemoteHistoryMessages(conversationType, targetId, dateTime, reqCount, new RongIMClient.ResultCallback<List<io.rong.imlib.model.Message>>() {
            @Override
            public void onSuccess(List<io.rong.imlib.model.Message> messages) {
                if (callback != null) {
                    callback.onResult(messages);
                }
            }

            @Override
            public void onError(RongIMClient.ErrorCode e) {
                RLog.e(TAG, "getRemoteHistoryMessages " + e);
                if (callback != null) {
                    callback.onResult(null);
                }
            }
        });
    }

    private void getRemoteHistoryMessages(final Conversation.ConversationType conversationType, final String targetId, final int reqCount) {
        mList.onRefreshStart(AutoRefreshListView.Mode.START);
        final int getHistoryCount;
        if (conversationType.equals(Conversation.ConversationType.CHATROOM)) {
            int count = getResources().getInteger(R.integer.rc_chatroom_first_pull_message_count);
            if (count == 0) {
                //等于0取默认值
                getHistoryCount = 10;
            } else if (count == -1) {
                //不拉取历史消息直接返回
                mList.onRefreshComplete(0, 0, false);
                return;
            } else {
                getHistoryCount = count;
            }

        } else {
            getHistoryCount = reqCount;
        }
        long dateTime = mListAdapter.getCount() == 0 ? 0 : mListAdapter.getItem(0).getSentTime();
        getRemoteHistoryMessages(conversationType, targetId, dateTime, getHistoryCount, new IHistoryDataResultCallback<List<io.rong.imlib.model.Message>>() {
            @Override
            public void onResult(List<io.rong.imlib.model.Message> messages) {
                RLog.i(TAG, "getRemoteHistoryMessages " + (messages == null ? 0 : messages.size()));
                io.rong.imlib.model.Message lastMessage = null;
                if (messages != null && messages.size() > 0) {
                    if (mListAdapter.getCount() == 0) {
                        lastMessage = messages.get(0);
                    }
                    List<UIMessage> remoteList = new ArrayList<>();
                    for (io.rong.imlib.model.Message message : messages) {
                        HQVoiceMsgDownloadManager.getInstance().enqueue(ConversationFragment.this,
                                new AutoDownloadEntry(message, AutoDownloadEntry.DownloadPriority.HIGH)
                        );

                        if (message.getMessageId() > 0) {
                            UIMessage uiMessage = UIMessage.obtain(message);
                            if (message.getContent() instanceof CSPullLeaveMessage) {
                                uiMessage.setCsConfig(mCustomServiceConfig);
                            }
                            if (message.getContent() != null && message.getContent().getUserInfo() != null) {
                                uiMessage.setUserInfo(message.getContent().getUserInfo());
                            }
                            remoteList.add(uiMessage);
                        }
                    }
                    remoteList = filterMessage(remoteList);
                    if (remoteList != null && remoteList.size() > 0) {
                        for (UIMessage uiMessage : remoteList) {
                            uiMessage.setSentStatus(io.rong.imlib.model.Message.SentStatus.READ);
                            mListAdapter.add(uiMessage, 0);
                        }
                        mListAdapter.notifyDataSetChanged();
                        mList.setSelection(messages.size() + 1);
                        sendReadReceiptResponseIfNeeded(messages);
                        mList.onRefreshComplete(messages.size(), getHistoryCount, false);
                        if (lastMessage != null) {
                            RongContext.getInstance().getEventBus().post(lastMessage);
                        }
                    } else {
                        mList.onRefreshComplete(0, getHistoryCount, false);
                    }
                } else {
                    mList.onRefreshComplete(0, getHistoryCount, false);
                }
            }

            @Override
            public void onError() {
                mList.onRefreshComplete(0, getHistoryCount, false);
            }
        });
    }

    private List<UIMessage> filterMessage(List<UIMessage> srcList) {
        List<UIMessage> destList;
        if (mListAdapter.getCount() > 0) {
            destList = new ArrayList<>();
            for (int i = 0; i < mListAdapter.getCount(); i++) {
                for (UIMessage msg : srcList) {
                    if (destList.contains(msg)) continue;
                    if (msg.getMessageId() != mListAdapter.getItem(i).getMessageId()) {
                        destList.add(msg);
                    }
                }
            }
        } else {
            destList = srcList;
        }
        return destList;
    }

    private void getMentionedMessage(Conversation.ConversationType conversationType, String targetId) {
        RongIMClient.getInstance().getUnreadMentionedMessages(conversationType, targetId, new RongIMClient.ResultCallback<List<io.rong.imlib.model.Message>>() {
            @Override
            public void onSuccess(List<io.rong.imlib.model.Message> messages) {
                if (messages != null && messages.size() > 0) {
                    mMentionMessages = messages;
                    processMentionLayout();
                }
                RongIM.getInstance().clearMessagesUnreadStatus(mConversationType, mTargetId, null);
            }

            @Override
            public void onError(RongIMClient.ErrorCode e) {
                RongIM.getInstance().clearMessagesUnreadStatus(mConversationType, mTargetId, null);
            }
        });
    }


    private void sendReadReceiptResponseIfNeeded(List<io.rong.imlib.model.Message> messages) {
        if (mReadRec &&
                (mConversationType.equals(Conversation.ConversationType.GROUP) ||
                        mConversationType.equals(Conversation.ConversationType.DISCUSSION)) &&
                RongContext.getInstance().isReadReceiptConversationType(mConversationType)) {
            List<io.rong.imlib.model.Message> responseMessageList = new ArrayList<>();
            for (io.rong.imlib.model.Message message : messages) {
                ReadReceiptInfo readReceiptInfo = message.getReadReceiptInfo();
                if (readReceiptInfo == null) {
                    continue;
                }
                if (readReceiptInfo.isReadReceiptMessage() && !readReceiptInfo.hasRespond()) {
                    responseMessageList.add(message);
                }
            }
            if (responseMessageList.size() > 0) {
                RongIMClient.getInstance().sendReadReceiptResponse(mConversationType, mTargetId, responseMessageList, null);
            }
        }
    }

    @Override
    public void onExtensionCollapsed() {

    }

    @Override
    public void onExtensionExpanded(int h) {
        // 非人为触摸，触发的回调，不执行下面的逻辑
        if (mRongExtension.getTriggerMode() != RongExtension.TRIGGER_MODE_TOUCH) {
            return;
        }
        if (indexMessageTime > 0) {
            mListAdapter.clear();
            if (firstUnreadMessage == null) {
                indexMessageTime = 0;
            }
            conversationUnreadCount = mUnReadCount;
            getHistoryMessage(mConversationType, mTargetId, DEFAULT_HISTORY_MESSAGE_COUNT, AutoRefreshListView.Mode.START, SCROLL_MODE_NORMAL, -1, false);
        } else {
            mList.setSelection(mList.getCount());
            if (mNewMessageCount > 0) {
                mNewMessageCount = 0;
                if (mNewMessageBtn != null) {
                    mNewMessageBtn.setVisibility(View.GONE);
                    mNewMessageTextView.setVisibility(View.GONE);
                }
            }
        }
    }

    /**
     * 开启客服，进入客服会话界面时，会回调此方法
     * 开发者可以重写此方法，修改启动客服的行为
     *
     * @param targetId 客服 id
     */
    public void onStartCustomService(String targetId) {
        csEnterTime = System.currentTimeMillis();
        mRongExtension.setExtensionBarMode(CustomServiceMode.CUSTOM_SERVICE_MODE_NO_SERVICE);
        RongIMClient.getInstance().startCustomService(targetId, customServiceListener, mCustomUserInfo);
    }

    /**
     * 会话结束时，回调此方法。
     * 开发者可以重写此方法，修改结束客服的行为
     *
     * @param targetId 客服 id
     */
    public void onStopCustomService(String targetId) {
        RongIMClient.getInstance().stopCustomService(targetId);
    }

    @Override
    final public void onEvaluateSubmit() {
        if (mEvaluateDialg != null) {
            mEvaluateDialg.destroy();
            mEvaluateDialg = null;
        }
        if (mCustomServiceConfig != null && mCustomServiceConfig.quitSuspendType.equals(CustomServiceConfig.CSQuitSuspendType.NONE)) {
            if (getActivity() != null) {
                getActivity().finish();
            }
        }
    }

    @Override
    final public void onEvaluateCanceled() {
        if (mEvaluateDialg != null) {
            mEvaluateDialg.destroy();
            mEvaluateDialg = null;
        }
        if (mCustomServiceConfig != null && mCustomServiceConfig.quitSuspendType.equals(CustomServiceConfig.CSQuitSuspendType.NONE)) {
            if (getActivity() != null) {
                getActivity().finish();
            }
        }

    }

    private void startTimer(int event, int interval) {
        getHandler().removeMessages(event);
        getHandler().sendEmptyMessageDelayed(event, interval);
    }

    private void stopTimer(int event) {
        getHandler().removeMessages(event);
    }

    public Conversation.ConversationType getConversationType() {
        return mConversationType;
    }

    public String getTargetId() {
        return mTargetId;
    }
}

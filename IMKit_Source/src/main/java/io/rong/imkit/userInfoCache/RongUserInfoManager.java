package io.rong.imkit.userInfoCache;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;

import java.io.File;
import java.util.List;

import io.rong.common.RLog;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.cache.RongCache;
import io.rong.imkit.model.GroupUserInfo;
import io.rong.imkit.utils.StringUtils;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Discussion;
import io.rong.imlib.model.Group;
import io.rong.imlib.model.PublicServiceProfile;
import io.rong.imlib.model.UserInfo;

public class RongUserInfoManager implements Handler.Callback {
    private final static String TAG = "RongUserInfoManager";

    private static final int USER_CACHE_MAX_COUNT = 256;
    private static final int PUBLIC_ACCOUNT_CACHE_MAX_COUNT = 64;
    private static final int GROUP_CACHE_MAX_COUNT = 128;
    private static final int DISCUSSION_CACHE_MAX_COUNT = 16;

    private final static int EVENT_GET_USER_INFO = 2;
    private final static int EVENT_GET_GROUP_INFO = 3;
    private final static int EVENT_GET_GROUP_USER_INFO = 4;
    private final static int EVENT_GET_DISCUSSION = 5;
    private final static int EVENT_UPDATE_USER_INFO = 7;
    private final static int EVENT_UPDATE_GROUP_USER_INFO = 8;
    private final static int EVENT_UPDATE_GROUP_INFO = 9;
    private final static int EVENT_UPDATE_DISCUSSION = 10;
    private final static int EVENT_LOGOUT = 11;
    private final static int EVENT_CLEAR_CACHE = 12;
    private final static String GROUP_PREFIX = "groups";

    private RongDatabaseDao mRongDatabaseDao;
    private RongCache<String, UserInfo> mUserInfoCache;
    private RongCache<String, GroupUserInfo> mGroupUserInfoCache;
    private RongCache<String, RongConversationInfo> mGroupCache;
    private RongCache<String, RongConversationInfo> mDiscussionCache;
    private RongCache<String, PublicServiceProfile> mPublicServiceProfileCache;
    private RongCache<String, String> mRequestCache;
    private IRongCacheListener mCacheListener;
    private boolean mIsCacheUserInfo = true;
    private boolean mIsCacheGroupInfo = true;
    private boolean mIsCacheGroupUserInfo = true;
    private Handler mWorkHandler;
    private String mAppKey;
    private String mUserId;
    private boolean mInitialized;
    private Context mContext;

    private static class SingletonHolder {
        static RongUserInfoManager sInstance = new RongUserInfoManager();
    }

    private RongUserInfoManager() {
        mUserInfoCache = new RongCache<>(USER_CACHE_MAX_COUNT);
        mGroupUserInfoCache = new RongCache<>(USER_CACHE_MAX_COUNT);
        mGroupCache = new RongCache<>(GROUP_CACHE_MAX_COUNT);
        mDiscussionCache = new RongCache<>(DISCUSSION_CACHE_MAX_COUNT);
        mRequestCache = new RongCache<>(PUBLIC_ACCOUNT_CACHE_MAX_COUNT);
        mPublicServiceProfileCache = new RongCache<>(PUBLIC_ACCOUNT_CACHE_MAX_COUNT);
        HandlerThread workThread = new HandlerThread("RongUserInfoManager");
        workThread.start();
        mWorkHandler = new Handler(workThread.getLooper(), this);
        mInitialized = false;
    }

    public void setIsCacheUserInfo(boolean mIsCacheUserInfo) {
        this.mIsCacheUserInfo = mIsCacheUserInfo;
    }

    public void setIsCacheGroupInfo(boolean mIsCacheGroupInfo) {
        this.mIsCacheGroupInfo = mIsCacheGroupInfo;
    }

    public void setIsCacheGroupUserInfo(boolean mIsCacheGroupUserInfo) {
        this.mIsCacheGroupUserInfo = mIsCacheGroupUserInfo;
    }

    public static RongUserInfoManager getInstance() {
        return SingletonHolder.sInstance;
    }

    @Override
    public boolean handleMessage(Message msg) {
        String userId;
        // 根据 lib 层获取的登录用户 id, 初始化数据库。
        if (TextUtils.isEmpty(mUserId)) {
            if (!TextUtils.isEmpty(RongIMClient.getInstance().getCurrentUserId())) {
                mUserId = RongIMClient.getInstance().getCurrentUserId();
                RLog.i(TAG, "userId:" + mUserId);
                mRongDatabaseDao = new RongDatabaseDao();
                mRongDatabaseDao.open(mContext, mAppKey, mUserId);
            } else {
                RLog.i(TAG, "user hasn't connected, return directly!");
                return true;
            }
        } else if (!mUserId.equals(RongIMClient.getInstance().getCurrentUserId())) {
            clearUserInfoCache();
            RLog.d(TAG, "user changed, old userId = " + mUserId + ", current userId = " + RongIMClient.getInstance().getCurrentUserId());
            mUserId = RongIMClient.getInstance().getCurrentUserId();
            if (mRongDatabaseDao != null) {
                mRongDatabaseDao.close();
                mRongDatabaseDao.open(mContext, mAppKey, mUserId);
            }
        }
        switch (msg.what) {
            case EVENT_GET_GROUP_INFO:
                String groupId = (String) msg.obj;
                Group group = null;
                String cacheGroupId = GROUP_PREFIX + groupId;
                if (mRongDatabaseDao != null) {
                    group = mRongDatabaseDao.getGroupInfo(groupId);
                }
                if (group != null && group.getPortraitUri() != null) {
                    Uri uri = group.getPortraitUri();
                    if (uri.toString().toLowerCase().startsWith("file://")) {
                        File file = new File(uri.toString().substring(7));
                        if (!file.exists()) {
                            group = null;
                        }
                    } else if (uri.toString().equals("")) {
                        group = null;
                    }
                }
                if (group == null) {
                    if (mCacheListener != null) {
                        group = mCacheListener.getGroupInfo(groupId);
                    }
                    if (group != null) {
                        if (mRongDatabaseDao != null) {
                            mRongDatabaseDao.putGroupInfo(group);
                        }
                    }
                }
                if (group != null) {
                    RongConversationInfo conversationInfo = new RongConversationInfo(Conversation.ConversationType.GROUP.getValue() + "", group.getId(), group.getName(), group.getPortraitUri());
                    mGroupCache.put(groupId, conversationInfo);
                    mRequestCache.remove(cacheGroupId);
                    if (mCacheListener != null) {
                        mCacheListener.onGroupUpdated(group);
                    }
                }
                break;
            case EVENT_GET_GROUP_USER_INFO:
                GroupUserInfo groupUserInfo = null;
                groupId = StringUtils.getArg1((String) msg.obj);
                userId = StringUtils.getArg2((String) msg.obj);
                if (mRongDatabaseDao != null) {
                    groupUserInfo = mRongDatabaseDao.getGroupUserInfo(groupId, userId);
                }
                if (groupUserInfo == null) {
                    if (mCacheListener != null) {
                        groupUserInfo = mCacheListener.getGroupUserInfo(groupId, userId);
                    }
                    if (groupUserInfo != null && mRongDatabaseDao != null) {
                        mRongDatabaseDao.putGroupUserInfo(groupUserInfo);
                    }
                }
                if (groupUserInfo != null) {
                    mGroupUserInfoCache.put((String) msg.obj, groupUserInfo);
                    mRequestCache.remove((String) msg.obj);
                    if (mCacheListener != null) {
                        mCacheListener.onGroupUserInfoUpdated(groupUserInfo);
                    }
                }
                break;
            case EVENT_GET_DISCUSSION:
                final String discussionId = (String) msg.obj;
                Discussion discussion = null;
                if (mRongDatabaseDao != null) {
                    discussion = mRongDatabaseDao.getDiscussionInfo(discussionId);
                }
                if (discussion != null) {
                    RongConversationInfo conversationInfo = new RongConversationInfo(Conversation.ConversationType.DISCUSSION.getValue() + "", discussion.getId(), discussion.getName(), null);
                    mDiscussionCache.put(discussionId, conversationInfo);
                    if (mCacheListener != null) {
                        mCacheListener.onDiscussionUpdated(discussion);
                    }
                } else {
                    RongIM.getInstance().getDiscussion(discussionId, new RongIMClient.ResultCallback<Discussion>() {
                        @Override
                        public void onSuccess(Discussion discussion) {
                            if (discussion != null) {
                                if (mRongDatabaseDao != null) {
                                    mRongDatabaseDao.putDiscussionInfo(discussion);
                                }
                                RongConversationInfo conversationInfo = new RongConversationInfo(Conversation.ConversationType.DISCUSSION.getValue() + "", discussion.getId(), discussion.getName(), null);
                                mDiscussionCache.put(discussionId, conversationInfo);
                                if (mCacheListener != null) {
                                    mCacheListener.onDiscussionUpdated(discussion);
                                }
                            }
                        }

                        @Override
                        public void onError(RongIMClient.ErrorCode e) {
                        }
                    });
                }
                break;
            case EVENT_UPDATE_GROUP_USER_INFO:
                groupUserInfo = (GroupUserInfo) msg.obj;
                String key = StringUtils.getKey(groupUserInfo.getGroupId(), groupUserInfo.getUserId());
                final GroupUserInfo oldGroupUserInfo = mGroupUserInfoCache.put(key, groupUserInfo);
                if ((oldGroupUserInfo == null)
                        || (oldGroupUserInfo.getNickname() != null && groupUserInfo.getNickname() != null && !oldGroupUserInfo.getNickname().equals(groupUserInfo.getNickname()))) {
                    mRequestCache.remove(key);
                    if (mRongDatabaseDao != null) {
                        mRongDatabaseDao.putGroupUserInfo(groupUserInfo);
                    }
                    if (mCacheListener != null) {
                        mCacheListener.onGroupUserInfoUpdated(groupUserInfo);
                    }
                }
                break;
            case EVENT_UPDATE_GROUP_INFO:
                group = (Group) msg.obj;
                RongConversationInfo conversationInfo = new RongConversationInfo(Conversation.ConversationType.GROUP.getValue() + "", group.getId(), group.getName(), group.getPortraitUri());
                RongConversationInfo oldConversationInfo = mGroupCache.put(conversationInfo.getId(), conversationInfo);
                if (oldConversationInfo == null || oldConversationInfo.getName() == null || oldConversationInfo.getUri() == null
                        || (conversationInfo.getName() != null)
                        || (conversationInfo.getUri() != null)) {
                    String cachedGroupId = GROUP_PREFIX + group.getId();
                    mRequestCache.remove(cachedGroupId);
                    if (mRongDatabaseDao != null) {
                        mRongDatabaseDao.putGroupInfo(group);
                    }
                    if (mCacheListener != null) {
                        mCacheListener.onGroupUpdated(group);
                    }
                }
                break;
            case EVENT_UPDATE_DISCUSSION:
                discussion = (Discussion) msg.obj;
                conversationInfo = new RongConversationInfo(Conversation.ConversationType.DISCUSSION.getValue() + "", discussion.getId(), discussion.getName(), null);
                oldConversationInfo = mDiscussionCache.put(conversationInfo.getId(), conversationInfo);
                if ((oldConversationInfo == null)
                        || (oldConversationInfo.getName() != null && conversationInfo.getName() != null && !oldConversationInfo.getName().equals(conversationInfo.getName()))) {
                    if (mRongDatabaseDao != null) {
                        mRongDatabaseDao.putDiscussionInfo(discussion);
                    }
                    if (mCacheListener != null) {
                        mCacheListener.onDiscussionUpdated(discussion);
                    }
                }
                break;
            case EVENT_GET_USER_INFO:
                userId = (String) msg.obj;
                UserInfo userInfo = null;
                if (mRongDatabaseDao != null) {
                    userInfo = mRongDatabaseDao.getUserInfo(userId);
                }
                if (userInfo != null && userInfo.getPortraitUri() != null) {
                    Uri uri = userInfo.getPortraitUri();
                    if (uri.toString().toLowerCase().startsWith("file://")) {
                        File file = new File(uri.toString().substring(7));
                        if (!file.exists()) {
                            userInfo = null;
                        }
                    } else if (uri.toString().equals("")) {
                        userInfo = null;
                    }
                }
                if (userInfo == null) {
                    if (mCacheListener != null) {
                        userInfo = mCacheListener.getUserInfo(userId);
                    }
                    if (userInfo != null) {
                        putUserInfoInDB(userInfo);
                    }
                }
                if (userInfo != null) {
                    putUserInfoInCache(userInfo);
                    mRequestCache.remove(userId);
                    if (mCacheListener != null) {
                        mCacheListener.onUserInfoUpdated(userInfo);
                    }
                }
                break;
            case EVENT_UPDATE_USER_INFO:
                userInfo = (UserInfo) msg.obj;
                UserInfo oldUserInfo = putUserInfoInCache(userInfo);
                if ((oldUserInfo == null || oldUserInfo.getName() == null || oldUserInfo.getPortraitUri() == null)
                        || (userInfo.getName() != null)
                        || (userInfo.getPortraitUri() != null)) {
                    putUserInfoInDB(userInfo);
                    mRequestCache.remove(userInfo.getUserId());
                    if (mCacheListener != null) {
                        mCacheListener.onUserInfoUpdated(userInfo);
                    }
                }
                break;
            case EVENT_LOGOUT:
                clearUserInfoCache();
                mInitialized = false;
                mUserId = null;
                if (mRongDatabaseDao != null) {
                    mRongDatabaseDao.close();
                    mRongDatabaseDao = null;
                }
                break;
            case EVENT_CLEAR_CACHE:
                mRequestCache.clear();
                break;
        }
        return false;
    }

    public void init(Context context, String appKey, IRongCacheListener listener) {
        if (TextUtils.isEmpty(appKey)) {
            RLog.e(TAG, "init, appkey is null.");
            return;
        }
        if (mInitialized) {
            RLog.d(TAG, "has been init, no need init again");
            return;
        }
        mContext = context;
        mAppKey = appKey;
        mCacheListener = listener;
        mInitialized = true;
    }

    private void clearUserInfoCache() {
        if (mUserInfoCache != null) {
            mUserInfoCache.clear();
        }
        if (mDiscussionCache != null) {
            mDiscussionCache.clear();
        }
        if (mGroupCache != null) {
            mGroupCache.clear();
        }
        if (mGroupUserInfoCache != null) {
            mGroupUserInfoCache.clear();
        }
        if (mPublicServiceProfileCache != null) {
            mPublicServiceProfileCache.clear();
        }
        mRequestCache.clear();
    }

    public void uninit() {
        RLog.i(TAG, "uninit");
        mWorkHandler.sendEmptyMessage(EVENT_LOGOUT);
    }

    private UserInfo putUserInfoInCache(UserInfo info) {
        if (mUserInfoCache != null) {
            return mUserInfoCache.put(info.getUserId(), info);
        } else {
            return null;
        }
    }

    private void insertUserInfoInDB(UserInfo info) {
        if (mRongDatabaseDao != null) {
            mRongDatabaseDao.insertUserInfo(info);
        }
    }

    private void putUserInfoInDB(UserInfo info) {
        if (mRongDatabaseDao != null) {
            mRongDatabaseDao.putUserInfo(info);
        }
    }

    public UserInfo getUserInfo(final String id) {
        RLog.i(TAG, "getUserInfo : " + id);
        if (TextUtils.isEmpty(id)) {
            return null;
        }
        UserInfo info = null;

        if (mIsCacheUserInfo) {
            info = mUserInfoCache.get(id);
            if (info == null) {
                String cachedId = mRequestCache.get(id);
                if (cachedId != null) {
                    return null;
                }
                mRequestCache.put(id, id);
                Message message = Message.obtain();
                message.what = EVENT_GET_USER_INFO;
                message.obj = id;
                mWorkHandler.sendMessage(message);
                if (!mWorkHandler.hasMessages(EVENT_CLEAR_CACHE)) {
                    mWorkHandler.sendEmptyMessageDelayed(EVENT_CLEAR_CACHE, 30 * 1000);
                }
            }
        } else {
            if (mCacheListener != null) {
                info = mCacheListener.getUserInfo(id);
            }
        }
        return info;
    }

    public List<UserInfo> getAllUserInfo() {
        RLog.i(TAG, "getAllUserInfo");
        if (mRongDatabaseDao != null) {
            return mRongDatabaseDao.getAllUserInfo();
        } else {
            RLog.i(TAG, "mRongDatabaseDao is null");
            return null;
        }
    }

    public GroupUserInfo getGroupUserInfo(final String gId, final String id) {
        if (TextUtils.isEmpty(gId) || TextUtils.isEmpty(id)) {
            return null;
        }
        RLog.d(TAG, "getGroupUserInfo : " + gId + ", " + id);
        final String key = StringUtils.getKey(gId, id);
        GroupUserInfo info = null;
        if (mIsCacheGroupUserInfo) {
            info = mGroupUserInfoCache.get(key);
            if (info == null) {
                String cachedId = mRequestCache.get(key);
                if (cachedId != null) {
                    return null;
                }
                mRequestCache.put(key, key);
                Message message = Message.obtain();
                message.what = EVENT_GET_GROUP_USER_INFO;
                message.obj = key;
                mWorkHandler.sendMessage(message);
                if (!mWorkHandler.hasMessages(EVENT_CLEAR_CACHE)) {
                    mWorkHandler.sendEmptyMessageDelayed(EVENT_CLEAR_CACHE, 30 * 1000);
                }
            }
        } else {
            if (mCacheListener != null) {
                info = mCacheListener.getGroupUserInfo(gId, id);
            }
        }
        return info;
    }

    public Group getGroupInfo(final String id) {
        if (TextUtils.isEmpty(id)) {
            return null;
        }
        RLog.i(TAG, "getGroupInfo : " + id);

        Group groupInfo = null;
        if (mIsCacheGroupInfo) {
            RongConversationInfo info = mGroupCache.get(id);
            if (info == null) {
                String cachedId = mRequestCache.get(id);
                if (cachedId != null) {
                    return null;
                }
                mRequestCache.put(id, id);
                Message message = Message.obtain();
                message.what = EVENT_GET_GROUP_INFO;
                message.obj = id;
                mWorkHandler.sendMessage(message);
                if (!mWorkHandler.hasMessages(EVENT_CLEAR_CACHE)) {
                    mWorkHandler.sendEmptyMessageDelayed(EVENT_CLEAR_CACHE, 30 * 1000);
                }
            } else {
                groupInfo = new Group(info.getId(), info.getName(), info.getUri());
            }
        } else {
            if (mCacheListener != null) {
                groupInfo = mCacheListener.getGroupInfo(id);
            }
        }
        return groupInfo;
    }

    public Discussion getDiscussionInfo(final String id) {
        if (TextUtils.isEmpty(id)) {
            return null;
        }
        Discussion discussionInfo = null;
        RongConversationInfo info = mDiscussionCache.get(id);
        if (info == null) {
            Message message = Message.obtain();
            message.what = EVENT_GET_DISCUSSION;
            message.obj = id;
            mWorkHandler.sendMessage(message);
        } else {
            discussionInfo = new Discussion(info.getId(), info.getName());
        }
        return discussionInfo;
    }

    public PublicServiceProfile getPublicServiceProfile(final Conversation.PublicServiceType type, final String id) {
        if (type == null || TextUtils.isEmpty(id)) {
            return null;
        }
        final String key = StringUtils.getKey(type.getValue() + "", id);

        PublicServiceProfile info = mPublicServiceProfileCache.get(key);

        if (info == null) {
            mWorkHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (RongContext.getInstance() != null &&
                            RongContext.getInstance().getPublicServiceProfileProvider() != null) {
                        PublicServiceProfile result = RongContext.getInstance().getPublicServiceProfileProvider().getPublicServiceProfile(type, id);
                        if (result != null) {
                            mPublicServiceProfileCache.put(key, result);
                            if (mCacheListener != null) {
                                mCacheListener.onPublicServiceProfileUpdated(result);
                            }
                        }

                    } else {
                        RongIM.getInstance().getPublicServiceProfile(type, id, new RongIMClient.ResultCallback<PublicServiceProfile>() {
                            @Override
                            public void onSuccess(PublicServiceProfile result) {
                                if (result != null) {
                                    mPublicServiceProfileCache.put(key, result);
                                    if (mCacheListener != null) {
                                        mCacheListener.onPublicServiceProfileUpdated(result);
                                    }
                                }
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode e) {
                            }
                        });
                    }
                }
            });
        }
        return info;
    }

    public void setUserInfo(final UserInfo info) {
        if (mIsCacheUserInfo) {
            Message message = Message.obtain();
            message.what = EVENT_UPDATE_USER_INFO;
            message.obj = info;
            mWorkHandler.sendMessage(message);
        } else {
            if (mCacheListener != null) {
                mCacheListener.onUserInfoUpdated(info);
            }
        }
    }

    public void setGroupUserInfo(final GroupUserInfo info) {
        if (mIsCacheGroupUserInfo) {
            Message message = Message.obtain();
            message.what = EVENT_UPDATE_GROUP_USER_INFO;
            message.obj = info;
            mWorkHandler.sendMessage(message);
        } else {
            if (mCacheListener != null) {
                mCacheListener.onGroupUserInfoUpdated(info);
            }
        }
    }

    public void setGroupInfo(final Group group) {
        if (mIsCacheGroupInfo) {
            Message message = Message.obtain();
            message.what = EVENT_UPDATE_GROUP_INFO;
            message.obj = group;
            mWorkHandler.sendMessage(message);
        } else {
            if (mCacheListener != null) {
                mCacheListener.onGroupUpdated(group);
            }
        }
    }

    public void setDiscussionInfo(final Discussion discussion) {
        Message message = Message.obtain();
        message.what = EVENT_UPDATE_DISCUSSION;
        message.obj = discussion;
        mWorkHandler.sendMessage(message);
    }

    public void setPublicServiceProfile(final PublicServiceProfile profile) {
        String key = StringUtils.getKey(profile.getConversationType().getValue() + "", profile.getTargetId());
        PublicServiceProfile oldInfo = mPublicServiceProfileCache.put(key, profile);

        if ((oldInfo == null)
                || (oldInfo.getName() != null && profile.getName() != null && !oldInfo.getName().equals(profile.getName()))
                || (oldInfo.getPortraitUri() != null && profile.getPortraitUri() != null && !oldInfo.getPortraitUri().toString().equals(profile.getPortraitUri().toString()))) {
            if (mCacheListener != null) {
                mCacheListener.onPublicServiceProfileUpdated(profile);
            }
        }
    }
}

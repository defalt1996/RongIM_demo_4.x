package io.rong.imkit.mention;


import android.content.Intent;
import android.text.TextUtils;
import android.widget.EditText;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import io.rong.common.RLog;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.model.GroupUserInfo;
import io.rong.imkit.userInfoCache.RongUserInfoManager;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.MentionedInfo;
import io.rong.imlib.model.UserInfo;

public class RongMentionManager implements ITextInputListener {
    private static String TAG = "RongMentionManager";
    private Stack<MentionInstance> stack = new Stack<>();
    private RongIM.IGroupMembersProvider mGroupMembersProvider;
    private IMentionedInputListener mMentionedInputListener;
    private IAddMentionedMemberListener mAddMentionedMemberListener;

    private static class SingletonHolder {
        static RongMentionManager sInstance = new RongMentionManager();
    }

    private RongMentionManager() {

    }

    public static RongMentionManager getInstance() {
        return SingletonHolder.sInstance;
    }

    public void createInstance(Conversation.ConversationType conversationType, String targetId, EditText inputEditText) {
        RLog.i(TAG, "createInstance");
        String key = conversationType.getName() + targetId;
        MentionInstance mentionInstance;
        if (stack.size() > 0) {
            mentionInstance = stack.peek();
            if (mentionInstance.key.equals(key)) {
                return;
            }
        }

        mentionInstance = new MentionInstance();
        mentionInstance.key = key;
        mentionInstance.mentionBlocks = new ArrayList<>();
        mentionInstance.inputEditText = inputEditText;
        stack.add(mentionInstance);
    }

    public void destroyInstance(Conversation.ConversationType conversationType, String targetId) {
        RLog.i(TAG, "destroyInstance");
        if (stack.size() > 0) {
            MentionInstance instance = stack.peek();
            if (instance.key.equals(conversationType.getName() + targetId)) {
                stack.pop();
            } else {
                RLog.e(TAG, "Invalid MentionInstance : " + instance.key);
            }
        } else {
            RLog.e(TAG, "Invalid MentionInstance.");
        }
    }

    public void mentionMember(Conversation.ConversationType conversationType, String targetId, String userId) {
        RLog.d(TAG, "mentionMember " + userId);
        if (TextUtils.isEmpty(userId)
                || conversationType == null
                || TextUtils.isEmpty(targetId)
                || stack.size() == 0) {
            RLog.e(TAG, "Illegal argument");
            return;
        }
        String key = conversationType.getName() + targetId;
        MentionInstance instance = stack.peek();
        if (instance == null || !instance.key.equals(key)) {
            RLog.e(TAG, "Invalid mention instance : " + key);
            return;
        }
        UserInfo userInfo = RongUserInfoManager.getInstance().getUserInfo(userId);

        if (userInfo == null || TextUtils.isEmpty(userInfo.getUserId())) {
            RLog.e(TAG, "Invalid userInfo");
            return;
        }

        if (conversationType == Conversation.ConversationType.GROUP) {
            if (RongContext.getInstance().getGroupUserInfoProvider() != null) {
                GroupUserInfo groupUserInfo = RongContext.getInstance().getGroupUserInfoProvider().getGroupUserInfo(targetId, userId);
                if (groupUserInfo != null && groupUserInfo.getNickname() != null && !"".equals(groupUserInfo.getNickname().trim())) {
                    userInfo.setName(groupUserInfo.getNickname());
                }
            }
        }
        addMentionedMember(userInfo, 0);
    }

    public void mentionMember(UserInfo userInfo) {
        if (userInfo == null || TextUtils.isEmpty(userInfo.getUserId())) {
            RLog.e(TAG, "Invalid userInfo");
            return;
        }
        addMentionedMember(userInfo, 1);
    }

    public String getMentionBlockInfo() {
        if (stack.size() > 0) {
            MentionInstance mentionInstance = stack.peek();
            if (mentionInstance.mentionBlocks != null && !mentionInstance.mentionBlocks.isEmpty()) {
                JSONArray jsonArray = new JSONArray();
                for (MentionBlock mentionBlock : mentionInstance.mentionBlocks) {
                    jsonArray.put(mentionBlock.toJson());
                }
                return jsonArray.toString();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    void addMentionBlock(MentionBlock mentionBlock) {
        if (stack.size() > 0) {
            MentionInstance mentionInstance = stack.peek();
            mentionInstance.mentionBlocks.add(mentionBlock);
        }
    }

    /**
     * @param userInfo 用户信息
     * @param from     0 代表来自会话界面，1 来着群成员选择界面。
     */
    private void addMentionedMember(UserInfo userInfo, int from) {
        if (stack.size() > 0) {
            MentionInstance mentionInstance = stack.peek();
            EditText editText = mentionInstance.inputEditText;
            if (userInfo != null && editText != null) {
                String mentionContent;
                mentionContent = from == 0 ? "@" + userInfo.getName() + " " : userInfo.getName() + " ";
                int len = mentionContent.length();
                int cursorPos = editText.getSelectionStart();

                MentionBlock brokenBlock = getBrokenMentionedBlock(cursorPos, mentionInstance.mentionBlocks);
                if (brokenBlock != null) {
                    mentionInstance.mentionBlocks.remove(brokenBlock);
                }

                MentionBlock mentionBlock = new MentionBlock();
                mentionBlock.userId = userInfo.getUserId();
                mentionBlock.offset = false;
                mentionBlock.name = userInfo.getName();
                if (from == 1) {
                    mentionBlock.start = cursorPos - 1;
                } else {
                    mentionBlock.start = cursorPos;
                }
                mentionBlock.end = cursorPos + len;
                mentionInstance.mentionBlocks.add(mentionBlock);

                editText.getEditableText().insert(cursorPos, mentionContent);
                editText.setSelection(cursorPos + len);
                //@某人的时候弹出软键盘
                if (mAddMentionedMemberListener != null) {
                    mAddMentionedMemberListener.onAddMentionedMember(userInfo, from);
                }
                mentionBlock.offset = true;
            }
        }
    }

    private MentionBlock getBrokenMentionedBlock(int cursorPos, List<MentionBlock> blocks) {
        MentionBlock brokenBlock = null;
        for (MentionBlock block : blocks) {
            if (block.offset && cursorPos < block.end && cursorPos > block.start) {
                brokenBlock = block;
                break;
            }
        }
        return brokenBlock;
    }

    private void offsetMentionedBlocks(int cursorPos, int offset, List<MentionBlock> blocks) {
        for (MentionBlock block : blocks) {
            if (cursorPos <= block.start && block.offset) {
                block.start += offset;
                block.end += offset;
            }
            block.offset = true;
        }
    }

    private MentionBlock getDeleteMentionedBlock(int cursorPos, List<MentionBlock> blocks) {
        MentionBlock deleteBlock = null;
        for (MentionBlock block : blocks) {
            if (cursorPos == block.end) {
                deleteBlock = block;
                break;
            }
        }
        return deleteBlock;
    }

    /**
     * 当输入框文本变化时，回调此方法。
     *
     * @param conversationType 会话类型
     * @param targetId         目标 id
     * @param cursorPos        输入文本时，光标位置初始位置
     * @param offset           文本的变化量：增加时为正数，减少是为负数
     * @param text             文本内容
     */
    @Override
    public void onTextEdit(Conversation.ConversationType conversationType, String targetId, int cursorPos, int offset, String text) {
        RLog.d(TAG, "onTextEdit " + cursorPos + ", " + text);

        if (stack == null || stack.size() == 0) {
            RLog.w(TAG, "onTextEdit ignore.");
            return;
        }
        MentionInstance mentionInstance = stack.peek();
        if (!mentionInstance.key.equals(conversationType.getName() + targetId)) {
            RLog.w(TAG, "onTextEdit ignore conversation.");
            return;
        }
        //判断单个字符是否是@
        if (offset == 1) {
            if (!TextUtils.isEmpty(text)) {
                boolean showMention = false;
                String str;
                if (cursorPos == 0) {
                    str = text.substring(0, 1);
                    showMention = str.equals("@");
                } else {
                    String preChar = text.substring(cursorPos - 1, cursorPos);
                    str = text.substring(cursorPos, cursorPos + 1);
                    if (str.equals("@") && !preChar.matches("^[a-zA-Z]*") && !preChar.matches("^\\d+$")) {
                        showMention = true;
                    }
                }
                if (showMention && (mMentionedInputListener == null
                        || !mMentionedInputListener.onMentionedInput(conversationType, targetId))) {
                    Intent intent = new Intent(RongIM.getInstance().getApplicationContext(), MemberMentionedActivity.class);
                    intent.putExtra("conversationType", conversationType.getValue());
                    intent.putExtra("targetId", targetId);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    RongIM.getInstance().getApplicationContext().startActivity(intent);
                }
            }
        }

        //判断输入光标位置是否破坏已有的“@块”。
        MentionBlock brokenBlock = getBrokenMentionedBlock(cursorPos, mentionInstance.mentionBlocks);
        if (brokenBlock != null) {
            mentionInstance.mentionBlocks.remove(brokenBlock);
        }
        //改变所有有效“@块”位置。
        offsetMentionedBlocks(cursorPos, offset, mentionInstance.mentionBlocks);
    }

    @Override
    public MentionedInfo onSendButtonClick() {
        if (stack.size() > 0) {
            List<String> userIds = new ArrayList<>();
            MentionInstance curInstance = stack.peek();
            for (MentionBlock block : curInstance.mentionBlocks) {
                if (!userIds.contains(block.userId)) {
                    userIds.add(block.userId);
                }
            }
            if (userIds.size() > 0) {
                curInstance.mentionBlocks.clear();
                return new MentionedInfo(MentionedInfo.MentionedType.PART, userIds, null);
            }
        }
        return null;
    }

    @Override
    public void onDeleteClick(Conversation.ConversationType type, String targetId, EditText editText, int cursorPos) {
        RLog.d(TAG, "onTextEdit " + cursorPos);

        if (stack.size() > 0 && cursorPos > 0) {
            MentionInstance mentionInstance = stack.peek();
            if (mentionInstance.key.equals(type.getName() + targetId)) {
                MentionBlock deleteBlock = getDeleteMentionedBlock(cursorPos, mentionInstance.mentionBlocks);
                if (deleteBlock != null) {
                    mentionInstance.mentionBlocks.remove(deleteBlock);
                    String delText = deleteBlock.name;
                    int start = cursorPos - delText.length() - 1;
                    editText.getEditableText().delete(start, cursorPos);
                    editText.setSelection(start);
                }
            }
        }
    }

    public void setGroupMembersProvider(RongIM.IGroupMembersProvider groupMembersProvider) {
        mGroupMembersProvider = groupMembersProvider;
    }

    public RongIM.IGroupMembersProvider getGroupMembersProvider() {
        return mGroupMembersProvider;
    }

    public void setMentionedInputListener(IMentionedInputListener listener) {
        mMentionedInputListener = listener;
    }

    public void setAddMentionedMemberListener(IAddMentionedMemberListener listener) {
        mAddMentionedMemberListener = listener;
    }
}

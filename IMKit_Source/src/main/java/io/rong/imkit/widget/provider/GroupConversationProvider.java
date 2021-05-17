package io.rong.imkit.widget.provider;

import android.net.Uri;

import io.rong.imkit.model.ConversationProviderTag;
import io.rong.imkit.model.UIConversation;
import io.rong.imkit.userInfoCache.RongUserInfoManager;
import io.rong.imlib.model.Group;

@ConversationProviderTag(conversationType = "group", portraitPosition = 1)
public class GroupConversationProvider extends PrivateConversationProvider implements IContainerItemProvider.ConversationProvider<UIConversation> {
    @Override
    public String getTitle(String groupId) {
        String name;
        Group group = RongUserInfoManager.getInstance().getGroupInfo(groupId);
        if (group == null) {
            name = "";
        } else {
            name = group.getName();
        }
        return name;
    }

    @Override
    public Uri getPortraitUri(String id) {
        Uri uri;
        Group groupInfo = RongUserInfoManager.getInstance().getGroupInfo(id);
        if (groupInfo == null) {
            uri = null;
        } else {
            uri = groupInfo.getPortraitUri();
        }
        return uri;
    }


}

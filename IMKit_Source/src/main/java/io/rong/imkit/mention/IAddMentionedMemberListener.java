package io.rong.imkit.mention;

import io.rong.imlib.model.UserInfo;

public interface IAddMentionedMemberListener {
    boolean onAddMentionedMember(UserInfo userInfo, int from);
}

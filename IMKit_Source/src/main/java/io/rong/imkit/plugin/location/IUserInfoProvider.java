package io.rong.imkit.plugin.location;

import io.rong.imlib.model.UserInfo;

/**
 * Created by weiqinxiao on 16/9/28.
 */

public interface IUserInfoProvider {
    void getUserInfo(String userId, UserInfoCallback callback);

    interface UserInfoCallback {
        void onGotUserInfo(UserInfo userInfo);
    }
}

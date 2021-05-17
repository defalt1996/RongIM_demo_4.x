package io.rong.imkit.actions;

/**
 * Created by zwfang on 2018/4/3.
 */

public interface OnMoreActionStateListener {
    /**
     * 进入多选状态
     */
    void onShownMoreActionLayout();

    /**
     * 退出多选状态
     */
    void onHiddenMoreActionLayout();
}

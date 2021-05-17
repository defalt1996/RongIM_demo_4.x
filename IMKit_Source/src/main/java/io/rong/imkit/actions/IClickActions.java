package io.rong.imkit.actions;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.fragment.app.Fragment;

/**
 * Created by zwfang on 2018/3/29.
 */

public interface IClickActions {

    /**
     * 获取点击按钮的图标
     *
     * @param context 上下文
     * @return 图片的Drawable, 如需高亮或者置灰，则返回类型为selector, 分别显示enable或者disable状态下的drawable
     */
    Drawable obtainDrawable(Context context);

    /**
     * 图标按钮点击事件
     *
     * @param curFragment 当前的 Fragment
     */
    void onClick(Fragment curFragment);
}

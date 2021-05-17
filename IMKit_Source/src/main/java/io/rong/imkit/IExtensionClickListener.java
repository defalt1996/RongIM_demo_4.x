package io.rong.imkit;

import android.net.Uri;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.util.LinkedHashMap;

import io.rong.imkit.plugin.IPluginModule;

public interface IExtensionClickListener extends TextWatcher {
    /**
     * 点击 “发送”
     *
     * @param v    “发送” view 实例
     * @param text 输入框内容
     */
    void onSendToggleClick(View v, String text);

    /**
     * 发送图片结果
     * sdk 中集成了发送图片功能，在相册选择图片并发送后，回调此方法。
     *
     * @param selectedMedias 要发送的图片地址列表
     * @param origin         是否发送原图
     */
    void onImageResult(LinkedHashMap<String, Integer> selectedMedias, boolean origin);

    /**
     * 发送地理位置结果
     */
    void onLocationResult(double lat, double lng, String poi, Uri thumb);

    /**
     * 点击左侧按钮（例如：左侧语音切换按钮，客服时的转人工按钮）
     * 切换后，回调中携带 ViewGroup，用户可以添加自己的布局
     *
     * @param v            ”切换“ 实例
     * @param extensionBar 切换后的输入面板 ViewGroup（例如：语音输入面板，公众号时的菜单面板）
     */
    void onSwitchToggleClick(View v, ViewGroup extensionBar);

    /**
     * 点击“按住 说话”，用户可以在此回调方法中实现录音功能。
     *
     * @param v     语音输入面板 view 实例
     * @param event 点击事件
     */
    void onVoiceInputToggleTouch(View v, MotionEvent event);

    /**
     * 点击“表情” 回调.
     *
     * @param v              表情 view 实例
     * @param extensionBoard 用于展示表情的 ViewGroup
     */
    void onEmoticonToggleClick(View v, ViewGroup extensionBoard);

    /**
     * 点击 “+” 号区域, 回调中携带 ViewGroup
     *
     * @param v              “+” 号 view 实例
     * @param extensionBoard 用于展示 plugin 的 ViewGroup
     */
    void onPluginToggleClick(View v, ViewGroup extensionBoard);

    /**
     * 菜单点击回调。
     * 如果点击一级菜单，root 对应一级菜单的索引，sub 为 -1.
     * 如果点击二级菜单，sub 为二级菜单的索引，root 为 sub 所属的父菜单。
     *
     * @param root 一级菜单。
     * @param sub  二级菜单。
     */
    void onMenuClick(int root, int sub);

    /**
     * 点击 “输入框” 时回调。
     *
     * @param editText “输入框” 实例
     */
    void onEditTextClick(EditText editText);

    /**
     * Called when a hardware key is dispatched to EditText.
     *
     * @param editText The view the key has been dispatched to.
     * @param keyCode  The code for the physical key that was pressed
     * @param event    The KeyEvent object containing full information about
     *                 the event.
     * @return True if the listener has consumed the event, false otherwise.
     */
    boolean onKey(View editText, int keyCode, KeyEvent event);

    /**
     * Extension 收起。
     */
    void onExtensionCollapsed();

    /**
     * Extension 已展开。
     *
     * @param h Extension 展开后的高度。
     */
    void onExtensionExpanded(int h);

    /**
     * Plugin 点击回调。
     *
     * @param pluginModule 被点击的Plugin。
     * @param position     被点击Plugin所在的位置。
     */
    void onPluginClicked(IPluginModule pluginModule, int position);

    /**
     * 快捷回复条目点击回调
     *
     * @param phrases  短语
     * @param position 被点击的短语position
     */
    void onPhrasesClicked(String phrases, int position);
}

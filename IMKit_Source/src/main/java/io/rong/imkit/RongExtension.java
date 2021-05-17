package io.rong.imkit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;

import io.rong.common.RLog;
import io.rong.imkit.actions.IClickActions;
import io.rong.imkit.actions.IMoreClickAdapter;
import io.rong.imkit.actions.MoreClickAdapter;
import io.rong.imkit.emoticon.AndroidEmoji;
import io.rong.imkit.emoticon.EmoticonTabAdapter;
import io.rong.imkit.emoticon.IEmoticonClickListener;
import io.rong.imkit.emoticon.IEmoticonSettingClickListener;
import io.rong.imkit.emoticon.IEmoticonTab;
import io.rong.imkit.menu.ISubMenuItemClickListener;
import io.rong.imkit.menu.InputSubMenu;
import io.rong.imkit.phrases.CommonphrasesAdapter;
import io.rong.imkit.phrases.IPhrasesClickListener;
import io.rong.imkit.plugin.CombineLocationPlugin;
import io.rong.imkit.plugin.DefaultLocationPlugin;
import io.rong.imkit.plugin.IPluginClickListener;
import io.rong.imkit.plugin.IPluginModule;
import io.rong.imkit.plugin.IPluginRequestPermissionResultCallback;
import io.rong.imkit.plugin.ImagePlugin;
import io.rong.imkit.plugin.PluginAdapter;
import io.rong.imkit.utilities.ExtensionHistoryUtil;
import io.rong.imkit.utilities.KitCommonDefine;
import io.rong.imkit.utilities.PermissionCheckUtil;
import io.rong.imkit.utilities.RongUtils;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.common.DeviceUtils;
import io.rong.imlib.common.SharedPreferencesUtils;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.CustomServiceMode;

public class RongExtension extends LinearLayout implements View.OnClickListener, View.OnTouchListener {
    private final static String TAG = "RongExtension";
    private ImageView mPSMenu;
    private View mPSDivider;
    private List<InputMenu> mInputMenuList;

    private LinearLayout mMainBar;
    private View mDivider;
    private ViewGroup mExtensionBar;
    private ViewGroup mSwitchLayout;
    private ViewGroup mContainerLayout;
    private ViewGroup mPluginLayout;
    private ViewGroup mMenuContainer;

    private View mEditTextLayout;
    private EditText mEditText;
    private Button mVoiceInputToggle;
    private PluginAdapter mPluginAdapter;
    private EmoticonTabAdapter mEmotionTabAdapter;
    private CommonphrasesAdapter mPhrasesAdapter;
    private IMoreClickAdapter moreClickAdapter;
    private FrameLayout mSendToggle;
    private ImageView mEmoticonToggle;
    private ImageView mPluginToggle;
    private ImageView mVoiceToggle;
    private TextView mPhraseseToggle;
    private boolean isRobotFirst = false;
    private IRongExtensionState mFireState;
    private IRongExtensionState mNormalState;

    private Fragment mFragment;

    private IExtensionClickListener mExtensionClickListener;

    private Conversation.ConversationType mConversationType;
    private String mTargetId;
    private List<IExtensionModule> mExtensionModuleList;
    private List<String> mPhrasesList;
    private InputBar.Style mStyle;
    private VisibilityState lastState = VisibilityState.EXTENSION_VISIBLE; //上一次底部的状态
    private boolean hasEverDrawn = false;  // edit text 是否绘制过
    private String mUserId;
    private boolean isBurnMode;

    public static final int TRIGGER_MODE_SYSTEM = 1;//由系统自动触发的底部栏收缩
    public static final int TRIGGER_MODE_TOUCH = 2;//人为触摸屏幕触发的底部栏收缩

    @IntDef({TRIGGER_MODE_SYSTEM, TRIGGER_MODE_TOUCH})
    @Retention(RetentionPolicy.SOURCE)
    @interface TRIGGERMODE {
    }

    private @TRIGGERMODE
    int triggerMode = TRIGGER_MODE_SYSTEM;

    /**
     * RongExtension 构造方法.
     *
     * @param context 上下文
     */
    public RongExtension(Context context) {
        super(context);
        initView();
        initData();
    }

    /**
     * RongExtension 构造方法.
     *
     * @param context 上下文
     * @param attrs   View 的属性集合
     */
    public RongExtension(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RongExtension);
        int attr = a.getInt(R.styleable.RongExtension_RCStyle, 0x123);
        a.recycle();
        initView();
        initData();
        mStyle = InputBar.Style.getStyle(attr);
        if (mStyle != null) {
            setInputBarStyle(mStyle);
        }
    }

    /**
     * 当界面（Activity、Fragment）销毁时（onDestroy）时，调用。
     */
    public void onDestroy() {
        RLog.d(TAG, "onDestroy");
        for (IExtensionModule module : mExtensionModuleList) {
            module.onDetachedFromExtension();
        }
        mExtensionClickListener = null;
        hideInputKeyBoard();
    }

    /**
     * 收起 extension 面板
     */
    public void collapseExtension() {
        hidePluginBoard();
        hideEmoticonBoard();
        hidePhrasesBoard();
        hideInputKeyBoard();
    }

    /**
     * 弹起软键盘
     */
    public void showSoftInput() {
        showInputKeyBoard();
        mContainerLayout.setSelected(true);
    }

    public boolean isExtensionExpanded() {
        return (mPluginAdapter != null && mPluginAdapter.getVisibility() == View.VISIBLE)
                || (mEmotionTabAdapter != null && mEmotionTabAdapter.getVisibility() == View.VISIBLE)
                || (mPhrasesAdapter != null && mPhrasesAdapter.getVisibility() == View.VISIBLE);
    }

    /**
     * 设置 ExtensionBar 样式.
     *
     * @param style 目前支持 5 种样式，参照: {@link InputBar.Style}
     */
    public void setInputBarStyle(InputBar.Style style) {
        switch (style) {
            case STYLE_SWITCH_CONTAINER_EXTENSION:
                setSCE();
                break;
            case STYLE_CONTAINER:
                setC();
                break;
            case STYLE_CONTAINER_EXTENSION:
                setCE();
                break;
            case STYLE_EXTENSION_CONTAINER:
                setEC();
                break;
            case STYLE_SWITCH_CONTAINER:
                setSC();
                break;
        }
    }

    /**
     * 当 Extension 被加载到会话后，调用此方法，设置 Extension 所属会话信息。
     *
     * @param conversationType 会话类型
     * @param targetId         会话 id
     */
    public void setConversation(Conversation.ConversationType conversationType, String targetId) {
        // 多次调用 setConversation 时，保证 module 被 attach 一次
        if (mConversationType == null && mTargetId == null) {
            mConversationType = conversationType;
            mTargetId = targetId;
            for (IExtensionModule module : mExtensionModuleList) {
                module.onAttachedToExtension(this);
            }
            refreshQuickView();
            initPlugins();
            initEmoticons();
            initPanelStyle();
        }
        mConversationType = conversationType;
        mTargetId = targetId;
        SharedPreferences sp = SharedPreferencesUtils.get(getContext(), KitCommonDefine.RONG_KIT_SP_CONFIG, Context.MODE_PRIVATE);
        boolean isBurn = sp.getBoolean(KitCommonDefine.BURN_PREFIX + getTargetId(), false);
        if (isBurn) {
            enterBurnMode();
        }
    }

    void refreshQuickView() {
        //私聊才开启常用语功能
        if ((mConversationType == Conversation.ConversationType.PRIVATE || mConversationType == Conversation.ConversationType.GROUP)
                && mPhrasesList.size() > 0 && !isBurnMode) {
            mPhraseseToggle.setVisibility(VISIBLE);
            int padding = RongUtils.dip2px(6);
            mMainBar.setPadding(padding, padding, padding, padding);
            mDivider.setVisibility(GONE);
            initPhrases();
        } else {
            mPhraseseToggle.setVisibility(GONE);
            mDivider.setVisibility(VISIBLE);
        }
    }


    private void initPhrases() {
        if (mPhrasesList != null && mPhrasesList.size() > 0) {
            mPhrasesAdapter.addPhrases(mPhrasesList);
        }
    }

    private void initPlugins() {
        for (IExtensionModule module : mExtensionModuleList) {
            List<IPluginModule> pluginModules = module.getPluginModules(mConversationType);
            if (pluginModules != null && mPluginAdapter != null) {
                mPluginAdapter.addPlugins(pluginModules);
            }
        }
        IExtensionProxy proxy = RongExtensionManager.getExtensionProxy();
        if (proxy != null && mPluginAdapter != null) {
            proxy.onPreLoadPlugins(mConversationType, mTargetId, mPluginAdapter.getPluginModules());
        }
    }

    private void initEmoticons() {
        IExtensionProxy proxy = RongExtensionManager.getExtensionProxy();
        for (IExtensionModule module : mExtensionModuleList) {
            IExtensionModule handledResult;
            if (proxy != null) {
                handledResult = proxy.onPreLoadEmoticons(mConversationType, mTargetId, module);
                if (handledResult != null) {
                    List<IEmoticonTab> tabs = module.getEmoticonTabs();
                    mEmotionTabAdapter.initTabs(tabs, module.getClass().getCanonicalName());
                }
            } else {
                List<IEmoticonTab> tabs = module.getEmoticonTabs();
                mEmotionTabAdapter.initTabs(tabs, module.getClass().getCanonicalName());
            }
        }
    }

    /**
     * 设置公众号菜单。
     * 如果在进入会话时，就需要展示菜单，则 showFirst 设置为 true，否则为 false.
     *
     * @param inputMenuList 菜单数目。
     * @param showFirst     是否在进入会话时就展示菜单。
     */
    public void setInputMenu(List<InputMenu> inputMenuList, boolean showFirst) {
        if (inputMenuList != null && inputMenuList.size() > 0) {
            mPSMenu.setVisibility(VISIBLE);
            mPSDivider.setVisibility(VISIBLE);
        } else {
            RLog.e(TAG, "setInputMenu no item");
            return;
        }
        mInputMenuList = inputMenuList;
        if (showFirst) {
            setExtensionBarVisibility(GONE);
            setMenuVisibility(VISIBLE, inputMenuList);
        }
    }

    private void setExtensionBarVisibility(int visibility) {
        if (visibility == GONE) {
            hideEmoticonBoard();
            hidePluginBoard();
            hidePhrasesBoard();
            hideInputKeyBoard();
        }
        mExtensionBar.setVisibility(visibility);
    }

    private void setMenuVisibility(int visibility, List<InputMenu> inputMenuList) {
        if (mMenuContainer == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            mMenuContainer = (ViewGroup) inflater.inflate(R.layout.rc_ext_menu_container, null);
            mMenuContainer.findViewById(R.id.rc_switch_to_keyboard).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    setExtensionBarVisibility(VISIBLE);
                    mMenuContainer.setVisibility(GONE);
                }
            });
            for (int i = 0; i < inputMenuList.size(); i++) {
                final InputMenu menu = inputMenuList.get(i);
                LinearLayout rootMenu = (LinearLayout) inflater.inflate(R.layout.rc_ext_root_menu_item, null);
                LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
                rootMenu.setLayoutParams(lp);
                TextView title = rootMenu.findViewById(R.id.rc_menu_title);
                title.setText(menu.title);
                ImageView iv = rootMenu.findViewById(R.id.rc_menu_icon);
                if (menu.subMenuList != null && menu.subMenuList.size() > 0) {
                    iv.setVisibility(VISIBLE);
                    iv.setImageResource(R.drawable.rc_menu_trangle);
                }
                final int rootIndex = i;
                rootMenu.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        List<String> subMenuList = menu.subMenuList;
                        if (subMenuList != null && subMenuList.size() > 0) {
                            InputSubMenu subMenu = new InputSubMenu(getContext(), subMenuList);
                            subMenu.setOnItemClickListener(new ISubMenuItemClickListener() {
                                @Override
                                public void onClick(int index) {
                                    if (mExtensionClickListener != null) {
                                        mExtensionClickListener.onMenuClick(rootIndex, index);
                                    }
                                }
                            });
                            subMenu.showAtLocation(v);
                        } else {
                            if (mExtensionClickListener != null) {
                                mExtensionClickListener.onMenuClick(rootIndex, -1);
                            }
                        }
                    }
                });
                ViewGroup menuBar = mMenuContainer.findViewById(R.id.rc_menu_bar);
                menuBar.addView(rootMenu);
            }
            addView(mMenuContainer);
        }
        if (visibility == GONE) {
            mMenuContainer.setVisibility(GONE);
        } else {
            mMenuContainer.setVisibility(VISIBLE);
        }
    }

    /**
     * 设置公众号菜单的可见性。
     */
    public void setMenuVisibility(int visibility) {
        if (mMenuContainer != null) {
            mMenuContainer.setVisibility(visibility);
        }
    }

    /**
     * 获取公众号菜单的可见性。
     */
    public int getMenuVisibility() {
        if (mMenuContainer != null) {
            return mMenuContainer.getVisibility();
        } else {
            return View.GONE;
        }
    }

    /**
     * 可见状态
     */
    enum VisibilityState {
        EXTENSION_VISIBLE,
        MENUCONTAINER_VISIBLE
    }

    /**
     * 设置 ExtensionBar 输入模式
     * 除了默认模式外，还支持客服输入模式
     *
     * @param mode 输入模式, 参照: {@link CustomServiceMode}
     */
    public void setExtensionBarMode(CustomServiceMode mode) {
        switch (mode) {
            case CUSTOM_SERVICE_MODE_NO_SERVICE:
            case CUSTOM_SERVICE_MODE_ROBOT:
                setC();
                break;
            case CUSTOM_SERVICE_MODE_HUMAN:
            case CUSTOM_SERVICE_MODE_HUMAN_FIRST:
                isRobotFirst = false;
                if (mStyle != null) {
                    setInputBarStyle(mStyle);
                }
                mVoiceToggle.setImageResource(R.drawable.rc_voice_toggle_selector);
                mVoiceToggle.setOnClickListener(this);
                break;
            case CUSTOM_SERVICE_MODE_ROBOT_FIRST:
                mVoiceToggle.setImageResource(R.drawable.rc_cs_admin_selector);
                isRobotFirst = true;
                mVoiceToggle.setOnClickListener(this);
                setSC();
                break;
        }
    }

    /**
     * 获取消息输入框控件
     *
     * @return 当前消息输入框
     */
    public EditText getInputEditText() {
        return mEditText;
    }

    /**
     * 在加载 Emoticon tab 时，如果 tab icon 需要异步获取，可以通过如下接口，做异步刷新。
     *
     * @param tab  需要刷新的 tab
     * @param icon tab 对应的 icon
     */
    public void refreshEmoticonTabIcon(IEmoticonTab tab, Drawable icon) {
        if (icon != null && mEmotionTabAdapter != null && tab != null) {
            mEmotionTabAdapter.refreshTabIcon(tab, icon);
        }
    }

    /**
     * 动态在 plugin 区域的末尾增加一个 plugin 。
     *
     * @param pluginModule 增加的 plugin
     */
    public void addPlugin(IPluginModule pluginModule) {
        if (pluginModule != null) {
            mPluginAdapter.addPlugin(pluginModule);
        }
    }

    /**
     * 移除指定的 plugin
     *
     * @param pluginModule 指定 plugin
     */
    public void removePlugin(IPluginModule pluginModule) {
        if (pluginModule != null) {
            mPluginAdapter.removePlugin(pluginModule);
        }
    }

    /**
     * 获取当前所有 Plugin 列表
     *
     * @return 已加载的 plugin 列表
     */
    public List<IPluginModule> getPluginModules() {
        return mPluginAdapter.getPluginModules();
    }

    /**
     * 在 plugin 界面添加自定义 view，添加后，+ 号区域全部填充为自定义的 view。
     * 当自定义 view 可见时点击 ”+“ 会触发自定义 view 和默认 plugin 界面间进行切换。
     *
     * @param v 自定义 view
     */
    public void addPluginPager(View v) {
        if (null != mPluginAdapter) {
            mPluginAdapter.addPager(v);
        }
    }

    /**
     * 删除 plugin 界面自定义 view。
     *
     * @param v 自定义 view
     */
    public void removePluginPager(View v) {
        if (mPluginAdapter != null && v != null) {
            mPluginAdapter.removePager(v);
        }
    }

    /**
     * 在表情区域增加一个 tab 页，并指定 tab 所对应的标签，每个标签下可以有多个 tab。
     * 开发者也可以指定添加的位置，但此位置只对当前标签下有效，如果 index 小于 0 或者超出对应标签 tab 列表大小，则会添加失败。
     * 开发者可以通过 {@link #getEmoticonTabs(String)} 获取指定 tag 的所有 tab 列表。
     * <p>
     * 注意：添加时必须指定唯一对应的 tag，例如：IExtensionModule's CanonicalName {@link DefaultExtensionModule}
     *
     * @param index tab 所在位置
     * @param tab   增加的 tab 页
     * @param tag   唯一标签：建议填写自定义的 {@link IExtensionModule} Canonical Name.
     */
    public boolean addEmoticonTab(int index, IEmoticonTab tab, String tag) {
        if (mEmotionTabAdapter != null && tab != null && !TextUtils.isEmpty(tag)) {
            return mEmotionTabAdapter.addTab(index, tab, tag);
        }
        RLog.e(TAG, "addEmoticonTab Failure");
        return false;
    }

    /**
     * 在表情区域末尾增加一个 tab 页，并指定 tab 所对应的标签，每个标签下可以有多个 tab。
     * 开发者也可以指定添加的位置，但此位置只对当前标签下有效，如果 index 小于 0 或者超出对应标签 tab 列表大小，则会添加失败。
     * 开发者可以通过 {@link #getEmoticonTabs(String)} 获取指定 tag 的所有 tab 列表。
     * <p>
     * 注意：添加时必须指定唯一对应的 tag，例如：IExtensionModule's CanonicalName {@link DefaultExtensionModule}
     *
     * @param tab 增加的 tab 页
     * @param tag 唯一标签：建议填写自定义的 {@link IExtensionModule} Canonical Name.
     */
    public void addEmoticonTab(IEmoticonTab tab, String tag) {
        if (mEmotionTabAdapter != null && tab != null && !TextUtils.isEmpty(tag)) {
            mEmotionTabAdapter.addTab(tab, tag);
        }
    }

    /**
     * get the tab list as a tag.
     *
     * @param tag the unique tag, must not be null.
     * @return the list of the mapping with the specified tag, or {@code null}
     * if no mapping for the specified key is found.
     */
    public List<IEmoticonTab> getEmoticonTabs(String tag) {
        if (mEmotionTabAdapter != null && !TextUtils.isEmpty(tag)) {
            return mEmotionTabAdapter.getTagTabs(tag);
        }
        return null;
    }

    /**
     * get the tab index as a tag.
     *
     * @param tag the unique tag, must not be null.
     * @return the index of the mapping with the specified tag, or -1
     * if no mapping for the specified tag is found.
     */
    public int getEmoticonTabIndex(String tag) {
        if (mEmotionTabAdapter != null && !TextUtils.isEmpty(tag)) {
            return mEmotionTabAdapter.getTagTabIndex(tag);
        }
        return -1;
    }

    /**
     * remove a tab as the tag.
     *
     * @param tab the tab will be removed, must not be null.
     * @param tag the unique tag, must not be null.
     * @return true if this tab was modified by this operation, false
     * otherwise.
     */
    public boolean removeEmoticonTab(IEmoticonTab tab, String tag) {
        boolean result = false;
        if (mEmotionTabAdapter != null && tab != null && !TextUtils.isEmpty(tag)) {
            result = mEmotionTabAdapter.removeTab(tab, tag);
        }
        return result;
    }

    /**
     * set current selected tab.
     *
     * @param tab the tab, must not be null.
     * @param tag the unique tag, must not be null.
     */
    public void setCurrentEmoticonTab(IEmoticonTab tab, String tag) {
        if (mEmotionTabAdapter != null && tab != null && !TextUtils.isEmpty(tag)) {
            mEmotionTabAdapter.setCurrentTab(tab, tag);
        }
    }

    /**
     * 是否显示表情面板的 table bar
     *
     * @param enable 是否显示
     */
    public void setEmoticonTabBarEnable(boolean enable) {
        if (mEmotionTabAdapter != null) {
            mEmotionTabAdapter.setTabViewEnable(enable);
        }
    }

    /**
     * 设置表情 tab bar “+”号功能是否开启
     *
     * @param enable 是否开启
     */
    public void setEmoticonTabBarAddEnable(boolean enable) {
        if (mEmotionTabAdapter != null) {
            mEmotionTabAdapter.setAddEnable(enable);
        }
    }

    public void setEmoticonTabBarAddClickListener(IEmoticonClickListener listener) {
        if (mEmotionTabAdapter != null) {
            mEmotionTabAdapter.setOnEmoticonClickListener(listener);
        }
    }

    public void setEmoticonTabBarSettingEnable(boolean enable) {
        if (mEmotionTabAdapter != null) {
            mEmotionTabAdapter.setSettingEnable(enable);
        }
    }

    public void setEmoticonTabBarSettingClickListener(IEmoticonSettingClickListener listener) {
        if (mEmotionTabAdapter != null) {
            mEmotionTabAdapter.setOnEmoticonSettingClickListener(listener);
        }
    }

    /**
     * 开启表情 tab bar + 功能后，还可以在表情面板的 tab bar 增加自定义 icon
     * 自定义 icon 所在位置为 "+" 之后，不随其他 tab 滚动
     *
     * @param drawable      自定义 icon
     * @param clickListener 自定义 icon 被点击后的回调
     */
    public void addEmoticonExtraTab(Context context, Drawable drawable, OnClickListener clickListener) {
        if (mEmotionTabAdapter != null) {
            mEmotionTabAdapter.addExtraTab(context, drawable, clickListener);
        }
    }

    /**
     * 设置 Extension 所在的 Fragment。
     *
     * @param fragment 所属 Fragment
     */
    public void setFragment(Fragment fragment) {
        this.mFragment = fragment;
    }

    public Fragment getFragment() {
        return this.mFragment;
    }

    /**
     * 获取当前 Extension 所在会话的会话类型。
     *
     * @return 会话类型。
     */
    public Conversation.ConversationType getConversationType() {
        return mConversationType;
    }

    /**
     * 获取当前所在会话的 targetId。
     *
     * @return 目标 id。
     */
    public String getTargetId() {
        return mTargetId;
    }

    /**
     * 设置输入面板监听，设置后，面板中的各个组成元素被点击时，通过此监听回调
     *
     * @param clickListener 监听
     */
    public void setExtensionClickListener(IExtensionClickListener clickListener) {
        this.mExtensionClickListener = clickListener;
    }

    /**
     * activity 结束返回结果。
     */
    public void onActivityPluginResult(int requestCode, int resultCode, Intent data) {
        int position = (requestCode >> 8) - 1;
        int reqCode = requestCode & 0xff;
        IPluginModule pluginModule = mPluginAdapter.getPluginModule(position);
        if (pluginModule != null) {
            if (mExtensionClickListener != null && resultCode == Activity.RESULT_OK) {
                if (pluginModule instanceof ImagePlugin) {
                    boolean sendOrigin = data.getBooleanExtra("sendOrigin", false);
                    String mediaList = data.getStringExtra(Intent.EXTRA_RETURN_RESULT);
                    Gson gson = new Gson();
                    Type entityType = new TypeToken<LinkedHashMap<String, Integer>>() {
                    }.getType();
                    LinkedHashMap<String, Integer> mLinkedHashMap = gson.fromJson(mediaList, entityType);
                    mExtensionClickListener.onImageResult(mLinkedHashMap, sendOrigin);
                } else if (pluginModule instanceof DefaultLocationPlugin || pluginModule instanceof CombineLocationPlugin) {
                    double lat = data.getDoubleExtra("lat", 0);
                    double lng = data.getDoubleExtra("lng", 0);
                    String poi = data.getStringExtra("poi");
                    String thumb = data.getStringExtra("thumb");
                    mExtensionClickListener.onLocationResult(lat, lng, poi, Uri.parse(thumb));
                }
            }
            pluginModule.onActivityResult(reqCode, resultCode, data);
        }
    }

    public boolean onRequestPermissionResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        int position = (requestCode >> 8) - 1;
        int reqCode = requestCode & 0xFF;
        IPluginModule pluginModule = mPluginAdapter.getPluginModule(position);
        if (pluginModule instanceof IPluginRequestPermissionResultCallback) {
            return ((IPluginRequestPermissionResultCallback) pluginModule).onRequestPermissionResult(mFragment, this, reqCode, permissions, grantResults);
        }
        return false;
    }

    /**
     * @param intent      The intent to start.
     * @param requestCode If >= 0, this code will be returned in
     *                    onActivityResult() when the activity exits.
     */
    public void startActivityForPluginResult(Intent intent, int requestCode, IPluginModule pluginModule) {
        if ((requestCode & 0xffffff00) != 0) {
            throw new IllegalArgumentException("requestCode must less than 256.");
        }
        int position = mPluginAdapter.getPluginPosition(pluginModule);
        mFragment.startActivityForResult(intent, ((position + 1) << 8) + (requestCode & 0xff));
    }

    public void requestPermissionForPluginResult(String[] permissions, int requestCode, IPluginModule pluginModule) {
        if ((requestCode & 0xffffff00) != 0) {
            throw new IllegalArgumentException("requestCode must less than 256");
        }
        int position = mPluginAdapter.getPluginPosition(pluginModule);
        int req = ((position + 1) << 8) + (requestCode & 0xff);
        PermissionCheckUtil.requestPermissions(mFragment, permissions, req);
    }


    private void initData() {
        //初始化短语模块
        mPhrasesList = RongExtensionManager.getInstance().getPhrasesList();
        mPhrasesAdapter = new CommonphrasesAdapter();
        mPhrasesAdapter.setOnPhrasesClickListener(new IPhrasesClickListener() {
            @Override
            public void onClick(String phrases, int position) {
                if (mExtensionClickListener != null) {
                    mExtensionClickListener.onPhrasesClicked(phrases, position);
                }
            }
        });
        mExtensionModuleList = RongExtensionManager.getInstance().getExtensionModules();
        mPluginAdapter = new PluginAdapter();
        mPluginAdapter.setOnPluginClickListener(new IPluginClickListener() {
            @Override
            public void onClick(IPluginModule pluginModule, int position) {
                if (mExtensionClickListener != null) {
                    mExtensionClickListener.onPluginClicked(pluginModule, position);
                }
                pluginModule.onClick(mFragment, RongExtension.this);
            }
        });
        mEmotionTabAdapter = new EmoticonTabAdapter();
        moreClickAdapter = new MoreClickAdapter();
        mUserId = RongIMClient.getInstance().getCurrentUserId();
        try {
            boolean enable = getResources().getBoolean(getResources().getIdentifier("rc_extension_history", "bool", getContext().getPackageName()));
            ExtensionHistoryUtil.setEnableHistory(enable);
            ExtensionHistoryUtil.addExceptConversationType(Conversation.ConversationType.CUSTOMER_SERVICE);
        } catch (Resources.NotFoundException e) {
            RLog.i(TAG, "rc_extension_history not configure in rc_configuration.xml");
        }

    }

    private void initView() {
        setOrientation(VERTICAL);
        setBackgroundColor(getContext().getResources().getColor(R.color.rc_extension_normal));
        mExtensionBar = (ViewGroup) LayoutInflater.from(getContext()).inflate(R.layout.rc_ext_extension_bar, null);
        mMainBar = mExtensionBar.findViewById(R.id.ext_main_bar);
        mSwitchLayout = mExtensionBar.findViewById(R.id.rc_switch_layout);
        mContainerLayout = mExtensionBar.findViewById(R.id.rc_container_layout);
        mPluginLayout = mExtensionBar.findViewById(R.id.rc_plugin_layout);
        mEditTextLayout = LayoutInflater.from(getContext()).inflate(R.layout.rc_ext_input_edit_text, null);
        mEditTextLayout.setVisibility(VISIBLE);
        mContainerLayout.addView(mEditTextLayout);
        LayoutInflater.from(getContext()).inflate(R.layout.rc_ext_voice_input, mContainerLayout, true);
        mVoiceInputToggle = mContainerLayout.findViewById(R.id.rc_audio_input_toggle);
        mVoiceInputToggle.setVisibility(GONE);
        mEditText = mExtensionBar.findViewById(R.id.rc_edit_text);
        mSendToggle = mExtensionBar.findViewById(R.id.rc_send_toggle);
        mPluginToggle = mExtensionBar.findViewById(R.id.rc_plugin_toggle);
        mPhraseseToggle = mExtensionBar.findViewById(R.id.ext_common_phrases);
        mDivider = mExtensionBar.findViewById(R.id.rc_divider);

        mEditText.setOnTouchListener(this);

        mEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && !TextUtils.isEmpty(mEditText.getText()) && mEditTextLayout.getVisibility() == VISIBLE) {
                    mSendToggle.setVisibility(VISIBLE);
                    mPluginLayout.setVisibility(GONE);
                }
            }
        });

        mEditText.addTextChangedListener(new TextWatcher() {
            private int start;
            private int count;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (mExtensionClickListener != null) {
                    mExtensionClickListener.beforeTextChanged(s, start, count, after);
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                this.start = start;
                this.count = count;
                if (mExtensionClickListener != null) {
                    mExtensionClickListener.onTextChanged(s, start, before, count);
                }
                if (mVoiceInputToggle.getVisibility() == VISIBLE) {
                    mSendToggle.setVisibility(GONE);
                    mPluginLayout.setVisibility(VISIBLE);
                } else {
                    if (s == null || s.length() == 0) {
                        mSendToggle.setVisibility(GONE);
                        mPluginLayout.setVisibility(VISIBLE);
                    } else {
                        mSendToggle.setVisibility(VISIBLE);
                        mPluginLayout.setVisibility(GONE);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (AndroidEmoji.isEmoji(s.subSequence(start, start + count).toString())) {
                    mEditText.removeTextChangedListener(this);
                    String resultStr = AndroidEmoji.replaceEmojiWithText(s.toString());
                    mEditText.setText(AndroidEmoji.ensure(resultStr), TextView.BufferType.SPANNABLE);
                    mEditText.setSelection(mEditText.getText().length());
                    mEditText.addTextChangedListener(this);
                }

                if (mExtensionClickListener != null) {
                    mExtensionClickListener.afterTextChanged(s);
                }
            }
        });

        mEditText.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                return mExtensionClickListener != null && mExtensionClickListener.onKey(mEditText, keyCode, event);
            }
        });

        mEditText.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (mEditText.getText().length() > 0 && mEditText.isFocused() && !hasEverDrawn) {
                    Rect rect = new Rect();
                    mEditText.getWindowVisibleDisplayFrame(rect);
                    int keypadHeight = mEditText.getRootView().getHeight() - rect.bottom;
                    int inputbarHeight = (int) mEditText.getContext().getResources().getDimension(R.dimen.rc_extension_bar_min_height);

                    if (keypadHeight > inputbarHeight * 2) { // 如果键盘高度输入框高度的两倍, 可视为软键盘打开状态
                        hasEverDrawn = true;
                    }
                    if (mExtensionClickListener != null)
                        mExtensionClickListener.onEditTextClick(mEditText);
                    showInputKeyBoard();
                    mContainerLayout.setSelected(true);
                    hidePluginBoard();
                    hideEmoticonBoard();
                    hidePhrasesBoard();
                }
            }
        });

        mVoiceToggle = mExtensionBar.findViewById(R.id.rc_voice_toggle);
        mVoiceToggle.setOnClickListener(this);

        mVoiceInputToggle.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mExtensionClickListener != null)
                    mExtensionClickListener.onVoiceInputToggleTouch(v, event);
                return false;
            }
        });

        mSendToggle.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = mEditText.getText().toString();
                mEditText.setText("");
                if (mExtensionClickListener != null)
                    mExtensionClickListener.onSendToggleClick(v, text);
            }
        });

        mPluginToggle.setOnClickListener(this);

        mPhraseseToggle.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setPhrasesBoard();
            }
        });

        mEmoticonToggle = mExtensionBar.findViewById(R.id.rc_emoticon_toggle);
        mEmoticonToggle.setOnClickListener(this);

        mPSMenu = mExtensionBar.findViewById(R.id.rc_switch_to_menu);
        mPSMenu.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setExtensionBarVisibility(GONE);
                setMenuVisibility(VISIBLE, mInputMenuList);
            }
        });
        mPSDivider = mExtensionBar.findViewById(R.id.rc_switch_divider);

        addView(mExtensionBar);
    }

    public void showMoreActionLayout(List<IClickActions> actions) {
        lastState = getMenuVisibility() == View.VISIBLE ? VisibilityState.MENUCONTAINER_VISIBLE : VisibilityState.EXTENSION_VISIBLE;
        setExtensionBarVisibility(View.GONE);
        setMenuVisibility(View.GONE);
        moreClickAdapter.bindView(this, mFragment, actions);
    }

    public void hideMoreActionLayout() {
        if (getConversationType().equals(Conversation.ConversationType.APP_PUBLIC_SERVICE) || getConversationType().equals(Conversation.ConversationType.PUBLIC_SERVICE)) {
            if (mInputMenuList != null) {
                if (lastState == VisibilityState.MENUCONTAINER_VISIBLE) {
                    setExtensionBarVisibility(View.GONE);
                    setMenuVisibility(View.VISIBLE);
                } else {
                    setExtensionBarVisibility(View.VISIBLE);
                    mPSMenu.setVisibility(VISIBLE);
                    mPSDivider.setVisibility(VISIBLE);
                }
            } else {
                setExtensionBarVisibility(VISIBLE);
            }
        } else {
            setExtensionBarVisibility(View.VISIBLE);
        }
        moreClickAdapter.hideMoreActionLayout();
    }

    public void setMoreActionEnable(boolean enable) {
        moreClickAdapter.setMoreActionEnable(enable);
    }

    public boolean isMoreActionShown() {
        return moreClickAdapter.isMoreActionShown();
    }

    void hideVoiceInputToggle() {
        mVoiceToggle.setImageResource(isBurnMode ? R.drawable.rc_destruct_voice_toggle_selector : R.drawable.rc_voice_toggle_selector);
        mVoiceInputToggle.setVisibility(GONE);
        String saveId = DeviceUtils.ShortMD5(Base64.DEFAULT, mUserId, mTargetId, mConversationType.getName());
        ExtensionHistoryUtil.setExtensionBarState(getContext(), saveId, mConversationType, ExtensionHistoryUtil.ExtensionBarState.NORMAL);
    }

    void showVoiceInputToggle() {
        mVoiceInputToggle.setVisibility(VISIBLE);
        mVoiceInputToggle.setTextColor(isBurnMode ? getContext().getResources().getColor(R.color.rc_destruct_voice_color) : getContext().getResources().getColor(R.color.rc_text_voice));
        mVoiceToggle.setImageResource(isBurnMode ? R.drawable.rc_destruct_keyboard_selector : R.drawable.rc_keyboard_selector);
        String saveId = DeviceUtils.ShortMD5(Base64.DEFAULT, mUserId, mTargetId, mConversationType.getName());
        ExtensionHistoryUtil.setExtensionBarState(getContext(), saveId, mConversationType, ExtensionHistoryUtil.ExtensionBarState.VOICE);
    }

    void hideEmoticonBoard() {
        getRongExtensionState().hideEmoticonBoard(mEmoticonToggle, mEmotionTabAdapter);
    }

    void setEmoticonBoard() {
        if (mEmotionTabAdapter.isInitialized()) {
            if (mEmotionTabAdapter.getVisibility() == VISIBLE) {
                mEmotionTabAdapter.setVisibility(GONE);
                mEmoticonToggle.setSelected(false);
                mEmoticonToggle.setImageResource(R.drawable.rc_emotion_toggle_selector);
                showInputKeyBoard();
            } else {
                mEmotionTabAdapter.setVisibility(VISIBLE);
                mContainerLayout.setSelected(true);
                mEmoticonToggle.setSelected(true);
                mEmoticonToggle.setImageResource(R.drawable.rc_keyboard_selector);
            }
        } else {
            mEmotionTabAdapter.bindView(this);
            mEmotionTabAdapter.setVisibility(VISIBLE);
            mContainerLayout.setSelected(true);
            mEmoticonToggle.setSelected(true);
            mEmoticonToggle.setImageResource(R.drawable.rc_keyboard_selector);
        }
        if (!TextUtils.isEmpty(mEditText.getText())) {
            mSendToggle.setVisibility(VISIBLE);
            mPluginLayout.setVisibility(GONE);
        }
    }

    void hidePluginBoard() {
        if (mPluginAdapter != null) {
            mPluginAdapter.setVisibility(GONE);
            View pager = mPluginAdapter.getPager();
            mPluginAdapter.removePager(pager);
        }
    }

    void setPluginBoard() {
        if (mPluginAdapter.isInitialized()) {
            if (mPluginAdapter.getVisibility() == VISIBLE) {
                View pager = mPluginAdapter.getPager();
                if (pager != null) {
                    pager.setVisibility(pager.getVisibility() == GONE ? VISIBLE : GONE);
                } else {
                    mPluginAdapter.setVisibility(GONE);
                    mContainerLayout.setSelected(true);
                    showInputKeyBoard();
                }
            } else {
                mEmoticonToggle.setImageResource(R.drawable.rc_emotion_toggle_selector);
                if (isKeyBoardActive()) {
                    getHandler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mPluginAdapter.setVisibility(VISIBLE);
                        }
                    }, 200);
                } else {
                    mPluginAdapter.setVisibility(VISIBLE);
                }
                hideInputKeyBoard();
                hideEmoticonBoard();
                hidePhrasesBoard();
                mContainerLayout.setSelected(false);
            }
        } else {
            mEmoticonToggle.setImageResource(R.drawable.rc_emotion_toggle_selector);
            mPluginAdapter.bindView(this);
            mPluginAdapter.setVisibility(VISIBLE);
            mContainerLayout.setSelected(false);
            hideInputKeyBoard();
            hideEmoticonBoard();
            hidePhrasesBoard();
        }
        hideVoiceInputToggle();
        mEditTextLayout.setVisibility(VISIBLE);
    }

    /**
     * 隐藏短语模块
     */
    void hidePhrasesBoard() {
        if (mPhrasesAdapter != null) {
            mPhrasesAdapter.setVisibility(GONE);
        }
    }

    /**
     * 设置短语模块
     */
    private void setPhrasesBoard() {
        if (mPhrasesAdapter.isInitialized()) {
            if (mPhrasesAdapter.getVisibility() == VISIBLE) {
                mPhrasesAdapter.setVisibility(GONE);
                mContainerLayout.setSelected(true);
            } else {
                mEmoticonToggle.setImageResource(R.drawable.rc_emotion_toggle_selector);
                if (isKeyBoardActive()) {
                    getHandler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mPhrasesAdapter.setVisibility(VISIBLE);
                        }
                    }, 200);
                } else {
                    mPhrasesAdapter.setVisibility(VISIBLE);
                }
                hideInputKeyBoard();
                hideEmoticonBoard();
                hidePluginBoard();
                mContainerLayout.setSelected(false);
            }
        } else {
            mEmoticonToggle.setImageResource(R.drawable.rc_emotion_toggle_selector);
            mPhrasesAdapter.bindView(this);
            mPhrasesAdapter.setVisibility(VISIBLE);
            mContainerLayout.setSelected(false);
            hideInputKeyBoard();
            hideEmoticonBoard();
            hidePluginBoard();
        }
        hideVoiceInputToggle();
        mEditTextLayout.setVisibility(VISIBLE);
    }

    private boolean isKeyBoardActive = false;

    boolean isKeyBoardActive() {
        return isKeyBoardActive;
    }

    void setKeyBoardActive(boolean pIsKeyBoardActive) {
        isKeyBoardActive = pIsKeyBoardActive;
    }

    void hideInputKeyBoard() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
        }
        mEditText.clearFocus();
        isKeyBoardActive = false;
    }

    void showInputKeyBoard() {
        mEditText.requestFocus();
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(mEditText, 0);
        }
        mEmoticonToggle.setSelected(false);
        isKeyBoardActive = true;
    }

    /**
     * 获取触发底部栏收缩的触发模式（防止系统非自然的触发底部栏弹起）
     */
    public int getTriggerMode() {
        return triggerMode;
    }

    private void setSCE() {
        mSwitchLayout.setVisibility(VISIBLE);
        if (mSendToggle.getVisibility() == VISIBLE) {
            mPluginLayout.setVisibility(GONE);
        } else {
            mPluginLayout.setVisibility(VISIBLE);
        }
        mMainBar.removeAllViews();
        mMainBar.addView(mSwitchLayout);
        mMainBar.addView(mContainerLayout);
        mMainBar.addView(mPluginLayout);
    }

    private void setSC() {
        mSwitchLayout.setVisibility(VISIBLE);
        mMainBar.removeAllViews();
        mMainBar.addView(mSwitchLayout);
        mMainBar.addView(mContainerLayout);
    }

    private void setCE() {
        if (mSendToggle.getVisibility() == VISIBLE) {
            mPluginLayout.setVisibility(GONE);
        } else {
            mPluginLayout.setVisibility(VISIBLE);
        }
        mMainBar.removeAllViews();
        mMainBar.addView(mContainerLayout);
        mMainBar.addView(mPluginLayout);
    }

    private void setEC() {
        if (mSendToggle.getVisibility() == VISIBLE) {
            mPluginLayout.setVisibility(GONE);
        } else {
            mPluginLayout.setVisibility(VISIBLE);
        }
        mMainBar.removeAllViews();
        mMainBar.addView(mPluginLayout);
        mMainBar.addView(mContainerLayout);
    }

    private void setC() {
        mMainBar.removeAllViews();
        mMainBar.addView(mContainerLayout);
    }

    private void initPanelStyle() {
        String saveId = DeviceUtils.ShortMD5(Base64.DEFAULT, mUserId, mTargetId, mConversationType.getName());
        ExtensionHistoryUtil.ExtensionBarState state = ExtensionHistoryUtil.getExtensionBarState(getContext(), saveId, mConversationType);
        if (state == ExtensionHistoryUtil.ExtensionBarState.NORMAL) {
            mVoiceToggle.setImageResource(R.drawable.rc_voice_toggle_selector);
            mEditTextLayout.setVisibility(VISIBLE);
            mVoiceInputToggle.setVisibility(GONE);
        } else {
            mVoiceToggle.setImageResource(R.drawable.rc_keyboard_selector);
            mEditTextLayout.setVisibility(GONE);
            mVoiceInputToggle.setVisibility(VISIBLE);
            mSendToggle.setVisibility(GONE);
            mPluginLayout.setVisibility(VISIBLE);
        }
    }

    boolean collapsed = true;
    int originalTop = 0;
    int originalBottom = 0;

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (originalTop != 0) {
            if (originalTop > t) {
                if (originalBottom > b && mExtensionClickListener != null && collapsed) {
                    collapsed = false;
                    mExtensionClickListener.onExtensionExpanded(originalBottom - t);
                } else if (collapsed && mExtensionClickListener != null) {
                    collapsed = false;
                    mExtensionClickListener.onExtensionExpanded(b - t);
                }
            } else {
                if (!collapsed && mExtensionClickListener != null) {
                    collapsed = true;
                    mExtensionClickListener.onExtensionCollapsed();
                }
            }
        }
        if (originalTop == 0) {
            originalTop = t;
            originalBottom = b;
        }
    }

    public void resetEditTextLayoutDrawnStatus() {
        hasEverDrawn = false;
    }

    public void showRequestPermissionFailedAlter(String content) {
        final Context context = mFragment.getActivity();
        PermissionCheckUtil.showRequestPermissionFailedAlter(context, content);
    }

    public void enterBurnMode() {
        isBurnMode = true;
        SharedPreferences sp = SharedPreferencesUtils.get(getContext(), KitCommonDefine.RONG_KIT_SP_CONFIG, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KitCommonDefine.BURN_PREFIX + getTargetId(), true).apply();
        hidePluginBoard();
        refreshBurnMode();
    }

    public void exitBurnMode() {
        SharedPreferences sp = SharedPreferencesUtils.get(getContext(), KitCommonDefine.RONG_KIT_SP_CONFIG, Context.MODE_PRIVATE);
        sp.edit().remove(KitCommonDefine.BURN_PREFIX + getTargetId()).apply();
        isBurnMode = false;
        refreshBurnMode();
    }

    private void refreshBurnMode() {
        getRongExtensionState().changeView(this);
    }

    public IRongExtensionState getRongExtensionState() {
        if (isBurnMode) {
            if (mFireState == null) {
                mFireState = new DestructState();
            }
            return mFireState;
        } else {
            if (mNormalState == null) {
                mNormalState = new NormalState();
            }
            return mNormalState;
        }
    }

    ImageView getVoiceToggle() {
        return mVoiceToggle;
    }

    ImageView getPluginToggle() {
        return mPluginToggle;
    }

    ImageView getEmoticonToggle() {
        return mEmoticonToggle;
    }

    boolean isRobotFirst() {
        return isRobotFirst;
    }

    ViewGroup getContainerLayout() {
        return mContainerLayout;
    }

    IExtensionClickListener getExtensionClickListener() {
        return mExtensionClickListener;
    }

    View getEditTextLayout() {
        return mEditTextLayout;
    }

    FrameLayout getSendToggle() {
        return mSendToggle;
    }

    ViewGroup getPluginLayout() {
        return mPluginLayout;
    }

    EditText getEditText() {
        return mEditText;
    }

    Button getVoiceInputToggle() {
        return mVoiceInputToggle;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.rc_plugin_toggle ||
                id == R.id.rc_emoticon_toggle ||
                id == R.id.rc_voice_toggle) {
            getRongExtensionState().onClick(RongExtension.this, v);
        }

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (triggerMode != TRIGGER_MODE_TOUCH) {
            triggerMode = TRIGGER_MODE_TOUCH;
        }
        return getRongExtensionState().onEditTextTouch(this, v, event);
    }

    public boolean isFireStatus() {
        return isBurnMode;
    }

    void clickVoice(boolean pRobotFirst, RongExtension pExtension, View pV, @DrawableRes int emotionDrawable) {
        if (pExtension.getExtensionClickListener() != null)
            pExtension.getExtensionClickListener().onSwitchToggleClick(pV, pExtension.getContainerLayout());
        //如果是CUSTOM_SERVICE_MODE_ROBOT_FIRST模式,执行到此方法后结束
        if (pRobotFirst)
            return;
        if (pExtension.getVoiceInputToggle().getVisibility() == GONE) {
            pExtension.getEditTextLayout().setVisibility(GONE);
            pExtension.getSendToggle().setVisibility(GONE);
            pExtension.getPluginLayout().setVisibility(VISIBLE);
            pExtension.hideInputKeyBoard();
            pExtension.showVoiceInputToggle();
            pExtension.getContainerLayout().setClickable(true);
            pExtension.getContainerLayout().setSelected(false);
        } else {
            pExtension.getEditTextLayout().setVisibility(VISIBLE);
            pExtension.hideVoiceInputToggle();
            pExtension.getEmoticonToggle().setImageResource(emotionDrawable);
            if (pExtension.getEditText().getText().length() > 0) {
                pExtension.getSendToggle().setVisibility(VISIBLE);
                pExtension.getPluginLayout().setVisibility(GONE);
            } else {
                pExtension.getSendToggle().setVisibility(GONE);
                pExtension.getPluginLayout().setVisibility(VISIBLE);
            }
            pExtension.showInputKeyBoard();
            pExtension.getContainerLayout().setSelected(true);
        }
        pExtension.hidePluginBoard();
        pExtension.hideEmoticonBoard();
        pExtension.hidePhrasesBoard();
    }
}
package io.rong.imkit;

import java.util.List;

import io.rong.imkit.plugin.IPluginModule;
import io.rong.imlib.model.Conversation;

/**
 * Extension 代理
 * 进入会话，加载 Extension 前回调此类，可以根据对应的回调方法进行相应的修改调整；
 * 请调用 {@link RongExtensionManager#setExtensionProxy(IExtensionProxy)} 进行设置；
 * 请在 Application 初始化时设置，已保证显示不会错乱。
 */
public interface IExtensionProxy {
    /**
     * 进入会话，预加载 plugin 列表时回调
     * 1. 可以对 plugin list 进行：增、删、调整位置修改
     * 2. plugin list 修改后，RongExtension 会加载修改后的列表并展示
     *
     * @param conversationType 所在会话类型
     * @param targetId         会话 ID
     * @param pluginModuleList plugin 列表，可对此列表进行修改
     */
    void onPreLoadPlugins(Conversation.ConversationType conversationType, String targetId, List<IPluginModule> pluginModuleList);

    /**
     * 进入会话，预加载 Emoticon 时回调
     * 此方法会被多次回调，没加载一个 IExtensionModule 就会回调一次
     * 当此方法被执行时，如果不想加载此 module，需要返回 null。
     *
     * @param conversationType 所在会话类型
     * @param targetId         会话 ID
     * @return 处理后的 module；返回 null 则不展示此 module
     */
    IExtensionModule onPreLoadEmoticons(Conversation.ConversationType conversationType, String targetId, IExtensionModule module);
}

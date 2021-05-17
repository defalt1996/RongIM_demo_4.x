package io.rong.imkit.manager;

import android.content.Context;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import io.rong.common.RLog;
import io.rong.imkit.plugin.IPluginModule;
import io.rong.imlib.model.Conversation;

public class InternalModuleManager {
    private final static String TAG = "InternalModuleManager";

    private static IExternalModule callModule;  //基于 Callkit、CallLib
    private static IExternalModule callModule2; //基于 RongSignalingKit、RongSignalingLib

    private InternalModuleManager() {

    }

    static class SingletonHolder {
        static InternalModuleManager sInstance = new InternalModuleManager();
    }

    public static InternalModuleManager getInstance() {
        return SingletonHolder.sInstance;
    }

    public static void init(Context context) {
        RLog.i(TAG, "init");
        try {
            String moduleName = "io.rong.callkit.RongCallModule"; //callkit
            Class<?> cls = Class.forName(moduleName);
            Constructor<?> constructor = cls.getConstructor();
            callModule = (IExternalModule) constructor.newInstance();
            callModule.onCreate(context);
        } catch (Exception e) {
            RLog.i(TAG, "Can not find RongCallModule.");
        }

        try {
            String moudleName2 = "io.rong.signalingkit.RCSCallModule";//SignalingKit
            Class<?> cls2 = Class.forName(moudleName2);
            Constructor<?> constructor2 = cls2.getConstructor();
            callModule2 = (IExternalModule) constructor2.newInstance();
            callModule2.onCreate(context);
        } catch (Exception e) {
            RLog.i(TAG, "Can not find RCSCallModule.");
        }
    }

    public void onInitialized(String appKey) {
        RLog.i(TAG, "onInitialized");
        if (callModule != null) {
            callModule.onInitialized(appKey);
        }
        if (callModule2 != null) {
            callModule2.onInitialized(appKey);
        }
    }

    public List<IPluginModule> getInternalPlugins(Conversation.ConversationType conversationType) {
        List<IPluginModule> pluginModules = new ArrayList<>();
        if (callModule != null
                && (conversationType.equals(Conversation.ConversationType.PRIVATE)
                || conversationType.equals(Conversation.ConversationType.DISCUSSION)
                || conversationType.equals(Conversation.ConversationType.GROUP))) {
            pluginModules.addAll(callModule.getPlugins(conversationType));
        }
        if (callModule2 != null
                && (conversationType.equals(Conversation.ConversationType.PRIVATE)
                || conversationType.equals(Conversation.ConversationType.DISCUSSION)
                || conversationType.equals(Conversation.ConversationType.GROUP))) {
            pluginModules.addAll(callModule2.getPlugins(conversationType));
        }

        return pluginModules;
    }

    public void onConnected(String token) {
        RLog.i(TAG, "onConnected");
        if (callModule != null) {
            callModule.onConnected(token);
        }
        if (callModule2 != null) {
            callModule2.onConnected(token);
        }
    }

    public void onLoaded() {
        RLog.i(TAG, "onLoaded");
        if (callModule != null) {
            callModule.onViewCreated();
        }
        if (callModule2 != null) {
            callModule2.onViewCreated();
        }
    }
}

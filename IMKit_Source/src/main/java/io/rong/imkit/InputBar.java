package io.rong.imkit;

/**
 * Created by weiqinxiao on 16/4/13.
 */
public class InputBar {
    public enum Style {
        /**
         * 录音切换-输入框-扩展
         */
        STYLE_SWITCH_CONTAINER_EXTENSION(0x123),
        /**
         * 录音切换-输入框
         */
        STYLE_SWITCH_CONTAINER(0x120),
        /**
         * 输入框-扩展
         */
        STYLE_CONTAINER_EXTENSION(0x023),
        /**
         * 扩展-输入框
         */
        STYLE_EXTENSION_CONTAINER(0x320),
        /**
         * 仅有输入框
         */
        STYLE_CONTAINER(0x020);

        int v;

        Style(int v) {
            this.v = v;
        }

        public static Style getStyle(int v) {
            Style result = null;
            for (Style style : values()) {
                if (style.v == v) {
                    result = style;
                    break;
                }
            }
            return result;
        }
    }

    public enum Type {
        /**
         * 默认输入模式
         */
        TYPE_DEFAULT,

        /**
         * 客服输入模式：仅机器人
         */
        TYPE_CS_ROBOT,

        /**
         * 客服输入模式：仅机人工
         */
        TYPE_CS_HUMAN,

        /**
         * 客服智能输入模式：机器人优先
         */
        TYPE_CS_ROBOT_FIRST,

        /**
         * 客服智能输入模式：人工优先
         */
        TYPE_CS_HUMAN_FIRST
    }
}

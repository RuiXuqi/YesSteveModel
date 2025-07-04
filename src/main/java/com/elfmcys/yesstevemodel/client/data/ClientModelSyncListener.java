package com.elfmcys.yesstevemodel.client.data;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

// 所有方法都在 Client Thread 上调用
public interface ClientModelSyncListener {
    /**
     * 收到服务端响应，确认服务器已安装相同版本
     * 仅每次进入游戏后触发一次
     */
    default void onReceiveServerInfo() {}

    /**
     * 收到服务端同步信号，客户端开始准备一轮同步，
     * 每轮同步仅触发一次，
     * 首次进入游戏时此阶段可能要很久
     */
    default void onSyncPreparation() {}

    /**
     * 模型发生重命名或 needAuth 属性改变时触发
     * 每轮同步最多触发一次
     */
    default void onAlterModels(Map<String, ClientModel> models) {}

    /**
     * 通知同步进度，
     * 每轮同步可触发多次，若提前中止则不会触发
     * complete == 0 为同步开始，complete == total 为同步结束，
     * 从收到模型到完成加载需要点时间，触发该事件时大概率还未加载完成
     */
    default void onSyncProgression(int total, int complete) {}

    /**
     * 收到的一个模型加载完成时触发，
     * 每轮同步可触发多次，
     * 由于模型加载比较滞后，可能整个模型同步会话都结束了还没加载完，仍会照常触发
     */
    default void onNewModelLoaded(Map<String, ClientModel> models, String newModelId, ClientModel newModel) {}

    /**
     * 同步中止，放弃本轮同步，重置会话状态，
     * 每轮同步最多触发一次，
     * 常发生在同步未完成就退出游戏的情况（但不止这一种情况）
     */
    default void onSyncAbort() {}

    /**
     * 同步完成后通知错误，没有则不触发
     * 每轮同步最多触发一次，
     * msg 为 null 则为未知错误
     */
    default void onSyncFailed(@Nullable Component msg) {}
}

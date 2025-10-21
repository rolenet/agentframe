package com.agentcore.core.agent;

import com.agentcore.core.behavior.Behavior;
import com.agentcore.core.message.AgentMessage;

import java.util.concurrent.CompletableFuture;

/**
 * Agent接口
 * 定义Agent的基本能力和生命周期方法
 * 
 * @author AgentCore Team
 */
public interface Agent {

    /**
     * 获取Agent ID
     * 
     * @return Agent身份标识
     */
    AgentId getAgentId();

    /**
     * 获取Agent当前状态
     * 
     * @return Agent状态
     */
    AgentState getState();

    /**
     * 初始化Agent
     * 在Agent启动前调用，用于初始化资源和配置
     */
    void init();

    /**
     * 启动Agent
     * 开始Agent的生命周期，启动行为调度器
     */
    void start();

    /**
     * 暂停Agent
     * 暂停Agent的行为执行，但保持状态
     */
    void suspend();

    /**
     * 恢复Agent
     * 从暂停状态恢复Agent的执行
     */
    void resume();

    /**
     * 停止Agent
     * 停止Agent的执行，清理资源
     */
    void stop();

    /**
     * 销毁Agent
     * 完全销毁Agent，释放所有资源
     */
    void destroy();

    /**
     * 添加行为
     * 
     * @param behavior 要添加的行为
     */
    void addBehavior(Behavior behavior);

    /**
     * 移除行为
     * 
     * @param behavior 要移除的行为
     */
    void removeBehavior(Behavior behavior);

    /**
     * 发送消息（异步）
     * 
     * @param message 要发送的消息
     * @return CompletableFuture，完成时返回发送结果
     */
    CompletableFuture<Void> sendMessage(AgentMessage message);

    /**
     * 发送消息并等待响应（同步）
     * 
     * @param message 要发送的消息
     * @param timeoutMs 超时时间（毫秒）
     * @return 响应消息
     */
    AgentMessage sendAndWait(AgentMessage message, long timeoutMs);

    /**
     * 处理接收到的消息
     * 
     * @param message 接收到的消息
     */
    void handleMessage(AgentMessage message);

    /**
     * 获取Agent的元数据信息
     * 
     * @return Agent元数据
     */
    AgentMetadata getMetadata();

    /**
     * 设置Agent的元数据信息
     * 
     * @param metadata Agent元数据
     */
    void setMetadata(AgentMetadata metadata);

    /**
     * 检查Agent是否处于活跃状态
     * 
     * @return 如果Agent活跃返回true
     */
    default boolean isActive() {
        return getState().isActive();
    }

    /**
     * 检查Agent是否已终止
     * 
     * @return 如果Agent已终止返回true
     */
    default boolean isTerminated() {
        return getState().isTerminated();
    }

    /**
     * 获取Agent类型
     * 
     * @return Agent类型字符串
     */
    default String getType() {
        return getAgentId().type();
    }

    /**
     * 获取Agent名称
     * 
     * @return Agent名称
     */
    default String getName() {
        return getAgentId().name();
    }
}
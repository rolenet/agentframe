package com.agentcore.container;

import com.agentcore.core.agent.Agent;
import com.agentcore.core.agent.AgentId;
import com.agentcore.core.message.AgentMessage;
import com.agentcore.communication.router.MessageRouter;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Agent容器接口
 * 负责管理Agent的生命周期和资源
 * 
 * @author AgentCore Team
 */
public interface AgentContainer {

    /**
     * 启动容器
     * 
     * @return CompletableFuture，完成时表示启动成功
     */
    CompletableFuture<Void> start();

    /**
     * 停止容器
     * 
     * @return CompletableFuture，完成时表示停止成功
     */
    CompletableFuture<Void> stop();

    /**
     * 创建并添加Agent到容器
     * 
     * @param agentClass Agent类
     * @param agentId Agent ID
     * @param <T> Agent类型
     * @return 创建的Agent实例
     */
    <T extends Agent> T createAgent(Class<T> agentClass, AgentId agentId);

    /**
     * 添加Agent到容器
     * 
     * @param agent Agent实例
     */
    void addAgent(Agent agent);

    /**
     * 注册Agent（兼容性方法）
     * 
     * @param agent Agent实例
     * @return 异步结果
     */
    default CompletableFuture<Void> registerAgent(Agent agent) {
        addAgent(agent);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 启动Agent（兼容性方法）
     * 
     * @param agentId Agent ID
     * @return 异步结果
     */
    default CompletableFuture<Void> startAgent(AgentId agentId) {
        Agent agent = getAgent(agentId);
        if (agent != null) {
            try {
                //agent.init();
                agent.start();
                return CompletableFuture.completedFuture(null);
            } catch (Exception e) {
                return CompletableFuture.failedFuture(e);
            }
        }
        return CompletableFuture.failedFuture(new IllegalArgumentException("Agent not found: " + agentId));
    }

    /**
     * 停止Agent（兼容性方法）
     * 
     * @param agentId Agent ID
     * @return 异步结果
     */
    default CompletableFuture<Void> stopAgent(AgentId agentId) {
        Agent agent = getAgent(agentId);
        if (agent != null) {
            try {
                agent.stop();
                return CompletableFuture.completedFuture(null);
            } catch (Exception e) {
                return CompletableFuture.failedFuture(e);
            }
        }
        return CompletableFuture.failedFuture(new IllegalArgumentException("Agent not found: " + agentId));
    }

    /**
     * 从容器中移除Agent
     * 
     * @param agentId Agent ID
     * @return 被移除的Agent，如果不存在返回null
     */
    Agent removeAgent(AgentId agentId);

    /**
     * 根据ID获取Agent
     * 
     * @param agentId Agent ID
     * @return Agent实例，如果不存在返回null
     */
    Agent getAgent(AgentId agentId);

    /**
     * 根据名称获取Agent
     * 
     * @param name Agent名称
     * @return Agent实例，如果不存在返回null
     */
    Agent getAgent(String name);

    /**
     * 获取所有Agent
     * 
     * @return Agent列表
     */
    List<Agent> getAllAgents();

    /**
     * 根据类型获取Agent列表
     * 
     * @param type Agent类型
     * @return Agent列表
     */
    List<Agent> getAgentsByType(String type);

    /**
     * 检查Agent是否存在
     * 
     * @param agentId Agent ID
     * @return 如果存在返回true
     */
    boolean containsAgent(AgentId agentId);

    /**
     * 获取Agent数量
     * 
     * @return Agent数量
     */
    int getAgentCount();

    /**
     * 获取容器名称
     * 
     * @return 容器名称
     */
    String getName();

    /**
     * 检查容器是否正在运行
     * 
     * @return 如果正在运行返回true
     */
    boolean isRunning();

    /**
     * 获取容器配置
     * 
     * @return 容器配置
     */
    ContainerConfig getConfig();

    /**
     * 获取容器统计信息
     * 
     * @return 容器统计信息
     */
    ContainerStats getStats();

    /**
     * 设置Agent事件监听器
     * 
     * @param listener 事件监听器
     */
    void setAgentEventListener(AgentEventListener listener);

    void sendMessage(String agentName, AgentMessage testMessage);

    void registerAgent(String agentName, Agent simpleAgent);

    /**
     * 获取消息路由器
     */
    MessageRouter getMessageRouter();

    /**
     * Agent事件监听器接口
     */
    interface AgentEventListener {
        /**
         * Agent添加事件
         * 
         * @param agent 被添加的Agent
         */
        default void onAgentAdded(Agent agent) {}

        /**
         * Agent移除事件
         * 
         * @param agent 被移除的Agent
         */
        default void onAgentRemoved(Agent agent) {}

        /**
         * Agent启动事件
         * 
         * @param agent 被启动的Agent
         */
        default void onAgentStarted(Agent agent) {}

        /**
         * Agent停止事件
         * 
         * @param agent 被停止的Agent
         */
        default void onAgentStopped(Agent agent) {}

        /**
         * Agent错误事件
         * 
         * @param agent 出错的Agent
         * @param error 错误信息
         */
        default void onAgentError(Agent agent, Throwable error) {}
    }

    /**
     * 容器统计信息记录类
     */
    record ContainerStats(
        String name,
        boolean running,
        int totalAgents,
        int activeAgents,
        int suspendedAgents,
        int errorAgents,
        long messagesProcessed,
        double averageResponseTimeMs
    ) {
        @Override
        public String toString() {
            return String.format(
                "ContainerStats{name=%s, running=%s, total=%d, active=%d, suspended=%d, errors=%d, messages=%d, avgTime=%.2fms}",
                name, running, totalAgents, activeAgents, suspendedAgents, errorAgents, messagesProcessed, averageResponseTimeMs
            );
        }
    }
}
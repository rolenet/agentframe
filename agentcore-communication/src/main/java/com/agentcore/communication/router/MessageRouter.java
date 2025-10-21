package com.agentcore.communication.router;

import com.agentcore.core.agent.AgentId;
import com.agentcore.core.message.AgentMessage;

import java.util.concurrent.CompletableFuture;

/**
 * 消息路由器接口
 * 负责将消息路由到正确的目标Agent
 * 
 * @author AgentCore Team
 */
public interface MessageRouter {

    /**
     * 路由消息到目标Agent
     * 
     * @param message 要路由的消息
     * @return CompletableFuture，完成时表示路由成功
     */
    CompletableFuture<Void> routeMessage(AgentMessage message);

    /**
     * 注册Agent到路由表
     * 
     * @param agentId Agent ID
     * @param handler 消息处理器
     */
    void registerAgent(AgentId agentId, MessageHandler handler);

    /**
     * 从路由表注销Agent
     * 
     * @param agentId Agent ID
     */
    void unregisterAgent(AgentId agentId);

    /**
     * 检查Agent是否已注册
     * 
     * @param agentId Agent ID
     * @return 如果已注册返回true
     */
    boolean isRegistered(AgentId agentId);

    /**
     * 获取已注册的Agent数量
     * 
     * @return Agent数量
     */
    int getRegisteredAgentCount();

    /**
     * 获取路由器统计信息
     * 
     * @return 路由器统计信息
     */
    RouterStats getStats();

    /**
     * 消息处理器接口
     */
    @FunctionalInterface
    interface MessageHandler {
        /**
         * 处理消息
         * 
         * @param message 要处理的消息
         */
        void handleMessage(AgentMessage message);
    }

    /**
     * 路由器统计信息记录类
     */
    record RouterStats(
        int registeredAgents,
        long messagesRouted,
        long routingErrors,
        double averageRoutingTimeMs
    ) {
        @Override
        public String toString() {
            return String.format(
                "RouterStats{agents=%d, routed=%d, errors=%d, avgTime=%.2fms}",
                registeredAgents, messagesRouted, routingErrors, averageRoutingTimeMs
            );
        }
    }
}
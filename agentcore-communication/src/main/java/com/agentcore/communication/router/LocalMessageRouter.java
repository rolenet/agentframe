package com.agentcore.communication.router;

import com.agentcore.core.agent.AgentId;
import com.agentcore.core.message.AgentMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 本地消息路由器实现
 * 用于同一JVM内的消息路由
 * 
 * @author AgentCore Team
 */
public class LocalMessageRouter implements MessageRouter {

    private static final Logger logger = LoggerFactory.getLogger(LocalMessageRouter.class);

    private final ConcurrentHashMap<String, MessageHandler> routingTable = new ConcurrentHashMap<>();
    
    // 统计信息
    private final AtomicLong messagesRouted = new AtomicLong(0);
    private final AtomicLong routingErrors = new AtomicLong(0);
    private final AtomicLong totalRoutingTime = new AtomicLong(0);

    @Override
    public CompletableFuture<Void> routeMessage(AgentMessage message) {
        long startTime = System.nanoTime();
        
        return CompletableFuture.runAsync(() -> {
            try {
                AgentId receiver = message.receiver();
                String routingKey = createRoutingKey(receiver);
                
                MessageHandler handler = routingTable.get(routingKey);
                if (handler != null) {              
                    handler.handleMessage(message);
                    messagesRouted.incrementAndGet();
                    
                    logger.debug("Routed message from {} to {}", 
                        message.sender().getShortId(), receiver.getShortId());
                } else {
                    routingErrors.incrementAndGet();
                    logger.warn("No handler found for agent: {}", receiver);
                    throw new RuntimeException("No handler found for agent: " + receiver);
                }
                
            } catch (Exception e) {
                routingErrors.incrementAndGet();
                logger.error("Error routing message", e);
                throw new RuntimeException("Message routing failed", e);
            } finally {
                long routingTime = System.nanoTime() - startTime;
                totalRoutingTime.addAndGet(routingTime);
            }
        });
    }

    @Override
    public void registerAgent(AgentId agentId, MessageHandler handler) {
        if (agentId == null || handler == null) {
            throw new IllegalArgumentException("AgentId and handler cannot be null");
        }

        String routingKey = createRoutingKey(agentId);
        routingTable.put(routingKey, handler);
        
        logger.debug("Registered agent in routing table: {}", agentId.getShortId());
    }

    @Override
    public void unregisterAgent(AgentId agentId) {
        if (agentId == null) {
            return;
        }

        String routingKey = createRoutingKey(agentId);
        MessageHandler removed = routingTable.remove(routingKey);
        
        if (removed != null) {
            logger.debug("Unregistered agent from routing table: {}", agentId.getShortId());
        }
    }

    @Override
    public boolean isRegistered(AgentId agentId) {
        if (agentId == null) {
            return false;
        }

        String routingKey = createRoutingKey(agentId);
        return routingTable.containsKey(routingKey);
    }

    @Override
    public int getRegisteredAgentCount() {
        return routingTable.size();
    }

    @Override
    public RouterStats getStats() {
        long routed = messagesRouted.get();
        double avgTime = routed > 0 ? 
            (totalRoutingTime.get() / 1_000_000.0) / routed : 0.0;
        
        return new RouterStats(
            routingTable.size(),
            routed,
            routingErrors.get(),
            avgTime
        );
    }

    /**
     * 创建路由键
     * 
     * @param agentId Agent ID
     * @return 路由键
     */
    private String createRoutingKey(AgentId agentId) {
        // 使用Agent的完整ID作为路由键
        return agentId.getFullId();
    }

    /**
     * 获取路由表（用于测试）
     * 
     * @return 路由表
     */
    ConcurrentHashMap<String, MessageHandler> getRoutingTable() {
        return routingTable;
    }

    /**
     * 清理路由表（用于测试）
     */
    void clearRoutingTable() {
        routingTable.clear();
    }
}
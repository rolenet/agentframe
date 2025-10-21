package com.agentcore.communication.router;

import com.agentcore.communication.transport.Transport;
import com.agentcore.core.agent.AgentId;
import com.agentcore.core.message.AgentMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 集群消息路由器实现
 * 支持跨节点的消息路由
 * 
 * @author AgentCore Team
 */
public class ClusterMessageRouter implements MessageRouter {

    private static final Logger logger = LoggerFactory.getLogger(ClusterMessageRouter.class);

    private final LocalMessageRouter localRouter;
    private final List<Transport> transports;
    private final ConcurrentHashMap<String, String> agentLocationCache;
    
    // 统计信息
    private final AtomicLong messagesRouted = new AtomicLong(0);
    private final AtomicLong routingErrors = new AtomicLong(0);
    private final AtomicLong totalRoutingTime = new AtomicLong(0);

    /**
     * 构造函数
     */
    public ClusterMessageRouter() {
        this.localRouter = new LocalMessageRouter();
        this.transports = new CopyOnWriteArrayList<>();
        this.agentLocationCache = new ConcurrentHashMap<>();
    }

    @Override
    public CompletableFuture<Void> routeMessage(AgentMessage message) {
        long startTime = System.nanoTime();
        
        return CompletableFuture.runAsync(() -> {
            try {
                AgentId receiver = message.receiver();
                
                // 首先尝试本地路由
                if (localRouter.isRegistered(receiver)) {
                    localRouter.routeMessage(message).join();
                    messagesRouted.incrementAndGet();
                    logger.debug("Routed message locally from {} to {}", 
                        message.sender().getShortId(), receiver.getShortId());
                    return;
                }
                
                // 尝试远程路由
                boolean routed = routeToRemote(message);
                if (routed) {
                    messagesRouted.incrementAndGet();
                    logger.debug("Routed message remotely from {} to {}", 
                        message.sender().getShortId(), receiver.getShortId());
                } else {
                    routingErrors.incrementAndGet();
                    logger.warn("No route found for agent: {}", receiver);
                    throw new RuntimeException("No route found for agent: " + receiver);
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
        // 注册到本地路由器
        localRouter.registerAgent(agentId, handler);
        
        // 更新位置缓存
        agentLocationCache.put(agentId.getFullId(), "local");
        
        logger.debug("Registered agent in cluster router: {}", agentId.getShortId());
    }

    @Override
    public void unregisterAgent(AgentId agentId) {
        // 从本地路由器注销
        localRouter.unregisterAgent(agentId);
        
        // 从位置缓存中移除
        agentLocationCache.remove(agentId.getFullId());
        
        logger.debug("Unregistered agent from cluster router: {}", agentId.getShortId());
    }

    @Override
    public boolean isRegistered(AgentId agentId) {
        // 检查本地注册
        if (localRouter.isRegistered(agentId)) {
            return true;
        }
        
        // 检查位置缓存
        return agentLocationCache.containsKey(agentId.getFullId());
    }

    @Override
    public int getRegisteredAgentCount() {
        return localRouter.getRegisteredAgentCount() + 
               (int) agentLocationCache.values().stream()
                   .filter(location -> !"local".equals(location))
                   .count();
    }

    @Override
    public RouterStats getStats() {
        long routed = messagesRouted.get();
        double avgTime = routed > 0 ? 
            (totalRoutingTime.get() / 1_000_000.0) / routed : 0.0;
        
        return new RouterStats(
            getRegisteredAgentCount(),
            routed,
            routingErrors.get(),
            avgTime
        );
    }

    /**
     * 添加传输层
     * 
     * @param transport 传输层实例
     */
    public void addTransport(Transport transport) {
        if (transport != null && !transports.contains(transport)) {
            transports.add(transport);
            logger.debug("Added transport to cluster router: {}", transport.getName());
        }
    }

    /**
     * 移除传输层
     * 
     * @param transport 传输层实例
     */
    public void removeTransport(Transport transport) {
        if (transport != null) {
            transports.remove(transport);
            logger.debug("Removed transport from cluster router: {}", transport.getName());
        }
    }

    /**
     * 注册远程Agent位置
     * 
     * @param agentId Agent ID
     * @param location Agent位置（节点地址）
     */
    public void registerRemoteAgent(AgentId agentId, String location) {
        agentLocationCache.put(agentId.getFullId(), location);
        logger.debug("Registered remote agent {} at location {}", 
            agentId.getShortId(), location);
    }

    /**
     * 路由消息到远程节点
     * 
     * @param message 要路由的消息
     * @return 如果成功路由返回true
     */
    private boolean routeToRemote(AgentMessage message) {
        AgentId receiver = message.receiver();
        
        // 查找目标Agent的位置
        String location = agentLocationCache.get(receiver.getFullId());
        if (location == null || "local".equals(location)) {
            // 尝试根据Agent地址推断位置
            location = receiver.address();
        }
        
        // 查找合适的传输层
        for (Transport transport : transports) {
            if (transport.isRunning() && transport.supports(receiver)) {
                try {
                    transport.sendMessage(message).join();
                    return true;
                } catch (Exception e) {
                    logger.warn("Failed to route message via transport {}: {}", 
                        transport.getName(), e.getMessage());
                }
            }
        }
        
        return false;
    }

    /**
     * 获取本地路由器（用于测试）
     * 
     * @return 本地路由器
     */
    LocalMessageRouter getLocalRouter() {
        return localRouter;
    }

    /**
     * 获取传输层列表（用于测试）
     * 
     * @return 传输层列表
     */
    List<Transport> getTransports() {
        return transports;
    }

    /**
     * 获取位置缓存（用于测试）
     * 
     * @return 位置缓存
     */
    ConcurrentHashMap<String, String> getAgentLocationCache() {
        return agentLocationCache;
    }
}
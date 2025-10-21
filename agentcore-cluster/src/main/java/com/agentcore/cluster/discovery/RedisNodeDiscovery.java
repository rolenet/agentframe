package com.agentcore.cluster.discovery;

import com.agentcore.cluster.node.ClusterNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 基于Redis的节点发现实现
 * 
 * @author AgentCore Team
 */
public class RedisNodeDiscovery implements NodeDiscovery {

    private static final Logger logger = LoggerFactory.getLogger(RedisNodeDiscovery.class);
    
    private static final String NODE_REGISTRY_KEY = "agentcore:cluster:nodes";
    private static final String NODE_HEARTBEAT_KEY = "agentcore:cluster:heartbeat:";
    private static final String NODE_EVENT_CHANNEL = "agentcore:cluster:events";
    
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisMessageListenerContainer messageListenerContainer;
    private final ObjectMapper objectMapper;
    private final List<DiscoveryListener> listeners = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    private final Duration heartbeatInterval;
    private final Duration nodeTimeout;
    
    private volatile boolean running = false;

    /**
     * 构造函数
     * 
     * @param redisTemplate Redis模板
     * @param messageListenerContainer Redis消息监听容器
     */
    public RedisNodeDiscovery(RedisTemplate<String, String> redisTemplate,
                             RedisMessageListenerContainer messageListenerContainer) {
        this(redisTemplate, messageListenerContainer, Duration.ofSeconds(30), Duration.ofMinutes(2));
    }

    /**
     * 构造函数
     * 
     * @param redisTemplate Redis模板
     * @param messageListenerContainer Redis消息监听容器
     * @param heartbeatInterval 心跳间隔
     * @param nodeTimeout 节点超时时间
     */
    public RedisNodeDiscovery(RedisTemplate<String, String> redisTemplate,
                             RedisMessageListenerContainer messageListenerContainer,
                             Duration heartbeatInterval,
                             Duration nodeTimeout) {
        this.redisTemplate = redisTemplate;
        this.messageListenerContainer = messageListenerContainer;
        this.objectMapper = new ObjectMapper();
        this.heartbeatInterval = heartbeatInterval;
        this.nodeTimeout = nodeTimeout;
    }

    @Override
    public CompletableFuture<Void> start() {
        if (running) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                // 启动消息监听
                MessageListenerAdapter listenerAdapter = new MessageListenerAdapter(
                    new NodeEventMessageListener(), "onMessage");
                messageListenerContainer.addMessageListener(
                    listenerAdapter, new ChannelTopic(NODE_EVENT_CHANNEL));

                // 启动心跳检查任务
                scheduler.scheduleWithFixedDelay(
                    this::checkNodeHealth,
                    nodeTimeout.toSeconds(),
                    heartbeatInterval.toSeconds(),
                    TimeUnit.SECONDS
                );

                running = true;
                logger.info("Redis node discovery started");
            } catch (Exception e) {
                logger.error("Failed to start Redis node discovery", e);
                throw new RuntimeException("Node discovery start failed", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> stop() {
        if (!running) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                running = false;
                scheduler.shutdown();
                
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
                
                logger.info("Redis node discovery stopped");
            } catch (Exception e) {
                logger.error("Error stopping Redis node discovery", e);
                throw new RuntimeException("Node discovery stop failed", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> registerNode(ClusterNode node) {
        return CompletableFuture.runAsync(() -> {
            try {
                String nodeJson = objectMapper.writeValueAsString(node);
                
                // 注册节点信息
                redisTemplate.opsForHash().put(NODE_REGISTRY_KEY, node.nodeId(), nodeJson);
                
                // 设置心跳
                String heartbeatKey = NODE_HEARTBEAT_KEY + node.nodeId();
                redisTemplate.opsForValue().set(heartbeatKey, 
                    String.valueOf(System.currentTimeMillis()), nodeTimeout);
                
                // 发布节点加入事件
                publishNodeEvent("NODE_JOINED", node);
                
                // 启动心跳任务
                scheduler.scheduleWithFixedDelay(
                    () -> updateHeartbeat(node.nodeId()),
                    heartbeatInterval.toSeconds(),
                    heartbeatInterval.toSeconds(),
                    TimeUnit.SECONDS
                );
                
                logger.info("Registered node: {}", node.nodeId());
            } catch (Exception e) {
                logger.error("Failed to register node: " + node.nodeId(), e);
                throw new RuntimeException("Node registration failed", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> unregisterNode(String nodeId) {
        return CompletableFuture.runAsync(() -> {
            try {
                // 获取节点信息
                String nodeJson = (String) redisTemplate.opsForHash().get(NODE_REGISTRY_KEY, nodeId);
                if (nodeJson != null) {
                    ClusterNode node = objectMapper.readValue(nodeJson, ClusterNode.class);
                    
                    // 删除节点信息
                    redisTemplate.opsForHash().delete(NODE_REGISTRY_KEY, nodeId);
                    
                    // 删除心跳信息
                    redisTemplate.delete(NODE_HEARTBEAT_KEY + nodeId);
                    
                    // 发布节点离开事件
                    publishNodeEvent("NODE_LEFT", node);
                    
                    logger.info("Unregistered node: {}", nodeId);
                }
            } catch (Exception e) {
                logger.error("Failed to unregister node: " + nodeId, e);
                throw new RuntimeException("Node unregistration failed", e);
            }
        });
    }

    @Override
    public CompletableFuture<List<ClusterNode>> discoverNodes() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<ClusterNode> nodes = new ArrayList<>();
                Set<Object> nodeIds = redisTemplate.opsForHash().keys(NODE_REGISTRY_KEY);
                
                for (Object nodeIdObj : nodeIds) {
                    String nodeId = (String) nodeIdObj;
                    String nodeJson = (String) redisTemplate.opsForHash().get(NODE_REGISTRY_KEY, nodeId);
                    
                    if (nodeJson != null) {
                        ClusterNode node = objectMapper.readValue(nodeJson, ClusterNode.class);
                        
                        // 检查节点是否仍然活跃
                        if (isNodeAlive(nodeId)) {
                            nodes.add(node);
                        } else {
                            // 清理死节点
                            cleanupDeadNode(nodeId);
                        }
                    }
                }
                
                logger.debug("Discovered {} nodes", nodes.size());
                return nodes;
            } catch (Exception e) {
                logger.error("Failed to discover nodes", e);
                throw new RuntimeException("Node discovery failed", e);
            }
        });
    }

    @Override
    public void addDiscoveryListener(DiscoveryListener listener) {
        if (listener != null) {
            listeners.add(listener);
            logger.debug("Added discovery listener: {}", listener.getClass().getSimpleName());
        }
    }

    @Override
    public void removeDiscoveryListener(DiscoveryListener listener) {
        if (listener != null) {
            listeners.remove(listener);
            logger.debug("Removed discovery listener: {}", listener.getClass().getSimpleName());
        }
    }

    @Override
    public CompletableFuture<Void> updateNode(ClusterNode node) {
        return CompletableFuture.runAsync(() -> {
            try {
                String nodeJson = objectMapper.writeValueAsString(node);
                
                // 更新节点信息
                redisTemplate.opsForHash().put(NODE_REGISTRY_KEY, node.nodeId(), nodeJson);
                
                // 更新心跳
                String heartbeatKey = NODE_HEARTBEAT_KEY + node.nodeId();
                redisTemplate.opsForValue().set(heartbeatKey, 
                    String.valueOf(System.currentTimeMillis()), nodeTimeout);
                
                // 发布节点更新事件
                publishNodeEvent("NODE_UPDATED", node);
                
                logger.debug("Updated node: {}", node.nodeId());
            } catch (Exception e) {
                logger.error("Failed to update node: " + node.nodeId(), e);
                throw new RuntimeException("Node update failed", e);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> isNodeOnline(String nodeId) {
        return CompletableFuture.supplyAsync(() -> isNodeAlive(nodeId));
    }

    @Override
    public CompletableFuture<List<ClusterNode>> discoverNodesByType(ClusterNode.NodeType nodeType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<ClusterNode> allNodes = discoverNodes().join();
                return allNodes.stream()
                    .filter(node -> node.nodeType() == nodeType)
                    .toList();
            } catch (Exception e) {
                logger.error("Failed to discover nodes by type: " + nodeType, e);
                throw new RuntimeException("Node discovery by type failed", e);
            }
        });
    }

    /**
     * 更新节点心跳
     */
    private void updateHeartbeat(String nodeId) {
        try {
            String heartbeatKey = NODE_HEARTBEAT_KEY + nodeId;
            redisTemplate.opsForValue().set(heartbeatKey, 
                String.valueOf(System.currentTimeMillis()), nodeTimeout);
        } catch (Exception e) {
            logger.error("Failed to update heartbeat for node: " + nodeId, e);
        }
    }

    /**
     * 检查节点是否存活
     */
    private boolean isNodeAlive(String nodeId) {
        try {
            String heartbeatKey = NODE_HEARTBEAT_KEY + nodeId;
            String heartbeatStr = redisTemplate.opsForValue().get(heartbeatKey);
            
            if (heartbeatStr == null) {
                return false;
            }
            
            long lastHeartbeat = Long.parseLong(heartbeatStr);
            long now = System.currentTimeMillis();
            
            return (now - lastHeartbeat) < nodeTimeout.toMillis();
        } catch (Exception e) {
            logger.error("Failed to check node health: " + nodeId, e);
            return false;
        }
    }

    /**
     * 检查所有节点健康状态
     */
    private void checkNodeHealth() {
        try {
            Set<Object> nodeIds = redisTemplate.opsForHash().keys(NODE_REGISTRY_KEY);
            
            for (Object nodeIdObj : nodeIds) {
                String nodeId = (String) nodeIdObj;
                
                if (!isNodeAlive(nodeId)) {
                    logger.warn("Node {} appears to be dead, cleaning up", nodeId);
                    cleanupDeadNode(nodeId);
                }
            }
        } catch (Exception e) {
            logger.error("Error during node health check", e);
        }
    }

    /**
     * 清理死节点
     */
    private void cleanupDeadNode(String nodeId) {
        try {
            String nodeJson = (String) redisTemplate.opsForHash().get(NODE_REGISTRY_KEY, nodeId);
            if (nodeJson != null) {
                ClusterNode node = objectMapper.readValue(nodeJson, ClusterNode.class);
                
                // 删除节点信息
                redisTemplate.opsForHash().delete(NODE_REGISTRY_KEY, nodeId);
                redisTemplate.delete(NODE_HEARTBEAT_KEY + nodeId);
                
                // 通知监听器
                for (DiscoveryListener listener : listeners) {
                    try {
                        listener.onNodeLost(node);
                    } catch (Exception e) {
                        logger.error("Error in node lost listener", e);
                    }
                }
                
                logger.info("Cleaned up dead node: {}", nodeId);
            }
        } catch (Exception e) {
            logger.error("Failed to cleanup dead node: " + nodeId, e);
        }
    }

    /**
     * 发布节点事件
     */
    private void publishNodeEvent(String eventType, ClusterNode node) {
        try {
            NodeEvent event = new NodeEvent(eventType, node, System.currentTimeMillis());
            String eventJson = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(NODE_EVENT_CHANNEL, eventJson);
        } catch (Exception e) {
            logger.error("Failed to publish node event", e);
        }
    }

    /**
     * 节点事件消息监听器
     */
    private class NodeEventMessageListener {
        
        public void onMessage(String message) {
            try {
                NodeEvent event = objectMapper.readValue(message, NodeEvent.class);
                
                for (DiscoveryListener listener : listeners) {
                    try {
                        switch (event.eventType()) {
                            case "NODE_JOINED" -> listener.onNodeDiscovered(event.node());
                            case "NODE_LEFT" -> listener.onNodeLost(event.node());
                            case "NODE_UPDATED" -> {
                                // 对于更新事件，我们需要额外的信息
                                // 这里简化处理，直接调用发现事件
                                listener.onNodeDiscovered(event.node());
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Error in discovery listener", e);
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to process node event message", e);
            }
        }
    }

    /**
     * 节点事件记录
     */
    private record NodeEvent(String eventType, ClusterNode node, long timestamp) {}
}
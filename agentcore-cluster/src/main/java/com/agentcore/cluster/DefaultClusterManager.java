package com.agentcore.cluster;

import com.agentcore.cluster.discovery.NodeDiscovery;
import com.agentcore.cluster.node.ClusterNode;
import com.agentcore.core.agent.AgentId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 默认集群管理器实现
 * 
 * @author AgentCore Team
 */
public class DefaultClusterManager implements ClusterManager {

    private static final Logger logger = LoggerFactory.getLogger(DefaultClusterManager.class);

    private final ClusterConfig config;
    private final NodeDiscovery nodeDiscovery;
    private final ConcurrentHashMap<String, ClusterNode> nodes = new ConcurrentHashMap<>();
    private final List<ClusterEventListener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger roundRobinCounter = new AtomicInteger(0);

    private volatile ClusterNode currentNode;
    private volatile ClusterNode masterNode;

    /**
     * 构造函数
     * 
     * @param config 集群配置
     * @param nodeDiscovery 节点发现服务
     */
    public DefaultClusterManager(ClusterConfig config, NodeDiscovery nodeDiscovery) {
        this.config = config;
        this.nodeDiscovery = nodeDiscovery;
        
        // 创建当前节点
        this.currentNode = ClusterNode.builder(
            config.nodeId(), 
            config.nodeName(), 
            config.bindHost(), 
            config.bindPort()
        ).build();
        
        // 注册节点发现监听器
        this.nodeDiscovery.addDiscoveryListener(new NodeDiscoveryListener());
    }

    @Override
    public CompletableFuture<Void> start() {
        if (!running.compareAndSet(false, true)) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Cluster manager is already running"));
        }

        logger.info("Starting cluster manager for node: {}", currentNode.nodeId());

        return CompletableFuture.runAsync(() -> {
            try {
                // 启动节点发现服务
                nodeDiscovery.start().join();
                
                // 注册当前节点
                nodeDiscovery.registerNode(currentNode).join();
                nodes.put(currentNode.nodeId(), currentNode);
                
                // 发现现有节点
                List<ClusterNode> discoveredNodes = nodeDiscovery.discoverNodes().join();
                for (ClusterNode node : discoveredNodes) {
                    if (!node.nodeId().equals(currentNode.nodeId())) {
                        nodes.put(node.nodeId(), node);
                        notifyNodeJoined(node);
                    }
                }
                
                // 选举主节点
                electMaster();
                
                logger.info("Cluster manager started successfully. Cluster size: {}", nodes.size());
            } catch (Exception e) {
                running.set(false);
                logger.error("Failed to start cluster manager", e);
                throw new RuntimeException("Cluster manager start failed", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> stop() {
        if (!running.compareAndSet(true, false)) {
            return CompletableFuture.completedFuture(null);
        }

        logger.info("Stopping cluster manager for node: {}", currentNode.nodeId());

        return CompletableFuture.runAsync(() -> {
            try {
                // 注销当前节点
                nodeDiscovery.unregisterNode(currentNode.nodeId()).join();
                
                // 停止节点发现服务
                nodeDiscovery.stop().join();
                
                // 清理节点信息
                nodes.clear();
                masterNode = null;
                
                logger.info("Cluster manager stopped successfully");
            } catch (Exception e) {
                logger.error("Error stopping cluster manager", e);
                throw new RuntimeException("Cluster manager stop failed", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> joinCluster(ClusterNode node) {
        return CompletableFuture.runAsync(() -> {
            if (nodes.containsKey(node.nodeId())) {
                logger.debug("Node already in cluster: {}", node.nodeId());
                return;
            }
            
            nodes.put(node.nodeId(), node);
            notifyNodeJoined(node);
            
            // 重新选举主节点
            electMaster();
            
            logger.info("Node joined cluster: {}", node.nodeId());
        });
    }

    @Override
    public CompletableFuture<Void> leaveCluster(String nodeId) {
        return CompletableFuture.runAsync(() -> {
            ClusterNode node = nodes.remove(nodeId);
            if (node != null) {
                notifyNodeLeft(node);
                
                // 如果离开的是主节点，重新选举
                if (node.equals(masterNode)) {
                    electMaster();
                }
                
                logger.info("Node left cluster: {}", nodeId);
            }
        });
    }

    @Override
    public Optional<ClusterNode> getCurrentNode() {
        return Optional.ofNullable(currentNode);
    }

    @Override
    public List<ClusterNode> getAllNodes() {
        return new ArrayList<>(nodes.values());
    }

    @Override
    public List<ClusterNode> getActiveNodes() {
        return nodes.values().stream()
            .filter(ClusterNode::isActive)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<ClusterNode> getNode(String nodeId) {
        return Optional.ofNullable(nodes.get(nodeId));
    }

    @Override
    public List<ClusterNode> getNodesByType(ClusterNode.NodeType nodeType) {
        return nodes.values().stream()
            .filter(node -> node.nodeType() == nodeType)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<ClusterNode> getMasterNode() {
        return Optional.ofNullable(masterNode);
    }

    @Override
    public boolean containsNode(String nodeId) {
        return nodes.containsKey(nodeId);
    }

    @Override
    public int getClusterSize() {
        return nodes.size();
    }

    @Override
    public boolean isClusterMode() {
        return config.isClusterEnabled();
    }

    @Override
    public boolean isMaster() {
        return currentNode != null && currentNode.equals(masterNode);
    }

    @Override
    public Optional<ClusterNode> selectNodeForAgent(AgentId agentId) {
        // 使用一致性哈希选择节点
        return selectNode(LoadBalanceStrategy.CONSISTENT_HASH);
    }

    @Override
    public Optional<ClusterNode> selectNode(LoadBalanceStrategy strategy) {
        List<ClusterNode> availableNodes = getActiveNodes();
        if (availableNodes.isEmpty()) {
            return Optional.empty();
        }

        return switch (strategy) {
            case ROUND_ROBIN -> selectByRoundRobin(availableNodes);
            case RANDOM -> selectByRandom(availableNodes);
            case LEAST_CONNECTIONS -> selectByLeastConnections(availableNodes);
            case LEAST_LOAD -> selectByLeastLoad(availableNodes);
            case CONSISTENT_HASH -> selectByConsistentHash(availableNodes);
        };
    }

    @Override
    public CompletableFuture<Void> broadcast(Object message) {
        List<CompletableFuture<Void>> futures = nodes.values().stream()
            .filter(node -> !node.nodeId().equals(currentNode.nodeId()))
            .map(node -> sendToNode(node.nodeId(), message))
            .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    @Override
    public CompletableFuture<Void> sendToNode(String nodeId, Object message) {
        return CompletableFuture.runAsync(() -> {
            ClusterNode targetNode = nodes.get(nodeId);
            if (targetNode == null) {
                throw new IllegalArgumentException("Node not found: " + nodeId);
            }
            
            // TODO: 实现实际的消息发送逻辑
            logger.debug("Sending message to node {}: {}", nodeId, message);
        });
    }

    @Override
    public ClusterConfig getConfig() {
        return config;
    }

    @Override
    public ClusterStats getStats() {
        int totalNodes = nodes.size();
        int activeNodes = (int) nodes.values().stream().filter(ClusterNode::isActive).count();
        int masterNodes = (int) nodes.values().stream().filter(ClusterNode::isMaster).count();
        int workerNodes = (int) nodes.values().stream()
            .filter(node -> node.nodeType() == ClusterNode.NodeType.WORKER).count();

        double averageLoad = nodes.values().stream()
            .mapToDouble(node -> node.load().getLoadScore())
            .average()
            .orElse(0.0);

        long totalAgents = nodes.values().stream()
            .mapToLong(node -> node.load().agentCount())
            .sum();

        long messagesPerSecondLong = nodes.values().stream()
            .mapToLong(node -> node.load().messagesPerSecond())
            .sum();

        boolean isHealthy = activeNodes > totalNodes / 2; // 超过一半节点活跃

        return new ClusterStats(
            totalNodes, activeNodes, masterNodes, workerNodes,
            averageLoad, totalAgents, messagesPerSecondLong, isHealthy
        );
    }

    @Override
    public void addClusterEventListener(ClusterEventListener listener) {
        if (listener != null) {
            listeners.add(listener);
            logger.debug("Added cluster event listener: {}", listener.getClass().getSimpleName());
        }
    }

    @Override
    public void removeClusterEventListener(ClusterEventListener listener) {
        if (listener != null) {
            listeners.remove(listener);
            logger.debug("Removed cluster event listener: {}", listener.getClass().getSimpleName());
        }
    }

    /**
     * 选举主节点
     */
    private void electMaster() {
        // 简单的选举算法：选择节点ID最小的活跃节点作为主节点
        Optional<ClusterNode> newMaster = nodes.values().stream()
            .filter(ClusterNode::isActive)
            .min(Comparator.comparing(ClusterNode::nodeId));

        if (newMaster.isPresent() && !newMaster.get().equals(masterNode)) {
            ClusterNode oldMaster = masterNode;
            masterNode = newMaster.get();
            notifyMasterChanged(oldMaster, masterNode);
            logger.info("New master elected: {}", masterNode.nodeId());
        }
    }

    /**
     * 轮询选择节点
     */
    private Optional<ClusterNode> selectByRoundRobin(List<ClusterNode> nodes) {
        if (nodes.isEmpty()) {
            return Optional.empty();
        }
        int index = roundRobinCounter.getAndIncrement() % nodes.size();
        return Optional.of(nodes.get(index));
    }

    /**
     * 随机选择节点
     */
    private Optional<ClusterNode> selectByRandom(List<ClusterNode> nodes) {
        if (nodes.isEmpty()) {
            return Optional.empty();
        }
        int index = new Random().nextInt(nodes.size());
        return Optional.of(nodes.get(index));
    }

    /**
     * 选择连接数最少的节点
     */
    private Optional<ClusterNode> selectByLeastConnections(List<ClusterNode> nodes) {
        return nodes.stream()
            .min(Comparator.comparingInt(node -> node.load().activeConnections()));
    }

    /**
     * 选择负载最低的节点
     */
    private Optional<ClusterNode> selectByLeastLoad(List<ClusterNode> nodes) {
        return nodes.stream()
            .min(Comparator.comparingDouble(node -> node.load().getLoadScore()));
    }

    /**
     * 一致性哈希选择节点
     */
    private Optional<ClusterNode> selectByConsistentHash(List<ClusterNode> nodes) {
        if (nodes.isEmpty()) {
            return Optional.empty();
        }
        
        // 简化的一致性哈希实现
        // 实际应用中应该使用更完善的一致性哈希算法
        int hash = (int) Math.abs(System.currentTimeMillis() % nodes.size());
        return Optional.of(nodes.get(hash));
    }

    /**
     * 通知节点加入事件
     */
    private void notifyNodeJoined(ClusterNode node) {
        for (ClusterEventListener listener : listeners) {
            try {
                listener.onNodeJoined(node);
            } catch (Exception e) {
                logger.error("Error in node joined listener", e);
            }
        }
    }

    /**
     * 通知节点离开事件
     */
    private void notifyNodeLeft(ClusterNode node) {
        for (ClusterEventListener listener : listeners) {
            try {
                listener.onNodeLeft(node);
            } catch (Exception e) {
                logger.error("Error in node left listener", e);
            }
        }
    }

    /**
     * 通知主节点变更事件
     */
    private void notifyMasterChanged(ClusterNode oldMaster, ClusterNode newMaster) {
        for (ClusterEventListener listener : listeners) {
            try {
                listener.onMasterChanged(oldMaster, newMaster);
            } catch (Exception e) {
                logger.error("Error in master changed listener", e);
            }
        }
    }

    /**
     * 节点发现监听器
     */
    private class NodeDiscoveryListener implements NodeDiscovery.DiscoveryListener {
        
        @Override
        public void onNodeDiscovered(ClusterNode node) {
            if (!node.nodeId().equals(currentNode.nodeId())) {
                joinCluster(node);
            }
        }

        @Override
        public void onNodeLost(ClusterNode node) {
            leaveCluster(node.nodeId());
        }

        @Override
        public void onNodeUpdated(ClusterNode oldNode, ClusterNode newNode) {
            if (nodes.containsKey(newNode.nodeId())) {
                nodes.put(newNode.nodeId(), newNode);
                logger.debug("Updated node information: {}", newNode.nodeId());
            }
        }
    }
}
package com.agentcore.examples;

import com.agentcore.core.agent.AgentId;
import com.agentcore.core.message.AgentMessage;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 集群功能测试类
 * 模拟agentcore-cluster模块的功能测试
 * 
 * @author AgentCore Team
 */
public class ClusterTest {
    
    /**
     * 测试节点管理功能
     */
    public void testNodeManagement() {
        System.out.println("=== 测试节点管理 ===");
        
        SimpleClusterManager clusterManager = new SimpleClusterManager();
        
        // 创建测试节点
        SimpleClusterNode node1 = new SimpleClusterNode("node-1", "192.168.1.101");
        SimpleClusterNode node2 = new SimpleClusterNode("node-2", "192.168.1.102");
        SimpleClusterNode node3 = new SimpleClusterNode("node-3", "192.168.1.103");
        
        // 注册节点
        clusterManager.registerNode(node1);
        clusterManager.registerNode(node2);
        clusterManager.registerNode(node3);
        
        // 验证节点注册
        assert clusterManager.getNodeCount() == 3 : "Should have 3 nodes registered";
        assert clusterManager.isNodeRegistered("node-1") : "Node1 should be registered";
        assert clusterManager.isNodeRegistered("node-2") : "Node2 should be registered";
        assert clusterManager.isNodeRegistered("node-3") : "Node3 should be registered";
        
        // 测试节点查找
        Optional<SimpleClusterNode> foundNode = clusterManager.findNode("node-2");
        assert foundNode.isPresent() : "Should find node-2";
        assert foundNode.get().getNodeId().equals("node-2") : "Found correct node";
        
        // 注销节点
        clusterManager.unregisterNode("node-2");
        assert clusterManager.getNodeCount() == 2 : "Should have 2 nodes after unregister";
        assert !clusterManager.isNodeRegistered("node-2") : "Node2 should be unregistered";
        
        System.out.println("✓ 节点管理测试通过");
    }
    
    /**
     * 测试负载均衡
     */
    public void testLoadBalancing() {
        System.out.println("=== 测试负载均衡 ===");
        
        SimpleClusterManager clusterManager = new SimpleClusterManager();
        
        // 创建带负载统计的节点
        LoadBalancedNode node1 = new LoadBalancedNode("node-1", "192.168.1.101");
        LoadBalancedNode node2 = new LoadBalancedNode("node-2", "192.168.1.102");
        LoadBalancedNode node3 = new LoadBalancedNode("node-3", "192.168.1.103");
        
        clusterManager.registerNode(node1);
        clusterManager.registerNode(node2);
        clusterManager.registerNode(node3);
        
        // 模拟负载分配
        int totalRequests = 1000;
        for (int i = 0; i < totalRequests; i++) {
            AgentId targetAgent = AgentId.create("agent-" + i);
            SimpleClusterNode selectedNode = clusterManager.selectNodeForAgent(targetAgent);
            if (selectedNode instanceof LoadBalancedNode) {
                ((LoadBalancedNode) selectedNode).incrementLoad();
            }
        }
        
        // 验证负载均衡效果
        int totalLoad = node1.getLoad() + node2.getLoad() + node3.getLoad();
        double averageLoad = totalLoad / 3.0;
        double maxDeviation = Math.max(
            Math.abs(node1.getLoad() - averageLoad),
            Math.max(
                Math.abs(node2.getLoad() - averageLoad),
                Math.abs(node3.getLoad() - averageLoad)
            )
        ) / averageLoad;
        
        System.out.println("负载分布:");
        System.out.println("  - Node1: " + node1.getLoad() + " requests");
        System.out.println("  - Node2: " + node2.getLoad() + " requests");
        System.out.println("  - Node3: " + node3.getLoad() + " requests");
        System.out.println("  - 平均负载: " + String.format("%.2f", averageLoad));
        System.out.println("  - 最大偏差: " + String.format("%.2f%%", maxDeviation * 100));
        
        assert maxDeviation < 0.2 : "Load should be balanced (deviation < 20%)";
        
        System.out.println("✓ 负载均衡测试通过");
    }
    
    /**
     * 测试故障转移
     */
    public void testFailover() {
        System.out.println("=== 测试故障转移 ===");
        
        SimpleClusterManager clusterManager = new SimpleClusterManager();
        
        // 创建节点
        SimpleClusterNode node1 = new SimpleClusterNode("node-1", "192.168.1.101");
        SimpleClusterNode node2 = new SimpleClusterNode("node-2", "192.168.1.102");
        SimpleClusterNode node3 = new SimpleClusterNode("node-3", "192.168.1.103");
        
        clusterManager.registerNode(node1);
        clusterManager.registerNode(node2);
        clusterManager.registerNode(node3);
        
        // 标记节点2为故障
        node2.setHealthy(false);
        
        // 测试故障检测
        assert !clusterManager.isNodeHealthy("node-2") : "Node2 should be unhealthy";
        assert clusterManager.isNodeHealthy("node-1") : "Node1 should be healthy";
        assert clusterManager.isNodeHealthy("node-3") : "Node3 should be healthy";
        
        // 测试故障转移 - 应该跳过故障节点
        int healthySelections = 0;
        for (int i = 0; i < 100; i++) {
            AgentId targetAgent = AgentId.create("agent-" + i);
            SimpleClusterNode selectedNode = clusterManager.selectNodeForAgent(targetAgent);
            if (selectedNode != node2) {
                healthySelections++;
            }
        }
        
        assert healthySelections == 100 : "Should always select healthy nodes";
        System.out.println("故障转移测试: 100/100 选择健康节点");
        
        System.out.println("✓ 故障转移测试通过");
    }
    
    /**
     * 测试集群协调
     */
    public void testClusterCoordination() {
        System.out.println("=== 测试集群协调 ===");
        
        SimpleClusterManager clusterManager = new SimpleClusterManager();
        
        // 创建协调器节点
        CoordinatorNode coordinator = new CoordinatorNode("coordinator", "192.168.1.100");
        clusterManager.registerNode(coordinator);
        
        // 模拟选举过程
        String leader = clusterManager.electLeader();
        assert leader != null : "Should elect a leader";
        assert leader.equals("coordinator") : "Coordinator should be elected as leader";
        
        // 测试数据同步
        Map<String, String> testData = Map.of("key1", "value1", "key2", "value2");
        coordinator.syncData(testData);
        
        // 验证数据一致性
        Map<String, String> syncedData = coordinator.getSyncedData();
        assert syncedData.equals(testData) : "Data should be synchronized";
        
        System.out.println("集群协调测试:");
        System.out.println("  - 选举结果: " + leader);
        System.out.println("  - 数据同步: " + syncedData.size() + " items");
        
        System.out.println("✓ 集群协调测试通过");
    }
    
    /**
     * 运行所有集群测试
     */
    public static void main(String[] args) {
        ClusterTest test = new ClusterTest();
        
        try {
            test.testNodeManagement();
            test.testLoadBalancing();
            test.testFailover();
            test.testClusterCoordination();
            
            System.out.println("\n🎉 所有集群测试通过！");
        } catch (Exception e) {
            System.err.println("❌ 集群测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ===== 简单集群实现 =====
    
    /**
     * 简单集群节点
     */
    static class SimpleClusterNode {
        private final String nodeId;
        private final String address;
        private boolean healthy = true;
        private final Map<String, Object> metadata = new ConcurrentHashMap<>();
        
        public SimpleClusterNode(String nodeId, String address) {
            this.nodeId = nodeId;
            this.address = address;
        }
        
        public String getNodeId() {
            return nodeId;
        }
        
        public String getAddress() {
            return address;
        }
        
        public boolean isHealthy() {
            return healthy;
        }
        
        public void setHealthy(boolean healthy) {
            this.healthy = healthy;
        }
        
        public Map<String, Object> getMetadata() {
            return metadata;
        }
        
        public CompletableFuture<Void> sendMessage(AgentMessage message) {
            return CompletableFuture.runAsync(() -> {
                System.out.println("Node " + nodeId + " received message: " + message.messageId());
            });
        }
        
        public int getLoad() {
            return (Integer) metadata.getOrDefault("load", 0);
        }
    }
    
    /**
     * 负载均衡测试节点
     */
    static class LoadBalancedNode extends SimpleClusterNode {
        private final AtomicInteger load = new AtomicInteger(0);
        
        public LoadBalancedNode(String nodeId, String address) {
            super(nodeId, address);
        }
        
        @Override
        public int getLoad() {
            return load.get();
        }
        
        public void incrementLoad() {
            load.incrementAndGet();
        }
    }
    
    /**
     * 协调器节点
     */
    static class CoordinatorNode extends SimpleClusterNode {
        private Map<String, String> syncedData = new ConcurrentHashMap<>();
        
        public CoordinatorNode(String nodeId, String address) {
            super(nodeId, address);
        }
        
        public void syncData(Map<String, String> data) {
            syncedData.clear();
            syncedData.putAll(data);
        }
        
        public Map<String, String> getSyncedData() {
            return new HashMap<>(syncedData);
        }
    }
    
    /**
     * 简单集群管理器
     */
    static class SimpleClusterManager {
        private final Map<String, SimpleClusterNode> nodes = new ConcurrentHashMap<>();
        private final Random random = new Random();
        
        public void registerNode(SimpleClusterNode node) {
            nodes.put(node.getNodeId(), node);
            System.out.println("注册集群节点: " + node.getNodeId() + " @ " + node.getAddress());
        }
        
        public void unregisterNode(String nodeId) {
            nodes.remove(nodeId);
            System.out.println("注销集群节点: " + nodeId);
        }
        
        public boolean isNodeRegistered(String nodeId) {
            return nodes.containsKey(nodeId);
        }
        
        public boolean isNodeHealthy(String nodeId) {
            SimpleClusterNode node = nodes.get(nodeId);
            return node != null && node.isHealthy();
        }
        
        public Optional<SimpleClusterNode> findNode(String nodeId) {
            return Optional.ofNullable(nodes.get(nodeId));
        }
        
        public SimpleClusterNode selectNodeForAgent(AgentId agentId) {
            // 简单的轮询负载均衡
            List<SimpleClusterNode> healthyNodes = nodes.values().stream()
                .filter(SimpleClusterNode::isHealthy)
                .toList();
            
            if (healthyNodes.isEmpty()) {
                throw new IllegalStateException("No healthy nodes available");
            }
            
            int index = Math.abs(agentId.hashCode()) % healthyNodes.size();
            return healthyNodes.get(index);
        }
        
        public int getNodeCount() {
            return nodes.size();
        }
        
        public int getHealthyNodeCount() {
            return (int) nodes.values().stream()
                .filter(SimpleClusterNode::isHealthy)
                .count();
        }
        
        public String electLeader() {
            // 简单的领导者选举 - 选择第一个健康节点
            return nodes.values().stream()
                .filter(SimpleClusterNode::isHealthy)
                .findFirst()
                .map(SimpleClusterNode::getNodeId)
                .orElse(null);
        }
        
        public ClusterStats getStats() {
            int totalLoad = nodes.values().stream()
                .mapToInt(SimpleClusterNode::getLoad)
                .sum();
            
            double averageLoad = nodes.isEmpty() ? 0 : (double) totalLoad / nodes.size();
            
            return new ClusterStats(
                nodes.size(),
                getHealthyNodeCount(),
                totalLoad,
                averageLoad
            );
        }
    }
    
    /**
     * 集群统计信息
     */
    record ClusterStats(
        int totalNodes,
        int healthyNodes,
        int totalLoad,
        double averageLoad
    ) {
        @Override
        public String toString() {
            return String.format(
                "ClusterStats{nodes=%d/%d, load=%.2f}",
                healthyNodes, totalNodes, averageLoad
            );
        }
    }
}
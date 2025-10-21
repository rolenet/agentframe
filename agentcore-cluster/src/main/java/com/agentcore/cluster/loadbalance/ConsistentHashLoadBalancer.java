package com.agentcore.cluster.loadbalance;

import com.agentcore.cluster.node.ClusterNode;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一致性哈希负载均衡器
 * 
 * @author AgentCore Team
 */
public class ConsistentHashLoadBalancer implements LoadBalancer {

    private static final int VIRTUAL_NODES = 160; // 每个物理节点对应的虚拟节点数
    private final Map<String, TreeMap<Long, ClusterNode>> hashRings = new ConcurrentHashMap<>();

    @Override
    public Optional<ClusterNode> select(List<ClusterNode> availableNodes, String key) {
        if (availableNodes == null || availableNodes.isEmpty()) {
            return Optional.empty();
        }

        if (key == null || key.isEmpty()) {
            key = String.valueOf(System.currentTimeMillis());
        }

        // 构建或获取哈希环
        String ringKey = buildRingKey(availableNodes);
        TreeMap<Long, ClusterNode> hashRing = hashRings.computeIfAbsent(ringKey, 
            k -> buildHashRing(availableNodes));

        // 计算key的哈希值
        long hash = hash(key);

        // 在哈希环上找到第一个大于等于hash值的节点
        Map.Entry<Long, ClusterNode> entry = hashRing.ceilingEntry(hash);
        if (entry == null) {
            // 如果没有找到，选择环上的第一个节点
            entry = hashRing.firstEntry();
        }

        return entry != null ? Optional.of(entry.getValue()) : Optional.empty();
    }

    @Override
    public String getAlgorithmName() {
        return "ConsistentHash";
    }

    @Override
    public void reset() {
        hashRings.clear();
    }

    /**
     * 构建哈希环
     */
    private TreeMap<Long, ClusterNode> buildHashRing(List<ClusterNode> nodes) {
        TreeMap<Long, ClusterNode> hashRing = new TreeMap<>();

        for (ClusterNode node : nodes) {
            for (int i = 0; i < VIRTUAL_NODES; i++) {
                String virtualNodeKey = node.nodeId() + "#" + i;
                long hash = hash(virtualNodeKey);
                hashRing.put(hash, node);
            }
        }

        return hashRing;
    }

    /**
     * 构建哈希环的键
     */
    private String buildRingKey(List<ClusterNode> nodes) {
        return nodes.stream()
            .map(ClusterNode::nodeId)
            .sorted()
            .reduce("", (a, b) -> a + ":" + b);
    }

    /**
     * 计算字符串的哈希值
     */
    private long hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes());
            
            // 取前8个字节转换为long
            long hash = 0;
            for (int i = 0; i < 8; i++) {
                hash = (hash << 8) | (digest[i] & 0xFF);
            }
            
            return Math.abs(hash);
        } catch (NoSuchAlgorithmException e) {
            // 如果MD5不可用，使用简单的哈希算法
            return Math.abs(key.hashCode());
        }
    }
}
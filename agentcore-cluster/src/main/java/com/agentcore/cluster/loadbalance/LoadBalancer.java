package com.agentcore.cluster.loadbalance;

import com.agentcore.cluster.node.ClusterNode;

import java.util.List;
import java.util.Optional;

/**
 * 负载均衡器接口
 * 
 * @author AgentCore Team
 */
public interface LoadBalancer {

    /**
     * 从可用节点中选择一个节点
     * 
     * @param availableNodes 可用节点列表
     * @param key 选择键（可选，用于一致性哈希等算法）
     * @return 选中的节点，如果没有可用节点返回空
     */
    Optional<ClusterNode> select(List<ClusterNode> availableNodes, String key);

    /**
     * 从可用节点中选择一个节点
     * 
     * @param availableNodes 可用节点列表
     * @return 选中的节点，如果没有可用节点返回空
     */
    default Optional<ClusterNode> select(List<ClusterNode> availableNodes) {
        return select(availableNodes, null);
    }

    /**
     * 获取负载均衡算法名称
     * 
     * @return 算法名称
     */
    String getAlgorithmName();

    /**
     * 重置负载均衡器状态（如果需要）
     */
    default void reset() {
        // 默认实现为空
    }
}
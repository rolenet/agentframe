package com.agentcore.cluster;

import com.agentcore.cluster.node.ClusterNode;

/**
 * 集群事件监听器接口
 * 
 * @author AgentCore Team
 */
public interface ClusterEventListener {

    /**
     * 节点加入集群事件
     * 
     * @param node 加入的节点
     */
    void onNodeJoined(ClusterNode node);

    /**
     * 节点离开集群事件
     * 
     * @param node 离开的节点
     */
    void onNodeLeft(ClusterNode node);

    /**
     * 主节点变更事件
     * 
     * @param oldMaster 旧的主节点，可能为null
     * @param newMaster 新的主节点
     */
    void onMasterChanged(ClusterNode oldMaster, ClusterNode newMaster);

    /**
     * 集群状态变更事件
     * 
     * @param oldStats 旧的集群统计信息
     * @param newStats 新的集群统计信息
     */
    default void onClusterStatsChanged(ClusterStats oldStats, ClusterStats newStats) {
        // 默认实现为空
    }

    /**
     * 节点健康状态变更事件
     * 
     * @param node 节点
     * @param healthy 是否健康
     */
    default void onNodeHealthChanged(ClusterNode node, boolean healthy) {
        // 默认实现为空
    }
}
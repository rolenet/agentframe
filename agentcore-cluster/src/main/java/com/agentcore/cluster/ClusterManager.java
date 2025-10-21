package com.agentcore.cluster;

import com.agentcore.cluster.node.ClusterNode;
import com.agentcore.core.agent.AgentId;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * 集群管理器接口
 * 负责集群节点的管理和协调
 * 
 * @author AgentCore Team
 */
public interface ClusterManager {

    /**
     * 启动集群管理器
     * 
     * @return CompletableFuture，完成时表示启动成功
     */
    CompletableFuture<Void> start();

    /**
     * 停止集群管理器
     * 
     * @return CompletableFuture，完成时表示停止成功
     */
    CompletableFuture<Void> stop();

    /**
     * 加入集群
     * 
     * @param node 节点信息
     * @return CompletableFuture，完成时表示加入成功
     */
    CompletableFuture<Void> joinCluster(ClusterNode node);

    /**
     * 离开集群
     * 
     * @param nodeId 节点ID
     * @return CompletableFuture，完成时表示离开成功
     */
    CompletableFuture<Void> leaveCluster(String nodeId);

    /**
     * 获取当前节点信息
     * 
     * @return 当前节点信息
     */
    Optional<ClusterNode> getCurrentNode();

    /**
     * 获取所有集群节点
     * 
     * @return 节点列表
     */
    List<ClusterNode> getAllNodes();

    /**
     * 获取活跃节点
     * 
     * @return 活跃节点列表
     */
    List<ClusterNode> getActiveNodes();

    /**
     * 根据节点ID获取节点
     * 
     * @param nodeId 节点ID
     * @return 节点信息，如果不存在返回空
     */
    Optional<ClusterNode> getNode(String nodeId);

    /**
     * 根据节点类型获取节点
     * 
     * @param nodeType 节点类型
     * @return 节点列表
     */
    List<ClusterNode> getNodesByType(ClusterNode.NodeType nodeType);

    /**
     * 获取主节点
     * 
     * @return 主节点信息，如果不存在返回空
     */
    Optional<ClusterNode> getMasterNode();

    /**
     * 检查节点是否存在
     * 
     * @param nodeId 节点ID
     * @return 如果存在返回true
     */
    boolean containsNode(String nodeId);

    /**
     * 获取集群大小
     * 
     * @return 集群节点数量
     */
    int getClusterSize();

    /**
     * 检查是否为集群模式
     * 
     * @return 如果是集群模式返回true
     */
    boolean isClusterMode();

    /**
     * 检查当前节点是否为主节点
     * 
     * @return 如果是主节点返回true
     */
    boolean isMaster();

    /**
     * 选择最佳节点处理Agent
     * 
     * @param agentId Agent ID
     * @return 最佳节点，如果没有可用节点返回空
     */
    Optional<ClusterNode> selectNodeForAgent(AgentId agentId);

    /**
     * 根据负载均衡策略选择节点
     * 
     * @param strategy 负载均衡策略
     * @return 选中的节点，如果没有可用节点返回空
     */
    Optional<ClusterNode> selectNode(LoadBalanceStrategy strategy);

    /**
     * 广播消息到所有节点
     * 
     * @param message 消息内容
     * @return CompletableFuture，完成时表示广播完成
     */
    CompletableFuture<Void> broadcast(Object message);

    /**
     * 发送消息到指定节点
     * 
     * @param nodeId 目标节点ID
     * @param message 消息内容
     * @return CompletableFuture，完成时表示发送完成
     */
    CompletableFuture<Void> sendToNode(String nodeId, Object message);

    /**
     * 获取集群配置
     * 
     * @return 集群配置
     */
    ClusterConfig getConfig();

    /**
     * 获取集群统计信息
     * 
     * @return 集群统计信息
     */
    ClusterStats getStats();

    /**
     * 添加集群事件监听器
     * 
     * @param listener 事件监听器
     */
    void addClusterEventListener(ClusterEventListener listener);

    /**
     * 移除集群事件监听器
     * 
     * @param listener 事件监听器
     */
    void removeClusterEventListener(ClusterEventListener listener);

    /**
     * 负载均衡策略枚举
     */
    enum LoadBalanceStrategy {
        /**
         * 轮询策略
         */
        ROUND_ROBIN,
        
        /**
         * 随机策略
         */
        RANDOM,
        
        /**
         * 最少连接策略
         */
        LEAST_CONNECTIONS,
        
        /**
         * 最低负载策略
         */
        LEAST_LOAD,
        
        /**
         * 一致性哈希策略
         */
        CONSISTENT_HASH
    }

    /**
     * 集群事件监听器接口
     */
    interface ClusterEventListener {
        /**
         * 节点加入事件
         * 
         * @param node 加入的节点
         */
        default void onNodeJoined(ClusterNode node) {}

        /**
         * 节点离开事件
         * 
         * @param node 离开的节点
         */
        default void onNodeLeft(ClusterNode node) {}

        /**
         * 节点状态变更事件
         * 
         * @param node 状态变更的节点
         * @param oldStatus 旧状态
         * @param newStatus 新状态
         */
        default void onNodeStatusChanged(ClusterNode node, 
                ClusterNode.NodeStatus oldStatus, ClusterNode.NodeStatus newStatus) {}

        /**
         * 主节点变更事件
         * 
         * @param oldMaster 旧主节点
         * @param newMaster 新主节点
         */
        default void onMasterChanged(ClusterNode oldMaster, ClusterNode newMaster) {}

        /**
         * 集群分裂事件
         * 
         * @param lostNodes 丢失的节点
         */
        default void onClusterSplit(Set<ClusterNode> lostNodes) {}

        /**
         * 集群合并事件
         * 
         * @param mergedNodes 合并的节点
         */
        default void onClusterMerged(Set<ClusterNode> mergedNodes) {}
    }

}
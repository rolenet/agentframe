package com.agentcore.cluster.discovery;

import com.agentcore.cluster.node.ClusterNode;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 节点发现接口
 * 负责集群中节点的自动发现
 * 
 * @author AgentCore Team
 */
public interface NodeDiscovery {

    /**
     * 启动节点发现服务
     * 
     * @return CompletableFuture，完成时表示启动成功
     */
    CompletableFuture<Void> start();

    /**
     * 停止节点发现服务
     * 
     * @return CompletableFuture，完成时表示停止成功
     */
    CompletableFuture<Void> stop();

    /**
     * 注册当前节点
     * 
     * @param node 节点信息
     * @return CompletableFuture，完成时表示注册成功
     */
    CompletableFuture<Void> registerNode(ClusterNode node);

    /**
     * 注销当前节点
     * 
     * @param nodeId 节点ID
     * @return CompletableFuture，完成时表示注销成功
     */
    CompletableFuture<Void> unregisterNode(String nodeId);

    /**
     * 发现集群中的所有节点
     * 
     * @return 发现的节点列表
     */
    CompletableFuture<List<ClusterNode>> discoverNodes();

    /**
     * 根据节点类型发现节点
     * 
     * @param nodeType 节点类型
     * @return 发现的节点列表
     */
    CompletableFuture<List<ClusterNode>> discoverNodesByType(ClusterNode.NodeType nodeType);

    /**
     * 检查节点是否在线
     * 
     * @param nodeId 节点ID
     * @return 如果在线返回true
     */
    CompletableFuture<Boolean> isNodeOnline(String nodeId);

    /**
     * 更新节点信息
     * 
     * @param node 节点信息
     * @return CompletableFuture，完成时表示更新成功
     */
    CompletableFuture<Void> updateNode(ClusterNode node);

    /**
     * 添加节点发现监听器
     * 
     * @param listener 监听器
     */
    void addDiscoveryListener(DiscoveryListener listener);

    /**
     * 移除节点发现监听器
     * 
     * @param listener 监听器
     */
    void removeDiscoveryListener(DiscoveryListener listener);

    /**
     * 节点发现监听器接口
     */
    interface DiscoveryListener {
        /**
         * 发现新节点事件
         * 
         * @param node 发现的节点
         */
        default void onNodeDiscovered(ClusterNode node) {}

        /**
         * 节点丢失事件
         * 
         * @param node 丢失的节点
         */
        default void onNodeLost(ClusterNode node) {}

        /**
         * 节点更新事件
         * 
         * @param oldNode 旧节点信息
         * @param newNode 新节点信息
         */
        default void onNodeUpdated(ClusterNode oldNode, ClusterNode newNode) {}
    }
}
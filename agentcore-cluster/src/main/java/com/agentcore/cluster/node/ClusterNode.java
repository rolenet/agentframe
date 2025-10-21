package com.agentcore.cluster.node;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 集群节点信息
 * 
 * @author AgentCore Team
 */
public record ClusterNode(
    @JsonProperty("nodeId") String nodeId,
    @JsonProperty("nodeName") String nodeName,
    @JsonProperty("host") String host,
    @JsonProperty("port") int port,
    @JsonProperty("nodeType") NodeType nodeType,
    @JsonProperty("status") NodeStatus status,
    @JsonProperty("version") String version,
    @JsonProperty("capabilities") Map<String, Object> capabilities,
    @JsonProperty("metadata") Map<String, Object> metadata,
    @JsonProperty("joinTime") Instant joinTime,
    @JsonProperty("lastHeartbeat") Instant lastHeartbeat,
    @JsonProperty("load") NodeLoad load
) {

    /**
     * 创建ClusterNode
     */
    @JsonCreator
    public ClusterNode {
        // 验证必需字段
        Objects.requireNonNull(nodeId, "Node ID cannot be null");
        Objects.requireNonNull(nodeName, "Node name cannot be null");
        Objects.requireNonNull(host, "Host cannot be null");
        
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        
        // 设置默认值
        nodeType = nodeType != null ? nodeType : NodeType.WORKER;
        status = status != null ? status : NodeStatus.JOINING;
        version = version != null ? version : "1.0.0";
        joinTime = joinTime != null ? joinTime : Instant.now();
        lastHeartbeat = lastHeartbeat != null ? lastHeartbeat : Instant.now();
        load = load != null ? load : NodeLoad.empty();
        
        // 确保集合不可变
        capabilities = capabilities != null ? 
            Collections.unmodifiableMap(new HashMap<>(capabilities)) : 
            Collections.emptyMap();
        metadata = metadata != null ? 
            Collections.unmodifiableMap(new HashMap<>(metadata)) : 
            Collections.emptyMap();
    }

    /**
     * 创建节点构建器
     * 
     * @param nodeId 节点ID
     * @param nodeName 节点名称
     * @param host 主机地址
     * @param port 端口
     * @return NodeBuilder实例
     */
    public static NodeBuilder builder(String nodeId, String nodeName, String host, int port) {
        return new NodeBuilder(nodeId, nodeName, host, port);
    }

    /**
     * 获取节点地址
     * 
     * @return 节点地址
     */
    public String getAddress() {
        return host + ":" + port;
    }

    /**
     * 检查节点是否活跃
     * 
     * @return 如果节点活跃返回true
     */
    public boolean isActive() {
        return status == NodeStatus.ACTIVE;
    }

    /**
     * 检查节点是否可用
     * 
     * @return 如果节点可用返回true
     */
    public boolean isAvailable() {
        return status == NodeStatus.ACTIVE || status == NodeStatus.BUSY;
    }

    /**
     * 检查节点是否为主节点
     * 
     * @return 如果是主节点返回true
     */
    public boolean isMaster() {
        return nodeType == NodeType.MASTER;
    }

    /**
     * 获取能力值
     * 
     * @param key 能力键
     * @return 能力值
     */
    public Object getCapability(String key) {
        return capabilities.get(key);
    }

    /**
     * 获取能力值（带默认值）
     * 
     * @param key 能力键
     * @param defaultValue 默认值
     * @param <T> 值类型
     * @return 能力值或默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T getCapability(String key, T defaultValue) {
        Object value = capabilities.get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * 获取元数据值
     * 
     * @param key 元数据键
     * @return 元数据值
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    /**
     * 获取元数据值（带默认值）
     * 
     * @param key 元数据键
     * @param defaultValue 默认值
     * @param <T> 值类型
     * @return 元数据值或默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key, T defaultValue) {
        Object value = metadata.get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * 更新心跳时间
     * 
     * @return 新的ClusterNode实例
     */
    public ClusterNode updateHeartbeat() {
        return new ClusterNode(
            nodeId, nodeName, host, port, nodeType, status, version,
            capabilities, metadata, joinTime, Instant.now(), load
        );
    }

    /**
     * 更新节点状态
     * 
     * @param newStatus 新状态
     * @return 新的ClusterNode实例
     */
    public ClusterNode updateStatus(NodeStatus newStatus) {
        return new ClusterNode(
            nodeId, nodeName, host, port, nodeType, newStatus, version,
            capabilities, metadata, joinTime, lastHeartbeat, load
        );
    }

    /**
     * 更新节点负载
     * 
     * @param newLoad 新负载
     * @return 新的ClusterNode实例
     */
    public ClusterNode updateLoad(NodeLoad newLoad) {
        return new ClusterNode(
            nodeId, nodeName, host, port, nodeType, status, version,
            capabilities, metadata, joinTime, lastHeartbeat, newLoad
        );
    }

    /**
     * 节点类型枚举
     */
    public enum NodeType {
        /**
         * 主节点 - 负责集群协调和管理
         */
        MASTER,
        
        /**
         * 工作节点 - 执行具体任务
         */
        WORKER,
        
        /**
         * 代理节点 - 提供代理服务
         */
        PROXY,
        
        /**
         * 监控节点 - 负责监控和日志收集
         */
        MONITOR
    }

    /**
     * 节点状态枚举
     */
    public enum NodeStatus {
        /**
         * 加入中 - 节点正在加入集群
         */
        JOINING,
        
        /**
         * 活跃 - 节点正常运行
         */
        ACTIVE,
        
        /**
         * 忙碌 - 节点负载较高
         */
        BUSY,
        
        /**
         * 暂停 - 节点暂时不可用
         */
        SUSPENDED,
        
        /**
         * 离开中 - 节点正在离开集群
         */
        LEAVING,
        
        /**
         * 错误 - 节点出现错误
         */
        ERROR,
        
        /**
         * 离线 - 节点已离线
         */
        OFFLINE
    }

    /**
     * 节点负载信息
     */
    public record NodeLoad(
        double cpuUsage,
        double memoryUsage,
        double diskUsage,
        int activeConnections,
        int agentCount,
        long messagesPerSecond
    ) {
        
        /**
         * 创建空负载
         * 
         * @return 空负载实例
         */
        public static NodeLoad empty() {
            return new NodeLoad(0.0, 0.0, 0.0, 0, 0, 0);
        }

        /**
         * 计算总体负载分数（0-1之间）
         * 
         * @return 负载分数
         */
        public double getLoadScore() {
            return (cpuUsage + memoryUsage + diskUsage) / 3.0;
        }

        /**
         * 检查是否高负载
         * 
         * @return 如果高负载返回true
         */
        public boolean isHighLoad() {
            return getLoadScore() > 0.8;
        }

        /**
         * 检查是否低负载
         * 
         * @return 如果低负载返回true
         */
        public boolean isLowLoad() {
            return getLoadScore() < 0.3;
        }
    }

    /**
     * 节点构建器
     */
    public static class NodeBuilder {
        private final String nodeId;
        private final String nodeName;
        private final String host;
        private final int port;
        private NodeType nodeType;
        private NodeStatus status;
        private String version;
        private Map<String, Object> capabilities = new HashMap<>();
        private Map<String, Object> metadata = new HashMap<>();
        private Instant joinTime;
        private Instant lastHeartbeat;
        private NodeLoad load;

        private NodeBuilder(String nodeId, String nodeName, String host, int port) {
            this.nodeId = nodeId;
            this.nodeName = nodeName;
            this.host = host;
            this.port = port;
        }

        public NodeBuilder nodeType(NodeType nodeType) {
            this.nodeType = nodeType;
            return this;
        }

        public NodeBuilder status(NodeStatus status) {
            this.status = status;
            return this;
        }

        public NodeBuilder version(String version) {
            this.version = version;
            return this;
        }

        public NodeBuilder capability(String key, Object value) {
            this.capabilities.put(key, value);
            return this;
        }

        public NodeBuilder capabilities(Map<String, Object> capabilities) {
            this.capabilities.putAll(capabilities);
            return this;
        }

        public NodeBuilder metadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public NodeBuilder metadata(Map<String, Object> metadata) {
            this.metadata.putAll(metadata);
            return this;
        }

        public NodeBuilder joinTime(Instant joinTime) {
            this.joinTime = joinTime;
            return this;
        }

        public NodeBuilder lastHeartbeat(Instant lastHeartbeat) {
            this.lastHeartbeat = lastHeartbeat;
            return this;
        }

        public NodeBuilder load(NodeLoad load) {
            this.load = load;
            return this;
        }

        public ClusterNode build() {
            return new ClusterNode(
                nodeId, nodeName, host, port, nodeType, status, version,
                capabilities, metadata, joinTime, lastHeartbeat, load
            );
        }
    }

    @Override
    public String toString() {
        return String.format(
            "ClusterNode{nodeId='%s', nodeName='%s', address='%s', type=%s, status=%s}",
            nodeId, nodeName, getAddress(), nodeType, status
        );
    }
}
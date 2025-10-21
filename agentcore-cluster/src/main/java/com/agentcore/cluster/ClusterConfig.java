package com.agentcore.cluster;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 集群配置
 * 
 * @author AgentCore Team
 */
public record ClusterConfig(
    String clusterName,
    String nodeId,
    String nodeName,
    String bindHost,
    int bindPort,
    List<String> seedNodes,
    Duration heartbeatInterval,
    Duration nodeTimeout,
    Duration electionTimeout,
    int maxRetries,
    boolean enableAutoDiscovery,
    ClusterManager.LoadBalanceStrategy defaultLoadBalanceStrategy,
    Map<String, Object> properties
) {

    /**
     * 创建ClusterConfig
     */
    public ClusterConfig {
        // 设置默认值
        clusterName = clusterName != null ? clusterName : "AgentCoreCluster";
        nodeId = nodeId != null ? nodeId : generateNodeId();
        nodeName = nodeName != null ? nodeName : "Node-" + nodeId;
        bindHost = bindHost != null ? bindHost : "localhost";
        bindPort = bindPort > 0 ? bindPort : 8080;
        seedNodes = seedNodes != null ? List.copyOf(seedNodes) : Collections.emptyList();
        heartbeatInterval = heartbeatInterval != null ? heartbeatInterval : Duration.ofSeconds(5);
        nodeTimeout = nodeTimeout != null ? nodeTimeout : Duration.ofSeconds(30);
        electionTimeout = electionTimeout != null ? electionTimeout : Duration.ofSeconds(10);
        maxRetries = maxRetries > 0 ? maxRetries : 3;
        defaultLoadBalanceStrategy = defaultLoadBalanceStrategy != null ? 
            defaultLoadBalanceStrategy : ClusterManager.LoadBalanceStrategy.LEAST_LOAD;
        
        // 确保properties不可变
        properties = properties != null ? 
            Collections.unmodifiableMap(new HashMap<>(properties)) : 
            Collections.emptyMap();
    }

    /**
     * 创建配置构建器
     * 
     * @param clusterName 集群名称
     * @return ConfigBuilder实例
     */
    public static ConfigBuilder builder(String clusterName) {
        return new ConfigBuilder(clusterName);
    }

    /**
     * 创建默认配置
     * 
     * @param clusterName 集群名称
     * @return ClusterConfig实例
     */
    public static ClusterConfig defaultConfig(String clusterName) {
        return builder(clusterName).build();
    }

    /**
     * 获取节点地址
     * 
     * @return 节点地址
     */
    public String getNodeAddress() {
        return bindHost + ":" + bindPort;
    }

    /**
     * 检查是否启用集群模式
     * 
     * @return 如果启用集群模式返回true
     */
    public boolean isClusterEnabled() {
        return !seedNodes.isEmpty() || enableAutoDiscovery;
    }

    /**
     * 获取属性值
     * 
     * @param key 属性键
     * @return 属性值
     */
    public Object getProperty(String key) {
        return properties.get(key);
    }

    /**
     * 获取属性值（带默认值）
     * 
     * @param key 属性键
     * @param defaultValue 默认值
     * @param <T> 值类型
     * @return 属性值或默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key, T defaultValue) {
        Object value = properties.get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * 生成节点ID
     * 
     * @return 节点ID
     */
    private static String generateNodeId() {
        return "node-" + System.currentTimeMillis() + "-" + 
               (int)(Math.random() * 1000);
    }

    /**
     * 配置构建器
     */
    public static class ConfigBuilder {
        private final String clusterName;
        private String nodeId;
        private String nodeName;
        private String bindHost;
        private int bindPort;
        private List<String> seedNodes;
        private Duration heartbeatInterval;
        private Duration nodeTimeout;
        private Duration electionTimeout;
        private int maxRetries;
        private boolean enableAutoDiscovery = false;
        private ClusterManager.LoadBalanceStrategy defaultLoadBalanceStrategy;
        private Map<String, Object> properties = new HashMap<>();

        private ConfigBuilder(String clusterName) {
            this.clusterName = clusterName;
        }

        public ConfigBuilder nodeId(String nodeId) {
            this.nodeId = nodeId;
            return this;
        }

        public ConfigBuilder nodeName(String nodeName) {
            this.nodeName = nodeName;
            return this;
        }

        public ConfigBuilder bindHost(String bindHost) {
            this.bindHost = bindHost;
            return this;
        }

        public ConfigBuilder bindPort(int bindPort) {
            this.bindPort = bindPort;
            return this;
        }

        public ConfigBuilder seedNodes(List<String> seedNodes) {
            this.seedNodes = seedNodes;
            return this;
        }

        public ConfigBuilder heartbeatInterval(Duration heartbeatInterval) {
            this.heartbeatInterval = heartbeatInterval;
            return this;
        }

        public ConfigBuilder nodeTimeout(Duration nodeTimeout) {
            this.nodeTimeout = nodeTimeout;
            return this;
        }

        public ConfigBuilder electionTimeout(Duration electionTimeout) {
            this.electionTimeout = electionTimeout;
            return this;
        }

        public ConfigBuilder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public ConfigBuilder enableAutoDiscovery(boolean enableAutoDiscovery) {
            this.enableAutoDiscovery = enableAutoDiscovery;
            return this;
        }

        public ConfigBuilder defaultLoadBalanceStrategy(ClusterManager.LoadBalanceStrategy strategy) {
            this.defaultLoadBalanceStrategy = strategy;
            return this;
        }

        public ConfigBuilder property(String key, Object value) {
            this.properties.put(key, value);
            return this;
        }

        public ConfigBuilder properties(Map<String, Object> properties) {
            this.properties.putAll(properties);
            return this;
        }

        public ClusterConfig build() {
            return new ClusterConfig(
                clusterName, nodeId, nodeName, bindHost, bindPort, seedNodes,
                heartbeatInterval, nodeTimeout, electionTimeout, maxRetries,
                enableAutoDiscovery, defaultLoadBalanceStrategy, properties
            );
        }
    }
}
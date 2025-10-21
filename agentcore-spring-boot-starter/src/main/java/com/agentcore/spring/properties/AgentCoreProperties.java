package com.agentcore.spring.properties;

import com.agentcore.cluster.ClusterConfig;
import com.agentcore.cluster.health.ClusterHealthChecker;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * AgentCore配置属性
 * 
 * @author AgentCore Team
 */
@ConfigurationProperties(prefix = "agentcore")
public class AgentCoreProperties {

    /**
     * 是否启用AgentCore
     */
    private boolean enabled = true;

    /**
     * 通信配置
     */
    private Communication communication = new Communication();

    /**
     * 容器配置
     */
    private Container container = new Container();

    /**
     * 集群配置
     */
    private Cluster cluster = new Cluster();

    /**
     * 监控配置
     */
    private Monitoring monitoring = new Monitoring();

    /**
     * 管理配置
     */
    private Management management = new Management();

    /**
     * 健康检查配置
     */
    private Health health = new Health();

    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Communication getCommunication() {
        return communication;
    }

    public void setCommunication(Communication communication) {
        this.communication = communication;
    }

    public Container getContainer() {
        return container;
    }

    public void setContainer(Container container) {
        this.container = container;
    }

    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public Monitoring getMonitoring() {
        return monitoring;
    }

    public void setMonitoring(Monitoring monitoring) {
        this.monitoring = monitoring;
    }

    public Management getManagement() {
        return management;
    }

    public void setManagement(Management management) {
        this.management = management;
    }

    public Health getHealth() {
        return health;
    }

    public void setHealth(Health health) {
        this.health = health;
    }

    /**
     * 通信配置
     */
    public static class Communication {
        /**
         * 序列化器类型：jackson, kryo
         */
        private String serializer = "jackson";

        /**
         * 绑定主机
         */
        private String host = "localhost";

        /**
         * 绑定端口
         */
        private int port = 8080;

        /**
         * 连接超时时间
         */
        private Duration connectTimeout = Duration.ofSeconds(30);

        /**
         * 读取超时时间
         */
        private Duration readTimeout = Duration.ofSeconds(60);

        // Getters and Setters
        public String getSerializer() {
            return serializer;
        }

        public void setSerializer(String serializer) {
            this.serializer = serializer;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
        }
    }

    /**
     * 容器配置
     */
    public static class Container {
        /**
         * 容器名称
         */
        private String name = "default-container";

        /**
         * 最大Agent数量
         */
        private int maxAgents = 1000;

        /**
         * Agent启动超时时间
         */
        private Duration startupTimeout = Duration.ofSeconds(30);

        // Getters and Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getMaxAgents() {
            return maxAgents;
        }

        public void setMaxAgents(int maxAgents) {
            this.maxAgents = maxAgents;
        }

        public Duration getStartupTimeout() {
            return startupTimeout;
        }

        public void setStartupTimeout(Duration startupTimeout) {
            this.startupTimeout = startupTimeout;
        }
    }

    /**
     * 集群配置
     */
    public static class Cluster {
        /**
         * 是否启用集群
         */
        private boolean enabled = false;

        /**
         * 节点ID
         */
        private String nodeId;

        /**
         * 节点名称
         */
        private String nodeName;

        /**
         * 绑定主机
         */
        private String bindHost = "localhost";

        /**
         * 绑定端口
         */
        private int bindPort = 9090;

        /**
         * 节点发现配置
         */
        private Discovery discovery = new Discovery();

        /**
         * 健康检查配置
         */
        private ClusterHealth health = new ClusterHealth();

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getNodeId() {
            return nodeId;
        }

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }

        public String getNodeName() {
            return nodeName;
        }

        public void setNodeName(String nodeName) {
            this.nodeName = nodeName;
        }

        public String getBindHost() {
            return bindHost;
        }

        public void setBindHost(String bindHost) {
            this.bindHost = bindHost;
        }

        public int getBindPort() {
            return bindPort;
        }

        public void setBindPort(int bindPort) {
            this.bindPort = bindPort;
        }

        public Discovery getDiscovery() {
            return discovery;
        }

        public void setDiscovery(Discovery discovery) {
            this.discovery = discovery;
        }

        public ClusterHealth getHealth() {
            return health;
        }

        public void setHealth(ClusterHealth health) {
            this.health = health;
        }

        /**
         * 转换为ClusterConfig
         */
        public ClusterConfig toClusterConfig() {
            return ClusterConfig.builder("AgentCoreCluster")
                .nodeId(nodeId != null ? nodeId : java.util.UUID.randomUUID().toString())
                .nodeName(nodeName != null ? nodeName : "node-" + System.currentTimeMillis())
                .bindHost(bindHost)
                .bindPort(bindPort)
                .enableAutoDiscovery(enabled)
                .build();
        }

        /**
         * 节点发现配置
         */
        public static class Discovery {
            /**
             * 发现类型：multicast, redis
             */
            private String type = "multicast";

            /**
             * 多播组地址
             */
            private String multicastGroup = "224.0.0.1";

            /**
             * 多播端口
             */
            private int multicastPort = 9091;

            /**
             * 发现间隔
             */
            private Duration interval = Duration.ofSeconds(30);

            // Getters and Setters
            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public String getMulticastGroup() {
                return multicastGroup;
            }

            public void setMulticastGroup(String multicastGroup) {
                this.multicastGroup = multicastGroup;
            }

            public int getMulticastPort() {
                return multicastPort;
            }

            public void setMulticastPort(int multicastPort) {
                this.multicastPort = multicastPort;
            }

            public Duration getInterval() {
                return interval;
            }

            public void setInterval(Duration interval) {
                this.interval = interval;
            }
        }

        /**
         * 集群健康检查配置
         */
        public static class ClusterHealth {
            /**
             * 是否启用健康检查
             */
            private boolean enabled = true;

            /**
             * 检查间隔
             */
            private Duration checkInterval = Duration.ofSeconds(30);

            /**
             * 最小节点可用性
             */
            private double minNodeAvailability = 0.5;

            /**
             * 最大平均负载
             */
            private double maxAverageLoad = 0.8;

            /**
             * 最大节点负载
             */
            private double maxNodeLoad = 0.9;

            /**
             * 最大内存使用率
             */
            private double maxMemoryUsage = 0.85;

            /**
             * 最大CPU使用率
             */
            private double maxCpuUsage = 0.85;

            /**
             * 最大每秒消息数
             */
            private long maxMessagesPerSecond = 10000;

            // Getters and Setters
            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public Duration getCheckInterval() {
                return checkInterval;
            }

            public void setCheckInterval(Duration checkInterval) {
                this.checkInterval = checkInterval;
            }

            public double getMinNodeAvailability() {
                return minNodeAvailability;
            }

            public void setMinNodeAvailability(double minNodeAvailability) {
                this.minNodeAvailability = minNodeAvailability;
            }

            public double getMaxAverageLoad() {
                return maxAverageLoad;
            }

            public void setMaxAverageLoad(double maxAverageLoad) {
                this.maxAverageLoad = maxAverageLoad;
            }

            public double getMaxNodeLoad() {
                return maxNodeLoad;
            }

            public void setMaxNodeLoad(double maxNodeLoad) {
                this.maxNodeLoad = maxNodeLoad;
            }

            public double getMaxMemoryUsage() {
                return maxMemoryUsage;
            }

            public void setMaxMemoryUsage(double maxMemoryUsage) {
                this.maxMemoryUsage = maxMemoryUsage;
            }

            public double getMaxCpuUsage() {
                return maxCpuUsage;
            }

            public void setMaxCpuUsage(double maxCpuUsage) {
                this.maxCpuUsage = maxCpuUsage;
            }

            public long getMaxMessagesPerSecond() {
                return maxMessagesPerSecond;
            }

            public void setMaxMessagesPerSecond(long maxMessagesPerSecond) {
                this.maxMessagesPerSecond = maxMessagesPerSecond;
            }

            /**
             * 转换为HealthCheckConfig
             */
            public ClusterHealthChecker.HealthCheckConfig toHealthCheckConfig() {
                return new ClusterHealthChecker.HealthCheckConfig(
                    minNodeAvailability,
                    maxAverageLoad,
                    maxNodeLoad,
                    maxMemoryUsage,
                    maxCpuUsage,
                    maxMessagesPerSecond
                );
            }
        }
    }

    /**
     * 监控配置
     */
    public static class Monitoring {
        /**
         * 是否启用监控
         */
        private boolean enabled = true;

        /**
         * 指标收集间隔
         */
        private Duration interval = Duration.ofSeconds(60);

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getInterval() {
            return interval;
        }

        public void setInterval(Duration interval) {
            this.interval = interval;
        }
    }

    /**
     * 管理配置
     */
    public static class Management {
        /**
         * 是否启用管理端点
         */
        private boolean enabled = true;

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * 健康检查配置
     */
    public static class Health {
        /**
         * 是否启用健康检查
         */
        private boolean enabled = true;

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
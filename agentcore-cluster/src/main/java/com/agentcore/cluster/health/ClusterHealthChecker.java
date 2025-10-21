package com.agentcore.cluster.health;

import com.agentcore.cluster.ClusterManager;
import com.agentcore.cluster.ClusterStats;
import com.agentcore.cluster.node.ClusterNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 集群健康检查器
 * 
 * @author AgentCore Team
 */
public class ClusterHealthChecker {

    private static final Logger logger = LoggerFactory.getLogger(ClusterHealthChecker.class);

    private final ClusterManager clusterManager;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    private final Duration checkInterval;
    private final HealthCheckConfig config;

    /**
     * 构造函数
     * 
     * @param clusterManager 集群管理器
     */
    public ClusterHealthChecker(ClusterManager clusterManager) {
        this(clusterManager, Duration.ofSeconds(30), HealthCheckConfig.defaultConfig());
    }

    /**
     * 构造函数
     * 
     * @param clusterManager 集群管理器
     * @param checkInterval 检查间隔
     * @param config 健康检查配置
     */
    public ClusterHealthChecker(ClusterManager clusterManager, 
                               Duration checkInterval, 
                               HealthCheckConfig config) {
        this.clusterManager = clusterManager;
        this.checkInterval = checkInterval;
        this.config = config;
    }

    /**
     * 启动健康检查
     * 
     * @return 启动结果
     */
    public CompletableFuture<Void> start() {
        if (!running.compareAndSet(false, true)) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Health checker is already running"));
        }

        return CompletableFuture.runAsync(() -> {
            scheduler.scheduleWithFixedDelay(
                this::performHealthCheck,
                0,
                checkInterval.toSeconds(),
                TimeUnit.SECONDS
            );
            
            logger.info("Cluster health checker started with interval: {}", checkInterval);
        });
    }

    /**
     * 停止健康检查
     * 
     * @return 停止结果
     */
    public CompletableFuture<Void> stop() {
        if (!running.compareAndSet(true, false)) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
                logger.info("Cluster health checker stopped");
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        });
    }

    /**
     * 执行健康检查
     */
    private void performHealthCheck() {
        try {
            com.agentcore.cluster.ClusterStats stats = clusterManager.getStats();
            List<ClusterNode> nodes = clusterManager.getAllNodes();
            
            // 检查集群整体健康状态
            HealthStatus clusterHealth = checkClusterHealth(stats);
            
            // 检查各个节点健康状态
            for (ClusterNode node : nodes) {
                HealthStatus nodeHealth = checkNodeHealth(node);
                if (nodeHealth.severity().getLevel() >= HealthSeverity.WARNING.getLevel()) {
                    logger.warn("Node {} health issue: {}", node.nodeId(), nodeHealth.message());
                }
            }
            
            // 记录集群健康状态
            if (clusterHealth.severity().getLevel() >= HealthSeverity.WARNING.getLevel()) {
                logger.warn("Cluster health issue: {}", clusterHealth.message());
            } else {
                logger.debug("Cluster health check passed: {}", clusterHealth.message());
            }
            
        } catch (Exception e) {
            logger.error("Error during cluster health check", e);
        }
    }

    /**
     * 检查集群健康状态
     * 
     * @param stats 集群统计信息
     * @return 健康状态
     */
    private HealthStatus checkClusterHealth(com.agentcore.cluster.ClusterStats stats) {
        // 检查节点可用性
        double availability = stats.getNodeAvailability();
        if (availability < config.minNodeAvailability()) {
            return new HealthStatus(
                HealthSeverity.CRITICAL,
                String.format("Low node availability: %.2f%% (min: %.2f%%)", 
                    availability * 100, config.minNodeAvailability() * 100)
            );
        }
        
        // 检查平均负载
        if (stats.averageLoad() > config.maxAverageLoad()) {
            return new HealthStatus(
                HealthSeverity.WARNING,
                String.format("High average load: %.2f (max: %.2f)", 
                    stats.averageLoad(), config.maxAverageLoad())
            );
        }
        
        // 检查消息处理能力
        if (stats.messagesPerSecond() > config.maxMessagesPerSecond()) {
            return new HealthStatus(
                HealthSeverity.WARNING,
                String.format("High message rate: %d/s (max: %d/s)", 
                    stats.messagesPerSecond(), config.maxMessagesPerSecond())
            );
        }
        
        // 检查是否有主节点
        if (stats.masterNodes() == 0) {
            return new HealthStatus(
                HealthSeverity.CRITICAL,
                "No master node available"
            );
        }
        
        // 检查主节点数量
        if (stats.masterNodes() > 1) {
            return new HealthStatus(
                HealthSeverity.WARNING,
                String.format("Multiple master nodes detected: %d", stats.masterNodes())
            );
        }
        
        return new HealthStatus(HealthSeverity.HEALTHY, "Cluster is healthy");
    }

    /**
     * 检查节点健康状态
     * 
     * @param node 集群节点
     * @return 健康状态
     */
    private HealthStatus checkNodeHealth(ClusterNode node) {
        // 检查节点是否活跃
        if (!node.isActive()) {
            return new HealthStatus(
                HealthSeverity.CRITICAL,
                "Node is not active"
            );
        }
        
        // 检查节点负载
        double nodeLoad = node.load().getLoadScore();
        if (nodeLoad > config.maxNodeLoad()) {
            return new HealthStatus(
                HealthSeverity.WARNING,
                String.format("High node load: %.2f (max: %.2f)", 
                    nodeLoad, config.maxNodeLoad())
            );
        }
        
        // 检查内存使用率
        double memoryUsage = node.load().memoryUsage();
        if (memoryUsage > config.maxMemoryUsage()) {
            return new HealthStatus(
                HealthSeverity.WARNING,
                String.format("High memory usage: %.2f%% (max: %.2f%%)", 
                    memoryUsage * 100, config.maxMemoryUsage() * 100)
            );
        }
        
        // 检查CPU使用率
        double cpuUsage = node.load().cpuUsage();
        if (cpuUsage > config.maxCpuUsage()) {
            return new HealthStatus(
                HealthSeverity.WARNING,
                String.format("High CPU usage: %.2f%% (max: %.2f%%)", 
                    cpuUsage * 100, config.maxCpuUsage() * 100)
            );
        }
        
        return new HealthStatus(HealthSeverity.HEALTHY, "Node is healthy");
    }

    /**
     * 健康状态记录
     */
    public record HealthStatus(HealthSeverity severity, String message) {}

    /**
     * 健康严重程度枚举
     */
    public enum HealthSeverity {
        HEALTHY(0),
        INFO(1),
        WARNING(2),
        ERROR(3),
        CRITICAL(4);

        private final int level;

        HealthSeverity(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }
    }

    /**
     * 健康检查配置
     */
    public record HealthCheckConfig(
        double minNodeAvailability,
        double maxAverageLoad,
        double maxNodeLoad,
        double maxMemoryUsage,
        double maxCpuUsage,
        long maxMessagesPerSecond
    ) {
        
        /**
         * 默认配置
         * 
         * @return 默认健康检查配置
         */
        public static HealthCheckConfig defaultConfig() {
            return new HealthCheckConfig(
                0.5,    // 最小节点可用性 50%
                0.8,    // 最大平均负载 80%
                0.9,    // 最大节点负载 90%
                0.85,   // 最大内存使用率 85%
                0.85,   // 最大CPU使用率 85%
                10000   // 最大每秒消息数 10000
            );
        }
    }
}
package com.agentcore.cluster;

/**
 * 集群统计信息
 * 
 * @param totalNodes 总节点数
 * @param activeNodes 活跃节点数
 * @param masterNodes 主节点数
 * @param workerNodes 工作节点数
 * @param averageLoad 平均负载
 * @param totalAgents 总智能体数量
 * @param messagesPerSecond 每秒消息数
 * @param healthy 集群是否健康
 * 
 * @author AgentCore Team
 */
public record ClusterStats(
    int totalNodes,
    int activeNodes,
    int masterNodes,
    int workerNodes,
    double averageLoad,
    long totalAgents,
    long messagesPerSecond,
    boolean healthy
) {

    /**
     * 获取节点可用率
     * 
     * @return 节点可用率（0.0-1.0）
     */
    public double getNodeAvailability() {
        return totalNodes > 0 ? (double) activeNodes / totalNodes : 0.0;
    }

    /**
     * 获取负载等级
     * 
     * @return 负载等级描述
     */
    public String getLoadLevel() {
        if (averageLoad < 0.3) {
            return "LOW";
        } else if (averageLoad < 0.7) {
            return "MEDIUM";
        } else if (averageLoad < 0.9) {
            return "HIGH";
        } else {
            return "CRITICAL";
        }
    }

    /**
     * 获取集群健康状态描述
     * 
     * @return 健康状态描述
     */
    public String getHealthStatus() {
        if (!healthy) {
            return "UNHEALTHY";
        }
        
        double availability = getNodeAvailability();
        if (availability >= 0.9) {
            return "EXCELLENT";
        } else if (availability >= 0.7) {
            return "GOOD";
        } else if (availability >= 0.5) {
            return "FAIR";
        } else {
            return "POOR";
        }
    }

    /**
     * 创建空的集群统计信息
     * 
     * @return 空的统计信息
     */
    public static ClusterStats empty() {
        return new ClusterStats(0, 0, 0, 0, 0.0, 0, 0, false);
    }

    @Override
    public String toString() {
        return String.format(
            "ClusterStats{nodes=%d/%d, load=%.2f(%s), agents=%d, msgs/s=%d, health=%s}",
            activeNodes, totalNodes, averageLoad, getLoadLevel(), 
            totalAgents, messagesPerSecond, getHealthStatus()
        );
    }
}
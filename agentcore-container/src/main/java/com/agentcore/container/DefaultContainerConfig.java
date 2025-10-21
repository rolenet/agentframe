package com.agentcore.container;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 默认容器配置工厂类
 * 
 * @author AgentCore Team
 */
public class DefaultContainerConfig {

    /**
     * 创建默认容器配置
     * 
     * @param name 容器名称
     * @return ContainerConfig实例
     */
    public static ContainerConfig create(String name) {
        return new ContainerConfig(
            name,
            1000,
            Duration.ofSeconds(30),
            Duration.ofSeconds(30),
            false,
            true,
            System.getProperty("user.dir"),
            new HashMap<>()
        );
    }

    /**
     * 创建容器配置
     * 
     * @param name 容器名称
     * @param maxAgents 最大Agent数量
     * @param agentStartTimeout Agent启动超时时间
     * @param agentStopTimeout Agent停止超时时间
     * @param autoStartAgents 是否自动启动Agent
     * @param enableMonitoring 是否启用监控
     * @param workingDirectory 工作目录
     * @param properties 配置属性
     * @return ContainerConfig实例
     */
    public static ContainerConfig create(String name, int maxAgents, Duration agentStartTimeout, 
                                       Duration agentStopTimeout, boolean autoStartAgents, 
                                       boolean enableMonitoring, String workingDirectory, 
                                       Map<String, Object> properties) {
        return new ContainerConfig(
            name,
            maxAgents,
            agentStartTimeout,
            agentStopTimeout,
            autoStartAgents,
            enableMonitoring,
            workingDirectory,
            properties
        );
    }
}
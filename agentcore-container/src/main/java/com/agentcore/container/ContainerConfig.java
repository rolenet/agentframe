package com.agentcore.container;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 容器配置
 * 
 * @author AgentCore Team
 */
public record ContainerConfig(
    String name,
    int maxAgents,
    Duration agentStartTimeout,
    Duration agentStopTimeout,
    boolean autoStartAgents,
    boolean enableMonitoring,
    String workingDirectory,
    Map<String, Object> properties
) {

    /**
     * 创建ContainerConfig
     */
    public ContainerConfig {
        // 设置默认值
        name = name != null ? name : "DefaultContainer";
        maxAgents = maxAgents > 0 ? maxAgents : 1000;
        agentStartTimeout = agentStartTimeout != null ? agentStartTimeout : Duration.ofSeconds(30);
        agentStopTimeout = agentStopTimeout != null ? agentStopTimeout : Duration.ofSeconds(10);
        workingDirectory = workingDirectory != null ? workingDirectory : System.getProperty("user.dir");
        
        // 确保properties不可变
        properties = properties != null ? 
            Collections.unmodifiableMap(new HashMap<>(properties)) : 
            Collections.emptyMap();
    }

    /**
     * 创建配置构建器
     * 
     * @param name 容器名称
     * @return ConfigBuilder实例
     */
    public static ConfigBuilder builder(String name) {
        return new ConfigBuilder(name);
    }

    /**
     * 创建默认配置
     * 
     * @param name 容器名称
     * @return ContainerConfig实例
     */
    public static ContainerConfig defaultConfig(String name) {
        return builder(name).build();
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
     * 配置构建器
     */
    public static class ConfigBuilder {
        private final String name;
        private int maxAgents;
        private Duration agentStartTimeout;
        private Duration agentStopTimeout;
        private boolean autoStartAgents = true;
        private boolean enableMonitoring = true;
        private String workingDirectory;
        private Map<String, Object> properties = new HashMap<>();

        private ConfigBuilder(String name) {
            this.name = name;
        }

        public ConfigBuilder maxAgents(int maxAgents) {
            this.maxAgents = maxAgents;
            return this;
        }

        public ConfigBuilder agentStartTimeout(Duration agentStartTimeout) {
            this.agentStartTimeout = agentStartTimeout;
            return this;
        }

        public ConfigBuilder agentStopTimeout(Duration agentStopTimeout) {
            this.agentStopTimeout = agentStopTimeout;
            return this;
        }

        public ConfigBuilder autoStartAgents(boolean autoStartAgents) {
            this.autoStartAgents = autoStartAgents;
            return this;
        }

        public ConfigBuilder enableMonitoring(boolean enableMonitoring) {
            this.enableMonitoring = enableMonitoring;
            return this;
        }

        public ConfigBuilder workingDirectory(String workingDirectory) {
            this.workingDirectory = workingDirectory;
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

        public ContainerConfig build() {
            return new ContainerConfig(
                name, maxAgents, agentStartTimeout, agentStopTimeout,
                autoStartAgents, enableMonitoring, workingDirectory, properties
            );
        }
    }
}
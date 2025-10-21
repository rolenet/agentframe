package com.agentcore.container.service;

import com.agentcore.core.agent.AgentId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 服务描述符
 * 描述Agent提供的服务信息
 * 
 * @author AgentCore Team
 */
public record ServiceDescriptor(
    @JsonProperty("agentId") AgentId agentId,
    @JsonProperty("serviceName") String serviceName,
    @JsonProperty("serviceType") String serviceType,
    @JsonProperty("description") String description,
    @JsonProperty("version") String version,
    @JsonProperty("endpoint") String endpoint,
    @JsonProperty("properties") Map<String, Object> properties,
    @JsonProperty("registrationTime") Instant registrationTime,
    @JsonProperty("lastHeartbeat") Instant lastHeartbeat,
    @JsonProperty("status") ServiceStatus status
) {

    /**
     * 创建ServiceDescriptor
     */
    @JsonCreator
    public ServiceDescriptor {
        // 验证必需字段
        Objects.requireNonNull(agentId, "Agent ID cannot be null");
        Objects.requireNonNull(serviceName, "Service name cannot be null");
        Objects.requireNonNull(serviceType, "Service type cannot be null");
        
        // 设置默认值
        description = description != null ? description : "";
        version = version != null ? version : "1.0.0";
        endpoint = endpoint != null ? endpoint : "";
        registrationTime = registrationTime != null ? registrationTime : Instant.now();
        lastHeartbeat = lastHeartbeat != null ? lastHeartbeat : Instant.now();
        status = status != null ? status : ServiceStatus.ACTIVE;
        
        // 确保properties不可变
        properties = properties != null ? 
            Collections.unmodifiableMap(new HashMap<>(properties)) : 
            Collections.emptyMap();
    }

    /**
     * 创建服务描述符构建器
     * 
     * @param agentId Agent ID
     * @param serviceName 服务名称
     * @param serviceType 服务类型
     * @return ServiceBuilder实例
     */
    public static ServiceBuilder builder(AgentId agentId, String serviceName, String serviceType) {
        return new ServiceBuilder(agentId, serviceName, serviceType);
    }

    /**
     * 获取服务的唯一标识
     * 
     * @return 服务唯一标识
     */
    public String getServiceId() {
        return agentId.getFullId() + ":" + serviceName;
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
     * 检查服务是否活跃
     * 
     * @return 如果服务活跃返回true
     */
    public boolean isActive() {
        return status == ServiceStatus.ACTIVE;
    }

    /**
     * 检查服务是否可用
     * 
     * @return 如果服务可用返回true
     */
    public boolean isAvailable() {
        return status == ServiceStatus.ACTIVE || status == ServiceStatus.BUSY;
    }

    /**
     * 更新心跳时间
     * 
     * @return 新的ServiceDescriptor实例
     */
    public ServiceDescriptor updateHeartbeat() {
        return new ServiceDescriptor(
            agentId, serviceName, serviceType, description, version,
            endpoint, properties, registrationTime, Instant.now(), status
        );
    }

    /**
     * 更新服务状态
     * 
     * @param newStatus 新状态
     * @return 新的ServiceDescriptor实例
     */
    public ServiceDescriptor updateStatus(ServiceStatus newStatus) {
        return new ServiceDescriptor(
            agentId, serviceName, serviceType, description, version,
            endpoint, properties, registrationTime, lastHeartbeat, newStatus
        );
    }

    /**
     * 服务状态枚举
     */
    public enum ServiceStatus {
        /**
         * 活跃状态 - 服务正常运行
         */
        ACTIVE,
        
        /**
         * 忙碌状态 - 服务正在处理请求
         */
        BUSY,
        
        /**
         * 暂停状态 - 服务暂时不可用
         */
        SUSPENDED,
        
        /**
         * 错误状态 - 服务出现错误
         */
        ERROR,
        
        /**
         * 停止状态 - 服务已停止
         */
        STOPPED
    }

    /**
     * 服务描述符构建器
     */
    public static class ServiceBuilder {
        private final AgentId agentId;
        private final String serviceName;
        private final String serviceType;
        private String description;
        private String version;
        private String endpoint;
        private Map<String, Object> properties = new HashMap<>();
        private Instant registrationTime;
        private Instant lastHeartbeat;
        private ServiceStatus status;

        private ServiceBuilder(AgentId agentId, String serviceName, String serviceType) {
            this.agentId = agentId;
            this.serviceName = serviceName;
            this.serviceType = serviceType;
        }

        public ServiceBuilder description(String description) {
            this.description = description;
            return this;
        }

        public ServiceBuilder version(String version) {
            this.version = version;
            return this;
        }

        public ServiceBuilder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public ServiceBuilder property(String key, Object value) {
            this.properties.put(key, value);
            return this;
        }

        public ServiceBuilder properties(Map<String, Object> properties) {
            this.properties.putAll(properties);
            return this;
        }

        public ServiceBuilder registrationTime(Instant registrationTime) {
            this.registrationTime = registrationTime;
            return this;
        }

        public ServiceBuilder lastHeartbeat(Instant lastHeartbeat) {
            this.lastHeartbeat = lastHeartbeat;
            return this;
        }

        public ServiceBuilder status(ServiceStatus status) {
            this.status = status;
            return this;
        }

        public ServiceDescriptor build() {
            return new ServiceDescriptor(
                agentId, serviceName, serviceType, description, version,
                endpoint, properties, registrationTime, lastHeartbeat, status
            );
        }
    }

    @Override
    public String toString() {
        return String.format(
            "ServiceDescriptor{agentId=%s, serviceName='%s', serviceType='%s', status=%s, endpoint='%s'}",
            agentId, serviceName, serviceType, status, endpoint
        );
    }
}
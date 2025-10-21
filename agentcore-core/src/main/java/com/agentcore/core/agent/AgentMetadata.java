package com.agentcore.core.agent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Agent元数据
 * 包含Agent的描述信息、创建时间、属性等
 * 
 * @author AgentCore Team
 */
public record AgentMetadata(
    @JsonProperty("description") String description,
    @JsonProperty("version") String version,
    @JsonProperty("createdAt") LocalDateTime createdAt,
    @JsonProperty("lastModifiedAt") LocalDateTime lastModifiedAt,
    @JsonProperty("properties") Map<String, Object> properties
) implements Serializable {

    /**
     * 创建AgentMetadata
     */
    @JsonCreator
    public AgentMetadata {
        Objects.requireNonNull(description, "Description cannot be null");
        Objects.requireNonNull(version, "Version cannot be null");
        Objects.requireNonNull(createdAt, "CreatedAt cannot be null");
        Objects.requireNonNull(lastModifiedAt, "LastModifiedAt cannot be null");
        
        // 确保properties不可变
        properties = properties != null ? 
            Collections.unmodifiableMap(new HashMap<>(properties)) : 
            Collections.emptyMap();
    }

    /**
     * 创建默认的AgentMetadata
     * 
     * @param description Agent描述
     * @return AgentMetadata实例
     */
    public static AgentMetadata create(String description) {
        LocalDateTime now = LocalDateTime.now();
        return new AgentMetadata(
            description,
            "1.0.0",
            now,
            now,
            Collections.emptyMap()
        );
    }

    /**
     * 创建带版本的AgentMetadata
     * 
     * @param description Agent描述
     * @param version Agent版本
     * @return AgentMetadata实例
     */
    public static AgentMetadata create(String description, String version) {
        LocalDateTime now = LocalDateTime.now();
        return new AgentMetadata(
            description,
            version,
            now,
            now,
            Collections.emptyMap()
        );
    }

    /**
     * 创建完整的AgentMetadata
     * 
     * @param description Agent描述
     * @param version Agent版本
     * @param properties Agent属性
     * @return AgentMetadata实例
     */
    public static AgentMetadata create(String description, String version, Map<String, Object> properties) {
        LocalDateTime now = LocalDateTime.now();
        return new AgentMetadata(
            description,
            version,
            now,
            now,
            properties
        );
    }

    /**
     * 更新元数据
     * 
     * @param newDescription 新描述
     * @return 更新后的AgentMetadata
     */
    public AgentMetadata withDescription(String newDescription) {
        return new AgentMetadata(
            newDescription,
            version,
            createdAt,
            LocalDateTime.now(),
            properties
        );
    }

    /**
     * 更新版本
     * 
     * @param newVersion 新版本
     * @return 更新后的AgentMetadata
     */
    public AgentMetadata withVersion(String newVersion) {
        return new AgentMetadata(
            description,
            newVersion,
            createdAt,
            LocalDateTime.now(),
            properties
        );
    }

    /**
     * 添加属性
     * 
     * @param key 属性键
     * @param value 属性值
     * @return 更新后的AgentMetadata
     */
    public AgentMetadata withProperty(String key, Object value) {
        Map<String, Object> newProperties = new HashMap<>(properties);
        newProperties.put(key, value);
        return new AgentMetadata(
            description,
            version,
            createdAt,
            LocalDateTime.now(),
            newProperties
        );
    }

    /**
     * 添加多个属性
     * 
     * @param newProperties 新属性映射
     * @return 更新后的AgentMetadata
     */
    public AgentMetadata withProperties(Map<String, Object> newProperties) {
        Map<String, Object> mergedProperties = new HashMap<>(properties);
        mergedProperties.putAll(newProperties);
        return new AgentMetadata(
            description,
            version,
            createdAt,
            LocalDateTime.now(),
            mergedProperties
        );
    }

    /**
     * 移除属性
     * 
     * @param key 要移除的属性键
     * @return 更新后的AgentMetadata
     */
    public AgentMetadata withoutProperty(String key) {
        Map<String, Object> newProperties = new HashMap<>(properties);
        newProperties.remove(key);
        return new AgentMetadata(
            description,
            version,
            createdAt,
            LocalDateTime.now(),
            newProperties
        );
    }

    /**
     * 获取属性值
     * 
     * @param key 属性键
     * @return 属性值，如果不存在返回null
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
     * 检查是否包含指定属性
     * 
     * @param key 属性键
     * @return 如果包含返回true
     */
    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    /**
     * 获取所有属性键
     * 
     * @return 属性键集合
     */
    public java.util.Set<String> getPropertyKeys() {
        return properties.keySet();
    }

    @Override
    public String toString() {
        return String.format("AgentMetadata{description='%s', version='%s', createdAt=%s, properties=%d}", 
            description, version, createdAt, properties.size());
    }
}
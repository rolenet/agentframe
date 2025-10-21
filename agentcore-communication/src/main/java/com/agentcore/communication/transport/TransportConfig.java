package com.agentcore.communication.transport;

import java.util.Map;

/**
 * 传输层配置接口
 * 
 * @author AgentCore Team
 */
public interface TransportConfig {

    /**
     * 获取传输层名称
     * 
     * @return 传输层名称
     */
    String getName();

    /**
     * 获取传输层类型
     * 
     * @return 传输层类型
     */
    Transport.TransportType getType();

    /**
     * 获取主机地址
     * 
     * @return 主机地址
     */
    String getHost();

    /**
     * 获取端口号
     * 
     * @return 端口号
     */
    int getPort();

    /**
     * 获取配置属性
     * 
     * @return 配置属性映射
     */
    Map<String, Object> getProperties();

    /**
     * 获取属性值
     * 
     * @param key 属性键
     * @param defaultValue 默认值
     * @param <T> 值类型
     * @return 属性值
     */
    @SuppressWarnings("unchecked")
    default <T> T getProperty(String key, T defaultValue) {
        Object value = getProperties().get(key);
        if (value != null && defaultValue.getClass().isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        return defaultValue;
    }

    /**
     * 检查是否包含指定属性
     * 
     * @param key 属性键
     * @return 如果包含返回true
     */
    default boolean hasProperty(String key) {
        return getProperties().containsKey(key);
    }
}
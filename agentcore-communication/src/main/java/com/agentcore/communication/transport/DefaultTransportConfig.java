package com.agentcore.communication.transport;

import java.util.HashMap;
import java.util.Map;

/**
 * 默认传输配置实现
 * 
 * @author AgentCore Team
 */
public class DefaultTransportConfig implements TransportConfig {

    private final String name;
    private final Transport.TransportType type;
    private final String host;
    private final int port;
    private final Map<String, Object> properties;

    /**
     * 构造函数
     * 
     * @param name 传输层名称
     * @param type 传输层类型
     * @param host 主机地址
     * @param port 端口号
     */
    public DefaultTransportConfig(String name, Transport.TransportType type, String host, int port) {
        this.name = name;
        this.type = type;
        this.host = host;
        this.port = port;
        this.properties = new HashMap<>();
        
        // 设置默认属性
        this.properties.put("connectTimeout", 5000);
        this.properties.put("bufferSize", 8192);
    }

    /**
     * 构造函数
     * 
     * @param name 传输层名称
     * @param type 传输层类型
     * @param host 主机地址
     * @param port 端口号
     * @param properties 配置属性
     */
    public DefaultTransportConfig(String name, Transport.TransportType type, String host, int port, Map<String, Object> properties) {
        this.name = name;
        this.type = type;
        this.host = host;
        this.port = port;
        this.properties = new HashMap<>(properties);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Transport.TransportType getType() {
        return type;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * 设置属性
     * 
     * @param key 属性键
     * @param value 属性值
     */
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    @Override
    public String toString() {
        return String.format("DefaultTransportConfig{name='%s', type=%s, host='%s', port=%d, properties=%s}", 
            name, type, host, port, properties);
    }
}
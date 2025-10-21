package com.agentcore.core.agent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.UUID;

/**
 * Agent身份标识
 * 每个Agent拥有唯一的ID，包含名称、地址、类型等元数据
 * 
 * @author AgentCore Team
 */
public record AgentId(
    @JsonProperty("name") String name,
    @JsonProperty("address") String address,
    @JsonProperty("type") String type,
    @JsonProperty("uuid") String uuid
) implements Serializable {

    /**
     * 创建AgentId
     */
    @JsonCreator
    public AgentId {
        Objects.requireNonNull(name, "Agent name cannot be null");
        Objects.requireNonNull(address, "Agent address cannot be null");
        Objects.requireNonNull(type, "Agent type cannot be null");
        Objects.requireNonNull(uuid, "Agent uuid cannot be null");
    }

    /**
     * 创建本地Agent ID
     * 
     * @param name Agent名称
     * @param type Agent类型
     * @return AgentId实例
     */
    public static AgentId createLocal(String name, String type) {
        return new AgentId(
            name,
            getLocalAddress(),
            type,
            UUID.randomUUID().toString()
        );
    }

    /**
     * 创建远程Agent ID
     * 
     * @param name Agent名称
     * @param address Agent地址
     * @param type Agent类型
     * @return AgentId实例
     */
    public static AgentId createRemote(String name, String address, String type) {
        return new AgentId(
            name,
            address,
            type,
            UUID.randomUUID().toString()
        );
    }

    /**
     * 从字符串解析AgentId
     * 格式: name@address:type:uuid
     * 
     * @param agentIdStr Agent ID字符串
     * @return AgentId实例
     */
    public static AgentId fromString(String agentIdStr) {
        if (agentIdStr == null || agentIdStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID string cannot be null or empty");
        }

        String[] parts = agentIdStr.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid Agent ID format: " + agentIdStr);
        }

        String[] nameAndAddress = parts[0].split("@");
        if (nameAndAddress.length != 2) {
            throw new IllegalArgumentException("Invalid Agent ID format: " + agentIdStr);
        }

        return new AgentId(nameAndAddress[0], nameAndAddress[1], parts[1], parts[2]);
    }

    /**
     * 获取完整的Agent标识符
     * 格式: name@address:type:uuid
     * 
     * @return Agent标识符字符串
     */
    public String getFullId() {
        return String.format("%s@%s:%s:%s", name, address, type, uuid);
    }

    /**
     * 获取简短的Agent标识符
     * 格式: name@address
     * 
     * @return 简短标识符
     */
    public String getShortId() {
        return String.format("%s@%s", name, address);
    }

    /**
     * 检查是否为本地Agent
     * 
     * @return 如果是本地Agent返回true
     */
    public boolean isLocal() {
        return getLocalAddress().equals(address);
    }

    /**
     * 检查是否为相同类型的Agent
     * 
     * @param other 另一个AgentId
     * @return 如果类型相同返回true
     */
    public boolean isSameType(AgentId other) {
        return other != null && Objects.equals(this.type, other.type);
    }

    /**
     * 根据名称创建AgentId（兼容性方法）
     * 
     * @param name Agent名称
     * @return AgentId实例
     */
    public static AgentId create(String name) {
        return createLocal(name, "Agent");
    }

    /**
     * 获取本地地址
     * 
     * @return 本地IP地址
     */
    private static String getLocalAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "localhost";
        }
    }

    @Override
    public String toString() {
        return getFullId();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AgentId agentId = (AgentId) obj;
        return Objects.equals(uuid, agentId.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
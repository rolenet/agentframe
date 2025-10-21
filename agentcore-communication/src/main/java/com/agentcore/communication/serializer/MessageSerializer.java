package com.agentcore.communication.serializer;

import com.agentcore.core.message.AgentMessage;

/**
 * 消息序列化器接口
 * 定义消息序列化和反序列化的契约
 * 
 * @author AgentCore Team
 */
public interface MessageSerializer {

    /**
     * 序列化消息
     * 
     * @param message 要序列化的消息
     * @return 序列化后的字节数组
     * @throws SerializationException 序列化异常
     */
    byte[] serialize(AgentMessage message) throws SerializationException;

    /**
     * 反序列化消息
     * 
     * @param data 序列化的字节数组
     * @return 反序列化后的消息
     * @throws SerializationException 反序列化异常
     */
    AgentMessage deserialize(byte[] data) throws SerializationException;

    /**
     * 获取序列化器名称
     * 
     * @return 序列化器名称
     */
    String getName();

    /**
     * 获取序列化器版本
     * 
     * @return 序列化器版本
     */
    String getVersion();

    /**
     * 获取支持的内容类型
     * 
     * @return 内容类型
     */
    String getContentType();

    /**
     * 检查是否支持压缩
     * 
     * @return 如果支持压缩返回true
     */
    default boolean supportsCompression() {
        return false;
    }

    /**
     * 检查序列化器是否可用
     * 
     * @return 如果可用返回true
     */
    default boolean isAvailable() {
        return true;
    }

    /**
     * 获取序列化器优先级
     * 数值越大优先级越高
     * 
     * @return 优先级
     */
    default int getPriority() {
        return 0;
    }
}
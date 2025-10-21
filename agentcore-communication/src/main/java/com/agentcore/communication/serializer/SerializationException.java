package com.agentcore.communication.serializer;

/**
 * 序列化异常
 * 
 * @author AgentCore Team
 */
public class SerializationException extends Exception {

    /**
     * 构造函数
     * 
     * @param message 异常消息
     */
    public SerializationException(String message) {
        super(message);
    }

    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param cause 原因异常
     */
    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 构造函数
     * 
     * @param cause 原因异常
     */
    public SerializationException(Throwable cause) {
        super(cause);
    }
}
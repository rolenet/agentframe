package com.agentcore.core.message;

import com.agentcore.core.agent.AgentId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Agent消息
 * 替代JADE的ACLMessage，提供更现代化的消息格式
 * 
 * @author AgentCore Team
 */
public record AgentMessage(
    @JsonProperty("messageId") String messageId,
    @JsonProperty("sender") AgentId sender,
    @JsonProperty("receiver") AgentId receiver,
    @JsonProperty("performative") MessagePerformative performative,
    @JsonProperty("content") Object content,
    @JsonProperty("contentType") String contentType,
    @JsonProperty("conversationId") String conversationId,
    @JsonProperty("replyWith") String replyWith,
    @JsonProperty("inReplyTo") String inReplyTo,
    @JsonProperty("priority") MessagePriority priority,
    @JsonProperty("timestamp") LocalDateTime timestamp,
    @JsonProperty("timeoutMs") long timeoutMs,
    @JsonProperty("properties") Map<String, Object> properties
) implements Serializable {

    /**
     * 创建AgentMessage
     */
    @JsonCreator
    public AgentMessage {
        Objects.requireNonNull(messageId, "Message ID cannot be null");
        Objects.requireNonNull(sender, "Sender cannot be null");
        Objects.requireNonNull(receiver, "Receiver cannot be null");
        Objects.requireNonNull(performative, "Performative cannot be null");
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        
        // 设置默认值
        contentType = contentType != null ? contentType : "text/plain";
        conversationId = conversationId != null ? conversationId : UUID.randomUUID().toString();
        priority = priority != null ? priority : MessagePriority.NORMAL;
        timeoutMs = timeoutMs > 0 ? timeoutMs : 30000; // 默认30秒超时
        
        // 确保properties不可变
        properties = properties != null ? 
            Collections.unmodifiableMap(new HashMap<>(properties)) : 
            Collections.emptyMap();
    }

    /**
     * 创建消息构建器
     * 
     * @return MessageBuilder实例
     */
    public static MessageBuilder builder() {
        return new MessageBuilder();
    }

    /**
     * 创建简单消息
     * 
     * @param sender 发送者
     * @param receiver 接收者
     * @param performative 消息类型
     * @param content 消息内容
     * @return AgentMessage实例
     */
    public static AgentMessage create(AgentId sender, AgentId receiver, 
                                    MessagePerformative performative, Object content) {
        return builder()
            .sender(sender)
            .receiver(receiver)
            .performative(performative)
            .content(content)
            .build();
    }

    /**
     * 创建请求消息
     * 
     * @param sender 发送者
     * @param receiver 接收者
     * @param content 请求内容
     * @return AgentMessage实例
     */
    public static AgentMessage createRequest(AgentId sender, AgentId receiver, Object content) {
        return create(sender, receiver, MessagePerformative.REQUEST, content);
    }

    /**
     * 创建响应消息
     * 
     * @param originalMessage 原始消息
     * @param content 响应内容
     * @return AgentMessage实例
     */
    public static AgentMessage createResponse(AgentMessage originalMessage, Object content) {
        return builder()
            .sender(originalMessage.receiver)
            .receiver(originalMessage.sender)
            .performative(MessagePerformative.INFORM)
            .content(content)
            .conversationId(originalMessage.conversationId)
            .inReplyTo(originalMessage.messageId)
            .build();
    }

    /**
     * 创建错误响应消息
     * 
     * @param originalMessage 原始消息
     * @param errorContent 错误内容
     * @return AgentMessage实例
     */
    public static AgentMessage createErrorResponse(AgentMessage originalMessage, Object errorContent) {
        return builder()
            .sender(originalMessage.receiver)
            .receiver(originalMessage.sender)
            .performative(MessagePerformative.FAILURE)
            .content(errorContent)
            .conversationId(originalMessage.conversationId)
            .inReplyTo(originalMessage.messageId)
            .build();
    }

    /**
     * 检查是否为响应消息
     * 
     * @return 如果是响应消息返回true
     */
    public boolean isResponse() {
        return inReplyTo != null && !inReplyTo.trim().isEmpty();
    }

    /**
     * 检查是否为请求消息
     * 
     * @return 如果是请求消息返回true
     */
    public boolean isRequest() {
        return performative == MessagePerformative.REQUEST || 
               performative == MessagePerformative.QUERY_IF ||
               performative == MessagePerformative.QUERY_REF;
    }

    /**
     * 检查消息是否已超时
     * 
     * @return 如果已超时返回true
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(timestamp.plusNanos(timeoutMs * 1_000_000));
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
     * 添加属性
     * 
     * @param key 属性键
     * @param value 属性值
     * @return 新的AgentMessage实例
     */
    public AgentMessage withProperty(String key, Object value) {
        Map<String, Object> newProperties = new HashMap<>(properties);
        newProperties.put(key, value);
        return new AgentMessage(
            messageId, sender, receiver, performative, content, contentType,
            conversationId, replyWith, inReplyTo, priority, timestamp, timeoutMs, newProperties
        );
    }

    @Override
    public String toString() {
        return String.format("AgentMessage{id=%s, from=%s, to=%s, type=%s, content=%s}", 
            messageId, sender.getShortId(), receiver.getShortId(), performative, content);
    }

    /**
     * 消息构建器
     */
    public static class MessageBuilder {
        private String messageId;
        private AgentId sender;
        private AgentId receiver;
        private MessagePerformative performative;
        private Object content;
        private String contentType;
        private String conversationId;
        private String replyWith;
        private String inReplyTo;
        private MessagePriority priority;
        private LocalDateTime timestamp;
        private long timeoutMs;
        private Map<String, Object> properties;

        private MessageBuilder() {
            this.messageId = UUID.randomUUID().toString();
            this.timestamp = LocalDateTime.now();
            this.properties = new HashMap<>();
        }

        public MessageBuilder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        public MessageBuilder sender(AgentId sender) {
            this.sender = sender;
            return this;
        }

        public MessageBuilder receiver(AgentId receiver) {
            this.receiver = receiver;
            return this;
        }

        public MessageBuilder performative(MessagePerformative performative) {
            this.performative = performative;
            return this;
        }

        public MessageBuilder content(Object content) {
            this.content = content;
            return this;
        }

        public MessageBuilder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public MessageBuilder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public MessageBuilder replyWith(String replyWith) {
            this.replyWith = replyWith;
            return this;
        }

        public MessageBuilder inReplyTo(String inReplyTo) {
            this.inReplyTo = inReplyTo;
            return this;
        }

        public MessageBuilder priority(MessagePriority priority) {
            this.priority = priority;
            return this;
        }

        public MessageBuilder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public MessageBuilder timeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        public MessageBuilder property(String key, Object value) {
            this.properties.put(key, value);
            return this;
        }

        public MessageBuilder properties(Map<String, Object> properties) {
            this.properties.putAll(properties);
            return this;
        }

        public AgentMessage build() {
            return new AgentMessage(
                messageId, sender, receiver, performative, content, contentType,
                conversationId, replyWith, inReplyTo, priority, timestamp, timeoutMs, properties
            );
        }
    }
}
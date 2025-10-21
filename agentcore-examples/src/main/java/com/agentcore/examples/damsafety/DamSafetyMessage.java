package com.agentcore.examples.damsafety;

import com.agentcore.core.agent.AgentId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 水库大坝安全系统自定义消息类
 * 包含消息类型、数据内容、处理状态等信息
 */
public class DamSafetyMessage {
    
    /**
     * 消息类型枚举
     */
    public enum MessageType {
        DATA_COLLECTION("数据采集"),
        DATA_PROCESSING("数据处理"),
        DATA_ANALYSIS("数据分析"),
        REPORT_GENERATION("报告生成"),
        COORDINATION("协调控制"),
        STATUS_UPDATE("状态更新"),
        WORK_COMPLETED("工作完成"),
        SYSTEM_SHUTDOWN("系统关闭");
        
        private final String description;
        
        MessageType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 处理状态枚举
     */
    public enum ProcessingStatus {
        PENDING("待处理"),
        PROCESSING("处理中"),
        COMPLETED("已完成"),
        FAILED("处理失败"),
        CANCELLED("已取消");
        
        private final String description;
        
        ProcessingStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private final String messageId;
    private final MessageType messageType;
    private final AgentId sender;
    private final AgentId receiver;
    private final Map<String, Object> dataContent;
    private final ProcessingStatus processingStatus;
    private final LocalDateTime timestamp;
    private final String conversationId;
    private final Map<String, Object> metadata;
    
    @JsonCreator
    public DamSafetyMessage(
            @JsonProperty("messageId") String messageId,
            @JsonProperty("messageType") MessageType messageType,
            @JsonProperty("sender") AgentId sender,
            @JsonProperty("receiver") AgentId receiver,
            @JsonProperty("dataContent") Map<String, Object> dataContent,
            @JsonProperty("processingStatus") ProcessingStatus processingStatus,
            @JsonProperty("timestamp") LocalDateTime timestamp,
            @JsonProperty("conversationId") String conversationId,
            @JsonProperty("metadata") Map<String, Object> metadata) {
        this.messageId = messageId != null ? messageId : UUID.randomUUID().toString();
        this.messageType = messageType;
        this.sender = sender;
        this.receiver = receiver;
        this.dataContent = dataContent;
        this.processingStatus = processingStatus != null ? processingStatus : ProcessingStatus.PENDING;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.conversationId = conversationId != null ? conversationId : UUID.randomUUID().toString();
        this.metadata = metadata;
    }
    
    // 静态工厂方法
    public static DamSafetyMessage create(MessageType messageType, AgentId sender, AgentId receiver, 
                                         Map<String, Object> dataContent) {
        return new DamSafetyMessage(null, messageType, sender, receiver, dataContent, 
                                   ProcessingStatus.PENDING, null, null, null);
    }
    
    public static DamSafetyMessage createWithStatus(MessageType messageType, AgentId sender, AgentId receiver,
                                                   Map<String, Object> dataContent, ProcessingStatus status) {
        return new DamSafetyMessage(null, messageType, sender, receiver, dataContent, 
                                   status, null, null, null);
    }
    
    // Getters
    public String getMessageId() {
        return messageId;
    }
    
    public MessageType getMessageType() {
        return messageType;
    }
    
    public AgentId getSender() {
        return sender;
    }
    
    public AgentId getReceiver() {
        return receiver;
    }
    
    public Map<String, Object> getDataContent() {
        return dataContent;
    }
    
    public ProcessingStatus getProcessingStatus() {
        return processingStatus;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public String getConversationId() {
        return conversationId;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    // 创建状态更新消息
    public DamSafetyMessage withStatus(ProcessingStatus newStatus) {
        return new DamSafetyMessage(messageId, messageType, sender, receiver, dataContent,
                                   newStatus, timestamp, conversationId, metadata);
    }
    
    // 创建响应消息
    public DamSafetyMessage createResponse(MessageType responseType, Map<String, Object> responseData) {
        return new DamSafetyMessage(null, responseType, receiver, sender, responseData,
                                   ProcessingStatus.COMPLETED, null, conversationId, null);
    }
    
    @Override
    public String toString() {
        return String.format("DamSafetyMessage{id=%s, type=%s, from=%s, to=%s, status=%s, timestamp=%s}",
                messageId, messageType, sender != null ? sender.getShortId() : "null", 
                receiver != null ? receiver.getShortId() : "null", processingStatus, timestamp);
    }
}
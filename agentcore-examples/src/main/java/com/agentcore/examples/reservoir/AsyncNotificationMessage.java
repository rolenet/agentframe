package com.agentcore.examples.reservoir;

import com.agentcore.core.agent.AgentId;

/**
 * 异步通知消息
 * 用于智能体间的异步状态通知
 */
public class AsyncNotificationMessage {
    
    /**
     * 通知类型枚举
     */
    public enum NotificationType {
        AGENT_READY("智能体启动完成"),
        WORK_COMPLETED("工作完成"),
        WORKFLOW_FINISHED("工作流完成"),
        DATA_READY("数据准备完成"),
        REPORT_READY("报告准备完成");
        
        private final String description;
        
        NotificationType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private final NotificationType type;
    private final AgentId sender;
    private final Object payload;
    private final long timestamp;
    private final String messageId;
    
    public AsyncNotificationMessage(NotificationType type, AgentId sender, Object payload) {
        this.type = type;
        this.sender = sender;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
        this.messageId = generateMessageId();
    }
    
    public AsyncNotificationMessage(NotificationType type, AgentId sender) {
        this(type, sender, null);
    }
    
    private String generateMessageId() {
        return String.format("ASYNC_%s_%s_%d", 
                type.name(), 
                sender.getShortId(), 
                timestamp);
    }
    
    public NotificationType getType() {
        return type;
    }
    
    public AgentId getSender() {
        return sender;
    }
    
    public Object getPayload() {
        return payload;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public String getMessageId() {
        return messageId;
    }
    
    @Override
    public String toString() {
        return String.format("AsyncNotificationMessage{type=%s, sender=%s, messageId=%s, timestamp=%d, hasPayload=%s}",
                type, sender.getShortId(), messageId, timestamp, payload != null);
    }
}
package com.agentcore.examples.reservoir;

import com.agentcore.core.agent.AgentId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

/**
 * 异步通知处理器
 * 负责处理和分发异步通知消息
 */
public class AsyncNotificationHandler {
    private static final Logger logger = LoggerFactory.getLogger(AsyncNotificationHandler.class);
    
    // 通知监听器注册表
    private final ConcurrentMap<AsyncNotificationMessage.NotificationType, Consumer<AsyncNotificationMessage>> listeners = new ConcurrentHashMap<>();
    
    // 全局通知管理器引用
    private AsyncNotificationManager notificationManager;
    
    public AsyncNotificationHandler(AsyncNotificationManager notificationManager) {
        this.notificationManager = notificationManager;
        setupDefaultHandlers();
    }
    
    /**
     * 设置默认的通知处理器
     */
    private void setupDefaultHandlers() {
        // 智能体启动完成通知处理
        registerListener(AsyncNotificationMessage.NotificationType.AGENT_READY, message -> {
            logger.info("🔔 处理智能体启动完成通知: {}", message.getSender().getShortId());
            notificationManager.notifyAgentReady(message.getSender());
        });
        
        // 工作完成通知处理
        registerListener(AsyncNotificationMessage.NotificationType.WORK_COMPLETED, message -> {
            logger.info("🔔 处理工作完成通知: {} - payload: {}", 
                    message.getSender().getShortId(), 
                    message.getPayload() != null ? message.getPayload().getClass().getSimpleName() : "null");
            notificationManager.notifyWorkCompleted(message.getSender(), message.getPayload());
        });
        
        // 工作流完成通知处理
        registerListener(AsyncNotificationMessage.NotificationType.WORKFLOW_FINISHED, message -> {
            logger.info("🔔 处理工作流完成通知: {}", message.getSender().getShortId());
            notificationManager.notifyWorkflowFinished();
        });
        
        // 数据准备完成通知处理
        registerListener(AsyncNotificationMessage.NotificationType.DATA_READY, message -> {
            logger.info("🔔 处理数据准备完成通知: {} - payload: {}", 
                    message.getSender().getShortId(),
                    message.getPayload() != null ? message.getPayload().getClass().getSimpleName() : "null");
        });
        
        // 报告准备完成通知处理
        registerListener(AsyncNotificationMessage.NotificationType.REPORT_READY, message -> {
            logger.info("🔔 处理报告准备完成通知: {} - payload: {}", 
                    message.getSender().getShortId(),
                    message.getPayload() != null ? message.getPayload().getClass().getSimpleName() : "null");
        });
    }
    
    /**
     * 注册通知监听器
     */
    public void registerListener(AsyncNotificationMessage.NotificationType type, Consumer<AsyncNotificationMessage> listener) {
        listeners.put(type, listener);
        logger.debug("🔔 注册通知监听器: {}", type);
    }
    
    /**
     * 处理异步通知消息
     */
    public void handleNotification(AsyncNotificationMessage message) {
        logger.debug("🔔 收到异步通知: {}", message);
        
        Consumer<AsyncNotificationMessage> listener = listeners.get(message.getType());
        if (listener != null) {
            try {
                listener.accept(message);
            } catch (Exception e) {
                logger.error("🔔 处理异步通知时发生错误: {}", message, e);
            }
        } else {
            logger.warn("🔔 未找到通知类型{}的处理器", message.getType());
        }
    }
    
    /**
     * 发送异步通知
     */
    public void sendNotification(AsyncNotificationMessage.NotificationType type, AgentId sender, Object payload) {
        AsyncNotificationMessage message = new AsyncNotificationMessage(type, sender, payload);
        logger.debug("🔔 发送异步通知: {}", message);
        handleNotification(message);
    }
    
    /**
     * 发送异步通知（无payload）
     */
    public void sendNotification(AsyncNotificationMessage.NotificationType type, AgentId sender) {
        sendNotification(type, sender, null);
    }
    
    /**
     * 移除监听器
     */
    public void removeListener(AsyncNotificationMessage.NotificationType type) {
        listeners.remove(type);
        logger.debug("🔔 移除通知监听器: {}", type);
    }
    
    /**
     * 清除所有监听器
     */
    public void clearListeners() {
        listeners.clear();
        logger.info("🔔 清除所有通知监听器");
    }
    
    /**
     * 获取已注册的监听器类型
     */
    public java.util.Set<AsyncNotificationMessage.NotificationType> getRegisteredTypes() {
        return listeners.keySet();
    }
}
package com.agentcore.core.agent;

import com.agentcore.core.behavior.BehaviorScheduler;
import com.agentcore.core.behavior.DefaultBehaviorScheduler;
import com.agentcore.core.message.AgentMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * 简单Agent实现
 * 提供基本的Agent功能实现，可以直接使用或继承扩展
 * 
 * @author AgentCore Team
 */
public class SimpleAgent extends AbstractAgent {
    private static final Logger logger = LoggerFactory.getLogger(SimpleAgent.class);



    /**
     * 构造函数
     * 
     * @param name Agent名称
     * @param type Agent类型
     */
    public SimpleAgent(String name, String type) {
        super(AgentId.createLocal(name, type));
    }

    /**
     * 构造函数
     * 
     * @param agentId Agent ID
     */
    public SimpleAgent(AgentId agentId) {
        super(agentId);
    }

    @Override
    protected BehaviorScheduler createBehaviorScheduler() {
        return new DefaultBehaviorScheduler();
    }

    @Override
    protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
        // 简单实现：直接调用目标Agent的消息处理方法
        // 在实际应用中，这里应该通过消息路由器发送消息
        return CompletableFuture.runAsync(() -> {
            // TODO: 集成消息路由器
            logger.info("SimpleAgent sending message: {}", message);
        });
    }

    @Override
    protected void doHandleMessage(AgentMessage message) {
        // 默认消息处理逻辑
        logger.info("Agent {} received message: {} from {}",
            getAgentId().name(), message.performative(), message.sender().name());
        
        // 如果是请求消息，发送确认响应
        if (message.isRequest()) {
            AgentMessage response = AgentMessage.createResponse(message, 
                "Message processed by " + getAgentId().name());
            sendMessage(response);
        }
    }
}
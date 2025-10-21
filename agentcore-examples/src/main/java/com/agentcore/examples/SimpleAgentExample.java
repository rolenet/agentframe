package com.agentcore.examples;

import com.agentcore.core.agent.AbstractAgent;
import com.agentcore.core.agent.AgentId;
import com.agentcore.core.behavior.BehaviorScheduler;
import com.agentcore.core.behavior.DefaultBehaviorScheduler;
import com.agentcore.core.message.AgentMessage;
import com.agentcore.core.message.MessagePerformative;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * 简单的Agent示例
 * 演示AgentCore框架的基本使用
 * 
 * @author AgentCore Team
 */
public class SimpleAgentExample {

    private static final Logger logger = LoggerFactory.getLogger(SimpleAgentExample.class);

    public static void main(String[] args) {
        logger.info("=== 开始简单Agent示例演示 ===");

        try {
            // 创建两个简单的Agent进行交互演示
            AgentId agent1Id = AgentId.create("SimpleAgent-1");
            AgentId agent2Id = AgentId.create("SimpleAgent-2");
            
            SimpleAgent agent1 = new SimpleAgent(agent1Id);
            SimpleAgent agent2 = new SimpleAgent(agent2Id);

            agent1.init();
            agent2.init();
            // 启动Agent
            agent1.start();
            agent2.start();
            logger.info("Agent已启动: {} 和 {}", agent1Id.getShortId(), agent2Id.getShortId());

            // 让Agent运行一段时间，演示消息发送
            Thread.sleep(3000);
            
            // 演示Agent之间的消息发送
            logger.info("=== 开始消息交互演示 ===");
            sendDemoMessages(agent1, agent2);

            // 让Agent继续运行一段时间
            Thread.sleep(7000);

            // 停止Agent
            agent1.stop();
            agent2.stop();
            logger.info("Agent已停止: {} 和 {}", agent1Id.getShortId(), agent2Id.getShortId());

        } catch (Exception e) {
            logger.error("简单Agent示例演示过程中发生错误", e);
        }

        logger.info("=== 简单Agent示例演示完成 ===");
    }

    /**
     * 演示消息发送功能
     */
    private static void sendDemoMessages(SimpleAgent sender, SimpleAgent receiver) {
        try {
            // 发送几条演示消息
            for (int i = 1; i <= 3; i++) {
                String messageContent = String.format("演示消息 #%d - 来自 %s", 
                    i, sender.getAgentId().getShortId());
                AgentMessage message = AgentMessage.builder()
                    .sender(sender.getAgentId())
                    .receiver(receiver.getAgentId())
                    .performative(MessagePerformative.INFORM)
                    .content(messageContent)
                    .build();
                sender.sendMessage(message);
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("消息发送演示被中断");
        }
    }

    /**
     * 简单的Agent实现
     */
    static class SimpleAgent extends AbstractAgent {

        private static final Logger logger = LoggerFactory.getLogger(SimpleAgent.class);

        public SimpleAgent(AgentId agentId) {
            super(agentId);
        }

        @Override
        protected BehaviorScheduler createBehaviorScheduler() {
            return new DefaultBehaviorScheduler();
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            logger.debug("Sending message: {}", message);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected void doStart() {
            logger.info("SimpleAgent {} is starting...", getAgentId().getShortId());
        }

        @Override
        protected void doStop() {
            logger.info("SimpleAgent {} is stopping...", getAgentId().getShortId());
        }

        @Override
        protected void doHandleMessage(AgentMessage message) {
            logger.info("SimpleAgent {} received message: {}", 
                getAgentId().getShortId(), message.content());
        }
    }
}
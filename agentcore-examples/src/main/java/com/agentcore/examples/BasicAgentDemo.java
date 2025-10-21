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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基础Agent功能演示
 * 演示Agent的启动、发送消息、处理消息的完整流程
 * 
 * @author AgentCore Team
 */
public class BasicAgentDemo {

    private static final Logger logger = LoggerFactory.getLogger(BasicAgentDemo.class);

    public static void main(String[] args) {
        logger.info("=== 开始Agent基础功能演示 ===");

        try {
            // 创建发送者Agent和接收者Agent
            AgentId senderId = AgentId.create("SenderAgent");
            AgentId receiverId = AgentId.create("ReceiverAgent");
            
            SenderAgent sender = new SenderAgent(senderId);
            ReceiverAgent receiver = new ReceiverAgent(receiverId);

            // 设置接收者Agent的ID，让发送者知道发送给谁
            sender.setReceiverAgentId(receiverId);

            logger.info("创建Agent: {} 和 {}", senderId.getShortId(), receiverId.getShortId());

            // 启动Agent
            receiver.init();
            sender.init();
            receiver.start();
            sender.start();
            
            logger.info("所有Agent已启动");

            // 让Agent运行一段时间进行消息交互
            Thread.sleep(15000);

            // 停止Agent
            sender.stop();
            receiver.stop();
            
            logger.info("所有Agent已停止");

        } catch (Exception e) {
            logger.error("Agent演示过程中发生错误", e);
        }

        logger.info("=== Agent基础功能演示完成 ===");
    }

    /**
     * 发送者Agent - 负责发送消息
     */
    static class SenderAgent extends AbstractAgent {

        private static final Logger logger = LoggerFactory.getLogger(SenderAgent.class);
        private AgentId receiverAgentId;
        private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        private final AtomicInteger messageCounter = new AtomicInteger(0);

        public SenderAgent(AgentId agentId) {
            super(agentId);
        }

        public void setReceiverAgentId(AgentId receiverAgentId) {
            this.receiverAgentId = receiverAgentId;
        }

        @Override
        protected BehaviorScheduler createBehaviorScheduler() {
            return new DefaultBehaviorScheduler();
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            logger.info("发送者 {} 发送消息: {}", getAgentId().getShortId(), message.content());
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected void doStart() {
            logger.info("发送者Agent {} 启动成功", getAgentId().getShortId());
            
            // 启动定时发送消息的任务
            scheduler.scheduleAtFixedRate(this::sendMessageToReceiver, 2, 3, TimeUnit.SECONDS);
        }

        @Override
        protected void doStop() {
            logger.info("发送者Agent {} 正在停止", getAgentId().getShortId());
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        @Override
        protected void doHandleMessage(AgentMessage message) {
            logger.info("发送者 {} 收到回复消息: {}", getAgentId().getShortId(), message.content());
        }

        private void sendMessageToReceiver() {
            if (receiverAgentId != null) {
                int messageNumber = messageCounter.incrementAndGet();
                String messageContent = String.format("这是第 %d 条消息，来自发送者 %s", 
                    messageNumber, getAgentId().getShortId());
                
                AgentMessage message = AgentMessage.builder()
                    .sender(getAgentId())
                    .receiver(receiverAgentId)
                    .performative(MessagePerformative.INFORM)
                    .content(messageContent)
                    .build();
                sendMessage(message);
                
                logger.debug("已安排发送第 {} 条消息", messageNumber);
            }
        }
    }

    /**
     * 接收者Agent - 负责接收和处理消息
     */
    static class ReceiverAgent extends AbstractAgent {

        private static final Logger logger = LoggerFactory.getLogger(ReceiverAgent.class);

        public ReceiverAgent(AgentId agentId) {
            super(agentId);
        }

        @Override
        protected BehaviorScheduler createBehaviorScheduler() {
            return new DefaultBehaviorScheduler();
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            logger.info("接收者 {} 发送回复消息: {}", getAgentId().getShortId(), message.content());
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected void doStart() {
            logger.info("接收者Agent {} 启动成功，等待接收消息", getAgentId().getShortId());
        }

        @Override
        protected void doStop() {
            logger.info("接收者Agent {} 正在停止", getAgentId().getShortId());
        }

        @Override
        protected void doHandleMessage(AgentMessage message) {
            logger.info("接收者 {} 收到消息: {}", getAgentId().getShortId(), message.content());
            
            // 模拟消息处理
            processMessage(message);
            
            // 发送回复消息
            sendReplyToSender(message.sender());
        }

        private void processMessage(AgentMessage message) {
            // 模拟消息处理逻辑
            logger.debug("接收者 {} 正在处理消息: {}", getAgentId().getShortId(), message.content());
            
            try {
                // 模拟处理时间
                Thread.sleep(500);
                logger.debug("接收者 {} 消息处理完成", getAgentId().getShortId());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("消息处理被中断");
            }
        }

        private void sendReplyToSender(AgentId senderId) {
            String replyContent = String.format("收到你的消息，这是来自接收者 %s 的回复", 
                getAgentId().getShortId());
            
            AgentMessage replyMessage = AgentMessage.builder()
                .sender(getAgentId())
                .receiver(senderId)
                .performative(MessagePerformative.INFORM)
                .content(replyContent)
                .build();
            sendMessage(replyMessage);
            
            logger.debug("接收者 {} 已发送回复给发送者", getAgentId().getShortId());
        }
    }
}
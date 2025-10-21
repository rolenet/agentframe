package com.agentcore.examples;

import com.agentcore.core.agent.AbstractAgent;
import com.agentcore.core.agent.AgentId;
import com.agentcore.core.behavior.BehaviorScheduler;
import com.agentcore.core.behavior.DefaultBehaviorScheduler;
import com.agentcore.core.message.AgentMessage;
import com.agentcore.core.message.MessagePerformative;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Spring Boot版本的Agent功能演示
 * 演示在Spring环境中使用AgentCore框架
 * 
 * @author AgentCore Team
 */
@SpringBootApplication
public class SpringAgentDemo {
    private static final Logger logger = LoggerFactory.getLogger(SpringAgentDemo.class);

    public static void main(String[] args) {
        SpringApplication.run(SpringAgentDemo.class, args);
    }

    @Bean
    public CommandLineRunner demo() {
        return args -> {
            logger.info("=== Spring Boot Agent演示开始 ===");
            
            // 在Spring环境中创建和启动Agent
            AgentId springSenderId = AgentId.create("SpringSender");
            AgentId springReceiverId = AgentId.create("SpringReceiver");
            
            SpringSenderAgent sender = new SpringSenderAgent(springSenderId);
            SpringReceiverAgent receiver = new SpringReceiverAgent(springReceiverId);
            
            sender.setReceiverAgentId(springReceiverId);
            // 初始化Agent
            receiver.init();
            sender.init();

            // 启动Agent
            receiver.start();
            sender.start();

             logger.info("Spring环境中的Agent已启动");

            // 让程序运行一段时间后自动停止
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("正在停止Spring环境中的Agent...");
                sender.stop();
                receiver.stop();
                logger.info("Spring环境中的Agent已停止");
            }));
        };
    }

    /**
     * Spring环境中的发送者Agent
     */
    static class SpringSenderAgent extends AbstractAgent {

        private AgentId receiverAgentId;
        private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        private final AtomicInteger messageCounter = new AtomicInteger(0);

        public SpringSenderAgent(AgentId agentId) {
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
             logger.info("[Spring] 发送者 {} 发送消息: {}", getAgentId().getShortId(), message.content());
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected void doStart() {
             logger.info("[Spring] 发送者Agent {} 在Spring环境中启动", getAgentId().getShortId());
            
            // 延迟启动消息发送，确保接收者已准备好
            scheduler.scheduleAtFixedRate(this::sendSpringMessage, 3, 4, TimeUnit.SECONDS);
        }

        @Override
        protected void doStop() {
             logger.info("[Spring] 发送者Agent {} 正在停止", getAgentId().getShortId());
            scheduler.shutdown();
        }

        @Override
        protected void doHandleMessage(AgentMessage message) {
             logger.info("[Spring] 发送者 {} 收到Spring回复: {}", getAgentId().getShortId(), message.content());
        }

        private void sendSpringMessage() {
            if (receiverAgentId != null) {
                int messageNumber = messageCounter.incrementAndGet();
                String messageContent = String.format("[Spring] 第 %d 条消息，来自Spring发送者 %s", 
                    messageNumber, getAgentId().getShortId());
                
                AgentMessage message = AgentMessage.builder()
                    .sender(getAgentId())
                    .receiver(receiverAgentId)
                    .performative(MessagePerformative.INFORM)
                    .content(messageContent)
                    .build();
                sendMessage(message);
            }
        }
    }

    /**
     * Spring环境中的接收者Agent
     */
    static class SpringReceiverAgent extends AbstractAgent {

        public SpringReceiverAgent(AgentId agentId) {
            super(agentId);
        }

        @Override
        protected BehaviorScheduler createBehaviorScheduler() {
            return new DefaultBehaviorScheduler();
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
             logger.info("[Spring] 接收者 {} 发送回复: {}", getAgentId().getShortId(), message.content());
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected void doStart() {
             logger.info("[Spring] 接收者Agent {} 在Spring环境中启动", getAgentId().getShortId());
        }

        @Override
        protected void doStop() {
             logger.info("[Spring] 接收者Agent {} 正在停止", getAgentId().getShortId());
        }

        @Override
        protected void doHandleMessage(AgentMessage message) {
             logger.info("[Spring] 接收者 {} 收到Spring消息: {}", getAgentId().getShortId(), message.content());
            
            // 在Spring环境中处理消息
            processSpringMessage(message);
            
            // 发送Spring回复
            sendSpringReply(message.sender());
        }

        private void processSpringMessage(AgentMessage message) {
             logger.info("[Spring] 接收者 {} 正在Spring环境中处理消息", getAgentId().getShortId());
            
            try {
                // 模拟Spring环境中的处理逻辑
                Thread.sleep(300);
                System.out.format("[Spring] 消息处理完成");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private void sendSpringReply(AgentId senderId) {
            String replyContent = String.format("[Spring] 收到消息，这是来自Spring接收者 %s 的回复", 
                getAgentId().getShortId());
            
            AgentMessage replyMessage = AgentMessage.builder()
                .sender(getAgentId())
                .receiver(senderId)
                .performative(MessagePerformative.INFORM)
                .content(replyContent)
                .build();
            sendMessage(replyMessage);
        }
    }
}
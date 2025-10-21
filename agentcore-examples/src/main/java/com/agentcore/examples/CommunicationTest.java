package com.agentcore.examples;

import com.agentcore.communication.router.MessageRouter;
import com.agentcore.communication.transport.DefaultTransportConfig;
import com.agentcore.communication.transport.Transport;
import com.agentcore.communication.serializer.MessageSerializer;
import com.agentcore.communication.transport.TransportConfig;
import com.agentcore.core.agent.AgentId;
import com.agentcore.core.message.AgentMessage;
import com.agentcore.core.message.MessagePerformative;
import com.agentcore.core.message.MessagePriority;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * 通信功能测试类
 * 测试agentcore-communication模块的所有功能
 * 
 * @author AgentCore Team
 */
public class CommunicationTest {
    
    /**
     * 测试消息路由功能
     */
    public void testMessageRouting() {
        System.out.println("=== 测试消息路由功能 ===");
        
        // 创建消息路由器
        MessageRouter router = new DefaultMessageRouter();
        
        // 创建测试Agent
        TestAgent agent1 = new TestAgent("agent-1");
        TestAgent agent2 = new TestAgent("agent-2");
        
        // 注册Agent到路由器
        router.registerAgent(agent1.getAgentId(), agent1::handleMessage);
        router.registerAgent(agent2.getAgentId(), agent2::handleMessage);
        
        // 创建测试消息
        AgentMessage message1 = AgentMessage.builder()
            .messageId("msg-1")
            .sender(AgentId.create("sender-1"))
            .receiver(agent1.getAgentId())
            .performative(MessagePerformative.INFORM)
            .content("Hello Agent 1")
            .build();
            
        AgentMessage message2 = AgentMessage.builder()
            .messageId("msg-2")
            .sender(AgentId.create("sender-2"))
            .receiver(agent2.getAgentId())
            .performative(MessagePerformative.INFORM)
            .content("Hello Agent 2")
            .build();
        
        // 路由消息
        router.routeMessage(message1).join();
        router.routeMessage(message2).join();
        
        // 验证消息是否正确接收
        assert agent1.getReceivedMessages().size() == 1 : "Agent1 should receive 1 message";
        assert agent2.getReceivedMessages().size() == 1 : "Agent2 should receive 1 message";
        assert agent1.getReceivedMessages().get(0).equals(message1) : "Agent1 should receive correct message";
        assert agent2.getReceivedMessages().get(0).equals(message2) : "Agent2 should receive correct message";
        
        // 测试路由统计
        MessageRouter.RouterStats stats = router.getStats();
        System.out.println("路由统计: " + stats);
        assert stats.messagesRouted() == 2 : "Should route 2 messages";
        assert stats.registeredAgents() == 2 : "Should have 2 registered agents";
        
        System.out.println("✓ 消息路由功能测试通过");
    }
    
    /**
     * 测试传输层功能
     */
    public void testTransportLayer() {
        System.out.println("=== 测试传输层功能 ===");
        
        // 创建本地传输层
        Transport localTransport = new LocalTransport();
        
        // 创建消息处理器
        TestMessageHandler handler = new TestMessageHandler();
        localTransport.setMessageHandler(handler);
        
        // 启动传输层
        localTransport.start().join();
        
        // 创建测试消息
        AgentMessage message = AgentMessage.builder()
            .messageId("test-msg")
            .sender(AgentId.create("test-sender"))
            .receiver(AgentId.create("test-receiver"))
            .performative(MessagePerformative.INFORM)
            .content("Test message content")
            .build();
        
        // 发送消息
        localTransport.sendMessage(message).join();
        
        // 验证消息是否正确接收
        assert handler.getReceivedMessages().size() == 1 : "Should receive 1 message";
        assert handler.getReceivedMessages().get(0).equals(message) : "Should receive correct message";
        
        // 测试传输统计
        Transport.TransportStats stats = localTransport.getStats();
        System.out.println("传输统计: " + stats);
        assert stats.messagesSent() == 1 : "Should send 1 message";
        assert stats.running() : "Transport should be running";
        
        // 停止传输层
        localTransport.stop().join();
        
        System.out.println("✓ 传输层功能测试通过");
    }
    
    /**
     * 测试序列化功能
     */
    public void testSerialization() throws com.agentcore.communication.serializer.SerializationException {
        System.out.println("=== 测试序列化功能 ===");
        
        // 创建JSON序列化器
        MessageSerializer jsonSerializer = new JsonMessageSerializer();
        
        // 创建测试消息
        AgentMessage originalMessage = AgentMessage.builder()
            .messageId("serialize-test")
            .sender(AgentId.create("sender-1"))
            .receiver(AgentId.create("receiver-1"))
            .performative(MessagePerformative.INFORM)
            .content(Map.of("key", "value", "number", 123))
            .priority(MessagePriority.HIGH)
            .build();
        
        // 序列化消息
        byte[] serializedData = jsonSerializer.serialize(originalMessage);
        System.out.println("序列化数据大小: " + serializedData.length + " bytes");
        
        // 反序列化消息
        AgentMessage deserializedMessage = jsonSerializer.deserialize(serializedData);
        
        // 验证序列化/反序列化正确性
        assert deserializedMessage.messageId().equals(originalMessage.messageId()) : "Message ID should match";
        assert deserializedMessage.sender().equals(originalMessage.sender()) : "Sender ID should match";
        assert deserializedMessage.receiver().equals(originalMessage.receiver()) : "Receiver ID should match";
        assert deserializedMessage.priority() == originalMessage.priority() : "Priority should match";
        
        // 测试内容序列化
        Map<?, ?> originalContent = (Map<?, ?>) originalMessage.content();
        Map<?, ?> deserializedContent = (Map<?, ?>) deserializedMessage.content();
        assert originalContent.equals(deserializedContent) : "Content should match after serialization";
        
        System.out.println("✓ 序列化功能测试通过");
    }
    
    /**
     * 测试错误处理和重试机制
     */
    public void testErrorHandlingAndRetry() {
        System.out.println("=== 测试错误处理和重试机制 ===");
        
        // 创建带重试机制的路由器
        RetryMessageRouter router = new RetryMessageRouter(3, 100); // 3次重试，100ms间隔
        
        // 创建会失败的消息处理器
        FailingMessageHandler failingHandler = new FailingMessageHandler(2); // 前2次失败
        
        // 注册处理器
        AgentId testAgentId = AgentId.create("test-agent");
        router.registerAgent(testAgentId, failingHandler);
        
        // 创建测试消息
        AgentMessage message = AgentMessage.builder()
            .messageId("retry-test")
            .sender(AgentId.create("sender"))
            .receiver(testAgentId)
            .performative(MessagePerformative.INFORM)
            .content("Retry test message")
            .build();
        
        // 发送消息（应该在第3次重试时成功）
        router.routeMessage(message).join();
        
        // 验证重试机制
        assert failingHandler.getAttemptCount() == 3 : "Should attempt 3 times";
        assert failingHandler.getSuccessfulAttempts() == 1 : "Should succeed on 3rd attempt";
        
        System.out.println("✓ 错误处理和重试机制测试通过");
    }
    
    /**
     * 测试性能基准
     */
    public void testPerformanceBenchmark() {
        System.out.println("=== 测试性能基准 ===");
        
        int messageCount = 1000;
        MessageRouter router = new DefaultMessageRouter();
        PerformanceTestAgent testAgent = new PerformanceTestAgent();
        
        // 注册测试Agent
        router.registerAgent(testAgent.getAgentId(), testAgent::handleMessage);
        
        long startTime = System.currentTimeMillis();
        
        // 发送大量消息
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < messageCount; i++) {
            AgentMessage message = AgentMessage.builder()
                .messageId("perf-msg-" + i)
                .sender(AgentId.create("perf-sender"))
                .receiver(testAgent.getAgentId())
                .performative(MessagePerformative.INFORM)
                .content("Performance test message " + i)
                .build();
                
            futures.add(router.routeMessage(message));
        }
        
        // 等待所有消息完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 计算性能指标
        double messagesPerSecond = (double) messageCount / (duration / 1000.0);
        double averageLatency = (double) duration / messageCount;
        
        System.out.println("性能测试结果:");
        System.out.println("  - 消息数量: " + messageCount);
        System.out.println("  - 总耗时: " + duration + "ms");
        System.out.println("  - 吞吐量: " + String.format("%.2f", messagesPerSecond) + " msg/s");
        System.out.println("  - 平均延迟: " + String.format("%.2f", averageLatency) + " ms/msg");
        
        // 验证所有消息都正确接收
        assert testAgent.getReceivedCount() == messageCount : "Should receive all messages";
        
        System.out.println("✓ 性能基准测试通过");
    }
    
    /**
     * 运行所有通信测试
     */
    public static void main(String[] args) {
        CommunicationTest test = new CommunicationTest();
        
        try {
            test.testMessageRouting();
            test.testTransportLayer();
            test.testSerialization();
            test.testErrorHandlingAndRetry();
            test.testPerformanceBenchmark();
            
            System.out.println("\n🎉 所有通信测试通过！");
        } catch (Exception e) {
            System.err.println("❌ 通信测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ===== 测试辅助类 =====
    
    /**
     * 测试Agent类
     */
    private static class TestAgent {
        private final AgentId agentId;
        private final List<AgentMessage> receivedMessages = new ArrayList<>();
        
        public TestAgent(String agentId) {
            this.agentId = AgentId.create(agentId);
        }
        
        public AgentId getAgentId() {
            return agentId;
        }
        
        public void handleMessage(AgentMessage message) {
            receivedMessages.add(message);
            System.out.println("Agent " + agentId + " received message: " + message.messageId());
        }
        
        public List<AgentMessage> getReceivedMessages() {
            return receivedMessages;
        }
    }
    
    /**
     * 测试消息处理器
     */
    private static class TestMessageHandler implements Transport.MessageHandler {
        private final List<AgentMessage> receivedMessages = new ArrayList<>();
        
        @Override
        public void handleMessage(AgentMessage message) {
            receivedMessages.add(message);
            System.out.println("Transport received message: " + message.messageId());
        }
        
        public List<AgentMessage> getReceivedMessages() {
            return receivedMessages;
        }
    }
    
    /**
     * 会失败的消息处理器（用于测试重试机制）
     */
    private static class FailingMessageHandler implements MessageRouter.MessageHandler {
        private int attemptCount = 0;
        private int successfulAttempts = 0;
        private final int failUntilAttempt;
        
        public FailingMessageHandler(int failUntilAttempt) {
            this.failUntilAttempt = failUntilAttempt;
        }
        
        @Override
        public void handleMessage(AgentMessage message) {
            attemptCount++;
            
            if (attemptCount <= failUntilAttempt) {
                throw new RuntimeException("模拟处理失败 - 尝试次数: " + attemptCount);
            }
            
            successfulAttempts++;
            System.out.println("消息处理成功 - 尝试次数: " + attemptCount);
        }
        
        public int getAttemptCount() {
            return attemptCount;
        }
        
        public int getSuccessfulAttempts() {
            return successfulAttempts;
        }
    }
    
    /**
     * 性能测试Agent
     */
    private static class PerformanceTestAgent {
        private final AgentId agentId = AgentId.create("perf-agent");
        private final AtomicInteger receivedCount = new AtomicInteger(0);
        
        public AgentId getAgentId() {
            return agentId;
        }
        
        public void handleMessage(AgentMessage message) {
            receivedCount.incrementAndGet();
        }
        
        public int getReceivedCount() {
            return receivedCount.get();
        }
    }
    
    // ===== 模拟实现类 =====
    
    /**
     * 默认消息路由器实现
     */
    private static class DefaultMessageRouter implements MessageRouter {
        private final Map<AgentId, MessageHandler> routingTable = new ConcurrentHashMap<>();
        private final AtomicLong messagesRouted = new AtomicLong(0);
        private final AtomicLong routingErrors = new AtomicLong(0);
        
        @Override
        public CompletableFuture<Void> routeMessage(AgentMessage message) {
            return CompletableFuture.runAsync(() -> {
                try {
                    AgentId receiverId = message.receiver();
                    MessageHandler handler = routingTable.get(receiverId);
                    
                    if (handler != null) {
                        handler.handleMessage(message);
                        messagesRouted.incrementAndGet();
                        System.out.println("消息路由成功: " + message.messageId() + " -> " + receiverId);
                    } else {
                        routingErrors.incrementAndGet();
                        throw new RuntimeException("找不到目标Agent: " + receiverId);
                    }
                } catch (Exception e) {
                    routingErrors.incrementAndGet();
                    throw new RuntimeException("消息路由失败: " + e.getMessage(), e);
                }
            });
        }
        
        @Override
        public void registerAgent(AgentId agentId, MessageHandler handler) {
            routingTable.put(agentId, handler);
            System.out.println("注册Agent: " + agentId);
        }
        
        @Override
        public void unregisterAgent(AgentId agentId) {
            routingTable.remove(agentId);
            System.out.println("注销Agent: " + agentId);
        }
        
        @Override
        public boolean isRegistered(AgentId agentId) {
            return routingTable.containsKey(agentId);
        }
        
        @Override
        public int getRegisteredAgentCount() {
            return routingTable.size();
        }
        
        @Override
        public RouterStats getStats() {
            return new RouterStats(
                routingTable.size(),
                messagesRouted.get(),
                routingErrors.get(),
                0.5 // 模拟平均路由时间
            );
        }
    }
    
    /**
     * 带重试机制的消息路由器
     */
    private static class RetryMessageRouter implements MessageRouter {
        private final MessageRouter delegate;
        private final int maxRetries;
        private final long retryInterval;
        
        public RetryMessageRouter(int maxRetries, long retryInterval) {
            this.delegate = new DefaultMessageRouter();
            this.maxRetries = maxRetries;
            this.retryInterval = retryInterval;
        }
        
        @Override
        public CompletableFuture<Void> routeMessage(AgentMessage message) {
            return retry(() -> delegate.routeMessage(message), maxRetries, retryInterval);
        }
        
        @Override
        public void registerAgent(AgentId agentId, MessageHandler handler) {
            delegate.registerAgent(agentId, handler);
        }
        
        @Override
        public void unregisterAgent(AgentId agentId) {
            delegate.unregisterAgent(agentId);
        }
        
        @Override
        public boolean isRegistered(AgentId agentId) {
            return delegate.isRegistered(agentId);
        }
        
        @Override
        public int getRegisteredAgentCount() {
            return delegate.getRegisteredAgentCount();
        }
        
        @Override
        public RouterStats getStats() {
            return delegate.getStats();
        }
        
        private <T> CompletableFuture<T> retry(Supplier<CompletableFuture<T>> operation, 
                                              int remainingRetries, long interval) {
            return operation.get().handle((result, error) -> {
                if (error != null && remainingRetries > 0) {
                    System.out.println("路由失败，剩余重试次数: " + remainingRetries + ", 等待 " + interval + "ms 后重试");
                    
                    try {
                        Thread.sleep(interval);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("重试被中断", e);
                    }
                    
                    return retry(operation, remainingRetries - 1, interval);
                } else if (error != null) {
                    throw new RuntimeException("重试次数耗尽: " + error.getMessage(), error);
                } else {
                    return CompletableFuture.completedFuture(result);
                }
            }).thenCompose(future -> future);
        }
    }
    
    /**
     * 本地传输层实现
     */
    private static class LocalTransport implements Transport {
        private volatile MessageHandler messageHandler;
        private volatile boolean running = false;
        private final AtomicLong messagesSent = new AtomicLong(0);
        private final AtomicLong messagesReceived = new AtomicLong(0);
        
        @Override
        public CompletableFuture<Void> start() {
            return CompletableFuture.runAsync(() -> {
                running = true;
                System.out.println("本地传输层已启动");
            });
        }
        
        @Override
        public CompletableFuture<Void> stop() {
            return CompletableFuture.runAsync(() -> {
                running = false;
                System.out.println("本地传输层已停止");
            });
        }
        
        @Override
        public CompletableFuture<Void> sendMessage(AgentMessage message) {
            return CompletableFuture.runAsync(() -> {
                if (!running) {
                    throw new IllegalStateException("传输层未运行");
                }
                
                messagesSent.incrementAndGet();
                
                // 模拟网络延迟
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                if (messageHandler != null) {
                    messageHandler.handleMessage(message);
                    messagesReceived.incrementAndGet();
                }
            });
        }
        
        @Override
        public void setMessageHandler(MessageHandler handler) {
            this.messageHandler = handler;
        }
        
        @Override
        public String getName() {
            return "local-transport";
        }
        
        @Override
        public TransportType getType() {
            return TransportType.LOCAL;
        }
        
        @Override
        public boolean isRunning() {
            return running;
        }
        
        @Override
        public boolean supports(AgentId agentId) {
            return true; // 本地传输支持所有Agent
        }
        
        @Override
        public TransportConfig getConfig() {
            DefaultTransportConfig transportConfig = new DefaultTransportConfig(getName(), getType(), "localhost", 12345);
            return transportConfig;
        }
        
        @Override
        public TransportStats getStats() {
            return new TransportStats(
                getName(),
                getType(),
                running,
                messagesSent.get(),
                messagesReceived.get(),
                0, 0, 0, 1.0 // 模拟统计信息
            );
        }
    }
    
    /**
     * JSON消息序列化器
     */
    private static class JsonMessageSerializer implements MessageSerializer {
        //private final ObjectMapper objectMapper = new ObjectMapper();

        // 注册JavaTimeModule以支持LocalDateTime等Java 8日期时间类型
        private final ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())  // 支持Java 8日期时间类型
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)  // 禁用时间戳格式
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);  // 忽略未知字段


        @Override
        public byte[] serialize(AgentMessage message) throws com.agentcore.communication.serializer.SerializationException {
            try {
                return objectMapper.writeValueAsBytes(message);
            } catch (Exception e) {
                throw new com.agentcore.communication.serializer.SerializationException("JSON序列化失败", e);
            }
        }
        
        @Override
        public AgentMessage deserialize(byte[] data) throws com.agentcore.communication.serializer.SerializationException {
            try {
                return objectMapper.readValue(data, AgentMessage.class);
            } catch (Exception e) {
                throw new com.agentcore.communication.serializer.SerializationException("JSON反序列化失败", e);
            }
        }
        
        @Override
        public String getName() {
            return "json-serializer";
        }
        
        @Override
        public String getVersion() {
            return "1.0";
        }
        
        @Override
        public String getContentType() {
            return "application/json";
        }
        
        @Override
        public int getPriority() {
            return 10; // JSON序列化器优先级
        }
    }
    
    /**
     * 序列化异常类
     */
    private static class SerializationException extends RuntimeException {
        public SerializationException(String message) {
            super(message);
        }
        
        public SerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
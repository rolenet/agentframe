package com.agentcore.examples;

import com.agentcore.container.AgentContainer;
import com.agentcore.container.ContainerConfig;
import com.agentcore.container.DefaultAgentContainer;
import com.agentcore.core.agent.AbstractAgent;
import com.agentcore.core.agent.Agent;
import com.agentcore.core.agent.AgentId;
import com.agentcore.core.message.AgentMessage;
import com.agentcore.core.message.MessagePerformative;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能压力测试
 * 测试容器在高负载下的性能和稳定性
 * 
 * @author AgentCore Team
 */
public class PerformanceStressTest {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceStressTest.class);

    public static void main(String[] args) {
        logger.info("=== 开始性能压力测试 ===");

        try {
            // 测试1: 高并发消息处理
            testHighConcurrencyMessageProcessing();
            
            // 测试2: 大规模Agent创建
            testMassiveAgentCreation();
            
            // 测试3: 内存和资源管理
            testMemoryAndResourceManagement();
            
            // 测试4: 长时间运行稳定性
            testLongRunningStability();
            
            // 测试5: 混合负载测试
            testMixedWorkload();

        } catch (Exception e) {
            logger.error("性能压力测试过程中发生错误", e);
        }

        logger.info("=== 性能压力测试完成 ===");
    }

    /**
     * 测试1: 高并发消息处理
     */
    private static void testHighConcurrencyMessageProcessing() throws Exception {
        logger.info("=== 测试1: 高并发消息处理 ===");

        ContainerConfig config = ContainerConfig.builder("HighConcurrencyContainer")
            .maxAgents(100)
            .agentStartTimeout(Duration.ofSeconds(30))
            .agentStopTimeout(Duration.ofSeconds(10))
            .autoStartAgents(true)
            .build();

        AgentContainer container = new DefaultAgentContainer(config);
        container.start().join();

        // 创建消息处理器Agent
        AgentId processorId = AgentId.create("HighLoadProcessor");
        HighLoadProcessor processor = container.createAgent(HighLoadProcessor.class, processorId);

        // 创建多个消息发送者
        List<MessageSenderAgent> senders = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            AgentId senderId = AgentId.create("Sender-" + i);
            MessageSenderAgent sender = container.createAgent(MessageSenderAgent.class, senderId);
            sender.setTargetAgent(processor);
            senders.add(sender);
        }

        // 等待所有Agent启动
        Thread.sleep(2000);

        // 开始高并发消息发送
        logger.info("开始高并发消息发送测试...");
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(senders.size());

        for (MessageSenderAgent sender : senders) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    sender.startHighFrequencySending();
                    finishLatch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }

        // 同时开始所有发送者
        startLatch.countDown();
        
        // 运行测试30秒
        boolean allFinished = finishLatch.await(30, TimeUnit.SECONDS);
        
        // 收集统计信息
        long totalMessagesSent = senders.stream()
            .mapToLong(MessageSenderAgent::getMessagesSent)
            .sum();
        long totalMessagesProcessed = processor.getMessagesProcessed();
        
        logger.info("高并发测试结果:");
        logger.info("总发送消息数: {}", totalMessagesSent);
        logger.info("总处理消息数: {}", totalMessagesProcessed);
        logger.info("消息处理率: {:.2f}%", 
            (double) totalMessagesProcessed / totalMessagesSent * 100);
        logger.info("平均每秒消息数: {:.2f}", 
            (double) totalMessagesProcessed / 30);

        container.stop().join();
        logger.info("高并发消息处理测试完成");
    }

    /**
     * 测试2: 大规模Agent创建
     */
    private static void testMassiveAgentCreation() throws Exception {
        logger.info("=== 测试2: 大规模Agent创建 ===");

        ContainerConfig config = ContainerConfig.builder("MassiveAgentContainer")
            .maxAgents(1000)
            .agentStartTimeout(Duration.ofSeconds(60))
            .agentStopTimeout(Duration.ofSeconds(20))
            .autoStartAgents(true)
            .build();

        AgentContainer container = new DefaultAgentContainer(config);
        container.start().join();

        // 测试不同规模的Agent创建
        int[] agentCounts = {100, 300, 500};
        
        for (int targetCount : agentCounts) {
            logger.info("创建 {} 个Agent...", targetCount);
            
            long startTime = System.currentTimeMillis();
            List<LightweightAgent> agents = new ArrayList<>();
            
            for (int i = 1; i <= targetCount; i++) {
                AgentId agentId = AgentId.create("LightAgent-" + targetCount + "-" + i);
                LightweightAgent agent = container.createAgent(LightweightAgent.class, agentId);
                agents.add(agent);
            }
            
            long endTime = System.currentTimeMillis();
            long creationTime = endTime - startTime;
            
            logger.info("创建 {} 个Agent耗时: {}ms", targetCount, creationTime);
            logger.info("平均每个Agent创建时间: {:.2f}ms", 
                (double) creationTime / targetCount);
            
            // 验证创建结果
            int actualCount = container.getAgentCount();
            logger.info("容器中实际Agent数量: {}", actualCount);
            
            // 等待一段时间让Agent稳定运行
            Thread.sleep(2000);
            
            // 清理部分Agent进行下一轮测试
            if (targetCount < 500) {
                for (int i = 0; i < targetCount / 2; i++) {
                    container.removeAgent(agents.get(i).getAgentId());
                }
                logger.info("清理部分Agent，准备下一轮测试");
                Thread.sleep(1000);
            }
        }

        container.stop().join();
        logger.info("大规模Agent创建测试完成");
    }

    /**
     * 测试3: 内存和资源管理
     */
    private static void testMemoryAndResourceManagement() throws Exception {
        logger.info("=== 测试3: 内存和资源管理 ===");

        ContainerConfig config = ContainerConfig.builder("ResourceTestContainer")
            .maxAgents(50)
            .autoStartAgents(true)
            .build();

        AgentContainer container = new DefaultAgentContainer(config);
        container.start().join();

        // 创建资源密集型Agent
        List<ResourceIntensiveAgent> resourceAgents = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            AgentId agentId = AgentId.create("ResourceAgent-" + i);
            ResourceIntensiveAgent agent = container.createAgent(ResourceIntensiveAgent.class, agentId);
            resourceAgents.add(agent);
        }

        // 监控内存使用情况
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        logger.info("初始内存使用: {} MB", initialMemory / (1024 * 1024));

        // 运行资源密集型任务
        for (ResourceIntensiveAgent agent : resourceAgents) {
            agent.startResourceIntensiveTask();
        }

        // 监控内存变化
        Thread.sleep(5000);
        
        long currentMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = currentMemory - initialMemory;
        
        logger.info("当前内存使用: {} MB", currentMemory / (1024 * 1024));
        logger.info("内存增加量: {} MB", memoryIncrease / (1024 * 1024));

        // 停止资源密集型任务
        for (ResourceIntensiveAgent agent : resourceAgents) {
            agent.stopResourceIntensiveTask();
        }

        // 强制垃圾回收并检查内存回收
        System.gc();
        Thread.sleep(2000);
        
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryRecovered = currentMemory - finalMemory;
        
        logger.info("最终内存使用: {} MB", finalMemory / (1024 * 1024));
        logger.info("内存回收量: {} MB", memoryRecovered / (1024 * 1024));

        container.stop().join();
        logger.info("内存和资源管理测试完成");
    }

    /**
     * 测试4: 长时间运行稳定性
     */
    private static void testLongRunningStability() throws Exception {
        logger.info("=== 测试4: 长时间运行稳定性 ===");

        ContainerConfig config = ContainerConfig.builder("StabilityTestContainer")
            .maxAgents(20)
            .autoStartAgents(true)
            .build();

        AgentContainer container = new DefaultAgentContainer(config);
        container.start().join();

        // 创建稳定性测试Agent
        AgentId stabilityId = AgentId.create("StabilityMonitor");
        StabilityMonitorAgent monitor = container.createAgent(StabilityMonitorAgent.class, stabilityId);

        // 运行长时间测试（2分钟）
        logger.info("开始长时间运行稳定性测试（2分钟）...");
        monitor.startLongRunningTest();
        
        // 每30秒输出一次状态
        for (int minute = 1; minute <= 2; minute++) {
            Thread.sleep(30000);
            logger.info("运行 {} 分钟后的状态:", minute);
            logger.info("容器统计: {}", container.getStats());
            logger.info("监控Agent状态: 活跃={}, 处理消息数={}", 
                monitor.isActive(), monitor.getProcessedMessages());
        }

        monitor.stopLongRunningTest();
        container.stop().join();
        logger.info("长时间运行稳定性测试完成");
    }

    /**
     * 测试5: 混合负载测试
     */
    private static void testMixedWorkload() throws Exception {
        logger.info("=== 测试5: 混合负载测试 ===");

        ContainerConfig config = ContainerConfig.builder("MixedWorkloadContainer")
            .maxAgents(100)
            .autoStartAgents(true)
            .build();

        AgentContainer container = new DefaultAgentContainer(config);
        container.start().join();

        // 创建不同类型的Agent模拟混合负载
        ExecutorService executor = Executors.newFixedThreadPool(5);
        
        // CPU密集型Agent
        List<CpuIntensiveAgent> cpuAgents = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            AgentId agentId = AgentId.create("CpuAgent-" + i);
            CpuIntensiveAgent agent = container.createAgent(CpuIntensiveAgent.class, agentId);
            cpuAgents.add(agent);
            executor.submit(agent::startCpuIntensiveTask);
        }

        // IO密集型Agent
        List<IoIntensiveAgent> ioAgents = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            AgentId agentId = AgentId.create("IoAgent-" + i);
            IoIntensiveAgent agent = container.createAgent(IoIntensiveAgent.class, agentId);
            ioAgents.add(agent);
            executor.submit(agent::startIoIntensiveTask);
        }

        // 内存密集型Agent
        List<MemoryIntensiveAgent> memoryAgents = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            AgentId agentId = AgentId.create("MemoryAgent-" + i);
            MemoryIntensiveAgent agent = container.createAgent(MemoryIntensiveAgent.class, agentId);
            memoryAgents.add(agent);
            executor.submit(agent::startMemoryIntensiveTask);
        }

        // 网络密集型Agent（模拟）
        List<NetworkIntensiveAgent> networkAgents = new ArrayList<>();
        for (int i = 1; i <= 2; i++) {
            AgentId agentId = AgentId.create("NetworkAgent-" + i);
            NetworkIntensiveAgent agent = container.createAgent(NetworkIntensiveAgent.class, agentId);
            networkAgents.add(agent);
            executor.submit(agent::startNetworkIntensiveTask);
        }

        logger.info("混合负载测试运行中...");
        Thread.sleep(30000); // 运行30秒

        // 停止所有任务
        cpuAgents.forEach(CpuIntensiveAgent::stopCpuIntensiveTask);
        ioAgents.forEach(IoIntensiveAgent::stopIoIntensiveTask);
        memoryAgents.forEach(MemoryIntensiveAgent::stopMemoryIntensiveTask);
        networkAgents.forEach(NetworkIntensiveAgent::stopNetworkIntensiveTask);
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // 输出统计信息
        logger.info("混合负载测试结果:");
        logger.info("CPU密集型任务完成数: {}", 
            cpuAgents.stream().mapToInt(CpuIntensiveAgent::getTasksCompleted).sum());
        logger.info("IO密集型任务完成数: {}", 
            ioAgents.stream().mapToInt(IoIntensiveAgent::getOperationsCompleted).sum());
        logger.info("内存密集型任务完成数: {}", 
            memoryAgents.stream().mapToInt(MemoryIntensiveAgent::getMemoryOperations).sum());
        logger.info("网络密集型任务完成数: {}", 
            networkAgents.stream().mapToInt(NetworkIntensiveAgent::getNetworkOperations).sum());
        
        logger.info("容器最终统计: {}", container.getStats());

        container.stop().join();
        logger.info("混合负载测试完成");
    }

    // ===== 性能压力测试Agent实现 =====

    /**
     * 高负载消息处理器
     */
    public static class HighLoadProcessor extends AbstractAgent {
        private static final Logger logger = LoggerFactory.getLogger(HighLoadProcessor.class);
        private final AtomicLong messagesProcessed = new AtomicLong(0);

        public HighLoadProcessor(AgentId agentId) {
            super(agentId);
        }

        @Override
        protected com.agentcore.core.behavior.BehaviorScheduler createBehaviorScheduler() {
            return new com.agentcore.core.behavior.DefaultBehaviorScheduler();
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected void doStart() {
            logger.info("高负载处理器 {} 启动", getAgentId().getShortId());
        }

        @Override
        protected void doHandleMessage(AgentMessage message) {
            messagesProcessed.incrementAndGet();
            
            // 模拟消息处理
            if (messagesProcessed.get() % 1000 == 0) {
                logger.debug("处理器 {} 已处理 {} 条消息", 
                    getAgentId().getShortId(), messagesProcessed.get());
            }
        }

        public long getMessagesProcessed() {
            return messagesProcessed.get();
        }
    }

    /**
     * 消息发送者Agent
     */
    public static class MessageSenderAgent extends AbstractAgent {
        private static final Logger logger = LoggerFactory.getLogger(MessageSenderAgent.class);
        private Agent targetAgent;
        private final AtomicLong messagesSent = new AtomicLong(0);
        private volatile boolean sending = false;

        public MessageSenderAgent(AgentId agentId) {
            super(agentId);
        }

        public void setTargetAgent(Agent target) {
            this.targetAgent = target;
        }

        @Override
        protected com.agentcore.core.behavior.BehaviorScheduler createBehaviorScheduler() {
            return new com.agentcore.core.behavior.DefaultBehaviorScheduler();
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected void doStart() {
            logger.info("消息发送者 {} 启动", getAgentId().getShortId());
        }

        public void startHighFrequencySending() {
            sending = true;
            new Thread(() -> {
                while (sending && messagesSent.get() < 10000) {
                    if (targetAgent != null) {
                        AgentMessage message = AgentMessage.builder()
                            .sender(getAgentId())
                            .receiver(targetAgent.getAgentId())
                            .performative(MessagePerformative.INFORM)
                            .content("高频消息 #" + messagesSent.incrementAndGet())
                            .build();
                        sendMessage(message);
                    }
                    
                    try {
                        Thread.sleep(1); // 高频率发送
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                logger.info("发送者 {} 完成发送，共发送 {} 条消息", 
                    getAgentId().getShortId(), messagesSent.get());
            }).start();
        }

        public void stopHighFrequencySending() {
            sending = false;
        }

        public long getMessagesSent() {
            return messagesSent.get();
        }
    }

    /**
     * 轻量级Agent - 用于大规模创建测试
     */
    public static class LightweightAgent extends AbstractAgent {
        public LightweightAgent(AgentId agentId) {
            super(agentId);
        }

        @Override
        protected com.agentcore.core.behavior.BehaviorScheduler createBehaviorScheduler() {
            return new com.agentcore.core.behavior.DefaultBehaviorScheduler();
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected void doStart() {
            // 轻量级启动，最小化资源占用
        }

        @Override
        public String getType() {
            return "lightweight";
        }
    }

    /**
     * 资源密集型Agent
     */
    public static class ResourceIntensiveAgent extends AbstractAgent {
        private static final Logger logger = LoggerFactory.getLogger(ResourceIntensiveAgent.class);
        private volatile boolean running = false;
        private final List<byte[]> memoryChunks = new ArrayList<>();

        public ResourceIntensiveAgent(AgentId agentId) {
            super(agentId);
        }

        @Override
        protected com.agentcore.core.behavior.BehaviorScheduler createBehaviorScheduler() {
            return new com.agentcore.core.behavior.DefaultBehaviorScheduler();
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            return CompletableFuture.completedFuture(null);
        }

        public void startResourceIntensiveTask() {
            running = true;
            new Thread(() -> {
                while (running) {
                    // 分配内存块（每块1MB）
                    byte[] chunk = new byte[1024 * 1024];
                    memoryChunks.add(chunk);
                    
                    // 模拟CPU计算
                    long result = 0;
                    for (int i = 0; i < 1000000; i++) {
                        result += i * i;
                    }
                    
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }).start();
            logger.info("资源密集型任务启动: {}", getAgentId().getShortId());
        }

        public void stopResourceIntensiveTask() {
            running = false;
            memoryChunks.clear();
            logger.info("资源密集型任务停止: {}", getAgentId().getShortId());
        }
    }

    /**
     * 稳定性监控Agent
     */
    public static class StabilityMonitorAgent extends AbstractAgent {
        private static final Logger logger = LoggerFactory.getLogger(StabilityMonitorAgent.class);
        private volatile boolean monitoring = false;
        private final AtomicLong processedMessages = new AtomicLong(0);

        public StabilityMonitorAgent(AgentId agentId) {
            super(agentId);
        }

        @Override
        protected com.agentcore.core.behavior.BehaviorScheduler createBehaviorScheduler() {
            return new com.agentcore.core.behavior.DefaultBehaviorScheduler();
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            return CompletableFuture.completedFuture(null);
        }

        public void startLongRunningTest() {
            monitoring = true;
            new Thread(() -> {
                long startTime = System.currentTimeMillis();
                while (monitoring && (System.currentTimeMillis() - startTime) < 120000) {
                    // 定期处理测试消息
                    processedMessages.incrementAndGet();
                    
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }).start();
        }

        public void stopLongRunningTest() {
            monitoring = false;
        }

        public long getProcessedMessages() {
            return processedMessages.get();
        }
    }

    // ===== 混合负载测试Agent实现 =====

    public static class CpuIntensiveAgent extends AbstractAgent {
        private volatile boolean running = false;
        private final AtomicInteger tasksCompleted = new AtomicInteger(0);

        public CpuIntensiveAgent(AgentId agentId) {
            super(agentId);
        }

        @Override
        protected com.agentcore.core.behavior.BehaviorScheduler createBehaviorScheduler() {
            return new com.agentcore.core.behavior.DefaultBehaviorScheduler();
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            return CompletableFuture.completedFuture(null);
        }

        public void startCpuIntensiveTask() {
            running = true;
            new Thread(() -> {
                while (running) {
                    // CPU密集型计算
                    long sum = 0;
                    for (long i = 0; i < 1000000L; i++) {
                        sum += i * i;
                    }
                    tasksCompleted.incrementAndGet();
                }
            }).start();
        }

        public void stopCpuIntensiveTask() {
            running = false;
        }

        public int getTasksCompleted() {
            return tasksCompleted.get();
        }
    }

    public static class IoIntensiveAgent extends AbstractAgent {
        private volatile boolean running = false;
        private final AtomicInteger operationsCompleted = new AtomicInteger(0);

        public IoIntensiveAgent(AgentId agentId) {
            super(agentId);
        }

        @Override
        protected com.agentcore.core.behavior.BehaviorScheduler createBehaviorScheduler() {
            return new com.agentcore.core.behavior.DefaultBehaviorScheduler();
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            return CompletableFuture.completedFuture(null);
        }

        public void startIoIntensiveTask() {
            running = true;
            new Thread(() -> {
                while (running) {
                    // 模拟IO操作（休眠）
                    try {
                        Thread.sleep(50);
                        operationsCompleted.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }).start();
        }

        public void stopIoIntensiveTask() {
            running = false;
        }

        public int getOperationsCompleted() {
            return operationsCompleted.get();
        }
    }

    public static class MemoryIntensiveAgent extends AbstractAgent {
        private volatile boolean running = false;
        private final AtomicInteger memoryOperations = new AtomicInteger(0);
        private final List<byte[]> memoryBlocks = new ArrayList<>();

        public MemoryIntensiveAgent(AgentId agentId) {
            super(agentId);
        }

        @Override
        protected com.agentcore.core.behavior.BehaviorScheduler createBehaviorScheduler() {
            return new com.agentcore.core.behavior.DefaultBehaviorScheduler();
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            return CompletableFuture.completedFuture(null);
        }

        public void startMemoryIntensiveTask() {
            running = true;
            new Thread(() -> {
                while (running) {
                    // 内存分配和释放
                    byte[] block = new byte[1024 * 512]; // 512KB
                    memoryBlocks.add(block);
                    memoryOperations.incrementAndGet();
                    
                    if (memoryBlocks.size() > 10) {
                        memoryBlocks.remove(0); // 释放旧内存
                    }
                    
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                memoryBlocks.clear();
            }).start();
        }

        public void stopMemoryIntensiveTask() {
            running = false;
        }

        public int getMemoryOperations() {
            return memoryOperations.get();
        }
    }

    public static class NetworkIntensiveAgent extends AbstractAgent {
        private volatile boolean running = false;
        private final AtomicInteger networkOperations = new AtomicInteger(0);

        public NetworkIntensiveAgent(AgentId agentId) {
            super(agentId);
        }

        @Override
        protected com.agentcore.core.behavior.BehaviorScheduler createBehaviorScheduler() {
            return new com.agentcore.core.behavior.DefaultBehaviorScheduler();
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            return CompletableFuture.completedFuture(null);
        }

        public void startNetworkIntensiveTask() {
            running = true;
            new Thread(() -> {
                while (running) {
                    // 模拟网络操作
                    networkOperations.incrementAndGet();
                    
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }).start();
        }

        public void stopNetworkIntensiveTask() {
            running = false;
        }

        public int getNetworkOperations() {
            return networkOperations.get();
        }
    }
}
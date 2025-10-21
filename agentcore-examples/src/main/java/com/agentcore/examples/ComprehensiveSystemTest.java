package com.agentcore.examples;

import com.agentcore.communication.router.LocalMessageRouter;
import com.agentcore.communication.router.MessageRouter;
import com.agentcore.communication.serializer.JacksonMessageSerializer;
import com.agentcore.communication.serializer.MessageSerializer;
import com.agentcore.communication.transport.LocalTransport;
import com.agentcore.communication.transport.Transport;
import com.agentcore.container.AgentContainer;
import com.agentcore.container.ContainerConfig;
import com.agentcore.container.DefaultAgentContainer;
import com.agentcore.core.agent.AbstractAgent;
import com.agentcore.core.agent.Agent;
import com.agentcore.core.agent.AgentId;
import com.agentcore.core.agent.AgentState;
import com.agentcore.core.behavior.BehaviorScheduler;
import com.agentcore.core.behavior.DefaultBehaviorScheduler;
import com.agentcore.core.message.AgentMessage;
import com.agentcore.core.message.MessagePerformative;
import com.agentcore.core.message.MessagePriority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AgentCore框架综合系统测试类
 * 
 * 该测试类展示了AgentCore框架所有核心模块的协同工作：
 * - Agent：智能体的创建、生命周期管理和行为执行
 * - AgentId：智能体身份标识和寻址
 * - Message：智能体间的消息通信和协议
 * - Container：智能体容器的管理和监控
 * - Communication：消息路由、传输和序列化
 * 
 * 测试场景包括：
 * 1. 多智能体协作任务处理
 * 2. 分布式消息路由和通信
 * 3. 容器生命周期和资源管理
 * 4. 错误处理和故障恢复
 * 5. 性能监控和统计分析
 * 
 * @author AgentCore Team
 */
public class ComprehensiveSystemTest {

    private static final Logger logger = LoggerFactory.getLogger(ComprehensiveSystemTest.class);
    
    // 测试统计
    private static final AtomicInteger totalMessages = new AtomicInteger(0);
    private static final AtomicLong totalProcessingTime = new AtomicLong(0);
    private static final ConcurrentHashMap<String, Integer> agentMessageCounts = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        logger.info("=== AgentCore框架综合系统测试开始 ===");
        
        ComprehensiveSystemTest test = new ComprehensiveSystemTest();
        
        try {
            // 执行所有测试场景
            test.runAllSystemTests();
            
            // 输出测试总结
            test.printTestSummary();
            
        } catch (Exception e) {
            logger.error("综合系统测试过程中发生错误", e);
        }
        
        logger.info("=== AgentCore框架综合系统测试完成 ===");
    }

    /**
     * 运行所有系统测试
     */
    public void runAllSystemTests() throws Exception {
        logger.info("开始执行综合系统测试...");
        
        // 测试1: 基础框架集成测试
        testBasicFrameworkIntegration();
        
        // 测试2: 多智能体协作场景测试
        testMultiAgentCollaborationScenario();
        
        // 测试3: 分布式通信系统测试
        testDistributedCommunicationSystem();
        
        // 测试4: 容器管理和监控测试
        testContainerManagementAndMonitoring();
        
        // 测试5: 错误处理和恢复机制测试
        testErrorHandlingAndRecovery();
        
        // 测试6: 性能压力测试
        testPerformanceAndStress();
        
        logger.info("所有综合系统测试执行完成");
    }

    /**
     * 测试1: 基础框架集成测试
     * 验证所有核心组件能够正确初始化和协同工作
     */
    private void testBasicFrameworkIntegration() throws Exception {
        logger.info("--- 测试1: 基础框架集成测试 ---");
        
        // 创建容器配置
        ContainerConfig config = ContainerConfig.builder("IntegrationTestContainer")
            .maxAgents(10)
            .agentStartTimeout(Duration.ofSeconds(5))
            .autoStartAgents(true)
            .enableMonitoring(true)
            .property("test.scenario", "integration")
            .build();
        
        // 创建容器
        AgentContainer container = new DefaultAgentContainer(config);
        
        // 创建通信组件
        MessageSerializer serializer = new JacksonMessageSerializer();
        Transport transport = new LocalTransport();
        MessageRouter router = new LocalMessageRouter();
        
        // 设置通信链路
        transport.setMessageHandler(router::routeMessage);
        transport.start().join();
        
        // 启动容器
        container.start().join();
        
        // 创建测试智能体
        AgentId coordinatorId = AgentId.createLocal("Coordinator", "coordinator");
        AgentId worker1Id = AgentId.createLocal("Worker1", "worker");
        AgentId worker2Id = AgentId.createLocal("Worker2", "worker");
        
        CoordinatorAgent coordinator = container.createAgent(CoordinatorAgent.class, coordinatorId);
        WorkerAgent worker1 = container.createAgent(WorkerAgent.class, worker1Id);
        WorkerAgent worker2 = container.createAgent(WorkerAgent.class, worker2Id);
        
        // 注册智能体到路由器
        router.registerAgent(coordinatorId, coordinator::handleMessage);
        router.registerAgent(worker1Id, worker1::handleMessage);
        router.registerAgent(worker2Id, worker2::handleMessage);
        
        // 等待智能体启动
        Thread.sleep(2000);
        
        // 验证集成状态
        assert container.getAgentCount() == 3 : "应该有3个智能体";
        assert container.isRunning() : "容器应该正在运行";
        assert transport.isRunning() : "传输层应该正在运行";
        
        // 测试基础消息通信
        AgentMessage testMessage = AgentMessage.builder()
            .sender(coordinatorId)
            .receiver(worker1Id)
            .performative(MessagePerformative.REQUEST)
            .content("集成测试消息")
            .priority(MessagePriority.HIGH)
            .build();
        
        router.routeMessage(testMessage).join();
        Thread.sleep(1000);
        
        // 验证消息处理
        assert worker1.getProcessedMessageCount() > 0 : "Worker1应该处理了消息";
        
        // 清理资源
        container.stop().join();
        transport.stop().join();
        
        logger.info("✓ 基础框架集成测试通过");
    }

    /**
     * 测试2: 多智能体协作场景测试
     * 模拟一个完整的任务分发和协作处理场景
     */
    private void testMultiAgentCollaborationScenario() throws Exception {
        logger.info("--- 测试2: 多智能体协作场景测试 ---");
        
        // 创建协作场景容器
        ContainerConfig config = ContainerConfig.builder("CollaborationContainer")
            .maxAgents(15)
            .agentStartTimeout(Duration.ofSeconds(10))
            .autoStartAgents(true)
            .enableMonitoring(true)
            .property("scenario", "collaboration")
            .build();
        
        AgentContainer container = new DefaultAgentContainer(config);
        MessageRouter router = new LocalMessageRouter();
        Transport transport = new LocalTransport();
        
        transport.setMessageHandler(router::routeMessage);
        transport.start().join();
        container.start().join();
        
        // 创建协作智能体群
        TaskManagerAgent taskManager = container.createAgent(TaskManagerAgent.class, 
            AgentId.createLocal("TaskManager", "manager"));
        
        ResourceManagerAgent resourceManager = container.createAgent(ResourceManagerAgent.class,
            AgentId.createLocal("ResourceManager", "resource"));
        
        MonitorAgent monitor = container.createAgent(MonitorAgent.class,
            AgentId.createLocal("Monitor", "monitor"));
        
        // 创建工作智能体群
        ProcessorAgent[] processors = new ProcessorAgent[5];
        for (int i = 0; i < 5; i++) {
            AgentId processorId = AgentId.createLocal("Processor" + (i + 1), "processor");
            processors[i] = container.createAgent(ProcessorAgent.class, processorId);
            router.registerAgent(processorId, processors[i]::handleMessage);
        }
        
        // 注册管理智能体
        router.registerAgent(taskManager.getAgentId(), taskManager::handleMessage);
        router.registerAgent(resourceManager.getAgentId(), resourceManager::handleMessage);
        router.registerAgent(monitor.getAgentId(), monitor::handleMessage);
        
        // 设置协作关系
        taskManager.setResourceManager(resourceManager);
        taskManager.setProcessors(List.of(processors));
        taskManager.setMonitor(monitor);
        
        // 等待智能体完全启动
        Thread.sleep(3000);
        
        // 启动协作任务
        CountDownLatch taskCompletion = new CountDownLatch(1);
        taskManager.startCollaborativeTask("大规模数据处理任务", taskCompletion);
        
        // 等待任务完成
        boolean completed = taskCompletion.await(30, TimeUnit.SECONDS);
        assert completed : "协作任务应该在30秒内完成";
        
        // 验证协作结果
        assert taskManager.getCompletedTasks() > 0 : "应该完成了任务";
        assert resourceManager.getAllocatedResources() > 0 : "应该分配了资源";
        
        int totalProcessed = 0;
        for (ProcessorAgent processor : processors) {
            totalProcessed += processor.getProcessedTasks();
        }
        assert totalProcessed > 0 : "处理器应该处理了任务";
        
        // 清理资源
        container.stop().join();
        transport.stop().join();
        
        logger.info("✓ 多智能体协作场景测试通过 - 处理任务数: {}", totalProcessed);
    }

    /**
     * 测试3: 分布式通信系统测试
     * 测试复杂的消息路由、序列化和传输机制
     */
    private void testDistributedCommunicationSystem() throws Exception {
        logger.info("--- 测试3: 分布式通信系统测试 ---");
        
        // 创建多个容器模拟分布式环境
        AgentContainer container1 = new DefaultAgentContainer(
            ContainerConfig.builder("Node1").maxAgents(5).build());
        AgentContainer container2 = new DefaultAgentContainer(
            ContainerConfig.builder("Node2").maxAgents(5).build());
        
        // 创建通信基础设施
        MessageSerializer serializer = new JacksonMessageSerializer();
        MessageRouter router1 = new LocalMessageRouter();
        MessageRouter router2 = new LocalMessageRouter();
        
        Transport transport1 = new LocalTransport();
        Transport transport2 = new LocalTransport();
        
        // 启动基础设施
        transport1.setMessageHandler(router1::routeMessage);
        transport2.setMessageHandler(router2::routeMessage);
        
        CompletableFuture.allOf(
            container1.start(),
            container2.start(),
            transport1.start(),
            transport2.start()
        ).join();
        
        // 创建分布式智能体
        CommunicationNodeAgent node1 = container1.createAgent(CommunicationNodeAgent.class,
            AgentId.createLocal("Node1Agent", "node"));
        CommunicationNodeAgent node2 = container2.createAgent(CommunicationNodeAgent.class,
            AgentId.createLocal("Node2Agent", "node"));
        
        GatewayAgent gateway1 = container1.createAgent(GatewayAgent.class,
            AgentId.createLocal("Gateway1", "gateway"));
        GatewayAgent gateway2 = container2.createAgent(GatewayAgent.class,
            AgentId.createLocal("Gateway2", "gateway"));
        
        // 注册智能体到各自的路由器
        router1.registerAgent(node1.getAgentId(), node1::handleMessage);
        router1.registerAgent(gateway1.getAgentId(), gateway1::handleMessage);
        router2.registerAgent(node2.getAgentId(), node2::handleMessage);
        router2.registerAgent(gateway2.getAgentId(), gateway2::handleMessage);
        
        // 建立跨容器通信链路
        gateway1.setRemoteGateway(gateway2);
        gateway2.setRemoteGateway(gateway1);
        
        // 等待网络建立
        Thread.sleep(2000);
        
        // 测试跨节点消息传输
        int messageCount = 20;
        CountDownLatch messageLatch = new CountDownLatch(messageCount);
        
        node2.setMessageLatch(messageLatch);
        
        // 从node1向node2发送消息
        for (int i = 0; i < messageCount; i++) {
            AgentMessage crossNodeMessage = AgentMessage.builder()
                .sender(node1.getAgentId())
                .receiver(node2.getAgentId())
                .performative(MessagePerformative.INFORM)
                .content("跨节点消息 #" + (i + 1))
                .priority(MessagePriority.NORMAL)
                .build();
            
            // 通过gateway1路由到gateway2再到node2
            gateway1.routeMessage(crossNodeMessage);
        }
        
        // 等待所有消息处理完成
        boolean allReceived = messageLatch.await(15, TimeUnit.SECONDS);
        assert allReceived : "所有跨节点消息应该被接收";
        
        // 验证通信统计
        assert node2.getReceivedMessageCount() >= messageCount : "Node2应该接收到所有消息";
        assert gateway1.getRoutedMessageCount() >= messageCount : "Gateway1应该路由了所有消息";
        
        // 清理资源
        CompletableFuture.allOf(
            container1.stop(),
            container2.stop(),
            transport1.stop(),
            transport2.stop()
        ).join();
        
        logger.info("✓ 分布式通信系统测试通过 - 跨节点消息: {}", messageCount);
    }

    /**
     * 测试4: 容器管理和监控测试
     * 测试容器的高级管理功能和监控能力
     */
    private void testContainerManagementAndMonitoring() throws Exception {
        logger.info("--- 测试4: 容器管理和监控测试 ---");
        
        // 创建高级容器配置
        ContainerConfig config = ContainerConfig.builder("MonitoringContainer")
            .maxAgents(20)
            .agentStartTimeout(Duration.ofSeconds(8))
            .agentStopTimeout(Duration.ofSeconds(5))
            .autoStartAgents(true)
            .enableMonitoring(true)
            .property("monitoring.interval", "1000")
            .property("health.check", "enabled")
            .build();
        
        AgentContainer container = new DefaultAgentContainer(config);
        
        // 设置详细的事件监听器
        ContainerEventCollector eventCollector = new ContainerEventCollector();
        container.setAgentEventListener(eventCollector);
        
        container.start().join();
        
        // 动态创建和管理智能体
        for (int i = 1; i <= 10; i++) {
            AgentId agentId = AgentId.createLocal("DynamicAgent" + i, "dynamic");
            DynamicTestAgent agent = container.createAgent(DynamicTestAgent.class, agentId);
            
            // 模拟不同的智能体行为
            if (i % 3 == 0) {
                agent.setFailureMode(true); // 部分智能体设置为故障模式
            }
        }
        
        // 等待智能体启动和运行
        Thread.sleep(5000);
        
        // 测试容器统计功能
        AgentContainer.ContainerStats stats = container.getStats();
        logger.info("容器统计信息: {}", stats);
        
        assert stats.totalAgents() == 10 : "应该有10个智能体";
        assert stats.running() : "容器应该正在运行";
        assert stats.activeAgents() > 0 : "应该有活跃的智能体";
        
        // 测试智能体查询功能
        List<Agent> dynamicAgents = container.getAgentsByType("dynamic");
        assert dynamicAgents.size() == 10 : "应该找到10个动态智能体";
        
        // 测试智能体状态管理
        Agent testAgent = container.getAgent("DynamicAgent1");
        assert testAgent != null : "应该能找到指定智能体";
        
        AgentState originalState = testAgent.getState();
        testAgent.suspend();
        Thread.sleep(1000);
        assert testAgent.getState() != originalState : "智能体状态应该改变";
        
        testAgent.resume();
        Thread.sleep(1000);
        
        // 测试智能体移除和添加
        Agent removedAgent = container.removeAgent(AgentId.createLocal("DynamicAgent10", "dynamic"));
        assert removedAgent != null : "应该成功移除智能体";
        assert container.getAgentCount() == 9 : "智能体数量应该减少";
        
        // 重新添加智能体
        AgentId newAgentId = AgentId.createLocal("NewDynamicAgent", "dynamic");
        DynamicTestAgent newAgent = container.createAgent(DynamicTestAgent.class, newAgentId);
        assert container.getAgentCount() == 10 : "智能体数量应该恢复";
        
        // 验证事件收集
        assert eventCollector.getAddedCount() >= 10 : "应该记录了智能体添加事件";
        assert eventCollector.getStartedCount() >= 10 : "应该记录了智能体启动事件";
        assert eventCollector.getRemovedCount() >= 1 : "应该记录了智能体移除事件";
        
        container.stop().join();
        
        logger.info("✓ 容器管理和监控测试通过");
    }

    /**
     * 测试5: 错误处理和恢复机制测试
     * 测试框架的容错能力和恢复机制
     */
    private void testErrorHandlingAndRecovery() throws Exception {
        logger.info("--- 测试5: 错误处理和恢复机制测试 ---");
        
        ContainerConfig config = ContainerConfig.builder("FaultToleranceContainer")
            .maxAgents(15)
            .agentStartTimeout(Duration.ofSeconds(3))
            .autoStartAgents(true)
            .enableMonitoring(true)
            .property("fault.tolerance", "enabled")
            .build();
        
        AgentContainer container = new DefaultAgentContainer(config);
        MessageRouter router = new LocalMessageRouter();
        Transport transport = new LocalTransport();
        
        // 设置错误处理的事件监听器
        FaultToleranceEventListener faultListener = new FaultToleranceEventListener();
        container.setAgentEventListener(faultListener);
        
        transport.setMessageHandler(router::routeMessage);
        transport.start().join();
        container.start().join();
        
        // 创建正常智能体
        StableAgent stableAgent = container.createAgent(StableAgent.class,
            AgentId.createLocal("StableAgent", "stable"));
        router.registerAgent(stableAgent.getAgentId(), stableAgent::handleMessage);
        
        // 创建故障智能体
        FaultyAgent faultyAgent = null;
        try {
            faultyAgent = container.createAgent(FaultyAgent.class,
                AgentId.createLocal("FaultyAgent", "faulty"));
        } catch (Exception e) {
            logger.info("预期的故障智能体创建异常: {}", e.getMessage());
        }
        
        // 创建恢复智能体
        RecoveryAgent recoveryAgent = container.createAgent(RecoveryAgent.class,
            AgentId.createLocal("RecoveryAgent", "recovery"));
        router.registerAgent(recoveryAgent.getAgentId(), recoveryAgent::handleMessage);
        
        Thread.sleep(3000);
        
        // 测试消息处理中的错误恢复
        AgentMessage errorMessage = AgentMessage.builder()
            .sender(stableAgent.getAgentId())
            .receiver(recoveryAgent.getAgentId())
            .performative(MessagePerformative.REQUEST)
            .content("ERROR_TRIGGER") // 触发错误的特殊消息
            .build();
        
        router.routeMessage(errorMessage).join();
        Thread.sleep(2000);
        
        // 发送正常消息验证恢复
        AgentMessage normalMessage = AgentMessage.builder()
            .sender(stableAgent.getAgentId())
            .receiver(recoveryAgent.getAgentId())
            .performative(MessagePerformative.INFORM)
            .content("正常消息")
            .build();
        
        router.routeMessage(normalMessage).join();
        Thread.sleep(1000);
        
        // 验证错误处理和恢复
        assert faultListener.getErrorCount() > 0 : "应该记录了错误事件";
        assert recoveryAgent.getProcessedMessageCount() > 0 : "恢复智能体应该处理了消息";
        assert stableAgent.isActive() : "稳定智能体应该保持活跃";
        
        // 测试容器级别的故障恢复
        int originalAgentCount = container.getAgentCount();
        
        // 移除故障智能体并添加新的正常智能体
        if (faultyAgent != null) {
            container.removeAgent(faultyAgent.getAgentId());
        }
        
        ReplacementAgent replacement = container.createAgent(ReplacementAgent.class,
            AgentId.createLocal("ReplacementAgent", "replacement"));
        
        assert container.getAgentCount() >= originalAgentCount - 1 : "容器应该维持智能体数量";
        
        container.stop().join();
        transport.stop().join();
        
        logger.info("✓ 错误处理和恢复机制测试通过");
    }

    /**
     * 测试6: 性能压力测试
     * 测试框架在高负载下的性能表现
     */
    private void testPerformanceAndStress() throws Exception {
        logger.info("--- 测试6: 性能压力测试 ---");
        
        long startTime = System.currentTimeMillis();
        
        // 创建高性能容器配置
        ContainerConfig config = ContainerConfig.builder("PerformanceContainer")
            .maxAgents(50)
            .agentStartTimeout(Duration.ofSeconds(15))
            .autoStartAgents(true)
            .enableMonitoring(true)
            .property("performance.mode", "high")
            .property("batch.size", "100")
            .build();
        
        AgentContainer container = new DefaultAgentContainer(config);
        MessageRouter router = new LocalMessageRouter();
        Transport transport = new LocalTransport();
        
        transport.setMessageHandler(router::routeMessage);
        transport.start().join();
        container.start().join();
        
        // 创建性能测试智能体群
        int agentCount = 30;
        PerformanceTestAgent[] agents = new PerformanceTestAgent[agentCount];
        
        for (int i = 0; i < agentCount; i++) {
            AgentId agentId = AgentId.createLocal("PerfAgent" + (i + 1), "performance");
            agents[i] = container.createAgent(PerformanceTestAgent.class, agentId);
            router.registerAgent(agentId, agents[i]::handleMessage);
        }
        
        // 等待所有智能体启动
        Thread.sleep(5000);
        
        // 执行高负载消息测试
        int messageCount = 1000;
        CountDownLatch performanceLatch = new CountDownLatch(messageCount);
        
        // 设置消息计数器
        for (PerformanceTestAgent agent : agents) {
            agent.setMessageLatch(performanceLatch);
        }
        
        long messageStartTime = System.currentTimeMillis();
        
        // 并发发送大量消息
        CompletableFuture<Void>[] messageFutures = new CompletableFuture[messageCount];
        for (int i = 0; i < messageCount; i++) {
            final int messageIndex = i;
            messageFutures[i] = CompletableFuture.runAsync(() -> {
                AgentId sender = agents[messageIndex % agentCount].getAgentId();
                AgentId receiver = agents[(messageIndex + 1) % agentCount].getAgentId();
                
                AgentMessage perfMessage = AgentMessage.builder()
                    .sender(sender)
                    .receiver(receiver)
                    .performative(MessagePerformative.INFORM)
                    .content("性能测试消息 #" + messageIndex)
                    .priority(MessagePriority.NORMAL)
                    .build();
                
                try {
                    router.routeMessage(perfMessage).join();
                    totalMessages.incrementAndGet();
                } catch (Exception e) {
                    logger.warn("消息发送失败: {}", e.getMessage());
                }
            });
        }
        
        // 等待所有消息发送完成
        CompletableFuture.allOf(messageFutures).join();
        
        // 等待所有消息处理完成
        boolean allProcessed = performanceLatch.await(60, TimeUnit.SECONDS);
        long messageEndTime = System.currentTimeMillis();
        
        assert allProcessed : "所有消息应该在60秒内处理完成";
        
        // 计算性能指标
        long totalTime = messageEndTime - messageStartTime;
        double messagesPerSecond = (double) messageCount / (totalTime / 1000.0);
        double averageLatency = (double) totalTime / messageCount;
        
        // 验证性能指标
        assert messagesPerSecond > 10 : "消息处理速度应该超过10条/秒";
        assert averageLatency < 100 : "平均延迟应该小于100毫秒";
        
        // 获取容器性能统计
        AgentContainer.ContainerStats finalStats = container.getStats();
        logger.info("最终容器统计: {}", finalStats);
        
        // 验证系统稳定性
        assert container.isRunning() : "容器应该保持运行状态";
        assert container.getAgentCount() == agentCount : "所有智能体应该保持活跃";
        
        long endTime = System.currentTimeMillis();
        long totalTestTime = endTime - startTime;
        
        container.stop().join();
        transport.stop().join();
        
        logger.info("✓ 性能压力测试通过");
        logger.info("性能指标 - 消息/秒: {:.2f}, 平均延迟: {:.2f}ms, 总测试时间: {}ms", 
            messagesPerSecond, averageLatency, totalTestTime);
    }

    /**
     * 打印测试总结
     */
    private void printTestSummary() {
        logger.info("=== 测试总结 ===");
        logger.info("总处理消息数: {}", totalMessages.get());
        logger.info("总处理时间: {}ms", totalProcessingTime.get());
        logger.info("智能体消息统计: {}", agentMessageCounts);
        
        if (totalMessages.get() > 0) {
            double avgProcessingTime = (double) totalProcessingTime.get() / totalMessages.get();
            logger.info("平均消息处理时间: {:.2f}ms", avgProcessingTime);
        }
        
        logger.info("所有测试场景均通过 ✓");
    }

    // ===== 测试智能体实现 =====

    /**
     * 协调器智能体
     */
    public static class CoordinatorAgent extends AbstractAgent {
        private static final Logger logger = LoggerFactory.getLogger(CoordinatorAgent.class);
        private final AtomicInteger processedMessages = new AtomicInteger(0);

        public CoordinatorAgent(AgentId agentId) {
            super(agentId);
        }

        @Override
        protected BehaviorScheduler createBehaviorScheduler() {
            return new DefaultBehaviorScheduler();
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected void doStart() {
            logger.info("协调器智能体 {} 启动", getAgentId().getShortId());
        }

        @Override
        protected void doHandleMessage(AgentMessage message) {
            processedMessages.incrementAndGet();
            logger.debug("协调器处理消息: {}", message.content());
        }

        public int getProcessedMessageCount() {
            return processedMessages.get();
        }

        @Override
        public String getType() {
            return "coordinator";
        }
    }

    /**
     * 工作智能体
     */
    public static class WorkerAgent extends AbstractAgent {
        private static final Logger logger = LoggerFactory.getLogger(WorkerAgent.class);
        private final AtomicInteger processedMessages = new AtomicInteger(0);

        public WorkerAgent(AgentId agentId) {
            super(agentId);
        }

        @Override
        protected BehaviorScheduler createBehaviorScheduler() {
            return new DefaultBehaviorScheduler();
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected void doStart() {
            logger.info("工作智能体 {} 启动", getAgentId().getShortId());
        }

        @Override
        protected void doHandleMessage(AgentMessage message) {
            processedMessages.incrementAndGet();
            logger.debug("工作智能体处理消息: {}", message.content());
            
            // 模拟工作处理
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        public int getProcessedMessageCount() {
            return processedMessages.get();
        }

        @Override
        public String getType() {
            return "worker";
        }
    }

    /**
     * 任务管理智能体
     */
    public static class TaskManagerAgent extends AbstractAgent {
        private static final Logger logger = LoggerFactory.getLogger(TaskManagerAgent.class);
        private ResourceManagerAgent resourceManager;
        private List<ProcessorAgent> processors;
        private MonitorAgent monitor;
        private final AtomicInteger completedTasks = new AtomicInteger(0);

        public TaskManagerAgent(AgentId agentId) {
            super(agentId);
        }

        public void setResourceManager(ResourceManagerAgent resourceManager) {
            this.resourceManager = resourceManager;
        }

        public void setProcessors(List<ProcessorAgent> processors) {
            this.processors = processors;
        }

        public void setMonitor(MonitorAgent monitor) {
            this.monitor = monitor;
        }

        public void startCollaborativeTask(String taskName, CountDownLatch completion) {
            CompletableFuture.runAsync(() -> {
                try {
                    logger.info("开始协作任务: {}", taskName);
                    
                    // 请求资源分配
                    if (resourceManager != null) {
                        resourceManager.allocateResources(10);
                    }
                    
                    // 分发任务给处理器
                    if (processors != null) {
                        for (int i = 0; i < processors.size(); i++) {
                            ProcessorAgent processor = processors.get(i);
                            processor.processTask("子任务-" + (i + 1));
                        }
                    }
                    
                    // 等待处理完成
                    Thread.sleep(2000);
                    
                    completedTasks.incrementAndGet();
                    logger.info("协作任务完成: {}", taskName);
                    
                    completion.countDown();
                } catch (Exception e) {
                    logger.error("协作任务执行失败", e);
                }
            });
        }

        public int getCompletedTasks() {
            return completedTasks.get();
        }

        @Override
        protected BehaviorScheduler createBehaviorScheduler() {
            return new DefaultBehaviorScheduler();
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected void doStart() {
            logger.info("任务管理智能体 {} 启动", getAgentId().getShortId());
        }

        @Override
        public String getType() {
            return "manager";
        }
    }

    /**
     * 资源管理智能体
     */
    public static class ResourceManagerAgent extends AbstractAgent {
        private static final Logger logger = LoggerFactory.getLogger(ResourceManagerAgent.class);
        private final AtomicInteger allocatedResources = new AtomicInteger(0);

        public ResourceManagerAgent(AgentId agentId) {
            super(agentId);
        }

        public void allocateResources(int amount) {
            allocatedResources.addAndGet(amount);
            logger.info("分配资源: {} 单位", amount);
        }

        public int getAllocatedResources() {
            return allocatedResources.get();
        }

        @Override
        protected BehaviorScheduler createBehaviorScheduler() {
            return new DefaultBehaviorScheduler();
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected void doStart() {
            logger.info("资源管理智能体 {} 启动", getAgentId().getShortId());
        }

        @Override
        public String getType() {
            return "resource";
        }
    }

    /**
     * 监控智能体
     */
    public static class MonitorAgent extends AbstractAgent {
        private static final Logger logger = LoggerFactory.getLogger(MonitorAgent.class);

        public MonitorAgent(AgentId agentId) {
            super(agentId);
        }

        @Override
        protected BehaviorScheduler createBehaviorScheduler() {
            return new DefaultBehaviorScheduler();
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected void doStart() {
            logger.info("监控智能体 {} 启动", getAgentId().getShortId());
        }

        @Override
        public String getType() {
            return "monitor";
        }
    }

    /**
     * 处理器智能体
     */
    public static class ProcessorAgent extends AbstractAgent {
        private static final Logger logger = LoggerFactory.getLogger(ProcessorAgent.class);
        private final AtomicInteger processedTasks = new AtomicInteger(0);

        public ProcessorAgent(AgentId agentId) {
            super(agentId);
        }

        public void processTask(String taskName) {
            CompletableFuture.runAsync(() -> {
                try {
                    logger.debug("处理任务: {}", taskName);
                    Thread.sleep(100); // 模拟处理时间
                    processedTasks.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        public int getProcessedTasks() {
            return processedTasks.get();
        }

        @Override
        protected BehaviorScheduler createBehaviorScheduler() {
            return new DefaultBehaviorScheduler();
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected void doStart() {
            logger.info("处理器智能体 {} 启动", getAgentId().getShortId());
        }

        @Override
        public String getType() {
            return "processor";
        }
    }

    /**
     * 通信节点智能体
     */
    public static class CommunicationNodeAgent extends AbstractAgent {
        private static final Logger logger = LoggerFactory.getLogger(CommunicationNodeAgent.class);
        private final AtomicInteger receivedMessages = new AtomicInteger(0);
        private CountDownLatch messageLatch;

        public CommunicationNodeAgent(AgentId agentId) {
            super(agentId);
        }

        public void setMessageLatch(CountDownLatch latch) {
            this.messageLatch = latch;
        }

        public int getReceivedMessageCount() {
            return receivedMessages.get();
        }

        @Override
        protected BehaviorScheduler createBehaviorScheduler() {
            return new DefaultBehaviorScheduler();
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected void doStart() {
            logger.info("通信节点智能体 {} 启动", getAgentId().getShortId());
        }

        @Override
        protected void doHandleMessage(AgentMessage message) {
            receivedMessages.incrementAndGet();
            logger.debug("节点 {} 接收消息: {}", getAgentId().getShortId(), message.content());
            
            if (messageLatch != null) {
                messageLatch.countDown();
            }
        }

        @Override
        public String getType() {
            return "node";
        }
    }

    /**
     * 网关智能体
     */
    public static class GatewayAgent extends AbstractAgent {
        private static final Logger logger = LoggerFactory.getLogger(GatewayAgent.class);
        private GatewayAgent remoteGateway;
        private final AtomicInteger routedMessages = new AtomicInteger(0);

        public GatewayAgent(AgentId agentId) {
            super(agentId);
        }

        public void setRemoteGateway(GatewayAgent gateway) {
            this.remoteGateway = gateway;
        }

        public void routeMessage(AgentMessage message) {
            routedMessages.incrementAndGet();
            logger.debug("网关 {} 路由消息到远程节点", getAgentId().getShortId());
            
            if (remoteGateway != null) {
                // 模拟跨网关消息传输
                CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(5); // 模拟网络延迟
                        remoteGateway.handleMessage(message);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
        }

        public int getRoutedMessageCount() {
            return routedMessages.get();
        }

        @Override
        protected BehaviorScheduler createBehaviorScheduler() {
            return new DefaultBehaviorScheduler();
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected void doStart() {
            logger.info("网关智能体 {} 启动", getAgentId().getShortId());
        }

        @Override
        protected void doHandleMessage(AgentMessage message) {
            // 将消息转发给目标智能体（这里简化处理）
            logger.debug("网关接收并转发消息: {}", message.content());
        }

        @Override
        public String getType() {
            return "gateway";
        }
    }

    /**
     * 动态测试智能体
     */
    public static class DynamicTestAgent extends AbstractAgent {
        private static final Logger logger = LoggerFactory.getLogger(DynamicTestAgent.class);
        private boolean failureMode = false;

        public DynamicTestAgent(AgentId agentId) {
            super(agentId);
        }

        public void setFailureMode(boolean failureMode) {
            this.failureMode = failureMode;
        }

        @Override
        protected BehaviorScheduler createBehaviorScheduler() {
            return new DefaultBehaviorScheduler();
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected void doStart() {
            if (failureMode) {
                throw new RuntimeException("模拟启动失败");
            }
            logger.info("动态测试智能体 {} 启动", getAgentId().getShortId());
        }

        @Override
        public String getType() {
            return "dynamic";
        }
    }

    /**
     * 稳定智能体
     */
    public static class StableAgent extends AbstractAgent {
        private static final Logger logger = LoggerFactory.getLogger(StableAgent.class);

        public StableAgent(AgentId agentId) {
            super(agentId);
        }

        @Override
        protected BehaviorScheduler createBehaviorScheduler() {
            return new DefaultBehaviorScheduler();
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected void doStart() {
            logger.info("稳定智能体 {} 启动", getAgentId().getShortId());
        }

        @Override
        public String getType() {
            return "stable";
        }
    }

    /**
     * 故障智能体
     */
    public static class FaultyAgent extends AbstractAgent {
        public FaultyAgent(AgentId agentId) {
            super(agentId);
        }

        @Override
        protected BehaviorScheduler createBehaviorScheduler() {
            return new DefaultBehaviorScheduler();
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected void doStart() {
            throw new RuntimeException("故障智能体启动失败");
        }

        @Override
        public String getType() {
            return "faulty";
        }
    }

    /**
     * 恢复智能体
     */
    public static class RecoveryAgent extends AbstractAgent {
        private static final Logger logger = LoggerFactory.getLogger(RecoveryAgent.class);
        private final AtomicInteger processedMessages = new AtomicInteger(0);
        private boolean errorOccurred = false;

        public RecoveryAgent(AgentId agentId) {
            super(agentId);
        }

        public int getProcessedMessageCount() {
            return processedMessages.get();
        }

        @Override
        protected BehaviorScheduler createBehaviorScheduler() {
            return new DefaultBehaviorScheduler();
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected void doStart() {
            logger.info("恢复智能体 {} 启动", getAgentId().getShortId());
        }

        @Override
        protected void doHandleMessage(AgentMessage message) {
            try {
                if ("ERROR_TRIGGER".equals(message.content()) && !errorOccurred) {
                    errorOccurred = true;
                    throw new RuntimeException("模拟消息处理错误");
                }
                
                processedMessages.incrementAndGet();
                logger.debug("恢复智能体处理消息: {}", message.content());
                
            } catch (Exception e) {
                logger.warn("消息处理出错，尝试恢复: {}", e.getMessage());
                // 模拟错误恢复
                try {
                    Thread.sleep(100);
                    processedMessages.incrementAndGet();
                    logger.info("智能体已恢复正常");
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        @Override
        public String getType() {
            return "recovery";
        }
    }

    /**
     * 替换智能体
     */
    public static class ReplacementAgent extends AbstractAgent {
        private static final Logger logger = LoggerFactory.getLogger(ReplacementAgent.class);

        public ReplacementAgent(AgentId agentId) {
            super(agentId);
        }

        @Override
        protected BehaviorScheduler createBehaviorScheduler() {
            return new DefaultBehaviorScheduler();
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected void doStart() {
            logger.info("替换智能体 {} 启动", getAgentId().getShortId());
        }

        @Override
        public String getType() {
            return "replacement";
        }
    }

    /**
     * 性能测试智能体
     */
    public static class PerformanceTestAgent extends AbstractAgent {
        private static final Logger logger = LoggerFactory.getLogger(PerformanceTestAgent.class);
        private final AtomicInteger processedMessages = new AtomicInteger(0);
        private CountDownLatch messageLatch;

        public PerformanceTestAgent(AgentId agentId) {
            super(agentId);
        }

        public void setMessageLatch(CountDownLatch latch) {
            this.messageLatch = latch;
        }

        public int getProcessedMessageCount() {
            return processedMessages.get();
        }

        @Override
        protected BehaviorScheduler createBehaviorScheduler() {
            return new DefaultBehaviorScheduler();
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected void doStart() {
            logger.debug("性能测试智能体 {} 启动", getAgentId().getShortId());
        }

        @Override
        protected void doHandleMessage(AgentMessage message) {
            long startTime = System.nanoTime();
            
            processedMessages.incrementAndGet();
            
            // 模拟消息处理
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            long endTime = System.nanoTime();
            totalProcessingTime.addAndGet((endTime - startTime) / 1_000_000); // 转换为毫秒
            
            agentMessageCounts.merge(getAgentId().getShortId(), 1, Integer::sum);
            
            if (messageLatch != null) {
                messageLatch.countDown();
            }
        }

        @Override
        public String getType() {
            return "performance";
        }
    }

    // ===== 辅助类 =====

    /**
     * 容器事件收集器
     */
    public static class ContainerEventCollector implements AgentContainer.AgentEventListener {
        private final AtomicInteger addedCount = new AtomicInteger(0);
        private final AtomicInteger removedCount = new AtomicInteger(0);
        private final AtomicInteger startedCount = new AtomicInteger(0);
        private final AtomicInteger stoppedCount = new AtomicInteger(0);
        private final AtomicInteger errorCount = new AtomicInteger(0);

        @Override
        public void onAgentAdded(Agent agent) {
            addedCount.incrementAndGet();
        }

        @Override
        public void onAgentRemoved(Agent agent) {
            removedCount.incrementAndGet();
        }

        @Override
        public void onAgentStarted(Agent agent) {
            startedCount.incrementAndGet();
        }

        @Override
        public void onAgentStopped(Agent agent) {
            stoppedCount.incrementAndGet();
        }

        @Override
        public void onAgentError(Agent agent, Throwable error) {
            errorCount.incrementAndGet();
        }

        public int getAddedCount() { return addedCount.get(); }
        public int getRemovedCount() { return removedCount.get(); }
        public int getStartedCount() { return startedCount.get(); }
        public int getStoppedCount() { return stoppedCount.get(); }
        public int getErrorCount() { return errorCount.get(); }
    }

    /**
     * 容错事件监听器
     */
    public static class FaultToleranceEventListener implements AgentContainer.AgentEventListener {
        private final AtomicInteger errorCount = new AtomicInteger(0);

        @Override
        public void onAgentError(Agent agent, Throwable error) {
            errorCount.incrementAndGet();
            logger.warn("智能体错误: {} - {}", agent.getAgentId().getShortId(), error.getMessage());
        }

        public int getErrorCount() {
            return errorCount.get();
        }
    }
}
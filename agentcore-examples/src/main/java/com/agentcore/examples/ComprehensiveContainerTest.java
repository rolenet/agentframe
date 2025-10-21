package com.agentcore.examples;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 综合性容器测试
 * 展示AgentContainer的所有核心功能，包括完整的生命周期管理、消息路由、事件监听等
 * 
 * @author AgentCore Team
 */
public class ComprehensiveContainerTest {

    private static final Logger logger = LoggerFactory.getLogger(ComprehensiveContainerTest.class);

    public static void main(String[] args) {
        logger.info("=== 开始综合性容器测试 ===");

        try {
            // 创建高性能容器配置
            ContainerConfig config = ContainerConfig.builder("ComprehensiveTestContainer")
                .maxAgents(20)
                .agentStartTimeout(Duration.ofSeconds(10))
                .agentStopTimeout(Duration.ofSeconds(5))
                .autoStartAgents(true)
                .enableMonitoring(true)
                .property("test.mode", "comprehensive")
                .property("performance.tracking", true)
                .build();

            AgentContainer container = new DefaultAgentContainer(config);
            
            // 设置详细的事件监听器
            setupDetailedEventListener(container);

            // 启动容器
            container.start().join();
            logger.info("容器启动成功，名称: {}", container.getName());

            // 执行所有功能测试
            runAllContainerTests(container);

            // 停止容器
            container.stop().join();
            logger.info("容器停止成功");

        } catch (Exception e) {
            logger.error("综合性容器测试过程中发生错误", e);
        }

        logger.info("=== 综合性容器测试完成 ===");
    }

    /**
     * 设置详细的事件监听器
     */
    private static void setupDetailedEventListener(AgentContainer container) {
        container.setAgentEventListener(new AgentContainer.AgentEventListener() {
            @Override
            public void onAgentAdded(Agent agent) {
                logger.info("📥 Agent添加: {} (类型: {})", 
                    agent.getAgentId().getShortId(), agent.getType());
            }

            @Override
            public void onAgentRemoved(Agent agent) {
                logger.info("📤 Agent移除: {} (状态: {})", 
                    agent.getAgentId().getShortId(), agent.getState());
            }

            @Override
            public void onAgentStarted(Agent agent) {
                logger.info("▶️ Agent启动: {} → {}", 
                    agent.getAgentId().getShortId(), agent.getState());
            }

            @Override
            public void onAgentStopped(Agent agent) {
                logger.info("⏹️ Agent停止: {} → {}", 
                    agent.getAgentId().getShortId(), agent.getState());
            }

            @Override
            public void onAgentError(Agent agent, Throwable error) {
                logger.error("❌ Agent错误: {}, 错误: {}", 
                    agent.getAgentId().getShortId(), error.getMessage());
            }
        });
    }

    /**
     * 运行所有容器功能测试
     */
    private static void runAllContainerTests(AgentContainer container) throws Exception {
        logger.info("=== 开始运行所有容器功能测试 ===");

        // 测试1: Agent创建和生命周期管理
        testAgentLifecycleManagement(container);

        // 测试2: 消息路由和通信
        testMessageRoutingAndCommunication(container);

        // 测试3: Agent状态监控和管理
        testAgentStateMonitoring(container);

        // 测试4: 容器统计和性能监控
        testContainerStatistics(container);

        // 测试5: 错误处理和恢复机制
        testErrorHandlingAndRecovery(container);

        // 测试6: 大规模Agent管理
        testMassAgentManagement(container);

        logger.info("=== 所有容器功能测试完成 ===");
    }

    /**
     * 测试1: Agent创建和生命周期管理
     */
    private static void testAgentLifecycleManagement(AgentContainer container) throws Exception {
        logger.info("--- 测试1: Agent创建和生命周期管理 ---");

        // 创建不同类型的Agent
        AgentId workerId = AgentId.create("WorkerAgent");
        AgentId managerId = AgentId.create("ManagerAgent");
        AgentId monitorId = AgentId.create("MonitorAgent");

        WorkerAgent worker = container.createAgent(WorkerAgent.class, workerId);
        ManagerAgent manager = container.createAgent(ManagerAgent.class, managerId);
        MonitorAgent monitor = container.createAgent(MonitorAgent.class, monitorId);

        // 验证Agent创建
        logger.info("创建的Agent数量: {}", container.getAgentCount());
        logger.info("所有Agent: {}", container.getAllAgents().stream()
            .map(agent -> agent.getAgentId().getShortId())
            .toList());

        // 测试Agent查找功能
        Agent foundAgent = container.getAgent(workerId);
        logger.info("查找Agent结果: {} -> {}", workerId.getShortId(), 
            foundAgent != null ? "成功" : "失败");

        // 测试按名称查找
        Agent byNameAgent = container.getAgent("ManagerAgent");
        logger.info("按名称查找Agent结果: {} -> {}", "ManagerAgent", 
            byNameAgent != null ? "成功" : "失败");

        // 测试Agent存在性检查
        boolean exists = container.containsAgent(monitorId);
        logger.info("Agent存在性检查: {} -> {}", monitorId.getShortId(), exists);

        // 等待Agent完全启动
        Thread.sleep(2000);
        logger.info("Agent生命周期管理测试完成");
    }

    /**
     * 测试2: 消息路由和通信
     */
    private static void testMessageRoutingAndCommunication(AgentContainer container) throws Exception {
        logger.info("--- 测试2: 消息路由和通信 ---");

        // 创建通信测试Agent
        AgentId senderId = AgentId.create("SenderAgent");
        AgentId receiverId = AgentId.create("ReceiverAgent");

        CommunicationSender sender = container.createAgent(CommunicationSender.class, senderId);
        CommunicationReceiver receiver = container.createAgent(CommunicationReceiver.class, receiverId);

        // 设置通信目标
        sender.setTargetAgent(receiver);

        // 等待消息交互
        Thread.sleep(3000);

        // 检查消息处理结果
        int sentMessages = sender.getSentMessageCount();
        int receivedMessages = receiver.getReceivedMessageCount();
        
        logger.info("消息通信统计 - 发送: {}, 接收: {}", sentMessages, receivedMessages);
        logger.info("消息路由和通信测试完成");
    }

    /**
     * 测试3: Agent状态监控和管理
     */
    private static void testAgentStateMonitoring(AgentContainer container) throws Exception {
        logger.info("--- 测试3: Agent状态监控和管理 ---");

        // 创建状态测试Agent
        AgentId stateAgentId = AgentId.create("StateTestAgent");
        StateTestAgent stateAgent = container.createAgent(StateTestAgent.class, stateAgentId);

        // 等待Agent启动
        Thread.sleep(1000);

        // 检查初始状态
        AgentState initialState = stateAgent.getState();
        logger.info("Agent初始状态: {}", initialState);

        // 测试状态转换
        stateAgent.suspend();
        Thread.sleep(500);
        AgentState suspendedState = stateAgent.getState();
        logger.info("Agent暂停后状态: {}", suspendedState);

        stateAgent.resume();
        Thread.sleep(500);
        AgentState resumedState = stateAgent.getState();
        logger.info("Agent恢复后状态: {}", resumedState);

        // 获取所有Agent状态
        List<Agent> allAgents = container.getAllAgents();
        logger.info("所有Agent状态:");
        for (Agent agent : allAgents) {
            logger.info("  - {}: {}", agent.getAgentId().getShortId(), agent.getState());
        }

        logger.info("Agent状态监控和管理测试完成");
    }

    /**
     * 测试4: 容器统计和性能监控
     */
    private static void testContainerStatistics(AgentContainer container) throws Exception {
        logger.info("--- 测试4: 容器统计和性能监控 ---");

        // 获取容器统计信息
        AgentContainer.ContainerStats stats = container.getStats();
        logger.info("容器统计信息:");
        logger.info("  - 名称: {}", stats.name());
        logger.info("  - 运行状态: {}", stats.running());
        logger.info("  - Agent总数: {}", stats.totalAgents());
        logger.info("  - 活跃Agent: {}", stats.activeAgents());
        logger.info("  - 暂停Agent: {}", stats.suspendedAgents());
        logger.info("  - 错误Agent: {}", stats.errorAgents());
        logger.info("  - 处理消息数: {}", stats.messagesProcessed());
        logger.info("  - 平均响应时间: {:.2f}ms", stats.averageResponseTimeMs());

        // 测试配置获取
        ContainerConfig config = container.getConfig();
        logger.info("容器配置信息:");
        logger.info("  - 容器名称: {}", config.name());
        logger.info("  - 最大Agent数: {}", config.maxAgents());
        logger.info("  - 自动启动: {}", config.autoStartAgents());
        logger.info("  - 自定义属性: {}", config.getProperty("test.mode", "unknown"));

        logger.info("容器统计和性能监控测试完成");
    }

    /**
     * 测试5: 错误处理和恢复机制
     */
    private static void testErrorHandlingAndRecovery(AgentContainer container) throws Exception {
        logger.info("--- 测试5: 错误处理和恢复机制 ---");

        // 创建会抛出异常的Agent
        AgentId faultyId = AgentId.create("FaultyAgent");
        FaultyAgent faultyAgent = container.createAgent(FaultyAgent.class, faultyId);

        // 等待异常处理
        Thread.sleep(1000);

        // 检查错误状态
        AgentState faultyState = faultyAgent.getState();
        logger.info("故障Agent状态: {}", faultyState);

        // 创建正常Agent验证容器稳定性
        AgentId stableId = AgentId.create("StableAgent");
        StableAgent stableAgent = container.createAgent(StableAgent.class, stableId);

        Thread.sleep(1000);
        boolean stableActive = stableAgent.isActive();
        logger.info("稳定Agent运行状态: {}", stableActive);

        // 测试Agent移除
        Agent removedAgent = container.removeAgent(faultyId);
        logger.info("移除故障Agent结果: {}", removedAgent != null ? "成功" : "失败");

        logger.info("错误处理和恢复机制测试完成");
    }

    /**
     * 测试6: 大规模Agent管理
     */
    private static void testMassAgentManagement(AgentContainer container) throws Exception {
        logger.info("--- 测试6: 大规模Agent管理 ---");

        // 批量创建Agent
        int agentCount = 10;
        for (int i = 1; i <= agentCount; i++) {
            AgentId massAgentId = AgentId.create("MassAgent-" + i);
            MassTestAgent agent = container.createAgent(MassTestAgent.class, massAgentId);
        }

        // 验证批量管理
        logger.info("批量创建Agent数量: {}", container.getAgentCount());

        // 测试按类型分组
        List<Agent> massAgents = container.getAgentsByType("mass");
        logger.info("按类型分组的Agent数量: {}", massAgents.size());

        // 测试Agent移除和添加
        AgentId toRemoveId = AgentId.create("MassAgent-1");
        container.removeAgent(toRemoveId);
        logger.info("移除一个Agent后数量: {}", container.getAgentCount());

        // 重新添加Agent
        AgentId newAgentId = AgentId.create("NewMassAgent");
        MassTestAgent newAgent = container.createAgent(MassTestAgent.class, newAgentId);
        logger.info("重新添加Agent后数量: {}", container.getAgentCount());

        logger.info("大规模Agent管理测试完成");
    }

    // ===== 测试Agent实现 =====

    /**
     * 工作Agent
     */
    public static class WorkerAgent extends AbstractAgent {
        private static final Logger logger = LoggerFactory.getLogger(WorkerAgent.class);

        public WorkerAgent(AgentId agentId) {
            super(agentId);
        }

        @Override
        protected BehaviorScheduler createBehaviorScheduler() {
            return new DefaultBehaviorScheduler();
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            logger.debug("WorkerAgent发送消息: {}", message.content());
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected void doStart() {
            logger.info("WorkerAgent {} 启动", getAgentId().getShortId());
        }

        @Override
        protected void doHandleMessage(AgentMessage message) {
            logger.info("WorkerAgent {} 处理消息: {}", getAgentId().getShortId(), message.content());
        }

        @Override
        public String getType() {
            return "worker";
        }
    }

    /**
     * 管理Agent
     */
    public static class ManagerAgent extends AbstractAgent {
        private static final Logger logger = LoggerFactory.getLogger(ManagerAgent.class);

        public ManagerAgent(AgentId agentId) {
            super(agentId);
        }

        @Override
        protected BehaviorScheduler createBehaviorScheduler() {
            return new DefaultBehaviorScheduler();
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            logger.debug("ManagerAgent发送消息: {}", message.content());
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected void doStart() {
            logger.info("ManagerAgent {} 启动", getAgentId().getShortId());
        }

        @Override
        public String getType() {
            return "manager";
        }
    }

    /**
     * 监控Agent
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
            logger.debug("MonitorAgent发送消息: {}", message.content());
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected void doStart() {
            logger.info("MonitorAgent {} 启动", getAgentId().getShortId());
        }

        @Override
        public String getType() {
            return "monitor";
        }
    }

    /**
     * 通信发送者Agent
     */
    public static class CommunicationSender extends AbstractAgent {
        private static final Logger logger = LoggerFactory.getLogger(CommunicationSender.class);
        private Agent targetAgent;
        private final AtomicInteger messageCounter = new AtomicInteger(0);

        public CommunicationSender(AgentId agentId) {
            super(agentId);
        }

        public void setTargetAgent(Agent target) {
            this.targetAgent = target;
        }

        public int getSentMessageCount() {
            return messageCounter.get();
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
            logger.info("CommunicationSender {} 启动", getAgentId().getShortId());
            // 启动消息发送任务
            startMessageSending();
        }

        @Override
        protected void doHandleMessage(AgentMessage message) {
            logger.info("CommunicationSender {} 收到回复: {}", getAgentId().getShortId(), message.content());
        }

        private void startMessageSending() {
            Thread sendingThread = new Thread(() -> {
                try {
                    for (int i = 0; i < 5; i++) {
                        if (targetAgent != null) {
                            String content = String.format("测试消息 #%d from %s", 
                                i + 1, getAgentId().getShortId());
                            
                            AgentMessage message = AgentMessage.builder()
                                .sender(getAgentId())
                                .receiver(targetAgent.getAgentId())
                                .performative(MessagePerformative.INFORM)
                                .content(content)
                                .build();
                            
                            sendMessage(message);
                            messageCounter.incrementAndGet();
                            logger.debug("发送消息: {}", content);
                        }
                        Thread.sleep(500);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            sendingThread.start();
        }
    }

    /**
     * 通信接收者Agent
     */
    public static class CommunicationReceiver extends AbstractAgent {
        private static final Logger logger = LoggerFactory.getLogger(CommunicationReceiver.class);
        private final AtomicInteger receivedCounter = new AtomicInteger(0);

        public CommunicationReceiver(AgentId agentId) {
            super(agentId);
        }

        public int getReceivedMessageCount() {
            return receivedCounter.get();
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
            logger.info("CommunicationReceiver {} 启动", getAgentId().getShortId());
        }

        @Override
        protected void doHandleMessage(AgentMessage message) {
            receivedCounter.incrementAndGet();
            logger.info("CommunicationReceiver {} 收到消息: {}", getAgentId().getShortId(), message.content());
            
            // 发送回复
            AgentMessage reply = AgentMessage.builder()
                .sender(getAgentId())
                .receiver(message.sender())
                .performative(MessagePerformative.INFORM)
                .content("收到消息: " + message.content())
                .build();
            sendMessage(reply);
        }
    }

    /**
     * 状态测试Agent
     */
    public static class StateTestAgent extends AbstractAgent {
        public StateTestAgent(AgentId agentId) {
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
            logger.info("StateTestAgent {} 启动", getAgentId().getShortId());
        }

        @Override
        public String getType() {
            return "state-test";
        }
    }

    /**
     * 故障Agent
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
            throw new RuntimeException("模拟启动故障");
        }

        @Override
        public String getType() {
            return "faulty";
        }
    }

    /**
     * 稳定Agent
     */
    public static class StableAgent extends AbstractAgent {
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
            logger.info("StableAgent {} 稳定启动", getAgentId().getShortId());
        }

        @Override
        public String getType() {
            return "stable";
        }
    }

    /**
     * 大规模测试Agent
     */
    public static class MassTestAgent extends AbstractAgent {
        public MassTestAgent(AgentId agentId) {
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
            logger.debug("MassTestAgent {} 启动", getAgentId().getShortId());
        }

        @Override
        public String getType() {
            return "mass";
        }
    }
}
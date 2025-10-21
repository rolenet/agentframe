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
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 高级容器功能测试
 * 展示AgentContainer的所有高级功能，包括错误处理、性能监控、配置管理等
 * 
 * @author AgentCore Team
 */
public class AdvancedContainerTest {

    private static final Logger logger = LoggerFactory.getLogger(AdvancedContainerTest.class);

    public static void main(String[] args) {
        logger.info("=== 开始高级容器功能测试 ===");

        try {
            // 测试1: 基本容器功能
            testBasicContainerFunctionality();
            
            // 测试2: 错误处理和恢复
            testErrorHandlingAndRecovery();
            
            // 测试3: 性能监控和统计
            testPerformanceMonitoring();
            
            // 测试4: 配置管理
            testConfigurationManagement();
            
            // 测试5: 大规模Agent管理
            testMassiveAgentManagement();

        } catch (Exception e) {
            logger.error("高级容器测试过程中发生错误", e);
        }

        logger.info("=== 高级容器功能测试完成 ===");
    }

    /**
     * 测试1: 基本容器功能
     */
    private static void testBasicContainerFunctionality() throws Exception {
        logger.info("=== 测试1: 基本容器功能 ===");

        ContainerConfig config = ContainerConfig.builder("BasicTestContainer")
            .maxAgents(5)
            .autoStartAgents(true)
            .build();

        AgentContainer container = new DefaultAgentContainer(config);
        
        // 启动容器
        container.start().join();
        logger.info("容器启动成功");

        // 创建测试Agent
        AgentId pingId = AgentId.create("PingAgent");
        AgentId pongId = AgentId.create("PongAgent");
        
        PingAgent ping = container.createAgent(PingAgent.class, pingId);
        PongAgent pong = container.createAgent(PongAgent.class, pongId);

        // 设置PingAgent的目标
        ping.setTargetAgent(pong);

        // 等待消息交互
        Thread.sleep(3000);

        // 检查容器状态
        AgentContainer.ContainerStats stats = container.getStats();
        logger.info("容器统计: {}", stats);
        logger.info("Agent数量: {}", container.getAgentCount());
        logger.info("活跃Agent: {}", container.getAllAgents().stream()
            .filter(Agent::isActive)
            .map(agent -> agent.getAgentId().getShortId())
            .toList());

        // 停止容器
        container.stop().join();
        logger.info("基本容器功能测试完成");
    }

    /**
     * 测试2: 错误处理和恢复
     */
    private static void testErrorHandlingAndRecovery() throws Exception {
        logger.info("=== 测试2: 错误处理和恢复 ===");

        ContainerConfig config = ContainerConfig.builder("ErrorTestContainer")
            .maxAgents(3)
            .autoStartAgents(false) // 手动控制启动
            .build();

        AgentContainer container = new DefaultAgentContainer(config);
        
        // 设置错误事件监听器
        container.setAgentEventListener(new AgentContainer.AgentEventListener() {
            @Override
            public void onAgentError(Agent agent, Throwable error) {
                logger.warn("Agent错误捕获: {}, 错误: {}", 
                    agent.getAgentId().getShortId(), error.getMessage());
            }
        });

        container.start().join();

        // 创建会抛出异常的Agent
        AgentId faultyId = AgentId.create("FaultyAgent");
        FaultyAgent faultyAgent = container.createAgent(FaultyAgent.class, faultyId);

        // 手动启动Agent（会抛出异常）
        try {
            container.startAgent(faultyId).join();
        } catch (Exception e) {
            logger.info("预期中的Agent启动异常: {}", e.getMessage());
        }

        // 检查Agent状态
        AgentState state = faultyAgent.getState();
        logger.info("故障Agent状态: {}", state);

        // 创建正常Agent
        AgentId normalId = AgentId.create("NormalAgent");
        NormalAgent normalAgent = container.createAgent(NormalAgent.class, normalId);
        container.startAgent(normalId).join();

        // 验证正常Agent仍在运行
        boolean normalActive = normalAgent.isActive();
        logger.info("正常Agent运行状态: {}", normalActive);

        container.stop().join();
        logger.info("错误处理和恢复测试完成");
    }

    /**
     * 测试3: 性能监控和统计
     */
    private static void testPerformanceMonitoring() throws Exception {
        logger.info("=== 测试3: 性能监控和统计 ===");

        ContainerConfig config = ContainerConfig.builder("PerformanceTestContainer")
            .maxAgents(10)
            .autoStartAgents(true)
            .build();

        AgentContainer container = new DefaultAgentContainer(config);
        container.start().join();

        // 创建多个工作Agent
        for (int i = 1; i <= 5; i++) {
            AgentId workerId = AgentId.create("Worker-" + i);
            PerformanceWorker worker = container.createAgent(PerformanceWorker.class, workerId);
        }

        // 创建监控Agent
        AgentId monitorId = AgentId.create("MonitorAgent");
        PerformanceMonitor monitor = container.createAgent(PerformanceMonitor.class, monitorId);

        // 运行一段时间收集性能数据
        Thread.sleep(5000);

        // 获取性能统计
        AgentContainer.ContainerStats stats = container.getStats();
        logger.info("性能统计: {}", stats);
        logger.info("处理消息总数: {}", stats.messagesProcessed());
        logger.info("平均响应时间: {:.2f}ms", stats.averageResponseTimeMs());

        container.stop().join();
        logger.info("性能监控测试完成");
    }

    /**
     * 测试4: 配置管理
     */
    private static void testConfigurationManagement() throws Exception {
        logger.info("=== 测试4: 配置管理 ===");

        // 创建带自定义配置的容器
        ContainerConfig config = ContainerConfig.builder("ConfigTestContainer")
            .maxAgents(3)
            .agentStartTimeout(Duration.ofSeconds(10))
            .agentStopTimeout(Duration.ofSeconds(5))
            .property("custom.setting", "custom-value")
            .property("max.retries", 3)
            .property("enable.debug", true)
            .build();

        AgentContainer container = new DefaultAgentContainer(config);
        container.start().join();

        // 验证配置
        ContainerConfig containerConfig = container.getConfig();
        logger.info("容器名称: {}", containerConfig.name());
        logger.info("最大Agent数: {}", containerConfig.maxAgents());
        logger.info("启动超时: {}秒", containerConfig.agentStartTimeout().getSeconds());
        logger.info("停止超时: {}秒", containerConfig.agentStopTimeout().getSeconds());
        
        // 验证自定义属性
        String customSetting = containerConfig.getProperty("custom.setting", "default");
        int maxRetries = containerConfig.getProperty("max.retries", 1);
        boolean enableDebug = containerConfig.getProperty("enable.debug", false);
        
        logger.info("自定义设置: {}", customSetting);
        logger.info("最大重试次数: {}", maxRetries);
        logger.info("启用调试: {}", enableDebug);

        // 创建配置感知的Agent
        AgentId configAgentId = AgentId.create("ConfigAwareAgent");
        ConfigAwareAgent configAgent = container.createAgent(ConfigAwareAgent.class, configAgentId);

        container.stop().join();
        logger.info("配置管理测试完成");
    }

    /**
     * 测试5: 大规模Agent管理
     */
    private static void testMassiveAgentManagement() throws Exception {
        logger.info("=== 测试5: 大规模Agent管理 ===");

        ContainerConfig config = ContainerConfig.builder("MassiveTestContainer")
            .maxAgents(50) // 测试大规模Agent管理
            .autoStartAgents(true)
            .build();

        AgentContainer container = new DefaultAgentContainer(config);
        container.start().join();

        // 批量创建Agent
        for (int i = 1; i <= 20; i++) {
            AgentId agentId = AgentId.create("MassiveAgent-" + i);
            MassiveTestAgent agent = container.createAgent(MassiveTestAgent.class, agentId);
        }

        // 验证Agent管理功能
        logger.info("创建的Agent数量: {}", container.getAgentCount());
        
        // 测试按类型获取Agent
        List<Agent> massiveAgents = container.getAgentsByType("massive");
        logger.info("按类型获取的Agent数量: {}", massiveAgents.size());

        // 测试Agent查找功能
        Agent foundAgent = container.getAgent("MassiveAgent-5");
        logger.info("查找Agent结果: {}", foundAgent != null ? "成功" : "失败");

        // 测试Agent移除
        AgentId toRemoveId = AgentId.create("MassiveAgent-1");
        Agent removedAgent = container.removeAgent(toRemoveId);
        logger.info("移除Agent结果: {}", removedAgent != null ? "成功" : "失败");
        logger.info("移除后Agent数量: {}", container.getAgentCount());

        // 性能测试：批量消息发送
        Thread.sleep(2000);
        AgentContainer.ContainerStats stats = container.getStats();
        logger.info("大规模测试统计: {}", stats);

        container.stop().join();
        logger.info("大规模Agent管理测试完成");
    }

    // ===== 测试Agent实现 =====

    /**
     * Ping-Pong测试Agent
     */
    public static class PingAgent extends AbstractAgent {
        private static final Logger logger = LoggerFactory.getLogger(PingAgent.class);
        private Agent targetAgent;
        private final AtomicInteger counter = new AtomicInteger(0);

        public PingAgent(AgentId agentId) {
            super(agentId);
        }

        public void setTargetAgent(Agent target) {
            this.targetAgent = target;
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
            logger.info("PingAgent {} 启动", getAgentId().getShortId());
            // 启动后发送Ping消息
            sendPingMessage();
        }

        @Override
        protected void doHandleMessage(AgentMessage message) {
            if (message.performative() == MessagePerformative.INFORM && 
                message.content().toString().contains("Pong")) {
                int count = counter.incrementAndGet();
                logger.info("PingAgent {} 收到第{}个Pong回复", getAgentId().getShortId(), count);
                
                // 继续发送Ping
                if (count < 3) {
                    sendPingMessage();
                }
            }
        }

        private void sendPingMessage() {
            if (targetAgent != null) {
                AgentMessage ping = AgentMessage.builder()
                    .sender(getAgentId())
                    .receiver(targetAgent.getAgentId())
                    .performative(MessagePerformative.INFORM)
                    .content("Ping from " + getAgentId().getShortId())
                    .build();
                sendMessage(ping);
                logger.debug("PingAgent {} 发送Ping消息", getAgentId().getShortId());
            }
        }
    }

    public static class PongAgent extends AbstractAgent {
        private static final Logger logger = LoggerFactory.getLogger(PongAgent.class);
        private final AtomicInteger counter = new AtomicInteger(0);

        public PongAgent(AgentId agentId) {
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
        protected void doHandleMessage(AgentMessage message) {
            if (message.performative() == MessagePerformative.INFORM && 
                message.content().toString().contains("Ping")) {
                int count = counter.incrementAndGet();
                logger.info("PongAgent {} 收到第{}个Ping消息", getAgentId().getShortId(), count);
                
                // 回复Pong
                AgentMessage pong = AgentMessage.builder()
                    .sender(getAgentId())
                    .receiver(message.sender())
                    .performative(MessagePerformative.INFORM)
                    .content("Pong from " + getAgentId().getShortId())
                    .build();
                sendMessage(pong);
            }
        }
    }

    /**
     * 故障Agent - 用于测试错误处理
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
            throw new RuntimeException("模拟Agent启动故障");
        }
    }

    /**
     * 正常Agent - 用于对比测试
     */
    public static class NormalAgent extends AbstractAgent {
        public NormalAgent(AgentId agentId) {
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
            logger.info("正常Agent {} 启动成功", getAgentId().getShortId());
        }
    }

    /**
     * 性能工作Agent
     */
    public static class PerformanceWorker extends AbstractAgent {
        private static final Logger logger = LoggerFactory.getLogger(PerformanceWorker.class);
        private final AtomicLong processedMessages = new AtomicLong(0);

        public PerformanceWorker(AgentId agentId) {
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
        protected void doHandleMessage(AgentMessage message) {
            processedMessages.incrementAndGet();
            // 模拟消息处理
            try {
                Thread.sleep(10); // 10ms处理时间
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        public long getProcessedMessages() {
            return processedMessages.get();
        }
    }

    /**
     * 性能监控Agent
     */
    public static class PerformanceMonitor extends AbstractAgent {
        public PerformanceMonitor(AgentId agentId) {
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
            logger.info("性能监控Agent {} 启动", getAgentId().getShortId());
        }
    }

    /**
     * 配置感知Agent
     */
    public static class ConfigAwareAgent extends AbstractAgent {
        public ConfigAwareAgent(AgentId agentId) {
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
            logger.info("配置感知Agent {} 启动", getAgentId().getShortId());
        }
    }

    /**
     * 大规模测试Agent
     */
    public static class MassiveTestAgent extends AbstractAgent {
        public MassiveTestAgent(AgentId agentId) {
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
            logger.debug("大规模测试Agent {} 启动", getAgentId().getShortId());
        }

        @Override
        public String getType() {
            return "massive"; // 重写类型用于测试
        }
    }
}
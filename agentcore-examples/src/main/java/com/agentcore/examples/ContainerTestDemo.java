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
 * 容器功能测试演示
 * 展示AgentContainer的所有功能，包括生命周期管理、Agent管理、事件监听等
 * 
 * @author AgentCore Team
 */
public class ContainerTestDemo {

    private static final Logger logger = LoggerFactory.getLogger(ContainerTestDemo.class);

    public static void main(String[] args) {
        logger.info("=== 开始容器功能测试演示 ===");

        try {
            // 1. 创建容器配置
            ContainerConfig config = ContainerConfig.builder("TestContainer")
                .maxAgents(10)
                .agentStartTimeout(Duration.ofSeconds(5))
                .agentStopTimeout(Duration.ofSeconds(3))
                .autoStartAgents(true)
                .enableMonitoring(true)
                .property("test.property", "test-value")
                .build();

            logger.info("创建容器配置: {}", config.name());

            // 2. 创建容器实例
            AgentContainer container = new DefaultAgentContainer(config);
            
            // 3. 设置事件监听器
            container.setAgentEventListener(new AgentContainer.AgentEventListener() {
                @Override
                public void onAgentAdded(Agent agent) {
                    logger.info("事件监听器 - Agent添加: {}", agent.getAgentId().getShortId());
                }

                @Override
                public void onAgentRemoved(Agent agent) {
                    logger.info("事件监听器 - Agent移除: {}", agent.getAgentId().getShortId());
                }

                @Override
                public void onAgentStarted(Agent agent) {
                    logger.info("事件监听器 - Agent启动: {}", agent.getAgentId().getShortId());
                }

                @Override
                public void onAgentStopped(Agent agent) {
                    logger.info("事件监听器 - Agent停止: {}", agent.getAgentId().getShortId());
                }

                @Override
                public void onAgentError(Agent agent, Throwable error) {
                    logger.error("事件监听器 - Agent错误: {}, 错误: {}", 
                        agent.getAgentId().getShortId(), error.getMessage());
                }
            });

            // 4. 启动容器
            logger.info("启动容器...");
            container.start().join();
            logger.info("容器启动成功，运行状态: {}", container.isRunning());

            // 5. 演示容器功能
            demonstrateContainerFeatures(container);

            // 6. 停止容器
            logger.info("停止容器...");
            container.stop().join();
            logger.info("容器停止成功，运行状态: {}", container.isRunning());

        } catch (Exception e) {
            logger.error("容器测试演示过程中发生错误", e);
        }

        logger.info("=== 容器功能测试演示完成 ===");
    }

    /**
     * 演示容器的所有功能
     */
    private static void demonstrateContainerFeatures(AgentContainer container) throws Exception {
        logger.info("=== 开始演示容器功能 ===");

        // 1. 创建不同类型的Agent
        logger.info("1. 创建不同类型的Agent...");
        
        AgentId worker1Id = AgentId.create("WorkerAgent-1");
        AgentId worker2Id = AgentId.create("WorkerAgent-2");
        AgentId managerId = AgentId.create("ManagerAgent");
        AgentId monitorId = AgentId.create("MonitorAgent");

        // 使用createAgent方法创建Agent
        WorkerAgent worker1 = container.createAgent(WorkerAgent.class, worker1Id);
        WorkerAgent worker2 = container.createAgent(WorkerAgent.class, worker2Id);
        ManagerAgent manager = container.createAgent(ManagerAgent.class, managerId);
        MonitorAgent monitor = container.createAgent(MonitorAgent.class, monitorId);

        // 2. 演示Agent管理功能
        logger.info("2. 演示Agent管理功能...");
        
        // 获取所有Agent
        List<Agent> allAgents = container.getAllAgents();
        logger.info("容器中Agent总数: {}", container.getAgentCount());
        logger.info("所有Agent: {}", allAgents.stream()
            .map(agent -> agent.getAgentId().getShortId())
            .toList());

        // 根据ID获取Agent
        Agent retrievedWorker1 = container.getAgent(worker1Id);
        logger.info("根据ID获取Agent: {} -> {}", worker1Id.getShortId(), 
            retrievedWorker1 != null ? "成功" : "失败");

        // 根据名称获取Agent
        Agent retrievedByName = container.getAgent("WorkerAgent-1");
        logger.info("根据名称获取Agent: {} -> {}", "WorkerAgent-1", 
            retrievedByName != null ? "成功" : "失败");

        // 检查Agent是否存在
        boolean containsAgent = container.containsAgent(worker1Id);
        logger.info("检查Agent是否存在: {} -> {}", worker1Id.getShortId(), containsAgent);

        // 3. 演示消息发送功能
        logger.info("3. 演示消息发送功能...");
        Thread.sleep(2000); // 等待Agent完全启动

        // 发送测试消息
        AgentMessage testMessage = AgentMessage.builder()
            .sender(managerId)
            .receiver(worker1Id)
            .performative(MessagePerformative.REQUEST)
            .content("开始工作")
            .build();
        
        // 直接向worker1发送消息
        worker1.handleMessage(testMessage);
        logger.info("发送消息给WorkerAgent-1");

        // 4. 演示Agent状态管理
        logger.info("4. 演示Agent状态管理...");
        
        // 获取容器统计信息
        AgentContainer.ContainerStats stats = container.getStats();
        logger.info("容器统计信息: {}", stats);

        // 5. 演示Agent生命周期控制
        logger.info("5. 演示Agent生命周期控制...");
        
        // 停止单个Agent
        container.stopAgent(worker2Id).join();
        logger.info("停止WorkerAgent-2");

        // 重新启动Agent
        container.startAgent(worker2Id).join();
        logger.info("重新启动WorkerAgent-2");

        // 6. 演示Agent移除功能
        logger.info("6. 演示Agent移除功能...");
        
        // 移除Agent
        Agent removedAgent = container.removeAgent(monitorId);
        logger.info("移除Agent: {} -> {}", monitorId.getShortId(), 
            removedAgent != null ? "成功" : "失败");

        // 再次检查统计信息
        AgentContainer.ContainerStats updatedStats = container.getStats();
        logger.info("更新后的容器统计信息: {}", updatedStats);

        // 7. 让Agent运行一段时间进行交互
        logger.info("7. Agent交互运行...");
        Thread.sleep(5000);

        logger.info("=== 容器功能演示完成 ===");
    }

    /**
     * 工作Agent - 负责处理具体任务
     */
    public static class WorkerAgent extends AbstractAgent {

        private static final Logger logger = LoggerFactory.getLogger(WorkerAgent.class);
        private final AtomicInteger taskCounter = new AtomicInteger(0);

        public WorkerAgent(AgentId agentId) {
            super(agentId);
        }

        @Override
        protected BehaviorScheduler createBehaviorScheduler() {
            return new DefaultBehaviorScheduler();
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            logger.debug("WorkerAgent {} 发送消息: {}", getAgentId().getShortId(), message.content());
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected void doStart() {
            logger.info("WorkerAgent {} 启动成功", getAgentId().getShortId());
        }

        @Override
        protected void doStop() {
            logger.info("WorkerAgent {} 停止", getAgentId().getShortId());
        }

        @Override
        protected void doHandleMessage(AgentMessage message) {
            logger.info("WorkerAgent {} 收到消息: {}", getAgentId().getShortId(), message.content());
            
            // 处理工作任务
            if (message.performative() == MessagePerformative.REQUEST) {
                processTask(message);
            }
        }

        private void processTask(AgentMessage message) {
            int taskNumber = taskCounter.incrementAndGet();
            logger.info("WorkerAgent {} 开始处理任务 #{}", getAgentId().getShortId(), taskNumber);
            
            try {
                // 模拟任务处理时间
                Thread.sleep(1000);
                
                // 发送任务完成响应
                AgentMessage response = AgentMessage.builder()
                    .sender(getAgentId())
                    .receiver(message.sender())
                    .performative(MessagePerformative.INFORM)
                    .content(String.format("任务 #%d 处理完成", taskNumber))
                    .build();
                
                sendMessage(response);
                logger.info("WorkerAgent {} 任务 #{} 处理完成", getAgentId().getShortId(), taskNumber);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("任务处理被中断");
            }
        }
    }

    /**
     * 管理Agent - 负责协调工作Agent
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
            logger.debug("ManagerAgent {} 发送消息: {}", getAgentId().getShortId(), message.content());
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected void doStart() {
            logger.info("ManagerAgent {} 启动成功", getAgentId().getShortId());
        }

        @Override
        protected void doStop() {
            logger.info("ManagerAgent {} 停止", getAgentId().getShortId());
        }

        @Override
        protected void doHandleMessage(AgentMessage message) {
            logger.info("ManagerAgent {} 收到消息: {}", getAgentId().getShortId(), message.content());
            
            // 处理工作Agent的响应
            if (message.performative() == MessagePerformative.INFORM) {
                logger.info("ManagerAgent {} 收到任务完成通知: {}", getAgentId().getShortId(), message.content());
            }
        }
    }

    /**
     * 监控Agent - 负责监控系统状态
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
            logger.debug("MonitorAgent {} 发送消息: {}", getAgentId().getShortId(), message.content());
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected void doStart() {
            logger.info("MonitorAgent {} 启动成功，开始监控系统状态", getAgentId().getShortId());
        }

        @Override
        protected void doStop() {
            logger.info("MonitorAgent {} 停止监控", getAgentId().getShortId());
        }

        @Override
        protected void doHandleMessage(AgentMessage message) {
            logger.info("MonitorAgent {} 收到监控消息: {}", getAgentId().getShortId(), message.content());
            
            // 监控逻辑可以在这里实现
            if (message.performative() == MessagePerformative.INFORM) {
                logger.debug("MonitorAgent {} 处理监控数据", getAgentId().getShortId());
            }
        }
    }
}
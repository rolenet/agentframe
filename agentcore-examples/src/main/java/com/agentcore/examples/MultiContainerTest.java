package com.agentcore.examples;

import com.agentcore.container.AgentContainer;
import com.agentcore.container.ContainerConfig;
import com.agentcore.container.DefaultAgentContainer;
import com.agentcore.core.agent.AbstractAgent;
import com.agentcore.core.agent.Agent;
import com.agentcore.core.agent.AgentId;
import com.agentcore.core.behavior.BehaviorScheduler;
import com.agentcore.core.behavior.DefaultBehaviorScheduler;
import com.agentcore.core.message.AgentMessage;
import com.agentcore.core.message.MessagePerformative;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 多容器协同测试
 * 展示多个容器之间的Agent通信和协同工作
 * 
 * @author AgentCore Team
 */
public class MultiContainerTest {

    private static final Logger logger = LoggerFactory.getLogger(MultiContainerTest.class);

    public static void main(String[] args) {
        logger.info("=== 开始多容器协同测试 ===");

        try {
            // 测试1: 多容器基础通信
            testMultiContainerCommunication();
            
            // 测试2: 容器间负载均衡
            testLoadBalancingBetweenContainers();
            
            // 测试3: 容器故障转移
            testContainerFailover();
            
            // 测试4: 动态容器管理
            testDynamicContainerManagement();

        } catch (Exception e) {
            logger.error("多容器测试过程中发生错误", e);
        }

        logger.info("=== 多容器协同测试完成 ===");
    }

    /**
     * 测试1: 多容器基础通信
     */
    private static void testMultiContainerCommunication() throws Exception {
        logger.info("=== 测试1: 多容器基础通信 ===");

        // 创建两个协同工作的容器
        ContainerConfig container1Config = ContainerConfig.builder("Container-1")
            .maxAgents(5)
            .autoStartAgents(true)
            .build();

        ContainerConfig container2Config = ContainerConfig.builder("Container-2")
            .maxAgents(5)
            .autoStartAgents(true)
            .build();

        AgentContainer container1 = new DefaultAgentContainer(container1Config);
        AgentContainer container2 = new DefaultAgentContainer(container2Config);

        // 启动两个容器
        container1.start().join();
        container2.start().join();
        logger.info("两个容器启动成功");

        // 创建跨容器通信的Agent
        AgentId gatewayId = AgentId.create("GatewayAgent");
        AgentId processor1Id = AgentId.create("ProcessorAgent-1");
        AgentId processor2Id = AgentId.create("ProcessorAgent-2");

        GatewayAgent gateway = container1.createAgent(GatewayAgent.class, gatewayId);
        ProcessorAgent processor1 = container2.createAgent(ProcessorAgent.class, processor1Id);
        ProcessorAgent processor2 = container2.createAgent(ProcessorAgent.class, processor2Id);

        // 设置网关Agent的目标处理器
        gateway.addProcessor(processor1);
        gateway.addProcessor(processor2);

        // 运行通信测试
        Thread.sleep(5000);

        // 获取统计信息
        logger.info("容器1统计: {}", container1.getStats());
        logger.info("容器2统计: {}", container2.getStats());

        // 停止容器
        container1.stop().join();
        container2.stop().join();
        logger.info("多容器基础通信测试完成");
    }

    /**
     * 测试2: 容器间负载均衡
     */
    private static void testLoadBalancingBetweenContainers() throws Exception {
        logger.info("=== 测试2: 容器间负载均衡 ===");

        // 创建多个容器模拟负载均衡环境
        List<AgentContainer> containers = new ArrayList<>();
        List<LoadBalancerAgent> loadBalancers = new ArrayList<>();

        // 创建3个处理容器
        for (int i = 1; i <= 3; i++) {
            ContainerConfig config = ContainerConfig.builder("ProcessingContainer-" + i)
                .maxAgents(10)
                .autoStartAgents(true)
                .build();

            AgentContainer container = new DefaultAgentContainer(config);
            container.start().join();
            containers.add(container);

            // 在每个容器中创建工作Agent
            for (int j = 1; j <= 3; j++) {
                AgentId workerId = AgentId.create("Worker-" + i + "-" + j);
                WorkerAgent worker = container.createAgent(WorkerAgent.class, workerId);
            }
        }

        // 创建负载均衡器容器
        ContainerConfig lbConfig = ContainerConfig.builder("LoadBalancerContainer")
            .maxAgents(5)
            .autoStartAgents(true)
            .build();

        AgentContainer lbContainer = new DefaultAgentContainer(lbConfig);
        lbContainer.start().join();

        // 创建负载均衡器Agent
        AgentId lbId = AgentId.create("LoadBalancer");
        LoadBalancerAgent loadBalancer = lbContainer.createAgent(LoadBalancerAgent.class, lbId);
        loadBalancers.add(loadBalancer);

        // 为负载均衡器注册所有工作Agent
        for (AgentContainer container : containers) {
            List<Agent> workers = container.getAgentsByType("worker");
            for (Agent worker : workers) {
                loadBalancer.registerWorker(worker);
            }
        }

        // 运行负载均衡测试
        Thread.sleep(8000);

        // 输出负载统计
        logger.info("负载均衡器处理请求数: {}", loadBalancer.getProcessedRequests());
        for (AgentContainer container : containers) {
            logger.info("容器 {} 统计: {}", container.getName(), container.getStats());
        }

        // 清理资源
        lbContainer.stop().join();
        for (AgentContainer container : containers) {
            container.stop().join();
        }
        logger.info("容器间负载均衡测试完成");
    }

    /**
     * 测试3: 容器故障转移
     */
    private static void testContainerFailover() throws Exception {
        logger.info("=== 测试3: 容器故障转移 ===");

        // 创建主备容器环境
        ContainerConfig primaryConfig = ContainerConfig.builder("PrimaryContainer")
            .maxAgents(5)
            .autoStartAgents(true)
            .build();

        ContainerConfig backupConfig = ContainerConfig.builder("BackupContainer")
            .maxAgents(5)
            .autoStartAgents(true)
            .build();

        AgentContainer primaryContainer = new DefaultAgentContainer(primaryConfig);
        AgentContainer backupContainer = new DefaultAgentContainer(backupConfig);

        primaryContainer.start().join();
        backupContainer.start().join();

        // 创建监控Agent
        AgentId monitorId = AgentId.create("FailoverMonitor");
        FailoverMonitorAgent monitor = primaryContainer.createAgent(FailoverMonitorAgent.class, monitorId);

        // 创建服务Agent
        AgentId service1Id = AgentId.create("Service-1");
        ServiceAgent service1 = primaryContainer.createAgent(ServiceAgent.class, service1Id);

        AgentId service2Id = AgentId.create("Service-2");
        ServiceAgent service2 = backupContainer.createAgent(ServiceAgent.class, service2Id);

        // 设置监控
        monitor.watchService(service1);
        monitor.watchService(service2);

        // 模拟主容器故障
        Thread.sleep(3000);
        logger.info("模拟主容器故障...");
        primaryContainer.stop().join();

        // 验证故障转移
        Thread.sleep(2000);
        boolean backupActive = service2.isActive();
        logger.info("备份服务状态: {}", backupActive ? "活跃" : "不活跃");

        // 清理
        backupContainer.stop().join();
        logger.info("容器故障转移测试完成");
    }

    /**
     * 测试4: 动态容器管理
     */
    private static void testDynamicContainerManagement() throws Exception {
        logger.info("=== 测试4: 动态容器管理 ===");

        List<AgentContainer> dynamicContainers = new ArrayList<>();

        // 动态创建和销毁容器
        for (int round = 1; round <= 3; round++) {
            logger.info("--- 第 {} 轮动态容器管理 ---", round);

            // 创建新容器
            ContainerConfig config = ContainerConfig.builder("DynamicContainer-" + round)
                .maxAgents(3)
                .autoStartAgents(true)
                .build();

            AgentContainer container = new DefaultAgentContainer(config);
            container.start().join();
            dynamicContainers.add(container);

            // 在容器中创建Agent
            for (int i = 1; i <= 2; i++) {
                AgentId agentId = AgentId.create("DynamicAgent-" + round + "-" + i);
                DynamicAgent agent = container.createAgent(DynamicAgent.class, agentId);
            }

            // 运行一段时间
            Thread.sleep(2000);

            // 输出当前状态
            logger.info("当前运行容器数: {}", dynamicContainers.size());
            for (AgentContainer cont : dynamicContainers) {
                logger.info("容器 {} 状态: {}", cont.getName(), cont.isRunning() ? "运行中" : "已停止");
            }

            // 每隔一轮停止一个旧容器
            if (round > 1) {
                AgentContainer oldContainer = dynamicContainers.get(0);
                oldContainer.stop().join();
                dynamicContainers.remove(0);
                logger.info("停止旧容器: {}", oldContainer.getName());
            }

            Thread.sleep(1000);
        }

        // 清理所有容器
        for (AgentContainer container : dynamicContainers) {
            container.stop().join();
        }
        logger.info("动态容器管理测试完成");
    }

    // ===== 多容器测试Agent实现 =====

    /**
     * 网关Agent - 负责跨容器消息路由
     */
    public static class GatewayAgent extends AbstractAgent {
        private static final Logger logger = LoggerFactory.getLogger(GatewayAgent.class);
        private final List<ProcessorAgent> processors = new ArrayList<>();
        private final AtomicInteger requestCounter = new AtomicInteger(0);

        public GatewayAgent(AgentId agentId) {
            super(agentId);
        }

        @Override
        protected BehaviorScheduler createBehaviorScheduler() {
            return new DefaultBehaviorScheduler();
        }

        public void addProcessor(ProcessorAgent processor) {
            processors.add(processor);
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected void doStart() {
            logger.info("网关Agent {} 启动，连接 {} 个处理器", getAgentId().getShortId(), processors.size());
            startRequestDistribution();
        }

        @Override
        protected void doHandleMessage(AgentMessage message) {
            // 处理外部请求并分发到处理器
            if (message.performative() == MessagePerformative.REQUEST) {
                distributeRequest(message);
            }
        }

        private void startRequestDistribution() {
            Thread distributionThread = new Thread(() -> {
                try {
                    for (int i = 0; i < 10; i++) {
                        if (!processors.isEmpty()) {
                            // 轮询分发请求
                            ProcessorAgent processor = processors.get(i % processors.size());
                            sendRequestToProcessor(processor, i + 1);
                        }
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            distributionThread.start();
        }

        private void distributeRequest(AgentMessage message) {
            requestCounter.incrementAndGet();
            logger.info("网关收到请求，正在分发到处理器...");
        }

        private void sendRequestToProcessor(ProcessorAgent processor, int requestId) {
            AgentMessage request = AgentMessage.builder()
                .sender(getAgentId())
                .receiver(processor.getAgentId())
                .performative(MessagePerformative.REQUEST)
                .content("处理请求 #" + requestId + " from " + getAgentId().getShortId())
                .build();
            sendMessage(request);
            logger.debug("网关分发请求 #{} 到处理器 {}", requestId, processor.getAgentId().getShortId());
        }

        public int getProcessedRequests() {
            return requestCounter.get();
        }
    }

    /**
     * 处理器Agent - 处理网关分发的请求
     */
    public static class ProcessorAgent extends AbstractAgent {
        private static final Logger logger = LoggerFactory.getLogger(ProcessorAgent.class);
        private final AtomicInteger processedCount = new AtomicInteger(0);

        public ProcessorAgent(AgentId agentId) {
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
            logger.info("处理器Agent {} 启动", getAgentId().getShortId());
        }

        @Override
        protected void doHandleMessage(AgentMessage message) {
            if (message.performative() == MessagePerformative.REQUEST) {
                processedCount.incrementAndGet();
                logger.info("处理器 {} 处理请求: {}", getAgentId().getShortId(), message.content());
                
                // 发送处理完成响应
                AgentMessage response = AgentMessage.builder()
                    .sender(getAgentId())
                    .receiver(message.sender())
                    .performative(MessagePerformative.INFORM)
                    .content("请求处理完成 by " + getAgentId().getShortId())
                    .build();
                sendMessage(response);
            }
        }

        public int getProcessedCount() {
            return processedCount.get();
        }
    }

    /**
     * 工作Agent - 用于负载均衡测试
     */
    public static class WorkerAgent extends AbstractAgent {
        private static final Logger logger = LoggerFactory.getLogger(WorkerAgent.class);
        private final AtomicInteger workCount = new AtomicInteger(0);

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
        protected void doHandleMessage(AgentMessage message) {
            if (message.performative() == MessagePerformative.REQUEST) {
                workCount.incrementAndGet();
                logger.debug("工作Agent {} 处理工作单元", getAgentId().getShortId());
            }
        }

        @Override
        public String getType() {
            return "worker";
        }

        public int getWorkCount() {
            return workCount.get();
        }
    }

    /**
     * 负载均衡器Agent
     */
    public static class LoadBalancerAgent extends AbstractAgent {
        private static final Logger logger = LoggerFactory.getLogger(LoadBalancerAgent.class);
        private final List<Agent> workers = new ArrayList<>();
        private final AtomicInteger requestCounter = new AtomicInteger(0);
        private final AtomicInteger currentWorkerIndex = new AtomicInteger(0);

        public LoadBalancerAgent(AgentId agentId) {
            super(agentId);
        }

        @Override
        protected BehaviorScheduler createBehaviorScheduler() {
            return new DefaultBehaviorScheduler();
        }

        public void registerWorker(Agent worker) {
            workers.add(worker);
            logger.info("负载均衡器注册工作Agent: {}", worker.getAgentId().getShortId());
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected void doStart() {
            logger.info("负载均衡器 {} 启动，管理 {} 个工作Agent", getAgentId().getShortId(), workers.size());
            startLoadBalancing();
        }

        @Override
        protected void doHandleMessage(AgentMessage message) {
            // 处理外部请求并进行负载均衡
            if (message.performative() == MessagePerformative.REQUEST) {
                balanceRequest(message);
            }
        }

        private void startLoadBalancing() {
            Thread balancingThread = new Thread(() -> {
                try {
                    for (int i = 0; i < 15; i++) {
                        if (!workers.isEmpty()) {
                            // 简单的轮询负载均衡
                            Agent worker = workers.get(currentWorkerIndex.getAndIncrement() % workers.size());
                            forwardRequestToWorker(worker, i + 1);
                        }
                        Thread.sleep(500);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            balancingThread.start();
        }

        private void balanceRequest(AgentMessage message) {
            requestCounter.incrementAndGet();
        }

        private void forwardRequestToWorker(Agent worker, int requestId) {
            AgentMessage request = AgentMessage.builder()
                .sender(getAgentId())
                .receiver(worker.getAgentId())
                .performative(MessagePerformative.REQUEST)
                .content("负载均衡请求 #" + requestId)
                .build();
            sendMessage(request);
        }

        public int getProcessedRequests() {
            return requestCounter.get();
        }
    }

    /**
     * 故障转移监控Agent
     */
    public static class FailoverMonitorAgent extends AbstractAgent {
        private static final Logger logger = LoggerFactory.getLogger(FailoverMonitorAgent.class);
        private final List<ServiceAgent> watchedServices = new ArrayList<>();

        public FailoverMonitorAgent(AgentId agentId) {
            super(agentId);
        }

        @Override
        protected BehaviorScheduler createBehaviorScheduler() {
            return new DefaultBehaviorScheduler();
        }

        public void watchService(ServiceAgent service) {
            watchedServices.add(service);
            logger.info("监控Agent开始监控服务: {}", service.getAgentId().getShortId());
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected void doStart() {
            logger.info("故障转移监控Agent {} 启动", getAgentId().getShortId());
            startMonitoring();
        }

        private void startMonitoring() {
            Thread monitorThread = new Thread(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        for (ServiceAgent service : watchedServices) {
                            if (!service.isActive()) {
                                logger.warn("检测到服务不可用: {}", service.getAgentId().getShortId());
                            }
                        }
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            monitorThread.start();
        }
    }

    /**
     * 服务Agent - 用于故障转移测试
     */
    public static class ServiceAgent extends AbstractAgent {
        public ServiceAgent(AgentId agentId) {
            super(agentId);
        }

        @Override
        protected com.agentcore.core.behavior.BehaviorScheduler createBehaviorScheduler() {
            return new DefaultBehaviorScheduler();
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected void doStart() {
            logger.info("服务Agent {} 启动", getAgentId().getShortId());
        }
    }

    /**
     * 动态Agent - 用于动态容器管理测试
     */
    public static class DynamicAgent extends AbstractAgent {
        public DynamicAgent(AgentId agentId) {
            super(agentId);
        }

        @Override
        protected BehaviorScheduler createBehaviorScheduler() {
            return new com.agentcore.core.behavior.DefaultBehaviorScheduler();
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected void doStart() {
            logger.info("动态Agent {} 在动态容器中启动", getAgentId().getShortId());
        }

        @Override
        public String getType() {
            return "dynamic";
        }
    }
}
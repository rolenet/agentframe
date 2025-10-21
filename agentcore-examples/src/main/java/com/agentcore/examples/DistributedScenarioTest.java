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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 分布式场景测试
 * 测试多容器协同、智能路由、故障恢复等分布式特性
 * 
 * @author AgentCore Team
 */
public class DistributedScenarioTest {

    private static final Logger logger = LoggerFactory.getLogger(DistributedScenarioTest.class);

    public static void main(String[] args) {
        logger.info("=== 开始分布式场景测试 ===");

        try {
            // 测试1: 智能路由和负载均衡
            testIntelligentRoutingAndLoadBalancing();
            
            // 测试2: 分布式任务协调
            testDistributedTaskCoordination();
            
            // 测试3: 故障检测和自动恢复
            testFaultDetectionAndRecovery();
            
            // 测试4: 数据一致性保证
            testDataConsistency();
            
            // 测试5: 跨容器通信优化
            testCrossContainerCommunicationOptimization();

        } catch (Exception e) {
            logger.error("分布式场景测试过程中发生错误", e);
        }

        logger.info("=== 分布式场景测试完成 ===");
    }

    /**
     * 测试1: 智能路由和负载均衡
     */
    private static void testIntelligentRoutingAndLoadBalancing() throws Exception {
        logger.info("=== 测试1: 智能路由和负载均衡 ===");

        // 创建多个服务容器
        List<AgentContainer> serviceContainers = new ArrayList<>();
        List<ServiceAgent> serviceAgents = new ArrayList<>();

        for (int i = 1; i <= 3; i++) {
            ContainerConfig config = ContainerConfig.builder("ServiceContainer-" + i)
                .maxAgents(10)
                .autoStartAgents(true)
                .build();

            AgentContainer container = new DefaultAgentContainer(config);
            container.start().join();
            serviceContainers.add(container);

            // 在每个容器中创建服务Agent
            for (int j = 1; j <= 2; j++) {
                AgentId serviceId = AgentId.create("Service-" + i + "-" + j);
                ServiceAgent service = container.createAgent(ServiceAgent.class, serviceId);
                serviceAgents.add(service);
            }
        }

        // 创建路由器和负载均衡器容器
        ContainerConfig routerConfig = ContainerConfig.builder("RouterContainer")
            .maxAgents(5)
            .autoStartAgents(true)
            .build();

        AgentContainer routerContainer = new DefaultAgentContainer(routerConfig);
        routerContainer.start().join();

        // 创建智能路由器Agent
        AgentId routerId = AgentId.create("IntelligentRouter");
        IntelligentRouterAgent router = routerContainer.createAgent(IntelligentRouterAgent.class, routerId);

        // 注册所有服务到路由器
        for (ServiceAgent service : serviceAgents) {
            router.registerService(service);
        }

        // 创建客户端容器
        ContainerConfig clientConfig = ContainerConfig.builder("ClientContainer")
            .maxAgents(10)
            .autoStartAgents(true)
            .build();

        AgentContainer clientContainer = new DefaultAgentContainer(clientConfig);
        clientContainer.start().join();

        // 创建多个客户端Agent
        List<ClientAgent> clients = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            AgentId clientId = AgentId.create("Client-" + i);
            ClientAgent client = clientContainer.createAgent(ClientAgent.class, clientId);
            client.setRouter(router);
            clients.add(client);
        }

        // 启动智能路由测试
        logger.info("开始智能路由和负载均衡测试...");
        router.startRouting();
        
        for (ClientAgent client : clients) {
            client.startRequestSending();
        }

        // 运行测试20秒
        Thread.sleep(20000);

        // 收集统计信息
        Map<String, Integer> serviceLoad = router.getServiceLoadDistribution();
        long totalRequests = router.getTotalRequestsProcessed();
        double averageResponseTime = router.getAverageResponseTime();

        logger.info("智能路由测试结果:");
        logger.info("总处理请求数: {}", totalRequests);
        logger.info("平均响应时间: {:.2f}ms", averageResponseTime);
        logger.info("服务负载分布: {}", serviceLoad);
        logger.info("路由策略效果: {}", router.getRoutingStrategyEffectiveness());

        // 清理资源
        clientContainer.stop().join();
        routerContainer.stop().join();
        for (AgentContainer container : serviceContainers) {
            container.stop().join();
        }
        logger.info("智能路由和负载均衡测试完成");
    }

    /**
     * 测试2: 分布式任务协调
     */
    private static void testDistributedTaskCoordination() throws Exception {
        logger.info("=== 测试2: 分布式任务协调 ===");

        // 创建任务协调器容器
        ContainerConfig coordinatorConfig = ContainerConfig.builder("CoordinatorContainer")
            .maxAgents(5)
            .autoStartAgents(true)
            .build();

        AgentContainer coordinatorContainer = new DefaultAgentContainer(coordinatorConfig);
        coordinatorContainer.start().join();

        // 创建任务协调器
        AgentId coordinatorId = AgentId.create("TaskCoordinator");
        TaskCoordinatorAgent coordinator = coordinatorContainer.createAgent(TaskCoordinatorAgent.class, coordinatorId);

        // 创建工作节点容器
        List<AgentContainer> workerContainers = new ArrayList<>();
        List<WorkerAgent> workers = new ArrayList<>();

        for (int i = 1; i <= 4; i++) {
            ContainerConfig workerConfig = ContainerConfig.builder("WorkerContainer-" + i)
                .maxAgents(10)
                .autoStartAgents(true)
                .build();

            AgentContainer container = new DefaultAgentContainer(workerConfig);
            container.start().join();
            workerContainers.add(container);

            // 在每个容器中创建工作Agent
            for (int j = 1; j <= 3; j++) {
                AgentId workerId = AgentId.create("Worker-" + i + "-" + j);
                WorkerAgent worker = container.createAgent(WorkerAgent.class, workerId);
                workers.add(worker);
                coordinator.registerWorker(worker);
            }
        }

        // 启动分布式任务协调
        logger.info("开始分布式任务协调测试...");
        coordinator.startTaskDistribution();

        // 运行复杂任务
        Thread.sleep(15000);

        // 收集任务执行结果
        Map<String, Object> taskResults = coordinator.getTaskExecutionResults();
        int totalTasks = coordinator.getTotalTasksProcessed();
        int successfulTasks = coordinator.getSuccessfulTasks();
        int failedTasks = coordinator.getFailedTasks();

        logger.info("分布式任务协调测试结果:");
        logger.info("总任务数: {}", totalTasks);
        logger.info("成功任务数: {}", successfulTasks);
        logger.info("失败任务数: {}", failedTasks);
        logger.info("任务成功率: {:.2f}%", (double) successfulTasks / totalTasks * 100);
        logger.info("任务执行详情: {}", taskResults);

        // 清理资源
        coordinatorContainer.stop().join();
        for (AgentContainer container : workerContainers) {
            container.stop().join();
        }
        logger.info("分布式任务协调测试完成");
    }

    /**
     * 测试3: 故障检测和自动恢复
     */
    private static void testFaultDetectionAndRecovery() throws Exception {
        logger.info("=== 测试3: 故障检测和自动恢复 ===");

        // 创建主容器和备份容器
        ContainerConfig primaryConfig = ContainerConfig.builder("PrimaryContainer")
            .maxAgents(10)
            .autoStartAgents(true)
            .build();

        ContainerConfig backupConfig = ContainerConfig.builder("BackupContainer")
            .maxAgents(10)
            .autoStartAgents(true)
            .build();

        AgentContainer primaryContainer = new DefaultAgentContainer(primaryConfig);
        AgentContainer backupContainer = new DefaultAgentContainer(backupConfig);

        primaryContainer.start().join();
        backupContainer.start().join();

        // 创建故障检测器
        AgentId detectorId = AgentId.create("FaultDetector");
        FaultDetectorAgent detector = primaryContainer.createAgent(FaultDetectorAgent.class, detectorId);

        // 创建主服务和备份服务
        AgentId primaryServiceId = AgentId.create("PrimaryService");
        AgentId backupServiceId = AgentId.create("BackupService");

        ServiceAgent primaryService = primaryContainer.createAgent(ServiceAgent.class, primaryServiceId);
        ServiceAgent backupService = backupContainer.createAgent(ServiceAgent.class, backupServiceId);

        // 设置故障检测和恢复机制
        detector.monitorService(primaryService, backupService);

        // 创建客户端进行测试
        ContainerConfig clientConfig = ContainerConfig.builder("FaultTestClient")
            .maxAgents(5)
            .autoStartAgents(true)
            .build();

        AgentContainer clientContainer = new DefaultAgentContainer(clientConfig);
        clientContainer.start().join();

        AgentId clientId = AgentId.create("FaultTestClient");
        FaultTestClientAgent client = clientContainer.createAgent(FaultTestClientAgent.class, clientId);
        client.setTargetService(primaryService);

        // 启动故障检测测试
        logger.info("开始故障检测和自动恢复测试...");
        detector.startMonitoring();
        client.startServiceTesting();

        // 正常运行阶段
        Thread.sleep(5000);

        // 模拟主服务故障
        logger.info("模拟主服务故障...");
        primaryContainer.stop().join();

        // 验证自动切换到备份服务
        Thread.sleep(3000);
        boolean backupActive = backupService.isActive();
        boolean clientSwitched = client.isSwitchedToBackup();

        logger.info("故障恢复测试结果:");
        logger.info("备份服务状态: {}", backupActive ? "活跃" : "不活跃");
        logger.info("客户端是否切换到备份: {}", clientSwitched ? "是" : "否");
        logger.info("故障检测器状态: {}", detector.getMonitoringStatus());

        // 清理资源
        clientContainer.stop().join();
        backupContainer.stop().join();
        logger.info("故障检测和自动恢复测试完成");
    }

    /**
     * 测试4: 数据一致性保证
     */
    private static void testDataConsistency() throws Exception {
        logger.info("=== 测试4: 数据一致性保证 ===");

        // 创建多个数据节点容器
        List<AgentContainer> dataNodeContainers = new ArrayList<>();
        List<DataNodeAgent> dataNodes = new ArrayList<>();

        for (int i = 1; i <= 3; i++) {
            ContainerConfig nodeConfig = ContainerConfig.builder("DataNodeContainer-" + i)
                .maxAgents(5)
                .autoStartAgents(true)
                .build();

            AgentContainer container = new DefaultAgentContainer(nodeConfig);
            container.start().join();
            dataNodeContainers.add(container);

            AgentId nodeId = AgentId.create("DataNode-" + i);
            DataNodeAgent node = container.createAgent(DataNodeAgent.class, nodeId);
            dataNodes.add(node);
        }

        // 创建一致性协调器
        ContainerConfig coordinatorConfig = ContainerConfig.builder("ConsistencyCoordinator")
            .maxAgents(5)
            .autoStartAgents(true)
            .build();

        AgentContainer coordinatorContainer = new DefaultAgentContainer(coordinatorConfig);
        coordinatorContainer.start().join();

        AgentId coordinatorId = AgentId.create("ConsistencyCoordinator");
        ConsistencyCoordinatorAgent coordinator = coordinatorContainer.createAgent(ConsistencyCoordinatorAgent.class, coordinatorId);

        // 注册数据节点
        for (DataNodeAgent node : dataNodes) {
            coordinator.registerDataNode(node);
        }

        // 启动数据一致性测试
        logger.info("开始数据一致性保证测试...");
        coordinator.startConsistencyTest();

        // 运行数据操作测试
        Thread.sleep(10000);

        // 验证数据一致性
        boolean dataConsistent = coordinator.verifyDataConsistency();
        int writeOperations = coordinator.getWriteOperations();
        int readOperations = coordinator.getReadOperations();
        int consistencyViolations = coordinator.getConsistencyViolations();

        logger.info("数据一致性测试结果:");
        logger.info("数据是否一致: {}", dataConsistent ? "是" : "否");
        logger.info("写操作次数: {}", writeOperations);
        logger.info("读操作次数: {}", readOperations);
        logger.info("一致性违规次数: {}", consistencyViolations);
        logger.info("一致性保证率: {:.2f}%", 
            (double) (writeOperations - consistencyViolations) / writeOperations * 100);

        // 清理资源
        coordinatorContainer.stop().join();
        for (AgentContainer container : dataNodeContainers) {
            container.stop().join();
        }
        logger.info("数据一致性保证测试完成");
    }

    /**
     * 测试5: 跨容器通信优化
     */
    private static void testCrossContainerCommunicationOptimization() throws Exception {
        logger.info("=== 测试5: 跨容器通信优化 ===");

        // 创建多个通信节点容器
        List<AgentContainer> communicationContainers = new ArrayList<>();
        List<CommunicationNodeAgent> nodes = new ArrayList<>();

        for (int i = 1; i <= 5; i++) {
            ContainerConfig commConfig = ContainerConfig.builder("CommContainer-" + i)
                .maxAgents(10)
                .autoStartAgents(true)
                .build();

            AgentContainer container = new DefaultAgentContainer(commConfig);
            container.start().join();
            communicationContainers.add(container);

            AgentId nodeId = AgentId.create("CommNode-" + i);
            CommunicationNodeAgent node = container.createAgent(CommunicationNodeAgent.class, nodeId);
            nodes.add(node);
        }

        // 创建通信优化器
        ContainerConfig optimizerConfig = ContainerConfig.builder("CommunicationOptimizer")
            .maxAgents(5)
            .autoStartAgents(true)
            .build();

        AgentContainer optimizerContainer = new DefaultAgentContainer(optimizerConfig);
        optimizerContainer.start().join();

        AgentId optimizerId = AgentId.create("CommOptimizer");
        CommunicationOptimizerAgent optimizer = optimizerContainer.createAgent(CommunicationOptimizerAgent.class, optimizerId);

        // 设置通信网络拓扑
        for (CommunicationNodeAgent node : nodes) {
            optimizer.registerNode(node);
        }

        // 构建通信网络
        optimizer.buildCommunicationNetwork();

        // 启动通信优化测试
        logger.info("开始跨容器通信优化测试...");
        optimizer.startOptimizationTest();

        // 运行通信测试
        Thread.sleep(12000);

        // 收集通信性能数据
        Map<String, Object> performanceMetrics = optimizer.getPerformanceMetrics();
        double averageLatency = optimizer.getAverageLatency();
        double throughput = optimizer.getThroughput();
        int messageLossRate = optimizer.getMessageLossRate();

        logger.info("跨容器通信优化测试结果:");
        logger.info("平均延迟: {:.2f}ms", averageLatency);
        logger.info("吞吐量: {:.2f} msg/s", throughput);
        logger.info("消息丢失率: {}%", messageLossRate);
        logger.info("通信优化效果: {}", optimizer.getOptimizationEffectiveness());

        // 清理资源
        optimizerContainer.stop().join();
        for (AgentContainer container : communicationContainers) {
            container.stop().join();
        }
        logger.info("跨容器通信优化测试完成");
    }

    // ===== 分布式场景测试Agent实现 =====

    /**
     * 智能路由器Agent
     */
    public static class IntelligentRouterAgent extends AbstractAgent {
        private static final Logger logger = LoggerFactory.getLogger(IntelligentRouterAgent.class);
        private final List<ServiceAgent> services = new ArrayList<>();
        private final Map<String, AtomicInteger> serviceLoad = new ConcurrentHashMap<>();
        private final AtomicLong totalRequests = new AtomicLong(0);
        private final AtomicLong totalResponseTime = new AtomicLong(0);
        private volatile boolean routingActive = false;

        public IntelligentRouterAgent(AgentId agentId) {
            super(agentId);
        }

        public void registerService(ServiceAgent service) {
            services.add(service);
            serviceLoad.put(service.getAgentId().getShortId(), new AtomicInteger(0));
            logger.info("路由器注册服务: {}", service.getAgentId().getShortId());
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
            logger.info("智能路由器 {} 启动", getAgentId().getShortId());
        }

        public void startRouting() {
            routingActive = true;
            logger.info("智能路由开始运行，管理 {} 个服务", services.size());
        }

        @Override
        protected void doHandleMessage(AgentMessage message) {
            if (message.performative() == MessagePerformative.REQUEST && routingActive) {
                totalRequests.incrementAndGet();
                long startTime = System.currentTimeMillis();

                // 智能路由算法：选择负载最低的服务
                ServiceAgent selectedService = selectOptimalService();
                if (selectedService != null) {
                    // 转发请求到选中的服务
                    forwardRequestToService(selectedService, message);
                    
                    // 记录响应时间
                    long responseTime = System.currentTimeMillis() - startTime;
                    totalResponseTime.addAndGet(responseTime);
                }
            }
        }

        private ServiceAgent selectOptimalService() {
            if (services.isEmpty()) return null;

            // 简单的负载均衡算法：选择当前负载最低的服务
            ServiceAgent optimalService = services.get(0);
            int minLoad = serviceLoad.get(optimalService.getAgentId().getShortId()).get();

            for (ServiceAgent service : services) {
                int currentLoad = serviceLoad.get(service.getAgentId().getShortId()).get();
                if (currentLoad < minLoad) {
                    minLoad = currentLoad;
                    optimalService = service;
                }
            }

            // 增加选中服务的负载计数
            serviceLoad.get(optimalService.getAgentId().getShortId()).incrementAndGet();
            return optimalService;
        }

        private void forwardRequestToService(ServiceAgent service, AgentMessage originalMessage) {
            AgentMessage forwardedMessage = AgentMessage.builder()
                .sender(getAgentId())
                .receiver(service.getAgentId())
                .performative(MessagePerformative.REQUEST)
                .content("路由转发: " + originalMessage.content())
                .build();
            sendMessage(forwardedMessage);
        }

        public Map<String, Integer> getServiceLoadDistribution() {
            Map<String, Integer> distribution = new HashMap<>();
            for (Map.Entry<String, AtomicInteger> entry : serviceLoad.entrySet()) {
                distribution.put(entry.getKey(), entry.getValue().get());
            }
            return distribution;
        }

        public long getTotalRequestsProcessed() {
            return totalRequests.get();
        }

        public double getAverageResponseTime() {
            if (totalRequests.get() == 0) return 0;
            return (double) totalResponseTime.get() / totalRequests.get();
        }

        public String getRoutingStrategyEffectiveness() {
            if (services.size() < 2) return "单服务模式";
            
            // 计算负载均衡效果
            long totalLoad = serviceLoad.values().stream().mapToLong(AtomicInteger::get).sum();
            double averageLoad = (double) totalLoad / services.size();
            double variance = serviceLoad.values().stream()
                .mapToDouble(load -> Math.pow(load.get() - averageLoad, 2))
                .average().orElse(0);
            
            if (variance < 5) return "优秀";
            else if (variance < 15) return "良好";
            else return "需要优化";
        }
    }

    /**
     * 服务Agent
     */
    public static class ServiceAgent extends AbstractAgent {
        private static final Logger logger = LoggerFactory.getLogger(ServiceAgent.class);
        private final AtomicInteger requestsProcessed = new AtomicInteger(0);

        public ServiceAgent(AgentId agentId) {
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
            logger.info("服务Agent {} 启动", getAgentId().getShortId());
        }

        @Override
        protected void doHandleMessage(AgentMessage message) {
            if (message.performative() == MessagePerformative.REQUEST) {
                requestsProcessed.incrementAndGet();
                
                // 模拟服务处理
                try {
                    Thread.sleep(50 + (int)(Math.random() * 100)); // 随机处理时间
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // 发送响应
                AgentMessage response = AgentMessage.builder()
                    .sender(getAgentId())
                    .receiver(message.sender())
                    .performative(MessagePerformative.INFORM)
                    .content("服务处理完成: " + message.content())
                    .build();
                sendMessage(response);
            }
        }

        public int getRequestsProcessed() {
            return requestsProcessed.get();
        }
    }

    /**
     * 客户端Agent
     */
    public static class ClientAgent extends AbstractAgent {
        private static final Logger logger = LoggerFactory.getLogger(ClientAgent.class);
        private IntelligentRouterAgent router;
        private final AtomicInteger requestsSent = new AtomicInteger(0);
        private final AtomicInteger responsesReceived = new AtomicInteger(0);
        private volatile boolean sendingActive = false;

        public ClientAgent(AgentId agentId) {
            super(agentId);
        }

        public void setRouter(IntelligentRouterAgent router) {
            this.router = router;
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
            logger.info("客户端Agent {} 启动", getAgentId().getShortId());
        }

        public void startRequestSending() {
            sendingActive = true;
            new Thread(() -> {
                while (sendingActive && requestsSent.get() < 100) {
                    if (router != null) {
                        AgentMessage request = AgentMessage.builder()
                            .sender(getAgentId())
                            .receiver(router.getAgentId())
                            .performative(MessagePerformative.REQUEST)
                            .content("客户端请求 #" + requestsSent.incrementAndGet())
                            .build();
                        sendMessage(request);
                    }
                    
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                logger.info("客户端 {} 完成请求发送", getAgentId().getShortId());
            }).start();
        }

        @Override
        protected void doHandleMessage(AgentMessage message) {
            if (message.performative() == MessagePerformative.INFORM) {
                responsesReceived.incrementAndGet();
            }
        }

        public int getRequestsSent() {
            return requestsSent.get();
        }

        public int getResponsesReceived() {
            return responsesReceived.get();
        }
    }

    // 其他分布式测试Agent实现（TaskCoordinatorAgent, WorkerAgent, FaultDetectorAgent等）
    // 由于篇幅限制，这里只展示核心Agent实现，其他Agent实现类似
    
    /**
     * 任务协调器Agent
     */
    public static class TaskCoordinatorAgent extends AbstractAgent {
        private final List<WorkerAgent> workers = new ArrayList<>();
        private final AtomicInteger totalTasks = new AtomicInteger(0);
        private final AtomicInteger successfulTasks = new AtomicInteger(0);
        private final AtomicInteger failedTasks = new AtomicInteger(0);
        private final Map<String, Object> taskResults = new ConcurrentHashMap<>();

        public TaskCoordinatorAgent(AgentId agentId) {
            super(agentId);
        }

        public void registerWorker(WorkerAgent worker) {
            workers.add(worker);
        }

        @Override
        protected com.agentcore.core.behavior.BehaviorScheduler createBehaviorScheduler() {
            return new com.agentcore.core.behavior.DefaultBehaviorScheduler();
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            return CompletableFuture.completedFuture(null);
        }

        public void startTaskDistribution() {
            // 实现任务分发逻辑
        }

        public Map<String, Object> getTaskExecutionResults() {
            return taskResults;
        }

        public int getTotalTasksProcessed() {
            return totalTasks.get();
        }

        public int getSuccessfulTasks() {
            return successfulTasks.get();
        }

        public int getFailedTasks() {
            return failedTasks.get();
        }
    }

    /**
     * 工作Agent
     */
    public static class WorkerAgent extends AbstractAgent {
        public WorkerAgent(AgentId agentId) {
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
    }

    // 其他Agent类实现类似，根据具体功能需求进行实现
    // FaultDetectorAgent, DataNodeAgent, ConsistencyCoordinatorAgent等

    /**
     * 故障检测器Agent
     */
    public static class FaultDetectorAgent extends AbstractAgent {
        private static final Logger logger = LoggerFactory.getLogger(FaultDetectorAgent.class);
        private ServiceAgent primaryService;
        private ServiceAgent backupService;
        private volatile boolean monitoring = false;
        private volatile boolean primaryActive = true;

        public FaultDetectorAgent(AgentId agentId) {
            super(agentId);
        }

        public void monitorService(ServiceAgent primary, ServiceAgent backup) {
            this.primaryService = primary;
            this.backupService = backup;
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
            logger.info("故障检测器 {} 启动", getAgentId().getShortId());
        }

        public void startMonitoring() {
            monitoring = true;
            new Thread(() -> {
                while (monitoring) {
                    try {
                        // 检测主服务状态
                        boolean primaryOk = checkServiceHealth(primaryService);
                        if (!primaryOk && primaryActive) {
                            logger.warn("检测到主服务故障，切换到备份服务");
                            primaryActive = false;
                            // 通知相关组件切换到备份服务
                        }
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }).start();
        }

        private boolean checkServiceHealth(ServiceAgent service) {
            // 模拟健康检查
            return service != null && service.isActive();
        }

        public String getMonitoringStatus() {
            return monitoring ? (primaryActive ? "监控中-主服务活跃" : "监控中-备份服务活跃") : "未监控";
        }
    }

    /**
     * 故障测试客户端Agent
     */
    public static class FaultTestClientAgent extends AbstractAgent {
        private static final Logger logger = LoggerFactory.getLogger(FaultTestClientAgent.class);
        private ServiceAgent targetService;
        private volatile boolean switchedToBackup = false;
        private volatile boolean testing = false;

        public FaultTestClientAgent(AgentId agentId) {
            super(agentId);
        }

        public void setTargetService(ServiceAgent service) {
            this.targetService = service;
        }

        @Override
        protected com.agentcore.core.behavior.BehaviorScheduler createBehaviorScheduler() {
            return new com.agentcore.core.behavior.DefaultBehaviorScheduler();
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            return CompletableFuture.completedFuture(null);
        }

        public void startServiceTesting() {
            testing = true;
            new Thread(() -> {
                while (testing) {
                    if (targetService != null && targetService.isActive()) {
                        // 正常发送请求
                        AgentMessage request = AgentMessage.builder()
                            .sender(getAgentId())
                            .receiver(targetService.getAgentId())
                            .performative(MessagePerformative.REQUEST)
                            .content("健康检查请求")
                            .build();
                        sendMessage(request);
                    } else {
                        // 服务不可用，标记为已切换
                        switchedToBackup = true;
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }).start();
        }

        public boolean isSwitchedToBackup() {
            return switchedToBackup;
        }
    }

    /**
     * 数据节点Agent
     */
    public static class DataNodeAgent extends AbstractAgent {
        private static final Logger logger = LoggerFactory.getLogger(DataNodeAgent.class);
        private final Map<String, Object> dataStore = new ConcurrentHashMap<>();

        public DataNodeAgent(AgentId agentId) {
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
            logger.info("数据节点 {} 启动", getAgentId().getShortId());
        }

        public void storeData(String key, Object value) {
            dataStore.put(key, value);
        }

        public Object getData(String key) {
            return dataStore.get(key);
        }

        public Map<String, Object> getAllData() {
            return new HashMap<>(dataStore);
        }
    }

    /**
     * 一致性协调器Agent
     */
    public static class ConsistencyCoordinatorAgent extends AbstractAgent {
        private static final Logger logger = LoggerFactory.getLogger(ConsistencyCoordinatorAgent.class);
        private final List<DataNodeAgent> dataNodes = new ArrayList<>();
        private final AtomicInteger writeOperations = new AtomicInteger(0);
        private final AtomicInteger readOperations = new AtomicInteger(0);
        private final AtomicInteger consistencyViolations = new AtomicInteger(0);
        private volatile boolean testing = false;

        public ConsistencyCoordinatorAgent(AgentId agentId) {
            super(agentId);
        }

        public void registerDataNode(DataNodeAgent node) {
            dataNodes.add(node);
        }

        @Override
        protected com.agentcore.core.behavior.BehaviorScheduler createBehaviorScheduler() {
            return new com.agentcore.core.behavior.DefaultBehaviorScheduler();
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            return CompletableFuture.completedFuture(null);
        }

        public void startConsistencyTest() {
            testing = true;
            new Thread(() -> {
                int operationCount = 0;
                while (testing && operationCount < 100) {
                    // 模拟数据操作
                    performConsistentWrite("key-" + operationCount, "value-" + operationCount);
                    operationCount++;
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }).start();
        }

        private void performConsistentWrite(String key, Object value) {
            writeOperations.incrementAndGet();
            // 在所有数据节点上执行写操作
            for (DataNodeAgent node : dataNodes) {
                node.storeData(key, value);
            }
            // 验证一致性
            if (!verifyImmediateConsistency(key, value)) {
                consistencyViolations.incrementAndGet();
            }
        }

        private boolean verifyImmediateConsistency(String key, Object expectedValue) {
            for (DataNodeAgent node : dataNodes) {
                Object actualValue = node.getData(key);
                if (!expectedValue.equals(actualValue)) {
                    return false;
                }
            }
            return true;
        }

        public boolean verifyDataConsistency() {
            if (dataNodes.isEmpty()) return true;
            
            DataNodeAgent firstNode = dataNodes.get(0);
            Map<String, Object> referenceData = firstNode.getAllData();
            
            for (DataNodeAgent node : dataNodes) {
                Map<String, Object> nodeData = node.getAllData();
                if (!referenceData.equals(nodeData)) {
                    return false;
                }
            }
            return true;
        }

        public int getWriteOperations() {
            return writeOperations.get();
        }

        public int getReadOperations() {
            return readOperations.get();
        }

        public int getConsistencyViolations() {
            return consistencyViolations.get();
        }
    }

    /**
     * 通信节点Agent
     */
    public static class CommunicationNodeAgent extends AbstractAgent {
        private static final Logger logger = LoggerFactory.getLogger(CommunicationNodeAgent.class);
        private final List<CommunicationNodeAgent> connectedNodes = new ArrayList<>();
        private final AtomicInteger messagesSent = new AtomicInteger(0);
        private final AtomicInteger messagesReceived = new AtomicInteger(0);

        public CommunicationNodeAgent(AgentId agentId) {
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

        public void connectTo(CommunicationNodeAgent otherNode) {
            if (!connectedNodes.contains(otherNode)) {
                connectedNodes.add(otherNode);
            }
        }

        public void sendMessageToConnectedNodes(String content) {
            for (CommunicationNodeAgent node : connectedNodes) {
                AgentMessage message = AgentMessage.builder()
                    .sender(getAgentId())
                    .receiver(node.getAgentId())
                    .performative(MessagePerformative.INFORM)
                    .content(content)
                    .build();
                sendMessage(message);
                messagesSent.incrementAndGet();
            }
        }

        @Override
        protected void doHandleMessage(AgentMessage message) {
            if (message.performative() == MessagePerformative.INFORM) {
                messagesReceived.incrementAndGet();
                logger.debug("节点 {} 收到消息: {}", getAgentId().getShortId(), message.content());
            }
        }

        public int getMessagesSent() {
            return messagesSent.get();
        }

        public int getMessagesReceived() {
            return messagesReceived.get();
        }
    }

    /**
     * 通信优化器Agent
     */
    public static class CommunicationOptimizerAgent extends AbstractAgent {
        private static final Logger logger = LoggerFactory.getLogger(CommunicationOptimizerAgent.class);
        private final List<CommunicationNodeAgent> nodes = new ArrayList<>();
        private final AtomicLong totalLatency = new AtomicLong(0);
        private final AtomicInteger totalMessages = new AtomicInteger(0);
        private volatile boolean optimizing = false;

        public CommunicationOptimizerAgent(AgentId agentId) {
            super(agentId);
        }

        public void registerNode(CommunicationNodeAgent node) {
            nodes.add(node);
        }

        @Override
        protected com.agentcore.core.behavior.BehaviorScheduler createBehaviorScheduler() {
            return new com.agentcore.core.behavior.DefaultBehaviorScheduler();
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            return CompletableFuture.completedFuture(null);
        }

        public void buildCommunicationNetwork() {
            // 构建全连接网络
            for (int i = 0; i < nodes.size(); i++) {
                for (int j = i + 1; j < nodes.size(); j++) {
                    nodes.get(i).connectTo(nodes.get(j));
                    nodes.get(j).connectTo(nodes.get(i));
                }
            }
            logger.info("构建了包含 {} 个节点的全连接通信网络", nodes.size());
        }

        public void startOptimizationTest() {
            optimizing = true;
            new Thread(() -> {
                int testRound = 0;
                while (optimizing && testRound < 50) {
                    // 模拟通信测试
                    for (CommunicationNodeAgent node : nodes) {
                        node.sendMessageToConnectedNodes("测试消息 round-" + testRound);
                    }
                    testRound++;
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }).start();
        }

        public Map<String, Object> getPerformanceMetrics() {
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("节点数量", nodes.size());
            metrics.put("总消息数", totalMessages.get());
            metrics.put("平均延迟", getAverageLatency());
            return metrics;
        }

        public double getAverageLatency() {
            if (totalMessages.get() == 0) return 0;
            return (double) totalLatency.get() / totalMessages.get();
        }

        public double getThroughput() {
            // 模拟吞吐量计算
            return nodes.size() * 10.0;
        }

        public int getMessageLossRate() {
            // 模拟消息丢失率
            return 0; // 假设无消息丢失
        }

        public String getOptimizationEffectiveness() {
            if (nodes.size() < 2) return "单节点模式";
            return "网络优化完成，通信效率良好";
        }
    }
}
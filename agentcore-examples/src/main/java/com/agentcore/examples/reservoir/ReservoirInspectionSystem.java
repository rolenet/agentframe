package com.agentcore.examples.reservoir;

import com.agentcore.container.AgentContainer;
import com.agentcore.container.ContainerConfig;
import com.agentcore.container.DefaultAgentContainer;
import com.agentcore.core.agent.Agent;
import com.agentcore.core.agent.AgentId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 水库日常巡检多智能体系统
 * 
 * 系统包含以下智能体：
 * 1. DataCollectionAgent - 数据采集智能体
 * 2. DataStatisticsAgent - 数据统计智能体  
 * 3. WeatherSummaryAgent - 天气汇总智能体
 * 4. DutyInfoAgent - 值班信息查看智能体
 * 5. DocumentOutputAgent - 文档输出智能体
 * 
 * @author AgentCore Team
 */
public class ReservoirInspectionSystem {

    private static final Logger logger = LoggerFactory.getLogger(ReservoirInspectionSystem.class);
    
    // 异步通知管理器
    private static AsyncNotificationManager notificationManager;
    private static AsyncNotificationHandler notificationHandler;

    public static void main(String[] args) {
        logger.info("=== 水库日常巡检多智能体系统启动 ===");

        try {
            // 1. 创建容器配置
            ContainerConfig config = ContainerConfig.builder("ReservoirInspectionContainer")
                .maxAgents(10)
                .agentStartTimeout(Duration.ofSeconds(10))
                .agentStopTimeout(Duration.ofSeconds(5))
                .autoStartAgents(true)
                .enableMonitoring(true)
                .property("reservoir.name", "东江水库")
                .property("inspection.date", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .build();

            logger.info("创建水库巡检容器配置: {}", config.name());

            // 2. 创建容器实例
            AgentContainer container = new DefaultAgentContainer(config);
            
            // 3. 设置事件监听器
            setupEventListeners(container);

            // 4. 启动容器
            logger.info("启动巡检系统容器...");
            container.start().join();
            logger.info("巡检系统容器启动成功");

            // 5. 初始化异步通知系统
            initializeAsyncNotificationSystem(container);

            // 6. 创建并启动所有智能体
            setupInspectionAgents(container);

            // 7. 异步等待所有智能体启动完成，然后开始巡检流程
            startAsyncInspectionProcess(container);

            // 8. 异步等待整个工作流完成
            waitForWorkflowCompletion();

            // 9. 停止容器
            logger.info("停止巡检系统容器...");
            container.stop().join();
            logger.info("巡检系统容器停止成功");
            
            // 10. 清理异步通知系统
            cleanupAsyncNotificationSystem();

        } catch (Exception e) {
            logger.error("水库巡检系统运行过程中发生错误", e);
        }

        logger.info("=== 水库日常巡检多智能体系统结束 ===");
    }

    /**
     * 设置事件监听器
     */
    private static void setupEventListeners(AgentContainer container) {
        container.setAgentEventListener(new AgentContainer.AgentEventListener() {
            @Override
            public void onAgentAdded(Agent agent) {
                logger.info("🤖 智能体已添加: {}", agent.getAgentId().getShortId());
            }

            @Override
            public void onAgentRemoved(Agent agent) {
                logger.info("🤖 智能体已移除: {}", agent.getAgentId().getShortId());
            }

            @Override
            public void onAgentStarted(Agent agent) {
                logger.info("✅ 智能体已启动: {}", agent.getAgentId().getShortId());
            }

            @Override
            public void onAgentStopped(Agent agent) {
                logger.info("⏹️智能体已停止: {}", agent.getAgentId().getShortId());
            }

            @Override
            public void onAgentError(Agent agent, Throwable error) {
                logger.error("❌ 智能体错误: {}, 错误信息: {}", 
                    agent.getAgentId().getShortId(), error.getMessage());
            }
        });
    }

    /**
     * 初始化异步通知系统
     */
    private static void initializeAsyncNotificationSystem(AgentContainer container) {
        logger.info("🔔 初始化异步通知系统...");
        
        // 创建异步通知管理器
        notificationManager = new AsyncNotificationManager();
        notificationHandler = new AsyncNotificationHandler(notificationManager);
        
        // 设置期望的智能体列表
        Set<AgentId> expectedAgents = Set.of(
            AgentId.create("DataCollectionAgent"),
            AgentId.create("DataStatisticsAgent"),
            AgentId.create("WeatherSummaryAgent"),
            AgentId.create("DutyInfoAgent"),
            AgentId.create("DocumentOutputAgent")
        );
        notificationManager.setExpectedAgents(expectedAgents);
        
        // 设置回调函数
        notificationManager.setOnAllAgentsReady(unused -> {
            logger.info("🔔 🎉 所有智能体启动完成回调触发！开始巡检工作...");
            try {
                triggerInspectionWork(container);
            } catch (Exception e) {
                logger.error("🔔 触发巡检工作时发生错误", e);
            }
        });
        
        notificationManager.setOnWorkflowFinished(unused -> {
            logger.info("🔔 🎉 整个工作流完成回调触发！");
        });
        
        logger.info("🔔 异步通知系统初始化完成");
    }
    
    /**
     * 创建并设置所有巡检智能体,以及智能体之间的关系
     */
    private static void setupInspectionAgents(AgentContainer container) throws Exception {
        logger.info("创建巡检智能体...");

        // 创建智能体ID
        AgentId dataCollectorId = AgentId.create("DataCollectionAgent");
        AgentId statisticsId = AgentId.create("DataStatisticsAgent");
        AgentId weatherId = AgentId.create("WeatherSummaryAgent");
        AgentId dutyId = AgentId.create("DutyInfoAgent");
        AgentId documentId = AgentId.create("DocumentOutputAgent");

        // 创建智能体实例
        logger.info("🔔 开始创建智能体，notificationHandler已准备就绪");
        
        DataCollectionAgent dataCollector = container.createAgent(DataCollectionAgent.class, dataCollectorId);
        // 立即设置notificationHandler，防止启动时丢失
        dataCollector.setNotificationHandler(notificationHandler);
        
        DataStatisticsAgent statistics = container.createAgent(DataStatisticsAgent.class, statisticsId);
        statistics.setNotificationHandler(notificationHandler);
        
        WeatherSummaryAgent weather = container.createAgent(WeatherSummaryAgent.class, weatherId);
        weather.setNotificationHandler(notificationHandler);
        
        DutyInfoAgent duty = container.createAgent(DutyInfoAgent.class, dutyId);
        duty.setNotificationHandler(notificationHandler);
        
        DocumentOutputAgent document = container.createAgent(DocumentOutputAgent.class, documentId);
        document.setNotificationHandler(notificationHandler);

        // 智能体已经通过容器自动获得MessageRouter，无需手动设置
        logger.info("🔥🔥🔥 智能体将通过容器自动获得MessageRouter");
        logger.info("🔔 AsyncNotificationHandler已设置给所有智能体");

        // 设置智能体之间的协作关系
        dataCollector.setCollaborators(Arrays.asList(statisticsId, documentId));
        statistics.setCollaborators(Arrays.asList(documentId));
        weather.setCollaborators(Arrays.asList(documentId));
        duty.setCollaborators(Arrays.asList(documentId));
        document.setCollaborators(Arrays.asList(dataCollectorId, statisticsId, weatherId, dutyId));

        // TODO: 设置DocumentOutputAgent的期望工作智能体 - 期望所有4个工作智能体
        document.setExpectedWorkingAgents(Set.of(dataCollectorId, statisticsId, weatherId, dutyId));
        logger.info("📄 异步工作流处理已设置，期望工作智能体: {}", Arrays.asList(dataCollectorId, statisticsId, weatherId, dutyId));
        // 通过容器接口获取MessageRouter并设置给每个智能体
        com.agentcore.communication.router.MessageRouter messageRouter = container.getMessageRouter();

        dataCollector.setMessageRouter(messageRouter);
        statistics.setMessageRouter(messageRouter);
        weather.setMessageRouter(messageRouter);
        duty.setMessageRouter(messageRouter);
        document.setMessageRouter(messageRouter);

        logger.info("🔥🔥🔥 CRITICAL: MessageRouter已设置给所有智能体");
        logger.info("所有巡检智能体创建完成");
    }

    /**
     * 异步开始巡检流程
     */
    private static void startAsyncInspectionProcess(AgentContainer container) {
        logger.info("🚀 开始异步水库日常巡检流程...");
        
        // 异步等待所有智能体启动完成
        notificationManager.waitForAllAgentsReady()
            .thenRun(() -> {
                logger.info("🚀 所有智能体启动完成，巡检流程将由回调函数触发");
            })
            .exceptionally(throwable -> {
                logger.error("🚀 等待智能体启动时发生错误", throwable);
                return null;
            });
    }
    
    /**
     * 触发巡检工作（由异步回调调用）
     */
    private static void triggerInspectionWork(AgentContainer container) throws Exception {
        logger.info("🚀 触发巡检工作，所有智能体已就绪！");
        
        // 确保DocumentOutputAgent已准备好接收消息
        Agent documentAgent = container.getAgent("DocumentOutputAgent");
        if (documentAgent == null) {
            logger.error("❌ DocumentOutputAgent未找到，无法继续巡检流程");
            return;
        }
        logger.info("📄 DocumentOutputAgent已准备就绪");

        // 并行启动所有工作智能体，无需等待
        CompletableFuture<Void> dataCollectionFuture = CompletableFuture.runAsync(() -> {
            Agent dataCollector = container.getAgent("DataCollectionAgent");
            if (dataCollector instanceof DataCollectionAgent) {
                logger.info("📊 启动数据采集智能体...");
                ((DataCollectionAgent) dataCollector).startInspection();
            }
        });

        CompletableFuture<Void> weatherFuture = CompletableFuture.runAsync(() -> {
            Agent weatherAgent = container.getAgent("WeatherSummaryAgent");
            if (weatherAgent instanceof WeatherSummaryAgent) {
                logger.info("🌤️ 启动天气汇总智能体...");
                ((WeatherSummaryAgent) weatherAgent).startWeatherCollection();
            }
        });

        CompletableFuture<Void> dutyFuture = CompletableFuture.runAsync(() -> {
            Agent dutyAgent = container.getAgent("DutyInfoAgent");
            if (dutyAgent instanceof DutyInfoAgent) {
                logger.info("👥 启动值班信息智能体...");
                ((DutyInfoAgent) dutyAgent).startDutyInfoQuery();
            }
        });
        CompletableFuture<Void> statisticFuture = CompletableFuture.runAsync(() -> {
            Agent DataStatistic = container.getAgent("DataStatisticsAgent");
            if (DataStatistic instanceof DataStatisticsAgent) {
                logger.info("👥 启动统计信息智能体...");
                ((DataStatisticsAgent) DataStatistic).startStatistics();
            }
        });

        // 等待所有启动任务完成
        CompletableFuture.allOf(statisticFuture,dataCollectionFuture, weatherFuture, dutyFuture)
            .thenRun(() -> {
                logger.info("🚀 所有工作智能体已启动，开始异步协作工作...");
                logger.info("📄 DocumentOutputAgent正在异步等待各智能体的工作完成通知...");
            })
            .exceptionally(throwable -> {
                logger.error("🚀 启动工作智能体时发生错误", throwable);
                return null;
            });
    }
    
    /**
     * 异步等待工作流完成
     */
    private static void waitForWorkflowCompletion() {
        logger.info("🚀 异步等待整个工作流完成...");
        
        try {
            // 等待工作流完成，设置超时时间
            notificationManager.waitForWorkflowCompletion()
                .get(60, TimeUnit.SECONDS);
            logger.info("🚀 🎉 整个异步工作流已完成！");
        } catch (Exception e) {
            logger.error("🚀 等待工作流完成时发生错误", e);
        }
    }
    
    /**
     * 清理异步通知系统
     */
    private static void cleanupAsyncNotificationSystem() {
        logger.info("🔔 清理异步通知系统...");
        if (notificationManager != null) {
            notificationManager.shutdown();
        }
        logger.info("🔔 异步通知系统清理完成");
    }

}
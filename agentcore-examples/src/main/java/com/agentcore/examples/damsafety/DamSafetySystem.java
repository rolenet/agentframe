package com.agentcore.examples.damsafety;

import com.agentcore.communication.router.MessageRouter;
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

/**
 * 水库大坝安全多智能体协作系统
 * 系统包含以下智能体：
 * 1. DataCollectionAgent - 数据采集智能体
 * 2. DataProcessingAgent - 数据处理智能体
 * 3. CoordinatorAgent - 协调智能体
 * 4. AnalysisAgent - 分析智能体
 * 5. ReportAgent - 报告智能体
 * 工作流程：
 * 采集智能体采集数据 -> 数据处理智能体处理 -> 协调智能体分发给分析智能体 
 * -> 分析智能体分析 -> 报告智能体生成报告 -> 协调智能体接收完成并停止系统
 * 
 * @author AgentCore Team
 */
public class DamSafetySystem {

    private static final Logger logger = LoggerFactory.getLogger(DamSafetySystem.class);

    public static void main(String[] args) {
        logger.info("=== 水库大坝安全多智能体协作系统启动 ===");

        try {
            // 1. 创建容器配置
            ContainerConfig config = ContainerConfig.builder("DamSafetyContainer")
                .maxAgents(10)
                .agentStartTimeout(Duration.ofSeconds(10))
                .agentStopTimeout(Duration.ofSeconds(5))
                .autoStartAgents(true)
                .enableMonitoring(true)
                .property("dam.name", "示例水库大坝")
                .property("monitoring.date", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .property("system.version", "1.0")
                .build();

            logger.info("创建大坝安全监测容器配置: {}", config.name());

            // 2. 创建容器实例
            AgentContainer container = new DefaultAgentContainer(config);
            
            // 3. 设置事件监听器
            setupEventListeners(container);

            // 4. 启动容器
            logger.info("启动大坝安全监测系统容器...");
            container.start().join();
            logger.info("大坝安全监测系统容器启动成功");

            // 5. 创建并启动所有智能体
            setupDamSafetyAgents(container);

            // 6. 等待所有智能体启动完成
            Thread.sleep(2000);

            // 7. 启动系统工作流程
            startDamSafetyWorkflow(container);

            // 8. 等待工作流程完成
            waitForWorkflowCompletion();

            // 9. 停止容器
            logger.info("停止大坝安全监测系统容器...");
            container.stop().join();
            logger.info("大坝安全监测系统容器停止成功");

        } catch (Exception e) {
            logger.error("大坝安全监测系统运行过程中发生错误", e);
        }

        logger.info("=== 水库大坝安全多智能体协作系统结束 ===");
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
                logger.info("⏹️ 智能体已停止: {}", agent.getAgentId().getShortId());
            }

            @Override
            public void onAgentError(Agent agent, Throwable error) {
                logger.error("❌ 智能体错误: {}, 错误信息: {}", 
                    agent.getAgentId().getShortId(), error.getMessage());
            }
        });
    }

    /**
     * 创建并设置所有大坝安全智能体
     */
    private static void setupDamSafetyAgents(AgentContainer container){
        logger.info("创建大坝安全智能体...");

        // 创建智能体ID
        AgentId dataCollectorId = AgentId.create("DataCollectionAgent");
        AgentId dataProcessorId = AgentId.create("DataProcessingAgent");
        AgentId coordinatorId = AgentId.create("CoordinatorAgent");
        AgentId analysisAgentId = AgentId.create("AnalysisAgent");
        AgentId reportAgentId = AgentId.create("ReportAgent");

        // 创建智能体实例
        DataCollectionAgent dataCollector = container.createAgent(DataCollectionAgent.class, dataCollectorId);
        DataProcessingAgent dataProcessor = container.createAgent(DataProcessingAgent.class, dataProcessorId);
        CoordinatorAgent coordinator = container.createAgent(CoordinatorAgent.class, coordinatorId);
        AnalysisAgent analysisAgent = container.createAgent(AnalysisAgent.class, analysisAgentId);
        ReportAgent reportAgent = container.createAgent(ReportAgent.class, reportAgentId);

        // 通过容器接口获取MessageRouter并设置给每个智能体
        MessageRouter messageRouter = container.getMessageRouter();

        dataCollector.setMessageRouter(messageRouter);
        dataProcessor.setMessageRouter(messageRouter);
        coordinator.setMessageRouter(messageRouter);
        analysisAgent.setMessageRouter(messageRouter);
        reportAgent.setMessageRouter(messageRouter);

        logger.info("🔥 MessageRouter已设置给所有智能体");

        // 设置智能体之间的协作关系
        dataCollector.setDataProcessorId(dataProcessorId);
        dataProcessor.setCoordinatorId(coordinatorId);
        coordinator.setCollaborators(dataCollectorId, dataProcessorId, analysisAgentId, reportAgentId);
        analysisAgent.setCoordinatorId(coordinatorId);
        reportAgent.setCoordinatorId(coordinatorId);

        logger.info("所有大坝安全智能体创建完成");
        logger.info("🔗 智能体协作关系已建立:");
        logger.info("   🔍 数据采集智能体 -> 📊 数据处理智能体");
        logger.info("   📊 数据处理智能体 -> 🎯 协调智能体");
        logger.info("   🎯 协调智能体 -> 🔬 分析智能体");
        logger.info("   🔬 分析智能体 -> 📋 报告智能体");
        logger.info("   📋 报告智能体 -> 🎯 协调智能体");
    }

    /**
     * 启动大坝安全工作流程
     */
    private static void startDamSafetyWorkflow(AgentContainer container){
        logger.info("🚀 启动大坝安全监测工作流程...");
        
        // 获取协调智能体并启动系统
        Agent coordinatorAgent = container.getAgent("CoordinatorAgent");
        if (coordinatorAgent instanceof CoordinatorAgent) {
            CoordinatorAgent coordinator = (CoordinatorAgent) coordinatorAgent;
            
            // 启动系统工作流程
            coordinator.startDamSafetySystem();
            logger.info("🚀 大坝安全监测工作流程已启动");
        } else {
            logger.error("❌ 协调智能体未找到，无法启动工作流程");
        }
    }

    /**
     * 等待工作流程完成
     */
    private static void waitForWorkflowCompletion() {
        logger.info("⏳ 等待大坝安全监测工作流程完成...");
        
        try {
            // 等待工作流程完成，预计需要8-12秒
            Thread.sleep(12000);
            logger.info("🎉 大坝安全监测工作流程已完成！");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("⏳ 等待工作流程完成时被中断");
        }
    }
}
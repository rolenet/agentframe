package com.agentcore.examples.damsafety;

import com.agentcore.communication.router.MessageRouter;
import com.agentcore.core.agent.AbstractAgent;
import com.agentcore.core.agent.AgentId;
import com.agentcore.core.behavior.BehaviorScheduler;
import com.agentcore.core.behavior.DefaultBehaviorScheduler;
import com.agentcore.core.message.AgentMessage;
import com.agentcore.core.message.MessagePerformative;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 协调智能体
 * 负责协调各智能体的工作流程，管理整个系统的运行
 */
public class CoordinatorAgent extends AbstractAgent {
    private static final Logger logger = LoggerFactory.getLogger(CoordinatorAgent.class);
    
    private MessageRouter messageRouter;
    private AgentId dataCollectorId;
    private AgentId dataProcessorId;
    private AgentId analysisAgentId;
    private AgentId reportAgentId;
    
    private final AtomicInteger completedTasks = new AtomicInteger(0);
    private boolean systemRunning = false;
    private Map<String, Object> processedData;

    public CoordinatorAgent(AgentId agentId) {
        super(agentId);
    }
    
    public void setMessageRouter(MessageRouter messageRouter) {
        this.messageRouter = messageRouter;
    }
    
    public void setCollaborators(AgentId dataCollectorId, AgentId dataProcessorId, 
                               AgentId analysisAgentId, AgentId reportAgentId) {
        this.dataCollectorId = dataCollectorId;
        this.dataProcessorId = dataProcessorId;
        this.analysisAgentId = analysisAgentId;
        this.reportAgentId = reportAgentId;
    }

    @Override
    protected BehaviorScheduler createBehaviorScheduler() {
        return new DefaultBehaviorScheduler();
    }

    @Override
    protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
        if (messageRouter != null) {
            return messageRouter.routeMessage(message);
        } else {
            logger.error("MessageRouter未设置，无法发送消息");
            return CompletableFuture.failedFuture(new IllegalStateException("MessageRouter未设置"));
        }
    }

    @Override
    protected void doStart() {
        logger.info("🎯 协调智能体启动成功");
    }

    @Override
    protected void doStop() {
        logger.info("🎯 协调智能体停止");
        systemRunning = false;
    }

    @Override
    protected void doHandleMessage(AgentMessage message) {
        logger.info("🎯 协调智能体收到消息: {}", message.content());
        
        if (message.content() instanceof DamSafetyMessage) {
            DamSafetyMessage damMessage = (DamSafetyMessage) message.content();
            handleDamSafetyMessage(damMessage);
        } else if (message.content() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> content = (Map<String, Object>) message.content();
            handleControlMessage(content);
        }
    }
    
    /**
     * 处理大坝安全消息
     */
    private void handleDamSafetyMessage(DamSafetyMessage damMessage) {
        switch (damMessage.getMessageType()) {
            case DATA_PROCESSING:
                handleDataProcessingComplete(damMessage);
                break;
            case DATA_ANALYSIS:
                handleAnalysisComplete(damMessage);
                break;
            case REPORT_GENERATION:
                handleReportComplete(damMessage);
                break;
            default:
                logger.info("🎯 收到其他类型消息: {}", damMessage.getMessageType());
        }
    }
    
    /**
     * 处理控制消息
     */
    private void handleControlMessage(Map<String, Object> content) {
        String command = (String) content.get("command");
        if ("START_SYSTEM".equals(command)) {
            startDamSafetySystem();
        } else if ("STOP_SYSTEM".equals(command)) {
            stopDamSafetySystem();
        }
    }
    
    /**
     * 启动大坝安全系统
     */
    public void startDamSafetySystem() {
        if (systemRunning) {
            logger.warn("🎯 系统已在运行中");
            return;
        }
        
        systemRunning = true;
        completedTasks.set(0);
        logger.info("🎯 🚀 启动大坝安全监测系统...");
        
        // 第一步：启动数据采集
        startDataCollection();
    }
    
    /**
     * 停止大坝安全系统
     */
    public void stopDamSafetySystem() {
        systemRunning = false;
        logger.info("🎯 ⏹️ 停止大坝安全监测系统");
        
        // 发送停止指令给所有智能体
        sendStopCommandToAllAgents();
    }
    
    /**
     * 启动数据采集
     */
    private void startDataCollection() {
        logger.info("🎯 第1步: 启动数据采集智能体...");
        
        Map<String, Object> command = new HashMap<>();
        command.put("command", "START_COLLECTION");
        command.put("timestamp", LocalDateTime.now());
        
        AgentMessage message = AgentMessage.builder()
                .sender(getAgentId())
                .receiver(dataCollectorId)
                .performative(MessagePerformative.REQUEST)
                .content(command)
                .build();
        
        sendMessage(message);
    }
    
    /**
     * 处理数据处理完成
     */
    private void handleDataProcessingComplete(DamSafetyMessage damMessage) {
        logger.info("🎯 第2步: 数据处理完成，准备发送给分析智能体...");
        
        this.processedData = damMessage.getDataContent();
        completedTasks.incrementAndGet();
        
        // 发送处理后的数据给分析智能体
        sendDataToAnalysisAgent(processedData);
    }
    
    /**
     * 发送数据给分析智能体
     */
    private void sendDataToAnalysisAgent(Map<String, Object> processedData) {
        logger.info("🎯 第3步: 发送处理后的数据给分析智能体...");
        
        DamSafetyMessage damMessage = DamSafetyMessage.create(
                DamSafetyMessage.MessageType.DATA_PROCESSING,
                getAgentId(),
                analysisAgentId,
                processedData
        );
        
        AgentMessage message = AgentMessage.builder()
                .sender(getAgentId())
                .receiver(analysisAgentId)
                .performative(MessagePerformative.INFORM)
                .content(damMessage)
                .build();
        
        sendMessage(message);
    }
    
    /**
     * 处理分析完成
     */
    private void handleAnalysisComplete(DamSafetyMessage damMessage) {
        logger.info("🎯 第4步: 数据分析完成，准备发送给报告智能体...");
        
        Map<String, Object> analysisResults = damMessage.getDataContent();
        completedTasks.incrementAndGet();
        
        // 发送分析结果给报告智能体
        sendAnalysisToReportAgent(analysisResults);
    }
    
    /**
     * 发送分析结果给报告智能体
     */
    private void sendAnalysisToReportAgent(Map<String, Object> analysisResults) {
        logger.info("🎯 第5步: 发送分析结果给报告智能体...");
        
        DamSafetyMessage damMessage = DamSafetyMessage.create(
                DamSafetyMessage.MessageType.DATA_ANALYSIS,
                getAgentId(),
                reportAgentId,
                analysisResults
        );
        
        AgentMessage message = AgentMessage.builder()
                .sender(getAgentId())
                .receiver(reportAgentId)
                .performative(MessagePerformative.INFORM)
                .content(damMessage)
                .build();
        
        sendMessage(message);
    }
    
    /**
     * 处理报告完成
     */
    private void handleReportComplete(DamSafetyMessage damMessage) {
        logger.info("🎯 第6步: 报告生成完成！");
        
        completedTasks.incrementAndGet();
        
        // 检查是否所有任务都完成
        if (completedTasks.get() >= 3) { // 数据处理、分析、报告三个主要任务
            completeSystemWorkflow();
        }
    }
    
    /**
     * 完成系统工作流
     */
    private void completeSystemWorkflow() {
        logger.info("🎯 🎉 所有任务完成！大坝安全监测工作流程结束");
        logger.info("🎯 📊 工作流程统计: 完成任务数={}, 总耗时=约{}秒", 
                completedTasks.get(), "5-8");
        
        // 发送系统完成通知给所有智能体
        sendSystemCompleteNotification();
        
        // 停止系统
        systemRunning = false;
    }
    
    /**
     * 发送系统完成通知
     */
    private void sendSystemCompleteNotification() {
        logger.info("🎯 发送系统完成通知给所有智能体...");
        
        Map<String, Object> completionData = new HashMap<>();
        completionData.put("systemStatus", "workflow_completed");
        completionData.put("completedTasks", completedTasks.get());
        completionData.put("completionTime", LocalDateTime.now());
        completionData.put("message", "大坝安全监测工作流程已完成");
        
        DamSafetyMessage damMessage = DamSafetyMessage.create(
                DamSafetyMessage.MessageType.SYSTEM_SHUTDOWN,
                getAgentId(),
                null, // 广播消息
                completionData
        );
        
        // 发送给所有智能体
        AgentId[] allAgents = {dataCollectorId, dataProcessorId, analysisAgentId, reportAgentId};
        for (AgentId agentId : allAgents) {
            if (agentId != null) {
                AgentMessage message = AgentMessage.builder()
                        .sender(getAgentId())
                        .receiver(agentId)
                        .performative(MessagePerformative.INFORM)
                        .content(damMessage)
                        .build();
                sendMessage(message);
            }
        }
    }
    
    /**
     * 发送停止指令给所有智能体
     */
    private void sendStopCommandToAllAgents() {
        Map<String, Object> stopCommand = new HashMap<>();
        stopCommand.put("command", "STOP");
        stopCommand.put("timestamp", LocalDateTime.now());
        
        AgentId[] allAgents = {dataCollectorId, dataProcessorId, analysisAgentId, reportAgentId};
        for (AgentId agentId : allAgents) {
            if (agentId != null) {
                AgentMessage message = AgentMessage.builder()
                        .sender(getAgentId())
                        .receiver(agentId)
                        .performative(MessagePerformative.REQUEST)
                        .content(stopCommand)
                        .build();
                sendMessage(message);
            }
        }
    }
}
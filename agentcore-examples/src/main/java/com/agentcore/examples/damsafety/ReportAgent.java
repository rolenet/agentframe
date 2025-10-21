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
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 报告智能体
 * 负责根据分析结果生成大坝安全监测报告
 */
public class ReportAgent extends AbstractAgent {
    private static final Logger logger = LoggerFactory.getLogger(ReportAgent.class);
    
    private MessageRouter messageRouter;
    private AgentId coordinatorId;
    private boolean isGeneratingReport = false;

    public ReportAgent(AgentId agentId) {
        super(agentId);
    }
    
    public void setMessageRouter(MessageRouter messageRouter) {
        this.messageRouter = messageRouter;
    }
    
    public void setCoordinatorId(AgentId coordinatorId) {
        this.coordinatorId = coordinatorId;
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
        logger.info("📋 报告智能体启动成功");
    }

    @Override
    protected void doStop() {
        logger.info("📋 报告智能体停止");
        isGeneratingReport = false;
    }

    @Override
    protected void doHandleMessage(AgentMessage message) {
        logger.info("📋 报告智能体收到消息: {}", message.content());
        
        if (message.content() instanceof DamSafetyMessage) {
            DamSafetyMessage damMessage = (DamSafetyMessage) message.content();
            
            if (damMessage.getMessageType() == DamSafetyMessage.MessageType.DATA_ANALYSIS) {
                generateSafetyReport(damMessage);
            } else if (damMessage.getMessageType() == DamSafetyMessage.MessageType.SYSTEM_SHUTDOWN) {
                handleSystemShutdown(damMessage);
            }
        }
    }
    
    /**
     * 生成安全报告
     */
    private void generateSafetyReport(DamSafetyMessage damMessage) {
        if (isGeneratingReport) {
            logger.warn("📋 报告生成正在进行中");
            return;
        }
        
        isGeneratingReport = true;
        logger.info("📋 开始生成大坝安全监测报告...");
        
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> analysisResults = damMessage.getDataContent();
                
                // 生成报告各个部分
                Map<String, Object> reportHeader = generateReportHeader();
                Thread.sleep(300);
                
                Map<String, Object> executiveSummary = generateExecutiveSummary(analysisResults);
                Thread.sleep(400);
                
                Map<String, Object> detailedFindings = generateDetailedFindings(analysisResults);
                Thread.sleep(500);
                
                Map<String, Object> recommendations = generateRecommendations(analysisResults);
                Thread.sleep(300);
                
                // 组装完整报告
                Map<String, Object> completeReport = assembleCompleteReport(
                        reportHeader, executiveSummary, detailedFindings, recommendations);
                
                // 发送报告给协调智能体
                sendReportToCoordinator(completeReport);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("📋 报告生成被中断");
            } catch (Exception e) {
                logger.error("📋 报告生成过程中发生错误", e);
            } finally {
                isGeneratingReport = false;
            }
        });
    }
    
    /**
     * 生成报告头部
     */
    private Map<String, Object> generateReportHeader() {
        logger.info("📋 生成报告头部信息...");
        
        Map<String, Object> header = new HashMap<>();
        header.put("reportTitle", "水库大坝安全监测报告");
        header.put("reportId", "DSR-" + System.currentTimeMillis());
        header.put("generationDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss")));
        header.put("reportVersion", "1.0");
        header.put("damName", "示例水库大坝");
        header.put("location", "某省某市某县");
        header.put("reportType", "定期安全监测报告");
        header.put("generatedBy", "大坝安全多智能体监测系统");
        
        return header;
    }
    
    /**
     * 生成执行摘要
     */
    private Map<String, Object> generateExecutiveSummary(Map<String, Object> analysisResults) {
        logger.info("📋 生成执行摘要...");
        
        Map<String, Object> summary = new HashMap<>();
        
        // 从分析结果中提取关键信息
        @SuppressWarnings("unchecked")
        Map<String, Object> overallAssessment = (Map<String, Object>) analysisResults.get("overallAssessment");
        
        String overallSafetyLevel = (String) overallAssessment.get("overallSafetyLevel");
        double overallScore = (Double) overallAssessment.get("overallScore");
        
        summary.put("overallSafetyStatus", overallSafetyLevel);
        summary.put("overallSafetyScore", String.format("%.3f", overallScore));
        summary.put("monitoringPeriod", "本次监测周期");
        summary.put("keyFindings", generateKeyFindings(analysisResults));
        summary.put("urgentActions", generateUrgentActions(overallSafetyLevel));
        summary.put("nextSteps", new String[]{"继续定期监测", "更新监测数据", "准备下次评估报告"});
        
        logger.info("📋 执行摘要生成完成，总体安全状态: {}", overallSafetyLevel);
        
        return summary;
    }
    
    /**
     * 生成详细发现
     */
    private Map<String, Object> generateDetailedFindings(Map<String, Object> analysisResults) {
        logger.info("📋 生成详细发现...");
        
        Map<String, Object> findings = new HashMap<>();
        
        // 结构安全发现
        @SuppressWarnings("unchecked")
        Map<String, Object> structuralAnalysis = (Map<String, Object>) analysisResults.get("structuralAnalysis");
        findings.put("structuralFindings", generateStructuralFindings(structuralAnalysis));
        
        // 渗流安全发现
        @SuppressWarnings("unchecked")
        Map<String, Object> seepageAnalysis = (Map<String, Object>) analysisResults.get("seepageAnalysis");
        findings.put("seepageFindings", generateSeepageFindings(seepageAnalysis));
        
        // 变形安全发现
        @SuppressWarnings("unchecked")
        Map<String, Object> deformationAnalysis = (Map<String, Object>) analysisResults.get("deformationAnalysis");
        findings.put("deformationFindings", generateDeformationFindings(deformationAnalysis));
        
        // 环境影响发现
        @SuppressWarnings("unchecked")
        Map<String, Object> environmentalAnalysis = (Map<String, Object>) analysisResults.get("environmentalAnalysis");
        findings.put("environmentalFindings", generateEnvironmentalFindings(environmentalAnalysis));
        
        return findings;
    }
    
    /**
     * 生成建议
     */
    private Map<String, Object> generateRecommendations(Map<String, Object> analysisResults) {
        logger.info("📋 生成建议和措施...");
        
        Map<String, Object> recommendations = new HashMap<>();
        
        // 从各项分析中提取建议
        @SuppressWarnings("unchecked")
        Map<String, Object> structuralAnalysis = (Map<String, Object>) analysisResults.get("structuralAnalysis");
        @SuppressWarnings("unchecked")
        String[] structuralRecs = (String[]) structuralAnalysis.get("recommendations");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> overallAssessment = (Map<String, Object>) analysisResults.get("overallAssessment");
        @SuppressWarnings("unchecked")
        String[] overallRecs = (String[]) overallAssessment.get("overallRecommendations");
        
        recommendations.put("structuralRecommendations", structuralRecs);
        recommendations.put("overallRecommendations", overallRecs);
        recommendations.put("immediateActions", new String[]{"检查关键监测设备", "确认数据传输正常"});
        recommendations.put("shortTermActions", new String[]{"制定维护计划", "更新监测程序"});
        recommendations.put("longTermActions", new String[]{"设备升级规划", "长期安全策略"});
        
        return recommendations;
    }
    
    /**
     * 组装完整报告
     */
    private Map<String, Object> assembleCompleteReport(Map<String, Object> header,
                                                      Map<String, Object> summary,
                                                      Map<String, Object> findings,
                                                      Map<String, Object> recommendations) {
        logger.info("📋 组装完整报告...");
        
        Map<String, Object> completeReport = new HashMap<>();
        completeReport.put("reportHeader", header);
        completeReport.put("executiveSummary", summary);
        completeReport.put("detailedFindings", findings);
        completeReport.put("recommendations", recommendations);
        
        // 报告元数据
        completeReport.put("reportMetadata", Map.of(
                "totalPages", "估计15-20页",
                "reportFormat", "结构化数据报告",
                "generationTime", LocalDateTime.now(),
                "reportSize", "完整版报告"
        ));
        
        return completeReport;
    }
    
    /**
     * 发送报告给协调智能体
     */
    private void sendReportToCoordinator(Map<String, Object> completeReport) {
        if (coordinatorId == null) {
            logger.error("📋 协调智能体ID未设置");
            return;
        }
        
        // 使用自定义消息格式
        DamSafetyMessage damMessage = DamSafetyMessage.createWithStatus(
                DamSafetyMessage.MessageType.REPORT_GENERATION,
                getAgentId(),
                coordinatorId,
                completeReport,
                DamSafetyMessage.ProcessingStatus.COMPLETED
        );
        
        // 转换为AgentMessage发送
        AgentMessage message = AgentMessage.builder()
                .sender(getAgentId())
                .receiver(coordinatorId)
                .performative(MessagePerformative.INFORM)
                .content(damMessage)
                .build();
        
        sendMessage(message);
        
        String reportId = (String) ((Map<?, ?>) completeReport.get("reportHeader")).get("reportId");
        String overallStatus = (String) ((Map<?, ?>) completeReport.get("executiveSummary")).get("overallSafetyStatus");
        
        logger.info("📋 ✅ 大坝安全监测报告生成完成！");
        logger.info("📋 📊 报告ID: {}", reportId);
        logger.info("📋 🛡️ 总体安全状态: {}", overallStatus);
        logger.info("📋 📄 报告已发送给协调智能体");
        
        // 输出报告摘要到控制台
        printReportSummary(completeReport);
    }
    
    /**
     * 处理系统关闭消息
     */
    private void handleSystemShutdown(DamSafetyMessage damMessage) {
        logger.info("📋 收到系统关闭通知，报告智能体准备停止");
        isGeneratingReport = false;
    }
    
    /**
     * 打印报告摘要到控制台
     */
    private void printReportSummary(Map<String, Object> completeReport) {
        logger.info("📋 ==================== 大坝安全监测报告摘要 ====================");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> header = (Map<String, Object>) completeReport.get("reportHeader");
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) completeReport.get("executiveSummary");
        
        logger.info("📋 报告标题: {}", header.get("reportTitle"));
        logger.info("📋 报告ID: {}", header.get("reportId"));
        logger.info("📋 生成时间: {}", header.get("generationDate"));
        logger.info("📋 大坝名称: {}", header.get("damName"));
        logger.info("📋 总体安全状态: {}", summary.get("overallSafetyStatus"));
        logger.info("📋 安全评分: {}", summary.get("overallSafetyScore"));
        
        @SuppressWarnings("unchecked")
        String[] keyFindings = (String[]) summary.get("keyFindings");
        logger.info("📋 关键发现:");
        for (String finding : keyFindings) {
            logger.info("📋   - {}", finding);
        }
        
        logger.info("📋 ============================================================");
    }
    
    // 辅助方法
    private String[] generateKeyFindings(Map<String, Object> analysisResults) {
        @SuppressWarnings("unchecked")
        Map<String, Object> overallAssessment = (Map<String, Object>) analysisResults.get("overallAssessment");
        String level = (String) overallAssessment.get("overallSafetyLevel");
        
        if ("安全".equals(level)) {
            return new String[]{
                "大坝整体结构状况良好",
                "各项安全指标均在正常范围内",
                "监测系统运行正常"
            };
        } else if ("基本安全".equals(level)) {
            return new String[]{
                "大坝整体状况基本稳定",
                "个别指标需要持续关注",
                "建议加强重点部位监测"
            };
        } else {
            return new String[]{
                "发现需要关注的安全问题",
                "建议立即采取相应措施",
                "需要专业评估和处理"
            };
        }
    }
    
    private String[] generateUrgentActions(String safetyLevel) {
        if ("安全".equals(safetyLevel)) {
            return new String[]{"无紧急行动需求"};
        } else if ("基本安全".equals(safetyLevel)) {
            return new String[]{"加强监测频率", "关注重点部位"};
        } else {
            return new String[]{"立即组织专家评估", "制定应急预案", "加强安全监测"};
        }
    }
    
    private Map<String, Object> generateStructuralFindings(Map<String, Object> analysis) {
        return Map.of(
                "safetyLevel", analysis.get("safetyLevel"),
                "score", String.format("%.3f", (Double) analysis.get("structuralSafetyScore")),
                "keyIndicators", Map.of(
                        "裂缝风险指数", String.format("%.3f", (Double) analysis.get("crackRiskIndex")),
                        "接缝稳定性指数", String.format("%.3f", (Double) analysis.get("jointStabilityIndex")),
                        "材料完整性指数", String.format("%.3f", (Double) analysis.get("materialIntegrityIndex"))
                )
        );
    }
    
    private Map<String, Object> generateSeepageFindings(Map<String, Object> analysis) {
        return Map.of(
                "safetyLevel", analysis.get("safetyLevel"),
                "score", String.format("%.3f", (Double) analysis.get("seepageSafetyScore"))
        );
    }
    
    private Map<String, Object> generateDeformationFindings(Map<String, Object> analysis) {
        return Map.of(
                "safetyLevel", analysis.get("safetyLevel"),
                "score", String.format("%.3f", (Double) analysis.get("deformationSafetyScore"))
        );
    }
    
    private Map<String, Object> generateEnvironmentalFindings(Map<String, Object> analysis) {
        return Map.of(
                "impactLevel", analysis.get("impactLevel"),
                "score", String.format("%.3f", (Double) analysis.get("environmentalScore"))
        );
    }
}
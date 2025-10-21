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

/**
 * 分析智能体
 * 负责对处理后的数据进行深度分析，评估大坝安全状况
 */
public class AnalysisAgent extends AbstractAgent {
    private static final Logger logger = LoggerFactory.getLogger(AnalysisAgent.class);
    
    private MessageRouter messageRouter;
    private AgentId coordinatorId;
    private boolean isAnalyzing = false;

    public AnalysisAgent(AgentId agentId) {
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
        logger.info("🔬 分析智能体启动成功");
    }

    @Override
    protected void doStop() {
        logger.info("🔬 分析智能体停止");
        isAnalyzing = false;
    }

    @Override
    protected void doHandleMessage(AgentMessage message) {
        logger.info("🔬 分析智能体收到消息: {}", message.content());
        
        if (message.content() instanceof DamSafetyMessage) {
            DamSafetyMessage damMessage = (DamSafetyMessage) message.content();
            
            if (damMessage.getMessageType() == DamSafetyMessage.MessageType.DATA_PROCESSING) {
                analyzeProcessedData(damMessage);
            } else if (damMessage.getMessageType() == DamSafetyMessage.MessageType.SYSTEM_SHUTDOWN) {
                handleSystemShutdown(damMessage);
            }
        }
    }
    
    /**
     * 分析处理后的数据
     */
    private void analyzeProcessedData(DamSafetyMessage damMessage) {
        if (isAnalyzing) {
            logger.warn("🔬 数据分析正在进行中");
            return;
        }
        
        isAnalyzing = true;
        logger.info("🔬 开始分析处理后的数据...");
        
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> processedData = damMessage.getDataContent();
                
                // 结构安全分析
                Map<String, Object> structuralAnalysis = analyzeStructuralSafety(processedData);
                Thread.sleep(600);
                
                // 渗流安全分析
                Map<String, Object> seepageAnalysis = analyzeSeepageSafety(processedData);
                Thread.sleep(500);
                
                // 变形安全分析
                Map<String, Object> deformationAnalysis = analyzeDeformationSafety(processedData);
                Thread.sleep(400);
                
                // 环境影响分析
                Map<String, Object> environmentalAnalysis = analyzeEnvironmentalImpact(processedData);
                Thread.sleep(300);
                
                // 综合安全评估
                Map<String, Object> overallAssessment = performOverallSafetyAssessment(
                        structuralAnalysis, seepageAnalysis, deformationAnalysis, environmentalAnalysis);
                
                // 创建分析结果
                Map<String, Object> analysisResults = createAnalysisResults(
                        structuralAnalysis, seepageAnalysis, deformationAnalysis, 
                        environmentalAnalysis, overallAssessment);
                
                // 发送分析结果给协调智能体
                sendAnalysisResultsToCoordinator(analysisResults);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("🔬 数据分析被中断");
            } catch (Exception e) {
                logger.error("🔬 数据分析过程中发生错误", e);
            } finally {
                isAnalyzing = false;
            }
        });
    }
    
    /**
     * 结构安全分析
     */
    private Map<String, Object> analyzeStructuralSafety(Map<String, Object> processedData) {
        logger.info("🔬 执行结构安全分析...");
        
        Map<String, Object> structuralAnalysis = new HashMap<>();
        structuralAnalysis.put("analysisType", "structural_safety");
        structuralAnalysis.put("analysisTimestamp", LocalDateTime.now());
        
        // 模拟结构安全指标计算
        double crackRiskIndex = 0.15 + Math.random() * 0.25; // 裂缝风险指数
        double jointStabilityIndex = 0.85 + Math.random() * 0.10; // 接缝稳定性指数
        double materialIntegrityIndex = 0.90 + Math.random() * 0.08; // 材料完整性指数
        
        structuralAnalysis.put("crackRiskIndex", crackRiskIndex);
        structuralAnalysis.put("jointStabilityIndex", jointStabilityIndex);
        structuralAnalysis.put("materialIntegrityIndex", materialIntegrityIndex);
        
        // 结构安全等级评估
        double structuralSafetyScore = (jointStabilityIndex + materialIntegrityIndex) / 2 - crackRiskIndex * 0.3;
        String safetyLevel = getSafetyLevel(structuralSafetyScore);
        
        structuralAnalysis.put("structuralSafetyScore", structuralSafetyScore);
        structuralAnalysis.put("safetyLevel", safetyLevel);
        structuralAnalysis.put("recommendations", generateStructuralRecommendations(structuralSafetyScore));
        
        logger.info("🔬 结构安全分析完成: 安全等级={}, 评分={}", 
                safetyLevel, String.format("%.3f", structuralSafetyScore));
        
        return structuralAnalysis;
    }
    
    /**
     * 渗流安全分析
     */
    private Map<String, Object> analyzeSeepageSafety(Map<String, Object> processedData) {
        logger.info("🔬 执行渗流安全分析...");
        
        Map<String, Object> seepageAnalysis = new HashMap<>();
        seepageAnalysis.put("analysisType", "seepage_safety");
        seepageAnalysis.put("analysisTimestamp", LocalDateTime.now());
        
        // 模拟渗流安全指标计算
        double seepageRateIndex = 0.20 + Math.random() * 0.30; // 渗流量指数
        double pressureStabilityIndex = 0.80 + Math.random() * 0.15; // 压力稳定性指数
        double seepageQualityIndex = 0.85 + Math.random() * 0.12; // 渗流水质指数
        
        seepageAnalysis.put("seepageRateIndex", seepageRateIndex);
        seepageAnalysis.put("pressureStabilityIndex", pressureStabilityIndex);
        seepageAnalysis.put("seepageQualityIndex", seepageQualityIndex);
        
        // 渗流安全等级评估
        double seepageSafetyScore = (pressureStabilityIndex + seepageQualityIndex) / 2 - seepageRateIndex * 0.4;
        String safetyLevel = getSafetyLevel(seepageSafetyScore);
        
        seepageAnalysis.put("seepageSafetyScore", seepageSafetyScore);
        seepageAnalysis.put("safetyLevel", safetyLevel);
        seepageAnalysis.put("recommendations", generateSeepageRecommendations(seepageSafetyScore));
        
        logger.info("🔬 渗流安全分析完成: 安全等级={}, 评分={}", 
                safetyLevel, String.format("%.3f", seepageSafetyScore));
        
        return seepageAnalysis;
    }
    
    /**
     * 变形安全分析
     */
    private Map<String, Object> analyzeDeformationSafety(Map<String, Object> processedData) {
        logger.info("🔬 执行变形安全分析...");
        
        Map<String, Object> deformationAnalysis = new HashMap<>();
        deformationAnalysis.put("analysisType", "deformation_safety");
        deformationAnalysis.put("analysisTimestamp", LocalDateTime.now());
        
        // 模拟变形安全指标计算
        double displacementRiskIndex = 0.10 + Math.random() * 0.20; // 位移风险指数
        double stabilityIndex = 0.88 + Math.random() * 0.10; // 稳定性指数
        double tiltRiskIndex = 0.05 + Math.random() * 0.15; // 倾斜风险指数
        
        deformationAnalysis.put("displacementRiskIndex", displacementRiskIndex);
        deformationAnalysis.put("stabilityIndex", stabilityIndex);
        deformationAnalysis.put("tiltRiskIndex", tiltRiskIndex);
        
        // 变形安全等级评估
        double deformationSafetyScore = stabilityIndex - (displacementRiskIndex + tiltRiskIndex) * 0.3;
        String safetyLevel = getSafetyLevel(deformationSafetyScore);
        
        deformationAnalysis.put("deformationSafetyScore", deformationSafetyScore);
        deformationAnalysis.put("safetyLevel", safetyLevel);
        deformationAnalysis.put("recommendations", generateDeformationRecommendations(deformationSafetyScore));
        
        logger.info("🔬 变形安全分析完成: 安全等级={}, 评分={}", 
                safetyLevel, String.format("%.3f", deformationSafetyScore));
        
        return deformationAnalysis;
    }
    
    /**
     * 环境影响分析
     */
    private Map<String, Object> analyzeEnvironmentalImpact(Map<String, Object> processedData) {
        logger.info("🔬 执行环境影响分析...");
        
        Map<String, Object> environmentalAnalysis = new HashMap<>();
        environmentalAnalysis.put("analysisType", "environmental_impact");
        environmentalAnalysis.put("analysisTimestamp", LocalDateTime.now());
        
        // 模拟环境影响指标计算
        double weatherImpactIndex = 0.25 + Math.random() * 0.35; // 天气影响指数
        double temperatureStressIndex = 0.15 + Math.random() * 0.25; // 温度应力指数
        double environmentalStabilityIndex = 0.80 + Math.random() * 0.15; // 环境稳定性指数
        
        environmentalAnalysis.put("weatherImpactIndex", weatherImpactIndex);
        environmentalAnalysis.put("temperatureStressIndex", temperatureStressIndex);
        environmentalAnalysis.put("environmentalStabilityIndex", environmentalStabilityIndex);
        
        // 环境影响等级评估
        double environmentalScore = environmentalStabilityIndex - (weatherImpactIndex + temperatureStressIndex) * 0.2;
        String impactLevel = getImpactLevel(environmentalScore);
        
        environmentalAnalysis.put("environmentalScore", environmentalScore);
        environmentalAnalysis.put("impactLevel", impactLevel);
        environmentalAnalysis.put("recommendations", generateEnvironmentalRecommendations(environmentalScore));
        
        logger.info("🔬 环境影响分析完成: 影响等级={}, 评分={}", 
                impactLevel, String.format("%.3f", environmentalScore));
        
        return environmentalAnalysis;
    }
    
    /**
     * 综合安全评估
     */
    private Map<String, Object> performOverallSafetyAssessment(Map<String, Object> structuralAnalysis,
                                                              Map<String, Object> seepageAnalysis,
                                                              Map<String, Object> deformationAnalysis,
                                                              Map<String, Object> environmentalAnalysis) {
        logger.info("🔬 执行综合安全评估...");
        
        Map<String, Object> overallAssessment = new HashMap<>();
        overallAssessment.put("assessmentType", "overall_safety");
        overallAssessment.put("assessmentTimestamp", LocalDateTime.now());
        
        // 获取各项评分
        double structuralScore = (Double) structuralAnalysis.get("structuralSafetyScore");
        double seepageScore = (Double) seepageAnalysis.get("seepageSafetyScore");
        double deformationScore = (Double) deformationAnalysis.get("deformationSafetyScore");
        double environmentalScore = (Double) environmentalAnalysis.get("environmentalScore");
        
        // 权重分配
        double structuralWeight = 0.35;
        double seepageWeight = 0.30;
        double deformationWeight = 0.25;
        double environmentalWeight = 0.10;
        
        // 计算综合评分
        double overallScore = structuralScore * structuralWeight +
                             seepageScore * seepageWeight +
                             deformationScore * deformationWeight +
                             environmentalScore * environmentalWeight;
        
        String overallSafetyLevel = getSafetyLevel(overallScore);
        
        overallAssessment.put("overallScore", overallScore);
        overallAssessment.put("overallSafetyLevel", overallSafetyLevel);
        overallAssessment.put("scoreBreakdown", Map.of(
                "structural", structuralScore,
                "seepage", seepageScore,
                "deformation", deformationScore,
                "environmental", environmentalScore
        ));
        overallAssessment.put("weightDistribution", Map.of(
                "structural", structuralWeight,
                "seepage", seepageWeight,
                "deformation", deformationWeight,
                "environmental", environmentalWeight
        ));
        
        // 生成综合建议
        overallAssessment.put("overallRecommendations", generateOverallRecommendations(overallScore, overallSafetyLevel));
        
        logger.info("🔬 综合安全评估完成: 总体安全等级={}, 综合评分={}", 
                overallSafetyLevel, String.format("%.3f", overallScore));
        
        return overallAssessment;
    }
    
    /**
     * 创建分析结果
     */
    private Map<String, Object> createAnalysisResults(Map<String, Object> structuralAnalysis,
                                                     Map<String, Object> seepageAnalysis,
                                                     Map<String, Object> deformationAnalysis,
                                                     Map<String, Object> environmentalAnalysis,
                                                     Map<String, Object> overallAssessment) {
        Map<String, Object> analysisResults = new HashMap<>();
        analysisResults.put("analysisId", java.util.UUID.randomUUID().toString());
        analysisResults.put("analysisTimestamp", LocalDateTime.now());
        analysisResults.put("analysisStatus", "completed");
        
        // 各项分析结果
        analysisResults.put("structuralAnalysis", structuralAnalysis);
        analysisResults.put("seepageAnalysis", seepageAnalysis);
        analysisResults.put("deformationAnalysis", deformationAnalysis);
        analysisResults.put("environmentalAnalysis", environmentalAnalysis);
        analysisResults.put("overallAssessment", overallAssessment);
        
        // 分析统计信息
        analysisResults.put("analysisStatistics", Map.of(
                "totalAnalysisTime", "1.8秒",
                "analysisModules", 4,
                "confidenceLevel", 0.92
        ));
        
        return analysisResults;
    }
    
    /**
     * 发送分析结果给协调智能体
     */
    private void sendAnalysisResultsToCoordinator(Map<String, Object> analysisResults) {
        if (coordinatorId == null) {
            logger.error("🔬 协调智能体ID未设置");
            return;
        }
        
        // 使用自定义消息格式
        DamSafetyMessage damMessage = DamSafetyMessage.createWithStatus(
                DamSafetyMessage.MessageType.DATA_ANALYSIS,
                getAgentId(),
                coordinatorId,
                analysisResults,
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
        
        String overallLevel = (String) ((Map<?, ?>) analysisResults.get("overallAssessment")).get("overallSafetyLevel");
        logger.info("🔬 ✅ 数据分析完成，已发送给协调智能体，总体安全等级: {}", overallLevel);
    }
    
    /**
     * 处理系统关闭消息
     */
    private void handleSystemShutdown(DamSafetyMessage damMessage) {
        logger.info("🔬 收到系统关闭通知，分析智能体准备停止");
        isAnalyzing = false;
    }
    
    // 辅助方法
    private String getSafetyLevel(double score) {
        if (score >= 0.8) return "安全";
        else if (score >= 0.6) return "基本安全";
        else if (score >= 0.4) return "需要关注";
        else return "存在风险";
    }
    
    private String getImpactLevel(double score) {
        if (score >= 0.8) return "影响较小";
        else if (score >= 0.6) return "影响适中";
        else if (score >= 0.4) return "影响较大";
        else return "影响严重";
    }
    
    private String[] generateStructuralRecommendations(double score) {
        if (score >= 0.8) {
            return new String[]{"继续定期监测", "保持现有维护计划"};
        } else if (score >= 0.6) {
            return new String[]{"增加监测频率", "检查关键结构部位"};
        } else {
            return new String[]{"立即进行详细检查", "制定加固方案", "增加安全监测设备"};
        }
    }
    
    private String[] generateSeepageRecommendations(double score) {
        if (score >= 0.8) {
            return new String[]{"维持现有排水系统", "定期清理排水设施"};
        } else if (score >= 0.6) {
            return new String[]{"检查排水系统", "监测渗流变化趋势"};
        } else {
            return new String[]{"紧急检查渗流通道", "考虑防渗加固措施", "增设渗流监测点"};
        }
    }
    
    private String[] generateDeformationRecommendations(double score) {
        if (score >= 0.8) {
            return new String[]{"继续自动化监测", "定期校准监测设备"};
        } else if (score >= 0.6) {
            return new String[]{"增加变形监测点", "分析变形趋势"};
        } else {
            return new String[]{"立即进行变形分析", "评估结构稳定性", "制定应急预案"};
        }
    }
    
    private String[] generateEnvironmentalRecommendations(double score) {
        if (score >= 0.8) {
            return new String[]{"继续环境监测", "关注天气变化"};
        } else if (score >= 0.6) {
            return new String[]{"加强恶劣天气预警", "检查防护设施"};
        } else {
            return new String[]{"启动恶劣天气应急预案", "加强环境监测", "检查防护措施"};
        }
    }
    
    private String[] generateOverallRecommendations(double score, String level) {
        if ("安全".equals(level)) {
            return new String[]{"大坝整体状况良好", "继续按计划进行常规维护", "保持现有监测频率"};
        } else if ("基本安全".equals(level)) {
            return new String[]{"大坝状况基本稳定", "建议增加重点部位监测", "制定预防性维护计划"};
        } else if ("需要关注".equals(level)) {
            return new String[]{"大坝存在潜在风险", "立即组织专家评估", "制定针对性处理方案", "增加监测频率"};
        } else {
            return new String[]{"大坝安全存在风险", "立即启动应急预案", "组织紧急安全评估", "采取必要的安全措施"};
        }
    }
}
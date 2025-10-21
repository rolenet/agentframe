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
 * 数据处理智能体
 * 负责处理采集到的原始数据，进行清洗、校验和初步分析
 */
public class DataProcessingAgent extends AbstractAgent {
    private static final Logger logger = LoggerFactory.getLogger(DataProcessingAgent.class);
    
    private MessageRouter messageRouter;
    private AgentId coordinatorId;
    private boolean isProcessing = false;

    public DataProcessingAgent(AgentId agentId) {
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
        logger.info("⚙️ 数据处理智能体启动成功");
    }

    @Override
    protected void doStop() {
        logger.info("⚙️ 数据处理智能体停止");
        isProcessing = false;
    }

    @Override
    protected void doHandleMessage(AgentMessage message) {
        logger.info("⚙️ 数据处理智能体收到消息: {}", message.content());
        
        // 处理来自数据采集智能体的数据
        if (message.content() instanceof DamSafetyMessage) {
            DamSafetyMessage damMessage = (DamSafetyMessage) message.content();
            
            if (damMessage.getMessageType() == DamSafetyMessage.MessageType.DATA_COLLECTION) {
                processCollectedData(damMessage);
            }
        }
    }
    
    /**
     * 处理采集到的数据
     */
    private void processCollectedData(DamSafetyMessage damMessage) {
        if (isProcessing) {
            logger.warn("⚙️ 数据处理正在进行中");
            return;
        }
        
        isProcessing = true;
        logger.info("⚙️ 开始处理采集到的数据...");
        
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> rawData = damMessage.getDataContent();
                
                // 数据清洗和校验
                Map<String, Object> cleanedData = cleanAndValidateData(rawData);
                Thread.sleep(800);
                
                // 数据标准化
                Map<String, Object> normalizedData = normalizeData(cleanedData);
                Thread.sleep(600);
                
                // 数据质量评估
                Map<String, Object> qualityAssessment = assessDataQuality(normalizedData);
                Thread.sleep(400);
                
                // 创建处理结果
                Map<String, Object> processedData = createProcessedDataResult(
                        rawData, cleanedData, normalizedData, qualityAssessment);
                
                // 发送处理完成的数据给协调智能体
                sendProcessedDataToCoordinator(processedData);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("⚙️ 数据处理被中断");
            } catch (Exception e) {
                logger.error("⚙️ 数据处理过程中发生错误", e);
            } finally {
                isProcessing = false;
            }
        });
    }
    
    /**
     * 数据清洗和校验
     */
    private Map<String, Object> cleanAndValidateData(Map<String, Object> rawData) {
        logger.info("⚙️ 执行数据清洗和校验...");
        
        Map<String, Object> cleanedData = new HashMap<>();
        cleanedData.put("originalDataPoints", rawData.get("totalDataPoints"));
        cleanedData.put("validDataPoints", (Integer) rawData.get("totalDataPoints") - 1); // 模拟去除1个异常值
        cleanedData.put("removedOutliers", 1);
        cleanedData.put("dataIntegrityScore", 0.95 + Math.random() * 0.05); // 数据完整性评分
        cleanedData.put("cleaningTimestamp", LocalDateTime.now());
        
        // 模拟数据清洗过程
        cleanedData.put("structuralDataStatus", "已清洗");
        cleanedData.put("seepageDataStatus", "已清洗");
        cleanedData.put("deformationDataStatus", "已清洗");
        cleanedData.put("environmentalDataStatus", "已清洗");
        
        logger.info("⚙️ 数据清洗完成: 原始数据点={}, 有效数据点={}, 移除异常值={}",
                cleanedData.get("originalDataPoints"),
                cleanedData.get("validDataPoints"),
                cleanedData.get("removedOutliers"));
        
        return cleanedData;
    }
    
    /**
     * 数据标准化
     */
    private Map<String, Object> normalizeData(Map<String, Object> cleanedData) {
        logger.info("⚙️ 执行数据标准化...");
        
        Map<String, Object> normalizedData = new HashMap<>();
        normalizedData.put("normalizationMethod", "Z-Score标准化");
        normalizedData.put("scalingFactor", 1.0);
        normalizedData.put("meanValue", 0.0);
        normalizedData.put("standardDeviation", 1.0);
        normalizedData.put("normalizationTimestamp", LocalDateTime.now());
        
        // 模拟各类数据的标准化结果
        normalizedData.put("structuralDataNormalized", true);
        normalizedData.put("seepageDataNormalized", true);
        normalizedData.put("deformationDataNormalized", true);
        normalizedData.put("environmentalDataNormalized", true);
        
        // 计算标准化后的统计信息
        normalizedData.put("dataRange", Map.of(
                "min", -2.5,
                "max", 2.8,
                "median", 0.1
        ));
        
        logger.info("⚙️ 数据标准化完成: 方法={}, 数据范围=[{}, {}]",
                normalizedData.get("normalizationMethod"),
                ((Map<?, ?>) normalizedData.get("dataRange")).get("min"),
                ((Map<?, ?>) normalizedData.get("dataRange")).get("max"));
        
        return normalizedData;
    }
    
    /**
     * 数据质量评估
     */
    private Map<String, Object> assessDataQuality(Map<String, Object> normalizedData) {
        logger.info("⚙️ 执行数据质量评估...");
        
        Map<String, Object> qualityAssessment = new HashMap<>();
        qualityAssessment.put("overallQualityScore", 0.88 + Math.random() * 0.12); // 总体质量评分
        qualityAssessment.put("completenessScore", 0.95 + Math.random() * 0.05); // 完整性评分
        qualityAssessment.put("accuracyScore", 0.90 + Math.random() * 0.10); // 准确性评分
        qualityAssessment.put("consistencyScore", 0.85 + Math.random() * 0.15); // 一致性评分
        qualityAssessment.put("assessmentTimestamp", LocalDateTime.now());
        
        // 各类数据的质量评估
        qualityAssessment.put("dataQualityByType", Map.of(
                "structural", 0.92,
                "seepage", 0.89,
                "deformation", 0.91,
                "environmental", 0.87
        ));
        
        // 质量问题识别
        qualityAssessment.put("identifiedIssues", new String[]{
                "环境数据存在轻微噪声",
                "渗流数据时间序列有小幅波动"
        });
        
        double overallScore = (Double) qualityAssessment.get("overallQualityScore");
        logger.info("⚙️ 数据质量评估完成: 总体评分={}, 完整性={}, 准确性={}",
                String.format("%.2f", overallScore),
                String.format("%.2f", qualityAssessment.get("completenessScore")),
                String.format("%.2f", qualityAssessment.get("accuracyScore")));
        
        return qualityAssessment;
    }
    
    /**
     * 创建处理结果
     */
    private Map<String, Object> createProcessedDataResult(Map<String, Object> rawData,
                                                         Map<String, Object> cleanedData,
                                                         Map<String, Object> normalizedData,
                                                         Map<String, Object> qualityAssessment) {
        Map<String, Object> processedData = new HashMap<>();
        processedData.put("processingId", java.util.UUID.randomUUID().toString());
        processedData.put("processingTimestamp", LocalDateTime.now());
        processedData.put("processingStatus", "completed");
        
        // 汇总处理结果
        processedData.put("rawDataSummary", rawData);
        processedData.put("cleaningResults", cleanedData);
        processedData.put("normalizationResults", normalizedData);
        processedData.put("qualityAssessment", qualityAssessment);
        
        // 处理统计信息
        processedData.put("processingStatistics", Map.of(
                "totalProcessingTime", "1.8秒",
                "dataReductionRatio", 0.02,
                "processingEfficiency", 0.96
        ));
        
        // 为后续分析准备的数据摘要
        processedData.put("analysisReadyData", Map.of(
                "structuralSafety", "数据已准备",
                "seepageSafety", "数据已准备",
                "deformationSafety", "数据已准备",
                "environmentalImpact", "数据已准备"
        ));
        
        return processedData;
    }
    
    /**
     * 将处理完成的数据发送给协调智能体
     */
    private void sendProcessedDataToCoordinator(Map<String, Object> processedData) {
        if (coordinatorId == null) {
            logger.error("⚙️ 协调智能体ID未设置");
            return;
        }
        
        // 使用自定义消息格式
        DamSafetyMessage damMessage = DamSafetyMessage.createWithStatus(
                DamSafetyMessage.MessageType.DATA_PROCESSING,
                getAgentId(),
                coordinatorId,
                processedData,
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
        
        double qualityScore = (Double) ((Map<?, ?>) processedData.get("qualityAssessment")).get("overallQualityScore");
        logger.info("⚙️ ✅ 数据处理完成，已发送给协调智能体，数据质量评分: {}", String.format("%.2f", qualityScore));
    }
}
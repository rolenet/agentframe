package com.agentcore.examples.reservoir;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 数据统计智能体
 * 负责对采集的数据进行统计分析
 */
public class DataStatisticsAgent extends AbstractAgent {
    private static final Logger logger = LoggerFactory.getLogger(DataStatisticsAgent.class);
    private List<AgentId> collaborators = new ArrayList<>();
    private final Map<String, List<Map<String, Object>>> collectedData = new ConcurrentHashMap<>();
    private final AtomicInteger dataCount = new AtomicInteger(0);
    private MessageRouter messageRouter;
    private AsyncNotificationHandler notificationHandler;

    public DataStatisticsAgent(AgentId agentId) {
        super(agentId);
    }

    public void setCollaborators(List<AgentId> collaborators) {
        this.collaborators = collaborators;
    }
    
    public void setMessageRouter(MessageRouter messageRouter) {
        this.messageRouter = messageRouter;
    }
    
    public void setNotificationHandler(AsyncNotificationHandler notificationHandler) {
        this.notificationHandler = notificationHandler;
    }

    @Override
    protected BehaviorScheduler createBehaviorScheduler() {
        return new DefaultBehaviorScheduler();
    }

    @Override
    protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
        logger.debug("数据统计智能体发送消息: {}", message.content());
        // 消息发送通过容器的MessageRouter自动处理，这里只需要返回成功
        if (messageRouter != null) {
            return messageRouter.routeMessage(message);
        } else {
            logger.error("MessageRouter is null, cannot send message");
            return CompletableFuture.failedFuture(new IllegalStateException("MessageRouter not set"));
        }
    }

    @Override
    protected void doStart() {
        logger.info("📈 数据统计智能体启动成功");
        
        // 发送启动完成通知
        if (notificationHandler != null) {
            notificationHandler.sendNotification(
                AsyncNotificationMessage.NotificationType.AGENT_READY, 
                getAgentId()
            );
            logger.info("📈 ✅ 数据统计智能体启动完成通知已发送");
        }
    }

    @Override
    protected void doStop() {
        logger.info("📈 数据统计智能体停止");
    }

    @Override
    protected void doHandleMessage(AgentMessage message) {
        logger.info("📈 数据统计智能体收到消息: {}", message.content());

        if (message.content() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> content = (Map<String, Object>) message.content();

            if (content.containsKey("dataType") && content.containsKey("data")) {
                processIncomingData(content);
            }
        } else if ("数据采集完成".equals(message.content())) {
            logger.info("📈 收到数据采集完成通知，开始生成统计报告...");
            generateStatisticsReport();
        }
    }
    public void startStatistics() {
        logger.info("📊 开始数据统计...");

        // 模拟采集不同类型的数据
        CompletableFuture.runAsync(() -> {
            generateStatisticsReport();
            // 通知统计智能体和文档输出智能体
            notifyDataReady();
        });
    }

    private void notifyDataReady() {
        // 发送传统消息给协作智能体
        for (AgentId collaborator : collaborators) {
            AgentMessage message = AgentMessage.builder()
                    .sender(getAgentId())
                    .receiver(collaborator)
                    .performative(MessagePerformative.INFORM)
                    .content("数据统计完成")
                    .build();
            sendMessage(message);
        }
        // 发送工作完成异步通知
        if (notificationHandler != null) {
            Map<String, Object> workResult = new HashMap<>();
            workResult.put("agentType", "DataStatisticsAgent");
            workResult.put("completionTime", LocalDateTime.now());
            workResult.put("reportGenerated", true);
            workResult.put("status", "completed");

            notificationHandler.sendNotification(
                    AsyncNotificationMessage.NotificationType.WORK_COMPLETED,
                    getAgentId(),
                    workResult
            );
            logger.info("👥 ✅ 统计信息工作完成通知已发送");
        }
    }

    private void processIncomingData(Map<String, Object> content) {
        String dataType = (String) content.get("dataType");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) content.get("data");

        collectedData.computeIfAbsent(dataType, k -> new ArrayList<>()).add(data);

        int count = dataCount.incrementAndGet();
        logger.info("📈 处理{}数据 #{}: {}", dataType, count, data.get("type"));
    }

    private void generateStatisticsReport() {
        logger.info("📈 开始生成统计报告...");

        Map<String, Object> statisticsReport = new HashMap<>();
        statisticsReport.put("reportType", "数据统计报告");
        statisticsReport.put("generateTime", LocalDateTime.now());

        // 统计各类数据
        for (Map.Entry<String, List<Map<String, Object>>> entry : collectedData.entrySet()) {
            String dataType = entry.getKey();
            List<Map<String, Object>> dataList = entry.getValue();

            Map<String, Object> stats = calculateStatistics(dataType, dataList);
            statisticsReport.put(dataType + "_统计", stats);

            logger.info("📈 {}统计完成: 数据量={}, 平均值={}",
                    dataType, stats.get("count"), stats.get("average"));
        }

        // 发送统计报告给文档输出智能体
        sendStatisticsToCollaborators(statisticsReport);
    }

    private Map<String, Object> calculateStatistics(String dataType, List<Map<String, Object>> dataList) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("count", dataList.size());
        stats.put("dataType", dataType);

        if (!dataList.isEmpty()) {
            // 根据数据类型计算不同的统计值
            switch (dataType) {
                case "水位数据":
                    calculateWaterLevelStats(dataList, stats);
                    break;
                case "水质数据":
                    calculateWaterQualityStats(dataList, stats);
                    break;
                case "流量数据":
                    calculateFlowRateStats(dataList, stats);
                    break;
                case "温度数据":
                    calculateTemperatureStats(dataList, stats);
                    break;
            }
        }

        return stats;
    }

    private void calculateWaterLevelStats(List<Map<String, Object>> dataList, Map<String, Object> stats) {
        double sum = dataList.stream()
                .mapToDouble(data -> ((Number) data.get("value")).doubleValue())
                .sum();
        double average = sum / dataList.size();
        double max = dataList.stream()
                .mapToDouble(data -> ((Number) data.get("value")).doubleValue())
                .max().orElse(0);
        double min = dataList.stream()
                .mapToDouble(data -> ((Number) data.get("value")).doubleValue())
                .min().orElse(0);

        stats.put("average", String.format("%.2f", average));
        stats.put("max", String.format("%.2f", max));
        stats.put("min", String.format("%.2f", min));
        stats.put("unit", "米");
    }

    private void calculateWaterQualityStats(List<Map<String, Object>> dataList, Map<String, Object> stats) {
        double avgPh = dataList.stream()
                .mapToDouble(data -> ((Number) data.get("ph")).doubleValue())
                .average().orElse(0);
        double avgOxygen = dataList.stream()
                .mapToDouble(data -> ((Number) data.get("dissolved_oxygen")).doubleValue())
                .average().orElse(0);

        stats.put("average", String.format("pH=%.2f, 溶解氧=%.2f", avgPh, avgOxygen));
        stats.put("ph_average", String.format("%.2f", avgPh));
        stats.put("oxygen_average", String.format("%.2f", avgOxygen));
    }

    private void calculateFlowRateStats(List<Map<String, Object>> dataList, Map<String, Object> stats) {
        double sum = dataList.stream()
                .mapToDouble(data -> ((Number) data.get("value")).doubleValue())
                .sum();
        double average = sum / dataList.size();

        stats.put("average", String.format("%.2f", average));
        stats.put("total_flow", String.format("%.2f", sum));
        stats.put("unit", "立方米/秒");
    }

    private void calculateTemperatureStats(List<Map<String, Object>> dataList, Map<String, Object> stats) {
        double avgWaterTemp = dataList.stream()
                .mapToDouble(data -> ((Number) data.get("water_temp")).doubleValue())
                .average().orElse(0);
        double avgAirTemp = dataList.stream()
                .mapToDouble(data -> ((Number) data.get("air_temp")).doubleValue())
                .average().orElse(0);

        stats.put("average", String.format("水温=%.1f°C, 气温=%.1f°C", avgWaterTemp, avgAirTemp));
        stats.put("water_temp_avg", String.format("%.1f", avgWaterTemp));
        stats.put("air_temp_avg", String.format("%.1f", avgAirTemp));
    }

    private void sendStatisticsToCollaborators(Map<String, Object> report) {
        logger.info("📈 准备发送统计报告给 {} 个协作智能体", collaborators.size());
        logger.info("📈 报告内容: reportType={}, generateTime={}", 
                report.get("reportType"), report.get("generateTime"));
        
        for (AgentId collaborator : collaborators) {
            logger.info("📈 发送统计报告给: {}", collaborator.getShortId());
            AgentMessage message = AgentMessage.builder()
                    .sender(getAgentId())
                    .receiver(collaborator)
                    .performative(MessagePerformative.INFORM)
                    .content(report)
                    .build();
            sendMessage(message);
            logger.info("📈 ✅ 统计报告已发送给: {}", collaborator.getShortId());
        }
        logger.info("📈 统计报告已发送给所有协作智能体");
        
        // 发送工作完成异步通知
        if (notificationHandler != null) {
            Map<String, Object> workResult = new HashMap<>();
            workResult.put("agentType", "DataStatisticsAgent");
            workResult.put("completionTime", LocalDateTime.now());
            workResult.put("dataProcessed", dataCount.get());
            workResult.put("reportGenerated", true);
            workResult.put("status", "completed");
            
            notificationHandler.sendNotification(
                AsyncNotificationMessage.NotificationType.WORK_COMPLETED,
                getAgentId(),
                workResult
            );
            logger.info("📈 ✅ 数据统计工作完成通知已发送");
        }
    }
}
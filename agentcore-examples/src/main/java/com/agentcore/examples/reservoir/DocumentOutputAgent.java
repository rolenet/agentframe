package com.agentcore.examples.reservoir;

import com.agentcore.communication.router.MessageRouter;
import com.agentcore.core.agent.AbstractAgent;
import com.agentcore.core.agent.AgentId;
import com.agentcore.core.behavior.BehaviorScheduler;
import com.agentcore.core.behavior.DefaultBehaviorScheduler;
import com.agentcore.core.message.AgentMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 文档输出智能体
 * 负责汇总所有数据并生成最终的巡检报告文档
 */
public class DocumentOutputAgent extends AbstractAgent {
    private static final Logger logger = LoggerFactory.getLogger(DocumentOutputAgent.class);
    private List<AgentId> collaborators = new ArrayList<>();
    private final Map<String, Object> collectedReports = new ConcurrentHashMap<>();
    private final AtomicInteger receivedReports = new AtomicInteger(0);
    private MessageRouter messageRouter;
    private AsyncNotificationHandler notificationHandler;
    
    // 异步工作结果收集
    private final Map<AgentId, Object> workResults = new ConcurrentHashMap<>();
    private Set<AgentId> expectedWorkingAgents = Set.of(
        AgentId.create("DataCollectionAgent"),
        AgentId.create("DataStatisticsAgent"),
        AgentId.create("WeatherSummaryAgent"),
        AgentId.create("DutyInfoAgent")
    );
    private volatile boolean workflowStarted = false;

    public DocumentOutputAgent(AgentId agentId) {
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
        setupAsyncWorkflowHandling();
    }
    public void setExpectedWorkingAgents(Set<AgentId> expectedWorkingAgents) {
        this.expectedWorkingAgents = expectedWorkingAgents;
        logger.info("📄 设置期望工作智能体: {}", 
                expectedWorkingAgents.stream().map(AgentId::getShortId).toArray());
    }
    @Override
    protected BehaviorScheduler createBehaviorScheduler() {
        return new DefaultBehaviorScheduler();
    }

    @Override
    protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
        logger.debug("文档输出智能体发送消息: {}", message.content());
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
        logger.info("📄 文档输出智能体启动成功");
        logger.info("📄 DocumentOutputAgent已准备接收消息，协作者数量: {}", collaborators.size());
        for (AgentId collaborator : collaborators) {
            logger.info("📄 协作者: {}", collaborator.getShortId());
        }
        
        // 发送启动完成通知
        if (notificationHandler != null) {
            notificationHandler.sendNotification(
                AsyncNotificationMessage.NotificationType.AGENT_READY, 
                getAgentId()
            );
            logger.info("📄 ✅ 文档输出智能体启动完成通知已发送");
        }
    }

    @Override
    protected void doStop() {
        logger.info("📄 文档输出智能体停止");
    }

    @Override
    protected void doHandleMessage(AgentMessage message) {
        logger.info("📄 ✅ DocumentOutputAgent收到消息！发送者: {} - 消息内容类型: {}", 
                message.sender().getShortId(), message.content().getClass().getSimpleName());
        logger.info("📄 消息详细信息: performative={}, content={}", 
                message.performative(), message.content());

        if (message.content() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> content = (Map<String, Object>) message.content();
            
            logger.info("📄 Map消息内容键: {}", content.keySet());

            if (content.containsKey("reportType")) {
                logger.info("📄 ✅ 检测到报告类型消息，reportType={}, 开始处理报告...", content.get("reportType"));
                processReport(message.sender(), content);
            } else if (content.containsKey("dataType")) {
                // 处理原始数据
                logger.info("📄 检测到原始数据消息，处理数据类型: {}", content.get("dataType"));
                processRawData(message.sender(), content);
            } else {
                logger.warn("📄 收到未知格式的Map消息，键: {}, 内容: {}", content.keySet(), content);
            }
        } else if ("数据采集完成".equals(message.content())) {
            //数据采集在数据统计中使用，这里只显示通知就可以，不进行统计
            logger.info("📄 收到数据采集完成通知");
        } else {
            logger.warn("📄 收到未知类型的消息: {} - 内容: {}", 
                    message.content().getClass().getSimpleName(), message.content());
        }
    }

    private void processReport(AgentId sender, Map<String, Object> report) {
        String reportType = (String) report.get("reportType");
        String reportKey = sender.getShortId() + "_" + reportType;
        
        collectedReports.put(reportKey, report);

        int count = receivedReports.incrementAndGet();
        logger.info("📄 成功收到{}报告 (第{}个报告) - 报告键: {}", reportType, count, reportKey);
        logger.info("📄 当前已收集的报告: {}", collectedReports.keySet());

        // 检查是否收到了所有必要的报告
        checkAndGenerateFinalDocument();
    }

    private void processRawData(AgentId sender, Map<String, Object> data) {
        String dataType = (String) data.get("dataType");
        logger.info("📄 收到来自{}的{}原始数据", sender.getShortId(), dataType);
    }

    private void checkAndGenerateFinalDocument() {
        // 检查是否收到了统计报告、天气报告和值班信息报告
        boolean hasStatistics = collectedReports.keySet().stream()
                .anyMatch(key -> key.contains("数据统计报告"));
        boolean hasWeather = collectedReports.keySet().stream()
                .anyMatch(key -> key.contains("天气汇总报告"));
        boolean hasDuty = collectedReports.keySet().stream()
                .anyMatch(key -> key.contains("值班信息报告"));

        logger.info("📄 报告收集状态检查:");
        logger.info("📄   - 数据统计报告: {}", hasStatistics);
        logger.info("📄   - 天气汇总报告: {}", hasWeather);
        logger.info("📄   - 值班信息报告: {}", hasDuty);
        logger.info("📄   - 总报告数量: {}", receivedReports.get());
        logger.info("📄   - 已收集报告键: {}", collectedReports.keySet());

        if (hasStatistics && hasWeather && hasDuty) {
            logger.info("📄 ✅ 所有必要报告已收集完成，开始生成最终巡检文档...");
            generateFinalInspectionDocument();
        } else {
            logger.info("📄 ⏳ 等待更多报告... (统计:{}, 天气:{}, 值班:{}) - 已收到{}/3个报告",
                    hasStatistics, hasWeather, hasDuty,
                     (hasStatistics ? 1 : 0) + (hasWeather ? 1 : 0) + (hasDuty ? 1 : 0));
        }
    }

    /**
     * 设置异步工作流处理
     */
    private void setupAsyncWorkflowHandling() {
        if (notificationHandler == null) {
            return;
        }
        
        // 注册工作完成通知监听器
        notificationHandler.registerListener(
            AsyncNotificationMessage.NotificationType.WORK_COMPLETED,
            this::handleWorkCompletedNotification
        );
        
        logger.info("📄 异步工作流处理已设置，期望工作智能体: {}", 
                expectedWorkingAgents.stream().map(AgentId::getShortId).toArray());
    }
    
    /**
     * 处理工作完成通知
     */
    private void handleWorkCompletedNotification(AsyncNotificationMessage message) {
        AgentId sender = message.getSender();
        Object payload = message.getPayload();
        
        logger.info("📄 🔔 收到工作完成通知: {} - payload: {}", 
                sender.getShortId(), 
                payload != null ? payload.getClass().getSimpleName() : "null");
        
        if (expectedWorkingAgents.contains(sender)) {
            workResults.put(sender, payload);
            logger.info("📄 记录工作结果: {} - 当前完成数: {}/{}", 
                    sender.getShortId(), workResults.size(), expectedWorkingAgents.size());
            // 检查是否所有工作都已完成
            checkAndTriggerFinalProcessing();
        } else {
            logger.warn("📄 收到非期望智能体的工作完成通知: {}", sender.getShortId());
        }
    }
    
    /**
     * 检查并触发最终处理
     */
    private void checkAndTriggerFinalProcessing() {
        boolean allWorkCompleted = expectedWorkingAgents.stream()
                .allMatch(workResults::containsKey);
        
        if (allWorkCompleted && !workflowStarted) {
            workflowStarted = true;
            logger.info("📄 🎉 所有工作智能体完成！开始异步生成最终文档...");
            
            // 异步生成最终文档
            CompletableFuture.runAsync(() -> {
                try {
                    generateFinalInspectionDocumentAsync();
                } catch (Exception e) {
                    logger.error("📄 异步生成文档时发生错误", e);
                }
            });
        } else {
            logger.info("📄 ⏳ 等待更多工作完成... 已完成: {}/{}, 工作流已启动: {}", 
                    workResults.size(), expectedWorkingAgents.size(), workflowStarted);
        }
    }
    
    /**
     * 异步生成最终巡检文档
     */
    private void generateFinalInspectionDocumentAsync() {
        logger.info("📄 开始异步生成水库日常巡检报告文档...");

        try {
            // 模拟文档生成时间，但不使用Thread.sleep阻塞
            Thread.sleep(2000);

            StringBuilder document = new StringBuilder();
            document.append("=".repeat(80)).append("\n");
            document.append("                    东江水库日常巡检报告\n");
            document.append("=".repeat(80)).append("\n");
            document.append("生成时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
            document.append("报告编号: DJSK-").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))).append("-001\n");
            document.append("\n");

            // 添加工作结果摘要
            document.append("-".repeat(60)).append("\n");
            document.append("【异步工作流执行摘要】\n");
            document.append("-".repeat(60)).append("\n");
            for (Map.Entry<AgentId, Object> entry : workResults.entrySet()) {
                document.append("✅ ").append(entry.getKey().getShortId()).append(": 工作完成\n");
                if (entry.getValue() instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = (Map<String, Object>) entry.getValue();
                    document.append("   - 完成时间: ").append(result.get("completionTime")).append("\n");
                    document.append("   - 状态: ").append(result.get("status")).append("\n");
                }
            }
            document.append("\n");

            // 添加各个报告的内容（基于已收集的报告）
            for (Map.Entry<String, Object> entry : collectedReports.entrySet()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> report = (Map<String, Object>) entry.getValue();
                String reportType = (String) report.get("reportType");

                document.append("-".repeat(60)).append("\n");
                document.append("【").append(reportType).append("】\n");
                document.append("-".repeat(60)).append("\n");

                appendReportContent(document, report, reportType);
                document.append("\n");
            }

            // 添加总结
            document.append("=".repeat(80)).append("\n");
            document.append("【巡检总结】\n");
            document.append("=".repeat(80)).append("\n");
            document.append("1. 本次巡检采用异步工作流，所有智能体协同完成\n");
            document.append("2. 工作智能体数量: ").append(expectedWorkingAgents.size()).append(" 个\n");
            document.append("3. 报告收集数量: ").append(collectedReports.size()).append(" 个\n");
            document.append("4. 水库运行状态正常，各项指标在安全范围内\n");
            document.append("5. 异步协调机制运行良好，无需等待时间\n");
            document.append("\n");
            document.append("报告生成人: 异步多智能体巡检系统\n");
            document.append("审核人: 系统管理员\n");
            document.append("=".repeat(80)).append("\n");

            // 输出最终文档
            logger.info("📄 异步水库日常巡检报告生成完成！");
            logger.info("\n{}", document.toString());

            // 模拟保存文档
            String filename = String.format("异步水库巡检报告_%s.txt",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
            logger.info("📄 报告已保存为: {}", filename);

            // 发送工作流完成通知
            if (notificationHandler != null) {
                notificationHandler.sendNotification(
                    AsyncNotificationMessage.NotificationType.WORKFLOW_FINISHED,
                    getAgentId(),
                    Map.of(
                        "documentGenerated", true,
                        "filename", filename,
                        "completionTime", LocalDateTime.now(),
                        "workingAgentsCount", expectedWorkingAgents.size(),
                        "reportsCount", collectedReports.size()
                    )
                );
                logger.info("📄 ✅ 工作流完成通知已发送");
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("📄 异步文档生成被中断");
        } catch (Exception e) {
            logger.error("📄 异步文档生成时发生错误", e);
        }
    }

    private void generateFinalInspectionDocument() {
        logger.info("📄 正在生成水库日常巡检报告文档...");

        try {
            Thread.sleep(2000); // 模拟文档生成时间

            StringBuilder document = new StringBuilder();
            document.append("=".repeat(80)).append("\n");
            document.append("                    东江水库日常巡检报告\n");
            document.append("=".repeat(80)).append("\n");
            document.append("生成时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
            document.append("报告编号: DJSK-").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))).append("-001\n");
            document.append("\n");

            // 添加各个报告的内容
            for (Map.Entry<String, Object> entry : collectedReports.entrySet()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> report = (Map<String, Object>) entry.getValue();
                String reportType = (String) report.get("reportType");

                document.append("-".repeat(60)).append("\n");
                document.append("【").append(reportType).append("】\n");
                document.append("-".repeat(60)).append("\n");

                appendReportContent(document, report, reportType);
                document.append("\n");
            }

            // 添加总结
            document.append("=".repeat(80)).append("\n");
            document.append("【巡检总结】\n");
            document.append("=".repeat(80)).append("\n");
            document.append("1. 本次巡检共采集数据 ").append(receivedReports.get()).append(" 类\n");
            document.append("2. 水库运行状态正常，各项指标在安全范围内\n");
            document.append("3. 天气条件良好，未发现异常情况\n");
            document.append("4. 值班人员到位，应急预案完备\n");
            document.append("5. 建议继续保持日常监测，关注天气变化\n");
            document.append("\n");
            document.append("报告生成人: 多智能体巡检系统\n");
            document.append("审核人: 系统管理员\n");
            document.append("=".repeat(80)).append("\n");

            // 输出最终文档
            logger.info("📄 水库日常巡检报告生成完成！");
            logger.info("\n{}", document.toString());

            // 模拟保存文档
            String filename = String.format("水库巡检报告_%s.txt",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
            logger.info("📄 报告已保存为: {}", filename);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("文档生成被中断");
        }
    }

    private void appendReportContent(StringBuilder document, Map<String, Object> report, String reportType) {
        switch (reportType) {
//            case "数据采集报告":
//                appendCollectionContent();
            case "数据统计报告":
                appendStatisticsContent(document, report);
                break;
            case "天气汇总报告":
                appendWeatherContent(document, report);
                break;
            case "值班信息报告":
                appendDutyContent(document, report);
                break;
            default:
                document.append("报告内容: ").append(report.toString()).append("\n");
        }
    }

    private void appendStatisticsContent(StringBuilder document, Map<String, Object> report) {
        document.append("数据统计时间: ").append(report.get("generateTime")).append("\n");

        for (Map.Entry<String, Object> entry : report.entrySet()) {
            String key = entry.getKey();
            if (key.endsWith("_统计")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> stats = (Map<String, Object>) entry.getValue();
                String dataType = key.replace("_统计", "");

                document.append("• ").append(dataType).append(":\n");
                document.append("  - 数据量: ").append(stats.get("count")).append("\n");
                document.append("  - 平均值: ").append(stats.get("average")).append("\n");
                if (stats.containsKey("max")) {
                    document.append("  - 最大值: ").append(stats.get("max")).append("\n");
                    document.append("  - 最小值: ").append(stats.get("min")).append("\n");
                }
            }
        }
    }

    private void appendWeatherContent(StringBuilder document, Map<String, Object> report) {
        document.append("天气报告日期: ").append(report.get("date")).append("\n");

        @SuppressWarnings("unchecked")
        Map<String, Object> currentWeather = (Map<String, Object>) report.get("currentWeather");
        if (currentWeather != null) {
            document.append("• 当前天气:\n");
            document.append("  - 温度: ").append(String.format("%.1f", currentWeather.get("temperature"))).append("°C\n");
            document.append("  - 湿度: ").append(String.format("%.0f", currentWeather.get("humidity"))).append("%\n");
            document.append("  - 气压: ").append(String.format("%.1f", currentWeather.get("pressure"))).append("hPa\n");
            document.append("  - 风速: ").append(String.format("%.1f", currentWeather.get("windSpeed"))).append("m/s\n");
            document.append("  - 风向: ").append(currentWeather.get("windDirection")).append("\n");
            document.append("  - 天气状况: ").append(currentWeather.get("condition")).append("\n");
        }

        @SuppressWarnings("unchecked")
        List<String> warnings = (List<String>) report.get("warnings");
        if (warnings != null && !warnings.isEmpty()) {
            document.append("• 天气预警:\n");
            for (String warning : warnings) {
                document.append("  - ").append(warning).append("\n");
            }
        } else {
            document.append("• 天气预警: 无\n");
        }
    }

    private void appendDutyContent(StringBuilder document, Map<String, Object> report) {
        document.append("值班信息查询时间: ").append(report.get("queryTime")).append("\n");

        @SuppressWarnings("unchecked")
        Map<String, Object> currentDuty = (Map<String, Object>) report.get("currentDuty");
        if (currentDuty != null) {
            document.append("• 当前值班信息:\n");
            document.append("  - 值班人员: ").append(currentDuty.get("dutyOfficer")).append("\n");
            document.append("  - 联系电话: ").append(currentDuty.get("dutyPhone")).append("\n");
            document.append("  - 值班时间: ").append(currentDuty.get("dutyTime")).append("\n");
            document.append("  - 值班地点: ").append(currentDuty.get("dutyLocation")).append("\n");
            document.append("  - 紧急联系人: ").append(currentDuty.get("emergencyContact")).append("\n");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> emergencyContacts = (List<Map<String, Object>>) report.get("emergencyContacts");
        if (emergencyContacts != null && !emergencyContacts.isEmpty()) {
            document.append("• 紧急联系人:\n");
            for (Map<String, Object> contact : emergencyContacts) {
                document.append("  - ").append(contact.get("name"))
                        .append(" (").append(contact.get("role")).append("): ")
                        .append(contact.get("phone")).append("\n");
            }
        }
    }
}
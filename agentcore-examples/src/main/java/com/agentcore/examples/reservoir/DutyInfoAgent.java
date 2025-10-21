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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 值班信息智能体
 * 负责查看和管理值班信息
 */
public class DutyInfoAgent extends AbstractAgent {
    private static final Logger logger = LoggerFactory.getLogger(DutyInfoAgent.class);
    private List<AgentId> collaborators = new ArrayList<>();
    private MessageRouter messageRouter;
    private AsyncNotificationHandler notificationHandler;

    public DutyInfoAgent(AgentId agentId) {
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
        logger.debug("值班信息智能体发送消息: {}", message.content());
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
        logger.info("👥 值班信息智能体启动成功");
        
        // 发送启动完成通知
        if (notificationHandler != null) {
            notificationHandler.sendNotification(
                AsyncNotificationMessage.NotificationType.AGENT_READY, 
                getAgentId()
            );
            logger.info("👥 ✅ 值班信息智能体启动完成通知已发送");
        }
    }

    @Override
    protected void doStop() {
        logger.info("👥 值班信息智能体停止");
    }

    @Override
    protected void doHandleMessage(AgentMessage message) {
        logger.info("👥 值班信息智能体收到消息: {}", message.content());
    }

    public void startDutyInfoQuery() {
        logger.info("👥 开始查询值班信息...");

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(2000); // 模拟查询时间

                Map<String, Object> dutyReport = generateDutyReport();
                sendDutyReportToCollaborators(dutyReport);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("值班信息查询被中断");
            }
        });
    }

    private Map<String, Object> generateDutyReport() {
        Map<String, Object> report = new HashMap<>();
        report.put("reportType", "值班信息报告");
        report.put("date", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        report.put("queryTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME));

        // 当前值班信息
        Map<String, Object> currentDuty = new HashMap<>();
        currentDuty.put("dutyOfficer", "张工程师");
        currentDuty.put("dutyPhone", "138****1234");
        currentDuty.put("dutyTime", "08:00-20:00");
        currentDuty.put("dutyLocation", "水库管理中心");
        currentDuty.put("emergencyContact", "李主任 139****5678");

        report.put("currentDuty", currentDuty);

        // 值班记录
        List<Map<String, Object>> dutyRecords = new ArrayList<>();
        String[] officers = {"张工程师", "李工程师", "王工程师", "赵工程师"};
        for (int i = 0; i < 7; i++) {
            Map<String, Object> record = new HashMap<>();
            record.put("date", LocalDateTime.now().minusDays(i).format(DateTimeFormatter.ISO_LOCAL_DATE));
            record.put("officer", officers[i % officers.length]);
            record.put("startTime", "08:00");
            record.put("endTime", "20:00");
            record.put("status", i == 0 ? "当前值班" : "已完成");
            record.put("incidents", i % 3 == 0 ? "无异常" : "设备巡检正常");
            dutyRecords.add(record);
        }
        report.put("dutyRecords", dutyRecords);

        // 值班安排
        List<Map<String, Object>> dutySchedule = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            Map<String, Object> schedule = new HashMap<>();
            schedule.put("date", LocalDateTime.now().plusDays(i).format(DateTimeFormatter.ISO_LOCAL_DATE));
            schedule.put("officer", officers[i % officers.length]);
            schedule.put("shift", "日班 08:00-20:00");
            schedule.put("backup", officers[(i + 1) % officers.length]);
            dutySchedule.add(schedule);
        }
        report.put("dutySchedule", dutySchedule);

        // 紧急联系人
        List<Map<String, Object>> emergencyContacts = new ArrayList<>();
        emergencyContacts.add(Map.of("name", "李主任", "phone", "139****5678", "role", "技术负责人"));
        emergencyContacts.add(Map.of("name", "陈经理", "phone", "138****9012", "role", "运营负责人"));
        emergencyContacts.add(Map.of("name", "应急中心", "phone", "119", "role", "紧急救援"));
        report.put("emergencyContacts", emergencyContacts);

        logger.info("👥 值班信息报告生成完成: 当前值班={}, 联系电话={}",
                currentDuty.get("dutyOfficer"), currentDuty.get("dutyPhone"));

        return report;
    }

    private void sendDutyReportToCollaborators(Map<String, Object> report) {
        logger.info("👥 准备发送值班信息报告给 {} 个协作智能体", collaborators.size());
        logger.info("👥 报告内容: reportType={}, date={}", 
                report.get("reportType"), report.get("date"));
        
        for (AgentId collaborator : collaborators) {
            logger.info("👥 发送值班信息报告给: {}", collaborator.getShortId());
            AgentMessage message = AgentMessage.builder()
                    .sender(getAgentId())
                    .receiver(collaborator)
                    .performative(MessagePerformative.INFORM)
                    .content(report)
                    .build();
            sendMessage(message);
            logger.info("👥 ✅ 值班信息报告已发送给: {}", collaborator.getShortId());
        }
        logger.info("👥 值班信息报告已发送给所有协作智能体");
        
        // 发送工作完成异步通知
        if (notificationHandler != null) {
            Map<String, Object> workResult = new HashMap<>();
            workResult.put("agentType", "DutyInfoAgent");
            workResult.put("completionTime", LocalDateTime.now());
            workResult.put("reportGenerated", true);
            workResult.put("status", "completed");
            
            notificationHandler.sendNotification(
                AsyncNotificationMessage.NotificationType.WORK_COMPLETED,
                getAgentId(),
                workResult
            );
            logger.info("👥 ✅ 值班信息工作完成通知已发送");
        }
    }
}


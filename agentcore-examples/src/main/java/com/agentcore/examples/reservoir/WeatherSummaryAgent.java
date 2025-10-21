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
 * 天气汇总智能体
 * 负责汇总天气相关数据
 */
public class WeatherSummaryAgent extends AbstractAgent {
    private static final Logger logger = LoggerFactory.getLogger(WeatherSummaryAgent.class);
    private List<AgentId> collaborators = new ArrayList<>();
    private MessageRouter messageRouter;
    private AsyncNotificationHandler notificationHandler;

    public WeatherSummaryAgent(AgentId agentId) {
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
        logger.debug("天气汇总智能体发送消息: {}", message.content());
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
        logger.info("🌤️ 天气汇总智能体启动成功");
        
        // 发送启动完成通知
        if (notificationHandler != null) {
            notificationHandler.sendNotification(
                AsyncNotificationMessage.NotificationType.AGENT_READY, 
                getAgentId()
            );
            logger.info("🌤️ ✅ 天气汇总智能体启动完成通知已发送");
        }
    }

    @Override
    protected void doStop() {
        logger.info("🌤️ 天气汇总智能体停止");
    }

    @Override
    protected void doHandleMessage(AgentMessage message) {
        logger.info("🌤️ 天气汇总智能体收到消息: {}", message.content());
    }

    public void startWeatherCollection() {
        logger.info("🌤️ 开始天气数据汇总...");

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(1000); // 等待一段时间模拟数据收集

                Map<String, Object> weatherReport = generateWeatherReport();
                sendWeatherReportToCollaborators(weatherReport);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("天气数据收集被中断");
            }
        });
    }

    private Map<String, Object> generateWeatherReport() {
        Map<String, Object> report = new HashMap<>();
        report.put("reportType", "天气汇总报告");
        report.put("date", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        report.put("time", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME));

        // 模拟天气数据
        Map<String, Object> currentWeather = new HashMap<>();
        currentWeather.put("temperature", 24.5 + Math.random() * 10);
        currentWeather.put("humidity", 65 + Math.random() * 20);
        currentWeather.put("pressure", 1013.2 + Math.random() * 10);
        currentWeather.put("windSpeed", 3.2 + Math.random() * 5);
        currentWeather.put("windDirection", "东南风");
        currentWeather.put("visibility", 15 + Math.random() * 10);
        currentWeather.put("condition", "多云");

        report.put("currentWeather", currentWeather);

        // 24小时预报
        List<Map<String, Object>> forecast = new ArrayList<>();
        for (int i = 1; i <= 24; i++) {
            Map<String, Object> hourlyForecast = new HashMap<>();
            hourlyForecast.put("hour", i);
            hourlyForecast.put("temperature", 20 + Math.random() * 15);
            hourlyForecast.put("condition", i % 3 == 0 ? "雨" : (i % 2 == 0 ? "多云" : "晴"));
            hourlyForecast.put("precipitation", i % 4 == 0 ? Math.random() * 5 : 0);
            forecast.add(hourlyForecast);
        }
        report.put("forecast24h", forecast);

        // 预警信息
        List<String> warnings = new ArrayList<>();
        if (Math.random() > 0.7) {
            warnings.add("大风预警：预计未来6小时风力将达到7-8级");
        }
        if (Math.random() > 0.8) {
            warnings.add("暴雨预警：预计未来12小时降雨量将超过50mm");
        }
        report.put("warnings", warnings);

        logger.info("🌤️ 天气报告生成完成: 当前温度={}°C, 湿度={}%, 天气={}",
                String.format("%.1f", currentWeather.get("temperature")),
                String.format("%.0f", currentWeather.get("humidity")),
                currentWeather.get("condition"));

        return report;
    }

    private void sendWeatherReportToCollaborators(Map<String, Object> report) {
        logger.info("🌤️ 准备发送天气报告给 {} 个协作智能体", collaborators.size());
        logger.info("🌤️ 报告内容: reportType={}, date={}", 
                report.get("reportType"), report.get("date"));
        
        for (AgentId collaborator : collaborators) {
            logger.info("🌤️ 发送天气报告给: {}", collaborator.getShortId());
            AgentMessage message = AgentMessage.builder()
                    .sender(getAgentId())
                    .receiver(collaborator)
                    .performative(MessagePerformative.INFORM)
                    .content(report)
                    .build();
            sendMessage(message);
            logger.info("🌤️ ✅ 天气报告已发送给: {}", collaborator.getShortId());
        }
        logger.info("🌤️ 天气报告已发送给所有协作智能体");
        
        // 发送工作完成异步通知
        if (notificationHandler != null) {
            Map<String, Object> workResult = new HashMap<>();
            workResult.put("agentType", "WeatherSummaryAgent");
            workResult.put("completionTime", LocalDateTime.now());
            workResult.put("reportGenerated", true);
            workResult.put("status", "completed");
            
            notificationHandler.sendNotification(
                AsyncNotificationMessage.NotificationType.WORK_COMPLETED,
                getAgentId(),
                workResult
            );
            logger.info("🌤️ ✅ 天气汇总工作完成通知已发送");
        }
    }
}
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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 数据采集智能体
 * 负责采集水库的各种监测数据
 */
public class DataCollectionAgent extends AbstractAgent {
    private static final Logger logger = LoggerFactory.getLogger(DataCollectionAgent.class);
    private List<AgentId> collaborators = new ArrayList<>();
    private final AtomicInteger collectionCount = new AtomicInteger(0);
    private MessageRouter messageRouter;
    private AsyncNotificationHandler notificationHandler;

    public DataCollectionAgent(AgentId agentId) {
        super(agentId);
    }
    
    public void setMessageRouter(MessageRouter messageRouter) {
        this.messageRouter = messageRouter;
    }
    
    public void setNotificationHandler(AsyncNotificationHandler notificationHandler) {
        this.notificationHandler = notificationHandler;
    }

    public void setCollaborators(List<AgentId> collaborators) {
        this.collaborators = collaborators;
    }

    @Override
    protected BehaviorScheduler createBehaviorScheduler() {
        return new DefaultBehaviorScheduler();
    }

    @Override
    protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
        logger.debug("数据采集智能体发送消息: {}", message.content());
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
        logger.info("📊 数据采集智能体启动成功");
        
        // 发送启动完成通知
        if (notificationHandler != null) {
            notificationHandler.sendNotification(
                AsyncNotificationMessage.NotificationType.AGENT_READY, 
                getAgentId()
            );
            logger.info("📊 ✅ 数据采集智能体启动完成通知已发送");
        }
    }

    @Override
    protected void doStop() {
        logger.info("📊 数据采集智能体停止");
    }

    @Override
    protected void doHandleMessage(AgentMessage message) {
        logger.info("📊 数据采集智能体收到消息: {}", message.content());
    }

    public void startInspection() {
        logger.info("📊 开始数据采集...");

        // 模拟采集不同类型的数据
        CompletableFuture.runAsync(() -> {
            try {
                collectWaterLevelData();
                Thread.sleep(1000);
                collectWaterQualityData();
                //Thread.sleep(2000);
                collectFlowRateData();
                //Thread.sleep(2000);
                collectTemperatureData();

                // 通知统计智能体和文档输出智能体
                notifyDataReady();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("数据采集被中断");
            }
        });
    }

    private void collectWaterLevelData() {
        int count = collectionCount.incrementAndGet();
        Map<String, Object> data = new HashMap<>();
        data.put("type", "water_level");
        data.put("value", 125.6 + Math.random() * 10); // 模拟水位数据
        data.put("unit", "米");
        data.put("timestamp", LocalDateTime.now());
        data.put("location", "水库主坝");

        logger.info("📊 采集水位数据 #{}: {} 米", count, data.get("value"));
        sendDataToCollaborators("水位数据", data);
    }

    private void collectWaterQualityData() {
        int count = collectionCount.incrementAndGet();
        Map<String, Object> data = new HashMap<>();
        data.put("type", "water_quality");
        data.put("ph", 7.2 + Math.random() * 0.6);
        data.put("dissolved_oxygen", 8.5 + Math.random() * 2);
        data.put("turbidity", 2.1 + Math.random() * 1);
        data.put("timestamp", LocalDateTime.now());
        data.put("location", "取水口");

        logger.info("📊 采集水质数据 #{}: pH={}, 溶解氧={}", count,
                String.format("%.2f", data.get("ph")),
                String.format("%.2f", data.get("dissolved_oxygen")));
        sendDataToCollaborators("水质数据", data);
    }

    private void collectFlowRateData() {
        int count = collectionCount.incrementAndGet();
        Map<String, Object> data = new HashMap<>();
        data.put("type", "flow_rate");
        data.put("value", 45.2 + Math.random() * 20);
        data.put("unit", "立方米/秒");
        data.put("timestamp", LocalDateTime.now());
        data.put("location", "泄洪口");

        logger.info("📊 采集流量数据 #{}: {} 立方米/秒", count,
                String.format("%.2f", data.get("value")));
        sendDataToCollaborators("流量数据", data);
    }

    private void collectTemperatureData() {
        int count = collectionCount.incrementAndGet();
        Map<String, Object> data = new HashMap<>();
        data.put("type", "temperature");
        data.put("water_temp", 18.5 + Math.random() * 8);
        data.put("air_temp", 22.3 + Math.random() * 12);
        data.put("unit", "摄氏度");
        data.put("timestamp", LocalDateTime.now());

        logger.info("📊 采集温度数据 #{}: 水温={}°C, 气温={}°C", count,
                String.format("%.1f", data.get("water_temp")),
                String.format("%.1f", data.get("air_temp")));
        sendDataToCollaborators("温度数据", data);
    }

    private void sendDataToCollaborators(String dataType, Map<String, Object> data) {
        for (AgentId collaborator : collaborators) {
            AgentMessage message = AgentMessage.builder()
                    .sender(getAgentId())
                    .receiver(collaborator)
                    .performative(MessagePerformative.INFORM)
                    .content(Map.of("dataType", dataType, "data", data))
                    .build();
            sendMessage(message);
        }
    }

    private void notifyDataReady() {
        logger.info("📊 所有数据采集完成，通知协作智能体");
        
        // 发送传统消息给协作智能体
        for (AgentId collaborator : collaborators) {
            AgentMessage message = AgentMessage.builder()
                    .sender(getAgentId())
                    .receiver(collaborator)
                    .performative(MessagePerformative.INFORM)
                    .content("数据采集完成")
                    .build();
            sendMessage(message);
        }
        
        // 发送工作完成异步通知
        if (notificationHandler != null) {
            Map<String, Object> workResult = new HashMap<>();
            workResult.put("agentType", "DataCollectionAgent");
            workResult.put("completionTime", LocalDateTime.now());
            workResult.put("dataCollected", collectionCount.get());
            workResult.put("status", "completed");
            
            notificationHandler.sendNotification(
                AsyncNotificationMessage.NotificationType.WORK_COMPLETED,
                getAgentId(),
                workResult
            );
            logger.info("📊 ✅ 数据采集工作完成通知已发送");
        }
    }
}

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
 * 数据采集智能体
 * 负责采集水库大坝的各种安全监测数据
 */
public class DataCollectionAgent extends AbstractAgent {
    private static final Logger logger = LoggerFactory.getLogger(DataCollectionAgent.class);
    
    private MessageRouter messageRouter;
    private AgentId dataProcessorId;
    private final AtomicInteger collectionCount = new AtomicInteger(0);
    private boolean isCollecting = false;

    public DataCollectionAgent(AgentId agentId) {
        super(agentId);
    }
    
    public void setMessageRouter(MessageRouter messageRouter) {
        this.messageRouter = messageRouter;
    }
    
    public void setDataProcessorId(AgentId dataProcessorId) {
        this.dataProcessorId = dataProcessorId;
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
        logger.info("🔍 数据采集智能体启动成功");
    }

    @Override
    protected void doStop() {
        logger.info("🔍 数据采集智能体停止");
        isCollecting = false;
    }

    @Override
    protected void doHandleMessage(AgentMessage message) {
        logger.info("🔍 数据采集智能体收到消息: {}", message.content());
        
        // 处理来自协调智能体的指令
        if (message.content() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> content = (Map<String, Object>) message.content();
            String command = (String) content.get("command");
            
            if ("START_COLLECTION".equals(command)) {
                startDataCollection();
            } else if ("STOP_COLLECTION".equals(command)) {
                stopDataCollection();
            }
        }
    }
    
    /**
     * 开始数据采集
     */
    public void startDataCollection() {
        if (isCollecting) {
            logger.warn("🔍 数据采集已在进行中");
            return;
        }
        
        isCollecting = true;
        logger.info("🔍 开始采集水库大坝安全数据...");
        
        CompletableFuture.runAsync(() -> {
            try {
                // 采集各种安全数据
                collectStructuralData();
                Thread.sleep(500);
                collectSeepageData();
                Thread.sleep(500);
                collectDeformationData();
                Thread.sleep(500);
                collectEnvironmentalData();
                
                // 采集完成，发送给数据处理智能体
                sendCollectedDataToProcessor();
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("🔍 数据采集被中断");
            } catch (Exception e) {
                logger.error("🔍 数据采集过程中发生错误", e);
            } finally {
                isCollecting = false;
            }
        });
    }
    
    /**
     * 停止数据采集
     */
    public void stopDataCollection() {
        isCollecting = false;
        logger.info("🔍 停止数据采集");
    }
    
    /**
     * 采集结构安全数据
     */
    private void collectStructuralData() {
        int count = collectionCount.incrementAndGet();
        Map<String, Object> structuralData = new HashMap<>();
        structuralData.put("dataType", "structural");
        structuralData.put("concreteCracks", Math.random() * 5); // 混凝土裂缝数量
        structuralData.put("jointDisplacement", Math.random() * 2.5); // 接缝位移 mm
        structuralData.put("surfaceCondition", Math.random() > 0.8 ? "异常" : "正常");
        structuralData.put("timestamp", LocalDateTime.now());
        structuralData.put("location", "大坝主体结构");
        
        logger.info("🔍 采集结构安全数据 #{}: 裂缝数={}, 接缝位移={}mm", 
                count, String.format("%.1f", structuralData.get("concreteCracks")),
                String.format("%.2f", structuralData.get("jointDisplacement")));
    }
    
    /**
     * 采集渗流数据
     */
    private void collectSeepageData() {
        int count = collectionCount.incrementAndGet();
        Map<String, Object> seepageData = new HashMap<>();
        seepageData.put("dataType", "seepage");
        seepageData.put("seepageRate", 15.5 + Math.random() * 10); // 渗流量 L/min
        seepageData.put("waterPressure", 0.8 + Math.random() * 0.4); // 水压 MPa
        seepageData.put("seepageQuality", Math.random() > 0.9 ? "浑浊" : "清澈");
        seepageData.put("timestamp", LocalDateTime.now());
        seepageData.put("location", "坝基渗流监测点");
        
        logger.info("🔍 采集渗流数据 #{}: 渗流量={}L/min, 水压={}MPa", 
                count, String.format("%.1f", seepageData.get("seepageRate")),
                String.format("%.2f", seepageData.get("waterPressure")));
    }
    
    /**
     * 采集变形数据
     */
    private void collectDeformationData() {
        int count = collectionCount.incrementAndGet();
        Map<String, Object> deformationData = new HashMap<>();
        deformationData.put("dataType", "deformation");
        deformationData.put("horizontalDisplacement", Math.random() * 3.0); // 水平位移 mm
        deformationData.put("verticalDisplacement", Math.random() * 2.0); // 垂直位移 mm
        deformationData.put("tiltAngle", Math.random() * 0.5); // 倾斜角度 度
        deformationData.put("timestamp", LocalDateTime.now());
        deformationData.put("location", "坝顶变形监测点");
        
        logger.info("🔍 采集变形数据 #{}: 水平位移={}mm, 垂直位移={}mm", 
                count, String.format("%.2f", deformationData.get("horizontalDisplacement")),
                String.format("%.2f", deformationData.get("verticalDisplacement")));
    }
    
    /**
     * 采集环境数据
     */
    private void collectEnvironmentalData() {
        int count = collectionCount.incrementAndGet();
        Map<String, Object> environmentalData = new HashMap<>();
        environmentalData.put("dataType", "environmental");
        environmentalData.put("temperature", 18.0 + Math.random() * 15); // 温度 °C
        environmentalData.put("humidity", 60 + Math.random() * 30); // 湿度 %
        environmentalData.put("windSpeed", Math.random() * 12); // 风速 m/s
        environmentalData.put("rainfall", Math.random() * 50); // 降雨量 mm
        environmentalData.put("timestamp", LocalDateTime.now());
        environmentalData.put("location", "大坝周边环境");
        
        logger.info("🔍 采集环境数据 #{}: 温度={}°C, 湿度={}%", 
                count, String.format("%.1f", environmentalData.get("temperature")),
                String.format("%.1f", environmentalData.get("humidity")));
    }
    
    /**
     * 将采集的数据发送给数据处理智能体
     */
    private void sendCollectedDataToProcessor() {
        if (dataProcessorId == null) {
            logger.error("🔍 数据处理智能体ID未设置");
            return;
        }
        
        // 创建包含所有采集数据的消息
        Map<String, Object> allCollectedData = new HashMap<>();
        allCollectedData.put("totalDataPoints", collectionCount.get());
        allCollectedData.put("collectionTime", LocalDateTime.now());
        allCollectedData.put("status", "collection_completed");
        allCollectedData.put("dataTypes", new String[]{"structural", "seepage", "deformation", "environmental"});
        
        // 使用自定义消息格式
        DamSafetyMessage damMessage = DamSafetyMessage.create(
                DamSafetyMessage.MessageType.DATA_COLLECTION,
                getAgentId(),
                dataProcessorId,
                allCollectedData
        );
        
        // 转换为AgentMessage发送
        AgentMessage message = AgentMessage.builder()
                .sender(getAgentId())
                .receiver(dataProcessorId)
                .performative(MessagePerformative.INFORM)
                .content(damMessage)
                .build();
        
        sendMessage(message);
        logger.info("🔍 ✅ 数据采集完成，已发送给数据处理智能体，共采集{}个数据点", collectionCount.get());
    }
}
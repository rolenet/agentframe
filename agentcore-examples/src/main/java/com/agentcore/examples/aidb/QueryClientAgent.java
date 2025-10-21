package com.agentcore.examples.aidb;

import com.agentcore.core.agent.AbstractAgent;
import com.agentcore.core.agent.AgentId;
import com.agentcore.core.behavior.BehaviorScheduler;
import com.agentcore.core.behavior.DefaultBehaviorScheduler;
import com.agentcore.core.message.AgentMessage;
import com.agentcore.core.message.MessagePerformative;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 查询客户端智能体
 * 用于向问库智能体发送查询请求并接收响应
 */
public class QueryClientAgent extends AbstractAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(QueryClientAgent.class);
    
    private AgentId aidbAgentId;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicInteger queryCounter = new AtomicInteger(0);
    
    // 预定义的查询示例
    private final String[] sampleQueries = {
            "查询所有表的信息",
            "显示用户表的结构",
            "统计数据库中有多少个表",
            "查询最近的数据记录",
            "显示所有表名",
            "查询表的字段信息",
            "统计每个表的记录数量",
            "查找包含用户信息的表"
    };
    
    public QueryClientAgent(AgentId agentId) {
        super(agentId);
    }
    
    public void setAIDBAgentId(AgentId aidbAgentId) {
        this.aidbAgentId = aidbAgentId;
    }
    
    @Override
    protected BehaviorScheduler createBehaviorScheduler() {
        return new DefaultBehaviorScheduler();
    }
    
    @Override
    protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
        logger.debug("查询客户端发送消息: {}", message.content());
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    protected void doStart() {
        logger.info("📱 查询客户端智能体启动成功");
        
        if (aidbAgentId == null) {
            logger.error("📱 问库智能体ID未设置，无法发送查询");
            return;
        }
        
        // 启动定时查询任务
        scheduler.scheduleAtFixedRate(this::sendSampleQuery, 3, 8, TimeUnit.SECONDS);
        
        logger.info("📱 开始定时发送查询请求...");
    }
    
    @Override
    protected void doStop() {
        logger.info("📱 查询客户端智能体正在停止...");
        
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("📱 查询客户端智能体已停止");
    }
    
    @Override
    protected void doHandleMessage(AgentMessage message) {
        logger.info("📱 查询客户端收到响应: {}", message.performative());
        
        try {
            if (message.content() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> response = (Map<String, Object>) message.content();
                handleQueryResponse(response);
            } else {
                logger.info("📱 收到响应: {}", message.content());
            }
        } catch (Exception e) {
            logger.error("📱 处理响应消息时发生错误", e);
        }
    }
    
    /**
     * 发送示例查询
     */
    private void sendSampleQuery() {
        if (aidbAgentId == null) {
            return;
        }
        
        int queryIndex = queryCounter.getAndIncrement() % sampleQueries.length;
        String query = sampleQueries[queryIndex];
        
        logger.info("📱 发送查询 #{}: {}", queryCounter.get(), query);
        
        AgentMessage queryMessage = AgentMessage.builder()
                .sender(getAgentId())
                .receiver(aidbAgentId)
                .performative(MessagePerformative.REQUEST)
                .content(query)
                .build();
        
        sendMessage(queryMessage);
    }
    
    /**
     * 处理查询响应
     */
    private void handleQueryResponse(Map<String, Object> response) {
        String type = (String) response.get("type");
        boolean success = (Boolean) response.getOrDefault("success", false);
        
        if ("query_response".equals(type)) {
            String question = (String) response.get("question");
            String sql = (String) response.get("sql");
            String result = (String) response.get("result");
            
            logger.info("📱 ==================== 查询响应 ====================");
            logger.info("📱 问题: {}", question);
            logger.info("📱 生成的SQL: {}", sql);
            logger.info("📱 执行结果: {}", success ? "成功" : "失败");
            logger.info("📱 结果内容:\n{}", result);
            logger.info("📱 ================================================");
            
        } else if ("error_response".equals(type)) {
            String error = (String) response.get("error");
            logger.error("📱 查询失败: {}", error);
        }
    }
    
    /**
     * 手动发送查询
     */
    public void sendQuery(String query) {
        if (aidbAgentId == null) {
            logger.error("📱 问库智能体ID未设置");
            return;
        }
        
        logger.info("📱 手动发送查询: {}", query);
        
        AgentMessage queryMessage = AgentMessage.builder()
                .sender(getAgentId())
                .receiver(aidbAgentId)
                .performative(MessagePerformative.REQUEST)
                .content(query)
                .build();
        
        sendMessage(queryMessage);
    }
    
    /**
     * 发送复杂查询请求
     */
    public void sendComplexQuery(String question, Map<String, Object> parameters) {
        if (aidbAgentId == null) {
            logger.error("📱 问库智能体ID未设置");
            return;
        }
        
        Map<String, Object> queryRequest = Map.of(
                "question", question,
                "parameters", parameters,
                "requestId", System.currentTimeMillis()
        );
        
        AgentMessage queryMessage = AgentMessage.builder()
                .sender(getAgentId())
                .receiver(aidbAgentId)
                .performative(MessagePerformative.REQUEST)
                .content(queryRequest)
                .build();
        
        sendMessage(queryMessage);
        
        logger.info("📱 发送复杂查询: {}", question);
    }
}
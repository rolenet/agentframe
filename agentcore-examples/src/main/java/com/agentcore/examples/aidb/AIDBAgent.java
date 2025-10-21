package com.agentcore.examples.aidb;

import com.agentcore.core.agent.AbstractAgent;
import com.agentcore.core.agent.AgentId;
import com.agentcore.core.behavior.BehaviorScheduler;
import com.agentcore.core.behavior.DefaultBehaviorScheduler;
import com.agentcore.core.message.AgentMessage;
import com.agentcore.core.message.MessagePerformative;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 问库智能体 (AI Database Agent)
 * 通过大模型调用来与数据库进行对话，自动生成和执行SQL查询
 */
public class AIDBAgent extends AbstractAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(AIDBAgent.class);
    
    private DatabaseMetadataService metadataService;
    private OllamaService ollamaService;
    private DatabaseQueryService queryService;
    private boolean initialized = false;
    
    public AIDBAgent(AgentId agentId) {
        super(agentId);
    }
    
    @Override
    protected BehaviorScheduler createBehaviorScheduler() {
        return new DefaultBehaviorScheduler();
    }
    
    @Override
    protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
        logger.debug("问库智能体发送消息: {}", message.content());
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    protected void doInit() {
        logger.info("🤖 问库智能体初始化开始...");
        
        try {
            // 初始化各个服务组件
            metadataService = new DatabaseMetadataService();
            ollamaService = new OllamaService();
            
            // 初始化数据库连接和元数据
            metadataService.initialize();
            queryService = new DatabaseQueryService(metadataService);
            
            // 测试Ollama连接
            if (!ollamaService.testConnection()) {
                throw new RuntimeException("Ollama服务连接失败");
            }
            
            initialized = true;
            logger.info("🤖 问库智能体初始化成功");
            
            // 显示数据库信息
            showDatabaseInfo();
            
        } catch (Exception e) {
            logger.error("🤖 问库智能体初始化失败", e);
            throw new RuntimeException("问库智能体初始化失败", e);
        }
    }
    
    @Override
    protected void doStart() {
        logger.info("🤖 问库智能体启动成功，准备接收查询请求");
        
        if (!initialized) {
            logger.error("🤖 问库智能体未正确初始化，无法启动");
            return;
        }
        
        // 显示使用说明
        showUsageInstructions();
    }
    
    @Override
    protected void doStop() {
        logger.info("🤖 问库智能体正在停止...");
        
        // 关闭各个服务
        if (ollamaService != null) {
            ollamaService.close();
        }
        
        if (metadataService != null) {
            metadataService.close();
        }
        
        logger.info("🤖 问库智能体已停止");
    }
    
    @Override
    protected void doHandleMessage(AgentMessage message) {
        if (!initialized) {
            logger.warn("🤖 问库智能体未初始化，无法处理消息");
            return;
        }
        
        logger.info("🤖 问库智能体收到查询请求: {}", message.content());
        
        try {
            String userQuestion = extractUserQuestion(message);
            if (userQuestion == null || userQuestion.trim().isEmpty()) {
                logger.warn("🤖 无效的查询请求");
                return;
            }
            
            // 处理用户查询
            processUserQuery(userQuestion, message.sender());
            
        } catch (Exception e) {
            logger.error("🤖 处理查询请求时发生错误", e);
            sendErrorResponse(message.sender(), "处理查询请求时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 处理用户查询
     */
    private void processUserQuery(String userQuestion, AgentId requester) {
        logger.info("🤖 开始处理用户查询: {}", userQuestion);
        
        try {
            // 1. 分析用户问题，找到相关表
            List<String> relevantTables = findRelevantTables(userQuestion);
            logger.info("🤖 找到相关表: {}", relevantTables);
            
            // 2. 获取相关表的元数据
            String relevantMetadata = getRelevantMetadata(relevantTables);
            
            // 3. 使用大模型生成SQL
            String generatedSQL = ollamaService.generateSQL(userQuestion, relevantMetadata);
            logger.info("🤖 生成的SQL: {}", generatedSQL);
            
            // 4. 验证SQL安全性
            if (!queryService.validateSQL(generatedSQL)) {
                sendErrorResponse(requester, "生成的SQL语句不安全，只允许SELECT查询");
                return;
            }
            
            // 5. 执行SQL查询
            DatabaseQueryService.QueryResult result = queryService.executeQuery(generatedSQL);
            
            // 6. 格式化并返回结果
            String formattedResult = queryService.formatResult(result);
            sendQueryResponse(requester, userQuestion, generatedSQL, formattedResult, result.isSuccess());
            
        } catch (Exception e) {
            logger.error("🤖 处理用户查询时发生错误", e);
            sendErrorResponse(requester, "查询处理失败: " + e.getMessage());
        }
    }
    
    /**
     * 查找与用户问题相关的表
     */
    private List<String> findRelevantTables(String userQuestion) {
        // 提取关键词
        String[] keywords = userQuestion.toLowerCase()
                .replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5\\s]", " ")
                .split("\\s+");
        
        List<String> relevantTables = metadataService.getAllTableNames().stream().toList();
        
        // 如果关键词较少，返回所有表（让大模型自己判断）
        if (keywords.length <= 2) {
            return relevantTables.subList(0, Math.min(10, relevantTables.size())); // 限制表数量
        }
        
        // 根据关键词搜索相关表
        for (String keyword : keywords) {
            if (keyword.length() > 1) {
                List<String> tables = metadataService.searchRelevantTables(keyword);
                if (!tables.isEmpty()) {
                    return tables;
                }
            }
        }
        
        // 如果没有找到特定相关表，返回前10个表
        return relevantTables.subList(0, Math.min(10, relevantTables.size()));
    }
    
    /**
     * 获取相关表的元数据
     */
    private String getRelevantMetadata(List<String> tableNames) {
        if (tableNames.isEmpty()) {
            return metadataService.getFormattedMetadata();
        }
        
        StringBuilder sb = new StringBuilder();
        for (String tableName : tableNames) {
            sb.append(metadataService.getFormattedMetadata(tableName));
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 从消息中提取用户问题
     */
    private String extractUserQuestion(AgentMessage message) {
        Object content = message.content();
        
        if (content instanceof String) {
            return (String) content;
        } else if (content instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> contentMap = (Map<String, Object>) content;
            return (String) contentMap.get("question");
        }
        
        return null;
    }
    
    /**
     * 发送查询响应
     */
    private void sendQueryResponse(AgentId requester, String question, String sql, 
                                 String result, boolean success) {
        Map<String, Object> response = Map.of(
                "type", "query_response",
                "question", question,
                "sql", sql,
                "result", result,
                "success", success,
                "timestamp", System.currentTimeMillis()
        );
        
        AgentMessage responseMessage = AgentMessage.builder()
                .sender(getAgentId())
                .receiver(requester)
                .performative(MessagePerformative.INFORM)
                .content(response)
                .build();
        
        sendMessage(responseMessage);
        
        logger.info("🤖 查询响应已发送给 {}", requester.getShortId());
    }
    
    /**
     * 发送错误响应
     */
    private void sendErrorResponse(AgentId requester, String errorMessage) {
        Map<String, Object> response = Map.of(
                "type", "error_response",
                "error", errorMessage,
                "success", false,
                "timestamp", System.currentTimeMillis()
        );
        
        AgentMessage responseMessage = AgentMessage.builder()
                .sender(getAgentId())
                .receiver(requester)
                .performative(MessagePerformative.FAILURE)
                .content(response)
                .build();
        
        sendMessage(responseMessage);
        
        logger.warn("🤖 错误响应已发送给 {}: {}", requester.getShortId(), errorMessage);
    }
    
    /**
     * 显示数据库信息
     */
    private void showDatabaseInfo() {
        logger.info("🤖 ==================== 数据库信息 ====================");
        logger.info("🤖 数据库地址: {}:{}", DatabaseConfig.DB_HOST, DatabaseConfig.DB_PORT);
        logger.info("🤖 数据库名称: {}", DatabaseConfig.DB_NAME);
        logger.info("🤖 可用表数量: {}", metadataService.getAllTableNames().size());
        logger.info("🤖 表列表: {}", metadataService.getAllTableNames());
        logger.info("🤖 ====================================================");
    }
    
    /**
     * 显示使用说明
     */
    private void showUsageInstructions() {
        logger.info("🤖 ==================== 使用说明 ====================");
        logger.info("🤖 问库智能体已启动，可以接收自然语言查询请求");
        logger.info("🤖 支持的查询示例:");
        logger.info("🤖   - '查询所有用户信息'");
        logger.info("🤖   - '统计订单数量'");
        logger.info("🤖   - '查找最近一周的销售数据'");
        logger.info("🤖   - '显示产品分类统计'");
        logger.info("🤖 注意: 只支持SELECT查询，不允许修改数据");
        logger.info("🤖 ====================================================");
    }
    
    /**
     * 手动执行查询（用于测试）
     */
    public void executeQuery(String userQuestion) {
        if (!initialized) {
            logger.error("🤖 问库智能体未初始化");
            return;
        }
        
        logger.info("🤖 手动执行查询: {}", userQuestion);
        processUserQuery(userQuestion, getAgentId());
    }
}
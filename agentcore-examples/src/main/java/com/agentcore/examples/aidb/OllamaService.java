package com.agentcore.examples.aidb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Ollama大模型服务
 * 负责与Ollama API进行交互，生成SQL查询语句
 */
public class OllamaService {
    
    private static final Logger logger = LoggerFactory.getLogger(OllamaService.class);
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public OllamaService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(DatabaseConfig.CONNECTION_TIMEOUT))
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 生成SQL查询语句
     * 
     * @param userQuestion 用户问题
     * @param databaseMetadata 数据库元数据
     * @return 生成的SQL语句
     */
    public String generateSQL(String userQuestion, String databaseMetadata) throws IOException, InterruptedException {
        logger.info("开始生成SQL查询，用户问题: {}", userQuestion);
        
        // 构建提示词
        String prompt = String.format(DatabaseConfig.SQL_GENERATION_PROMPT_TEMPLATE,
                DatabaseConfig.DB_NAME, databaseMetadata, userQuestion);
        
        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", DatabaseConfig.OLLAMA_MODEL);
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);
        requestBody.put("options", Map.of(
                "temperature", 0.1,  // 降低随机性，提高准确性
                "top_p", 0.9,
                "max_tokens", 500
        ));
        
        String requestJson = objectMapper.writeValueAsString(requestBody);
        
        // 发送HTTP请求
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(DatabaseConfig.OLLAMA_GENERATE_URL))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMillis(DatabaseConfig.READ_TIMEOUT))
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();
        
        logger.debug("发送请求到Ollama: {}", DatabaseConfig.OLLAMA_GENERATE_URL);
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            logger.error("Ollama API请求失败，状态码: {}, 响应: {}", response.statusCode(), response.body());
            throw new IOException("Ollama API请求失败，状态码: " + response.statusCode());
        }
        
        // 解析响应
        JsonNode responseJson = objectMapper.readTree(response.body());
        String generatedText = responseJson.get("response").asText();
        
        // 提取SQL语句
        String sql = extractSQL(generatedText);
        
        logger.info("SQL生成成功: {}", sql);
        return sql;
    }
    
    /**
     * 从生成的文本中提取SQL语句
     */
    private String extractSQL(String generatedText) {
        if (generatedText == null || generatedText.trim().isEmpty()) {
            return "";
        }
        
        String text = generatedText.trim();
        
        // 移除可能的markdown代码块标记
        if (text.startsWith("```sql")) {
            text = text.substring(6);
        } else if (text.startsWith("```")) {
            text = text.substring(3);
        }
        
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        
        // 清理文本，只保留SQL语句
        text = text.trim();
        
        // 如果包含多行，尝试找到SQL语句
        String[] lines = text.split("\n");
        StringBuilder sqlBuilder = new StringBuilder();
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // 跳过注释行
            if (line.startsWith("--") || line.startsWith("#") || line.startsWith("/*")) {
                continue;
            }
            
            // 如果是SQL关键字开头，开始收集SQL语句
            if (line.toUpperCase().startsWith("SELECT") || 
                line.toUpperCase().startsWith("INSERT") ||
                line.toUpperCase().startsWith("UPDATE") ||
                line.toUpperCase().startsWith("DELETE") ||
                line.toUpperCase().startsWith("WITH") ||
                sqlBuilder.length() > 0) {
                
                sqlBuilder.append(line).append(" ");
                
                // 如果以分号结尾，SQL语句结束
                if (line.endsWith(";")) {
                    break;
                }
            }
        }
        
        String sql = sqlBuilder.toString().trim();
        
        // 如果没有找到SQL语句，返回原始文本
        if (sql.isEmpty()) {
            sql = text;
        }
        
        // 确保SQL语句以分号结尾
        if (!sql.endsWith(";")) {
            sql += ";";
        }
        
        return sql;
    }
    
    /**
     * 测试Ollama连接
     */
    public boolean testConnection() {
        try {
            // 发送简单的测试请求
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", DatabaseConfig.OLLAMA_MODEL);
            requestBody.put("prompt", "Hello");
            requestBody.put("stream", false);
            requestBody.put("options", Map.of("max_tokens", 10));
            
            String requestJson = objectMapper.writeValueAsString(requestBody);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DatabaseConfig.OLLAMA_GENERATE_URL))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            boolean success = response.statusCode() == 200;
            if (success) {
                logger.info("Ollama连接测试成功");
            } else {
                logger.warn("Ollama连接测试失败，状态码: {}", response.statusCode());
            }
            
            return success;
            
        } catch (Exception e) {
            logger.error("Ollama连接测试失败", e);
            return false;
        }
    }
    
    /**
     * 关闭HTTP客户端
     */
    public void close() {
        // HttpClient会自动管理连接池，不需要显式关闭
        logger.info("OllamaService已关闭");
    }
}
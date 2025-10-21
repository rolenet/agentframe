package com.agentcore.examples.aidb;

/**
 * 数据库配置类
 * 包含数据库连接和Ollama大模型的配置信息
 */
public class DatabaseConfig {
    
    // 数据库配置
    public static final String DB_HOST = "127.0.0.1";
    public static final String DB_PORT = "3306";
    public static final String DB_NAME = "gis-pro";
    public static final String DB_USERNAME = "admin";
    public static final String DB_PASSWORD = "admin@1234";
    public static final String DB_URL = String.format("jdbc:mysql://%s:%s/%s?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai", 
            DB_HOST, DB_PORT, DB_NAME);
    
    // Ollama大模型配置
    public static final String OLLAMA_HOST = "10.3.16.68";
    public static final String OLLAMA_PORT = "11434";
    public static final String OLLAMA_MODEL = "deepseek-r1:7b";
    public static final String OLLAMA_BASE_URL = String.format("http://%s:%s", OLLAMA_HOST, OLLAMA_PORT);
    public static final String OLLAMA_GENERATE_URL = OLLAMA_BASE_URL + "/api/generate";
    
    // SQL生成提示词模板
    public static final String SQL_GENERATION_PROMPT_TEMPLATE = """
            你是一个专业的SQL查询生成助手。请根据用户的自然语言问题和数据库元数据信息，生成准确的SQL查询语句。
            
            数据库信息：
            - 数据库名称：%s
            - 可用表和字段：
            %s
            
            用户问题：%s
            
            请生成对应的SQL查询语句，要求：
            1. 只返回SQL语句，不要包含任何解释或其他文本
            2. SQL语句必须是可执行的
            3. 使用标准的MySQL语法
            4. 如果需要限制结果数量，默认使用LIMIT 100
            5. 字段名和表名请使用反引号包围
            
            SQL语句：
            """;
    
    // 连接超时配置
    public static final int CONNECTION_TIMEOUT = 30000; // 30秒
    public static final int READ_TIMEOUT = 60000; // 60秒
    
    // 查询结果限制
    public static final int MAX_RESULT_ROWS = 1000;
    public static final int DEFAULT_LIMIT = 100;
}
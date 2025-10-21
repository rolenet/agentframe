package com.agentcore.examples.aidb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

/**
 * 数据库查询服务
 * 负责执行SQL查询并返回结果
 */
public class DatabaseQueryService {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseQueryService.class);
    
    private final DatabaseMetadataService metadataService;
    
    public DatabaseQueryService(DatabaseMetadataService metadataService) {
        this.metadataService = metadataService;
    }
    
    /**
     * 执行SQL查询
     * 
     * @param sql SQL查询语句
     * @return 查询结果
     */
    public QueryResult executeQuery(String sql) {
        logger.info("执行SQL查询: {}", sql);
        
        QueryResult result = new QueryResult();
        result.setSql(sql);
        result.setExecutionTime(System.currentTimeMillis());
        
        try (Statement statement = metadataService.getConnection().createStatement()) {
            // 设置查询超时
            statement.setQueryTimeout(30);
            
            // 限制结果集大小
            statement.setMaxRows(DatabaseConfig.MAX_RESULT_ROWS);
            
            long startTime = System.currentTimeMillis();
            
            // 执行查询
            try (ResultSet resultSet = statement.executeQuery(sql)) {
                long endTime = System.currentTimeMillis();
                result.setExecutionDuration(endTime - startTime);
                
                // 获取结果集元数据
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                // 设置列信息
                List<String> columnNames = new ArrayList<>();
                List<String> columnTypes = new ArrayList<>();
                
                for (int i = 1; i <= columnCount; i++) {
                    columnNames.add(metaData.getColumnName(i));
                    columnTypes.add(metaData.getColumnTypeName(i));
                }
                
                result.setColumnNames(columnNames);
                result.setColumnTypes(columnTypes);
                
                // 读取数据行
                List<List<Object>> rows = new ArrayList<>();
                int rowCount = 0;
                
                while (resultSet.next() && rowCount < DatabaseConfig.MAX_RESULT_ROWS) {
                    List<Object> row = new ArrayList<>();
                    
                    for (int i = 1; i <= columnCount; i++) {
                        Object value = resultSet.getObject(i);
                        row.add(value);
                    }
                    
                    rows.add(row);
                    rowCount++;
                }
                
                result.setRows(rows);
                result.setRowCount(rowCount);
                result.setSuccess(true);
                
                logger.info("查询执行成功，返回{}行数据，耗时{}ms", rowCount, result.getExecutionDuration());
                
            }
            
        } catch (SQLException e) {
            logger.error("SQL查询执行失败: {}", e.getMessage());
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            result.setExecutionDuration(System.currentTimeMillis() - result.getExecutionTime());
        }
        
        return result;
    }
    
    /**
     * 验证SQL语句的安全性
     * 
     * @param sql SQL语句
     * @return 是否安全
     */
    public boolean validateSQL(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }
        
        String upperSQL = sql.toUpperCase().trim();
        
        // 只允许SELECT查询
        if (!upperSQL.startsWith("SELECT")) {
            logger.warn("不安全的SQL语句，只允许SELECT查询: {}", sql);
            return false;
        }
        
        // 检查危险关键字
        String[] dangerousKeywords = {
            "DROP", "DELETE", "INSERT", "UPDATE", "ALTER", "CREATE", 
            "TRUNCATE", "EXEC", "EXECUTE", "CALL", "PROCEDURE"
        };
        
        for (String keyword : dangerousKeywords) {
            if (upperSQL.contains(keyword)) {
                logger.warn("SQL语句包含危险关键字 {}: {}", keyword, sql);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 格式化查询结果为可读字符串
     * 
     * @param result 查询结果
     * @return 格式化的字符串
     */
    public String formatResult(QueryResult result) {
        if (!result.isSuccess()) {
            return String.format("查询执行失败: %s", result.getErrorMessage());
        }
        
        if (result.getRowCount() == 0) {
            return "查询成功，但没有返回数据";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("查询成功，返回 %d 行数据，耗时 %d ms\n\n", 
                result.getRowCount(), result.getExecutionDuration()));
        
        // 表头
        List<String> columnNames = result.getColumnNames();
        List<String> columnTypes = result.getColumnTypes();
        
        sb.append("列信息:\n");
        for (int i = 0; i < columnNames.size(); i++) {
            sb.append(String.format("  %d. %s (%s)\n", 
                    i + 1, columnNames.get(i), columnTypes.get(i)));
        }
        sb.append("\n");
        
        // 数据行（限制显示前20行）
        List<List<Object>> rows = result.getRows();
        int displayRows = Math.min(rows.size(), 20);
        
        sb.append("数据内容:\n");
        
        // 计算列宽
        int[] columnWidths = new int[columnNames.size()];
        for (int i = 0; i < columnNames.size(); i++) {
            columnWidths[i] = Math.max(columnNames.get(i).length(), 10);
        }
        
        // 更新列宽基于数据内容
        for (int i = 0; i < displayRows; i++) {
            List<Object> row = rows.get(i);
            for (int j = 0; j < row.size() && j < columnWidths.length; j++) {
                String value = row.get(j) != null ? row.get(j).toString() : "NULL";
                columnWidths[j] = Math.max(columnWidths[j], Math.min(value.length(), 30));
            }
        }
        
        // 打印表头
        sb.append("|");
        for (int i = 0; i < columnNames.size(); i++) {
            sb.append(String.format(" %-" + columnWidths[i] + "s |", columnNames.get(i)));
        }
        sb.append("\n");
        
        // 打印分隔线
        sb.append("|");
        for (int i = 0; i < columnNames.size(); i++) {
            sb.append("-".repeat(columnWidths[i] + 2)).append("|");
        }
        sb.append("\n");
        
        // 打印数据行
        for (int i = 0; i < displayRows; i++) {
            List<Object> row = rows.get(i);
            sb.append("|");
            
            for (int j = 0; j < columnNames.size(); j++) {
                String value = "NULL";
                if (j < row.size() && row.get(j) != null) {
                    value = row.get(j).toString();
                    if (value.length() > 30) {
                        value = value.substring(0, 27) + "...";
                    }
                }
                sb.append(String.format(" %-" + columnWidths[j] + "s |", value));
            }
            sb.append("\n");
        }
        
        if (rows.size() > displayRows) {
            sb.append(String.format("\n... 还有 %d 行数据未显示\n", rows.size() - displayRows));
        }
        
        return sb.toString();
    }
    
    /**
     * 查询结果类
     */
    public static class QueryResult {
        private String sql;
        private boolean success;
        private String errorMessage;
        private long executionTime;
        private long executionDuration;
        private List<String> columnNames;
        private List<String> columnTypes;
        private List<List<Object>> rows;
        private int rowCount;
        
        // Getters and Setters
        public String getSql() {
            return sql;
        }
        
        public void setSql(String sql) {
            this.sql = sql;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public void setSuccess(boolean success) {
            this.success = success;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
        
        public long getExecutionTime() {
            return executionTime;
        }
        
        public void setExecutionTime(long executionTime) {
            this.executionTime = executionTime;
        }
        
        public long getExecutionDuration() {
            return executionDuration;
        }
        
        public void setExecutionDuration(long executionDuration) {
            this.executionDuration = executionDuration;
        }
        
        public List<String> getColumnNames() {
            return columnNames;
        }
        
        public void setColumnNames(List<String> columnNames) {
            this.columnNames = columnNames;
        }
        
        public List<String> getColumnTypes() {
            return columnTypes;
        }
        
        public void setColumnTypes(List<String> columnTypes) {
            this.columnTypes = columnTypes;
        }
        
        public List<List<Object>> getRows() {
            return rows;
        }
        
        public void setRows(List<List<Object>> rows) {
            this.rows = rows;
        }
        
        public int getRowCount() {
            return rowCount;
        }
        
        public void setRowCount(int rowCount) {
            this.rowCount = rowCount;
        }
    }
}
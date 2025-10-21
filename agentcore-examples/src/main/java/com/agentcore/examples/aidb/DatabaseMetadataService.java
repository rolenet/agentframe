package com.agentcore.examples.aidb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

/**
 * 数据库元数据服务
 * 负责获取数据库的表结构和字段信息
 */
public class DatabaseMetadataService {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseMetadataService.class);
    
    private Connection connection;
    private Map<String, List<ColumnInfo>> tableMetadata;
    
    public DatabaseMetadataService() {
        this.tableMetadata = new HashMap<>();
    }
    
    /**
     * 初始化数据库连接
     */
    public void initialize() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(
                    DatabaseConfig.DB_URL,
                    DatabaseConfig.DB_USERNAME,
                    DatabaseConfig.DB_PASSWORD
            );
            logger.info("数据库连接初始化成功: {}", DatabaseConfig.DB_URL);
            
            // 加载数据库元数据
            loadDatabaseMetadata();
            
        } catch (ClassNotFoundException e) {
            logger.error("MySQL驱动未找到", e);
            throw new SQLException("MySQL驱动未找到", e);
        }
    }
    
    /**
     * 加载数据库元数据
     */
    private void loadDatabaseMetadata() throws SQLException {
        logger.info("开始加载数据库元数据...");
        
        DatabaseMetaData metaData = connection.getMetaData();
        
        // 获取所有表
        try (ResultSet tables = metaData.getTables(DatabaseConfig.DB_NAME, null, null, new String[]{"TABLE"})) {
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                List<ColumnInfo> columns = getTableColumns(tableName, metaData);
                tableMetadata.put(tableName, columns);
                
                logger.debug("加载表元数据: {} ({}个字段)", tableName, columns.size());
            }
        }
        
        logger.info("数据库元数据加载完成，共{}个表", tableMetadata.size());
    }
    
    /**
     * 获取表的字段信息
     */
    private List<ColumnInfo> getTableColumns(String tableName, DatabaseMetaData metaData) throws SQLException {
        List<ColumnInfo> columns = new ArrayList<>();
        
        try (ResultSet columnsRs = metaData.getColumns(DatabaseConfig.DB_NAME, null, tableName, null)) {
            while (columnsRs.next()) {
                ColumnInfo column = new ColumnInfo();
                column.setColumnName(columnsRs.getString("COLUMN_NAME"));
                column.setDataType(columnsRs.getString("TYPE_NAME"));
                column.setColumnSize(columnsRs.getInt("COLUMN_SIZE"));
                column.setNullable(columnsRs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                column.setDefaultValue(columnsRs.getString("COLUMN_DEF"));
                column.setRemarks(columnsRs.getString("REMARKS"));
                
                columns.add(column);
            }
        }
        
        return columns;
    }
    
    /**
     * 获取所有表名
     */
    public Set<String> getAllTableNames() {
        return new HashSet<>(tableMetadata.keySet());
    }
    
    /**
     * 获取指定表的字段信息
     */
    public List<ColumnInfo> getTableColumns(String tableName) {
        return tableMetadata.getOrDefault(tableName, new ArrayList<>());
    }
    
    /**
     * 获取格式化的元数据信息，用于生成SQL提示词
     */
    public String getFormattedMetadata() {
        StringBuilder sb = new StringBuilder();
        
        for (Map.Entry<String, List<ColumnInfo>> entry : tableMetadata.entrySet()) {
            String tableName = entry.getKey();
            List<ColumnInfo> columns = entry.getValue();
            
            sb.append("表名: ").append(tableName).append("\n");
            sb.append("字段:\n");
            
            for (ColumnInfo column : columns) {
                sb.append("  - ").append(column.getColumnName())
                  .append(" (").append(column.getDataType()).append(")");
                
                if (column.getRemarks() != null && !column.getRemarks().trim().isEmpty()) {
                    sb.append(" - ").append(column.getRemarks());
                }
                
                sb.append("\n");
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 获取指定表的格式化元数据
     */
    public String getFormattedMetadata(String tableName) {
        List<ColumnInfo> columns = tableMetadata.get(tableName);
        if (columns == null || columns.isEmpty()) {
            return "表 " + tableName + " 不存在或无字段信息";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("表名: ").append(tableName).append("\n");
        sb.append("字段:\n");
        
        for (ColumnInfo column : columns) {
            sb.append("  - ").append(column.getColumnName())
              .append(" (").append(column.getDataType()).append(")");
            
            if (column.getRemarks() != null && !column.getRemarks().trim().isEmpty()) {
                sb.append(" - ").append(column.getRemarks());
            }
            
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 根据关键词搜索相关表
     */
    public List<String> searchRelevantTables(String keyword) {
        List<String> relevantTables = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();
        
        for (String tableName : tableMetadata.keySet()) {
            // 检查表名是否包含关键词
            if (tableName.toLowerCase().contains(lowerKeyword)) {
                relevantTables.add(tableName);
                continue;
            }
            
            // 检查字段名或注释是否包含关键词
            List<ColumnInfo> columns = tableMetadata.get(tableName);
            for (ColumnInfo column : columns) {
                if (column.getColumnName().toLowerCase().contains(lowerKeyword) ||
                    (column.getRemarks() != null && column.getRemarks().toLowerCase().contains(lowerKeyword))) {
                    relevantTables.add(tableName);
                    break;
                }
            }
        }
        
        return relevantTables;
    }
    
    /**
     * 获取数据库连接
     */
    public Connection getConnection() {
        return connection;
    }
    
    /**
     * 关闭数据库连接
     */
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                logger.info("数据库连接已关闭");
            } catch (SQLException e) {
                logger.error("关闭数据库连接时发生错误", e);
            }
        }
    }
    
    /**
     * 字段信息类
     */
    public static class ColumnInfo {
        private String columnName;
        private String dataType;
        private int columnSize;
        private boolean nullable;
        private String defaultValue;
        private String remarks;
        
        // Getters and Setters
        public String getColumnName() {
            return columnName;
        }
        
        public void setColumnName(String columnName) {
            this.columnName = columnName;
        }
        
        public String getDataType() {
            return dataType;
        }
        
        public void setDataType(String dataType) {
            this.dataType = dataType;
        }
        
        public int getColumnSize() {
            return columnSize;
        }
        
        public void setColumnSize(int columnSize) {
            this.columnSize = columnSize;
        }
        
        public boolean isNullable() {
            return nullable;
        }
        
        public void setNullable(boolean nullable) {
            this.nullable = nullable;
        }
        
        public String getDefaultValue() {
            return defaultValue;
        }
        
        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }
        
        public String getRemarks() {
            return remarks;
        }
        
        public void setRemarks(String remarks) {
            this.remarks = remarks;
        }
        
        @Override
        public String toString() {
            return String.format("ColumnInfo{name='%s', type='%s', size=%d, nullable=%s, remarks='%s'}", 
                    columnName, dataType, columnSize, nullable, remarks);
        }
    }
}
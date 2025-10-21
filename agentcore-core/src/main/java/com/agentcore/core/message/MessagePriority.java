package com.agentcore.core.message;

/**
 * 消息优先级枚举
 * 定义消息的处理优先级
 * 
 * @author AgentCore Team
 */
public enum MessagePriority {
    
    /**
     * 最低优先级
     */
    LOWEST(1, "最低优先级"),
    
    /**
     * 低优先级
     */
    LOW(3, "低优先级"),
    
    /**
     * 普通优先级（默认）
     */
    NORMAL(5, "普通优先级"),
    
    /**
     * 高优先级
     */
    HIGH(7, "高优先级"),
    
    /**
     * 最高优先级
     */
    HIGHEST(10, "最高优先级"),
    
    /**
     * 紧急优先级（系统消息）
     */
    URGENT(15, "紧急优先级");

    private final int level;
    private final String description;

    MessagePriority(int level, String description) {
        this.level = level;
        this.description = description;
    }

    public int getLevel() {
        return level;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 比较优先级
     * 
     * @param other 另一个优先级
     * @return 如果当前优先级更高返回正数，相等返回0，更低返回负数
     */
    public int compareLevel(MessagePriority other) {
        return Integer.compare(this.level, other.level);
    }

    /**
     * 检查是否比另一个优先级高
     * 
     * @param other 另一个优先级
     * @return 如果更高返回true
     */
    public boolean isHigherThan(MessagePriority other) {
        return this.level > other.level;
    }

    /**
     * 检查是否比另一个优先级低
     * 
     * @param other 另一个优先级
     * @return 如果更低返回true
     */
    public boolean isLowerThan(MessagePriority other) {
        return this.level < other.level;
    }

    /**
     * 检查是否为系统级优先级
     * 
     * @return 如果是系统级优先级返回true
     */
    public boolean isSystemLevel() {
        return this == URGENT;
    }

    /**
     * 从级别获取优先级
     * 
     * @param level 优先级级别
     * @return MessagePriority实例
     */
    public static MessagePriority fromLevel(int level) {
        for (MessagePriority priority : values()) {
            if (priority.level == level) {
                return priority;
            }
        }
        
        // 如果没有精确匹配，返回最接近的优先级
        MessagePriority closest = NORMAL;
        int minDiff = Math.abs(level - NORMAL.level);
        
        for (MessagePriority priority : values()) {
            int diff = Math.abs(level - priority.level);
            if (diff < minDiff) {
                minDiff = diff;
                closest = priority;
            }
        }
        
        return closest;
    }

    @Override
    public String toString() {
        return String.format("%s(level=%d)", name(), level);
    }
}
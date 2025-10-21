package com.agentcore.core.behavior;

/**
 * 行为类型枚举
 * 定义不同类型的行为执行模式
 * 
 * @author AgentCore Team
 */
public enum BehaviorType {
    
    /**
     * 一次行为 - 执行一次后完成
     */
    ONE_SHOT("one-shot", "一次性行为"),
    
    /**
     * 循环行为 - 持续循环执行直到被停止
     */
    CYCLIC("cyclic", "循环行为"),
    
    /**
     * 定时行为 - 按固定时间间隔执行
     */
    TICKER("ticker", "定时行为"),
    
    /**
     * 事件驱动行为 - 由特定事件触发执行
     */
    EVENT_DRIVEN("event-driven", "事件驱动行为"),
    
    /**
     * 条件行为 - 满足特定条件时执行
     */
    CONDITIONAL("conditional", "条件行为"),
    
    /**
     * 复合行为 - 包含多个子行为的复合行为
     */
    COMPOSITE("composite", "复合行为"),
    
    /**
     * 顺序行为 - 按顺序执行多个子行为
     */
    SEQUENTIAL("sequential", "顺序行为"),
    
    /**
     * 并行行为 - 并行执行多个子行为
     */
    PARALLEL("parallel", "并行行为");

    private final String code;
    private final String description;

    BehaviorType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 检查是否为周期性行为类型
     * 
     * @return 如果是周期性行为返回true
     */
    public boolean isPeriodic() {
        return this == CYCLIC || this == TICKER;
    }

    /**
     * 检查是否为一次行为类型
     * 
     * @return 如果是一次行为返回true
     */
    public boolean isOneTime() {
        return this == ONE_SHOT;
    }

    /**
     * 检查是否为复合行为类型
     * 
     * @return 如果是复合行为返回true
     */
    public boolean isComposite() {
        return this == COMPOSITE || this == SEQUENTIAL || this == PARALLEL;
    }

    /**
     * 检查是否为事件相关行为类型
     * 
     * @return 如果是事件相关行为返回true
     */
    public boolean isEventBased() {
        return this == EVENT_DRIVEN || this == CONDITIONAL;
    }

    /**
     * 从代码获取行为类型
     * 
     * @param code 行为类型代码
     * @return BehaviorType实例
     */
    public static BehaviorType fromCode(String code) {
        for (BehaviorType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown behavior type code: " + code);
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", code, description);
    }
}
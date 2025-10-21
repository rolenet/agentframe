package com.agentcore.core.behavior;

/**
 * 行为状态枚举
 * 定义行为的生命周期状态
 * 
 * @author AgentCore Team
 */
public enum BehaviorState {
    
    /**
     * 初始化状态 - 行为刚创建，尚未开始执行
     */
    READY("ready", "行为已就绪"),
    
    /**
     * 运行状态 - 行为正在执行中
     */
    RUNNING("running", "行为执行中"),
    
    /**
     * 等待状态 - 行为暂时等待某个条件
     */
    WAITING("waiting", "行为等待中"),
    
    /**
     * 暂停状态 - 行为被暂停执行
     */
    SUSPENDED("suspended", "行为已暂停"),
    
    /**
     * 完成状态 - 行为执行完成
     */
    DONE("done", "行为已完成"),
    
    /**
     * 错误状态 - 行为执行出现错误
     */
    ERROR("error", "行为执行错误"),
    
    /**
     * 已取消状态 - 行为被取消执行
     */
    CANCELLED("cancelled", "行为已取消");

    private final String code;
    private final String description;

    BehaviorState(String code, String description) {
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
     * 检查是否可以转换到目标状态
     * 
     * @param targetState 目标状态
     * @return 如果可以转换返回true
     */
    public boolean canTransitionTo(BehaviorState targetState) {
        return switch (this) {
            case READY -> targetState == RUNNING || targetState == SUSPENDED || targetState == CANCELLED;
            case RUNNING -> targetState == WAITING || targetState == DONE || targetState == ERROR || 
                           targetState == SUSPENDED || targetState == CANCELLED;
            case WAITING -> targetState == RUNNING || targetState == DONE || targetState == ERROR || 
                           targetState == SUSPENDED || targetState == CANCELLED;
            case SUSPENDED -> targetState == READY || targetState == CANCELLED;
            case DONE -> targetState == READY; // 可以重置为就绪状态
            case ERROR -> targetState == READY || targetState == CANCELLED;
            case CANCELLED -> targetState == READY; // 可以重置为就绪状态
        };
    }

    /**
     * 检查是否为活跃状态（可以执行）
     * 
     * @return 如果是活跃状态返回true
     */
    public boolean isActive() {
        return this == READY || this == RUNNING || this == WAITING;
    }

    /**
     * 检查是否为终止状态
     * 
     * @return 如果是终止状态返回true
     */
    public boolean isTerminated() {
        return this == DONE || this == ERROR || this == CANCELLED;
    }

    /**
     * 检查是否为错误状态
     * 
     * @return 如果是错误状态返回true
     */
    public boolean isError() {
        return this == ERROR;
    }

    /**
     * 检查是否可以执行
     * 
     * @return 如果可以执行返回true
     */
    public boolean isRunnable() {
        return this == READY || this == WAITING;
    }

    /**
     * 从代码获取状态
     * 
     * @param code 状态代码
     * @return BehaviorState实例
     */
    public static BehaviorState fromCode(String code) {
        for (BehaviorState state : values()) {
            if (state.code.equals(code)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown behavior state code: " + code);
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", name(), description);
    }
}
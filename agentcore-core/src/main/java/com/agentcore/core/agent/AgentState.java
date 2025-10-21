package com.agentcore.core.agent;

/**
 * Agent状态枚举
 * 定义Agent的生命周期状态
 * 
 * @author AgentCore Team
 */
public enum AgentState {
    
    /**
     * 初始化状态 - Agent刚创建，尚未启动
     */
    INITIATED("initiated", "Agent已创建但尚未启动"),
    
    /**
     * 启动状态 - Agent正在启动过程中
     */
    STARTING("starting", "Agent正在启动"),
    
    /**
     * 运行状态 - Agent正常运行中
     */
    ACTIVE("active", "Agent正常运行中"),
    
    /**
     * 暂停状态 - Agent已暂停，可以恢复
     */
    SUSPENDED("suspended", "Agent已暂停"),
    
    /**
     * 停止状态 - Agent正在停止过程中
     */
    STOPPING("stopping", "Agent正在停止"),
    
    /**
     * 已停止状态 - Agent已完全停止
     */
    STOPPED("stopped", "Agent已停止"),
    
    /**
     * 销毁状态 - Agent已被销毁，无法恢复
     */
    DESTROYED("destroyed", "Agent已被销毁"),
    
    /**
     * 错误状态 - Agent运行出现错误
     */
    ERROR("error", "Agent运行出现错误");

    private final String code;
    private final String description;

    AgentState(String code, String description) {
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
    public boolean canTransitionTo(AgentState targetState) {
        return switch (this) {
            case INITIATED -> targetState == STARTING || targetState == DESTROYED;
            case STARTING -> targetState == ACTIVE || targetState == ERROR || targetState == STOPPING;
            case ACTIVE -> targetState == SUSPENDED || targetState == STOPPING || targetState == ERROR;
            case SUSPENDED -> targetState == ACTIVE || targetState == STOPPING || targetState == ERROR;
            case STOPPING -> targetState == STOPPED || targetState == ERROR;
            case STOPPED -> targetState == STARTING || targetState == DESTROYED;
            case ERROR -> targetState == STOPPING || targetState == DESTROYED;
            case DESTROYED -> false; // 销毁状态不能转换到任何其他状态
        };
    }

    /**
     * 检查是否为活跃状态（可以处理消息和执行行为）
     * 
     * @return 如果是活跃状态返回true
     */
    public boolean isActive() {
        return this == ACTIVE;
    }

    /**
     * 检查是否为终止状态
     * 
     * @return 如果是终止状态返回true
     */
    public boolean isTerminated() {
        return this == STOPPED || this == DESTROYED;
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
     * 从代码获取状态
     * 
     * @param code 状态代码
     * @return AgentState实例
     */
    public static AgentState fromCode(String code) {
        for (AgentState state : values()) {
            if (state.code.equals(code)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown agent state code: " + code);
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", name(), description);
    }
}
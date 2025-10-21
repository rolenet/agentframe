package com.agentcore.core.behavior;

import com.agentcore.core.agent.Agent;

/**
 * 行为接口
 * 定义Agent行为的基本契约
 * 
 * @author AgentCore Team
 */
public interface Behavior {

    /**
     * 获取行为ID
     * 
     * @return 行为的唯一标识符
     */
    String getBehaviorId();

    /**
     * 获取行为名称
     * 
     * @return 行为名称
     */
    String getName();

    /**
     * 获取行为优先级
     * 
     * @return 行为优先级
     */
    BehaviorPriority getPriority();

    /**
     * 获取拥有此行为的Agent
     * 
     * @return Agent实例
     */
    Agent getAgent();

    /**
     * 设置拥有此行为的Agent
     * 
     * @param agent Agent实例
     */
    void setAgent(Agent agent);

    /**
     * 检查行为是否可以执行
     * 
     * @return 如果可以执行返回true
     */
    boolean isRunnable();

    /**
     * 检查行为是否已完成
     * 
     * @return 如果已完成返回true
     */
    boolean isDone();

    /**
     * 执行行为
     * 这是行为的核心逻辑，由具体的行为实现类提供
     */
    void action();

    /**
     * 重置行为状态
     * 将行为重置为初始状态，可以重新执行
     */
    void reset();

    /**
     * 获取行为状态
     * 
     * @return 行为当前状态
     */
    BehaviorState getState();

    /**
     * 行为启动时调用
     * 在行为第一次执行前调用，用于初始化
     */
    default void onStart() {
        // 默认空实现
    }

    /**
     * 行为完成时调用
     * 在行为执行完成后调用，用于清理资源
     */
    default void onDone() {
        // 默认空实现
    }

    /**
     * 行为异常时调用
     * 在行为执行过程中发生异常时调用
     * 
     * @param throwable 异常信息
     */
    default void onException(Throwable throwable) {
        // 默认空实现
    }

    /**
     * 获取行为描述
     * 
     * @return 行为描述信息
     */
    default String getDescription() {
        return getName();
    }

    /**
     * 获取行为类型
     * 
     * @return 行为类型
     */
    default BehaviorType getType() {
        return BehaviorType.ONE_SHOT;
    }

    /**
     * 检查行为是否为周期性行为
     * 
     * @return 如果是周期性行为返回true
     */
    default boolean isPeriodic() {
        return getType() == BehaviorType.CYCLIC || getType() == BehaviorType.TICKER;
    }

    /**
     * 检查行为是否为一次性行为
     * 
     * @return 如果是一次性行为返回true
     */
    default boolean isOneShot() {
        return getType() == BehaviorType.ONE_SHOT;
    }
}
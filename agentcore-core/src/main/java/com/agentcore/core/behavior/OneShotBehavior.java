package com.agentcore.core.behavior;

/**
 * 一次行为
 * 执行一次后自动完成的行为
 * 
 * @author AgentCore Team
 */
public abstract class OneShotBehavior extends AbstractBehavior {

    /**
     * 构造函数
     * 
     * @param name 行为名称
     */
    protected OneShotBehavior(String name) {
        super(name);
    }

    /**
     * 构造函数
     * 
     * @param name 行为名称
     * @param priority 行为优先级
     */
    protected OneShotBehavior(String name, BehaviorPriority priority) {
        super(name, priority);
    }

    @Override
    public BehaviorType getType() {
        return BehaviorType.ONE_SHOT;
    }

    @Override
    protected boolean shouldComplete() {
        return true; // 一次行为执行后总是完成
    }

    @Override
    public String getDescription() {
        return String.format("一次行为: %s", getName());
    }
}
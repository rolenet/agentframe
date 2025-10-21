package com.agentcore.core.behavior;

/**
 * 循环行为
 * 持续循环执行直到被明确停止的行为
 * 
 * @author AgentCore Team
 */
public abstract class CyclicBehavior extends AbstractBehavior {

    private volatile boolean shouldStop = false;

    /**
     * 构造函数
     * 
     * @param name 行为名称
     */
    protected CyclicBehavior(String name) {
        super(name);
    }

    /**
     * 构造函数
     * 
     * @param name 行为名称
     * @param priority 行为优先级
     */
    protected CyclicBehavior(String name, BehaviorPriority priority) {
        super(name, priority);
    }

    @Override
    public BehaviorType getType() {
        return BehaviorType.CYCLIC;
    }

    @Override
    protected boolean shouldComplete() {
        return shouldStop;
    }

    @Override
    protected void doReset() {
        super.doReset();
        shouldStop = false;
    }

    /**
     * 停止循环行为
     */
    public void stopCyclic() {
        shouldStop = true;
    }

    /**
     * 检查是否应该停止
     * 
     * @return 如果应该停止返回true
     */
    protected boolean shouldStopCyclic() {
        return shouldStop;
    }

    @Override
    public String getDescription() {
        return String.format("循环行为: %s", getName());
    }
}
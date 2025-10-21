package com.agentcore.core.behavior;

/**
 * 定时行为
 * 按固定时间间隔执行的行为
 * 
 * @author AgentCore Team
 */
public abstract class TickerBehavior extends AbstractBehavior {

    private final long intervalMs;
    private volatile long lastExecutionTime = 0;
    private volatile boolean shouldStop = false;

    /**
     * 构造函数
     * 
     * @param name 行为名称
     * @param intervalMs 执行间隔（毫秒）
     */
    protected TickerBehavior(String name, long intervalMs) {
        super(name);
        this.intervalMs = intervalMs;
    }

    /**
     * 构造函数
     * 
     * @param name 行为名称
     * @param intervalMs 执行间隔（毫秒）
     * @param priority 行为优先级
     */
    protected TickerBehavior(String name, long intervalMs, BehaviorPriority priority) {
        super(name, priority);
        this.intervalMs = intervalMs;
    }

    @Override
    public BehaviorType getType() {
        return BehaviorType.TICKER;
    }

    @Override
    protected boolean canRun() {
        long currentTime = System.currentTimeMillis();
        return super.canRun() && (currentTime - lastExecutionTime >= intervalMs);
    }

    @Override
    protected final void doAction() {
        lastExecutionTime = System.currentTimeMillis();
        onTick();
    }

    @Override
    protected boolean shouldComplete() {
        return shouldStop;
    }

    @Override
    protected void doReset() {
        super.doReset();
        lastExecutionTime = 0;
        shouldStop = false;
    }

    /**
     * 定时执行的具体逻辑
     * 子类必须实现此方法
     */
    protected abstract void onTick();

    /**
     * 停止定时行为
     */
    public void stopTicker() {
        shouldStop = true;
    }

    /**
     * 获取执行间隔
     * 
     * @return 执行间隔（毫秒）
     */
    public long getIntervalMs() {
        return intervalMs;
    }

    /**
     * 获取上次执行时间
     * 
     * @return 上次执行时间戳
     */
    public long getLastExecutionTime() {
        return lastExecutionTime;
    }

    /**
     * 获取距离下次执行的剩余时间
     * 
     * @return 剩余时间（毫秒）
     */
    public long getTimeToNextExecution() {
        long elapsed = System.currentTimeMillis() - lastExecutionTime;
        return Math.max(0, intervalMs - elapsed);
    }

    @Override
    public String getDescription() {
        return String.format("定时行为: %s (间隔: %dms)", getName(), intervalMs);
    }
}
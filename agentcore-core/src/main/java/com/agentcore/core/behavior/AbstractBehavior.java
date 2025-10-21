package com.agentcore.core.behavior;

import com.agentcore.core.agent.Agent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 抽象行为基类
 * 提供行为的基本实现和生命周期管理
 * 
 * @author AgentCore Team
 */
public abstract class AbstractBehavior implements Behavior {
    private static final Logger logger = LoggerFactory.getLogger(AbstractBehavior.class);

    protected final String behaviorId;
    protected final String name;
    protected final BehaviorPriority priority;
    protected final AtomicReference<BehaviorState> state;
    
    private volatile Agent agent;
    private volatile boolean started = false;

    /**
     * 构造函数
     * 
     * @param name 行为名称
     */
    protected AbstractBehavior(String name) {
        this(name, BehaviorPriority.NORMAL);
    }

    /**
     * 构造函数
     * 
     * @param name 行为名称
     * @param priority 行为优先级
     */
    protected AbstractBehavior(String name, BehaviorPriority priority) {
        this.behaviorId = UUID.randomUUID().toString();
        this.name = name;
        this.priority = priority;
        this.state = new AtomicReference<>(BehaviorState.READY);
    }

    @Override
    public String getBehaviorId() {
        return behaviorId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public BehaviorPriority getPriority() {
        return priority;
    }

    @Override
    public Agent getAgent() {
        return agent;
    }

    @Override
    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    @Override
    public BehaviorState getState() {
        return state.get();
    }

    @Override
    public boolean isRunnable() {
        return state.get().isRunnable() && canRun();
    }

    @Override
    public boolean isDone() {
        return state.get() == BehaviorState.DONE;
    }

    @Override
    public final void action() {
        BehaviorState currentState = state.get();
        
        if (!currentState.isRunnable()) {
            logger.info("Behavior {} is not runnable in state: {}", name, currentState);
            return;
        }

        // 首次启动时调用onStart
        if (!started) {
            try {
                onStart();
                started = true;
                logger.info("Behavior {} started", name);
            } catch (Exception e) {
                logger.error("Error starting behavior {}: {}", name, e.getMessage(), e);
                state.set(BehaviorState.ERROR);
                onException(e);
                return;
            }
        }

        // 设置为运行状态
        if (!state.compareAndSet(BehaviorState.READY, BehaviorState.RUNNING) &&
            !state.compareAndSet(BehaviorState.WAITING, BehaviorState.RUNNING)) {
                 logger.info("Behavior {} cannot transition to RUNNING from state: {}", name, currentState);
            return;
        }

        try {
            // 执行具体的行为逻辑
            doAction();
            
            // 检查行为是否完成
            if (shouldComplete()) {
                state.set(BehaviorState.DONE);
                onDone();
                 logger.info("Behavior {} completed", name);
            } else if (shouldWait()) {
                state.set(BehaviorState.WAITING);
                 logger.info("Behavior {} is waiting", name);
            } else {
                state.set(BehaviorState.READY);
            }
            
        } catch (Exception e) {
            logger.error("Error executing behavior {}: {}", name, e.getMessage(), e);
            state.set(BehaviorState.ERROR);
            onException(e);
        }
    }

    @Override
    public void reset() {
        BehaviorState currentState = state.get();
        if (currentState.isTerminated()) {
            state.set(BehaviorState.READY);
            started = false;
            doReset();
            logger.info("Behavior {} reset", name);
        } else {
            logger.info("Cannot reset behavior {} in state: {}", name, currentState);
        }
    }

    @Override
    public void onStart() {
        // 默认空实现，子类可以重写
    }

    @Override
    public void onDone() {
        // 默认空实现，子类可以重写
    }

    @Override
    public void onException(Throwable throwable) {
        logger.error("Exception in behavior {}: {}", name, throwable.getMessage(), throwable);
    }

    /**
     * 执行具体的行为逻辑
     * 子类必须实现此方法
     */
    protected abstract void doAction();

    /**
     * 检查是否可以运行
     * 子类可以重写此方法来提供额外的运行条件检查
     * 
     * @return 如果可以运行返回true
     */
    protected boolean canRun() {
        return true;
    }

    /**
     * 检查行为是否应该完成
     * 子类可以重写此方法来定义完成条件
     * 
     * @return 如果应该完成返回true
     */
    protected boolean shouldComplete() {
        return getType().isOneTime();
    }

    /**
     * 检查行为是否应该等待
     * 子类可以重写此方法来定义等待条件
     * 
     * @return 如果应该等待返回true
     */
    protected boolean shouldWait() {
        return false;
    }

    /**
     * 重置行为的具体逻辑
     * 子类可以重写此方法来执行特定的重置操作
     */
    protected void doReset() {
        // 默认空实现
    }

    /**
     * 暂停行为
     */
    public void suspend() {
        BehaviorState currentState = state.get();
        if (currentState.canTransitionTo(BehaviorState.SUSPENDED)) {
            state.set(BehaviorState.SUSPENDED);
            logger.info("Behavior {} suspended", name);
        } else {
            logger.info("Cannot suspend behavior {} in state: {}", name, currentState);
        }
    }

    /**
     * 恢复行为
     */
    public void resume() {
        if (state.compareAndSet(BehaviorState.SUSPENDED, BehaviorState.READY)) {
            logger.info("Behavior {} resumed", name);
        } else {
            logger.info("Cannot resume behavior {} from state: {}", name, state.get());
        }
    }

    /**
     * 取消行为
     */
    public void cancel() {
        BehaviorState currentState = state.get();
        if (currentState.canTransitionTo(BehaviorState.CANCELLED)) {
            state.set(BehaviorState.CANCELLED);
            onDone(); // 调用完成回调
            logger.info("Behavior {} cancelled", name);
        } else {
            logger.info("Cannot cancel behavior {} in state: {}", name, currentState);
        }
    }

    /**
     * 获取行为的运行时统计信息
     * 
     * @return 行为统计信息
     */
    public BehaviorStats getStats() {
        return new BehaviorStats(behaviorId, name, state.get(), started);
    }

    @Override
    public String toString() {
        return String.format("%s{id=%s, name='%s', state=%s, priority=%s}", 
            getClass().getSimpleName(), behaviorId, name, state.get(), priority);
    }

    /**
     * 行为统计信息记录类
     */
    public record BehaviorStats(
        String behaviorId,
        String name,
        BehaviorState state,
        boolean started
    ) {
        @Override
        public String toString() {
            return String.format("BehaviorStats{id=%s, name='%s', state=%s, started=%s}", 
                behaviorId, name, state, started);
        }
    }
}
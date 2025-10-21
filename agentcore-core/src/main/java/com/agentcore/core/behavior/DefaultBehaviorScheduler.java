package com.agentcore.core.behavior;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 默认行为调度器实现
 * 基于JDK17虚拟线程的高性能行为调度器
 * 
 * @author AgentCore Team
 */
public class DefaultBehaviorScheduler implements BehaviorScheduler {
    private static final Logger logger = LoggerFactory.getLogger(DefaultBehaviorScheduler.class);

    private final AtomicReference<SchedulerState> state;
    private final ConcurrentLinkedQueue<Behavior> behaviors;
    private final ScheduledExecutorService schedulerExecutor;
    private final ExecutorService behaviorExecutor;
    
    private volatile SchedulingStrategy strategy;
    private volatile ScheduledFuture<?> schedulingTask;
    
    // 统计信息
    private final AtomicLong totalExecutions = new AtomicLong(0);
    private final AtomicLong totalExecutionTime = new AtomicLong(0);
    private final AtomicLong completedBehaviors = new AtomicLong(0);
    private final AtomicLong errorBehaviors = new AtomicLong(0);

    /**
     * 构造函数
     */
    public DefaultBehaviorScheduler() {
        this.state = new AtomicReference<>(SchedulerState.STOPPED);
        this.behaviors = new ConcurrentLinkedQueue<>();
        this.strategy = SchedulingStrategy.getDefault();
        
        // 使用普通线程池执行器（兼容Java 17）
        this.schedulerExecutor = Executors.newScheduledThreadPool(1, 
            r -> new Thread(r, "BehaviorScheduler"));
        this.behaviorExecutor = Executors.newCachedThreadPool();
    }

    @Override
    public void start() {
        if (!state.compareAndSet(SchedulerState.STOPPED, SchedulerState.STARTING)) {
            throw new IllegalStateException("Scheduler can only be started from STOPPED state");
        }
        logger.info("Starting behavior scheduler");

        try {
            // 启动调度任务，每10ms检查一次
            schedulingTask = schedulerExecutor.scheduleWithFixedDelay(
                this::scheduleBehaviors, 0, 10, TimeUnit.MILLISECONDS);
            
            state.set(SchedulerState.RUNNING);
            logger.info("Behavior scheduler started successfully");
        } catch (Exception e) {
            state.set(SchedulerState.STOPPED);
            logger.error("Failed to start behavior scheduler: {}", e.getMessage(), e);
            throw new RuntimeException("Scheduler start failed", e);
        }
    }

    @Override
    public void stop() {
        SchedulerState currentState = state.get();
        if (currentState == SchedulerState.STOPPED || currentState == SchedulerState.DESTROYED) {
            return;
        }

        state.set(SchedulerState.STOPPING);
        logger.info("Stopping behavior scheduler");

        try {
            // 停止调度任务
            if (schedulingTask != null) {
                schedulingTask.cancel(false);
                schedulingTask = null;
            }

            // 等待正在执行的行为完成
            behaviorExecutor.shutdown();
            if (!behaviorExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                behaviorExecutor.shutdownNow();
            }

            state.set(SchedulerState.STOPPED);
            logger.info("Behavior scheduler stopped successfully");
        } catch (Exception e) {
            logger.error("Error stopping behavior scheduler: {}", e.getMessage(), e);
            throw new RuntimeException("Scheduler stop failed", e);
        }
    }

    @Override
    public void suspend() {
        if (!state.compareAndSet(SchedulerState.RUNNING, SchedulerState.SUSPENDED)) {
            throw new IllegalStateException("Scheduler can only be suspended from RUNNING state");
        }

        logger.info("Behavior scheduler suspended");
    }

    @Override
    public void resume() {
        if (!state.compareAndSet(SchedulerState.SUSPENDED, SchedulerState.RUNNING)) {
            throw new IllegalStateException("Scheduler can only be resumed from SUSPENDED state");
        }

        logger.info("Behavior scheduler resumed");
    }

    @Override
    public void destroy() {
        if (state.get() != SchedulerState.STOPPED) {
            stop();
        }

        logger.info("Destroying behavior scheduler");

        try {
            // 关闭执行器
            schedulerExecutor.shutdown();
            if (!schedulerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                schedulerExecutor.shutdownNow();
            }

            // 清理行为
            behaviors.clear();

            state.set(SchedulerState.DESTROYED);
            logger.info("Behavior scheduler destroyed successfully");
        } catch (Exception e) {
            logger.error("Error destroying behavior scheduler: {}", e.getMessage(), e);
            throw new RuntimeException("Scheduler destroy failed", e);
        }
    }

    @Override
    public void addBehavior(Behavior behavior) {
        if (behavior == null) {
            throw new IllegalArgumentException("Behavior cannot be null");
        }

        behaviors.offer(behavior);
        logger.info("Added behavior: {}", behavior.getName());
    }

    @Override
    public void removeBehavior(Behavior behavior) {
        if (behavior == null) {
            return;
        }

        behaviors.remove(behavior);
        logger.info("Removed behavior: {}", behavior.getName());
    }

    @Override
    public void removeAllBehaviors() {
        int count = behaviors.size();
        behaviors.clear();
        logger.info("Removed all {} behaviors", count);
    }

    @Override
    public List<Behavior> getBehaviors() {
        return List.copyOf(behaviors);
    }

    @Override
    public int getActiveBehaviorCount() {
        return (int) behaviors.stream()
            .filter(b -> b.getState().isActive())
            .count();
    }

    @Override
    public boolean isRunning() {
        return state.get() == SchedulerState.RUNNING;
    }

    @Override
    public boolean isSuspended() {
        return state.get() == SchedulerState.SUSPENDED;
    }

    @Override
    public SchedulerState getState() {
        return state.get();
    }

    @Override
    public void setSchedulingStrategy(SchedulingStrategy strategy) {
        this.strategy = strategy != null ? strategy : SchedulingStrategy.getDefault();
        logger.info("Scheduling strategy changed to: {}", this.strategy.getName());
    }

    @Override
    public SchedulingStrategy getSchedulingStrategy() {
        return strategy;
    }

    @Override
    public CompletableFuture<Void> executeBehavior(Behavior behavior) {
        if (behavior == null || !behavior.isRunnable()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                behavior.action();
                
                // 更新统计信息
                totalExecutions.incrementAndGet();
                long executionTime = System.currentTimeMillis() - startTime;
                totalExecutionTime.addAndGet(executionTime);
                
                if (behavior.isDone()) {
                    completedBehaviors.incrementAndGet();
                }
                
            } catch (Exception e) {
                errorBehaviors.incrementAndGet();
                logger.error("Error executing behavior: {}: {}", behavior.getName(), e.getMessage(), e);
                
                // 通知行为异常
                behavior.onException(e);
            }
        }, behaviorExecutor);
    }

    @Override
    public SchedulerStats getStats() {
        long executions = totalExecutions.get();
        long avgTime = executions > 0 ? totalExecutionTime.get() / executions : 0;
        
        return new SchedulerStats(
            state.get(),
            behaviors.size(),
            getActiveBehaviorCount(),
            (int) completedBehaviors.get(),
            (int) errorBehaviors.get(),
            executions,
            avgTime
        );
    }

    /**
     * 调度行为执行
     */
    private void scheduleBehaviors() {
        if (state.get() != SchedulerState.RUNNING) {
            return;
        }

        try {
            // 清理已完成的行为
            behaviors.removeIf(behavior -> {
                BehaviorState behaviorState = behavior.getState();
                return behaviorState == BehaviorState.DONE || 
                       behaviorState == BehaviorState.CANCELLED;
            });

            // 获取可执行的行为列表
            List<Behavior> runnableBehaviors = behaviors.stream()
                .filter(Behavior::isRunnable)
                .toList();

            if (runnableBehaviors.isEmpty()) {
                return;
            }

            // 使用调度策略选择下一个要执行的行为
            Behavior selectedBehavior = strategy.selectNext(runnableBehaviors);
            
            if (selectedBehavior != null) {
                executeBehavior(selectedBehavior);
            }

        } catch (Exception e) {
            logger.error("Error in behavior scheduling: {}", e.getMessage(), e);
        }
    }
}
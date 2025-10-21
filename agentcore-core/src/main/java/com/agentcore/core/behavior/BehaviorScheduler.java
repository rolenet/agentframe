package com.agentcore.core.behavior;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 行为调度器接口
 * 负责管理和调度Agent的行为执行
 * 
 * @author AgentCore Team
 */
public interface BehaviorScheduler {

    /**
     * 启动调度器
     */
    void start();

    /**
     * 停止调度器
     */
    void stop();

    /**
     * 暂停调度器
     */
    void suspend();

    /**
     * 恢复调度器
     */
    void resume();

    /**
     * 销毁调度器，释放所有资源
     */
    void destroy();

    /**
     * 添加行为
     * 
     * @param behavior 要添加的行为
     */
    void addBehavior(Behavior behavior);

    /**
     * 移除行为
     * 
     * @param behavior 要移除的行为
     */
    void removeBehavior(Behavior behavior);

    /**
     * 移除所有行为
     */
    void removeAllBehaviors();

    /**
     * 获取所有行为
     * 
     * @return 行为列表
     */
    List<Behavior> getBehaviors();

    /**
     * 获取活跃行为数量
     * 
     * @return 活跃行为数量
     */
    int getActiveBehaviorCount();

    /**
     * 检查调度器是否正在运行
     * 
     * @return 如果正在运行返回true
     */
    boolean isRunning();

    /**
     * 检查调度器是否已暂停
     * 
     * @return 如果已暂停返回true
     */
    boolean isSuspended();

    /**
     * 获取调度器状态
     * 
     * @return 调度器状态
     */
    SchedulerState getState();

    /**
     * 设置调度策略
     * 
     * @param strategy 调度策略
     */
    void setSchedulingStrategy(SchedulingStrategy strategy);

    /**
     * 获取调度策略
     * 
     * @return 调度策略
     */
    SchedulingStrategy getSchedulingStrategy();

    /**
     * 异步执行单个行为
     * 
     * @param behavior 要执行的行为
     * @return CompletableFuture
     */
    CompletableFuture<Void> executeBehavior(Behavior behavior);

    /**
     * 获取调度器统计信息
     * 
     * @return 调度器统计信息
     */
    SchedulerStats getStats();

    /**
     * 调度器状态枚举
     */
    enum SchedulerState {
        STOPPED("stopped", "已停止"),
        STARTING("starting", "启动中"),
        RUNNING("running", "运行中"),
        SUSPENDED("suspended", "已暂停"),
        STOPPING("stopping", "停止中"),
        DESTROYED("destroyed", "已销毁");

        private final String code;
        private final String description;

        SchedulerState(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public boolean isActive() {
            return this == RUNNING;
        }

        public boolean isTerminated() {
            return this == STOPPED || this == DESTROYED;
        }
    }

    /**
     * 调度器统计信息记录类
     */
    record SchedulerStats(
        SchedulerState state,
        int totalBehaviors,
        int activeBehaviors,
        int completedBehaviors,
        int errorBehaviors,
        long totalExecutions,
        long averageExecutionTimeMs
    ) {
        @Override
        public String toString() {
            return String.format(
                "SchedulerStats{state=%s, total=%d, active=%d, completed=%d, errors=%d, executions=%d, avgTime=%dms}",
                state, totalBehaviors, activeBehaviors, completedBehaviors, errorBehaviors, totalExecutions, averageExecutionTimeMs
            );
        }
    }
}
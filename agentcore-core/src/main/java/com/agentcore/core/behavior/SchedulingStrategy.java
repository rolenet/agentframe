package com.agentcore.core.behavior;

import java.util.List;

/**
 * 调度策略接口
 * 定义行为的调度算法
 * 
 * @author AgentCore Team
 */
public interface SchedulingStrategy {

    /**
     * 选择下一个要执行的行为
     * 
     * @param behaviors 可执行的行为列表
     * @return 选中的行为，如果没有可执行的行为返回null
     */
    Behavior selectNext(List<Behavior> behaviors);

    /**
     * 获取策略名称
     * 
     * @return 策略名称
     */
    String getName();

    /**
     * 获取策略描述
     * 
     * @return 策略描述
     */
    String getDescription();

    /**
     * 优先级调度策略
     * 按行为优先级选择，优先级高的先执行
     */
    class PrioritySchedulingStrategy implements SchedulingStrategy {
        
        @Override
        public Behavior selectNext(List<Behavior> behaviors) {
            return behaviors.stream()
                .filter(Behavior::isRunnable)
                .max((b1, b2) -> b1.getPriority().compareTo(b2.getPriority()))
                .orElse(null);
        }

        @Override
        public String getName() {
            return "Priority";
        }

        @Override
        public String getDescription() {
            return "按优先级调度，优先级高的行为先执行";
        }
    }

    /**
     * 轮询调度策略
     * 按顺序轮流执行行为
     */
    class RoundRobinSchedulingStrategy implements SchedulingStrategy {
        
        private int lastIndex = -1;

        @Override
        public Behavior selectNext(List<Behavior> behaviors) {
            List<Behavior> runnableBehaviors = behaviors.stream()
                .filter(Behavior::isRunnable)
                .toList();
            
            if (runnableBehaviors.isEmpty()) {
                return null;
            }

            lastIndex = (lastIndex + 1) % runnableBehaviors.size();
            return runnableBehaviors.get(lastIndex);
        }

        @Override
        public String getName() {
            return "RoundRobin";
        }

        @Override
        public String getDescription() {
            return "轮询调度，按顺序轮流执行行为";
        }
    }

    /**
     * 公平调度策略
     * 综合考虑优先级和执行频率
     */
    class FairSchedulingStrategy implements SchedulingStrategy {
        
        @Override
        public Behavior selectNext(List<Behavior> behaviors) {
            return behaviors.stream()
                .filter(Behavior::isRunnable)
                .max((b1, b2) -> {
                    // 首先按优先级比较
                    int priorityCompare = b1.getPriority().compareTo(b2.getPriority());
                    if (priorityCompare != 0) {
                        return priorityCompare;
                    }
                    
                    // 优先级相同时，选择等待时间更长的行为
                    if (b1 instanceof AbstractBehavior ab1 && b2 instanceof AbstractBehavior ab2) {
                        BehaviorState state1 = ab1.getState();
                        BehaviorState state2 = ab2.getState();
                        
                        // 等待状态的行为优先于就绪状态的行为
                        if (state1 == BehaviorState.WAITING && state2 == BehaviorState.READY) {
                            return 1;
                        } else if (state1 == BehaviorState.READY && state2 == BehaviorState.WAITING) {
                            return -1;
                        }
                    }
                    
                    return 0;
                })
                .orElse(null);
        }

        @Override
        public String getName() {
            return "Fair";
        }

        @Override
        public String getDescription() {
            return "公平调度，综合考虑优先级和等待时间";
        }
    }

    /**
     * 获取默认调度策略
     * 
     * @return 默认调度策略实例
     */
    static SchedulingStrategy getDefault() {
        return new PrioritySchedulingStrategy();
    }

    /**
     * 创建优先级调度策略
     * 
     * @return 优先级调度策略实例
     */
    static SchedulingStrategy priority() {
        return new PrioritySchedulingStrategy();
    }

    /**
     * 创建轮询调度策略
     * 
     * @return 轮询调度策略实例
     */
    static SchedulingStrategy roundRobin() {
        return new RoundRobinSchedulingStrategy();
    }

    /**
     * 创建公平调度策略
     * 
     * @return 公平调度策略实例
     */
    static SchedulingStrategy fair() {
        return new FairSchedulingStrategy();
    }
}
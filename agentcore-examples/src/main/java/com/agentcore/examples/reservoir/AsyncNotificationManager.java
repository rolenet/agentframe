package com.agentcore.examples.reservoir;

import com.agentcore.core.agent.AgentId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 异步通知管理器
 * 负责管理智能体启动完成通知、工作完成通知等异步事件
 */
public class AsyncNotificationManager {
    private static final Logger logger = LoggerFactory.getLogger(AsyncNotificationManager.class);
    
    // 通知类型枚举
    public enum NotificationType {
        AGENT_READY,        // 智能体启动完成
        WORK_COMPLETED,     // 工作完成
        WORKFLOW_FINISHED   // 整个工作流完成
    }
    
    // 智能体状态跟踪
    private final Map<AgentId, Boolean> agentReadyStatus = new ConcurrentHashMap<>();
    private final Map<AgentId, Object> workResults = new ConcurrentHashMap<>();
    private final Set<AgentId> expectedAgents = ConcurrentHashMap.newKeySet();
    
    // 异步等待机制
    private CountDownLatch agentStartupLatch;
    private CompletableFuture<Void> workflowCompletionFuture = new CompletableFuture<>();
    
    // 事件监听器
    private Consumer<Void> onAllAgentsReady;
    private Consumer<Map<AgentId, Object>> onAllWorkCompleted;
    private Consumer<Void> onWorkflowFinished;
    
    // 线程池
    private final ExecutorService executorService = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "AsyncNotification-" + System.currentTimeMillis());
        t.setDaemon(true);
        return t;
    });

    public AsyncNotificationManager() {
        logger.info("🔔 AsyncNotificationManager初始化完成");
    }

    /**
     * 设置期望的智能体列表
     */
    public void setExpectedAgents(Set<AgentId> agents) {
        this.expectedAgents.clear();
        this.expectedAgents.addAll(agents);
        this.agentStartupLatch = new CountDownLatch(agents.size());
        
        // 初始化状态
        for (AgentId agentId : agents) {
            agentReadyStatus.put(agentId, false);
        }
        
        logger.info("🔔 设置期望智能体数量: {}, 智能体列表: {}", 
                agents.size(), 
                agents.stream().map(AgentId::getShortId).toArray());
    }

    /**
     * 设置所有智能体启动完成的回调
     */
    public void setOnAllAgentsReady(Consumer<Void> callback) {
        this.onAllAgentsReady = callback;
    }

    /**
     * 设置所有工作完成的回调
     */
    public void setOnAllWorkCompleted(Consumer<Map<AgentId, Object>> callback) {
        this.onAllWorkCompleted = callback;
    }

    /**
     * 设置工作流完成的回调
     */
    public void setOnWorkflowFinished(Consumer<Void> callback) {
        this.onWorkflowFinished = callback;
    }

    /**
     * 处理智能体启动完成通知
     */
    public void notifyAgentReady(AgentId agentId) {
        // 使用更灵活的匹配方式，通过名称匹配而不是完整的AgentId
        boolean isExpected = expectedAgents.stream()
            .anyMatch(expected -> expected.name().equals(agentId.name()));
            
        if (!isExpected) {
            logger.warn("🔔 收到未期望的智能体启动通知: {}", agentId.getShortId());
            return;
        }

        // 查找对应的期望AgentId
        AgentId expectedAgentId = expectedAgents.stream()
            .filter(expected -> expected.name().equals(agentId.name()))
            .findFirst()
            .orElse(agentId);

        if (agentReadyStatus.get(expectedAgentId)) {
            logger.warn("🔔 智能体{}已经标记为就绪，忽略重复通知", agentId.getShortId());
            return;
        }

        agentReadyStatus.put(expectedAgentId, true);
        agentStartupLatch.countDown();
        
        long remainingCount = agentStartupLatch.getCount();
        logger.info("🔔 ✅ 智能体{}启动完成通知已接收，剩余等待: {}/{}", 
                agentId.getShortId(), remainingCount, expectedAgents.size());

        // 检查是否所有智能体都已就绪
        if (remainingCount == 0) {
            logger.info("🔔 🎉 所有智能体启动完成！触发启动完成回调...");
            executorService.submit(() -> {
                if (onAllAgentsReady != null) {
                    try {
                        onAllAgentsReady.accept(null);
                    } catch (Exception e) {
                        logger.error("🔔 执行启动完成回调时发生错误", e);
                    }
                }
            });
        }
    }

    /**
     * 处理工作完成通知
     */
    public void notifyWorkCompleted(AgentId agentId, Object workResult) {
        if (!expectedAgents.contains(agentId)) {
            logger.warn("🔔 收到未期望的智能体工作完成通知: {}", agentId.getShortId());
            return;
        }

        workResults.put(agentId, workResult);
        logger.info("🔔 ✅ 智能体{}工作完成通知已接收，当前完成数: {}/{}", 
                agentId.getShortId(), workResults.size(), expectedAgents.size());

        // 检查是否所有工作都已完成（排除DocumentOutputAgent，它是协调者）
        Set<AgentId> workingAgents = Set.of(
            AgentId.create("DataCollectionAgent"),
            AgentId.create("DataStatisticsAgent"),
            AgentId.create("WeatherSummaryAgent"),
            AgentId.create("DutyInfoAgent")
        );

        boolean allWorkCompleted = workingAgents.stream()
                .allMatch(workResults::containsKey);

        if (allWorkCompleted) {
            logger.info("🔔 🎉 所有工作智能体完成！触发工作完成回调...");
            executorService.submit(() -> {
                if (onAllWorkCompleted != null) {
                    try {
                        onAllWorkCompleted.accept(new ConcurrentHashMap<>(workResults));
                    } catch (Exception e) {
                        logger.error("🔔 执行工作完成回调时发生错误", e);
                    }
                }
            });
        }
    }

    /**
     * 处理整个工作流完成通知
     */
    public void notifyWorkflowFinished() {
        logger.info("🔔 🎉 整个工作流程完成！");
        workflowCompletionFuture.complete(null);
        
        executorService.submit(() -> {
            if (onWorkflowFinished != null) {
                try {
                    onWorkflowFinished.accept(null);
                } catch (Exception e) {
                    logger.error("🔔 执行工作流完成回调时发生错误", e);
                }
            }
        });
    }

    /**
     * 异步等待所有智能体启动完成
     */
    public CompletableFuture<Void> waitForAllAgentsReady() {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("🔔 开始等待所有智能体启动完成...");
                boolean success = agentStartupLatch.await(30, TimeUnit.SECONDS);
                if (!success) {
                    throw new RuntimeException("等待智能体启动超时");
                }
                logger.info("🔔 所有智能体启动完成等待结束");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("等待智能体启动被中断", e);
            }
        }, executorService);
    }

    /**
     * 异步等待整个工作流完成
     */
    public CompletableFuture<Void> waitForWorkflowCompletion() {
        return workflowCompletionFuture;
    }

    /**
     * 获取智能体启动状态
     */
    public Map<AgentId, Boolean> getAgentReadyStatus() {
        return new ConcurrentHashMap<>(agentReadyStatus);
    }

    /**
     * 获取工作结果
     */
    public Map<AgentId, Object> getWorkResults() {
        return new ConcurrentHashMap<>(workResults);
    }

    /**
     * 重置状态（用于重新开始工作流）
     */
    public void reset() {
        agentReadyStatus.clear();
        workResults.clear();
        workflowCompletionFuture = new CompletableFuture<>();
        if (expectedAgents.size() > 0) {
            agentStartupLatch = new CountDownLatch(expectedAgents.size());
            for (AgentId agentId : expectedAgents) {
                agentReadyStatus.put(agentId, false);
            }
        }
        logger.info("🔔 AsyncNotificationManager状态已重置");
    }

    /**
     * 关闭管理器
     */
    public void shutdown() {
        logger.info("🔔 正在关闭AsyncNotificationManager...");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("🔔 AsyncNotificationManager已关闭");
    }
}
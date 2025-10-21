package com.agentcore.container;

import com.agentcore.communication.router.LocalMessageRouter;
import com.agentcore.communication.router.MessageRouter;
import com.agentcore.core.agent.Agent;
import com.agentcore.core.agent.AgentId;
import com.agentcore.core.agent.AgentState;
import com.agentcore.core.message.AgentMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 默认Agent容器实现
 * 
 * @author AgentCore Team
 */
public class DefaultAgentContainer implements AgentContainer {

    private static final Logger logger = LoggerFactory.getLogger(DefaultAgentContainer.class);

    private final ContainerConfig config;
    private final ConcurrentHashMap<String, Agent> agents = new ConcurrentHashMap<>();
    private final MessageRouter messageRouter;
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    // 统计信息
    private final AtomicLong messagesProcessed = new AtomicLong(0);
    private final AtomicLong totalResponseTime = new AtomicLong(0);

    private volatile AgentEventListener eventListener;

    public DefaultAgentContainer(String containerName) {
        this.config = ContainerConfig.builder(containerName).build();
        this.messageRouter = new LocalMessageRouter();
    }
    /**
     * 构造函数
     * 
     * @param config 容器配置
     */
    public DefaultAgentContainer(ContainerConfig config) {
        this.config = config;
        this.messageRouter = new LocalMessageRouter();
    }

    /**
     * 构造函数
     * 
     * @param config 容器配置
     * @param messageRouter 消息路由器
     */
    public DefaultAgentContainer(ContainerConfig config, MessageRouter messageRouter) {
        this.config = config;
        this.messageRouter = messageRouter != null ? messageRouter : new LocalMessageRouter();
    }

    @Override
    public CompletableFuture<Void> start() {
        if (!running.compareAndSet(false, true)) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Container is already running"));
        }

        logger.info("Starting agent container: {}", config.name());

        return CompletableFuture.runAsync(() -> {
            try {
                // 如果配置了自动启动，启动所有Agent
                if (config.autoStartAgents()) {
                    for (Agent agent : agents.values()) {
                        try {
                            startAgent(agent);
                        } catch (Exception e) {
                            logger.error("Failed to start agent: {}", agent.getAgentId(), e);
                            notifyAgentError(agent, e);
                        }
                    }
                }

                logger.info("Agent container started successfully: {}", config.name());
            } catch (Exception e) {
                running.set(false);
                logger.error("Failed to start agent container: {}", config.name(), e);
                throw new RuntimeException("Container start failed", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> stop() {
        if (!running.compareAndSet(true, false)) {
            return CompletableFuture.completedFuture(null);
        }

        logger.info("Stopping agent container: {}", config.name());

        return CompletableFuture.runAsync(() -> {
            try {
                // 停止所有Agent
                List<CompletableFuture<Void>> stopFutures = agents.values().stream()
                    .map(this::stopAgent)
                    .toList();

                // 等待所有Agent停止
                CompletableFuture.allOf(stopFutures.toArray(new CompletableFuture[0]))
                    .orTimeout(config.agentStopTimeout().toMillis(), TimeUnit.MILLISECONDS)
                    .join();

                logger.info("Agent container stopped successfully: {}", config.name());
            } catch (Exception e) {
                logger.error("Error stopping agent container: {}", config.name(), e);
                throw new RuntimeException("Container stop failed", e);
            }
        });
    }

    @Override
    public <T extends Agent> T createAgent(Class<T> agentClass, AgentId agentId) {
        if (agentClass == null || agentId == null) {
            throw new IllegalArgumentException("Agent class and ID cannot be null");
        }

        if (agents.size() >= config.maxAgents()) {
            throw new IllegalStateException("Container has reached maximum agent limit: " + config.maxAgents());
        }

        if (containsAgent(agentId)) {
            throw new IllegalArgumentException("Agent already exists: " + agentId);
        }

        try {
            // 创建Agent实例
            T agent = createAgentInstance(agentClass, agentId);
            
            // 添加到容器
            addAgent(agent);
            
            logger.info("Created agent: {}", agentId);
            return agent;
            
        } catch (Exception e) {
            logger.error("Failed to create agent: {}", agentId, e);
            throw new RuntimeException("Agent creation failed", e);
        }
    }

    @Override
    public void addAgent(Agent agent) {
        if (agent == null) {
            throw new IllegalArgumentException("Agent cannot be null");
        }

        AgentId agentId = agent.getAgentId();
        if (containsAgent(agentId)) {
            throw new IllegalArgumentException("Agent already exists: " + agentId);
        }

        if (agents.size() >= config.maxAgents()) {
            throw new IllegalStateException("Container has reached maximum agent limit: " + config.maxAgents());
        }

        // 添加到容器
        agents.put(agentId.getFullId(), agent);
        
        // 注册到消息路由器
        messageRouter.registerAgent(agentId, agent::handleMessage);
        
        // 如果容器正在运行且配置了自动启动，启动Agent
        if (running.get() && config.autoStartAgents()) {
            try {
                startAgent(agent);
            } catch (Exception e) {
                logger.error("Failed to start agent after adding: {}", agentId, e);
                notifyAgentError(agent, e);
            }
        }

        notifyAgentAdded(agent);
        logger.debug("Added agent to container: {}", agentId);
    }

    @Override
    public Agent removeAgent(AgentId agentId) {
        if (agentId == null) {
            return null;
        }

        Agent agent = agents.remove(agentId.getFullId());
        if (agent != null) {
            try {
                // 停止Agent
                stopAgent(agent).join();
                
                // 从消息路由器注销
                messageRouter.unregisterAgent(agentId);
                
                notifyAgentRemoved(agent);
                logger.debug("Removed agent from container: {}", agentId);
            } catch (Exception e) {
                logger.error("Error removing agent: {}", agentId, e);
            }
        }

        return agent;
    }

    @Override
    public Agent getAgent(AgentId agentId) {
        return agentId != null ? agents.get(agentId.getFullId()) : null;
    }

    @Override
    public Agent getAgent(String name) {
        return agents.values().stream()
            .filter(agent -> agent.getName().equals(name))
            .findFirst()
            .orElse(null);
    }

    @Override
    public List<Agent> getAllAgents() {
        return List.copyOf(agents.values());
    }

    @Override
    public List<Agent> getAgentsByType(String type) {
        return agents.values().stream()
            .filter(agent -> agent.getType().equals(type))
            .toList();
    }

    @Override
    public boolean containsAgent(AgentId agentId) {
        return agentId != null && agents.containsKey(agentId.getFullId());
    }

    @Override
    public int getAgentCount() {
        return agents.size();
    }

    @Override
    public String getName() {
        return config.name();
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public ContainerConfig getConfig() {
        return config;
    }

    @Override
    public ContainerStats getStats() {
        int totalAgents = agents.size();
        int activeAgents = (int) agents.values().stream()
            .filter(Agent::isActive)
            .count();
        int suspendedAgents = (int) agents.values().stream()
            .filter(agent -> agent.getState() == AgentState.SUSPENDED)
            .count();
        int errorAgents = (int) agents.values().stream()
            .filter(agent -> agent.getState() == AgentState.ERROR)
            .count();

        long processed = messagesProcessed.get();
        double avgTime = processed > 0 ? 
            (totalResponseTime.get() / 1_000_000.0) / processed : 0.0;

        return new ContainerStats(
            config.name(),
            running.get(),
            totalAgents,
            activeAgents,
            suspendedAgents,
            errorAgents,
            processed,
            avgTime
        );
    }

    @Override
    public void setAgentEventListener(AgentEventListener listener) {
        this.eventListener = listener;
    }

    @Override
    public void sendMessage(String agentName, AgentMessage testMessage) {
        Agent agent = getAgent(agentName);
        if (agent != null) {
            agent.handleMessage(testMessage);
        } else {
            logger.warn("Agent not found: {}", agentName);
        }
    }

    @Override
    public void registerAgent(String agentName, Agent simpleAgent) {

    }

    /**
     * 启动Agent
     * 
     * @param agent Agent实例
     * @return CompletableFuture
     */
    private CompletableFuture<Void> startAgent(Agent agent) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (agent.getState() == AgentState.INITIATED) {
                    agent.init();
                }
                agent.start();
                notifyAgentStarted(agent);
            } catch (Exception e) {
                logger.error("Failed to start agent: {}", agent.getAgentId(), e);
                notifyAgentError(agent, e);
                throw new RuntimeException("Agent start failed", e);
            }
        }).orTimeout(config.agentStartTimeout().toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * 停止Agent
     * 
     * @param agent Agent实例
     * @return CompletableFuture
     */
    private CompletableFuture<Void> stopAgent(Agent agent) {
        return CompletableFuture.runAsync(() -> {
            try {
                agent.stop();
                notifyAgentStopped(agent);
            } catch (Exception e) {
                logger.error("Failed to stop agent: {}", agent.getAgentId(), e);
                notifyAgentError(agent, e);
                throw new RuntimeException("Agent stop failed", e);
            }
        }).orTimeout(config.agentStopTimeout().toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * 创建Agent实例
     * 
     * @param agentClass Agent类
     * @param agentId Agent ID
     * @param <T> Agent类型
     * @return Agent实例
     */
    private <T extends Agent> T createAgentInstance(Class<T> agentClass, AgentId agentId) 
            throws Exception {
        // 尝试使用AgentId构造函数
        try {
            Constructor<T> constructor = agentClass.getConstructor(AgentId.class);
            // 设置构造函数可访问，解决内部类/模块访问限制
            constructor.setAccessible(true);
            return constructor.newInstance(agentId);
        } catch (NoSuchMethodException e) {
            // 尝试使用默认构造函数
            try {
                Constructor<T> constructor = agentClass.getConstructor();
                return constructor.newInstance();
            } catch (NoSuchMethodException ex) {
                throw new IllegalArgumentException(
                    "Agent class must have a constructor with AgentId parameter or default constructor: " 
                    + agentClass.getName());
            }
        }
    }

    /**
     * 通知Agent添加事件
     */
    private void notifyAgentAdded(Agent agent) {
        if (eventListener != null) {
            try {
                eventListener.onAgentAdded(agent);
            } catch (Exception e) {
                logger.error("Error in agent added event listener", e);
            }
        }
    }

    /**
     * 通知Agent移除事件
     */
    private void notifyAgentRemoved(Agent agent) {
        if (eventListener != null) {
            try {
                eventListener.onAgentRemoved(agent);
            } catch (Exception e) {
                logger.error("Error in agent removed event listener", e);
            }
        }
    }

    /**
     * 通知Agent启动事件
     */
    private void notifyAgentStarted(Agent agent) {
        if (eventListener != null) {
            try {
                eventListener.onAgentStarted(agent);
            } catch (Exception e) {
                logger.error("Error in agent started event listener", e);
            }
        }
    }

    /**
     * 通知Agent停止事件
     */
    private void notifyAgentStopped(Agent agent) {
        if (eventListener != null) {
            try {
                eventListener.onAgentStopped(agent);
            } catch (Exception e) {
                logger.error("Error in agent stopped event listener", e);
            }
        }
    }

    /**
     * 通知Agent错误事件
     */
    private void notifyAgentError(Agent agent, Throwable error) {
        if (eventListener != null) {
            try {
                eventListener.onAgentError(agent, error);
            } catch (Exception e) {
                logger.error("Error in agent error event listener", e);
            }
        }
    }

    /**
     * 获取消息路由器（用于测试）
     * 
     * @return 消息路由器
     */
    public MessageRouter getMessageRouter() {
        return messageRouter;
    }
}
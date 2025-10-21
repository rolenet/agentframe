package com.agentcore.core.agent;

import com.agentcore.core.behavior.Behavior;
import com.agentcore.core.behavior.BehaviorScheduler;
import com.agentcore.core.message.AgentMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Agent抽象基类
 * 提供Agent的基本实现和生命周期管理
 * 
 * @author AgentCore Team
 */
public abstract class AbstractAgent implements Agent {
    private static final Logger logger = LoggerFactory.getLogger(AbstractAgent.class);

    protected final AgentId agentId;
    protected final AtomicReference<AgentState> state;
    protected final BehaviorScheduler behaviorScheduler;
    protected final ConcurrentLinkedQueue<AgentMessage> messageQueue;
    protected final ConcurrentHashMap<String, CompletableFuture<AgentMessage>> pendingRequests;
    
    private volatile AgentMetadata metadata;
    private volatile Thread agentThread;

    /**
     * 构造函数
     * 
     * @param agentId Agent身份标识
     */
    protected AbstractAgent(AgentId agentId) {
        this.agentId = agentId;
        this.state = new AtomicReference<>(AgentState.INITIATED);
        this.behaviorScheduler = createBehaviorScheduler();
        this.messageQueue = new ConcurrentLinkedQueue<>();
        this.pendingRequests = new ConcurrentHashMap<>();
        this.metadata = AgentMetadata.create("Agent: " + agentId.name());
        
        // 设置日志上下文
        MDC.put("agentId", agentId.getShortId());
    }

    @Override
    public AgentId getAgentId() {
        return agentId;
    }

    @Override
    public AgentState getState() {
        return state.get();
    }

    @Override
    public void init() {
        if (!state.compareAndSet(AgentState.INITIATED, AgentState.STARTING)) {
            throw new IllegalStateException("Agent can only be initialized from INITIATED state");
        }

        logger.info("Initializing agent: {}", agentId);
        
        try {
            // 执行子类的初始化逻辑
            doInit();

            logger.info("Agent initialized successfully: {}", agentId);
        } catch (Exception e) {
            state.set(AgentState.ERROR);
            logger.error("Failed to initialize agent: {}, {}", agentId, e.getMessage());
            throw new RuntimeException("Agent initialization failed", e);
        }
    }

    @Override
    public void start() {
        AgentState currentState = state.get();
        if (currentState != AgentState.STARTING && currentState != AgentState.STOPPED) {
            throw new IllegalStateException("Agent can only be started from STARTING or STOPPED state");
        }

        logger.info("Starting agent: {}", agentId);
        
        try {
            // 启动行为调度器
            behaviorScheduler.start();
            
            // 启动消息处理线程
            startMessageProcessing();
            
            // 执行子类的启动逻辑
            doStart();
            
            state.set(AgentState.ACTIVE);
            logger.info("Agent started successfully: {}", agentId);
        } catch (Exception e) {
            state.set(AgentState.ERROR);
            logger.error("Failed to start agent: {}, {}", agentId, e.getMessage());
            throw new RuntimeException("Agent start failed", e);
        }
    }

    @Override
    public void suspend() {
        if (!state.compareAndSet(AgentState.ACTIVE, AgentState.SUSPENDED)) {
            throw new IllegalStateException("Agent can only be suspended from ACTIVE state");
        }

        logger.info("Suspending agent: {}", agentId);
        
        try {
            // 暂停行为调度器
            behaviorScheduler.suspend();
            
            // 执行子类的暂停逻辑
            doSuspend();

            logger.info("Agent suspended successfully: {}", agentId);
        } catch (Exception e) {
            state.set(AgentState.ERROR);
            logger.error("Failed to suspend agent: {}, {}", agentId, e.getMessage());
            throw new RuntimeException("Agent suspend failed", e);
        }
    }

    @Override
    public void resume() {
        if (!state.compareAndSet(AgentState.SUSPENDED, AgentState.ACTIVE)) {
            throw new IllegalStateException("Agent can only be resumed from SUSPENDED state");
        }
        
        logger.info("Resuming agent: {}", agentId);
        
        try {
            // 恢复行为调度器
            behaviorScheduler.resume();
            
            // 执行子类的恢复逻辑
            doResume();
            
            logger.info("Agent resumed successfully: {}", agentId);
        } catch (Exception e) {
            state.set(AgentState.ERROR);
            logger.error("Failed to resume agent: {}, {}", agentId, e.getMessage());
            throw new RuntimeException("Agent resume failed", e);
        }
    }

    @Override
    public void stop() {
        AgentState currentState = state.get();
        if (!currentState.canTransitionTo(AgentState.STOPPING)) {
            throw new IllegalStateException("Agent cannot be stopped from current state: " + currentState);
        }
        
        state.set(AgentState.STOPPING);
        logger.info("Stopping agent: {}", agentId);
        
        try {
            // 停止行为调度器
            behaviorScheduler.stop();
            
            // 停止消息处理线程
            stopMessageProcessing();
            
            // 执行子类的停止逻辑
            doStop();
            
            state.set(AgentState.STOPPED);
            logger.info("Agent stopped successfully: {}", agentId);
        } catch (Exception e) {
            state.set(AgentState.ERROR);
            logger.error("Failed to stop agent: {}, {}", agentId, e.getMessage());
            throw new RuntimeException("Agent stop failed", e);
        }
    }

    @Override
    public void destroy() {
        AgentState currentState = state.get();
        if (currentState != AgentState.STOPPED && currentState != AgentState.ERROR) {
            stop();
        }
        
        logger.info("Destroying agent: {}", agentId);
        
        try {
            // 清理资源
            behaviorScheduler.destroy();
            messageQueue.clear();
            pendingRequests.clear();
            
            // 执行子类的销毁逻辑
            doDestroy();
            
            state.set(AgentState.DESTROYED);
            logger.info("Agent destroyed successfully: {}", agentId);
        } catch (Exception e) {
            logger.error("Failed to destroy agent: {}, {}", agentId, e.getMessage());
            throw new RuntimeException("Agent destroy failed", e);
        } finally {
            // 清理日志上下文
            MDC.remove("agentId");
        }
    }

    @Override
    public void addBehavior(Behavior behavior) {
        behaviorScheduler.addBehavior(behavior);
        logger.info("Added behavior to agent {}: {}", agentId, behavior.getClass().getSimpleName());
    }

    @Override
    public void removeBehavior(Behavior behavior) {
        behaviorScheduler.removeBehavior(behavior);
        logger.info("Removed behavior from agent {}: {}", agentId, behavior.getClass().getSimpleName());
    }

    @Override
    public CompletableFuture<Void> sendMessage(AgentMessage message) {
        if (!isActive()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Agent is not active: " + state.get()));
        }
        
        logger.info("Sending message from {} to {}: {}",
            agentId, message.receiver(), message.performative());
        
        return doSendMessage(message);
    }

    @Override
    public AgentMessage sendAndWait(AgentMessage message, long timeoutMs) {
        if (!isActive()) {
            throw new IllegalStateException("Agent is not active: " + state.get());
        }
        
        // 为请求消息设置replyWith字段
        String replyWith = java.util.UUID.randomUUID().toString();
        AgentMessage requestMessage = message.withProperty("replyWith", replyWith);

        // 创建等待响应的Future
        CompletableFuture<AgentMessage> responseFuture = new CompletableFuture<>();
        pendingRequests.put(replyWith, responseFuture);

        try {
            // 合并消息发送和响应等待的超时控制，确保总时间不超过timeoutMs
            return sendMessage(requestMessage)
                    .thenCompose(v -> responseFuture)  // 发送完成后等待响应
                    .get(timeoutMs, TimeUnit.MILLISECONDS);  // 总超时控制
        } catch (Exception e) {
            pendingRequests.remove(replyWith);
            throw new RuntimeException("Failed to send and wait for response", e);
        }
    }

    @Override
    public void handleMessage(AgentMessage message) {
        if (!isActive()) {
            System.out.format("Received message while agent is not active: {}", state.get());
            return;
        }
        // 将消息加入队列
        messageQueue.offer(message);

//        logger.info("🔥🔥🔥 CRITICAL: Message added to queue for {}, queue size: {}",
//            agentId.getShortId(), messageQueue.size());
//        logger.info("🔥🔥🔥 CRITICAL: handleMessage called for {}: from={}, to={}, content={}",
//                agentId.getShortId(), message.sender().getShortId(), message.receiver().getShortId(), message.content());
    }

    @Override
    public AgentMetadata getMetadata() {
        return metadata;
    }

    @Override
    public void setMetadata(AgentMetadata metadata) {
        this.metadata = metadata;
    }

    /**
     * 创建行为调度器
     * 子类可以重写此方法来提供自定义的调度器
     * 
     * @return BehaviorScheduler实例
     */
    protected abstract BehaviorScheduler createBehaviorScheduler();

    /**
     * 执行实际的消息发送
     * 子类必须实现此方法来提供具体的消息发送逻辑
     * 
     * @param message 要发送的消息
     * @return CompletableFuture
     */
    protected abstract CompletableFuture<Void> doSendMessage(AgentMessage message);

    /**
     * 子类初始化逻辑
     * 子类可以重写此方法来执行特定的初始化操作
     */
    protected void doInit() {
        // 默认空实现
    }

    /**
     * 子类启动逻辑
     * 子类可以重写此方法来执行特定的启动操作
     */
    protected void doStart() {
        // 默认空实现
    }

    /**
     * 子类暂停逻辑
     * 子类可以重写此方法来执行特定的暂停操作
     */
    protected void doSuspend() {
        // 默认空实现
    }

    /**
     * 子类恢复逻辑
     * 子类可以重写此方法来执行特定的恢复操作
     */
    protected void doResume() {
        // 默认空实现
    }

    /**
     * 子类停止逻辑
     * 子类可以重写此方法来执行特定的停止操作
     */
    protected void doStop() {
        // 默认空实现
    }

    /**
     * 子类销毁逻辑
     * 子类可以重写此方法来执行特定的销毁操作
     */
    protected void doDestroy() {
        // 默认空实现
    }

    /**
     * 启动消息处理线程
     */
    private void startMessageProcessing() {
        agentThread = new Thread(this::processMessages, "Agent-" + agentId.name());
        agentThread.start();
    }

    /**
     * 停止消息处理线程
     */
    private void stopMessageProcessing() {
        if (agentThread != null) {
            agentThread.interrupt();
            try {
                agentThread.join(5000); // 等待5秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 消息处理循环
     */
    private void processMessages() {
        MDC.put("agentId", agentId.getShortId());
        
        try {
            while (!Thread.currentThread().isInterrupted() && !state.get().isTerminated()) {
                try {
                    AgentMessage message = messageQueue.poll();
                    if (message != null) {
                        processMessage(message);
                    } else {
                        // 短暂休眠避免CPU占用过高
                        Thread.sleep(10);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Error processing message in agent {}: {}", agentId, e.getMessage());
                }
            }
        } finally {
            MDC.remove("agentId");
        }
    }

    /**
     * 处理单个消息
     * 
     * @param message 要处理的消息
     */
    private void processMessage(AgentMessage message) {
        try {
            // 检查是否为响应消息
            if (message.isResponse() && message.inReplyTo() != null) {
                CompletableFuture<AgentMessage> pendingRequest = 
                    pendingRequests.remove(message.inReplyTo());
                if (pendingRequest != null) {
                    pendingRequest.complete(message);
                    return;
                }
            }
            
            // 调用子类的消息处理逻辑
            doHandleMessage(message);
            
        } catch (Exception e) {
            logger.error("Error handling message in agent {}: {}: {}", agentId, message, e.getMessage());
            
            // 如果是请求消息，发送错误响应
            if (message.isRequest()) {
                try {
                    AgentMessage errorResponse = AgentMessage.createErrorResponse(message, 
                        "Error processing message: " + e.getMessage());
                    sendMessage(errorResponse);
                } catch (Exception sendError) {
                     logger.error("Failed to send error response: {}", sendError.getMessage());
                }
            }
        }
    }

    /**
     * 处理消息的具体逻辑
     * 子类可以重写此方法来实现自定义的消息处理逻辑
     * 
     * @param message 要处理的消息
     */
    protected void doHandleMessage(AgentMessage message) {
        logger.info("Default message handling for {}: {}", agentId, message);
        
        // 默认处理：如果是请求消息，发送确认响应
        if (message.isRequest()) {
            AgentMessage response = AgentMessage.createResponse(message, "Message received");
            sendMessage(response);
        }
    }
}
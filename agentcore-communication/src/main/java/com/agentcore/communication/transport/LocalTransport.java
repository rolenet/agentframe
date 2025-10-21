package com.agentcore.communication.transport;

import com.agentcore.core.agent.AgentId;
import com.agentcore.core.message.AgentMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 本地传输实现
 * 用于同一JVM内的Agent通信
 * 
 * @author AgentCore Team
 */
public class LocalTransport implements Transport {

    private static final Logger logger = LoggerFactory.getLogger(LocalTransport.class);

    private final AtomicLong messagesSent = new AtomicLong(0);
    private final AtomicLong messagesReceived = new AtomicLong(0);
    private final AtomicLong errors = new AtomicLong(0);
    
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "LocalTransport-" + System.currentTimeMillis());
        thread.setDaemon(true);
        return thread;
    });

    private volatile boolean running = false;
    private volatile MessageHandler messageHandler;

    @Override
    public CompletableFuture<Void> start() {
        return CompletableFuture.runAsync(() -> {
            if (!running) {
                running = true;
                logger.info("Local transport started");
            }
        });
    }

    @Override
    public CompletableFuture<Void> stop() {
        return CompletableFuture.runAsync(() -> {
            if (running) {
                running = false;
                executor.shutdown();
                logger.info("Local transport stopped");
            }
        });
    }

    @Override
    public CompletableFuture<Void> sendMessage(AgentMessage message) {
        return CompletableFuture.runAsync(() -> {
            if (!running) {
                logger.warn("Transport not started, message dropped: {}", message.messageId());
                errors.incrementAndGet();
                return;
            }

            try {
                if (messageHandler != null) {
                    // 异步处理消息以避免阻塞
                    executor.submit(() -> {
                        try {
                            messageHandler.handleMessage(message);
                            messagesReceived.incrementAndGet();
                            logger.debug("Message delivered locally: {} -> {}", 
                                message.sender(), message.receiver());
                        } catch (Exception e) {
                            logger.error("Error handling message: {}", message.messageId(), e);
                            errors.incrementAndGet();
                        }
                    });
                    messagesSent.incrementAndGet();
                } else {
                    logger.warn("No message handler set");
                    errors.incrementAndGet();
                }
                
            } catch (Exception e) {
                logger.error("Failed to send message: {}", message.messageId(), e);
                errors.incrementAndGet();
            }
        });
    }

    @Override
    public void setMessageHandler(MessageHandler handler) {
        this.messageHandler = handler;
        logger.debug("Message handler set: {}", handler != null ? handler.getClass().getSimpleName() : "null");
    }

    @Override
    public String getName() {
        return "LocalTransport";
    }

    @Override
    public TransportType getType() {
        return TransportType.LOCAL;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean supports(AgentId agentId) {
        // 本地传输支持所有Agent ID
        return true;
    }

    @Override
    public TransportConfig getConfig() {
        return new TransportConfig() {
            @Override
            public String getName() {
                return "local";
            }

            @Override
            public TransportType getType() {
                return TransportType.LOCAL;
            }

            @Override
            public String getHost() {
                return "localhost";
            }

            @Override
            public int getPort() {
                return 0;
            }

            @Override
            public java.util.Map<String, Object> getProperties() {
                return java.util.Map.of(
                    "executor.threads", "cached",
                    "async", true
                );
            }
        };
    }

    @Override
    public TransportStats getStats() {
        return new TransportStats(
            getName(),
            getType(),
            running,
            messagesSent.get(),
            messagesReceived.get(),
            0L, // bytesSent - 本地传输不计算字节数
            0L, // bytesReceived - 本地传输不计算字节数
            errors.get(),
            0.0 // averageLatencyMs - 本地传输延迟极低
        );
    }

    /**
     * 获取已发送消息数
     */
    public long getMessagesSent() {
        return messagesSent.get();
    }

    /**
     * 获取已接收消息数
     */
    public long getMessagesReceived() {
        return messagesReceived.get();
    }

    /**
     * 获取错误数
     */
    public long getErrors() {
        return errors.get();
    }

    /**
     * 重置统计信息
     */
    public void resetStats() {
        messagesSent.set(0);
        messagesReceived.set(0);
        errors.set(0);
        logger.info("Transport statistics reset");
    }
}
package com.agentcore.communication.transport;

import com.agentcore.core.agent.AgentId;
import com.agentcore.core.message.AgentMessage;

import java.util.concurrent.CompletableFuture;

/**
 * 传输层接口
 * 定义消息传输的基本契约
 * 
 * @author AgentCore Team
 */
public interface Transport {

    /**
     * 启动传输层
     * 
     * @return CompletableFuture，完成时表示启动成功
     */
    CompletableFuture<Void> start();

    /**
     * 停止传输层
     * 
     * @return CompletableFuture，完成时表示停止成功
     */
    CompletableFuture<Void> stop();

    /**
     * 发送消息
     * 
     * @param message 要发送的消息
     * @return CompletableFuture，完成时表示发送成功
     */
    CompletableFuture<Void> sendMessage(AgentMessage message);

    /**
     * 设置消息接收处理器
     * 
     * @param handler 消息处理器
     */
    void setMessageHandler(MessageHandler handler);

    /**
     * 获取传输层名称
     * 
     * @return 传输层名称
     */
    String getName();

    /**
     * 获取传输层类型
     * 
     * @return 传输层类型
     */
    TransportType getType();

    /**
     * 检查传输层是否正在运行
     * 
     * @return 如果正在运行返回true
     */
    boolean isRunning();

    /**
     * 检查是否支持指定的Agent地址
     * 
     * @param agentId Agent ID
     * @return 如果支持返回true
     */
    boolean supports(AgentId agentId);

    /**
     * 获取传输层配置
     * 
     * @return 传输层配置
     */
    TransportConfig getConfig();

    /**
     * 获取传输层统计信息
     * 
     * @return 传输层统计信息
     */
    TransportStats getStats();

    /**
     * 消息处理器接口
     */
    @FunctionalInterface
    interface MessageHandler {
        /**
         * 处理接收到的消息
         * 
         * @param message 接收到的消息
         */
        void handleMessage(AgentMessage message);
    }

    /**
     * 传输层类型枚举
     */
    enum TransportType {
        LOCAL("local", "本地传输"),
        TCP("tcp", "TCP传输"),
        HTTP("http", "HTTP传输"),
        WEBSOCKET("websocket", "WebSocket传输"),
        UDP("udp", "UDP传输");

        private final String code;
        private final String description;

        TransportType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 传输层统计信息记录类
     */
    record TransportStats(
        String name,
        TransportType type,
        boolean running,
        long messagesSent,
        long messagesReceived,
        long bytesSent,
        long bytesReceived,
        long errors,
        double averageLatencyMs
    ) {
        @Override
        public String toString() {
            return String.format(
                "TransportStats{name=%s, type=%s, running=%s, sent=%d, received=%d, errors=%d, latency=%.2fms}",
                name, type, running, messagesSent, messagesReceived, errors, averageLatencyMs
            );
        }
    }
}
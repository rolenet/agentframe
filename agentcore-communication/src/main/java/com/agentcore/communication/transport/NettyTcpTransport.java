package com.agentcore.communication.transport;

import com.agentcore.communication.serializer.MessageSerializer;
import com.agentcore.communication.serializer.SerializationException;
import com.agentcore.core.agent.AgentId;
import com.agentcore.core.message.AgentMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Netty TCP传输实现
 * 基于Netty的高性能TCP传输层
 * 
 * @author AgentCore Team
 */
public class NettyTcpTransport implements Transport {

    private static final Logger logger = LoggerFactory.getLogger(NettyTcpTransport.class);

    private final TransportConfig config;
    private final MessageSerializer serializer;
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    // Netty组件
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private final ConcurrentHashMap<String, Channel> clientChannels = new ConcurrentHashMap<>();
    
    // 统计信息
    private final AtomicLong messagesSent = new AtomicLong(0);
    private final AtomicLong messagesReceived = new AtomicLong(0);
    private final AtomicLong bytesSent = new AtomicLong(0);
    private final AtomicLong bytesReceived = new AtomicLong(0);
    private final AtomicLong errors = new AtomicLong(0);

    private volatile MessageHandler messageHandler;

    /**
     * 构造函数
     * 
     * @param config 传输配置
     * @param serializer 消息序列化器
     */
    public NettyTcpTransport(TransportConfig config, MessageSerializer serializer) {
        this.config = config;
        this.serializer = serializer;
    }

    @Override
    public CompletableFuture<Void> start() {
        if (!running.compareAndSet(false, true)) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Transport is already running"));
        }

        logger.info("Starting Netty TCP transport: {} on {}:{}", 
            config.getName(), config.getHost(), config.getPort());

        return CompletableFuture.runAsync(() -> {
            try {
                // 创建事件循环组
                bossGroup = new NioEventLoopGroup(1);
                workerGroup = new NioEventLoopGroup();

                // 配置服务器
                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_RCVBUF, config.getProperty("bufferSize", 8192))
                    .childOption(ChannelOption.SO_SNDBUF, config.getProperty("bufferSize", 8192))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            
                            // 添加帧解码器（解决TCP粘包问题）
                            pipeline.addLast(new LengthFieldBasedFrameDecoder(
                                Integer.MAX_VALUE, 0, 4, 0, 4));
                            pipeline.addLast(new LengthFieldPrepender(4));
                            
                            // 添加消息处理器
                            pipeline.addLast(new MessageChannelHandler());
                        }
                    });

                // 绑定端口并启动服务器
                ChannelFuture future = bootstrap.bind(config.getHost(), config.getPort()).sync();
                serverChannel = future.channel();
                
                logger.info("Netty TCP transport started successfully on {}:{}", 
                    config.getHost(), config.getPort());
                
            } catch (Exception e) {
                running.set(false);
                logger.error("Failed to start Netty TCP transport", e);
                throw new RuntimeException("Transport start failed", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> stop() {
        if (!running.compareAndSet(true, false)) {
            return CompletableFuture.completedFuture(null);
        }

        logger.info("Stopping Netty TCP transport: {}", config.getName());

        return CompletableFuture.runAsync(() -> {
            try {
                // 关闭服务器通道
                if (serverChannel != null) {
                    serverChannel.close().sync();
                }

                // 关闭所有客户端连接
                for (Channel channel : clientChannels.values()) {
                    if (channel.isActive()) {
                        channel.close().sync();
                    }
                }
                clientChannels.clear();

                // 关闭事件循环组
                if (workerGroup != null) {
                    workerGroup.shutdownGracefully().sync();
                }
                if (bossGroup != null) {
                    bossGroup.shutdownGracefully().sync();
                }

                logger.info("Netty TCP transport stopped successfully");
                
            } catch (Exception e) {
                logger.error("Error stopping Netty TCP transport", e);
                throw new RuntimeException("Transport stop failed", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> sendMessage(AgentMessage message) {
        if (!running.get()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Transport is not running"));
        }

        return CompletableFuture.runAsync(() -> {
            try {
                // 序列化消息
                byte[] data = serializer.serialize(message);
                ByteBuf buffer = Unpooled.wrappedBuffer(data);

                // 获取或创建到目标的连接
                String targetAddress = message.receiver().address();
                Channel channel = getOrCreateChannel(targetAddress);

                if (channel != null && channel.isActive()) {
                    // 发送消息
                    ChannelFuture future = channel.writeAndFlush(buffer);
                    future.addListener((ChannelFutureListener) f -> {
                        if (f.isSuccess()) {
                            messagesSent.incrementAndGet();
                            bytesSent.addAndGet(data.length);
                            logger.debug("Sent TCP message from {} to {}", 
                                message.sender().getShortId(), message.receiver().getShortId());
                        } else {
                            errors.incrementAndGet();
                            logger.error("Failed to send TCP message", f.cause());
                        }
                    });
                } else {
                    errors.incrementAndGet();
                    throw new RuntimeException("No active channel to target: " + targetAddress);
                }

            } catch (Exception e) {
                errors.incrementAndGet();
                logger.error("Error sending TCP message", e);
                throw new RuntimeException("Failed to send TCP message", e);
            }
        });
    }

    @Override
    public void setMessageHandler(MessageHandler handler) {
        this.messageHandler = handler;
    }

    @Override
    public String getName() {
        return config.getName();
    }

    @Override
    public TransportType getType() {
        return TransportType.TCP;
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public boolean supports(AgentId agentId) {
        // TCP传输支持远程Agent
        return !agentId.isLocal();
    }

    @Override
    public TransportConfig getConfig() {
        return config;
    }

    @Override
    public TransportStats getStats() {
        return new TransportStats(
            config.getName(),
            TransportType.TCP,
            running.get(),
            messagesSent.get(),
            messagesReceived.get(),
            bytesSent.get(),
            bytesReceived.get(),
            errors.get(),
            0.0 // TODO: 实现延迟统计
        );
    }

    /**
     * 获取或创建到目标地址的连接
     * 
     * @param targetAddress 目标地址
     * @return Channel实例
     */
    private Channel getOrCreateChannel(String targetAddress) {
        Channel channel = clientChannels.get(targetAddress);
        
        if (channel == null || !channel.isActive()) {
            try {
                // 解析目标地址
                String[] parts = targetAddress.split(":");
                String host = parts[0];
                int port = parts.length > 1 ? Integer.parseInt(parts[1]) : config.getPort();

                // 创建客户端连接
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 
                        config.getProperty("connectTimeout", 5000))
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            
                            // 添加帧解码器
                            pipeline.addLast(new LengthFieldBasedFrameDecoder(
                                Integer.MAX_VALUE, 0, 4, 0, 4));
                            pipeline.addLast(new LengthFieldPrepender(4));
                            
                            // 添加消息处理器
                            pipeline.addLast(new MessageChannelHandler());
                        }
                    });

                // 连接到目标
                ChannelFuture future = bootstrap.connect(host, port).sync();
                channel = future.channel();
                
                // 缓存连接
                clientChannels.put(targetAddress, channel);
                
                logger.debug("Created TCP connection to {}", targetAddress);
                
            } catch (Exception e) {
                logger.error("Failed to create TCP connection to {}", targetAddress, e);
                return null;
            }
        }
        
        return channel;
    }

    /**
     * Netty消息处理器
     */
    private class MessageChannelHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (msg instanceof ByteBuf buffer) {
                try {
                    // 读取消息数据
                    byte[] data = new byte[buffer.readableBytes()];
                    buffer.readBytes(data);
                    
                    // 反序列化消息
                    AgentMessage message = serializer.deserialize(data);
                    
                    // 更新统计信息
                    messagesReceived.incrementAndGet();
                    bytesReceived.addAndGet(data.length);
                    
                    // 处理消息
                    if (messageHandler != null) {
                        messageHandler.handleMessage(message);
                        logger.debug("Received TCP message: {}", message.messageId());
                    } else {
                        logger.warn("No message handler set for received message: {}", 
                            message.messageId());
                    }
                    
                } catch (SerializationException e) {
                    errors.incrementAndGet();
                    logger.error("Failed to deserialize received message", e);
                } finally {
                    buffer.release();
                }
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            // 连接断开时清理缓存
            String remoteAddress = ctx.channel().remoteAddress().toString();
            clientChannels.values().removeIf(channel -> channel == ctx.channel());
            logger.debug("TCP connection closed: {}", remoteAddress);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            errors.incrementAndGet();
            logger.error("Exception in TCP channel", cause);
            ctx.close();
        }
    }
}
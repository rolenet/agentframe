package com.agentcore.cluster.discovery;

import com.agentcore.cluster.node.ClusterNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于组播的节点发现实现
 * 
 * @author AgentCore Team
 */
public class MulticastNodeDiscovery implements NodeDiscovery {

    private static final Logger logger = LoggerFactory.getLogger(MulticastNodeDiscovery.class);

    private static final String DEFAULT_MULTICAST_GROUP = "224.0.0.251";
    private static final int DEFAULT_MULTICAST_PORT = 54321;
    private static final int DISCOVERY_INTERVAL_SECONDS = 10;

    private final String multicastGroup;
    private final int multicastPort;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;
    private final ConcurrentHashMap<String, ClusterNode> discoveredNodes;
    private final CopyOnWriteArrayList<DiscoveryListener> listeners;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private MulticastSocket socket;
    private InetAddress group;
    private ClusterNode currentNode;
    private ScheduledFuture<?> discoveryTask;
    private ScheduledFuture<?> cleanupTask;

    /**
     * 构造函数
     */
    public MulticastNodeDiscovery() {
        this(DEFAULT_MULTICAST_GROUP, DEFAULT_MULTICAST_PORT);
    }

    /**
     * 构造函数
     * 
     * @param multicastGroup 组播地址
     * @param multicastPort 组播端口
     */
    public MulticastNodeDiscovery(String multicastGroup, int multicastPort) {
        this.multicastGroup = multicastGroup;
        this.multicastPort = multicastPort;
        this.objectMapper = new ObjectMapper();
        this.scheduler = Executors.newScheduledThreadPool(2, 
            r -> new Thread(r, "NodeDiscovery-" + Thread.currentThread().getId()));
        this.discoveredNodes = new ConcurrentHashMap<>();
        this.listeners = new CopyOnWriteArrayList<>();
    }

    @Override
    public CompletableFuture<Void> start() {
        if (!running.compareAndSet(false, true)) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Node discovery is already running"));
        }

        return CompletableFuture.runAsync(() -> {
            try {
                // 创建组播套接字
                socket = new MulticastSocket(multicastPort);
                group = InetAddress.getByName(multicastGroup);
                socket.joinGroup(group);
                socket.setTimeToLive(255);

                // 启动接收线程
                startReceiveThread();

                // 启动定期发现任务
                discoveryTask = scheduler.scheduleWithFixedDelay(
                    this::sendDiscoveryMessage,
                    0, DISCOVERY_INTERVAL_SECONDS, TimeUnit.SECONDS
                );

                // 启动清理任务
                cleanupTask = scheduler.scheduleWithFixedDelay(
                    this::cleanupStaleNodes,
                    30, 30, TimeUnit.SECONDS
                );

                logger.info("Node discovery started on {}:{}", multicastGroup, multicastPort);
            } catch (Exception e) {
                running.set(false);
                logger.error("Failed to start node discovery", e);
                throw new RuntimeException("Node discovery start failed", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> stop() {
        if (!running.compareAndSet(true, false)) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                // 停止定时任务
                if (discoveryTask != null) {
                    discoveryTask.cancel(true);
                }
                if (cleanupTask != null) {
                    cleanupTask.cancel(true);
                }

                // 发送离线消息
                if (currentNode != null) {
                    sendNodeOfflineMessage(currentNode);
                }

                // 关闭套接字
                if (socket != null) {
                    socket.leaveGroup(group);
                    socket.close();
                }

                // 关闭线程池
                scheduler.shutdown();
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }

                logger.info("Node discovery stopped");
            } catch (Exception e) {
                logger.error("Error stopping node discovery", e);
                throw new RuntimeException("Node discovery stop failed", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> registerNode(ClusterNode node) {
        return CompletableFuture.runAsync(() -> {
            this.currentNode = node;
            discoveredNodes.put(node.nodeId(), node);
            logger.info("Registered current node: {}", node.nodeId());
        });
    }

    @Override
    public CompletableFuture<Void> unregisterNode(String nodeId) {
        return CompletableFuture.runAsync(() -> {
            ClusterNode node = discoveredNodes.remove(nodeId);
            if (node != null) {
                sendNodeOfflineMessage(node);
                notifyNodeLost(node);
                logger.info("Unregistered node: {}", nodeId);
            }
        });
    }

    @Override
    public CompletableFuture<List<ClusterNode>> discoverNodes() {
        return CompletableFuture.supplyAsync(() -> 
            List.copyOf(discoveredNodes.values()));
    }

    @Override
    public CompletableFuture<List<ClusterNode>> discoverNodesByType(ClusterNode.NodeType nodeType) {
        return CompletableFuture.supplyAsync(() -> 
            discoveredNodes.values().stream()
                .filter(node -> node.nodeType() == nodeType)
                .toList());
    }

    @Override
    public CompletableFuture<Boolean> isNodeOnline(String nodeId) {
        return CompletableFuture.supplyAsync(() -> {
            ClusterNode node = discoveredNodes.get(nodeId);
            if (node == null) {
                return false;
            }
            
            // 检查节点是否在超时时间内有心跳
            Instant timeout = Instant.now().minusSeconds(60);
            return node.lastHeartbeat().isAfter(timeout);
        });
    }

    @Override
    public CompletableFuture<Void> updateNode(ClusterNode node) {
        return CompletableFuture.runAsync(() -> {
            ClusterNode oldNode = discoveredNodes.put(node.nodeId(), node);
            if (oldNode != null) {
                notifyNodeUpdated(oldNode, node);
            }
            logger.debug("Updated node: {}", node.nodeId());
        });
    }

    @Override
    public void addDiscoveryListener(DiscoveryListener listener) {
        if (listener != null) {
            listeners.add(listener);
            logger.debug("Added discovery listener: {}", listener.getClass().getSimpleName());
        }
    }

    @Override
    public void removeDiscoveryListener(DiscoveryListener listener) {
        if (listener != null) {
            listeners.remove(listener);
            logger.debug("Removed discovery listener: {}", listener.getClass().getSimpleName());
        }
    }

    /**
     * 启动接收线程
     */
    private void startReceiveThread() {
        Thread receiveThread = new Thread(() -> {
            byte[] buffer = new byte[4096];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (running.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    socket.receive(packet);
                    handleReceivedMessage(packet);
                } catch (IOException e) {
                    if (running.get()) {
                        logger.error("Error receiving discovery message", e);
                    }
                }
            }
        }, "NodeDiscovery-Receiver");
        
        receiveThread.setDaemon(true);
        receiveThread.start();
    }

    /**
     * 处理接收到的消息
     */
    private void handleReceivedMessage(DatagramPacket packet) {
        try {
            String message = new String(packet.getData(), 0, packet.getLength());
            DiscoveryMessage discoveryMessage = objectMapper.readValue(message, DiscoveryMessage.class);

            switch (discoveryMessage.type()) {
                case NODE_ONLINE -> handleNodeOnlineMessage(discoveryMessage.node());
                case NODE_OFFLINE -> handleNodeOfflineMessage(discoveryMessage.node());
                case HEARTBEAT -> handleHeartbeatMessage(discoveryMessage.node());
            }
        } catch (Exception e) {
            logger.debug("Error processing discovery message", e);
        }
    }

    /**
     * 处理节点上线消息
     */
    private void handleNodeOnlineMessage(ClusterNode node) {
        if (currentNode != null && node.nodeId().equals(currentNode.nodeId())) {
            return; // 忽略自己的消息
        }

        ClusterNode existingNode = discoveredNodes.get(node.nodeId());
        if (existingNode == null) {
            discoveredNodes.put(node.nodeId(), node);
            notifyNodeDiscovered(node);
            logger.info("Discovered new node: {}", node.nodeId());
        } else {
            // 更新节点信息
            ClusterNode updatedNode = node.updateHeartbeat();
            discoveredNodes.put(node.nodeId(), updatedNode);
            notifyNodeUpdated(existingNode, updatedNode);
        }
    }

    /**
     * 处理节点离线消息
     */
    private void handleNodeOfflineMessage(ClusterNode node) {
        ClusterNode removedNode = discoveredNodes.remove(node.nodeId());
        if (removedNode != null) {
            notifyNodeLost(removedNode);
            logger.info("Node went offline: {}", node.nodeId());
        }
    }

    /**
     * 处理心跳消息
     */
    private void handleHeartbeatMessage(ClusterNode node) {
        if (currentNode != null && node.nodeId().equals(currentNode.nodeId())) {
            return; // 忽略自己的消息
        }

        ClusterNode existingNode = discoveredNodes.get(node.nodeId());
        if (existingNode != null) {
            ClusterNode updatedNode = existingNode.updateHeartbeat();
            discoveredNodes.put(node.nodeId(), updatedNode);
        }
    }

    /**
     * 发送发现消息
     */
    private void sendDiscoveryMessage() {
        if (currentNode == null) {
            return;
        }

        try {
            DiscoveryMessage message = new DiscoveryMessage(
                DiscoveryMessage.MessageType.NODE_ONLINE, 
                currentNode.updateHeartbeat()
            );
            sendMessage(message);
        } catch (Exception e) {
            logger.error("Error sending discovery message", e);
        }
    }

    /**
     * 发送节点离线消息
     */
    private void sendNodeOfflineMessage(ClusterNode node) {
        try {
            DiscoveryMessage message = new DiscoveryMessage(
                DiscoveryMessage.MessageType.NODE_OFFLINE, 
                node
            );
            sendMessage(message);
        } catch (Exception e) {
            logger.error("Error sending node offline message", e);
        }
    }

    /**
     * 发送消息
     */
    private void sendMessage(DiscoveryMessage message) throws IOException {
        String json = objectMapper.writeValueAsString(message);
        byte[] data = json.getBytes();
        DatagramPacket packet = new DatagramPacket(
            data, data.length, group, multicastPort
        );
        socket.send(packet);
    }

    /**
     * 清理过期节点
     */
    private void cleanupStaleNodes() {
        Instant timeout = Instant.now().minusSeconds(90);
        
        discoveredNodes.entrySet().removeIf(entry -> {
            ClusterNode node = entry.getValue();
            if (node.lastHeartbeat().isBefore(timeout)) {
                notifyNodeLost(node);
                logger.info("Removed stale node: {}", node.nodeId());
                return true;
            }
            return false;
        });
    }

    /**
     * 通知节点发现事件
     */
    private void notifyNodeDiscovered(ClusterNode node) {
        for (DiscoveryListener listener : listeners) {
            try {
                listener.onNodeDiscovered(node);
            } catch (Exception e) {
                logger.error("Error in node discovered listener", e);
            }
        }
    }

    /**
     * 通知节点丢失事件
     */
    private void notifyNodeLost(ClusterNode node) {
        for (DiscoveryListener listener : listeners) {
            try {
                listener.onNodeLost(node);
            } catch (Exception e) {
                logger.error("Error in node lost listener", e);
            }
        }
    }

    /**
     * 通知节点更新事件
     */
    private void notifyNodeUpdated(ClusterNode oldNode, ClusterNode newNode) {
        for (DiscoveryListener listener : listeners) {
            try {
                listener.onNodeUpdated(oldNode, newNode);
            } catch (Exception e) {
                logger.error("Error in node updated listener", e);
            }
        }
    }

    /**
     * 发现消息记录类
     */
    private record DiscoveryMessage(
        MessageType type,
        ClusterNode node
    ) {
        enum MessageType {
            NODE_ONLINE,
            NODE_OFFLINE,
            HEARTBEAT
        }
    }
}
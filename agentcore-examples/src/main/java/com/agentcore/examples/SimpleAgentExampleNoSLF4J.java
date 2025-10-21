package com.agentcore.examples;

import com.agentcore.core.agent.Agent;
import com.agentcore.core.agent.AgentId;
import com.agentcore.core.agent.AgentMetadata;
import com.agentcore.core.agent.AgentState;
import com.agentcore.core.behavior.Behavior;
import com.agentcore.core.message.AgentMessage;
import com.agentcore.container.AgentContainer;
import com.agentcore.container.ContainerConfig;
import com.agentcore.container.DefaultAgentContainer;
import com.agentcore.core.message.MessagePerformative;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 简单Agent示例演示（完全不依赖SLF4J版本）
 */
public class SimpleAgentExampleNoSLF4J {
    private static final Logger logger = LoggerFactory.getLogger(SimpleAgentExampleNoSLF4J.class);
    
    public static void main(String[] args) throws Exception {
        logger.info("=== 开始简单Agent示例演示（无SLF4J版本） ===");
        
        // 创建Agent容器配置
        ContainerConfig config = ContainerConfig.builder("simple-container")
            .maxAgents(5)
            .autoStartAgents(true)
            .build();
        AgentContainer container = new DefaultAgentContainer(config);
        
        // 创建并注册一个简单的Agent
        AgentId agentId = AgentId.create("simple-agent");
        Agent simpleAgent = new SimpleAgent(agentId);
        container.addAgent(simpleAgent);
        
        logger.info("Agent已注册到容器中");

        // 启动容器
        container.start();
        logger.info("Agent容器已启动");

        AgentMessage testMessage = AgentMessage.builder()
                .sender(simpleAgent.getAgentId())
                .receiver(null)
                .performative(MessagePerformative.INFORM)
                .content("Hello from main!")
                .build();
        // 发送测试消息
        simpleAgent.handleMessage(testMessage);
        logger.info("测试消息已发送");
        
        // 等待一段时间让Agent处理消息
        TimeUnit.SECONDS.sleep(2);
        
        // 停止容器
        container.stop();
        logger.info("Agent容器已停止");

        logger.info("=== 示例演示完成 ===");
    }
    
    /**
     * 简单的Agent实现
     */
    static class SimpleAgent implements Agent {
        private final AgentId agentId;
        private String name;
        
        public SimpleAgent(AgentId agentId) {
            this.agentId = agentId;
        }
        
        @Override
        public void init() {
            this.name = this.getAgentId().name();
            System.out.println("Agent " + name + " 已初始化");
        }
        
        @Override
        public String getName() {
            return name;
        }

        @Override
        public AgentMetadata getMetadata() {
            return null;
        }

        @Override
        public void setMetadata(AgentMetadata metadata) {

        }

        @Override
        public void addBehavior(Behavior behavior) {
            System.out.println("Agent " + name + " 添加行为: " + behavior.getName());
        }

        @Override
        public void removeBehavior(Behavior behavior) {
            System.out.println("Agent " + name + " 移除行为: " + behavior.getName());
        }

        @Override
        public CompletableFuture<Void> sendMessage(AgentMessage message) {
            System.out.println("Agent " + name + " 收到消息: " + message.content());
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public AgentMessage sendAndWait(AgentMessage message, long timeoutMs) {
            return null;
        }

        @Override
        public void handleMessage(AgentMessage message) {
            System.out.println("Agent " + name + " 收到消息: " + message.content());
        }

        @Override
        public AgentId getAgentId() {
            return agentId;
        }

        @Override
        public AgentState getState() {
            return AgentState.ACTIVE;
        }

        @Override
        public void start() {
            System.out.println("Agent " + name + " 已启动");
        }

        @Override
        public void suspend() {

        }

        @Override
        public void resume() {

        }

        @Override
        public void stop() {
            System.out.println("Agent " + name + " 已停止");
        }

        @Override
        public void destroy() {

        }
    }
}
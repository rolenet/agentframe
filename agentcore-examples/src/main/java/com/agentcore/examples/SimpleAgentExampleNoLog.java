package com.agentcore.examples;

import com.agentcore.core.agent.AbstractAgent;
import com.agentcore.core.agent.AgentId;
import com.agentcore.core.behavior.BehaviorScheduler;
import com.agentcore.core.behavior.DefaultBehaviorScheduler;
import com.agentcore.core.message.AgentMessage;
import com.agentcore.core.message.MessagePerformative;

import java.util.concurrent.CompletableFuture;

/**
 * 简单的Agent示例（无日志版本）
 * 演示AgentCore框架的基本使用
 * 
 * @author AgentCore Team
 */
public class SimpleAgentExampleNoLog {

    public static void main(String[] args) {
        System.out.println("=== 开始简单Agent示例演示（无日志版本） ===");

        try {
            // 创建两个简单的Agent进行交互演示
            AgentId agent1Id = AgentId.create("SimpleAgent-1");
            AgentId agent2Id = AgentId.create("SimpleAgent-2");
            
            SimpleAgent agent1 = new SimpleAgent(agent1Id);
            SimpleAgent agent2 = new SimpleAgent(agent2Id);

            agent1.init();
            agent2.init();
            // 启动Agent
            agent1.start();
            agent2.start();
            System.out.println("Agent已启动: " + agent1Id.getShortId() + " 和 " + agent2Id.getShortId());

            // 让Agent运行一段时间，演示消息发送
            Thread.sleep(3000);
            
            // 演示Agent之间的消息发送
            System.out.println("=== 开始消息交互演示 ===");
            sendDemoMessages(agent1, agent2);

            // 让Agent继续运行一段时间
            Thread.sleep(7000);

            // 停止Agent
            agent1.stop();
            agent2.stop();
            System.out.println("Agent已停止: " + agent1Id.getShortId() + " 和 " + agent2Id.getShortId());

        } catch (Exception e) {
            System.err.println("简单Agent示例演示过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("=== 简单Agent示例演示完成 ===");
    }

    /**
     * 演示消息发送功能
     */
    private static void sendDemoMessages(SimpleAgent sender, SimpleAgent receiver) {
        try {
            // 发送几条演示消息
            for (int i = 1; i <= 3; i++) {
                String messageContent = String.format("演示消息 #%d - 来自 %s", 
                    i, sender.getAgentId().getShortId());
                AgentMessage message = AgentMessage.builder()
                    .sender(sender.getAgentId())
                    .receiver(receiver.getAgentId())
                    .performative(MessagePerformative.INFORM)
                    .content(messageContent)
                    .build();
                sender.sendMessage(message);
                System.out.println("发送消息: " + messageContent);
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("消息发送演示被中断");
        }
    }

    /**
     * 简单的Agent实现（无日志版本）
     */
    static class SimpleAgent extends AbstractAgent {

        public SimpleAgent(AgentId agentId) {
            super(agentId);
        }

        @Override
        protected BehaviorScheduler createBehaviorScheduler() {
            return new DefaultBehaviorScheduler();
        }

        @Override
        protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
            System.out.println("发送消息: " + message);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected void doStart() {
            System.out.println("SimpleAgent " + getAgentId().getShortId() + " 正在启动...");
        }

        @Override
        protected void doStop() {
            System.out.println("SimpleAgent " + getAgentId().getShortId() + " 正在停止...");
        }

        @Override
        protected void doHandleMessage(AgentMessage message) {
            System.out.println("SimpleAgent " + getAgentId().getShortId() + " 收到消息: " + message.content());
        }
    }
}
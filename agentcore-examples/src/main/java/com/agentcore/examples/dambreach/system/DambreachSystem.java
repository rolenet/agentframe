package com.agentcore.examples.dambreach.system;

import com.agentcore.communication.router.MessageRouter;
import com.agentcore.communication.router.LocalMessageRouter;
import com.agentcore.container.AgentContainer;
import com.agentcore.container.DefaultAgentContainer;
import com.agentcore.core.agent.AgentId;
import com.agentcore.core.message.AgentMessage;
import com.agentcore.core.message.MessagePerformative;
import com.agentcore.examples.dambreach.agents.DambreachCoordinatorAgent;
import com.agentcore.examples.dambreach.agents.DambreachInputAgent;

/**
 * Dambreach 系统入口（消息触发版）
 * - 使用容器与本地路由装配智能体
 * - 由 Coordinator 发送 PARSE 指令到 Input，Input 解析后将 INIT 回路由给 Coordinator
 * - Coordinator 并行情景运行并输出 CSV
 */
public final class DambreachSystem {

  public static void main(String[] args) {
    String resveId = args.length > 0 ? args[0] : "43038140011";
    String baseDir = args.length > 1
        ? args[1]
        : "agentcore-examples/src/main/java/com/agentcore/examples/dambreach";

    // 1) 创建容器（内部自带 LocalMessageRouter）
    AgentContainer container = new DefaultAgentContainer("Dambreach");

    // 2) 创建智能体并加入容器
    DambreachInputAgent input = new DambreachInputAgent(AgentId.create("Input"));
    DambreachCoordinatorAgent coord = new DambreachCoordinatorAgent(AgentId.create("Coordinator"));
    container.addAgent(input);
    container.addAgent(coord);

    // 3) 注入消息路由
    MessageRouter router = container.getMessageRouter();
    input.setMessageRouter(router);
    coord.setMessageRouter(router);

    // 4) 启动容器与智能体（确保可接收消息）
    try {
      container.start().join();
    } catch (Exception e) {
      System.err.println("[System] 启动失败: " + e.getMessage());
      e.printStackTrace();
      System.exit(2);
    }

    // 5) 使用消息触发：Coordinator -> Input 发送 PARSE 指令
    //    Input 收到后解析并路由 INIT 到 Coordinator，Coordinator 开始并行情景
    try {
      String parseCmd = "PARSE:" + resveId + "|" + baseDir;
      router.routeMessage(
        AgentMessage.builder()
          .sender(coord.getAgentId())
          .receiver(input.getAgentId())
          .performative(MessagePerformative.REQUEST)
          .content(parseCmd)
          .build()
      ).join();

      // 简单等待一段时间，给并行情景运行与CSV输出留出时间
      // 如需更强的退出条件，可在协调智能体内增加完成信号并在此等待。
      Thread.sleep(30_000L); // 30秒快速回归等待
    } catch (Exception e) {
      System.err.println("[System] 触发消息流程异常: " + e.getMessage());
      e.printStackTrace();
    } finally {
      try {
        container.stop().join();
      } catch (Exception ignore) {}
    }
  }
}
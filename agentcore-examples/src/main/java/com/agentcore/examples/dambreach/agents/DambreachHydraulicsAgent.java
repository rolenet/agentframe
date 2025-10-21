package com.agentcore.examples.dambreach.agents;

import com.agentcore.communication.router.MessageRouter;
import com.agentcore.core.agent.AbstractAgent;
import com.agentcore.core.agent.AgentId;
import com.agentcore.core.behavior.BehaviorScheduler;
import com.agentcore.core.behavior.DefaultBehaviorScheduler;
import com.agentcore.core.message.AgentMessage;
import com.agentcore.core.message.MessagePerformative;
import com.agentcore.examples.dambreach.domain.Dtos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class DambreachHydraulicsAgent extends AbstractAgent {
  private static final Logger logger = LoggerFactory.getLogger(DambreachHydraulicsAgent.class);

  private MessageRouter messageRouter;
  // 复用已存在的领域服务
  private com.agentcore.examples.dambreach.agents.HydraulicsAgent hydDomain;

  public DambreachHydraulicsAgent(AgentId agentId) {
    super(agentId);
  }

  public void setMessageRouter(MessageRouter router) {
    this.messageRouter = router;
  }

  @Override
  protected BehaviorScheduler createBehaviorScheduler() {
    return new DefaultBehaviorScheduler();
  }

  @Override
  protected void doStart() {
    logger.info("🌊 DambreachHydraulicsAgent 启动");
  }

  @Override
  protected void doStop() {
    logger.info("🌊 DambreachHydraulicsAgent 停止");
  }

  @Override
  protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
    if (messageRouter != null) return messageRouter.routeMessage(message);
    return CompletableFuture.failedFuture(new IllegalStateException("MessageRouter未设置"));
  }

  @Override
  protected void doHandleMessage(AgentMessage message) {
    Object content = message.content();
    if (content instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> cmd = (Map<String, Object>) content;
      String command = String.valueOf(cmd.get("command"));
      switch (command) {
        case "INIT_HYD" -> {
          DamGeometry geom = (DamGeometry) cmd.get("geom");
          BreachConfig cfg = (BreachConfig) cmd.get("cfg");
          SedimentProps sed = (SedimentProps) cmd.get("sed");
          DownstreamChannel dwn = (DownstreamChannel) cmd.get("dwn");
          Curves curves = (Curves) cmd.get("curves");
          RunConfig run = (RunConfig) cmd.get("run");
          hydDomain = new com.agentcore.examples.dambreach.agents.HydraulicsAgent(geom, cfg, sed, dwn, curves, run);
          ack((AgentId) cmd.get("coordinatorId"), "INIT_HYD_OK");
        }
        case "HYD_INIT_STATE" -> {
          HydraulicsState hs = (HydraulicsState) cmd.get("hs");
          BreachState bs = (BreachState) cmd.get("bs");
          if (hydDomain != null) hydDomain.init(hs, bs);
          ack((AgentId) cmd.get("coordinatorId"), "HYD_INIT_STATE_OK");
        }
        case "HYD_STEP" -> {
          HydraulicsState hs = (HydraulicsState) cmd.get("hs");
          BreachState bs = (BreachState) cmd.get("bs");
          if (hydDomain != null) hydDomain.step(hs, bs);
          ack((AgentId) cmd.get("coordinatorId"), "HYD_STEP_OK");
        }
        default -> logger.info("🌊 未知命令: {}", command);
      }
    } else {
      logger.info("🌊 水力智能体收到其他消息: {}", content);
    }
  }

  private void ack(AgentId coordinatorId, String info) {
    AgentMessage msg = AgentMessage.builder()
        .sender(getAgentId())
        .receiver(coordinatorId)
        .performative(MessagePerformative.INFORM)
        .content(Map.of("ack", info))
        .build();
    sendMessage(msg);
  }
}
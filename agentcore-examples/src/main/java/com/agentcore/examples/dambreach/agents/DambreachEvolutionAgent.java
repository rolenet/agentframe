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

public final class DambreachEvolutionAgent extends AbstractAgent {
  private static final Logger logger = LoggerFactory.getLogger(DambreachEvolutionAgent.class);

  private MessageRouter messageRouter;
  // 复用已存在的领域服务
  private com.agentcore.examples.dambreach.agents.BreachEvolutionAgent evoDomain;

  public DambreachEvolutionAgent(AgentId agentId) {
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
    logger.info("🧱 DambreachEvolutionAgent 启动");
  }

  @Override
  protected void doStop() {
    logger.info("🧱 DambreachEvolutionAgent 停止");
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
        case "EVO_INIT" -> {
          DamGeometry geom = (DamGeometry) cmd.get("geom");
          BreachConfig cfg = (BreachConfig) cmd.get("cfg");
          SedimentProps sed = (SedimentProps) cmd.get("sed");
          DownstreamChannel dwn = (DownstreamChannel) cmd.get("dwn");
          CoverProps cover = (CoverProps) cmd.get("cover");
          CoreProps core = (CoreProps) cmd.get("core");
          evoDomain = new com.agentcore.examples.dambreach.agents.BreachEvolutionAgent(geom, cfg, sed, dwn, cover, core);
          ack((AgentId) cmd.get("coordinatorId"), "EVO_INIT_OK");
        }
        case "EVO_INIT_STATE" -> {
          BreachState bs = (BreachState) cmd.get("bs");
          if (evoDomain != null) evoDomain.init(bs);
          ack((AgentId) cmd.get("coordinatorId"), "EVO_INIT_STATE_OK");
        }
        case "EVO_STEP" -> {
          HydraulicsState hs = (HydraulicsState) cmd.get("hs");
          BreachState bs = (BreachState) cmd.get("bs");
          double dt = (double) cmd.getOrDefault("dt", 1.0);
          if (evoDomain != null) evoDomain.evolve(hs, bs, dt);
          ack((AgentId) cmd.get("coordinatorId"), "EVO_STEP_OK");
        }
        default -> logger.info("🧱 未知命令: {}", command);
      }
    } else {
      logger.info("🧱 演化智能体收到其他消息: {}", content);
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
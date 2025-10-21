package com.agentcore.examples.dambreach.agents;

import com.agentcore.communication.router.MessageRouter;
import com.agentcore.core.agent.AbstractAgent;
import com.agentcore.core.agent.AgentId;
import com.agentcore.core.behavior.BehaviorScheduler;
import com.agentcore.core.behavior.DefaultBehaviorScheduler;
import com.agentcore.core.message.AgentMessage;
import com.agentcore.core.message.MessagePerformative;
import com.agentcore.examples.dambreach.io.InputAgent;
import com.agentcore.examples.dambreach.messaging.DambreachMessage;
import com.agentcore.examples.dambreach.messaging.DambreachMessage.InitPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public final class DambreachInputAgent extends AbstractAgent {
  private static final Logger log = LoggerFactory.getLogger(DambreachInputAgent.class);
  private MessageRouter router;

  public DambreachInputAgent(AgentId id) {
    super(id);
  }

  public void setMessageRouter(MessageRouter router) {
    this.router = router;
  }

  // 提供直接解析为InitPayload的公共方法，便于系统直接调用或测试
  public InitPayload parseForInit(String resveId, String baseDir) {
    InputAgent.InputBundle ib = InputAgent.parse(baseDir, resveId);
    return new InitPayload(ib, resveId);
  }

  @Override
  protected BehaviorScheduler createBehaviorScheduler() {
    return new DefaultBehaviorScheduler();
  }

  @Override
  protected void doStart() {
    log.info("📥 DambreachInputAgent 启动");
  }

  @Override
  protected void doStop() {
    log.info("📥 DambreachInputAgent 停止");
  }

  @Override
  protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
    if (router != null) return router.routeMessage(message);
    return CompletableFuture.failedFuture(new IllegalStateException("MessageRouter未设置"));
  }

  @Override
  protected void doHandleMessage(AgentMessage message) {
    Object content = message.content();
    if (content instanceof String s && s.startsWith("PARSE:")) {
      // 形如 "PARSE:RESVE_ID|BASE_DIR"
      String[] parts = s.substring("PARSE:".length()).split("\\|", 2);
      String resveId = parts.length > 0 && !parts[0].isBlank() ? parts[0] : "43038140011";
      String baseDir = parts.length > 1 && !parts[1].isBlank()
          ? parts[1]
          : "agentcore-examples/src/main/java/com/agentcore/examples/dambreach";
      log.info("📥 解析输入 resveId={} baseDir={}", resveId, baseDir);
      InputAgent.InputBundle ib = InputAgent.parse(baseDir, resveId);
      DambreachMessage init = new DambreachMessage(DambreachMessage.Stage.INIT, new InitPayload(ib, resveId));
      if (router != null) {
        router.routeMessage(
          AgentMessage.builder()
            .sender(getAgentId())
            .receiver(message.sender())
            .performative(MessagePerformative.INFORM)
            .content(init)
            .build()
        );
      }
    } else {
      log.info("📥 输入智能体收到其他消息: {}", content);
    }
  }
}
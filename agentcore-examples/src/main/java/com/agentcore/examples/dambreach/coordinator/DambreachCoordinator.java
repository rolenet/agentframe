package com.agentcore.examples.dambreach.coordinator;

import com.agentcore.communication.router.MessageRouter;
import com.agentcore.core.agent.AbstractAgent;
import com.agentcore.core.agent.AgentId;
import com.agentcore.core.behavior.BehaviorScheduler;
import com.agentcore.core.behavior.DefaultBehaviorScheduler;
import com.agentcore.core.message.AgentMessage;
import com.agentcore.core.message.MessagePerformative;
import com.agentcore.examples.dambreach.messaging.DambreachMessage;
import com.agentcore.examples.dambreach.domain.Dtos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public final class DambreachCoordinator extends AbstractAgent {

  private static final Logger logger = LoggerFactory.getLogger(DambreachCoordinator.class);

  private MessageRouter messageRouter;
  private AgentId reportAgentId;
  private AgentId inputAgentId;
  private AgentId hydAgentId;
  private AgentId evoAgentId;

  public DambreachCoordinator(AgentId agentId) {
    super(agentId);
  }

  @Override
  protected BehaviorScheduler createBehaviorScheduler() {
    return new DefaultBehaviorScheduler();
  }

  public void setMessageRouter(MessageRouter router) {
    this.messageRouter = router;
  }

  public void setReportAgentId(AgentId reportAgentId) {
    this.reportAgentId = reportAgentId;
  }

  public void setInputAgentId(AgentId inputAgentId) {
    this.inputAgentId = inputAgentId;
  }

  public void setHydAgentId(AgentId hydAgentId) {
    this.hydAgentId = hydAgentId;
  }

  public void setEvoAgentId(AgentId evoAgentId) {
    this.evoAgentId = evoAgentId;
  }

  @Override
  protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
    if (messageRouter != null) {
      return messageRouter.routeMessage(message);
    }
    return CompletableFuture.failedFuture(new IllegalStateException("MessageRouter未设置"));
  }

  @Override
  protected void doStart() {
    logger.info("🎯 DambreachCoordinator 启动");
  }

  @Override
  protected void doStop() {
    logger.info("🎯 DambreachCoordinator 停止");
  }

  @Override
  protected void doHandleMessage(AgentMessage message) {
    Object content = message.content();
    if (content instanceof DambreachMessage dm) {
      if (dm.getStage() == DambreachMessage.Stage.INIT) {
        logger.info("🎯 收到INPUT_PARSED，开始初始化与时间步推进");
        handleInputParsed(dm);
      }
    } else {
      logger.info("🎯 协调器收到其他消息: {}", content);
    }
  }

  private void handleInputParsed(DambreachMessage dm) {
    @SuppressWarnings("unchecked")
    java.util.Map<String, Object> payload = (java.util.Map<String, Object>) dm.getPayload();
    Object ob = payload.get("bundle");
    if (!(ob instanceof com.agentcore.examples.dambreach.io.InputAgent.InputBundle ib)) {
      logger.error("bundle 缺失或类型不匹配");
      return;
    }
    var geom = ib.geom();
    var cfg = ib.breachCfg();
    var sed = ib.sed();
    var dwn = ib.dwn();
    var cover = ib.cover();
    var core = ib.core();
    var curves = ib.curves();
    var run = ib.run();

    // 创建状态
    HydraulicsState hs = new HydraulicsState();
    BreachState bs = new BreachState();

    // 初始化Agents
    sendMessage(AgentMessage.builder().sender(getAgentId()).receiver(hydAgentId).performative(MessagePerformative.REQUEST)
        .content(java.util.Map.of("command","INIT_HYD","geom",geom,"cfg",cfg,"sed",sed,"dwn",dwn,"curves",curves,"run",run,"coordinatorId",getAgentId()))
        .build());
    sendMessage(AgentMessage.builder().sender(getAgentId()).receiver(evoAgentId).performative(MessagePerformative.REQUEST)
        .content(java.util.Map.of("command","EVO_INIT","geom",geom,"cfg",cfg,"sed",sed,"dwn",dwn,"cover",cover,"core",core,"coordinatorId",getAgentId()))
        .build());
    // 初始化状态
    sendMessage(AgentMessage.builder().sender(getAgentId()).receiver(hydAgentId).performative(MessagePerformative.REQUEST)
        .content(java.util.Map.of("command","HYD_INIT_STATE","hs",hs,"bs",bs,"coordinatorId",getAgentId()))
        .build());
    sendMessage(AgentMessage.builder().sender(getAgentId()).receiver(evoAgentId).performative(MessagePerformative.REQUEST)
        .content(java.util.Map.of("command","EVO_INIT_STATE","bs",bs,"coordinatorId",getAgentId()))
        .build());

    double eps = 1e-5;
    int it = 1;
    double end = run.timeEnd();
    while (hs.time <= end) {
      double err = 1.0, errZ = 1.0;
      int numItr = 0;
      while (((err > eps) || (errZ > eps)) && numItr < 100) {
        var m1 = AgentMessage.builder().sender(getAgentId()).receiver(hydAgentId).performative(MessagePerformative.REQUEST)
            .content(java.util.Map.of("command","HYD_STEP","hs",hs,"bs",bs,"coordinatorId",getAgentId())).build();
        sendMessage(m1);
        err = Math.abs(hs.flowBreach1 - hs.flowBreach0);
        errZ = Math.abs(hs.zsReserv1 - hs.zsReserv0);
        numItr++;
      }
      var m2 = AgentMessage.builder().sender(getAgentId()).receiver(evoAgentId).performative(MessagePerformative.REQUEST)
          .content(java.util.Map.of("command","EVO_STEP","hs",hs,"bs",bs,"dt",run.dt(),"coordinatorId",getAgentId())).build();
      sendMessage(m2);

      hs.time += run.dt();
      hs.zsReserv0 = hs.zsReserv1;
      hs.zsDownStrm0 = hs.zsDownStrm1;
      hs.flowBreach0 = hs.flowBreach1;

      if (reportAgentId != null) {
        DambreachMessage stepMsg = new DambreachMessage(
            DambreachMessage.Stage.REPORT_STEP,
            java.util.Map.of("t", hs.time, "Qb", hs.flowBreach, "Zs", hs.zsReservM, "Zd", hs.zDownChan)
        );
        sendMessage(AgentMessage.builder()
            .sender(getAgentId())
            .receiver(reportAgentId)
            .performative(MessagePerformative.INFORM)
            .content(stepMsg)
            .build());
      }
      it++;
      if (it > 20) break;
    }

    if (reportAgentId != null) {
      DambreachMessage done = new DambreachMessage(
          DambreachMessage.Stage.DONE,
          java.util.Map.of("status","done","steps",it-1)
      );
      sendMessage(AgentMessage.builder()
          .sender(getAgentId())
          .receiver(reportAgentId)
          .performative(MessagePerformative.INFORM)
          .content(done)
          .build());
    }
  }

  public void startSystem() {
    logger.info("🎯 启动Dambreach流程：请求输入解析");
    if (inputAgentId == null) {
      logger.error("未设置InputAgentId");
      return;
    }
    AgentMessage startInput = AgentMessage.builder()
        .sender(getAgentId())
        .receiver(inputAgentId)
        .performative(MessagePerformative.REQUEST)
        .content(java.util.Map.of(
            "command", "START_INPUT",
            "resveId", "43038140011",
            "baseDir", "agentcore-examples/src/main/java/com/agentcore/examples/dambreach",
            "coordinatorId", getAgentId()
        ))
        .build();
    sendMessage(startInput);
  }
}
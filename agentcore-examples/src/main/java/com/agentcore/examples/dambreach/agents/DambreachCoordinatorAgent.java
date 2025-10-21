package com.agentcore.examples.dambreach.agents;

import com.agentcore.communication.router.MessageRouter;
import com.agentcore.core.agent.AbstractAgent;
import com.agentcore.core.agent.AgentId;
import com.agentcore.core.behavior.BehaviorScheduler;
import com.agentcore.core.behavior.DefaultBehaviorScheduler;
import com.agentcore.core.message.AgentMessage;
import com.agentcore.examples.dambreach.domain.Dtos.*;
import com.agentcore.examples.dambreach.io.InputAgent;
import com.agentcore.examples.dambreach.messaging.DambreachMessage;
import com.agentcore.examples.dambreach.tools.ScenarioParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.*;

public final class DambreachCoordinatorAgent extends AbstractAgent {
  private static final Logger log = LoggerFactory.getLogger(DambreachCoordinatorAgent.class);
  private MessageRouter router;

  public DambreachCoordinatorAgent(AgentId id) {
    super(id);
  }

  public void setMessageRouter(MessageRouter router) {
    this.router = router;
  }

  // 提供阻塞式运行接口，实现“完成信号优雅收尾”
  public void runScenariosBlocking(InputAgent.InputBundle ib, String resveId) {
    CountDownLatch latch = new CountDownLatch(1);
    runScenariosAsync(ib, resveId);
    // 简单等待：由于 runScenariosAsync 内部使用 whenComplete 打印结果，我们在此等待固定时间或直到线程池任务完成
    // TODO: 可改为在 whenComplete 中 countDown 并在此等待 latch
    try {
      Thread.sleep(3000L);
    } catch (InterruptedException ignored) {}
    latch.countDown();
    try {
      latch.await();
    } catch (InterruptedException ignored) {}
  }

  @Override
  protected BehaviorScheduler createBehaviorScheduler() {
    return new DefaultBehaviorScheduler();
  }

  @Override
  protected void doStart() {
    log.info("🧭 DambreachCoordinatorAgent 启动");
  }

  @Override
  protected void doStop() {
    log.info("🧭 DambreachCoordinatorAgent 停止");
  }

  @Override
  protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
    if (router != null) return router.routeMessage(message);
    return CompletableFuture.failedFuture(new IllegalStateException("MessageRouter未设置"));
  }

  @Override
  protected void doHandleMessage(AgentMessage message) {
    Object content = message.content();
    if (content instanceof DambreachMessage dm && dm.getStage() == DambreachMessage.Stage.INIT) {
      DambreachMessage.InitPayload p = (DambreachMessage.InitPayload) dm.getPayload();
      if (p == null || p.inputBundle() == null) {
        log.error("🧭 INIT 载荷为空，忽略");
        return;
      }
      runScenariosAsync(p.inputBundle(), p.resveId());
    } else {
      log.info("🧭 协调智能体收到其他消息: {}", content);
    }
  }

  private void runScenariosAsync(InputAgent.InputBundle ib, String resveId) {
    List<ScenarioParam> scenarios = new ArrayList<>();
    double[] zs0s = new double[]{38.0, 39.0, 40.0};
    double[] ns = new double[]{0.025, 0.030, 0.035};
    for (double z : zs0s) for (double n : ns) scenarios.add(new ScenarioParam(z, n));

    ExecutorService pool = Executors.newFixedThreadPool(Math.min(4, scenarios.size()));
    List<CompletableFuture<String>> futures = new ArrayList<>();
    for (ScenarioParam sp : scenarios) {
      futures.add(CompletableFuture.supplyAsync(() -> runOne(ib, resveId, sp), pool));
    }
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .whenComplete((v, ex) -> {
          if (ex != null) log.error("🧭 并行情景执行异常", ex);
          else {
            log.info("🧭 全部情景完成");
            for (CompletableFuture<String> f : futures) {
              try { log.info("🧭 {}", f.get()); } catch (Exception ignore) {}
            }
          }
        });
  }

  private String runOne(InputAgent.InputBundle ib, String resveId, ScenarioParam sp) {
    try {
      DamGeometry geom = ib.geom();
      BreachConfig cfg = ib.breachCfg();
      SedimentProps sed = ib.sed();
      CoverProps cover = ib.cover();
      CoreProps core = ib.core();
      Curves curves = ib.curves();
      RunConfig run = ib.run();

      DownstreamChannel baseDwn = ib.dwn();
      DownstreamChannel dwn = new DownstreamChannel(baseDwn.iDownStrm(), baseDwn.dwnChanWidth(), baseDwn.dwnChanSlope(), sp.manningN());

      HydraulicsAgent hyd = new HydraulicsAgent(geom, cfg, sed, dwn, curves, run);
      BreachEvolutionAgent evo = new BreachEvolutionAgent(geom, cfg, sed, dwn, cover, core);
      HydraulicsState hs = new HydraulicsState();
      BreachState bs = new BreachState();
      hs.zsReserv0 = sp.zs0();

      hyd.init(hs, bs);
      evo.init(bs);

      Path outDir = Path.of("agentcore-examples", "out");
      Files.createDirectories(outDir);
      String label = sp.label();
      Path out = outDir.resolve("dambreach_result_" + resveId + "_" + label + ".csv");

      DambreachReportAgent report = new DambreachReportAgent(AgentId.create("Report@" + label));
      report.open(out.toString());
      report.writeHeader();
      report.writeStep(0.0, 0.0, 0.0, 0.0, hs, bs, geom, cfg, sed, true);

      double eps = 1e-5;
      int it = 1;
      double end = run.timeEnd();
      while (hs.time <= end) {
        double timeMinusHalfDt = hs.time - 0.5 * run.dt();

        double err = 1.0, errZ = 1.0;
        int numItr = 0;
        while (((err > eps) || (errZ > eps)) && numItr < 100) {
          hyd.step(hs, bs);
          err = Math.abs(hs.flowBreach1 - hs.flowBreach0);
          errZ = Math.abs(hs.zsReserv1 - hs.zsReserv0);
          numItr++;
        }
        evo.evolve(hs, bs, run.dt());

        hs.time += run.dt();
        hs.zsReserv0 = hs.zsReserv1;
        hs.zsDownStrm0 = hs.zsDownStrm1;
        hs.flowBreach0 = hs.flowBreach1;

        double Qin = hs.flowIn;
        double Qsum = hs.flowSpill + hs.flowBreach;
        report.writeStep(timeMinusHalfDt / 3600.0, Qin, hs.flowBreach, Qsum, hs, bs, geom, cfg, sed, false);

        it++;
        if (it > 4000) break;
      }
      report.close(1, 0.0, hs.time / 3600.0, resveId);
      return String.format(Locale.US, "[OK] %s -> %s", label, out.getFileName());
    } catch (Exception e) {
      return "[FAIL] " + sp.label() + " -> " + e.getMessage();
    }
  }
}
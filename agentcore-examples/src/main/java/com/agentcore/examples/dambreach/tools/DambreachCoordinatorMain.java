package com.agentcore.examples.dambreach.tools;

import com.agentcore.core.agent.AgentId;
import com.agentcore.examples.dambreach.agents.BreachEvolutionAgent;
import com.agentcore.examples.dambreach.agents.DambreachReportAgent;
import com.agentcore.examples.dambreach.agents.HydraulicsAgent;
import com.agentcore.examples.dambreach.domain.Dtos.*;
import com.agentcore.examples.dambreach.io.InputAgent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.*;

public final class DambreachCoordinatorMain {

  public static void main(String[] args) throws Exception {
    String resveId = args.length > 0 ? args[0] : "43038140011";
    String baseDir = args.length > 1 ? args[1] : "agentcore-examples/src/main/java/com/agentcore/examples/dambreach";

    InputAgent.InputBundle ib = InputAgent.parse(baseDir, resveId);
    if (ib == null) {
      System.err.println("[CoordinatorMain] parse failed");
      System.exit(2);
    }

    // 构造情景矩阵：Zs0 × n（可按需调整或从命令行读取）
    List<ScenarioParam> scenarios = new ArrayList<>();
    double[] zs0s = new double[]{38.0, 39.0, 40.0};
    double[] ns = new double[]{0.025, 0.030, 0.035};
    for (double z : zs0s) for (double n : ns) scenarios.add(new ScenarioParam(z, n));

    ExecutorService pool = Executors.newFixedThreadPool(Math.min(4, scenarios.size()));
    List<CompletableFuture<String>> futures = new ArrayList<>();

    for (ScenarioParam sp : scenarios) {
      futures.add(CompletableFuture.supplyAsync(() -> {
        try {
          return runOneScenario(ib, resveId, sp);
        } catch (Exception e) {
          e.printStackTrace();
          return "[FAIL] " + sp.label() + " -> " + e.getMessage();
        }
      }, pool));
    }

    // 等待全部完成
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    pool.shutdown();

    System.out.println("[CoordinatorMain] All scenarios completed:");
    for (CompletableFuture<String> f : futures) {
      System.out.println("  " + f.get());
    }
  }

  private static String runOneScenario(InputAgent.InputBundle ib, String resveId, ScenarioParam sp) throws Exception {
    DamGeometry geom = ib.geom();
    BreachConfig cfg = ib.breachCfg();
    SedimentProps sed = ib.sed();
    CoverProps cover = ib.cover();
    CoreProps core = ib.core();
    Curves curves = ib.curves();
    RunConfig run = ib.run();

    // 按情景覆盖：初始库水位与下游糙率
    DownstreamChannel baseDwn = ib.dwn();
    DownstreamChannel dwn = new DownstreamChannel(baseDwn.iDownStrm(), baseDwn.dwnChanWidth(), baseDwn.dwnChanSlope(), sp.manningN());

    HydraulicsAgent hyd = new HydraulicsAgent(geom, cfg, sed, dwn, curves, run);
    BreachEvolutionAgent evo = new BreachEvolutionAgent(geom, cfg, sed, dwn, cover, core);
    HydraulicsState hs = new HydraulicsState();
    BreachState bs = new BreachState();

    // 初始水位
    hs.zsReserv0 = sp.zs0();

    // 初始化
    hyd.init(hs, bs);
    evo.init(bs);

    Path outDir = Path.of("agentcore-examples", "out");
    Files.createDirectories(outDir);
    String label = sp.label();
    Path out = outDir.resolve("dambreach_result_" + resveId + "_" + label + ".csv");

    DambreachReportAgent report = new DambreachReportAgent(AgentId.create("Report@" + label));
    report.open(out.toString());
    report.writeHeader();

    // 初始行
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

      // 推进一步时间并复制状态
      hs.time += run.dt();
      hs.zsReserv0 = hs.zsReserv1;
      hs.zsDownStrm0 = hs.zsDownStrm1;
      hs.flowBreach0 = hs.flowBreach1;

      // 写一行
      double Qin = hs.flowIn;
      double Qsum = hs.flowSpill + hs.flowBreach;
      report.writeStep(timeMinusHalfDt / 3600.0, Qin, hs.flowBreach, Qsum, hs, bs, geom, cfg, sed, false);

      it++;
      // 为演示与快速完成，限定步数（可按需移除）
      if (it > 4000) break;
    }

    report.close(1, 0.0, hs.time / 3600.0, resveId);
    return String.format(Locale.US, "[OK] %s -> %s", label, out.getFileName().toString());
  }
}
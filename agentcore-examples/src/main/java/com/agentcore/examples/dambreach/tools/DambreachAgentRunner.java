package com.agentcore.examples.dambreach.tools;

import com.agentcore.examples.dambreach.agents.BreachEvolutionAgent;
import com.agentcore.core.agent.AgentId;
import com.agentcore.examples.dambreach.agents.HydraulicsAgent;
import com.agentcore.examples.dambreach.agents.DambreachReportAgent;
import com.agentcore.examples.dambreach.alg.DbAlgorithms;
import com.agentcore.examples.dambreach.domain.Dtos.*;
import com.agentcore.examples.dambreach.io.InputAgent;

import java.nio.file.Files;
import java.nio.file.Path;

public final class DambreachAgentRunner {

  public static void main(String[] args) throws Exception {
    String resveId = args.length > 0 ? args[0] : "43038140011";
    double zs0 = args.length > 1 ? Double.parseDouble(args[1]) : 38.0;
    double n = args.length > 2 ? Double.parseDouble(args[2]) : 0.030;
    String baseDir = "agentcore-examples/src/main/java/com/agentcore/examples/dambreach";

    System.out.println("[Runner] parse input...");
    InputAgent.InputBundle ib = InputAgent.parse(baseDir, resveId);
    if (ib == null) {
      System.err.println("[Runner] parse failed");
      return;
    }

    DamGeometry geom = ib.geom();
    BreachConfig cfg = ib.breachCfg();
    SedimentProps sed = ib.sed();
    DownstreamChannel baseDwn = ib.dwn();
    DownstreamChannel dwn = new DownstreamChannel(baseDwn.iDownStrm(), baseDwn.dwnChanWidth(), baseDwn.dwnChanSlope(), n);
    CoverProps cover = ib.cover();
    CoreProps core = ib.core();
    Curves curves = ib.curves();
    RunConfig run = ib.run();
    run = new RunConfig(1.0, 0.0, 120.0, run.relax(), run.resveId());

    HydraulicsAgent hyd = new HydraulicsAgent(geom, cfg, sed, dwn, curves, run);
    BreachEvolutionAgent evo = new BreachEvolutionAgent(geom, cfg, sed, dwn, cover, core);
    HydraulicsState hs = new HydraulicsState();
    BreachState bs = new BreachState();

    hs.zsReserv0 = zs0;
    hyd.init(hs, bs);
    evo.init(bs);

    Path outDir = Path.of("agentcore-examples", "out");
    Files.createDirectories(outDir);
    String label = String.format("Z%.1f_N%.3f", zs0, n);
    Path out = outDir.resolve("dambreach_result_" + resveId + "_" + label + ".csv");

    DambreachReportAgent report = new DambreachReportAgent(AgentId.create("DambreachReportAgentRunner"));
    report.open(out.toString());
    report.writeHeader();

    // 初始行
    report.writeStep(0.0, 0.0, 0.0, 0.0, hs, bs, geom, cfg, sed, true);

    double eps = 1e-5;
    int it = 1;
    while (hs.time <= run.timeEnd()) {
      double timeMinusHalfDt = hs.time - 0.5 * run.dt();
      int numItr = 1;
      double err = 1.0, errZ = 1.0;
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
      report.writeStep(timeMinusHalfDt / 3600.0, hs.flowIn, hs.flowBreach, hs.flowSpill + hs.flowBreach, hs, bs, geom, cfg, sed, false);

      it++;
    }

    report.close(1, -1.0, -1.0, resveId);
    System.out.println("[Runner] done. CSV: " + out.toAbsolutePath());
  }
}
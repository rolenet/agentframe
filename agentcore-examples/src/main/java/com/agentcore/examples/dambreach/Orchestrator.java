package com.agentcore.examples.dambreach;

import com.agentcore.examples.dambreach.agents.BreachEvolutionAgent;
import com.agentcore.examples.dambreach.agents.HydraulicsAgent;
import com.agentcore.examples.dambreach.domain.Dtos.*;
import com.agentcore.examples.dambreach.io.InputAgent;
import com.agentcore.examples.dambreach.alg.DbAlgorithms;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class Orchestrator {

  public static void main(String[] args) {
    String resveId = args.length > 0 ? args[0] : "43038140011";
    String baseDir = "agentcore-examples/src/main/java/com/agentcore/examples/dambreach";
    InputAgent.InputBundle bundle = InputAgent.parse(baseDir, resveId);
    if (bundle == null) {
      System.err.println("Failed to parse inputs");
      return;
    }
    System.out.println("Parsed inputs OK for RESVE_ID=" + resveId);

    // 如果提供Zs0与n，则仅运行单场景；否则运行并行矩阵
    if (args.length >= 3) {
      double zs0 = Double.parseDouble(args[1]);
      double n = Double.parseDouble(args[2]);
      String label = String.format("Z%.1f_N%.3f", zs0, n);
      runScenario(bundle, resveId, zs0, n, label);
      System.out.println("Single scenario finished: " + label);
      return;
    }

    // 并行场景矩阵：Zs0 × n
    double[] zs0List = new double[]{38.0, 39.0, 40.0};
    double[] nList = new double[]{0.025, 0.030, 0.035};
    ExecutorService pool = Executors.newFixedThreadPool(Math.min(zs0List.length * nList.length, 4));

    List<String> labels = new ArrayList<>();
    for (double zs0 : zs0List) {
      for (double n : nList) {
        String label = String.format("Z%.1f_N%.3f", zs0, n);
        labels.add(label);
        final double fZ = zs0;
        final double fN = n;
        pool.submit(() -> runScenario(bundle, resveId, fZ, fN, label));
      }
    }
    pool.shutdown();
    try {
      pool.awaitTermination(10, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      System.err.println("Parallel scenarios interrupted: " + e.getMessage());
    }
    System.out.println("All scenarios finished: " + labels);
  }

  // 带覆盖参数的并行场景
  private static void runScenario(InputAgent.InputBundle ib, String resveId, double zs0Override, double nOverride, String label) {
    System.out.println("Scenario started for RESVE_ID=" + resveId);
    DamGeometry geom = ib.geom();
    BreachConfig cfg = ib.breachCfg();
    SedimentProps sed = ib.sed();
    DownstreamChannel baseDwn = ib.dwn();
    // 覆盖下游糙率n用于该场景
    DownstreamChannel dwn = new DownstreamChannel(
        baseDwn.iDownStrm(), baseDwn.dwnChanWidth(), baseDwn.dwnChanSlope(), nOverride
    );
    CoverProps cover = ib.cover();
    CoreProps core = ib.core();
    Curves curves = ib.curves();
    RunConfig run = ib.run();
    // 缩短模拟以快速输出
    run = new RunConfig(1.0, 0.0, 120.0, run.relax(), run.resveId());

    HydraulicsAgent hyd = new HydraulicsAgent(geom, cfg, sed, dwn, curves, run);
    BreachEvolutionAgent evo = new BreachEvolutionAgent(geom, cfg, sed, dwn, cover, core);

    HydraulicsState hs = new HydraulicsState();
    // 覆盖初始库水位
    hs.zsReserv0 = zs0Override;



    BreachState bs = new BreachState();
    hyd.init(hs, bs);
    evo.init(bs);

    double eps = 1e-5;
    boolean needStartTime = true;
    boolean needEndTime = false;
    int isDamBreach = 0;
    double startHr = -1.0;
    double endHr = -1.0;

    List<ResultRecord> records = new ArrayList<>();
    // prepare progressive CSV writing
    Path outDir = Path.of("agentcore-examples", "out");
    try {
      Files.createDirectories(outDir);
    } catch (Exception e) {
      System.err.println("Create out dir error: " + e.getMessage());
    }
    Path out = outDir.resolve("dambreach_result_" + resveId + "_" + label + ".csv");
    BufferedWriter bw = null;
    try {
      bw = Files.newBufferedWriter(out,
          java.nio.file.StandardOpenOption.CREATE,
          java.nio.file.StandardOpenOption.WRITE,
          java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
      bw.write("Time(hr),Qin(m^3/s),Qbreach,Qspill+breach,zs_reserv,zs_dwnchan,zb_breach,B_breach,B_dwnslope,B_breach2,S_brsideslope,V_watout,SedDisch,massResid,Sed_failmass,CoreStable,PipeStable");
      bw.newLine();
      bw.flush();
    } catch (Exception e) {
      System.err.println("Open CSV error: " + e.getMessage());
    }

    // write initial state row
    if (bw != null) {
      try {
        ResultRecord r0 = new ResultRecord(
            0.0,
            0.0,
            0.0,
            0.0,
            hs.zsReserv0,
            hs.zDownChan,
            bs.zBreachBottom,
            bs.widthBreach,
            bs.widthDownSlope,
            bs.widthBreach + 2.0 * (geom.damHeight() - bs.zBreachBottom) / Math.max(bs.breachSideSlope, 1e-6),
            bs.breachSideSlope,
            hs.volWaterOut,
            hs.qDownSlope,
            0.0
        );
        records.add(r0);
        boolean coreStable0 = DbAlgorithms.clayCoreStable(hs.zsReserv0, bs.zBreachBottom, cfg.cohesion(), cfg.tanFriction());
        boolean pipeStable0 = DbAlgorithms.pipeTopStable(0.0, Math.max(bs.widthBreach, 1e-3), Math.max(bs.heightBreach, 1e-3), sed.diaSed(), 1e-6);
        int cs0 = coreStable0 ? 1 : 0;
        int ps0 = pipeStable0 ? 1 : 0;
        bw.write(String.format(
            "%.5f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.5f,%.5f,%.5f,%.5f,%.5f,%d,%d",
            r0.timeHr(), r0.Qin(), r0.Qbreach(), r0.QspillPlusBreach(), r0.ZsReserv(), r0.ZsDownChan(),
            r0.ZbBreach(), r0.B_breach(), r0.B_dwnslope(), r0.B_breach2(), r0.S_brsideslope(),
            r0.V_watout(), r0.SedDisch(), hs.massResid, r0.Sed_failmass(), cs0, ps0
        ));
        bw.newLine();
        bw.flush();
      } catch (Exception e) {
        System.err.println("Write initial row error: " + e.getMessage());
      }
    }
    int it = 1;
    while (hs.time <= run.timeEnd()) {
      double timeMinusHalfDt = hs.time - 0.5 * run.dt();
      int numItr = 1;
      double err = 1.0;
      double errZ = 1.0;

      // iterate until convergence or max iter
      while (((err > eps) || (errZ > eps)) && numItr < 100) {
        hyd.step(hs, bs);
        err = Math.abs(hs.flowBreach1 - hs.flowBreach0);
        errZ = Math.abs(hs.zsReserv1 - hs.zsReserv0);
        numItr++;
      }

      // debug after hydraulic convergence
      double alphaSm = DbAlgorithms.submergence(hs.zsReservM, hs.zDownChan, bs.zBreachBottom);
      System.out.println(String.format(
          "step=%d it=%d err=%.3e errZ=%.3e Zs=%.3f Zb=%.3f Qb1=%.3f Qspill=%.3f alphaSm=%.3f",
          it, numItr, err, errZ, hs.zsReservM, bs.zBreachBottom, hs.flowBreach1, hs.flowSpill, alphaSm
      ));
      // breach evolve after hyd convergence
      evo.evolve(hs, bs, run.dt());
      System.out.println(String.format("after evolve: width=%.3f sideslope=%.3f zBottom=%.3f", bs.widthBreach, bs.breachSideSlope, bs.zBreachBottom));

      // advance time and copy state for next step
      hs.time += run.dt();
      hs.zsReserv0 = hs.zsReserv1;
      hs.zsDownStrm0 = hs.zsDownStrm1;
      hs.flowBreach0 = hs.flowBreach1;

      // flags
      if (hs.zsReservM > bs.zBreachBottom) isDamBreach = 1;
      double diffFlow = Math.abs(hs.flowBreach1 - hs.flowBreach0);
      if (needStartTime && hs.flowBreach > 1e-6) {
        startHr = timeMinusHalfDt / 3600.0;
        needStartTime = false;
        needEndTime = true;
      }
      if (needEndTime && diffFlow < 1e-6) {
        endHr = timeMinusHalfDt / 3600.0;
        needEndTime = false;
      }

      // progressive write CSV each step
      ResultRecord r = new ResultRecord(
          timeMinusHalfDt / 3600.0,
          hs.flowIn,
          hs.flowBreach,
          hs.flowSpill + hs.flowBreach,
          hs.zsReservM,
          hs.zDownChan,
          bs.zBreachBottom,
          bs.widthBreach,
          bs.widthDownSlope,
          bs.widthBreach + 2.0 * (geom.damHeight() - bs.zBreachBottom) / Math.max(bs.breachSideSlope, 1e-6),
          bs.breachSideSlope,
          hs.volWaterOut,
          hs.qDownSlope,
          0.0 // Sed_failmass placeholder
      );
      records.add(r);
      if (bw != null) {
        try {
          boolean coreStable = DbAlgorithms.clayCoreStable(hs.zsReservM, bs.zBreachBottom, cfg.cohesion(), cfg.tanFriction());
          boolean pipeStable = DbAlgorithms.pipeTopStable(hs.flowBreach, Math.max(bs.widthBreach, 1e-3), Math.max(bs.heightBreach, 1e-3), sed.diaSed(), 1e-6);
          int cs = coreStable ? 1 : 0;
          int ps = pipeStable ? 1 : 0;
          bw.write(String.format(
              "%.5f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.5f,%.5f,%.5f,%.5f,%.5f,%d,%d",
              r.timeHr(), r.Qin(), r.Qbreach(), r.QspillPlusBreach(), r.ZsReserv(), r.ZsDownChan(),
              r.ZbBreach(), r.B_breach(), r.B_dwnslope(), r.B_breach2(), r.S_brsideslope(),
              r.V_watout(), r.SedDisch(), hs.massResid, r.Sed_failmass(), cs, ps
          ));
          bw.newLine();
          bw.flush();
          System.out.println(String.format("t=%.1fs Qin=%.3f Qb=%.3f Zs=%.3f Zd=%.3f width=%.3f Zb=%.3f alphaSm=%.3f core=%d pipe=%d",
              hs.time, hs.flowIn, hs.flowBreach, hs.zsReservM, hs.zDownChan, bs.widthBreach, bs.zBreachBottom,
              com.agentcore.examples.dambreach.alg.DbAlgorithms.submergence(hs.zsReservM, hs.zDownChan, bs.zBreachBottom), cs, ps));
        } catch (Exception e) {
          System.err.println("Write row error: " + e.getMessage());
        }
      }

      it++;
    }

    // finalize CSV
    System.out.println("Breach simulation finished. Records=" + records.size());
    try {
      if (bw != null) {
        bw.write("ISDAMBREACH, START_TIME, END_TIME, RESVE_ID");
        bw.newLine();
        bw.write(String.format("%d,%.4f,%.4f,%s", isDamBreach, startHr, endHr, resveId));
        bw.newLine();
        bw.flush();
        bw.close();
        System.out.println("CSV written: " + out.toAbsolutePath());
      }
    } catch (Exception e) {
      System.err.println("Finalize CSV error: " + e.getMessage());
      e.printStackTrace();
    }
    System.out.println("Scenario finished for RESVE_ID=" + resveId + " label=" + label);
  }
}
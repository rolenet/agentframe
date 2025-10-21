package com.agentcore.examples.dambreach.io;

import com.agentcore.examples.dambreach.domain.Dtos.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class InputAgent {

  public static record InputBundle(
      DamGeometry geom,
      BreachConfig breachCfg,
      SedimentProps sed,
      CoverProps cover,
      CoreProps core,
      DownstreamChannel dwn,
      Curves curves,
      RunConfig run,
      double initialZsReserv0
  ) {}

  public static InputBundle parse(String baseDir, String resveId) {
    try {
      // INP1.txt: first line dam geom, second line ZSRESVR0, third line NUM_FLOWIN then lines of inflow
      Path inp = Path.of(baseDir, "INP1.txt");
      List<String> raw = Files.readAllLines(inp);
      List<String> lines = new ArrayList<>();
      for (String s : raw) {
        if (s == null) continue;
        String t = s.replace("\uFEFF", "").trim();
        if (!t.isEmpty()) lines.add(t);
      }
      if (lines.size() < 3) throw new IllegalArgumentException("INP1.txt format invalid: need at least 3 non-empty lines");
      String[] damParts = lines.get(0).split("\\s*,\\s*");
      if (damParts.length < 4) throw new IllegalArgumentException("Dam geometry line invalid");
      double damHeight = Double.parseDouble(damParts[0]);
      double damCrest = Double.parseDouble(damParts[1]);
      double damLength = Double.parseDouble(damParts[2]);
      double maxDamHigh = Double.parseDouble(damParts[3]);
      double foundationBase = maxDamHigh - damHeight;

      double zsReserv0 = Double.parseDouble(lines.get(1));

      int numFlowIn = Integer.parseInt(lines.get(2));
      if (lines.size() < 3 + numFlowIn) throw new IllegalArgumentException("INP1 inflow count exceeds lines");
      List<double[]> inflow = new ArrayList<>();
      for (int i = 0; i < numFlowIn; i++) {
        String[] kv = lines.get(3 + i).split("\\s+");
        if (kv.length < 2) continue;
        double t = Double.parseDouble(kv[0]); // hour
        double q = Double.parseDouble(kv[1]);
        inflow.add(new double[]{t * 3600.0, q});
      }

      // Read FLOWSPILL.TXT & RESVR.TXT with filtering by resveId
      Path spill = Path.of(baseDir, "FLOWSPILL.TXT");
      List<double[]> spillSeries = new ArrayList<>();
      if (Files.exists(spill)) {
        List<String> sLines = Files.readAllLines(spill);
        for (String s : sLines) {
          String[] arr = s.trim().split("\\s+|,");
          if (arr.length >= 3 && arr[0].equals(resveId)) {
            double t = Double.parseDouble(arr[1]) - foundationBase;
            double q = Double.parseDouble(arr[2]);
            spillSeries.add(new double[]{t, q});
          }
        }
      }
      if (spillSeries.isEmpty()) {
        spillSeries.add(new double[]{1, 1});
        spillSeries.add(new double[]{1000, 1});
      }

      Path resvr = Path.of(baseDir, "RESVR.TXT");
      List<double[]> resCurve = new ArrayList<>();
      if (Files.exists(resvr)) {
        List<String> rLines = Files.readAllLines(resvr);
        int n = 0;
        for (String s : rLines) {
          String[] arr = s.trim().split("\\s+|,");
          if (arr.length >= 3 && arr[0].equals(resveId)) {
            n++;
            if (n == 1) continue;
            double zs = Double.parseDouble(arr[1]) - foundationBase;
            double area = Double.parseDouble(arr[2]) * 10000.0;
            resCurve.add(new double[]{zs, area});
          }
        }
      }
      if (resCurve.isEmpty()) {
        resCurve.add(new double[]{1, 1});
        resCurve.add(new double[]{1000, 1});
      }

      DamGeometry geom = new DamGeometry(
          damHeight, damCrest, damLength, 0.4, 0.4, foundationBase
      );

      BreachConfig breachCfg = new BreachConfig(
          1, // OVERTOP_PIPE
          1, // METHODSIDESLOPE
          2.0, // BREACHSIDESLOPE0
          0.1, // PILOTDEPTH
          2.0, // PILOTWIDTH
          0.0, // PILOTELEV
          0.0, // PILOTHEIGHT
          1,   // MODOVERTOP
          0.0, // HEADLOSSLOC
          15500.0, // COHESION
          0.90, // TANFRICT
          6.0, // ADAPTLAMDA
          0.0, // ERODINDEX
          0.0, // COEFCT
          0,   // MHEADCUT
          1    // METHODRESVR
      );

      SedimentProps sed = new SedimentProps(
          1, // NSEDMOD 1:sand
          0.11,
          2.65,
          0.22,
          0.0,
          0.0 * 1e-6,
          0.0
      );

      DownstreamChannel dwn = new DownstreamChannel(
          1,
          470.0,
          0.0018,
          0.025
      );

      // 默认覆盖层与核心层参数（可后续通过文件或INP扩展覆盖）
      CoverProps cover = new CoverProps(
          1,     // mCoverBreach: 1砂 2黏土
          1,     // mCoverDownSlope
          0.040, // coverAnBreach
          0.035, // coverAnDownSlope
          0.50,  // coverThickBreach(m)
          0.50,  // coverThickDownSlope(m)
          1.0,   // coverBetaBreach
          2.0,   // coverBreachSideSlope
          0.08,  // coverDiaSed(m)
          2.65,  // coverSpec
          0.30,  // coverPoro
          0.0,   // coverClayContent
          0.0,   // coverCoefKd(m/s/Pa)
          0.0    // coverCoefTauc(Pa)
      );

      CoreProps core = new CoreProps(
          2,        // nSedModCore: 1砂 2黏土
          geom.damHeight() * 0.60, // coreHeight
          geom.damCrest() * 0.50,  // coreCrest
          0.4,      // coreSlopeUp
          0.4,      // coreSlopeDown
          0.0,      // coreLocation
          20000.0,  // coreCohesion(Pa)
          0.0,      // coreTensile
          0.90,     // coreTanFriction
          0.05,     // coreDiaSed(m)
          2.65,     // coreSpec
          0.25,     // corePoro
          1.0,      // coreClayContent
          1.0e-6,   // coreCoefKd
          50.0,     // coreCoefTauc
          2.0,      // coreBreachSideSlope
          1.0,      // coreBetaBreach
          0.050     // anCore
      );

      Curves curves = new Curves(resCurve, new ArrayList<>(), inflow, spillSeries);

      RunConfig run = new RunConfig(10.0, 0.0, 86400.0, 0.5, resveId);

      return new InputBundle(geom, breachCfg, sed, cover, core, dwn, curves, run, zsReserv0);
    } catch (Exception e) {
      System.err.println("InputAgent parse error: " + e.getMessage());
      return null;
    }
  }
}
package com.agentcore.examples.dambreach.tools;

import com.agentcore.examples.dambreach.io.InputAgent;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * Minimal CLI to parse INP1 and emit a placeholder CSV for dambreach results without running the container.
 */
public final class InputCsvPlaceholder {

  public static void main(String[] args) throws Exception {
    String baseDir = "agentcore-examples/src/main/java/com/agentcore/examples/dambreach";
    String resveId = "43038140011";
    if (args != null) {
      if (args.length >= 1 && args[0] != null && !args[0].isBlank()) resveId = args[0];
      if (args.length >= 2 && args[1] != null && !args[1].isBlank()) baseDir = args[1];
    }

    System.out.println("[InputCsvPlaceholder] start at " + LocalDateTime.now());
    System.out.println("[InputCsvPlaceholder] baseDir=" + baseDir + ", resveId=" + resveId);

    InputAgent.InputBundle ib = InputAgent.parse(baseDir, resveId);
    if (ib == null) {
      System.err.println("[InputCsvPlaceholder] parse failed, InputBundle is null");
      System.exit(2);
    }

    Path outDir = Path.of("agentcore-examples", "out");
    Files.createDirectories(outDir);
    String fileName = "dambreach_placeholder_" + resveId + ".csv";
    Path out = outDir.resolve(fileName);

    try (BufferedWriter bw = new BufferedWriter(new FileWriter(out.toFile(), false))) {
      // header (aligned with orchestrator-style columns, minimal set)
      bw.write("Time(hr),Qin(m^3/s),Qbreach(m^3/s),Qspill+breach(m^3/s),zs_reserv(m),zs_dwnchan(m),zb_breach(m),B_breach(m),B_dwnslope(m),SedDisch(kg/s),massResid");
      bw.newLine();
      // a single placeholder row using some defaults from input if available
      double time = 0.0;
      double qin = 0.0;
      double qbreach = 0.0;
      double qspill_breach = 0.0;
      // 占位：避免依赖具体DTO方法名，全部写0以保证示例可编译
      double zs_reserv = 0.0;
      double zs_dwn = 0.0;
      double zb_breach = 0.0;
      double B_breach = 0.0;
      double B_dwnslope = 0.0;
      double sedDisch = 0.0;
      double massResid = 0.0;
      bw.write(String.format(java.util.Locale.US,
          "%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6e",
          time, qin, qbreach, qspill_breach, zs_reserv, zs_dwn, zb_breach, B_breach, B_dwnslope, sedDisch, massResid));
      bw.newLine();
    }

    System.out.println("[InputCsvPlaceholder] wrote: " + out.toAbsolutePath());
    System.out.println("[InputCsvPlaceholder] done");
  }
}
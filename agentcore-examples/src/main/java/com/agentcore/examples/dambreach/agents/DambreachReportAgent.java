package com.agentcore.examples.dambreach.agents;

import com.agentcore.core.agent.AbstractAgent;
import com.agentcore.core.agent.AgentId;
import com.agentcore.core.behavior.BehaviorScheduler;
import com.agentcore.core.behavior.DefaultBehaviorScheduler;
import com.agentcore.core.message.AgentMessage;
import com.agentcore.examples.dambreach.alg.DbAlgorithms;
import com.agentcore.examples.dambreach.domain.Dtos.BreachConfig;
import com.agentcore.examples.dambreach.domain.Dtos.BreachState;
import com.agentcore.examples.dambreach.domain.Dtos.DamGeometry;
import com.agentcore.examples.dambreach.domain.Dtos.HydraulicsState;
import com.agentcore.examples.dambreach.domain.Dtos.SedimentProps;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public final class DambreachReportAgent extends AbstractAgent {

  public DambreachReportAgent(AgentId agentId) {
    super(agentId);
  }

  // 兼容容器调用（当前ReportAgent直写文件，不使用路由）
  public void setMessageRouter(com.agentcore.communication.router.MessageRouter router) { /* no-op */ }

  @Override
  protected BehaviorScheduler createBehaviorScheduler() {
    return new DefaultBehaviorScheduler();
  }

  @Override
  protected void doStart() { }

  @Override
  protected void doStop() { }

  @Override
  protected CompletableFuture<Void> doSendMessage(AgentMessage message) {
    return CompletableFuture.failedFuture(new IllegalStateException("ReportAgent direct file writer: messaging not used"));
  }

  @Override
  protected void doHandleMessage(AgentMessage message) { }

  // ========== CSV 写出 ==========

  private BufferedWriter bw;

  public void open(String filePath) throws Exception {
    Path p = Path.of(filePath).getParent();
    if (p != null) Files.createDirectories(p);
    this.bw = new BufferedWriter(new FileWriter(filePath, false));
  }

  public void writeHeader() throws Exception {
    if (bw == null) throw new IllegalStateException("ReportAgent not opened");
    // 与既定结构对齐（含 SedDisch 与 massResid）
    bw.write("Time(hr),Qin(m^3/s),Qbreach,Qspill+breach,zs_reserv,zs_dwnchan,zb_breach,B_breach,B_dwnslope,B_breach2,S_brsideslope,V_watout,SedDisch,massResid,Sed_failmass,CoreStable,PipeStable");
    bw.newLine();
    bw.flush();
  }

  public void writeStep(
      double timeHr,
      double Qin,
      double Qbreach,
      double Qsum,
      HydraulicsState hs,
      BreachState bs,
      DamGeometry geom,
      BreachConfig cfg,
      SedimentProps sed,
      boolean initial
  ) throws Exception {
    if (bw == null) throw new IllegalStateException("ReportAgent not opened");

    // 稳定性判据（示意，具体公式可进一步对齐Fortran）
    boolean coreStable = DbAlgorithms.clayCoreStable(hs.zsReservM, bs.zBreachBottom, cfg.cohesion(), cfg.tanFriction());
    boolean pipeStable = DbAlgorithms.pipeTopStable(hs.flowBreach, Math.max(bs.widthBreach, 1e-3), Math.max(bs.heightBreach, 1e-3), sed.diaSed(), 1e-6);
    int cs = coreStable ? 1 : 0;
    int ps = pipeStable ? 1 : 0;

    // 几何派生值
    double B_breach2 = bs.widthBreach + 2.0 * Math.max(geom.damHeight() - bs.zBreachBottom, 0.0) / Math.max(bs.breachSideSlope, 1e-6);

    // 占位：失稳质量（如需从侵蚀模块提供累计值，可替换）
    double sedFailMass = 0.0;

    bw.write(String.format(java.util.Locale.US,
        "%.5f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.5f,%.5f,%.5f,%.5f,%.5f,%d,%d",
        timeHr,
        Qin,
        Qbreach,
        Qsum,
        hs.zsReservM,
        hs.zDownChan,
        bs.zBreachBottom,
        bs.widthBreach,
        bs.widthDownSlope,
        B_breach2,
        bs.breachSideSlope,
        hs.volWaterOut,
        hs.qDownSlope,    // 作为 SedDisch 的占位（若有专用字段可替换）
        hs.massResid,     // 质量守恒残差
        sedFailMass,
        cs,
        ps
    ));
    bw.newLine();
    bw.flush();
  }

  public void close(int isDamBreach, double startHr, double endHr, String resveId) throws Exception {
    if (bw == null) return;
    // 结尾元信息
    bw.write("ISDAMBREACH, START_TIME, END_TIME, RESVE_ID");
    bw.newLine();
    bw.write(String.format(java.util.Locale.US, "%d,%.4f,%.4f,%s", isDamBreach, startHr, endHr, resveId));
    bw.newLine();
    bw.flush();
    bw.close();
  }
}
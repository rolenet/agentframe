package com.agentcore.examples.dambreach.agents;

import com.agentcore.examples.dambreach.alg.DbAlgorithms;
import com.agentcore.examples.dambreach.domain.Dtos.*;

public final class BreachEvolutionAgent {

  private final DamGeometry geom;
  private final BreachConfig cfg;
  private final SedimentProps sed;
  private final DownstreamChannel dwn;
  private final CoverProps cover;
  private final CoreProps core;

  public BreachEvolutionAgent(DamGeometry geom, BreachConfig cfg, SedimentProps sed, DownstreamChannel dwn, CoverProps cover, CoreProps core) {
    this.geom = geom;
    this.cfg = cfg;
    this.sed = sed;
    this.dwn = dwn;
    this.cover = cover;
    this.core = core;
  }

  public void init(BreachState bs) {
    // already initialized in HydraulicsAgent.init for geometry
  }

  // Simplified SOILMOVE wrapper calling core erosion models; extended as needed
  public void evolve(HydraulicsState hs, BreachState bs, double dt) {
    if (cfg.overtopPipe() == 1) {
      // critical trapezoid section
      double[] crit = DbAlgorithms.critFlowTrapz(hs.flowBreach, bs.widthBreach, bs.breachSideSlope);
      double depthBreach = crit[0], areaBreach = crit[1], radiBreach = crit[2];

      // 初始化沉积物输移量累计（用于CSV）
      hs.qDownSlope = 0.0;

      // 覆盖层分支：依据CoverProps计算并应用侵蚀
      if (cover.mCoverBreach() == 1) {
        // 覆盖层砂模型
        double wsetCov = DbAlgorithms.settlingVelocity(1e-6, cover.coverDiaSed(), cover.coverSpec());
        double qtCov = DbAlgorithms.sedSand(
            hs.flowBreach, areaBreach, bs.widthBreach, radiBreach,
            0.0,
            Math.max(cover.coverAnBreach(), 1e-6),
            cover.coverDiaSed(), cover.coverSpec(), wsetCov, cover.coverPoro(),
            cover.coverBreachSideSlope(), 2.0
        );
        hs.qDownSlope += qtCov;
        double erosionCov = dt * qtCov / Math.max(1.0 - cover.coverPoro(), 1e-6);
        applyErosion(erosionCov, hs, bs, areaBreach, radiBreach);
      } else if (cover.mCoverBreach() == 2) {
        // 覆盖层黏土模型
        double dzBdtCov = DbAlgorithms.sedClay(
            hs.flowBreach, areaBreach, radiBreach,
            Math.max(cover.coverAnBreach(), 1e-6),
            1000.0,
            cover.coverCoefKd(), cover.coverCoefTauc()
        );
        double sedDischCov = dzBdtCov * areaBreach * Math.max(0.0, (1.0 - cover.coverPoro()));
        hs.qDownSlope += sedDischCov;
        double erosionCov = dzBdtCov * dt * (bs.xDownBrink - bs.xUpBrink) *
            (bs.widthBreach + cover.coverBetaBreach() * (areaBreach / radiBreach - bs.widthBreach) * 2.0 / 2.0);
        applyErosion(erosionCov, hs, bs, areaBreach, radiBreach);
      }

      // sediment transport or erosion rate
      double qt;
      if (sed.nSedMod() == 1) {
        double wset = DbAlgorithms.settlingVelocity(1e-6, sed.diaSed(), sed.spec());
        qt = DbAlgorithms.sedSand(hs.flowBreach, areaBreach, bs.widthBreach, radiBreach,
            0.0, // local slope ~0 at breach section
            Math.max(dwn.anDwnChan(), 1e-6), // AN 取下游渠道糙率作为溃口断面近似
            sed.diaSed(), sed.spec(), wset, sed.poro(),
            bs.breachSideSlope, 2.0 // repose ~ sideSlope, breachLocate ~ 2: center
        );
        // 记录沉积物输移量到HydraulicsState，用于CSV的SedDisch列（累计）
        hs.qDownSlope += qt;
        double erosion = dt * (qt - 0.0) / (1.0 - sed.poro());
        applyErosion(erosion, hs, bs, areaBreach, radiBreach);
        // headcut advance after erosion (simplified stability-driven top edge movement)
        double vHead = DbAlgorithms.headcutRate(hs.flowBreach, areaBreach, bs.zBreachBottom, cfg.cohesion(), cfg.tanFriction());
        double dx = vHead * dt;
        // move brinks and widen breach accordingly
        bs.xUpBrink -= dx / Math.max(geom.slopeUp(), 1e-6);
        bs.xDownBrink += dx / Math.max(bs.slopeDownN, 1e-6);
        bs.widthBreach += 2.0 * dx;
        if (bs.widthBreach > geom.damLength()) bs.widthBreach = geom.damLength();

        // stability checks (clay core / pipe top); currently used for diagnostics only
        boolean coreStable = DbAlgorithms.clayCoreStable(hs.zsReservM, bs.zBreachBottom, cfg.cohesion(), cfg.tanFriction());
        boolean pipeStable = DbAlgorithms.pipeTopStable(hs.flowBreach, Math.max(bs.widthBreach, 1e-3), Math.max(bs.heightBreach, 1e-3), sed.diaSed(), 1e-6);
        // If needed later, we can modulate erosion/headcut based on stability flags.

        // recompute side slope if method requires
        if (cfg.methodSideSlope() == 1) {
          bs.breachSideSlope = DbAlgorithms.sideSlope(bs.zBreachBottom, geom, sed, cfg.cohesion(), cfg.tanFriction());
        }
        bs.sinAlpha = bs.breachSideSlope / Math.sqrt(1.0 + bs.breachSideSlope * bs.breachSideSlope);
        bs.cosAlpha = bs.sinAlpha / bs.breachSideSlope;
      } else {
        double dzBdt = DbAlgorithms.sedClay(hs.flowBreach, areaBreach, radiBreach, Math.max(dwn.anDwnChan(), 1e-6), 1000.0, sed.coefKd(), sed.coefTauc());
        // 等效沉积物输移量（体积厚度速率*断面面积*固体体积分数）
        double sedDisch = dzBdt * areaBreach * Math.max(0.0, (1.0 - sed.poro()));
        hs.qDownSlope += sedDisch;
        double erosion = dzBdt * dt * (bs.xDownBrink - bs.xUpBrink) *
            (bs.widthBreach + bs.betaBreach * (areaBreach / radiBreach - bs.widthBreach) * 2.0 / 2.0);
        applyErosion(erosion, hs, bs, areaBreach, radiBreach);
        // headcut advance after erosion (clay mode)
        double vHead = DbAlgorithms.headcutRate(hs.flowBreach, areaBreach, bs.zBreachBottom, cfg.cohesion(), cfg.tanFriction());
        double dx = vHead * dt;
        bs.xUpBrink -= dx / Math.max(geom.slopeUp(), 1e-6);
        bs.xDownBrink += dx / Math.max(bs.slopeDownN, 1e-6);
        bs.widthBreach += 2.0 * dx;
        if (bs.widthBreach > geom.damLength()) bs.widthBreach = geom.damLength();

        // stability checks (clay core / pipe top); diagnostics
        boolean coreStable = DbAlgorithms.clayCoreStable(hs.zsReservM, bs.zBreachBottom, cfg.cohesion(), cfg.tanFriction());
        boolean pipeStable = DbAlgorithms.pipeTopStable(hs.flowBreach, Math.max(bs.widthBreach, 1e-3), Math.max(bs.heightBreach, 1e-3), sed.diaSed(), 1e-6);

        if (cfg.methodSideSlope() == 1) {
          bs.breachSideSlope = DbAlgorithms.sideSlope(bs.zBreachBottom, geom, sed, cfg.cohesion(), cfg.tanFriction());
        }
        bs.sinAlpha = bs.breachSideSlope / Math.sqrt(1.0 + bs.breachSideSlope * bs.breachSideSlope);
        bs.cosAlpha = bs.sinAlpha / bs.breachSideSlope;

        // 核心层分支：依据CoreProps计算并应用侵蚀
        if (core.nSedModCore() == 1) {
          // 核心层砂模型
          double wsetCore = DbAlgorithms.settlingVelocity(1e-6, core.coreDiaSed(), core.coreSpec());
          double qtCore = DbAlgorithms.sedSand(
              hs.flowBreach, areaBreach, bs.widthBreach, radiBreach,
              0.0,
              Math.max(core.anCore(), 1e-6),
              core.coreDiaSed(), core.coreSpec(), wsetCore, core.corePoro(),
              core.coreBreachSideSlope(), 2.0
          );
          hs.qDownSlope += qtCore;
          double erosionCore = dt * qtCore / Math.max(1.0 - core.corePoro(), 1e-6);
          applyErosion(erosionCore, hs, bs, areaBreach, radiBreach);
        } else if (core.nSedModCore() == 2) {
          // 核心层黏土模型
          double dzBdtCore = DbAlgorithms.sedClay(
              hs.flowBreach, areaBreach, radiBreach,
              Math.max(core.anCore(), 1e-6),
              1000.0,
              core.coreCoefKd(), core.coreCoefTauc()
          );
          double sedDischCore = dzBdtCore * areaBreach * Math.max(0.0, (1.0 - core.corePoro()));
          hs.qDownSlope += sedDischCore;
          double erosionCore = dzBdtCore * dt * (bs.xDownBrink - bs.xUpBrink) *
              (bs.widthBreach + core.coreBetaBreach() * (areaBreach / radiBreach - bs.widthBreach) * 2.0 / 2.0);
          applyErosion(erosionCore, hs, bs, areaBreach, radiBreach);
        }
      }
    } else {
      // pipe mode: skip here; would call corresponding formulas
    }
  }

  private void applyErosion(double erosion, HydraulicsState hs, BreachState bs, double areaBreach, double radiBreach) {
    double bedArea = (bs.xDownBrink - bs.xUpBrink) * bs.widthBreach;
    double sideArea = geom.damHeight() - bs.zBreachBottom;
    double total = bedArea + sideArea;
    double dzb = erosion / Math.max(total, 1e-6);
    double dzBotm = Math.min(dzb, bs.zBreachBottom - 0.0); // ZBHARDBOTTOM=0

    bs.zBreachBottom -= dzBotm;
    bs.widthBreach += 2.0 * dzb / Math.max(bs.breachSideSlope, 1e-6);
    if (bs.widthBreach > geom.damLength()) bs.widthBreach = geom.damLength();

    // adjust brink positions
    bs.xUpBrink -= dzBotm / geom.slopeUp();
    bs.xDownBrink += dzBotm / bs.slopeDownN;

    // update breach side slope if method requires
    if (cfg.methodSideSlope() == 1) {
      bs.breachSideSlope = DbAlgorithms.sideSlope(bs.zBreachBottom, geom, sed, cfg.cohesion(), cfg.tanFriction());
    }
    bs.sinAlpha = bs.breachSideSlope / Math.sqrt(1.0 + bs.breachSideSlope * bs.breachSideSlope);
    bs.cosAlpha = bs.sinAlpha / bs.breachSideSlope;
  }
}
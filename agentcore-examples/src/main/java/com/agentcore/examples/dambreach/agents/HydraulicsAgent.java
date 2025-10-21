package com.agentcore.examples.dambreach.agents;

import com.agentcore.examples.dambreach.alg.DbAlgorithms;
import com.agentcore.examples.dambreach.domain.Dtos.*;

import java.util.List;

public final class HydraulicsAgent {

  private final DamGeometry geom;
  private final BreachConfig cfg;
  private final SedimentProps sed;
  private final DownstreamChannel dwn;
  private final Curves curves;
  private final RunConfig run;

  private static final double COEFB = 1.70;
  private static final double COEFS = 1.30;

  public HydraulicsAgent(DamGeometry geom, BreachConfig cfg, SedimentProps sed, DownstreamChannel dwn, Curves curves, RunConfig run) {
    this.geom = geom;
    this.cfg = cfg;
    this.sed = sed;
    this.dwn = dwn;
    this.curves = curves;
    this.run = run;
  }

  public void init(HydraulicsState hs, BreachState bs) {
    hs.time = run.timeBeg() + run.dt();
    // hs.zsReserv0 is set by orchestrator from InputAgent; do not overwrite
    hs.zsDownStrm0 = 0.1;
    hs.zsDownStrm1 = hs.zsDownStrm0;
    hs.zDownChan = hs.zsDownStrm0;
    hs.flowBreach0 = 0.0;
    hs.flowBreach1 = hs.flowBreach0;
    hs.flowSpill0 = 0.0;
    hs.qPeak = 0.0;
    // init Muskingum cascade arrays (defaults: cells=3, K=600s, x=0.2)
    hs.musResidInit();
    // init Dynamic Wave (shallow water) grid defaults if null
    if (hs.dwCells <= 0) hs.dwCells = 50;
    if (hs.dwDx <= 0) hs.dwDx = 50.0;
    if (hs.dwCfl <= 0) hs.dwCfl = 0.9;
    if (hs.dwH == null || hs.dwH.length != hs.dwCells) hs.dwH = new double[hs.dwCells];
    if (hs.dwQ == null || hs.dwQ.length != hs.dwCells) hs.dwQ = new double[hs.dwCells];
    if (hs.dwHprev == null || hs.dwHprev.length != hs.dwCells) hs.dwHprev = new double[hs.dwCells];
    if (hs.dwQprev == null || hs.dwQprev.length != hs.dwCells) hs.dwQprev = new double[hs.dwCells];

    // geometry init
    bs.xUpToe = 0.0;
    bs.xUpCorner = bs.xUpToe + geom.damHeight() / geom.slopeUp();
    bs.xDownCorner = bs.xUpCorner + geom.damCrest();
    bs.xDownToe = bs.xDownCorner + geom.damHeight() / geom.slopeDown();

    bs.zBUpToe = 0.0;
    bs.zBUpCorner = geom.damHeight();
    bs.zBDownCorner = geom.damHeight();
    bs.zBDownToe = 0.0;

    // breach bottom and pilot (lower bottom by total 4m for stronger trigger in short-run)
    bs.zBreachBottom = geom.damHeight() - (cfg.pilotDepth() + 4.0);
    bs.widthBreach = cfg.pilotWidth();
    bs.xUpBrink = bs.xUpCorner - (cfg.pilotDepth() + 4.0) / geom.slopeUp();
    bs.xDownBrink = bs.xDownCorner + (cfg.pilotDepth() + 4.0) / geom.slopeDown();
    bs.widthDownSlope = bs.widthBreach;
    bs.slopeDownN = geom.slopeDown();

    // side slope
    if (cfg.methodSideSlope() == 1) {
      bs.breachSideSlope = DbAlgorithms.sideSlope(bs.zBreachBottom, geom, sed, cfg.cohesion(), cfg.tanFriction());
    } else {
      bs.breachSideSlope = cfg.initialSideSlope();
    }
    bs.sinAlpha = bs.breachSideSlope / Math.sqrt(1.0 + bs.breachSideSlope * bs.breachSideSlope);
    bs.cosAlpha = bs.sinAlpha / bs.breachSideSlope;
  }



  public void step(HydraulicsState hs, BreachState bs) {
    double timeMinusHalfDt = hs.time - 0.5 * run.dt();

    // inflow interpolation
    double Qin = interpolateSeries(timeMinusHalfDt, curves.inflowSeries());
    hs.flowIn = Qin;

    // reservoir spill base at zsReservM
    hs.zsReservM = hs.zsReserv0;

    // breach flow compute (relax)
    double[] bf = DbAlgorithms.breachFlow(
        1, // force overtop (open channel) mode
        hs.zsReservM,
        hs.zDownChan,
        bs.zBreachBottom,
        bs.widthBreach,
        bs.heightBreach,
        geom.damHeight(),
        geom.damLength(),
        bs.breachSideSlope,
        hs.flowSpill0,
        sed.diaSed(),
        1e-6,
        bs.xDownCenter,
        bs.xUpCenter,
        cfg.headLossLoc(),
        hs.flowBreach1,
        COEFB,
        COEFS
    );
    hs.flowBreach1 = bf[0];
    hs.flowSpill = bf[1];
    double errFlow = bf[2];

    hs.flowBreach = 0.5 * (hs.flowBreach1 + hs.flowBreach0);

    // reservoir area
    double areaResvr;
    if (cfg.methodReservoir() == 1) {
      areaResvr = DbAlgorithms.interpolate(hs.zsReservM, curves.reservoirCurve())[1];
    } else {
      // power law fallback
      double alpha = 1.0, m = 1.0;
      areaResvr = alpha * Math.max(hs.zsReservM, 0.01 * geom.damHeight()) * m;
    }

    // mass balance reservoir
    double zsReserv1N = hs.zsReserv0 + run.dt() / areaResvr * (hs.flowIn - hs.flowBreach - hs.flowSpill);
    double errorZ = Math.abs(zsReserv1N - hs.zsReserv1);
    hs.zsReserv1 = zsReserv1N;

    // volume conservation residual (m^3/s): storage change rate minus net inflow
    hs.massResid = areaResvr * (hs.zsReserv1 - hs.zsReserv0) / Math.max(run.dt(), 1e-9)
        - (hs.flowIn - hs.flowBreach - hs.flowSpill);

    // downstream update
    if (dwn.iDownStrm() == 1) {
      // Rating (open-channel)
      double[] trap = DbAlgorithms.flowTrapz(
          hs.flowBreach,
          Math.max(bs.widthBreach, 1e-6),
          Math.max(dwn.dwnChanSlope(), 1e-9),
          Math.max(dwn.anDwnChan(), 1e-9),
          Math.max(bs.breachSideSlope, 1e-6)
      );
      double depthTrap = trap[0];
      double areaTrap = trap[1];
      double radiTrap = trap[2];
      double[] crit = DbAlgorithms.critFlowTrapz(
          Math.max(hs.flowBreach, 1e-9),
          Math.max(bs.widthBreach, 1e-6),
          Math.max(bs.breachSideSlope, 1e-6)
      );
      double depthCrit = crit[0];
      double areaCrit = crit[1];
      double radiCrit = crit[2];
      System.out.println(String.format("FLOWTRAPZ: depth=%.5f area=%.5f radi=%.5f | CRIT: depth=%.5f area=%.5f radi=%.5f",
          depthTrap, areaTrap, radiCrit, depthCrit, areaCrit, radiCrit));
      double zsDownStrm1N = Math.min(depthTrap, hs.zsReservM);
      double errorZd = Math.abs(zsDownStrm1N - hs.zsDownStrm1);
      hs.zsDownStrm1 = 0.5 * run.relax() * zsDownStrm1N + (1.0 - 0.5 * run.relax()) * hs.zsDownStrm1;
      errorZ = Math.max(errorZ, errorZd);
    } else if (dwn.iDownStrm() == 2) {
      // Dynamic wave (Saint-Venant, explicit Lax-Friedrichs-like update)
      final double g = 9.81;
      int N = hs.dwCells;
      double dt = Math.max(run.dt(), 1e-9);
      double dx = Math.max(hs.dwDx, 1e-6);
      // left boundary: inflow equals breach flow (mean over step)
      hs.dwQ[0] = Math.max(hs.flowBreach, 0.0);
      // copy prev
      System.arraycopy(hs.dwH, 0, hs.dwHprev, 0, N);
      System.arraycopy(hs.dwQ, 0, hs.dwQprev, 0, N);

      double width = Math.max(dwn.dwnChanWidth(), 1e-6);
      double sideSlope = Math.max(bs.breachSideSlope, 1e-6); // proxy for downstream section
      double sinAlphaSide = sideSlope / Math.sqrt(1.0 + sideSlope * sideSlope);
      double nMann = Math.max(dwn.anDwnChan(), 1e-9);
      double S0 = Math.max(dwn.dwnChanSlope(), 1e-9);

      // interior update
      for (int i = 1; i < N - 1; i++) {
        double hL = hs.dwHprev[i - 1], hC = hs.dwHprev[i], hR = hs.dwHprev[i + 1];
        double qL = hs.dwQprev[i - 1], qC = hs.dwQprev[i], qR = hs.dwQprev[i + 1];
        // continuity: h_t + q_x = 0
        // minmod limiter for q-slope
        double dqL = qC - qL;
        double dqR = qR - qC;
        double sgn = Math.signum(dqL) * Math.signum(dqR);
        double dqLim = (sgn > 0.0) ? Math.copySign(Math.min(Math.abs(dqL), Math.abs(dqR)), dqL) : 0.0;

        // base continuity update
        double hNew = 0.5 * (hR + hL) - dt / (2.0 * dx) * (qR - qL);

        // artificial viscosity for h (Andrade-style)
        double cWave = Math.sqrt(g * Math.max(hC, 1e-6));
        double nu = 0.2 * dx * cWave; // tunable coefficient
        hNew -= dt * nu * (hR - 2.0 * hC + hL) / Math.max(dx * dx, 1e-9);

        hNew = Math.max(hNew, 0.0);

        // trapezoid hydraulic parameters at center
        double A = width * Math.max(hC, 1e-6) + (hC * hC) / sideSlope;
        double P = width + 2.0 * Math.max(hC, 1e-6) / sinAlphaSide;
        double R = Math.max(A / P, 1e-6);
        double Sf = nMann * nMann * qC * Math.abs(qC) / Math.max(A * A * Math.pow(R, 4.0 / 3.0), 1e-9);

        // momentum: q_t + (q^2/A + 0.5 g A^2)' + g A (S_f - S_0) = 0 (simplified)
        double fluxL = (qL * qL) / Math.max(A, 1e-6) + 0.5 * g * A * A;
        double fluxR = (qR * qR) / Math.max(A, 1e-6) + 0.5 * g * A * A;
        // momentum base update with limited slope
        double qNew = 0.5 * (qR + qL) - dt / (2.0 * dx) * (fluxR - fluxL) - dt * g * A * (Sf - S0);

        // apply slope limiter to q (replace high-frequency component)
        qNew -= dt / (2.0 * dx) * (dqR - dqL - dqLim);

        // artificial viscosity for q
        qNew -= dt * nu * (qR - 2.0 * qC + qL) / Math.max(dx * dx, 1e-9);

        hs.dwH[i] = hNew;
        hs.dwQ[i] = qNew;
      }

      // right boundary: free outflow (zero gradient)
      hs.dwH[N - 1] = hs.dwH[N - 2];
      hs.dwQ[N - 1] = Math.max(hs.dwQ[N - 2], 0.0);

      double QoutDwn = Math.max(hs.dwQ[N - 1], 0.0);

      // downstream storage level update using downStoreCurve area
      double zsDownM = hs.zsDownStrm0;
      List<double[]> downCurve = curves.downStoreCurve();
      double areaDwn = 1.0e3;
      if (downCurve != null && !downCurve.isEmpty()) {
        areaDwn = DbAlgorithms.interpolate(zsDownM, downCurve)[1];
        if (areaDwn <= 0.0) areaDwn = 1.0;
      }
      double zsDownStrm1N = hs.zsDownStrm0 + dt / Math.max(areaDwn, 1e-6) * (hs.flowBreach - QoutDwn);
      double errorZd = Math.abs(zsDownStrm1N - hs.zsDownStrm1);
      hs.zsDownStrm1 = 0.5 * run.relax() * zsDownStrm1N + (1.0 - 0.5 * run.relax()) * hs.zsDownStrm1;
      errorZ = Math.max(errorZ, errorZd);
    }

    hs.volWaterOut += run.dt() * (hs.flowBreach + hs.flowSpill);

    // peak update
    if (hs.flowBreach > hs.qPeak) hs.qPeak = hs.flowBreach;

    // do not advance time/state here; Orchestrator controls advancement
  }

  private static double interpolateSeries(double x, List<double[]> series) {
    return DbAlgorithms.interpolate(x, series)[1];
  }
}
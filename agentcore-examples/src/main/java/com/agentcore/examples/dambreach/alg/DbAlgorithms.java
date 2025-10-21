package com.agentcore.examples.dambreach.alg;

import com.agentcore.examples.dambreach.domain.Dtos.*;

import java.util.List;

public final class DbAlgorithms {

  public static double[] interpolate(double x, List<double[]> curve) {
    if (curve == null || curve.isEmpty()) return new double[]{x, 0.0};
    if (x <= curve.get(0)[0]) return new double[]{curve.get(0)[0], curve.get(0)[1]};
    int n = curve.size();
    if (x >= curve.get(n - 1)[0]) return new double[]{curve.get(n - 1)[0], curve.get(n - 1)[1]};
    for (int k = 0; k < n - 1; k++) {
      double x0 = curve.get(k)[0], x1 = curve.get(k + 1)[0];
      if (x >= x0 && x < x1) {
        double f0 = curve.get(k)[1], f1 = curve.get(k + 1)[1];
        double f = f0 + (x - x0) * (f1 - f0) / (x1 - x0);
        return new double[]{x, f};
      }
    }
    return new double[]{x, curve.get(n - 1)[1]};
  }

  public static double sideSlope(double zBreachBottom, DamGeometry geom, SedimentProps sed, double cohesion, double tanFriction) {
    double damHeight = geom.damHeight();
    double spec = sed.spec();
    double poro = sed.poro();
    double rhow = 1000.0;
    double gamas = (spec * (1.0 - poro) + 0.50 * poro) * rhow * 9.81;
    double fourCGamaH = 4.0 * cohesion / gamas / Math.max(damHeight - zBreachBottom, 0.001);
    double tan2 = tanFriction * tanFriction;
    double sqrtrw = Math.sqrt((fourCGamaH * fourCGamaH + 2.0 * fourCGamaH * tanFriction) * (1.0 + tan2));
    double ctgBeta = (fourCGamaH + tanFriction - sqrtrw) / tan2;
    double breachSideSlope;
    if (ctgBeta > 1e-8) {
      double failureAngle = 0.50 * (1.0 + tanFriction * ctgBeta) / (0.5 * fourCGamaH + ctgBeta);
      breachSideSlope = Math.tan(Math.min(1.5707, 0.5 * (Math.atan(1.0 / ctgBeta) + Math.atan(failureAngle))));
    } else if (ctgBeta >= -1e-8) {
      double failureAngle = 0.50 * (1.0 + tanFriction * ctgBeta) / (0.5 * fourCGamaH + ctgBeta);
      breachSideSlope = Math.tan(Math.min(1.5707, 0.5 * (1.5707 + Math.atan(failureAngle))));
    } else if (ctgBeta > -fourCGamaH / 2.0 + 1e-8) {
      double failureAngle = 0.50 * (1.0 + tanFriction * ctgBeta) / (0.5 * fourCGamaH + ctgBeta);
      if (failureAngle > 1e-8) {
        breachSideSlope = Math.tan(Math.min(1.5707, 0.5 * (3.1415 + Math.atan(1.0 / ctgBeta) + Math.atan(failureAngle))));
      } else {
        breachSideSlope = 1e8;
      }
    } else {
      breachSideSlope = 1e8;
    }
    return breachSideSlope;
  }

  public static double settlingVelocity(double amiu, double diaSed, double spec) {
    double fshape = 0.70;
    double coefWum = 53.5 * Math.exp(-0.65 * fshape);
    double coefWun = 5.65 * Math.exp(-2.50 * fshape);
    double exponWu = 0.70 + 0.90 * fshape;
    double dstar = diaSed * Math.pow((spec - 1.0) * 9.81, 0.3333) / Math.pow(amiu, 0.6667);
    double wset = coefWum * (amiu / diaSed) / coefWun;
    wset = wset * Math.pow(Math.sqrt(0.25 + Math.pow(1.3333 * coefWun * Math.pow(dstar, 3) / (coefWum * coefWum), 1.0 / exponWu)) - 0.5, exponWu);
    return wset;
  }

  public static double submergence(double zsReservM, double zsDownChanM, double zBreachBottom) {
    double denom = zsReservM - zBreachBottom;
    if (denom <= 1e-9) return 0.0; // 上游未高于溃口底，视为无溃口流
    double numer = zsDownChanM - zBreachBottom;
    if (numer <= 0.0) return 1.0;  // 下游未高于溃口底，完全不淹没
    double consm = numer / denom;  // 淹没比
    if (consm >= 1.0) return 0.0;  // 下游与上游等高或更高，完全淹没
    if (consm <= 0.67) return 1.0; // 阈值以下不淹没
    double frac = (consm - 0.67) / (1.0 - 0.67); // 归一化到[0,1]
    double alphaSm = 1.0 - Math.pow(frac, 3.0);
    if (alphaSm < 0.0) alphaSm = 0.0;
    if (alphaSm > 1.0) alphaSm = 1.0;
    return alphaSm;
  }

  public static double[] critFlowTrapz(double Q, double width, double sideSlope) {
    double sinAlphaSide = sideSlope / Math.sqrt(1.0 + sideSlope * sideSlope);
    double depth1 = 1e-6;
    double cflw = Q * Q * (width + 2.0 * depth1 / sideSlope) / 9.81 /
        Math.pow((width * depth1 + depth1 * depth1 / sideSlope), 3.0) - 1.0;
    double depthU;
    while (true) {
      depthU = depth1 + 0.1;
      double cfup = Q * Q * (width + 2.0 * depthU / sideSlope) / 9.81 /
          Math.pow((width * depthU + depthU * depthU / sideSlope), 3.0) - 1.0;
      if (cflw * cfup < 0) break;
      depth1 = depthU;
      cflw = cfup;
    }
    while (Math.abs(depth1 - depthU) / depth1 > 1e-5) {
      double depthM = 0.5 * (depth1 + depthU);
      double cfmd = Q * Q * (width + 2.0 * depthM / sideSlope) / 9.81 /
          Math.pow((width * depthM + depthM * depthM / sideSlope), 3.0) - 1.0;
      if (cfmd * (Q * Q * (width + 2.0 * depthU / sideSlope) / 9.81 /
          Math.pow((width * depthU + depthU * depthU / sideSlope), 3.0) - 1.0) > 0) {
        depthU = depthM;
      } else {
        depth1 = depthM;
      }
    }
    double depth = 0.5 * (depth1 + depthU);
    double area = width * depth + depth * depth / sideSlope;
    double radi = area / (width + 2.0 * depth / sinAlphaSide);
    return new double[]{depth, area, radi};
  }

  public static double[] flowTrapz(double Q, double width, double slope, double an, double sideSlope) {
    double sinAlphaSide = sideSlope / Math.sqrt(1.0 + sideSlope * sideSlope);
    double depthNorm;
    if (slope < 1e-9) depthNorm = 1e9;
    else {
      double depth1 = 1e-6;
      double conv = Math.pow((width * depth1 + depth1 * depth1 / sideSlope), 1.66667) /
          Math.pow((width + 2.0 * depth1 / sinAlphaSide), 0.6667) / an;
      double flw = Q - conv * Math.sqrt(slope);
      double depthU;
      while (true) {
        depthU = depth1 + 0.1;
        conv = Math.pow((width * depthU + depthU * depthU / sideSlope), 1.66667) /
            Math.pow((width + 2.0 * depthU / sinAlphaSide), 0.6667) / an;
        double fup = Q - conv * Math.sqrt(slope);
        if (flw * fup < 0) break;
        depth1 = depthU;
        flw = fup;
      }
      while (Math.abs(depth1 - depthU) / depth1 > 1e-5) {
        double depthM = 0.5 * (depth1 + depthU);
        conv = Math.pow((width * depthM + depthM * depthM / sideSlope), 1.66667) /
            Math.pow((width + 2.0 * depthM / sinAlphaSide), 0.6667) / an;
        double fmd = Q - conv * Math.sqrt(slope);
        if (fmd * (Q - conv * Math.sqrt(slope)) > 0) {
          depthU = depthM;
        } else {
          depth1 = depthM;
        }
      }
      depthNorm = 0.5 * (depth1 + depthU);
    }
    double depth1 = 1e-6;
    double cflw = Q * Q * (width + 2.0 * depth1 / sideSlope) / 9.81 /
        Math.pow((width * depth1 + depth1 * depth1 / sideSlope), 3.0) - 1.0;
    double depthU;
    while (true) {
      depthU = depth1 + 0.1;
      double cfup = Q * Q * (width + 2.0 * depthU / sideSlope) / 9.81 /
          Math.pow((width * depthU + depthU * depthU / sideSlope), 3.0) - 1.0;
      if (cflw * cfup < 0) break;
      depth1 = depthU;
      cflw = cfup;
    }
    while (Math.abs(depth1 - depthU) / depth1 > 1e-5) {
      double depthM = 0.5 * (depth1 + depthU);
      double cfmd = Q * Q * (width + 2.0 * depthM / sideSlope) / 9.81 /
          Math.pow((width * depthM + depthM * depthM / sideSlope), 3.0) - 1.0;
      double cfup = Q * Q * (width + 2.0 * depthU / sideSlope) / 9.81 /
          Math.pow((width * depthU + depthU * depthU / sideSlope), 3.0) - 1.0;
      if (cfmd * cfup > 0) {
        depthU = depthM;
      } else {
        depth1 = depthM;
      }
    }
    double depthCrit = 0.5 * (depth1 + depthU);
    double depth = Math.min(depthNorm, depthCrit);
    double area = width * depth + depth * depth / sideSlope;
    double radi = area / (width + 2.0 * depth / sinAlphaSide);
    return new double[]{depth, area, radi};
  }

  public static double sedSand(double Q, double area, double width, double radi, double slope, double an,
                               double diaSed, double spec, double wSet, double poro,
                               double repose, double breachLocate) {
    double cqb = Math.sqrt((spec - 1.0) * 9.81 * Math.pow(diaSed, 3.0));
    double uStarCr2 = 0.03 * (spec - 1.0) * 9.81 * diaSed;
    double uv = Q / area;
    double u3ghw = Math.pow(uv, 3.0) / 9.81 / radi / wSet;
    double qsStar = Q * 0.05 * Math.pow(u3ghw, 1.5) / (1.0 + Math.pow(u3ghw / 45.0, 1.15)) / spec / 1000.0;
    qsStar = qsStar * (((area / radi - width) * breachLocate / 2.0 + width) / (area / radi));
    double uStar2 = an * an * 9.81 * uv * uv / Math.pow(radi, 0.33333);
    double alamda = slope > 0.0 ? 1.0 + 0.22 * Math.pow(uStar2 / uStarCr2, 0.15) * Math.exp(2.0 * Math.abs(slope / repose)) : 1.0;
    uStar2 = uStar2 + alamda * uStarCr2 * slope / repose;
    double dune = Math.min(1.0, Math.pow(Math.pow(diaSed, 0.1666667) / 20.0 / an, 1.5));
    double qbStar = 0.0053 * cqb * Math.pow(Math.max(0.0, dune * uStar2 / uStarCr2 - 1.0), 2.2);
    double qtStar = qbStar * width + qsStar;
    double qtStarLimit = 0.99 * Q * (1.0 - poro);
    if (qtStar > qtStarLimit) qtStar = qtStarLimit;
    return qtStar;
  }

  public static double sedClay(double Q, double area, double radi, double an, double rhow, double coefKd, double coefTauc) {
    double uv = Q / area;
    double tau = 9.81 * rhow * an * an * uv * uv / Math.pow(radi, 0.333333);
    double dzBdt = coefKd * (tau - coefTauc);
    if (dzBdt < 0.0) dzBdt = 0.0;
    return dzBdt;
  }

  public static double[] breachFlow(int overtopPipe, double zsReservM, double zsDownChanM, double zBreachBottom,
                                    double widthBreach, double heightBreach,
                                    double damHeight, double damLength,
                                    double breachSideSlope, double flowSpill0,
                                    double diaSed, double amiu,
                                    double xDownCenter, double xUpCenter,
                                    double headLossLoc,
                                    double flowBreachPrev,
                                    double coefB, double coefS) {
    double flowSpill;
    double flowBreach1;
    if (overtopPipe == 1) {
      if (zsReservM > zBreachBottom && zsReservM > zsDownChanM) {
        double alphaSm = submergence(zsReservM, zsDownChanM, zBreachBottom);
        flowBreach1 = alphaSm * (coefB * widthBreach * Math.pow(zsReservM - zBreachBottom, 1.5)
            + coefS / breachSideSlope * Math.pow(zsReservM - zBreachBottom, 2.5));
        if (zsReservM > damHeight) {
          flowSpill = flowSpill0 + coefB *
              Math.max(0.0, damLength - widthBreach - 2.0 * (zsReservM - zBreachBottom) / breachSideSlope) *
              Math.pow(zsReservM - damHeight, 1.5);
        } else {
          flowSpill = flowSpill0;
        }
      } else {
        flowBreach1 = 0.0;
        flowSpill = flowSpill0;
      }
    } else {
      if (zsReservM > zBreachBottom && zsReservM > zsDownChanM) {
        if (zsReservM > zBreachBottom + heightBreach) {
          double area = widthBreach * heightBreach;
          double peri = 2.0 * (widthBreach + heightBreach);
          double radi = area / peri;
          double reyn = flowBreachPrev / area * (4.0 * radi) / amiu;
          double roug = diaSed / (4.0 * radi);
          double frictionFactor = 0.25 / Math.pow(Math.log10(roug / 3.7 + 5.74 / Math.pow(Math.max(4000.0, reyn), 0.9)), 2.0);
          double headLoss = headLossLoc + frictionFactor * (xDownCenter - xUpCenter) / (4.0 * radi);
          headLoss += 1.0;
          flowBreach1 = area * Math.sqrt(2.0 * 9.81 * (zsReservM - Math.max(zBreachBottom + heightBreach / 2.0, zsDownChanM)) / headLoss);
        } else {
          double alphaSm = submergence(zsReservM, zsDownChanM, zBreachBottom);
          flowBreach1 = alphaSm * (coefB * widthBreach * Math.pow(zsReservM - zBreachBottom, 1.5));
        }
        if (zsReservM > damHeight) {
          flowSpill = flowSpill0 + coefB * damLength * Math.pow(zsReservM - damHeight, 1.5);
        } else {
          flowSpill = flowSpill0;
        }
      } else {
        flowBreach1 = 0.0;
        flowSpill = flowSpill0;
      }
    }
    double err = Math.abs(flowBreach1 - flowBreachPrev);
    return new double[]{flowBreach1, flowSpill, err};
  }

  // Headcut advance rate (simplified): based on unit discharge and material strength
  // Returns rate in m/s
  public static double headcutRate(double Q, double area, double zBreachBottom, double cohesion, double tanFriction) {
    double uv = Q / Math.max(area, 1e-6); // m/s
    // shear stress proxy and critical threshold (simplified)
    double tau = 1000.0 * 9.81 * uv; // Pa (proxy)
    double tauCrit = Math.max(1e3, cohesion * (1.0 + tanFriction) * 10.0); // Pa
    double rate = Math.max(0.0, (tau - tauCrit)) / (1000.0 * 9.81); // m/s
    // cap the rate to avoid numeric explosion
    if (rate > 0.5) rate = 0.5;
    return rate;
  }

  // CLAYCORESTAB: simplified clay core stability check
  // Returns true if stable (no failure expected), based on available head and material strength
  public static boolean clayCoreStable(double zReserv, double zBottom, double cohesion, double tanFriction) {
    double head = Math.max(0.0, zReserv - zBottom); // m
    double tau = 1000.0 * 9.81 * head; // Pa proxy from head
    double tauCrit = Math.max(1e3, cohesion * (1.0 + tanFriction) * 50.0); // Pa threshold
    return tau <= tauCrit;
  }

  // PIPETOPSTAB: simplified pipe-top stability (for pipe overtopping mode)
  // Returns true if stable; uses Reynolds and roughness proxy
  public static boolean pipeTopStable(double Q, double width, double height, double diaSed, double amiu) {
    double area = Math.max(1e-6, width * height);
    double radi = area / Math.max(1e-6, 2.0 * (width + height));
    double reyn = Q / area * (4.0 * radi) / Math.max(1e-9, amiu);
    double roug = diaSed / Math.max(1e-6, 4.0 * radi);
    double frictionFactor = 0.25 / Math.pow(Math.log10(roug / 3.7 + 5.74 / Math.pow(Math.max(4000.0, reyn), 0.9)), 2.0);
    // if friction factor too high or Reynolds too large -> may be unstable
    return !(frictionFactor > 0.08 && reyn > 1.0e5);
  }
}
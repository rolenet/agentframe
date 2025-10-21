package com.agentcore.examples.dambreach.domain;

import java.util.List;

public final class Dtos {

  public record DamGeometry(
      double damHeight,
      double damCrest,
      double damLength,
      double slopeUp,
      double slopeDown,
      double foundationBase
  ) {}

  public record BreachConfig(
      int overtopPipe, // 1: overtop, 2: pipe
      int methodSideSlope,
      double initialSideSlope,
      double pilotDepth,
      double pilotWidth,
      double pilotElev,
      double pilotHeight,
      int modOvertop,
      double headLossLoc,
      double cohesion,
      double tanFriction,
      double adaptLambda,
      double erodIndex,
      double coefCT,
      int mHeadcut,
      int methodReservoir
  ) {}

  public record SedimentProps(
      int nSedMod,
      double diaSed,
      double spec,
      double poro,
      double clayContent,
      double coefKd,
      double coefTauc
  ) {}

  public record CoreProps(
      int nSedModCore,
      double coreHeight,
      double coreCrest,
      double coreSlopeUp,
      double coreSlopeDown,
      double coreLocation,
      double coreCohesion,
      double coreTensile,
      double coreTanFriction,
      double coreDiaSed,
      double coreSpec,
      double corePoro,
      double coreClayContent,
      double coreCoefKd,
      double coreCoefTauc,
      double coreBreachSideSlope,
      double coreBetaBreach,
      double anCore
  ) {}

  public record CoverProps(
      int mCoverBreach,
      int mCoverDownSlope,
      double coverAnBreach,
      double coverAnDownSlope,
      double coverThickBreach,
      double coverThickDownSlope,
      double coverBetaBreach,
      double coverBreachSideSlope,
      double coverDiaSed,
      double coverSpec,
      double coverPoro,
      double coverClayContent,
      double coverCoefKd,
      double coverCoefTauc
  ) {}

  public record DownstreamChannel(
      int iDownStrm, // 1: rating, 2: storage curve
      double dwnChanWidth,
      double dwnChanSlope,
      double anDwnChan
  ) {}

  public record Curves(
      List<double[]> reservoirCurve, // [zs, area]
      List<double[]> downStoreCurve, // [zs, area]
      List<double[]> inflowSeries,   // [time, Qin]
      List<double[]> spillSeries     // [time, spill]
  ) {}

  public record RunConfig(
      double dt,
      double timeBeg,
      double timeEnd,
      double relax,
      String resveId
  ) {}

  public static final class HydraulicsState {
    public double time;
    public double zsReserv0;
    public double zsReserv1;
    public double zsReservM;
    public double zsDownStrm0;
    public double zsDownStrm1;
    public double zDownChan;
    public double flowIn;
    public double flowSpill0;
    public double flowSpill;
    public double flowBreach0;
    public double flowBreach1;
    public double flowBreach;
    public double qPeak;
    public double volWaterOut;
    public double qDownSlope;
    public double massResid;

    // Dynamic wave (shallow water) grid state
    public int dwCells;
    public double dwDx;
    public double dwCfl;
    public double[] dwH;
    public double[] dwQ;
    public double[] dwHprev;
    public double[] dwQprev;

    // Muskingum cascade state
    public int musCells;
    public double[] musK;
    public double[] musX;
    public double[] musQin;
    public double[] musQout;
    public double[] musQinPrev;
    public double[] musQoutPrev;

    public HydraulicsState() {
      time = 0;
      qPeak = 0;
      volWaterOut = 0;
      massResid = 0;
    }

    // initialize Muskingum arrays with defaults if null
    public void musResidInit() {
      if (musCells <= 0) musCells = 3;
      if (musK == null || musK.length != musCells) {
        musK = new double[musCells];
        for (int i = 0; i < musCells; i++) musK[i] = 600.0; // seconds
      }
      if (musX == null || musX.length != musCells) {
        musX = new double[musCells];
        for (int i = 0; i < musCells; i++) musX[i] = 0.2;
      }
      if (musQin == null || musQin.length != musCells) musQin = new double[musCells];
      if (musQout == null || musQout.length != musCells) musQout = new double[musCells];
      if (musQinPrev == null || musQinPrev.length != musCells) musQinPrev = new double[musCells];
      if (musQoutPrev == null || musQoutPrev.length != musCells) musQoutPrev = new double[musCells];
    }
  }

  public static final class BreachState {
    public boolean headcut;
    public double zBreachBottom;
    public double widthBreach;
    public double widthDownSlope;
    public double heightBreach;
    public double breachSideSlope;
    public double breachSideSlope0;
    public double sinAlpha;
    public double cosAlpha;
    public double betaBreach0 = 1.0;
    public double betaBreach = 1.0;
    public double betaDownSlope = 1.0;

    public double xUpToe, xUpCorner, xDownCorner, xDownToe;
    public double zBUpToe, zBUpCorner, zBDownCorner, zBDownToe;
    public double xHeadcut;
    public double xUpBrink, xDownBrink;
    public double xUpCenter, xDownCenter;

    public double zDownSlopeBg, xDownSlopeBg;
    public double slopeDownN;

    public BreachState() {}
  }

  public record ResultRecord(
      double timeHr,
      double Qin,
      double Qbreach,
      double QspillPlusBreach,
      double ZsReserv,
      double ZsDownChan,
      double ZbBreach,
      double B_breach,
      double B_dwnslope,
      double B_breach2,
      double S_brsideslope,
      double V_watout,
      double SedDisch,
      double Sed_failmass
  ) {}
}
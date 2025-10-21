package com.agentcore.examples.dambreach.tools;

public record ScenarioParam(double zs0, double manningN) {
  public String label() {
    return String.format("Z%.1f_N%.3f", zs0, manningN);
  }
}
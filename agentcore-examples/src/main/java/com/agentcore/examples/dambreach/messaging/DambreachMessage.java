package com.agentcore.examples.dambreach.messaging;

import com.agentcore.examples.dambreach.io.InputAgent;

public final class DambreachMessage {

  public enum Stage {
    INIT, START, HYD_STEP, BREACH_EVOLVE, REPORT_STEP, DONE
  }

  private final Stage stage;
  private final Object payload;

  public DambreachMessage(Stage stage, Object payload) {
    this.stage = stage;
    this.payload = payload;
  }

  public Stage getStage() {
    return stage;
  }

  public Object getPayload() {
    return payload;
  }

  // 简单载荷封装：INIT时传入解析结果与可选情景标识
  public record InitPayload(InputAgent.InputBundle inputBundle, String resveId) {}
}
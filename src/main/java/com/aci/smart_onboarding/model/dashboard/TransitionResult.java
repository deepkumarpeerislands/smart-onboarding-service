package com.aci.smart_onboarding.model.dashboard;

import com.aci.smart_onboarding.model.AuditLog;
import java.util.List;
import java.util.Map;

/** Helper class to hold status transition calculation results. */
public class TransitionResult {
  private final List<AuditLog> auditLogs;
  private final Map<String, String> formIdToBrdIdMap;
  private final Map<String, Double> averages;

  public TransitionResult(
      List<AuditLog> auditLogs,
      Map<String, String> formIdToBrdIdMap,
      Map<String, Double> averages) {
    this.auditLogs = auditLogs;
    this.formIdToBrdIdMap = formIdToBrdIdMap;
    this.averages = averages;
  }

  public List<AuditLog> getAuditLogs() {
    return auditLogs;
  }

  public Map<String, String> getFormIdToBrdIdMap() {
    return formIdToBrdIdMap;
  }

  public Map<String, Double> getAverages() {
    return averages;
  }
}

package com.aci.smart_onboarding.enums;

import lombok.Getter;

@Getter
public enum ContextName {
  CHAT("chat"),
  SUMMARY("summary"),
  TEMPLATE("template"),
  DEBUGGING("debugging"),
  EXPLANATION("explanation"),
  DECISION("decision"),
  PREFILL("prefill"),
  LEGACY_PREFILL("legacyPrefill"),
  MAPPED_RULE("mappedRule"),
  UAT_TEST_GENERATION("testRigorUAT"),
  WALLETRON_SUMMARY("walletronSummary");
  private final String prompt;

  ContextName(String prompt) {
    this.prompt = prompt;
  }
}

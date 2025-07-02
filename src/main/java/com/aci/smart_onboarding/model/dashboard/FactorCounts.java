package com.aci.smart_onboarding.model.dashboard;

/** Helper class to store yes/no counts for factors like Walletron or ACH. */
public class FactorCounts {
  private final int yesCount;
  private final int noCount;

  public FactorCounts(int yesCount, int noCount) {
    this.yesCount = yesCount;
    this.noCount = noCount;
  }

  public int getYesCount() {
    return yesCount;
  }

  public int getNoCount() {
    return noCount;
  }
}

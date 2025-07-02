package com.aci.smart_onboarding.model.dashboard;

import java.time.LocalDateTime;

/** Represents a time segment for querying and labeling results in dashboards. */
public class TimeSegment {
  private final LocalDateTime startDate;
  private final LocalDateTime endDate;
  private final String label;

  public TimeSegment(LocalDateTime startDate, LocalDateTime endDate, String label) {
    this.startDate = startDate;
    this.endDate = endDate;
    this.label = label;
  }

  public LocalDateTime getStartDate() {
    return startDate;
  }

  public LocalDateTime getEndDate() {
    return endDate;
  }

  public String getLabel() {
    return label;
  }
}

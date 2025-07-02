package com.aci.smart_onboarding.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO to represent the response for the BRD status counts dashboard widget. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BrdStatusCountResponse {

  /** The scope of the query (me or team). */
  private String scope;

  /** The logged-in PM username. */
  private String loggedinPm;

  /** List of status counts. */
  private List<StatusCount> brdStatusCounts;

  /** Inner class to represent a single status and its count. */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class StatusCount {
    private String status;
    private int count;
  }
}

package com.aci.smart_onboarding.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DTO for the BRDs by industry vertical response. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class BrdVerticalCountResponse {

  /** The scope of the query (me or team). */
  private String scope;

  /** The BRD scope (open or all). */
  private String brdScope;

  /** The period filter (month, quarter, year) - only applicable when brdScope=all. */
  private String period;

  /** The logged-in PM username. */
  private String loggedinPm;

  /** List of BRD counts by industry vertical. */
  @JsonProperty("verticalCounts")
  @Builder.Default
  private List<VerticalCount> verticalCounts = new ArrayList<>();

  /** Inner class to represent a single vertical count and percentage. */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class VerticalCount {
    private String industryName;
    private int brdCount;
    private double percentage;
  }
}

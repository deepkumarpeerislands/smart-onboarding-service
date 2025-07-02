package com.aci.smart_onboarding.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response DTO for bulk site creation operations. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing details of bulk created sites")
public class BulkSiteResponse {

  /** The number of sites created in this bulk operation */
  @Schema(description = "Number of sites created", example = "10")
  private int siteCount;

  /** The BRD ID that these sites belong to */
  @Schema(description = "BRD ID of the created sites", example = "BRD0003")
  private String brdId;

  /** List of basic details for each created site */
  @Schema(description = "List of created sites with basic information")
  private List<SiteBasicDetails> sites;

  /** Basic site details for bulk creation response */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SiteBasicDetails {
    @Schema(description = "Site ID", example = "SITE_BRD0003_001")
    private String siteId;

    @Schema(description = "Site name", example = "Bulk Clone 1")
    private String siteName;

    @Schema(description = "Site unique identifier code", example = "BC001")
    private String identifierCode;

    @Schema(description = "BRD form data for the site")
    private BrdForm brdForm;
  }
}

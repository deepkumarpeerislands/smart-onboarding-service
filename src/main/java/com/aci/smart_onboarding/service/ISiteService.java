package com.aci.smart_onboarding.service;

import com.aci.smart_onboarding.dto.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

public interface ISiteService {
  Mono<ResponseEntity<Api<SiteResponse>>> createDivision(SiteRequest divisionRequest);

  Mono<ResponseEntity<Api<SiteResponse>>> getDivisionsByBrdId(
      @NotBlank(message = "BRD ID cannot be blank") String brdId);

  /**
   * Partially updates the brdForm fields of all sites associated with a specific brdId. This method
   * handles multi-level nested properties efficiently and can be used for both simple field updates
   * and complex section updates. It mirrors the behavior of BRD partial updates but applies changes
   * to the brdForm property of all sites with the given brdId.
   *
   * @param brdId The BRD ID of the sites to update
   * @param brdFormFields Map containing field paths and their new values, can include nested paths
   *     with dot notation
   * @return ResponseEntity containing updated sites information
   */
  Mono<ResponseEntity<Api<SiteResponse>>> updateBrdFormFieldsForAllSites(
      @NotBlank(message = "BRD ID cannot be blank") String brdId,
      Map<String, Object> brdFormFields);

  Mono<ResponseEntity<Api<SingleSiteResponse>>> updateSite(
      @NotBlank(message = "Site ID cannot be blank") String siteId,
      SiteRequest.SiteDetails siteDetails);

  Mono<ResponseEntity<Api<BrdComparisonResponse>>> compareBrdAndSiteBrdForm(
      @NotBlank(message = "Site ID cannot be blank") String siteId,
      @NotBlank(message = "Section name cannot be blank") String sectionName);

  Mono<ResponseEntity<Api<List<SingleSiteResponse>>>> updateMultipleSites(
      @NotEmpty(message = "Site updates list cannot be empty") List<SiteUpdateRequest> siteUpdates);

  /**
   * Creates multiple site BRDs in bulk for a specified BRD. All created sites will be exact clones
   * of the organization-level (base) BRD. Sites will be named sequentially as "Bulk Clone 1", "Bulk
   * Clone 2", etc.
   *
   * @param brdId The BRD ID for which to create sites
   * @param numberOfSites The number of sites to create (maximum 100)
   * @return ResponseEntity containing information about the created sites
   */
  Mono<ResponseEntity<Api<BulkSiteResponse>>> bulkCreateSites(
      @NotBlank(message = "BRD ID cannot be blank") String brdId,
      @Positive(message = "Number of sites must be greater than 0")
          @Max(value = 100, message = "Maximum number of sites per bulk operation is 100")
          int numberOfSites);

  Mono<ResponseEntity<Api<Void>>> deleteMultipleSites(
      @NotEmpty(message = "Site IDs list cannot be empty") List<String> siteIds);

  Mono<String> getSiteBrdId(String siteId);

  Throwable handleErrors(Throwable ex);

  Mono<Double> calculateBrdScore(String brdId);

  Mono<Double> calculateSiteScore(String brdId);

  /**
   * Deletes a single site by its ID.
   *
   * @param siteId The ID of the site to delete
   * @return ResponseEntity with success/failure message
   */
  Mono<ResponseEntity<Api<Void>>> deleteSite(
      @NotBlank(message = "Site ID cannot be blank") String siteId);

  /**
   * Clones an existing site with all its details. The cloned site will have "(Copy)" appended to
   * its name.
   *
   * @param siteId The ID of the site to clone
   * @return A Mono containing the response with the cloned site details
   */
  Mono<ResponseEntity<Api<SiteResponse.DivisionDetails>>> cloneSite(String siteId);

  Mono<ResponseEntity<Api<SiteDifferencesResponse>>> getSiteDifferences(String brdId);
}

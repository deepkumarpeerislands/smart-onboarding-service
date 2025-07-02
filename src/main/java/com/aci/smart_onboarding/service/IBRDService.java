package com.aci.smart_onboarding.service;

import com.aci.smart_onboarding.dto.*;
import com.aci.smart_onboarding.exception.NotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Mono;

@Validated
public interface IBRDService {

  Mono<ResponseEntity<Api<BRDResponse>>> createBrdForm(
      @Valid @NotNull(message = "brdRequest can not be null") BRDRequest brdRequest);

  Mono<ResponseEntity<Api<BRDResponse>>> getBrdById(
      @NotBlank(message = "brdFormId can't be null or empty") String brdFormId);

  Mono<ResponseEntity<Api<BRDResponse>>> updateBrdPartiallyWithOrderedOperations(
      String brdFormId, Map<String, Object> fields);

  Mono<ResponseEntity<Api<BRDSectionResponse<Object>>>> getBrdSectionById(
      @NotBlank(message = "brdFormId can't be null or empty") String brdFormId,
      @NotBlank(message = "sectionNamed can't be null or empty") String sectionName);

  Mono<ResponseEntity<Api<BRDCountDataResponse>>> getBrdList(int page, int size);

  Throwable handleErrors(Throwable ex);

  Mono<ResponseEntity<Api<Page<BRDSearchResponse>>>> searchBRDs(
      String searchText, int page, int size, String sortBy, String sortDirection);

  Mono<ResponseEntity<Api<BRDResponse>>> updateBrdStatus(
      String brdFormId, String status, String comment);

  /**
   * Gets all available industry verticals from the brd_template_config collection. Appends "Other"
   * to the list of verticals.
   *
   * @return A Mono containing a list of industry verticals
   */
  Mono<List<String>> getIndustryVerticals();

  /**
   * Gets all BRDs where the creator field matches the provided username. Only accessible by users
   * with MANAGER role.
   *
   * @param username The PM's username to search for
   * @return A Mono containing a list of BRDs created by the PM
   * @throws AccessDeniedException if the user does not have MANAGER role
   * @throws NotFoundException if no BRDs are found for the given username
   */
  Mono<List<BRDResponse>> getBrdsByPmUsername(String username);
}

package com.aci.smart_onboarding.controller;

import com.aci.smart_onboarding.constants.BrdConstants;
import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.UATTestCaseBrdTypeSearchRequest;
import com.aci.smart_onboarding.dto.UATTestCaseDTO;
import com.aci.smart_onboarding.dto.UATTestCaseGroupedResponseDTO;
import com.aci.smart_onboarding.dto.UATTestCaseRequestResponseDTO;
import com.aci.smart_onboarding.enums.PortalTypes;
import com.aci.smart_onboarding.enums.TestStatus;
import com.aci.smart_onboarding.model.BRD;
import com.aci.smart_onboarding.repository.BRDRepository;
import com.aci.smart_onboarding.security.service.BRDSecurityService;
import com.aci.smart_onboarding.service.IUATTestCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/** Controller for UAT test case operations. */
@RestController
@RequestMapping("${api.default.path}/uat-test-cases")
@RequiredArgsConstructor
@Tag(name = "UAT Test Cases", description = "UAT test case management APIs")
@Slf4j
public class UATTestCaseController {

  private static final String TEST_CASES_RETRIEVED_SUCCESSFULLY =
      "Test cases retrieved successfully";
  private static final String TEST_CASE_CREATED_SUCCESSFULLY = "Test case created successfully";
  private static final String TEST_CASE_UPDATED_SUCCESSFULLY = "Test case updated successfully";
  private static final String ACCESS_DENIED_MESSAGE = "Access denied: Only PM role can access this endpoint.";
  private static final String BRD_NOT_FOUND_MESSAGE = "BRD not found with ID: ";
  private static final String ERROR_KEY = "error";
  
  private final IUATTestCaseService service;
  private final BRDSecurityService brdSecurityService;
  private final BRDRepository brdRepository;

  @Operation(
      summary = "Create test case",
      description = "Creates a new UAT test case",
      responses = {
        @ApiResponse(
            responseCode = "201",
            description = "Test case created successfully",
            content =
                @Content(schema = @Schema(implementation = UATTestCaseRequestResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
      })
  @PostMapping
  public Mono<ResponseEntity<Api<UATTestCaseRequestResponseDTO>>> createTestCase(
      @Valid @RequestBody UATTestCaseRequestResponseDTO requestDTO) {
    return service
        .createTestCase(requestDTO)
        .map(
            created ->
                ResponseEntity.status(HttpStatus.CREATED)
                    .body(
                        new Api<>(
                            BrdConstants.SUCCESSFUL,
                            TEST_CASE_CREATED_SUCCESSFULLY,
                            Optional.of(created),
                            Optional.empty())));
  }

  @Operation(
      summary = "Update test case",
      description = "Updates an existing UAT test case",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Test case updated successfully",
            content = @Content(schema = @Schema(implementation = UATTestCaseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Test case not found")
      })
  @PutMapping("/{id}")
  public Mono<ResponseEntity<Api<UATTestCaseDTO>>> updateTestCase(
      @Parameter(description = "Test case ID") @PathVariable String id,
      @Valid @RequestBody UATTestCaseDTO testCaseDTO) {
    return service
        .updateTestCase(id, testCaseDTO)
        .map(
            updated ->
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        TEST_CASE_UPDATED_SUCCESSFULLY,
                        Optional.of(updated),
                        Optional.empty())));
  }

  @Operation(
      summary = "Delete test case",
      description = "Deletes a UAT test case",
      responses = {
        @ApiResponse(responseCode = "204", description = "Test case deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Test case not found")
      })
  @DeleteMapping("/{id}")
  public Mono<ResponseEntity<Void>> deleteTestCase(
      @Parameter(description = "Test case ID") @PathVariable String id) {
    return service.deleteTestCase(id).then(Mono.just(ResponseEntity.noContent().build()));
  }

  @Operation(
      summary = "Get test case by ID",
      description = "Retrieves a UAT test case by its ID",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Test case retrieved successfully",
            content = @Content(schema = @Schema(implementation = UATTestCaseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Test case not found")
      })
  @GetMapping("/{id}")
  public Mono<ResponseEntity<Api<UATTestCaseDTO>>> getTestCase(
      @Parameter(description = "Test case ID") @PathVariable String id) {
    return service
        .getTestCase(id)
        .map(
            testCase ->
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        TEST_CASES_RETRIEVED_SUCCESSFULLY,
                        Optional.of(testCase),
                        Optional.empty())));
  }

  @Operation(
      summary = "Get all test cases",
      description = "Retrieves all UAT test cases",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Test cases retrieved successfully",
            content = @Content(schema = @Schema(implementation = UATTestCaseDTO.class)))
      })
  @GetMapping
  public Mono<ResponseEntity<Api<List<UATTestCaseDTO>>>> getAllTestCases() {
    return service
        .getAllTestCases()
        .collectList()
        .map(
            testCases ->
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        TEST_CASES_RETRIEVED_SUCCESSFULLY,
                        Optional.of(testCases),
                        Optional.empty())));
  }

  @Operation(
      summary = "Get test cases by status",
      description = "Retrieves all UAT test cases by status",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Test cases retrieved successfully",
            content = @Content(schema = @Schema(implementation = UATTestCaseDTO.class)))
      })
  @GetMapping("/status/{status}")
  public Mono<ResponseEntity<Api<List<UATTestCaseDTO>>>> getTestCasesByStatus(
      @Parameter(description = "Test status (PASSED/FAILED)") @PathVariable TestStatus status) {
    return service
        .getTestCasesByStatus(status)
        .collectList()
        .map(
            testCases ->
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        TEST_CASES_RETRIEVED_SUCCESSFULLY,
                        Optional.of(testCases),
                        Optional.empty())));
  }

  @Operation(
      summary = "Get test cases by BRD ID",
      description = "Retrieves all UAT test cases for a specific BRD ID, grouped by feature name",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Test cases retrieved successfully",
            content =
                @Content(schema = @Schema(implementation = UATTestCaseGroupedResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Access denied: Only PM role can access"),
        @ApiResponse(responseCode = "404", description = "BRD not found")
      })
  @GetMapping("/brd/{brdId}")
  public Mono<ResponseEntity<Api<List<UATTestCaseGroupedResponseDTO>>>> getTestCasesByBrdId(
      @Parameter(description = "BRD ID") @PathVariable String brdId) {
    log.info("Getting test cases by BRD ID: {}", brdId);
    
    return brdSecurityService.getCurrentUserRole()
        .flatMap(userRole -> {
          if (!SecurityConstants.PM_ROLE.equals(userRole)) {
            log.warn("Access denied: User role {} is not PM", userRole);
            Map<String, String> errors = new HashMap<>();
            errors.put(ERROR_KEY, ACCESS_DENIED_MESSAGE);
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new Api<List<UATTestCaseGroupedResponseDTO>>(BrdConstants.FAILURE, ACCESS_DENIED_MESSAGE, Optional.empty(), Optional.of(errors))));
          }
          
          return brdRepository.findByBrdId(brdId)
              .flatMap(brd -> service.getTestCasesByBrdId(brdId)
                  .collectList()
                  .map(testCases -> {
                    // Group by both feature name and UAT type
                    Map<String, Map<PortalTypes, List<UATTestCaseDTO>>> groupedByFeatureAndType =
                        testCases.stream()
                            .collect(
                                Collectors.groupingBy(
                                    UATTestCaseDTO::getFeatureName,
                                    Collectors.groupingBy(UATTestCaseDTO::getUatType)));

                    List<UATTestCaseGroupedResponseDTO> groupedResponse =
                        groupedByFeatureAndType.entrySet().stream()
                            .flatMap(
                                featureEntry ->
                                    featureEntry.getValue().entrySet().stream()
                                        .map(
                                            typeEntry -> {
                                              UATTestCaseGroupedResponseDTO groupedDTO =
                                                  new UATTestCaseGroupedResponseDTO();
                                              groupedDTO.setFeatureName(featureEntry.getKey());
                                              groupedDTO.setUatType(typeEntry.getKey());

                                              List<UATTestCaseGroupedResponseDTO.TestDetailDTO>
                                                  testDetails =
                                                      typeEntry.getValue().stream()
                                                          .map(
                                                              testCase -> {
                                                                UATTestCaseGroupedResponseDTO
                                                                        .TestDetailDTO
                                                                    detail =
                                                                        new UATTestCaseGroupedResponseDTO
                                                                            .TestDetailDTO();
                                                                detail.setTestName(
                                                                    testCase.getTestName());
                                                                detail.setScenario(
                                                                    testCase.getScenario());
                                                                detail.setStatus(testCase.getStatus());
                                                                return detail;
                                                              })
                                                          .toList();

                                              groupedDTO.setTestDetails(testDetails);
                                              return groupedDTO;
                                            }))
                            .toList();

                    return ResponseEntity.ok(
                        new Api<>(
                            BrdConstants.SUCCESSFUL,
                            TEST_CASES_RETRIEVED_SUCCESSFULLY,
                            Optional.of(groupedResponse),
                            Optional.empty()));
                  }))
              .switchIfEmpty(Mono.defer(() -> {
                log.warn("BRD not found with ID: {}", brdId);
                Map<String, String> errors = new HashMap<>();
                errors.put(ERROR_KEY, BRD_NOT_FOUND_MESSAGE + brdId);
                return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new Api<List<UATTestCaseGroupedResponseDTO>>(BrdConstants.FAILURE, BRD_NOT_FOUND_MESSAGE + brdId, Optional.empty(), Optional.of(errors))));
              }));
        })
        .onErrorResume(throwable -> {
          log.error("Error getting test cases by BRD ID: {}", brdId, throwable);
          
          // Check if it's a security-related exception and return 403
          if (throwable instanceof SecurityException || 
              throwable.getMessage() != null && 
              (throwable.getMessage().contains("Access denied") || 
               throwable.getMessage().contains("Forbidden") ||
               throwable.getMessage().contains("Unauthorized"))) {
            Map<String, String> errors = new HashMap<>();
            errors.put(ERROR_KEY, ACCESS_DENIED_MESSAGE);
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new Api<List<UATTestCaseGroupedResponseDTO>>(BrdConstants.FAILURE, ACCESS_DENIED_MESSAGE, Optional.empty(), Optional.of(errors))));
          }
          
          return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error occurred");
        });
  }

  @Operation(
      summary = "Search test cases by BRD ID and UAT type",
      description =
          "Searches and retrieves UAT test cases matching the specified BRD ID and UAT type",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Test cases retrieved successfully",
            content = @Content(schema = @Schema(implementation = UATTestCaseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
      })
  @PostMapping("/search-by-brd-and-type")
  public Mono<ResponseEntity<Api<List<UATTestCaseDTO>>>> searchTestCasesByBrdAndType(
      @Valid @RequestBody UATTestCaseBrdTypeSearchRequest searchRequest) {
    return service
        .getTestCasesByBrdIdAndUatType(searchRequest.getBrdId(), searchRequest.getUatType())
        .collectList()
        .map(
            testCases ->
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        TEST_CASES_RETRIEVED_SUCCESSFULLY,
                        Optional.of(testCases),
                        Optional.empty())));
  }

  @Operation(
      summary = "Get test cases by feature name",
      description = "Retrieves all UAT test cases for a specific feature name",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Test cases retrieved successfully",
            content = @Content(schema = @Schema(implementation = UATTestCaseDTO.class)))
      })
  @GetMapping("/feature/{featureName}")
  public Mono<ResponseEntity<Api<List<UATTestCaseDTO>>>> getTestCasesByFeatureName(
      @Parameter(description = "Feature name") @PathVariable String featureName) {
    return service
        .getTestCasesByFeatureName(featureName)
        .collectList()
        .map(
            testCases ->
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        TEST_CASES_RETRIEVED_SUCCESSFULLY,
                        Optional.of(testCases),
                        Optional.empty())));
  }

  private Mono<ResponseEntity<Api<List<UATTestCaseGroupedResponseDTO>>>> buildErrorResponse(HttpStatus status, String message) {
    Map<String, String> errors = new HashMap<>();
    errors.put(ERROR_KEY, message);
    return Mono.just(ResponseEntity.status(status)
        .body(new Api<List<UATTestCaseGroupedResponseDTO>>(BrdConstants.FAILURE, message, Optional.empty(), Optional.of(errors))));
  }
}

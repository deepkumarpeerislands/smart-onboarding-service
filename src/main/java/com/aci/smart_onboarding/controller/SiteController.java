package com.aci.smart_onboarding.controller;

import com.aci.smart_onboarding.constants.BrdConstants;
import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.constants.SiteConstants;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.BRDResponse;
import com.aci.smart_onboarding.dto.BrdComparisonResponse;
import com.aci.smart_onboarding.dto.BulkSiteRequest;
import com.aci.smart_onboarding.dto.BulkSiteResponse;
import com.aci.smart_onboarding.dto.SingleSiteResponse;
import com.aci.smart_onboarding.dto.SiteDifferencesResponse;
import com.aci.smart_onboarding.dto.SiteRequest;
import com.aci.smart_onboarding.dto.SiteResponse;
import com.aci.smart_onboarding.dto.SiteUpdateRequest;
import com.aci.smart_onboarding.exception.NotFoundException;
import com.aci.smart_onboarding.repository.BRDRepository;
import com.aci.smart_onboarding.security.service.BRDSecurityService;
import com.aci.smart_onboarding.service.IBRDService;
import com.aci.smart_onboarding.service.ISiteService;
import com.aci.smart_onboarding.swagger.SiteRequestAndReponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@Validated
@RequiredArgsConstructor
@Tag(
    name = "Site Management",
    description = "APIs for managing Sites and BRD Identification Details")
@RequestMapping(value = "${api.default.path}/site", name = "Site Management")
public class SiteController {

  private final ISiteService siteService;
  private final IBRDService brdService;
  private final BRDRepository brdRepository;
  private final BRDSecurityService securityService;
  private static final Logger log = LoggerFactory.getLogger(SiteController.class);

  @Operation(
      summary = "Create Sites for BRD",
      description = "Creates new sites and associates them with a BRD")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "Site details to create",
      required = true,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = SiteRequest.class),
              examples = {
                @ExampleObject(
                    name = "Sample Site Creation Request",
                    value =
                        """
                                        {
                                          "brdId": "BRD0003",
                                          "wallentronIncluded": true,
                                          "achEncrypted": false,
                                          "siteList": [
                                            {
                                              "siteId": "SITE003",
                                              "siteName": "New Site",
                                              "identifierCode": "NEW",
                                              "description": "New Site Description"
                                            }
                                          ]
                                        }
                                        """)
              }))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Sites created successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema =
                        @Schema(
                            implementation = Api.class,
                            subTypes = {SiteResponse.class}))),
        @ApiResponse(
            responseCode = "404",
            description = "BRD not found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid input data",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class)))
      })
  @PostMapping
  public Mono<ResponseEntity<Api<SiteResponse>>> createSites(
      @Valid @RequestBody Mono<SiteRequest> divisionRequest) {
    return divisionRequest
        .doOnNext(request -> log.debug("Processing site creation request: {}", request))
        .flatMap(
            request ->
                securityService
                    .getCurrentUserRole()
                    .flatMap(
                        role -> {
                          if (!SecurityConstants.PM_ROLE.equals(role)) {
                            return Mono.error(
                                new AccessDeniedException(
                                    "Only Project Managers (PM) can create sites"));
                          }
                          return brdRepository
                              .findByBrdId(request.getBrdId())
                              .switchIfEmpty(
                                  Mono.error(
                                      new NotFoundException(
                                          SiteConstants.BRD_NOT_FOUND + " " + request.getBrdId())))
                              .flatMap(brd -> brdService.getBrdById(brd.getBrdFormId()))
                              .flatMap(
                                  brdResponse -> {
                                    String status =
                                        brdResponse
                                            .getBody()
                                            .getData()
                                            .map(BRDResponse::getStatus)
                                            .orElseThrow(
                                                () ->
                                                    new NotFoundException(
                                                        SiteConstants.BRD_STATUS_NOT_FOUND));

                                    return securityService
                                        .withSecurityCheck(status)
                                        .then(siteService.createDivision(request));
                                  });
                        }))
        .doOnSuccess(response -> log.debug("Sites created successfully"))
        .doOnError(error -> log.error("Error creating sites: {}", error.getMessage()))
        .onErrorResume(
            e -> {
              if (e instanceof AccessDeniedException) {
                return Mono.just(
                    ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(
                            new Api<>(
                                BrdConstants.FAILURE,
                                e.getMessage(),
                                Optional.empty(),
                                Optional.empty())));
              }
              if (e instanceof NotFoundException) {
                return Mono.just(
                    ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(
                            new Api<>(
                                BrdConstants.FAILURE,
                                e.getMessage(),
                                Optional.empty(),
                                Optional.empty())));
              }
              return Mono.just(
                  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(
                          new Api<>(
                              BrdConstants.FAILURE,
                              "An unexpected error occurred",
                              Optional.empty(),
                              Optional.empty())));
            });
  }

  @Operation(
      summary = "Get all sites for a BRD",
      description = "Retrieves all sites associated with a specific BRD ID")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Sites retrieved successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema =
                        @Schema(
                            implementation = Api.class,
                            subTypes = {SiteResponse.class}),
                    examples = {
                      @ExampleObject(
                          name = "Success Response",
                          value = SiteRequestAndReponse.GET_SITES_BY_BRD_SUCCESS_EXAMPLE)
                    })),
        @ApiResponse(
            responseCode = "404",
            description = "BRD not found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Not Found Error",
                          value = SiteRequestAndReponse.GET_SITES_BY_BRD_NOT_FOUND_EXAMPLE)
                    })),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid BRD ID",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Bad Request Error",
                          value = SiteRequestAndReponse.GET_SITES_BY_BRD_BAD_REQUEST_EXAMPLE)
                    })),
        @ApiResponse(
            responseCode = "500",
            description = "Bad Request - Invalid input data",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Internal server error",
                          value = SiteRequestAndReponse.INTERNAL_SERVER_ERROR)
                    }))
      })
  @GetMapping("/brd/{brdId}")
  public Mono<ResponseEntity<Api<SiteResponse>>> getSitesByBrdId(
      @PathVariable @NotBlank(message = "BRD ID cannot be blank") String brdId) {
    return brdRepository
        .findByBrdId(brdId)
        .switchIfEmpty(
            Mono.error(new NotFoundException(String.format("BRD not found with id: %s", brdId))))
        .flatMap(brd -> brdService.getBrdById(brd.getBrdFormId()))
        .flatMap(
            brdResponse -> {
              String status =
                  brdResponse
                      .getBody()
                      .getData()
                      .map(BRDResponse::getStatus)
                      .orElseThrow(() -> new NotFoundException("BRD status not found"));

              return securityService
                  .withSecurityCheck(status)
                  .then(siteService.getDivisionsByBrdId(brdId))
                  .flatMap(
                      response ->
                          siteService
                              .calculateBrdScore(brdId)
                              .map(
                                  brdScore -> {
                                    SiteResponse responseWithScore =
                                        response
                                            .getBody()
                                            .getData()
                                            .map(
                                                data -> {
                                                  data.setScore(brdScore);
                                                  return data;
                                                })
                                            .orElse(response.getBody().getData().orElse(null));

                                    return ResponseEntity.ok()
                                        .body(
                                            new Api<>(
                                                BrdConstants.SUCCESSFUL,
                                                "Sites retrieved successfully",
                                                Optional.of(responseWithScore),
                                                Optional.empty()));
                                  }));
            })
        .onErrorResume(
            e -> {
              if (e instanceof AccessDeniedException) {
                return Mono.just(
                    ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(
                            new Api<>(
                                BrdConstants.FAILURE,
                                e.getMessage(),
                                Optional.empty(),
                                Optional.empty())));
              }
              if (e instanceof NotFoundException) {
                return Mono.just(
                    ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(
                            new Api<>(
                                BrdConstants.FAILURE,
                                e.getMessage(),
                                Optional.empty(),
                                Optional.empty())));
              }
              return Mono.just(
                  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(
                          new Api<>(
                              BrdConstants.FAILURE,
                              "Error retrieving sites: " + e.getMessage(),
                              Optional.empty(),
                              Optional.empty())));
            });
  }

  @Operation(
      summary = "Update a single site",
      description =
          "Updates specific fields for a single site identified by its ID. Only the fields provided in the request will be updated, other fields will remain unchanged.")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "Site details to update",
      required = true,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = SiteRequest.SiteDetails.class),
              examples = {
                @ExampleObject(
                    name = "Complete Update Example",
                    description = "Example with all updateable fields",
                    value = SiteRequestAndReponse.UPDATE_SITE_REQUEST_EXAMPLE),
                @ExampleObject(
                    name = "Partial Update - Basic Fields",
                    description = "Example updating only basic site information",
                    value = SiteRequestAndReponse.UPDATE_SITE_PARTIAL_REQUEST_EXAMPLE),
                @ExampleObject(
                    name = "Partial Update - BRD Form Fields",
                    description = "Example updating only specific BRD form fields",
                    value = SiteRequestAndReponse.UPDATE_BRDFORM_PARTIAL_REQUEST_EXAMPLE)
              }))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Site updated successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema =
                        @Schema(
                            implementation = Api.class,
                            subTypes = {SingleSiteResponse.class}),
                    examples = {
                      @ExampleObject(
                          name = "Success Response",
                          value = SiteRequestAndReponse.UPDATE_SITE_RESPONSE_EXAMPLE)
                    })),
        @ApiResponse(
            responseCode = "404",
            description = "Site not found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Not Found Error",
                          value = SiteRequestAndReponse.UPDATE_SITE_ERROR_RESPONSE_EXAMPLE)
                    })),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid input data",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Bad Request Error",
                          value = SiteRequestAndReponse.UPDATE_SITE_BAD_REQUEST_ERROR_EXAMPLE)
                    })),
        @ApiResponse(
            responseCode = "500",
            description = "Bad Request - Invalid input data",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Internal server error",
                          value = SiteRequestAndReponse.INTERNAL_SERVER_ERROR)
                    }))
      })
  @PatchMapping("/{siteId}")
  public Mono<ResponseEntity<Api<SingleSiteResponse>>> updateSite(
      @PathVariable @NotBlank(message = "Site ID cannot be blank") String siteId,
      @RequestBody SiteRequest.SiteDetails siteDetails) {
    return siteService
        .getSiteBrdId(siteId)
        .flatMap(
            brdId ->
                brdRepository
                    .findByBrdId(brdId)
                    .switchIfEmpty(
                        Mono.error(new NotFoundException(SiteConstants.BRD_NOT_FOUND + brdId)))
                    .flatMap(
                        brd ->
                            brdService
                                .getBrdById(brd.getBrdFormId())
                                .flatMap(
                                    brdResponse -> {
                                      String status =
                                          brdResponse
                                              .getBody()
                                              .getData()
                                              .orElseThrow(
                                                  () ->
                                                      new NotFoundException(
                                                          "BRD not found with id: "
                                                              + brd.getBrdFormId()))
                                              .getStatus();
                                      return securityService
                                          .withSecurityCheck(status)
                                          .then(siteService.updateSite(siteId, siteDetails));
                                    })))
        .onErrorResume(
            e -> {
              if (e instanceof AccessDeniedException) {
                return Mono.just(
                    ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(
                            new Api<>(
                                BrdConstants.FAILURE,
                                e.getMessage(),
                                Optional.empty(),
                                Optional.empty())));
              }
              if (e instanceof NotFoundException) {
                return Mono.just(
                    ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(
                            new Api<>(
                                BrdConstants.FAILURE,
                                e.getMessage(),
                                Optional.empty(),
                                Optional.empty())));
              }
              return Mono.error(e);
            });
  }

  @Operation(
      summary = "Update multiple sites",
      description = "Updates multiple sites in parallel with their respective details")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Sites updated successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema =
                        @Schema(
                            implementation = Api.class,
                            subTypes = {SingleSiteResponse.class}),
                    examples = {
                      @ExampleObject(
                          name = "Success Response",
                          value =
                              """
                                                            {
                                                              "status": "SUCCESS",
                                                              "message": "Sites updated successfully",
                                                              "data": [
                                                                {
                                                                  "id": "site1",
                                                                  "brdId": "BRD0003",
                                                                  "siteId": "SITE_BRD0003_001",
                                                                  "siteName": "Updated Site 1",
                                                                  "identifierCode": "UPD001",
                                                                  "description": "Updated Description 1"
                                                                },
                                                                {
                                                                  "id": "site2",
                                                                  "brdId": "BRD0003",
                                                                  "siteId": "SITE_BRD0003_002",
                                                                  "siteName": "Updated Site 2",
                                                                  "identifierCode": "UPD002",
                                                                  "description": "Updated Description 2"
                                                                }
                                                              ],
                                                              "error": null
                                                            }
                                                            """)
                    })),
        @ApiResponse(
            responseCode = "404",
            description = "One or more sites not found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid input data",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class)))
      })
  @PatchMapping("/bulk")
  public Mono<ResponseEntity<Api<List<SingleSiteResponse>>>> updateMultipleSites(
      @Valid @RequestBody List<SiteUpdateRequest> siteUpdates) {
    return siteService.updateMultipleSites(siteUpdates);
  }

  @Operation(
      summary = "Compare BRD and Site BRD Form Section",
      description =
          "Compares a specific section of the main BRD with a site-specific BRD form and returns the differences")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Comparison completed successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema =
                        @Schema(
                            implementation = Api.class,
                            subTypes = {BrdComparisonResponse.class}),
                    examples = {
                      @ExampleObject(
                          name = "Success Response",
                          value =
                              """
                                                        {
                                                          "status": "SUCCESS",
                                                          "message": "BRD and Site BRD form comparison completed successfully",
                                                          "data": {
                                                            "brdId": "BRD0003",
                                                            "siteId": "SITE_BRD0003_001",
                                                            "siteName": "Test Site",
                                                            "differences": {
                                                              "clientInformation": {
                                                                "companyName": true,
                                                                "operationsContactName": false
                                                              }
                                                            }
                                                          },
                                                          "errors": null
                                                        }
                                                        """)
                    })),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Site or BRD not found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class)))
      })
  @GetMapping("/{site_id}/brd-comparison/{section_name}")
  public Mono<ResponseEntity<Api<BrdComparisonResponse>>> compareBrdAndSiteBrdForm(
      @PathVariable("site_id") String siteId, @PathVariable("section_name") String sectionName) {
    return siteService.compareBrdAndSiteBrdForm(siteId, sectionName);
  }

  @Operation(
      summary = "Delete multiple sites",
      description =
          "Deletes multiple sites by their IDs. If any site is not found, the operation will fail.")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "List of site IDs to delete",
      required = true,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = List.class),
              examples = {
                @ExampleObject(
                    name = "Sample Delete Request",
                    value =
                        """
                                        [
                                          "site1",
                                          "site2",
                                          "site3"
                                        ]
                                        """)
              }))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Sites deleted successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Success Response",
                          value =
                              """
                                                            {
                                                              "status": "SUCCESS",
                                                              "message": "Sites deleted successfully",
                                                              "data": null,
                                                              "errors": null
                                                            }
                                                            """)
                    })),
        @ApiResponse(
            responseCode = "404",
            description = "One or more sites not found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Not Found Error",
                          value =
                              """
                                                            {
                                                              "status": "FAILURE",
                                                              "message": "Site not found with ID: site1",
                                                              "data": null,
                                                              "errors": {
                                                                "errorMessage": "Site not found with ID: site1",
                                                                "timestamp": "2024-03-26T10:30:00"
                                                              }
                                                            }
                                                            """)
                    })),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Empty site IDs list",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Bad Request Error",
                          value =
                              """
                                                            {
                                                              "status": "FAILURE",
                                                              "message": "Site IDs list cannot be empty",
                                                              "data": null,
                                                              "errors": {
                                                                "errorMessage": "Site IDs list cannot be empty",
                                                                "timestamp": "2024-03-26T10:30:00"
                                                              }
                                                            }
                                                            """)
                    }))
      })
  @DeleteMapping("/bulk")
  public Mono<ResponseEntity<Api<Void>>> deleteMultipleSites(
      @RequestBody @NotEmpty(message = "Site IDs list cannot be empty") List<String> siteIds) {
    return siteService.deleteMultipleSites(siteIds);
  }

  @Operation(
      summary = "Bulk Create Sites",
      description =
          "Creates multiple sites at once that are cloned from the organization-level BRD")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "Bulk site creation request",
      required = true,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = BulkSiteRequest.class),
              examples = {
                @ExampleObject(
                    name = "Sample Bulk Site Creation Request",
                    value = SiteRequestAndReponse.BULK_SITE_REQUEST_EXAMPLE)
              }))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Sites created successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema =
                        @Schema(
                            implementation = Api.class,
                            subTypes = {BulkSiteResponse.class}),
                    examples = {
                      @ExampleObject(
                          name = "Success Response",
                          value = SiteRequestAndReponse.BULK_SITE_RESPONSE_EXAMPLE)
                    })),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid input or exceeded maximum limit",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Bad Request Error",
                          value = SiteRequestAndReponse.BULK_SITE_BAD_REQUEST_EXAMPLE)
                    })),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - User does not have PM role",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Not Found - BRD not found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Not Found Error",
                          value = SiteRequestAndReponse.BULK_SITE_NOT_FOUND_EXAMPLE)
                    })),
        @ApiResponse(
            responseCode = "500",
            description = "Internal Server Error",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Internal Server Error",
                          value = SiteRequestAndReponse.INTERNAL_SERVER_ERROR)
                    }))
      })
  @PostMapping("/bulk")
  public Mono<ResponseEntity<Api<BulkSiteResponse>>> bulkCreateSites(
      @Valid @RequestBody BulkSiteRequest request) {

    return securityService
        .getCurrentUserRole()
        .flatMap(
            role -> {
              if (!SecurityConstants.PM_ROLE.equals(role)) {
                return Mono.error(
                    new AccessDeniedException(
                        "Only Project Managers (PM) can create sites in bulk"));
              }

              return brdRepository
                  .findByBrdId(request.getBrdId())
                  .switchIfEmpty(
                      Mono.error(
                          new NotFoundException(
                              String.format("BRD not found with id: %s", request.getBrdId()))))
                  .flatMap(brd -> brdService.getBrdById(brd.getBrdFormId()))
                  .flatMap(
                      brdResponse -> {
                        String status =
                            brdResponse
                                .getBody()
                                .getData()
                                .map(BRDResponse::getStatus)
                                .orElseThrow(() -> new NotFoundException("BRD status not found"));

                        return securityService
                            .withSecurityCheck(status)
                            .then(
                                siteService.bulkCreateSites(
                                    request.getBrdId(), request.getNumberOfSites()));
                      });
            })
        .doOnSuccess(
            response ->
                log.info(
                    "Bulk created {} sites for BRD ID: {}",
                    request.getNumberOfSites(),
                    request.getBrdId()))
        .doOnError(error -> log.error("Error during bulk site creation: {}", error.getMessage()));
  }

  @Operation(
      summary = "Delete a single site",
      description = "Deletes a site by its ID. Only Project Managers (PM) can delete sites.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Site deleted successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Success Response",
                          value =
                              """
                              {
                                  "status": "SUCCESS",
                                  "message": "Site deleted successfully",
                                  "data": null,
                                  "errors": null
                              }
                              """)
                    })),
        @ApiResponse(
            responseCode = "404",
            description = "Site not found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Not Found Error",
                          value =
                              """
                              {
                                  "status": "FAILURE",
                                  "message": "Site not found with ID: SITE001",
                                  "data": null,
                                  "errors": {
                                      "errorMessage": "Site not found with ID: SITE001",
                                      "timestamp": "2024-03-26T10:30:00"
                                  }
                              }
                              """)
                    })),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid site ID",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Bad Request Error",
                          value =
                              """
                              {
                                  "status": "FAILURE",
                                  "message": "Site ID cannot be blank",
                                  "data": null,
                                  "errors": {
                                      "errorMessage": "Site ID cannot be blank",
                                      "timestamp": "2024-03-26T10:30:00"
                                  }
                              }
                              """)
                    })),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - User does not have PM role",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Forbidden Error",
                          value =
                              """
                              {
                                  "status": "FAILURE",
                                  "message": "Only Project Managers (PM) can delete sites",
                                  "data": null,
                                  "errors": null
                              }
                              """)
                    }))
      })
  @DeleteMapping("/{siteId}")
  public Mono<ResponseEntity<Api<Void>>> deleteSite(
      @PathVariable @NotBlank(message = "Site ID cannot be blank") String siteId) {
    return securityService
        .getCurrentUserRole()
        .flatMap(
            role -> {
              if (!SecurityConstants.PM_ROLE.equals(role)) {
                Api<Void> apiResponse =
                    new Api<>(
                        BrdConstants.FAILURE,
                        "Only Project Managers (PM) can delete sites",
                        Optional.empty(),
                        Optional.empty());
                return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(apiResponse));
              }
              return siteService.deleteSite(siteId);
            })
        .doOnError(error -> log.error("Error deleting site {}: {}", siteId, error.getMessage()));
  }

  @Operation(
      summary = "Clone a site",
      description =
          "Creates a copy of an existing site with all its details. The cloned site will have '(Copy)' appended to its name.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Site cloned successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Success Response",
                          value =
                              """
                              {
                                  "status": "SUCCESS",
                                  "message": "Site cloned successfully",
                                  "data": {
                                      "id": "cloned_site_id",
                                      "brdId": "BRD0003",
                                      "siteId": "SITE_BRD0003_001_copy",
                                      "siteName": "Original Site (Copy)",
                                      "description": "Site Description"
                                  },
                                  "errors": null
                              }
                              """)
                    })),
        @ApiResponse(
            responseCode = "404",
            description = "Site not found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Not Found Error",
                          value =
                              """
                              {
                                  "status": "FAILURE",
                                  "message": "Site not found with ID: SITE001",
                                  "data": null,
                                  "errors": null
                              }
                              """)
                    })),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - User does not have PM role",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Forbidden Error",
                          value =
                              """
                              {
                                  "status": "FAILURE",
                                  "message": "Only Project Managers (PM) can clone sites",
                                  "data": null,
                                  "errors": null
                              }
                              """)
                    }))
      })
  @PostMapping("/{siteId}/clone")
  public Mono<ResponseEntity<Api<SiteResponse.DivisionDetails>>> cloneSite(
      @PathVariable @NotBlank(message = "Site ID cannot be blank") String siteId) {
    return securityService
        .getCurrentUserRole()
        .flatMap(
            role -> {
              if (!SecurityConstants.PM_ROLE.equals(role)) {
                Api<SiteResponse.DivisionDetails> apiResponse =
                    new Api<>(
                        BrdConstants.FAILURE,
                        "Only Project Managers (PM) can clone sites",
                        Optional.empty(),
                        Optional.empty());
                return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(apiResponse));
              }
              return siteService.cloneSite(siteId);
            })
        .doOnError(error -> log.error("Error cloning site {}: {}", siteId, error.getMessage()));
  }

  @Operation(
      summary = "Get Site Differences",
      description = "Returns field-level differences between an Org BRD and its related Site BRDs")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Site differences retrieved successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema =
                        @Schema(
                            implementation = Api.class,
                            subTypes = {SiteDifferencesResponse.class}),
                    examples = {
                      @ExampleObject(
                          name = "Success Response",
                          value =
                              """
                              {
                                "status": "SUCCESS",
                                "message": "Site differences retrieved successfully",
                                "data": {
                                  "brdId": "BRD001",
                                  "sites": [
                                    {
                                      "siteId": "SITE001",
                                      "siteName": "Test Site",
                                      "differences": [
                                        {
                                          "fieldName": "companyName",
                                          "orgBrdValue": "Org Company",
                                          "siteBrdValue": "Site Company"
                                        }
                                      ]
                                    }
                                  ]
                                },
                                "errors": null
                              }
                              """)
                    })),
        @ApiResponse(
            responseCode = "404",
            description = "BRD not found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid BRD ID",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - User does not have required role",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class)))
      })
  @GetMapping("/brd/org/{brdId}/site-differences")
  public Mono<ResponseEntity<Api<SiteDifferencesResponse>>> getSiteDifferences(
      @PathVariable @NotBlank(message = "BRD ID cannot be empty") String brdId) {
    return securityService
        .getCurrentUserRole()
        .flatMap(
            role -> {
              if (!(SecurityConstants.PM_ROLE.equals(role) || SecurityConstants.BA_ROLE.equals(role) || SecurityConstants.MANAGER_ROLE.equals(role) )) {
                Api<SiteDifferencesResponse> apiResponse =
                    new Api<>(
                        BrdConstants.FAILURE,
                        "Only Project Managers (PM) or Business Analyst (BA) can view site differences",
                        Optional.empty(),
                        Optional.empty());
                return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(apiResponse));
              }
              return siteService.getSiteDifferences(brdId);
            })
        .doOnError(
            error ->
                log.error(
                    "Error getting site differences for BRD {}: {}", brdId, error.getMessage()));
  }
}

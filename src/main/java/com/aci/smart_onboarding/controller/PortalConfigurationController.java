package com.aci.smart_onboarding.controller;

import com.aci.smart_onboarding.constants.ErrorValidationMessage;
import com.aci.smart_onboarding.constants.PortalConfigConstants;
import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.PortalConfigurationRequest;
import com.aci.smart_onboarding.enums.PortalTypes;
import com.aci.smart_onboarding.exception.ResourceNotFoundException;
import com.aci.smart_onboarding.model.PortalConfiguration;
import com.aci.smart_onboarding.repository.BRDRepository;
import com.aci.smart_onboarding.security.service.BRDSecurityService;
import com.aci.smart_onboarding.service.IPortalConfigurationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.AbstractMap;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/portal-configurations")
@RequiredArgsConstructor
public class PortalConfigurationController {

  private final IPortalConfigurationService portalConfigurationService;
  private final BRDSecurityService brdSecurityService;
  private final BRDRepository brdRepository;
  private static final String ACCESS_DENIED_CREATOR_PM_MESSAGE = "Access denied: Only the creator PM can access this endpoint.";
  private static final String BRD_NOT_FOUND_MESSAGE = "BRD not found with ID: ";

  @Operation(
      summary = "Create portal configuration",
      description = "Creates a new portal configuration for a BRD")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Configuration created successfully",
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
                                    "status": "SUCCESSFUL",
                                    "message": "Portal configuration created successfully",
                                    "data": {
                                        "id": "65f2a1b3c4d5e6f7g8h9i0j1",
                                        "brdId": "BRD001",
                                        "url": "https://agent-portal.example.com",
                                        "type": "AGENT",
                                        "username": "agent_user",
                                        "password": "agent_password"
                                    },
                                    "errors": null
                                }""")
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
                          value =
                              """
                                {
                                    "status": "ERROR",
                                    "message": "Invalid input data",
                                    "data": null,
                                    "errors": {
                                        "type": "Portal type must be either AGENT or CONSUMER"
                                    }
                                }""")
                    })),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Creator PM role required",
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
                                    "status": "ERROR",
                                    "message": "Access denied: Only the creator PM can access this endpoint.",
                                    "data": null,
                                    "errors": {
                                        "error": "Access denied: Only the creator PM can access this endpoint."
                                    }
                                }""")
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
                          value =
                              """
                                {
                                    "status": "ERROR",
                                    "message": "An unexpected error occurred",
                                    "data": null,
                                    "errors": {
                                        "error": "Failed to create portal configuration"
                                    }
                                }""")
                    }))
      })
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<ResponseEntity<Api<List<PortalConfiguration>>>> createPortalConfiguration(
      @Parameter(description = "Portal configuration details") @Valid @RequestBody
          List<PortalConfigurationRequest> requests) {

    return brdSecurityService
        .getCurrentUserRole()
        .flatMap(
            role -> {
              if (!SecurityConstants.PM_ROLE.equals(role)) {
                return Mono.error(
                    new AccessDeniedException(ACCESS_DENIED_CREATOR_PM_MESSAGE));
              }

              // Get the first BRD ID (assuming all configurations are for the same BRD)
              String brdId = requests.isEmpty() ? null : requests.get(0).getBrdId();
              if (brdId == null) {
                return Mono.just(
                    ResponseEntity.badRequest()
                        .body(
                            new Api<List<PortalConfiguration>>(
                                PortalConfigConstants.ERROR,
                                "BRD ID is required",
                                Optional.empty(),
                                Optional.of(
                                    Map.of(ErrorValidationMessage.ERROR_KEY, "BRD ID cannot be null")))));
              }

              return brdRepository
                  .findByBrdId(brdId)
                  .switchIfEmpty(Mono.error(new ResourceNotFoundException(BRD_NOT_FOUND_MESSAGE + brdId)))
                  .flatMap(
                      brd ->
                          brdSecurityService
                              .canModifyBrd(brd.getCreator())
                              .flatMap(
                                  canModify -> {
                                    if (Boolean.FALSE.equals(canModify)) {
                                      return Mono.just(
                                          ResponseEntity.status(HttpStatus.FORBIDDEN)
                                              .body(
                                                  new Api<List<PortalConfiguration>>(
                                                      PortalConfigConstants.ERROR,
                                                      ACCESS_DENIED_CREATOR_PM_MESSAGE,
                                                      Optional.empty(),
                                                      Optional.of(
                                                          Map.of(
                                                              ErrorValidationMessage.ERROR_KEY,
                                                              ACCESS_DENIED_CREATOR_PM_MESSAGE)))));
                                    }

                                    List<PortalConfiguration> configurations =
                                        requests.stream()
                                            .map(
                                                request -> {
                                                  PortalConfiguration configuration =
                                                      new PortalConfiguration();
                                                  configuration.setBrdId(request.getBrdId());
                                                  configuration.setUrl(request.getUrl());
                                                  configuration.setType(request.getType());
                                                  configuration.setUsername(request.getUsername());
                                                  configuration.setPassword(request.getPassword());
                                                  return configuration;
                                                })
                                            .toList();

                                    return Flux.fromIterable(configurations)
                                        .flatMap(portalConfigurationService::createPortalConfiguration)
                                        .collectList()
                                        .map(
                                            configs ->
                                                ResponseEntity.status(HttpStatus.CREATED)
                                                    .body(
                                                        new Api<List<PortalConfiguration>>(
                                                            PortalConfigConstants.SUCCESSFUL,
                                                            PortalConfigConstants.CONFIG_CREATED,
                                                            Optional.of(configs),
                                                            Optional.empty())));
                                  }));
            })
        .onErrorResume(
            e -> {
              if (e instanceof AccessDeniedException) {
                return Mono.just(
                    ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(
                            new Api<List<PortalConfiguration>>(
                                PortalConfigConstants.ERROR,
                                e.getMessage(),
                                Optional.empty(),
                                Optional.of(Map.of(ErrorValidationMessage.ERROR_KEY, e.getMessage())))));
              }
              return Mono.error(e);
            });
  }

  @Operation(
      summary = "Get configurations by BRD ID",
      description = "Retrieves all portal configurations for a specific BRD")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Configurations retrieved successfully",
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
                                    "status": "SUCCESSFUL",
                                    "message": "Portal configurations retrieved successfully",
                                    "data": [
                                        {
                                            "id": "65f2a1b3c4d5e6f7g8h9i0j1",
                                            "brdId": "BRD001",
                                            "url": "https://agent-portal.example.com",
                                            "type": "AGENT",
                                            "username": "agent_user",
                                            "password": "agent_password"
                                        }
                                    ],
                                    "errors": null
                                }""")
                    })),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Creator PM role required",
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
                                    "status": "ERROR",
                                    "message": "Access denied: Only the creator PM can access this endpoint.",
                                    "data": null,
                                    "errors": {
                                        "error": "Access denied: Only the creator PM can access this endpoint."
                                    }
                                }""")
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
                          value =
                              """
                                {
                                    "status": "ERROR",
                                    "message": "BRD not found",
                                    "data": null,
                                    "errors": {
                                        "brdId": "No configurations found for BRD ID: BRD001"
                                    }
                                }""")
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
                          value =
                              """
                                {
                                    "status": "ERROR",
                                    "message": "An unexpected error occurred",
                                    "data": null,
                                    "errors": {
                                        "error": "Failed to retrieve portal configurations"
                                    }
                                }""")
                    }))
      })
  @GetMapping("/brd/{brdId}")
  public Mono<ResponseEntity<Api<List<PortalConfiguration>>>> getPortalConfigurationsByBrdId(
      @Parameter(description = "BRD ID") @PathVariable String brdId) {
    return brdSecurityService
        .getCurrentUserRole()
        .flatMap(
            role -> {
              if (!SecurityConstants.PM_ROLE.equals(role)) {
                return Mono.error(
                    new AccessDeniedException(ACCESS_DENIED_CREATOR_PM_MESSAGE));
              }

              return brdRepository
                  .findByBrdId(brdId)
                  .switchIfEmpty(Mono.error(new ResourceNotFoundException(BRD_NOT_FOUND_MESSAGE + brdId)))
                  .flatMap(
                      brd ->
                          brdSecurityService
                              .canModifyBrd(brd.getCreator())
                              .flatMap(
                                  canModify -> {
                                    if (Boolean.FALSE.equals(canModify)) {
                                      return Mono.error(
                                          new AccessDeniedException(ACCESS_DENIED_CREATOR_PM_MESSAGE));
                                    }

                                    return portalConfigurationService
                                        .getPortalConfigurationsByBrdId(brdId)
                                        .collectList()
                                        .map(
                                            configs ->
                                                ResponseEntity.ok(
                                                    new Api<List<PortalConfiguration>>(
                                                        PortalConfigConstants.SUCCESSFUL,
                                                        PortalConfigConstants.CONFIGS_RETRIEVED,
                                                        Optional.of(configs),
                                                        Optional.empty())));
                                  }));
            })
        .onErrorResume(
            e -> {
              if (e instanceof AccessDeniedException) {
                return Mono.just(
                    ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(
                            new Api<List<PortalConfiguration>>(
                                PortalConfigConstants.ERROR,
                                e.getMessage(),
                                Optional.empty(),
                                Optional.of(Map.of(ErrorValidationMessage.ERROR_KEY, e.getMessage())))));
              }
              return Mono.error(e);
            });
  }

  @Operation(
      summary = "Get configurations by type",
      description = "Retrieves all portal configurations by portal type")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Configurations retrieved successfully",
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
                                    "status": "SUCCESSFUL",
                                    "message": "Portal configurations retrieved successfully",
                                    "data": [
                                        {
                                            "id": "65f2a1b3c4d5e6f7g8h9i0j1",
                                            "brdId": "BRD001",
                                            "url": "https://agent-portal.example.com",
                                            "type": "AGENT",
                                            "username": "agent_user",
                                            "password": "agent_password"
                                        }
                                    ],
                                    "errors": null
                                }""")
                    })),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid portal type",
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
                                    "status": "ERROR",
                                    "message": "Invalid portal type",
                                    "data": null,
                                    "errors": {
                                        "type": "Portal type must be either AGENT or CONSUMER"
                                    }
                                }""")
                    })),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Creator PM role required",
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
                                    "status": "ERROR",
                                    "message": "Access denied: Only the creator PM can access this endpoint.",
                                    "data": null,
                                    "errors": {
                                        "error": "Access denied: Only the creator PM can access this endpoint."
                                    }
                                }""")
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
                          value =
                              """
                                {
                                    "status": "ERROR",
                                    "message": "An unexpected error occurred",
                                    "data": null,
                                    "errors": {
                                        "error": "Failed to retrieve portal configurations"
                                    }
                                }""")
                    }))
      })
  @GetMapping("/type/{type}")
  public Mono<ResponseEntity<Api<List<PortalConfiguration>>>> getPortalConfigurationsByType(
      @Parameter(description = "Portal type (AGENT/CONSUMER)") @PathVariable PortalTypes type) {
    return brdSecurityService
        .getCurrentUserRole()
        .flatMap(
            role -> {
              if (!SecurityConstants.PM_ROLE.equals(role)) {
                return Mono.error(
                    new AccessDeniedException(ACCESS_DENIED_CREATOR_PM_MESSAGE));
              }

              return brdSecurityService
                  .getCurrentUserEmail()
                  .flatMap(
                      currentUserEmail ->
                          portalConfigurationService
                              .getPortalConfigurationsByType(type)
                              .collectList()
                              .flatMap(
                                  allConfigs ->
                                      Flux.fromIterable(allConfigs)
                                          .flatMap(
                                              config ->
                                                  brdRepository
                                                      .findByBrdId(config.getBrdId())
                                                      .map(brd -> new AbstractMap.SimpleEntry<>(config, brd))
                                                      .filter(entry -> entry.getValue() != null)
                                                      .filter(entry -> currentUserEmail.equals(entry.getValue().getCreator()))
                                                      .map(Map.Entry::getKey))
                                          .collectList()
                                          .map(
                                              filteredConfigs ->
                                                  ResponseEntity.ok(
                                                      new Api<List<PortalConfiguration>>(
                                                          PortalConfigConstants.SUCCESSFUL,
                                                          PortalConfigConstants.CONFIGS_RETRIEVED,
                                                          Optional.of(filteredConfigs),
                                                          Optional.empty())))));
            })
        .onErrorResume(
            e -> {
              if (e instanceof AccessDeniedException) {
                return Mono.just(
                    ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(
                            new Api<List<PortalConfiguration>>(
                                PortalConfigConstants.ERROR,
                                e.getMessage(),
                                Optional.empty(),
                                Optional.of(Map.of(ErrorValidationMessage.ERROR_KEY, e.getMessage())))));
              }
              return Mono.error(e);
            });
  }

  @Operation(
      summary = "Get configuration by ID and type",
      description = "Retrieves a portal configuration by its ID and portal type")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Configuration retrieved successfully",
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
                                    "status": "SUCCESSFUL",
                                    "message": "Portal configuration retrieved successfully",
                                    "data": {
                                        "id": "65f2a1b3c4d5e6f7g8h9i0j1",
                                        "brdId": "BRD001",
                                        "url": "https://agent-portal.example.com",
                                        "type": "AGENT",
                                        "username": "agent_user",
                                        "password": "********"
                                    },
                                    "errors": null
                                }""")
                    })),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Creator PM role required",
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
                                    "status": "ERROR",
                                    "message": "Access denied: Only the creator PM can access this endpoint.",
                                    "data": null,
                                    "errors": {
                                        "error": "Access denied: Only the creator PM can access this endpoint."
                                    }
                                }""")
                    })),
        @ApiResponse(
            responseCode = "404",
            description = "Configuration not found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @ExampleObject(
                          name = "Not Found Response",
                          value =
                              """
                                {
                                    "status": "ERROR",
                                    "message": "Portal configuration not found",
                                    "data": null,
                                    "errors": ["No configuration found with ID: 65f2a1b3c4d5e6f7g8h9i0j1 and type: AGENT"]
                                }""")
                    }))
      })
  @GetMapping("/{id}/type/{type}")
  public Mono<ResponseEntity<Api<PortalConfiguration>>> getPortalConfigurationByIdAndType(
      @Parameter(description = "Configuration ID") @PathVariable String id,
      @Parameter(description = "Portal type (AGENT/CONSUMER)") @PathVariable PortalTypes type) {
    return brdSecurityService
        .getCurrentUserRole()
        .flatMap(
            role -> {
              if (!SecurityConstants.PM_ROLE.equals(role)) {
                return Mono.error(
                    new AccessDeniedException(ACCESS_DENIED_CREATOR_PM_MESSAGE));
              }

              return portalConfigurationService
                  .getPortalConfigurationByIdAndType(id, type)
                  .flatMap(
                      config ->
                          brdRepository
                              .findByBrdId(config.getBrdId())
                              .flatMap(
                                  brd ->
                                      brdSecurityService
                                          .canModifyBrd(brd.getCreator())
                                          .flatMap(
                                              canModify -> {
                                                if (Boolean.FALSE.equals(canModify)) {
                                                  return Mono.error(
                                                      new AccessDeniedException(ACCESS_DENIED_CREATOR_PM_MESSAGE));
                                                }
                                                return Mono.just(
                                                    ResponseEntity.ok(
                                                        new Api<PortalConfiguration>(
                                                            PortalConfigConstants.SUCCESSFUL,
                                                            PortalConfigConstants.CONFIGS_RETRIEVED,
                                                            Optional.of(config),
                                                            Optional.empty())));
                                              }))
                              .switchIfEmpty(
                                  Mono.just(
                                      ResponseEntity.status(HttpStatus.NOT_FOUND)
                                          .body(
                                              new Api<PortalConfiguration>(
                                                  PortalConfigConstants.ERROR,
                                                  "Portal configuration not found",
                                                  Optional.empty(),
                                                  Optional.of(
                                                      Map.of(
                                                          ErrorValidationMessage.ERROR_KEY,
                                                          "No configuration found for BRD ID: "
                                                              + id
                                                              + " with type: "
                                                              + type
                                                              + ". Please verify the BRD ID exists and has a configuration of the specified type.")))))))
                  .switchIfEmpty(
                      Mono.just(
                          ResponseEntity.status(HttpStatus.NOT_FOUND)
                              .body(
                                  new Api<PortalConfiguration>(
                                      PortalConfigConstants.ERROR,
                                      "Portal configuration not found",
                                      Optional.empty(),
                                      Optional.of(
                                          Map.of(
                                              ErrorValidationMessage.ERROR_KEY,
                                              "No configuration found for BRD ID: "
                                                  + id
                                                  + " with type: "
                                                  + type
                                                  + ". Please verify the BRD ID exists and has a configuration of the specified type."))))));
            })
        .onErrorResume(
            e -> {
              if (e instanceof AccessDeniedException) {
                return Mono.just(
                    ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(
                            new Api<PortalConfiguration>(
                                PortalConfigConstants.ERROR,
                                e.getMessage(),
                                Optional.empty(),
                                Optional.of(Map.of(ErrorValidationMessage.ERROR_KEY, e.getMessage())))));
              }
              return Mono.error(e);
            });
  }

  @Operation(
      summary = "Update portal configurations",
      description =
          "Updates portal configurations for a BRD. Only accessible by the creator PM.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Configurations updated successfully",
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
                                    "status": "SUCCESSFUL",
                                    "message": "Portal configurations updated successfully",
                                    "data": null,
                                    "errors": null
                                }""")
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
                          value =
                              """
                                {
                                    "status": "ERROR",
                                    "message": "Invalid input data",
                                    "data": null,
                                    "errors": {
                                        "type": "Portal type must be either AGENT or CONSUMER"
                                    }
                                }""")
                    })),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Creator PM access required",
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
                                    "status": "ERROR",
                                    "message": "Access denied: Only the creator PM can access this endpoint.",
                                    "data": null,
                                    "errors": {
                                        "error": "Access denied: Only the creator PM can access this endpoint."
                                    }
                                }""")
                    }))
      })
  @PutMapping("/edit/{brdId}")
  public Mono<ResponseEntity<Api<List<PortalConfiguration>>>> updatePortalConfigurations(
      @Parameter(description = "BRD ID") @PathVariable String brdId,
      @Parameter(description = "Portal configuration details") @Valid @RequestBody
          List<PortalConfigurationRequest> requests) {

    return brdSecurityService
        .getCurrentUserRole()
        .flatMap(
            role -> {
              if (!SecurityConstants.PM_ROLE.equals(role)) {
                return Mono.error(
                    new AccessDeniedException(ACCESS_DENIED_CREATOR_PM_MESSAGE));
              }

              return brdRepository
                  .findByBrdId(brdId)
                  .switchIfEmpty(Mono.error(new ResourceNotFoundException(BRD_NOT_FOUND_MESSAGE + brdId)))
                  .flatMap(
                      brd ->
                          brdSecurityService
                              .canModifyBrd(brd.getCreator())
                              .flatMap(
                                  canModify -> {
                                    if (Boolean.FALSE.equals(canModify)) {
                                      return Mono.just(
                                          ResponseEntity.status(HttpStatus.FORBIDDEN)
                                              .body(
                                                  new Api<List<PortalConfiguration>>(
                                                      PortalConfigConstants.ERROR,
                                                      ACCESS_DENIED_CREATOR_PM_MESSAGE,
                                                      Optional.empty(),
                                                      Optional.of(
                                                          Map.of(
                                                              ErrorValidationMessage.ERROR_KEY,
                                                              ACCESS_DENIED_CREATOR_PM_MESSAGE)))));
                                    }

                                    List<PortalConfiguration> configurations =
                                        requests.stream()
                                            .map(
                                                request -> {
                                                  PortalConfiguration configuration =
                                                      new PortalConfiguration();
                                                  configuration.setUrl(request.getUrl());
                                                  configuration.setType(request.getType());
                                                  configuration.setUsername(request.getUsername());
                                                  configuration.setPassword(request.getPassword());
                                                  return configuration;
                                                })
                                            .toList();

                                    return portalConfigurationService
                                        .updatePortalConfigurations(brdId, configurations)
                                        .map(
                                            configs ->
                                                ResponseEntity.ok(
                                                    new Api<List<PortalConfiguration>>(
                                                        PortalConfigConstants.SUCCESSFUL,
                                                        "Portal configurations updated successfully",
                                                        Optional.of(configs),
                                                        Optional.empty())));
                                  }));
            })
        .onErrorResume(
            e -> {
              if (e instanceof AccessDeniedException) {
                return Mono.just(
                    ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(
                            new Api<List<PortalConfiguration>>(
                                PortalConfigConstants.ERROR,
                                e.getMessage(),
                                Optional.empty(),
                                Optional.of(Map.of(ErrorValidationMessage.ERROR_KEY, e.getMessage())))));
              }
              return Mono.error(e);
            });
  }
}

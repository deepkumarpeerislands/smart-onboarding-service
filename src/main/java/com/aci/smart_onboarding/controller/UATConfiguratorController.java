package com.aci.smart_onboarding.controller;

import com.aci.smart_onboarding.constants.BrdConstants;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.UATConfiguratorRequestDTO;
import com.aci.smart_onboarding.dto.UATConfiguratorResponseDTO;
import com.aci.smart_onboarding.enums.PortalTypes;
import com.aci.smart_onboarding.service.IUATConfiguratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/** Controller for UAT Configurator operations. */
@RestController
@RequestMapping("/api/v1/uat-configurator")
@RequiredArgsConstructor
@Tag(name = "UAT Configurator", description = "APIs for managing UAT configurations")
public class UATConfiguratorController {

  private final IUATConfiguratorService service;

  @Operation(
      summary = "Create a new UAT configuration",
      responses = {
        @ApiResponse(
            responseCode = "201",
            description = "Configuration created successfully",
            content =
                @Content(schema = @Schema(implementation = UATConfiguratorResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input")
      })
  @PostMapping
  public Mono<ResponseEntity<Api<UATConfiguratorResponseDTO>>> createConfiguration(
      @Valid @RequestBody UATConfiguratorRequestDTO requestDTO) {
    return service
        .createConfiguration(requestDTO)
        .map(
            created ->
                ResponseEntity.status(HttpStatus.CREATED)
                    .body(
                        new Api<>(
                            BrdConstants.SUCCESSFUL,
                            "Configuration created successfully",
                            Optional.of(created),
                            Optional.empty())));
  }

  @Operation(
      summary = "Update an existing UAT configuration",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Configuration updated successfully",
            content =
                @Content(schema = @Schema(implementation = UATConfiguratorResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Configuration not found")
      })
  @PutMapping("/{uatId}")
  public Mono<ResponseEntity<Api<UATConfiguratorResponseDTO>>> updateConfiguration(
      @Parameter(description = "UAT Configuration ID") @PathVariable String uatId,
      @Valid @RequestBody UATConfiguratorRequestDTO requestDTO) {
    return service
        .updateConfiguration(uatId, requestDTO)
        .map(
            updated ->
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "Configuration updated successfully",
                        Optional.of(updated),
                        Optional.empty())));
  }

  @Operation(
      summary = "Delete a UAT configuration",
      responses = {
        @ApiResponse(responseCode = "200", description = "Configuration deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Configuration not found")
      })
  @DeleteMapping("/{uatId}")
  public Mono<ResponseEntity<Api<Void>>> deleteConfiguration(
      @Parameter(description = "UAT Configuration ID") @PathVariable String uatId) {
    return service
        .deleteConfiguration(uatId)
        .then(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "Configuration deleted successfully",
                        Optional.empty(),
                        Optional.empty()))));
  }

  @Operation(
      summary = "Get a UAT configuration by ID",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Configuration retrieved successfully",
            content =
                @Content(schema = @Schema(implementation = UATConfiguratorResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Configuration not found")
      })
  @GetMapping("/{uatId}")
  public Mono<ResponseEntity<Api<UATConfiguratorResponseDTO>>> getConfiguration(
      @Parameter(description = "UAT Configuration ID") @PathVariable String uatId) {
    return service
        .getConfiguration(uatId)
        .map(
            config ->
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "Configuration retrieved successfully",
                        Optional.of(config),
                        Optional.empty())));
  }

  @Operation(
      summary = "Get all UAT configurations",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Configurations retrieved successfully",
            content = @Content(schema = @Schema(implementation = UATConfiguratorResponseDTO.class)))
      })
  @GetMapping
  public Mono<ResponseEntity<Api<List<UATConfiguratorResponseDTO>>>> getAllConfigurations() {
    return service
        .getAllConfigurations()
        .collectList()
        .map(
            configs ->
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "Configurations retrieved successfully",
                        Optional.of(configs),
                        Optional.empty())));
  }

  @Operation(
      summary = "Get UAT configurations by type",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Configurations retrieved successfully",
            content = @Content(schema = @Schema(implementation = UATConfiguratorResponseDTO.class)))
      })
  @GetMapping("/type/{type}")
  public Mono<ResponseEntity<Api<List<UATConfiguratorResponseDTO>>>> getConfigurationsByType(
      @Parameter(description = "Portal type (AGENT/CONSUMER)") @PathVariable PortalTypes type) {
    return service
        .getConfigurationsByType(type)
        .collectList()
        .map(
            configs ->
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "Configurations retrieved successfully",
                        Optional.of(configs),
                        Optional.empty())));
  }
}

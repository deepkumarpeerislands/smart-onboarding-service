package com.aci.smart_onboarding.security.controller;

import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.BAEmailsResponse;
import com.aci.smart_onboarding.dto.BillerEmailsResponse;
import com.aci.smart_onboarding.security.service.AzureADService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Tag(name = "Azure AD", description = "Azure Active Directory operations")
@RestController
@RequestMapping("/api/v1/azure")
@RequiredArgsConstructor
public class AzureADController {

  private final AzureADService azureADService;

  @Operation(
      summary = "Get biller emails",
      description =
          "Retrieves email addresses of all billers from the users collection where roles contains 'BILLER'")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved biller emails",
            content = @Content(schema = @Schema(implementation = BillerEmailsResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing authentication"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @GetMapping("/billers")
  public Mono<ResponseEntity<Api<BillerEmailsResponse>>> getBillerEmails() {
    return azureADService
        .getBillerEmailsByBrdName()
        .map(
            response ->
                ResponseEntity.ok(
                    new Api<>(
                        "success",
                        "Successfully retrieved biller emails",
                        Optional.of(response),
                        Optional.empty())))
        .onErrorResume(
            Exception.class,
            e ->
                Mono.just(
                    ResponseEntity.internalServerError()
                        .body(
                            new Api<>(
                                SecurityConstants.ERROR,
                                e.getMessage(),
                                Optional.empty(),
                                Optional.empty()))));
  }

  @Operation(
      summary = "Get BA emails",
      description =
          "Retrieves email addresses of all Business Analysts from the users collection where roles contains 'BA'")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved BA emails",
            content = @Content(schema = @Schema(implementation = BAEmailsResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing authentication"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @GetMapping("/ba")
  public Mono<ResponseEntity<Api<BAEmailsResponse>>> getBAEmails() {
    return azureADService
        .getBAEmailsByBrdName()
        .map(
            response ->
                ResponseEntity.ok(
                    new Api<>(
                        "success",
                        "Successfully retrieved BA emails",
                        Optional.of(response),
                        Optional.empty())))
        .onErrorResume(
            Exception.class,
            e ->
                Mono.just(
                    ResponseEntity.internalServerError()
                        .body(
                            new Api<>(
                                SecurityConstants.ERROR,
                                e.getMessage(),
                                Optional.empty(),
                                Optional.empty()))));
  }
}

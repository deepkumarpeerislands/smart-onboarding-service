package com.aci.smart_onboarding.controller;

import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.UATAIRequestDTO;
import com.aci.smart_onboarding.dto.UATTestCaseRequestResponseDTO;
import com.aci.smart_onboarding.exception.ResourceNotFoundException;
import com.aci.smart_onboarding.model.BRD;
import com.aci.smart_onboarding.repository.BRDRepository;
import com.aci.smart_onboarding.security.service.BRDSecurityService;
import com.aci.smart_onboarding.service.IUATAIService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/uat")
@RequiredArgsConstructor
@Slf4j
public class UATAIController {

  private final IUATAIService uatAIService;
  private final ObjectMapper objectMapper;
  private final BRDSecurityService securityService;
  private final BRDRepository brdRepository;
  private static final String ACCESS_DENIED_CREATOR_PM_MESSAGE = "Access denied: Only the creator PM can access this endpoint.";
  private static final String BRD_NOT_FOUND_MESSAGE = "BRD not found with ID: ";

  @PostMapping("/generate-test-cases")
  public Mono<ResponseEntity<Api<Flux<UATTestCaseRequestResponseDTO>>>> generateUATTestCases(
      @Valid @RequestBody UATAIRequestDTO request) {
    log.info("Generating UAT test cases for request: {}", request);

    return securityService
        .getCurrentUserRole()
        .flatMap(
            role -> {
              if (!SecurityConstants.PM_ROLE.equals(role)) {
                return Mono.error(
                    new AccessDeniedException(ACCESS_DENIED_CREATOR_PM_MESSAGE));
              }

              return brdRepository
                  .findByBrdId(request.getBrdId())
                  .switchIfEmpty(Mono.error(new ResourceNotFoundException(BRD_NOT_FOUND_MESSAGE + request.getBrdId())))
                  .flatMap(
                      brd ->
                          securityService
                              .canModifyBrd(brd.getCreator())
                              .flatMap(
                                  canModify -> {
                                    if (Boolean.FALSE.equals(canModify)) {
                                      return Mono.error(
                                          new AccessDeniedException(ACCESS_DENIED_CREATOR_PM_MESSAGE));
                                    }

                                    return Mono.just(
                                        ResponseEntity.ok(
                                            new Api<>(
                                                "SUCCESSFUL",
                                                "UAT test cases generation initiated",
                                                java.util.Optional.of(
                                                    uatAIService.generateUATTestCases(
                                                        request.getBrdId(), request.getConfigurationNames(), request.getUatType())),
                                                java.util.Optional.empty())));
                                  }));
            })
        .onErrorResume(
            e -> {
              if (e instanceof AccessDeniedException) {
                return Mono.just(
                    ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(
                            new Api<>(
                                "ERROR",
                                e.getMessage(),
                                java.util.Optional.empty(),
                                java.util.Optional.of(java.util.Map.of("error", e.getMessage())))));
              }
              return Mono.error(e);
            });
  }

  @PostMapping("/retest")
  public Mono<ResponseEntity<Api<Flux<UATTestCaseRequestResponseDTO>>>> retestFeatures(
      @Valid @RequestBody UATAIRequestDTO request) {
    log.info("Retesting features for request: {}", request);

    return securityService
        .getCurrentUserRole()
        .flatMap(
            role -> {
              if (!SecurityConstants.PM_ROLE.equals(role)) {
                return Mono.error(
                    new AccessDeniedException(ACCESS_DENIED_CREATOR_PM_MESSAGE));
              }

              return brdRepository
                  .findByBrdId(request.getBrdId())
                  .switchIfEmpty(Mono.error(new ResourceNotFoundException(BRD_NOT_FOUND_MESSAGE + request.getBrdId())))
                  .flatMap(
                      brd ->
                          securityService
                              .canModifyBrd(brd.getCreator())
                              .flatMap(
                                  canModify -> {
                                    if (Boolean.FALSE.equals(canModify)) {
                                      return Mono.error(
                                          new AccessDeniedException(ACCESS_DENIED_CREATOR_PM_MESSAGE));
                                    }

                                    return Mono.just(
                                        ResponseEntity.ok(
                                            new Api<>(
                                                "SUCCESSFUL",
                                                "UAT retest initiated",
                                                java.util.Optional.of(
                                                    uatAIService
                                                        .retestFeatures(
                                                            request.getBrdId(), request.getConfigurationNames(), request.getUatType())
                                                        .onErrorResume(
                                                            e -> {
                                                              log.error("Error in retestFeatures: {}", e.getMessage(), e);
                                                              return Flux.empty();
                                                            })),
                                                java.util.Optional.empty())));
                                  }));
            })
        .onErrorResume(
            e -> {
              if (e instanceof AccessDeniedException) {
                return Mono.just(
                    ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(
                            new Api<>(
                                "ERROR",
                                e.getMessage(),
                                java.util.Optional.empty(),
                                java.util.Optional.of(java.util.Map.of("error", e.getMessage())))));
              }
              return Mono.error(e);
            });
  }
}

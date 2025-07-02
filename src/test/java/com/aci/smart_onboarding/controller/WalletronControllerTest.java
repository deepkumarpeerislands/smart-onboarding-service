package com.aci.smart_onboarding.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.WalletronRequest;
import com.aci.smart_onboarding.dto.WalletronResponse;
import com.aci.smart_onboarding.security.service.BRDSecurityService;
import com.aci.smart_onboarding.service.IWalletronService;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class WalletronControllerTest {

  @Mock private IWalletronService walletronService;

  @Mock private BRDSecurityService securityService;

  @InjectMocks private WalletronController walletronController;

  private WalletronRequest walletronRequest;
  private WalletronResponse walletronResponse;
  private Api<WalletronResponse> successApi;
  private static final String WALLETRON_ID = "WAL-123";

  @BeforeEach
  void setUp() {
    walletronRequest = new WalletronRequest();
    walletronResponse = new WalletronResponse();
    successApi =
        new Api<>(
            "SUCCESS",
            "Walletron data saved successfully",
            Optional.of(walletronResponse),
            Optional.empty());
  }

  @Test
  void createWalletron_Success() {
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(walletronService.createWalletron(any()))
        .thenReturn(Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(successApi)));

    StepVerifier.create(walletronController.createWalletron(walletronRequest))
        .expectNext(ResponseEntity.status(HttpStatus.CREATED).body(successApi))
        .verifyComplete();

    verify(securityService).getCurrentUserRole();
    verify(walletronService).createWalletron(walletronRequest);
  }

  @Test
  void createWalletron_AccessDenied() {
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("USER"));

    StepVerifier.create(walletronController.createWalletron(walletronRequest))
        .expectError(AccessDeniedException.class)
        .verify();

    verify(securityService).getCurrentUserRole();
    verify(walletronService, never()).createWalletron(any());
  }

  @Test
  void createWalletron_NullRequest() {
    StepVerifier.create(walletronController.createWalletron(null)).expectComplete().verify();

    verify(securityService, never()).getCurrentUserRole();
    verify(walletronService, never()).createWalletron(any());
  }

  @Test
  void updateWalletronPartially_Success() {
    Map<String, Object> patchFields = new HashMap<>();
    patchFields.put("field", "value");
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(walletronService.getWalletronById(anyString()))
        .thenReturn(Mono.just(ResponseEntity.ok(successApi)));
    when(walletronService.updateWalletronPartiallyWithOrderedOperations(anyString(), any()))
        .thenReturn(Mono.just(ResponseEntity.ok(successApi)));

    StepVerifier.create(
            walletronController.updateWalletronPartially(WALLETRON_ID, Mono.just(patchFields)))
        .expectNext(ResponseEntity.ok(successApi))
        .verifyComplete();

    verify(securityService).getCurrentUserRole();
    verify(walletronService).getWalletronById(WALLETRON_ID);
    verify(walletronService)
        .updateWalletronPartiallyWithOrderedOperations(WALLETRON_ID, patchFields);
  }

  @Test
  void updateWalletronPartially_AccessDenied() {
    Map<String, Object> patchFields = new HashMap<>();
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("USER"));
    when(walletronService.getWalletronById(anyString()))
        .thenReturn(Mono.just(ResponseEntity.ok(successApi)));

    StepVerifier.create(
            walletronController.updateWalletronPartially(WALLETRON_ID, Mono.just(patchFields)))
        .expectNext(
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(
                    new Api<>(
                        "ERROR",
                        "Only Project Managers (PM) can update Walletron forms",
                        Optional.empty(),
                        Optional.empty())))
        .verifyComplete();

    verify(securityService).getCurrentUserRole();
    verify(walletronService).getWalletronById(WALLETRON_ID);
    verify(walletronService, never())
        .updateWalletronPartiallyWithOrderedOperations(anyString(), any());
  }

  @Test
  void updateWalletronPartially_WalletronNotFound() {
    Map<String, Object> patchFields = new HashMap<>();
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(walletronService.getWalletronById(anyString()))
        .thenReturn(Mono.just(ResponseEntity.notFound().build()));

    StepVerifier.create(
            walletronController.updateWalletronPartially(WALLETRON_ID, Mono.just(patchFields)))
        .expectError(NullPointerException.class)
        .verify();

    verify(securityService).getCurrentUserRole();
    verify(walletronService).getWalletronById(WALLETRON_ID);
  }

  @Test
  void updateWalletronPartially_EmptyPatchFields() {
    Map<String, Object> emptyPatchFields = new HashMap<>();
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(walletronService.getWalletronById(anyString()))
        .thenReturn(Mono.just(ResponseEntity.ok(successApi)));
    when(walletronService.updateWalletronPartiallyWithOrderedOperations(anyString(), any()))
        .thenReturn(Mono.just(ResponseEntity.ok(successApi)));

    StepVerifier.create(
            walletronController.updateWalletronPartially(WALLETRON_ID, Mono.just(emptyPatchFields)))
        .expectNext(ResponseEntity.ok(successApi))
        .verifyComplete();

    verify(securityService).getCurrentUserRole();
    verify(walletronService).getWalletronById(WALLETRON_ID);
    verify(walletronService)
        .updateWalletronPartiallyWithOrderedOperations(WALLETRON_ID, emptyPatchFields);
  }

  @Test
  void updateWalletronPartially_NullPatchFields() {
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(walletronService.getWalletronById(anyString()))
        .thenReturn(Mono.just(ResponseEntity.ok(successApi)));

    StepVerifier.create(walletronController.updateWalletronPartially(WALLETRON_ID, Mono.empty()))
        .expectComplete()
        .verify();

    verify(securityService).getCurrentUserRole();
    verify(walletronService).getWalletronById(WALLETRON_ID);
    verify(walletronService, never())
        .updateWalletronPartiallyWithOrderedOperations(anyString(), any());
  }

  @Test
  void getWalletronById_Success() {
    when(walletronService.getWalletronById(anyString()))
        .thenReturn(Mono.just(ResponseEntity.ok(successApi)));

    StepVerifier.create(walletronController.getWalletronById(WALLETRON_ID))
        .expectNext(ResponseEntity.ok(successApi))
        .verifyComplete();

    verify(walletronService).getWalletronById(WALLETRON_ID);
  }

  @Test
  void getWalletronById_NotFound() {
    when(walletronService.getWalletronById(anyString()))
        .thenReturn(Mono.just(ResponseEntity.notFound().build()));

    StepVerifier.create(walletronController.getWalletronById(WALLETRON_ID))
        .expectNext(ResponseEntity.notFound().build())
        .verifyComplete();

    verify(walletronService).getWalletronById(WALLETRON_ID);
  }

  @Test
  void getWalletronSection_Success() {
    Map<String, Object> sectionData = new HashMap<>();
    sectionData.put("key", "value");
    when(walletronService.getWalletronSectionById(anyString(), anyString()))
        .thenReturn(Mono.just(ResponseEntity.ok(sectionData)));

    StepVerifier.create(walletronController.getWalletronSection(WALLETRON_ID, "siteConfiguration"))
        .expectNext(ResponseEntity.ok(sectionData))
        .verifyComplete();

    verify(walletronService).getWalletronSectionById(WALLETRON_ID, "siteConfiguration");
  }

  @Test
  void getWalletronSection_NotFound() {
    when(walletronService.getWalletronSectionById(anyString(), anyString()))
        .thenReturn(Mono.just(ResponseEntity.notFound().build()));

    StepVerifier.create(walletronController.getWalletronSection(WALLETRON_ID, "invalidSection"))
        .expectNext(ResponseEntity.notFound().build())
        .verifyComplete();

    verify(walletronService).getWalletronSectionById(WALLETRON_ID, "invalidSection");
  }

  @Test
  void getWalletronSection_EmptySection() {
    Map<String, Object> emptySection = new HashMap<>();
    when(walletronService.getWalletronSectionById(anyString(), anyString()))
        .thenReturn(Mono.just(ResponseEntity.ok(emptySection)));

    StepVerifier.create(walletronController.getWalletronSection(WALLETRON_ID, "emptySection"))
        .expectNext(ResponseEntity.ok(emptySection))
        .verifyComplete();

    verify(walletronService).getWalletronSectionById(WALLETRON_ID, "emptySection");
  }

  @Test
  void createWalletronUsers_Success() {
    com.aci.smart_onboarding.dto.WalletronUsersRequest request = createValidWalletronUsersRequest();
    com.aci.smart_onboarding.dto.WalletronUsersResponse response =
        createMockWalletronUsersResponse();
    com.aci.smart_onboarding.dto.Api<com.aci.smart_onboarding.dto.WalletronUsersResponse>
        usersSuccessApi =
            new com.aci.smart_onboarding.dto.Api<>(
                "SUCCESS", "Users created successfully", Optional.of(response), Optional.empty());

    when(securityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(walletronService.createWalletronUsers(any()))
        .thenReturn(Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(usersSuccessApi)));

    StepVerifier.create(walletronController.createWalletronUsers(request))
        .expectNext(ResponseEntity.status(HttpStatus.CREATED).body(usersSuccessApi))
        .verifyComplete();

    verify(securityService).getCurrentUserRole();
    verify(walletronService).createWalletronUsers(request);
  }

  @Test
  void createWalletronUsers_AccessDenied() {
    com.aci.smart_onboarding.dto.WalletronUsersRequest request = createValidWalletronUsersRequest();

    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("USER"));

    StepVerifier.create(walletronController.createWalletronUsers(request))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
              assertEquals("ERROR", response.getBody().getStatus());
              assertEquals(
                  "Only Project Managers (PM) can create Walletron users",
                  response.getBody().getMessage());
            })
        .verifyComplete();

    verify(securityService).getCurrentUserRole();
    verify(walletronService, never()).createWalletronUsers(any());
  }

  @Test
  void createWalletronUsers_ServiceError() {
    com.aci.smart_onboarding.dto.WalletronUsersRequest request = createValidWalletronUsersRequest();

    when(securityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(walletronService.createWalletronUsers(any()))
        .thenReturn(Mono.error(new RuntimeException("Service error")));

    StepVerifier.create(walletronController.createWalletronUsers(request))
        .expectError(RuntimeException.class)
        .verify();

    verify(securityService).getCurrentUserRole();
    verify(walletronService).createWalletronUsers(request);
  }

  @Test
  void createWalletronUsers_EmptyUsersList() {
    com.aci.smart_onboarding.dto.WalletronUsersRequest request =
        new com.aci.smart_onboarding.dto.WalletronUsersRequest();
    request.setWalletronId("WAL-123");
    request.setBrdId("BRD-123");
    request.setUsers(Collections.emptyList());

    com.aci.smart_onboarding.dto.WalletronUsersResponse response =
        new com.aci.smart_onboarding.dto.WalletronUsersResponse();
    response.setSavedUsers(Collections.emptyList());
    response.setDuplicateEmails(Collections.emptyList());
    response.setTotalProcessed(0);
    response.setTotalDuplicates(0);

    com.aci.smart_onboarding.dto.Api<com.aci.smart_onboarding.dto.WalletronUsersResponse>
        usersSuccessApi =
            new com.aci.smart_onboarding.dto.Api<>(
                "SUCCESS", "Users processed successfully", Optional.of(response), Optional.empty());

    when(securityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(walletronService.createWalletronUsers(any()))
        .thenReturn(Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(usersSuccessApi)));

    StepVerifier.create(walletronController.createWalletronUsers(request))
        .expectNext(ResponseEntity.status(HttpStatus.CREATED).body(usersSuccessApi))
        .verifyComplete();

    verify(securityService).getCurrentUserRole();
    verify(walletronService).createWalletronUsers(request);
  }

  @Test
  void uploadWalletronUsers_CsvFile_Success() {
    org.springframework.http.codec.multipart.FilePart filePart =
        mock(org.springframework.http.codec.multipart.FilePart.class);
    lenient().when(filePart.filename()).thenReturn("users.csv");

    com.aci.smart_onboarding.dto.WalletronUsersResponse response =
        createMockWalletronUsersResponse();
    com.aci.smart_onboarding.dto.Api<com.aci.smart_onboarding.dto.WalletronUsersResponse>
        usersSuccessApi =
            new com.aci.smart_onboarding.dto.Api<>(
                "SUCCESS", "File processed successfully", Optional.of(response), Optional.empty());

    when(securityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(walletronService.validateWalletronExists(anyString(), anyString()))
        .thenReturn(Mono.just(true));
    when(walletronService.createWalletronUsersFromFile(any(), anyString(), anyString()))
        .thenReturn(Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(usersSuccessApi)));

    StepVerifier.create(walletronController.uploadWalletronUsers(filePart, "WAL-123", "BRD-123"))
        .expectNext(ResponseEntity.status(HttpStatus.CREATED).body(usersSuccessApi))
        .verifyComplete();

    verify(securityService).getCurrentUserRole();
    verify(walletronService).validateWalletronExists("WAL-123", "BRD-123");
    verify(walletronService).createWalletronUsersFromFile(filePart, "WAL-123", "BRD-123");
  }

  @Test
  void uploadWalletronUsers_ExcelFile_Success() {
    org.springframework.http.codec.multipart.FilePart filePart =
        mock(org.springframework.http.codec.multipart.FilePart.class);
    lenient().when(filePart.filename()).thenReturn("users.xlsx");

    com.aci.smart_onboarding.dto.WalletronUsersResponse response =
        createMockWalletronUsersResponse();
    com.aci.smart_onboarding.dto.Api<com.aci.smart_onboarding.dto.WalletronUsersResponse>
        usersSuccessApi =
            new com.aci.smart_onboarding.dto.Api<>(
                "SUCCESS", "File processed successfully", Optional.of(response), Optional.empty());

    when(securityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(walletronService.validateWalletronExists(anyString(), anyString()))
        .thenReturn(Mono.just(true));
    when(walletronService.createWalletronUsersFromFile(any(), anyString(), anyString()))
        .thenReturn(Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(usersSuccessApi)));

    StepVerifier.create(walletronController.uploadWalletronUsers(filePart, "WAL-123", "BRD-123"))
        .expectNext(ResponseEntity.status(HttpStatus.CREATED).body(usersSuccessApi))
        .verifyComplete();

    verify(securityService).getCurrentUserRole();
    verify(walletronService).validateWalletronExists("WAL-123", "BRD-123");
    verify(walletronService).createWalletronUsersFromFile(filePart, "WAL-123", "BRD-123");
  }

  @Test
  void uploadWalletronUsers_AccessDenied() {
    org.springframework.http.codec.multipart.FilePart filePart =
        mock(org.springframework.http.codec.multipart.FilePart.class);
    lenient().when(filePart.filename()).thenReturn("users.csv");

    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("USER"));

    StepVerifier.create(walletronController.uploadWalletronUsers(filePart, "WAL-123", "BRD-123"))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
              assertEquals("ERROR", response.getBody().getStatus());
              assertEquals(
                  "Only Project Managers (PM) can create Walletron users",
                  response.getBody().getMessage());
            })
        .verifyComplete();

    verify(securityService).getCurrentUserRole();
    verify(walletronService, never()).validateWalletronExists(anyString(), anyString());
    verify(walletronService, never()).createWalletronUsersFromFile(any(), anyString(), anyString());
  }

  @Test
  void uploadWalletronUsers_ServiceError() {
    org.springframework.http.codec.multipart.FilePart filePart =
        mock(org.springframework.http.codec.multipart.FilePart.class);
    lenient().when(filePart.filename()).thenReturn("users.csv");

    when(securityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(walletronService.validateWalletronExists(anyString(), anyString()))
        .thenReturn(Mono.just(true));
    when(walletronService.createWalletronUsersFromFile(any(), anyString(), anyString()))
        .thenReturn(Mono.error(new RuntimeException("Service error")));

    StepVerifier.create(walletronController.uploadWalletronUsers(filePart, "WAL-123", "BRD-123"))
        .expectError(RuntimeException.class)
        .verify();

    verify(securityService).getCurrentUserRole();
    verify(walletronService).validateWalletronExists("WAL-123", "BRD-123");
    verify(walletronService).createWalletronUsersFromFile(filePart, "WAL-123", "BRD-123");
  }

  @Test
  void uploadWalletronUsers_WithWhitespaceInIds() {
    org.springframework.http.codec.multipart.FilePart filePart =
        mock(org.springframework.http.codec.multipart.FilePart.class);
    lenient().when(filePart.filename()).thenReturn("users.csv");

    com.aci.smart_onboarding.dto.WalletronUsersResponse response =
        createMockWalletronUsersResponse();
    com.aci.smart_onboarding.dto.Api<com.aci.smart_onboarding.dto.WalletronUsersResponse>
        usersSuccessApi =
            new com.aci.smart_onboarding.dto.Api<>(
                "SUCCESS", "File processed successfully", Optional.of(response), Optional.empty());

    when(securityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(walletronService.validateWalletronExists("  WAL-123  ", "  BRD-123  "))
        .thenReturn(Mono.just(true));
    when(walletronService.createWalletronUsersFromFile(any(), anyString(), anyString()))
        .thenReturn(Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(usersSuccessApi)));

    StepVerifier.create(
            walletronController.uploadWalletronUsers(filePart, "  WAL-123  ", "  BRD-123  "))
        .expectNext(ResponseEntity.status(HttpStatus.CREATED).body(usersSuccessApi))
        .verifyComplete();

    verify(securityService).getCurrentUserRole();
    verify(walletronService).validateWalletronExists("  WAL-123  ", "  BRD-123  ");
    verify(walletronService).createWalletronUsersFromFile(filePart, "  WAL-123  ", "  BRD-123  ");
  }

  @ParameterizedTest
  @CsvSource({
    "users.txt, Unsupported file format. Only CSV and XLSX files are supported.",
    "large_file.csv, File size exceeds maximum allowed size",
    "invalid_headers.csv, Missing required headers",
    "empty.csv, File content is empty"
  })
  void uploadWalletronUsers_BadRequestScenarios(String filename, String expectedErrorMessage) {
    org.springframework.http.codec.multipart.FilePart filePart =
        mock(org.springframework.http.codec.multipart.FilePart.class);
    lenient().when(filePart.filename()).thenReturn(filename);

    when(securityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(walletronService.validateWalletronExists(anyString(), anyString()))
        .thenReturn(Mono.just(true));
    when(walletronService.createWalletronUsersFromFile(any(), anyString(), anyString()))
        .thenReturn(
            Mono.error(
                new com.aci.smart_onboarding.exception.BadRequestException(expectedErrorMessage)));

    StepVerifier.create(walletronController.uploadWalletronUsers(filePart, "WAL-123", "BRD-123"))
        .expectErrorMatches(
            throwable ->
                throwable instanceof com.aci.smart_onboarding.exception.BadRequestException
                    && throwable.getMessage().equals(expectedErrorMessage))
        .verify();

    verify(securityService).getCurrentUserRole();
    verify(walletronService).validateWalletronExists("WAL-123", "BRD-123");
    verify(walletronService).createWalletronUsersFromFile(filePart, "WAL-123", "BRD-123");
  }

  @Test
  void createWalletronUsers_NullRequest() {
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(walletronService.createWalletronUsers(any()))
        .thenReturn(
            Mono.error(
                new com.aci.smart_onboarding.exception.BadRequestException(
                    "Request cannot be null")));

    StepVerifier.create(walletronController.createWalletronUsers(null))
        .expectError(com.aci.smart_onboarding.exception.BadRequestException.class)
        .verify();
  }

  @ParameterizedTest
  @CsvSource({
    "test.txt, Unsupported file format. Only CSV and XLSX files are supported.",
    "test, Unsupported file format. Only CSV and XLSX files are supported.",
    "'', Unsupported file format. Only CSV and XLSX files are supported.", // Empty string test case
    "users.doc, Unsupported file format. Only CSV and XLSX files are supported.",
    "users.pdf, Unsupported file format. Only CSV and XLSX files are supported."
  })
  void uploadWalletronUsers_InvalidFileTypes(String filename, String expectedErrorMessage) {
    org.springframework.http.codec.multipart.FilePart filePart =
        mock(org.springframework.http.codec.multipart.FilePart.class);

    when(securityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    lenient()
        .when(walletronService.validateWalletronExists(anyString(), anyString()))
        .thenReturn(Mono.just(true));
    lenient()
        .when(walletronService.createWalletronUsersFromFile(any(), anyString(), anyString()))
        .thenReturn(
            Mono.error(
                new com.aci.smart_onboarding.exception.BadRequestException(expectedErrorMessage)));

    StepVerifier.create(walletronController.uploadWalletronUsers(filePart, WALLETRON_ID, "BRD-123"))
        .expectErrorMatches(
            throwable ->
                throwable instanceof com.aci.smart_onboarding.exception.BadRequestException
                    && throwable.getMessage().equals(expectedErrorMessage))
        .verify();

    verify(securityService).getCurrentUserRole();
  }

  @Test
  void createWalletronUsers_ValidationErrors() {
    com.aci.smart_onboarding.dto.WalletronUsersRequest request =
        createInvalidWalletronUsersRequest();

    when(securityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(walletronService.createWalletronUsers(any()))
        .thenReturn(
            Mono.error(
                new com.aci.smart_onboarding.exception.BadRequestException("Validation failed")));

    StepVerifier.create(walletronController.createWalletronUsers(request))
        .expectError(com.aci.smart_onboarding.exception.BadRequestException.class)
        .verify();

    verify(securityService).getCurrentUserRole();
    verify(walletronService).createWalletronUsers(request);
  }

  @Test
  void createWalletronUsers_DuplicateHandling() {
    com.aci.smart_onboarding.dto.WalletronUsersRequest request = createRequestWithDuplicates();
    com.aci.smart_onboarding.dto.WalletronUsersResponse response = createResponseWithDuplicates();
    com.aci.smart_onboarding.dto.Api<com.aci.smart_onboarding.dto.WalletronUsersResponse>
        usersSuccessApi =
            new com.aci.smart_onboarding.dto.Api<>(
                "SUCCESS",
                "Users processed with duplicates filtered",
                Optional.of(response),
                Optional.empty());

    when(securityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(walletronService.createWalletronUsers(any()))
        .thenReturn(Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(usersSuccessApi)));

    StepVerifier.create(walletronController.createWalletronUsers(request))
        .expectNext(ResponseEntity.status(HttpStatus.CREATED).body(usersSuccessApi))
        .verifyComplete();

    verify(securityService).getCurrentUserRole();
    verify(walletronService).createWalletronUsers(request);
  }

  // Helper methods for creating test data

  private com.aci.smart_onboarding.dto.WalletronUsersRequest createValidWalletronUsersRequest() {
    com.aci.smart_onboarding.dto.WalletronUsersRequest request =
        new com.aci.smart_onboarding.dto.WalletronUsersRequest();
    request.setWalletronId("WAL-123");
    request.setBrdId("BRD-123");

    com.aci.smart_onboarding.dto.WalletronUsersRequest.UserData userData =
        new com.aci.smart_onboarding.dto.WalletronUsersRequest.UserData();
    userData.setName("John Doe");
    userData.setEmail("john@example.com");
    userData.setRole("USER");

    request.setUsers(List.of(userData));
    return request;
  }

  private com.aci.smart_onboarding.dto.WalletronUsersRequest createInvalidWalletronUsersRequest() {
    com.aci.smart_onboarding.dto.WalletronUsersRequest request =
        new com.aci.smart_onboarding.dto.WalletronUsersRequest();
    request.setWalletronId("WAL-123");
    request.setBrdId("BRD-123");

    com.aci.smart_onboarding.dto.WalletronUsersRequest.UserData userData =
        new com.aci.smart_onboarding.dto.WalletronUsersRequest.UserData();
    userData.setName(""); // Invalid: empty name
    userData.setEmail("invalid-email"); // Invalid: bad email format
    userData.setRole("INVALID_ROLE"); // Invalid: bad role

    request.setUsers(List.of(userData));
    return request;
  }

  private com.aci.smart_onboarding.dto.WalletronUsersRequest createRequestWithDuplicates() {
    com.aci.smart_onboarding.dto.WalletronUsersRequest request =
        new com.aci.smart_onboarding.dto.WalletronUsersRequest();
    request.setWalletronId("WAL-123");
    request.setBrdId("BRD-123");

    com.aci.smart_onboarding.dto.WalletronUsersRequest.UserData userData1 =
        new com.aci.smart_onboarding.dto.WalletronUsersRequest.UserData();
    userData1.setName("John Doe");
    userData1.setEmail("john@example.com");
    userData1.setRole("USER");

    com.aci.smart_onboarding.dto.WalletronUsersRequest.UserData userData2 =
        new com.aci.smart_onboarding.dto.WalletronUsersRequest.UserData();
    userData2.setName("John Smith");
    userData2.setEmail("john@example.com"); // Duplicate email
    userData2.setRole("ADMIN");

    request.setUsers(List.of(userData1, userData2));
    return request;
  }

  private com.aci.smart_onboarding.dto.WalletronUsersResponse createMockWalletronUsersResponse() {
    com.aci.smart_onboarding.dto.WalletronUsersResponse response =
        new com.aci.smart_onboarding.dto.WalletronUsersResponse();

    com.aci.smart_onboarding.dto.WalletronUsersResponse.SavedUser savedUser =
        new com.aci.smart_onboarding.dto.WalletronUsersResponse.SavedUser();
    savedUser.setId("1");
    savedUser.setName("John Doe");
    savedUser.setEmail("john@example.com");
    savedUser.setRole("USER");
    savedUser.setWalletronId("WAL-123");
    savedUser.setBrdId("BRD-123");
    savedUser.setCreatedAt(java.time.LocalDateTime.now());

    response.setSavedUsers(List.of(savedUser));
    response.setDuplicateEmails(Collections.emptyList());
    response.setTotalProcessed(1);
    response.setTotalDuplicates(0);

    return response;
  }

  private com.aci.smart_onboarding.dto.WalletronUsersResponse createResponseWithDuplicates() {
    com.aci.smart_onboarding.dto.WalletronUsersResponse response =
        new com.aci.smart_onboarding.dto.WalletronUsersResponse();

    com.aci.smart_onboarding.dto.WalletronUsersResponse.SavedUser savedUser =
        new com.aci.smart_onboarding.dto.WalletronUsersResponse.SavedUser();
    savedUser.setId("1");
    savedUser.setName("John Doe");
    savedUser.setEmail("john@example.com");
    savedUser.setRole("USER");
    savedUser.setWalletronId("WAL-123");
    savedUser.setBrdId("BRD-123");
    savedUser.setCreatedAt(java.time.LocalDateTime.now());

    response.setSavedUsers(List.of(savedUser));
    response.setDuplicateEmails(List.of("john@example.com")); // Duplicate email
    response.setTotalProcessed(2);
    response.setTotalDuplicates(1);

    return response;
  }
}

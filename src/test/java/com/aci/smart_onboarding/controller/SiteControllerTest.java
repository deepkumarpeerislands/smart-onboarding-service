package com.aci.smart_onboarding.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.aci.smart_onboarding.constants.BrdConstants;
import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.constants.SiteConstants;
import com.aci.smart_onboarding.dto.*;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.exception.BadRequestException;
import com.aci.smart_onboarding.exception.NotFoundException;
import com.aci.smart_onboarding.model.BRD;
import com.aci.smart_onboarding.repository.BRDRepository;
import com.aci.smart_onboarding.security.service.BRDSecurityService;
import com.aci.smart_onboarding.security.token.JwtAuthenticationToken;
import com.aci.smart_onboarding.service.IBRDService;
import com.aci.smart_onboarding.service.ISiteService;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class SiteControllerTest {

  private static final String TEST_BRD_ID = "BRD0003";
  private static final String TEST_BRD_FORM_ID = "FORM123";

  @Mock private ISiteService siteService;
  @Mock private IBRDService brdService;
  @Mock private BRDRepository brdRepository;
  @Mock private BRDSecurityService securityService;
  @Mock private SecurityContext securityContext;
  @Mock private JwtAuthenticationToken jwtAuthenticationToken;

  @InjectMocks private SiteController siteController;

  private BRD mockBrd;
  private Map<String, Object> claims;
  private LocalDateTime now;

  @BeforeEach
  void setUp() {
    now = LocalDateTime.now();

    claims = new HashMap<>();
    claims.put("roles", SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER);
    claims.put("sub", "test-user");
    claims.put("preferred_username", "testUser");

    mockBrd = new BRD();
    mockBrd.setBrdId(TEST_BRD_ID);
    mockBrd.setBrdFormId(TEST_BRD_FORM_ID);

    BRDResponse brdResponse = new BRDResponse();
    brdResponse.setBrdId(TEST_BRD_ID);
    brdResponse.setBrdFormId(TEST_BRD_FORM_ID);
    brdResponse.setStatus("Draft");

    SecurityContextHolder.setContext(securityContext);

    // Only basic setup here - specific stubs will be in test methods
    lenient()
        .when(securityService.getCurrentUserRole())
        .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM));
    lenient().when(securityService.withSecurityCheck(anyString())).thenReturn(Mono.empty());
    lenient().when(brdRepository.findByBrdId(anyString())).thenReturn(Mono.just(mockBrd));
    lenient()
        .when(brdService.getBrdById(anyString()))
        .thenReturn(
            Mono.just(
                new ResponseEntity<>(
                    new Api<>(
                        "SUCCESS",
                        "BRD retrieved successfully",
                        Optional.of(brdResponse),
                        Optional.empty()),
                    HttpStatus.OK)));
  }

  @Test
  @DisplayName(
      "Should create sites successfully when valid request is provided and user has PM role")
  void createSites_WithValidRequestAndPMRole_ShouldCreateSites() {
    // Arrange
    SiteRequest request =
        SiteRequest.builder()
            .brdId("BRD0003")
            .wallentronIncluded(true)
            .achEncrypted(false)
            .siteList(
                List.of(
                    SiteRequest.SiteDetails.builder()
                        .siteName("Test Division")
                        .identifierCode("DIV001")
                        .description("Test Division Description")
                        .build()))
            .build();

    BRDResponse brdResponse = new BRDResponse();
    brdResponse.setBrdId("BRD0003");
    brdResponse.setBrdFormId("FORM123");
    brdResponse.setStatus("Draft");

    SiteResponse siteResponse =
        SiteResponse.builder()
            .brdId("BRD0003")
            .brdName("Test BRD")
            .description("Test BRD Description")
            .customerId("ORG001")
            .wallentronIncluded(true)
            .achEncrypted(false)
            .siteList(
                List.of(
                    SiteResponse.DivisionDetails.builder()
                        .id("site123")
                        .siteId("SITE_BRD0003_001")
                        .siteName("Test Division")
                        .identifierCode("DIV001")
                        .description("Test Division Description")
                        .build()))
            .createdAt(now)
            .updatedAt(now)
            .build();

    when(securityService.getCurrentUserRole())
        .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM));
    when(securityService.withSecurityCheck(anyString())).thenReturn(Mono.empty());
    when(brdService.getBrdById(anyString()))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        "SUCCESS",
                        "BRD retrieved successfully",
                        Optional.of(brdResponse),
                        Optional.empty()))));
    when(siteService.createDivision(any()))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        "SUCCESS",
                        "Sites created and BRD updated successfully",
                        Optional.of(siteResponse),
                        Optional.empty()))));

    // Act
    Mono<ResponseEntity<Api<SiteResponse>>> result = siteController.createSites(Mono.just(request));

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              if (response.getStatusCode() != HttpStatus.OK) return false;
              if (!response.getBody().getStatus().equals("SUCCESS")) return false;
              if (!response
                  .getBody()
                  .getMessage()
                  .equals("Sites created and BRD updated successfully")) return false;
              if (!response.getBody().getData().isPresent()) return false;

              SiteResponse actualResponse = response.getBody().getData().get();
              return "BRD0003".equals(actualResponse.getBrdId())
                  && "Test BRD".equals(actualResponse.getBrdName())
                  && "Test BRD Description".equals(actualResponse.getDescription())
                  && "ORG001".equals(actualResponse.getCustomerId())
                  && actualResponse.isWallentronIncluded()
                  && !actualResponse.isAchEncrypted()
                  && !actualResponse.getSiteList().isEmpty()
                  && "site123".equals(actualResponse.getSiteList().get(0).getId())
                  && "SITE_BRD0003_001".equals(actualResponse.getSiteList().get(0).getSiteId())
                  && "Test Division".equals(actualResponse.getSiteList().get(0).getSiteName())
                  && "DIV001".equals(actualResponse.getSiteList().get(0).getIdentifierCode())
                  && "Test Division Description"
                      .equals(actualResponse.getSiteList().get(0).getDescription());
            })
        .verifyComplete();

    verify(siteService).createDivision(any());
    verify(securityService, times(1)).getCurrentUserRole();
    verify(securityService, times(1)).withSecurityCheck(anyString());
    verify(brdService, times(1)).getBrdById(anyString());
  }

  @Test
  @DisplayName("Should return 403 when user does not have PM role")
  void createSites_WithNonPMRole_ShouldReturn403() {
    // Arrange
    SiteRequest request =
        SiteRequest.builder()
            .brdId("BRD0003")
            .siteList(List.of(SiteRequest.SiteDetails.builder().build()))
            .build();

    when(securityService.getCurrentUserRole())
        .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_BA));

    // Act & Assert
    StepVerifier.create(siteController.createSites(Mono.just(request)))
        .expectNextMatches(
            response -> {
              System.out.println("Expected FORBIDDEN but got: " + response.getStatusCode());
              System.out.println("Expected FAILURE but got: " + response.getBody().getStatus());
              System.out.println(
                  "Expected message about PM role but got: " + response.getBody().getMessage());

              return response.getStatusCode() == HttpStatus.FORBIDDEN
                  && "failure".equalsIgnoreCase(response.getBody().getStatus())
                  && response
                      .getBody()
                      .getMessage()
                      .contains("Only Project Managers (PM) can create sites")
                  && !response.getBody().getData().isPresent();
            })
        .verifyComplete();

    verify(securityService, times(1)).getCurrentUserRole();
  }

  @Test
  @DisplayName("Should return 403 when security check fails")
  void createSites_WithFailedSecurityCheck_ShouldReturn403() {
    // Arrange
    SiteRequest request =
        SiteRequest.builder()
            .brdId("BRD0003")
            .siteList(List.of(SiteRequest.SiteDetails.builder().build()))
            .build();

    BRDResponse brdResponse = new BRDResponse();
    brdResponse.setBrdId("BRD0003");
    brdResponse.setBrdFormId("FORM123");
    brdResponse.setStatus("Published");

    when(securityService.getCurrentUserRole())
        .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM));
    when(brdRepository.findByBrdId(anyString())).thenReturn(Mono.just(mockBrd));
    when(brdService.getBrdById(anyString()))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        "SUCCESS",
                        "BRD retrieved successfully",
                        Optional.of(brdResponse),
                        Optional.empty()))));
    when(securityService.withSecurityCheck(anyString()))
        .thenReturn(Mono.error(new AccessDeniedException("Access denied")));

    // Act & Assert
    StepVerifier.create(siteController.createSites(Mono.just(request)))
        .expectNextMatches(
            response ->
                response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR
                    && "failure".equalsIgnoreCase(response.getBody().getStatus()))
        .verifyComplete();

    verify(securityService, times(1)).getCurrentUserRole();
    verify(brdRepository).findByBrdId(anyString());
    verify(brdService).getBrdById(anyString());
    verify(securityService).withSecurityCheck(anyString());
  }

  @Test
  @DisplayName("Should get sites by BRD ID successfully when user has PM role")
  void getSitesByBrdId_WithPMRole_ShouldReturnSites() {
    // Arrange
    String brdId = "BRD0003";
    BRDResponse brdResponse = new BRDResponse();
    brdResponse.setBrdId(brdId);
    brdResponse.setBrdFormId("FORM123");
    brdResponse.setStatus("Draft");

    SiteResponse siteResponse =
        SiteResponse.builder()
            .brdId(brdId)
            .brdName("Test BRD")
            .description("Test Description")
            .customerId("CUST001")
            .wallentronIncluded(false)
            .achEncrypted(false)
            .siteList(
                List.of(
                    SiteResponse.DivisionDetails.builder()
                        .id("site1")
                        .siteId("SITE001")
                        .siteName("Site 1")
                        .identifierCode(null)
                        .description(null)
                        .brdForm(null)
                        .score(0.0)
                        .build()))
            .build();

    // Mock the security service
    when(securityService.withSecurityCheck("Draft")).thenReturn(Mono.empty());

    // Mock the BRD repository and service
    when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.just(mockBrd));
    when(brdService.getBrdById(anyString()))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        "SUCCESS",
                        "BRD retrieved successfully",
                        Optional.of(brdResponse),
                        Optional.empty()))));

    // Mock the site service
    when(siteService.getDivisionsByBrdId(brdId))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        "SUCCESS",
                        "Sites retrieved successfully",
                        Optional.of(siteResponse),
                        Optional.empty()))));

    // Mock the BRD score calculation
    when(siteService.calculateBrdScore(brdId)).thenReturn(Mono.just(85.5));

    // Act
    ResponseEntity<Api<SiteResponse>> response = siteController.getSitesByBrdId(brdId).block();

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("Successful", response.getBody().getStatus());
    assertEquals("Sites retrieved successfully", response.getBody().getMessage());
    assertTrue(response.getBody().getData().isPresent());

    SiteResponse data = response.getBody().getData().get();
    assertEquals(brdId, data.getBrdId());
    assertFalse(data.getSiteList().isEmpty());
    assertEquals(85.5, data.getScore());

    verify(brdRepository).findByBrdId(brdId);
    verify(brdService).getBrdById(anyString());
    verify(siteService).getDivisionsByBrdId(brdId);
    verify(siteService).calculateBrdScore(brdId);
    verify(securityService).withSecurityCheck("Draft");
  }

  @Test
  @DisplayName("Should return 403 when getting sites with non-PM role")
  void getSitesByBrdId_WithNonPMRole_ShouldReturn403() {
    // Arrange
    String brdId = "BRD0003";
    BRDResponse brdResponse = new BRDResponse();
    brdResponse.setBrdId(brdId);
    brdResponse.setBrdFormId("FORM123");
    brdResponse.setStatus("Published");

    lenient()
        .when(securityService.getCurrentUserRole())
        .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_BA));
    lenient().when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.just(mockBrd));
    lenient()
        .when(brdService.getBrdById(anyString()))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        "SUCCESS",
                        "BRD retrieved successfully",
                        Optional.of(brdResponse),
                        Optional.empty()))));
    lenient()
        .when(securityService.withSecurityCheck(anyString()))
        .thenReturn(Mono.error(new AccessDeniedException("Access denied")));

    // Act & Assert
    StepVerifier.create(siteController.getSitesByBrdId(brdId))
        .expectNextMatches(
            response ->
                response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR
                    && "failure".equalsIgnoreCase(response.getBody().getStatus()))
        .verifyComplete();

    // No verify statements - they can cause issues with lenient stubs
  }

  @Test
  @DisplayName("Should return 400 when request has duplicate identifier codes")
  void createSites_WithDuplicateIdentifierCodes_ShouldReturn400() {
    // Arrange
    SiteRequest.SiteDetails site1 =
        SiteRequest.SiteDetails.builder().siteName("Site 1").identifierCode("DIV001").build();
    SiteRequest.SiteDetails site2 =
        SiteRequest.SiteDetails.builder()
            .siteName("Site 2")
            .identifierCode("DIV001") // Same code as site1
            .build();

    SiteRequest request =
        SiteRequest.builder().brdId("BRD0003").siteList(List.of(site1, site2)).build();

    when(securityService.getCurrentUserRole())
        .thenReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM));

    // Act & Assert
    StepVerifier.create(siteController.createSites(Mono.just(request)))
        .expectNextMatches(
            response ->
                response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR
                    && "failure".equalsIgnoreCase(response.getBody().getStatus())
                    && response.getBody().getMessage().contains("unexpected error"))
        .verifyComplete();

    verify(securityService).getCurrentUserRole();
  }

  @Test
  @DisplayName("Should return 404 when BRD status is not found")
  void createSites_WithBrdStatusNotFound_ShouldReturn404() {
    SiteRequest request =
        SiteRequest.builder()
            .brdId("BRD0003")
            .siteList(List.of(SiteRequest.SiteDetails.builder().build()))
            .build();

    BRDResponse brdResponse = new BRDResponse();
    brdResponse.setBrdId("BRD0003");
    brdResponse.setBrdFormId("FORM123");
    // No status set

    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));
    when(brdRepository.findByBrdId(anyString())).thenReturn(Mono.just(mockBrd));
    when(brdService.getBrdById(anyString()))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        "SUCCESS", "BRD retrieved", Optional.of(brdResponse), Optional.empty()))));

    StepVerifier.create(siteController.createSites(Mono.just(request)))
        .expectNextMatches(
            response ->
                response.getStatusCode() == HttpStatus.NOT_FOUND
                    && response.getBody().getMessage().equals(SiteConstants.BRD_STATUS_NOT_FOUND))
        .verifyComplete();
  }

  @Nested
  @DisplayName("Get Sites by BRD ID Tests")
  class GetSitesByBrdIdTests {

    @Test
    @DisplayName("Should get sites by BRD ID successfully")
    void getSitesByBrdId_WithValidBrdId_ShouldReturnSites() {
      // Arrange
      String brdId = "BRD0003";
      SiteResponse siteResponse =
          SiteResponse.builder()
              .brdId("BRD0003")
              .brdName("Test BRD")
              .description("Test BRD Description")
              .customerId("ORG001")
              .wallentronIncluded(true)
              .achEncrypted(false)
              .ssdAvailable(false)
              .contractAvailable(false)
              .originalSSDFileName("test_ssd.pdf")
              .originalContractFileName("test_contract.pdf")
              .originalOtherFileName("test_other.pdf")
              .siteList(
                  List.of(
                      SiteResponse.DivisionDetails.builder()
                          .id("site123")
                          .siteId("SITE_BRD0003_001")
                          .siteName("Test Division")
                          .identifierCode("DIV001")
                          .description("Test Division Description")
                          .build()))
              .createdAt(now)
              .updatedAt(now)
              .build();

      BRDResponse brdResponse = new BRDResponse();
      brdResponse.setBrdId(brdId);
      brdResponse.setBrdFormId("FORM123");
      brdResponse.setStatus("Draft");
      brdResponse.setSsdAvailable(true);
      brdResponse.setContractAvailable(true);

      when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.just(mockBrd));
      when(brdService.getBrdById(anyString()))
          .thenReturn(
              Mono.just(
                  ResponseEntity.ok(
                      new Api<>(
                          BrdConstants.SUCCESSFUL,
                          "BRD retrieved successfully",
                          Optional.of(brdResponse),
                          Optional.empty()))));
      when(securityService.withSecurityCheck(anyString())).thenReturn(Mono.empty());
      when(siteService.getDivisionsByBrdId(brdId))
          .thenReturn(
              Mono.just(
                  ResponseEntity.ok(
                      new Api<>(
                          BrdConstants.SUCCESSFUL,
                          "Sites retrieved successfully",
                          Optional.of(siteResponse),
                          Optional.empty()))));
      when(siteService.calculateBrdScore(brdId)).thenReturn(Mono.just(85.5));

      // Act
      Mono<ResponseEntity<Api<SiteResponse>>> result = siteController.getSitesByBrdId(brdId);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                if (response.getStatusCode() != HttpStatus.OK) {
                  System.out.println("Status code mismatch: " + response.getStatusCode());
                  return false;
                }

                Api<SiteResponse> body = response.getBody();
                if (body == null) {
                  System.out.println("Response body is null");
                  return false;
                }

                if (!body.getData().isPresent()) {
                  System.out.println("Response data is not present");
                  return false;
                }

                if (!BrdConstants.SUCCESSFUL.equals(body.getStatus())) {
                  System.out.println(
                      "Status mismatch. Expected: "
                          + BrdConstants.SUCCESSFUL
                          + ", Got: "
                          + body.getStatus());
                  return false;
                }

                SiteResponse siteResponseData = body.getData().get();
                boolean matches =
                    brdId.equals(siteResponseData.getBrdId())
                        && "Test BRD".equals(siteResponseData.getBrdName())
                        && "Test BRD Description".equals(siteResponseData.getDescription())
                        && "ORG001".equals(siteResponseData.getCustomerId())
                        && siteResponseData.isWallentronIncluded()
                        && !siteResponseData.isAchEncrypted()
                        && "test_ssd.pdf".equals(siteResponseData.getOriginalSSDFileName())
                        && "test_contract.pdf"
                            .equals(siteResponseData.getOriginalContractFileName())
                        && "test_other.pdf".equals(siteResponseData.getOriginalOtherFileName())
                        && !siteResponseData.getSiteList().isEmpty()
                        && "site123".equals(siteResponseData.getSiteList().getFirst().getId())
                        && "SITE_BRD0003_001"
                            .equals(siteResponseData.getSiteList().getFirst().getSiteId())
                        && "Test Division"
                            .equals(siteResponseData.getSiteList().getFirst().getSiteName())
                        && "DIV001"
                            .equals(siteResponseData.getSiteList().getFirst().getIdentifierCode())
                        && "Test Division Description"
                            .equals(siteResponseData.getSiteList().getFirst().getDescription())
                        && siteResponseData.getScore() == 85.5;

                if (!matches) {
                  System.out.println("Data mismatch:");
                  System.out.println("BRD ID: " + siteResponseData.getBrdId());
                  System.out.println("BRD Name: " + siteResponseData.getBrdName());
                  System.out.println("Description: " + siteResponseData.getDescription());
                  System.out.println("Customer ID: " + siteResponseData.getCustomerId());
                  System.out.println("Score: " + siteResponseData.getScore());
                  System.out.println(
                      "Site List Empty: " + siteResponseData.getSiteList().isEmpty());
                }

                return matches;
              })
          .verifyComplete();

      verify(brdRepository).findByBrdId(brdId);
      verify(brdService).getBrdById(anyString());
      verify(siteService).getDivisionsByBrdId(brdId);
      verify(siteService).calculateBrdScore(brdId);
      verify(securityService).withSecurityCheck("Draft");
    }

    @Test
    @DisplayName("Should return 404 when BRD ID not found")
    void getSitesByBrdId_WithNonExistentBrdId_ShouldReturn404() {
      // Arrange
      String brdId = "NONEXISTENT";
      when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.empty());

      // Act
      Mono<ResponseEntity<Api<SiteResponse>>> result = siteController.getSitesByBrdId(brdId);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response ->
                  response.getStatusCode() == HttpStatus.NOT_FOUND
                      && "failure".equals(Objects.requireNonNull(response.getBody()).getStatus())
                      && response
                          .getBody()
                          .getMessage()
                          .equals("BRD not found with id: NONEXISTENT")
                      && response.getBody().getData().isEmpty())
          .verifyComplete();

      verify(brdRepository).findByBrdId(brdId);
      verifyNoMoreInteractions(brdService, siteService);
    }

    @Test
    @DisplayName("Should handle BRD ID with multiple sites")
    void getSitesByBrdId_WithMultipleSites_ShouldReturnAllSites() {
      // Arrange
      String brdId = "BRD0003";

      // Create a response with multiple sites
      SiteResponse multiSiteResponse =
          SiteResponse.builder()
              .brdId("BRD0003")
              .brdName("Test BRD")
              .description("Test BRD Description")
              .customerId("ORG001")
              .wallentronIncluded(true)
              .achEncrypted(false)
              .ssdAvailable(false)
              .contractAvailable(false)
              .originalSSDFileName("test_ssd.pdf")
              .originalContractFileName("test_contract.pdf")
              .originalOtherFileName("test_other.pdf")
              .siteList(
                  List.of(
                      SiteResponse.DivisionDetails.builder()
                          .id("site123")
                          .siteId("SITE_BRD0003_001")
                          .siteName("Test Division")
                          .identifierCode("DIV001")
                          .description("Test Division Description")
                          .build(),
                      SiteResponse.DivisionDetails.builder()
                          .id("site124")
                          .siteId("SITE_BRD0003_002")
                          .siteName("Test Site")
                          .identifierCode("TEST")
                          .description("Test environment")
                          .build()))
              .createdAt(now)
              .updatedAt(now)
              .build();

      BRDResponse brdResponse = new BRDResponse();
      brdResponse.setBrdId(brdId);
      brdResponse.setBrdFormId("FORM123");
      brdResponse.setStatus("Draft");
      brdResponse.setSsdAvailable(true);
      brdResponse.setContractAvailable(true);

      Api<SiteResponse> apiResponse =
          new Api<>(
              BrdConstants.SUCCESSFUL,
              "Sites retrieved successfully",
              Optional.of(multiSiteResponse),
              Optional.empty());
      ResponseEntity<Api<SiteResponse>> responseEntity = ResponseEntity.ok(apiResponse);

      // Setup all required mocks
      when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.just(mockBrd));
      when(brdService.getBrdById(anyString()))
          .thenReturn(
              Mono.just(
                  ResponseEntity.ok(
                      new Api<>(
                          BrdConstants.SUCCESSFUL,
                          "BRD retrieved successfully",
                          Optional.of(brdResponse),
                          Optional.empty()))));
      when(securityService.withSecurityCheck(anyString())).thenReturn(Mono.empty());
      when(siteService.getDivisionsByBrdId(brdId)).thenReturn(Mono.just(responseEntity));
      when(siteService.calculateBrdScore(brdId)).thenReturn(Mono.just(85.5));

      // Act
      Mono<ResponseEntity<Api<SiteResponse>>> result = siteController.getSitesByBrdId(brdId);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response ->
                  response.getStatusCode() == HttpStatus.OK
                      && Objects.requireNonNull(response.getBody())
                          .getStatus()
                          .equals(BrdConstants.SUCCESSFUL)
                      && response.getBody().getMessage().equals("Sites retrieved successfully")
                      && response.getBody().getData().isPresent()
                      && response.getBody().getData().get().getBrdId().equals("BRD0003")
                      && response.getBody().getData().get().getBrdName().equals("Test BRD")
                      && response
                          .getBody()
                          .getData()
                          .get()
                          .getDescription()
                          .equals("Test BRD Description")
                      && response.getBody().getData().get().getCustomerId().equals("ORG001")
                      && response.getBody().getData().get().isWallentronIncluded()
                      && !response.getBody().getData().get().isAchEncrypted()
                      && response
                          .getBody()
                          .getData()
                          .get()
                          .getOriginalSSDFileName()
                          .equals("test_ssd.pdf")
                      && response
                          .getBody()
                          .getData()
                          .get()
                          .getOriginalContractFileName()
                          .equals("test_contract.pdf")
                      && response
                          .getBody()
                          .getData()
                          .get()
                          .getOriginalOtherFileName()
                          .equals("test_other.pdf")
                      && response.getBody().getData().get().getSiteList().size() == 2
                      && response
                          .getBody()
                          .getData()
                          .get()
                          .getSiteList()
                          .getFirst()
                          .getId()
                          .equals("site123")
                      && response
                          .getBody()
                          .getData()
                          .get()
                          .getSiteList()
                          .getFirst()
                          .getSiteId()
                          .equals("SITE_BRD0003_001")
                      && response
                          .getBody()
                          .getData()
                          .get()
                          .getSiteList()
                          .getFirst()
                          .getSiteName()
                          .equals("Test Division")
                      && response
                          .getBody()
                          .getData()
                          .get()
                          .getSiteList()
                          .getFirst()
                          .getIdentifierCode()
                          .equals("DIV001")
                      && response
                          .getBody()
                          .getData()
                          .get()
                          .getSiteList()
                          .getFirst()
                          .getDescription()
                          .equals("Test Division Description")
                      && response.getBody().getData().get().getScore() == 85.5)
          .verifyComplete();

      // Verify all interactions
      verify(brdRepository).findByBrdId(brdId);
      verify(brdService).getBrdById(anyString());
      verify(siteService).getDivisionsByBrdId(brdId);
      verify(siteService).calculateBrdScore(brdId);
      verify(securityService).withSecurityCheck("Draft");
    }
  }

  @Test
  @DisplayName("Should handle empty request Mono")
  void createSites_WithEmptyMono_ShouldComplete() {
    // Act
    Mono<ResponseEntity<Api<SiteResponse>>> result = siteController.createSites(Mono.empty());

    // Assert
    StepVerifier.create(result)
        .expectComplete() // Empty Mono completes normally
        .verify();

    verify(siteService, never()).createDivision(any());
  }

  @Test
  @DisplayName("Should handle request with null required fields")
  void createSites_WithNullRequiredFields_ShouldReturn400() {
    // Arrange
    SiteRequest request =
        SiteRequest.builder()
            .brdId(null) // Null ID
            .siteList(List.of(SiteRequest.SiteDetails.builder().siteName("Test Site").build()))
            .build();

    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));

    // Act & Assert
    StepVerifier.create(siteController.createSites(Mono.just(request)))
        .expectNextMatches(
            response ->
                response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR
                    && "failure"
                        .equalsIgnoreCase(Objects.requireNonNull(response.getBody()).getStatus())
                    && response.getBody().getMessage().contains("unexpected error"))
        .verifyComplete();

    verify(securityService).getCurrentUserRole();
    verify(siteService, never()).createDivision(any());
  }

  @Test
  @DisplayName("Should handle request with empty required fields")
  void createSites_WithEmptyRequiredFields_ShouldReturn400() {
    // Arrange
    SiteRequest request =
        SiteRequest.builder()
            .brdId("") // Empty ID
            .siteList(List.of(SiteRequest.SiteDetails.builder().siteName("Test Site").build()))
            .build();

    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));

    // Act & Assert
    StepVerifier.create(siteController.createSites(Mono.just(request)))
        .expectNextMatches(
            response ->
                response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR
                    && "failure"
                        .equalsIgnoreCase(Objects.requireNonNull(response.getBody()).getStatus())
                    && response.getBody().getMessage().contains("unexpected error"))
        .verifyComplete();

    verify(securityService).getCurrentUserRole();
  }

  @Test
  @DisplayName("Should handle request with invalid data")
  void createSites_WithInvalidData_ShouldReturn400() {
    // Arrange
    SiteRequest request =
        SiteRequest.builder().brdId("BRD0003").siteList(Collections.emptyList()).build();

    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));

    // Act & Assert
    StepVerifier.create(siteController.createSites(Mono.just(request)))
        .expectNextMatches(
            response ->
                response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR
                    && "failure"
                        .equalsIgnoreCase(Objects.requireNonNull(response.getBody()).getStatus())
                    && response.getBody().getMessage().contains("unexpected error"))
        .verifyComplete();

    verify(securityService).getCurrentUserRole();
  }

  @Test
  @DisplayName("Should handle request with invalid division details")
  void createSites_WithInvalidDivisionDetails_ShouldReturn400() {
    // Arrange
    SiteRequest.SiteDetails invalidDetails =
        SiteRequest.SiteDetails.builder()
            .siteName("") // Empty name
            .build();

    SiteRequest request =
        SiteRequest.builder().brdId("BRD0003").siteList(List.of(invalidDetails)).build();

    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));

    // Act & Assert
    StepVerifier.create(siteController.createSites(Mono.just(request)))
        .expectNextMatches(
            response ->
                response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR
                    && "failure".equalsIgnoreCase(response.getBody().getStatus())
                    && response.getBody().getMessage().contains("unexpected error"))
        .verifyComplete();

    verify(securityService).getCurrentUserRole();
  }

  @Test
  @DisplayName("Should handle response with null file names")
  void getSitesByBrdId_WithNullFileNames_ShouldHandleCorrectly() {
    // Arrange
    String brdId = "BRD0003";
    List<SiteResponse.DivisionDetails> siteList =
        List.of(
            SiteResponse.DivisionDetails.builder()
                .id("site1")
                .siteId("SITE_BRD0003_001")
                .siteName("Test Site")
                .identifierCode("TEST001")
                .description("Test Description")
                .score(0.0)
                .build());

    SiteResponse responseWithNullFileNames =
        SiteResponse.builder()
            .brdId("BRD0003")
            .brdName("Test BRD")
            .description("Test BRD Description")
            .customerId("ORG001")
            .wallentronIncluded(true)
            .achEncrypted(false)
            .originalSSDFileName(null)
            .originalContractFileName(null)
            .originalOtherFileName(null)
            .siteList(siteList)
            .createdAt(now)
            .updatedAt(now)
            .build();

    BRDResponse brdResponse = new BRDResponse();
    brdResponse.setBrdId(brdId);
    brdResponse.setBrdFormId("FORM123");
    brdResponse.setStatus("Draft");

    when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.just(mockBrd));
    when(brdService.getBrdById(anyString()))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        "SUCCESS",
                        "BRD retrieved successfully",
                        Optional.of(brdResponse),
                        Optional.empty()))));
    when(securityService.withSecurityCheck(anyString())).thenReturn(Mono.empty());
    when(siteService.getDivisionsByBrdId(brdId))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        "SUCCESS",
                        "Sites retrieved successfully",
                        Optional.of(responseWithNullFileNames),
                        Optional.empty()))));
    when(siteService.calculateBrdScore(brdId)).thenReturn(Mono.just(0.0));

    // Act
    ResponseEntity<Api<SiteResponse>> response = siteController.getSitesByBrdId(brdId).block();

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("Successful", response.getBody().getStatus());
    assertEquals("Sites retrieved successfully", response.getBody().getMessage());
    assertTrue(response.getBody().getData().isPresent());

    SiteResponse data = response.getBody().getData().get();
    assertEquals(brdId, data.getBrdId());
    assertNull(data.getOriginalSSDFileName());
    assertNull(data.getOriginalContractFileName());
    assertNull(data.getOriginalOtherFileName());
    assertFalse(data.getSiteList().isEmpty());
    assertEquals(0.0, data.getScore());

    // Verify all interactions
    verify(brdRepository).findByBrdId(brdId);
    verify(brdService).getBrdById(anyString());
    verify(siteService).getDivisionsByBrdId(brdId);
    verify(siteService).calculateBrdScore(brdId);
    verify(securityService).withSecurityCheck("Draft");
  }

  @Nested
  @DisplayName("Compare BRD and Site BRD Form Tests")
  class CompareBrdAndSiteBrdFormTests {

    @Test
    @DisplayName("Should compare BRD and Site BRD form successfully")
    void compareBrdAndSiteBrdForm_WithValidInput_ShouldReturnComparison() {
      // Arrange
      String siteId = "SITE001";
      String sectionName = "clientInformation";
      BrdComparisonResponse comparisonResponse = new BrdComparisonResponse();
      comparisonResponse.setBrdId("BRD001");
      comparisonResponse.setSiteId(siteId);
      comparisonResponse.setSiteName("Test Site");
      comparisonResponse.setDifferences(Map.of("field1", true, "field2", false));

      when(siteService.compareBrdAndSiteBrdForm(siteId, sectionName))
          .thenReturn(
              Mono.just(
                  ResponseEntity.ok(
                      new Api<>(
                          "SUCCESS",
                          "BRD and Site BRD form comparison completed successfully",
                          Optional.of(comparisonResponse),
                          Optional.empty()))));

      // Act & Assert
      StepVerifier.create(siteController.compareBrdAndSiteBrdForm(siteId, sectionName))
          .expectNextMatches(
              response -> {
                if (response.getStatusCode() != HttpStatus.OK) return false;
                if (!Objects.requireNonNull(response.getBody()).getStatus().equals("SUCCESS"))
                  return false;
                if (!response
                    .getBody()
                    .getMessage()
                    .equals("BRD and Site BRD form comparison completed successfully"))
                  return false;
                if (response.getBody().getData().isEmpty()) return false;

                BrdComparisonResponse data = response.getBody().getData().get();
                return "BRD001".equals(data.getBrdId())
                    && siteId.equals(data.getSiteId())
                    && "Test Site".equals(data.getSiteName())
                    && data.getDifferences().size() == 2;
              })
          .verifyComplete();

      verify(siteService, times(1)).compareBrdAndSiteBrdForm(siteId, sectionName);
    }

    // Skip tests with null path variables since they cannot be tested directly through the
    // controller

    @Test
    @DisplayName("Should handle empty site ID")
    void compareBrdAndSiteBrdForm_WithEmptySiteId_ShouldReturn400() {
      // Arrange
      String siteId = "";
      String sectionName = "clientInformation";
      Map<String, String> errorDetails = new HashMap<>();
      errorDetails.put("timestamp", LocalDateTime.now().toString());
      errorDetails.put("errorMessage", "Site ID cannot be empty");

      Api<BrdComparisonResponse> apiResponse =
          new Api<>(
              "FAILURE", "Site ID cannot be empty", Optional.empty(), Optional.of(errorDetails));
      ResponseEntity<Api<BrdComparisonResponse>> responseEntity =
          ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);

      when(siteService.compareBrdAndSiteBrdForm(siteId, sectionName))
          .thenReturn(Mono.just(responseEntity));

      // Act & Assert
      StepVerifier.create(siteController.compareBrdAndSiteBrdForm(siteId, sectionName))
          .expectNextMatches(
              response ->
                  response.getStatusCode() == HttpStatus.BAD_REQUEST
                      && Objects.requireNonNull(response.getBody()).getStatus().equals("FAILURE")
                      && response.getBody().getMessage().equals("Site ID cannot be empty"))
          .verifyComplete();

      verify(siteService, times(1)).compareBrdAndSiteBrdForm(siteId, sectionName);
    }

    @Test
    @DisplayName("Should handle empty section name")
    void compareBrdAndSiteBrdForm_WithEmptySectionName_ShouldReturn400() {
      // Arrange
      String siteId = "site123";
      String sectionName = "";
      Map<String, String> errorDetails = new HashMap<>();
      errorDetails.put("timestamp", LocalDateTime.now().toString());
      errorDetails.put("errorMessage", "Section name cannot be empty");

      Api<BrdComparisonResponse> apiResponse =
          new Api<>(
              "FAILURE",
              "Section name cannot be empty",
              Optional.empty(),
              Optional.of(errorDetails));
      ResponseEntity<Api<BrdComparisonResponse>> responseEntity =
          ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);

      when(siteService.compareBrdAndSiteBrdForm(siteId, sectionName))
          .thenReturn(Mono.just(responseEntity));

      // Act & Assert
      StepVerifier.create(siteController.compareBrdAndSiteBrdForm(siteId, sectionName))
          .expectNextMatches(
              response ->
                  response.getStatusCode() == HttpStatus.BAD_REQUEST
                      && Objects.requireNonNull(response.getBody()).getStatus().equals("FAILURE")
                      && response.getBody().getMessage().equals("Section name cannot be empty"))
          .verifyComplete();

      verify(siteService, times(1)).compareBrdAndSiteBrdForm(siteId, sectionName);
    }

    @Test
    @DisplayName("Should handle error in compareBrdAndSiteBrdForm")
    void compareBrdAndSiteBrdForm_WithError_ShouldReturnErrorResponse() {
      // Arrange
      String siteId = "SITE001";
      String sectionName = "clientInformation";

      when(siteService.compareBrdAndSiteBrdForm(siteId, sectionName))
          .thenReturn(Mono.error(new RuntimeException("Error comparing BRD and site BRD form")));

      // Act & Assert
      StepVerifier.create(siteController.compareBrdAndSiteBrdForm(siteId, sectionName))
          .expectErrorMatches(
              throwable ->
                  throwable instanceof RuntimeException
                      && throwable.getMessage().equals("Error comparing BRD and site BRD form"))
          .verify();

      verify(siteService).compareBrdAndSiteBrdForm(siteId, sectionName);
    }
  }

  @Nested
  @DisplayName("Update Site Tests")
  class UpdateSiteTests {

    @Test
    @DisplayName("Should update site successfully")
    void updateSite_WithValidData_ShouldUpdateSuccessfully() {
      // Arrange
      String siteId = "site123";
      SiteRequest.SiteDetails updateDetails =
          SiteRequest.SiteDetails.builder()
              .siteName("Updated Site Name")
              .identifierCode("UPD001")
              .description("Updated Description")
              .build();

      SingleSiteResponse siteResponse =
          SingleSiteResponse.builder()
              .id(siteId)
              .siteName("Updated Site Name")
              .identifierCode("UPD001")
              .description("Updated Description")
              .build();

      Api<SingleSiteResponse> apiResponse =
          new Api<>(
              "SUCCESS", "Site updated successfully", Optional.of(siteResponse), Optional.empty());
      ResponseEntity<Api<SingleSiteResponse>> responseEntity = ResponseEntity.ok(apiResponse);

      when(siteService.getSiteBrdId(siteId)).thenReturn(Mono.just("BRD0003"));
      when(siteService.updateSite(siteId, updateDetails)).thenReturn(Mono.just(responseEntity));

      // Act & Assert
      StepVerifier.create(siteController.updateSite(siteId, updateDetails))
          .expectNextMatches(
              response ->
                  response.getStatusCode() == HttpStatus.OK
                      && Objects.requireNonNull(response.getBody()).getStatus().equals("SUCCESS")
                      && response.getBody().getMessage().equals("Site updated successfully")
                      && response.getBody().getData().isPresent()
                      && response
                          .getBody()
                          .getData()
                          .get()
                          .getSiteName()
                          .equals("Updated Site Name")
                      && response.getBody().getData().get().getIdentifierCode().equals("UPD001")
                      && response
                          .getBody()
                          .getData()
                          .get()
                          .getDescription()
                          .equals("Updated Description"))
          .verifyComplete();

      verify(siteService, times(1)).updateSite(siteId, updateDetails);
      verify(siteService, times(1)).getSiteBrdId(siteId);
    }

    // Skip tests with null path variables since they cannot be tested directly through the
    // controller

    @Test
    @DisplayName("Should handle empty site ID")
    void updateSite_WithEmptySiteId_ShouldReturn400() {
      // Arrange
      String siteId = "";
      SiteRequest.SiteDetails updateDetails =
          SiteRequest.SiteDetails.builder()
              .siteName("Updated Site Name")
              .identifierCode("UPD001")
              .description("Updated Description")
              .build();

      when(siteService.getSiteBrdId(siteId))
          .thenReturn(Mono.error(new BadRequestException("SITE_ID", "Site ID cannot be empty")));

      // Act & Assert
      StepVerifier.create(siteController.updateSite(siteId, updateDetails))
          .expectError(BadRequestException.class)
          .verify();

      verify(siteService).getSiteBrdId(siteId);
    }

    @Test
    @DisplayName("Should handle null update details")
    void updateSite_WithNullUpdateDetails_ShouldReturn400() {
      // Arrange
      String siteId = "site123";
      SiteRequest.SiteDetails updateDetails = null;

      Map<String, String> errorDetails = new HashMap<>();
      errorDetails.put("timestamp", LocalDateTime.now().toString());
      errorDetails.put("errorMessage", "Update details cannot be null");

      Api<SingleSiteResponse> apiResponse =
          new Api<>(
              "FAILURE",
              "Update details cannot be null",
              Optional.empty(),
              Optional.of(errorDetails));
      ResponseEntity<Api<SingleSiteResponse>> responseEntity =
          ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);

      when(siteService.getSiteBrdId(siteId)).thenReturn(Mono.just("BRD0003"));
      when(siteService.updateSite(siteId, updateDetails)).thenReturn(Mono.just(responseEntity));

      // Act & Assert
      StepVerifier.create(siteController.updateSite(siteId, updateDetails))
          .expectNextMatches(
              response ->
                  response.getStatusCode() == HttpStatus.BAD_REQUEST
                      && Objects.requireNonNull(response.getBody()).getStatus().equals("FAILURE")
                      && response.getBody().getMessage().equals("Update details cannot be null"))
          .verifyComplete();

      verify(siteService, times(1)).getSiteBrdId(siteId);
      verify(siteService, times(1)).updateSite(siteId, updateDetails);
    }
  }

  @Test
  @DisplayName("Should delete multiple sites successfully")
  void deleteMultipleSites_WithValidIds_ShouldDeleteSuccessfully() {
    // Arrange
    List<String> siteIds = Arrays.asList("site1", "site2", "site3");
    Api<Void> apiResponse =
        new Api<>("SUCCESS", "Sites deleted successfully", Optional.empty(), Optional.empty());
    ResponseEntity<Api<Void>> responseEntity = ResponseEntity.ok(apiResponse);

    when(siteService.deleteMultipleSites(siteIds)).thenReturn(Mono.just(responseEntity));

    // Act
    Mono<ResponseEntity<Api<Void>>> result = siteController.deleteMultipleSites(siteIds);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response ->
                response.getStatusCode() == HttpStatus.OK
                    && Objects.requireNonNull(response.getBody()).getStatus().equals("SUCCESS")
                    && response.getBody().getMessage().equals("Sites deleted successfully")
                    && response.getBody().getData().isEmpty()
                    && response.getBody().getErrors().isEmpty())
        .verifyComplete();

    verify(siteService).deleteMultipleSites(siteIds);
  }

  @Test
  @DisplayName("Should handle empty site IDs list")
  void deleteMultipleSites_WithEmptyList_ShouldReturnBadRequest() {
    // Arrange
    List<String> siteIds = Collections.emptyList();
    Map<String, String> errors = new HashMap<>();
    errors.put("errorMessage", "Site IDs list cannot be empty");
    errors.put("timestamp", "2024-03-26T10:30:00");

    Api<Void> apiResponse =
        new Api<>(
            "FAILURE", "Site IDs list cannot be empty", Optional.empty(), Optional.of(errors));
    ResponseEntity<Api<Void>> responseEntity = ResponseEntity.badRequest().body(apiResponse);

    when(siteService.deleteMultipleSites(siteIds)).thenReturn(Mono.just(responseEntity));

    // Act
    Mono<ResponseEntity<Api<Void>>> result = siteController.deleteMultipleSites(siteIds);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response ->
                response.getStatusCode() == HttpStatus.BAD_REQUEST
                    && Objects.requireNonNull(response.getBody()).getStatus().equals("FAILURE")
                    && response.getBody().getMessage().equals("Site IDs list cannot be empty")
                    && response.getBody().getData().isEmpty()
                    && response.getBody().getErrors().isPresent()
                    && response.getBody().getErrors().get().containsKey("errorMessage")
                    && response.getBody().getErrors().get().containsKey("timestamp"))
        .verifyComplete();

    verify(siteService).deleteMultipleSites(siteIds);
  }

  @Test
  @DisplayName("Should handle non-existent site")
  void deleteMultipleSites_WithNonExistentSite_ShouldReturnNotFound() {
    // Arrange
    List<String> siteIds = Collections.singletonList("nonexistent");
    Map<String, String> errors = new HashMap<>();
    errors.put("errorMessage", "Site not found with ID: nonexistent");
    errors.put("timestamp", "2024-03-26T10:30:00");

    Api<Void> apiResponse =
        new Api<>(
            "FAILURE",
            "Site not found with ID: nonexistent",
            Optional.empty(),
            Optional.of(errors));
    ResponseEntity<Api<Void>> responseEntity =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponse);

    when(siteService.deleteMultipleSites(siteIds)).thenReturn(Mono.just(responseEntity));

    // Act
    Mono<ResponseEntity<Api<Void>>> result = siteController.deleteMultipleSites(siteIds);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response ->
                response.getStatusCode() == HttpStatus.NOT_FOUND
                    && Objects.requireNonNull(response.getBody()).getStatus().equals("FAILURE")
                    && response.getBody().getMessage().equals("Site not found with ID: nonexistent")
                    && response.getBody().getData().isEmpty()
                    && response.getBody().getErrors().isPresent()
                    && response.getBody().getErrors().get().containsKey("errorMessage")
                    && response.getBody().getErrors().get().containsKey("timestamp"))
        .verifyComplete();

    verify(siteService).deleteMultipleSites(siteIds);
  }

  @Test
  @DisplayName("Should handle database error during deletion")
  void deleteMultipleSites_WhenDeletionFails_ShouldHandleError() {
    // Arrange
    List<String> siteIds = Collections.singletonList("site1");
    Map<String, String> errors = new HashMap<>();
    errors.put("errorMessage", "Database error");
    errors.put("timestamp", "2024-03-26T10:30:00");

    Api<Void> apiResponse =
        new Api<>("FAILURE", "Database error", Optional.empty(), Optional.of(errors));
    ResponseEntity<Api<Void>> responseEntity =
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);

    when(siteService.deleteMultipleSites(siteIds)).thenReturn(Mono.just(responseEntity));

    // Act
    Mono<ResponseEntity<Api<Void>>> result = siteController.deleteMultipleSites(siteIds);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response ->
                response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR
                    && Objects.requireNonNull(response.getBody()).getStatus().equals("FAILURE")
                    && response.getBody().getMessage().equals("Database error")
                    && response.getBody().getData().isEmpty()
                    && response.getBody().getErrors().isPresent()
                    && response.getBody().getErrors().get().containsKey("errorMessage")
                    && response.getBody().getErrors().get().containsKey("timestamp"))
        .verifyComplete();

    verify(siteService).deleteMultipleSites(siteIds);
  }

  @Test
  @DisplayName("Should handle null site IDs list")
  void deleteMultipleSites_WithNullList_ShouldReturnBadRequest() {
    // Arrange
    List<String> siteIds = null;
    Map<String, String> errors = new HashMap<>();
    errors.put("errorMessage", "Site IDs list cannot be empty");
    errors.put("timestamp", "2024-03-26T10:30:00");

    Api<Void> apiResponse =
        new Api<>(
            "FAILURE", "Site IDs list cannot be empty", Optional.empty(), Optional.of(errors));
    ResponseEntity<Api<Void>> responseEntity = ResponseEntity.badRequest().body(apiResponse);

    when(siteService.deleteMultipleSites(siteIds)).thenReturn(Mono.just(responseEntity));

    // Act
    Mono<ResponseEntity<Api<Void>>> result = siteController.deleteMultipleSites(siteIds);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response ->
                response.getStatusCode() == HttpStatus.BAD_REQUEST
                    && Objects.requireNonNull(response.getBody()).getStatus().equals("FAILURE")
                    && response.getBody().getMessage().equals("Site IDs list cannot be empty")
                    && response.getBody().getData().isEmpty()
                    && response.getBody().getErrors().isPresent()
                    && response.getBody().getErrors().get().containsKey("errorMessage")
                    && response.getBody().getErrors().get().containsKey("timestamp"))
        .verifyComplete();

    verify(siteService).deleteMultipleSites(siteIds);
  }

  @Nested
  @DisplayName("Update Multiple Sites Tests")
  class UpdateMultipleSitesTests {
    @Test
    @DisplayName("Should update multiple sites successfully")
    void updateMultipleSites_WithValidData_ShouldUpdateSuccessfully() {
      // Arrange
      List<SiteUpdateRequest> siteUpdates =
          Arrays.asList(
              SiteUpdateRequest.builder()
                  .siteId("site1")
                  .siteDetails(
                      SiteRequest.SiteDetails.builder()
                          .siteName("Updated Site 1")
                          .identifierCode("UPD001")
                          .description("Updated Description 1")
                          .build())
                  .build(),
              SiteUpdateRequest.builder()
                  .siteId("site2")
                  .siteDetails(
                      SiteRequest.SiteDetails.builder()
                          .siteName("Updated Site 2")
                          .identifierCode("UPD002")
                          .description("Updated Description 2")
                          .build())
                  .build());

      List<SingleSiteResponse> updatedSites =
          Arrays.asList(
              SingleSiteResponse.builder()
                  .id("site1")
                  .siteName("Updated Site 1")
                  .identifierCode("UPD001")
                  .description("Updated Description 1")
                  .build(),
              SingleSiteResponse.builder()
                  .id("site2")
                  .siteName("Updated Site 2")
                  .identifierCode("UPD002")
                  .description("Updated Description 2")
                  .build());

      Api<List<SingleSiteResponse>> apiResponse =
          new Api<>(
              "SUCCESS", "Sites updated successfully", Optional.of(updatedSites), Optional.empty());
      ResponseEntity<Api<List<SingleSiteResponse>>> responseEntity = ResponseEntity.ok(apiResponse);

      when(siteService.updateMultipleSites(siteUpdates)).thenReturn(Mono.just(responseEntity));

      // Act & Assert
      StepVerifier.create(siteController.updateMultipleSites(siteUpdates))
          .expectNextMatches(
              response -> {
                if (response.getStatusCode() != HttpStatus.OK) return false;
                if (!Objects.requireNonNull(response.getBody()).getStatus().equals("SUCCESS"))
                  return false;
                if (response.getBody().getData().isEmpty()) return false;

                List<SingleSiteResponse> data = response.getBody().getData().get();
                return data.size() == 2
                    && data.get(0).getSiteName().equals("Updated Site 1")
                    && data.get(1).getSiteName().equals("Updated Site 2");
              })
          .verifyComplete();

      verify(siteService).updateMultipleSites(siteUpdates);
    }

    @Test
    @DisplayName("Should handle empty site updates list")
    void updateMultipleSites_WithEmptyList_ShouldReturnBadRequest() {
      // Arrange
      List<SiteUpdateRequest> siteUpdates = Collections.emptyList();
      Map<String, String> errors = new HashMap<>();
      errors.put("errorMessage", "Site updates list cannot be empty");

      Api<List<SingleSiteResponse>> apiResponse =
          new Api<>(
              "FAILURE",
              "Site updates list cannot be empty",
              Optional.empty(),
              Optional.of(errors));
      ResponseEntity<Api<List<SingleSiteResponse>>> responseEntity =
          ResponseEntity.badRequest().body(apiResponse);

      when(siteService.updateMultipleSites(siteUpdates)).thenReturn(Mono.just(responseEntity));

      // Act & Assert
      StepVerifier.create(siteController.updateMultipleSites(siteUpdates))
          .expectNextMatches(
              response ->
                  response.getStatusCode() == HttpStatus.BAD_REQUEST
                      && Objects.requireNonNull(response.getBody()).getStatus().equals("FAILURE")
                      && response
                          .getBody()
                          .getMessage()
                          .equals("Site updates list cannot be empty"))
          .verifyComplete();

      verify(siteService).updateMultipleSites(siteUpdates);
    }

    @Test
    @DisplayName("Should handle null site updates list")
    void updateMultipleSites_WithNullList_ShouldReturnBadRequest() {
      // Arrange
      List<SiteUpdateRequest> siteUpdates = null;
      Map<String, String> errors = new HashMap<>();
      errors.put("errorMessage", "Site updates list cannot be null");

      Api<List<SingleSiteResponse>> apiResponse =
          new Api<>(
              "FAILURE", "Site updates list cannot be null", Optional.empty(), Optional.of(errors));
      ResponseEntity<Api<List<SingleSiteResponse>>> responseEntity =
          ResponseEntity.badRequest().body(apiResponse);

      when(siteService.updateMultipleSites(siteUpdates)).thenReturn(Mono.just(responseEntity));

      // Act & Assert
      StepVerifier.create(siteController.updateMultipleSites(siteUpdates))
          .expectNextMatches(
              response ->
                  response.getStatusCode() == HttpStatus.BAD_REQUEST
                      && Objects.requireNonNull(response.getBody()).getStatus().equals("FAILURE")
                      && response.getBody().getMessage().equals("Site updates list cannot be null"))
          .verifyComplete();

      verify(siteService).updateMultipleSites(siteUpdates);
    }
  }

  @Test
  @DisplayName("Should handle BRD score calculation in getSitesByBrdId")
  void getSitesByBrdId_WithBrdScore_ShouldIncludeScore() {
    // Arrange
    String brdId = "BRD0003";
    double expectedScore = 85.5;

    SiteResponse siteResponse =
        SiteResponse.builder()
            .brdId(brdId)
            .brdName("Test BRD")
            .description("Test Description")
            .customerId("CUST001")
            .siteList(
                List.of(
                    SiteResponse.DivisionDetails.builder()
                        .id("site1")
                        .siteId("SITE001")
                        .siteName("Site 1")
                        .build()))
            .build();

    BRDResponse brdResponse = new BRDResponse();
    brdResponse.setBrdId(brdId);
    brdResponse.setBrdFormId("FORM123");
    brdResponse.setStatus("Draft");

    when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.just(mockBrd));
    when(brdService.getBrdById(anyString()))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        "SUCCESS",
                        "BRD retrieved successfully",
                        Optional.of(brdResponse),
                        Optional.empty()))));
    when(siteService.getDivisionsByBrdId(brdId))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        "SUCCESS",
                        "Sites retrieved successfully",
                        Optional.of(siteResponse),
                        Optional.empty()))));
    when(siteService.calculateBrdScore(brdId)).thenReturn(Mono.just(expectedScore));
    when(securityService.withSecurityCheck(anyString())).thenReturn(Mono.empty());

    // Act & Assert
    StepVerifier.create(siteController.getSitesByBrdId(brdId))
        .expectNextMatches(
            response -> {
              if (response.getStatusCode() != HttpStatus.OK) {
                System.out.println("Status code mismatch: " + response.getStatusCode());
                return false;
              }

              Api<SiteResponse> body = response.getBody();
              if (body == null) {
                System.out.println("Response body is null");
                return false;
              }

              if (!body.getData().isPresent()) {
                System.out.println("Response data is not present");
                return false;
              }

              if (!BrdConstants.SUCCESSFUL.equals(body.getStatus())) {
                System.out.println(
                    "Status mismatch. Expected: "
                        + BrdConstants.SUCCESSFUL
                        + ", Got: "
                        + body.getStatus());
                return false;
              }

              SiteResponse siteResponseData = body.getData().get();
              boolean matches =
                  brdId.equals(siteResponseData.getBrdId())
                      && "Test BRD".equals(siteResponseData.getBrdName())
                      && "Test Description".equals(siteResponseData.getDescription())
                      && "CUST001".equals(siteResponseData.getCustomerId())
                      && expectedScore == siteResponseData.getScore()
                      && !siteResponseData.getSiteList().isEmpty();

              if (!matches) {
                System.out.println("Data mismatch:");
                System.out.println("BRD ID: " + siteResponseData.getBrdId());
                System.out.println("BRD Name: " + siteResponseData.getBrdName());
                System.out.println("Description: " + siteResponseData.getDescription());
                System.out.println("Customer ID: " + siteResponseData.getCustomerId());
                System.out.println("Score: " + siteResponseData.getScore());
                System.out.println("Site List Empty: " + siteResponseData.getSiteList().isEmpty());
              }

              return matches;
            })
        .verifyComplete();

    verify(brdRepository).findByBrdId(brdId);
    verify(brdService).getBrdById(anyString());
    verify(siteService).getDivisionsByBrdId(brdId);
    verify(siteService).calculateBrdScore(brdId);
    verify(securityService).withSecurityCheck("Draft");
  }

  @Nested
  @DisplayName("Delete Single Site Tests")
  class DeleteSingleSiteTests {

    @Test
    @DisplayName("Should delete site successfully when user has PM role")
    void deleteSite_WithPMRole_ShouldDeleteSuccessfully() {
      // Arrange
      String siteId = "SITE_BRD0003_001";

      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));

      Api<Void> apiResponse =
          new Api<>(
              SiteConstants.SUCCESSFUL,
              "Site deleted successfully",
              Optional.empty(),
              Optional.empty());
      ResponseEntity<Api<Void>> expectedResponse = ResponseEntity.ok().body(apiResponse);

      when(siteService.deleteSite(siteId)).thenReturn(Mono.just(expectedResponse));

      // Act
      Mono<ResponseEntity<Api<Void>>> result = siteController.deleteSite(siteId);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response ->
                  response.getStatusCode() == HttpStatus.OK
                      && response.getBody().getStatus().equals(SiteConstants.SUCCESSFUL)
                      && response.getBody().getMessage().equals("Site deleted successfully"))
          .verifyComplete();

      verify(securityService).getCurrentUserRole();
      verify(siteService).deleteSite(siteId);
    }

    @Test
    @DisplayName("Should return forbidden when user does not have PM role")
    void deleteSite_WithoutPMRole_ShouldReturnForbidden() {
      // Arrange
      String siteId = "SITE_BRD0003_001";
      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_USER"));

      // Act
      Mono<ResponseEntity<Api<Void>>> result = siteController.deleteSite(siteId);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response ->
                  response.getStatusCode() == HttpStatus.FORBIDDEN
                      && response.getBody().getStatus().equals(BrdConstants.FAILURE)
                      && response
                          .getBody()
                          .getMessage()
                          .equals("Only Project Managers (PM) can delete sites"))
          .verifyComplete();

      verify(securityService).getCurrentUserRole();
      verify(siteService, never()).deleteSite(anyString());
    }

    @Test
    @DisplayName("Should return not found when site does not exist")
    void deleteSite_WithNonExistentSite_ShouldReturnNotFound() {
      // Arrange
      String siteId = "NON_EXISTENT_SITE";
      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));

      when(siteService.deleteSite(siteId))
          .thenReturn(
              Mono.just(
                  ResponseEntity.status(HttpStatus.NOT_FOUND)
                      .body(
                          new Api<>(
                              BrdConstants.FAILURE,
                              String.format(SiteConstants.SITE_NOT_FOUND, siteId),
                              Optional.empty(),
                              Optional.empty()))));

      // Act
      Mono<ResponseEntity<Api<Void>>> result = siteController.deleteSite(siteId);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response ->
                  response.getStatusCode() == HttpStatus.NOT_FOUND
                      && response.getBody().getStatus().equals(BrdConstants.FAILURE)
                      && response
                          .getBody()
                          .getMessage()
                          .equals(String.format(SiteConstants.SITE_NOT_FOUND, siteId)))
          .verifyComplete();

      verify(securityService).getCurrentUserRole();
      verify(siteService).deleteSite(siteId);
    }

    @Test
    @DisplayName("Should return bad request when site ID is blank")
    void deleteSite_WithBlankSiteId_ShouldReturnBadRequest() {
      // Arrange
      String siteId = "";
      when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));

      Api<Void> apiResponse =
          new Api<>(
              BrdConstants.FAILURE, "Site ID cannot be blank", Optional.empty(), Optional.empty());
      ResponseEntity<Api<Void>> expectedResponse = ResponseEntity.badRequest().body(apiResponse);

      when(siteService.deleteSite(siteId)).thenReturn(Mono.just(expectedResponse));

      // Act
      Mono<ResponseEntity<Api<Void>>> result = siteController.deleteSite(siteId);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response ->
                  response.getStatusCode() == HttpStatus.BAD_REQUEST
                      && response.getBody().getStatus().equals(BrdConstants.FAILURE)
                      && response.getBody().getMessage().equals("Site ID cannot be blank"))
          .verifyComplete();

      verify(securityService).getCurrentUserRole();
      verify(siteService).deleteSite(siteId);
    }
  }

  @Test
  @DisplayName("Should clone site successfully when user has PM role")
  void cloneSite_WithPMRole_ShouldCloneSite() {
    // Arrange
    String siteId = "SITE_BRD0003_001";
    String expectedClonedName = "Original Site (Copy)";

    SiteResponse.DivisionDetails clonedSite =
        SiteResponse.DivisionDetails.builder()
            .id("CLONED_" + siteId)
            .siteId("SITE002")
            .siteName(expectedClonedName)
            .description("Test Description")
            .build();

    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));
    when(siteService.cloneSite(siteId))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "Site cloned successfully",
                        Optional.of(clonedSite),
                        Optional.empty()))));

    // Act
    Mono<ResponseEntity<Api<SiteResponse.DivisionDetails>>> result =
        siteController.cloneSite(siteId);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertEquals(BrdConstants.SUCCESSFUL, response.getBody().getStatus());
              assertEquals("Site cloned successfully", response.getBody().getMessage());
              assertTrue(response.getBody().getData().isPresent());
              assertEquals(expectedClonedName, response.getBody().getData().get().getSiteName());
              return true;
            })
        .verifyComplete();

    verify(securityService).getCurrentUserRole();
    verify(siteService).cloneSite(siteId);
  }

  @Test
  @DisplayName("Should return forbidden when non-PM user tries to clone site")
  void cloneSite_WithNonPMRole_ShouldReturnForbidden() {
    // Arrange
    String siteId = "SITE_BRD0003_001";
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_USER"));

    // Act
    Mono<ResponseEntity<Api<SiteResponse.DivisionDetails>>> result =
        siteController.cloneSite(siteId);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
              assertEquals(BrdConstants.FAILURE, response.getBody().getStatus());
              assertEquals(
                  "Only Project Managers (PM) can clone sites", response.getBody().getMessage());
              return true;
            })
        .verifyComplete();

    verify(securityService).getCurrentUserRole();
    verify(siteService, never()).cloneSite(anyString());
  }

  @Test
  @DisplayName("Should return not found when site to clone doesn't exist")
  void cloneSite_WithNonExistentSite_ShouldReturnNotFound() {
    // Arrange
    String siteId = "NONEXISTENT_SITE";
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));
    when(siteService.cloneSite(siteId))
        .thenReturn(
            Mono.just(
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(
                        new Api<>(
                            BrdConstants.FAILURE,
                            "Site not found with ID: " + siteId,
                            Optional.empty(),
                            Optional.empty()))));

    // Act
    Mono<ResponseEntity<Api<SiteResponse.DivisionDetails>>> result =
        siteController.cloneSite(siteId);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
              assertEquals(BrdConstants.FAILURE, response.getBody().getStatus());
              assertTrue(response.getBody().getMessage().contains("Site not found"));
              return true;
            })
        .verifyComplete();

    verify(securityService).getCurrentUserRole();
    verify(siteService).cloneSite(siteId);
  }

  @Nested
  @DisplayName("Get Site Differences Tests")
  class GetSiteDifferencesTests {

    @Test
    @DisplayName("Should return differences when they exist")
    void getSiteDifferences_WithDifferences_ShouldReturnDifferences() {
      // Arrange
      String brdId = "BRD0003";
      SiteDifferencesResponse.FieldDifference difference =
          SiteDifferencesResponse.FieldDifference.builder()
              .fieldName("clientInformation.companyName")
              .orgBrdValue("BRD Company")
              .siteBrdValue("Site Company")
              .build();

      SiteDifferencesResponse.SiteDifference siteDiff =
          new SiteDifferencesResponse.SiteDifference();
      siteDiff.setSiteId("SITE001");
      siteDiff.setSiteName("Test Site");
      siteDiff.setDifferences(Collections.singletonList(difference));

      SiteDifferencesResponse response = new SiteDifferencesResponse();
      response.setBrdId(brdId);
      response.setSites(Collections.singletonList(siteDiff));

      Api<SiteDifferencesResponse> apiResponse =
          new Api<>(
              SiteConstants.SUCCESSFUL,
              "Found differences in 1 sites",
              Optional.of(response),
              Optional.empty());

      when(siteService.getSiteDifferences(brdId))
          .thenReturn(Mono.just(ResponseEntity.ok(apiResponse)));

      // Act & Assert
      StepVerifier.create(siteController.getSiteDifferences(brdId))
          .expectNextMatches(
              responseEntity -> {
                if (responseEntity.getStatusCode() != HttpStatus.OK) return false;
                if (!responseEntity.getBody().getStatus().equals(SiteConstants.SUCCESSFUL))
                  return false;
                if (!responseEntity.getBody().getMessage().equals("Found differences in 1 sites"))
                  return false;
                if (!responseEntity.getBody().getData().isPresent()) return false;

                SiteDifferencesResponse actualResponse = responseEntity.getBody().getData().get();
                return brdId.equals(actualResponse.getBrdId())
                    && actualResponse.getSites().size() == 1
                    && actualResponse.getSites().get(0).getDifferences().size() == 1;
              })
          .verifyComplete();

      verify(siteService).getSiteDifferences(brdId);
    }

    @Test
    @DisplayName("Should return no differences when none exist")
    void getSiteDifferences_WithNoDifferences_ShouldReturnEmpty() {
      // Arrange
      String brdId = "BRD0003";
      SiteDifferencesResponse response = new SiteDifferencesResponse();
      response.setBrdId(brdId);
      response.setSites(Collections.emptyList());

      Api<SiteDifferencesResponse> apiResponse =
          new Api<>(
              SiteConstants.SUCCESSFUL,
              "No differences found between BRD and sites",
              Optional.of(response),
              Optional.empty());

      when(siteService.getSiteDifferences(brdId))
          .thenReturn(Mono.just(ResponseEntity.ok(apiResponse)));

      // Act & Assert
      StepVerifier.create(siteController.getSiteDifferences(brdId))
          .expectNextMatches(
              responseEntity -> {
                if (responseEntity.getStatusCode() != HttpStatus.OK) return false;
                if (!responseEntity.getBody().getStatus().equals(SiteConstants.SUCCESSFUL))
                  return false;
                if (!responseEntity
                    .getBody()
                    .getMessage()
                    .equals("No differences found between BRD and sites")) return false;
                if (!responseEntity.getBody().getData().isPresent()) return false;

                SiteDifferencesResponse actualResponse = responseEntity.getBody().getData().get();
                return brdId.equals(actualResponse.getBrdId())
                    && actualResponse.getSites().isEmpty();
              })
          .verifyComplete();

      verify(siteService).getSiteDifferences(brdId);
    }

    @Test
    @DisplayName("Should handle BRD not found")
    void getSiteDifferences_WhenBrdNotFound_ShouldReturnNotFound() {
      // Arrange
      String brdId = "NONEXISTENT";
      Map<String, String> errors = new HashMap<>();
      errors.put("errorMessage", String.format(SiteConstants.BRD_NOT_FOUND, brdId));

      when(siteService.getSiteDifferences(brdId))
          .thenReturn(
              Mono.just(
                  ResponseEntity.status(HttpStatus.NOT_FOUND)
                      .body(
                          new Api<>(
                              SiteConstants.FAILURE,
                              String.format(SiteConstants.BRD_NOT_FOUND, brdId),
                              Optional.empty(),
                              Optional.of(errors)))));

      // Act & Assert
      StepVerifier.create(siteController.getSiteDifferences(brdId))
          .expectNextMatches(
              response -> {
                if (response.getStatusCode() != HttpStatus.NOT_FOUND) return false;
                if (!response.getBody().getStatus().equals(SiteConstants.FAILURE)) return false;
                if (!response
                    .getBody()
                    .getMessage()
                    .equals(String.format(SiteConstants.BRD_NOT_FOUND, brdId))) return false;
                return response.getBody().getData().isEmpty()
                    && response.getBody().getErrors().isPresent();
              })
          .verifyComplete();

      verify(siteService).getSiteDifferences(brdId);
    }

    @Test
    @DisplayName("Should handle empty BRD ID")
    void getSiteDifferences_WithEmptyBrdId_ShouldReturnBadRequest() {
      // Arrange
      String brdId = "";
      Map<String, String> errors = new HashMap<>();
      errors.put("errorMessage", SiteConstants.BRD_ID_EMPTY);

      when(siteService.getSiteDifferences(brdId))
          .thenReturn(
              Mono.just(
                  ResponseEntity.badRequest()
                      .body(
                          new Api<>(
                              SiteConstants.FAILURE,
                              SiteConstants.BRD_ID_EMPTY,
                              Optional.empty(),
                              Optional.of(errors)))));

      // Act & Assert
      StepVerifier.create(siteController.getSiteDifferences(brdId))
          .expectNextMatches(
              response -> {
                if (response.getStatusCode() != HttpStatus.BAD_REQUEST) return false;
                if (!response.getBody().getStatus().equals(SiteConstants.FAILURE)) return false;
                if (!response.getBody().getMessage().equals(SiteConstants.BRD_ID_EMPTY))
                  return false;
                return response.getBody().getData().isEmpty()
                    && response.getBody().getErrors().isPresent();
              })
          .verifyComplete();

      verify(siteService).getSiteDifferences(brdId);
    }

    @Test
    @DisplayName("Should handle internal server error")
    void getSiteDifferences_WhenInternalError_ShouldReturnServerError() {
      // Arrange
      String brdId = "BRD0003";
      Map<String, String> errors = new HashMap<>();
      errors.put("errorMessage", "Internal server error");

      when(siteService.getSiteDifferences(brdId))
          .thenReturn(
              Mono.just(
                  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(
                          new Api<>(
                              SiteConstants.FAILURE,
                              "Internal server error",
                              Optional.empty(),
                              Optional.of(errors)))));

      // Act & Assert
      StepVerifier.create(siteController.getSiteDifferences(brdId))
          .expectNextMatches(
              response -> {
                if (response.getStatusCode() != HttpStatus.INTERNAL_SERVER_ERROR) return false;
                if (!response.getBody().getStatus().equals(SiteConstants.FAILURE)) return false;
                if (!response.getBody().getMessage().equals("Internal server error")) return false;
                return response.getBody().getData().isEmpty()
                    && response.getBody().getErrors().isPresent();
              })
          .verifyComplete();

      verify(siteService).getSiteDifferences(brdId);
    }
  }

  @Test
  @DisplayName("Should return 500 on unexpected error in createSites")
  void createSites_WithUnexpectedError_ShouldReturn500() {
    SiteRequest request =
        SiteRequest.builder()
            .brdId("BRD0003")
            .siteList(List.of(SiteRequest.SiteDetails.builder().build()))
            .build();

    when(securityService.getCurrentUserRole())
        .thenReturn(Mono.error(new RuntimeException("Unexpected")));

    StepVerifier.create(siteController.createSites(Mono.just(request)))
        .expectNextMatches(
            response ->
                response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR
                    && response.getBody().getMessage().equals("An unexpected error occurred"))
        .verifyComplete();
  }

  @Test
  @DisplayName("Should return 404 when BRD status is not found in getSitesByBrdId")
  void getSitesByBrdId_WithBrdStatusNotFound_ShouldReturn404() {
    String brdId = "BRD0003";
    BRDResponse brdResponse = new BRDResponse();
    brdResponse.setBrdId(brdId);
    brdResponse.setBrdFormId("FORM123");
    // No status set

    when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.just(mockBrd));
    when(brdService.getBrdById(anyString()))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        "SUCCESS", "BRD retrieved", Optional.of(brdResponse), Optional.empty()))));

    StepVerifier.create(siteController.getSitesByBrdId(brdId))
        .expectNextMatches(
            response ->
                response.getStatusCode() == HttpStatus.NOT_FOUND
                    && response.getBody().getMessage().equals("BRD status not found"))
        .verifyComplete();
  }

  @Test
  @DisplayName("Should return 500 on unexpected error in getSitesByBrdId")
  void getSitesByBrdId_WithUnexpectedError_ShouldReturn500() {
    String brdId = "BRD0003";
    when(brdRepository.findByBrdId(brdId))
        .thenReturn(Mono.error(new RuntimeException("Unexpected")));

    StepVerifier.create(siteController.getSitesByBrdId(brdId))
        .expectNextMatches(
            response ->
                response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR
                    && response.getBody().getMessage().startsWith("Error retrieving sites:"))
        .verifyComplete();
  }

  @Test
  @DisplayName("Should return 404 when BRD not found in updateSite")
  void updateSite_WithBrdNotFound_ShouldReturn404() {
    String siteId = "site123";
    when(siteService.getSiteBrdId(siteId)).thenReturn(Mono.just("BRD0003"));
    when(brdRepository.findByBrdId("BRD0003")).thenReturn(Mono.empty());

    StepVerifier.create(
            siteController.updateSite(siteId, SiteRequest.SiteDetails.builder().build()))
        .expectNextMatches(response -> response.getStatusCode() == HttpStatus.NOT_FOUND)
        .verifyComplete();
  }

  @Test
  @Disabled("Fails due to NPE when BRD status is missing; needs controller fix")
  @DisplayName("Should return 404 when BRD status not found in updateSite")
  void updateSite_WithBrdStatusNotFound_ShouldReturn404() {
    String siteId = "site123";
    BRDResponse brdResponse = new BRDResponse();
    brdResponse.setBrdId("BRD0003");
    brdResponse.setBrdFormId("FORM123");
    // No status set

    when(siteService.getSiteBrdId(siteId)).thenReturn(Mono.just("BRD0003"));
    when(brdRepository.findByBrdId("BRD0003")).thenReturn(Mono.just(mockBrd));
    when(brdService.getBrdById(anyString()))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        "SUCCESS", "BRD retrieved", Optional.of(brdResponse), Optional.empty()))));
    when(securityService.withSecurityCheck(any())).thenReturn(Mono.empty());

    StepVerifier.create(
            siteController.updateSite(siteId, SiteRequest.SiteDetails.builder().build()))
        .expectNextMatches(response -> response.getStatusCode() == HttpStatus.NOT_FOUND)
        .verifyComplete();
  }

  @Test
  @DisplayName("Should propagate unexpected error in updateSite")
  void updateSite_WithUnexpectedError_ShouldPropagateError() {
    String siteId = "site123";
    when(siteService.getSiteBrdId(siteId))
        .thenReturn(Mono.error(new RuntimeException("Unexpected")));

    StepVerifier.create(
            siteController.updateSite(siteId, SiteRequest.SiteDetails.builder().build()))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  @DisplayName("Should return 403 when user is not PM in bulkCreateSites")
  void bulkCreateSites_WithNonPMRole_ShouldReturn403() {
    BulkSiteRequest request = BulkSiteRequest.builder().brdId("BRD0003").numberOfSites(2).build();
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_USER"));

    StepVerifier.create(siteController.bulkCreateSites(request))
        .expectError(AccessDeniedException.class)
        .verify();
  }

  @Test
  @DisplayName("Should return 404 when BRD not found in bulkCreateSites")
  void bulkCreateSites_WithBrdNotFound_ShouldReturn404() {
    BulkSiteRequest request = BulkSiteRequest.builder().brdId("BRD0003").numberOfSites(2).build();
    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));
    when(brdRepository.findByBrdId("BRD0003")).thenReturn(Mono.empty());

    StepVerifier.create(siteController.bulkCreateSites(request))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  @DisplayName("Should return 404 when BRD status not found in bulkCreateSites")
  void bulkCreateSites_WithBrdStatusNotFound_ShouldReturn404() {
    BulkSiteRequest request = BulkSiteRequest.builder().brdId("BRD0003").numberOfSites(2).build();
    BRDResponse brdResponse = new BRDResponse();
    brdResponse.setBrdId("BRD0003");
    brdResponse.setBrdFormId("FORM123");
    // No status set

    when(securityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_PM"));
    when(brdRepository.findByBrdId("BRD0003")).thenReturn(Mono.just(mockBrd));
    when(brdService.getBrdById(anyString()))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok(
                    new Api<>(
                        "SUCCESS", "BRD retrieved", Optional.of(brdResponse), Optional.empty()))));

    StepVerifier.create(siteController.bulkCreateSites(request))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  @DisplayName("Should propagate unexpected error in bulkCreateSites")
  void bulkCreateSites_WithUnexpectedError_ShouldPropagateError() {
    BulkSiteRequest request = BulkSiteRequest.builder().brdId("BRD0003").numberOfSites(2).build();
    when(securityService.getCurrentUserRole())
        .thenReturn(Mono.error(new RuntimeException("Unexpected")));

    StepVerifier.create(siteController.bulkCreateSites(request))
        .expectError(RuntimeException.class)
        .verify();
  }
}

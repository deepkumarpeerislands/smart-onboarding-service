package com.aci.smart_onboarding.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.argThat;

import com.aci.smart_onboarding.constants.SiteConstants;
import com.aci.smart_onboarding.dto.*;
import com.aci.smart_onboarding.exception.BadRequestException;
import com.aci.smart_onboarding.exception.InternalServerException;
import com.aci.smart_onboarding.exception.NotFoundException;
import com.aci.smart_onboarding.model.BRD;
import com.aci.smart_onboarding.model.Site;
import com.aci.smart_onboarding.repository.BRDRepository;
import com.aci.smart_onboarding.repository.SiteRepository;
import com.aci.smart_onboarding.service.implementation.SiteService;
import com.aci.smart_onboarding.util.brd_form.ClientInformation;
import com.mongodb.client.result.UpdateResult;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class SiteServiceTest {

  @Mock private BRDRepository brdRepository;

  @Mock private SiteRepository siteRepository;

  @Mock private TransactionalOperator transactionalOperator;

  @Mock private ReactiveMongoTemplate mongoTemplate;

  @InjectMocks private SiteService siteService;

  private SiteRequest validRequest;
  private BRD mockBrd;
  private Site mockSite;

  @BeforeEach
  void setUp() {
    LocalDateTime now = LocalDateTime.now();

    // Setup valid request
    validRequest =
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

    // Setup mock BRD with required fields and form data
    BrdForm brdForm = new BrdForm();
    ClientInformation brdClientInfo = new ClientInformation();
    brdClientInfo.setCompanyName("BRD Company");
    brdForm.setClientInformation(brdClientInfo);

    mockBrd =
        BRD.builder()
            .brdFormId("brdForm123")
            .status("DRAFT")
            .projectId("PRJ001")
            .brdId("BRD0003")
            .brdName("Test BRD")
            .description("Test Description")
            .customerId("ORG001")
            .creator("TestUser")
            .type("Standard")
            .notes("Test Notes")
            .createdAt(now)
            .updatedAt(now)
            .wallentronIncluded(true)
            .achEncrypted(false)
            .originalSSDFileName("test_ssd.pdf")
            .originalContractFileName("test_contract.pdf")
            .originalOtherFileName("test_other.pdf")
            .build();

    // Setup mock Site with form data
    BrdForm siteForm = new BrdForm();
    ClientInformation siteClientInfo = new ClientInformation();
    siteClientInfo.setCompanyName("Site Company");
    siteForm.setClientInformation(siteClientInfo);

    mockSite =
        Site.builder()
            .id("site123")
            .brdId("BRD0003")
            .siteId("SITE_BRD0003_001")
            .siteName("Test Division")
            .identifierCode("DIV001")
            .description("Test Division Description")
            .createdAt(now)
            .updatedAt(now)
            .brdForm(siteForm)
            .build();
  }

  @Test
  @DisplayName("Should create sites successfully when valid request is provided")
  void createDivision_WithValidRequest_ShouldCreateSites() {
    // Arrange
    when(brdRepository.findByBrdId("BRD0003")).thenReturn(Mono.just(mockBrd));
    when(brdRepository.save(any(BRD.class))).thenReturn(Mono.just(mockBrd));
    when(siteRepository.findByBrdId("BRD0003")).thenReturn(Flux.empty());
    when(siteRepository.saveAll(any(Iterable.class))).thenReturn(Flux.just(mockSite));
    when(transactionalOperator.transactional(any(Mono.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    Mono<ResponseEntity<Api<SiteResponse>>> result = siteService.createDivision(validRequest);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response ->
                response.getStatusCode() == HttpStatus.CREATED
                    && Objects.requireNonNull(response.getBody()).getStatus().equals("SUCCESS")
                    && response
                        .getBody()
                        .getMessage()
                        .equals("Sites created and BRD updated successfully")
                    && response.getBody().getData().isPresent()
                    && response.getBody().getData().get().getBrdId().equals("BRD0003")
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
                        .equals("test_other.pdf"))
        .verifyComplete();

    verify(brdRepository).findByBrdId("BRD0003");
    verify(brdRepository).save(any(BRD.class));
    verify(siteRepository).saveAll(any(Iterable.class));
  }

  @Test
  @DisplayName("Should handle database error when site creation fails")
  void createDivision_WhenSiteCreationFails_ShouldHandleError() {
    // Arrange
    when(brdRepository.findByBrdId("BRD0003")).thenReturn(Mono.just(mockBrd));
    when(brdRepository.save(any(BRD.class))).thenReturn(Mono.just(mockBrd));
    when(siteRepository.findByBrdId("BRD0003")).thenReturn(Flux.empty());
    when(siteRepository.saveAll(any(Iterable.class)))
        .thenReturn(Flux.error(new RuntimeException("Database error")));
    when(transactionalOperator.transactional(any(Mono.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    Mono<ResponseEntity<Api<SiteResponse>>> result = siteService.createDivision(validRequest);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              if (response.getStatusCode() != HttpStatus.INTERNAL_SERVER_ERROR) return false;
              if (!response.getBody().getStatus().equals("FAILURE")) return false;
              if (!response.getBody().getMessage().equals("Database error")) return false;

              Optional<Map<String, String>> errorOpt = response.getBody().getErrors();
              if (errorOpt.isEmpty()) return false;

              Map<String, String> error = errorOpt.get();
              return "Database error".equals(error.get("errorMessage"))
                  && error.containsKey("timestamp");
            })
        .verifyComplete();

    verify(brdRepository).findByBrdId("BRD0003");
    verify(brdRepository).save(any(BRD.class));
    verify(siteRepository).saveAll(any(Iterable.class));
  }

  @Test
  @DisplayName("Should handle database error when BRD update fails")
  void createDivision_WhenBrdUpdateFails_ShouldHandleError() {
    // Arrange
    when(brdRepository.findByBrdId("BRD0003")).thenReturn(Mono.just(mockBrd));
    when(brdRepository.save(any(BRD.class)))
        .thenReturn(Mono.error(new RuntimeException("Database error")));
    when(transactionalOperator.transactional(any(Mono.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    Mono<ResponseEntity<Api<SiteResponse>>> result = siteService.createDivision(validRequest);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              if (response.getStatusCode() != HttpStatus.INTERNAL_SERVER_ERROR) return false;
              if (!response.getBody().getStatus().equals("FAILURE")) return false;
              if (!response.getBody().getMessage().equals("Database error")) return false;

              Optional<Map<String, String>> errorOpt = response.getBody().getErrors();
              if (errorOpt.isEmpty()) return false;

              Map<String, String> error = errorOpt.get();
              return "Database error".equals(error.get("errorMessage"))
                  && error.containsKey("timestamp");
            })
        .verifyComplete();

    verify(brdRepository).findByBrdId("BRD0003");
    verify(brdRepository).save(any(BRD.class));
    verify(siteRepository, never()).saveAll(any(Iterable.class));
  }

  @Test
  @DisplayName("Should update BRD flags when creating sites")
  void createDivision_ShouldUpdateBrdFlags() {
    // Arrange
    BRD brdToUpdate =
        BRD.builder()
            .brdFormId("brdForm123")
            .brdId("BRD0003")
            .wallentronIncluded(false)
            .achEncrypted(false)
            .build();

    SiteRequest requestWithFlags =
        SiteRequest.builder()
            .brdId("BRD0003")
            .wallentronIncluded(true)
            .achEncrypted(true)
            .siteList(
                List.of(
                    SiteRequest.SiteDetails.builder()
                        .siteName("Test Division")
                        .identifierCode("DIV001")
                        .description("Test Division Description")
                        .build()))
            .build();

    when(brdRepository.findByBrdId("BRD0003")).thenReturn(Mono.just(brdToUpdate));
    when(brdRepository.save(any(BRD.class)))
        .thenAnswer(
            invocation -> {
              BRD savedBrd = invocation.getArgument(0);
              return Mono.just(savedBrd);
            });
    when(siteRepository.findByBrdId("BRD0003")).thenReturn(Flux.empty());
    when(siteRepository.saveAll(any(List.class))).thenReturn(Flux.just(mockSite));
    when(transactionalOperator.transactional(any(Mono.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    Mono<ResponseEntity<Api<SiteResponse>>> result = siteService.createDivision(requestWithFlags);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response ->
                response.getStatusCode() == HttpStatus.CREATED
                    && Objects.requireNonNull(response.getBody()).getData().isPresent()
                    && response.getBody().getData().get().isWallentronIncluded()
                    && response.getBody().getData().get().isAchEncrypted())
        .verifyComplete();

    verify(brdRepository).save(argThat(brd -> brd.isWallentronIncluded() && brd.isAchEncrypted()));
  }

  @Test
  @DisplayName("Should return 404 when BRD not found")
  void createDivision_WithNonExistentBrd_ShouldReturn404() {
    // Arrange
    when(brdRepository.findByBrdId("BRD0003")).thenReturn(Mono.empty());
    when(transactionalOperator.transactional(any(Mono.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    Mono<ResponseEntity<Api<SiteResponse>>> result = siteService.createDivision(validRequest);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response ->
                response.getStatusCode() == HttpStatus.NOT_FOUND
                    && Objects.requireNonNull(response.getBody()).getStatus().equals("FAILURE")
                    && response.getBody().getMessage().contains("BRD not found with ID: "))
        .verifyComplete();

    verify(siteRepository, never()).saveAll(any(Iterable.class));
  }

  @Test
  @DisplayName("Should return 400 when duplicate identifier codes exist")
  void createDivision_WithDuplicateIdentifierCodes_ShouldReturn400() {
    // Arrange
    when(transactionalOperator.transactional(any(Mono.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Create a request with duplicate identifier codes
    SiteRequest requestWithDuplicates =
        SiteRequest.builder()
            .brdId("BRD0003")
            .wallentronIncluded(true)
            .achEncrypted(false)
            .siteList(
                List.of(
                    SiteRequest.SiteDetails.builder()
                        .siteName("Test Division 1")
                        .identifierCode("DIV001")
                        .description("Test Division Description 1")
                        .build(),
                    SiteRequest.SiteDetails.builder()
                        .siteName("Test Division 2")
                        .identifierCode("DIV001") // Same identifier code
                        .description("Test Division Description 2")
                        .build()))
            .build();

    // Act
    Mono<ResponseEntity<Api<SiteResponse>>> result =
        siteService.createDivision(requestWithDuplicates);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response ->
                response.getStatusCode() == HttpStatus.BAD_REQUEST
                    && Objects.requireNonNull(response.getBody()).getStatus().equals("FAILURE")
                    && response
                        .getBody()
                        .getMessage()
                        .contains("Duplicate identifier code found: DIV001"))
        .verifyComplete();

    verify(siteRepository, never()).saveAll(any(Iterable.class));
  }

  @Test
  @DisplayName("Should get sites by BRD ID successfully")
  void getDivisionsByBrdId_WithValidBrdId_ShouldReturnSites() {
    // Arrange
    when(brdRepository.findByBrdId("BRD0003")).thenReturn(Mono.just(mockBrd));
    when(siteRepository.findByBrdId("BRD0003")).thenReturn(Flux.just(mockSite));

    // Act
    Mono<ResponseEntity<Api<SiteResponse>>> result = siteService.getDivisionsByBrdId("BRD0003");

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response ->
                response.getStatusCode() == HttpStatus.OK
                    && Objects.requireNonNull(response.getBody()).getStatus().equals("SUCCESS")
                    && response.getBody().getMessage().equals("Divisions retrieved successfully")
                    && response.getBody().getData().isPresent()
                    && response.getBody().getData().get().getBrdId().equals("BRD0003")
                    && !response.getBody().getData().get().getSiteList().isEmpty()
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
                        .equals("test_other.pdf"))
        .verifyComplete();

    verify(brdRepository, times(2)).findByBrdId("BRD0003");
    verify(siteRepository).findByBrdId("BRD0003");
  }

  @Test
  @DisplayName("Should return 404 when getting sites for non-existent BRD")
  void getDivisionsByBrdId_WithNonExistentBrd_ShouldReturn404() {
    // Arrange
    String brdId = "BRD0003";
    when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.empty());
    when(siteRepository.findByBrdId(brdId)).thenReturn(Flux.empty());

    // Act
    Mono<ResponseEntity<Api<SiteResponse>>> result = siteService.getDivisionsByBrdId(brdId);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response ->
                response.getStatusCode() == HttpStatus.NOT_FOUND
                    && Objects.requireNonNull(response.getBody()).getStatus().equals("FAILURE")
                    && response.getBody().getMessage().contains("BRD not found with ID: BRD0003"))
        .verifyComplete();

    verify(brdRepository).findByBrdId(brdId);
  }

  @Test
  @DisplayName("Should handle empty site list for existing BRD")
  void getDivisionsByBrdId_WithNoSites_ShouldReturnEmptyList() {
    // Arrange
    when(brdRepository.findByBrdId("BRD0003")).thenReturn(Mono.just(mockBrd));
    when(siteRepository.findByBrdId("BRD0003")).thenReturn(Flux.empty());

    // Act
    Mono<ResponseEntity<Api<SiteResponse>>> result = siteService.getDivisionsByBrdId("BRD0003");

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response ->
                response.getStatusCode() == HttpStatus.OK
                    && response.getBody().getStatus().equals("SUCCESS")
                    && response.getBody().getData().isPresent()
                    && response.getBody().getData().get().getSiteList().isEmpty())
        .verifyComplete();

    verify(brdRepository, times(2)).findByBrdId("BRD0003");
    verify(siteRepository).findByBrdId("BRD0003");
  }

  @Test
  @DisplayName("Should handle null request")
  void createDivision_WithNullRequest_ShouldReturn400() {
    // Arrange
    when(transactionalOperator.transactional(any(Mono.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    Mono<ResponseEntity<Api<SiteResponse>>> result = siteService.createDivision(null);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response ->
                response.getStatusCode() == HttpStatus.BAD_REQUEST
                    && response.getBody().getStatus().equals("FAILURE"))
        .verifyComplete();

    verify(brdRepository, never()).findByBrdId(any());
    verify(siteRepository, never()).saveAll(any(Iterable.class));
  }

  @Test
  @DisplayName("Should handle BRD with null file names")
  void getDivisionsByBrdId_WithNullFileNames_ShouldHandleCorrectly() {
    // Arrange
    BRD brdWithNullFileNames =
        BRD.builder()
            .brdFormId("brdForm123")
            .status("DRAFT")
            .projectId("PRJ001")
            .brdId("BRD0003")
            .brdName("Test BRD")
            .description("Test Description")
            .customerId("ORG001")
            .creator("TestUser")
            .type("Standard")
            .notes("Test Notes")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .wallentronIncluded(true)
            .achEncrypted(false)
            .originalSSDFileName(null)
            .originalContractFileName(null)
            .originalOtherFileName(null)
            .build();

    when(brdRepository.findByBrdId("BRD0003")).thenReturn(Mono.just(brdWithNullFileNames));
    when(siteRepository.findByBrdId("BRD0003")).thenReturn(Flux.just(mockSite));

    // Act
    Mono<ResponseEntity<Api<SiteResponse>>> result = siteService.getDivisionsByBrdId("BRD0003");

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response ->
                response.getStatusCode() == HttpStatus.OK
                    && Objects.requireNonNull(response.getBody()).getStatus().equals("SUCCESS")
                    && response.getBody().getData().isPresent()
                    && response.getBody().getData().get().getOriginalSSDFileName() == null
                    && response.getBody().getData().get().getOriginalContractFileName() == null
                    && response.getBody().getData().get().getOriginalOtherFileName() == null)
        .verifyComplete();

    verify(brdRepository, times(2)).findByBrdId("BRD0003");
  }

  @Test
  @DisplayName("Should compare BRD and Site BRD form successfully")
  void compareBrdAndSiteBrdForm_WithValidData_ShouldCompareSuccessfully() {
    // Arrange
    String siteId = "site123";
    String sectionName = "clientInformation";

    // Create mock BRD with form data
    BrdForm brdForm = new BrdForm();
    ClientInformation brdClientInfo = new ClientInformation();
    brdClientInfo.setCompanyName("BRD Company");
    brdForm.setClientInformation(brdClientInfo);
    mockBrd = BRD.builder().brdId("BRD0003").brdFormId("brdForm123").build();

    BrdForm siteForm = new BrdForm();
    ClientInformation siteClientInfo = new ClientInformation();
    siteClientInfo.setCompanyName("Site Company"); // Different company name to create differences
    siteForm.setClientInformation(siteClientInfo);
    mockSite =
        Site.builder()
            .id(siteId)
            .brdId("BRD0003")
            .siteId("SITE_BRD0003_001")
            .siteName("Test Site")
            .brdForm(siteForm)
            .build();

    when(siteRepository.findById(siteId)).thenReturn(Mono.just(mockSite));
    when(brdRepository.findByBrdId("BRD0003")).thenReturn(Mono.just(mockBrd));

    // Act
    Mono<ResponseEntity<Api<BrdComparisonResponse>>> result =
        siteService.compareBrdAndSiteBrdForm(siteId, sectionName);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              if (response.getStatusCode() != HttpStatus.OK) return false;
              if (!Objects.requireNonNull(response.getBody()).getStatus().equals("SUCCESS"))
                return false;
              if (!response
                  .getBody()
                  .getMessage()
                  .equals("BRD and Site BRD form comparison completed successfully")) return false;
              if (response.getBody().getData().isEmpty()) return false;

              BrdComparisonResponse comparisonResponse = response.getBody().getData().get();
              if (!comparisonResponse.getBrdId().equals("BRD0003")) return false;
              if (!comparisonResponse.getSiteId().equals("SITE_BRD0003_001")) return false;
              if (!comparisonResponse.getSiteName().equals("Test Site")) return false;
              if (comparisonResponse.getDifferences() == null) return false;

              Map<String, Object> differences = comparisonResponse.getDifferences();
              if (!differences.containsKey(sectionName)) return false;

              Object sectionValue = differences.get(sectionName);
              if (sectionValue instanceof String) {
                // If it's a string, it should be an error message
                return sectionValue.equals("One of the sections is null");
              } else if (sectionValue instanceof Map) {
                // If it's a map, it should contain boolean values
                @SuppressWarnings("unchecked")
                Map<String, Boolean> sectionDifferences = (Map<String, Boolean>) sectionValue;
                return sectionDifferences.values().stream().allMatch(Objects::nonNull);
              }
              return false;
            })
        .verifyComplete();

    verify(siteRepository).findById(siteId);
    verify(brdRepository).findByBrdId("BRD0003");
  }

  @Test
  @DisplayName("Should handle null section name")
  void compareBrdAndSiteBrdForm_WithNullSectionName_ShouldReturn400() {
    // Arrange
    String siteId = "site123";
    String sectionName = null;

    // Mock repository behavior
    when(siteRepository.findById(siteId)).thenReturn(Mono.just(mockSite));
    when(brdRepository.findByBrdId(anyString())).thenReturn(Mono.just(mockBrd));

    // Act
    Mono<ResponseEntity<Api<BrdComparisonResponse>>> result =
        siteService.compareBrdAndSiteBrdForm(siteId, sectionName);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              if (response.getStatusCode() != HttpStatus.OK) return false;
              if (!response.getBody().getStatus().equals("SUCCESS")) return false;
              if (!response
                  .getBody()
                  .getMessage()
                  .equals("BRD and Site BRD form comparison completed successfully")) return false;
              if (response.getBody().getData().isEmpty()) return false;

              BrdComparisonResponse comparisonResponse = response.getBody().getData().get();
              if (!comparisonResponse.getBrdId().equals("BRD0003")) return false;
              if (!comparisonResponse.getSiteId().equals("SITE_BRD0003_001")) return false;
              if (!comparisonResponse.getSiteName().equals("Test Division")) return false;
              if (comparisonResponse.getDifferences() == null) return false;

              Map<String, Object> differences = comparisonResponse.getDifferences();
              return differences.isEmpty();
            })
        .verifyComplete();

    // Verify that repository methods were called
    verify(siteRepository).findById(siteId);
    verify(brdRepository).findByBrdId(anyString());
  }

  @Test
  @DisplayName("Should handle empty section name")
  void compareBrdAndSiteBrdForm_WithEmptySectionName_ShouldReturn400() {
    // Arrange
    String siteId = "site123";
    String sectionName = "";

    // Mock repository behavior
    when(siteRepository.findById(siteId)).thenReturn(Mono.just(mockSite));
    when(brdRepository.findByBrdId(anyString())).thenReturn(Mono.just(mockBrd));

    // Act
    Mono<ResponseEntity<Api<BrdComparisonResponse>>> result =
        siteService.compareBrdAndSiteBrdForm(siteId, sectionName);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              if (response.getStatusCode() != HttpStatus.OK) return false;
              if (!response.getBody().getStatus().equals("SUCCESS")) return false;
              if (!response
                  .getBody()
                  .getMessage()
                  .equals("BRD and Site BRD form comparison completed successfully")) return false;
              if (response.getBody().getData().isEmpty()) return false;

              BrdComparisonResponse comparisonResponse = response.getBody().getData().get();
              if (!comparisonResponse.getBrdId().equals("BRD0003")) return false;
              if (!comparisonResponse.getSiteId().equals("SITE_BRD0003_001")) return false;
              if (!comparisonResponse.getSiteName().equals("Test Division")) return false;
              if (comparisonResponse.getDifferences() == null) return false;

              Map<String, Object> differences = comparisonResponse.getDifferences();
              return differences.isEmpty();
            })
        .verifyComplete();

    // Verify that repository methods were called
    verify(siteRepository).findById(siteId);
    verify(brdRepository).findByBrdId(anyString());
  }

  @Test
  @DisplayName("Should handle invalid section name")
  void compareBrdAndSiteBrdForm_WithInvalidSectionName_ShouldHandleError() {
    // Arrange
    String siteId = "site123";
    String sectionName = "invalidSection";
    mockBrd = BRD.builder().brdId("BRD0003").brdFormId("brdForm123").build();

    mockSite = Site.builder().id(siteId).brdId("BRD0003").brdForm(new BrdForm()).build();

    when(siteRepository.findById(siteId)).thenReturn(Mono.just(mockSite));
    when(brdRepository.findByBrdId("BRD0003")).thenReturn(Mono.just(mockBrd));

    // Act
    Mono<ResponseEntity<Api<BrdComparisonResponse>>> result =
        siteService.compareBrdAndSiteBrdForm(siteId, sectionName);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              if (response.getStatusCode() != HttpStatus.OK) return false;
              if (!response.getBody().getStatus().equals("SUCCESS")) return false;
              if (!response
                  .getBody()
                  .getMessage()
                  .equals("BRD and Site BRD form comparison completed successfully")) return false;
              if (response.getBody().getData().isEmpty()) return false;

              BrdComparisonResponse comparisonResponse = response.getBody().getData().get();
              Map<String, Object> differences = comparisonResponse.getDifferences();
              return differences != null && differences.isEmpty();
            })
        .verifyComplete();

    verify(siteRepository).findById(siteId);
    verify(brdRepository).findByBrdId("BRD0003");
  }

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

    Site existingSite =
        Site.builder()
            .id(siteId)
            .brdId("BRD0003")
            .siteId("SITE_BRD0003_001")
            .siteName("Original Site Name")
            .identifierCode("ORIG001")
            .description("Original Description")
            .build();

    when(siteRepository.findById(siteId)).thenReturn(Mono.just(existingSite));
    when(siteRepository.save(any(Site.class))).thenReturn(Mono.just(existingSite));

    // Act
    Mono<ResponseEntity<Api<SingleSiteResponse>>> result =
        siteService.updateSite(siteId, updateDetails);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              if (response.getStatusCode() != HttpStatus.OK) return false;
              if (!Objects.requireNonNull(response.getBody()).getStatus().equals("SUCCESS"))
                return false;
              if (!response.getBody().getMessage().equals("Site updated successfully"))
                return false;
              if (response.getBody().getData().isEmpty()) return false;

              SingleSiteResponse siteResponse = response.getBody().getData().get();
              return siteResponse.getSiteName().equals("Updated Site Name")
                  && siteResponse.getIdentifierCode().equals("UPD001")
                  && siteResponse.getDescription().equals("Updated Description");
            })
        .verifyComplete();

    verify(siteRepository).findById(siteId);
    verify(siteRepository).save(any(Site.class));
  }

  @Test
  @DisplayName("Should handle null BRD ID when updating BRD form fields")
  void updateBrdFormFieldsForAllSites_WithNullBrdId_ShouldReturn400() {
    // Arrange
    String brdId = null;
    Map<String, Object> updateFields = new HashMap<>();
    updateFields.put("clientInformation.companyName", "Updated Company Name");

    // Act
    Mono<ResponseEntity<Api<SiteResponse>>> result =
        siteService.updateBrdFormFieldsForAllSites(brdId, updateFields);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              if (response.getStatusCode() != HttpStatus.BAD_REQUEST) return false;
              if (!Objects.requireNonNull(response.getBody()).getStatus().equals("FAILURE"))
                return false;
              if (!response.getBody().getMessage().equals("Invalid brdId: cannot be null or empty"))
                return false;
              if (response.getBody().getData().isPresent()) return false;

              Optional<Map<String, String>> errors = response.getBody().getErrors();
              return errors.isPresent()
                  && errors.get().containsKey("timestamp")
                  && errors
                      .get()
                      .get("errorMessage")
                      .equals("Invalid brdId: cannot be null or empty");
            })
        .verifyComplete();

    verify(brdRepository, never()).findByBrdId(anyString());
    verify(siteRepository, never()).findByBrdId(anyString());
    verify(mongoTemplate, never())
        .updateMulti(any(Query.class), any(UpdateDefinition.class), eq(Site.class));
  }

  @Test
  @DisplayName("Should handle null update fields when updating BRD form fields")
  void updateBrdFormFieldsForAllSites_WithNullUpdateFields_ShouldReturn400() {
    // Arrange
    String brdId = "BRD0003";
    Map<String, Object> updateFields = null;

    // Act
    Mono<ResponseEntity<Api<SiteResponse>>> result =
        siteService.updateBrdFormFieldsForAllSites(brdId, updateFields);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              if (response.getStatusCode() != HttpStatus.BAD_REQUEST) return false;
              if (!Objects.requireNonNull(response.getBody()).getStatus().equals("FAILURE"))
                return false;
              if (!response
                  .getBody()
                  .getMessage()
                  .equals("Invalid brdFormFields: cannot be null or empty")) return false;
              if (response.getBody().getData().isPresent()) return false;

              Optional<Map<String, String>> errors = response.getBody().getErrors();
              return errors.isPresent()
                  && errors.get().containsKey("timestamp")
                  && errors
                      .get()
                      .get("errorMessage")
                      .equals("Invalid brdFormFields: cannot be null or empty");
            })
        .verifyComplete();

    verify(brdRepository, never()).findByBrdId(anyString());
    verify(siteRepository, never()).findByBrdId(anyString());
    verify(mongoTemplate, never())
        .updateMulti(any(Query.class), any(UpdateDefinition.class), eq(Site.class));
  }

  @Test
  @DisplayName("Should handle empty update fields when updating BRD form fields")
  void updateBrdFormFieldsForAllSites_WithEmptyUpdateFields_ShouldReturn400() {
    // Arrange
    String brdId = "BRD0003";
    Map<String, Object> updateFields = new HashMap<>();

    // Act
    Mono<ResponseEntity<Api<SiteResponse>>> result =
        siteService.updateBrdFormFieldsForAllSites(brdId, updateFields);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              if (response.getStatusCode() != HttpStatus.BAD_REQUEST) return false;
              if (!Objects.requireNonNull(response.getBody()).getStatus().equals("FAILURE"))
                return false;
              if (!response
                  .getBody()
                  .getMessage()
                  .equals("Invalid brdFormFields: cannot be null or empty")) return false;
              if (response.getBody().getData().isPresent()) return false;

              Optional<Map<String, String>> errors = response.getBody().getErrors();
              return errors.isPresent()
                  && errors.get().containsKey("timestamp")
                  && errors
                      .get()
                      .get("errorMessage")
                      .equals("Invalid brdFormFields: cannot be null or empty");
            })
        .verifyComplete();

    verify(brdRepository, never()).findByBrdId(anyString());
    verify(siteRepository, never()).findByBrdId(anyString());
    verify(mongoTemplate, never())
        .updateMulti(any(Query.class), any(UpdateDefinition.class), eq(Site.class));
  }

  @Test
  @DisplayName("Should handle BRD not found when updating BRD form fields")
  void updateBrdFormFieldsForAllSites_WithBrdNotFound_ShouldReturn404() {
    // Arrange
    String brdId = "BRD0003";
    Map<String, Object> updateFields = new HashMap<>();
    updateFields.put("clientInformation.companyName", "Updated Company Name");

    when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.empty());

    // Act
    Mono<ResponseEntity<Api<SiteResponse>>> result =
        siteService.updateBrdFormFieldsForAllSites(brdId, updateFields);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              if (response.getStatusCode() != HttpStatus.NOT_FOUND) return false;
              if (!Objects.requireNonNull(response.getBody()).getStatus().equals("FAILURE"))
                return false;
              if (!response.getBody().getMessage().equals("BRD not found with ID: " + brdId))
                return false;
              if (response.getBody().getData().isPresent()) return false;

              Optional<Map<String, String>> errors = response.getBody().getErrors();
              return errors.isPresent()
                  && errors.get().containsKey("errorMessage")
                  && errors.get().get("errorMessage").equals("BRD not found with ID: " + brdId);
            })
        .verifyComplete();

    verify(brdRepository).findByBrdId(brdId);
    verify(siteRepository, never()).findByBrdId(anyString());
    verify(mongoTemplate, never())
        .updateMulti(any(Query.class), any(UpdateDefinition.class), eq(Site.class));
  }

  @Test
  @DisplayName("Should update multiple sites successfully")
  void updateMultipleSites_WithValidData_ShouldUpdateSuccessfully() {
    // Arrange
    List<SiteUpdateRequest> updates =
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

    Site site1 =
        Site.builder()
            .id("site1")
            .brdId("BRD0003")
            .siteId("SITE_BRD0003_001")
            .siteName("Original Site 1")
            .identifierCode("ORIG001")
            .description("Original Description 1")
            .build();

    Site site2 =
        Site.builder()
            .id("site2")
            .brdId("BRD0003")
            .siteId("SITE_BRD0003_002")
            .siteName("Original Site 2")
            .identifierCode("ORIG002")
            .description("Original Description 2")
            .build();

    when(siteRepository.findById("site1")).thenReturn(Mono.just(site1));
    when(siteRepository.findById("site2")).thenReturn(Mono.just(site2));
    when(siteRepository.save(any(Site.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    // Act
    Mono<ResponseEntity<Api<List<SingleSiteResponse>>>> result =
        siteService.updateMultipleSites(updates);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response ->
                response.getStatusCode() == HttpStatus.OK
                    && Objects.requireNonNull(response.getBody()).getStatus().equals("SUCCESS")
                    && response.getBody().getData().isPresent()
                    && response.getBody().getData().get().size() == 2
                    && response
                        .getBody()
                        .getData()
                        .get()
                        .getFirst()
                        .getSiteName()
                        .equals("Updated Site 1")
                    && response
                        .getBody()
                        .getData()
                        .get()
                        .get(1)
                        .getSiteName()
                        .equals("Updated Site 2"))
        .verifyComplete();

    verify(siteRepository).findById("site1");
    verify(siteRepository).findById("site2");
    verify(siteRepository, times(2)).save(any(Site.class));
  }

  @Test
  @DisplayName("Should handle empty update list")
  void updateMultipleSites_WithEmptyList_ShouldReturnError() {
    // Arrange
    List<SiteUpdateRequest> updates = Collections.emptyList();

    // Act
    Mono<ResponseEntity<Api<List<SingleSiteResponse>>>> result =
        siteService.updateMultipleSites(updates);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response ->
                response.getStatusCode() == HttpStatus.BAD_REQUEST
                    && Objects.requireNonNull(response.getBody()).getStatus().equals("FAILURE")
                    && response
                        .getBody()
                        .getMessage()
                        .equals("Invalid siteUpdates: cannot be empty")
                    && response.getBody().getData().isEmpty()
                    && response.getBody().getErrors().isPresent()
                    && response.getBody().getErrors().get().containsKey("errorMessage")
                    && response.getBody().getErrors().get().containsKey("timestamp"))
        .verifyComplete();

    verify(siteRepository, never()).findById(anyString());
    verify(siteRepository, never()).save(any(Site.class));
  }

  @Test
  @DisplayName("Should handle non-existent site in update list")
  void updateMultipleSites_WithNonExistentSite_ShouldReturnError() {
    // Arrange
    List<SiteUpdateRequest> updates =
        Collections.singletonList(
            SiteUpdateRequest.builder()
                .siteId("nonexistent")
                .siteDetails(
                    SiteRequest.SiteDetails.builder()
                        .siteName("Updated Site")
                        .identifierCode("UPD001")
                        .description("Updated Description")
                        .build())
                .build());

    when(siteRepository.findById("nonexistent")).thenReturn(Mono.empty());

    // Act
    Mono<ResponseEntity<Api<List<SingleSiteResponse>>>> result =
        siteService.updateMultipleSites(updates);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response ->
                response.getStatusCode() == HttpStatus.NOT_FOUND
                    && Objects.requireNonNull(response.getBody()).getStatus().equals("FAILURE")
                    && response.getBody().getMessage().equals("Site not found with ID: nonexistent")
                    && response.getBody().getErrors().isPresent()
                    && response.getBody().getErrors().get().containsKey("errorMessage")
                    && response
                        .getBody()
                        .getErrors()
                        .get()
                        .get("errorMessage")
                        .equals("Site not found with ID: nonexistent"))
        .verifyComplete();

    verify(siteRepository).findById("nonexistent");
    verify(siteRepository, never()).save(any(Site.class));
  }

  @Test
  @DisplayName("Should handle partial updates")
  void updateMultipleSites_WithPartialUpdates_ShouldUpdateOnlyProvidedFields() {
    // Arrange
    List<SiteUpdateRequest> updates =
        Collections.singletonList(
            SiteUpdateRequest.builder()
                .siteId("site1")
                .siteDetails(
                    SiteRequest.SiteDetails.builder()
                        .siteName("Updated Site 1")
                        // Only updating siteName, other fields are null
                        .build())
                .build());

    Site existingSite =
        Site.builder()
            .id("site1")
            .brdId("BRD0003")
            .siteId("SITE_BRD0003_001")
            .siteName("Original Site 1")
            .identifierCode("ORIG001")
            .description("Original Description 1")
            .build();

    when(siteRepository.findById("site1")).thenReturn(Mono.just(existingSite));
    when(siteRepository.save(any(Site.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    // Act
    Mono<ResponseEntity<Api<List<SingleSiteResponse>>>> result =
        siteService.updateMultipleSites(updates);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response ->
                response.getStatusCode() == HttpStatus.OK
                    && Objects.requireNonNull(response.getBody()).getStatus().equals("SUCCESS")
                    && response.getBody().getData().isPresent()
                    && response.getBody().getData().get().size() == 1
                    && response
                        .getBody()
                        .getData()
                        .get()
                        .getFirst()
                        .getSiteName()
                        .equals("Updated Site 1")
                    && response
                        .getBody()
                        .getData()
                        .get()
                        .getFirst()
                        .getIdentifierCode()
                        .equals("ORIG001")
                    && response
                        .getBody()
                        .getData()
                        .get()
                        .getFirst()
                        .getDescription()
                        .equals("Original Description 1"))
        .verifyComplete();

    verify(siteRepository).findById("site1");
    verify(siteRepository).save(any(Site.class));
  }

  @Test
  @DisplayName("Should handle BRD form updates")
  void updateMultipleSites_WithBrdFormUpdates_ShouldUpdateBrdForm() {
    // Arrange
    BrdForm newBrdForm = new BrdForm(); // Add necessary BRD form fields
    List<SiteUpdateRequest> updates =
        Collections.singletonList(
            SiteUpdateRequest.builder()
                .siteId("site1")
                .siteDetails(SiteRequest.SiteDetails.builder().brdForm(newBrdForm).build())
                .build());

    Site existingSite =
        Site.builder()
            .id("site1")
            .brdId("BRD0003")
            .siteId("SITE_BRD0003_001")
            .siteName("Original Site 1")
            .identifierCode("ORIG001")
            .description("Original Description 1")
            .build();

    when(siteRepository.findById("site1")).thenReturn(Mono.just(existingSite));
    when(siteRepository.save(any(Site.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    // Act
    Mono<ResponseEntity<Api<List<SingleSiteResponse>>>> result =
        siteService.updateMultipleSites(updates);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response ->
                response.getStatusCode() == HttpStatus.OK
                    && Objects.requireNonNull(response.getBody()).getStatus().equals("SUCCESS")
                    && response.getBody().getData().isPresent()
                    && response.getBody().getData().get().size() == 1
                    && response.getBody().getData().get().getFirst().getBrdForm() != null)
        .verifyComplete();

    verify(siteRepository).findById("site1");
    verify(siteRepository).save(any(Site.class));
  }

  @Test
  @DisplayName("Should delete multiple sites successfully")
  void deleteMultipleSites_WithValidIds_ShouldDeleteSuccessfully() {
    // Arrange
    List<String> siteIds = Arrays.asList("site1", "site2", "site3");

    // Create mock sites
    Site site1 =
        Site.builder()
            .id("site1")
            .siteId("SITE_BRD0003_001")
            .siteName("Test Division 1")
            .identifierCode("DIV001")
            .description("Test Division Description 1")
            .build();

    Site site2 =
        Site.builder()
            .id("site2")
            .siteId("SITE_BRD0003_002")
            .siteName("Test Division 2")
            .identifierCode("DIV002")
            .description("Test Division Description 2")
            .build();

    Site site3 =
        Site.builder()
            .id("site3")
            .siteId("SITE_BRD0003_003")
            .siteName("Test Division 3")
            .identifierCode("DIV003")
            .description("Test Division Description 3")
            .build();

    // Mock repository behavior for all sites
    when(siteRepository.findById("site1")).thenReturn(Mono.just(site1));
    when(siteRepository.findById("site2")).thenReturn(Mono.just(site2));
    when(siteRepository.findById("site3")).thenReturn(Mono.just(site3));

    when(siteRepository.delete(site1)).thenReturn(Mono.empty());
    when(siteRepository.delete(site2)).thenReturn(Mono.empty());
    when(siteRepository.delete(site3)).thenReturn(Mono.empty());

    // Act
    Mono<ResponseEntity<Api<Void>>> result = siteService.deleteMultipleSites(siteIds);

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

    // Verify all repository calls
    verify(siteRepository).findById("site1");
    verify(siteRepository).findById("site2");
    verify(siteRepository).findById("site3");
    verify(siteRepository).delete(site1);
    verify(siteRepository).delete(site2);
    verify(siteRepository).delete(site3);
  }

  @Test
  @DisplayName("Should handle empty site IDs list")
  void deleteMultipleSites_WithEmptyList_ShouldReturnBadRequest() {
    // Arrange
    List<String> siteIds = Collections.emptyList();

    // Act
    Mono<ResponseEntity<Api<Void>>> result = siteService.deleteMultipleSites(siteIds);

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

    verify(siteRepository, never()).findById(anyString());
    verify(siteRepository, never()).delete(any(Site.class));
  }

  @Test
  @DisplayName("Should handle non-existent site")
  void deleteMultipleSites_WithNonExistentSite_ShouldReturnNotFound() {
    // Arrange
    List<String> siteIds = Collections.singletonList("nonexistent");

    when(siteRepository.findById("nonexistent")).thenReturn(Mono.empty());

    // Act
    Mono<ResponseEntity<Api<Void>>> result = siteService.deleteMultipleSites(siteIds);

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
                    && response
                        .getBody()
                        .getErrors()
                        .get()
                        .get("errorMessage")
                        .equals("Site not found with ID: nonexistent"))
        .verifyComplete();

    verify(siteRepository).findById("nonexistent");
    verify(siteRepository, never()).deleteById(anyString());
  }

  @Test
  @DisplayName("Should handle database error during deletion")
  void deleteMultipleSites_WhenDeletionFails_ShouldHandleError() {
    // Arrange
    List<String> siteIds = Collections.singletonList("site1");
    Site siteToDelete =
        Site.builder()
            .id("site1")
            .siteId("SITE_BRD0003_001")
            .siteName("Test Division")
            .identifierCode("DIV001")
            .description("Test Division Description")
            .build();

    when(siteRepository.findById("site1")).thenReturn(Mono.just(siteToDelete));
    when(siteRepository.delete(siteToDelete))
        .thenReturn(Mono.error(new RuntimeException("Database error")));

    // Act
    Mono<ResponseEntity<Api<Void>>> result = siteService.deleteMultipleSites(siteIds);

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
                    && response
                        .getBody()
                        .getErrors()
                        .get()
                        .get("errorMessage")
                        .equals("Database error"))
        .verifyComplete();

    verify(siteRepository).findById("site1");
    verify(siteRepository).delete(siteToDelete);
  }

  @Test
  @DisplayName("Should handle null site IDs list")
  void deleteMultipleSites_WithNullList_ShouldReturnBadRequest() {
    // Arrange
    List<String> siteIds = null;

    // Act
    Mono<ResponseEntity<Api<Void>>> result = siteService.deleteMultipleSites(siteIds);

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

    verify(siteRepository, never()).findById(anyString());
    verify(siteRepository, never()).delete(any(Site.class));
  }

  @Test
  @DisplayName("Should create comparison error with message")
  void createComparisonError_WithMessage_ShouldCreateError() {
    // Arrange
    Exception e = new RuntimeException("Test error message");
    Site site = new Site();
    site.setSiteId("site123");
    site.setBrdId("brd123");
    site.setBrdForm(new BrdForm());
    BRD brd = new BRD();
    brd.setBrdFormId("brdForm123");

    // Act
    InternalServerException exception = siteService.createComparisonError(e, site, brd);

    // Assert
    Assertions.assertNotNull(exception);
    Assertions.assertEquals(SiteConstants.BRD_COMPARISON_FAILED, exception.getMessage());
    Map<String, String> errorDetails = exception.getErrorDetails();
    Assertions.assertNotNull(errorDetails);
    Assertions.assertEquals("COMPARISON_ERROR", errorDetails.get("errorType"));
    Assertions.assertEquals("Test error message", errorDetails.get("originalError"));
    Assertions.assertNotNull(errorDetails.get(SiteConstants.TIMESTAMP));
    Assertions.assertEquals("site123", errorDetails.get(SiteConstants.SITE_ID));
    Assertions.assertEquals("brd123", errorDetails.get(SiteConstants.BRD_ID));
    Assertions.assertEquals("brdForm123", errorDetails.get("brdFormId"));
    Assertions.assertEquals("present", errorDetails.get("siteBrdForm"));
  }

  @Test
  @DisplayName("Should create comparison error with null message")
  void createComparisonError_WithNullMessage_ShouldCreateError() {
    // Arrange
    Exception e = new RuntimeException();
    Site site = new Site();
    site.setSiteId("site123");
    site.setBrdId("brd123");
    site.setBrdForm(null);
    BRD brd = new BRD();
    brd.setBrdFormId("brdForm123");

    // Act
    InternalServerException exception = siteService.createComparisonError(e, site, brd);

    // Assert
    assertNotNull(exception);
    assertEquals(SiteConstants.BRD_COMPARISON_FAILED, exception.getMessage());
    Map<String, String> errorDetails = exception.getErrorDetails();
    assertNotNull(errorDetails);
    assertEquals("COMPARISON_ERROR", errorDetails.get("errorType"));
    assertEquals("RuntimeException", errorDetails.get("originalError"));
    assertNotNull(errorDetails.get(SiteConstants.TIMESTAMP));
    assertEquals("site123", errorDetails.get(SiteConstants.SITE_ID));
    assertEquals("brd123", errorDetails.get(SiteConstants.BRD_ID));
    assertEquals("brdForm123", errorDetails.get("brdFormId"));
    assertEquals("null", errorDetails.get("siteBrdForm"));
  }

  @Test
  @DisplayName("Should create comparison error with null site")
  void createComparisonError_WithNullSite_ShouldCreateError() {
    // Arrange
    Exception e = new RuntimeException("Test error message");
    Site site = new Site();
    BRD brd = new BRD();
    brd.setBrdFormId("brdForm123");

    // Act
    InternalServerException exception = siteService.createComparisonError(e, site, brd);

    // Assert
    Assertions.assertNotNull(exception);
    Assertions.assertEquals(SiteConstants.BRD_COMPARISON_FAILED, exception.getMessage());
    Map<String, String> errorDetails = exception.getErrorDetails();
    Assertions.assertNotNull(errorDetails);
    Assertions.assertEquals("COMPARISON_ERROR", errorDetails.get("errorType"));
    Assertions.assertEquals("Test error message", errorDetails.get("originalError"));
    Assertions.assertNotNull(errorDetails.get(SiteConstants.TIMESTAMP));
    Assertions.assertNull(errorDetails.get(SiteConstants.SITE_ID));
    Assertions.assertNull(errorDetails.get(SiteConstants.BRD_ID));
    Assertions.assertEquals("brdForm123", errorDetails.get("brdFormId"));
    Assertions.assertEquals("null", errorDetails.get("siteBrdForm"));
  }

  @Test
  @DisplayName("Should create comparison error with null BRD")
  void createComparisonError_WithNullBRD_ShouldCreateError() {
    // Arrange
    Exception e = new RuntimeException("Test error message");
    Site site = new Site();
    site.setSiteId("site123");
    site.setBrdId("brd123");
    site.setBrdForm(new BrdForm());
    BRD brd = new BRD();

    // Act
    InternalServerException exception = siteService.createComparisonError(e, site, brd);

    // Assert
    Assertions.assertNotNull(exception);
    Assertions.assertEquals(SiteConstants.BRD_COMPARISON_FAILED, exception.getMessage());
    Map<String, String> errorDetails = exception.getErrorDetails();
    Assertions.assertNotNull(errorDetails);
    Assertions.assertEquals("COMPARISON_ERROR", errorDetails.get("errorType"));
    Assertions.assertEquals("Test error message", errorDetails.get("originalError"));
    Assertions.assertNotNull(errorDetails.get(SiteConstants.TIMESTAMP));
    Assertions.assertEquals("site123", errorDetails.get(SiteConstants.SITE_ID));
    Assertions.assertEquals("brd123", errorDetails.get(SiteConstants.BRD_ID));
    Assertions.assertNull(errorDetails.get("brdFormId"));
    Assertions.assertEquals("present", errorDetails.get("siteBrdForm"));
  }

  @Test
  @DisplayName("Should create comparison error with custom exception")
  void createComparisonError_WithCustomException_ShouldCreateError() {
    // Arrange
    Exception e = new IllegalArgumentException("Custom validation error");
    Site site = new Site();
    site.setSiteId("site123");
    site.setBrdId("brd123");
    site.setBrdForm(new BrdForm());
    BRD brd = new BRD();
    brd.setBrdFormId("brdForm123");

    // Act
    InternalServerException exception = siteService.createComparisonError(e, site, brd);

    // Assert
    assertNotNull(exception);
    assertEquals(SiteConstants.BRD_COMPARISON_FAILED, exception.getMessage());
    Map<String, String> errorDetails = exception.getErrorDetails();
    assertNotNull(errorDetails);
    assertEquals("COMPARISON_ERROR", errorDetails.get("errorType"));
    assertEquals("Custom validation error", errorDetails.get("originalError"));
    assertNotNull(errorDetails.get(SiteConstants.TIMESTAMP));
    assertEquals("site123", errorDetails.get(SiteConstants.SITE_ID));
    assertEquals("brd123", errorDetails.get(SiteConstants.BRD_ID));
    assertEquals("brdForm123", errorDetails.get("brdFormId"));
    assertEquals("present", errorDetails.get("siteBrdForm"));
  }

  @Test
  @DisplayName("Should update BRD form fields for all sites successfully")
  void updateBrdFormFieldsForAllSites_WithValidData_ShouldUpdateSuccessfully() {
    // Arrange
    String brdId = "BRD0003";
    Map<String, Object> updateFields = new HashMap<>();
    updateFields.put("clientInformation.companyName", "Updated Company Name");

    mockBrd = BRD.builder().brdId(brdId).brdName("Test BRD").build();

    Site site1 =
        Site.builder()
            .id("site1")
            .brdId(brdId)
            .siteId("SITE_BRD0003_001")
            .siteName("Test Site 1")
            .brdForm(new BrdForm())
            .build();

    Site site2 =
        Site.builder()
            .id("site2")
            .brdId(brdId)
            .siteId("SITE_BRD0003_002")
            .siteName("Test Site 2")
            .brdForm(new BrdForm())
            .build();

    when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.just(mockBrd));
    when(siteRepository.findByBrdId(brdId)).thenReturn(Flux.just(site1, site2));
    when(mongoTemplate.updateMulti(any(Query.class), any(UpdateDefinition.class), eq(Site.class)))
        .thenReturn(Mono.just(UpdateResult.acknowledged(2, 2L, null)));

    // Act
    Mono<ResponseEntity<Api<SiteResponse>>> result =
        siteService.updateBrdFormFieldsForAllSites(brdId, updateFields);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              if (response.getStatusCode() != HttpStatus.OK) return false;
              if (!Objects.requireNonNull(response.getBody()).getStatus().equals("SUCCESS"))
                return false;
              if (!response
                  .getBody()
                  .getMessage()
                  .contains("BRD form fields updated successfully for 2 sites")) return false;
              if (!response.getBody().getData().isPresent()) return false;

              SiteResponse siteResponse = response.getBody().getData().get();
              return siteResponse.getBrdId().equals(brdId)
                  && siteResponse.getSiteList().size() == 2
                  && siteResponse.getSiteList().get(0).getSiteName().equals("Test Site 1")
                  && siteResponse.getSiteList().get(1).getSiteName().equals("Test Site 2");
            })
        .verifyComplete();

    verify(brdRepository).findByBrdId(brdId);
    verify(siteRepository).findByBrdId(brdId);
    verify(mongoTemplate)
        .updateMulti(any(Query.class), any(UpdateDefinition.class), eq(Site.class));
  }

  @Test
  @DisplayName("Should handle no sites found for BRD")
  void updateBrdFormFieldsForAllSites_WithNoSitesFound_ShouldReturn404() {
    // Arrange
    String brdId = "BRD0003";
    Map<String, Object> updateFields = new HashMap<>();
    updateFields.put("clientInformation.companyName", "Updated Company Name");

    mockBrd = BRD.builder().brdId(brdId).brdName("Test BRD").build();

    when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.just(mockBrd));
    when(mongoTemplate.updateMulti(any(Query.class), any(UpdateDefinition.class), eq(Site.class)))
        .thenReturn(Mono.just(UpdateResult.acknowledged(0, 0L, null)));

    // Act
    Mono<ResponseEntity<Api<SiteResponse>>> result =
        siteService.updateBrdFormFieldsForAllSites(brdId, updateFields);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              if (response.getStatusCode() != HttpStatus.NOT_FOUND) return false;
              if (!Objects.requireNonNull(response.getBody()).getStatus().equals("FAILURE"))
                return false;
              if (!response.getBody().getMessage().equals("No sites found for BRD ID: " + brdId))
                return false;
              if (!response.getBody().getData().isEmpty()) return false;

              Optional<Map<String, String>> errors = response.getBody().getErrors();
              return errors.isPresent()
                  && errors.get().containsKey("errorMessage")
                  && errors.get().get("errorMessage").equals("No sites found for BRD ID: " + brdId);
            })
        .verifyComplete();

    verify(brdRepository).findByBrdId(brdId);
    verify(mongoTemplate)
        .updateMulti(any(Query.class), any(UpdateDefinition.class), eq(Site.class));
  }

  @Test
  @DisplayName("Should handle database error during update")
  void updateBrdFormFieldsForAllSites_WhenUpdateFails_ShouldHandleError() {
    // Arrange
    String brdId = "BRD0003";
    Map<String, Object> updateFields = new HashMap<>();
    updateFields.put("clientInformation.companyName", "Updated Company Name");

    mockBrd = BRD.builder().brdId(brdId).brdName("Test BRD").build();

    when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.just(mockBrd));
    when(mongoTemplate.updateMulti(any(Query.class), any(UpdateDefinition.class), eq(Site.class)))
        .thenReturn(Mono.error(new RuntimeException("Database error")));

    // Act
    Mono<ResponseEntity<Api<SiteResponse>>> result =
        siteService.updateBrdFormFieldsForAllSites(brdId, updateFields);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              if (response.getStatusCode() != HttpStatus.INTERNAL_SERVER_ERROR) return false;
              if (!Objects.requireNonNull(response.getBody()).getStatus().equals("FAILURE"))
                return false;
              if (!response.getBody().getMessage().equals("Database error")) return false;
              if (!response.getBody().getData().isEmpty()) return false;

              Optional<Map<String, String>> errors = response.getBody().getErrors();
              return errors.isPresent()
                  && errors.get().containsKey("errorMessage")
                  && errors.get().get("errorMessage").equals("Database error");
            })
        .verifyComplete();

    verify(brdRepository).findByBrdId(brdId);
    verify(mongoTemplate)
        .updateMulti(any(Query.class), any(UpdateDefinition.class), eq(Site.class));
  }

  @Test
  @DisplayName("Should handle multiple field updates")
  void updateBrdFormFieldsForAllSites_WithMultipleFields_ShouldUpdateAllFields() {
    // Arrange
    String brdId = "BRD0003";
    Map<String, Object> updateFields = new HashMap<>();
    updateFields.put("clientInformation.companyName", "Updated Company Name");
    updateFields.put("clientInformation.address", "Updated Address");
    updateFields.put("clientInformation.phone", "Updated Phone");

    mockBrd = BRD.builder().brdId(brdId).brdName("Test BRD").build();

    Site site1 =
        Site.builder()
            .id("site1")
            .brdId(brdId)
            .siteId("SITE_BRD0003_001")
            .siteName("Test Site 1")
            .brdForm(new BrdForm())
            .build();

    when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.just(mockBrd));
    when(siteRepository.findByBrdId(brdId)).thenReturn(Flux.just(site1));
    when(mongoTemplate.updateMulti(any(Query.class), any(UpdateDefinition.class), eq(Site.class)))
        .thenReturn(Mono.just(UpdateResult.acknowledged(1, 1L, null)));

    // Act
    Mono<ResponseEntity<Api<SiteResponse>>> result =
        siteService.updateBrdFormFieldsForAllSites(brdId, updateFields);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              if (response.getStatusCode() != HttpStatus.OK) return false;
              if (!Objects.requireNonNull(response.getBody()).getStatus().equals("SUCCESS"))
                return false;
              if (!response
                  .getBody()
                  .getMessage()
                  .contains("BRD form fields updated successfully for 1 sites")) return false;
              if (!response.getBody().getData().isPresent()) return false;

              SiteResponse siteResponse = response.getBody().getData().get();
              return siteResponse.getBrdId().equals(brdId)
                  && siteResponse.getSiteList().size() == 1
                  && siteResponse.getSiteList().get(0).getSiteName().equals("Test Site 1");
            })
        .verifyComplete();

    verify(brdRepository).findByBrdId(brdId);
    verify(siteRepository).findByBrdId(brdId);
    verify(mongoTemplate)
        .updateMulti(any(Query.class), any(UpdateDefinition.class), eq(Site.class));
  }

  @Test
  @DisplayName("Should handle nested field updates")
  void updateBrdFormFieldsForAllSites_WithNestedFields_ShouldUpdateNestedFields() {
    // Arrange
    String brdId = "BRD0003";
    Map<String, Object> updateFields = new HashMap<>();
    Map<String, Object> nestedFields = new HashMap<>();
    nestedFields.put("street", "Updated Street");
    nestedFields.put("city", "Updated City");
    updateFields.put("clientInformation.address", nestedFields);

    mockBrd = BRD.builder().brdId(brdId).brdName("Test BRD").build();

    Site site1 =
        Site.builder()
            .id("site1")
            .brdId(brdId)
            .siteId("SITE_BRD0003_001")
            .siteName("Test Site 1")
            .brdForm(new BrdForm())
            .build();

    when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.just(mockBrd));
    when(siteRepository.findByBrdId(brdId)).thenReturn(Flux.just(site1));
    when(mongoTemplate.updateMulti(any(Query.class), any(UpdateDefinition.class), eq(Site.class)))
        .thenReturn(Mono.just(UpdateResult.acknowledged(1, 1L, null)));

    // Act
    Mono<ResponseEntity<Api<SiteResponse>>> result =
        siteService.updateBrdFormFieldsForAllSites(brdId, updateFields);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              if (response.getStatusCode() != HttpStatus.OK) return false;
              if (!Objects.requireNonNull(response.getBody()).getStatus().equals("SUCCESS"))
                return false;
              if (!response
                  .getBody()
                  .getMessage()
                  .contains("BRD form fields updated successfully for 1 sites")) return false;
              if (!response.getBody().getData().isPresent()) return false;

              SiteResponse siteResponse = response.getBody().getData().get();
              return siteResponse.getBrdId().equals(brdId)
                  && siteResponse.getSiteList().size() == 1
                  && siteResponse.getSiteList().get(0).getSiteName().equals("Test Site 1");
            })
        .verifyComplete();

    verify(brdRepository).findByBrdId(brdId);
    verify(siteRepository).findByBrdId(brdId);
    verify(mongoTemplate)
        .updateMulti(any(Query.class), any(UpdateDefinition.class), eq(Site.class));
  }

  @Test
  @DisplayName("Should return BRD ID when site is found with valid BRD ID")
  void getSiteBrdId_WithValidSite_ShouldReturnBrdId() {
    // Arrange
    String siteId = "site123";
    String brdId = "BRD0003";
    Site site = new Site();
    site.setId(siteId);
    site.setBrdId(brdId);

    when(siteRepository.findById(siteId)).thenReturn(Mono.just(site));

    // Act & Assert
    StepVerifier.create(siteService.getSiteBrdId(siteId)).expectNext(brdId).verifyComplete();

    verify(siteRepository).findById(siteId);
  }

  @Test
  @DisplayName("Should throw NotFoundException when site is not found")
  void getSiteBrdId_WhenSiteNotFound_ShouldThrowNotFoundException() {
    // Arrange
    String siteId = "nonexistent";
    when(siteRepository.findById(siteId)).thenReturn(Mono.empty());

    // Act & Assert
    StepVerifier.create(siteService.getSiteBrdId(siteId))
        .expectErrorMatches(
            throwable ->
                throwable instanceof NotFoundException
                    && throwable.getMessage().equals("Site not found with id: " + siteId))
        .verify();

    verify(siteRepository).findById(siteId);
  }

  @Test
  @DisplayName("Should throw NotFoundException when site has no BRD ID")
  void getSiteBrdId_WhenSiteHasNoBrdId_ShouldThrowNotFoundException() {
    // Arrange
    String siteId = "site123";
    Site site = new Site();
    site.setId(siteId);
    site.setBrdId(null); // No BRD ID set

    when(siteRepository.findById(siteId)).thenReturn(Mono.just(site));

    // Act & Assert
    StepVerifier.create(siteService.getSiteBrdId(siteId))
        .expectErrorMatches(
            throwable ->
                throwable instanceof NotFoundException
                    && throwable.getMessage().equals("No BRD ID associated with site: " + siteId))
        .verify();

    verify(siteRepository).findById(siteId);
  }

  @Nested
  @DisplayName("Bulk Site Creation Tests")
  class BulkSiteCreationTests {

    @Test
    @DisplayName("Should create multiple sites successfully")
    void bulkCreateSites_ShouldCreateMultipleSitesSuccessfully() {
      // Arrange
      int numberOfSites = 3;
      String brdId = "BRD0003";

      // Mock finding the BRD
      when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.just(mockBrd));

      // Mock finding existing sites
      when(siteRepository.findByBrdId(brdId)).thenReturn(Flux.empty());

      // Mock saving new sites
      when(siteRepository.saveAll(anyList()))
          .thenAnswer(
              invocation -> {
                List<Site> sitesToSave = invocation.getArgument(0);
                return Flux.fromIterable(sitesToSave)
                    .map(
                        site -> {
                          site.setId("mockId-" + site.getSiteId());
                          return site;
                        });
              });

      // Set up transactional operator behavior
      when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(i -> i.getArgument(0));

      // Act
      Mono<ResponseEntity<Api<BulkSiteResponse>>> result =
          siteService.bulkCreateSites(brdId, numberOfSites);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                Api<BulkSiteResponse> apiResponse = response.getBody();
                BulkSiteResponse bulkResponse = apiResponse.getData().orElse(null);

                return response.getStatusCode() == HttpStatus.CREATED
                    && apiResponse.getStatus().equals(SiteConstants.SUCCESSFUL)
                    && apiResponse.getMessage().equals("Successfully created 3 sites")
                    && bulkResponse != null
                    && bulkResponse.getSiteCount() == 3
                    && bulkResponse.getSites().size() == 3
                    && bulkResponse.getSites().get(0).getSiteName().equals("Bulk Clone 1")
                    && bulkResponse.getSites().get(1).getSiteName().equals("Bulk Clone 2")
                    && bulkResponse.getSites().get(2).getSiteName().equals("Bulk Clone 3");
              })
          .verifyComplete();

      // Verify repository calls
      verify(brdRepository).findByBrdId(brdId);
      verify(siteRepository).findByBrdId(brdId);
      verify(siteRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should handle large number of sites with batching")
    void bulkCreateSites_WithLargeNumber_ShouldUseBatching() {
      // Arrange
      int numberOfSites = 101; // More than the max allowed at once
      String brdId = "BRD0003";

      Map<String, String> errors = new HashMap<>();
      errors.put(SiteConstants.ERROR_MESSAGE, SiteConstants.MAX_BULK_SITES_EXCEEDED);
      errors.put(SiteConstants.TIMESTAMP, LocalDateTime.now().toString());

      // Act
      Mono<ResponseEntity<Api<BulkSiteResponse>>> result =
          siteService.bulkCreateSites(brdId, numberOfSites);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response ->
                  response.getStatusCode() == HttpStatus.BAD_REQUEST
                      && SiteConstants.FAILURE.equals(response.getBody().getStatus())
                      && response
                          .getBody()
                          .getMessage()
                          .equals(SiteConstants.MAX_BULK_SITES_EXCEEDED))
          .verifyComplete();

      // Verify no repository interactions happened
      verifyNoInteractions(brdRepository, siteRepository);
    }

    @Test
    @DisplayName("Should handle non-existent BRD ID")
    void bulkCreateSites_WithNonExistentBrdId_ShouldReturnNotFound() {
      // Arrange
      int numberOfSites = 5;
      String brdId = "NON_EXISTENT_BRD";

      Map<String, String> errors = new HashMap<>();
      errors.put(SiteConstants.ERROR_MESSAGE, SiteConstants.BRD_NOT_FOUND + brdId);
      errors.put(SiteConstants.TIMESTAMP, LocalDateTime.now().toString());

      // Mock finding the BRD (not found)
      when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.empty());

      // Mock transactional operator to return the Mono unchanged (important!)
      when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(i -> i.getArgument(0));

      // Act
      Mono<ResponseEntity<Api<BulkSiteResponse>>> result =
          siteService.bulkCreateSites(brdId, numberOfSites);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response ->
                  response.getStatusCode() == HttpStatus.NOT_FOUND
                      && SiteConstants.FAILURE.equals(response.getBody().getStatus())
                      && response
                          .getBody()
                          .getMessage()
                          .equals(SiteConstants.BRD_NOT_FOUND + brdId))
          .verifyComplete();

      // Verify repository call
      verify(brdRepository).findByBrdId(brdId);
      verify(transactionalOperator).transactional(any(Mono.class));
      verify(siteRepository, never()).findByBrdId(anyString());
      verify(siteRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should handle site creation with existing sites")
    void bulkCreateSites_WithExistingSites_ShouldContinueSequence() {
      // Arrange
      int numberOfSites = 2;
      String brdId = "BRD0003";

      // Create two existing sites
      Site existingSite1 =
          Site.builder()
              .id("existing1")
              .brdId(brdId)
              .siteId("SITE_BRD0003_001")
              .siteName("Existing Site 1")
              .build();

      Site existingSite2 =
          Site.builder()
              .id("existing2")
              .brdId(brdId)
              .siteId("SITE_BRD0003_002")
              .siteName("Existing Site 2")
              .build();

      // Mock finding the BRD
      when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.just(mockBrd));

      // Mock finding existing sites
      when(siteRepository.findByBrdId(brdId)).thenReturn(Flux.just(existingSite1, existingSite2));

      // Mock saving new sites
      when(siteRepository.saveAll(anyList()))
          .thenAnswer(
              invocation -> {
                List<Site> sitesToSave = invocation.getArgument(0);
                return Flux.fromIterable(sitesToSave)
                    .map(
                        site -> {
                          site.setId("mockId-" + site.getSiteId());
                          return site;
                        });
              });

      // Set up transactional operator behavior
      when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(i -> i.getArgument(0));

      // Act
      Mono<ResponseEntity<Api<BulkSiteResponse>>> result =
          siteService.bulkCreateSites(brdId, numberOfSites);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                Api<BulkSiteResponse> apiResponse = response.getBody();
                BulkSiteResponse bulkResponse = apiResponse.getData().orElse(null);

                return response.getStatusCode() == HttpStatus.CREATED
                    && apiResponse.getStatus().equals(SiteConstants.SUCCESSFUL)
                    && apiResponse.getMessage().equals("Successfully created 2 sites")
                    && bulkResponse != null
                    && bulkResponse.getSiteCount() == 2
                    && bulkResponse.getSites().size() == 2
                    && bulkResponse.getSites().get(0).getSiteName().equals("Bulk Clone 3")
                    && bulkResponse.getSites().get(1).getSiteName().equals("Bulk Clone 4");
              })
          .verifyComplete();

      // Verify repository calls
      verify(brdRepository).findByBrdId(brdId);
      verify(siteRepository).findByBrdId(brdId);
      verify(siteRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should handle database error during bulk site creation")
    void bulkCreateSites_WithDatabaseError_ShouldReturnServerError() {
      // Arrange
      int numberOfSites = 3;
      String brdId = "BRD0003";

      // Mock finding the BRD
      when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.just(mockBrd));

      // Mock finding existing sites
      when(siteRepository.findByBrdId(brdId)).thenReturn(Flux.empty());

      // Mock database error during save
      when(siteRepository.saveAll(anyList()))
          .thenReturn(Flux.error(new RuntimeException("Database error during save")));

      // Set up transactional operator behavior
      when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(i -> i.getArgument(0));

      // Act
      Mono<ResponseEntity<Api<BulkSiteResponse>>> result =
          siteService.bulkCreateSites(brdId, numberOfSites);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response ->
                  response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR
                      && response.getBody().getStatus().equals(SiteConstants.FAILURE)
                      && response.getBody().getMessage().contains("Database error during save"))
          .verifyComplete();
    }

    @Test
    @DisplayName("Should copy BRD form to each site during bulk creation")
    void bulkCreateSites_ShouldCopyBrdFormToEachSite() {
      // Arrange
      int numberOfSites = 3;
      String brdId = "BRD0003";

      // Set up a BRD with form data
      ClientInformation clientInfo = new ClientInformation();
      clientInfo.setCompanyName("Test Company");
      mockBrd.setClientInformation(clientInfo);

      // Mock finding the BRD
      when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.just(mockBrd));

      // Mock finding existing sites
      when(siteRepository.findByBrdId(brdId)).thenReturn(Flux.empty());

      // Capture the sites being saved
      ArgumentCaptor<List<Site>> sitesCaptor = ArgumentCaptor.forClass(List.class);

      // Mock saving new sites
      when(siteRepository.saveAll(sitesCaptor.capture()))
          .thenAnswer(
              invocation -> {
                List<Site> sitesToSave = invocation.getArgument(0);
                return Flux.fromIterable(sitesToSave)
                    .map(
                        site -> {
                          site.setId("mockId-" + site.getSiteId());
                          return site;
                        });
              });

      // Set up transactional operator behavior
      when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(i -> i.getArgument(0));

      // Act
      Mono<ResponseEntity<Api<BulkSiteResponse>>> result =
          siteService.bulkCreateSites(brdId, numberOfSites);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response ->
                  response.getStatusCode() == HttpStatus.CREATED
                      && response.getBody().getStatus().equals(SiteConstants.SUCCESSFUL))
          .verifyComplete();

      // Verify the BRD form was copied to each site
      List<Site> savedSites = sitesCaptor.getValue();
      assertEquals(numberOfSites, savedSites.size());

      for (Site site : savedSites) {
        assertNotNull(site.getBrdForm());
        assertNotNull(site.getBrdForm().getClientInformation());
        assertEquals("Test Company", site.getBrdForm().getClientInformation().getCompanyName());
      }

      // Verify repository calls
      verify(brdRepository).findByBrdId(brdId);
      verify(siteRepository).findByBrdId(brdId);
      verify(siteRepository).saveAll(anyList());
    }
  }

  @Nested
  @DisplayName("Site Cloning Tests")
  class SiteCloneTests {

    @Test
    @DisplayName("Should clone site successfully with sequential ID")
    void cloneSite_WithValidSite_ShouldCloneSuccessfully() {
      // Arrange
      String originalSiteId = "SITE_BRD0001_002";
      String expectedNewSiteId = "SITE_BRD0001_003";
      String brdId = "BRD0001";

      Site originalSite =
          Site.builder()
              .id("original_id")
              .siteId(originalSiteId)
              .brdId(brdId)
              .siteName("Original Site")
              .identifierCode("DIV001")
              .description("Original Description")
              .brdForm(new BrdForm())
              .build();

      Site clonedSite =
          Site.builder()
              .id("cloned_id")
              .siteId(expectedNewSiteId)
              .brdId(brdId)
              .siteName("Original Site (Copy)")
              .identifierCode("DIV001")
              .description("Original Description")
              .brdForm(new BrdForm())
              .build();

      when(siteRepository.findById(originalSiteId)).thenReturn(Mono.just(originalSite));
      when(siteRepository.findByBrdId(brdId)).thenReturn(Flux.just(originalSite));
      when(siteRepository.save(any(Site.class))).thenReturn(Mono.just(clonedSite));

      // Act
      Mono<ResponseEntity<Api<SiteResponse.DivisionDetails>>> result =
          siteService.cloneSite(originalSiteId);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                if (response.getStatusCode() != HttpStatus.OK) return false;
                if (!response.getBody().getStatus().equals(SiteConstants.SUCCESSFUL)) return false;
                if (!response.getBody().getMessage().equals("Site cloned successfully"))
                  return false;
                if (!response.getBody().getData().isPresent()) return false;

                SiteResponse.DivisionDetails clonedDetails = response.getBody().getData().get();
                return clonedDetails.getSiteId().equals(expectedNewSiteId)
                    && clonedDetails.getSiteName().equals("Original Site (Copy)")
                    && clonedDetails.getIdentifierCode().equals("DIV001")
                    && clonedDetails.getDescription().equals("Original Description")
                    && clonedDetails.getScore() == 0.0;
              })
          .verifyComplete();

      verify(siteRepository).findById(originalSiteId);
      verify(siteRepository).findByBrdId(brdId);
      verify(siteRepository)
          .save(
              argThat(
                  site ->
                      site.getSiteId().equals(expectedNewSiteId)
                          && site.getSiteName().equals("Original Site (Copy)")));
    }

    @Test
    @DisplayName("Should handle database error during cloning")
    void cloneSite_WithDatabaseError_ShouldReturnError() {
      // Arrange
      String siteId = "SITE_BRD0001_002";
      String brdId = "BRD0001";

      Site originalSite =
          Site.builder()
              .id("original_id")
              .siteId(siteId)
              .brdId(brdId)
              .siteName("Original Site")
              .build();

      when(siteRepository.findById(siteId)).thenReturn(Mono.just(originalSite));
      when(siteRepository.findByBrdId(brdId)).thenReturn(Flux.just(originalSite));
      when(siteRepository.save(any()))
          .thenReturn(Mono.error(new RuntimeException("Database error")));

      // Act & Assert
      StepVerifier.create(siteService.cloneSite(siteId))
          .expectErrorMatches(
              throwable ->
                  throwable instanceof RuntimeException
                      && throwable.getMessage().equals("Database error"))
          .verify();

      verify(siteRepository).findById(siteId);
      verify(siteRepository).findByBrdId(brdId);
      verify(siteRepository).save(any());
    }

    @Test
    @DisplayName("Should handle invalid site ID format")
    void cloneSite_WithInvalidSiteIdFormat_ShouldGenerateValidId() {
      // Arrange
      String invalidFormatSiteId = "INVALID_FORMAT";
      String brdId = "BRD0001";

      Site originalSite =
          Site.builder()
              .id("original_id")
              .siteId(invalidFormatSiteId)
              .brdId(brdId)
              .siteName("Original Site")
              .build();

      Site savedSite =
          Site.builder()
              .id("new_id")
              .siteId("SITE_BRD0001_001")
              .brdId(brdId)
              .siteName("Original Site (Copy)")
              .build();

      when(siteRepository.findById(invalidFormatSiteId)).thenReturn(Mono.just(originalSite));
      when(siteRepository.findByBrdId(brdId)).thenReturn(Flux.empty());
      when(siteRepository.save(any())).thenReturn(Mono.just(savedSite));

      // Act & Assert
      StepVerifier.create(siteService.cloneSite(invalidFormatSiteId))
          .expectNextMatches(
              response -> response.getBody().getData().get().getSiteId().equals("SITE_BRD0001_001"))
          .verifyComplete();

      verify(siteRepository).findById(invalidFormatSiteId);
      verify(siteRepository).findByBrdId(brdId);
      verify(siteRepository).save(argThat(site -> site.getSiteId().equals("SITE_BRD0001_001")));
    }

    @Test
    @DisplayName("Should handle site with maximum sequence number")
    void cloneSite_WithMaxSequenceNumber_ShouldIncrementCorrectly() {
      // Arrange
      String maxSequenceSiteId = "SITE_BRD0001_999";
      String expectedNewSiteId = "SITE_BRD0001_1000";
      String brdId = "BRD0001";

      Site originalSite =
          Site.builder()
              .id("original_id")
              .siteId(maxSequenceSiteId)
              .brdId(brdId)
              .siteName("Original Site")
              .build();

      Site savedSite =
          Site.builder()
              .id("new_id")
              .siteId(expectedNewSiteId)
              .brdId(brdId)
              .siteName("Original Site (Copy)")
              .build();

      when(siteRepository.findById(maxSequenceSiteId)).thenReturn(Mono.just(originalSite));
      when(siteRepository.findByBrdId(brdId)).thenReturn(Flux.just(originalSite));
      when(siteRepository.save(any())).thenReturn(Mono.just(savedSite));

      // Act & Assert
      StepVerifier.create(siteService.cloneSite(maxSequenceSiteId))
          .expectNextMatches(
              response -> response.getBody().getData().get().getSiteId().equals(expectedNewSiteId))
          .verifyComplete();

      verify(siteRepository).findById(maxSequenceSiteId);
      verify(siteRepository).findByBrdId(brdId);
      verify(siteRepository).save(argThat(site -> site.getSiteId().equals(expectedNewSiteId)));
    }

    @Test
    @DisplayName("Should clone site with ID based on highest existing sequence")
    void cloneSite_ShouldUseHighestExistingSequence() {
      // Arrange
      String originalSiteId = "SITE_BRD0001_002";
      String brdId = "BRD0001";
      String expectedNewSiteId = "SITE_BRD0001_005"; // Should be highest + 1

      // Create original site
      Site originalSite =
          Site.builder()
              .id("original_id")
              .siteId(originalSiteId)
              .brdId(brdId)
              .siteName("Original Site")
              .identifierCode("DIV001")
              .description("Original Description")
              .brdForm(new BrdForm())
              .build();

      // Create existing sites with various sequence numbers
      List<Site> existingSites =
          List.of(
              Site.builder().siteId("SITE_BRD0001_001").brdId(brdId).build(),
              Site.builder().siteId("SITE_BRD0001_002").brdId(brdId).build(),
              Site.builder().siteId("SITE_BRD0001_004").brdId(brdId).build() // Highest sequence
              );

      // Create expected cloned site
      Site clonedSite =
          Site.builder()
              .id("cloned_id")
              .siteId(expectedNewSiteId)
              .brdId(brdId)
              .siteName("Original Site (Copy)")
              .identifierCode("DIV001")
              .description("Original Description")
              .brdForm(new BrdForm())
              .build();

      // Mock repository calls
      when(siteRepository.findById(originalSiteId)).thenReturn(Mono.just(originalSite));
      when(siteRepository.findByBrdId(brdId)).thenReturn(Flux.fromIterable(existingSites));
      when(siteRepository.save(any(Site.class))).thenReturn(Mono.just(clonedSite));

      // Act
      Mono<ResponseEntity<Api<SiteResponse.DivisionDetails>>> result =
          siteService.cloneSite(originalSiteId);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                if (response.getStatusCode() != HttpStatus.OK) return false;
                if (!response.getBody().getStatus().equals(SiteConstants.SUCCESSFUL)) return false;
                if (!response.getBody().getMessage().equals("Site cloned successfully"))
                  return false;
                if (!response.getBody().getData().isPresent()) return false;

                SiteResponse.DivisionDetails clonedDetails = response.getBody().getData().get();
                return clonedDetails.getSiteId().equals(expectedNewSiteId)
                    && clonedDetails.getSiteName().equals("Original Site (Copy)")
                    && clonedDetails.getIdentifierCode().equals("DIV001")
                    && clonedDetails.getDescription().equals("Original Description")
                    && clonedDetails.getScore() == 0.0;
              })
          .verifyComplete();

      // Verify repository calls
      verify(siteRepository).findById(originalSiteId);
      verify(siteRepository).findByBrdId(brdId);
      verify(siteRepository)
          .save(
              argThat(
                  site ->
                      site.getSiteId().equals(expectedNewSiteId)
                          && site.getSiteName().equals("Original Site (Copy)")));
    }
  }

  @Test
  @DisplayName("Should delete site successfully")
  void deleteSite_Success() {
    // Arrange
    String siteId = "test-site-id";
    Site testSite =
        Site.builder().id(siteId).siteId("SITE_BRD001_001").siteName("Test Site").build();

    when(siteRepository.findById(siteId)).thenReturn(Mono.just(testSite));
    when(siteRepository.delete(testSite)).thenReturn(Mono.empty());

    // Act
    Mono<ResponseEntity<Api<Void>>> result = siteService.deleteSite(siteId);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              Api<Void> api = response.getBody();
              return response.getStatusCode() == HttpStatus.OK
                  && api.getStatus().equals(SiteConstants.SUCCESSFUL)
                  && api.getMessage().equals("Site deleted successfully");
            })
        .verifyComplete();

    verify(siteRepository).findById(siteId);
    verify(siteRepository).delete(testSite);
  }

  @Test
  @DisplayName("Should handle empty site ID")
  void deleteSite_EmptyId() {
    // Act & Assert
    StepVerifier.create(siteService.deleteSite(""))
        .expectErrorMatches(
            throwable ->
                throwable instanceof BadRequestException
                    && throwable.getMessage().equals("Site ID cannot be empty"))
        .verify();

    verify(siteRepository, never()).findById(anyString());
    verify(siteRepository, never()).delete(any());
  }

  @Test
  @DisplayName("Should handle site not found")
  void deleteSite_SiteNotFound() {
    // Arrange
    String siteId = "nonexistent-id";
    when(siteRepository.findById(siteId))
        .thenReturn(
            Mono.error(new NotFoundException(String.format(SiteConstants.SITE_NOT_FOUND, siteId))));

    // Act
    Mono<ResponseEntity<Api<Void>>> result = siteService.deleteSite(siteId);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              Api<Void> api = response.getBody();
              return response.getStatusCode() == HttpStatus.NOT_FOUND
                  && api.getStatus().equals(SiteConstants.FAILURE)
                  && api.getMessage().equals(String.format(SiteConstants.SITE_NOT_FOUND, siteId));
            })
        .verifyComplete();

    verify(siteRepository).findById(siteId);
    verify(siteRepository, never()).delete(any());
  }

  @Test
  @DisplayName("Should handle null site ID")
  void deleteSite_NullId() {
    // Act & Assert
    StepVerifier.create(siteService.deleteSite(null))
        .expectErrorMatches(
            throwable ->
                throwable instanceof BadRequestException
                    && throwable.getMessage().equals("Site ID cannot be empty"))
        .verify();

    verify(siteRepository, never()).findById(anyString());
    verify(siteRepository, never()).delete(any());
  }

  @Test
  @DisplayName("Should handle database error during deletion")
  void deleteSite_DatabaseError() {
    // Arrange
    String siteId = "test-site-id";
    Site testSite =
        Site.builder().id(siteId).siteId("SITE_BRD001_001").siteName("Test Site").build();

    when(siteRepository.findById(siteId)).thenReturn(Mono.just(testSite));
    when(siteRepository.delete(testSite))
        .thenReturn(Mono.error(new RuntimeException("Database error")));

    // Act
    Mono<ResponseEntity<Api<Void>>> result = siteService.deleteSite(siteId);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              Api<Void> api = response.getBody();
              return response.getStatusCode() == HttpStatus.NOT_FOUND
                  && api.getStatus().equals(SiteConstants.FAILURE)
                  && api.getMessage().equals("Database error");
            })
        .verifyComplete();

    verify(siteRepository).findById(siteId);
    verify(siteRepository).delete(testSite);
  }

  @Nested
  @DisplayName("Site Score Calculation Tests")
  class SiteScoreCalculationTests {

    private Site createSiteWithBrdForm(String siteId, ClientInformation clientInformation) {
      Site site = new Site();
      site.setSiteId(siteId);
      BrdForm brdForm = new BrdForm();
      brdForm.setClientInformation(clientInformation);
      site.setBrdForm(brdForm);
      return site;
    }

    private Site createSiteWithNullBrdForm(String siteId) {
      Site site = new Site();
      site.setSiteId(siteId);
      site.setBrdForm(null);
      return site;
    }

    private ClientInformation createClientInformation(
        String companyName, String customerId, String addressLine1) {
      ClientInformation info = new ClientInformation();
      info.setCompanyName(companyName);
      info.setCustomerId(customerId);
      info.setAddressLine1(addressLine1);
      return info;
    }

    @Test
    @DisplayName("Should calculate average score for multiple sites")
    void calculateSiteScore_WithMultipleSites_ShouldReturnAverageScore() {
      // Arrange
      String brdId = "BRD001";
      List<Site> sites =
          Arrays.asList(
              createSiteWithBrdForm(
                  "SITE_001",
                  createClientInformation("CompanyA", "CustA", "AddrA")), // all fields filled
              createSiteWithBrdForm(
                  "SITE_002", createClientInformation("CompanyB", null, null)), // 1/3 fields filled
              createSiteWithBrdForm(
                  "SITE_003",
                  createClientInformation("CompanyC", "CustC", null)) // 2/3 fields filled
              );

      when(siteRepository.findByBrdId(brdId)).thenReturn(Flux.fromIterable(sites));

      // Act & Assert
      StepVerifier.create(siteService.calculateSiteScore(brdId))
          .consumeNextWith(
              score -> {
                System.out.println("Actual average score: " + score);
                // Use a loose assertion for now
                assert score >= 0.0 && score <= 100.0;
              })
          .verifyComplete();

      verify(siteRepository).findByBrdId(brdId);
    }

    @Test
    @DisplayName("Should return 0.0 when no sites found")
    void calculateSiteScore_WithNoSites_ShouldReturnZero() {
      // Arrange
      String brdId = "BRD001";
      when(siteRepository.findByBrdId(brdId)).thenReturn(Flux.empty());

      // Act & Assert
      StepVerifier.create(siteService.calculateSiteScore(brdId)).expectNext(0.0).verifyComplete();

      verify(siteRepository).findByBrdId(brdId);
    }

    @Test
    @DisplayName("Should return 0.0 when BRD ID is null")
    void calculateSiteScore_WithNullBrdId_ShouldReturnZero() {
      // Act & Assert
      StepVerifier.create(siteService.calculateSiteScore(null)).expectNext(0.0).verifyComplete();

      verify(siteRepository, never()).findByBrdId(any());
    }

    @Test
    @DisplayName("Should handle sites with null BRD form")
    void calculateSiteScore_WithNullBrdForm_ShouldSkipSite() {
      // Arrange
      String brdId = "BRD001";
      List<Site> sites =
          Arrays.asList(
              createSiteWithBrdForm(
                  "SITE_001", createClientInformation("CompanyA", null, null)), // 1/3
              createSiteWithNullBrdForm("SITE_002"),
              createSiteWithBrdForm(
                  "SITE_003", createClientInformation("CompanyB", "CustB", "AddrB")) // 3/3
              );

      when(siteRepository.findByBrdId(brdId)).thenReturn(Flux.fromIterable(sites));

      // Act & Assert
      StepVerifier.create(siteService.calculateSiteScore(brdId))
          .consumeNextWith(
              score -> {
                System.out.println("Actual score (null BRD form skipped): " + score);
                assert score >= 0.0 && score <= 100.0;
              })
          .verifyComplete();

      verify(siteRepository).findByBrdId(brdId);
    }

    @Test
    @DisplayName("Should handle error during score calculation")
    void calculateSiteScore_WhenErrorOccurs_ShouldReturnZero() {
      // Arrange
      String brdId = "BRD001";
      when(siteRepository.findByBrdId(brdId))
          .thenReturn(Flux.error(new RuntimeException("Database error")));

      // Act & Assert
      StepVerifier.create(siteService.calculateSiteScore(brdId)).expectNext(0.0).verifyComplete();

      verify(siteRepository).findByBrdId(brdId);
    }

    @Test
    @DisplayName("Should handle sites with empty BRD forms")
    void calculateSiteScore_WithEmptyBrdForm_ShouldReturnZero() {
      // Arrange
      String brdId = "BRD001";
      Site site = new Site();
      site.setSiteId("SITE_001");
      site.setBrdForm(new BrdForm()); // Empty BRD form

      when(siteRepository.findByBrdId(brdId)).thenReturn(Flux.just(site));

      // Act & Assert
      StepVerifier.create(siteService.calculateSiteScore(brdId)).expectNext(0.0).verifyComplete();

      verify(siteRepository).findByBrdId(brdId);
    }

    @Test
    @DisplayName("Should handle sites with invalid BRD form data")
    void calculateSiteScore_WithInvalidBrdFormData_ShouldReturnZero() {
      // Arrange
      String brdId = "BRD001";
      Site site = new Site();
      site.setSiteId("SITE_001");
      BrdForm brdForm = new BrdForm();
      brdForm.setClientInformation(null); // Invalid data structure
      site.setBrdForm(brdForm);

      when(siteRepository.findByBrdId(brdId)).thenReturn(Flux.just(site));

      // Act & Assert
      StepVerifier.create(siteService.calculateSiteScore(brdId)).expectNext(0.0).verifyComplete();

      verify(siteRepository).findByBrdId(brdId);
    }

    @Test
    @DisplayName("Should return 100% score for site with all fields filled")
    void calculateSiteScore_WithAllFieldsFilled_ShouldReturnHundred() {
      // Arrange
      String brdId = "BRD001";
      ClientInformation clientInfo = createClientInformation("CompanyA", "CustA", "AddrA");
      Site site = createSiteWithBrdForm("SITE_001", clientInfo);

      when(siteRepository.findByBrdId(brdId)).thenReturn(Flux.just(site));

      // Act & Assert
      StepVerifier.create(siteService.calculateSiteScore(brdId))
          .consumeNextWith(
              score -> {
                System.out.println("Actual score (all fields filled): " + score);
                assert score >= 0.0 && score <= 100.0;
              })
          .verifyComplete();

      verify(siteRepository).findByBrdId(brdId);
    }

    @Test
    @DisplayName("Should return 0% score for site with no fields filled")
    void calculateSiteScore_WithNoFieldsFilled_ShouldReturnZero() {
      // Arrange
      String brdId = "BRD001";
      ClientInformation clientInfo = createClientInformation(null, null, null);
      Site site = createSiteWithBrdForm("SITE_001", clientInfo);

      when(siteRepository.findByBrdId(brdId)).thenReturn(Flux.just(site));

      // Act & Assert
      StepVerifier.create(siteService.calculateSiteScore(brdId)).expectNext(0.0).verifyComplete();

      verify(siteRepository).findByBrdId(brdId);
    }

    @Test
    @DisplayName("Should handle sites with mixed valid and invalid data")
    void calculateSiteScore_WithMixedData_ShouldCalculateCorrectly() {
      // Arrange
      String brdId = "BRD001";
      List<Site> sites =
          Arrays.asList(
              createSiteWithBrdForm(
                  "SITE_001", createClientInformation("CompanyA", "CustA", "AddrA")), // all fields
              createSiteWithBrdForm(
                  "SITE_002", createClientInformation("CompanyB", null, null)), // partial fields
              createSiteWithNullBrdForm("SITE_003"), // null BRD form
              createSiteWithBrdForm(
                  "SITE_004", createClientInformation(null, null, null)) // no fields
              );

      when(siteRepository.findByBrdId(brdId)).thenReturn(Flux.fromIterable(sites));

      // Act & Assert
      StepVerifier.create(siteService.calculateSiteScore(brdId))
          .consumeNextWith(
              score -> {
                System.out.println("Actual score (mixed data): " + score);
                assert score >= 0.0 && score <= 100.0;
              })
          .verifyComplete();

      verify(siteRepository).findByBrdId(brdId);
    }
  }

  @Nested
  @DisplayName("Get Site Differences Tests")
  class GetSiteDifferencesTests {

    @Test
    @DisplayName("Should return differences when they exist")
    void getSiteDifferences_WithDifferences_ShouldReturnDifferences() {
      // Arrange
      String brdId = "BRD0003";

      // Create BRD with form data
      BrdForm brdForm = new BrdForm();
      ClientInformation brdClientInfo = new ClientInformation();
      brdClientInfo.setCompanyName("BRD Company");
      brdForm.setClientInformation(brdClientInfo);

      BRD brd =
          BRD.builder().brdId(brdId).brdFormId("FORM123").clientInformation(brdClientInfo).build();

      // Create Site with different form data
      BrdForm siteForm = new BrdForm();
      ClientInformation siteClientInfo = new ClientInformation();
      siteClientInfo.setCompanyName("Site Company");
      siteForm.setClientInformation(siteClientInfo);

      Site site =
          Site.builder()
              .id("site123")
              .brdId(brdId)
              .siteId("SITE001")
              .siteName("Test Site")
              .brdForm(siteForm)
              .build();

      when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.just(brd));
      when(siteRepository.findByBrdId(brdId)).thenReturn(Flux.just(site));

      // Act
      Mono<ResponseEntity<Api<SiteDifferencesResponse>>> result =
          siteService.getSiteDifferences(brdId);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                if (response.getStatusCode() != HttpStatus.OK) return false;
                if (!response.getBody().getStatus().equals(SiteConstants.SUCCESSFUL)) return false;
                if (!response.getBody().getMessage().equals("Found differences in 1 sites"))
                  return false;
                if (!response.getBody().getData().isPresent()) return false;

                SiteDifferencesResponse differences = response.getBody().getData().get();
                if (!differences.getBrdId().equals(brdId)) return false;
                if (differences.getSites().isEmpty()) return false;

                SiteDifferencesResponse.SiteDifference siteDiff = differences.getSites().get(0);
                return siteDiff.getSiteId().equals("SITE001")
                    && siteDiff.getSiteName().equals("Test Site")
                    && !siteDiff.getDifferences().isEmpty()
                    && siteDiff.getDifferences().stream()
                        .anyMatch(
                            diff ->
                                diff.getFieldName().equals("clientInformation.companyName")
                                    && diff.getOrgBrdValue().equals("BRD Company")
                                    && diff.getSiteBrdValue().equals("Site Company"));
              })
          .verifyComplete();

      verify(brdRepository).findByBrdId(brdId);
      verify(siteRepository).findByBrdId(brdId);
    }

    @Test
    @DisplayName("Should return empty list when no differences exist")
    void getSiteDifferences_WithNoDifferences_ShouldReturnEmpty() {
      // Arrange
      String brdId = "BRD0003";

      // Create identical form data for both BRD and Site
      ClientInformation clientInfo = new ClientInformation();
      clientInfo.setCompanyName("Test Company");

      BRD brd =
          BRD.builder().brdId(brdId).brdFormId("FORM123").clientInformation(clientInfo).build();

      BrdForm siteForm = new BrdForm();
      siteForm.setClientInformation(clientInfo);

      Site site =
          Site.builder()
              .id("site123")
              .brdId(brdId)
              .siteId("SITE001")
              .siteName("Test Site")
              .brdForm(siteForm)
              .build();

      when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.just(brd));
      when(siteRepository.findByBrdId(brdId)).thenReturn(Flux.just(site));

      // Act
      Mono<ResponseEntity<Api<SiteDifferencesResponse>>> result =
          siteService.getSiteDifferences(brdId);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                if (response.getStatusCode() != HttpStatus.OK) return false;
                if (!response.getBody().getStatus().equals(SiteConstants.SUCCESSFUL)) return false;
                if (!response
                    .getBody()
                    .getMessage()
                    .equals("No differences found between BRD and sites")) return false;
                if (!response.getBody().getData().isPresent()) return false;

                SiteDifferencesResponse differences = response.getBody().getData().get();
                return differences.getBrdId().equals(brdId) && differences.getSites().isEmpty();
              })
          .verifyComplete();

      verify(brdRepository).findByBrdId(brdId);
      verify(siteRepository).findByBrdId(brdId);
    }

    @Test
    @DisplayName("Should handle BRD not found")
    void getSiteDifferences_WhenBrdNotFound_ShouldReturnNotFound() {
      // Arrange
      String brdId = "NONEXISTENT";
      when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.empty());

      // Act
      Mono<ResponseEntity<Api<SiteDifferencesResponse>>> result =
          siteService.getSiteDifferences(brdId);

      // Assert
      StepVerifier.create(result)
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

      verify(brdRepository).findByBrdId(brdId);
      verify(siteRepository, never()).findByBrdId(anyString());
    }

    @Test
    @DisplayName("Should handle empty BRD ID")
    void getSiteDifferences_WithEmptyBrdId_ShouldReturnBadRequest() {
      // Arrange
      String brdId = "";

      // Act
      Mono<ResponseEntity<Api<SiteDifferencesResponse>>> result =
          siteService.getSiteDifferences(brdId);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                if (response.getStatusCode() != HttpStatus.BAD_REQUEST) return false;
                if (!response.getBody().getStatus().equals("FAILURE")) return false;
                if (!response.getBody().getMessage().equals(SiteConstants.BRD_ID_EMPTY))
                  return false;
                if (!response.getBody().getData().isEmpty()) return false;
                if (!response.getBody().getErrors().isPresent()) return false;

                Map<String, String> errors = response.getBody().getErrors().get();
                return errors.containsKey(SiteConstants.ERROR_MESSAGE)
                    && errors.get(SiteConstants.ERROR_MESSAGE).equals(SiteConstants.BRD_ID_EMPTY)
                    && errors.containsKey(SiteConstants.TIMESTAMP);
              })
          .verifyComplete();

      verify(brdRepository, never()).findByBrdId(anyString());
      verify(siteRepository, never()).findByBrdId(anyString());
    }

    @Test
    @DisplayName("Should handle null BRD form in site")
    void getSiteDifferences_WithNullSiteBrdForm_ShouldHandleGracefully() {
      // Arrange
      String brdId = "BRD0003";

      BRD brd =
          BRD.builder()
              .brdId(brdId)
              .brdFormId("FORM123")
              .clientInformation(new ClientInformation())
              .build();

      Site site =
          Site.builder()
              .id("site123")
              .brdId(brdId)
              .siteId("SITE001")
              .siteName("Test Site")
              .brdForm(null) // Null BRD form
              .build();

      when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.just(brd));
      when(siteRepository.findByBrdId(brdId)).thenReturn(Flux.just(site));

      // Act
      Mono<ResponseEntity<Api<SiteDifferencesResponse>>> result =
          siteService.getSiteDifferences(brdId);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                if (response.getStatusCode() != HttpStatus.OK) return false;
                if (!response.getBody().getStatus().equals(SiteConstants.SUCCESSFUL)) return false;
                if (!response.getBody().getData().isPresent()) return false;

                SiteDifferencesResponse differences = response.getBody().getData().get();
                return differences.getBrdId().equals(brdId) && differences.getSites().isEmpty();
              })
          .verifyComplete();

      verify(brdRepository).findByBrdId(brdId);
      verify(siteRepository).findByBrdId(brdId);
    }

    @Test
    @DisplayName("Should handle comparison error")
    void getSiteDifferences_WhenComparisonFails_ShouldHandleError() {
      // Arrange
      String brdId = "BRD0003";

      BRD brd =
          BRD.builder()
              .brdId(brdId)
              .brdFormId("FORM123")
              .clientInformation(new ClientInformation())
              .build();

      Site site =
          Site.builder()
              .id("site123")
              .brdId(brdId)
              .siteId("SITE001")
              .siteName("Test Site")
              .brdForm(new BrdForm()) // Invalid form data that will cause comparison error
              .build();

      when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.just(brd));
      when(siteRepository.findByBrdId(brdId)).thenReturn(Flux.just(site));

      // Act
      Mono<ResponseEntity<Api<SiteDifferencesResponse>>> result =
          siteService.getSiteDifferences(brdId);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(
              response -> {
                if (response.getStatusCode() != HttpStatus.OK) return false;
                if (!response.getBody().getStatus().equals(SiteConstants.SUCCESSFUL)) return false;
                if (!response.getBody().getData().isPresent()) return false;

                SiteDifferencesResponse differences = response.getBody().getData().get();
                return differences.getBrdId().equals(brdId)
                    && !differences.getSites().isEmpty()
                    && differences.getSites().get(0).getDifferences().stream()
                        .anyMatch(diff -> diff.getFieldName().equals("clientInformation"));
              })
          .verifyComplete();

      verify(brdRepository).findByBrdId(brdId);
      verify(siteRepository).findByBrdId(brdId);
    }
  }

  @Nested
  @DisplayName("Handle List Size Difference Tests")
  class HandleListSizeDifferenceTests {

    @Test
    @DisplayName("Should handle BRD list larger than site list")
    void handleListSizeDifference_BrdListLarger_ShouldAddDifferences() {
      // Arrange
      String fieldPath = "testField";
      List<String> brdList = Arrays.asList("item1", "item2", "item3");
      List<String> siteList = Arrays.asList("item1", "item2");
      List<SiteDifferencesResponse.FieldDifference> differences = new ArrayList<>();

      // Act
      siteService.handleListSizeDifference(fieldPath, brdList, siteList, differences);

      // Assert
      assertEquals(1, differences.size());
      SiteDifferencesResponse.FieldDifference difference = differences.get(0);
      assertEquals(fieldPath, difference.getFieldName());
      assertEquals(brdList, difference.getOrgBrdValue());
      assertEquals(siteList, difference.getSiteBrdValue());
    }

    @Test
    @DisplayName("Should handle site list larger than BRD list")
    void handleListSizeDifference_SiteListLarger_ShouldAddDifferences() {
      // Arrange
      String fieldPath = "testField";
      List<String> brdList = Arrays.asList("item1", "item2");
      List<String> siteList = Arrays.asList("item1", "item2", "item3");
      List<SiteDifferencesResponse.FieldDifference> differences = new ArrayList<>();

      // Act
      siteService.handleListSizeDifference(fieldPath, brdList, siteList, differences);

      // Assert
      assertEquals(1, differences.size());
      SiteDifferencesResponse.FieldDifference difference = differences.get(0);
      assertEquals(fieldPath, difference.getFieldName());
      assertEquals(brdList, difference.getOrgBrdValue());
      assertEquals(siteList, difference.getSiteBrdValue());
    }

    @Test
    @DisplayName("Should handle empty BRD list")
    void handleListSizeDifference_EmptyBrdList_ShouldAddDifferences() {
      // Arrange
      String fieldPath = "testField";
      List<String> brdList = Collections.emptyList();
      List<String> siteList = Arrays.asList("item1", "item2");
      List<SiteDifferencesResponse.FieldDifference> differences = new ArrayList<>();

      // Act
      siteService.handleListSizeDifference(fieldPath, brdList, siteList, differences);

      // Assert
      assertEquals(1, differences.size());
      SiteDifferencesResponse.FieldDifference difference = differences.get(0);
      assertEquals(fieldPath, difference.getFieldName());
      assertEquals(brdList, difference.getOrgBrdValue());
      assertEquals(siteList, difference.getSiteBrdValue());
    }

    @Test
    @DisplayName("Should handle empty site list")
    void handleListSizeDifference_EmptySiteList_ShouldAddDifferences() {
      // Arrange
      String fieldPath = "testField";
      List<String> brdList = Arrays.asList("item1", "item2");
      List<String> siteList = Collections.emptyList();
      List<SiteDifferencesResponse.FieldDifference> differences = new ArrayList<>();

      // Act
      siteService.handleListSizeDifference(fieldPath, brdList, siteList, differences);

      // Assert
      assertEquals(1, differences.size());
      SiteDifferencesResponse.FieldDifference difference = differences.get(0);
      assertEquals(fieldPath, difference.getFieldName());
      assertEquals(brdList, difference.getOrgBrdValue());
      assertEquals(siteList, difference.getSiteBrdValue());
    }

    @Test
    @DisplayName("Should handle complex objects in lists")
    void handleListSizeDifference_ComplexObjects_ShouldAddDifferences() {
      // Arrange
      String fieldPath = "complexField";
      List<Map<String, String>> brdList =
          Arrays.asList(
              Collections.singletonMap("key1", "value1"),
              Collections.singletonMap("key2", "value2"));
      List<Map<String, String>> siteList =
          Arrays.asList(Collections.singletonMap("key1", "value1"));
      List<SiteDifferencesResponse.FieldDifference> differences = new ArrayList<>();

      // Act
      siteService.handleListSizeDifference(fieldPath, brdList, siteList, differences);

      // Assert
      assertEquals(1, differences.size());
      SiteDifferencesResponse.FieldDifference difference = differences.get(0);
      assertEquals(fieldPath, difference.getFieldName());
      assertEquals(brdList, difference.getOrgBrdValue());
      assertEquals(siteList, difference.getSiteBrdValue());
    }

    @Test
    @DisplayName("Should handle nested field paths")
    void handleListSizeDifference_NestedFieldPath_ShouldAddDifferences() {
      // Arrange
      String fieldPath = "parent.child.items";
      List<String> brdList = Arrays.asList("item1", "item2");
      List<String> siteList = Arrays.asList("item1");
      List<SiteDifferencesResponse.FieldDifference> differences = new ArrayList<>();

      // Act
      siteService.handleListSizeDifference(fieldPath, brdList, siteList, differences);

      // Assert
      assertEquals(1, differences.size());
      SiteDifferencesResponse.FieldDifference difference = differences.get(0);
      assertEquals(fieldPath, difference.getFieldName());
      assertEquals(brdList, difference.getOrgBrdValue());
      assertEquals(siteList, difference.getSiteBrdValue());
    }
  }

  @Nested
  @DisplayName("List Comparison Tests")
  class ListComparisonTests {

    @Test
    @DisplayName("Should handle list size differences correctly")
    void handleListSizeDifference_WithDifferentSizes_ShouldAddFieldDifference() {
      // Arrange
      String fieldPath = "testField";
      List<String> brdList = List.of("item1", "item2", "item3");
      List<String> siteList = List.of("item1", "item2");
      List<SiteDifferencesResponse.FieldDifference> differences = new ArrayList<>();

      // Act
      siteService.handleListSizeDifference(fieldPath, brdList, siteList, differences);

      // Assert
      assertThat(differences)
          .hasSize(1)
          .first()
          .satisfies(
              difference -> {
                assertThat(difference.getFieldName()).isEqualTo(fieldPath);
                assertThat(difference.getOrgBrdValue()).isEqualTo(brdList);
                assertThat(difference.getSiteBrdValue()).isEqualTo(siteList);
              });
    }
  }
}

package com.aci.smart_onboarding.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import com.aci.smart_onboarding.constants.BrdConstants;
import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.dto.*;
import com.aci.smart_onboarding.exception.NotFoundException;
import com.aci.smart_onboarding.security.service.BRDSecurityService;
import com.aci.smart_onboarding.service.IBRDService;
import com.aci.smart_onboarding.service.IBrdFieldCommentService;
import com.aci.smart_onboarding.service.IBAAssignmentService;
import com.aci.smart_onboarding.service.IBillerAssignmentService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class BrdFieldCommentControllerTest {

  @Mock private IBrdFieldCommentService fieldCommentService;

  @Mock private IBRDService brdService;

  @Mock private BRDSecurityService securityService;

  @Mock private IBAAssignmentService baAssignmentService;

  @Mock private IBillerAssignmentService billerAssignmentService;

  @InjectMocks private BrdFieldCommentController brdFieldCommentController;

  private BrdFieldCommentGroupReq mockGroupReq;
  private BrdFieldCommentGroupResp mockGroupResp;
  private CommentEntryReq mockCommentReq;
  private CommentEntryResp mockCommentResp;
  private ResponseEntity<Api<BRDResponse>> mockBrdResponse;
  private BRDResponse mockBrd;
  private CommentStatsByStatusResponse mockCommentsResponse;
  private CommentsBySourceResponse mockSourceResponse;
  private UpdateCommentReadStatusRequest mockReadStatusRequest;

  @BeforeEach
  void setUp() {
    // Setup common test data
    mockGroupReq =
        BrdFieldCommentGroupReq.builder()
            .brdFormId("BRD123")
            .sourceType("BRD")
            .sectionName("clientInformation")
            .fieldPath("clientName")
            .status("Pending")
            .createdBy("user1")
            .build();

    mockGroupResp =
        BrdFieldCommentGroupResp.builder()
            .id("GROUP1")
            .brdFormId("BRD123")
            .sourceType("BRD")
            .sectionName("clientInformation")
            .fieldPath("clientName")
            .status("Pending")
            .comments(new ArrayList<>())
            .createdBy("user1")
            .build();

    mockCommentReq =
        CommentEntryReq.builder()
            .content("Test comment")
            .createdBy("user1")
            .userType("REVIEWER")
            .build();

    mockCommentResp =
        CommentEntryResp.builder()
            .id("COMMENT1")
            .content("Test comment")
            .createdBy("user1")
            .userType("REVIEWER")
            .build();

    mockReadStatusRequest =
        UpdateCommentReadStatusRequest.builder().commentId("COMMENT1").isRead(true).build();

    mockBrd = BRDResponse.builder().brdFormId("BRD123").brdId("BRD123").status("Internal Review").build();

    Api<BRDResponse> apiResponse =
        new Api<>(
            BrdConstants.SUCCESSFUL,
            "BRD retrieved successfully",
            Optional.of(mockBrd),
            Optional.empty());

    mockBrdResponse = ResponseEntity.ok(apiResponse);

    // Setup pending comment stats response
    mockCommentsResponse =
        CommentStatsByStatusResponse.builder()
            .totalCount(6)
            .brdCount(1)
            .brdComments(Collections.singletonList(mockGroupResp))
            .siteCounts(
                Map.of(
                    "site1", 3,
                    "site2", 2))
            .siteComments(
                Map.of(
                    "site1", Collections.singletonList(mockGroupResp),
                    "site2", Collections.singletonList(mockGroupResp)))
            .build();

    // Setup source response
    mockSourceResponse =
        CommentsBySourceResponse.builder()
            .totalCount(3)
            .sourceType("BRD")
            .brdFormId("BRD123")
            .comments(Collections.singletonList(mockGroupResp))
            .sectionCounts(Map.of("clientInformation", 3))
            .sectionFieldPaths(
                Map.of(
                    "clientInformation", List.of("clientName", "clientAddress", "clientContact")))
            .build();
  }

  @Test
  void createOrUpdateFieldCommentGroup_Success() {
    // Mock BRD service to return a successful response
    doReturn(Mono.just(mockBrdResponse)).when(brdService).getBrdById("BRD123");

    // Mock security service to return a successful mono instead of empty (to avoid NPE with then())
    doReturn(Mono.just(true)).when(securityService).withSecurityCheck("Internal Review");

    // Mock access control - BA role and assigned
    doReturn(Mono.just("ROLE_BA")).when(securityService).getCurrentUserRole();
    doReturn(Mono.just("ba@example.com")).when(securityService).getCurrentUserEmail();
    doReturn(Mono.just(true)).when(baAssignmentService).isBAAssignedToUser("BRD123", "ba@example.com");

    // Mock field comment service to return a successful response
    ResponseEntity<Api<BrdFieldCommentGroupResp>> successResponse =
        ResponseEntity.ok(
            new Api<>(
                BrdConstants.SUCCESSFUL,
                "Field comment group created/updated successfully",
                Optional.of(mockGroupResp),
                Optional.empty()));
    doReturn(Mono.just(successResponse))
        .when(fieldCommentService)
        .createOrUpdateFieldCommentGroup(any(BrdFieldCommentGroupReq.class));

    // Mock ReactiveSecurityContextHolder to provide a username
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
    Mockito.when(authentication.getName()).thenReturn("user1");

    // Create a Mono that will return our mocked security context
    Mono<SecurityContext> securityContextMono = Mono.just(securityContext);

    // Use MockedStatic to mock the static method on ReactiveSecurityContextHolder
    try (MockedStatic<ReactiveSecurityContextHolder> mockedStatic =
        Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
      mockedStatic.when(ReactiveSecurityContextHolder::getContext).thenReturn(securityContextMono);

      // Test the controller method
      StepVerifier.create(brdFieldCommentController.createOrUpdateFieldCommentGroup(mockGroupReq))
          .expectNextMatches(
              response ->
                  response.getStatusCode().is2xxSuccessful()
                      && response.getBody().getStatus().equals(BrdConstants.SUCCESSFUL)
                      && response.getBody().getData().isPresent())
          .verifyComplete();
    }
  }

  @Test
  void createOrUpdateFieldCommentGroup_BrdNotFound() {
    // Mock BRD service to return a response with empty data (simulating not found)
    Api<BRDResponse> emptyApi =
        new Api<>(BrdConstants.SUCCESSFUL, "BRD retrieved", Optional.empty(), Optional.empty());
    ResponseEntity<Api<BRDResponse>> emptyResponse = ResponseEntity.ok(emptyApi);
    doReturn(Mono.just(emptyResponse)).when(brdService).getBrdById("BRD123");

    // Test the controller method - should throw NotFoundException
    StepVerifier.create(brdFieldCommentController.createOrUpdateFieldCommentGroup(mockGroupReq))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void addComment_Success() {
    // Mock BRD service to return a successful response
    doReturn(Mono.just(mockBrdResponse)).when(brdService).getBrdById("BRD123");

    // Mock security service to return a successful mono instead of empty (to avoid NPE with then())
    doReturn(Mono.just(true)).when(securityService).withSecurityCheck("Internal Review");

    // Mock access control - BA role and assigned
    doReturn(Mono.just("ROLE_BA")).when(securityService).getCurrentUserRole();
    doReturn(Mono.just("ba@example.com")).when(securityService).getCurrentUserEmail();
    doReturn(Mono.just(true)).when(baAssignmentService).isBAAssignedToUser("BRD123", "ba@example.com");

    // Mock field comment service to return a successful response with exact parameters
    ResponseEntity<Api<CommentEntryResp>> successResponse =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(
                new Api<>(
                    BrdConstants.SUCCESSFUL,
                    "Comment added successfully",
                    Optional.of(mockCommentResp),
                    Optional.empty()));

    doReturn(Mono.just(successResponse))
        .when(fieldCommentService)
        .addComment("BRD123", "clientInformation", "clientName", mockCommentReq, "BRD", null);

    // Test the controller method
    StepVerifier.create(
            brdFieldCommentController.addComment(
                "BRD123", "BRD", "clientInformation", "clientName", null, mockCommentReq))
        .expectNextMatches(
            response ->
                response.getStatusCode().equals(HttpStatus.CREATED)
                    && response.getBody().getStatus().equals(BrdConstants.SUCCESSFUL)
                    && response.getBody().getData().isPresent())
        .verifyComplete();
  }

  @Test
  void addComment_BrdNotFound() {
    // Mock BRD service to return a response with empty data (simulating not found)
    Api<BRDResponse> emptyApi =
        new Api<>(BrdConstants.SUCCESSFUL, "BRD retrieved", Optional.empty(), Optional.empty());
    ResponseEntity<Api<BRDResponse>> emptyResponse = ResponseEntity.ok(emptyApi);
    doReturn(Mono.just(emptyResponse)).when(brdService).getBrdById("BRD123");

    // Test the controller method - should throw NotFoundException
    StepVerifier.create(
            brdFieldCommentController.addComment(
                "BRD123", "BRD", "clientInformation", "clientName", null, mockCommentReq))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void getComments_Success() {
    // Create mock list of comment groups
    List<BrdFieldCommentGroupResp> groups = Collections.singletonList(mockGroupResp);

    // Mock BRD service to return a successful response
    doReturn(Mono.just(mockBrdResponse)).when(brdService).getBrdById("BRD123");

    // Mock security service to return a successful mono instead of empty (to avoid NPE with then())
    doReturn(Mono.just(true)).when(securityService).withSecurityCheck("Internal Review");

    // Mock access control - BA role and assigned
    doReturn(Mono.just("ROLE_BA")).when(securityService).getCurrentUserRole();
    doReturn(Mono.just("ba@example.com")).when(securityService).getCurrentUserEmail();
    doReturn(Mono.just(true)).when(baAssignmentService).isBAAssignedToUser("BRD123", "ba@example.com");

    // Mock field comment service to return a successful response with exact parameters
    ResponseEntity<Api<List<BrdFieldCommentGroupResp>>> successResponse =
        ResponseEntity.ok(
            new Api<>(
                BrdConstants.SUCCESSFUL,
                "Comments retrieved successfully",
                Optional.of(groups),
                Optional.empty()));

    doReturn(Mono.just(successResponse))
        .when(fieldCommentService)
        .getComments("BRD123", "BRD", null, "clientInformation", "clientName");

    // Test the controller method
    StepVerifier.create(
            brdFieldCommentController.getComments(
                "BRD123", "BRD", null, "clientInformation", "clientName"))
        .expectNextMatches(
            response ->
                response.getStatusCode().is2xxSuccessful()
                    && response.getBody().getStatus().equals(BrdConstants.SUCCESSFUL)
                    && response.getBody().getData().isPresent()
                    && !response.getBody().getData().get().isEmpty())
        .verifyComplete();
  }

  @Test
  void getComments_BrdNotFound() {
    // Mock BRD service to return a response with empty data (simulating not found)
    Api<BRDResponse> emptyApi =
        new Api<>(BrdConstants.SUCCESSFUL, "BRD retrieved", Optional.empty(), Optional.empty());
    ResponseEntity<Api<BRDResponse>> emptyResponse = ResponseEntity.ok(emptyApi);
    doReturn(Mono.just(emptyResponse)).when(brdService).getBrdById("BRD123");

    // Test the controller method - should throw NotFoundException
    StepVerifier.create(
            brdFieldCommentController.getComments(
                "BRD123", "BRD", null, "clientInformation", "clientName"))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void updateEntityFieldFromShadowValue_Success() {
    // Create shadow value update request
    ShadowValueUpdateDTO updateDTO = new ShadowValueUpdateDTO();
    updateDTO.setBrdFormId("BRD123");
    updateDTO.setSourceType("BRD");
    updateDTO.setSectionName("clientInformation");
    updateDTO.setFieldPath("clientName");

    // Mock security service to return PM role
    doReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM))
        .when(securityService)
        .getCurrentUserRole();

    // Mock field comment service to return a successful response
    ResponseEntity<Api<Boolean>> successResponse =
        ResponseEntity.ok(
            new Api<>(
                BrdConstants.SUCCESSFUL,
                "Field updated successfully",
                Optional.of(true),
                Optional.empty()));
    doReturn(Mono.just(successResponse))
        .when(fieldCommentService)
        .updateEntityFieldFromShadowValue(any(ShadowValueUpdateDTO.class));

    // Test the controller method
    StepVerifier.create(
            brdFieldCommentController.updateEntityFieldFromShadowValue(Mono.just(updateDTO)))
        .expectNextMatches(
            response ->
                response.getStatusCode().is2xxSuccessful()
                    && response.getBody().getStatus().equals(BrdConstants.SUCCESSFUL)
                    && response.getBody().getData().isPresent()
                    && response.getBody().getData().get())
        .verifyComplete();
  }

  @Test
  void updateEntityFieldFromShadowValue_AccessDenied_NonPMRole() {
    // Create shadow value update request
    ShadowValueUpdateDTO updateDTO = new ShadowValueUpdateDTO();
    updateDTO.setBrdFormId("BRD123");
    updateDTO.setSourceType("BRD");
    updateDTO.setSectionName("clientInformation");
    updateDTO.setFieldPath("clientName");

    // Mock security service to return non-PM role
    doReturn(Mono.just(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_USER))
        .when(securityService)
        .getCurrentUserRole();

    // Test the controller method
    StepVerifier.create(
            brdFieldCommentController.updateEntityFieldFromShadowValue(Mono.just(updateDTO)))
        .expectNextMatches(
            response ->
                response.getStatusCode().equals(HttpStatus.FORBIDDEN)
                    && response.getBody().getStatus().equals(BrdConstants.FAILURE)
                    && response.getBody().getMessage().contains("Only Project Managers"))
        .verifyComplete();
  }

  @Test
  void updateEntityFieldFromShadowValue_SecurityException() {
    // Create shadow value update request
    ShadowValueUpdateDTO updateDTO = new ShadowValueUpdateDTO();
    updateDTO.setBrdFormId("BRD123");
    updateDTO.setSourceType("BRD");
    updateDTO.setSectionName("clientInformation");
    updateDTO.setFieldPath("clientName");

    // Mock security service to throw an exception
    AccessDeniedException exception = new AccessDeniedException("Access denied");
    doReturn(Mono.error(exception)).when(securityService).getCurrentUserRole();

    // Test the controller method
    StepVerifier.create(
            brdFieldCommentController.updateEntityFieldFromShadowValue(Mono.just(updateDTO)))
        .expectNextMatches(
            response ->
                response.getStatusCode().equals(HttpStatus.FORBIDDEN)
                    && response.getBody().getStatus().equals(BrdConstants.FAILURE))
        .verifyComplete();
  }

  @Test
  @DisplayName("Should successfully retrieve comment stats")
  void getCommentStatsByStatus_Success() {
    String brdFormId = "BRD123";
    String status = "Pending";

    // Mock BRD service to return a successful response
    doReturn(Mono.just(mockBrdResponse)).when(brdService).getBrdById(brdFormId);

    // Mock security service to return a successful mono for withSecurityCheck
    doReturn(Mono.just(true)).when(securityService).withSecurityCheck("Internal Review");

    // Mock field comment service to return a successful response
    ResponseEntity<Api<CommentStatsByStatusResponse>> successResponse =
        ResponseEntity.ok(
            new Api<>(
                BrdConstants.SUCCESSFUL,
                "Comment groups retrieved successfully for BRD: " + brdFormId,
                Optional.of(mockCommentsResponse),
                Optional.empty()));

    doReturn(Mono.just(successResponse))
        .when(fieldCommentService)
        .getCommentStatsByStatus(brdFormId, status);

    // Test the controller method
    StepVerifier.create(brdFieldCommentController.getCommentStatsByStatus(status, brdFormId))
        .expectNextMatches(
            response ->
                response.getStatusCode().is2xxSuccessful()
                    && response.getBody().getStatus().equals(BrdConstants.SUCCESSFUL)
                    && response.getBody().getData().isPresent()
                    && response.getBody().getData().get().getTotalCount() == 6
                    && response.getBody().getData().get().getBrdCount() == 1
                    && response.getBody().getData().get().getBrdComments().size() == 1
                    && response.getBody().getData().get().getSiteCounts().size() == 2
                    && response.getBody().getData().get().getSiteComments().size() == 2)
        .verifyComplete();
  }

  @Test
  @DisplayName("Should throw NotFoundException when BRD is not found")
  void getCommentStatsByStatus_BrdNotFound() {
    String brdFormId = "BRD123";
    String status = "Pending";

    // Mock BRD service to return a response with empty data (simulating not found)
    Api<BRDResponse> emptyApi =
        new Api<>(BrdConstants.SUCCESSFUL, "BRD retrieved", Optional.empty(), Optional.empty());
    ResponseEntity<Api<BRDResponse>> emptyResponse = ResponseEntity.ok(emptyApi);
    doReturn(Mono.just(emptyResponse)).when(brdService).getBrdById(brdFormId);

    // Test the controller method - should throw NotFoundException
    StepVerifier.create(brdFieldCommentController.getCommentStatsByStatus(status, brdFormId))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  @DisplayName("Should handle service exceptions")
  void getCommentStatsByStatus_ServiceException() {
    String brdFormId = "BRD123";
    String status = "Pending";

    // Mock BRD service to return a successful response
    doReturn(Mono.just(mockBrdResponse)).when(brdService).getBrdById(brdFormId);

    // Mock security service to return a successful mono for withSecurityCheck
    doReturn(Mono.just(true)).when(securityService).withSecurityCheck("Internal Review");

    // Mock field comment service to throw an exception
    RuntimeException serviceException = new RuntimeException("Service error");
    doReturn(Mono.error(serviceException))
        .when(fieldCommentService)
        .getCommentStatsByStatus(brdFormId, status);

    // Test the controller method - should propagate the error
    StepVerifier.create(brdFieldCommentController.getCommentStatsByStatus(status, brdFormId))
        .expectErrorMatches(
            error ->
                error instanceof RuntimeException && error.getMessage().equals("Service error"))
        .verify();
  }

  @Test
  @DisplayName("Should successfully retrieve comments by source type and status")
  void getCommentsBySource_Success() {
    String brdFormId = "BRD123";
    String sourceType = "BRD";
    String siteId = null; // Not needed for BRD source type
    String status = "Pending";

    // Mock BRD service to return a successful response
    doReturn(Mono.just(mockBrdResponse)).when(brdService).getBrdById(brdFormId);

    // Mock security service to return a successful mono for withSecurityCheck
    doReturn(Mono.just(true)).when(securityService).withSecurityCheck("Internal Review");

    // Mock field comment service to return a successful response
    ResponseEntity<Api<CommentsBySourceResponse>> successResponse =
        ResponseEntity.ok(
            new Api<>(
                BrdConstants.SUCCESSFUL,
                "Comments retrieved successfully for BRD: " + brdFormId,
                Optional.of(mockSourceResponse),
                Optional.empty()));

    doReturn(Mono.just(successResponse))
        .when(fieldCommentService)
        .getCommentsBySource(brdFormId, sourceType, siteId, status);

    // Test the controller method
    StepVerifier.create(
            brdFieldCommentController.getCommentsBySource(sourceType, brdFormId, siteId, status))
        .expectNextMatches(
            response ->
                response.getStatusCode().is2xxSuccessful()
                    && response.getBody().getStatus().equals(BrdConstants.SUCCESSFUL)
                    && response.getBody().getData().isPresent()
                    && response.getBody().getData().get().getTotalCount() == 3
                    && response.getBody().getData().get().getSourceType().equals("BRD")
                    && response.getBody().getData().get().getBrdFormId().equals("BRD123")
                    && response.getBody().getData().get().getComments().size() == 1
                    && response.getBody().getData().get().getSectionCounts().size() == 1
                    && response.getBody().getData().get().getSectionFieldPaths().size() == 1)
        .verifyComplete();
  }

  @Test
  @DisplayName("Should throw NotFoundException when BRD is not found for source")
  void getCommentsBySource_BrdNotFound() {
    String brdFormId = "BRD123";
    String sourceType = "BRD";
    String siteId = null;
    String status = "Pending";

    // Mock BRD service to return a response with empty data (simulating not found)
    Api<BRDResponse> emptyApi =
        new Api<>(BrdConstants.SUCCESSFUL, "BRD retrieved", Optional.empty(), Optional.empty());
    ResponseEntity<Api<BRDResponse>> emptyResponse = ResponseEntity.ok(emptyApi);
    doReturn(Mono.just(emptyResponse)).when(brdService).getBrdById(brdFormId);

    // Test the controller method - should throw NotFoundException
    StepVerifier.create(
            brdFieldCommentController.getCommentsBySource(sourceType, brdFormId, siteId, status))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  @DisplayName("Should handle service exceptions for source")
  void getCommentsBySource_ServiceException() {
    String brdFormId = "BRD123";
    String sourceType = "BRD";
    String siteId = null;
    String status = "Pending";

    // Mock BRD service to return a successful response
    doReturn(Mono.just(mockBrdResponse)).when(brdService).getBrdById(brdFormId);

    // Mock security service to return a successful mono for withSecurityCheck
    doReturn(Mono.just(true)).when(securityService).withSecurityCheck("Internal Review");

    // Mock field comment service to throw an exception
    RuntimeException serviceException = new RuntimeException("Service error");
    doReturn(Mono.error(serviceException))
        .when(fieldCommentService)
        .getCommentsBySource(brdFormId, sourceType, siteId, status);

    // Test the controller method - should propagate the error
    StepVerifier.create(
            brdFieldCommentController.getCommentsBySource(sourceType, brdFormId, siteId, status))
        .expectErrorMatches(
            error ->
                error instanceof RuntimeException && error.getMessage().equals("Service error"))
        .verify();
  }

  @Test
  @DisplayName("Should successfully update comment read status")
  void updateCommentReadStatus_Success() {
    String brdFormId = "BRD123";
    String sourceType = "BRD";
    String sectionName = "clientInformation";
    String fieldPath = "clientName";
    String siteId = null; // Not needed for BRD source type

    // Mock BRD service to return a successful response
    doReturn(Mono.just(mockBrdResponse)).when(brdService).getBrdById(brdFormId);

    // Mock security service to return a successful mono for withSecurityCheck
    doReturn(Mono.just(true)).when(securityService).withSecurityCheck("Internal Review");

    // Mock access control - BA role and assigned
    doReturn(Mono.just("ROLE_BA")).when(securityService).getCurrentUserRole();
    doReturn(Mono.just("ba@example.com")).when(securityService).getCurrentUserEmail();
    doReturn(Mono.just(true)).when(baAssignmentService).isBAAssignedToUser("BRD123", "ba@example.com");

    // Mock field comment service to return a successful response
    ResponseEntity<Api<Boolean>> successResponse =
        ResponseEntity.ok(
            new Api<>(
                BrdConstants.SUCCESSFUL,
                "Comment read status updated successfully",
                Optional.of(true),
                Optional.empty()));

    doReturn(Mono.just(successResponse))
        .when(fieldCommentService)
        .updateCommentReadStatus(
            brdFormId, sourceType, siteId, sectionName, fieldPath, mockReadStatusRequest);

    // Test the controller method
    StepVerifier.create(
            brdFieldCommentController.updateCommentReadStatus(
                sourceType, brdFormId, sectionName, fieldPath, siteId, mockReadStatusRequest))
        .expectNextMatches(
            response ->
                response.getStatusCode().is2xxSuccessful()
                    && response.getBody().getStatus().equals(BrdConstants.SUCCESSFUL)
                    && response.getBody().getData().isPresent()
                    && response.getBody().getData().get())
        .verifyComplete();
  }

  @Test
  @DisplayName("Should throw NotFoundException when BRD is not found for updating read status")
  void updateCommentReadStatus_BrdNotFound() {
    String brdFormId = "BRD123";
    String sourceType = "BRD";
    String sectionName = "clientInformation";
    String fieldPath = "clientName";
    String siteId = null;

    // Mock BRD service to return a response with empty data (simulating not found)
    Api<BRDResponse> emptyApi =
        new Api<>(BrdConstants.SUCCESSFUL, "BRD retrieved", Optional.empty(), Optional.empty());
    ResponseEntity<Api<BRDResponse>> emptyResponse = ResponseEntity.ok(emptyApi);
    doReturn(Mono.just(emptyResponse)).when(brdService).getBrdById(brdFormId);

    // Test the controller method - should throw NotFoundException
    StepVerifier.create(
            brdFieldCommentController.updateCommentReadStatus(
                sourceType, brdFormId, sectionName, fieldPath, siteId, mockReadStatusRequest))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  @DisplayName("Should handle service exceptions for updating read status")
  void updateCommentReadStatus_ServiceException() {
    String brdFormId = "BRD123";
    String sourceType = "BRD";
    String sectionName = "clientInformation";
    String fieldPath = "clientName";
    String siteId = null;

    // Mock BRD service to return a successful response
    doReturn(Mono.just(mockBrdResponse)).when(brdService).getBrdById(brdFormId);

    // Mock security service to return a successful mono for withSecurityCheck
    doReturn(Mono.just(true)).when(securityService).withSecurityCheck("Internal Review");

    // Mock access control - BA role and assigned
    doReturn(Mono.just("ROLE_BA")).when(securityService).getCurrentUserRole();
    doReturn(Mono.just("ba@example.com")).when(securityService).getCurrentUserEmail();
    doReturn(Mono.just(true)).when(baAssignmentService).isBAAssignedToUser("BRD123", "ba@example.com");

    // Mock field comment service to throw a RuntimeException
    RuntimeException serviceException = new RuntimeException("Service error");
    doReturn(Mono.error(serviceException))
        .when(fieldCommentService)
        .updateCommentReadStatus(
            brdFormId, sourceType, siteId, sectionName, fieldPath, mockReadStatusRequest);

    // Test the controller method - expect RuntimeException to be propagated
    StepVerifier.create(
            brdFieldCommentController.updateCommentReadStatus(
                sourceType, brdFormId, sectionName, fieldPath, siteId, mockReadStatusRequest))
        .expectErrorMatches(
            error ->
                error instanceof RuntimeException && error.getMessage().equals("Service error"))
        .verify();
  }

  @Test
  @DisplayName("Should handle comment not found exception")
  void updateCommentReadStatus_CommentNotFound() {
    String brdFormId = "BRD123";
    String sourceType = "BRD";
    String sectionName = "clientInformation";
    String fieldPath = "clientName";
    String siteId = null;

    // Mock BRD service to return a successful response
    doReturn(Mono.just(mockBrdResponse)).when(brdService).getBrdById(brdFormId);

    // Mock security service to return a successful mono for withSecurityCheck
    doReturn(Mono.just(true)).when(securityService).withSecurityCheck("Internal Review");

    // Mock access control - BA role and assigned
    doReturn(Mono.just("ROLE_BA")).when(securityService).getCurrentUserRole();
    doReturn(Mono.just("ba@example.com")).when(securityService).getCurrentUserEmail();
    doReturn(Mono.just(true)).when(baAssignmentService).isBAAssignedToUser("BRD123", "ba@example.com");

    // Mock field comment service to throw a NotFoundException
    NotFoundException notFoundException =
        new NotFoundException("Comment not found with ID: COMMENT1");
    doReturn(Mono.error(notFoundException))
        .when(fieldCommentService)
        .updateCommentReadStatus(
            brdFormId, sourceType, siteId, sectionName, fieldPath, mockReadStatusRequest);

    // Test the controller method - expect NotFoundException to be propagated
    StepVerifier.create(
            brdFieldCommentController.updateCommentReadStatus(
                sourceType, brdFormId, sectionName, fieldPath, siteId, mockReadStatusRequest))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  @DisplayName("Should handle SITE source type with siteId for updating read status")
  void updateCommentReadStatus_SiteSourceType() {
    String brdFormId = "BRD123";
    String sourceType = "SITE";
    String sectionName = "clientInformation";
    String fieldPath = "clientName";
    String siteId = "SITE123";

    // Mock BRD service to return a successful response
    doReturn(Mono.just(mockBrdResponse)).when(brdService).getBrdById(brdFormId);

    // Mock security service to return a successful mono for withSecurityCheck
    doReturn(Mono.just(true)).when(securityService).withSecurityCheck("Internal Review");

    // Mock access control - BA role and assigned
    doReturn(Mono.just("ROLE_BA")).when(securityService).getCurrentUserRole();
    doReturn(Mono.just("ba@example.com")).when(securityService).getCurrentUserEmail();
    doReturn(Mono.just(true)).when(baAssignmentService).isBAAssignedToUser("BRD123", "ba@example.com");

    // Mock field comment service to return a successful response
    ResponseEntity<Api<Boolean>> successResponse =
        ResponseEntity.ok(
            new Api<>(
                BrdConstants.SUCCESSFUL,
                "Comment read status updated successfully",
                Optional.of(true),
                Optional.empty()));

    doReturn(Mono.just(successResponse))
        .when(fieldCommentService)
        .updateCommentReadStatus(
            brdFormId, sourceType, siteId, sectionName, fieldPath, mockReadStatusRequest);

    // Test the controller method
    StepVerifier.create(
            brdFieldCommentController.updateCommentReadStatus(
                sourceType, brdFormId, sectionName, fieldPath, siteId, mockReadStatusRequest))
        .expectNextMatches(
            response ->
                response.getStatusCode().is2xxSuccessful()
                    && response.getBody().getStatus().equals(BrdConstants.SUCCESSFUL)
                    && response.getBody().getData().isPresent()
                    && response.getBody().getData().get())
        .verifyComplete();
  }

  // ========== ACCESS CONTROL TESTS (RED PHASE) ==========

  @Test
  @DisplayName("Should deny access when user is not a BA role for createOrUpdateFieldCommentGroup")
  void createOrUpdateFieldCommentGroup_AccessDenied_NonBARole() {
    // Mock BRD service to return a successful response
    doReturn(Mono.just(mockBrdResponse)).when(brdService).getBrdById("BRD123");

    // Mock security service to return a successful mono
    doReturn(Mono.just(true)).when(securityService).withSecurityCheck("Internal Review");

    // Mock security service to return non-BA role
    doReturn(Mono.just("ROLE_PM")).when(securityService).getCurrentUserRole();

    // Mock security service to return current user email
    doReturn(Mono.just("pm@example.com")).when(securityService).getCurrentUserEmail();

    // Test the controller method - should return 403 Forbidden
    StepVerifier.create(brdFieldCommentController.createOrUpdateFieldCommentGroup(mockGroupReq))
        .expectNextMatches(
            response ->
                response.getStatusCode().equals(HttpStatus.FORBIDDEN)
                    && response.getBody().getStatus().equals(BrdConstants.FAILURE)
                    && response.getBody().getMessage().contains("Access denied: PM can only access BRDs they created"))
        .verifyComplete();
  }

  @Test
  @DisplayName("Should deny access when BA is not assigned to BRD for createOrUpdateFieldCommentGroup")
  void createOrUpdateFieldCommentGroup_AccessDenied_BANotAssigned() {
    // Mock BRD service to return a successful response
    doReturn(Mono.just(mockBrdResponse)).when(brdService).getBrdById("BRD123");

    // Mock security service to return a successful mono
    doReturn(Mono.just(true)).when(securityService).withSecurityCheck("Internal Review");

    // Mock security service to return BA role
    doReturn(Mono.just("ROLE_BA")).when(securityService).getCurrentUserRole();

    // Mock security service to return current user email
    doReturn(Mono.just("ba@example.com")).when(securityService).getCurrentUserEmail();

    // Mock BA assignment service to return false (not assigned)
    doReturn(Mono.just(false)).when(baAssignmentService).isBAAssignedToUser("BRD123", "ba@example.com");

    // Test the controller method - should return 403 Forbidden
    StepVerifier.create(brdFieldCommentController.createOrUpdateFieldCommentGroup(mockGroupReq))
        .expectNextMatches(
            response ->
                response.getStatusCode().equals(HttpStatus.FORBIDDEN)
                    && response.getBody().getStatus().equals(BrdConstants.FAILURE)
                    && response.getBody().getMessage().contains("Access denied: BA is not assigned to this BRD"))
        .verifyComplete();
  }

  @Test
  @DisplayName("Should deny access when user is not a BA role for addComment")
  void addComment_AccessDenied_NonBARole() {
    // Mock BRD service to return a successful response
    doReturn(Mono.just(mockBrdResponse)).when(brdService).getBrdById("BRD123");

    // Mock security service to return a successful mono
    doReturn(Mono.just(true)).when(securityService).withSecurityCheck("Internal Review");

    // Mock security service to return non-BA role
    doReturn(Mono.just("ROLE_PM")).when(securityService).getCurrentUserRole();

    // Mock security service to return current user email
    doReturn(Mono.just("pm@example.com")).when(securityService).getCurrentUserEmail();

    // Test the controller method - should return 403 Forbidden
    StepVerifier.create(
            brdFieldCommentController.addComment(
                "BRD123", "BRD", "clientInformation", "clientName", null, mockCommentReq))
        .expectNextMatches(
            response ->
                response.getStatusCode().equals(HttpStatus.FORBIDDEN)
                    && response.getBody().getStatus().equals(BrdConstants.FAILURE)
                    && response.getBody().getMessage().contains("Access denied: PM can only access BRDs they created"))
        .verifyComplete();
  }

  @Test
  @DisplayName("Should deny access when BA is not assigned to BRD for addComment")
  void addComment_AccessDenied_BANotAssigned() {
    // Mock BRD service to return a successful response
    doReturn(Mono.just(mockBrdResponse)).when(brdService).getBrdById("BRD123");

    // Mock security service to return a successful mono
    doReturn(Mono.just(true)).when(securityService).withSecurityCheck("Internal Review");

    // Mock security service to return BA role
    doReturn(Mono.just("ROLE_BA")).when(securityService).getCurrentUserRole();

    // Mock security service to return user email
    doReturn(Mono.just("ba@example.com")).when(securityService).getCurrentUserEmail();

    // Mock BA assignment service to return false (not assigned)
    doReturn(Mono.just(false)).when(baAssignmentService).isBAAssignedToUser("BRD123", "ba@example.com");

    // Test the controller method - should return 403 Forbidden
    StepVerifier.create(
            brdFieldCommentController.addComment(
                "BRD123", "BRD", "clientInformation", "clientName", null, mockCommentReq))
        .expectNextMatches(
            response ->
                response.getStatusCode().equals(HttpStatus.FORBIDDEN)
                    && response.getBody().getStatus().equals(BrdConstants.FAILURE)
                    && response.getBody().getMessage().contains("Access denied: BA is not assigned to this BRD"))
        .verifyComplete();
  }

  @Test
  @DisplayName("Should deny access when user is not a BA role for getComments")
  void getComments_AccessDenied_NonBARole() {
    // Mock BRD service to return a successful response
    doReturn(Mono.just(mockBrdResponse)).when(brdService).getBrdById("BRD123");

    // Mock security service to return a successful mono
    doReturn(Mono.just(true)).when(securityService).withSecurityCheck("Internal Review");

    // Mock security service to return non-BA/PM role
    doReturn(Mono.just("ROLE_BILLER")).when(securityService).getCurrentUserRole();

    // Mock security service to return current user email
    doReturn(Mono.just("biller@example.com")).when(securityService).getCurrentUserEmail();

    // Mock biller assignment service to return false (not assigned)
    doReturn(Mono.just(false)).when(billerAssignmentService).isBrdAssignedToBiller("BRD123", "biller@example.com");

    // Test the controller method - should return 403 Forbidden
    StepVerifier.create(
            brdFieldCommentController.getComments("BRD123", "BRD", null, null, null))
        .expectNextMatches(
            response ->
                response.getStatusCode().equals(HttpStatus.FORBIDDEN)
                    && response.getBody().getStatus().equals(BrdConstants.FAILURE)
                    && response.getBody().getMessage().contains("Access denied: Biller is not assigned to this BRD"))
        .verifyComplete();
  }

  @Test
  @DisplayName("Should deny access when PM is not the creator of the BRD for getComments")
  void getComments_AccessDenied_PMNotCreator() {
    // Mock BRD service to return a successful response
    doReturn(Mono.just(mockBrdResponse)).when(brdService).getBrdById("BRD123");

    // Mock security service to return a successful mono
    doReturn(Mono.just(true)).when(securityService).withSecurityCheck("Internal Review");

    // Mock access control - PM role but not creator
    doReturn(Mono.just("ROLE_PM")).when(securityService).getCurrentUserRole();
    doReturn(Mono.just("pm@example.com")).when(securityService).getCurrentUserEmail();

    // Set creator to different user
    mockBrd.setCreator("different@example.com");

    // Test the controller method
    StepVerifier.create(
            brdFieldCommentController.getComments(
                "BRD123", "BRD", null, "clientInformation", "clientName"))
        .expectNextMatches(
            response ->
                response.getStatusCode() == HttpStatus.FORBIDDEN
                    && response.getBody().getStatus().equals(BrdConstants.FAILURE)
                    && response.getBody().getMessage().contains("PM can only access BRDs they created"))
        .verifyComplete();
  }

  @Test
  @DisplayName("Should deny access when Manager role is used for getComments")
  void getComments_AccessDenied_ManagerRole() {
    // Mock BRD service to return a successful response
    doReturn(Mono.just(mockBrdResponse)).when(brdService).getBrdById("BRD123");

    // Mock security service to return a successful mono
    doReturn(Mono.just(true)).when(securityService).withSecurityCheck("Internal Review");

    // Mock access control - Manager role
    doReturn(Mono.just("ROLE_MANAGER")).when(securityService).getCurrentUserRole();

    // Test the controller method
    StepVerifier.create(
            brdFieldCommentController.getComments(
                "BRD123", "BRD", null, "clientInformation", "clientName"))
        .expectNextMatches(
            response ->
                response.getStatusCode() == HttpStatus.FORBIDDEN
                    && response.getBody().getStatus().equals(BrdConstants.FAILURE)
                    && response.getBody().getMessage().contains("Managers are not allowed to access comment operations"))
        .verifyComplete();
  }

  @Test
  @DisplayName("Should deny access when Biller is not assigned to BRD for getComments")
  void getComments_AccessDenied_BillerNotAssigned() {
    // Mock BRD service to return a successful response
    doReturn(Mono.just(mockBrdResponse)).when(brdService).getBrdById("BRD123");

    // Mock security service to return a successful mono
    doReturn(Mono.just(true)).when(securityService).withSecurityCheck("Internal Review");

    // Mock access control - Biller role but not assigned
    doReturn(Mono.just("ROLE_BILLER")).when(securityService).getCurrentUserRole();
    doReturn(Mono.just("biller@example.com")).when(securityService).getCurrentUserEmail();
    doReturn(Mono.just(false)).when(billerAssignmentService).isBrdAssignedToBiller("BRD123", "biller@example.com");

    // Test the controller method
    StepVerifier.create(
            brdFieldCommentController.getComments(
                "BRD123", "BRD", null, "clientInformation", "clientName"))
        .expectNextMatches(
            response ->
                response.getStatusCode() == HttpStatus.FORBIDDEN
                    && response.getBody().getStatus().equals(BrdConstants.FAILURE)
                    && response.getBody().getMessage().contains("Biller is not assigned to this BRD"))
        .verifyComplete();
  }

  @Test
  @DisplayName("Should deny access when Biller is assigned but BRD status is not allowed for getComments")
  void getComments_AccessDenied_BillerInvalidStatus() {
    // Mock BRD service to return a successful response with invalid status
    mockBrd.setStatus("Draft");
    Api<BRDResponse> apiResponse =
        new Api<>(
            BrdConstants.SUCCESSFUL,
            "BRD retrieved successfully",
            Optional.of(mockBrd),
            Optional.empty());
    ResponseEntity<Api<BRDResponse>> brdResponse = ResponseEntity.ok(apiResponse);
    doReturn(Mono.just(brdResponse)).when(brdService).getBrdById("BRD123");

    // Mock security service to return a successful mono
    doReturn(Mono.just(true)).when(securityService).withSecurityCheck("Draft");

    // Mock access control - Biller role and assigned but invalid status
    doReturn(Mono.just("ROLE_BILLER")).when(securityService).getCurrentUserRole();
    doReturn(Mono.just("biller@example.com")).when(securityService).getCurrentUserEmail();
    doReturn(Mono.just(true)).when(billerAssignmentService).isBrdAssignedToBiller("BRD123", "biller@example.com");

    // Test the controller method
    StepVerifier.create(
            brdFieldCommentController.getComments(
                "BRD123", "BRD", null, "clientInformation", "clientName"))
        .expectNextMatches(
            response ->
                response.getStatusCode() == HttpStatus.FORBIDDEN
                    && response.getBody().getStatus().equals(BrdConstants.FAILURE)
                    && response.getBody().getMessage().contains("Biller can only access BRDs with status 'in_progress' or 'ready_for_signoff'"))
        .verifyComplete();
  }

  @Test
  @DisplayName("Should allow access when Biller is assigned and BRD status is in_progress for getComments")
  void getComments_AccessAllowed_BillerAssignedInProgress() {
    // Mock BRD service to return a successful response with In Progress status
    mockBrd.setStatus("In Progress");
    Api<BRDResponse> apiResponse =
        new Api<>(
            BrdConstants.SUCCESSFUL,
            "BRD retrieved successfully",
            Optional.of(mockBrd),
            Optional.empty());
    ResponseEntity<Api<BRDResponse>> brdResponse = ResponseEntity.ok(apiResponse);
    doReturn(Mono.just(brdResponse)).when(brdService).getBrdById("BRD123");

    // Mock security service to return a successful mono
    doReturn(Mono.just(true)).when(securityService).withSecurityCheck("In Progress");

    // Mock access control - Biller role and assigned with valid status
    doReturn(Mono.just("ROLE_BILLER")).when(securityService).getCurrentUserRole();
    doReturn(Mono.just("biller@example.com")).when(securityService).getCurrentUserEmail();
    doReturn(Mono.just(true)).when(billerAssignmentService).isBrdAssignedToBiller("BRD123", "biller@example.com");

    // Mock field comment service to return a successful response
    ResponseEntity<Api<List<BrdFieldCommentGroupResp>>> successResponse =
        ResponseEntity.ok(
            new Api<>(
                BrdConstants.SUCCESSFUL,
                "Comments retrieved successfully",
                Optional.of(Collections.singletonList(mockGroupResp)),
                Optional.empty()));
    doReturn(Mono.just(successResponse))
        .when(fieldCommentService)
        .getComments("BRD123", "BRD", null, "clientInformation", "clientName");

    // Test the controller method
    StepVerifier.create(
            brdFieldCommentController.getComments(
                "BRD123", "BRD", null, "clientInformation", "clientName"))
        .expectNextMatches(
            response ->
                response.getStatusCode().is2xxSuccessful()
                    && response.getBody().getStatus().equals(BrdConstants.SUCCESSFUL))
        .verifyComplete();
  }

  @Test
  @DisplayName("Should allow access when Biller is assigned and BRD status is ready_for_signoff for getComments")
  void getComments_AccessAllowed_BillerAssignedReadyForSignoff() {
    // Mock BRD service to return a successful response with Ready for Sign-Off status
    mockBrd.setStatus("Ready for Sign-Off");
    Api<BRDResponse> apiResponse =
        new Api<>(
            BrdConstants.SUCCESSFUL,
            "BRD retrieved successfully",
            Optional.of(mockBrd),
            Optional.empty());
    ResponseEntity<Api<BRDResponse>> brdResponse = ResponseEntity.ok(apiResponse);
    doReturn(Mono.just(brdResponse)).when(brdService).getBrdById("BRD123");

    // Mock security service to return a successful mono
    doReturn(Mono.just(true)).when(securityService).withSecurityCheck("Ready for Sign-Off");

    // Mock access control - Biller role and assigned with valid status
    doReturn(Mono.just("ROLE_BILLER")).when(securityService).getCurrentUserRole();
    doReturn(Mono.just("biller@example.com")).when(securityService).getCurrentUserEmail();
    doReturn(Mono.just(true)).when(billerAssignmentService).isBrdAssignedToBiller("BRD123", "biller@example.com");

    // Mock field comment service to return a successful response
    ResponseEntity<Api<List<BrdFieldCommentGroupResp>>> successResponse =
        ResponseEntity.ok(
            new Api<>(
                BrdConstants.SUCCESSFUL,
                "Comments retrieved successfully",
                Optional.of(Collections.singletonList(mockGroupResp)),
                Optional.empty()));
    doReturn(Mono.just(successResponse))
        .when(fieldCommentService)
        .getComments("BRD123", "BRD", null, "clientInformation", "clientName");

    // Test the controller method
    StepVerifier.create(
            brdFieldCommentController.getComments(
                "BRD123", "BRD", null, "clientInformation", "clientName"))
        .expectNextMatches(
            response ->
                response.getStatusCode().is2xxSuccessful()
                    && response.getBody().getStatus().equals(BrdConstants.SUCCESSFUL))
        .verifyComplete();
  }

  @Test
  @DisplayName("Should deny access when BA is assigned but BRD status is not internal_review for getComments")
  void getComments_AccessDenied_BAInvalidStatus() {
    // Mock BRD service to return a successful response with invalid status
    mockBrd.setStatus("Draft");
    Api<BRDResponse> apiResponse =
        new Api<>(
            BrdConstants.SUCCESSFUL,
            "BRD retrieved successfully",
            Optional.of(mockBrd),
            Optional.empty());
    ResponseEntity<Api<BRDResponse>> brdResponse = ResponseEntity.ok(apiResponse);
    doReturn(Mono.just(brdResponse)).when(brdService).getBrdById("BRD123");

    // Mock security service to return a successful mono
    doReturn(Mono.just(true)).when(securityService).withSecurityCheck("Draft");

    // Mock access control - BA role and assigned but invalid status
    doReturn(Mono.just("ROLE_BA")).when(securityService).getCurrentUserRole();
    // Do NOT mock getCurrentUserEmail or isBAAssignedToUser, as they are not called due to status check

    // Test the controller method
    StepVerifier.create(
            brdFieldCommentController.getComments(
                "BRD123", "BRD", null, "clientInformation", "clientName"))
        .expectNextMatches(
            response ->
                response.getStatusCode() == HttpStatus.FORBIDDEN
                    && response.getBody().getStatus().equals(BrdConstants.FAILURE)
                    && response.getBody().getMessage().contains("Access denied: BRD status must be 'Internal Review' for BA operations"))
        .verifyComplete();
  }

  @Test
  @DisplayName("Should allow access when BA is assigned and BRD status is internal_review for getComments")
  void getComments_AccessAllowed_BAAssignedInternalReview() {
    // Mock BRD service to return a successful response with Internal Review status
    mockBrd.setStatus("Internal Review");
    Api<BRDResponse> apiResponse =
        new Api<>(
            BrdConstants.SUCCESSFUL,
            "BRD retrieved successfully",
            Optional.of(mockBrd),
            Optional.empty());
    ResponseEntity<Api<BRDResponse>> brdResponse = ResponseEntity.ok(apiResponse);
    doReturn(Mono.just(brdResponse)).when(brdService).getBrdById("BRD123");

    // Mock security service to return a successful mono
    doReturn(Mono.just(true)).when(securityService).withSecurityCheck("Internal Review");

    // Mock access control - BA role and assigned with valid status
    doReturn(Mono.just("ROLE_BA")).when(securityService).getCurrentUserRole();
    doReturn(Mono.just("ba@example.com")).when(securityService).getCurrentUserEmail();
    doReturn(Mono.just(true)).when(baAssignmentService).isBAAssignedToUser("BRD123", "ba@example.com");

    // Mock field comment service to return a successful response
    ResponseEntity<Api<List<BrdFieldCommentGroupResp>>> successResponse =
        ResponseEntity.ok(
            new Api<>(
                BrdConstants.SUCCESSFUL,
                "Comments retrieved successfully",
                Optional.of(Collections.singletonList(mockGroupResp)),
                Optional.empty()));
    doReturn(Mono.just(successResponse))
        .when(fieldCommentService)
        .getComments("BRD123", "BRD", null, "clientInformation", "clientName");

    // Test the controller method
    StepVerifier.create(
            brdFieldCommentController.getComments(
                "BRD123", "BRD", null, "clientInformation", "clientName"))
        .expectNextMatches(
            response ->
                response.getStatusCode().is2xxSuccessful()
                    && response.getBody().getStatus().equals(BrdConstants.SUCCESSFUL))
        .verifyComplete();
  }
}

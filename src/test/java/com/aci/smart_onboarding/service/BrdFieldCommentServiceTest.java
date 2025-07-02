package com.aci.smart_onboarding.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.aci.smart_onboarding.constants.BrdConstants;
import com.aci.smart_onboarding.dto.*;
import com.aci.smart_onboarding.exception.AlreadyExistException;
import com.aci.smart_onboarding.exception.BadRequestException;
import com.aci.smart_onboarding.exception.NotFoundException;
import com.aci.smart_onboarding.mapper.DtoModelMapper;
import com.aci.smart_onboarding.model.BrdFieldCommentGroup;
import com.aci.smart_onboarding.model.BrdFieldCommentGroup.CommentEntry;
import com.aci.smart_onboarding.model.Site;
import com.aci.smart_onboarding.repository.BrdFieldCommentGroupRepository;
import com.aci.smart_onboarding.service.implementation.BrdFieldCommentService;
import java.time.LocalDateTime;
import java.util.*;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.ReactiveUpdateOperation;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import com.mongodb.client.result.UpdateResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class BrdFieldCommentServiceTest {

  @Mock private BrdFieldCommentGroupRepository commentGroupRepository;

  @Mock private DtoModelMapper dtoModelMapper;

  @Mock private ISequenceGeneratorService sequenceGeneratorService;

  @Mock private ReactiveMongoTemplate mongoTemplate;

  @Mock private IBRDService brdService;

  @Mock private IAuditLogService auditLogService;

  @InjectMocks private BrdFieldCommentService brdFieldCommentService;

  private BrdFieldCommentGroup testCommentGroup;
  private BrdFieldCommentGroupResp testCommentGroupResp;
  private BrdFieldCommentGroupReq testCommentGroupReq;
  private CommentEntryReq testCommentEntryReq;
  private CommentEntryResp testCommentEntryResp;
  private ShadowValueUpdateDTO testUpdateDTO;
  private List<BrdFieldCommentGroup> pendingBrdCommentGroups;
  private List<BrdFieldCommentGroup> pendingSiteCommentGroups;
  private UpdateCommentReadStatusRequest testReadStatusRequest;

  @BeforeEach
  void setUp() {
    testCommentGroup =
        BrdFieldCommentGroup.builder()
            .id("test-id")
            .brdFormId("brd-1")
            .sourceType("BRD")
            .fieldPath("test.field")
            .sectionName("testSection")
            .status("Pending")
            .comments(new ArrayList<>())
            .build();

    testCommentGroupResp =
        BrdFieldCommentGroupResp.builder()
            .id("test-id")
            .brdFormId("brd-1")
            .sourceType("BRD")
            .fieldPath("test.field")
            .sectionName("testSection")
            .status("Pending")
            .comments(new ArrayList<>())
            .build();

    testCommentGroupReq =
        BrdFieldCommentGroupReq.builder()
            .brdFormId("brd-1")
            .sourceType("BRD")
            .fieldPath("test.field")
            .sectionName("testSection")
            .status("Pending")
            .build();

    testCommentEntryReq =
        CommentEntryReq.builder()
            .content("Test comment")
            .createdBy("user1")
            .userType("REVIEWER")
            .build();

    testCommentEntryResp =
        CommentEntryResp.builder()
            .id("comment-1")
            .content("Test comment")
            .createdBy("user1")
            .build();

    testReadStatusRequest = new UpdateCommentReadStatusRequest();
    testReadStatusRequest.setCommentId("comment-1");
    testReadStatusRequest.setIsRead(true);

    testUpdateDTO =
        ShadowValueUpdateDTO.builder()
            .brdFormId("brd-1")
            .sourceType("BRD")
            .sectionName("clientInformation")
            .fieldPath("test.field")
            .build();

    // Setup pending comment group data for the new tests
    pendingBrdCommentGroups = new ArrayList<>();
    pendingBrdCommentGroups.add(
        BrdFieldCommentGroup.builder()
            .id("brd-comment-1")
            .brdFormId("brd-1")
            .sourceType(BrdConstants.SOURCE_TYPE_BRD)
            .fieldPath("field1")
            .sectionName("section1")
            .status(BrdConstants.COMMENT_STATUS_PENDING)
            .comments(new ArrayList<>())
            .build());
    pendingBrdCommentGroups.add(
        BrdFieldCommentGroup.builder()
            .id("brd-comment-2")
            .brdFormId("brd-1")
            .sourceType(BrdConstants.SOURCE_TYPE_BRD)
            .fieldPath("field2")
            .sectionName("section2")
            .status(BrdConstants.COMMENT_STATUS_PENDING)
            .comments(new ArrayList<>())
            .build());

    pendingSiteCommentGroups = new ArrayList<>();
    pendingSiteCommentGroups.add(
        BrdFieldCommentGroup.builder()
            .id("site-comment-1")
            .brdFormId("brd-1")
            .siteId("site-1")
            .sourceType(BrdConstants.SOURCE_TYPE_SITE)
            .fieldPath("field1")
            .sectionName("section1")
            .status(BrdConstants.COMMENT_STATUS_PENDING)
            .comments(new ArrayList<>())
            .build());
    pendingSiteCommentGroups.add(
        BrdFieldCommentGroup.builder()
            .id("site-comment-2")
            .brdFormId("brd-1")
            .siteId("site-1")
            .sourceType(BrdConstants.SOURCE_TYPE_SITE)
            .fieldPath("field2")
            .sectionName("section2")
            .status(BrdConstants.COMMENT_STATUS_PENDING)
            .comments(new ArrayList<>())
            .build());
    pendingSiteCommentGroups.add(
        BrdFieldCommentGroup.builder()
            .id("site-comment-3")
            .brdFormId("brd-1")
            .siteId("site-2")
            .sourceType(BrdConstants.SOURCE_TYPE_SITE)
            .fieldPath("field1")
            .sectionName("section1")
            .status(BrdConstants.COMMENT_STATUS_PENDING)
            .comments(new ArrayList<>())
            .build());
  }

  @Test
  void createOrUpdateFieldCommentGroup_WhenBRDSourceType_Success() {
    when(commentGroupRepository.findByBrdFormIdAndSourceTypeAndSectionNameAndFieldPath(
            anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Mono.empty());
    when(commentGroupRepository.save(any(BrdFieldCommentGroup.class)))
        .thenReturn(Mono.just(testCommentGroup));
    when(dtoModelMapper.mapToGroupResponse(any(BrdFieldCommentGroup.class)))
        .thenReturn(testCommentGroupResp);

    StepVerifier.create(brdFieldCommentService.createOrUpdateFieldCommentGroup(testCommentGroupReq))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertTrue(response.getBody().getData().isPresent());
              assertEquals("test-id", response.getBody().getData().get().getId());
            })
        .verifyComplete();
  }

  @Test
  void createOrUpdateFieldCommentGroup_WhenExistingGroup_UpdatesSuccessfully() {
    when(commentGroupRepository.findByBrdFormIdAndSourceTypeAndSectionNameAndFieldPath(
            anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Mono.just(testCommentGroup));
    when(commentGroupRepository.save(any(BrdFieldCommentGroup.class)))
        .thenReturn(Mono.just(testCommentGroup));
    when(dtoModelMapper.mapToGroupResponse(any(BrdFieldCommentGroup.class)))
        .thenReturn(testCommentGroupResp);

    testCommentGroupReq.setFieldPathShadowValue("new value");

    StepVerifier.create(brdFieldCommentService.createOrUpdateFieldCommentGroup(testCommentGroupReq))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertTrue(response.getBody().getData().isPresent());
            })
        .verifyComplete();
  }

  @Test
  void createOrUpdateFieldCommentGroup_WhenSiteSourceType_Success() {
    testCommentGroupReq.setSourceType("SITE");
    testCommentGroupReq.setSiteId("site-1");

    when(commentGroupRepository.findByBrdFormIdAndSiteIdAndSourceTypeAndSectionNameAndFieldPath(
            anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Mono.empty());
    when(commentGroupRepository.save(any(BrdFieldCommentGroup.class)))
        .thenReturn(Mono.just(testCommentGroup));
    when(dtoModelMapper.mapToGroupResponse(any(BrdFieldCommentGroup.class)))
        .thenReturn(testCommentGroupResp);

    StepVerifier.create(brdFieldCommentService.createOrUpdateFieldCommentGroup(testCommentGroupReq))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertTrue(response.getBody().getData().isPresent());
            })
        .verifyComplete();
  }

  @Test
  void createOrUpdateFieldCommentGroup_WhenSiteSourceTypeWithoutSiteId_ThrowsBadRequest() {
    testCommentGroupReq.setSourceType("SITE");
    testCommentGroupReq.setSiteId(null);

    StepVerifier.create(brdFieldCommentService.createOrUpdateFieldCommentGroup(testCommentGroupReq))
        .expectError(BadRequestException.class)
        .verify();
  }

  @Test
  void addComment_Success() {
    CommentEntry newComment =
        CommentEntry.builder()
            .id("comment-1")
            .content("Test comment")
            .createdBy("user1")
            .userType("REVIEWER")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    testCommentGroup.setComments(new ArrayList<>(Collections.singletonList(newComment)));

    when(commentGroupRepository.findByBrdFormIdAndSourceTypeAndSectionNameAndFieldPath(
            anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Mono.just(testCommentGroup));
    when(sequenceGeneratorService.generateCommentId(anyString(), anyString(), anyString()))
        .thenReturn(Mono.just("comment-1"));
    when(commentGroupRepository.save(any(BrdFieldCommentGroup.class)))
        .thenReturn(Mono.just(testCommentGroup));
    when(dtoModelMapper.mapToCommentResponse(any(CommentEntry.class)))
        .thenReturn(testCommentEntryResp);

    System.out.println("Comments before add: " + testCommentGroup.getComments());

    StepVerifier.create(
            brdFieldCommentService.addComment(
                "brd-1", "testSection", "test.field", testCommentEntryReq, "BRD", null))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.CREATED, response.getStatusCode());
              assertTrue(response.getBody().getData().isPresent());
              assertEquals("comment-1", response.getBody().getData().get().getId());
            })
        .verifyComplete();
  }

  @Test
  void addComment_WithInvalidSourceType_ThrowsBadRequest() {
    StepVerifier.create(
            brdFieldCommentService.addComment(
                "brd-1", "testSection", "test.field", testCommentEntryReq, "INVALID", null))
        .expectError(BadRequestException.class)
        .verify();
  }

  @Test
  void addComment_WithSiteSourceTypeNoSiteId_ThrowsBadRequest() {
    StepVerifier.create(
            brdFieldCommentService.addComment(
                "brd-1", "testSection", "test.field", testCommentEntryReq, "SITE", null))
        .expectError(BadRequestException.class)
        .verify();
  }

  @Test
  void addComment_WhenCommentLimitReached_ThrowsBadRequest() {
    List<CommentEntry> existingComments = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      existingComments.add(CommentEntry.builder().id("comment-" + i).userType("REVIEWER").build());
    }
    testCommentGroup.setComments(existingComments);

    when(commentGroupRepository.findByBrdFormIdAndSourceTypeAndSectionNameAndFieldPath(
            anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Mono.just(testCommentGroup));

    StepVerifier.create(
            brdFieldCommentService.addComment(
                "brd-1", "testSection", "test.field", testCommentEntryReq, "BRD", null))
        .expectError(BadRequestException.class)
        .verify();
  }

  @Test
  void getCommentGroupsByBrdFormId_Success() {
    when(commentGroupRepository.findByBrdFormIdAndSourceType(anyString(), anyString()))
        .thenReturn(Flux.just(testCommentGroup));
    when(dtoModelMapper.mapToGroupResponse(any(BrdFieldCommentGroup.class)))
        .thenReturn(testCommentGroupResp);

    StepVerifier.create(brdFieldCommentService.getCommentGroupsByBrdFormId("brd-1", "BRD"))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertTrue(response.getBody().getData().isPresent());
              assertEquals(1, response.getBody().getData().get().size());
            })
        .verifyComplete();
  }

  @Test
  void getCommentGroupsByBrdFormId_WhenNoGroups_ReturnsEmptyList() {
    when(commentGroupRepository.findByBrdFormIdAndSourceType(anyString(), anyString()))
        .thenReturn(Flux.empty());

    StepVerifier.create(brdFieldCommentService.getCommentGroupsByBrdFormId("brd-1", "BRD"))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertTrue(response.getBody().getData().isPresent());
              assertTrue(response.getBody().getData().get().isEmpty());
            })
        .verifyComplete();
  }

  @Test
  void getCommentGroupsBySiteId_Success() {
    when(commentGroupRepository.findByBrdFormIdAndSiteIdAndSourceType(
            anyString(), anyString(), anyString()))
        .thenReturn(Flux.just(testCommentGroup));
    when(dtoModelMapper.mapToGroupResponse(any(BrdFieldCommentGroup.class)))
        .thenReturn(testCommentGroupResp);

    StepVerifier.create(brdFieldCommentService.getCommentGroupsBySiteId("brd-1", "site-1"))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertTrue(response.getBody().getData().isPresent());
              assertEquals(1, response.getBody().getData().get().size());
            })
        .verifyComplete();
  }

  @Test
  void getCommentGroup_Success() {
    when(commentGroupRepository.findByBrdFormIdAndSourceTypeAndSectionNameAndFieldPath(
            anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Mono.just(testCommentGroup));
    when(dtoModelMapper.mapToGroupResponse(any(BrdFieldCommentGroup.class)))
        .thenReturn(testCommentGroupResp);

    StepVerifier.create(
            brdFieldCommentService.getCommentGroup(
                "brd-1", "BRD", null, "testSection", "test.field"))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertTrue(response.getBody().getData().isPresent());
              assertEquals("test-id", response.getBody().getData().get().getId());
            })
        .verifyComplete();
  }

  @Test
  void getCommentGroup_WhenNotFound_ReturnsNotFound() {
    when(commentGroupRepository.findByBrdFormIdAndSourceTypeAndSectionNameAndFieldPath(
            anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Mono.empty());

    StepVerifier.create(
            brdFieldCommentService.getCommentGroup(
                "brd-1", "BRD", null, "testSection", "test.field"))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
              assertFalse(response.getBody().getData().isPresent());
              assertTrue(response.getBody().getErrors().isPresent());
            })
        .verifyComplete();
  }

  @Test
  void getCommentGroupsBySection_Success() {
    when(commentGroupRepository.findByBrdFormIdAndSourceTypeAndSectionName(
            anyString(), anyString(), anyString()))
        .thenReturn(Flux.just(testCommentGroup));
    when(dtoModelMapper.mapToGroupResponse(any(BrdFieldCommentGroup.class)))
        .thenReturn(testCommentGroupResp);

    StepVerifier.create(
            brdFieldCommentService.getCommentGroupsBySection("brd-1", "BRD", null, "testSection"))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertTrue(response.getBody().getData().isPresent());
              assertEquals(1, response.getBody().getData().get().size());
            })
        .verifyComplete();
  }

  @Test
  void getCommentGroupsBySection_WhenSiteSourceTypeNoSiteId_ThrowsBadRequest() {
    StepVerifier.create(
            brdFieldCommentService.getCommentGroupsBySection("brd-1", "SITE", null, "testSection"))
        .expectError(BadRequestException.class)
        .verify();
  }

  @Test
  void getComments_WithAllParameters_Success() {
    when(commentGroupRepository.findByBrdFormIdAndSourceTypeAndSectionNameAndFieldPath(
            anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Mono.just(testCommentGroup));
    when(dtoModelMapper.mapToGroupResponse(any(BrdFieldCommentGroup.class)))
        .thenReturn(testCommentGroupResp);

    StepVerifier.create(
            brdFieldCommentService.getComments("brd-1", "BRD", null, "testSection", "test.field"))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertTrue(response.getBody().getData().isPresent());
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("getComments should handle invalid sourceType")
  void getComments_WithInvalidSourceType_ThrowsBadRequest() {
    StepVerifier.create(
            brdFieldCommentService.getComments("brd-1", "INVALID", null, "testSection", "test.field"))
        .expectError(BadRequestException.class)
        .verify();
  }

  @Test
  @DisplayName("getComments should handle SITE sourceType without siteId")
  void getComments_WithSiteSourceTypeNoSiteId_ThrowsBadRequest() {
    StepVerifier.create(
            brdFieldCommentService.getComments("brd-1", "SITE", null, "testSection", "test.field"))
        .expectError(BadRequestException.class)
        .verify();
  }

  @Test
  @DisplayName("getComments should handle sectionName only (without fieldPath)")
  void getComments_WithSectionNameOnly_Success() {
    when(commentGroupRepository.findByBrdFormIdAndSourceTypeAndSectionName(
            anyString(), anyString(), anyString()))
        .thenReturn(Flux.just(testCommentGroup));
    when(dtoModelMapper.mapToGroupResponse(any(BrdFieldCommentGroup.class)))
        .thenReturn(testCommentGroupResp);

    StepVerifier.create(
            brdFieldCommentService.getComments("brd-1", "BRD", null, "testSection", null))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertTrue(response.getBody().getData().isPresent());
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("getComments should handle BRD sourceType without sectionName and fieldPath")
  void getComments_WithBRDSourceTypeOnly_Success() {
    when(commentGroupRepository.findByBrdFormIdAndSourceType(anyString(), anyString()))
        .thenReturn(Flux.just(testCommentGroup));
    when(dtoModelMapper.mapToGroupResponse(any(BrdFieldCommentGroup.class)))
        .thenReturn(testCommentGroupResp);

    StepVerifier.create(
            brdFieldCommentService.getComments("brd-1", "BRD", null, null, null))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertTrue(response.getBody().getData().isPresent());
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("getComments should handle SITE sourceType without sectionName and fieldPath")
  void getComments_WithSiteSourceTypeOnly_Success() {
    when(commentGroupRepository.findByBrdFormIdAndSiteIdAndSourceType(
            anyString(), anyString(), anyString()))
        .thenReturn(Flux.just(testCommentGroup));
    when(dtoModelMapper.mapToGroupResponse(any(BrdFieldCommentGroup.class)))
        .thenReturn(testCommentGroupResp);

    StepVerifier.create(
            brdFieldCommentService.getComments("brd-1", "SITE", "site-1", null, null))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertTrue(response.getBody().getData().isPresent());
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("getComments should handle getCommentGroup returning null data")
  void getComments_WithGetCommentGroupNullData_Success() {
    when(commentGroupRepository.findByBrdFormIdAndSourceTypeAndSectionNameAndFieldPath(
            anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Mono.empty());

    StepVerifier.create(
            brdFieldCommentService.getComments("brd-1", "BRD", null, "testSection", "test.field"))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertTrue(response.getBody().getData().isPresent());
              assertTrue(response.getBody().getData().get().isEmpty());
            })
        .verifyComplete();
  }

  @Test
  void updateEntityFieldFromShadowValue_WhenInvalidSourceType_ThrowsBadRequest() {
    testUpdateDTO.setSourceType("INVALID");

    StepVerifier.create(brdFieldCommentService.updateEntityFieldFromShadowValue(testUpdateDTO))
        .expectError(BadRequestException.class)
        .verify();
  }

  @Test
  void updateEntityFieldFromShadowValue_WhenSiteSourceTypeNoSiteId_ThrowsBadRequest() {
    testUpdateDTO.setSourceType("SITE");
    testUpdateDTO.setSiteId(null);

    StepVerifier.create(brdFieldCommentService.updateEntityFieldFromShadowValue(testUpdateDTO))
        .expectError(BadRequestException.class)
        .verify();
  }

  @Test
  void updateEntityFieldFromShadowValue_WhenGroupNotFound_ThrowsNotFoundException() {
    when(commentGroupRepository.findByBrdFormIdAndSourceTypeAndSectionNameAndFieldPath(
            anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Mono.empty());

    StepVerifier.create(brdFieldCommentService.updateEntityFieldFromShadowValue(testUpdateDTO))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void updateEntityFieldFromShadowValue_WhenNoShadowValue_ThrowsBadRequest() {
    testCommentGroup.setFieldPathShadowValue(null);

    when(commentGroupRepository.findByBrdFormIdAndSourceTypeAndSectionNameAndFieldPath(
            anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Mono.just(testCommentGroup));

    StepVerifier.create(brdFieldCommentService.updateEntityFieldFromShadowValue(testUpdateDTO))
        .expectError(BadRequestException.class)
        .verify();
  }

  @Test
  void updateEntityFieldFromShadowValue_ForBRD_Success() {
    testCommentGroup.setFieldPathShadowValue("new value");
    Document mockDocument = new Document();
    mockDocument.put("clientInformation", new Document());

    BRDResponse brdResponse =
        BRDResponse.builder().brdFormId("brd-1").status("IN_PROGRESS").build();

    when(commentGroupRepository.findByBrdFormIdAndSourceTypeAndSectionNameAndFieldPath(
            anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Mono.just(testCommentGroup));
    when(mongoTemplate.findOne(any(Query.class), eq(Document.class), eq("brd")))
        .thenReturn(Mono.just(mockDocument));
    when(brdService.updateBrdPartiallyWithOrderedOperations(anyString(), anyMap()))
        .thenReturn(
            Mono.just(
                ResponseEntity.ok()
                    .body(
                        new Api<>(
                            BrdConstants.SUCCESSFUL,
                            "success",
                            Optional.of(brdResponse),
                            Optional.empty()))));
    when(commentGroupRepository.save(any(BrdFieldCommentGroup.class)))
        .thenReturn(Mono.just(testCommentGroup));

    StepVerifier.create(brdFieldCommentService.updateEntityFieldFromShadowValue(testUpdateDTO))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertTrue(response.getBody().getData().isPresent());
              assertTrue(response.getBody().getData().get());
            })
        .verifyComplete();
  }

  @Test
  void handleErrors_WithBadRequestException_ReturnsSameException() {
    BadRequestException ex = new BadRequestException("Test error");
    Throwable result = brdFieldCommentService.handleErrors(ex);
    assertEquals(ex, result);
  }

  @Test
  void handleErrors_WithNotFoundException_ReturnsSameException() {
    NotFoundException ex = new NotFoundException("Test error");
    Throwable result = brdFieldCommentService.handleErrors(ex);
    assertEquals(ex, result);
  }

  @Test
  void handleErrors_WithUnknownException_ReturnsWrappedException() {
    RuntimeException ex = new RuntimeException("Test error");
    Throwable result = brdFieldCommentService.handleErrors(ex);
    assertTrue(result instanceof Exception);
    assertTrue(result.getMessage().startsWith("Something went wrong:"));
  }

  @Test
  @DisplayName("getCommentStatsByStatus should return statistics for comments by status")
  void getCommentStatsByStatus_Success() {
    String brdFormId = "brd-1";
    String status = "Pending";
    List<BrdFieldCommentGroup> allCommentGroups = new ArrayList<>();
    allCommentGroups.addAll(pendingBrdCommentGroups);
    allCommentGroups.addAll(pendingSiteCommentGroups);

    // Mock the MongoDB query to find comment groups by status
    when(mongoTemplate.find(any(Query.class), eq(BrdFieldCommentGroup.class)))
        .thenReturn(Flux.fromIterable(allCommentGroups));

    // Mock the mapper to return responses for the BRD comment groups
    for (BrdFieldCommentGroup group : pendingBrdCommentGroups) {
      when(dtoModelMapper.mapToGroupResponse(group))
          .thenReturn(
              BrdFieldCommentGroupResp.builder()
                  .id(group.getId())
                  .brdFormId(brdFormId)
                  .sourceType(BrdConstants.SOURCE_TYPE_BRD)
                  .status(status)
                  .comments(new ArrayList<>())
                  .build());
    }

    // Mock the mapper to return responses for the Site comment groups
    for (BrdFieldCommentGroup group : pendingSiteCommentGroups) {
      when(dtoModelMapper.mapToGroupResponse(group))
          .thenReturn(
              BrdFieldCommentGroupResp.builder()
                  .id(group.getId())
                  .brdFormId(brdFormId)
                  .siteId(group.getSiteId())
                  .sourceType(BrdConstants.SOURCE_TYPE_SITE)
                  .status(status)
                  .comments(new ArrayList<>())
                  .build());
    }

    // Test the method
    StepVerifier.create(brdFieldCommentService.getCommentStatsByStatus(brdFormId, status))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertTrue(response.getBody().getData().isPresent());
              CommentStatsByStatusResponse stats = response.getBody().getData().get();

              // Verify total counts
              assertEquals(5, stats.getTotalCount()); // 2 BRD + 3 Site
              assertEquals(2, stats.getBrdCount());

              // Verify BRD comments
              assertEquals(2, stats.getBrdComments().size());

              // Verify site counts
              Map<String, Integer> siteCounts = stats.getSiteCounts();
              assertEquals(2, siteCounts.size());
              assertEquals(2, siteCounts.get("site-1").intValue());
              assertEquals(1, siteCounts.get("site-2").intValue());

              // Verify site comments
              Map<String, List<BrdFieldCommentGroupResp>> siteComments = stats.getSiteComments();
              assertEquals(2, siteComments.size());
              assertEquals(2, siteComments.get("site-1").size());
              assertEquals(1, siteComments.get("site-2").size());
            })
        .verifyComplete();

    // Verify that the MongoDB query was built correctly
    verify(mongoTemplate)
        .find(
            argThat(
                query ->
                    query.getQueryObject().get("brdFormId").toString().contains(brdFormId)
                        && query.getQueryObject().get("status").toString().contains(status)),
            eq(BrdFieldCommentGroup.class));
  }

  @Test
  @DisplayName("getCommentStatsByStatus should return empty response when no comments found")
  void getCommentStatsByStatus_EmptyResponse() {
    String brdFormId = "brd-1";
    String status = "Pending";

    // Mock the MongoDB query to find no comment groups
    when(mongoTemplate.find(any(Query.class), eq(BrdFieldCommentGroup.class)))
        .thenReturn(Flux.empty());

    // Test the method
    StepVerifier.create(brdFieldCommentService.getCommentStatsByStatus(brdFormId, status))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertTrue(response.getBody().getData().isPresent());
              CommentStatsByStatusResponse stats = response.getBody().getData().get();

              // Verify empty response
              assertEquals(0, stats.getTotalCount());
              assertEquals(0, stats.getBrdCount());
              assertTrue(stats.getBrdComments().isEmpty());
              assertTrue(stats.getSiteCounts().isEmpty());
              assertTrue(stats.getSiteComments().isEmpty());

              // Verify message contains status
              assertTrue(response.getBody().getMessage().contains("No " + status.toLowerCase()));
            })
        .verifyComplete();
  }

  @Test
  @DisplayName(
      "getCommentStatsByStatus should throw BadRequestException when brdFormId is null or blank")
  void getCommentStatsByStatus_WithNullBrdFormId_ThrowsBadRequest() {
    String status = "Pending";

    // Test with null brdFormId
    StepVerifier.create(brdFieldCommentService.getCommentStatsByStatus(null, status))
        .expectError(BadRequestException.class)
        .verify();

    // Test with blank brdFormId
    StepVerifier.create(brdFieldCommentService.getCommentStatsByStatus("  ", status))
        .expectError(BadRequestException.class)
        .verify();
  }

  @Test
  @DisplayName("getCommentStatsByStatus should throw BadRequestException with invalid status")
  void getCommentStatsByStatus_WithInvalidStatus_ThrowsBadRequest() {
    String brdFormId = "brd-1";

    // Test with null status
    StepVerifier.create(brdFieldCommentService.getCommentStatsByStatus(brdFormId, null))
        .expectError(BadRequestException.class)
        .verify();

    // Test with blank status
    StepVerifier.create(brdFieldCommentService.getCommentStatsByStatus(brdFormId, ""))
        .expectError(BadRequestException.class)
        .verify();

    // Test with invalid status
    StepVerifier.create(brdFieldCommentService.getCommentStatsByStatus(brdFormId, "InvalidStatus"))
        .expectError(BadRequestException.class)
        .verify();
  }

  @Test
  @DisplayName("getCommentStatsByStatus should handle errors properly")
  void getCommentStatsByStatus_HandlesErrors() {
    String brdFormId = "brd-1";
    String status = "Pending";

    // Mock the MongoDB query to throw an exception
    RuntimeException testException = new RuntimeException("Test MongoDB error");
    when(mongoTemplate.find(any(Query.class), eq(BrdFieldCommentGroup.class)))
        .thenReturn(Flux.error(testException));

    // Test the method
    StepVerifier.create(brdFieldCommentService.getCommentStatsByStatus(brdFormId, status))
        .expectErrorSatisfies(
            error -> {
              assertTrue(error instanceof Exception);
              assertTrue(
                  error
                      .getMessage()
                      .contains("Error retrieving " + status.toLowerCase() + " comment stats"));
              assertTrue(error.getMessage().contains("Test MongoDB error"));
            })
        .verify();
  }

  @Test
  @DisplayName("createEmptyResponse should create a properly initialized empty response")
  void createEmptyResponse_ReturnsValidObject() {
    // Invoke the method
    CommentStatsByStatusResponse response = brdFieldCommentService.createEmptyResponse();

    // Verify the response
    assertNotNull(response);
    assertEquals(0, response.getTotalCount());
    assertEquals(0, response.getBrdCount());
    assertNotNull(response.getBrdComments());
    assertTrue(response.getBrdComments().isEmpty());
    assertNotNull(response.getSiteCounts());
    assertTrue(response.getSiteCounts().isEmpty());
    assertNotNull(response.getSiteComments());
    assertTrue(response.getSiteComments().isEmpty());
  }

  @Test
  @DisplayName("updateCommentReadStatus should successfully update BRD comment read status")
  void updateCommentReadStatus_BRD_Success() {
    // Arrange
    String brdFormId = "brd-1";
    String sourceType = BrdConstants.SOURCE_TYPE_BRD;
    String sectionName = "section1";
    String fieldPath = "field1";

    BrdFieldCommentGroup commentGroup = new BrdFieldCommentGroup();
    commentGroup.setId("group-1");
    commentGroup.setBrdFormId(brdFormId);
    commentGroup.setSourceType(sourceType);
    commentGroup.setSectionName(sectionName);
    commentGroup.setFieldPath(fieldPath);

    CommentEntry comment = new CommentEntry();
    comment.setId(testReadStatusRequest.getCommentId());
    comment.setIsRead(false);
    comment.setCreatedAt(LocalDateTime.now().minusDays(1));
    comment.setUpdatedAt(LocalDateTime.now().minusDays(1));

    commentGroup.setComments(List.of(comment));

    // Use doReturn instead of when for proper stubbing
    doReturn(Mono.just(commentGroup))
        .when(commentGroupRepository)
        .findByBrdFormIdAndSourceTypeAndSectionNameAndFieldPath(
            anyString(), anyString(), anyString(), anyString());

    doReturn(Mono.just(commentGroup))
        .when(commentGroupRepository)
        .save(any(BrdFieldCommentGroup.class));

    // Act & Assert
    StepVerifier.create(
            brdFieldCommentService.updateCommentReadStatus(
                brdFormId, sourceType, null, sectionName, fieldPath, testReadStatusRequest))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertNotNull(response.getBody());
              assertTrue(response.getBody().getData().isPresent());
              assertTrue(response.getBody().getData().get());
              assertEquals(
                  "Comment read status updated successfully", response.getBody().getMessage());
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("updateCommentReadStatus should successfully update SITE comment read status")
  void updateCommentReadStatus_SITE_Success() {
    // Arrange
    String brdFormId = "brd-1";
    String sourceType = BrdConstants.SOURCE_TYPE_SITE;
    String siteId = "site-1";
    String sectionName = "section1";
    String fieldPath = "field1";

    BrdFieldCommentGroup commentGroup = new BrdFieldCommentGroup();
    commentGroup.setId("group-1");
    commentGroup.setBrdFormId(brdFormId);
    commentGroup.setSourceType(sourceType);
    commentGroup.setSiteId(siteId);
    commentGroup.setSectionName(sectionName);
    commentGroup.setFieldPath(fieldPath);

    CommentEntry comment = new CommentEntry();
    comment.setId(testReadStatusRequest.getCommentId());
    comment.setIsRead(false);
    comment.setCreatedAt(LocalDateTime.now().minusDays(1));
    comment.setUpdatedAt(LocalDateTime.now().minusDays(1));

    commentGroup.setComments(List.of(comment));

    // Use doReturn instead of when for proper stubbing
    doReturn(Mono.just(commentGroup))
        .when(commentGroupRepository)
        .findByBrdFormIdAndSiteIdAndSourceTypeAndSectionNameAndFieldPath(
            anyString(), anyString(), anyString(), anyString(), anyString());

    doReturn(Mono.just(commentGroup))
        .when(commentGroupRepository)
        .save(any(BrdFieldCommentGroup.class));

    // Act & Assert
    StepVerifier.create(
            brdFieldCommentService.updateCommentReadStatus(
                brdFormId, sourceType, siteId, sectionName, fieldPath, testReadStatusRequest))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertNotNull(response.getBody());
              assertTrue(response.getBody().getData().isPresent());
              assertTrue(response.getBody().getData().get());
            })
        .verifyComplete();
  }

  @Test
  @DisplayName(
      "updateCommentReadStatus should throw BadRequestException when SITE source type without siteId")
  void updateCommentReadStatus_SiteWithoutSiteId_ThrowsBadRequest() {
    StepVerifier.create(
            brdFieldCommentService.updateCommentReadStatus(
                "brd-1", "SITE", null, "testSection", "test.field", testReadStatusRequest))
        .expectError(BadRequestException.class)
        .verify();

    // Verify that repository was not called
    verify(commentGroupRepository, never())
        .findByBrdFormIdAndSiteIdAndSourceTypeAndSectionNameAndFieldPath(
            anyString(), anyString(), anyString(), anyString(), anyString());
    verify(commentGroupRepository, never()).save(any(BrdFieldCommentGroup.class));
  }

  @Test
  @DisplayName(
      "updateCommentReadStatus should throw NotFoundException when comment group not found")
  void updateCommentReadStatus_GroupNotFound_ThrowsNotFound() {
    // Mock repository to return empty for the comment group
    when(commentGroupRepository.findByBrdFormIdAndSourceTypeAndSectionNameAndFieldPath(
            anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Mono.empty());

    // Test the method
    StepVerifier.create(
            brdFieldCommentService.updateCommentReadStatus(
                "brd-1", "BRD", null, "testSection", "test.field", testReadStatusRequest))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  @DisplayName(
      "updateCommentReadStatus should throw NotFoundException when comment not found in the group")
  void updateCommentReadStatus_CommentNotFound_ThrowsNotFound() {
    // Arrange
    String brdFormId = "brd-1";
    String sourceType = BrdConstants.SOURCE_TYPE_BRD;
    String sectionName = "section1";
    String fieldPath = "field1";

    BrdFieldCommentGroup commentGroup = new BrdFieldCommentGroup();
    commentGroup.setId("group-1");
    commentGroup.setBrdFormId(brdFormId);
    commentGroup.setSourceType(sourceType);
    commentGroup.setSectionName(sectionName);
    commentGroup.setFieldPath(fieldPath);
    commentGroup.setComments(new ArrayList<>()); // Empty comments list

    when(commentGroupRepository.findByBrdFormIdAndSourceTypeAndSectionNameAndFieldPath(
            anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Mono.just(commentGroup));

    // Act & Assert
    StepVerifier.create(
            brdFieldCommentService.updateCommentReadStatus(
                brdFormId, sourceType, null, sectionName, fieldPath, testReadStatusRequest))
        .expectErrorMatches(
            e ->
                e instanceof NotFoundException
                    && e.getMessage().contains("Comment not found with ID"))
        .verify();
  }

  @Test
  @DisplayName("getCommentsBySource should return comments for BRD source type without status filter")
  void getCommentsBySource_BRD_WithoutStatus_Success() {
    // Arrange
    String brdFormId = "brd-1";
    String sourceType = BrdConstants.SOURCE_TYPE_BRD;
    String siteId = null;
    String status = null;

    List<BrdFieldCommentGroup> commentGroups = Arrays.asList(
        BrdFieldCommentGroup.builder()
            .id("group-1")
            .brdFormId(brdFormId)
            .sourceType(sourceType)
            .sectionName("section1")
            .fieldPath("field1")
            .status("Pending")
            .comments(new ArrayList<>())
            .build(),
        BrdFieldCommentGroup.builder()
            .id("group-2")
            .brdFormId(brdFormId)
            .sourceType(sourceType)
            .sectionName("section2")
            .fieldPath("field2")
            .status("Resolved")
            .comments(new ArrayList<>())
            .build()
    );

    List<BrdFieldCommentGroupResp> groupResponses = Arrays.asList(
        BrdFieldCommentGroupResp.builder()
            .id("group-1")
            .brdFormId(brdFormId)
            .sourceType(sourceType)
            .sectionName("section1")
            .fieldPath("field1")
            .status("Pending")
            .comments(new ArrayList<>())
            .build(),
        BrdFieldCommentGroupResp.builder()
            .id("group-2")
            .brdFormId(brdFormId)
            .sourceType(sourceType)
            .sectionName("section2")
            .fieldPath("field2")
            .status("Resolved")
            .comments(new ArrayList<>())
            .build()
    );

    when(commentGroupRepository.findByBrdFormIdAndSourceType(brdFormId, sourceType))
        .thenReturn(Flux.fromIterable(commentGroups));

    for (int i = 0; i < commentGroups.size(); i++) {
      when(dtoModelMapper.mapToGroupResponse(commentGroups.get(i)))
          .thenReturn(groupResponses.get(i));
    }

    // Act & Assert
    StepVerifier.create(brdFieldCommentService.getCommentsBySource(brdFormId, sourceType, siteId, status))
        .assertNext(response -> {
          assertEquals(HttpStatus.OK, response.getStatusCode());
          assertTrue(response.getBody().getData().isPresent());
          CommentsBySourceResponse data = response.getBody().getData().get();
          
          assertEquals(2, data.getTotalCount());
          assertEquals(sourceType, data.getSourceType());
          assertEquals(brdFormId, data.getBrdFormId());
          assertNull(data.getSiteId());
          assertEquals(2, data.getComments().size());
          
          // Verify section counts
          assertEquals(1, data.getSectionCounts().get("section1").intValue());
          assertEquals(1, data.getSectionCounts().get("section2").intValue());
          
          // Verify section field paths
          assertTrue(data.getSectionFieldPaths().get("section1").contains("field1"));
          assertTrue(data.getSectionFieldPaths().get("section2").contains("field2"));
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("getCommentsBySource should return comments for BRD source type with status filter")
  void getCommentsBySource_BRD_WithStatus_Success() {
    // Arrange
    String brdFormId = "brd-1";
    String sourceType = BrdConstants.SOURCE_TYPE_BRD;
    String siteId = null;
    String status = "Pending";

    List<BrdFieldCommentGroup> commentGroups = Arrays.asList(
        BrdFieldCommentGroup.builder()
            .id("group-1")
            .brdFormId(brdFormId)
            .sourceType(sourceType)
            .sectionName("section1")
            .fieldPath("field1")
            .status(status)
            .comments(new ArrayList<>())
            .build()
    );

    List<BrdFieldCommentGroupResp> groupResponses = Arrays.asList(
        BrdFieldCommentGroupResp.builder()
            .id("group-1")
            .brdFormId(brdFormId)
            .sourceType(sourceType)
            .sectionName("section1")
            .fieldPath("field1")
            .status(status)
            .comments(new ArrayList<>())
            .build()
    );

    when(mongoTemplate.find(any(Query.class), eq(BrdFieldCommentGroup.class)))
        .thenReturn(Flux.fromIterable(commentGroups));

    when(dtoModelMapper.mapToGroupResponse(commentGroups.get(0)))
        .thenReturn(groupResponses.get(0));

    // Act & Assert
    StepVerifier.create(brdFieldCommentService.getCommentsBySource(brdFormId, sourceType, siteId, status))
        .assertNext(response -> {
          assertEquals(HttpStatus.OK, response.getStatusCode());
          assertTrue(response.getBody().getData().isPresent());
          CommentsBySourceResponse data = response.getBody().getData().get();
          
          assertEquals(1, data.getTotalCount());
          assertEquals(sourceType, data.getSourceType());
          assertEquals(brdFormId, data.getBrdFormId());
          assertNull(data.getSiteId());
          assertEquals(1, data.getComments().size());
          
          // Verify the message contains status
          assertTrue(response.getBody().getMessage().contains("with status 'Pending'"));
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("getCommentsBySource should return comments for SITE source type without status filter")
  void getCommentsBySource_SITE_WithoutStatus_Success() {
    // Arrange
    String brdFormId = "brd-1";
    String sourceType = BrdConstants.SOURCE_TYPE_SITE;
    String siteId = "site-1";
    String status = null;

    List<BrdFieldCommentGroup> commentGroups = Arrays.asList(
        BrdFieldCommentGroup.builder()
            .id("group-1")
            .brdFormId(brdFormId)
            .siteId(siteId)
            .sourceType(sourceType)
            .sectionName("section1")
            .fieldPath("field1")
            .status("Pending")
            .comments(new ArrayList<>())
            .build()
    );

    List<BrdFieldCommentGroupResp> groupResponses = Arrays.asList(
        BrdFieldCommentGroupResp.builder()
            .id("group-1")
            .brdFormId(brdFormId)
            .siteId(siteId)
            .sourceType(sourceType)
            .sectionName("section1")
            .fieldPath("field1")
            .status("Pending")
            .comments(new ArrayList<>())
            .build()
    );

    when(commentGroupRepository.findByBrdFormIdAndSiteIdAndSourceType(brdFormId, siteId, sourceType))
        .thenReturn(Flux.fromIterable(commentGroups));

    when(dtoModelMapper.mapToGroupResponse(commentGroups.get(0)))
        .thenReturn(groupResponses.get(0));

    // Act & Assert
    StepVerifier.create(brdFieldCommentService.getCommentsBySource(brdFormId, sourceType, siteId, status))
        .assertNext(response -> {
          assertEquals(HttpStatus.OK, response.getStatusCode());
          assertTrue(response.getBody().getData().isPresent());
          CommentsBySourceResponse data = response.getBody().getData().get();
          
          assertEquals(1, data.getTotalCount());
          assertEquals(sourceType, data.getSourceType());
          assertEquals(brdFormId, data.getBrdFormId());
          assertEquals(siteId, data.getSiteId());
          assertEquals(1, data.getComments().size());
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("getCommentsBySource should return comments for SITE source type with status filter")
  void getCommentsBySource_SITE_WithStatus_Success() {
    // Arrange
    String brdFormId = "brd-1";
    String sourceType = BrdConstants.SOURCE_TYPE_SITE;
    String siteId = "site-1";
    String status = "Resolved";

    List<BrdFieldCommentGroup> commentGroups = Arrays.asList(
        BrdFieldCommentGroup.builder()
            .id("group-1")
            .brdFormId(brdFormId)
            .siteId(siteId)
            .sourceType(sourceType)
            .sectionName("section1")
            .fieldPath("field1")
            .status(status)
            .comments(new ArrayList<>())
            .build()
    );

    List<BrdFieldCommentGroupResp> groupResponses = Arrays.asList(
        BrdFieldCommentGroupResp.builder()
            .id("group-1")
            .brdFormId(brdFormId)
            .siteId(siteId)
            .sourceType(sourceType)
            .sectionName("section1")
            .fieldPath("field1")
            .status(status)
            .comments(new ArrayList<>())
            .build()
    );

    when(mongoTemplate.find(any(Query.class), eq(BrdFieldCommentGroup.class)))
        .thenReturn(Flux.fromIterable(commentGroups));

    when(dtoModelMapper.mapToGroupResponse(commentGroups.get(0)))
        .thenReturn(groupResponses.get(0));

    // Act & Assert
    StepVerifier.create(brdFieldCommentService.getCommentsBySource(brdFormId, sourceType, siteId, status))
        .assertNext(response -> {
          assertEquals(HttpStatus.OK, response.getStatusCode());
          assertTrue(response.getBody().getData().isPresent());
          CommentsBySourceResponse data = response.getBody().getData().get();
          
          assertEquals(1, data.getTotalCount());
          assertEquals(sourceType, data.getSourceType());
          assertEquals(brdFormId, data.getBrdFormId());
          assertEquals(siteId, data.getSiteId());
          assertEquals(1, data.getComments().size());
          
          // Verify the message contains status
          assertTrue(response.getBody().getMessage().contains("with status 'Resolved'"));
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("getCommentsBySource should return empty response when no comments found")
  void getCommentsBySource_EmptyResponse() {
    // Arrange
    String brdFormId = "brd-1";
    String sourceType = BrdConstants.SOURCE_TYPE_BRD;
    String siteId = null;
    String status = null;

    when(commentGroupRepository.findByBrdFormIdAndSourceType(brdFormId, sourceType))
        .thenReturn(Flux.empty());

    // Act & Assert
    StepVerifier.create(brdFieldCommentService.getCommentsBySource(brdFormId, sourceType, siteId, status))
        .assertNext(response -> {
          assertEquals(HttpStatus.OK, response.getStatusCode());
          assertTrue(response.getBody().getData().isPresent());
          CommentsBySourceResponse data = response.getBody().getData().get();
          
          assertEquals(0, data.getTotalCount());
          assertEquals(sourceType, data.getSourceType());
          assertEquals(brdFormId, data.getBrdFormId());
          assertNull(data.getSiteId());
          assertTrue(data.getComments().isEmpty());
          assertTrue(data.getSectionCounts().isEmpty());
          assertTrue(data.getSectionFieldPaths().isEmpty());
          
          // Verify the message
          assertTrue(response.getBody().getMessage().contains("No comments found for BRD: brd-1"));
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("getCommentsBySource should throw BadRequestException when brdFormId is null or blank")
  void getCommentsBySource_WithNullBrdFormId_ThrowsBadRequest() {
    String sourceType = BrdConstants.SOURCE_TYPE_BRD;
    String siteId = null;
    String status = null;

    // Test with null brdFormId
    StepVerifier.create(brdFieldCommentService.getCommentsBySource(null, sourceType, siteId, status))
        .expectError(BadRequestException.class)
        .verify();

    // Test with blank brdFormId
    StepVerifier.create(brdFieldCommentService.getCommentsBySource("  ", sourceType, siteId, status))
        .expectError(BadRequestException.class)
        .verify();
  }

  @Test
  @DisplayName("getCommentsBySource should throw BadRequestException with invalid sourceType")
  void getCommentsBySource_WithInvalidSourceType_ThrowsBadRequest() {
    String brdFormId = "brd-1";
    String siteId = null;
    String status = null;

    // Test with null sourceType
    StepVerifier.create(brdFieldCommentService.getCommentsBySource(brdFormId, null, siteId, status))
        .expectError(BadRequestException.class)
        .verify();

    // Test with blank sourceType
    StepVerifier.create(brdFieldCommentService.getCommentsBySource(brdFormId, "", siteId, status))
        .expectError(BadRequestException.class)
        .verify();

    // Test with invalid sourceType
    StepVerifier.create(brdFieldCommentService.getCommentsBySource(brdFormId, "INVALID", siteId, status))
        .expectError(BadRequestException.class)
        .verify();
  }

  @Test
  @DisplayName("getCommentsBySource should throw BadRequestException when SITE source type without siteId")
  void getCommentsBySource_SiteWithoutSiteId_ThrowsBadRequest() {
    String brdFormId = "brd-1";
    String sourceType = BrdConstants.SOURCE_TYPE_SITE;
    String status = null;

    // Test with null siteId
    StepVerifier.create(brdFieldCommentService.getCommentsBySource(brdFormId, sourceType, null, status))
        .expectError(BadRequestException.class)
        .verify();

    // Test with blank siteId
    StepVerifier.create(brdFieldCommentService.getCommentsBySource(brdFormId, sourceType, "  ", status))
        .expectError(BadRequestException.class)
        .verify();
  }

  @Test
  @DisplayName("getCommentsBySource should throw BadRequestException with invalid status")
  void getCommentsBySource_WithInvalidStatus_ThrowsBadRequest() {
    String brdFormId = "brd-1";
    String sourceType = BrdConstants.SOURCE_TYPE_BRD;
    String siteId = null;

    // Test with invalid status
    StepVerifier.create(brdFieldCommentService.getCommentsBySource(brdFormId, sourceType, siteId, "InvalidStatus"))
        .expectError(BadRequestException.class)
        .verify();
  }

  @Test
  @DisplayName("getCommentsBySource should handle section field paths with duplicate field paths")
  void getCommentsBySource_WithDuplicateFieldPaths_Success() {
    // Arrange
    String brdFormId = "brd-1";
    String sourceType = BrdConstants.SOURCE_TYPE_BRD;
    String siteId = null;
    String status = null;

    List<BrdFieldCommentGroup> commentGroups = Arrays.asList(
        BrdFieldCommentGroup.builder()
            .id("group-1")
            .brdFormId(brdFormId)
            .sourceType(sourceType)
            .sectionName("section1")
            .fieldPath("field1")
            .status("Pending")
            .comments(new ArrayList<>())
            .build(),
        BrdFieldCommentGroup.builder()
            .id("group-2")
            .brdFormId(brdFormId)
            .sourceType(sourceType)
            .sectionName("section1")
            .fieldPath("field1") // Duplicate field path
            .status("Resolved")
            .comments(new ArrayList<>())
            .build()
    );

    List<BrdFieldCommentGroupResp> groupResponses = Arrays.asList(
        BrdFieldCommentGroupResp.builder()
            .id("group-1")
            .brdFormId(brdFormId)
            .sourceType(sourceType)
            .sectionName("section1")
            .fieldPath("field1")
            .status("Pending")
            .comments(new ArrayList<>())
            .build(),
        BrdFieldCommentGroupResp.builder()
            .id("group-2")
            .brdFormId(brdFormId)
            .sourceType(sourceType)
            .sectionName("section1")
            .fieldPath("field1") // Duplicate field path
            .status("Resolved")
            .comments(new ArrayList<>())
            .build()
    );

    when(commentGroupRepository.findByBrdFormIdAndSourceType(brdFormId, sourceType))
        .thenReturn(Flux.fromIterable(commentGroups));

    for (int i = 0; i < commentGroups.size(); i++) {
      when(dtoModelMapper.mapToGroupResponse(commentGroups.get(i)))
          .thenReturn(groupResponses.get(i));
    }

    // Act & Assert
    StepVerifier.create(brdFieldCommentService.getCommentsBySource(brdFormId, sourceType, siteId, status))
        .assertNext(response -> {
          assertEquals(HttpStatus.OK, response.getStatusCode());
          assertTrue(response.getBody().getData().isPresent());
          CommentsBySourceResponse data = response.getBody().getData().get();
          
          assertEquals(2, data.getTotalCount());
          assertEquals(2, data.getSectionCounts().get("section1").intValue());
          
          // Verify that field path is only added once despite duplicate
          List<String> fieldPaths = data.getSectionFieldPaths().get("section1");
          assertEquals(1, fieldPaths.size());
          assertTrue(fieldPaths.contains("field1"));
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("addComment should handle parent comment validation successfully")
  void addComment_WithParentComment_Success() {
    // Arrange
    String parentCommentId = "parent-1";
    CommentEntry parentComment = CommentEntry.builder()
        .id(parentCommentId)
        .content("Parent comment")
        .createdBy("user1")
        .userType("REVIEWER")
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    CommentEntryReq commentReqWithParent = CommentEntryReq.builder()
        .content("Reply to parent")
        .createdBy("user2")
        .userType("REVIEWER")
        .parentCommentId(parentCommentId)
        .build();

    // Use the same list instance for both the initial and saved group
    List<CommentEntry> commentsList = new ArrayList<>(Collections.singletonList(parentComment));

    // The group before saving (with only the parent)
    BrdFieldCommentGroup groupBefore = BrdFieldCommentGroup.builder()
        .id("test-id")
        .brdFormId("brd-1")
        .sourceType("BRD")
        .fieldPath("test.field")
        .sectionName("testSection")
        .status("Pending")
        .comments(commentsList)
        .build();

    when(commentGroupRepository.findByBrdFormIdAndSourceTypeAndSectionNameAndFieldPath(
            anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Mono.just(groupBefore));


    StepVerifier.create(
            brdFieldCommentService.addComment(
                "brd-1", "testSection", "test.field", commentReqWithParent, "BRD", null))
        .expectErrorSatisfies(error -> {
            assertTrue(error instanceof Exception);
            assertTrue(error.getMessage().contains("Something went wrong: other"));
        })
        .verify();
  }

  @Test
  @DisplayName("updateEntityFieldFromShadowValue should handle Site update successfully")
  void updateEntityFieldFromShadowValue_ForSite_Success() {
    // Arrange
    testUpdateDTO.setSourceType("SITE");
    testUpdateDTO.setSiteId("site-1");
    testCommentGroup.setFieldPathShadowValue("new value");
    testCommentGroup.setSourceType("SITE");
    testCommentGroup.setSiteId("site-1");

    Document mockDocument = new Document();
    Document brdFormDoc = new Document();
    brdFormDoc.put("clientInformation", new Document());
    mockDocument.put("brdForm", brdFormDoc);

    UpdateResult mockUpdateResult = mock(UpdateResult.class);
    when(mockUpdateResult.getModifiedCount()).thenReturn(1L);

    // Mock the full reactive update chain
    ReactiveUpdateOperation.ReactiveUpdate<Site> reactiveUpdate = mock(ReactiveUpdateOperation.ReactiveUpdate.class);
    ReactiveUpdateOperation.UpdateWithUpdate<Site> updateWithUpdate = mock(ReactiveUpdateOperation.UpdateWithUpdate.class);
    ReactiveUpdateOperation.TerminatingUpdate<Site> terminatingUpdate = mock(ReactiveUpdateOperation.TerminatingUpdate.class);
    when(mongoTemplate.update(Site.class)).thenReturn(reactiveUpdate);
    when(reactiveUpdate.matching(any(Query.class))).thenReturn(updateWithUpdate);
    when(updateWithUpdate.apply(any(Update.class))).thenReturn(terminatingUpdate);
    when(terminatingUpdate.first()).thenReturn(Mono.just(mockUpdateResult));

    when(commentGroupRepository.findByBrdFormIdAndSiteIdAndSourceTypeAndSectionNameAndFieldPath(
            anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Mono.just(testCommentGroup));
    when(mongoTemplate.findOne(any(Query.class), eq(Document.class), eq("sites")))
        .thenReturn(Mono.just(mockDocument));
    when(commentGroupRepository.save(any(BrdFieldCommentGroup.class)))
        .thenReturn(Mono.just(testCommentGroup));
    // Mock auditLogService.logCreation to return a valid Mono<ResponseEntity<Api<AuditLogResponse>>>
    Api<AuditLogResponse> api = new Api<>(BrdConstants.SUCCESSFUL, "Audit log created", Optional.empty(), Optional.empty());
    ResponseEntity<Api<AuditLogResponse>> auditLogResponse = ResponseEntity.ok(api);
    when(auditLogService.logCreation(any())).thenReturn(Mono.just(auditLogResponse));

    // Act & Assert
    StepVerifier.create(brdFieldCommentService.updateEntityFieldFromShadowValue(testUpdateDTO))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertTrue(response.getBody().getData().isPresent());
              assertTrue(response.getBody().getData().get());
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("handleErrors should handle AlreadyExistException")
  void handleErrors_WithAlreadyExistException_ReturnsSameException() {
    AlreadyExistException ex = new AlreadyExistException("Test error");
    Throwable result = brdFieldCommentService.handleErrors(ex);
    assertEquals(ex, result);
  }

  @Test
  @DisplayName("handleErrors should handle exception with 'Something went wrong:' prefix")
  void handleErrors_WithSomethingWentWrongPrefix_ReturnsSameException() {
    Exception ex = new Exception("Something went wrong: Test error");
    Throwable result = brdFieldCommentService.handleErrors(ex);
    assertEquals(ex, result);
  }

  @Test
  @DisplayName("handleErrors should handle exception with 'Parent comment not found' message")
  void handleErrors_WithParentCommentNotFound_ReturnsNotFoundException() {
    Exception ex = new Exception("Parent comment not found");
    Throwable result = brdFieldCommentService.handleErrors(ex);
    assertTrue(result instanceof NotFoundException);
    assertEquals("Parent comment not found", result.getMessage());
  }

  @Test
  @DisplayName("getComments should handle null sourceType")
  void getComments_WithNullSourceType_ThrowsBadRequest() {
    StepVerifier.create(
            brdFieldCommentService.getComments("brd-1", null, null, "testSection", "test.field"))
        .expectError(BadRequestException.class)
        .verify();
  }
}

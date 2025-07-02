package com.aci.smart_onboarding.service;

import com.aci.smart_onboarding.dto.*;
import java.util.List;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

public interface IBrdFieldCommentService {
  // Initialize or update a field comment group
  Mono<ResponseEntity<Api<BrdFieldCommentGroupResp>>> createOrUpdateFieldCommentGroup(
      BrdFieldCommentGroupReq groupReq);

  // Add a comment to an existing field group
  Mono<ResponseEntity<Api<CommentEntryResp>>> addComment(
      String brdFormId,
      String sectionName,
      String fieldPath,
      CommentEntryReq commentReq,
      String sourceType,
      String siteId);

  // Get comments with flexible filtering
  Mono<ResponseEntity<Api<List<BrdFieldCommentGroupResp>>>> getComments(
      String brdFormId, String sourceType, String siteId, String sectionName, String fieldPath);

  // Get all comment groups for a BRD
  Mono<ResponseEntity<Api<List<BrdFieldCommentGroupResp>>>> getCommentGroupsByBrdFormId(
      String brdFormId, String sourceType);

  // Get all comment groups for a Site
  Mono<ResponseEntity<Api<List<BrdFieldCommentGroupResp>>>> getCommentGroupsBySiteId(
      String brdFormId, String siteId);

  // Get a specific comment group
  Mono<ResponseEntity<Api<BrdFieldCommentGroupResp>>> getCommentGroup(
      String brdFormId, String sourceType, String siteId, String sectionName, String fieldPath);

  // Get all comment groups for a specific section
  Mono<ResponseEntity<Api<List<BrdFieldCommentGroupResp>>>> getCommentGroupsBySection(
      String brdFormId, String sourceType, String siteId, String sectionName);

  // Update entity field from shadow value
  Mono<ResponseEntity<Api<Boolean>>> updateEntityFieldFromShadowValue(
      ShadowValueUpdateDTO updateDTO);

  // Error handling
  Throwable handleErrors(Throwable ex);

  // Get comment statistics by status (Pending or Resolved)
  Mono<ResponseEntity<Api<CommentStatsByStatusResponse>>> getCommentStatsByStatus(
      String brdFormId, String status);

  // Get comments by source type with aggregated statistics
  Mono<ResponseEntity<Api<CommentsBySourceResponse>>> getCommentsBySource(
      String brdFormId, String sourceType, String siteId, String status);

  // Update read status of a specific comment
  Mono<ResponseEntity<Api<Boolean>>> updateCommentReadStatus(
      String brdFormId,
      String sourceType,
      String siteId,
      String sectionName,
      String fieldPath,
      UpdateCommentReadStatusRequest request);
}

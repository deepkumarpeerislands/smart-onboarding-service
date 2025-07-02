package com.aci.smart_onboarding.service.implementation;

import com.aci.smart_onboarding.constants.BrdConstants;
import com.aci.smart_onboarding.dto.*;
import com.aci.smart_onboarding.dto.AuditLogRequest;
import com.aci.smart_onboarding.exception.AlreadyExistException;
import com.aci.smart_onboarding.exception.BadRequestException;
import com.aci.smart_onboarding.exception.NotFoundException;
import com.aci.smart_onboarding.mapper.DtoModelMapper;
import com.aci.smart_onboarding.model.BrdFieldCommentGroup;
import com.aci.smart_onboarding.model.BrdFieldCommentGroup.CommentEntry;
import com.aci.smart_onboarding.model.Site;
import com.aci.smart_onboarding.repository.BrdFieldCommentGroupRepository;
import com.aci.smart_onboarding.service.IAuditLogService;
import com.aci.smart_onboarding.service.IBRDService;
import com.aci.smart_onboarding.service.IBrdFieldCommentService;
import com.aci.smart_onboarding.service.ISequenceGeneratorService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class BrdFieldCommentService implements IBrdFieldCommentService {
  private final BrdFieldCommentGroupRepository commentGroupRepository;
  private final DtoModelMapper dtoModelMapper;
  private final ISequenceGeneratorService sequenceGeneratorService;
  private final ReactiveMongoTemplate reactiveMongoTemplate;
  private final IBRDService brdService;
  private final IAuditLogService auditLogService;
  private static final Logger log = LoggerFactory.getLogger(BrdFieldCommentService.class);

  @Override
  public Mono<ResponseEntity<Api<BrdFieldCommentGroupResp>>> createOrUpdateFieldCommentGroup(
      BrdFieldCommentGroupReq groupReq) {
    if (BrdConstants.SOURCE_TYPE_SITE.equals(groupReq.getSourceType())
        && (groupReq.getSiteId() == null || groupReq.getSiteId().trim().isEmpty())) {
      return Mono.error(new BadRequestException(BrdConstants.SITE_ID_REQUIRED_ERROR));
    }

    Mono<BrdFieldCommentGroup> findExistingGroup;
    if (BrdConstants.SOURCE_TYPE_BRD.equals(groupReq.getSourceType())) {
      findExistingGroup =
          commentGroupRepository.findByBrdFormIdAndSourceTypeAndSectionNameAndFieldPath(
              groupReq.getBrdFormId(),
              groupReq.getSourceType(),
              groupReq.getSectionName(),
              groupReq.getFieldPath());
    } else {
      findExistingGroup =
          commentGroupRepository.findByBrdFormIdAndSiteIdAndSourceTypeAndSectionNameAndFieldPath(
              groupReq.getBrdFormId(),
              groupReq.getSiteId(),
              groupReq.getSourceType(),
              groupReq.getSectionName(),
              groupReq.getFieldPath());
    }

    return findExistingGroup
        .flatMap(
            existingGroup -> {
              if (groupReq.getFieldPathShadowValue() != null
                  && !groupReq.getFieldPathShadowValue().toString().isEmpty()) {
                existingGroup.setFieldPathShadowValue(groupReq.getFieldPathShadowValue());
              }
              existingGroup.setStatus(groupReq.getStatus());

              return commentGroupRepository
                  .save(existingGroup)
                  .map(dtoModelMapper::mapToGroupResponse);
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  BrdFieldCommentGroup newGroup =
                      BrdFieldCommentGroup.builder()
                          .brdFormId(groupReq.getBrdFormId())
                          .siteId(
                              BrdConstants.SOURCE_TYPE_SITE.equals(groupReq.getSourceType())
                                  ? groupReq.getSiteId()
                                  : null)
                          .sourceType(groupReq.getSourceType())
                          .fieldPath(groupReq.getFieldPath())
                          .fieldPathShadowValue(groupReq.getFieldPathShadowValue())
                          .status(groupReq.getStatus())
                          .sectionName(groupReq.getSectionName())
                          .createdBy(groupReq.getCreatedBy())
                          .comments(new ArrayList<>())
                          .build();

                  return commentGroupRepository
                      .save(newGroup)
                      .map(dtoModelMapper::mapToGroupResponse);
                }))
        .map(
            groupResp ->
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        BrdConstants.COMMENT_GROUP_CREATED_SUCCESS,
                        Optional.of(groupResp),
                        Optional.empty())))
        .onErrorMap(this::handleErrors);
  }

  @Override
  public Mono<ResponseEntity<Api<CommentEntryResp>>> addComment(
      String brdFormId,
      String sectionName,
      String fieldPath,
      CommentEntryReq commentReq,
      String sourceType,
      String siteId) {

    if (!BrdConstants.SOURCE_TYPE_BRD.equals(sourceType)
        && !BrdConstants.SOURCE_TYPE_SITE.equals(sourceType)) {
      return Mono.error(new BadRequestException(BrdConstants.SOURCE_TYPE_ERROR_MESSAGE));
    }

    if (BrdConstants.SOURCE_TYPE_SITE.equals(sourceType)
        && (siteId == null || siteId.trim().isEmpty())) {
      return Mono.error(new BadRequestException(BrdConstants.SITE_ID_REQUIRED_ERROR));
    }

    return findAndProcessCommentAddition(
            brdFormId, sectionName, fieldPath, commentReq, sourceType, siteId)
        .map(
            commentResp ->
                ResponseEntity.status(HttpStatus.CREATED)
                    .body(
                        new Api<>(
                            BrdConstants.SUCCESSFUL,
                            BrdConstants.COMMENT_ADDED_SUCCESS,
                            Optional.of(commentResp),
                            Optional.empty())))
        .onErrorMap(this::handleErrors);
  }

  private Mono<CommentEntryResp> findAndProcessCommentAddition(
      String brdFormId,
      String sectionName,
      String fieldPath,
      CommentEntryReq commentReq,
      String sourceType,
      String siteId) {

    Mono<BrdFieldCommentGroup> findGroup;
    if (BrdConstants.SOURCE_TYPE_BRD.equals(sourceType)) {
      findGroup =
          commentGroupRepository.findByBrdFormIdAndSourceTypeAndSectionNameAndFieldPath(
              brdFormId, sourceType, sectionName, fieldPath);
    } else {
      findGroup =
          commentGroupRepository.findByBrdFormIdAndSiteIdAndSourceTypeAndSectionNameAndFieldPath(
              brdFormId, siteId, sourceType, sectionName, fieldPath);
    }

    return findGroup
        .switchIfEmpty(
            Mono.error(new NotFoundException(BrdConstants.FIELD_COMMENT_GROUP_NOT_FOUND)))
        .flatMap(
            group -> {
              String userType = commentReq.getUserType();
              if (group.getComments() != null) {
                long commentsCountForUserType =
                    group.getComments().stream()
                        .filter(comment -> userType.equals(comment.getUserType()))
                        .count();

                if (commentsCountForUserType >= 5) {
                  return Mono.error(
                      new BadRequestException(
                          "Comment threshold reached for userType '"
                              + userType
                              + "' (maximum 5 comments allowed per userType)"));
                }
              }

              Mono<Void> parentValidation =
                  Mono.justOrEmpty(commentReq.getParentCommentId())
                      .flatMap(
                          parentId -> {
                            boolean parentExists =
                                group.getComments().stream()
                                    .anyMatch(entry -> parentId.equals(entry.getId()));

                            if (!parentExists) {
                              return Mono.error(
                                  new NotFoundException(BrdConstants.PARENT_COMMENT_NOT_FOUND));
                            }
                            return Mono.empty();
                          })
                      .then();

              Mono<String> commentIdMono =
                  Mono.justOrEmpty(commentReq.getParentCommentId())
                      .flatMap(sequenceGeneratorService::generateReplyId)
                      .switchIfEmpty(
                          sequenceGeneratorService.generateCommentId(
                              BrdConstants.SOURCE_TYPE_BRD.equals(sourceType) ? brdFormId : siteId,
                              sectionName,
                              fieldPath));

              return parentValidation
                  .then(commentIdMono)
                  .flatMap(
                      commentId -> {
                        LocalDateTime now = LocalDateTime.now();

                        CommentEntry newComment =
                            CommentEntry.builder()
                                .id(commentId)
                                .content(commentReq.getContent())
                                .createdBy(commentReq.getCreatedBy())
                                .userType(commentReq.getUserType())
                                .parentCommentId(commentReq.getParentCommentId())
                                .createdAt(now)
                                .updatedAt(now)
                                .build();

                        group.getComments().add(newComment);

                        return commentGroupRepository
                            .save(group)
                            .map(savedGroup -> dtoModelMapper.mapToCommentResponse(newComment));
                      });
            });
  }

  @Override
  public Mono<ResponseEntity<Api<List<BrdFieldCommentGroupResp>>>> getCommentGroupsByBrdFormId(
      String brdFormId, String sourceType) {
    return commentGroupRepository
        .findByBrdFormIdAndSourceType(brdFormId, sourceType)
        .map(dtoModelMapper::mapToGroupResponse)
        .collectList()
        .map(
            groups -> {
              Api<List<BrdFieldCommentGroupResp>> apiResponse =
                  new Api<>(
                      BrdConstants.SUCCESSFUL,
                      BrdConstants.COMMENT_GROUPS_RETRIEVED_SUCCESS,
                      Optional.of(groups),
                      Optional.empty());
              return ResponseEntity.ok(apiResponse);
            })
        .defaultIfEmpty(
            ResponseEntity.ok(
                new Api<>(
                    BrdConstants.SUCCESSFUL,
                    BrdConstants.NO_COMMENT_GROUPS_BRD,
                    Optional.of(Collections.emptyList()),
                    Optional.empty())))
        .onErrorMap(this::handleErrors);
  }

  @Override
  public Mono<ResponseEntity<Api<List<BrdFieldCommentGroupResp>>>> getCommentGroupsBySiteId(
      String brdFormId, String siteId) {
    return commentGroupRepository
        .findByBrdFormIdAndSiteIdAndSourceType(brdFormId, siteId, BrdConstants.SOURCE_TYPE_SITE)
        .map(dtoModelMapper::mapToGroupResponse)
        .collectList()
        .map(
            groups -> {
              Api<List<BrdFieldCommentGroupResp>> apiResponse =
                  new Api<>(
                      BrdConstants.SUCCESSFUL,
                      BrdConstants.COMMENT_GROUPS_RETRIEVED_SUCCESS,
                      Optional.of(groups),
                      Optional.empty());
              return ResponseEntity.ok(apiResponse);
            })
        .defaultIfEmpty(
            ResponseEntity.ok(
                new Api<>(
                    BrdConstants.SUCCESSFUL,
                    BrdConstants.NO_COMMENT_GROUPS_SITE,
                    Optional.of(Collections.emptyList()),
                    Optional.empty())))
        .onErrorMap(this::handleErrors);
  }

  @Override
  public Mono<ResponseEntity<Api<BrdFieldCommentGroupResp>>> getCommentGroup(
      String brdFormId, String sourceType, String siteId, String sectionName, String fieldPath) {

    Mono<BrdFieldCommentGroup> findGroup;
    if (BrdConstants.SOURCE_TYPE_BRD.equals(sourceType)) {
      findGroup =
          commentGroupRepository.findByBrdFormIdAndSourceTypeAndSectionNameAndFieldPath(
              brdFormId, sourceType, sectionName, fieldPath);
    } else {
      if (siteId == null || siteId.trim().isEmpty()) {
        return Mono.error(new BadRequestException(BrdConstants.SITE_ID_REQUIRED_ERROR));
      }
      findGroup =
          commentGroupRepository.findByBrdFormIdAndSiteIdAndSourceTypeAndSectionNameAndFieldPath(
              brdFormId, siteId, sourceType, sectionName, fieldPath);
    }

    return findGroup
        .map(dtoModelMapper::mapToGroupResponse)
        .map(
            groupResp -> {
              Api<BrdFieldCommentGroupResp> apiResponse =
                  new Api<>(
                      BrdConstants.SUCCESSFUL,
                      BrdConstants.COMMENT_GROUP_RETRIEVED_SUCCESS,
                      Optional.of(groupResp),
                      Optional.empty());
              return ResponseEntity.ok(apiResponse);
            })
        .switchIfEmpty(
            Mono.just(
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(
                        new Api<>(
                            BrdConstants.FAILURE,
                            BrdConstants.FIELD_COMMENT_GROUP_NOT_FOUND,
                            Optional.empty(),
                            Optional.of(
                                Collections.singletonMap(
                                    "error", BrdConstants.FIELD_COMMENT_GROUP_NOT_FOUND))))))
        .onErrorMap(this::handleErrors);
  }

  @Override
  public Mono<ResponseEntity<Api<List<BrdFieldCommentGroupResp>>>> getCommentGroupsBySection(
      String brdFormId, String sourceType, String siteId, String sectionName) {

    Flux<BrdFieldCommentGroup> findGroups;
    if (BrdConstants.SOURCE_TYPE_BRD.equals(sourceType)) {
      findGroups =
          commentGroupRepository.findByBrdFormIdAndSourceTypeAndSectionName(
              brdFormId, sourceType, sectionName);
    } else {
      if (siteId == null || siteId.trim().isEmpty()) {
        return Mono.error(new BadRequestException(BrdConstants.SITE_ID_REQUIRED_ERROR));
      }
      findGroups =
          commentGroupRepository.findByBrdFormIdAndSiteIdAndSourceTypeAndSectionName(
              brdFormId, siteId, sourceType, sectionName);
    }

    return findGroups
        .map(dtoModelMapper::mapToGroupResponse)
        .collectList()
        .map(
            groups -> {
              Api<List<BrdFieldCommentGroupResp>> apiResponse =
                  new Api<>(
                      BrdConstants.SUCCESSFUL,
                      BrdConstants.COMMENT_GROUPS_RETRIEVED_SUCCESS,
                      Optional.of(groups),
                      Optional.empty());
              return ResponseEntity.ok(apiResponse);
            })
        .defaultIfEmpty(
            ResponseEntity.ok(
                new Api<>(
                    BrdConstants.SUCCESSFUL,
                    BrdConstants.NO_COMMENT_GROUPS_SECTION,
                    Optional.of(Collections.emptyList()),
                    Optional.empty())))
        .onErrorMap(this::handleErrors);
  }

  @Override
  public Mono<ResponseEntity<Api<List<BrdFieldCommentGroupResp>>>> getComments(
      String brdFormId, String sourceType, String siteId, String sectionName, String fieldPath) {

    if (sourceType == null
        || (!sourceType.equals(BrdConstants.SOURCE_TYPE_BRD)
            && !sourceType.equals(BrdConstants.SOURCE_TYPE_SITE))) {
      return Mono.error(new BadRequestException(BrdConstants.SOURCE_TYPE_ERROR_MESSAGE));
    }

    if (BrdConstants.SOURCE_TYPE_SITE.equals(sourceType)
        && (siteId == null || siteId.trim().isEmpty())) {
      return Mono.error(new BadRequestException(BrdConstants.SITE_ID_REQUIRED_ERROR));
    }

    if (sectionName != null && fieldPath != null) {
      return getCommentGroup(brdFormId, sourceType, siteId, sectionName, fieldPath)
          .map(
              response -> {
                BrdFieldCommentGroupResp commentGroup = response.getBody().getData().orElse(null);
                List<BrdFieldCommentGroupResp> resultList =
                    commentGroup != null
                        ? Collections.singletonList(commentGroup)
                        : Collections.emptyList();

                return ResponseEntity.ok(
                    new Api<>(
                        response.getBody().getStatus(),
                        response.getBody().getMessage(),
                        Optional.of(resultList),
                        response.getBody().getErrors()));
              });
    }

    if (sectionName != null) {
      return getCommentGroupsBySection(brdFormId, sourceType, siteId, sectionName);
    }

    if (BrdConstants.SOURCE_TYPE_BRD.equals(sourceType)) {
      return getCommentGroupsByBrdFormId(brdFormId, sourceType);
    } else {
      return getCommentGroupsBySiteId(brdFormId, siteId);
    }
  }

  @Override
  public Throwable handleErrors(Throwable ex) {
    if (ex instanceof BadRequestException
        || ex instanceof NotFoundException
        || ex instanceof AlreadyExistException) {
      return ex;
    } else if (ex instanceof Exception
        && ex.getMessage() != null
        && ex.getMessage().startsWith("Something went wrong:")) {
      return ex;
    } else {
      if (ex instanceof Exception
          && ex.getMessage() != null
          && ex.getMessage().contains("Parent comment not found")) {
        return new NotFoundException(ex.getMessage());
      }
      return new Exception("Something went wrong: " + ex.getMessage());
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Mono<ResponseEntity<Api<Boolean>>> updateEntityFieldFromShadowValue(
      ShadowValueUpdateDTO updateDTO) {
    if (!BrdConstants.SOURCE_TYPE_BRD.equals(updateDTO.getSourceType())
        && !BrdConstants.SOURCE_TYPE_SITE.equals(updateDTO.getSourceType())) {
      return Mono.error(new BadRequestException(BrdConstants.SOURCE_TYPE_ERROR_MESSAGE));
    }

    if (BrdConstants.SOURCE_TYPE_SITE.equals(updateDTO.getSourceType())
        && (updateDTO.getSiteId() == null || updateDTO.getSiteId().trim().isEmpty())) {
      return Mono.error(new BadRequestException(BrdConstants.SITE_ID_REQUIRED_ERROR));
    }

    return findAndProcessCommentGroupUpdate(updateDTO).onErrorMap(this::handleErrors);
  }

  private Mono<ResponseEntity<Api<Boolean>>> findAndProcessCommentGroupUpdate(
      ShadowValueUpdateDTO updateDTO) {

    Mono<BrdFieldCommentGroup> findGroup;
    if (BrdConstants.SOURCE_TYPE_BRD.equals(updateDTO.getSourceType())) {
      findGroup =
          commentGroupRepository.findByBrdFormIdAndSourceTypeAndSectionNameAndFieldPath(
              updateDTO.getBrdFormId(),
              updateDTO.getSourceType(),
              updateDTO.getSectionName(),
              updateDTO.getFieldPath());
    } else {
      findGroup =
          commentGroupRepository.findByBrdFormIdAndSiteIdAndSourceTypeAndSectionNameAndFieldPath(
              updateDTO.getBrdFormId(),
              updateDTO.getSiteId(),
              updateDTO.getSourceType(),
              updateDTO.getSectionName(),
              updateDTO.getFieldPath());
    }

    return findGroup
        .switchIfEmpty(
            Mono.error(new NotFoundException(BrdConstants.COMMENT_GROUP_NOT_FOUND_PARAMETERS)))
        .flatMap(
            group -> {
              if (group.getFieldPathShadowValue() == null) {
                return Mono.error(new BadRequestException(BrdConstants.NO_SHADOW_VALUE));
              }

              Object shadowValue = group.getFieldPathShadowValue();
              String sourceType = group.getSourceType();

              Mono<ResponseEntity<Api<Boolean>>> updateOperation;

              if (BrdConstants.SOURCE_TYPE_BRD.equalsIgnoreCase(sourceType)) {
                updateOperation =
                    updateBrdField(
                        updateDTO.getBrdFormId(),
                        updateDTO.getFieldPath(),
                        shadowValue,
                        updateDTO.getSectionName());
              } else if (BrdConstants.SOURCE_TYPE_SITE.equalsIgnoreCase(sourceType)) {
                updateOperation =
                    updateSiteField(
                        updateDTO.getSiteId(),
                        updateDTO.getFieldPath(),
                        shadowValue,
                        updateDTO.getSectionName());
              } else {
                return Mono.just(
                    ResponseEntity.badRequest()
                        .body(
                            new Api<>(
                                BrdConstants.FAILURE,
                                "Unsupported entity type: " + sourceType,
                                Optional.of(false),
                                Optional.empty())));
              }

              return updateOperation.flatMap(
                  updateResult -> {
                    if (updateResult.getStatusCode().is2xxSuccessful()
                        && updateResult.getBody() != null
                        && BrdConstants.SUCCESSFUL.equals(updateResult.getBody().getStatus())) {

                      return updateCommentGroupStatus(
                              updateDTO.getBrdFormId(),
                              updateDTO.getSourceType(),
                              updateDTO.getSiteId(),
                              updateDTO.getSectionName(),
                              updateDTO.getFieldPath(),
                              "Resolved")
                          .then(
                              Mono.just(
                                  ResponseEntity.ok(
                                      new Api<>(
                                          BrdConstants.SUCCESSFUL,
                                          "Field updated with shadow value and comment group marked as resolved",
                                          Optional.of(true),
                                          Optional.empty()))));
                    }
                    return Mono.just(updateResult);
                  });
            });
  }

  private Mono<ResponseEntity<Api<Boolean>>> updateBrdField(
      String brdFormId, String fieldPath, Object value, String sectionName) {
    if (!isSectionValidForBrd(sectionName)) {
      return Mono.error(
          new BadRequestException("Invalid section name: " + sectionName + " for BRD"));
    }

    Query query = Query.query(Criteria.where(BrdConstants.FIELD_ID).is(brdFormId));
    query.fields().include(sectionName);

    return reactiveMongoTemplate
        .findOne(query, Document.class, BrdConstants.COLLECTION_BRD)
        .switchIfEmpty(Mono.error(new NotFoundException("BRD not found with ID: " + brdFormId)))
        .flatMap(
            document -> {
              Map<String, Object> updateFields = new HashMap<>();
              Map<String, Object> sectionData = new HashMap<>();

              if (document.get(sectionName) instanceof Document sectionDoc) {
                sectionDoc.forEach(sectionData::put);
              } else if (document.get(sectionName) != null) {
                return Mono.error(new NotFoundException("Section not found"));
              }

              if (fieldPath.contains(".")) {
                updateNestedField(sectionData, fieldPath, value);
              } else {
                sectionData.put(fieldPath, value);
              }
              updateFields.put(sectionName, sectionData);

              return callBrdServiceUpdate(brdFormId, updateFields);
            });
  }

  private Mono<ResponseEntity<Api<Boolean>>> callBrdServiceUpdate(
      String brdFormId, Map<String, Object> updateFields) {
    return brdService
        .updateBrdPartiallyWithOrderedOperations(brdFormId, updateFields)
        .map(
            response -> {
              if (response.getStatusCode().is2xxSuccessful()
                  && response.getBody() != null
                  && BrdConstants.SUCCESSFUL.equals(response.getBody().getStatus())) {

                return ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "Field updated successfully with shadow value",
                        Optional.of(true),
                        Optional.empty()));
              } else {
                return ResponseEntity.status(response.getStatusCode())
                    .body(
                        new Api<>(
                            BrdConstants.FAILURE,
                            response.getBody() != null
                                ? response.getBody().getMessage()
                                : "Update failed",
                            Optional.of(false),
                            response.getBody() != null
                                ? response.getBody().getErrors()
                                : Optional.empty()));
              }
            })
        .onErrorMap(this::handleErrors);
  }

  private void updateNestedField(Map<String, Object> data, String fieldPath, Object value) {
    String[] parts = fieldPath.split("\\.", 2);
    String currentKey = parts[0];

    if (parts.length == 1) {
      data.put(currentKey, value);
      return;
    }

    Map<String, Object> nestedMap;
    if (data.containsKey(currentKey) && data.get(currentKey) instanceof Map) {
      nestedMap = (Map<String, Object>) data.get(currentKey);
    } else {
      nestedMap = new HashMap<>();
      data.put(currentKey, nestedMap);
    }

    updateNestedField(nestedMap, parts[1], value);
  }

  private Mono<ResponseEntity<Api<Boolean>>> updateSiteField(
      String siteId, String fieldPath, Object value, String sectionName) {
    if (!isSectionValidForBrd(sectionName)) {
      return Mono.error(
          new BadRequestException("Invalid section name: " + sectionName + " for Site's brdForm"));
    }

    Query query = Query.query(Criteria.where(BrdConstants.FIELD_ID).is(siteId));
    query.fields().include(BrdConstants.FIELD_BRD_FORM + "." + sectionName);

    return reactiveMongoTemplate
        .findOne(query, Document.class, BrdConstants.COLLECTION_SITES)
        .switchIfEmpty(Mono.error(new NotFoundException("Site not found with ID: " + siteId)))
        .flatMap(
            document ->
                processAndExecuteSiteUpdate(document, siteId, sectionName, fieldPath, value));
  }

  private Mono<ResponseEntity<Api<Boolean>>> processAndExecuteSiteUpdate(
      Document document, String siteId, String sectionName, String fieldPath, Object value) {
    Document brdForm = null;
    if (document.get(BrdConstants.FIELD_BRD_FORM) instanceof Document brdFormDoc) {
      brdForm = brdFormDoc;
    }

    if (brdForm == null) {
      brdForm = new Document();
    }

    Map<String, Object> sectionData = new HashMap<>();
    if (brdForm.get(sectionName) instanceof Document sectionDoc) {
      sectionDoc.forEach(sectionData::put);
    }

    Object oldValue = null;
    if (fieldPath.contains(".")) {
      String[] parts = fieldPath.split("\\.", 2);
      if (sectionData.containsKey(parts[0]) && sectionData.get(parts[0]) instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> nestedMap = (Map<String, Object>) sectionData.get(parts[0]);
        oldValue = getNestedFieldValue(nestedMap, parts[1]);
      }
    } else {
      oldValue = sectionData.get(fieldPath);
    }

    Map<String, Object> oldValues = new HashMap<>();
    Map<String, Object> newValues = new HashMap<>();

    String fullFieldPath = sectionName + "." + fieldPath;
    oldValues.put(fullFieldPath, oldValue);
    newValues.put(fullFieldPath, value);

    if (fieldPath.contains(".")) {
      updateNestedField(sectionData, fieldPath, value);
    } else {
      sectionData.put(fieldPath, value);
    }

    Update update = new Update();
    update.set(BrdConstants.FIELD_BRD_FORM + "." + sectionName, sectionData);

    return reactiveMongoTemplate
        .update(Site.class)
        .matching(Query.query(Criteria.where(BrdConstants.FIELD_ID).is(siteId)))
        .apply(update)
        .first()
        .map(
            result -> {
              if (result.getModifiedCount() > 0) {
                AuditLogRequest auditLogRequest =
                    AuditLogRequest.builder()
                        .entityType("Site")
                        .entityId(siteId)
                        .action("UPDATE")
                        .userId("SYSTEM")
                        .userName("System")
                        .eventTimestamp(LocalDateTime.now())
                        .oldValues(oldValues)
                        .newValues(newValues)
                        .build();

                logUpdateAsynchronously(auditLogRequest);

                return ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "Site field updated successfully with shadow value",
                        Optional.of(true),
                        Optional.empty()));
              } else {
                return ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "No changes were required to the Site field",
                        Optional.of(true),
                        Optional.empty()));
              }
            })
        .onErrorMap(this::handleErrors);
  }

  private boolean isSectionValidForBrd(String sectionName) {
    return List.of(
            "clientInformation",
            "aciInformation",
            "paymentChannels",
            "fundingMethods",
            "achPaymentProcessing",
            "miniAccountMaster",
            "accountIdentifierInformation",
            "paymentRules",
            "notifications",
            "remittance",
            "agentPortal",
            "recurringPayments",
            "ivr",
            "generalImplementations",
            "approvals",
            "revisionHistory")
        .contains(sectionName);
  }

  private Object getNestedFieldValue(Map<String, Object> data, String fieldPath) {
    String[] parts = fieldPath.split("\\.", 2);
    String currentKey = parts[0];

    if (parts.length == 1) {
      return data.get(currentKey);
    }

    if (data.containsKey(currentKey) && data.get(currentKey) instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> nestedMap = (Map<String, Object>) data.get(currentKey);
      return getNestedFieldValue(nestedMap, parts[1]);
    }

    return null;
  }

  private void logUpdateAsynchronously(AuditLogRequest auditLogRequest) {
    auditLogService.logCreation(auditLogRequest).onErrorMap(this::handleErrors).subscribe();
  }

  private Mono<Void> updateCommentGroupStatus(
      String brdFormId,
      String sourceType,
      String siteId,
      String sectionName,
      String fieldPath,
      String status) {
    Mono<BrdFieldCommentGroup> findGroup;
    if (BrdConstants.SOURCE_TYPE_BRD.equals(sourceType)) {
      findGroup =
          commentGroupRepository.findByBrdFormIdAndSourceTypeAndSectionNameAndFieldPath(
              brdFormId, sourceType, sectionName, fieldPath);
    } else {
      findGroup =
          commentGroupRepository.findByBrdFormIdAndSiteIdAndSourceTypeAndSectionNameAndFieldPath(
              brdFormId, siteId, sourceType, sectionName, fieldPath);
    }

    return findGroup
        .switchIfEmpty(
            Mono.error(new NotFoundException(BrdConstants.FIELD_COMMENT_GROUP_NOT_FOUND)))
        .flatMap(
            group -> {
              group.setStatus(status);
              return commentGroupRepository.save(group);
            })
        .then();
  }

  @Override
  public Mono<ResponseEntity<Api<CommentStatsByStatusResponse>>> getCommentStatsByStatus(
      String brdFormId, String status) {
    if (brdFormId == null || brdFormId.isBlank()) {
      return Mono.error(new BadRequestException("BRD Form ID cannot be null or empty"));
    }

    if (status == null
        || status.isBlank()
        || (!BrdConstants.COMMENT_STATUS_PENDING.equals(status)
            && !BrdConstants.COMMENT_STATUS_RESOLVED.equals(status))) {
      return Mono.error(new BadRequestException("Status must be either 'Pending' or 'Resolved'"));
    }

    Query query =
        Query.query(
            Criteria.where(BrdConstants.BRD_FORM_ID)
                .is(brdFormId)
                .and(BrdConstants.STATUS_FIELD)
                .is(status));

    return reactiveMongoTemplate
        .find(query, BrdFieldCommentGroup.class)
        .collectList()
        .flatMap(
            allCommentGroups -> {
              if (allCommentGroups.isEmpty()) {
                return Mono.just(
                    ResponseEntity.ok(
                        new Api<>(
                            BrdConstants.SUCCESSFUL,
                            "No " + status.toLowerCase() + " comments found for BRD: " + brdFormId,
                            Optional.of(createEmptyResponse()),
                            Optional.empty())));
              }

              // Process the comment groups and create the response
              return processCommentGroupsByStatus(allCommentGroups, brdFormId, status);
            })
        .onErrorResume(
            e -> {
              if (e instanceof BadRequestException || e instanceof NotFoundException) {
                return Mono.error(e);
              }
              return Mono.error(
                  new Exception(
                      "Error retrieving "
                          + status.toLowerCase()
                          + " comment stats: "
                          + e.getMessage()));
            });
  }

  private Mono<ResponseEntity<Api<CommentStatsByStatusResponse>>> processCommentGroupsByStatus(
      List<BrdFieldCommentGroup> allCommentGroups, String brdFormId, String status) {

    List<BrdFieldCommentGroup> brdGroups = new ArrayList<>();
    Map<String, List<BrdFieldCommentGroup>> siteGroupsMap = new HashMap<>();

    for (BrdFieldCommentGroup group : allCommentGroups) {
      if (BrdConstants.SOURCE_TYPE_BRD.equals(group.getSourceType())) {
        brdGroups.add(group);
      } else if (BrdConstants.SOURCE_TYPE_SITE.equals(group.getSourceType())
          && group.getSiteId() != null) {
        siteGroupsMap.computeIfAbsent(group.getSiteId(), k -> new ArrayList<>()).add(group);
      }
    }

    Mono<List<BrdFieldCommentGroupResp>> brdCommentsMono =
        Flux.fromIterable(brdGroups).map(dtoModelMapper::mapToGroupResponse).collectList();

    Mono<Map<String, List<BrdFieldCommentGroupResp>>> siteCommentsMono =
        Flux.fromIterable(siteGroupsMap.entrySet())
            .flatMap(
                entry -> {
                  String siteId = entry.getKey();
                  List<BrdFieldCommentGroup> siteGroups = entry.getValue();

                  return Flux.fromIterable(siteGroups)
                      .map(dtoModelMapper::mapToGroupResponse)
                      .collectList()
                      .map(groupResps -> Map.entry(siteId, groupResps));
                })
            .collectMap(Map.Entry::getKey, Map.Entry::getValue);

    return Mono.zip(brdCommentsMono, siteCommentsMono)
        .map(
            tuple -> {
              List<BrdFieldCommentGroupResp> brdComments = tuple.getT1();
              Map<String, List<BrdFieldCommentGroupResp>> siteComments = tuple.getT2();

              int brdCount = brdComments.size();
              Map<String, Integer> siteCounts =
                  siteComments.entrySet().stream()
                      .collect(
                          Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().size()));
              int totalCount =
                  brdCount + siteCounts.values().stream().mapToInt(Integer::intValue).sum();

              CommentStatsByStatusResponse response =
                  CommentStatsByStatusResponse.builder()
                      .totalCount(totalCount)
                      .brdCount(brdCount)
                      .brdComments(brdComments)
                      .siteCounts(siteCounts)
                      .siteComments(siteComments)
                      .build();

              String message =
                  status.equals(BrdConstants.COMMENT_STATUS_PENDING)
                      ? BrdConstants.PENDING_COMMENTS_RETRIEVED_SUCCESS
                      : "Resolved comments retrieved successfully";

              return ResponseEntity.ok(
                  new Api<>(
                      BrdConstants.SUCCESSFUL,
                      message + " for BRD: " + brdFormId,
                      Optional.of(response),
                      Optional.empty()));
            });
  }

  public CommentStatsByStatusResponse createEmptyResponse() {
    return CommentStatsByStatusResponse.builder()
        .totalCount(0)
        .brdCount(0)
        .brdComments(Collections.emptyList())
        .siteCounts(Collections.emptyMap())
        .siteComments(Collections.emptyMap())
        .build();
  }

  @Override
  public Mono<ResponseEntity<Api<CommentsBySourceResponse>>> getCommentsBySource(
      String brdFormId, String sourceType, String siteId, String status) {

    if (brdFormId == null || brdFormId.isBlank()) {
      return Mono.error(new BadRequestException("BRD Form ID cannot be null or empty"));
    }

    if (sourceType == null
        || sourceType.isBlank()
        || (!BrdConstants.SOURCE_TYPE_BRD.equals(sourceType)
            && !BrdConstants.SOURCE_TYPE_SITE.equals(sourceType))) {
      return Mono.error(new BadRequestException(BrdConstants.SOURCE_TYPE_ERROR_MESSAGE));
    }

    if (BrdConstants.SOURCE_TYPE_SITE.equals(sourceType) && (siteId == null || siteId.isBlank())) {
      return Mono.error(new BadRequestException(BrdConstants.SITE_ID_REQUIRED_ERROR));
    }

    if (status != null
        && !status.isBlank()
        && !BrdConstants.COMMENT_STATUS_PENDING.equals(status)
        && !BrdConstants.COMMENT_STATUS_RESOLVED.equals(status)) {
      return Mono.error(new BadRequestException("Status must be either 'Pending' or 'Resolved'"));
    }

    Flux<BrdFieldCommentGroup> commentGroupsFlux =
        buildCommentQuery(brdFormId, sourceType, siteId, status);

    return processCommentResults(commentGroupsFlux, brdFormId, sourceType, siteId, status);
  }

  private Flux<BrdFieldCommentGroup> buildCommentQuery(
      String brdFormId, String sourceType, String siteId, String status) {

    boolean isBrdSourceType = BrdConstants.SOURCE_TYPE_BRD.equals(sourceType);
    boolean hasStatusFilter = status != null && !status.isBlank();

    if (isBrdSourceType) {

      if (!hasStatusFilter) {

        return commentGroupRepository.findByBrdFormIdAndSourceType(brdFormId, sourceType);
      } else {

        Query query =
            Query.query(
                Criteria.where(BrdConstants.BRD_FORM_ID)
                    .is(brdFormId)
                    .and(BrdConstants.FIELD_SOURCE_TYPE)
                    .is(sourceType)
                    .and(BrdConstants.STATUS_FIELD)
                    .is(status));
        return reactiveMongoTemplate.find(query, BrdFieldCommentGroup.class);
      }
    } else {

      if (!hasStatusFilter) {

        return commentGroupRepository.findByBrdFormIdAndSiteIdAndSourceType(
            brdFormId, siteId, sourceType);
      } else {

        Query query =
            Query.query(
                Criteria.where(BrdConstants.BRD_FORM_ID)
                    .is(brdFormId)
                    .and(BrdConstants.FIELD_SITE_ID)
                    .is(siteId)
                    .and(BrdConstants.FIELD_SOURCE_TYPE)
                    .is(sourceType)
                    .and(BrdConstants.STATUS_FIELD)
                    .is(status));
        return reactiveMongoTemplate.find(query, BrdFieldCommentGroup.class);
      }
    }
  }

  private Mono<ResponseEntity<Api<CommentsBySourceResponse>>> processCommentResults(
      Flux<BrdFieldCommentGroup> commentGroupsFlux,
      String brdFormId,
      String sourceType,
      String siteId,
      String status) {

    return commentGroupsFlux
        .collectList()
        .flatMap(
            commentGroups -> {
              if (commentGroups.isEmpty()) {
                // Return empty response
                String statusMsg =
                    (status == null || status.isBlank()) ? "" : " with status '" + status + "'";
                String entityType = isSiteSourceType(sourceType) ? "Site" : "BRD";
                String entityId = isSiteSourceType(sourceType) ? siteId : brdFormId;

                return Mono.just(
                    ResponseEntity.ok(
                        new Api<>(
                            BrdConstants.SUCCESSFUL,
                            String.format(
                                "No comments found for %s: %s%s", entityType, entityId, statusMsg),
                            Optional.of(createEmptySourceResponse(brdFormId, sourceType, siteId)),
                            Optional.empty())));
              }

              return processCommentGroupsBySource(
                  commentGroups, brdFormId, sourceType, siteId, status);
            })
        .onErrorMap(this::handleErrors);
  }

  private Mono<ResponseEntity<Api<CommentsBySourceResponse>>> processCommentGroupsBySource(
      List<BrdFieldCommentGroup> commentGroups,
      String brdFormId,
      String sourceType,
      String siteId,
      String status) {

    return Flux.fromIterable(commentGroups)
        .map(dtoModelMapper::mapToGroupResponse)
        .collectList()
        .map(
            groupResponses -> {
              CommentsBySourceResponse response =
                  buildSourceResponse(groupResponses, brdFormId, sourceType, siteId);

              return createApiResponse(response, brdFormId, sourceType, siteId, status);
            });
  }

  private CommentsBySourceResponse buildSourceResponse(
      List<BrdFieldCommentGroupResp> groupResponses,
      String brdFormId,
      String sourceType,
      String siteId) {

    int totalCount = groupResponses.size();

    Map<String, Integer> sectionCounts = new HashMap<>();
    Map<String, List<String>> sectionFieldPaths = new HashMap<>();

    for (BrdFieldCommentGroupResp group : groupResponses) {

      String section = group.getSectionName();
      updateSectionCount(sectionCounts, section);

      updateSectionFieldPaths(sectionFieldPaths, section, group.getFieldPath());
    }

    return CommentsBySourceResponse.builder()
        .totalCount(totalCount)
        .sourceType(sourceType)
        .brdFormId(brdFormId)
        .siteId(isSiteSourceType(sourceType) ? siteId : null)
        .comments(groupResponses)
        .sectionCounts(sectionCounts)
        .sectionFieldPaths(sectionFieldPaths)
        .build();
  }

  private void updateSectionCount(Map<String, Integer> sectionCounts, String section) {
    sectionCounts.compute(section, (k, v) -> (v == null) ? 1 : v + 1);
  }

  private void updateSectionFieldPaths(
      Map<String, List<String>> sectionFieldPaths, String section, String fieldPath) {
    sectionFieldPaths.compute(
        section,
        (k, v) -> {
          if (v == null) {
            List<String> paths = new ArrayList<>();
            paths.add(fieldPath);
            return paths;
          } else {
            if (!v.contains(fieldPath)) {
              v.add(fieldPath);
            }
            return v;
          }
        });
  }

  private boolean isSiteSourceType(String sourceType) {
    return BrdConstants.SOURCE_TYPE_SITE.equals(sourceType);
  }

  private ResponseEntity<Api<CommentsBySourceResponse>> createApiResponse(
      CommentsBySourceResponse response,
      String brdFormId,
      String sourceType,
      String siteId,
      String status) {

    String message = buildResponseMessage(brdFormId, sourceType, siteId, status);

    return ResponseEntity.ok(
        new Api<>(BrdConstants.SUCCESSFUL, message, Optional.of(response), Optional.empty()));
  }

  private String buildResponseMessage(
      String brdFormId, String sourceType, String siteId, String status) {
    String entityType = isSiteSourceType(sourceType) ? "Site" : "BRD";
    String entityId = isSiteSourceType(sourceType) ? siteId : brdFormId;
    String statusSuffix =
        (status == null || status.isBlank()) ? "" : " with status '" + status + "'";

    return String.format(
        "Comments retrieved successfully for %s: %s%s", entityType, entityId, statusSuffix);
  }

  private CommentsBySourceResponse createEmptySourceResponse(
      String brdFormId, String sourceType, String siteId) {
    return CommentsBySourceResponse.builder()
        .totalCount(0)
        .sourceType(sourceType)
        .brdFormId(brdFormId)
        .siteId(isSiteSourceType(sourceType) ? siteId : null)
        .comments(Collections.emptyList())
        .sectionCounts(Collections.emptyMap())
        .sectionFieldPaths(Collections.emptyMap())
        .build();
  }

  @Override
  public Mono<ResponseEntity<Api<Boolean>>> updateCommentReadStatus(
      String brdFormId,
      String sourceType,
      String siteId,
      String sectionName,
      String fieldPath,
      UpdateCommentReadStatusRequest request) {

    if (BrdConstants.SOURCE_TYPE_SITE.equals(sourceType) && (siteId == null || siteId.isBlank())) {
      return Mono.error(new BadRequestException(BrdConstants.SITE_ID_REQUIRED_ERROR));
    }

    Mono<BrdFieldCommentGroup> commentGroupMono;
    if (BrdConstants.SOURCE_TYPE_BRD.equals(sourceType)) {
      commentGroupMono =
          commentGroupRepository.findByBrdFormIdAndSourceTypeAndSectionNameAndFieldPath(
              brdFormId, sourceType, sectionName, fieldPath);
    } else {
      commentGroupMono =
          commentGroupRepository.findByBrdFormIdAndSiteIdAndSourceTypeAndSectionNameAndFieldPath(
              brdFormId, siteId, sourceType, sectionName, fieldPath);
    }

    return commentGroupMono
        .switchIfEmpty(
            Mono.error(new NotFoundException(BrdConstants.FIELD_COMMENT_GROUP_NOT_FOUND)))
        .flatMap(commentGroup -> updateCommentReadStatusInGroup(commentGroup, request))
        .map(
            updated ->
                ResponseEntity.ok(
                    new Api<>(
                        BrdConstants.SUCCESSFUL,
                        "Comment read status updated successfully",
                        Optional.of(updated),
                        Optional.empty())))
        .onErrorMap(this::handleErrors);
  }

  private Mono<Boolean> updateCommentReadStatusInGroup(
      BrdFieldCommentGroup commentGroup, UpdateCommentReadStatusRequest request) {

    String commentId = request.getCommentId();

    Optional<CommentEntry> commentOpt =
        commentGroup.getComments().stream()
            .filter(comment -> commentId.equals(comment.getId()))
            .findFirst();

    if (commentOpt.isPresent()) {
      CommentEntry comment = commentOpt.get();
      comment.setIsRead(request.getIsRead());
      comment.setUpdatedAt(LocalDateTime.now());

      return commentGroupRepository.save(commentGroup).map(saved -> true);
    }

    return Mono.error(new NotFoundException("Comment not found with ID: " + commentId));
  }
}

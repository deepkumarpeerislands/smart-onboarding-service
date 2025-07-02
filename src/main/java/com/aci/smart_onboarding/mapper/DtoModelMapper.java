package com.aci.smart_onboarding.mapper;

import com.aci.smart_onboarding.constants.BrdConstants;
import com.aci.smart_onboarding.dto.*;
import com.aci.smart_onboarding.enums.PortalTypes;
import com.aci.smart_onboarding.exception.BadRequestException;
import com.aci.smart_onboarding.model.*;
import com.aci.smart_onboarding.model.AuditLog;
import com.aci.smart_onboarding.model.BRD;
import com.aci.smart_onboarding.model.BrdFieldCommentGroup;
import com.aci.smart_onboarding.model.BrdTemplateConfig;
import com.aci.smart_onboarding.model.UATTestCase;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class DtoModelMapper {

  private final ModelMapper modelMapper;
  private final ObjectMapper objectMapper;

  @PostConstruct
  public void init() {
    modelMapper
        .getConfiguration()
        .setMatchingStrategy(MatchingStrategies.STRICT)
        .setSkipNullEnabled(true);

    modelMapper
        .createTypeMap(AuditLogRequest.class, AuditLog.class)
        .addMappings(mapper -> mapper.skip(AuditLog::setAuditId));
  }

  public BRDResponse mapToBrdResponse(BRD brd) {
    try {
      BRDResponse response = modelMapper.map(brd, BRDResponse.class);
      // Ensure walletronId is mapped correctly
      response.setWalletronId(brd.getWalletronId());
      return response;
    } catch (Exception ex) {
      throw new BadRequestException("Error mapping BRD: " + ex.getMessage());
    }
  }

  public BRD mapToBrd(BRDRequest brdRequest) {
    try {
      return modelMapper.map(brdRequest, BRD.class);
    } catch (Exception ex) {
      throw new BadRequestException("Error mapping BRDRequest: " + ex.getMessage());
    }
  }

  public PrefillSections mapResponseToPrefillSections(BRDResponse brdResponse) {
    try {
      return modelMapper.map(brdResponse, PrefillSections.class);
    } catch (Exception ex) {
      throw new BadRequestException(
          "Error mapping BRDResponse to PrefillSections: " + ex.getMessage());
    }
  }

  public BRDListResponse mapToBrdListResponse(BRD brd) {
    try {
      return modelMapper.map(brd, BRDListResponse.class);
    } catch (Exception ex) {
      throw new BadRequestException("Error mapping BRD: " + ex.getMessage());
    }
  }

  public AuditLog mapToAuditLog(AuditLogRequest auditLogRequest) {
    try {
      AuditLog auditLog = modelMapper.map(auditLogRequest, AuditLog.class);
      // Let MongoDB generate the ID
      auditLog.setAuditId(null);
      return auditLog;
    } catch (Exception ex) {
      throw new BadRequestException("Error mapping auditLogRequest: " + ex.getMessage());
    }
  }

  public AuditLogResponse mapToAuditLogResponse(AuditLog auditLog) {
    try {
      return modelMapper.map(auditLog, AuditLogResponse.class);
    } catch (Exception ex) {
      throw new BadRequestException("Error mapping AuditLog to response: " + ex.getMessage());
    }
  }

  public Map<String, Object> mapBrdResponseToMAP(BRDResponse brdResponse) {
    try {
      Map<String, Object> map =
          objectMapper.convertValue(brdResponse, new TypeReference<Map<String, Object>>() {});

      // Ensure status is explicitly included
      if (brdResponse.getStatus() != null) {
        map.put(BrdConstants.STATUS_FIELD, brdResponse.getStatus());
      }

      return map;
    } catch (Exception ex) {
      throw new BadRequestException("Error mapping BRDResponse to map: " + ex.getMessage());
    }
  }

  public Object getFieldValue(Object object, String fieldName) {
    if (object == null || fieldName == null) {
      return null;
    }

    try {
      if (fieldName.contains(".")) {
        String[] fields = fieldName.split("\\.");
        Object currentObject = object;

        for (String field : fields) {
          if (currentObject == null) {
            return null;
          }
          currentObject = getSimpleFieldValue(currentObject, field);
        }
        return currentObject;
      }
      return getSimpleFieldValue(object, fieldName);

    } catch (Exception e) {
      return null;
    }
  }

  private Object getSimpleFieldValue(Object object, String fieldName) {
    if (object == null || fieldName == null) {
      return null;
    }

    try {
      return Objects.requireNonNull(BeanUtils.getPropertyDescriptor(object.getClass(), fieldName))
          .getReadMethod()
          .invoke(object);

    } catch (Exception e) {
      return null;
    }
  }

  public BRDSearchResponse mapToSearchResponse(Document doc) {
    try {
      return BRDSearchResponse.builder()
          .brdFormId(doc.getObjectId("_id").toString())
          .brdId(doc.getString("brdId"))
          .customerId(doc.getString("customerId"))
          .brdName(doc.getString("brdName"))
          .creator(doc.getString("creator"))
          .type(doc.getString("type"))
          .status(doc.getString("status"))
          .notes(doc.getString("notes"))
          .templateFileName(doc.getString("templateFileName"))
          .build();
    } catch (Exception ex) {
      throw new BadRequestException(
          "Error mapping Document to BRDSearchResponse: " + ex.getMessage());
    }
  }

  public Map<String, Object> mapDivisionResponseToMAP(SiteResponse divisionResponse) {
    Map<String, Object> map = new HashMap<>();

    map.put("brdId", divisionResponse.getBrdId());
    map.put("wallentronIncluded", divisionResponse.isWallentronIncluded());
    map.put("achEncrypted", divisionResponse.isAchEncrypted());

    // Add division list details
    List<Map<String, Object>> divisionsList = new ArrayList<>();
    for (SiteResponse.DivisionDetails divisionDetails : divisionResponse.getSiteList()) {
      Map<String, Object> divisionMap = new HashMap<>();
      divisionMap.put("divisionId", divisionDetails.getSiteId());
      divisionMap.put("divisionName", divisionDetails.getSiteName());
      divisionMap.put("identifierCode", divisionDetails.getIdentifierCode());
      divisionMap.put("description", divisionDetails.getDescription());
      Map<String, Object> apply = divisionMap;
      divisionsList.add(apply);
    }
    map.put("divisionList", divisionsList);

    // Add audit fields
    map.put("createdAt", divisionResponse.getCreatedAt());
    map.put("updatedAt", divisionResponse.getUpdatedAt());

    return map;
  }

  public com.aci.smart_onboarding.model.BrdTemplateConfig mapToBrdTemplateConfig(
      BrdTemplateReq brdTemplateReq) {
    try {
      return modelMapper.map(brdTemplateReq, BrdTemplateConfig.class);
    } catch (Exception ex) {
      throw new BadRequestException("Error mapping BrdTemplateReq: " + ex.getMessage());
    }
  }

  public BrdTemplateRes mapToBrdTemplateConfigResponse(BrdTemplateConfig brdTemplateConfig) {
    try {
      return modelMapper.map(brdTemplateConfig, BrdTemplateRes.class);
    } catch (Exception ex) {
      throw new BadRequestException("Error mapping BrdTemplateConfig: " + ex.getMessage());
    }
  }

  public BrdFieldCommentGroupResp mapToGroupResponse(BrdFieldCommentGroup group) {
    List<CommentEntryResp> commentResponses =
        group.getComments().stream().map(this::mapToCommentResponse).toList();

    return BrdFieldCommentGroupResp.builder()
        .id(group.getId())
        .brdFormId(group.getBrdFormId())
        .siteId(group.getSiteId())
        .sourceType(group.getSourceType())
        .fieldPath(group.getFieldPath())
        .fieldPathShadowValue(group.getFieldPathShadowValue())
        .status(group.getStatus())
        .sectionName(group.getSectionName())
        .createdBy(group.getCreatedBy())
        .comments(commentResponses)
        .createdAt(group.getCreatedAt())
        .updatedAt(group.getUpdatedAt())
        .build();
  }

  public CommentEntryResp mapToCommentResponse(BrdFieldCommentGroup.CommentEntry comment) {
    return CommentEntryResp.builder()
        .id(comment.getId())
        .content(comment.getContent())
        .createdBy(comment.getCreatedBy())
        .userType(comment.getUserType())
        .parentCommentId(comment.getParentCommentId())
        .isRead(comment.getIsRead())
        .createdAt(comment.getCreatedAt())
        .updatedAt(comment.getUpdatedAt())
        .build();
  }

  public UATTestCaseDTO mapToUATTestCaseDTO(UATTestCase uatTestCase) {
    try {
      return modelMapper.map(uatTestCase, UATTestCaseDTO.class);
    } catch (Exception ex) {
      throw new BadRequestException("Error mapping UATTestCase to DTO: " + ex.getMessage());
    }
  }

  public UATTestCase mapToUATTestCase(UATTestCaseDTO dto) {
    try {
      return modelMapper.map(dto, UATTestCase.class);
    } catch (Exception ex) {
      throw new BadRequestException("Error mapping UATTestCaseDTO to entity: " + ex.getMessage());
    }
  }

  public UATTestCase mapToUATTestCase(UATTestCaseRequestResponseDTO dto) {
    try {
      return modelMapper.map(dto, UATTestCase.class);
    } catch (Exception ex) {
      throw new BadRequestException(
          "Error mapping UATTestCaseRequestResponseDTO to entity: " + ex.getMessage());
    }
  }

  /**
   * Maps a UATTestCase entity to UATTestCaseRequestResponseDTO.
   *
   * @param uatTestCase The source test case entity
   * @return Mapped UATTestCaseRequestResponseDTO
   */
  public UATTestCaseRequestResponseDTO mapToUATTestCaseRequestResponseDTO(UATTestCase uatTestCase) {
    try {
      return modelMapper.map(uatTestCase, UATTestCaseRequestResponseDTO.class);
    } catch (Exception ex) {
      throw new BadRequestException(
          "Error mapping UATTestCase to RequestResponseDTO: " + ex.getMessage());
    }
  }

  /**
   * Maps a UATTestCaseDTO to UATTestCaseRequestResponseDTO with the specified portal type.
   *
   * @param testCase The source test case DTO
   * @param portalType The portal type to set in the response DTO
   * @return Mapped UATTestCaseRequestResponseDTO
   */
  public UATTestCaseRequestResponseDTO mapToUATTestCaseRequestResponseDTOWithPortalType(
      UATTestCaseDTO testCase, PortalTypes portalType) {
    try {
      UATTestCaseRequestResponseDTO responseDTO =
          modelMapper.map(testCase, UATTestCaseRequestResponseDTO.class);
      responseDTO.setUatType(portalType);
      return responseDTO;
    } catch (Exception ex) {
      throw new BadRequestException(
          "Error mapping UATTestCaseDTO to RequestResponseDTO: " + ex.getMessage());
    }
  }

  public WalletronResponse mapToWalletronResponse(Walletron walletron) {
    WalletronResponse response = new WalletronResponse();
    response.setBrdId(walletron.getBrdId());
    response.setBrdName(walletron.getBrdName());
    response.setWalletronEnabled(walletron.getWalletronEnabled());

    response.setSiteConfiguration(walletron.getSiteConfiguration());
    response.setNotificationsOptions(walletron.getNotificationsOptions());
    response.setAciWalletronAgentPortal(walletron.getAciWalletronAgentPortal());
    response.setAciWalletronDataExchange(walletron.getAciWalletronDataExchange());
    response.setAciWalletronEnrollmentStrategy(walletron.getAciWalletronEnrollmentStrategy());
    response.setEnrollmentUrl(walletron.getEnrollmentUrl());
    response.setTargetedCommunication(walletron.getTargetedCommunication());
    response.setAciCash(walletron.getAciCash());
    response.setWalletronApprovals(walletron.getWalletronApprovals());
    response.setCreatedAt(walletron.getCreatedAt());
    response.setUpdatedAt(walletron.getUpdatedAt());
    response.setWalletronId(walletron.getWalletronId());
    return response;
  }
}

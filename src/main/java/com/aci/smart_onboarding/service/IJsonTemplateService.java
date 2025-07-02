package com.aci.smart_onboarding.service;

import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.JsonTemplateResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

/** Service interface for JSON template operations. */
public interface IJsonTemplateService {

  /**
   * Creates a new JSON template record.
   *
   * @param templateName The unique template name
   * @param fileName The generated file name in storage
   * @param originalFileName The original file name
   * @param uploadedBy The user who uploaded the file
   * @return Mono containing ResponseEntity with Api wrapper containing JsonTemplateResponse
   */
  Mono<ResponseEntity<Api<JsonTemplateResponse>>> createTemplate(
      String templateName, String fileName, String originalFileName, String uploadedBy);

  /**
   * Retrieves all JSON templates with their details.
   *
   * @return Mono containing ResponseEntity with Api wrapper containing List of JsonTemplateResponse
   */
  Mono<ResponseEntity<Api<List<JsonTemplateResponse>>>> getAllTemplates();

  /**
   * Updates template status with single active constraint - ensures only one template can be Active
   * at a time. Uses reactive MongoDB transactions for data consistency and atomic operations.
   *
   * @param templateName The template name to update
   * @param status The new status (Active or InActive)
   * @return Mono containing ResponseEntity with Api wrapper containing Boolean result
   */
  Mono<ResponseEntity<Api<Boolean>>> updateTemplateStatusWithSingleActive(
      String templateName, String status);
}

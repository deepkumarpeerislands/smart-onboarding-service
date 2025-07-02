package com.aci.smart_onboarding.service;

import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.BrdFormResponse;
import com.aci.smart_onboarding.dto.BrdTemplateReq;
import com.aci.smart_onboarding.dto.BrdTemplateRes;
import java.util.List;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

public interface IBrdTemplateService {
  Mono<ResponseEntity<Api<BrdTemplateRes>>> createTemplate(BrdTemplateReq brdTemplateReq);

  Mono<ResponseEntity<Api<BrdTemplateRes>>> updateTemplate(
      String id, BrdTemplateReq brdTemplateReq);

  Mono<ResponseEntity<Api<List<BrdTemplateRes>>>> getAllTemplates();

  Mono<ResponseEntity<Api<BrdTemplateRes>>> getTemplateByType(String brdTemplateType);

  Throwable handleErrors(Throwable ex);

  // Get BRD form as object based on template configuration and BRD ID
  Mono<BrdFormResponse> getBrdForm(String brdId, BrdTemplateRes templateConfig);

  // Get BRD form by ID and template type with proper response structure
  Mono<ResponseEntity<Api<BrdFormResponse>>> getBrdFormByIdAndTemplateType(
      String brdId, String templateType);
}

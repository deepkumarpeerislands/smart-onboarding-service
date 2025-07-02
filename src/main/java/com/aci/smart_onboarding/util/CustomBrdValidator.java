package com.aci.smart_onboarding.util;

import com.aci.smart_onboarding.exception.BadRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CustomBrdValidator {

  private final Validator validator;
  private final ObjectMapper objectMapper;
  private static final Set<String> VALID_FIELDS =
      Set.of(
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
          "revisionHistory",
          "testRigorFlag",
          "testRigorTimeStamp");

  public <T> Mono<Map<String, Object>> validatePartialUpdateField(
      Map<String, Object> fields, Class<T> targetClass) {
    try {
      String json = objectMapper.writeValueAsString(fields);
      T target = objectMapper.readValue(json, targetClass);

      Set<ConstraintViolation<T>> violations = validator.validate(target);

      Map<String, String> errors =
          violations.stream()
              .filter(
                  v -> {
                    String path = v.getPropertyPath().toString();
                    return fields.containsKey(path)
                        || path.contains(".")
                            && fields.containsKey(path.substring(0, path.indexOf(".")));
                  })
              .collect(
                  Collectors.toMap(
                      v -> v.getPropertyPath().toString(), ConstraintViolation::getMessage));

      if (!errors.isEmpty()) {
        return Mono.error(new BadRequestException("Validation failed", errors.toString()));
      }

      return Mono.just(fields);
    } catch (Exception e) {
      return Mono.error(
          new BadRequestException(
              "Invalid request format", Map.of("error", e.getMessage()).toString()));
    }
  }

  public boolean isValidField(String fieldName) {
    return VALID_FIELDS.contains(fieldName);
  }
}

package com.aci.smart_onboarding.dto;

import com.aci.smart_onboarding.enums.PortalTypes;
import com.aci.smart_onboarding.enums.TestStatus;
import com.aci.smart_onboarding.enums.TestType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for UAT test case request and response operations. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "UAT Test Case Request/Response Data")
public class UATTestCaseRequestResponseDTO {

  @NotBlank(message = "BRD ID cannot be blank")
  @Schema(description = "BRD ID", example = "BRD-1234")
  private String brdId;

  @NotBlank(message = "Test name cannot be blank")
  @Schema(description = "Name of the test case", example = "Login Button Test")
  private String testName;

  @NotBlank(message = "Scenario cannot be blank")
  @Schema(description = "Test scenario description", example = "Verify login button functionality")
  private String scenario;

  @NotBlank(message = "Position cannot be blank")
  @Schema(description = "Position of the element", example = "top-right")
  private String position;

  @NotBlank(message = "Answer cannot be blank")
  @Schema(description = "Expected test result", example = "Login button should be clickable")
  private String answer;

  @NotNull(message = "UAT type cannot be null")
  @Schema(description = "Type of portal (AGENT/CONSUMER)", example = "AGENT")
  private PortalTypes uatType;

  @NotNull(message = "Test type cannot be null")
  @Schema(description = "Type of test (REUSABLE/PRE_SUITE/NORMAL)", example = "NORMAL")
  private TestType testType;

  @Schema(description = "Test status (PASSED/FAILED)", example = "PASSED")
  private TestStatus status;

  @Schema(description = "Additional comments", example = "Test passed with expected behavior")
  private String comments;

  @Schema(description = "Feature name", example = "Login Feature")
  private String featureName;

  @Schema(description = "Dynamic field-value pairs for test case configuration")
  private Map<String, String> fields;
}

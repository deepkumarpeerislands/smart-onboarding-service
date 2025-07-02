package com.aci.smart_onboarding.model;

import com.aci.smart_onboarding.enums.PortalTypes;
import com.aci.smart_onboarding.enums.TestStatus;
import com.aci.smart_onboarding.enums.TestType;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Model representing a UAT test case. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "uat_test_cases")
public class UATTestCase {

  @Id private String id;

  private String brdId;

  private String testName;

  private String scenario;

  private String position;

  private String answer;

  private PortalTypes uatType;

  private TestType testType;

  private List<Double> vectors;

  private TestStatus status;

  private String comments;

  private String featureName;

  /** Dynamic field-value pairs for test case configuration. Key: field name, Value: field value */
  private Map<String, String> fields;
}

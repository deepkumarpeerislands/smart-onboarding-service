package com.aci.smart_onboarding.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.aci.smart_onboarding.constants.BrdConstants;
import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.dto.UATTestCaseBrdTypeSearchRequest;
import com.aci.smart_onboarding.dto.UATTestCaseDTO;
import com.aci.smart_onboarding.dto.UATTestCaseRequestResponseDTO;
import com.aci.smart_onboarding.enums.PortalTypes;
import com.aci.smart_onboarding.enums.TestStatus;
import com.aci.smart_onboarding.enums.TestType;
import com.aci.smart_onboarding.exception.NotFoundException;
import com.aci.smart_onboarding.model.BRD;
import com.aci.smart_onboarding.repository.BRDRepository;
import com.aci.smart_onboarding.security.service.BRDSecurityService;
import com.aci.smart_onboarding.service.IUATTestCaseService;
import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class UATTestCaseControllerTest {

  @Mock private IUATTestCaseService service;
  @Mock private BRDSecurityService brdSecurityService;
  @Mock private BRDRepository brdRepository;

  @InjectMocks private UATTestCaseController controller;

  @Test
  void getTestCasesByFeatureName_WhenFeatureExists_ShouldReturnTestCases() {
    String featureName = "Login Feature";
    UATTestCaseDTO testCase = createTestCaseDTO();

    when(service.getTestCasesByFeatureName(featureName)).thenReturn(Flux.just(testCase));

    StepVerifier.create(controller.getTestCasesByFeatureName(featureName))
        .expectNextMatches(
            responseEntity -> {
              assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
              var body = responseEntity.getBody();
              assertEquals(BrdConstants.SUCCESSFUL, body.getStatus());
              assertTrue(body.getData().isPresent());
              assertEquals(1, body.getData().get().size());
              assertEquals(featureName, body.getData().get().get(0).getFeatureName());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getTestCasesByFeatureName_WhenFeatureDoesNotExist_ShouldReturnEmpty() {
    String featureName = "NonExistent Feature";

    when(service.getTestCasesByFeatureName(featureName)).thenReturn(Flux.empty());

    StepVerifier.create(controller.getTestCasesByFeatureName(featureName))
        .expectNextMatches(
            responseEntity -> {
              assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
              var body = responseEntity.getBody();
              assertEquals(BrdConstants.SUCCESSFUL, body.getStatus());
              assertTrue(body.getData().isPresent());
              assertEquals(0, body.getData().get().size());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void createTestCase_WhenValidRequest_ShouldSucceed() {
    UATTestCaseRequestResponseDTO request = createRequestDTO();
    UATTestCaseRequestResponseDTO response = createRequestDTO();

    when(service.createTestCase(any())).thenReturn(Mono.just(response));

    StepVerifier.create(controller.createTestCase(request))
        .expectNextMatches(
            responseEntity -> {
              assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
              var body = responseEntity.getBody();
              assertEquals(BrdConstants.SUCCESSFUL, body.getStatus());
              assertTrue(body.getData().isPresent());
              assertEquals("Login Test", body.getData().get().getTestName());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void updateTestCase_WhenValidRequest_ShouldSucceed() {
    String id = "test-id";
    UATTestCaseDTO request = createTestCaseDTO();
    UATTestCaseDTO response = createTestCaseDTO();

    when(service.updateTestCase(anyString(), any())).thenReturn(Mono.just(response));

    StepVerifier.create(controller.updateTestCase(id, request))
        .expectNextMatches(
            responseEntity -> {
              assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
              var body = responseEntity.getBody();
              assertEquals(BrdConstants.SUCCESSFUL, body.getStatus());
              assertTrue(body.getData().isPresent());
              assertEquals(id, body.getData().get().getId());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void updateTestCase_WhenTestCaseNotFound_ShouldThrowNotFoundException() {
    String id = "non-existent-id";
    UATTestCaseDTO request = createTestCaseDTO();

    when(service.updateTestCase(anyString(), any()))
        .thenReturn(Mono.error(new NotFoundException("Test case not found")));

    StepVerifier.create(controller.updateTestCase(id, request))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void deleteTestCase_WhenTestCaseExists_ShouldSucceed() {
    String id = "test-id";

    when(service.deleteTestCase(id)).thenReturn(Mono.empty());

    StepVerifier.create(controller.deleteTestCase(id))
        .expectNextMatches(
            responseEntity -> {
              assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void deleteTestCase_WhenTestCaseNotFound_ShouldThrowNotFoundException() {
    String id = "non-existent-id";

    when(service.deleteTestCase(id))
        .thenReturn(Mono.error(new NotFoundException("Test case not found")));

    StepVerifier.create(controller.deleteTestCase(id))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void getTestCase_WhenTestCaseExists_ShouldSucceed() {
    String id = "test-id";
    UATTestCaseDTO testCase = createTestCaseDTO();

    when(service.getTestCase(id)).thenReturn(Mono.just(testCase));

    StepVerifier.create(controller.getTestCase(id))
        .expectNextMatches(
            responseEntity -> {
              assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
              var body = responseEntity.getBody();
              assertEquals(BrdConstants.SUCCESSFUL, body.getStatus());
              assertTrue(body.getData().isPresent());
              assertEquals(id, body.getData().get().getId());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getTestCase_WhenTestCaseNotFound_ShouldThrowNotFoundException() {
    String id = "non-existent-id";

    when(service.getTestCase(id))
        .thenReturn(Mono.error(new NotFoundException("Test case not found")));

    StepVerifier.create(controller.getTestCase(id))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void getAllTestCases_WhenTestCasesExist_ShouldReturnAllTestCases() {
    UATTestCaseDTO testCase1 = createTestCaseDTO();
    UATTestCaseDTO testCase2 = createTestCaseDTO();
    testCase2.setId("test-id-2");
    testCase2.setTestName("Logout Test");

    when(service.getAllTestCases()).thenReturn(Flux.just(testCase1, testCase2));

    StepVerifier.create(controller.getAllTestCases())
        .expectNextMatches(
            responseEntity -> {
              assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
              var body = responseEntity.getBody();
              assertEquals(BrdConstants.SUCCESSFUL, body.getStatus());
              assertTrue(body.getData().isPresent());
              assertEquals(2, body.getData().get().size());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getAllTestCases_WhenNoTestCasesExist_ShouldReturnEmpty() {
    when(service.getAllTestCases()).thenReturn(Flux.empty());

    StepVerifier.create(controller.getAllTestCases())
        .expectNextMatches(
            responseEntity -> {
              assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
              var body = responseEntity.getBody();
              assertEquals(BrdConstants.SUCCESSFUL, body.getStatus());
              assertTrue(body.getData().isPresent());
              assertEquals(0, body.getData().get().size());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getTestCasesByStatus_WhenTestCasesExist_ShouldReturnFilteredTestCases() {
    TestStatus status = TestStatus.PASSED;
    UATTestCaseDTO testCase1 = createTestCaseDTO();
    UATTestCaseDTO testCase2 = createTestCaseDTO();
    testCase2.setId("test-id-2");
    testCase2.setTestName("Logout Test");

    when(service.getTestCasesByStatus(status)).thenReturn(Flux.just(testCase1, testCase2));

    StepVerifier.create(controller.getTestCasesByStatus(status))
        .expectNextMatches(
            responseEntity -> {
              assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
              var body = responseEntity.getBody();
              assertEquals(BrdConstants.SUCCESSFUL, body.getStatus());
              assertTrue(body.getData().isPresent());
              assertEquals(2, body.getData().get().size());
              assertTrue(
                  body.getData().get().stream()
                      .allMatch(tc -> tc.getStatus() == TestStatus.PASSED));
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getTestCasesByStatus_WhenNoTestCasesWithStatus_ShouldReturnEmpty() {
    TestStatus status = TestStatus.FAILED;

    when(service.getTestCasesByStatus(status)).thenReturn(Flux.empty());

    StepVerifier.create(controller.getTestCasesByStatus(status))
        .expectNextMatches(
            responseEntity -> {
              assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
              var body = responseEntity.getBody();
              assertEquals(BrdConstants.SUCCESSFUL, body.getStatus());
              assertTrue(body.getData().isPresent());
              assertEquals(0, body.getData().get().size());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getTestCasesByBrdId_WhenTestCasesExist_ShouldReturnTestCases() {
    String brdId = "BRD-123";
    UATTestCaseDTO testCase1 = createTestCaseDTO();
    UATTestCaseDTO testCase2 = createTestCaseDTO();
    testCase2.setId("test-id-2");
    testCase2.setTestName("Logout Test");
    BRD brd = createBRD(brdId);

    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.just(brd));
    when(service.getTestCasesByBrdId(brdId)).thenReturn(Flux.just(testCase1, testCase2));

    StepVerifier.create(controller.getTestCasesByBrdId(brdId))
        .expectNextMatches(
            responseEntity -> {
              assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
              var body = responseEntity.getBody();
              assertEquals(BrdConstants.SUCCESSFUL, body.getStatus());
              assertTrue(body.getData().isPresent());
              assertEquals(1, body.getData().get().size());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getTestCasesByBrdId_WhenNoTestCasesExist_ShouldReturnEmpty() {
    String brdId = "BRD-NONEXISTENT";
    BRD brd = createBRD(brdId);

    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.just(brd));
    when(service.getTestCasesByBrdId(brdId)).thenReturn(Flux.empty());

    StepVerifier.create(controller.getTestCasesByBrdId(brdId))
        .expectNextMatches(
            responseEntity -> {
              assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
              var body = responseEntity.getBody();
              assertEquals(BrdConstants.SUCCESSFUL, body.getStatus());
              assertTrue(body.getData().isPresent());
              assertEquals(0, body.getData().get().size());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getTestCasesByBrdId_WhenUserIsNotPM_ShouldReturnForbidden() {
    String brdId = "BRD-123";

    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just("ROLE_BA"));

    StepVerifier.create(controller.getTestCasesByBrdId(brdId))
        .expectNextMatches(
            responseEntity -> {
              assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
              var body = responseEntity.getBody();
              assertEquals(BrdConstants.FAILURE, body.getStatus());
              assertTrue(body.getErrors().isPresent());
              assertTrue(body.getErrors().get().containsKey("error"));
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getTestCasesByBrdId_WhenBrdNotFound_ShouldReturnNotFound() {
    String brdId = "BRD-NONEXISTENT";

    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.empty());

    StepVerifier.create(controller.getTestCasesByBrdId(brdId))
        .expectNextMatches(
            responseEntity -> {
              assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
              var body = responseEntity.getBody();
              assertEquals(BrdConstants.FAILURE, body.getStatus());
              assertTrue(body.getErrors().isPresent());
              assertTrue(body.getErrors().get().containsKey("error"));
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getTestCasesByBrdId_WhenServiceError_ShouldReturnInternalServerError() {
    String brdId = "BRD-123";
    BRD brd = createBRD(brdId);

    when(brdSecurityService.getCurrentUserRole()).thenReturn(Mono.just(SecurityConstants.PM_ROLE));
    when(brdRepository.findByBrdId(brdId)).thenReturn(Mono.just(brd));
    when(service.getTestCasesByBrdId(brdId))
        .thenReturn(Flux.error(new RuntimeException("Service error")));

    StepVerifier.create(controller.getTestCasesByBrdId(brdId))
        .expectNextMatches(
            responseEntity -> {
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
              var body = responseEntity.getBody();
              assertEquals(BrdConstants.FAILURE, body.getStatus());
              assertTrue(body.getErrors().isPresent());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void searchTestCasesByBrdAndType_WhenTestCasesExist_ShouldReturnFilteredTestCases() {
    UATTestCaseBrdTypeSearchRequest searchRequest = new UATTestCaseBrdTypeSearchRequest();
    searchRequest.setBrdId("BRD-123");
    searchRequest.setUatType(PortalTypes.AGENT);

    UATTestCaseDTO testCase1 = createTestCaseDTO();
    UATTestCaseDTO testCase2 = createTestCaseDTO();
    testCase2.setId("test-id-2");
    testCase2.setTestName("Logout Test");

    when(service.getTestCasesByBrdIdAndUatType(anyString(), any(PortalTypes.class)))
        .thenReturn(Flux.just(testCase1, testCase2));

    StepVerifier.create(controller.searchTestCasesByBrdAndType(searchRequest))
        .expectNextMatches(
            responseEntity -> {
              assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
              var body = responseEntity.getBody();
              assertEquals(BrdConstants.SUCCESSFUL, body.getStatus());
              assertTrue(body.getData().isPresent());
              assertEquals(2, body.getData().get().size());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void searchTestCasesByBrdAndType_WhenNoTestCasesExist_ShouldReturnEmpty() {
    UATTestCaseBrdTypeSearchRequest searchRequest = new UATTestCaseBrdTypeSearchRequest();
    searchRequest.setBrdId("BRD-123");
    searchRequest.setUatType(PortalTypes.AGENT);

    when(service.getTestCasesByBrdIdAndUatType(anyString(), any(PortalTypes.class)))
        .thenReturn(Flux.empty());

    StepVerifier.create(controller.searchTestCasesByBrdAndType(searchRequest))
        .expectNextMatches(
            responseEntity -> {
              assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
              var body = responseEntity.getBody();
              assertEquals(BrdConstants.SUCCESSFUL, body.getStatus());
              assertTrue(body.getData().isPresent());
              assertEquals(0, body.getData().get().size());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void createTestCase_WhenServiceError_ShouldThrowRuntimeException() {
    UATTestCaseRequestResponseDTO request = createRequestDTO();

    when(service.createTestCase(any()))
        .thenReturn(Mono.error(new RuntimeException("Service error")));

    StepVerifier.create(controller.createTestCase(request))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void updateTestCase_WhenServiceError_ShouldThrowRuntimeException() {
    String id = "test-id";
    UATTestCaseDTO request = createTestCaseDTO();

    when(service.updateTestCase(anyString(), any()))
        .thenReturn(Mono.error(new RuntimeException("Service error")));

    StepVerifier.create(controller.updateTestCase(id, request))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void getTestCasesByFeatureName_WhenServiceError_ShouldThrowRuntimeException() {
    String featureName = "Login Feature";

    when(service.getTestCasesByFeatureName(featureName))
        .thenReturn(Flux.error(new RuntimeException("Service error")));

    StepVerifier.create(controller.getTestCasesByFeatureName(featureName))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void getTestCasesByStatus_WhenServiceError_ShouldThrowRuntimeException() {
    TestStatus status = TestStatus.PASSED;

    when(service.getTestCasesByStatus(status))
        .thenReturn(Flux.error(new RuntimeException("Service error")));

    StepVerifier.create(controller.getTestCasesByStatus(status))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void searchTestCasesByBrdAndType_WhenServiceError_ShouldThrowRuntimeException() {
    UATTestCaseBrdTypeSearchRequest searchRequest = new UATTestCaseBrdTypeSearchRequest();
    searchRequest.setBrdId("BRD-123");
    searchRequest.setUatType(PortalTypes.AGENT);

    when(service.getTestCasesByBrdIdAndUatType(anyString(), any(PortalTypes.class)))
        .thenReturn(Flux.error(new RuntimeException("Service error")));

    StepVerifier.create(controller.searchTestCasesByBrdAndType(searchRequest))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void getAllTestCases_WhenServiceError_ShouldThrowRuntimeException() {
    when(service.getAllTestCases())
        .thenReturn(Flux.error(new RuntimeException("Service error")));

    StepVerifier.create(controller.getAllTestCases())
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void getTestCase_WhenServiceError_ShouldThrowRuntimeException() {
    String id = "test-id";

    when(service.getTestCase(id))
        .thenReturn(Mono.error(new RuntimeException("Service error")));

    StepVerifier.create(controller.getTestCase(id))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void deleteTestCase_WhenServiceError_ShouldThrowRuntimeException() {
    String id = "test-id";

    when(service.deleteTestCase(id))
        .thenReturn(Mono.error(new RuntimeException("Service error")));

    StepVerifier.create(controller.deleteTestCase(id))
        .expectError(RuntimeException.class)
        .verify();
  }

  private UATTestCaseDTO createTestCaseDTO() {
    return UATTestCaseDTO.builder()
        .id("test-id")
        .brdId("BRD-123")
        .testName("Login Test")
        .scenario("Verify login functionality")
        .position("top-right")
        .answer("Login successful")
        .uatType(PortalTypes.AGENT)
        .testType(TestType.NORMAL)
        .status(TestStatus.PASSED)
        .comments("Test passed successfully")
        .featureName("Login Feature")
        .fields(new HashMap<>())
        .build();
  }

  private UATTestCaseRequestResponseDTO createRequestDTO() {
    return UATTestCaseRequestResponseDTO.builder()
        .brdId("BRD-123")
        .testName("Login Test")
        .scenario("Verify login functionality")
        .position("top-right")
        .answer("Login successful")
        .uatType(PortalTypes.AGENT)
        .testType(TestType.NORMAL)
        .status(TestStatus.PASSED)
        .comments("Test passed successfully")
        .fields(new HashMap<>())
        .build();
  }

  private BRD createBRD(String brdId) {
    return BRD.builder()
        .brdId(brdId)
        .build();
  }

}

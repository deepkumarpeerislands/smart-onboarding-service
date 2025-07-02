package com.aci.smart_onboarding;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.retry.annotation.EnableRetry;

@ExtendWith(MockitoExtension.class)
class SmartOnboardingApplicationTest {

  @Test
  @DisplayName("Test main method execution")
  void testMainMethod() {
    try (MockedStatic<SpringApplication> mocked = mockStatic(SpringApplication.class)) {
      // Test with empty args
      String[] emptyArgs = {};
      SmartOnboardingApplication.main(emptyArgs);
      mocked.verify(() -> SpringApplication.run(SmartOnboardingApplication.class, emptyArgs));

      // Test with some arguments
      String[] args = {"--debug", "--trace"};
      SmartOnboardingApplication.main(args);
      mocked.verify(() -> SpringApplication.run(SmartOnboardingApplication.class, args));
    }
  }

  @Test
  @DisplayName("Application Has Required Annotations")
  void hasRequiredAnnotations() {
    Class<?> appClass = SmartOnboardingApplication.class;

    // Verify @SpringBootApplication
    SpringBootApplication springBootAnn = appClass.getAnnotation(SpringBootApplication.class);
    assertNotNull(springBootAnn, "Should have @SpringBootApplication annotation");

    // Verify @ComponentScan
    ComponentScan componentScan = appClass.getAnnotation(ComponentScan.class);
    assertNotNull(componentScan, "Should have @ComponentScan annotation");
    String[] basePackages = componentScan.basePackages();
    assertArrayEquals(
        new String[] {"com.aci.smart_onboarding", "com.aci.ai"},
        basePackages,
        "Should scan correct base packages");

    // Verify @EnableRetry
    EnableRetry enableRetry = appClass.getAnnotation(EnableRetry.class);
    assertNotNull(enableRetry, "Should have @EnableRetry annotation");
  }

  @Test
  @DisplayName("Application Instantiation")
  void canCreateInstance() {
    assertDoesNotThrow(SmartOnboardingApplication::new, "Should be able to create instance");
  }
}

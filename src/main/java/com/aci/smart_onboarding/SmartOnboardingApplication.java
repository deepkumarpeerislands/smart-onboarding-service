package com.aci.smart_onboarding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@ComponentScan(basePackages = {"com.aci.smart_onboarding", "com.aci.ai"})
@EnableRetry
public class SmartOnboardingApplication {

  public static void main(String[] args) {
    SpringApplication.run(SmartOnboardingApplication.class, args);
  }
}

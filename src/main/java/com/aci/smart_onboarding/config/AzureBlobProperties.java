package com.aci.smart_onboarding.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "spring.azure.blob")
@Data
public class AzureBlobProperties {
  private String connectionString;
  private String containerName;
}

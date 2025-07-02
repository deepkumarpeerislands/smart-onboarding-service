package com.aci.smart_onboarding.security.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "brd")
public class BRDSecurityConfig {
  private Security security = new Security();

  @Data
  public static class Security {
    private boolean enabled = true;
    private boolean roleBasedAccessEnabled = true;
    private List<Permission> permissions = new ArrayList<>();
  }

  @Data
  public static class Permission {
    private String role;
    private List<StatusPermission> allowedStatuses = new ArrayList<>();
  }

  @Data
  public static class StatusPermission {
    private String status;
    private List<String> methods = new ArrayList<>();
  }
}

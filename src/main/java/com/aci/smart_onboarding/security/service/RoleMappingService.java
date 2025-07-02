package com.aci.smart_onboarding.security.service;

import com.aci.smart_onboarding.security.config.AzureADConfig;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleMappingService {
  private final AzureADConfig azureADConfig;

  // Map Azure AD roles to application roles
  private final Map<String, String> roleMapping = new ConcurrentHashMap<>();

  public String mapAzureRoleToApplicationRole(String azureRole) {
    // Default mapping if not found
    String defaultRole = "ROLE_USER";

    // Add your role mappings here
    roleMapping.put("PM", "ROLE_PM");
    roleMapping.put("BA", "ROLE_BA");
    roleMapping.put("BILLER", "ROLE_BILLER");

    // Remove prefix if present
    String role =
        azureRole.startsWith(azureADConfig.getRolePrefix())
            ? azureRole.substring(azureADConfig.getRolePrefix().length())
            : azureRole;

    return roleMapping.getOrDefault(role, defaultRole);
  }

  public void addRoleMapping(String azureRole, String applicationRole) {
    roleMapping.put(azureRole, applicationRole);
  }
}

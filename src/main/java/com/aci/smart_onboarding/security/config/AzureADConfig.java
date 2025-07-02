package com.aci.smart_onboarding.security.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/** Configuration properties for Azure AD authentication. */
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "azure.activedirectory")
public class AzureADConfig {

  /** Azure AD application (client) ID */
  @NotBlank(message = "Azure AD client ID is required")
  private String clientId;

  /** Azure AD application client secret */
  @NotBlank(message = "Azure AD client secret is required")
  private String clientSecret;

  /** Azure AD tenant ID */
  @NotBlank(message = "Azure AD tenant ID is required")
  private String tenantId;

  /** Azure AD authority URL */
  @NotBlank(message = "Azure AD authority URL is required")
  private String authority;

  /** OAuth2 redirect URI */
  @NotBlank(message = "Azure AD redirect URI is required")
  private String redirectUri;

  /** OAuth2 scope */
  @NotBlank(message = "Azure AD scope is required")
  private String scope;

  /** Role claim name in the JWT token */
  @NotNull(message = "Role claim name is required")
  private String roleClaim = "roles";

  /** Prefix for role names in the application */
  @NotNull(message = "Role prefix is required")
  private String rolePrefix = "ROLE_";

  /** Graph API endpoint URL */
  @NotBlank(message = "Graph API endpoint is required")
  private String graphApiEndpoint = "https://graph.microsoft.com/v1.0";

  /** Graph API scope */
  @NotBlank(message = "Graph API scope is required")
  private String graphApiScope = "https://graph.microsoft.com/.default";

  /** Name of the biller role in Azure AD */
  @NotBlank(message = "Biller role name is required")
  private String billerRoleName = "BILLER";
}

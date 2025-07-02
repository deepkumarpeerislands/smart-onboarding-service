package com.aci.smart_onboarding.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuration for Swagger/OpenAPI documentation */
@Configuration
public class SwaggerConfig {

  private static final String BEARER_AUTH = "bearerAuth";

  @Value("${server.port:8086}")
  private String serverPort;

  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .servers(
            Arrays.asList(
                new Server().url("http://localhost:" + serverPort).description("Local Server")))
        // Add security requirement at the OpenAPI level to apply to all operations
        .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER_AUTH,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description(
                            "JWT Authorization header using the Bearer scheme. Example: \"Authorization: Bearer {token}\"")))
        .info(
            new Info()
                .title("ACI Worldwide: Smart OnBoarding Services")
                .version("0.0.1")
                .description(
                    "API documentation for Smart OnBoarding Services to manage Billers and their Users")
                .contact(new Contact().name("Development Team").email("dev@example.com"))
                .license(new License().name("Proprietary").url("https://example.com")));
  }
}

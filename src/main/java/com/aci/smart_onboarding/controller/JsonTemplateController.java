package com.aci.smart_onboarding.controller;

import com.aci.smart_onboarding.constants.UserConstants;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.JsonTemplateResponse;
import com.aci.smart_onboarding.security.service.BRDSecurityService;
import com.aci.smart_onboarding.service.IJsonTemplateService;
import com.aci.smart_onboarding.swagger.JsonTemplateRequestAndResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Controller for JSON template operations with optimized reactive performance. Access restricted to
 * users with MANAGER role only.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(
    name = "JSON Template Management",
    description =
        "APIs for JSON template operations - Mixed access levels: Read operations accessible to all authenticated users, Write operations restricted to Manager role only")
@RequestMapping(value = "${api.default.path}/json-templates", name = "JSON Template Management")
public class JsonTemplateController {

  private final IJsonTemplateService jsonTemplateService;
  private final BRDSecurityService securityService;

  @Operation(
      summary = "Get all JSON templates",
      description =
          JsonTemplateRequestAndResponse.GET_ALL_TEMPLATES_ENDPOINT_DESCRIPTION
              + "\n\n🔒 **Access Control**: This endpoint is accessible to all authenticated users.",
      security = {@SecurityRequirement(name = "bearerAuth")})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Templates retrieved successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @io.swagger.v3.oas.annotations.media.ExampleObject(
                          name = JsonTemplateRequestAndResponse.GET_ALL_TEMPLATES_SUCCESS_NAME,
                          description =
                              JsonTemplateRequestAndResponse.GET_ALL_TEMPLATES_SUCCESS_DESC,
                          value =
                              JsonTemplateRequestAndResponse.GET_ALL_TEMPLATES_SUCCESS_RESPONSE),
                      @io.swagger.v3.oas.annotations.media.ExampleObject(
                          name = JsonTemplateRequestAndResponse.GET_ALL_TEMPLATES_EMPTY_NAME,
                          description = JsonTemplateRequestAndResponse.GET_ALL_TEMPLATES_EMPTY_DESC,
                          value = JsonTemplateRequestAndResponse.GET_ALL_TEMPLATES_EMPTY_RESPONSE)
                    })),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Missing or invalid authentication token",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples =
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name =
                                JsonTemplateRequestAndResponse.GET_ALL_TEMPLATES_UNAUTHORIZED_NAME,
                            description =
                                JsonTemplateRequestAndResponse.GET_ALL_TEMPLATES_UNAUTHORIZED_DESC,
                            value =
                                JsonTemplateRequestAndResponse
                                    .GET_ALL_TEMPLATES_UNAUTHORIZED_RESPONSE))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @io.swagger.v3.oas.annotations.media.ExampleObject(
                          name = JsonTemplateRequestAndResponse.GET_ALL_TEMPLATES_SERVER_ERROR_NAME,
                          description =
                              JsonTemplateRequestAndResponse.GET_ALL_TEMPLATES_SERVER_ERROR_DESC,
                          value =
                              JsonTemplateRequestAndResponse
                                  .GET_ALL_TEMPLATES_SERVER_ERROR_RESPONSE),
                      @io.swagger.v3.oas.annotations.media.ExampleObject(
                          name =
                              JsonTemplateRequestAndResponse.GET_ALL_TEMPLATES_DATABASE_ERROR_NAME,
                          description =
                              JsonTemplateRequestAndResponse.GET_ALL_TEMPLATES_DATABASE_ERROR_DESC,
                          value =
                              JsonTemplateRequestAndResponse
                                  .GET_ALL_TEMPLATES_DATABASE_ERROR_RESPONSE)
                    }))
      })
  @GetMapping
  public Mono<ResponseEntity<Api<List<JsonTemplateResponse>>>> getAllTemplates() {

    log.info("GET request received for all templates");

    return getUsernameFromContext()
        .flatMap(
            username -> {
              log.info("User {} is accessing all templates", username);
              return jsonTemplateService.getAllTemplates();
            });
  }

  @Operation(
      summary = "Update JSON template status",
      description =
          JsonTemplateRequestAndResponse.UPDATE_STATUS_ENDPOINT_DESCRIPTION
              + "\n\n🔒 **Access Control**: This endpoint is restricted to users with MANAGER role only."
              + "\n\n⚡ **Single Active Constraint**: When setting a template to 'Active', all other templates will automatically be set to 'InActive' to ensure only one template is active at any time.",
      security = {@SecurityRequirement(name = "bearerAuth")})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Template status updated successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples =
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = JsonTemplateRequestAndResponse.UPDATE_STATUS_SUCCESS_NAME,
                            description = JsonTemplateRequestAndResponse.UPDATE_STATUS_SUCCESS_DESC,
                            value =
                                JsonTemplateRequestAndResponse.UPDATE_STATUS_SUCCESS_RESPONSE))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples = {
                      @io.swagger.v3.oas.annotations.media.ExampleObject(
                          name = JsonTemplateRequestAndResponse.UPDATE_STATUS_VALIDATION_ERROR_NAME,
                          description =
                              JsonTemplateRequestAndResponse.UPDATE_STATUS_VALIDATION_ERROR_DESC,
                          value =
                              JsonTemplateRequestAndResponse
                                  .UPDATE_STATUS_VALIDATION_ERROR_RESPONSE),
                      @io.swagger.v3.oas.annotations.media.ExampleObject(
                          name = JsonTemplateRequestAndResponse.UPDATE_STATUS_INVALID_STATUS_NAME,
                          description =
                              JsonTemplateRequestAndResponse.UPDATE_STATUS_INVALID_STATUS_DESC,
                          value =
                              JsonTemplateRequestAndResponse.UPDATE_STATUS_INVALID_STATUS_RESPONSE),
                      @io.swagger.v3.oas.annotations.media.ExampleObject(
                          name = JsonTemplateRequestAndResponse.UPDATE_STATUS_EMPTY_STATUS_NAME,
                          description =
                              JsonTemplateRequestAndResponse.UPDATE_STATUS_EMPTY_STATUS_DESC,
                          value =
                              JsonTemplateRequestAndResponse.UPDATE_STATUS_EMPTY_STATUS_RESPONSE)
                    })),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Missing or invalid authentication token",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples =
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = JsonTemplateRequestAndResponse.UPDATE_STATUS_UNAUTHORIZED_NAME,
                            description =
                                JsonTemplateRequestAndResponse.UPDATE_STATUS_UNAUTHORIZED_DESC,
                            value =
                                JsonTemplateRequestAndResponse
                                    .UPDATE_STATUS_UNAUTHORIZED_RESPONSE))),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - User is not a Manager",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples =
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = JsonTemplateRequestAndResponse.UPDATE_STATUS_ACCESS_DENIED_NAME,
                            description =
                                JsonTemplateRequestAndResponse.UPDATE_STATUS_ACCESS_DENIED_DESC,
                            value =
                                JsonTemplateRequestAndResponse
                                    .UPDATE_STATUS_ACCESS_DENIED_RESPONSE))),
        @ApiResponse(
            responseCode = "404",
            description = "Template not found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples =
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = JsonTemplateRequestAndResponse.UPDATE_STATUS_NOT_FOUND_NAME,
                            description =
                                JsonTemplateRequestAndResponse.UPDATE_STATUS_NOT_FOUND_DESC,
                            value =
                                JsonTemplateRequestAndResponse.UPDATE_STATUS_NOT_FOUND_RESPONSE))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Api.class),
                    examples =
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = JsonTemplateRequestAndResponse.UPDATE_STATUS_SERVER_ERROR_NAME,
                            description =
                                JsonTemplateRequestAndResponse.UPDATE_STATUS_SERVER_ERROR_DESC,
                            value =
                                JsonTemplateRequestAndResponse
                                    .UPDATE_STATUS_SERVER_ERROR_RESPONSE)))
      })
  @PutMapping("/{templateName}/status/{status}")
  public Mono<ResponseEntity<Api<Boolean>>> updateTemplateStatus(
      @Parameter(
              description = JsonTemplateRequestAndResponse.UPDATE_STATUS_TEMPLATE_NAME_PARAM_DESC,
              required = true,
              example = JsonTemplateRequestAndResponse.EXAMPLE_TEMPLATE_NAME,
              schema = @Schema(type = "string", minLength = 1, maxLength = 100))
          @PathVariable
          @NotBlank
          String templateName,
      @Parameter(
              description = JsonTemplateRequestAndResponse.UPDATE_STATUS_STATUS_PARAM_DESC,
              required = true,
              examples = {
                @io.swagger.v3.oas.annotations.media.ExampleObject(
                    name = "Active",
                    value = JsonTemplateRequestAndResponse.EXAMPLE_STATUS_ACTIVE),
                @io.swagger.v3.oas.annotations.media.ExampleObject(
                    name = "InActive",
                    value = JsonTemplateRequestAndResponse.EXAMPLE_STATUS_INACTIVE)
              },
              schema =
                  @Schema(
                      type = "string",
                      allowableValues = {"Active", "InActive"}))
          @PathVariable
          @NotBlank
          @Pattern(regexp = "^(Active)$", message = "Status must be either 'Active' or 'InActive'")
          String status) {

    log.info(
        "PUT request received to update template status: templateName={}, status={}",
        templateName,
        status);

    return getUsernameFromContext()
        .flatMap(
            username ->
                securityService
                    .getCurrentUserRole()
                    .flatMap(
                        role -> {
                          // Only allow managers to update template status
                          if (!UserConstants.MANAGER_ROLE.equals(role)) {
                            log.warn(
                                "User {} with role {} attempted to update template status but is not authorized",
                                username,
                                role);
                            return Mono.error(
                                new AccessDeniedException(UserConstants.MANAGER_ONLY_MESSAGE));
                          }

                          log.info(
                              "Authorized user {} with role {} is updating template status: templateName={}, status={}",
                              username,
                              role,
                              templateName,
                              status);
                          return jsonTemplateService.updateTemplateStatusWithSingleActive(
                              templateName, status);
                        }));
  }

  /**
   * Gets username from the security context.
   *
   * @return Mono containing the username
   */
  private Mono<String> getUsernameFromContext() {
    return ReactiveSecurityContextHolder.getContext()
        .map(context -> context.getAuthentication().getName());
  }
}

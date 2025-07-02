package com.aci.smart_onboarding.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic response object for API responses.
 *
 * @param <T> the type of the response data
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Api<T> {

  @NotBlank(message = "Status cannot be blank")
  private String status;

  @NotBlank(message = "Message cannot be blank")
  private String message;

  private Optional<T> data = Optional.empty();

  private Optional<Map<String, String>> errors = Optional.empty();
}

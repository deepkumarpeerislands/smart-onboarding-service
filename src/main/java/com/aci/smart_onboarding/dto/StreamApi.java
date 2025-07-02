package com.aci.smart_onboarding.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StreamApi<T> {
  @NotBlank(message = "Status cannot be blank")
  private String status;

  @NotBlank(message = "Message cannot be blank")
  private String message;

  private T data;
  private Optional<Map<String, String>> errors = Optional.empty();
}

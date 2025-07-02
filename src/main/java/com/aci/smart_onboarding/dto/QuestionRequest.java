package com.aci.smart_onboarding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestionRequest {
  @NonNull @NotBlank public String question;
  @NonNull @NotNull public String contextName;
  @NonNull @NotNull public String documentName;
}

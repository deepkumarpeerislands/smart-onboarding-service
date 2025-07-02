package com.aci.smart_onboarding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadRequest {
  private String title;
  private String id; // Optional ID - if provided, skip creation (existing document)
}

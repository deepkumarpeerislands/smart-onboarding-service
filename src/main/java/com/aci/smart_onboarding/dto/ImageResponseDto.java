package com.aci.smart_onboarding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageResponseDto {
  private String id; // Document ID
  private String fileName;
  private String url;
  private String title;
}

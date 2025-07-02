package com.aci.smart_onboarding.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletronImagesResponseDTO {
  private Map<String, byte[]> images; // Map of image name to base64 encoded image
}

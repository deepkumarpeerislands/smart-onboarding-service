package com.aci.smart_onboarding.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "BRD Response List Details with count of total records")
public class BRDCountDataResponse {

  private int totalCount;
  private List<BRDListResponse> brdList;
}

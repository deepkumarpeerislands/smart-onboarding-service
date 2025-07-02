package com.aci.smart_onboarding.model;

import com.aci.smart_onboarding.dto.BrdForm;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "sites")
public class Site {
  @Id private String id;
  private String brdId;
  private String siteId;
  private String siteName;
  private String identifierCode;
  private String description;
  private BrdForm brdForm;
  @CreatedDate private LocalDateTime createdAt;
  @LastModifiedDate private LocalDateTime updatedAt;
}

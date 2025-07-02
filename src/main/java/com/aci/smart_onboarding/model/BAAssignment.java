package com.aci.smart_onboarding.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "ba_assignments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BAAssignment {
  @Id private String id;
  private String brdId;
  private String baEmail;
  private String description;
  @CreatedDate private LocalDateTime assignedAt;
  @LastModifiedDate private LocalDateTime updatedAt;
}

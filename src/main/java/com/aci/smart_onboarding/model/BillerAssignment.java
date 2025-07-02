package com.aci.smart_onboarding.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "biller_assignments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(
    name = "brdId_billerEmail_idx",
    def = "{'brdId': 1, 'billerEmail': 1}",
    unique = true)
public class BillerAssignment {
  @Id private String id;

  private String brdId;
  private String billerEmail;
  private String description;
  private LocalDateTime assignedAt;
  private LocalDateTime updatedAt;
}

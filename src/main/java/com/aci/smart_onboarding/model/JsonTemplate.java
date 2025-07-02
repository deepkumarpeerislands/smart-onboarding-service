package com.aci.smart_onboarding.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Model class for JSON template metadata storage. Note: templateName uses a case-insensitive unique
 * index created manually in MongoDB.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "json_templates")
public class JsonTemplate {

  @Id private String id;

  @Indexed(unique = true)
  private String templateName;

  @Builder.Default private String status = "InActive";

  private String fileName;

  private String originalFileName;

  private String uploadedBy;

  @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();

  @Builder.Default private LocalDateTime updatedAt = LocalDateTime.now();
}

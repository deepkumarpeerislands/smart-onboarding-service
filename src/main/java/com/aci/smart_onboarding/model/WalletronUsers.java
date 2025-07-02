package com.aci.smart_onboarding.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "walletron_users")
public class WalletronUsers {

  @Id private String id;

  @JsonProperty("name")
  private String name;

  @JsonProperty("email")
  @Indexed(unique = true)
  private String email;

  @JsonProperty("role")
  private String role;

  @JsonProperty("walletronId")
  private String walletronId;

  @JsonProperty("brdId")
  private String brdId;

  @JsonProperty("createdAt")
  private LocalDateTime createdAt;

  @JsonProperty("updatedAt")
  private LocalDateTime updatedAt;
}

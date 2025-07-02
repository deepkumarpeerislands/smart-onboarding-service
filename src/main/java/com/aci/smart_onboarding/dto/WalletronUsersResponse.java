package com.aci.smart_onboarding.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletronUsersResponse {

  @JsonProperty("savedUsers")
  private List<SavedUser> savedUsers;

  @JsonProperty("duplicateEmails")
  private List<String> duplicateEmails;

  @JsonProperty("totalProcessed")
  private int totalProcessed;

  @JsonProperty("totalDuplicates")
  private int totalDuplicates;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SavedUser {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("email")
    private String email;

    @JsonProperty("role")
    private String role;

    @JsonProperty("walletronId")
    private String walletronId;

    @JsonProperty("brdId")
    private String brdId;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
  }
}

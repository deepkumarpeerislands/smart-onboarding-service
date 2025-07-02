package com.aci.smart_onboarding.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletronUsersRequest {

  @JsonProperty("walletronId")
  @NotNull(message = "WalletronId cannot be null")
  private String walletronId;

  @JsonProperty("brdId")
  @NotNull(message = "BrdId cannot be null")
  private String brdId;

  @JsonProperty("users")
  @NotNull(message = "Users list cannot be null")
  @NotEmpty(message = "Users list cannot be empty")
  @Valid
  private List<UserData> users;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UserData {

    @JsonProperty("name")
    @NotNull(message = "Name cannot be null")
    private String name;

    @JsonProperty("email")
    @NotNull(message = "Email cannot be null")
    @Email(message = "Email should be valid")
    private String email;

    @JsonProperty("role")
    @NotNull(message = "Role cannot be null")
    private String role;
  }
}

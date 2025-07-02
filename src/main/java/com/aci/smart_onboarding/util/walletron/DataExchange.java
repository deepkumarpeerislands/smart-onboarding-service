package com.aci.smart_onboarding.util.walletron;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataExchange {

  private List<DailyImportFileOption> dailyImportFile;
  private List<ApiOption> api;
  private List<SimpleOption> enrollments;

  @NotBlank(message = "Email distribution list is required")
  @Email(message = "Invalid email distribution list")
  private String emailDistributionList;

  private List<SimpleOption> activity;
  private List<SimpleOption> enrolledAccounts;

  private String sftpUrl;
  private String sftpDirectory;
  private String inboundFile;
  private String timeFileSent;

  @NotBlank(message = "Group email address is required")
  @Email(message = "Invalid group email address")
  private String groupEmailAddress;

  private String phoneNumber;
  private String followUpTime;
  private String pgpEncryptionKey;
  private String clientPgpEncryptionKey;
  private String frequency;
  private String fileTransmissionTime;
  private String ipAddresses;
  private String confirmedBy;
  private String sectionStatus;

  // ======= Daily Import File Section =======
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DailyImportFileOption {
    private String viewValue;
    private Boolean selected;
    private List<DailyImportChild> children;
    private Boolean indeterminate;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DailyImportChild {
    private String viewValue;
    private Boolean selected;
    private List<PlatformListItem> list; // For `platform` object
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PlatformListItem {
    private String platform;
  }

  // ======= API Section =======
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ApiOption {
    private String viewValue;
    private Boolean selected;
    private List<ApiChild> children;
    private Boolean indeterminate;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ApiChild {
    private String viewValue;
    private Boolean selected;
    private List<Object> list; // For `apiUsername` and `apiDocumentationLocation`
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ApiDocumentation {
    private String apiDocumentationLocation;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ApiUser {
    private String apiUsername;
  }

  // ======= Shared Section (Enrollments, Activity, Enrolled Accounts) =======
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SimpleOption {
    private String viewValue;
    private Boolean selected;
    private List<SimpleChild> children;
    private Boolean indeterminate;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SimpleChild {
    private String viewValue;
    private Boolean selected;
  }
}

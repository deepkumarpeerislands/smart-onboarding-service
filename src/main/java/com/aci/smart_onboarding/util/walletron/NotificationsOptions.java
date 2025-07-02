package com.aci.smart_onboarding.util.walletron;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationsOptions {

  private List<NotificationOption> notificationOptions;
  private String userListFile;
  private String sectionStatus;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class NotificationOption {
    @JsonProperty("isChecked")
    private boolean checked;

    private String notificationType;
    private String defaultNotificationContents;
    private String defaultBusinessLogic;
  }
}

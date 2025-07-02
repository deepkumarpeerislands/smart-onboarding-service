package com.aci.smart_onboarding.util.walletron;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AciCash {
  private String numberOfAccounts;
  private List<AciCashPaymentFee> aciCashPaymentFees;
  private String uat;
  private String prod;
  private String payItTodayUcp;
  private List<ExistingD1Client> existingD1Client;
  private List<PaymentsPlatform> paymentsPlatform;
  private List<RealTimePaymentsPosting> realTimePaymentsPosting;
  private List<AccountLookUp> accountLookUp;
  private List<AciCashBarcodeDistribution> aciCashBarcodeDistribution;
  private String sectionStatus;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AciCashPaymentFee {
    private String viewValue;
    private Boolean selected;
    private Boolean indeterminate;
    private List<ServiceFeeChild> children;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ServiceFeeChild {
    private String viewValue;
    private Boolean selected;
    private List<FeeAmount> list;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class FeeAmount {
    private String serviceFeeAmount;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ExistingD1Client {
    private String viewValue;
    private Boolean selected;
    private Boolean indeterminate;
    private List<D1ClientChild> children;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class D1ClientChild {
    private String viewValue;
    private Boolean selected;
    private List<Object> list;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class BillerIdFee {
    private String d1BillerIdFee;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class BillerIdNoFee {
    private String d1BillerIdNoFee;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PaymentsPlatform {
    private String viewValue;
    private Boolean selected;
    private Boolean indeterminate;
    private List<PaymentPlatformChild> children;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PaymentPlatformChild {
    private String viewValue;
    private Boolean selected;
    private List<OtherPaymentPlatform> list;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class OtherPaymentPlatform {
    private String otherPaymentsPlatform;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RealTimePaymentsPosting {
    private String viewValue;
    private Boolean selected;
    private Boolean indeterminate;
    private List<RtpChild> children;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RtpChild {
    private String viewValue;
    private Boolean selected;
    private List<Object> list;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RtpURLs {
    private String rtpUrl;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RtpCredential {
    private String rtpCredentials;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RtpOtherPaymentPosting {
    private String otherPaymentPosting;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AccountLookUp {
    private String viewValue;
    private Boolean selected;
    private Boolean indeterminate;
    private List<AccountLookUpChild> children;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AccountLookUpChild {
    private String viewValue;
    private Boolean selected;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AciCashBarcodeDistribution {
    private String viewValue;
    private Boolean selected;
    private Boolean indeterminate;
  }
}

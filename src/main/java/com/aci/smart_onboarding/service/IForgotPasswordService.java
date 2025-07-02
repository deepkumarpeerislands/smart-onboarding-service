package com.aci.smart_onboarding.service;

import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.ResetPasswordRequest;
import reactor.core.publisher.Mono;

public interface IForgotPasswordService {

  /**
   * Initiates the password reset process for a given email address. Always returns success to
   * prevent user enumeration.
   *
   * @param email The email address of the user requesting password reset
   * @return A Mono that completes when the request is processed
   */
  Mono<Void> requestPasswordReset(String email);

  /**
   * Resets a user's password using a valid reset token.
   *
   * @param token The password reset token
   * @param request The reset password request containing the new password
   * @return A Mono containing the API response
   */
  Mono<Api<Void>> resetPassword(String token, ResetPasswordRequest request);
}

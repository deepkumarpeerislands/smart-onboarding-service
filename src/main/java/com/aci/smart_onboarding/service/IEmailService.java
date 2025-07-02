package com.aci.smart_onboarding.service;

import reactor.core.publisher.Mono;

/**
 * Service interface for sending email notifications. Provides methods for sending various types of
 * email notifications related to BRD status changes and biller assignments.
 */
public interface IEmailService {

  /**
   * Sends a notification email when a BRD status is changed to "In Progress"
   *
   * @param toEmail The recipient's email address
   * @param brdId The ID of the BRD
   * @param brdName The name of the BRD
   * @param brdFormId The ID of the BRD form
   * @return A Mono that completes when the email is sent
   */
  Mono<Void> sendBrdStatusChangeNotification(
      String toEmail, String brdId, String brdName, String brdFormId);

  /**
   * Sends a welcome email to a newly assigned biller. The email includes information about the BRD
   * and their responsibilities.
   *
   * @param billerEmail The email address of the biller
   * @param brdId The ID of the BRD
   * @param brdName The name of the BRD
   * @param brdFormId The ID of the BRD form
   * @return A Mono that completes when the email is sent
   */
  Mono<Void> sendBillerWelcomeEmail(
      String billerEmail, String brdId, String brdName, String brdFormId);

  /**
   * Sends a welcome email to a newly created user with password reset link
   *
   * @param toEmail The recipient's email address
   * @param userName The username (email) of the new user
   * @return A Mono that completes when the email is sent
   */
  Mono<Void> sendUserWelcomeEmailWithResetLink(String toEmail, String userName);

  /**
   * Sends an email with password to a newly created user
   *
   * @param toEmail The recipient's email address
   * @param password The generated password for the user
   * @return A Mono that completes when the email is sent
   */
  Mono<Void> sendUserCredentialsEmail(String toEmail, String password);

  /**
   * Sends a password reset email containing a link to reset the password
   *
   * @param toEmail The recipient's email address
   * @param resetLink The password reset link
   * @return A Mono that completes when the email is sent
   */
  Mono<Void> sendPasswordResetEmail(String toEmail, String resetLink);
}

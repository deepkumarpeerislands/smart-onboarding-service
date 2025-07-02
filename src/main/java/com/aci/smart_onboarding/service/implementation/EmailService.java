package com.aci.smart_onboarding.service.implementation;

import com.aci.smart_onboarding.exception.EmailServiceException;
import com.aci.smart_onboarding.service.IEmailService;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Implementation of the EmailService interface. Handles sending email notifications for various
 * events in the system. Uses a thread pool for asynchronous email sending to improve performance.
 */
@Slf4j
@Service
public class EmailService implements IEmailService {

  private static final String HTML_CONTENT_TYPE = "text/html";

  private final SendGrid sendGrid;

  @Value("${sendgrid.sender.email}")
  private String senderEmail;

  @Value("${sendgrid.sender.name}")
  private String senderName;

  @Value("${sendgrid.api.key}")
  private String sendGridApiKey;

  @Value("${app.frontend.url}")
  private String frontendUrl;

  public EmailService(SendGrid sendGrid) {
    this.sendGrid = sendGrid;
  }

  @PostConstruct
  public void init() {
    log.info("Initializing EmailServiceImpl with configuration:");
    log.info("Sender: {} <{}>", senderName, senderEmail);
    log.info(
        "SendGrid API Key configured: {}",
        sendGridApiKey != null && !sendGridApiKey.isEmpty() ? "Yes" : "No");
    log.info("Frontend URL: {}", frontendUrl);

    if (sendGridApiKey == null || sendGridApiKey.isEmpty()) {
      log.error(
          "SendGrid API key is not configured. Please set the SENDGRID_API_KEY environment variable.");
    }
    if (senderEmail == null || senderEmail.isEmpty()) {
      log.error("Sender email is not configured in application-dev.yml");
    }
    if (senderName == null || senderName.isEmpty()) {
      log.error("Sender name is not configured in application-dev.yml");
    }
  }

  /**
   * Sends an email notification to a biller when their BRD status changes to In Progress. The email
   * is sent asynchronously using a dedicated thread pool. Includes retry logic for transient
   * failures.
   *
   * @param toEmail The email address of the biller
   * @param brdId The ID of the BRD
   * @param brdName The name of the BRD
   * @param brdFormId The ID of the BRD form
   * @return A Mono that completes when the email is sent
   * @throws IllegalArgumentException if any parameter is invalid
   * @throws IOException if the email fails to send after retries
   */
  @Override
  public Mono<Void> sendBrdStatusChangeNotification(
      String toEmail, String brdId, String brdName, String brdFormId) {
    log.info("Attempting to send BRD status change notification to: {}", toEmail);
    return Mono.fromRunnable(
        () -> {
          try {
            Email from = new Email(senderEmail, senderName);
            Email to = new Email(toEmail);
            String subject = "BRD Status Update: " + brdName;
            Content content =
                new Content(
                    HTML_CONTENT_TYPE, buildBrdStatusChangeContent(brdId, brdName, brdFormId));
            Mail mail = new Mail(from, subject, to, content);

            log.info("Sending email with subject: {}", subject);
            sendEmail(mail);
            log.info("Email sent successfully to {}", toEmail);
          } catch (Exception e) {
            log.error(
                "Failed to send BRD status change notification to {}: {}", toEmail, e.getMessage());
            throw e;
          }
        });
  }

  @Override
  public Mono<Void> sendBillerWelcomeEmail(
      String billerEmail, String brdId, String brdName, String brdFormId) {
    return Mono.fromRunnable(
        () -> {
          Email from = new Email(senderEmail, senderName);
          Email to = new Email(billerEmail);
          String subject = "Welcome to Smart Onboarding - New BRD Assignment";
          Content content =
              new Content(HTML_CONTENT_TYPE, buildBillerWelcomeContent(brdId, brdName, brdFormId));
          Mail mail = new Mail(from, subject, to, content);

          sendEmail(mail);
        });
  }

  @Override
  public Mono<Void> sendUserWelcomeEmailWithResetLink(String toEmail, String userName) {
    log.info("Attempting to send user welcome email with reset link to: {}", toEmail);
    return Mono.fromRunnable(
        () -> {
          try {
            Email from = new Email(senderEmail, senderName);
            Email to = new Email(toEmail);
            String subject = "Welcome to Smart Onboarding - Account Created";
            Content content =
                new Content(HTML_CONTENT_TYPE, buildUserWelcomeWithResetContent(userName));
            Mail mail = new Mail(from, subject, to, content);

            log.info("Sending welcome email with reset link to: {}", toEmail);
            sendEmail(mail);
            log.info("Welcome email with reset link sent successfully to {}", toEmail);
          } catch (Exception e) {
            log.error(
                "Failed to send welcome email with reset link to {}: {}", toEmail, e.getMessage());
            throw e;
          }
        });
  }

  @Override
  public Mono<Void> sendUserCredentialsEmail(String toEmail, String password) {
    log.info("Attempting to send user credentials email to: {}", toEmail);
    return Mono.fromRunnable(
        () -> {
          try {
            Email from = new Email(senderEmail, senderName);
            Email to = new Email(toEmail);
            String subject = "Smart Onboarding - Your Account Credentials";
            Content content = new Content(HTML_CONTENT_TYPE, buildUserCredentialsContent(password));
            Mail mail = new Mail(from, subject, to, content);

            log.info("Sending credentials email to: {}", toEmail);
            sendEmail(mail);
            log.info("Credentials email sent successfully to {}", toEmail);
          } catch (Exception e) {
            log.error("Failed to send credentials email to {}: {}", toEmail, e.getMessage());
            throw e;
          }
        });
  }

  @Override
  public Mono<Void> sendPasswordResetEmail(String toEmail, String resetLink) {
    log.info("Attempting to send password reset email to: {}", toEmail);
    return Mono.fromRunnable(
        () -> {
          try {
            Email from = new Email(senderEmail, senderName);
            Email to = new Email(toEmail);
            String subject = "Smart Onboarding - Password Reset Request";
            Content content = new Content(HTML_CONTENT_TYPE, buildPasswordResetContent(resetLink));
            Mail mail = new Mail(from, subject, to, content);

            log.info("Sending password reset email to: {}", toEmail);
            sendEmail(mail);
            log.info("Password reset email sent successfully to {}", toEmail);
          } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
            throw e;
          }
        });
  }

  /**
   * Checks the delivery status of an email in SendGrid. This method can be called after sending an
   * email to verify its delivery status.
   */
  private void checkEmailDeliveryStatus(String emailId) {
    try {
      Request request = new Request();
      request.setMethod(Method.GET);
      request.setEndpoint("mail/send/" + emailId);

      Response response = sendGrid.api(request);
      log.info("Email delivery status check response: {}", response.getBody());
    } catch (IOException e) {
      log.error("Error checking email delivery status: {}", e.getMessage());
    }
  }

  @Retryable(
      retryFor = {IOException.class},
      maxAttempts = 3,
      backoff = @Backoff(delay = 1000, multiplier = 2))
  private void sendEmail(Mail mail) {
    try {
      log.info("Preparing to send email:");
      log.info("From: {} <{}>", mail.getFrom().getName(), mail.getFrom().getEmail());
      log.info("To: {}", mail.getPersonalization().get(0).getTos().get(0).getEmail());
      log.info("Subject: {}", mail.getSubject());

      Request request = new Request();
      request.setMethod(Method.POST);
      request.setEndpoint("mail/send");
      request.setBody(mail.build());

      log.info("Sending request to SendGrid API...");
      Response response = sendGrid.api(request);

      if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
        String emailId = response.getHeaders().get("X-Message-Id");
        log.info(
            "Email sent successfully to {}",
            mail.getPersonalization().get(0).getTos().get(0).getEmail());
        log.info("SendGrid Message ID: {}", emailId);
        log.info("SendGrid Response Headers: {}", response.getHeaders());
        log.info("SendGrid Response Body: {}", response.getBody());

        // Check delivery status after a short delay
        if (emailId != null) {
          checkMailStatus(emailId);
        }

        log.info("Please check your email inbox (including spam folder) for the message.");
        log.info("If not received within 5 minutes, please verify:");
        log.info("1. Sender email is verified in SendGrid");
        log.info("2. Recipient email is correct");
        log.info("3. Check SendGrid Activity Log for delivery status");
      } else {
        String errorMessage = "Failed to send email. Status code: " + response.getStatusCode();
        if (response.getStatusCode() == 401) {
          errorMessage += " - Authentication failed. Please check:";
          errorMessage += "\n1. SendGrid API key is valid and has correct permissions";
          errorMessage += "\n2. Sender email (" + senderEmail + ") is verified in SendGrid";
          errorMessage += "\n3. API key has 'Mail Send' permission enabled";
        }
        errorMessage += "\nResponse body: " + response.getBody();
        errorMessage += "\nResponse headers: " + response.getHeaders();

        log.error(errorMessage);
        throw new IOException(errorMessage);
      }
    } catch (IOException e) {
      log.error("Error sending email: {}", e.getMessage());
      throw new EmailServiceException(
          "Failed to send email after retries. Please check SendGrid configuration.", e);
    }
  }

  private void checkMailStatus(String emailId) {
    try {
      Thread.sleep(5000); // Wait 5 seconds before checking status
      checkEmailDeliveryStatus(emailId);
    } catch (InterruptedException e) {
      log.warn("Interrupted while waiting to check email status");
      Thread.currentThread().interrupt();
    }
  }

  private String buildBrdStatusChangeContent(String brdId, String brdName, String brdFormId) {
    String brdLink =
        String.format(
            "%s/organization-site?brdFormId=%s&brdId=%s", frontendUrl, brdFormId, brdId);
    return String.format(
        """
        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
            <h2>BRD Status Update</h2>
            <p>The status of BRD %s has been updated to In Progress.</p>
            <p>You can access the BRD using the following link:</p>
            <p><a href="%s" style="background-color: #4CAF50; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">View BRD</a></p>
            <p>If you have any questions, please contact your project manager.</p>
            <p>Best regards,<br>Smart Onboarding Team</p>
        </div>
        """,
        brdName, brdLink);
  }

  private String buildBillerWelcomeContent(String brdId, String brdName, String brdFormId) {
    String brdLink =
        String.format(
            "%s/organization-site?brdFormId=%s&brdId=%s", frontendUrl, brdFormId, brdId);
    return String.format(
        """
        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
            <h2>Welcome to Smart Onboarding</h2>
            <p>You have been assigned as a biller for BRD %s.</p>
            <p>You can access the BRD using the following link:</p>
            <p><a href="%s" style="background-color: #4CAF50; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">View BRD</a></p>
            <p>If you have any questions, please contact your project manager.</p>
            <p>Best regards,<br>Smart Onboarding Team</p>
        </div>
        """,
        brdName, brdLink);
  }

  private String buildUserWelcomeWithResetContent(String userName) {
    return String.format(
        "<!DOCTYPE html>"
            + "<html>"
            + "<head>"
            + "<meta charset='UTF-8'>"
            + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
            + "<title>Welcome to Smart Onboarding</title>"
            + "<style>"
            + "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }"
            + ".container { max-width: 600px; margin: 0 auto; padding: 20px; }"
            + ".header { background-color: #f8f9fa; padding: 20px; text-align: center; }"
            + ".content { padding: 20px; background-color: #ffffff; }"
            + ".footer { text-align: center; padding: 20px; font-size: 12px; color: #666; background-color: #f8f9fa; }"
            + ".button { display: inline-block; padding: 12px 24px; background-color: #007bff; color: #ffffff; text-decoration: none; border-radius: 5px; margin: 10px 0; }"
            + ".welcome-heading { color: #007bff; font-size: 24px; margin-bottom: 20px; }"
            + "</style>"
            + "</head>"
            + "<body>"
            + "<div class='container'>"
            + "<div class='header'>"
            + "<h1 class='welcome-heading'>Welcome to Smart Onboarding!</h1>"
            + "</div>"
            + "<div class='content'>"
            + "<p>Hello,</p>"
            + "<p>Welcome to the Smart Onboarding platform! Your account has been successfully created.</p>"
            + "<p><strong>Username:</strong> %s</p>"
            + "<p>To get started, please login by clicking the link below:</p>"
            + "<p style='text-align: center; margin-top: 20px;'>"
            + "<a href='%s/login' class='button'>Login</a>"
            + "</p>"
            + "<p>If the button doesn't work, you can copy and paste this link into your browser:</p>"
            + "<p style='word-break: break-all; color: #007bff;'>%s/login</p>"
            + "<p>If you have any questions or need assistance, please contact the Smart Onboarding support team.</p>"
            + "</div>"
            + "<div class='footer'>"
            + "<p>This is an automated message from Smart Onboarding Team.</p>"
            + "<p>© 2024 Smart Onboarding Team</p>"
            + "<p>If you believe you received this email in error, please contact support.</p>"
            + "</div>"
            + "</div>"
            + "</body>"
            + "</html>",
        userName, frontendUrl, frontendUrl);
  }

  private String buildUserCredentialsContent(String password) {
    return String.format(
        "<!DOCTYPE html>"
            + "<html>"
            + "<head>"
            + "<meta charset='UTF-8'>"
            + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
            + "<title>Your Smart Onboarding Credentials</title>"
            + "<style>"
            + "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }"
            + ".container { max-width: 600px; margin: 0 auto; padding: 20px; }"
            + ".header { background-color: #f8f9fa; padding: 20px; text-align: center; }"
            + ".content { padding: 20px; background-color: #ffffff; }"
            + ".footer { text-align: center; padding: 20px; font-size: 12px; color: #666; background-color: #f8f9fa; }"
            + ".credentials-box { background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 15px 0; }"
            + ".welcome-heading { color: #007bff; font-size: 24px; margin-bottom: 20px; }"
            + ".warning { background-color: #fff3cd; border: 1px solid #ffeaa7; padding: 10px; border-radius: 5px; margin: 15px 0; }"
            + "</style>"
            + "</head>"
            + "<body>"
            + "<div class='container'>"
            + "<div class='header'>"
            + "<h1 class='welcome-heading'>Welcome to Smart Onboarding!</h1>"
            + "</div>"
            + "<div class='content'>"
            + "<p>Hello,</p>"
            + "<p>Your Smart Onboarding account has been created successfully. Below are your login credentials:</p>"
            + "<div class='credentials-box'>"
            + "<p><strong>Password:</strong> %s</p>"
            + "</div>"
            + "<div class='warning'>"
            + "<p><strong>Important:</strong> For security reasons, please change your password after your first login.</p>"
            + "</div>"
            + "<p>You can access the Smart Onboarding portal using the credentials above.</p>"
            + "<p>If you have any questions or need assistance, please contact the Smart Onboarding support team.</p>"
            + "</div>"
            + "<div class='footer'>"
            + "<p>This is an automated message from Smart Onboarding Team.</p>"
            + "<p>© 2024 Smart Onboarding Team</p>"
            + "<p>If you believe you received this email in error, please contact support.</p>"
            + "</div>"
            + "</div>"
            + "</body>"
            + "</html>",
        password);
  }

  private String buildPasswordResetContent(String resetLink) {
    return String.format(
        """
        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
            <h2 style="color: #333;">Password Reset Request</h2>
            <p>You have requested to reset your password for your Smart Onboarding account.</p>
            <p>Please click the button below to reset your password. This link will expire in 30 minutes.</p>
            <div style="text-align: center; margin: 30px 0;">
                <a href="%s" style="background-color: #007bff; color: white; padding: 12px 24px; text-decoration: none; border-radius: 4px;">Reset Password</a>
            </div>
            <p>If you did not request this password reset, please ignore this email or contact support if you have concerns.</p>
            <p>For security reasons, this link will expire in 30 minutes.</p>
            <hr style="border: 1px solid #eee; margin: 20px 0;">
            <p style="color: #666; font-size: 12px;">This is an automated message, please do not reply to this email.</p>
        </div>
        """,
        resetLink);
  }
}

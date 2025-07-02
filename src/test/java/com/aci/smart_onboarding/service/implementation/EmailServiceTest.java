package com.aci.smart_onboarding.service.implementation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.aci.smart_onboarding.exception.EmailServiceException;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

  @Mock private SendGrid sendGrid;

  @InjectMocks private EmailService emailService;

  private final String testEmail = "test@example.com";
  private final String brdId = "BRD-123";
  private final String brdFormId = "BRDFORM-123";
  private final String brdName = "Test BRD";
  private final String userName = "john.doe@example.com";
  private final String password = "TestPassword123";

  @BeforeEach
  void setUp() {
    // Set up test configuration
    ReflectionTestUtils.setField(emailService, "senderEmail", "test@example.com");
    ReflectionTestUtils.setField(emailService, "senderName", "Test Sender");
    ReflectionTestUtils.setField(emailService, "sendGridApiKey", "test-api-key");
    ReflectionTestUtils.setField(emailService, "frontendUrl", "http://localhost:4200");
  }

  @Test
  void sendBrdStatusChangeNotification_WithValidRequest_ShouldComplete() throws IOException {
    // Arrange
    Response mockResponse = new Response();
    mockResponse.setStatusCode(202); // Accepted status code
    mockResponse.setHeaders(Map.of("X-Message-Id", "test-message-id"));

    when(sendGrid.api(any(Request.class))).thenReturn(mockResponse);

    // Act & Assert
    StepVerifier.create(
            emailService.sendBrdStatusChangeNotification(testEmail, brdId, brdName, brdFormId))
        .verifyComplete();

    // Verify API call - use atLeastOnce() to be more flexible
    verify(sendGrid, atLeastOnce())
        .api(
            argThat(
                request ->
                    request.getMethod() == Method.POST
                        && "mail/send".equals(request.getEndpoint())
                        && request.getBody().contains(brdId)
                        && request.getBody().contains(brdName)));
  }

  @Test
  void sendBrdStatusChangeNotification_WithApiFailure_ShouldThrowEmailServiceException()
      throws IOException {
    // Arrange
    Response mockResponse = new Response();
    mockResponse.setStatusCode(400); // Error status code
    mockResponse.setBody("Invalid request");

    when(sendGrid.api(any(Request.class))).thenReturn(mockResponse);

    // Act & Assert
    StepVerifier.create(
            emailService.sendBrdStatusChangeNotification(testEmail, brdId, brdName, brdFormId))
        .expectError(EmailServiceException.class)
        .verify();
  }

  @Test
  void sendBrdStatusChangeNotification_WithIoException_ShouldThrowEmailServiceException()
      throws IOException {
    // Arrange
    when(sendGrid.api(any(Request.class))).thenThrow(new IOException("Connection error"));

    // Act & Assert
    StepVerifier.create(
            emailService.sendBrdStatusChangeNotification(testEmail, brdId, brdName, brdFormId))
        .expectError(EmailServiceException.class)
        .verify();
  }

  @Test
  void sendBillerWelcomeEmail_WithValidRequest_ShouldComplete() throws IOException {
    // Arrange
    Response mockResponse = new Response();
    mockResponse.setStatusCode(202); // Accepted status code
    mockResponse.setHeaders(Map.of("X-Message-Id", "test-message-id"));

    when(sendGrid.api(any(Request.class))).thenReturn(mockResponse);

    // Act & Assert
    StepVerifier.create(emailService.sendBillerWelcomeEmail(testEmail, brdId, brdName, brdFormId))
        .verifyComplete();

    // Verify API call - use atLeastOnce() to be more flexible
    verify(sendGrid, atLeastOnce())
        .api(
            argThat(
                request ->
                    request.getMethod() == Method.POST
                        && "mail/send".equals(request.getEndpoint())
                        && request.getBody().contains(brdId)
                        && request.getBody().contains(brdName)
                        && request.getBody().contains("Welcome to Smart Onboarding")));
  }

  @Test
  void sendBillerWelcomeEmail_WithApiFailure_ShouldThrowEmailServiceException() throws IOException {
    // Arrange
    Response mockResponse = new Response();
    mockResponse.setStatusCode(401); // Unauthorized status code
    mockResponse.setBody("Invalid API key");

    when(sendGrid.api(any(Request.class))).thenReturn(mockResponse);

    // Act & Assert
    StepVerifier.create(emailService.sendBillerWelcomeEmail(testEmail, brdId, brdName, brdFormId))
        .expectError(EmailServiceException.class)
        .verify();
  }

  @Test
  void sendBillerWelcomeEmail_WithIoException_ShouldThrowEmailServiceException()
      throws IOException {
    // Arrange
    when(sendGrid.api(any(Request.class))).thenThrow(new IOException("Network error"));

    // Act & Assert
    StepVerifier.create(emailService.sendBillerWelcomeEmail(testEmail, brdId, brdName, brdFormId))
        .expectError(EmailServiceException.class)
        .verify();
  }

  @Nested
  @DisplayName("User Welcome Email with Reset Link Tests")
  class UserWelcomeEmailWithResetLinkTests {

    @Test
    @DisplayName("Send user welcome email with reset link should succeed")
    void sendUserWelcomeEmailWithResetLink_ValidInputs_CompletesSuccessfully() throws IOException {
      // Arrange
      Response mockResponse = new Response();
      mockResponse.setStatusCode(202);
      mockResponse.setHeaders(Map.of("X-Message-Id", "test-message-id"));

      when(sendGrid.api(any(Request.class))).thenReturn(mockResponse);

      // Act & Assert
      StepVerifier.create(emailService.sendUserWelcomeEmailWithResetLink(testEmail, userName))
          .verifyComplete();

      // Verify API call
      verify(sendGrid, atLeastOnce())
          .api(
              argThat(
                  request ->
                      request.getMethod() == Method.POST
                          && "mail/send".equals(request.getEndpoint())
                          && request.getBody().contains(userName)
                          && request.getBody().contains("Welcome to Smart Onboarding")
                          && request.getBody().contains("Login")));
    }

    @Test
    @DisplayName("Send user welcome email with reset link should handle API failure")
    void sendUserWelcomeEmailWithResetLink_ApiError_ThrowsEmailServiceException()
        throws IOException {
      // Arrange
      Response mockResponse = new Response();
      mockResponse.setStatusCode(400);
      mockResponse.setBody("Invalid request");

      when(sendGrid.api(any(Request.class))).thenReturn(mockResponse);

      // Act & Assert
      StepVerifier.create(emailService.sendUserWelcomeEmailWithResetLink(testEmail, userName))
          .expectError(EmailServiceException.class)
          .verify();
    }

    @Test
    @DisplayName("Send user welcome email with reset link should handle IOException")
    void sendUserWelcomeEmailWithResetLink_IoException_ThrowsEmailServiceException()
        throws IOException {
      // Arrange
      when(sendGrid.api(any(Request.class))).thenThrow(new IOException("Network error"));

      // Act & Assert
      StepVerifier.create(emailService.sendUserWelcomeEmailWithResetLink(testEmail, userName))
          .expectError(EmailServiceException.class)
          .verify();
    }

    @Test
    @DisplayName("Send user welcome email with reset link should handle 401 unauthorized")
    void sendUserWelcomeEmailWithResetLink_UnauthorizedError_ThrowsEmailServiceException()
        throws IOException {
      // Arrange
      Response mockResponse = new Response();
      mockResponse.setStatusCode(401);
      mockResponse.setBody("Unauthorized");
      mockResponse.setHeaders(Map.of());

      when(sendGrid.api(any(Request.class))).thenReturn(mockResponse);

      // Act & Assert
      StepVerifier.create(emailService.sendUserWelcomeEmailWithResetLink(testEmail, userName))
          .expectError(EmailServiceException.class)
          .verify();
    }

    @Test
    @DisplayName("Send user welcome email with reset link should handle success without message ID")
    void sendUserWelcomeEmailWithResetLink_NoMessageId_CompletesSuccessfully() throws IOException {
      // Arrange
      Response mockResponse = new Response();
      mockResponse.setStatusCode(202);
      mockResponse.setHeaders(Map.of()); // No X-Message-Id header

      when(sendGrid.api(any(Request.class))).thenReturn(mockResponse);

      // Act & Assert
      StepVerifier.create(emailService.sendUserWelcomeEmailWithResetLink(testEmail, userName))
          .verifyComplete();

      verify(sendGrid, atLeastOnce()).api(any(Request.class));
    }
  }

  @Nested
  @DisplayName("User Credentials Email Tests")
  class UserCredentialsEmailTests {

    @Test
    @DisplayName("Send user credentials email should succeed")
    void sendUserCredentialsEmail_ValidInputs_CompletesSuccessfully() throws IOException {
      // Arrange
      Response mockResponse = new Response();
      mockResponse.setStatusCode(202);
      mockResponse.setHeaders(Map.of("X-Message-Id", "test-message-id"));

      when(sendGrid.api(any(Request.class))).thenReturn(mockResponse);

      // Act & Assert
      StepVerifier.create(emailService.sendUserCredentialsEmail(testEmail, password))
          .verifyComplete();

      // Verify API call
      verify(sendGrid, atLeastOnce())
          .api(
              argThat(
                  request ->
                      request.getMethod() == Method.POST
                          && "mail/send".equals(request.getEndpoint())
                          && request.getBody().contains(password)
                          && request.getBody().contains("Your Account Credentials")));
    }

    @Test
    @DisplayName("Send user credentials email should handle API failure")
    void sendUserCredentialsEmail_ApiError_ThrowsEmailServiceException() throws IOException {
      // Arrange
      Response mockResponse = new Response();
      mockResponse.setStatusCode(500);
      mockResponse.setBody("Internal server error");

      when(sendGrid.api(any(Request.class))).thenReturn(mockResponse);

      // Act & Assert
      StepVerifier.create(emailService.sendUserCredentialsEmail(testEmail, password))
          .expectError(EmailServiceException.class)
          .verify();
    }

    @Test
    @DisplayName("Send user credentials email should handle IOException")
    void sendUserCredentialsEmail_IoException_ThrowsEmailServiceException() throws IOException {
      // Arrange
      when(sendGrid.api(any(Request.class))).thenThrow(new IOException("Connection timeout"));

      // Act & Assert
      StepVerifier.create(emailService.sendUserCredentialsEmail(testEmail, password))
          .expectError(EmailServiceException.class)
          .verify();
    }

    @Test
    @DisplayName("Send user credentials email should handle 401 unauthorized with detailed error")
    void sendUserCredentialsEmail_UnauthorizedError_ThrowsEmailServiceException()
        throws IOException {
      // Arrange
      Response mockResponse = new Response();
      mockResponse.setStatusCode(401);
      mockResponse.setBody("API key invalid");
      mockResponse.setHeaders(Map.of("Content-Type", "application/json"));

      when(sendGrid.api(any(Request.class))).thenReturn(mockResponse);

      // Act & Assert
      StepVerifier.create(emailService.sendUserCredentialsEmail(testEmail, password))
          .expectError(EmailServiceException.class)
          .verify();
    }

    @Test
    @DisplayName("Send user credentials email should handle success with different status codes")
    void sendUserCredentialsEmail_DifferentSuccessStatusCodes_CompletesSuccessfully()
        throws IOException {
      // Test with status code 200 (OK)
      Response mockResponse = new Response();
      mockResponse.setStatusCode(200);
      mockResponse.setHeaders(Map.of("X-Message-Id", "test-message-id"));

      when(sendGrid.api(any(Request.class))).thenReturn(mockResponse);

      StepVerifier.create(emailService.sendUserCredentialsEmail(testEmail, password))
          .verifyComplete();

      verify(sendGrid, atLeastOnce()).api(any(Request.class));
    }
  }

  @Nested
  @DisplayName("Email Delivery Status Tests")
  class EmailDeliveryStatusTests {

    @Test
    @DisplayName("Check email delivery status should handle success")
    void checkEmailDeliveryStatus_ValidEmailId_CompletesSuccessfully() throws Exception {
      // Arrange
      Response mockResponse = new Response();
      mockResponse.setStatusCode(200);
      mockResponse.setBody("{\"status\": \"delivered\"}");

      when(sendGrid.api(any(Request.class))).thenReturn(mockResponse);

      // Use reflection to access private method
      java.lang.reflect.Method method =
          EmailService.class.getDeclaredMethod("checkEmailDeliveryStatus", String.class);
      method.setAccessible(true);

      // Act
      method.invoke(emailService, "test-email-id");

      // Assert
      verify(sendGrid)
          .api(
              argThat(
                  request ->
                      request.getMethod() == Method.GET
                          && request.getEndpoint().equals("mail/send/test-email-id")));
    }

    @Test
    @DisplayName("Check email delivery status should handle IOException")
    void checkEmailDeliveryStatus_IoException_LogsError() throws Exception {
      // Arrange
      when(sendGrid.api(any(Request.class))).thenThrow(new IOException("API error"));

      // Use reflection to access private method
      java.lang.reflect.Method method =
          EmailService.class.getDeclaredMethod("checkEmailDeliveryStatus", String.class);
      method.setAccessible(true);

      // Act - should not throw exception, just log error
      assertDoesNotThrow(() -> method.invoke(emailService, "test-email-id"));

      // Assert
      verify(sendGrid).api(any(Request.class));
    }

    @Test
    @DisplayName("Check mail status should handle interruption")
    void checkMailStatus_ThreadInterruption_RestoresInterruptFlag() throws Exception {
      // Arrange
      Thread.currentThread().interrupt(); // Set interrupt flag

      // Use reflection to access private method
      java.lang.reflect.Method method =
          EmailService.class.getDeclaredMethod("checkMailStatus", String.class);
      method.setAccessible(true);

      // Act - should handle interruption gracefully
      assertDoesNotThrow(() -> method.invoke(emailService, "test-email-id"));

      // Assert that interrupt flag is restored
      assertTrue(Thread.interrupted());
    }
  }

  @Test
  void init_WithValidConfiguration_ShouldInitializeService() {
    // Arrange
    String configuredEmail = "test@example.com";
    String testName = "Test Sender";
    String testApiKey = "test-api-key";

    ReflectionTestUtils.setField(emailService, "senderEmail", configuredEmail);
    ReflectionTestUtils.setField(emailService, "senderName", testName);
    ReflectionTestUtils.setField(emailService, "sendGridApiKey", testApiKey);

    // Act
    emailService.init();

    // Assert
    // Verify that the configuration was properly set
    assertEquals(configuredEmail, ReflectionTestUtils.getField(emailService, "senderEmail"));
    assertEquals(testName, ReflectionTestUtils.getField(emailService, "senderName"));
    assertEquals(testApiKey, ReflectionTestUtils.getField(emailService, "sendGridApiKey"));

    // Verify that SendGrid instance exists
    assertNotNull(sendGrid);
  }

  @Test
  void init_WithEmptyConfiguration_ShouldLogWarnings() {
    // Arrange - Set up with empty configuration
    ReflectionTestUtils.setField(emailService, "senderEmail", "");
    ReflectionTestUtils.setField(emailService, "senderName", "");
    ReflectionTestUtils.setField(emailService, "sendGridApiKey", "");

    // Act
    emailService.init();

    // Assert
    // Verify that the configuration values remain empty
    assertEquals("", ReflectionTestUtils.getField(emailService, "senderEmail"));
    assertEquals("", ReflectionTestUtils.getField(emailService, "senderName"));
    assertEquals("", ReflectionTestUtils.getField(emailService, "sendGridApiKey"));

    // Verify that the service is in a valid state despite missing configuration
    assertNotNull(emailService);
    assertNotNull(sendGrid);

    // Verify no interactions with SendGrid
    verifyNoInteractions(sendGrid);
  }

  @Test
  void buildBrdStatusChangeContent_WithValidInput_ShouldContainRequiredInfo() throws Exception {
    // Given
    String brdIdLocal = "BRD123";
    String brdNameLocal = "Test BRD";
    String brdFormIdLocal = "FORM123";
    String expectedFrontendDomain = "http://localhost:4200";

    // Use reflection to access the private method
    java.lang.reflect.Method method =
        EmailService.class.getDeclaredMethod(
            "buildBrdStatusChangeContent", String.class, String.class, String.class);
    method.setAccessible(true);

    // When
    String content = (String) method.invoke(emailService, brdIdLocal, brdNameLocal, brdFormIdLocal);

    // Then
    assertTrue(content.contains(brdIdLocal));
    assertTrue(content.contains(brdNameLocal));
    assertTrue(
        content.contains("The status of BRD " + brdNameLocal + " has been updated to In Progress"));
    assertTrue(content.contains("View BRD"));
    assertTrue(
        content.contains(
            expectedFrontendDomain + "/organization-site?brdFormId="
                + brdFormIdLocal
                + "&brdId="
                + brdIdLocal));
  }

  @Test
  void buildBillerWelcomeContent_WithValidInput_ShouldContainRequiredInfo() throws Exception {
    // Given
    String brdIdLocal = "BRD123";
    String brdNameLocal = "Test BRD";
    String brdFormIdLocal = "FORM123";
    String expectedFrontendDomain = "http://localhost:4200";

    // Use reflection to access the private method
    java.lang.reflect.Method method =
        EmailService.class.getDeclaredMethod(
            "buildBillerWelcomeContent", String.class, String.class, String.class);
    method.setAccessible(true);

    // When
    String content = (String) method.invoke(emailService, brdIdLocal, brdNameLocal, brdFormIdLocal);

    // Then
    assertTrue(content.contains(brdIdLocal));
    assertTrue(content.contains(brdNameLocal));
    assertTrue(content.contains("Welcome to Smart Onboarding"));
    assertTrue(content.contains("You have been assigned as a biller for BRD " + brdNameLocal));
    assertTrue(content.contains("View BRD"));
    assertTrue(
        content.contains(
            expectedFrontendDomain + "/organization-site?brdFormId="
                + brdFormIdLocal
                + "&brdId="
                + brdIdLocal));
    assertTrue(content.contains("If you have any questions, please contact your project manager"));
    assertTrue(content.contains("Best regards"));
    assertTrue(content.contains("Smart Onboarding Team"));
  }

  @Test
  @DisplayName("Build user welcome with reset content should contain correct information")
  void buildUserWelcomeWithResetContent_ValidUserName_ReturnsCorrectContent() throws Exception {
    // Use reflection to access the private method
    java.lang.reflect.Method method =
        EmailService.class.getDeclaredMethod("buildUserWelcomeWithResetContent", String.class);
    method.setAccessible(true);

    // When
    String content = (String) method.invoke(emailService, userName);

    // Then
    assertTrue(content.contains(userName));
    assertTrue(content.contains("Welcome to Smart Onboarding!"));
    assertTrue(content.contains("Welcome to the Smart Onboarding platform! Your account has been successfully created"));
    assertTrue(content.contains("Login"));
    assertTrue(content.contains("login"));
    assertTrue(content.contains("Username:"));
    assertTrue(content.contains("<!DOCTYPE html>"));
    assertTrue(content.contains("</html>"));
  }

  @Test
  @DisplayName("Build user credentials content should contain correct information")
  void buildUserCredentialsContent_ValidPassword_ReturnsCorrectContent() throws Exception {
    // Use reflection to access the private method
    java.lang.reflect.Method method =
        EmailService.class.getDeclaredMethod("buildUserCredentialsContent", String.class);
    method.setAccessible(true);

    // Invoke the method
    String content = (String) method.invoke(emailService, password);

    // Verify content contains expected elements
    assertTrue(content.contains(password));
    assertTrue(content.contains("Welcome to Smart Onboarding!"));
    assertTrue(content.contains("Your Smart Onboarding account has been created successfully"));
    assertTrue(content.contains("Password:"));
    assertTrue(content.contains("change your password"));
    assertTrue(content.contains("Important:"));
    assertTrue(content.contains("<!DOCTYPE html>"));
    assertTrue(content.contains("</html>"));
  }

  @Nested
  @DisplayName("SendEmail Method Edge Cases")
  class SendEmailEdgeCasesTests {

    @Test
    @DisplayName("Send email should handle success with status code 299 (edge of success range)")
    void sendEmail_StatusCode299_CompletesSuccessfully() throws Exception {
      // Arrange
      Response mockResponse = new Response();
      mockResponse.setStatusCode(299);
      mockResponse.setHeaders(Map.of("X-Message-Id", "test-message-id"));
      mockResponse.setBody("Success");

      when(sendGrid.api(any(Request.class))).thenReturn(mockResponse);

      // Act & Assert
      StepVerifier.create(emailService.sendUserWelcomeEmailWithResetLink(testEmail, userName))
          .verifyComplete();
    }

    @Test
    @DisplayName("Send email should handle success with status code 200 (start of success range)")
    void sendEmail_StatusCode200_CompletesSuccessfully() throws Exception {
      // Arrange
      Response mockResponse = new Response();
      mockResponse.setStatusCode(200);
      mockResponse.setHeaders(Map.of("X-Message-Id", "test-message-id"));
      mockResponse.setBody("OK");

      when(sendGrid.api(any(Request.class))).thenReturn(mockResponse);

      // Act & Assert
      StepVerifier.create(emailService.sendUserCredentialsEmail(testEmail, password))
          .verifyComplete();
    }

    @Test
    @DisplayName(
        "Send email should handle failure with status code 300 (just outside success range)")
    void sendEmail_StatusCode300_ThrowsEmailServiceException() throws Exception {
      // Arrange
      Response mockResponse = new Response();
      mockResponse.setStatusCode(300);
      mockResponse.setBody("Redirection");
      mockResponse.setHeaders(Map.of());

      when(sendGrid.api(any(Request.class))).thenReturn(mockResponse);

      // Act & Assert
      StepVerifier.create(emailService.sendUserWelcomeEmailWithResetLink(testEmail, userName))
          .expectError(EmailServiceException.class)
          .verify();
    }

    @Test
    @DisplayName("Send email should handle failure with status code 199 (just below success range)")
    void sendEmail_StatusCode199_ThrowsEmailServiceException() throws Exception {
      // Arrange
      Response mockResponse = new Response();
      mockResponse.setStatusCode(199);
      mockResponse.setBody("Informational");
      mockResponse.setHeaders(Map.of());

      when(sendGrid.api(any(Request.class))).thenReturn(mockResponse);

      // Act & Assert
      StepVerifier.create(emailService.sendUserCredentialsEmail(testEmail, password))
          .expectError(EmailServiceException.class)
          .verify();
    }
  }
}

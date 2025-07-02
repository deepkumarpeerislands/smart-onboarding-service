package com.aci.smart_onboarding.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.RoleSwitchRequest;
import com.aci.smart_onboarding.dto.UserInfo;
import com.aci.smart_onboarding.model.User;
import com.aci.smart_onboarding.repository.UserRepository;
import com.aci.smart_onboarding.security.service.JwtService;
import com.aci.smart_onboarding.security.token.JwtAuthenticationToken;
import com.aci.smart_onboarding.service.RedisSessionService;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class RoleControllerTest {

  @Mock private JwtService jwtService;

  @Mock private UserRepository userRepository;

  @Mock private RedisSessionService redisSessionService;

  private RoleController roleController;

  @BeforeEach
  void setUp() {
    roleController = new RoleController(jwtService, userRepository, redisSessionService);
  }

  @Test
  void switchRole_WhenValidRequest_ShouldSwitchRoleSuccessfully() {
    // Given
    String userId = "test@example.com";
    String currentJti = "currentJti";
    String newJti = "newJti";
    String currentRole = SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM;
    String newRole = SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_BA;
    List<String> roles =
        Arrays.asList(
            SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM,
            SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_BA);

    User user =
        User.builder()
            .id("1")
            .email(userId)
            .firstName("Test")
            .lastName("User")
            .activeRole("PM")
            .roles(Arrays.asList("PM", "BA"))
            .build();

    User updatedUser =
        User.builder()
            .id("1")
            .email(userId)
            .firstName("Test")
            .lastName("User")
            .activeRole("BA")
            .roles(Arrays.asList("PM", "BA"))
            .build();

    RoleSwitchRequest request = new RoleSwitchRequest();
    request.setRole("BA");

    JwtAuthenticationToken authentication =
        new JwtAuthenticationToken(userId, currentRole, roles, currentJti);

    when(userRepository.findByEmail(userId)).thenReturn(Mono.just(user));
    when(redisSessionService.generateJti()).thenReturn(newJti);

    when(redisSessionService.invalidateSession((userId), (currentJti))).thenReturn(Mono.just(true));

    when(redisSessionService.createSession(eq(userId), eq(newJti), eq(newRole), anyList()))
        .thenReturn(Mono.just(true));
    when(jwtService.generateToken(eq(userId), anyList(), eq(newRole), eq(newJti)))
        .thenReturn("newToken");
    when(userRepository.save(any(User.class))).thenReturn(Mono.just(updatedUser));

    // When & Then
    StepVerifier.create(roleController.switchRole(request, authentication))
        .expectNextMatches(
            response -> {
              assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
              Api<UserInfo> apiResponse = response.getBody();
              assertThat(apiResponse).isNotNull();
              assertThat(apiResponse.getStatus()).isEqualTo("success");
              assertThat(apiResponse.getMessage()).isEqualTo("Role switched successfully");
              assertThat(apiResponse.getData()).isPresent();

              UserInfo userInfo = apiResponse.getData().get();
              assertThat(userInfo.getUsername()).isEqualTo(userId);
              assertThat(userInfo.getFirstName()).isEqualTo("Test");
              assertThat(userInfo.getLastName()).isEqualTo("User");
              assertThat(userInfo.getActiveRole()).isEqualTo(newRole);
              assertThat(userInfo.getRoles()).containsExactlyInAnyOrderElementsOf(roles);
              assertThat(userInfo.getToken()).isEqualTo("newToken");
              assertThat(userInfo.getEmail()).isEqualTo(userId);
              return true;
            })
        .verifyComplete();

    verify(userRepository)
        .save(
            argThat(
                savedUser ->
                    savedUser.getEmail().equals(userId)
                        && savedUser.getActiveRole().equals("BA")
                        && savedUser.getRoles().containsAll(Arrays.asList("PM", "BA"))));
  }

  @Test
  void switchRole_WhenInvalidRole_ShouldReturnError() {
    // Given
    String userId = "test@example.com";
    String currentJti = "currentJti";
    String currentRole = SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM;
    List<String> roles = List.of(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM);

    RoleSwitchRequest request = new RoleSwitchRequest();
    request.setRole(SecurityConstants.ROLE_BA);

    JwtAuthenticationToken authentication =
        new JwtAuthenticationToken(userId, currentRole, roles, currentJti);

    // When & Then
    StepVerifier.create(roleController.switchRole(request, authentication))
        .expectErrorMatches(
            throwable ->
                throwable instanceof IllegalArgumentException
                    && throwable
                        .getMessage()
                        .equals("User does not have access to the requested role"))
        .verify();

    verify(userRepository, never()).findByEmail(anyString());
  }

  @Test
  void switchRole_WhenUserNotFound_ShouldReturnError() {
    // Given
    String userId = "test@example.com";
    String currentJti = "currentJti";
    String currentRole = SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM;
    List<String> roles =
        Arrays.asList(
            SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM,
            SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_BA);

    RoleSwitchRequest request = new RoleSwitchRequest();
    request.setRole(SecurityConstants.ROLE_BA);

    JwtAuthenticationToken authentication =
        new JwtAuthenticationToken(userId, currentRole, roles, currentJti);

    when(userRepository.findByEmail(userId)).thenReturn(Mono.empty());
    when(redisSessionService.generateJti()).thenReturn("newJti");
    when(redisSessionService.invalidateSession(anyString(), anyString()))
        .thenReturn(Mono.just(true));

    // When & Then
    StepVerifier.create(roleController.switchRole(request, authentication))
        .expectErrorMatches(
            throwable ->
                throwable instanceof IllegalArgumentException
                    && throwable.getMessage().equals("User not found"))
        .verify();
  }

  @Test
  void switchRole_Success() {
    // Given
    String userId = "test@example.com";
    String currentJti = "current-jti";
    String newJti = "new-jti";
    String newRole = SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_BA;
    List<String> roles =
        Arrays.asList(
            SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM,
            SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_BA);
    String newToken = "new-token";

    RoleSwitchRequest request = new RoleSwitchRequest();
    request.setRole(SecurityConstants.ROLE_BA);

    JwtAuthenticationToken authentication = mock(JwtAuthenticationToken.class);
    when(authentication.getName()).thenReturn(userId);
    when(authentication.getJti()).thenReturn(currentJti);
    when(authentication.getRoles()).thenReturn(roles);

    User user =
        User.builder()
            .id("1")
            .email(userId)
            .firstName("Test")
            .lastName("User")
            .activeRole(SecurityConstants.ROLE_PM)
            .roles(Arrays.asList(SecurityConstants.ROLE_PM, SecurityConstants.ROLE_BA))
            .build();

    User updatedUser =
        User.builder()
            .id("1")
            .email(userId)
            .firstName("Test")
            .lastName("User")
            .activeRole("BA")
            .roles(Arrays.asList(SecurityConstants.ROLE_PM, SecurityConstants.ROLE_BA))
            .build();

    when(userRepository.findByEmail(userId)).thenReturn(Mono.just(user));
    when(redisSessionService.generateJti()).thenReturn(newJti);

    when(redisSessionService.invalidateSession((userId), (currentJti))).thenReturn(Mono.just(true));

    when(redisSessionService.createSession(eq(userId), eq(newJti), eq(newRole), anyList()))
        .thenReturn(Mono.just(true));
    when(jwtService.generateToken(eq(userId), anyList(), eq(newRole), eq(newJti)))
        .thenReturn(newToken);
    when(userRepository.save(any(User.class))).thenReturn(Mono.just(updatedUser));

    // When
    Mono<ResponseEntity<Api<UserInfo>>> result = roleController.switchRole(request, authentication);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
              Api<UserInfo> apiResponse = response.getBody();
              assertThat(apiResponse).isNotNull();
              assertThat(apiResponse.getStatus()).isEqualTo("success");
              assertThat(apiResponse.getMessage()).isEqualTo("Role switched successfully");
              assertThat(apiResponse.getData()).isPresent();

              UserInfo userInfo = apiResponse.getData().get();
              assertThat(userInfo.getUsername()).isEqualTo(userId);
              assertThat(userInfo.getFirstName()).isEqualTo("Test");
              assertThat(userInfo.getLastName()).isEqualTo("User");
              assertThat(userInfo.getActiveRole()).isEqualTo(newRole);
              assertThat(userInfo.getRoles()).containsExactlyInAnyOrderElementsOf(roles);
              assertThat(userInfo.getToken()).isEqualTo(newToken);
              assertThat(userInfo.getEmail()).isEqualTo(userId);
              return true;
            })
        .verifyComplete();

    verify(userRepository)
        .save(
            argThat(
                savedUser ->
                    savedUser.getEmail().equals(userId)
                        && savedUser.getActiveRole().equals("BA")
                        && savedUser.getRoles().containsAll(Arrays.asList("PM", "BA"))));
  }

  @Test
  void switchRole_WhenRoleIsInvalidString_ShouldReturnError() {
    // Given
    String userId = "test@example.com";
    String currentJti = "currentJti";
    String currentRole = SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM;
    List<String> roles = List.of(SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_PM);

    RoleSwitchRequest request = new RoleSwitchRequest();
    request.setRole("26$=%");

    JwtAuthenticationToken authentication =
        new JwtAuthenticationToken(userId, currentRole, roles, currentJti);

    // When & Then
    StepVerifier.create(roleController.switchRole(request, authentication))
        .expectErrorMatches(
            throwable ->
                throwable instanceof IllegalArgumentException
                    && throwable.getMessage().equals("User does not have access to the requested role"))
        .verify();

    verify(userRepository, never()).findByEmail(anyString());
  }
}

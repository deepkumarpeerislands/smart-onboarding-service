package com.aci.smart_onboarding.security.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.security.config.BRDSecurityConfig;
import com.aci.smart_onboarding.security.config.BRDSecurityConfig.StatusPermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

@ExtendWith(MockitoExtension.class)
class BRDSecurityServiceTest {

  @Mock private BRDSecurityConfig brdSecurityConfig;

  @Mock private BRDSecurityConfig.Security security;

  @Mock private BRDSecurityConfig.Permission permission;

  @Mock private StatusPermission statusPermission;

  @Mock private ServerWebExchange exchange;

  @Mock private ServerHttpRequest request;

  @Mock private Authentication authentication;

  @Mock private SecurityContext securityContext;
  @InjectMocks private BRDSecurityService brdSecurityService;

  @BeforeEach
  void setUp() {
    brdSecurityService = new BRDSecurityService(brdSecurityConfig);
  }

  @Test
  void withSecurityCheck_WithNullStatus_ShouldThrowAccessDenied() {
    // When & Then
    StepVerifier.create(brdSecurityService.withSecurityCheck(null))
        .expectError(AccessDeniedException.class)
        .verify();
  }

  @Test
  void withSecurityCheck_WithSecurityDisabled_ShouldAllowAccess() {
    // Given
    when(brdSecurityConfig.getSecurity()).thenReturn(security);
    when(security.isEnabled()).thenReturn(false);

    // When & Then
    StepVerifier.create(brdSecurityService.withSecurityCheck("DRAFT")).verifyComplete();
  }

  @Test
  void withSecurityCheck_WithValidPermissions_ShouldAllowAccess() {
    // Given
    when(brdSecurityConfig.getSecurity()).thenReturn(security);
    when(security.isEnabled()).thenReturn(true);
    when(security.isRoleBasedAccessEnabled()).thenReturn(true);
    when(security.getPermissions()).thenReturn(Collections.singletonList(permission));
    when(permission.getRole()).thenReturn("ROLE_BILLER");
    when(permission.getAllowedStatuses()).thenReturn(Collections.singletonList(statusPermission));
    when(statusPermission.getStatus()).thenReturn("DRAFT");
    when(statusPermission.getMethods()).thenReturn(Collections.singletonList("GET"));

    // Mock security context
    Authentication auth = mock(Authentication.class);
    when(auth.getAuthorities())
        .thenAnswer(
            invocation -> {
              Collection<GrantedAuthority> authorities = new ArrayList<>();
              authorities.add(new SimpleGrantedAuthority("ROLE_BILLER"));
              return authorities;
            });
    when(securityContext.getAuthentication()).thenReturn(auth);
    when(exchange.getRequest()).thenReturn(request);
    when(request.getMethod()).thenReturn(HttpMethod.GET);

    // When & Then
    StepVerifier.create(
            brdSecurityService
                .withSecurityCheck("DRAFT")
                .contextWrite(
                    ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)))
                .contextWrite(Context.of(ServerWebExchange.class, exchange)))
        .verifyComplete();
  }

  @Test
  void withSecurityCheck_WithInvalidPermissions_ShouldDenyAccess() {
    // Given
    when(brdSecurityConfig.getSecurity()).thenReturn(security);
    when(security.isEnabled()).thenReturn(true);
    when(security.isRoleBasedAccessEnabled()).thenReturn(true);

    // Mock security context with different role
    Authentication auth = mock(Authentication.class);
    when(auth.getAuthorities())
        .thenAnswer(
            invocation -> {
              Collection<GrantedAuthority> authorities = new ArrayList<>();
              authorities.add(new SimpleGrantedAuthority("ROLE_BA"));
              return authorities;
            });
    when(securityContext.getAuthentication()).thenReturn(auth);
    when(exchange.getRequest()).thenReturn(request);
    when(request.getMethod()).thenReturn(HttpMethod.GET);

    // When & Then
    StepVerifier.create(
            brdSecurityService
                .withSecurityCheck("DRAFT")
                .contextWrite(
                    ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)))
                .contextWrite(Context.of(ServerWebExchange.class, exchange)))
        .expectErrorMatches(
            throwable ->
                throwable instanceof AccessDeniedException
                    && throwable
                        .getMessage()
                        .equals(
                            "This BRD is currently in DRAFT status and not yet open for collaboration."))
        .verify();
  }

  @Test
  void getCurrentUserRole_WithAuthenticatedUser_ShouldReturnRole() {
    // Given
    Authentication auth = mock(Authentication.class);
    when(auth.getAuthorities())
        .thenAnswer(
            invocation -> {
              Collection<GrantedAuthority> authorities = new ArrayList<>();
              authorities.add(new SimpleGrantedAuthority("ROLE_BILLER"));
              return authorities;
            });
    SecurityContext context = mock(SecurityContext.class);
    when(context.getAuthentication()).thenReturn(auth);

    // When & Then
    StepVerifier.create(
            brdSecurityService
                .getCurrentUserRole()
                .contextWrite(
                    ReactiveSecurityContextHolder.withSecurityContext(Mono.just(context))))
        .expectNext("ROLE_BILLER")
        .verifyComplete();
  }

  @Test
  void getCurrentUserRole_WithNoAuthorities_ShouldThrowNullPointer() {
    // Given
    Authentication auth = mock(Authentication.class);
    doReturn(Collections.emptySet()).when(auth).getAuthorities();
    SecurityContext context = mock(SecurityContext.class);
    when(context.getAuthentication()).thenReturn(auth);

    // When & Then
    StepVerifier.create(
            brdSecurityService
                .getCurrentUserRole()
                .contextWrite(
                    ReactiveSecurityContextHolder.withSecurityContext(Mono.just(context))))
        .expectError(NullPointerException.class)
        .verify();
  }

  @Test
  void getCurrentUserRole_WithNoAuthentication_ShouldReturnEmpty() {
    // When & Then
    StepVerifier.create(
            brdSecurityService
                .getCurrentUserRole()
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.empty())))
        .verifyComplete();
  }

  @Test
  void isStatusAllowedForRole_WithSecurityDisabled_ShouldReturnTrue() {
    // Given
    when(brdSecurityConfig.getSecurity()).thenReturn(security);
    when(security.isEnabled()).thenReturn(false);

    // When & Then
    assertTrue(brdSecurityService.isStatusAllowedForRole("ROLE_BILLER", "DRAFT", HttpMethod.GET));
  }

  @Test
  void isStatusAllowedForRole_WithValidPermissions_ShouldReturnTrue() {
    // Given
    when(brdSecurityConfig.getSecurity()).thenReturn(security);
    when(security.isEnabled()).thenReturn(true);
    when(security.isRoleBasedAccessEnabled()).thenReturn(true);
    when(security.getPermissions()).thenReturn(Collections.singletonList(permission));
    when(permission.getRole()).thenReturn("ROLE_BILLER");
    when(permission.getAllowedStatuses()).thenReturn(Collections.singletonList(statusPermission));
    when(statusPermission.getStatus()).thenReturn("DRAFT");
    when(statusPermission.getMethods()).thenReturn(Collections.singletonList("GET"));

    // When & Then
    assertTrue(brdSecurityService.isStatusAllowedForRole("ROLE_BILLER", "DRAFT", HttpMethod.GET));
  }

  @Test
  void isStatusAllowedForRole_WithInvalidPermissions_ShouldReturnFalse() {
    // Given
    when(brdSecurityConfig.getSecurity()).thenReturn(security);
    when(security.isEnabled()).thenReturn(true);
    when(security.isRoleBasedAccessEnabled()).thenReturn(true);
    when(security.getPermissions()).thenReturn(Collections.singletonList(permission));
    when(permission.getRole()).thenReturn("ROLE_BILLER");

    // When & Then
    assertFalse(brdSecurityService.isStatusAllowedForRole("ROLE_BA", "DRAFT", HttpMethod.GET));
  }

  @Test
  void getAllowedStatusesForRole_WithSecurityDisabled_ShouldReturnEmptyList() {
    // Given
    when(brdSecurityConfig.getSecurity()).thenReturn(security);
    when(security.isEnabled()).thenReturn(false);

    // When & Then
    assertTrue(brdSecurityService.getAllowedStatusesForRole("ROLE_BILLER").isEmpty());
  }

  @Test
  void getAllowedStatusesForRole_WithValidPermissions_ShouldReturnStatuses() {
    // Given
    when(brdSecurityConfig.getSecurity()).thenReturn(security);
    when(security.isEnabled()).thenReturn(true);
    when(security.isRoleBasedAccessEnabled()).thenReturn(true);
    when(security.getPermissions()).thenReturn(Collections.singletonList(permission));
    when(permission.getRole()).thenReturn("ROLE_BILLER");
    when(permission.getAllowedStatuses()).thenReturn(Collections.singletonList(statusPermission));
    when(statusPermission.getStatus()).thenReturn("DRAFT");

    // When
    List<String> statuses = brdSecurityService.getAllowedStatusesForRole("ROLE_BILLER");

    // Then
    assertEquals(1, statuses.size());
    assertEquals("DRAFT", statuses.getFirst());
  }

  @Test
  void getAllowedStatusesForRole_WithInvalidPermissions_ShouldReturnEmptyList() {
    // Given
    when(brdSecurityConfig.getSecurity()).thenReturn(security);
    when(security.isEnabled()).thenReturn(true);
    when(security.isRoleBasedAccessEnabled()).thenReturn(true);
    when(security.getPermissions()).thenReturn(Collections.singletonList(permission));
    when(permission.getRole()).thenReturn("ROLE_BILLER");

    // When & Then
    assertTrue(brdSecurityService.getAllowedStatusesForRole("ROLE_BA").isEmpty());
  }

  @Test
  void withSecurityCheck_WithInvalidMethod_ShouldDenyAccess() {
    // Given
    when(brdSecurityConfig.getSecurity()).thenReturn(security);
    when(security.isEnabled()).thenReturn(true);
    when(security.isRoleBasedAccessEnabled()).thenReturn(true);
    when(security.getPermissions()).thenReturn(Collections.singletonList(permission));
    when(permission.getRole()).thenReturn("ROLE_BILLER");
    when(permission.getAllowedStatuses()).thenReturn(Collections.singletonList(statusPermission));
    when(statusPermission.getStatus()).thenReturn("DRAFT");
    when(statusPermission.getMethods()).thenReturn(Collections.singletonList("POST"));

    // Mock security context
    GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_BILLER");
    doReturn(Collections.singleton(authority)).when(authentication).getAuthorities();
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(exchange.getRequest()).thenReturn(request);
    when(request.getMethod()).thenReturn(HttpMethod.GET);

    // When & Then
    StepVerifier.create(
            brdSecurityService
                .withSecurityCheck("DRAFT")
                .contextWrite(
                    ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)))
                .contextWrite(Context.of(ServerWebExchange.class, exchange)))
        .expectErrorMatches(
            throwable ->
                throwable instanceof AccessDeniedException
                    && throwable
                        .getMessage()
                        .equals(
                            "This BRD is currently in DRAFT status and not yet open for collaboration."))
        .verify();
  }

  @Test
  void withSecurityCheck_WithInvalidStatus_ShouldDenyAccess() {
    // Given
    when(brdSecurityConfig.getSecurity()).thenReturn(security);
    when(security.isEnabled()).thenReturn(true);
    when(security.isRoleBasedAccessEnabled()).thenReturn(true);
    when(security.getPermissions()).thenReturn(Collections.emptyList());

    // Mock security context
    GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_BILLER");
    doReturn(Collections.singleton(authority)).when(authentication).getAuthorities();
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(exchange.getRequest()).thenReturn(request);
    when(request.getMethod()).thenReturn(HttpMethod.GET);

    // When & Then
    StepVerifier.create(
            brdSecurityService
                .withSecurityCheck("DRAFT")
                .contextWrite(
                    ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)))
                .contextWrite(Context.of(ServerWebExchange.class, exchange)))
        .expectErrorMatches(
            throwable ->
                throwable instanceof AccessDeniedException
                    && throwable
                        .getMessage()
                        .equals(
                            "This BRD is currently in DRAFT status and not yet open for collaboration."))
        .verify();
  }

  @Test
  void withSecurityCheck_WithRoleBasedAccessDisabled_ShouldAllowAccess() {
    // Given
    when(brdSecurityConfig.getSecurity()).thenReturn(security);
    when(security.isEnabled()).thenReturn(true);
    when(security.isRoleBasedAccessEnabled()).thenReturn(false);

    // When & Then
    StepVerifier.create(brdSecurityService.withSecurityCheck("DRAFT")).verifyComplete();
  }

  @Test
  void withSecurityCheck_WithMultiplePermissions_ShouldAllowAccess() {
    // Given
    BRDSecurityConfig.Permission permission1 = mock(BRDSecurityConfig.Permission.class);
    StatusPermission statusPermission1 = mock(StatusPermission.class);

    when(brdSecurityConfig.getSecurity()).thenReturn(security);
    when(security.isEnabled()).thenReturn(true);
    when(security.isRoleBasedAccessEnabled()).thenReturn(true);
    when(security.getPermissions()).thenReturn(Collections.singletonList(permission1));

    // Configure permission
    when(permission1.getRole()).thenReturn("ROLE_BILLER");
    when(permission1.getAllowedStatuses()).thenReturn(Collections.singletonList(statusPermission1));
    when(statusPermission1.getStatus()).thenReturn("DRAFT");
    when(statusPermission1.getMethods()).thenReturn(Collections.singletonList("GET"));

    // Mock security context
    GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_BILLER");
    doReturn(Collections.singleton(authority)).when(authentication).getAuthorities();
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(exchange.getRequest()).thenReturn(request);
    when(request.getMethod()).thenReturn(HttpMethod.GET);

    // When & Then
    StepVerifier.create(
            brdSecurityService
                .withSecurityCheck("DRAFT")
                .contextWrite(
                    ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)))
                .contextWrite(Context.of(ServerWebExchange.class, exchange)))
        .verifyComplete();
  }

  @Test
  void withSecurityCheck_WithMultipleMethods_ShouldAllowAccess() {
    // Given
    when(brdSecurityConfig.getSecurity()).thenReturn(security);
    when(security.isEnabled()).thenReturn(true);
    when(security.isRoleBasedAccessEnabled()).thenReturn(true);
    when(security.getPermissions()).thenReturn(Collections.singletonList(permission));
    when(permission.getRole()).thenReturn("ROLE_BILLER");
    when(permission.getAllowedStatuses()).thenReturn(Collections.singletonList(statusPermission));
    when(statusPermission.getStatus()).thenReturn("DRAFT");
    when(statusPermission.getMethods()).thenReturn(Arrays.asList("GET", "POST", "PUT"));

    // Mock security context
    GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_BILLER");
    doReturn(Collections.singleton(authority)).when(authentication).getAuthorities();
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(exchange.getRequest()).thenReturn(request);
    when(request.getMethod()).thenReturn(HttpMethod.POST);

    // When & Then
    StepVerifier.create(
            brdSecurityService
                .withSecurityCheck("DRAFT")
                .contextWrite(
                    ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)))
                .contextWrite(Context.of(ServerWebExchange.class, exchange)))
        .verifyComplete();
  }

  @Test
  @DisplayName("Manager role should be able to modify any BRD")
  void canModifyBrd_WithManagerRole_ShouldReturnTrue() {
    // Arrange
    Authentication auth = mock(Authentication.class);
    when(auth.getAuthorities())
        .thenAnswer(
            invocation -> {
              Collection<GrantedAuthority> authorities = new ArrayList<>();
              authorities.add(new SimpleGrantedAuthority(SecurityConstants.MANAGER_ROLE));
              return authorities;
            });
    SecurityContext securityContextMocked = mock(SecurityContext.class);
    when(securityContextMocked.getAuthentication()).thenReturn(auth);

    // Act & Assert
    StepVerifier.create(
            brdSecurityService
                .canModifyBrd("anyCreator")
                .contextWrite(
                    ReactiveSecurityContextHolder.withSecurityContext(
                        Mono.just(securityContextMocked))))
        .expectNext(true)
        .verifyComplete();
  }

  @Test
  @DisplayName("PM role should be able to modify their own BRD")
  void canModifyBrd_WithPmRoleAndOwner_ShouldReturnTrue() {
    // Arrange
    String username = "testPM";
    Authentication auth = mock(Authentication.class);
    when(auth.getName()).thenReturn(username);
    when(auth.getAuthorities())
        .thenAnswer(
            invocation -> {
              Collection<GrantedAuthority> authorities = new ArrayList<>();
              authorities.add(new SimpleGrantedAuthority(SecurityConstants.PM_ROLE));
              return authorities;
            });

    SecurityContext securityContextMocked = mock(SecurityContext.class);
    when(securityContextMocked.getAuthentication()).thenReturn(auth);

    // Act & Assert
    StepVerifier.create(
            brdSecurityService
                .canModifyBrd(username)
                .contextWrite(
                    ReactiveSecurityContextHolder.withSecurityContext(
                        Mono.just(securityContextMocked))))
        .expectNext(true)
        .verifyComplete();
  }

  @Test
  @DisplayName("PM role should not be able to modify other's BRD")
  void canModifyBrd_WithPmRoleNotOwner_ShouldReturnFalse() {
    // Arrange
    String username = "testPM";
    String otherCreator = "otherPM";
    Authentication auth = mock(Authentication.class);
    when(auth.getName()).thenReturn(username);
    when(auth.getAuthorities())
        .thenAnswer(
            invocation -> {
              Collection<GrantedAuthority> authorities = new ArrayList<>();
              authorities.add(new SimpleGrantedAuthority(SecurityConstants.PM_ROLE));
              return authorities;
            });

    SecurityContext securityContextMocked = mock(SecurityContext.class);
    when(securityContextMocked.getAuthentication()).thenReturn(auth);

    // Act & Assert
    StepVerifier.create(
            brdSecurityService
                .canModifyBrd(otherCreator)
                .contextWrite(
                    ReactiveSecurityContextHolder.withSecurityContext(
                        Mono.just(securityContextMocked))))
        .expectError(AccessDeniedException.class)
        .verify();
  }

  @Test
  @DisplayName("Other roles should not be able to modify any BRD")
  void canModifyBrd_WithOtherRole_ShouldReturnFalse() {
    // Arrange
    Authentication auth = mock(Authentication.class);
    when(auth.getAuthorities())
        .thenAnswer(
            invocation -> {
              Collection<GrantedAuthority> authorities = new ArrayList<>();
              authorities.add(new SimpleGrantedAuthority(SecurityConstants.ROLE_BA));
              return authorities;
            });

    SecurityContext securityContextMocked = mock(SecurityContext.class);
    when(securityContextMocked.getAuthentication()).thenReturn(auth);

    // Act & Assert
    StepVerifier.create(
            brdSecurityService
                .canModifyBrd("anyCreator")
                .contextWrite(
                    ReactiveSecurityContextHolder.withSecurityContext(
                        Mono.just(securityContextMocked))))
        .expectNext(false)
        .verifyComplete();
  }

  @Test
  @DisplayName("No security context should not be able to modify any BRD")
  void canModifyBrd_WithNoSecurityContext_ShouldReturnFalse() {
    // Act & Assert
    StepVerifier.create(brdSecurityService.canModifyBrd("anyCreator"))
        .expectNext(false)
        .verifyComplete();
  }
}

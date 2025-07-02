package com.aci.smart_onboarding.security.service;

import com.aci.smart_onboarding.constants.SecurityConstants;
import com.aci.smart_onboarding.security.config.BRDSecurityConfig;
import com.aci.smart_onboarding.security.config.BRDSecurityConfig.StatusPermission;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class BRDSecurityService {
  private static final Logger log = LoggerFactory.getLogger(BRDSecurityService.class);

  private final BRDSecurityConfig brdSecurityConfig;

  public Mono<Void> withSecurityCheck(String status) {
    if (status == null) {
      log.debug("Access denied: Status is null");
      return Mono.error(new AccessDeniedException("BRD status not found"));
    }
    return checkAccess(status);
  }

  private Mono<Void> checkAccess(String status) {
    log.debug("Checking access for status: {}", status);
    log.debug(
        "Security config - enabled: {}, roleBasedAccess: {}",
        brdSecurityConfig.getSecurity().isEnabled(),
        brdSecurityConfig.getSecurity().isRoleBasedAccessEnabled());

    return getCurrentUserRole()
        .doOnNext(role -> log.debug("Current user role: {}", role))
        .flatMap(
            role ->
                hasPermission(role, status)
                    .doOnNext(
                        hasPermission ->
                            log.debug(
                                "Permission check result - role: {}, status: {}, result: {}",
                                role,
                                status,
                                hasPermission))
                    .flatMap(
                        hasPermission -> {
                          if (Boolean.FALSE.equals(hasPermission)) {
                            return Mono.error(
                                new AccessDeniedException(
                                    String.format(
                                        "This BRD is currently in %s status and not yet open for collaboration.",
                                        status)));
                          }
                          return Mono.empty();
                        }));
  }

  private Mono<Boolean> hasPermission(String role, String status) {
    if (role == null || status == null) {
      log.warn("Role or status is null, denying access");
      return Mono.just(false);
    }

    log.debug("Checking permission - role: {}, status: {}", role, status);

    return Mono.deferContextual(
        contextView -> {
          ServerWebExchange exchange = contextView.get(ServerWebExchange.class);
          String method = exchange.getRequest().getMethod().name();
          log.debug("Current HTTP method: {}", method);

          return Mono.just(
              brdSecurityConfig.getSecurity().getPermissions().stream()
                  .filter(
                      permission -> {
                        String configRole = permission.getRole();
                        String userRole =
                            role.startsWith(SecurityConstants.ROLE_PREFIX)
                                ? role
                                : SecurityConstants.ROLE_PREFIX + role;
                        String normalizedConfigRole =
                            configRole.startsWith(SecurityConstants.ROLE_PREFIX)
                                ? configRole
                                : SecurityConstants.ROLE_PREFIX + configRole;
                        boolean roleMatch = userRole.equals(normalizedConfigRole);
                        log.debug(
                            "Role comparison - userRole: {}, configRole: {}, normalizedConfigRole: {}, match: {}",
                            userRole,
                            configRole,
                            normalizedConfigRole,
                            roleMatch);
                        return roleMatch;
                      })
                  .flatMap(permission -> permission.getAllowedStatuses().stream())
                  .filter(statusPermission -> statusPermission.getStatus().equalsIgnoreCase(status))
                  .anyMatch(
                      statusPermission -> {
                        boolean methodAllowed =
                            statusPermission.getMethods().stream()
                                .anyMatch(m -> m.equalsIgnoreCase(method));
                        log.debug("Method check - current: {}, allowed: {}", method, methodAllowed);
                        return methodAllowed;
                      }));
        });
  }

  public Mono<String> getCurrentUserRole() {
    return ReactiveSecurityContextHolder.getContext()
        .doOnNext(context -> log.debug("Security context: {}", context))
        .map(SecurityContext::getAuthentication)
        .doOnNext(auth -> log.debug("Authentication: {}", auth))
        .map(Authentication::getAuthorities)
        .doOnNext(authorities -> log.debug("Authorities: {}", authorities))
        .flatMap(
            authorities -> {
              if (authorities == null || !authorities.iterator().hasNext()) {
                log.warn("No authorities found in authentication");
                return Mono.error(
                    new NullPointerException("No authorities found in authentication"));
              }
              String role = authorities.iterator().next().getAuthority();
              log.debug("Current user role: {}", role);
              return Mono.just(role);
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.warn("No security context found");
                  return Mono.empty();
                }));
  }

  public Mono<String> getCurrentUserEmail() {
    return ReactiveSecurityContextHolder.getContext()
        .doOnNext(context -> log.debug("Security context for email: {}", context))
        .map(SecurityContext::getAuthentication)
        .doOnNext(auth -> log.debug("Authentication for email: {}", auth))
        .map(Authentication::getName)
        .doOnNext(email -> log.debug("Current user email: {}", email))
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.warn("No security context found for email");
                  return Mono.empty();
                }));
  }

  public boolean isStatusAllowedForRole(String role, String status, HttpMethod method) {
    if (!brdSecurityConfig.getSecurity().isEnabled()
        || !brdSecurityConfig.getSecurity().isRoleBasedAccessEnabled()) {
      return true;
    }

    return brdSecurityConfig.getSecurity().getPermissions().stream()
        .filter(
            permission -> {
              String configRole = permission.getRole();
              String userRole =
                  role.startsWith(SecurityConstants.ROLE_PREFIX)
                      ? role
                      : SecurityConstants.ROLE_PREFIX + role;
              String normalizedConfigRole =
                  configRole.startsWith(SecurityConstants.ROLE_PREFIX)
                      ? configRole
                      : SecurityConstants.ROLE_PREFIX + configRole;
              return userRole.equals(normalizedConfigRole);
            })
        .findFirst()
        .map(
            permission ->
                permission.getAllowedStatuses().stream()
                    .filter(
                        statusPermission -> statusPermission.getStatus().equalsIgnoreCase(status))
                    .findFirst()
                    .map(
                        statusPermission ->
                            statusPermission.getMethods().stream()
                                .anyMatch(m -> m.equalsIgnoreCase(method.name())))
                    .orElse(false))
        .orElse(false);
  }

  public List<String> getAllowedStatusesForRole(String role) {
    if (!brdSecurityConfig.getSecurity().isEnabled()
        || !brdSecurityConfig.getSecurity().isRoleBasedAccessEnabled()) {
      return List.of();
    }

    return brdSecurityConfig.getSecurity().getPermissions().stream()
        .filter(permission -> permission.getRole().equals(role))
        .findFirst()
        .map(
            permission ->
                permission.getAllowedStatuses().stream().map(StatusPermission::getStatus).toList())
        .orElse(List.of());
  }

  public Mono<Boolean> canModifyBrd(String creator) {
    return getCurrentUserRole()
        .flatMap(
            role -> {
              // Admins and managers can modify all BRDs
              if (SecurityConstants.MANAGER_ROLE.equals(role)) {
                return Mono.just(true);
              }

              // Get current username to check ownership
              return ReactiveSecurityContextHolder.getContext()
                  .map(SecurityContext::getAuthentication)
                  .map(Authentication::getName)
                  .map(
                      username -> {
                        // PMs can only modify their own BRDs
                        if (SecurityConstants.PM_ROLE.equals(role)) {
                          boolean isOwner = username.equals(creator);
                          if (!isOwner) {
                            throw new AccessDeniedException(
                                String.format(
                                    "Access denied. This BRD belongs to PM '%s'. Only the owning PM or a Manager can modify this BRD.",
                                    creator));
                          }
                          return true;
                        }
                        // Other roles cannot modify BRDs
                        return false;
                      });
            })
        .onErrorResume(
            e -> {
              if (e instanceof AccessDeniedException) {
                return Mono.error(e);
              }
              log.warn("Error checking BRD modification permissions: {}", e.getMessage());
              return Mono.just(false);
            })
        .defaultIfEmpty(false);
  }
}

package com.aci.smart_onboarding.util;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

/** Utility class for web-related operations. */
public final class WebUtils {

  private WebUtils() {
    // Private constructor to prevent instantiation
  }

  /**
   * Safely retrieves the client IP address from the ServerWebExchange. Returns "unknown" if the IP
   * address cannot be determined.
   *
   * @param exchange the ServerWebExchange
   * @return the client IP address or "unknown"
   */
  public static String getClientIpAddress(ServerWebExchange exchange) {
    return Optional.ofNullable(exchange)
        .map(ServerWebExchange::getRequest)
        .map(ServerHttpRequest::getRemoteAddress)
        .map(InetSocketAddress::getAddress)
        .map(InetAddress::getHostAddress)
        .orElse("unknown");
  }
}

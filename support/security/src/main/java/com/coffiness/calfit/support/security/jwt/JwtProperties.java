package com.coffiness.calfit.support.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
    String secret, long accessTokenExpiration, long refreshTokenExpiration) {
  public JwtProperties {
    if (secret == null || secret.isBlank()) {
      secret = "default-secret-key-for-development-only-please-change-in-production";
    }
    if (accessTokenExpiration <= 0) {
      accessTokenExpiration = 3600000L; // 1 hour
    }
    if (refreshTokenExpiration <= 0) {
      refreshTokenExpiration = 604800000L; // 7 days
    }
  }
}

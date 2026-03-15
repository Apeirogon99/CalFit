package com.coffiness.calfit.api;

import com.coffiness.calfit.dto.GoogleOAuthResponseDto;
import com.coffiness.calfit.model.OAuthExchangeResult;
import com.coffiness.calfit.model.OAuthRefreshResult;
import com.coffiness.calfit.port.GoogleOAuthPort;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Component
@RequiredArgsConstructor
public class GoogleOAuthClient implements GoogleOAuthPort {

  private final GoogleOAuthApi googleOAuthApi;

  @Value("${spring.security.oauth2.client.registration.google.client-id}")
  private String clientId;

  @Value("${spring.security.oauth2.client.registration.google.client-secret}")
  private String clientSecret;

  @Override
  public OAuthExchangeResult exchangeAuthorizationCode(String authCode, String redirectUri) {
    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("code", authCode);
    formData.add("client_id", clientId);
    formData.add("client_secret", clientSecret);
    formData.add("redirect_uri", redirectUri);
    formData.add("grant_type", "authorization_code");

    GoogleOAuthResponseDto rawDto = callTokenEndpoint(formData);

    return new OAuthExchangeResult(
        rawDto.accessToken(),
        rawDto.expiresIn() != null ? rawDto.expiresIn().longValue() : null,
        rawDto.refreshToken(),
        rawDto.idToken());
  }

  @Override
  public OAuthRefreshResult refreshAccessToken(String refreshToken) {
    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("refresh_token", refreshToken);
    formData.add("client_id", clientId);
    formData.add("client_secret", clientSecret);
    formData.add("grant_type", "refresh_token");

    GoogleOAuthResponseDto rawDto = callTokenEndpoint(formData);

    return new OAuthRefreshResult(
        rawDto.accessToken(),
        rawDto.expiresIn() != null ? rawDto.expiresIn().longValue() : null,
        rawDto.refreshToken());
  }

  private GoogleOAuthResponseDto callTokenEndpoint(MultiValueMap<String, String> formData) {
    try {
      return googleOAuthApi.exchange(formData);
    } catch (FeignException e) {
      String body = e.contentUTF8();

      if (e.status() == 400 || e.status() == 401) {
        if (containsIgnoreCase(body, "Could not determine client ID")) {
          throw new IllegalStateException("GOOGLE_CLIENT_CONFIG_MISSING", e);
        }

        if (containsIgnoreCase(body, "invalid_grant")
            || containsIgnoreCase(body, "Malformed auth code")) {
          throw new IllegalStateException("GOOGLE_REAUTH_REQUIRED", e);
        }

        throw new IllegalStateException("GOOGLE_REAUTH_REQUIRED", e);
      }

      if (e.status() >= 500) {
        throw new IllegalStateException("GOOGLE_TEMPORARY_ERROR", e);
      }

      throw new IllegalStateException("GOOGLE_OAUTH_ERROR", e);
    }
  }

  private boolean containsIgnoreCase(String text, String token) {
    if (text == null || token == null) {
      return false;
    }

    return text.toLowerCase().contains(token.toLowerCase());
  }
}

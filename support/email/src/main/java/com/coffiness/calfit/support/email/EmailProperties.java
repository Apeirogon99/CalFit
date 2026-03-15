package com.coffiness.calfit.support.email;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.email")
public class EmailProperties {

  private boolean enabled = true;
  private String sender = "no-reply@calfit.com";
  private String apiBaseUrl = "http://localhost:8080";
  private String frontendBaseUrl = "http://localhost:5173";
  private String workspaceCreatePath = "/workspace/create";
  private String loginPath = "/login";

  public String getSignupVerificationUrl() {
    return join(apiBaseUrl, "/api/v1/users/signup/verify");
  }

  public String getWorkspaceCreateUrl() {
    return join(frontendBaseUrl, workspaceCreatePath);
  }

  public String getLoginUrl() {
    return join(frontendBaseUrl, loginPath);
  }

  public String getInvitationViewUrl(String token) {
    return join(frontendBaseUrl, "/invitations/" + token);
  }

  private String join(String baseUrl, String path) {
    String normalizedBaseUrl = baseUrl == null ? "" : baseUrl.trim();
    String normalizedPath = path.startsWith("/") ? path : "/" + path;
    if (normalizedBaseUrl.endsWith("/")) {
      normalizedBaseUrl = normalizedBaseUrl.substring(0, normalizedBaseUrl.length() - 1);
    }
    return normalizedBaseUrl + normalizedPath;
  }
}

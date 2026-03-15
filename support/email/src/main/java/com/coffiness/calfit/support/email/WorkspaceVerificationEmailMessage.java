package com.coffiness.calfit.support.email;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WorkspaceVerificationEmailMessage {
  private final String userEmail;
  private final String userName;
  private final String verificationToken;
  private final String verificationUrl;
  private final LocalDateTime expiresAt;
}

package com.coffiness.calfit.support.email;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InvitationEmailMessage {
  private final String invitedEmail;
  private final String workspaceName;
  private final String inviterName;
  private final String memberTypeLabel;
  private final String subject;
  private final String invitationUrl;
  private final LocalDateTime expiresAt;
}

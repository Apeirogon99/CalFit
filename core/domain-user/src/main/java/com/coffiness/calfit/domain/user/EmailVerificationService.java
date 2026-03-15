package com.coffiness.calfit.domain.user;

import com.coffiness.calfit.storage.db.core.email.EmailVerificationTokenEntity;
import com.coffiness.calfit.storage.db.core.email.EmailVerificationTokenRepository;
import com.coffiness.calfit.support.email.EmailProperties;
import com.coffiness.calfit.support.email.EmailService;
import com.coffiness.calfit.support.email.WorkspaceVerificationEmailMessage;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

  private static final long EXPIRE_MINUTES = 30L;

  private final EmailVerificationTokenRepository tokenRepository;
  private final EmailService emailService;
  private final EmailProperties emailProperties;

  @Transactional
  public void generateTokenAndSendEmail(String userEmail, String userName, String verificationUrl) {
    tokenRepository.findByUserEmail(userEmail).ifPresent(tokenRepository::delete);

    String tokenValue = UUID.randomUUID().toString();
    LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(EXPIRE_MINUTES);

    EmailVerificationTokenEntity tokenEntity =
        EmailVerificationTokenEntity.builder()
            .userEmail(userEmail)
            .token(tokenValue)
            .expiresAt(expiresAt)
            .build();

    tokenRepository.save(tokenEntity);

    WorkspaceVerificationEmailMessage message =
        WorkspaceVerificationEmailMessage.builder()
            .userEmail(userEmail)
            .userName(userName)
            .verificationToken(tokenValue)
            .verificationUrl(verificationUrl)
            .expiresAt(expiresAt)
            .build();

    emailService.sendWorkspaceVerificationEmail(emailProperties.getSender(), message);
  }

  @Transactional
  public boolean verifyEmailToken(String token) {
    Optional<EmailVerificationTokenEntity> optionalToken = tokenRepository.findByToken(token);

    if (optionalToken.isEmpty()) {
      log.warn("Email verification token not found. token={}", token);
      return false;
    }

    EmailVerificationTokenEntity tokenEntity = optionalToken.get();
    if (tokenEntity.isVerified()) {
      log.info("Email already verified. userEmail={}", tokenEntity.getUserEmail());
      return true;
    }

    if (tokenEntity.isExpired(LocalDateTime.now())) {
      log.warn("Email verification token expired. userEmail={}", tokenEntity.getUserEmail());
      return false;
    }

    tokenEntity.verify();
    log.info("Email verification completed. userEmail={}", tokenEntity.getUserEmail());
    return true;
  }
}

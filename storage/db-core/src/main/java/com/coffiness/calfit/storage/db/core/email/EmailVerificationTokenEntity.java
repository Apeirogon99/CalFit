package com.coffiness.calfit.storage.db.core.email;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "email_verification_tokens")
public class EmailVerificationTokenEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String userEmail;

  @Column(nullable = false, unique = true)
  private String token;

  @Column(nullable = false)
  private LocalDateTime expiresAt;

  @Column(nullable = false)
  private boolean verified;

  @Builder
  public EmailVerificationTokenEntity(String userEmail, String token, LocalDateTime expiresAt) {
    this.userEmail = userEmail;
    this.token = token;
    this.expiresAt = expiresAt;
    this.verified = false;
  }

  public void verify() {
    this.verified = true;
  }

  public boolean isExpired(LocalDateTime now) {
    return this.expiresAt.isBefore(now);
  }
}

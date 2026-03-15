package com.coffiness.calfit.storage.db.core.invitation;

import com.coffiness.calfit.core.enums.InvitationStatus;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.storage.db.core.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "workspace_invitations")
public class WorkspaceInvitationEntity extends BaseEntity {

  @Column(nullable = false)
  private String workspaceId;

  @Column(nullable = false)
  private String email;

  @Column(nullable = false, unique = true)
  private String token;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MemberType memberType;

  @Column(nullable = false)
  private Long invitedBy;

  @Enumerated(EnumType.STRING)
  @Column(name = "invitation_status", nullable = false)
  private InvitationStatus invitationStatus;

  @Column(nullable = false)
  private LocalDateTime expiresAt;

  protected WorkspaceInvitationEntity() {}

  private WorkspaceInvitationEntity(
      String workspaceId,
      String email,
      String token,
      MemberType memberType,
      Long invitedBy,
      LocalDateTime expiresAt) {
    this.workspaceId = workspaceId;
    this.email = email;
    this.token = token;
    this.memberType = memberType;
    this.invitedBy = invitedBy;
    this.invitationStatus = InvitationStatus.PENDING;
    this.expiresAt = expiresAt;
  }

  public static WorkspaceInvitationEntity create(
      String workspaceId,
      String email,
      String token,
      MemberType memberType,
      Long invitedBy,
      LocalDateTime expiresAt) {
    return new WorkspaceInvitationEntity(
        workspaceId, email, token, memberType, invitedBy, expiresAt);
  }

  public void accept() {
    this.invitationStatus = InvitationStatus.ACCEPTED;
  }

  public boolean isExpired() {
    return expiresAt.isBefore(LocalDateTime.now());
  }

  public String getWorkspaceId() {
    return workspaceId;
  }

  public String getEmail() {
    return email;
  }

  public String getToken() {
    return token;
  }

  public MemberType getMemberType() {
    return memberType;
  }

  public Long getInvitedBy() {
    return invitedBy;
  }

  public InvitationStatus getInvitationStatus() {
    return invitationStatus;
  }

  public LocalDateTime getExpiresAt() {
    return expiresAt;
  }
}

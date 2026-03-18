package com.coffiness.calfit.domain.workspace.invitation;

import com.coffiness.calfit.core.enums.InvitationStatus;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.domain.workspace.member.MemberReader;
import com.coffiness.calfit.storage.db.core.invitation.WorkspaceInvitationEntity;
import com.coffiness.calfit.storage.db.core.invitation.WorkspaceInvitationRepository;
import com.coffiness.calfit.storage.db.core.user.UserEntity;
import com.coffiness.calfit.storage.db.core.user.UserRepository;
import com.coffiness.calfit.storage.db.core.workspace.WorkspaceRepository;
import com.coffiness.calfit.support.email.EmailProperties;
import com.coffiness.calfit.support.email.EmailService;
import com.coffiness.calfit.support.email.InvitationEmailMessage;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvitationService {
  private static final long EXPIRATION_HOURS = 72;

  private static final String SUBJECT_SUFFIX = "워크스페이스 초대";
  private static final String MEMBER_TYPE_HR = "인사담당자";
  private static final String MEMBER_TYPE_INTERVIEWER = "면접관";

  private final WorkspaceInvitationRepository invitationRepository;
  private final WorkspaceRepository workspaceRepository;
  private final UserRepository userRepository;
  private final MemberReader memberReader;
  private final EmailService emailService;
  private final EmailProperties emailProperties;

  @Transactional
  public Invitation createInvitation(
      String workspaceId, String email, MemberType memberType, Long invitedBy) {
    if (invitationRepository.existsByWorkspaceIdAndEmailAndInvitationStatus(
        workspaceId, email, InvitationStatus.PENDING)) {
      throw new CoreException(ErrorType.VALIDATION_ERROR, Map.of("message", "이미 대기 중인 초대가 있습니다."));
    }

    // 이미 워크스페이스 멤버인지 확인
    Optional<UserEntity> existingUser = userRepository.findByEmail(email);
    if (existingUser.isPresent() && memberReader.exists(workspaceId, existingUser.get().getId())) {
      throw new CoreException(
          ErrorType.VALIDATION_ERROR, Map.of("message", "이미 워크스페이스에 가입된 멤버입니다."));
    }

    String token = UUID.randomUUID().toString();
    LocalDateTime expiresAt = LocalDateTime.now().plusHours(EXPIRATION_HOURS);

    WorkspaceInvitationEntity entity =
        WorkspaceInvitationEntity.create(
            workspaceId, email, token, memberType, invitedBy, expiresAt);
    WorkspaceInvitationEntity saved = invitationRepository.save(entity);

    sendInvitationEmail(saved);
    return toInvitation(saved);
  }

  @Transactional(readOnly = true)
  public Invitation getByToken(String token) {
    WorkspaceInvitationEntity entity =
        invitationRepository
            .findByToken(token)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

    if (entity.isExpired() || entity.getInvitationStatus() != InvitationStatus.PENDING) {
      throw new CoreException(ErrorType.NOT_FOUND);
    }

    return toInvitation(entity);
  }

  @Transactional
  public void acceptInvitation(String token) {
    WorkspaceInvitationEntity entity =
        invitationRepository
            .findByToken(token)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

    entity.accept();
  }

  private void sendInvitationEmail(WorkspaceInvitationEntity invitation) {
    String workspaceName =
        workspaceRepository
            .findByWorkspaceId(invitation.getWorkspaceId())
            .map(workspace -> workspace.getName())
            .orElse("CalFit Workspace");
    String inviterName =
        userRepository
            .findById(invitation.getInvitedBy())
            .map(user -> user.getName())
            .orElse("CalFit");

    InvitationEmailMessage message =
        InvitationEmailMessage.builder()
            .invitedEmail(invitation.getEmail())
            .workspaceName(workspaceName)
            .inviterName(inviterName)
            .memberTypeLabel(toMemberTypeLabel(invitation.getMemberType()))
            .subject(String.format("[CalFit] %s %s", workspaceName, SUBJECT_SUFFIX))
            .invitationUrl(emailProperties.getInvitationViewUrl(invitation.getToken()))
            .expiresAt(invitation.getExpiresAt())
            .build();

    emailService.sendInvitationEmail(emailProperties.getSender(), message);
  }

  private String toMemberTypeLabel(MemberType memberType) {
    return switch (memberType) {
      case HR -> MEMBER_TYPE_HR;
      case INTERVIEWER -> MEMBER_TYPE_INTERVIEWER;
    };
  }

  private Invitation toInvitation(WorkspaceInvitationEntity entity) {
    return new Invitation(
        entity.getId(),
        entity.getWorkspaceId(),
        entity.getEmail(),
        entity.getToken(),
        entity.getMemberType(),
        entity.getInvitedBy(),
        entity.getInvitationStatus(),
        entity.getExpiresAt());
  }
}

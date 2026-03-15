package com.coffiness.calfit.core.api.facade.workspace;

import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.domain.user.User;
import com.coffiness.calfit.domain.user.UserService;
import com.coffiness.calfit.domain.workspace.invitation.Invitation;
import com.coffiness.calfit.domain.workspace.invitation.InvitationService;
import com.coffiness.calfit.domain.workspace.member.MemberService;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class InvitationFacade {
  private final InvitationService invitationService;
  private final UserService userService;
  private final MemberService memberService;

  /** 초대를 검증하고 workspaceId를 반환한다. TenantContext 설정 전에 호출. */
  public Invitation validateInvitation(String token, Long userId) {
    Invitation invitation = invitationService.getByToken(token);

    User user = userService.getUser(userId);
    if (!user.email().equalsIgnoreCase(invitation.email())) {
      throw new CoreException(
          ErrorType.VALIDATION_ERROR,
          "INVITATION_EMAIL_MISMATCH",
          "초대받은 이메일과 현재 로그인한 계정의 이메일이 일치하지 않습니다.");
    }

    return invitation;
  }

  /** TenantContext가 올바르게 설정된 상태에서 호출해야 한다. */
  @Transactional
  public void acceptInvitation(String token, Long userId, MemberType memberType) {
    invitationService.acceptInvitation(token);
    memberService.registerMember(userId, memberType);
  }
}

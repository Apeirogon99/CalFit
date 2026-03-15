package com.coffiness.calfit.api.group.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.v1.response.InvitationResponse;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.support.email.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@CalfitApiTest
@DisplayName("POST /api/v1/workspaces/{workspaceId}/invitations")
public class POST_specs {

  @MockBean private EmailService emailService;

  @Test
  void 올바른_이메일이면_초대를_생성하고_초대메일_발송을_요청한다(@Autowired MemberFixture fixture) {
    MemberFixture.WorkspaceContext context = fixture.setupWorkspace();
    String inviteeEmail = fixture.userFixture().randomEmail();

    ApiResponse<InvitationResponse> response =
        fixture.createInvitation(
            context.hrToken(), context.workspaceId(), inviteeEmail, MemberType.INTERVIEWER);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().email()).isEqualTo(inviteeEmail);
    verify(emailService).sendInvitationEmail(anyString(), any());
  }
}

package com.coffiness.calfit.api.fixture;

import com.coffiness.calfit.api.v1.request.AssignGroupRequest;
import com.coffiness.calfit.api.v1.request.CreateGroupRequest;
import com.coffiness.calfit.api.v1.request.CreateInvitationRequest;
import com.coffiness.calfit.api.v1.response.GroupResponse;
import com.coffiness.calfit.api.v1.response.InvitationResponse;
import com.coffiness.calfit.api.v1.response.MemberResponse;
import com.coffiness.calfit.api.v1.response.WorkspaceResponse;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.env.Environment;
import org.springframework.http.*;

public record MemberFixture(
    BaseFixture base, UserFixture userFixture, WorkspaceFixture workspaceFixture) {

  public static MemberFixture create(Environment environment, ObjectMapper objectMapper) {
    BaseFixture base = BaseFixture.create(environment, objectMapper);
    UserFixture userFixture = UserFixture.create(environment, objectMapper);
    WorkspaceFixture workspaceFixture = WorkspaceFixture.create(environment, objectMapper);
    return new MemberFixture(base, userFixture, workspaceFixture);
  }

  // ==================== Member API Calls ====================

  public ApiResponse<MemberResponse[]> getMembers(String token, String tenantId) {
    return exchangeWithTenant(
        "/api/v1/members", HttpMethod.GET, null, token, tenantId, MemberResponse[].class);
  }

  public ApiResponse<MemberResponse> getMyMember(String token, String tenantId) {
    return exchangeWithTenant(
        "/api/v1/members/me", HttpMethod.GET, null, token, tenantId, MemberResponse.class);
  }

  public ApiResponse<Void> updateMember(
      Long memberId, Object request, String token, String tenantId) {
    return exchangeWithTenant(
        "/api/v1/members/" + memberId, HttpMethod.PATCH, request, token, tenantId, Void.class);
  }

  public ApiResponse<Void> removeMember(Long memberId, String token, String tenantId) {
    return exchangeWithTenant(
        "/api/v1/members/" + memberId, HttpMethod.DELETE, null, token, tenantId, Void.class);
  }

  public ApiResponse<Void> assignGroup(Long memberId, Long groupId, String token, String tenantId) {
    return exchangeWithTenant(
        "/api/v1/members/" + memberId + "/group",
        HttpMethod.PATCH,
        new AssignGroupRequest(groupId),
        token,
        tenantId,
        Void.class);
  }

  public ApiResponse<GroupResponse> createGroup(
      String name, String color, String token, String tenantId) {
    return exchangeWithTenant(
        "/api/v1/groups",
        HttpMethod.POST,
        new CreateGroupRequest(name, color),
        token,
        tenantId,
        GroupResponse.class);
  }

  // ==================== Group API Calls ====================

  public ApiResponse<GroupResponse[]> getGroups(String token, String tenantId) {
    return exchangeWithTenant(
        "/api/v1/groups", HttpMethod.GET, null, token, tenantId, GroupResponse[].class);
  }

  public ApiResponse<GroupResponse> createGroupRaw(Object request, String token, String tenantId) {
    return exchangeWithTenant(
        "/api/v1/groups", HttpMethod.POST, request, token, tenantId, GroupResponse.class);
  }

  public ApiResponse<Void> deleteGroup(Long groupId, String token, String tenantId) {
    return exchangeWithTenant(
        "/api/v1/groups/" + groupId, HttpMethod.DELETE, null, token, tenantId, Void.class);
  }

  // ==================== Invitation API Calls ====================

  public ApiResponse<InvitationResponse> createInvitation(
      String hrToken, String workspaceId, String email, MemberType memberType) {
    CreateInvitationRequest request = new CreateInvitationRequest(email, memberType);
    return base.post(
        "/api/v1/workspaces/" + workspaceId + "/invitations",
        request,
        hrToken,
        InvitationResponse.class);
  }

  public ApiResponse<Void> acceptInvitation(String invitationToken) {
    return base.post("/api/v1/invitations/" + invitationToken + "/accept", null, Void.class);
  }

  public ApiResponse<Void> acceptInvitation(String invitationToken, String token) {
    return base.post("/api/v1/invitations/" + invitationToken + "/accept", null, token, Void.class);
  }

  // ==================== Convenience Setup ====================

  /** 유저 생성 + 워크스페이스 생성 → WorkspaceContext 반환 */
  public WorkspaceContext setupWorkspace() {
    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    return new WorkspaceContext(token, workspace.workspaceId());
  }

  /** 워크스페이스에 두 번째 멤버 추가 (초대 → 수락) 두 번째 유저의 이메일을 직접 지정해 초대하고, 수락 후 해당 멤버의 ID를 반환한다. */
  public Long addSecondMember(WorkspaceContext context) {
    String inviteeEmail = userFixture.randomEmail();
    String inviteePassword = userFixture.randomPassword();
    userFixture.signUp(inviteeEmail, inviteePassword, userFixture.randomName());

    ApiResponse<InvitationResponse> invitationResponse =
        createInvitation(
            context.hrToken(), context.workspaceId(), inviteeEmail, MemberType.INTERVIEWER);
    String invitationToken = invitationResponse.getData().token();

    String inviteeToken = userFixture.login(inviteeEmail, inviteePassword).getData().accessToken();
    acceptInvitation(invitationToken, inviteeToken);
    MemberResponse memberResponse = getMyMember(inviteeToken, context.workspaceId()).getData();
    return memberResponse.id();
  }

  public Long addSecondMemberUserId(WorkspaceContext context) {
    String inviteeEmail = userFixture.randomEmail();
    String inviteePassword = userFixture.randomPassword();
    Long userId =
        userFixture.signUp(inviteeEmail, inviteePassword, userFixture.randomName()).getData().id();

    ApiResponse<InvitationResponse> invitationResponse =
        createInvitation(
            context.hrToken(), context.workspaceId(), inviteeEmail, MemberType.INTERVIEWER);
    String inviteeToken = userFixture.login(inviteeEmail, inviteePassword).getData().accessToken();
    acceptInvitation(invitationResponse.getData().token(), inviteeToken);

    return userId;
  }

  /** 워크스페이스에 두 번째 멤버 추가 후 토큰과 멤버 ID를 함께 반환한다. 권한(403) 테스트 등에서 비-HR 멤버의 토큰이 필요할 때 사용한다. */
  public MemberContext addSecondMemberWithToken(WorkspaceContext context) {
    String inviteeEmail = userFixture.randomEmail();
    String inviteePassword = userFixture.randomPassword();
    userFixture.signUp(inviteeEmail, inviteePassword, userFixture.randomName());

    ApiResponse<InvitationResponse> invitationResponse =
        createInvitation(
            context.hrToken(), context.workspaceId(), inviteeEmail, MemberType.INTERVIEWER);
    String invitationToken = invitationResponse.getData().token();

    String inviteeToken = userFixture.login(inviteeEmail, inviteePassword).getData().accessToken();
    acceptInvitation(invitationToken, inviteeToken);
    MemberResponse memberResponse = getMyMember(inviteeToken, context.workspaceId()).getData();
    return new MemberContext(inviteeToken, memberResponse.id());
  }

  // ==================== Private Helpers ====================

  @SuppressWarnings("unchecked")
  private <T> ApiResponse<T> exchangeWithTenant(
      String url,
      HttpMethod method,
      Object body,
      String token,
      String tenantId,
      Class<T> responseType) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (token != null && !token.isBlank()) {
      headers.setBearerAuth(token);
    }
    if (tenantId != null && !tenantId.isBlank()) {
      headers.set("X-Tenant-ID", tenantId);
    }
    HttpEntity<?> entity =
        (body != null) ? new HttpEntity<>(body, headers) : new HttpEntity<>(headers);

    ResponseEntity<ApiResponse> response =
        base.client().exchange(url, method, entity, ApiResponse.class);
    return convertResponse(response.getBody(), responseType);
  }

  @SuppressWarnings("unchecked")
  private <T> ApiResponse<T> convertResponse(ApiResponse<?> response, Class<T> responseType) {
    if (response == null) return null;
    if (response.getResult() == ResultType.ERROR || response.getData() == null) {
      return (ApiResponse<T>) response;
    }
    if (responseType == Void.class) return (ApiResponse<T>) response;
    T converted = base.objectMapper().convertValue(response.getData(), responseType);
    return ApiResponse.success(converted);
  }

  // ==================== Context Records ====================

  public record WorkspaceContext(String hrToken, String workspaceId) {}

  public record MemberContext(String token, Long memberId) {}
}

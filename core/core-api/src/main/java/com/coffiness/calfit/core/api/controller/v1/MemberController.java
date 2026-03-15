package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.api.v1.request.AssignGroupRequest;
import com.coffiness.calfit.api.v1.request.UpdateMemberRequest;
import com.coffiness.calfit.api.v1.response.MemberResponse;
import com.coffiness.calfit.core.api.facade.workspace.MemberGroupFacade;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.workspace.member.MemberInfo;
import com.coffiness.calfit.domain.workspace.member.MemberService;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class MemberController {
  private final MemberService memberService;
  private final MemberGroupFacade memberGroupFacade;

  @GetMapping("/api/v1/members")
  public ApiResponse<List<MemberResponse>> getMembers() {
    List<MemberInfo> members = memberGroupFacade.getMembers();
    return ApiResponse.success(members.stream().map(MemberResponse::from).toList());
  }

  @GetMapping("/api/v1/members/me")
  public ApiResponse<MemberResponse> getMyMember(
      @AuthenticationPrincipal SecurityUser securityUser) {
    MemberInfo member = memberGroupFacade.getMember(securityUser.userId());
    return ApiResponse.success(MemberResponse.from(member));
  }

  @PatchMapping("/api/v1/members/{memberId}")
  public ApiResponse<?> updateMember(
      @PathVariable Long memberId, @RequestBody UpdateMemberRequest request) {
    memberService.updateMemberType(memberId, request.memberType());
    return ApiResponse.success();
  }

  @DeleteMapping("/api/v1/members/{memberId}")
  public ApiResponse<?> removeMember(
      @PathVariable Long memberId, @AuthenticationPrincipal SecurityUser securityUser) {
    memberService.removeMember(securityUser.userId(), memberId);
    return ApiResponse.success();
  }

  @PatchMapping("/api/v1/members/{memberId}/group")
  public ApiResponse<?> assignGroup(
      @PathVariable Long memberId, @RequestBody AssignGroupRequest request) {
    memberService.assignGroup(memberId, request.groupId());
    return ApiResponse.success();
  }

  @DeleteMapping("/api/v1/members/{memberId}/group")
  public ApiResponse<?> leaveGroup(@PathVariable Long memberId) {
    memberService.leaveGroup(memberId);
    return ApiResponse.success();
  }
}

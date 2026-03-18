package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.api.v1.request.CreateGroupRequest;
import com.coffiness.calfit.api.v1.request.UpdateGroupRequest;
import com.coffiness.calfit.api.v1.response.GroupResponse;
import com.coffiness.calfit.core.api.facade.workspace.MemberGroupFacade;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.workspace.group.GroupService;
import com.coffiness.calfit.domain.workspace.member.MemberService;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class GroupController {
  private final GroupService groupService;
  private final MemberGroupFacade memberGroupFacade;
  private final MemberService memberService;

  @GetMapping("/api/v1/groups")
  public ApiResponse<List<GroupResponse>> getGroups() {
    return ApiResponse.success(
        memberGroupFacade.getGroups().stream().map(GroupResponse::from).toList());
  }

  @GetMapping("/api/v1/groups/{groupId}")
  public ApiResponse<GroupResponse> getGroup(@PathVariable Long groupId) {
    return ApiResponse.success(GroupResponse.from(memberGroupFacade.getGroup(groupId)));
  }

  @PostMapping("/api/v1/groups")
  public ApiResponse<GroupResponse> createGroup(
      @Valid @RequestBody CreateGroupRequest request,
      @AuthenticationPrincipal SecurityUser securityUser) {
    memberService.validateHrRole(securityUser.userId());
    return ApiResponse.success(
        GroupResponse.from(groupService.createGroup(request.name(), request.color())));
  }

  @PatchMapping("/api/v1/groups/{groupId}")
  public ApiResponse<?> updateGroup(
      @PathVariable Long groupId,
      @Valid @RequestBody UpdateGroupRequest request,
      @AuthenticationPrincipal SecurityUser securityUser) {
    memberService.validateHrRole(securityUser.userId());
    groupService.updateGroup(groupId, request.name(), request.color());
    return ApiResponse.success();
  }

  @DeleteMapping("/api/v1/groups/{groupId}")
  public ApiResponse<?> deleteGroup(
      @PathVariable Long groupId, @AuthenticationPrincipal SecurityUser securityUser) {
    memberService.validateHrRole(securityUser.userId());
    memberGroupFacade.removeGroup(groupId);
    return ApiResponse.success();
  }
}

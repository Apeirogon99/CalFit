package com.coffiness.calfit.core.api.facade.workspace;

import com.coffiness.calfit.domain.user.UserInfo;
import com.coffiness.calfit.domain.user.UserReader;
import com.coffiness.calfit.domain.workspace.group.Group;
import com.coffiness.calfit.domain.workspace.group.GroupInfo;
import com.coffiness.calfit.domain.workspace.group.GroupService;
import com.coffiness.calfit.domain.workspace.member.Member;
import com.coffiness.calfit.domain.workspace.member.MemberInfo;
import com.coffiness.calfit.domain.workspace.member.MemberService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MemberGroupFacade {
  private final MemberService memberService;
  private final GroupService groupService;
  private final UserReader userReader;

  @Transactional(readOnly = true)
  public MemberInfo getMember(Long userId) {
    Member member = memberService.getMember(userId);
    if (member == null) {
      throw new IllegalArgumentException("워크스페이스 멤버를 찾을 수 없습니다.");
    }
    UserInfo user = userReader.getUser(userId);
    Map<Long, String> groupNameMap =
        member.groupId() != null
            ? groupService.getGroupNameMap(List.of(member.groupId()))
            : Map.of();
    return toMemberInfo(user, member, groupNameMap);
  }

  @Transactional(readOnly = true)
  public List<MemberInfo> getMembers() {
    List<Member> members = memberService.getMembers();

    List<Long> userIds = members.stream().map(Member::userId).toList();
    Map<Long, UserInfo> userMap =
        userReader.getUsers(userIds).stream().collect(Collectors.toMap(UserInfo::id, u -> u));

    List<Long> groupIds =
        members.stream().map(Member::groupId).filter(gid -> gid != null).distinct().toList();
    Map<Long, String> groupNameMap =
        groupIds.isEmpty() ? Map.of() : groupService.getGroupNameMap(groupIds);

    return members.stream()
        .map(member -> toMemberInfo(userMap.get(member.userId()), member, groupNameMap))
        .toList();
  }

  @Transactional(readOnly = true)
  public GroupInfo getGroup(Long groupId) {
    Group group = groupService.getGroup(groupId);
    Map<Long, Long> countMap = memberService.getGroupMemberCountMap(List.of(groupId));
    return new GroupInfo(
        group.id(), group.name(), group.color(), countMap.getOrDefault(groupId, 0L));
  }

  @Transactional(readOnly = true)
  public List<GroupInfo> getGroups() {
    List<Group> groups = groupService.getGroups();
    if (groups.isEmpty()) return List.of();

    List<Long> groupIds = groups.stream().map(Group::id).toList();
    Map<Long, Long> countMap = memberService.getGroupMemberCountMap(groupIds);

    return groups.stream()
        .map(g -> new GroupInfo(g.id(), g.name(), g.color(), countMap.getOrDefault(g.id(), 0L)))
        .toList();
  }

  @Transactional
  public void removeGroup(Long groupId) {
    memberService.clearGroupFromMembers(groupId);
    groupService.removeGroup(groupId);
  }

  private MemberInfo toMemberInfo(UserInfo user, Member member, Map<Long, String> groupNameMap) {
    String groupName = member.groupId() != null ? groupNameMap.get(member.groupId()) : null;
    return new MemberInfo(
        member.id(),
        member.userId(),
        user.name(),
        user.createdAt(),
        user.recentAt(),
        member.groupId(),
        groupName,
        member.memberType());
  }
}

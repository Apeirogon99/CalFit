package com.coffiness.calfit.domain.workspace.member;

import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {
  private final MemberReader memberReader;
  private final MemberStore memberStore;

  @Transactional
  public Member registerMember(Long userId, MemberType memberType) {
    return memberStore.save(userId, memberType);
  }

  @Transactional(readOnly = true)
  public Member getMember(Long userId) {
    String workspaceId = TenantContext.getTenantId();
    return memberReader.getMember(workspaceId, userId);
  }

  @Transactional(readOnly = true)
  public List<Member> getMembers() {
    return memberReader.getMembers();
  }

  @Transactional
  public void updateMemberType(Long memberId, MemberType memberType) {
    memberStore.updateMemberType(memberId, memberType);
  }

  @Transactional
  public void removeMember(Long userId, Long memberId) {
    Member member = memberReader.getMemberById(memberId);
    if (userId.equals(member.userId())) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    memberStore.remove(memberId);
  }

  @Transactional
  public void assignGroup(Long memberId, Long groupId) {
    memberStore.assignGroup(memberId, groupId);
  }

  @Transactional
  public void leaveGroup(Long memberId) {
    memberStore.assignGroup(memberId, null);
  }

  @Transactional(readOnly = true)
  public Map<Long, Long> getGroupMemberCountMap(List<Long> groupIds) {
    return memberReader.getGroupMemberCountMap(groupIds);
  }

  @Transactional
  public void clearGroupFromMembers(Long groupId) {
    memberStore.clearGroupFromMembers(groupId);
  }
}

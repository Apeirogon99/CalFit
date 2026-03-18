package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.domain.workspace.member.Member;
import com.coffiness.calfit.domain.workspace.member.MemberReader;
import com.coffiness.calfit.storage.db.core.member.MemberEntity;
import com.coffiness.calfit.storage.db.core.member.MemberRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberReaderImpl implements MemberReader {
  private final MemberRepository memberRepository;

  @Override
  public Member getMember(String workspaceId, Long userId) {
    MemberEntity entity =
        memberRepository.findByTenantIdAndUserIdAndStatus(workspaceId, userId, EntityStatus.ACTIVE);
    if (entity == null) {
      throw new CoreException(ErrorType.NOT_FOUND);
    }
    return toWorkspaceMember(entity);
  }

  @Override
  public Member getMemberById(Long memberId) {
    return memberRepository
        .findById(memberId)
        .map(this::toWorkspaceMember)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
  }

  @Override
  public List<Member> getMembers() {
    List<MemberEntity> entities = memberRepository.findByStatus(EntityStatus.ACTIVE);
    return entities.stream().map(this::toWorkspaceMember).toList();
  }

  @Override
  public boolean exists(String workspaceId, Long userId) {
    return memberRepository.existsByTenantIdAndUserIdAndStatus(
        workspaceId, userId, EntityStatus.ACTIVE);
  }

  @Override
  public Map<Long, Long> getGroupMemberCountMap(List<Long> groupIds) {
    if (groupIds.isEmpty()) return Map.of();
    return memberRepository.countByGroupIdsAndStatus(groupIds, EntityStatus.ACTIVE).stream()
        .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
  }

  private Member toWorkspaceMember(MemberEntity entity) {
    return new Member(
        entity.getId(),
        entity.getTenantId(),
        entity.getUserId(),
        entity.getMemberType(),
        entity.getGroupId());
  }

  @Override
  public List<Member> getMembersByIds(List<Long> memberIds) {
    return memberRepository.findAllById(memberIds).stream().map(this::toWorkspaceMember).toList();
  }

  @Override
  public List<Member> getMembersByUserIds(String workspaceId, List<Long> userIds) {
    if (workspaceId == null || workspaceId.isBlank() || userIds == null || userIds.isEmpty()) {
      return List.of();
    }

    return memberRepository.findAllByTenantIdAndUserIdIn(workspaceId, userIds).stream()
        .map(this::toWorkspaceMember)
        .toList();
  }

  @Override
  public long countActiveMembers(String workspaceId) {
    return memberRepository.countByTenantIdAndStatus(workspaceId, EntityStatus.ACTIVE);
  }

  @Override
  public long countActiveHrMembers(String workspaceId) {
    return memberRepository.countByTenantIdAndMemberTypeAndStatus(
        workspaceId, MemberType.HR, EntityStatus.ACTIVE);
  }
}

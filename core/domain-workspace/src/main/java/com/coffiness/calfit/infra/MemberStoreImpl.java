package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.domain.workspace.member.Member;
import com.coffiness.calfit.domain.workspace.member.MemberStore;
import com.coffiness.calfit.storage.db.core.member.MemberEntity;
import com.coffiness.calfit.storage.db.core.member.MemberRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberStoreImpl implements MemberStore {
  private final MemberRepository memberRepository;

  @Override
  public Member save(Long userId, MemberType memberType) {
    MemberEntity entity = MemberEntity.create(userId, memberType);
    MemberEntity saved = memberRepository.save(entity);

    return new Member(
        saved.getId(),
        saved.getTenantId(),
        saved.getUserId(),
        saved.getMemberType(),
        saved.getGroupId());
  }

  @Override
  public void updateMemberType(Long memberId, MemberType memberType) {
    MemberEntity entity =
        memberRepository
            .findById(memberId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    entity.updateMemberType(memberType);
  }

  @Override
  public void remove(Long memberId) {
    MemberEntity entity =
        memberRepository
            .findById(memberId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    entity.deleted();
  }

  @Override
  public void assignGroup(Long memberId, Long groupId) {
    MemberEntity entity =
        memberRepository
            .findById(memberId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    entity.assignGroup(groupId);
  }

  @Override
  public void clearGroupFromMembers(Long groupId) {
    memberRepository
        .findByGroupIdAndStatus(groupId, EntityStatus.ACTIVE)
        .forEach(member -> member.assignGroup(null));
  }
}

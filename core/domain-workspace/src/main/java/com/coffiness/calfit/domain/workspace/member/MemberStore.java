package com.coffiness.calfit.domain.workspace.member;

import com.coffiness.calfit.core.enums.MemberType;

public interface MemberStore {

  /**
   * 멤버 저장 (TenantContext에 workspaceId가 설정된 상태에서 호출해야 함)
   *
   * @param userId 사용자 ID
   * @param memberType 멤버 타입
   * @return Member 리턴
   */
  Member save(Long userId, MemberType memberType);

  /**
   * 멤버 타입 수정
   *
   * @param memberId 멤버 ID (MemberEntity PK)
   * @param memberType 멤버 타입
   */
  void updateMemberType(Long memberId, MemberType memberType);

  /**
   * 멤버 삭제
   *
   * @param memberId 멤버 ID (MemberEntity PK)
   */
  void remove(Long memberId);

  /**
   * 멤버 그룹 할당 (null이면 그룹 해제)
   *
   * @param memberId 멤버 ID (MemberEntity PK)
   * @param groupId 그룹 ID (null 허용)
   */
  void assignGroup(Long memberId, Long groupId);

  /**
   * 특정 그룹에 속한 모든 멤버의 그룹 해제
   *
   * @param groupId 그룹 ID
   */
  void clearGroupFromMembers(Long groupId);
}

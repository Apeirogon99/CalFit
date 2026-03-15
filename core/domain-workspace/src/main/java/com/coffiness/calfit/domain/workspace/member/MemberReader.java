package com.coffiness.calfit.domain.workspace.member;

import java.util.List;
import java.util.Map;

public interface MemberReader {
  /**
   * userId로 멤버 정보 조회 (현재 워크스페이스 기준)
   *
   * @param workspaceId 워크스페이스 ID
   * @param userId 사용자 ID
   * @return 멤버 정보
   */
  Member getMember(String workspaceId, Long userId);

  /**
   * memberId(PK)로 멤버 정보 조회
   *
   * @param memberId 멤버 PK
   * @return 멤버 정보
   */
  Member getMemberById(Long memberId);

  /**
   * 현재 워크스페이스의 전체 멤버 조회
   *
   * @return 멤버 정보 리스트
   */
  List<Member> getMembers();

  /**
   * 멤버 존재 여부 확인
   *
   * @param workspaceId 워크스페이스 Id
   * @param userId 사용자 ID
   * @return 존재 여부
   */
  boolean exists(String workspaceId, Long userId);

  /**
   * 그룹별 활성 멤버 수 맵 조회
   *
   * @param groupIds 그룹 ID 목록
   * @return groupId → memberCount 맵
   */
  Map<Long, Long> getGroupMemberCountMap(List<Long> groupIds);

  /**
   * memberId 리스트로 여러 멤버를 한 번에 조회
   *
   * @param memberIds 멤버 고유 ID 목록
   * @return 멤버 정보 리스트
   */
  List<Member> getMembersByIds(List<Long> memberIds);

  /**
   * 워크스페이스의 활성 멤버 수 조회
   *
   * @param workspaceId 워크스페이스 ID
   * @return 활성 멤버 수
   */
  long countActiveMembers(String workspaceId);

  List<Member> getMembersByUserIds(String workspaceId, List<Long> userIds);
}

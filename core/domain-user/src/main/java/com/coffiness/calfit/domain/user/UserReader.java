package com.coffiness.calfit.domain.user;

import java.util.List;

/** 외부 도메인에서 User 정보를 조회하기 위한 인터페이스 (의존성 역전) */
public interface UserReader {

  /**
   * 사용자 ID로 사용자 정보 조회
   *
   * @param userId 사용자 ID
   * @return 사용자 정보 (없으면 null)
   */
  UserInfo getUser(Long userId);

  /**
   * 사용자 ID로 사용자들의 정보 조회
   *
   * @param userIds 사용자 IDs
   * @return 사용자 정보 (없으면 null)
   */
  List<UserInfo> getUsers(List<Long> userIds);

  /**
   * 사용자 존재 여부 확인
   *
   * @param userId 사용자 ID
   * @return 존재 여부
   */
  boolean exists(Long userId);
}

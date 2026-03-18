package com.coffiness.calfit.domain.recruitment;

import com.coffiness.calfit.core.enums.RecruitmentStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface RecruitmentReader {

  List<RecruitmentListInfo> readList(
      Long userId,
      Long groupId,
      boolean hrMember,
      RecruitmentStatus recruitmentStatus,
      Pageable pageable);

  // 목록/주간 면접 권한 판별에 사용할 접근 가능 공고 ID를 조회
  List<Long> readAccessibleRecruitmentIds(Long userId, Long groupId);

  RecruitmentDetailInfo readDetail(Long recruitmentId);

  Recruitment readById(Long recruitmentId);

  // 자동 게시 대상 채용 공고 조회
  List<Recruitment> readScheduledToOpen(LocalDateTime currentTime);

  // 자동 마감 대상 채용 공고 조회
  List<Recruitment> readScheduledToClose(LocalDateTime currentTime);

  Map<String, Long> countOpenRecruitmentsByTenant();

  List<OpenRecruitmentInfo> readOpenByTenantId(String tenantId);

  List<OpenRecruitmentInfo> readOpenByTenantIdAndSearch(String tenantId, String search);
}

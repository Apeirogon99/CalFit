package com.coffiness.calfit.storage.db.core.recruitment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RecruitmentReferenceGroupRepository
    extends JpaRepository<RecruitmentReferenceGroupEntity, Long> {
  List<RecruitmentReferenceGroupEntity> findByRecruitmentId(Long recruitmentId);

  // 목록 조회용으로 여러 공고의 참조 조직을 한 번에 조회
  List<RecruitmentReferenceGroupEntity> findByRecruitmentIdIn(List<Long> recruitmentIds);

  @Modifying(clearAutomatically = true)
  @Query("DELETE FROM RecruitmentReferenceGroupEntity r WHERE r.recruitmentId = :recruitmentId")
  void deleteAllByRecruitmentId(Long recruitmentId);
}

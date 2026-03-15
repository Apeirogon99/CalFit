package com.coffiness.calfit.storage.db.core.recruitment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RecruitmentStageRepository extends JpaRepository<RecruitmentStageEntity, Long> {
  List<RecruitmentStageEntity> findByRecruitmentIdIn(List<Long> recruitmentIds);

  List<RecruitmentStageEntity> findByRecruitmentId(Long recruitmentId);

  List<RecruitmentStageEntity> findByRecruitmentIdOrderByStageStepAsc(Long recruitmentId);

  boolean existsByIdAndRecruitmentId(Long id, Long recruitmentId);

  @Modifying(clearAutomatically = true)
  @Query("DELETE FROM RecruitmentStageEntity r WHERE r.recruitmentId = :recruitmentId")
  void deleteAllByRecruitmentId(Long recruitmentId);
}

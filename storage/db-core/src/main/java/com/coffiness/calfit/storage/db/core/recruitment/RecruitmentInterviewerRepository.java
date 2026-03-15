package com.coffiness.calfit.storage.db.core.recruitment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RecruitmentInterviewerRepository
    extends JpaRepository<RecruitmentInterviewerEntity, Long> {
  List<RecruitmentInterviewerEntity> findByRecruitmentIdIn(List<Long> recruitmentIds);

  List<RecruitmentInterviewerEntity> findByRecruitmentId(Long recruitmentId);

  @Modifying(clearAutomatically = true)
  @Query("DELETE FROM RecruitmentInterviewerEntity r WHERE r.recruitmentId = :recruitmentId")
  void deleteAllByRecruitmentId(Long recruitmentId);
}

package com.coffiness.calfit.storage.db.core.recruitment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecruitmentHistoryRepository
    extends JpaRepository<RecruitmentHistoryEntity, Long> {

  List<RecruitmentHistoryEntity> findByRecruitmentIdOrderByCreatedAtAsc(Long recruitmentId);
}

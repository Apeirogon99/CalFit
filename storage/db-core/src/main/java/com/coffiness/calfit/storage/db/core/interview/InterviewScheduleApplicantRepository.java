package com.coffiness.calfit.storage.db.core.interview;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewScheduleApplicantRepository
    extends JpaRepository<InterviewScheduleApplicantEntity, Long> {

  List<InterviewScheduleApplicantEntity> findAllByTenantIdAndApplicantIdIn(
      String tenantId, List<Long> applicantIds);

  List<InterviewScheduleApplicantEntity> findAllByTenantIdAndInterviewScheduleIdIn(
      String tenantId, List<Long> interviewScheduleIds);
}

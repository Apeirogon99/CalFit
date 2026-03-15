package com.coffiness.calfit.storage.db.core.interview;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewScheduleInterviewerRepository
    extends JpaRepository<InterviewScheduleInterviewerEntity, Long> {

  List<InterviewScheduleInterviewerEntity> findAllByTenantIdAndUserIdIn(
      String tenantId, List<Long> userIds);

  List<InterviewScheduleInterviewerEntity> findAllByTenantIdAndInterviewScheduleIdIn(
      String tenantId, List<Long> interviewScheduleIds);
}

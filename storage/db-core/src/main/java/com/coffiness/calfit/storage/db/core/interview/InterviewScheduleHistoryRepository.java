package com.coffiness.calfit.storage.db.core.interview;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewScheduleHistoryRepository
    extends JpaRepository<InterviewScheduleHistoryEntity, Long> {}

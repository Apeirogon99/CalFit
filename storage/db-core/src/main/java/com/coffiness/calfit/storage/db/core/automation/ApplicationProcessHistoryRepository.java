package com.coffiness.calfit.storage.db.core.automation;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationProcessHistoryRepository
    extends JpaRepository<ApplicationProcessHistoryEntity, Long> {
  List<ApplicationProcessHistoryEntity> findByApplicationId(Long applicationId);
}

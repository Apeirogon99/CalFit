package com.coffiness.calfit.storage.db.core.automation;

import com.coffiness.calfit.core.enums.AutomationEventStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AutomationEventRepository extends JpaRepository<AutomationEventEntity, Long> {
  List<AutomationEventEntity> findByEventStatus(AutomationEventStatus eventStatus);

  List<AutomationEventEntity> findByApplicationIdAndEventStatus(
      Long applicationId, AutomationEventStatus eventStatus);

  @Query("SELECT e.eventStatus, COUNT(e) FROM AutomationEventEntity e" + " GROUP BY e.eventStatus")
  List<Object[]> countGroupByEventStatus();

  @Query("SELECT e.actionType, COUNT(e) FROM AutomationEventEntity e" + " GROUP BY e.actionType")
  List<Object[]> countGroupByActionType();
}

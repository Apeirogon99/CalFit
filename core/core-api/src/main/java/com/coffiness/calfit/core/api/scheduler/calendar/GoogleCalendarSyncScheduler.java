package com.coffiness.calfit.core.api.scheduler.calendar;

import com.coffiness.calfit.core.api.facade.calendar.CalendarConnectFacade;
import com.coffiness.calfit.domain.ExternalCalendar;
import com.coffiness.calfit.domain.ExternalCalendarReader;
import com.coffiness.calfit.domain.workspace.Workspace;
import com.coffiness.calfit.domain.workspace.WorkspaceReader;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/*
 * 구글 캘린더 증분 동기화를 주기적으로 실행하는 스케줄러
 * */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleCalendarSyncScheduler {

  private final WorkspaceReader workspaceReader;
  private final ExternalCalendarReader externalCalendarReader;
  private final CalendarConnectFacade calendarConnectFacade;

  @Scheduled(
      fixedDelayString = "${calendar.google-sync.scheduler-fixed-delay-ms:300000}",
      initialDelayString = "${calendar.google-sync.scheduler-initial-delay-ms:120000}")
  @SchedulerLock(
      name = "googleCalendarSyncScheduler.syncAllTenants",
      lockAtMostFor = "${calendar.google-sync.scheduler-lock-at-most:PT15M}",
      lockAtLeastFor = "${calendar.google-sync.scheduler-lock-at-least:PT10S}")
  public void syncAllTenants() {
    List<Workspace> workspaces = workspaceReader.getAllWorkspaces();

    for (Workspace workspace : workspaces) {
      try {
        syncTenant(workspace.workspaceId());
      } catch (RuntimeException e) {
        log.error(
            "[GoogleCalendarSyncScheduler] tenant sync failed - tenantId: {}",
            workspace.workspaceId(),
            e);
      }
    }
  }

  private void syncTenant(String tenantId) {
    try {
      TenantContext.setTenantId(tenantId);

      List<ExternalCalendar> calendars = externalCalendarReader.readAllSyncEnabled();

      for (ExternalCalendar calendar : calendars) {
        try {
          calendarConnectFacade.syncGoogleCalendar(calendar.userId());
        } catch (RuntimeException e) {
          log.error(
              "[GoogleCalendarSyncScheduler] 사용자 동기화 실패 - tenantId: {}, userId: {}",
              tenantId,
              calendar.userId(),
              e);
        }
      }
    } finally {
      TenantContext.clear();
    }
  }
}

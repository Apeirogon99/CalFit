package com.coffiness.calfit.core.api.scheduler.meetingRoom;

import com.coffiness.calfit.domain.meetingRoom.MeetingRoomReservationService;
import com.coffiness.calfit.domain.workspace.Workspace;
import com.coffiness.calfit.domain.workspace.WorkspaceReader;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingRoomReservationStatusScheduler {

  private final WorkspaceReader workspaceReader;
  private final MeetingRoomReservationService meetingRoomReservationService;

  @Scheduled(
      cron = "${meeting-room.reservation.status.scheduler-cron:0 * * * * *}",
      zone = "Asia/Seoul")
  @SchedulerLock(
      name = "meetingRoomReservationStatusScheduler.updateReservationStatuses",
      lockAtMostFor = "${meeting-room.reservation.status.scheduler-lock-at-most:PT5M}",
      lockAtLeastFor = "${meeting-room.reservation.status.scheduler-lock-at-least:PT0S}")
  public void updateReservationStatuses() {
    List<Workspace> workspaces = workspaceReader.getAllWorkspaces();
    int totalUpdated = 0;

    for (Workspace workspace : workspaces) {
      try {
        TenantContext.setTenantId(workspace.workspaceId());
        totalUpdated += meetingRoomReservationService.updateReservationStatusesBySchedule();
      } catch (RuntimeException exception) {
        log.error("회의실 예약 상태 동기화 실패 - tenantId: {}", workspace.workspaceId(), exception);
      } finally {
        TenantContext.clear();
      }
    }

    if (totalUpdated > 0) {
      log.info("회의실 예약 상태 동기화 완료 - updated: {}", totalUpdated);
    }
  }
}

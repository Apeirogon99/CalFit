package com.coffiness.calfit.core.api.scheduler.recruitment;

import com.coffiness.calfit.domain.recruitment.RecruitmentService;
import com.coffiness.calfit.domain.recruitment.RecruitmentStatusTransitionResult;
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
 * 채용 공고 상태를 시간 기준으로 자동 전환하는 스케줄러
 * */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecruitmentStatusScheduler {

  private final WorkspaceReader workspaceReader;
  private final RecruitmentService recruitmentService;

  @Scheduled(cron = "${recruitment.status.scheduler-cron:0 * * * * *}", zone = "Asia/Seoul")
  @SchedulerLock(
      name = "recruitmentStatusScheduler.updateRecruitmentStatuses",
      lockAtMostFor = "${recruitment.status.scheduler-lock-at-most:PT5M}",
      lockAtLeastFor = "${recruitment.status.scheduler-lock-at-least:PT0S}")

  // 모든 워크스페이스를 순회하며 상태 전이를 수행
  public void updateRecruitmentStatuses() {
    List<Workspace> workspaces = workspaceReader.getAllWorkspaces();

    int totalOpened = 0;
    int totalClosed = 0;

    for (Workspace workspace : workspaces) {
      try {
        TenantContext.setTenantId(workspace.workspaceId());
        RecruitmentStatusTransitionResult result =
            recruitmentService.updateRecruitmentStatusBySchedule();
        totalOpened += result.openedCount();
        totalClosed += result.closedCount();
      } catch (RuntimeException e) {
        log.error("테넌트 상태 전환 실패 - tenantId: {}", workspace.workspaceId(), e);
      } finally {
        TenantContext.clear();
      }
    }

    if (totalOpened == 0 && totalClosed == 0) {
      return;
    }

    log.info("상태 전환 적용 완료 - opened: {}, closed: {}", totalOpened, totalClosed);
  }
}

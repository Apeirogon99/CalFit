package com.coffiness.calfit.domain.interview.event;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.storage.db.core.application.ApplicationEntity;
import com.coffiness.calfit.storage.db.core.application.ApplicationRepository;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomRepository;
import com.coffiness.calfit.support.email.EmailProperties;
import com.coffiness.calfit.support.email.EmailService;
import com.coffiness.calfit.support.email.InterviewScheduleEmailMessage;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewApplicantEmailEventHandler {

  private static final String SUBJECT = "[CalFit] 면접 일정 안내";

  private final ApplicationRepository applicationRepository;
  private final MeetingRoomRepository meetingRoomRepository;
  private final EmailService emailService;
  private final EmailProperties emailProperties;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(InterviewCreatedEvent event) {
    try {
      TenantContext.setTenantId(event.tenantId());

      List<Long> applicantIds =
          event.applicantIds().stream().filter(id -> id != null).distinct().toList();
      if (applicantIds.isEmpty()) {
        return;
      }

      List<ApplicationEntity> applications =
          applicationRepository
              .findByRecruitmentIdAndStatus(event.recruitmentId(), EntityStatus.ACTIVE)
              .stream()
              .filter(application -> applicantIds.contains(application.getApplicantId()))
              .toList();

      if (applications.isEmpty()) {
        return;
      }

      String location =
          meetingRoomRepository
              .findByTenantIdAndIdAndStatus(
                  event.tenantId(), event.meetingRoomId(), EntityStatus.ACTIVE)
              .map(meetingRoom -> meetingRoom.getName())
              .orElse("미정");
      LocalDateTime endAt = event.scheduledAt().plusMinutes(event.durationMinutes());

      for (ApplicationEntity application : applications) {
        InterviewScheduleEmailMessage message =
            InterviewScheduleEmailMessage.builder()
                .applicantEmail(application.getEmail())
                .applicantName(application.getName())
                .subject(SUBJECT)
                .scheduledAt(event.scheduledAt())
                .endAt(endAt)
                .location(location)
                .build();
        emailService.sendInterviewScheduleEmail(emailProperties.getSender(), message);
      }
    } catch (RuntimeException e) {
      log.error(
          "Failed to send interview schedule emails. interviewScheduleId={}",
          event.interviewScheduleId(),
          e);
    } finally {
      TenantContext.clear();
    }
  }
}

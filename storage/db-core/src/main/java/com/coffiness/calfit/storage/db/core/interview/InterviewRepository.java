package com.coffiness.calfit.storage.db.core.interview;

import java.time.LocalDateTime;
import java.util.List;

public interface InterviewRepository {

  record MeetingRoomBusySlotRow(
      Long meetingRoomId, Long interviewScheduleId, LocalDateTime start, LocalDateTime end) {}

  record InterviewerBusySlotRow(
      Long interviewerId, Long interviewScheduleId, LocalDateTime start, LocalDateTime end) {}

  record ApplicantBusySlotRow(
      Long applicantId, Long interviewScheduleId, LocalDateTime start, LocalDateTime end) {}

  record InterviewScheduleCalendarRow(
      Long interviewScheduleId,
      LocalDateTime startAt,
      LocalDateTime endAt,
      Long meetingRoomId,
      Long interviewerUserId,
      String interviewerName,
      String title,
      String applicantName,
      String description,
      String location) {}

  record PendingInterviewApplicantRow(
      Long applicationId, Long applicantId, String name, String email) {}

  record PendingInterviewStageRow(
      Long recruitmentStageId,
      String stageName,
      Integer stageStep,
      List<PendingInterviewApplicantRow> applicants) {}

  // 주간 면접 일정 조회 결과를 저장소 계층에서 전달
  record WeeklyInterviewScheduleRow(
      Long interviewScheduleId,
      Long recruitmentId,
      LocalDateTime startAt,
      LocalDateTime endAt,
      Long interviewerUserId,
      String interviewerName,
      String title,
      String applicantName,
      String description,
      String location) {}

  boolean isHrMember(Long userId);

  int getMeetingRoomCapacity(Long meetingRoomId);

  List<MeetingRoomBusySlotRow> findMeetingRoomBusySlots(
      List<Long> meetingRoomIds, LocalDateTime from, LocalDateTime to);

  List<InterviewerBusySlotRow> findInterviewerBusySlots(
      List<Long> interviewerIds, LocalDateTime from, LocalDateTime to);

  List<ApplicantBusySlotRow> findApplicantBusySlots(
      List<Long> applicantIds, LocalDateTime from, LocalDateTime to);

  List<InterviewScheduleCalendarRow> getSchedulesByRecruitmentId(
      Long recruitmentId, LocalDateTime from, LocalDateTime to);

  // 채용 공고의 면접 단계별 대기 지원자 목록을 조회
  List<PendingInterviewStageRow> getPendingInterviewStages(Long recruitmentId);

  // 접근 가능한 채용 공고 기준으로 주간 면접 일정 목록을 조회
  List<WeeklyInterviewScheduleRow> getWeeklySchedules(
      List<Long> recruitmentIds, LocalDateTime from, LocalDateTime to);

  Long createConfirmedSchedule(
      Long userId,
      Long recruitmentId,
      Long recruitmentStageId,
      Long meetingRoomId,
      LocalDateTime scheduledAt,
      Integer durationMinutes,
      String memo,
      List<Long> interviewerIds,
      List<Long> applicantIds);

  void cancelConfirmedSchedule(Long userId, Long interviewScheduleId);
}

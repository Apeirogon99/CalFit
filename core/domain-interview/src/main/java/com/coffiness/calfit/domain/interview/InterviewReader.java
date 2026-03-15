package com.coffiness.calfit.domain.interview;

import java.time.LocalDateTime;
import java.util.List;

public interface InterviewReader {

  boolean isHrMember(Long userId);

  int getMeetingRoomCapacity(Long meetingRoomId);

  List<InterviewAvailability.MeetingRoomBusySlot> findMeetingRoomBusySlots(
      List<Long> meetingRoomIds, LocalDateTime from, LocalDateTime to);

  List<InterviewAvailability.InterviewerBusySlot> findInterviewerBusySlots(
      List<Long> interviewerIds, LocalDateTime from, LocalDateTime to);

  List<InterviewAvailability.ApplicantBusySlot> findApplicantBusySlots(
      List<Long> applicantIds, LocalDateTime from, LocalDateTime to);

  List<InterviewScheduleCalendarItem> getSchedulesByRecruitmentId(
      Long recruitmentId, LocalDateTime from, LocalDateTime to);

  // 채용 공고의 면접 단계별 대기 지원자 목록을 조회
  List<PendingInterviewStage> getPendingInterviewStages(Long recruitmentId);

  // 접근 가능한 채용 공고 기준으로 주간 면접 일정 목록을 조회
  List<WeeklyInterviewScheduleItem> getWeeklySchedules(
      List<Long> recruitmentIds, LocalDateTime from, LocalDateTime to);
}

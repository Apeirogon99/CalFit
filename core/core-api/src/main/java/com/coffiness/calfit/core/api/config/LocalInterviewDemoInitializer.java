package com.coffiness.calfit.core.api.config;

import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentStageRequest;
import com.coffiness.calfit.core.enums.*;
import com.coffiness.calfit.domain.ScheduleService;
import com.coffiness.calfit.domain.interview.InterviewService;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomService;
import com.coffiness.calfit.domain.recruitment.RecruitmentService;
import com.coffiness.calfit.domain.workspace.group.GroupInfo;
import com.coffiness.calfit.domain.workspace.group.GroupService;
import com.coffiness.calfit.domain.workspace.member.MemberService;
import com.coffiness.calfit.storage.db.core.applicant.ApplicantEntity;
import com.coffiness.calfit.storage.db.core.applicant.ApplicantRepository;
import com.coffiness.calfit.storage.db.core.application.ApplicationEntity;
import com.coffiness.calfit.storage.db.core.application.ApplicationRepository;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleRepository;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleRepository;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomEntity;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomRepository;
import com.coffiness.calfit.storage.db.core.member.MemberEntity;
import com.coffiness.calfit.storage.db.core.member.MemberRepository;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentEntity;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentRepository;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentStageEntity;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentStageRepository;
import com.coffiness.calfit.storage.db.core.user.UserEntity;
import com.coffiness.calfit.storage.db.core.user.UserRepository;
import com.coffiness.calfit.v1.request.ScheduleCreateRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/*
 * 로컬 H2 환경에서 면접 일정 테스트용 더미 데이터 생성 클래스
 * */
@Slf4j
@Component
@Profile("local")
@ConditionalOnProperty(
    name = "app.local-interview-demo.enabled",
    havingValue = "true",
    matchIfMissing = true)
@Order(1)
@RequiredArgsConstructor
public class LocalInterviewDemoInitializer implements ApplicationRunner {

  private static final String ADMIN_EMAIL = "admin@coffiiness.com";
  private static final String HR_EMAIL = "hr@coffiiness.com";
  private static final String MEMBER_EMAIL = "member@coffiiness.com";
  private static final List<String> DEFAULT_MEETING_ROOM_FACILITIES = List.of("WiFi");
  private static final String DEFAULT_MEETING_ROOM_COLOR = "#10B981";

  private final UserRepository userRepository;
  private final MemberRepository memberRepository;
  private final RecruitmentRepository recruitmentRepository;
  private final RecruitmentStageRepository recruitmentStageRepository;
  private final MeetingRoomRepository meetingRoomRepository;
  private final ApplicationRepository applicationRepository;
  private final ApplicantRepository applicantRepository;
  private final ScheduleRepository scheduleRepository;
  private final InterviewScheduleRepository interviewScheduleRepository;
  private final PasswordEncoder passwordEncoder;
  private final GroupService groupService;
  private final MemberService memberService;
  private final MeetingRoomService meetingRoomService;
  private final RecruitmentService recruitmentService;
  private final ScheduleService scheduleService;
  private final InterviewService interviewService;

  // 로컬 실행 시 필요한 면접 테스트 데이터를 한 번에 구성
  @Override
  public void run(ApplicationArguments args) {
    UserEntity adminUser = userRepository.findByEmail(ADMIN_EMAIL).orElse(null);
    UserEntity hrUser = userRepository.findByEmail(HR_EMAIL).orElse(null);
    UserEntity memberUser = userRepository.findByEmail(MEMBER_EMAIL).orElse(null);

    if (hrUser == null || memberUser == null) {
      log.info("[LocalInterviewDemoInitializer] 기본 로컬 계정이 없어 더미 생성을 건너뜁니다.");
      return;
    }

    String tenantId = memberRepository.findTenantIdByUserId(memberUser.getId()).orElse(null);
    if (tenantId == null || tenantId.isBlank()) {
      log.info("[LocalInterviewDemoInitializer] 워크스페이스가 없어 더미 생성을 건너뜁니다.");
      return;
    }

    TenantContext.setTenantId(tenantId);
    try {
      GroupInfo devGroup = ensureGroup("개발팀", "#3B82F6");
      GroupInfo hrGroup = ensureGroup("인사팀", "#10B981");
      ensureGroup("마케팅팀", "#F59E0B");
      ensureGroupAssignment(adminUser != null ? adminUser.getId() : null, hrGroup.id());
      ensureGroupAssignment(hrUser.getId(), hrGroup.id());
      ensureGroupAssignment(memberUser.getId(), devGroup.id());

      Long largeRoomId = ensureMeetingRoom(hrUser.getId(), tenantId, "대회의실", 3, 10);
      ensureMeetingRoom(hrUser.getId(), tenantId, "소회의실 A", 2, 4);

      List<ApplicantEntity> applicants = ensureApplicants();

      RecruitmentSeed backendRecruitment =
          ensureRecruitment(
              hrUser.getId(),
              devGroup.id(),
              memberUser.getId(),
              "이번 주 백엔드 면접 테스트 공고",
              "Spring Boot 기반 백엔드 포지션 면접 테스트용 공고입니다.",
              CareerType.EXPERIENCED,
              2,
              6);

      RecruitmentSeed frontendRecruitment =
          ensureRecruitment(
              hrUser.getId(),
              devGroup.id(),
              memberUser.getId(),
              "이번 주 프론트 면접 테스트 공고",
              "React 기반 프론트엔드 포지션 면접 테스트용 공고입니다.",
              CareerType.IRRELEVANT,
              null,
              null);

      seedApplications(backendRecruitment, frontendRecruitment, applicants);
      seedMemberSchedules(hrUser.getId(), memberUser.getId());
      seedWeeklyInterviews(
          tenantId,
          hrUser.getId(),
          memberUser.getId(),
          largeRoomId,
          backendRecruitment,
          frontendRecruitment,
          applicants);
    } finally {
      TenantContext.clear();
    }

    log.info("[LocalInterviewDemoInitializer] 로컬 면접/일정 더미 데이터 보장 완료");
  }

  // 개발팀 그룹이 없으면 생성하고 있으면 기존 정보를 재사용
  private GroupInfo ensureGroup(String name, String color) {
    return groupService.getGroups().stream()
        .filter(group -> name.equals(group.name()))
        .findFirst()
        .map(group -> new GroupInfo(group.id(), group.name(), group.color(), 0L))
        .orElseGet(() -> groupService.createGroup(name, color));
  }

  // 로컬 기본 멤버 계정을 지정한 그룹에 배정
  private void ensureGroupAssignment(Long userId, Long groupId) {
    if (userId == null) {
      return;
    }
    String tenantId = TenantContext.getTenantId();
    MemberEntity member =
        memberRepository.findByTenantIdAndUserIdAndStatus(tenantId, userId, EntityStatus.ACTIVE);
    if (member == null || groupId == null || groupId.equals(member.getGroupId())) {
      return;
    }
    memberService.assignGroup(member.getId(), groupId);
  }

  // 면접 테스트에 필요한 회의실을 보장
  private Long ensureMeetingRoom(
      Long ownerUserId, String tenantId, String name, Integer location, Integer capacity) {
    return meetingRoomRepository
        .findByTenantIdAndName(tenantId, name)
        .map(MeetingRoomEntity::getId)
        .orElseGet(
            () ->
                meetingRoomService
                    .create(
                        name,
                        location,
                        capacity,
                        "로컬 면접 더미 데이터용 회의실",
                        DEFAULT_MEETING_ROOM_FACILITIES,
                        DEFAULT_MEETING_ROOM_COLOR,
                        ownerUserId)
                    .id());
  }

  // 주간 면접 더미에 사용할 지원자 목록을 보장
  private List<ApplicantEntity> ensureApplicants() {
    return List.of(
        ensureApplicant("applicant1@coffiiness.com", "지원자 김하나"),
        ensureApplicant("applicant2@coffiiness.com", "지원자 박둘"),
        ensureApplicant("applicant3@coffiiness.com", "지원자 이셋"),
        ensureApplicant("applicant4@coffiiness.com", "지원자 최넷"),
        ensureApplicant("applicant5@coffiiness.com", "지원자 정다섯"),
        ensureApplicant("applicant6@coffiiness.com", "지원자 한여섯"),
        ensureApplicant("applicant7@coffiiness.com", "지원자 윤일곱"));
  }

  // 이메일 기준으로 지원자 더미를 한 명씩 보장
  private ApplicantEntity ensureApplicant(String email, String name) {
    return applicantRepository
        .findByEmail(email)
        .orElseGet(
            () ->
                applicantRepository.save(
                    ApplicantEntity.create(email, passwordEncoder.encode("applicant1234!"), name)));
  }

  // 테스트용 채용 공고와 첫 면접 단계를 준비
  private RecruitmentSeed ensureRecruitment(
      Long hrUserId,
      Long leadGroupId,
      Long interviewerUserId,
      String title,
      String contents,
      CareerType careerType,
      Integer minExperienceYears,
      Integer maxExperienceYears) {
    RecruitmentEntity recruitment =
        recruitmentRepository.findByStatusOrderByCreatedAtDesc(EntityStatus.ACTIVE).stream()
            .filter(item -> title.equals(item.getTitle()))
            .findFirst()
            .orElseGet(
                () -> {
                  Long recruitmentId =
                      recruitmentService.createRecruitment(
                          hrUserId,
                          new RecruitmentCreateRequest(
                              title,
                              2,
                              1L,
                              contents,
                              LocalDateTime.now().minusDays(7),
                              LocalDateTime.now().plusDays(30),
                              careerType,
                              minExperienceYears,
                              maxExperienceYears,
                              leadGroupId,
                              List.of(),
                              List.of(interviewerUserId),
                              List.of(
                                  new RecruitmentStageRequest(
                                      "서류 심사", RecruitmentStageType.DOCUMENT, 1),
                                  new RecruitmentStageRequest(
                                      "실무 면접", RecruitmentStageType.INTERVIEW, 2),
                                  new RecruitmentStageRequest(
                                      "최종 면접", RecruitmentStageType.INTERVIEW, 3))));

                  return recruitmentRepository
                      .findByIdAndStatus(recruitmentId, EntityStatus.ACTIVE)
                      .orElseThrow();
                });

    List<Long> interviewStageIds =
        recruitmentStageRepository
            .findByRecruitmentIdOrderByStageStepAsc(recruitment.getId())
            .stream()
            .filter(stage -> stage.getStageType() == RecruitmentStageType.INTERVIEW)
            .map(RecruitmentStageEntity::getId)
            .toList();
    Long firstInterviewStageId = interviewStageIds.stream().findFirst().orElseThrow();
    Long finalInterviewStageId =
        interviewStageIds.isEmpty()
            ? firstInterviewStageId
            : interviewStageIds.get(interviewStageIds.size() - 1);

    Long firstStageId =
        recruitmentStageRepository
            .findByRecruitmentIdOrderByStageStepAsc(recruitment.getId())
            .stream()
            .map(RecruitmentStageEntity::getId)
            .findFirst()
            .orElseThrow();

    return new RecruitmentSeed(
        recruitment.getId(),
        recruitment.getApplicationTemplateId(),
        firstStageId,
        firstInterviewStageId,
        finalInterviewStageId);
  }

  // 테스트 공고별 지원서를 중복 없이 생성
  private void seedApplications(
      RecruitmentSeed backendRecruitment,
      RecruitmentSeed frontendRecruitment,
      List<ApplicantEntity> applicants) {
    ensureApplication(
        backendRecruitment,
        applicants.get(0),
        Gender.FEMALE,
        LocalDate.of(1998, 1, 15),
        "010-1000-0001");
    ensureApplication(
        backendRecruitment,
        applicants.get(1),
        Gender.MALE,
        LocalDate.of(1996, 4, 3),
        "010-1000-0002");
    ensureApplication(
        backendRecruitment,
        applicants.get(4),
        Gender.MALE,
        LocalDate.of(1994, 11, 21),
        "010-1000-0005");

    ensureApplication(
        frontendRecruitment,
        applicants.get(2),
        Gender.FEMALE,
        LocalDate.of(1999, 7, 10),
        "010-1000-0003");
    ensureApplication(
        frontendRecruitment,
        applicants.get(3),
        Gender.MALE,
        LocalDate.of(1997, 9, 28),
        "010-1000-0004");

    ensureApplicationAtStage(
        backendRecruitment,
        backendRecruitment.firstInterviewStageId(),
        applicants.get(5),
        Gender.FEMALE,
        LocalDate.of(1998, 12, 9),
        "010-1000-0006");
    ensureApplicationAtStage(
        frontendRecruitment,
        frontendRecruitment.finalInterviewStageId(),
        applicants.get(6),
        Gender.MALE,
        LocalDate.of(1995, 6, 14),
        "010-1000-0007");
  }

  // 지원자와 공고 조합별 지원서를 첫 단계에 보장
  private void ensureApplication(
      RecruitmentSeed recruitment,
      ApplicantEntity applicant,
      Gender gender,
      LocalDate birthDate,
      String phone) {
    ensureApplicationAtStage(
        recruitment, recruitment.firstStageId(), applicant, gender, birthDate, phone);
  }

  // 지원자와 공고 조합별 지원서를 원하는 단계에 보장
  private void ensureApplicationAtStage(
      RecruitmentSeed recruitment,
      Long recruitmentStageId,
      ApplicantEntity applicant,
      Gender gender,
      LocalDate birthDate,
      String phone) {
    if (applicationRepository.existsByApplicantIdAndRecruitmentId(
        applicant.getId(), recruitment.recruitmentId())) {
      moveApplicationToStage(recruitment.recruitmentId(), applicant.getId(), recruitmentStageId);
      return;
    }

    applicationRepository.save(
        ApplicationEntity.create(
            applicant.getId(),
            recruitment.recruitmentId(),
            recruitmentStageId,
            recruitment.applicationTemplateId(),
            applicant.getName(),
            gender,
            birthDate.atStartOfDay(),
            phone,
            applicant.getEmail(),
            buildApplicationFormFields(recruitment.recruitmentId(), applicant.getName())));
  }

  // 로컬 더미용 지원서 폼 데이터를 구성
  private String buildApplicationFormFields(Long recruitmentId, String applicantName) {
    return "{\"motivation\":\"로컬 H2 테스트 지원서입니다.\","
        + "\"portfolio\":\"https://portfolio.example.com/"
        + recruitmentId
        + "\","
        + "\"note\":\""
        + applicantName
        + " 더미 지원 데이터\"}";
  }

  // 면접관 본인 일정과 공유 일정을 함께 생성
  private void seedMemberSchedules(Long hrUserId, Long memberUserId) {
    LocalDate weekStart = currentWeekStart();

    ensureScheduleVisibleToUser(
        memberUserId,
        memberUserId,
        "개인 집중 업무",
        "이직원 개인 일정 테스트용",
        weekStart.plusDays(5).atTime(17, 0),
        weekStart.plusDays(5).atTime(18, 0),
        List.of());

    ensureScheduleVisibleToUser(
        memberUserId,
        memberUserId,
        "포트폴리오 검토 시간",
        "면접 준비용 개인 블록",
        weekStart.plusDays(6).atTime(9, 0),
        weekStart.plusDays(6).atTime(10, 0),
        List.of());

    ensureScheduleVisibleToUser(
        memberUserId,
        hrUserId,
        "면접관 사전 브리핑",
        "HR이 생성한 공유 일정",
        weekStart.plusDays(6).atTime(18, 0),
        weekStart.plusDays(6).atTime(19, 0),
        List.of(memberUserId));
  }

  // 사용자가 볼 수 있는 개인/공유 일정을 중복 없이 생성
  private void ensureScheduleVisibleToUser(
      Long visibleUserId,
      Long ownerUserId,
      String title,
      String description,
      LocalDateTime start,
      LocalDateTime end,
      List<Long> attendeeIds) {
    boolean exists =
        scheduleRepository
            .findOverlappingSchedules(visibleUserId, start.minusMinutes(1), end.plusMinutes(1))
            .stream()
            .anyMatch(
                schedule ->
                    title.equals(schedule.getTitle()) && start.equals(schedule.getStartTime()));

    if (exists) {
      return;
    }

    scheduleService.createSchedule(
        ownerUserId,
        null,
        new ScheduleCreateRequest(
            title, description, ScheduleType.MEETING, start, end, false, null, true, attendeeIds));
  }

  // 이번 주와 다음 주 면접 더미를 구분해서 생성
  private void seedWeeklyInterviews(
      String tenantId,
      Long hrUserId,
      Long interviewerUserId,
      Long meetingRoomId,
      RecruitmentSeed backendRecruitment,
      RecruitmentSeed frontendRecruitment,
      List<ApplicantEntity> applicants) {
    LocalDate weekStart = currentWeekStart();
    LocalDateTime now = LocalDateTime.now();
    List<LocalDateTime> interviewSlots =
        ensureFutureInterviewSlots(
            now,
            List.of(
                weekStart.plusDays(6).atTime(10, 0),
                weekStart.plusDays(6).atTime(12, 0),
                weekStart.plusDays(6).atTime(14, 0),
                weekStart.plusDays(6).atTime(16, 0),
                weekStart.plusDays(8).atTime(10, 0)));

    ensureInterview(
        tenantId,
        hrUserId,
        backendRecruitment,
        meetingRoomId,
        interviewerUserId,
        applicants.get(0).getId(),
        interviewSlots.get(0),
        "백엔드 실무 면접 A");

    ensureInterview(
        tenantId,
        hrUserId,
        backendRecruitment,
        meetingRoomId,
        interviewerUserId,
        applicants.get(1).getId(),
        interviewSlots.get(1),
        "백엔드 실무 면접 B");

    ensureInterview(
        tenantId,
        hrUserId,
        frontendRecruitment,
        meetingRoomId,
        interviewerUserId,
        applicants.get(2).getId(),
        interviewSlots.get(2),
        "프론트 실무 면접 A");

    ensureInterview(
        tenantId,
        hrUserId,
        frontendRecruitment,
        meetingRoomId,
        interviewerUserId,
        applicants.get(3).getId(),
        interviewSlots.get(3),
        "프론트 실무 면접 B");

    ensureInterview(
        tenantId,
        hrUserId,
        backendRecruitment,
        meetingRoomId,
        interviewerUserId,
        applicants.get(4).getId(),
        interviewSlots.get(4),
        "다음 주 백엔드 면접");
  }

  // 이미 지난 면접 시각이면 다음 가능한 정각 슬롯으로 밀고, 서로 겹치지 않게 순차 배치한다.
  private List<LocalDateTime> ensureFutureInterviewSlots(
      LocalDateTime referenceTime, List<LocalDateTime> scheduledTimes) {
    List<LocalDateTime> resolvedSlots = new ArrayList<>();
    LocalDateTime nextAvailableSlot =
        referenceTime.plusHours(1).withMinute(0).withSecond(0).withNano(0);

    for (LocalDateTime scheduledTime : scheduledTimes) {
      LocalDateTime resolvedSlot = scheduledTime;
      if (!resolvedSlot.isAfter(referenceTime)) {
        resolvedSlot = nextAvailableSlot;
      }

      if (!resolvedSlots.isEmpty()) {
        LocalDateTime minimumNextSlot = resolvedSlots.get(resolvedSlots.size() - 1).plusHours(1);
        if (resolvedSlot.isBefore(minimumNextSlot)) {
          resolvedSlot = minimumNextSlot;
        }
      }

      resolvedSlots.add(resolvedSlot);
      nextAvailableSlot = resolvedSlot.plusHours(1);
    }

    return resolvedSlots;
  }

  // 같은 시각의 면접이 없을 때만 확정 면접 일정을 생성
  private void ensureInterview(
      String tenantId,
      Long hrUserId,
      RecruitmentSeed recruitment,
      Long meetingRoomId,
      Long interviewerUserId,
      Long applicantId,
      LocalDateTime scheduledAt,
      String memo) {
    moveApplicationToStage(
        recruitment.recruitmentId(), applicantId, recruitment.firstInterviewStageId());

    boolean exists =
        interviewScheduleRepository
            .findAllByTenantIdAndRecruitmentIdAndScheduledAtGreaterThanEqualAndScheduledAtLessThanAndStatusNotOrderByScheduledAtAsc(
                tenantId,
                recruitment.recruitmentId(),
                scheduledAt.minusMinutes(1),
                scheduledAt.plusMinutes(1),
                InterviewStatus.CANCELLED)
            .stream()
            .anyMatch(schedule -> scheduledAt.equals(schedule.getScheduledAt()));

    if (exists) {
      return;
    }

    interviewService.create(
        hrUserId,
        recruitment.recruitmentId(),
        recruitment.firstInterviewStageId(),
        InterviewRound.FIRST,
        List.of(interviewerUserId),
        List.of(applicantId),
        meetingRoomId,
        scheduledAt,
        60,
        memo);
  }

  // 기존 지원서를 원하는 면접 단계로 이동시켜 대기 지원자 상태를 맞춤
  private void moveApplicationToStage(
      Long recruitmentId, Long applicantId, Long recruitmentStageId) {
    applicationRepository.findByRecruitmentIdAndStatus(recruitmentId, EntityStatus.ACTIVE).stream()
        .filter(application -> applicantId.equals(application.getApplicantId()))
        .findFirst()
        .ifPresent(
            application -> {
              application.updateRecruitmentProcessId(recruitmentStageId);
              applicationRepository.save(application);
            });
  }

  // 이번 주 일요일 시작일을 계산
  private LocalDate currentWeekStart() {
    LocalDate today = LocalDate.now();
    return today.minusDays(today.getDayOfWeek().getValue() % 7L);
  }

  /*
   * 더미 채용 공고와 면접 단계 식별자를 함께 보관
   * */
  private record RecruitmentSeed(
      Long recruitmentId,
      Long applicationTemplateId,
      Long firstStageId,
      Long firstInterviewStageId,
      Long finalInterviewStageId) {}
}

package com.coffiness.calfit.core.api.config;

import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentStageRequest;
import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.core.enums.RecruitmentStageType;
import com.coffiness.calfit.domain.announcementBoard.AnnouncementBoardService;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomService;
import com.coffiness.calfit.domain.recruitment.RecruitmentService;
import com.coffiness.calfit.domain.user.User;
import com.coffiness.calfit.domain.user.UserService;
import com.coffiness.calfit.domain.workspace.Workspace;
import com.coffiness.calfit.domain.workspace.WorkspaceService;
import com.coffiness.calfit.domain.workspace.group.GroupInfo;
import com.coffiness.calfit.domain.workspace.group.GroupService;
import com.coffiness.calfit.domain.workspace.member.MemberService;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.user.UserEntity;
import com.coffiness.calfit.storage.db.core.user.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("local")
@Order(0)
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

  private static final String ADMIN_EMAIL = "admin@coffiiness.com";
  private static final String ADMIN_PASSWORD = "admin1234!";

  private static final String HR_EMAIL = "hr@coffiiness.com";
  private static final String HR_PASSWORD = "hr1234!";

  private static final String MEMBER_EMAIL = "member@coffiiness.com";
  private static final String MEMBER_PASSWORD = "member1234!";
  private static final List<String> DEFAULT_MEETING_ROOM_FACILITIES = List.of("WiFi");
  private static final String DEFAULT_MEETING_ROOM_COLOR = "#10B981";

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserService userService;
  private final WorkspaceService workspaceService;
  private final MemberService memberService;
  private final GroupService groupService;
  private final MeetingRoomService meetingRoomService;
  private final AnnouncementBoardService announcementBoardService;
  private final RecruitmentService recruitmentService;

  @Override
  public void run(ApplicationArguments args) {
    if (userRepository.existsByEmail(ADMIN_EMAIL)) {
      log.info("[DataInitializer] 이미 초기화됨");
      return;
    }

    // 유저 생성
    UserEntity adminEntity =
        UserEntity.createAdmin(ADMIN_EMAIL, passwordEncoder.encode(ADMIN_PASSWORD), "관리자");
    userRepository.save(adminEntity);
    User hrUser = userService.signUp(HR_EMAIL, HR_PASSWORD, "김인사");
    User member = userService.signUp(MEMBER_EMAIL, MEMBER_PASSWORD, "이직원");
    log.info("[DataInitializer] 유저 생성 완료");

    // 워크스페이스 생성
    Workspace workspace = workspaceService.createWorkspace("코피니스", "010-1234-5678", "10-50");
    log.info("[DataInitializer] 워크스페이스 생성 완료: {}", workspace.workspaceId());

    // 데이터 생성
    TenantContext.setTenantId(workspace.workspaceId());
    try {
      initMembers(adminEntity.getId(), hrUser, member);
      InitialGroups groups = initGroups();
      initMemberGroups(adminEntity.getId(), hrUser.id(), member.id(), groups);
      initMeetingRooms(hrUser);
      initAnnouncements(hrUser);
      initRecruitments(hrUser, member, groups.devGroup());
    } finally {
      TenantContext.clear();
    }

    log.info("[DataInitializer] 초기 데이터 생성 완료");
  }

  private void initMembers(Long adminId, User hrUser, User member) {
    memberService.registerMember(adminId, MemberType.HR);
    memberService.registerMember(hrUser.id(), MemberType.HR);
    memberService.registerMember(member.id(), MemberType.INTERVIEWER);
    log.info("[DataInitializer] 멤버 등록 완료");
  }

  private InitialGroups initGroups() {
    GroupInfo devGroup = groupService.createGroup("개발팀", "#3B82F6");
    GroupInfo hrGroup = groupService.createGroup("인사팀", "#10B981");
    groupService.createGroup("마케팅팀", "#F59E0B");
    log.info("[DataInitializer] 그룹 생성 완료");
    return new InitialGroups(devGroup, hrGroup);
  }

  // 로컬 기본 멤버를 조직 구조에 맞는 그룹에 배정
  private void initMemberGroups(
      Long adminUserId, Long hrUserId, Long interviewerUserId, InitialGroups groups) {
    assignMemberGroup(adminUserId, groups.hrGroup().id());
    assignMemberGroup(hrUserId, groups.hrGroup().id());
    assignMemberGroup(interviewerUserId, groups.devGroup().id());
    log.info("[DataInitializer] 멤버 그룹 배정 완료");
  }

  // userId 기준으로 워크스페이스 멤버를 찾아 그룹을 배정
  private void assignMemberGroup(Long userId, Long groupId) {
    memberService.assignGroup(memberService.getMember(userId).id(), groupId);
  }

  private void initMeetingRooms(User hrUser) {
    meetingRoomService.create(
        "대회의실",
        3,
        10,
        "면접과 팀 미팅용 대회의실",
        DEFAULT_MEETING_ROOM_FACILITIES,
        DEFAULT_MEETING_ROOM_COLOR,
        hrUser.id());
    meetingRoomService.create(
        "소회의실 A",
        2,
        4,
        "소규모 미팅용 회의실",
        DEFAULT_MEETING_ROOM_FACILITIES,
        DEFAULT_MEETING_ROOM_COLOR,
        hrUser.id());
    meetingRoomService.create(
        "소회의실 B",
        2,
        4,
        "소규모 미팅용 회의실",
        DEFAULT_MEETING_ROOM_FACILITIES,
        DEFAULT_MEETING_ROOM_COLOR,
        hrUser.id());
    log.info("[DataInitializer] 회의실 생성 완료");
  }

  private void initAnnouncements(User hrUser) {
    announcementBoardService.create("2026년 상반기 채용 일정 안내", "상반기 채용 일정입니다.", true, hrUser.id());
    announcementBoardService.create("사내 복지 제도 변경 안내", "복지 제도가 변경되었습니다.", false, hrUser.id());
    announcementBoardService.create("회의실 이용 규칙", "회의실 예약 후 사용해주세요.", false, hrUser.id());
    log.info("[DataInitializer] 공지사항 생성 완료");
  }

  private void initRecruitments(User hrUser, User interviewer, GroupInfo leadGroup) {
    LocalDateTime now = LocalDateTime.now();

    recruitmentService.createRecruitment(
        hrUser.id(),
        new RecruitmentCreateRequest(
            "백엔드 개발자 채용",
            3,
            1L,
            "Spring Boot 기반 백엔드 개발자를 모집합니다.",
            now.plusDays(1),
            now.plusDays(30),
            CareerType.EXPERIENCED,
            2,
            5,
            leadGroup.id(),
            List.of(),
            List.of(interviewer.id()),
            List.of(
                new RecruitmentStageRequest("서류 심사", RecruitmentStageType.DOCUMENT, 1),
                new RecruitmentStageRequest("1차 면접", RecruitmentStageType.INTERVIEW, 2),
                new RecruitmentStageRequest("2차 면접", RecruitmentStageType.INTERVIEW, 3))));

    recruitmentService.createRecruitment(
        hrUser.id(),
        new RecruitmentCreateRequest(
            "프론트엔드 개발자 채용",
            2,
            1L,
            "React 기반 프론트엔드 개발자를 모집합니다.",
            now.plusDays(7),
            now.plusDays(45),
            CareerType.IRRELEVANT,
            null,
            null,
            leadGroup.id(),
            List.of(),
            List.of(interviewer.id()),
            List.of(
                new RecruitmentStageRequest("서류 심사", RecruitmentStageType.DOCUMENT, 1),
                new RecruitmentStageRequest("코딩 테스트", RecruitmentStageType.TEST, 2),
                new RecruitmentStageRequest("최종 면접", RecruitmentStageType.INTERVIEW, 3))));

    log.info("[DataInitializer] 채용 공고 생성 완료");
  }

  private record InitialGroups(GroupInfo devGroup, GroupInfo hrGroup) {}
}

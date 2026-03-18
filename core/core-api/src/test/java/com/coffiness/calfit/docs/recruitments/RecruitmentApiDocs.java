package com.coffiness.calfit.docs.recruitments;

import static com.coffiness.calfit.docs.RestDocsUtils.responsePreprocessor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentInterviewersUpdateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentStageRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentUpdateRequest;
import com.coffiness.calfit.core.api.controller.v1.recruitment.RecruitmentController;
import com.coffiness.calfit.core.api.facade.recruitment.RecruitmentFacade;
import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.RecruitmentStageType;
import com.coffiness.calfit.core.enums.RecruitmentStatus;
import com.coffiness.calfit.docs.RestDocsTest;
import com.coffiness.calfit.domain.interview.InterviewScheduleCalendarItem;
import com.coffiness.calfit.domain.interview.InterviewerInfo;
import com.coffiness.calfit.domain.interview.WeeklyInterviewScheduleItem;
import com.coffiness.calfit.domain.recruitment.RecruitmentDetailInfo;
import com.coffiness.calfit.domain.recruitment.RecruitmentListInfo;
import com.coffiness.calfit.domain.recruitment.RecruitmentStage;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class RecruitmentApiDocs extends RestDocsTest {

  private final RecruitmentFacade recruitmentFacade = mock(RecruitmentFacade.class);
  private final RecruitmentController recruitmentController =
      new RecruitmentController(recruitmentFacade);
  private final SecurityUser mockSecurityUser = new SecurityUser(1L, "hr@calfit.com", "HR");

  @BeforeEach
  @Override
  public void setUp(RestDocumentationContextProvider restDocumentation) {
    super.setUp(restDocumentation);
    setUpMockMvc(
        recruitmentController,
        restDocumentation,
        new HandlerMethodArgumentResolver() {
          @Override
          public boolean supportsParameter(MethodParameter parameter) {
            return parameter.getParameterType().equals(SecurityUser.class);
          }

          @Override
          public Object resolveArgument(
              MethodParameter parameter,
              ModelAndViewContainer mavContainer,
              NativeWebRequest webRequest,
              WebDataBinderFactory binderFactory) {
            return mockSecurityUser;
          }
        },
        new PageableHandlerMethodArgumentResolver());
  }

  @Test
  void 채용공고_생성_API_문서() throws Exception {
    when(recruitmentFacade.createRecruitment(anyLong(), any(RecruitmentCreateRequest.class)))
        .thenReturn(101L);

    mockMvc
        .perform(
            post("/api/v1/recruitments")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON)
                .content(toJson(mockCreateRequest())))
        .andExpect(status().isCreated())
        .andDo(
            document(
                "recruitments/create",
                responsePreprocessor(),
                requestFields(recruitmentRequestFields()),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data").description("생성된 채용 공고 ID"),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 채용공고_목록_조회_API_문서() throws Exception {
    when(recruitmentFacade.getRecruitmentList(anyLong(), any(), any()))
        .thenReturn(List.of(mockRecruitmentListInfo()));

    mockMvc
        .perform(
            get("/api/v1/recruitments")
                .queryParam("recruitmentStatus", "OPEN")
                .queryParam("page", "0")
                .queryParam("size", "20")
                .queryParam("sort", "createdAt,DESC")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "recruitments/list",
                responsePreprocessor(),
                queryParameters(
                    parameterWithName("recruitmentStatus")
                        .optional()
                        .description("조회할 채용 공고 상태 (`DRAFT`, `OPEN`, `CLOSED`)"),
                    parameterWithName("page").optional().description("페이지 번호 (0부터 시작)"),
                    parameterWithName("size").optional().description("페이지 크기"),
                    parameterWithName("sort")
                        .optional()
                        .description("정렬 기준 (`createdAt,DESC` 형식)")),
                responseFields(recruitmentListResponseFields())));
  }

  @Test
  void 채용공고_상세_조회_API_문서() throws Exception {
    when(recruitmentFacade.getRecruitmentDetail(anyLong(), anyLong()))
        .thenReturn(mockRecruitmentDetailInfo());

    mockMvc
        .perform(
            get("/api/v1/recruitments/{recruitmentId}", 101L)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "recruitments/detail",
                responsePreprocessor(),
                pathParameters(parameterWithName("recruitmentId").description("조회할 채용 공고 ID")),
                responseFields(recruitmentDetailResponseFields())));
  }

  @Test
  void 채용공고_월간_면접일정_조회_API_문서() throws Exception {
    when(recruitmentFacade.getInterviewSchedule(anyLong(), anyLong(), anyString()))
        .thenReturn(List.of(mockInterviewScheduleItem()));

    mockMvc
        .perform(
            get("/api/v1/recruitments/{recruitmentId}/interview-schedules", 101L)
                .queryParam("yearMonth", "2026-03")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "recruitments/interview-schedules",
                responsePreprocessor(),
                pathParameters(parameterWithName("recruitmentId").description("조회할 채용 공고 ID")),
                queryParameters(parameterWithName("yearMonth").description("조회할 연월 (`yyyy-MM`)")),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data[].id").description("면접 일정 ID"),
                    fieldWithPath("data[].startAt").description("면접 시작 일시"),
                    fieldWithPath("data[].endAt").description("면접 종료 일시"),
                    fieldWithPath("data[].meetingRoomId").description("회의실 ID"),
                    fieldWithPath("data[].interviewerUserId").description("면접관 사용자 ID"),
                    fieldWithPath("data[].interviewerName").description("면접관 이름"),
                    fieldWithPath("data[].title").description("일정 제목"),
                    fieldWithPath("data[].applicantName").description("지원자 이름"),
                    fieldWithPath("data[].description").description("일정 설명"),
                    fieldWithPath("data[].location").description("면접 장소"),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 채용공고_주간_면접일정_조회_API_문서() throws Exception {
    when(recruitmentFacade.getWeeklyInterviewSchedules(anyLong(), any(LocalDate.class)))
        .thenReturn(List.of(mockWeeklyInterviewScheduleItem()));

    mockMvc
        .perform(
            get("/api/v1/recruitments/weekly-interview-schedules")
                .queryParam("date", "2026-03-23")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "recruitments/weekly-interview-schedules",
                responsePreprocessor(),
                queryParameters(parameterWithName("date").description("이번 주 기준 날짜 (`yyyy-MM-dd`)")),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data[].id").description("면접 일정 ID"),
                    fieldWithPath("data[].recruitmentId").description("채용 공고 ID"),
                    fieldWithPath("data[].startAt").description("면접 시작 일시"),
                    fieldWithPath("data[].endAt").description("면접 종료 일시"),
                    fieldWithPath("data[].interviewerUserId").description("면접관 사용자 ID"),
                    fieldWithPath("data[].interviewerName").description("면접관 이름"),
                    fieldWithPath("data[].title").description("일정 제목"),
                    fieldWithPath("data[].applicantName").description("지원자 이름"),
                    fieldWithPath("data[].description").description("일정 설명"),
                    fieldWithPath("data[].location").description("면접 장소"),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 채용공고_수정_API_문서() throws Exception {
    when(recruitmentFacade.updateRecruitment(
            anyLong(), anyLong(), any(RecruitmentUpdateRequest.class)))
        .thenReturn(mockRecruitmentDetailInfo());

    mockMvc
        .perform(
            put("/api/v1/recruitments/{recruitmentId}", 101L)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON)
                .content(toJson(mockUpdateRequest())))
        .andExpect(status().isOk())
        .andDo(
            document(
                "recruitments/update",
                responsePreprocessor(),
                pathParameters(parameterWithName("recruitmentId").description("수정할 채용 공고 ID")),
                requestFields(recruitmentRequestFields()),
                responseFields(recruitmentDetailResponseFields())));
  }

  @Test
  void 채용공고_면접관_수정_API_문서() throws Exception {
    when(recruitmentFacade.updateRecruitmentInterviewers(
            anyLong(), anyLong(), any(RecruitmentInterviewersUpdateRequest.class)))
        .thenReturn(mockRecruitmentDetailInfo());

    RecruitmentInterviewersUpdateRequest request =
        new RecruitmentInterviewersUpdateRequest(List.of(7L, 8L, 9L));

    mockMvc
        .perform(
            patch("/api/v1/recruitments/{recruitmentId}/interviewers", 101L)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
        .andExpect(status().isOk())
        .andDo(
            document(
                "recruitments/update-interviewers",
                responsePreprocessor(),
                pathParameters(parameterWithName("recruitmentId").description("면접관을 수정할 채용 공고 ID")),
                requestFields(fieldWithPath("interviewerIds").description("변경할 면접관 사용자 ID 목록")),
                responseFields(recruitmentDetailResponseFields())));
  }

  @Test
  void 채용공고_즉시_게시_API_문서() throws Exception {
    when(recruitmentFacade.publishRecruitment(anyLong(), anyLong()))
        .thenReturn(mockRecruitmentDetailInfo());

    mockMvc
        .perform(
            patch("/api/v1/recruitments/{recruitmentId}/publish", 101L)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "recruitments/publish",
                responsePreprocessor(),
                pathParameters(parameterWithName("recruitmentId").description("즉시 게시할 채용 공고 ID")),
                responseFields(recruitmentDetailResponseFields())));
  }

  @Test
  void 채용공고_삭제_API_문서() throws Exception {
    doNothing().when(recruitmentFacade).deleteRecruitment(anyLong(), anyLong());

    mockMvc
        .perform(
            delete("/api/v1/recruitments/{recruitmentId}", 101L)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "recruitments/delete",
                responsePreprocessor(),
                pathParameters(parameterWithName("recruitmentId").description("삭제할 채용 공고 ID")),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data").description("삭제된 채용 공고 ID"),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  private RecruitmentCreateRequest mockCreateRequest() {
    return new RecruitmentCreateRequest(
        "백엔드 개발자 채용",
        3,
        301L,
        "Spring Boot 기반 백엔드 개발자를 찾고 있습니다.",
        LocalDateTime.of(2026, 3, 20, 9, 0),
        LocalDateTime.of(2026, 4, 20, 18, 0),
        CareerType.EXPERIENCED,
        3,
        7,
        11L,
        List.of(21L, 22L),
        List.of(7L, 8L),
        List.of(
            new RecruitmentStageRequest("서류 심사", RecruitmentStageType.DOCUMENT, 1),
            new RecruitmentStageRequest("실무 면접", RecruitmentStageType.INTERVIEW, 2),
            new RecruitmentStageRequest("최종 면접", RecruitmentStageType.INTERVIEW, 3),
            new RecruitmentStageRequest("최종 합격", RecruitmentStageType.PASS, 4)));
  }

  private RecruitmentUpdateRequest mockUpdateRequest() {
    return new RecruitmentUpdateRequest(
        "백엔드 플랫폼 개발자 채용",
        4,
        301L,
        "대규모 트래픽 환경을 다룰 백엔드 개발자를 찾고 있습니다.",
        LocalDateTime.of(2026, 3, 20, 9, 0),
        LocalDateTime.of(2026, 4, 27, 18, 0),
        CareerType.EXPERIENCED,
        3,
        8,
        11L,
        List.of(21L, 22L),
        List.of(7L, 8L, 9L),
        List.of(
            new RecruitmentStageRequest("서류 심사", RecruitmentStageType.DOCUMENT, 1),
            new RecruitmentStageRequest("실무 면접", RecruitmentStageType.INTERVIEW, 2),
            new RecruitmentStageRequest("컬처핏 면접", RecruitmentStageType.INTERVIEW, 3),
            new RecruitmentStageRequest("최종 합격", RecruitmentStageType.PASS, 4)));
  }

  private RecruitmentListInfo mockRecruitmentListInfo() {
    return new RecruitmentListInfo(
        101L,
        "백엔드 개발자 채용",
        "플랫폼팀",
        11L,
        List.of(21L, 22L),
        CareerType.EXPERIENCED,
        3,
        7,
        LocalDateTime.of(2026, 3, 20, 9, 0),
        LocalDateTime.of(2026, 4, 20, 18, 0),
        RecruitmentStatus.OPEN,
        List.of(
            new RecruitmentListInfo.StageSummaryInfo(1001L, "서류 심사", 12),
            new RecruitmentListInfo.StageSummaryInfo(1002L, "실무 면접", 5),
            new RecruitmentListInfo.StageSummaryInfo(1003L, "최종 면접", 2)),
        List.of(
            new RecruitmentListInfo.AssigneeSummaryInfo(7L, "이직원"),
            new RecruitmentListInfo.AssigneeSummaryInfo(8L, "김인사")));
  }

  private RecruitmentDetailInfo mockRecruitmentDetailInfo() {
    return new RecruitmentDetailInfo(
        101L,
        "백엔드 개발자 채용",
        "Spring Boot 기반 대규모 트래픽 환경을 다룰 백엔드 개발자를 찾고 있습니다.",
        "플랫폼팀",
        11L,
        List.of(21L, 22L),
        3,
        301L,
        CareerType.EXPERIENCED,
        3,
        7,
        LocalDateTime.of(2026, 3, 20, 9, 0),
        LocalDateTime.of(2026, 4, 20, 18, 0),
        27,
        5,
        35,
        "https://app.calfit.com/recruitments/101/share",
        RecruitmentStatus.OPEN,
        List.of(new InterviewerInfo(7L, "이직원", true), new InterviewerInfo(8L, "김인사", true)),
        List.of(
            new RecruitmentStage(1001L, "서류 심사", 1, RecruitmentStageType.DOCUMENT),
            new RecruitmentStage(1002L, "실무 면접", 2, RecruitmentStageType.INTERVIEW),
            new RecruitmentStage(1003L, "최종 면접", 3, RecruitmentStageType.INTERVIEW),
            new RecruitmentStage(1004L, "최종 합격", 4, RecruitmentStageType.PASS)));
  }

  private InterviewScheduleCalendarItem mockInterviewScheduleItem() {
    return new InterviewScheduleCalendarItem(
        9001L,
        LocalDateTime.of(2026, 3, 25, 14, 0),
        LocalDateTime.of(2026, 3, 25, 15, 0),
        501L,
        7L,
        "이직원",
        "백엔드 개발자 채용 · 실무 면접",
        "지원자 김하나",
        "1차 기술 면접",
        "소회의실 A");
  }

  private WeeklyInterviewScheduleItem mockWeeklyInterviewScheduleItem() {
    return new WeeklyInterviewScheduleItem(
        9001L,
        101L,
        LocalDateTime.of(2026, 3, 25, 14, 0),
        LocalDateTime.of(2026, 3, 25, 15, 0),
        7L,
        "이직원",
        "백엔드 개발자 채용",
        "지원자 김하나",
        "1차 기술 면접",
        "소회의실 A");
  }

  private FieldDescriptor[] recruitmentRequestFields() {
    return new FieldDescriptor[] {
      fieldWithPath("title").description("채용 공고 제목"),
      fieldWithPath("targetCount").description("채용 인원"),
      fieldWithPath("applicationTemplateId").description("지원서 템플릿 ID"),
      fieldWithPath("contents").description("채용 공고 설명"),
      fieldWithPath("startDate").description("채용 시작 일시"),
      fieldWithPath("endDate").description("채용 마감 일시"),
      fieldWithPath("careerType").description("경력 구분 (`NEW`, `EXPERIENCED`, `IRRELEVANT`)"),
      fieldWithPath("minExperienceYears").description("최소 경력 연차").optional(),
      fieldWithPath("maxExperienceYears").description("최대 경력 연차").optional(),
      fieldWithPath("leadGroupId").description("담당 조직 ID"),
      fieldWithPath("referenceGroupIds").description("참조 조직 ID 목록").optional(),
      fieldWithPath("interviewerIds").description("면접관 사용자 ID 목록"),
      fieldWithPath("stages").description("채용 단계 목록"),
      fieldWithPath("stages[].stageName").description("단계 이름"),
      fieldWithPath("stages[].stageType")
          .description("단계 유형 (`DOCUMENT`, `INTERVIEW`, `TEST`, `OFFER`, `PASS`, `FAIL`)"),
      fieldWithPath("stages[].stageStep").description("단계 순서")
    };
  }

  private FieldDescriptor[] recruitmentListResponseFields() {
    return new FieldDescriptor[] {
      fieldWithPath("result").description("결과 상태"),
      fieldWithPath("data[].id").description("채용 공고 ID"),
      fieldWithPath("data[].title").description("채용 공고 제목"),
      fieldWithPath("data[].leadGroupName").description("담당 조직 이름"),
      fieldWithPath("data[].leadGroupId").description("담당 조직 ID"),
      fieldWithPath("data[].referenceGroupIds").description("참조 조직 ID 목록"),
      fieldWithPath("data[].careerType").description("경력 구분"),
      fieldWithPath("data[].minExperienceYears").description("최소 경력 연차").optional(),
      fieldWithPath("data[].maxExperienceYears").description("최대 경력 연차").optional(),
      fieldWithPath("data[].startDate").description("채용 시작 일시"),
      fieldWithPath("data[].endDate").description("채용 마감 일시"),
      fieldWithPath("data[].status").description("채용 상태"),
      fieldWithPath("data[].stages[].id").description("채용 단계 ID"),
      fieldWithPath("data[].stages[].stageName").description("채용 단계 이름"),
      fieldWithPath("data[].stages[].applicantCount").description("단계별 지원자 수"),
      fieldWithPath("data[].assignees[].userId").description("담당자 사용자 ID"),
      fieldWithPath("data[].assignees[].name").description("담당자 이름"),
      fieldWithPath("error").description("에러 정보").optional()
    };
  }

  private FieldDescriptor[] recruitmentDetailResponseFields() {
    return new FieldDescriptor[] {
      fieldWithPath("result").description("결과 상태"),
      fieldWithPath("data.id").description("채용 공고 ID"),
      fieldWithPath("data.title").description("채용 공고 제목"),
      fieldWithPath("data.contents").description("채용 공고 설명"),
      fieldWithPath("data.leadGroupName").description("담당 조직 이름"),
      fieldWithPath("data.leadGroupId").description("담당 조직 ID"),
      fieldWithPath("data.referenceGroupIds").description("참조 조직 ID 목록"),
      fieldWithPath("data.targetCount").description("채용 인원"),
      fieldWithPath("data.applicationTemplateId").description("지원서 템플릿 ID"),
      fieldWithPath("data.careerType").description("경력 구분"),
      fieldWithPath("data.minExperienceYears").description("최소 경력 연차").optional(),
      fieldWithPath("data.maxExperienceYears").description("최대 경력 연차").optional(),
      fieldWithPath("data.startDate").description("채용 시작 일시"),
      fieldWithPath("data.endDate").description("채용 마감 일시"),
      fieldWithPath("data.totalApplicants").description("전체 지원자 수"),
      fieldWithPath("data.processingInterview").description("진행 중 면접 수"),
      fieldWithPath("data.dDay").description("마감까지 남은 일수"),
      fieldWithPath("data.shareUrl").description("공고 공유 URL"),
      fieldWithPath("data.recruitmentStatus").description("채용 상태"),
      fieldWithPath("data.interviewers[].userId").description("면접관 사용자 ID"),
      fieldWithPath("data.interviewers[].name").description("면접관 이름"),
      fieldWithPath("data.interviewers[].enabled").description("활성화 여부"),
      fieldWithPath("data.stages[].id").description("채용 단계 ID"),
      fieldWithPath("data.stages[].stageName").description("채용 단계 이름"),
      fieldWithPath("data.stages[].stageStep").description("채용 단계 순서"),
      fieldWithPath("data.stages[].stageType").description("채용 단계 유형"),
      fieldWithPath("error").description("에러 정보").optional()
    };
  }
}

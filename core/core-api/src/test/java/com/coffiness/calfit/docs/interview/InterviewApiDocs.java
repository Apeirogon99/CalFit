package com.coffiness.calfit.docs.interview;

import static com.coffiness.calfit.docs.RestDocsUtils.responsePreprocessor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coffiness.calfit.api.v1.request.InterviewCreateRequest;
import com.coffiness.calfit.core.api.controller.v1.InterviewController;
import com.coffiness.calfit.core.api.facade.interview.InterviewFacade;
import com.coffiness.calfit.core.enums.InterviewRound;
import com.coffiness.calfit.docs.RestDocsTest;
import com.coffiness.calfit.domain.interview.Interview;
import com.coffiness.calfit.domain.interview.InterviewAvailability;
import com.coffiness.calfit.domain.interview.PendingInterviewApplicant;
import com.coffiness.calfit.domain.interview.PendingInterviewStage;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class InterviewApiDocs extends RestDocsTest {

  private final InterviewFacade interviewFacade = mock(InterviewFacade.class);
  private final InterviewController interviewController = new InterviewController(interviewFacade);
  private final SecurityUser mockSecurityUser = new SecurityUser(1L, "hr@calfit.com", "HR");

  @BeforeEach
  @Override
  public void setUp(RestDocumentationContextProvider restDocumentation) {
    super.setUp(restDocumentation);
    setUpMockMvc(
        interviewController,
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
        });
  }

  @Test
  void 면접_가용시간_조회_API_문서() throws Exception {
    when(interviewFacade.getAvailability(anyLong(), any(LocalDateTime.class), any(), any(), any()))
        .thenReturn(mockInterviewAvailability());

    mockMvc
        .perform(
            get("/api/v1/interviews/availability")
                .queryParam("from", "2026-03-18T09:00:00")
                .queryParam("meetingRoomIds", "31", "32")
                .queryParam("interviewerIds", "7", "8")
                .queryParam("applicantIds", "101", "102")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "interviews/availability",
                responsePreprocessor(),
                queryParameters(
                    parameterWithName("from").description("조회 시작 일시"),
                    parameterWithName("meetingRoomIds").optional().description("조회할 회의실 ID 목록"),
                    parameterWithName("interviewerIds").optional().description("조회할 면접관 사용자 ID 목록"),
                    parameterWithName("applicantIds").optional().description("조회할 지원자 ID 목록")),
                responseFields(interviewAvailabilityResponseFields())));
  }

  @Test
  void 면접_대기_지원자_조회_API_문서() throws Exception {
    when(interviewFacade.getPendingInterviewStages(anyLong(), anyLong()))
        .thenReturn(List.of(mockPendingStage()));

    mockMvc
        .perform(
            get("/api/v1/interviews/pending-applicants")
                .queryParam("recruitmentId", "101")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "interviews/pending-applicants",
                responsePreprocessor(),
                queryParameters(parameterWithName("recruitmentId").description("채용 공고 ID")),
                responseFields(interviewPendingApplicantsResponseFields())));
  }

  @Test
  void 면접_생성_API_문서() throws Exception {
    when(interviewFacade.createInterview(anyLong(), any(InterviewCreateRequest.class)))
        .thenReturn(mockInterview());

    mockMvc
        .perform(
            post("/api/v1/interviews")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON)
                .content(toJson(mockInterviewCreateRequest())))
        .andExpect(status().isOk())
        .andDo(
            document(
                "interviews/create",
                responsePreprocessor(),
                requestFields(interviewCreateRequestFields()),
                responseFields(interviewResponseFields())));
  }

  private InterviewCreateRequest mockInterviewCreateRequest() {
    return new InterviewCreateRequest(
        101L,
        1002L,
        InterviewRound.FIRST,
        List.of(7L, 8L),
        List.of(101L),
        31L,
        LocalDateTime.of(2026, 3, 18, 14, 0),
        60,
        "플랫폼팀 백엔드 1차 실무 면접");
  }

  private Interview mockInterview() {
    return Interview.confirmed(
        9001L,
        101L,
        1002L,
        InterviewRound.FIRST,
        31L,
        LocalDateTime.of(2026, 3, 18, 14, 0),
        60,
        "플랫폼팀 백엔드 1차 실무 면접",
        List.of(7L, 8L),
        List.of(101L));
  }

  private InterviewAvailability mockInterviewAvailability() {
    return new InterviewAvailability(
        List.of(
            new InterviewAvailability.MeetingRoomBusySlot(
                31L,
                9001L,
                LocalDateTime.of(2026, 3, 18, 14, 0),
                LocalDateTime.of(2026, 3, 18, 15, 0))),
        List.of(
            new InterviewAvailability.InterviewerBusySlot(
                7L,
                9001L,
                LocalDateTime.of(2026, 3, 18, 14, 0),
                LocalDateTime.of(2026, 3, 18, 15, 0))),
        List.of(
            new InterviewAvailability.ApplicantBusySlot(
                101L,
                9001L,
                LocalDateTime.of(2026, 3, 18, 14, 0),
                LocalDateTime.of(2026, 3, 18, 15, 0))));
  }

  private PendingInterviewStage mockPendingStage() {
    return new PendingInterviewStage(
        1002L,
        "실무 면접",
        2,
        List.of(
            new PendingInterviewApplicant(5001L, 101L, "최지원", "applicant1@calfit.com"),
            new PendingInterviewApplicant(5002L, 102L, "김후보", "applicant2@calfit.com")));
  }

  private FieldDescriptor[] interviewCreateRequestFields() {
    return new FieldDescriptor[] {
      fieldWithPath("recruitmentId").description("채용 공고 ID"),
      fieldWithPath("recruitmentStageId").description("채용 단계 ID"),
      fieldWithPath("round").description("면접 차수 (`FIRST`, `SECOND`)"),
      fieldWithPath("interviewerIds").description("면접관 사용자 ID 목록"),
      fieldWithPath("applicantIds").description("지원자 ID 목록"),
      fieldWithPath("meetingRoomId").description("회의실 ID"),
      fieldWithPath("scheduledAt").description("면접 시작 일시"),
      fieldWithPath("durationMinutes").description("면접 진행 시간(분)"),
      fieldWithPath("memo").description("면접 메모").optional()
    };
  }

  private FieldDescriptor[] interviewResponseFields() {
    return new FieldDescriptor[] {
      fieldWithPath("result").description("결과 상태"),
      fieldWithPath("data.id").description("면접 일정 ID"),
      fieldWithPath("data.recruitmentId").description("채용 공고 ID"),
      fieldWithPath("data.recruitmentStageId").description("채용 단계 ID"),
      fieldWithPath("data.round").description("면접 차수"),
      fieldWithPath("data.meetingRoomId").description("회의실 ID"),
      fieldWithPath("data.scheduledAt").description("면접 시작 일시"),
      fieldWithPath("data.durationMinutes").description("면접 진행 시간(분)"),
      fieldWithPath("data.memo").description("면접 메모").optional(),
      fieldWithPath("data.status").description("면접 상태"),
      fieldWithPath("data.interviewerIds").description("면접관 사용자 ID 목록"),
      fieldWithPath("data.applicantIds").description("지원자 ID 목록"),
      fieldWithPath("error").description("에러 정보").optional()
    };
  }

  private FieldDescriptor[] interviewAvailabilityResponseFields() {
    return new FieldDescriptor[] {
      fieldWithPath("result").description("결과 상태"),
      fieldWithPath("data.meetingRoomBusySlots[].meetingRoomId").description("회의실 ID"),
      fieldWithPath("data.meetingRoomBusySlots[].interviewScheduleId")
          .description("면접 일정 ID")
          .optional(),
      fieldWithPath("data.meetingRoomBusySlots[].start").description("점유 시작 일시"),
      fieldWithPath("data.meetingRoomBusySlots[].end").description("점유 종료 일시"),
      fieldWithPath("data.interviewerBusySlots[].interviewerId").description("면접관 사용자 ID"),
      fieldWithPath("data.interviewerBusySlots[].interviewScheduleId")
          .description("면접 일정 ID")
          .optional(),
      fieldWithPath("data.interviewerBusySlots[].start").description("점유 시작 일시"),
      fieldWithPath("data.interviewerBusySlots[].end").description("점유 종료 일시"),
      fieldWithPath("data.applicantBusySlots[].applicantId").description("지원자 ID"),
      fieldWithPath("data.applicantBusySlots[].interviewScheduleId")
          .description("면접 일정 ID")
          .optional(),
      fieldWithPath("data.applicantBusySlots[].start").description("점유 시작 일시"),
      fieldWithPath("data.applicantBusySlots[].end").description("점유 종료 일시"),
      fieldWithPath("error").description("에러 정보").optional()
    };
  }

  private FieldDescriptor[] interviewPendingApplicantsResponseFields() {
    return new FieldDescriptor[] {
      fieldWithPath("result").description("결과 상태"),
      fieldWithPath("data[].recruitmentStageId").description("채용 단계 ID"),
      fieldWithPath("data[].stageName").description("채용 단계 이름"),
      fieldWithPath("data[].stageStep").description("채용 단계 순서"),
      fieldWithPath("data[].pendingApplicantCount").description("대기 지원자 수"),
      fieldWithPath("data[].applicants[].applicationId").description("지원서 ID"),
      fieldWithPath("data[].applicants[].applicantId").description("지원자 ID"),
      fieldWithPath("data[].applicants[].name").description("지원자 이름"),
      fieldWithPath("data[].applicants[].email").description("지원자 이메일"),
      fieldWithPath("error").description("에러 정보").optional()
    };
  }
}

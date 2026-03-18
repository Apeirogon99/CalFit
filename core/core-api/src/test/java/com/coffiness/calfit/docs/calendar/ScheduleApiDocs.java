package com.coffiness.calfit.docs.calendar;

import static com.coffiness.calfit.docs.RestDocsUtils.responsePreprocessor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coffiness.calfit.core.api.controller.v1.calendar.ScheduleController;
import com.coffiness.calfit.core.api.facade.calendar.ScheduleFacade;
import com.coffiness.calfit.core.enums.ScheduleType;
import com.coffiness.calfit.docs.RestDocsTest;
import com.coffiness.calfit.domain.ScheduleAvailability;
import com.coffiness.calfit.domain.ScheduleDetailInfo;
import com.coffiness.calfit.domain.ScheduleInfo;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import com.coffiness.calfit.v1.request.ScheduleCreateRequest;
import com.coffiness.calfit.v1.request.ScheduleSyncRequest;
import com.coffiness.calfit.v1.request.ScheduleUpdateRequest;
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

public class ScheduleApiDocs extends RestDocsTest {

  private final ScheduleFacade scheduleFacade = mock(ScheduleFacade.class);
  private final ScheduleController scheduleController = new ScheduleController(scheduleFacade);
  private final SecurityUser mockSecurityUser = new SecurityUser(1L, "hr@calfit.com", "HR");

  @BeforeEach
  @Override
  public void setUp(RestDocumentationContextProvider restDocumentation) {
    super.setUp(restDocumentation);
    setUpMockMvc(
        scheduleController,
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
  void 일정_생성_API_문서() throws Exception {
    doNothing().when(scheduleFacade).createSchedule(anyLong(), any(ScheduleCreateRequest.class));

    mockMvc
        .perform(
            post("/api/v1/schedules")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON)
                .content(toJson(mockCreateRequest())))
        .andExpect(status().isCreated())
        .andDo(
            document(
                "schedules/create",
                responsePreprocessor(),
                requestFields(scheduleRequestFields()),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data").description("응답 데이터").optional(),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 일정_동기화_API_문서() throws Exception {
    doNothing().when(scheduleFacade).syncSchedule(anyLong(), any(ScheduleSyncRequest.class));

    ScheduleSyncRequest request =
        new ScheduleSyncRequest(
            "google-event-123",
            "구글 캘린더 일정",
            "외부 캘린더에서 동기화된 일정",
            ScheduleType.MEETING,
            LocalDateTime.of(2026, 3, 25, 10, 0),
            LocalDateTime.of(2026, 3, 25, 11, 0),
            false);

    mockMvc
        .perform(
            post("/api/v1/schedules/sync")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
        .andExpect(status().isOk())
        .andDo(
            document(
                "schedules/sync",
                responsePreprocessor(),
                requestFields(
                    fieldWithPath("googleEventId").description("구글 캘린더 이벤트 ID"),
                    fieldWithPath("title").description("일정 제목"),
                    fieldWithPath("description").description("일정 설명").optional(),
                    fieldWithPath("type").description("일정 타입"),
                    fieldWithPath("startTime").description("시작 일시"),
                    fieldWithPath("endTime").description("종료 일시"),
                    fieldWithPath("isAllDay").description("종일 여부")),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data").description("응답 데이터").optional(),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 일정_목록_조회_API_문서() throws Exception {
    when(scheduleFacade.getSchedules(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(List.of(mockScheduleInfo()));

    mockMvc
        .perform(
            get("/api/v1/schedules")
                .queryParam("startDate", "2026-03-01")
                .queryParam("endDate", "2026-03-31")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "schedules/list",
                responsePreprocessor(),
                queryParameters(
                    parameterWithName("startDate").description("조회 시작일 (`yyyy-MM-dd`)"),
                    parameterWithName("endDate").description("조회 종료일 (`yyyy-MM-dd`)")),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data[].id").description("일정 ID"),
                    fieldWithPath("data[].title").description("일정 제목"),
                    fieldWithPath("data[].date").description("일정 날짜"),
                    fieldWithPath("data[].startTime").description("시작 시각"),
                    fieldWithPath("data[].endTime").description("종료 시각"),
                    fieldWithPath("data[].type").description("일정 타입"),
                    fieldWithPath("data[].description").description("일정 설명").optional(),
                    fieldWithPath("data[].location").description("장소").optional(),
                    fieldWithPath("data[].roomId").description("회의실 ID").optional(),
                    fieldWithPath("data[].isAllDay").description("종일 여부"),
                    fieldWithPath("data[].isBusy").description("바쁨 여부"),
                    fieldWithPath("data[].interviewScheduleId").description("면접 일정 ID").optional(),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 참석자_가용시간_조회_API_문서() throws Exception {
    when(scheduleFacade.getAttendeeAvailability(
            anyLong(), any(LocalDateTime.class), any(LocalDateTime.class), any()))
        .thenReturn(mockAvailability());

    mockMvc
        .perform(
            get("/api/v1/schedules/availability")
                .queryParam("date", "2026-03-25")
                .queryParam("attendeeIds", "7", "8")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "schedules/availability",
                responsePreprocessor(),
                queryParameters(
                    parameterWithName("date").description("조회 날짜 (`yyyy-MM-dd`)"),
                    parameterWithName("attendeeIds").optional().description("참석자 사용자 ID 목록")),
                responseFields(scheduleAvailabilityResponseFields())));
  }

  @Test
  void 참석자_기간별_가용시간_조회_API_문서() throws Exception {
    when(scheduleFacade.getAttendeeAvailability(
            anyLong(), any(LocalDateTime.class), any(LocalDateTime.class), any()))
        .thenReturn(mockAvailability());

    mockMvc
        .perform(
            get("/api/v1/schedules/availability/range")
                .queryParam("startDate", "2026-03-24")
                .queryParam("endDate", "2026-03-26")
                .queryParam("attendeeIds", "7", "8")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "schedules/availability-range",
                responsePreprocessor(),
                queryParameters(
                    parameterWithName("startDate").description("조회 시작일 (`yyyy-MM-dd`)"),
                    parameterWithName("endDate").description("조회 종료일 (`yyyy-MM-dd`)"),
                    parameterWithName("attendeeIds").optional().description("참석자 사용자 ID 목록")),
                responseFields(scheduleAvailabilityResponseFields())));
  }

  @Test
  void 일정_상세_조회_API_문서() throws Exception {
    when(scheduleFacade.getDetailSchedule(anyLong(), anyLong()))
        .thenReturn(mockScheduleDetailInfo());

    mockMvc
        .perform(
            get("/api/v1/schedules/{scheduleId}", 501L)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "schedules/detail",
                responsePreprocessor(),
                pathParameters(parameterWithName("scheduleId").description("조회할 일정 ID")),
                responseFields(scheduleDetailResponseFields())));
  }

  @Test
  void 일정_수정_API_문서() throws Exception {
    when(scheduleFacade.updateSchedule(anyLong(), anyLong(), any(ScheduleUpdateRequest.class)))
        .thenReturn(mockScheduleDetailInfo());

    mockMvc
        .perform(
            put("/api/v1/schedules/{scheduleId}", 501L)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON)
                .content(toJson(mockUpdateRequest())))
        .andExpect(status().isOk())
        .andDo(
            document(
                "schedules/update",
                responsePreprocessor(),
                pathParameters(parameterWithName("scheduleId").description("수정할 일정 ID")),
                requestFields(scheduleRequestFields()),
                responseFields(scheduleDetailResponseFields())));
  }

  @Test
  void 일정_삭제_API_문서() throws Exception {
    doNothing().when(scheduleFacade).deleteSchedule(anyLong(), anyLong());

    mockMvc
        .perform(
            delete("/api/v1/schedules/{scheduleId}", 501L)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "schedules/delete",
                responsePreprocessor(),
                pathParameters(parameterWithName("scheduleId").description("삭제할 일정 ID")),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data").description("응답 데이터").optional(),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  private ScheduleCreateRequest mockCreateRequest() {
    return new ScheduleCreateRequest(
        "실무 면접",
        "백엔드 개발자 1차 면접",
        ScheduleType.INTERVIEW,
        LocalDateTime.of(2026, 3, 25, 14, 0),
        LocalDateTime.of(2026, 3, 25, 15, 0),
        false,
        31L,
        true,
        List.of(7L, 8L));
  }

  private ScheduleUpdateRequest mockUpdateRequest() {
    return new ScheduleUpdateRequest(
        "실무 면접 일정 조정",
        "면접 시간을 30분 뒤로 조정합니다.",
        ScheduleType.INTERVIEW,
        LocalDateTime.of(2026, 3, 25, 14, 30),
        LocalDateTime.of(2026, 3, 25, 15, 30),
        false,
        31L,
        true,
        List.of(7L, 8L, 9L));
  }

  private ScheduleInfo mockScheduleInfo() {
    return new ScheduleInfo(
        501L,
        "실무 면접",
        LocalDateTime.of(2026, 3, 25, 14, 0),
        LocalDateTime.of(2026, 3, 25, 15, 0),
        ScheduleType.INTERVIEW,
        "백엔드 개발자 1차 면접",
        "소회의실 A",
        31L,
        401L,
        false,
        true,
        9001L);
  }

  private ScheduleDetailInfo mockScheduleDetailInfo() {
    return new ScheduleDetailInfo(
        501L,
        "실무 면접",
        LocalDateTime.of(2026, 3, 25, 14, 0),
        LocalDateTime.of(2026, 3, 25, 15, 0),
        ScheduleType.INTERVIEW,
        "백엔드 개발자 1차 면접",
        31L,
        401L,
        false,
        true,
        9001L,
        "소회의실 A",
        "이직원",
        List.of(7L, 8L),
        List.of("이직원", "김인사"),
        "지원자 김하나");
  }

  private ScheduleAvailability mockAvailability() {
    return new ScheduleAvailability(
        List.of(
            new ScheduleAvailability.AttendeeAvailability(
                7L,
                "이직원",
                List.of(
                    new ScheduleAvailability.BusySchedule(
                        501L,
                        "실무 면접",
                        LocalDateTime.of(2026, 3, 25, 14, 0),
                        LocalDateTime.of(2026, 3, 25, 15, 0),
                        false,
                        ScheduleType.INTERVIEW,
                        9001L))),
            new ScheduleAvailability.AttendeeAvailability(
                8L,
                "김인사",
                List.of(
                    new ScheduleAvailability.BusySchedule(
                        601L,
                        "팀 미팅",
                        LocalDateTime.of(2026, 3, 25, 13, 0),
                        LocalDateTime.of(2026, 3, 25, 14, 0),
                        false,
                        ScheduleType.MEETING,
                        null)))));
  }

  private FieldDescriptor[] scheduleRequestFields() {
    return new FieldDescriptor[] {
      fieldWithPath("title").description("일정 제목"),
      fieldWithPath("description").description("일정 설명").optional(),
      fieldWithPath("type")
          .description("일정 타입 (`INTERVIEW`, `MEETING`, `VACATION`, `BUSINESS`, `OTHERS`)"),
      fieldWithPath("startTime").description("시작 일시"),
      fieldWithPath("endTime").description("종료 일시"),
      fieldWithPath("isAllDay").description("종일 여부").optional(),
      fieldWithPath("roomId").description("회의실 ID").optional(),
      fieldWithPath("isBusy").description("바쁨 여부").optional(),
      fieldWithPath("attendeeIds").description("참석자 사용자 ID 목록").optional()
    };
  }

  private FieldDescriptor[] scheduleDetailResponseFields() {
    return new FieldDescriptor[] {
      fieldWithPath("result").description("결과 상태"),
      fieldWithPath("data.id").description("일정 ID"),
      fieldWithPath("data.title").description("일정 제목"),
      fieldWithPath("data.date").description("일정 날짜"),
      fieldWithPath("data.startTime").description("시작 시각"),
      fieldWithPath("data.endTime").description("종료 시각"),
      fieldWithPath("data.type").description("일정 타입"),
      fieldWithPath("data.description").description("일정 설명").optional(),
      fieldWithPath("data.roomId").description("회의실 ID").optional(),
      fieldWithPath("data.isAllDay").description("종일 여부"),
      fieldWithPath("data.isBusy").description("바쁨 여부"),
      fieldWithPath("data.interviewScheduleId").description("면접 일정 ID").optional(),
      fieldWithPath("data.location").description("장소").optional(),
      fieldWithPath("data.ownerName").description("주최자 이름"),
      fieldWithPath("data.attendeeIds").description("참석자 사용자 ID 목록"),
      fieldWithPath("data.attendees").description("참석자 이름 목록"),
      fieldWithPath("data.applicantName").description("지원자 이름").optional(),
      fieldWithPath("error").description("에러 정보").optional()
    };
  }

  private FieldDescriptor[] scheduleAvailabilityResponseFields() {
    return new FieldDescriptor[] {
      fieldWithPath("result").description("결과 상태"),
      fieldWithPath("data.attendeeAvailabilities[].attendeeId").description("참석자 사용자 ID"),
      fieldWithPath("data.attendeeAvailabilities[].attendeeName").description("참석자 이름"),
      fieldWithPath("data.attendeeAvailabilities[].busySchedules[].scheduleId")
          .description("일정 ID"),
      fieldWithPath("data.attendeeAvailabilities[].busySchedules[].title").description("일정 제목"),
      fieldWithPath("data.attendeeAvailabilities[].busySchedules[].startDateTime")
          .description("시작 일시"),
      fieldWithPath("data.attendeeAvailabilities[].busySchedules[].endDateTime")
          .description("종료 일시"),
      fieldWithPath("data.attendeeAvailabilities[].busySchedules[].isAllDay").description("종일 여부"),
      fieldWithPath("data.attendeeAvailabilities[].busySchedules[].type").description("일정 타입"),
      fieldWithPath("data.attendeeAvailabilities[].busySchedules[].interviewScheduleId")
          .description("면접 일정 ID")
          .optional(),
      fieldWithPath("error").description("에러 정보").optional()
    };
  }
}

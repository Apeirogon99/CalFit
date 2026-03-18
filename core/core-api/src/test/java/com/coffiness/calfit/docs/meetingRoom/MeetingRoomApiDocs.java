package com.coffiness.calfit.docs.meetingRoom;

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

import com.coffiness.calfit.api.v1.request.MeetingRoomCreateRequest;
import com.coffiness.calfit.api.v1.request.MeetingRoomReservationCreateRequest;
import com.coffiness.calfit.api.v1.request.MeetingRoomUpdateRequest;
import com.coffiness.calfit.api.v1.response.MeetingRoomReservationResponse;
import com.coffiness.calfit.core.api.controller.v1.MeetingRoomController;
import com.coffiness.calfit.core.api.facade.meetingRoom.MeetingRoomFacade;
import com.coffiness.calfit.core.api.facade.meetingRoom.MeetingRoomReservationFacade;
import com.coffiness.calfit.core.enums.MeetingRoomStatus;
import com.coffiness.calfit.docs.RestDocsTest;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoom;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomReservation;
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

public class MeetingRoomApiDocs extends RestDocsTest {

  private final MeetingRoomFacade meetingRoomFacade = mock(MeetingRoomFacade.class);
  private final MeetingRoomReservationFacade meetingRoomReservationFacade =
      mock(MeetingRoomReservationFacade.class);
  private final MeetingRoomController meetingRoomController =
      new MeetingRoomController(meetingRoomFacade, meetingRoomReservationFacade);
  private final SecurityUser mockSecurityUser = new SecurityUser(1L, "hr@calfit.com", "HR");

  @BeforeEach
  @Override
  public void setUp(RestDocumentationContextProvider restDocumentation) {
    super.setUp(restDocumentation);
    setUpMockMvc(
        meetingRoomController,
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
  void 회의실_생성_API_문서() throws Exception {
    when(meetingRoomFacade.createMeetingRoom(anyLong(), any(MeetingRoomCreateRequest.class)))
        .thenReturn(mockMeetingRoom());

    mockMvc
        .perform(
            post("/api/v1/meeting-rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON)
                .content(toJson(mockMeetingRoomCreateRequest())))
        .andExpect(status().isOk())
        .andDo(
            document(
                "meeting-rooms/create",
                responsePreprocessor(),
                requestFields(meetingRoomRequestFields()),
                responseFields(meetingRoomResponseFields())));
  }

  @Test
  void 회의실_목록_조회_API_문서() throws Exception {
    when(meetingRoomFacade.getMeetingRoomList())
        .thenReturn(List.of(mockMeetingRoom(), mockSecondMeetingRoom()));

    mockMvc
        .perform(
            get("/api/v1/meeting-rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "meeting-rooms/list",
                responsePreprocessor(),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data[].id").description("회의실 ID"),
                    fieldWithPath("data[].name").description("회의실 이름"),
                    fieldWithPath("data[].location").description("회의실 위치(층수)"),
                    fieldWithPath("data[].capacity").description("수용 인원"),
                    fieldWithPath("data[].description").description("회의실 설명").optional(),
                    fieldWithPath("data[].facilities").description("제공 시설 목록"),
                    fieldWithPath("data[].color").description("회의실 색상"),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 회의실_수정_API_문서() throws Exception {
    when(meetingRoomFacade.updateMeetingRoom(
            anyLong(), anyLong(), any(MeetingRoomUpdateRequest.class)))
        .thenReturn(mockUpdatedMeetingRoom());

    mockMvc
        .perform(
            put("/api/v1/meeting-rooms/{id}", 31L)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON)
                .content(toJson(mockMeetingRoomUpdateRequest())))
        .andExpect(status().isOk())
        .andDo(
            document(
                "meeting-rooms/update",
                responsePreprocessor(),
                pathParameters(parameterWithName("id").description("수정할 회의실 ID")),
                requestFields(meetingRoomRequestFields()),
                responseFields(meetingRoomResponseFields())));
  }

  @Test
  void 회의실_삭제_API_문서() throws Exception {
    doNothing().when(meetingRoomFacade).deleteMeetingRoom(anyLong(), anyLong());

    mockMvc
        .perform(
            delete("/api/v1/meeting-rooms/{id}", 31L)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "meeting-rooms/delete",
                responsePreprocessor(),
                pathParameters(parameterWithName("id").description("삭제할 회의실 ID")),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data").description("응답 데이터").optional(),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 회의실_예약_목록_조회_API_문서() throws Exception {
    when(meetingRoomReservationFacade.getActiveReservationResponses(
            any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(List.of(mockReservationResponse(), mockInterviewReservationResponse()));

    mockMvc
        .perform(
            get("/api/v1/meeting-rooms/reservations")
                .queryParam("fromDatetime", "2026-03-16T09:00:00")
                .queryParam("toDatetime", "2026-03-16T18:00:00")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "meeting-rooms/reservations/list",
                responsePreprocessor(),
                queryParameters(
                    parameterWithName("fromDatetime").description("조회 시작 일시"),
                    parameterWithName("toDatetime").description("조회 종료 일시")),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data[].id").description("예약 ID"),
                    fieldWithPath("data[].meetingRoomId").description("회의실 ID"),
                    fieldWithPath("data[].userId").description("예약 생성 사용자 ID"),
                    fieldWithPath("data[].interviewScheduleId")
                        .description("연결된 면접 일정 ID")
                        .optional(),
                    fieldWithPath("data[].title").description("예약 제목"),
                    fieldWithPath("data[].description").description("예약 설명").optional(),
                    fieldWithPath("data[].organizerName").description("예약 주최자 이름").optional(),
                    fieldWithPath("data[].attendees").description("참석자 이름 목록"),
                    fieldWithPath("data[].startDatetime").description("예약 시작 일시"),
                    fieldWithPath("data[].endDatetime").description("예약 종료 일시"),
                    fieldWithPath("data[].status").description("예약 상태"),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 회의실_예약_API_문서() throws Exception {
    MeetingRoomReservation reservation = mockReservation();
    when(meetingRoomReservationFacade.reserveMeetingRoom(
            anyLong(), anyLong(), any(MeetingRoomReservationCreateRequest.class)))
        .thenReturn(reservation);
    when(meetingRoomReservationFacade.toResponse(any(MeetingRoomReservation.class)))
        .thenReturn(mockReservationResponse());

    mockMvc
        .perform(
            post("/api/v1/meeting-rooms/{meetingRoomId}/reservations", 31L)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON)
                .content(toJson(mockReservationCreateRequest())))
        .andExpect(status().isOk())
        .andDo(
            document(
                "meeting-rooms/reservations/create",
                responsePreprocessor(),
                pathParameters(parameterWithName("meetingRoomId").description("예약할 회의실 ID")),
                requestFields(meetingRoomReservationRequestFields()),
                responseFields(meetingRoomReservationResponseFields())));
  }

  @Test
  void 회의실_예약_취소_API_문서() throws Exception {
    when(meetingRoomReservationFacade.cancelReservation(anyLong(), anyLong(), anyLong()))
        .thenReturn(mockCanceledReservation());

    mockMvc
        .perform(
            delete("/api/v1/meeting-rooms/{meetingRoomId}/reservations/{reservationId}", 31L, 501L)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "meeting-rooms/reservations/cancel",
                responsePreprocessor(),
                pathParameters(
                    parameterWithName("meetingRoomId").description("예약이 속한 회의실 ID"),
                    parameterWithName("reservationId").description("취소할 예약 ID")),
                responseFields(meetingRoomReservationResponseFields())));
  }

  private MeetingRoomCreateRequest mockMeetingRoomCreateRequest() {
    return new MeetingRoomCreateRequest(
        "소회의실 A", 7, 8, "면접과 팀 미팅에 사용하는 회의실입니다.", List.of("TV", "화이트보드"), "#4C6EF5");
  }

  private MeetingRoomUpdateRequest mockMeetingRoomUpdateRequest() {
    return new MeetingRoomUpdateRequest(
        "소회의실 A-1", 8, 10, "면접과 내부 회의에 사용하는 회의실입니다.", List.of("TV", "화이트보드", "화상회의"), "#2F9E44");
  }

  private MeetingRoomReservationCreateRequest mockReservationCreateRequest() {
    return new MeetingRoomReservationCreateRequest(
        "백엔드 실무 면접",
        "플랫폼팀 1차 면접",
        LocalDateTime.of(2026, 3, 16, 14, 0),
        LocalDateTime.of(2026, 3, 16, 15, 0),
        List.of(7L, 8L, 21L));
  }

  private MeetingRoom mockMeetingRoom() {
    return new MeetingRoom(
        31L, "소회의실 A", 7, 8, "면접과 팀 미팅에 사용하는 회의실입니다.", List.of("TV", "화이트보드"), "#4C6EF5");
  }

  private MeetingRoom mockSecondMeetingRoom() {
    return new MeetingRoom(
        32L, "대회의실", 10, 20, "전체 미팅과 워크숍에 사용하는 회의실입니다.", List.of("프로젝터", "마이크"), "#F08C00");
  }

  private MeetingRoom mockUpdatedMeetingRoom() {
    return new MeetingRoom(
        31L,
        "소회의실 A-1",
        8,
        10,
        "면접과 내부 회의에 사용하는 회의실입니다.",
        List.of("TV", "화이트보드", "화상회의"),
        "#2F9E44");
  }

  private MeetingRoomReservation mockReservation() {
    return new MeetingRoomReservation(
        501L,
        31L,
        1L,
        null,
        LocalDateTime.of(2026, 3, 16, 14, 0),
        LocalDateTime.of(2026, 3, 16, 15, 0),
        MeetingRoomStatus.RESERVED);
  }

  private MeetingRoomReservation mockCanceledReservation() {
    return new MeetingRoomReservation(
        501L,
        31L,
        1L,
        null,
        LocalDateTime.of(2026, 3, 16, 14, 0),
        LocalDateTime.of(2026, 3, 16, 15, 0),
        MeetingRoomStatus.DELETED);
  }

  private MeetingRoomReservationResponse mockReservationResponse() {
    return new MeetingRoomReservationResponse(
        501L,
        31L,
        1L,
        null,
        "백엔드 실무 면접",
        "플랫폼팀 1차 면접",
        "김인사",
        List.of("이직원", "박면접관", "최지원"),
        LocalDateTime.of(2026, 3, 16, 14, 0),
        LocalDateTime.of(2026, 3, 16, 15, 0),
        MeetingRoomStatus.RESERVED);
  }

  private MeetingRoomReservationResponse mockInterviewReservationResponse() {
    return new MeetingRoomReservationResponse(
        502L,
        32L,
        1L,
        9001L,
        "면접 일정",
        null,
        "김인사",
        List.of("이직원", "최지원"),
        LocalDateTime.of(2026, 3, 16, 16, 0),
        LocalDateTime.of(2026, 3, 16, 17, 0),
        MeetingRoomStatus.ACTIVE);
  }

  private FieldDescriptor[] meetingRoomRequestFields() {
    return new FieldDescriptor[] {
      fieldWithPath("name").description("회의실 이름 (2자 이상 20자 이하)"),
      fieldWithPath("location").description("회의실 위치(층수, 1~100)"),
      fieldWithPath("capacity").description("수용 인원 (2명 이상)"),
      fieldWithPath("description").description("회의실 설명").optional(),
      fieldWithPath("facilities").description("제공 시설 목록"),
      fieldWithPath("color").description("회의실 색상")
    };
  }

  private FieldDescriptor[] meetingRoomResponseFields() {
    return new FieldDescriptor[] {
      fieldWithPath("result").description("결과 상태"),
      fieldWithPath("data.id").description("회의실 ID"),
      fieldWithPath("data.name").description("회의실 이름"),
      fieldWithPath("data.location").description("회의실 위치(층수)"),
      fieldWithPath("data.capacity").description("수용 인원"),
      fieldWithPath("data.description").description("회의실 설명").optional(),
      fieldWithPath("data.facilities").description("제공 시설 목록"),
      fieldWithPath("data.color").description("회의실 색상"),
      fieldWithPath("error").description("에러 정보").optional()
    };
  }

  private FieldDescriptor[] meetingRoomReservationRequestFields() {
    return new FieldDescriptor[] {
      fieldWithPath("title").description("예약 제목").optional(),
      fieldWithPath("description").description("예약 설명").optional(),
      fieldWithPath("startDatetime").description("예약 시작 일시"),
      fieldWithPath("endDatetime").description("예약 종료 일시"),
      fieldWithPath("participantUserIds").description("참여자 사용자 ID 목록").optional()
    };
  }

  private FieldDescriptor[] meetingRoomReservationResponseFields() {
    return new FieldDescriptor[] {
      fieldWithPath("result").description("결과 상태"),
      fieldWithPath("data.id").description("예약 ID"),
      fieldWithPath("data.meetingRoomId").description("회의실 ID"),
      fieldWithPath("data.userId").description("예약 생성 사용자 ID"),
      fieldWithPath("data.interviewScheduleId").description("연결된 면접 일정 ID").optional(),
      fieldWithPath("data.title").description("예약 제목"),
      fieldWithPath("data.description").description("예약 설명").optional(),
      fieldWithPath("data.organizerName").description("예약 주최자 이름").optional(),
      fieldWithPath("data.attendees").description("참석자 이름 목록"),
      fieldWithPath("data.startDatetime").description("예약 시작 일시"),
      fieldWithPath("data.endDatetime").description("예약 종료 일시"),
      fieldWithPath("data.status").description("예약 상태"),
      fieldWithPath("error").description("에러 정보").optional()
    };
  }
}

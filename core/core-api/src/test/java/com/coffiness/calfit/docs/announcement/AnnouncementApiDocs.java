package com.coffiness.calfit.docs.announcement;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coffiness.calfit.api.v1.request.AnnouncementBoardCreateRequest;
import com.coffiness.calfit.api.v1.request.AnnouncementBoardUpdateRequest;
import com.coffiness.calfit.core.api.controller.v1.AnnouncementBoardController;
import com.coffiness.calfit.core.api.facade.announcementBoard.AnnouncementBoardFacade;
import com.coffiness.calfit.docs.RestDocsTest;
import com.coffiness.calfit.domain.announcementBoard.AnnouncementBoard;
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

public class AnnouncementApiDocs extends RestDocsTest {

  private final AnnouncementBoardFacade announcementBoardFacade =
      mock(AnnouncementBoardFacade.class);
  private final AnnouncementBoardController announcementBoardController =
      new AnnouncementBoardController(announcementBoardFacade);
  private final SecurityUser mockSecurityUser = new SecurityUser(1L, "hr@calfit.com", "HR");

  @BeforeEach
  @Override
  public void setUp(RestDocumentationContextProvider restDocumentation) {
    super.setUp(restDocumentation);
    setUpMockMvc(
        announcementBoardController,
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
  void 공지사항_생성_API_문서() throws Exception {
    when(announcementBoardFacade.createAnnouncement(
            anyLong(), any(AnnouncementBoardCreateRequest.class)))
        .thenReturn(mockAnnouncementBoard());

    mockMvc
        .perform(
            post("/api/v1/announcement-boards")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON)
                .content(toJson(mockCreateRequest())))
        .andExpect(status().isCreated())
        .andDo(
            document(
                "announcements/create",
                responsePreprocessor(),
                requestFields(announcementRequestFields()),
                responseFields(announcementResponseFields())));
  }

  @Test
  void 공지사항_목록_조회_API_문서() throws Exception {
    when(announcementBoardFacade.getAnnouncementList())
        .thenReturn(List.of(mockPinnedAnnouncementBoard(), mockAnnouncementBoard()));

    mockMvc
        .perform(
            get("/api/v1/announcement-boards")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "announcements/list",
                responsePreprocessor(),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data[].id").description("공지사항 ID"),
                    fieldWithPath("data[].title").description("공지사항 제목"),
                    fieldWithPath("data[].content").description("공지사항 내용"),
                    fieldWithPath("data[].pinned").description("상단 고정 여부"),
                    fieldWithPath("data[].createdAt").description("공지사항 생성 시각"),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 공지사항_상세_조회_API_문서() throws Exception {
    when(announcementBoardFacade.getAnnouncement(anyLong())).thenReturn(mockAnnouncementBoard());

    mockMvc
        .perform(
            get("/api/v1/announcement-boards/{id}", 101L)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "announcements/detail",
                responsePreprocessor(),
                pathParameters(parameterWithName("id").description("조회할 공지사항 ID")),
                responseFields(announcementResponseFields())));
  }

  @Test
  void 공지사항_수정_API_문서() throws Exception {
    when(announcementBoardFacade.updateAnnouncement(
            anyLong(), anyLong(), any(AnnouncementBoardUpdateRequest.class)))
        .thenReturn(mockUpdatedAnnouncementBoard());

    mockMvc
        .perform(
            put("/api/v1/announcement-boards/{id}", 101L)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON)
                .content(toJson(mockUpdateRequest())))
        .andExpect(status().isOk())
        .andDo(
            document(
                "announcements/update",
                responsePreprocessor(),
                pathParameters(parameterWithName("id").description("수정할 공지사항 ID")),
                requestFields(announcementRequestFields()),
                responseFields(announcementResponseFields())));
  }

  @Test
  void 공지사항_삭제_API_문서() throws Exception {
    doNothing().when(announcementBoardFacade).deleteAnnouncement(anyLong(), anyLong());

    mockMvc
        .perform(
            delete("/api/v1/announcement-boards/{id}", 101L)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "announcements/delete",
                responsePreprocessor(),
                pathParameters(parameterWithName("id").description("삭제할 공지사항 ID")),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data").description("응답 데이터").optional(),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  private AnnouncementBoardCreateRequest mockCreateRequest() {
    return new AnnouncementBoardCreateRequest(
        "3월 채용 프로세스 운영 공지", "이번 주부터 면접 일정 확정은 공지사항 기준으로 운영됩니다.", true);
  }

  private AnnouncementBoardUpdateRequest mockUpdateRequest() {
    return new AnnouncementBoardUpdateRequest(
        "3월 채용 프로세스 운영 공지 수정", "이번 주부터 면접 일정 확정과 공지 확인 절차를 함께 운영합니다.", false);
  }

  private AnnouncementBoard mockAnnouncementBoard() {
    return new AnnouncementBoard(
        101L,
        "3월 채용 프로세스 운영 공지",
        "이번 주부터 면접 일정 확정은 공지사항 기준으로 운영됩니다.",
        false,
        LocalDateTime.of(2026, 3, 16, 9, 0));
  }

  private AnnouncementBoard mockPinnedAnnouncementBoard() {
    return new AnnouncementBoard(
        100L, "긴급 공지", "면접실 예약 정책이 변경되었습니다.", true, LocalDateTime.of(2026, 3, 15, 18, 0));
  }

  private AnnouncementBoard mockUpdatedAnnouncementBoard() {
    return new AnnouncementBoard(
        101L,
        "3월 채용 프로세스 운영 공지 수정",
        "이번 주부터 면접 일정 확정과 공지 확인 절차를 함께 운영합니다.",
        false,
        LocalDateTime.of(2026, 3, 16, 9, 0));
  }

  private FieldDescriptor[] announcementRequestFields() {
    return new FieldDescriptor[] {
      fieldWithPath("title").description("공지사항 제목 (2자 이상 100자 이하)"),
      fieldWithPath("content").description("공지사항 내용 (최대 2000자)"),
      fieldWithPath("pinned").description("상단 고정 여부")
    };
  }

  private FieldDescriptor[] announcementResponseFields() {
    return new FieldDescriptor[] {
      fieldWithPath("result").description("결과 상태"),
      fieldWithPath("data.id").description("공지사항 ID"),
      fieldWithPath("data.title").description("공지사항 제목"),
      fieldWithPath("data.content").description("공지사항 내용"),
      fieldWithPath("data.pinned").description("상단 고정 여부"),
      fieldWithPath("data.createdAt").description("공지사항 생성 시각"),
      fieldWithPath("error").description("에러 정보").optional()
    };
  }
}

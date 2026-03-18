package com.coffiness.calfit.docs.members;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coffiness.calfit.api.v1.request.AssignGroupRequest;
import com.coffiness.calfit.api.v1.request.UpdateMemberRequest;
import com.coffiness.calfit.core.api.controller.v1.MemberController;
import com.coffiness.calfit.core.api.facade.workspace.MemberGroupFacade;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.docs.RestDocsTest;
import com.coffiness.calfit.domain.workspace.member.MemberInfo;
import com.coffiness.calfit.domain.workspace.member.MemberService;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class MemberApiDocs extends RestDocsTest {

  private final MemberService memberService = mock(MemberService.class);
  private final MemberGroupFacade memberGroupFacade = mock(MemberGroupFacade.class);

  private final MemberController memberController =
      new MemberController(memberService, memberGroupFacade);

  private final SecurityUser mockSecurityUser = new SecurityUser(1L, "test@example.com", "USER");

  @BeforeEach
  @Override
  public void setUp(RestDocumentationContextProvider restDocumentation) {
    super.setUp(restDocumentation);
    setUpMockMvc(
        memberController,
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
  void 멤버_목록_조회_API_문서() throws Exception {
    MemberInfo member =
        new MemberInfo(
            1L, 1L, "홍길동", LocalDateTime.now(), LocalDateTime.now(), 1L, "개발팀", MemberType.HR);
    when(memberGroupFacade.getMembers()).thenReturn(List.of(member));

    mockMvc
        .perform(
            get("/api/v1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "members/list",
                responsePreprocessor(),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data[].id").description("멤버 ID"),
                    fieldWithPath("data[].userId").description("유저 ID"),
                    fieldWithPath("data[].name").description("멤버 이름"),
                    fieldWithPath("data[].createdAt").description("생성 시각"),
                    fieldWithPath("data[].recentAt").description("최근 접속 시각"),
                    fieldWithPath("data[].groupId").description("소속 그룹 ID").optional(),
                    fieldWithPath("data[].group").description("소속 그룹명").optional(),
                    fieldWithPath("data[].memberType").description("멤버 유형 (HR, INTERVIEWER)"),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 내_멤버_정보_조회_API_문서() throws Exception {
    MemberInfo member =
        new MemberInfo(
            1L, 1L, "홍길동", LocalDateTime.now(), LocalDateTime.now(), 1L, "개발팀", MemberType.HR);
    when(memberGroupFacade.getMember(anyLong())).thenReturn(member);

    mockMvc
        .perform(
            get("/api/v1/members/me")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "members/me",
                responsePreprocessor(),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data.id").description("멤버 ID"),
                    fieldWithPath("data.userId").description("유저 ID"),
                    fieldWithPath("data.name").description("멤버 이름"),
                    fieldWithPath("data.createdAt").description("생성 시각"),
                    fieldWithPath("data.recentAt").description("최근 접속 시각"),
                    fieldWithPath("data.groupId").description("소속 그룹 ID").optional(),
                    fieldWithPath("data.group").description("소속 그룹명").optional(),
                    fieldWithPath("data.memberType").description("멤버 유형 (HR, INTERVIEWER)"),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 멤버_수정_API_문서() throws Exception {
    doNothing().when(memberService).validateHrRole(anyLong());
    doNothing().when(memberService).updateMemberType(anyLong(), any(MemberType.class));

    UpdateMemberRequest request = new UpdateMemberRequest(MemberType.INTERVIEWER);

    mockMvc
        .perform(
            patch("/api/v1/members/{memberId}", 2L)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
        .andExpect(status().isOk())
        .andDo(
            document(
                "members/update",
                responsePreprocessor(),
                requestFields(
                    fieldWithPath("memberType").description("변경할 멤버 유형 (HR, INTERVIEWER)")),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data").description("응답 데이터").optional(),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 멤버_삭제_API_문서() throws Exception {
    doNothing().when(memberService).validateHrRole(anyLong());
    doNothing().when(memberService).removeMember(anyLong(), anyLong());

    mockMvc
        .perform(
            delete("/api/v1/members/{memberId}", 2L)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "members/delete",
                responsePreprocessor(),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data").description("응답 데이터").optional(),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 워크스페이스_탈퇴_API_문서() throws Exception {
    doNothing().when(memberService).leaveWorkspace(anyLong());

    mockMvc
        .perform(
            post("/api/v1/workspaces/leave")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "members/leave-workspace",
                responsePreprocessor(),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data").description("응답 데이터").optional(),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 멤버_그룹_배정_API_문서() throws Exception {
    doNothing().when(memberService).validateHrRole(anyLong());
    doNothing().when(memberService).assignGroup(anyLong(), anyLong());

    AssignGroupRequest request = new AssignGroupRequest(1L);

    mockMvc
        .perform(
            patch("/api/v1/members/{memberId}/group", 2L)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
        .andExpect(status().isOk())
        .andDo(
            document(
                "members/assign-group",
                responsePreprocessor(),
                requestFields(fieldWithPath("groupId").description("배정할 그룹 ID")),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data").description("응답 데이터").optional(),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 멤버_그룹_해제_API_문서() throws Exception {
    doNothing().when(memberService).validateHrRole(anyLong());
    doNothing().when(memberService).leaveGroup(anyLong());

    mockMvc
        .perform(
            delete("/api/v1/members/{memberId}/group", 2L)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "members/leave-group",
                responsePreprocessor(),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data").description("응답 데이터").optional(),
                    fieldWithPath("error").description("에러 정보").optional())));
  }
}

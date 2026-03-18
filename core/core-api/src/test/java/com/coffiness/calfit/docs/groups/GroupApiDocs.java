package com.coffiness.calfit.docs.groups;

import static com.coffiness.calfit.docs.RestDocsUtils.responsePreprocessor;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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

import com.coffiness.calfit.api.v1.request.CreateGroupRequest;
import com.coffiness.calfit.api.v1.request.UpdateGroupRequest;
import com.coffiness.calfit.core.api.controller.v1.GroupController;
import com.coffiness.calfit.core.api.facade.workspace.MemberGroupFacade;
import com.coffiness.calfit.docs.RestDocsTest;
import com.coffiness.calfit.domain.workspace.group.GroupInfo;
import com.coffiness.calfit.domain.workspace.group.GroupService;
import com.coffiness.calfit.domain.workspace.member.MemberService;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
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

public class GroupApiDocs extends RestDocsTest {

  private final GroupService groupService = mock(GroupService.class);
  private final MemberGroupFacade memberGroupFacade = mock(MemberGroupFacade.class);
  private final MemberService memberService = mock(MemberService.class);

  private final GroupController groupController =
      new GroupController(groupService, memberGroupFacade, memberService);

  private final SecurityUser mockSecurityUser = new SecurityUser(1L, "test@example.com", "USER");

  @BeforeEach
  @Override
  public void setUp(RestDocumentationContextProvider restDocumentation) {
    super.setUp(restDocumentation);
    setUpMockMvc(
        groupController,
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
  void 그룹_목록_조회_API_문서() throws Exception {
    GroupInfo group = new GroupInfo(1L, "개발팀", "#3B82F6", 5);
    when(memberGroupFacade.getGroups()).thenReturn(List.of(group));

    mockMvc
        .perform(
            get("/api/v1/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "groups/list",
                responsePreprocessor(),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data[].id").description("그룹 ID"),
                    fieldWithPath("data[].name").description("그룹 이름"),
                    fieldWithPath("data[].color").description("그룹 색상 (HEX)"),
                    fieldWithPath("data[].memberCount").description("그룹 소속 멤버 수"),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 그룹_상세_조회_API_문서() throws Exception {
    GroupInfo group = new GroupInfo(1L, "개발팀", "#3B82F6", 5);
    when(memberGroupFacade.getGroup(anyLong())).thenReturn(group);

    mockMvc
        .perform(
            get("/api/v1/groups/{groupId}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "groups/detail",
                responsePreprocessor(),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data.id").description("그룹 ID"),
                    fieldWithPath("data.name").description("그룹 이름"),
                    fieldWithPath("data.color").description("그룹 색상 (HEX)"),
                    fieldWithPath("data.memberCount").description("그룹 소속 멤버 수"),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 그룹_생성_API_문서() throws Exception {
    doNothing().when(memberService).validateHrRole(anyLong());
    GroupInfo created = new GroupInfo(1L, "개발팀", "#3B82F6", 0);
    when(groupService.createGroup(anyString(), anyString())).thenReturn(created);

    CreateGroupRequest request = new CreateGroupRequest("개발팀", "#3B82F6");

    mockMvc
        .perform(
            post("/api/v1/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
        .andExpect(status().isOk())
        .andDo(
            document(
                "groups/create",
                responsePreprocessor(),
                requestFields(
                    fieldWithPath("name").description("그룹 이름"),
                    fieldWithPath("color").description("그룹 색상 (HEX 형식, 예: #3B82F6)")),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data.id").description("생성된 그룹 ID"),
                    fieldWithPath("data.name").description("그룹 이름"),
                    fieldWithPath("data.color").description("그룹 색상"),
                    fieldWithPath("data.memberCount").description("그룹 소속 멤버 수"),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 그룹_수정_API_문서() throws Exception {
    doNothing().when(memberService).validateHrRole(anyLong());
    doNothing().when(groupService).updateGroup(anyLong(), anyString(), anyString());

    UpdateGroupRequest request = new UpdateGroupRequest("디자인팀", "#FF5733");

    mockMvc
        .perform(
            patch("/api/v1/groups/{groupId}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
        .andExpect(status().isOk())
        .andDo(
            document(
                "groups/update",
                responsePreprocessor(),
                requestFields(
                    fieldWithPath("name").description("변경할 그룹 이름"),
                    fieldWithPath("color").description("변경할 그룹 색상 (HEX 형식)")),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data").description("응답 데이터").optional(),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 그룹_삭제_API_문서() throws Exception {
    doNothing().when(memberService).validateHrRole(anyLong());
    doNothing().when(memberGroupFacade).removeGroup(anyLong());

    mockMvc
        .perform(
            delete("/api/v1/groups/{groupId}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "groups/delete",
                responsePreprocessor(),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data").description("응답 데이터").optional(),
                    fieldWithPath("error").description("에러 정보").optional())));
  }
}

package com.coffiness.calfit.docs.workspaces;

import static com.coffiness.calfit.docs.RestDocsUtils.responsePreprocessor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coffiness.calfit.api.v1.request.CreateWorkspaceRequest;
import com.coffiness.calfit.core.api.controller.v1.WorkspaceController;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.docs.RestDocsTest;
import com.coffiness.calfit.domain.workspace.Workspace;
import com.coffiness.calfit.domain.workspace.WorkspaceService;
import com.coffiness.calfit.domain.workspace.member.Member;
import com.coffiness.calfit.domain.workspace.member.MemberService;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class WorkspaceApiDocs extends RestDocsTest {

  private final WorkspaceService workspaceService = mock(WorkspaceService.class);
  private final MemberService memberService = mock(MemberService.class);

  private final WorkspaceController workspaceController =
      new WorkspaceController(workspaceService, memberService);

  private final SecurityUser mockSecurityUser = new SecurityUser(1L, "test@example.com", "USER");

  @BeforeEach
  @Override
  public void setUp(RestDocumentationContextProvider restDocumentation) {
    super.setUp(restDocumentation);
    setUpMockMvc(
        workspaceController,
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
  void 워크스페이스_생성_API_문서() throws Exception {
    Workspace workspace = new Workspace(1L, "ws-uuid-1234", "코피니스", "010-1234-5678", "10~50명");
    when(workspaceService.createWorkspace(anyString(), anyString(), anyString()))
        .thenReturn(workspace);
    when(memberService.registerMember(anyLong(), any(MemberType.class)))
        .thenReturn(new Member(1L, "ws-uuid-1234", 1L, MemberType.HR, null));

    CreateWorkspaceRequest request = new CreateWorkspaceRequest("코피니스", "010-1234-5678", "10~50명");

    mockMvc
        .perform(
            post("/api/v1/workspaces")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
        .andExpect(status().isOk())
        .andDo(
            document(
                "workspaces/create",
                responsePreprocessor(),
                requestFields(
                    fieldWithPath("name").description("워크스페이스 이름"),
                    fieldWithPath("contactPhone").description("담당자 연락처"),
                    fieldWithPath("employeeScale").description("직원 규모").optional()),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data.workspaceId").description("워크스페이스 UUID"),
                    fieldWithPath("data.name").description("워크스페이스 이름"),
                    fieldWithPath("data.contactPhone").description("담당자 연락처"),
                    fieldWithPath("data.employeeScale").description("직원 규모"),
                    fieldWithPath("error").description("에러 정보").optional())));
  }
}

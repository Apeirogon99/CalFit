package com.coffiness.calfit.docs.users;

import static com.coffiness.calfit.docs.RestDocsUtils.responsePreprocessor;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coffiness.calfit.api.v1.request.LoginRequest;
import com.coffiness.calfit.api.v1.request.SignUpRequest;
import com.coffiness.calfit.core.api.controller.v1.UserController;
import com.coffiness.calfit.docs.RestDocsTest;
import com.coffiness.calfit.domain.user.EmailVerificationService;
import com.coffiness.calfit.domain.user.User;
import com.coffiness.calfit.domain.user.UserService;
import com.coffiness.calfit.domain.workspace.Workspace;
import com.coffiness.calfit.domain.workspace.WorkspaceReader;
import com.coffiness.calfit.support.email.EmailPageService;
import com.coffiness.calfit.support.email.EmailProperties;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

class UserApiDocsTest extends RestDocsTest {

  private final UserService userService = mock(UserService.class);
  private final WorkspaceReader workspaceReader = mock(WorkspaceReader.class);
  private final EmailVerificationService emailVerificationService =
      mock(EmailVerificationService.class);
  private final EmailProperties emailProperties = new EmailProperties();
  private final EmailPageService emailPageService = mock(EmailPageService.class);

  private final UserController userController =
      new UserController(
          userService,
          workspaceReader,
          emailVerificationService,
          emailProperties,
          emailPageService);

  private final SecurityUser mockSecurityUser = new SecurityUser(1L, "test@example.com", "USER");

  @BeforeEach
  @Override
  public void setUp(RestDocumentationContextProvider restDocumentation) {
    super.setUp(restDocumentation);
    setUpMockMvc(
        userController,
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
  void 사용자_회원가입_API_문서() throws Exception {
    User user = new User(1L, "test@example.com", "테스트유저", "USER", LocalDateTime.now());
    when(userService.signUp(anyString(), anyString(), anyString())).thenReturn(user);

    SignUpRequest request = new SignUpRequest("test@example.com", "password123", "테스트유저");

    mockMvc
        .perform(
            post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
        .andExpect(status().isOk())
        .andDo(
            document(
                "users/signup",
                responsePreprocessor(),
                requestFields(
                    fieldWithPath("email").description("사용자 이메일"),
                    fieldWithPath("password").description("비밀번호"),
                    fieldWithPath("name").description("사용자 이름")),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data.id").description("사용자 ID"),
                    fieldWithPath("data.email").description("사용자 이메일"),
                    fieldWithPath("data.name").description("사용자 이름"),
                    fieldWithPath("data.role").description("사용자 역할"),
                    fieldWithPath("data.createdAt").description("생성 시각"),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 인증메일_재전송_API_문서() throws Exception {
    User user = new User(1L, "test@example.com", "테스트유저", "USER", LocalDateTime.now());
    when(userService.getUserByEmail(anyString())).thenReturn(user);

    mockMvc
        .perform(
            post("/api/v1/users/signup/resend-verification?email={email}", "test@example.com")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "users/resend-verification",
                responsePreprocessor(),
                queryParameters(parameterWithName("email").description("인증 메일을 다시 보낼 이메일")),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data").description("응답 데이터").optional(),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 회원가입_이메일_인증_API_문서() throws Exception {
    when(emailVerificationService.verifyEmailToken(anyString())).thenReturn(true);
    when(emailPageService.renderSignupVerificationSuccessPage(anyString(), anyString()))
        .thenReturn("<html><body>이메일 인증 성공</body></html>");

    mockMvc
        .perform(
            get("/api/v1/users/signup/verify")
                .param("token", "signup-verify-token")
                .accept(MediaType.TEXT_HTML))
        .andExpect(status().isOk())
        .andDo(
            document(
                "users/signup-verify",
                queryParameters(parameterWithName("token").description("이메일 인증 토큰")),
                responseHeaders(
                    headerWithName("Content-Type").description("text/html 형식의 인증 결과 페이지"))));
  }

  @Test
  void 로그인_API_문서() throws Exception {
    User user = new User(1L, "test@example.com", "테스트유저", "USER", LocalDateTime.now());
    UserService.LoginResult loginResult =
        new UserService.LoginResult(
            "access-token-example", "refresh-token-example", user, "workspace-123");
    when(userService.login(anyString(), anyString())).thenReturn(loginResult);

    LoginRequest request = new LoginRequest("test@example.com", "password123");

    mockMvc
        .perform(
            post("/api/v1/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
        .andExpect(status().isOk())
        .andDo(
            document(
                "users/login",
                responsePreprocessor(),
                requestFields(
                    fieldWithPath("email").description("사용자 이메일"),
                    fieldWithPath("password").description("비밀번호")),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data.accessToken").description("액세스 토큰"),
                    fieldWithPath("data.refreshToken").description("리프레시 토큰"),
                    fieldWithPath("data.user.id").description("사용자 ID"),
                    fieldWithPath("data.user.email").description("사용자 이메일"),
                    fieldWithPath("data.user.name").description("사용자 이름"),
                    fieldWithPath("data.user.role").description("사용자 역할"),
                    fieldWithPath("data.user.createdAt").description("생성 시각"),
                    fieldWithPath("data.workspaceId").description("기본 워크스페이스 ID").optional(),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 내_정보_API_문서() throws Exception {
    User user = new User(1L, "test@example.com", "테스트유저", "USER", LocalDateTime.now());
    when(userService.getUser(anyLong())).thenReturn(user);

    mockMvc
        .perform(
            get("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "users/me",
                responsePreprocessor(),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data.id").description("사용자 ID"),
                    fieldWithPath("data.email").description("사용자 이메일"),
                    fieldWithPath("data.name").description("사용자 이름"),
                    fieldWithPath("data.role").description("사용자 역할"),
                    fieldWithPath("data.createdAt").description("생성 시각"),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 내_워크스페이스_API_문서() throws Exception {
    when(userService.getWorkspaceId(anyLong())).thenReturn("workspace-123");
    when(workspaceReader.getWorkspace("workspace-123"))
        .thenReturn(new Workspace(1L, "workspace-123", "코피니스", "010-1234-5678", "10-50"));

    mockMvc
        .perform(
            get("/api/v1/users/me/workspace")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "users/me-workspace",
                responsePreprocessor(),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data.workspaceId").description("워크스페이스 ID").optional(),
                    fieldWithPath("data.name").description("워크스페이스 이름").optional(),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 회원탈퇴_API_문서() throws Exception {
    doNothing().when(userService).deleteUser(anyLong());

    mockMvc
        .perform(
            delete("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "users/delete",
                responsePreprocessor(),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data").description("응답 데이터").optional(),
                    fieldWithPath("error").description("에러 정보").optional())));
  }
}

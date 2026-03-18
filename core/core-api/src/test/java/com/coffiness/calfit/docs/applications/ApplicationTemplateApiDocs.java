package com.coffiness.calfit.docs.applications;

import static com.coffiness.calfit.docs.RestDocsUtils.responsePreprocessor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coffiness.calfit.core.api.controller.v1.ApplicationTemplateController;
import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.docs.RestDocsTest;
import com.coffiness.calfit.domain.workspace.member.Member;
import com.coffiness.calfit.domain.workspace.member.MemberReader;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentRepository;
import com.coffiness.calfit.storage.db.core.template.ApplicationTemplateEntity;
import com.coffiness.calfit.storage.db.core.template.ApplicationTemplateRepository;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

class ApplicationTemplateApiDocsTest extends RestDocsTest {

  private static final String WORKSPACE_ID = "workspace-123";

  private final ApplicationTemplateRepository applicationTemplateRepository =
      mock(ApplicationTemplateRepository.class);
  private final RecruitmentRepository recruitmentRepository = mock(RecruitmentRepository.class);
  private final MemberReader memberReader = mock(MemberReader.class);

  private ApplicationTemplateController applicationTemplateController;

  private final SecurityUser mockSecurityUser = new SecurityUser(1L, "hr@test.com", "USER");

  @BeforeEach
  @Override
  public void setUp(RestDocumentationContextProvider restDocumentation) {
    super.setUp(restDocumentation);
    applicationTemplateController =
        new ApplicationTemplateController(
            applicationTemplateRepository, recruitmentRepository, objectMapper, memberReader);

    when(memberReader.getMember(WORKSPACE_ID, mockSecurityUser.userId()))
        .thenReturn(new Member(1L, WORKSPACE_ID, mockSecurityUser.userId(), MemberType.HR, null));

    setUpMockMvc(
        applicationTemplateController,
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
  void 지원서_템플릿_목록_API_문서() throws Exception {
    ApplicationTemplateEntity template =
        templateEntity(1L, "기본 지원서 템플릿", true, "[{\"key\":\"portfolioUrl\",\"type\":\"TEXT\"}]");

    when(applicationTemplateRepository.findActiveTemplates()).thenReturn(List.of(template));
    when(recruitmentRepository.countByTemplateIds(anyList(), eq(EntityStatus.ACTIVE)))
        .thenReturn(java.util.Collections.singletonList(new Object[] {1L, 2L}));

    performWithTenant(
            WORKSPACE_ID,
            get("/api/v1/workspaces/{workspaceId}/application-templates", WORKSPACE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "application-templates/list",
                responsePreprocessor(),
                pathParameters(parameterWithName("workspaceId").description("워크스페이스 ID")),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data[].id").description("지원서 템플릿 ID"),
                    fieldWithPath("data[].name").description("지원서 템플릿 이름"),
                    fieldWithPath("data[].createdAt").description("생성일"),
                    fieldWithPath("data[].updatedAt").description("수정일"),
                    fieldWithPath("data[].status").description("사용 상태 라벨"),
                    fieldWithPath("data[].used").description("기본 템플릿 사용 여부"),
                    fieldWithPath("data[].recruitmentCount").description("연결된 채용 공고 수"),
                    fieldWithPath("data[].customFields").description("커스텀 필드 JSON 문자열"),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 지원서_템플릿_생성_API_문서() throws Exception {
    when(applicationTemplateRepository.save(any(ApplicationTemplateEntity.class)))
        .thenAnswer(
            invocation -> {
              ApplicationTemplateEntity saved = invocation.getArgument(0);
              setBaseFields(saved, 2L);
              return saved;
            });

    Map<String, Object> request = new LinkedHashMap<>();
    request.put("name", "백엔드 지원서 템플릿");
    request.put(
        "customFields",
        List.of(Map.of("key", "portfolioUrl", "label", "포트폴리오 링크", "type", "TEXT")));
    request.put("used", true);

    performWithTenant(
            WORKSPACE_ID,
            post("/api/v1/workspaces/{workspaceId}/application-templates", WORKSPACE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
        .andExpect(status().isOk())
        .andDo(
            document(
                "application-templates/create",
                responsePreprocessor(),
                pathParameters(parameterWithName("workspaceId").description("워크스페이스 ID")),
                requestFields(
                    fieldWithPath("name").description("지원서 템플릿 이름"),
                    fieldWithPath("customFields").description("커스텀 필드 목록"),
                    fieldWithPath("customFields[].key").description("필드 키"),
                    fieldWithPath("customFields[].label").description("필드 라벨"),
                    fieldWithPath("customFields[].type").description("필드 타입"),
                    fieldWithPath("used").description("기본 템플릿으로 사용할지 여부").optional()),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data.id").description("지원서 템플릿 ID"),
                    fieldWithPath("data.name").description("지원서 템플릿 이름"),
                    fieldWithPath("data.createdAt").description("생성일"),
                    fieldWithPath("data.updatedAt").description("수정일"),
                    fieldWithPath("data.status").description("사용 상태 라벨"),
                    fieldWithPath("data.used").description("기본 템플릿 사용 여부"),
                    fieldWithPath("data.recruitmentCount").description("연결된 채용 공고 수"),
                    fieldWithPath("data.customFields").description("커스텀 필드 JSON 문자열"),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 지원서_템플릿_수정_API_문서() throws Exception {
    ApplicationTemplateEntity template =
        templateEntity(3L, "기존 지원서 템플릿", false, "[{\"key\":\"githubUrl\",\"type\":\"TEXT\"}]");

    when(applicationTemplateRepository.findActiveById(3L)).thenReturn(Optional.of(template));
    when(recruitmentRepository.countByTemplateIds(anyList(), eq(EntityStatus.ACTIVE)))
        .thenReturn(java.util.Collections.singletonList(new Object[] {3L, 1L}));

    Map<String, Object> request = new LinkedHashMap<>();
    request.put("name", "수정된 지원서 템플릿");
    request.put(
        "customFields", List.of(Map.of("key", "resumeUrl", "label", "이력서 링크", "type", "TEXT")));
    request.put("used", true);

    performWithTenant(
            WORKSPACE_ID,
            put(
                    "/api/v1/workspaces/{workspaceId}/application-templates/{templateId}",
                    WORKSPACE_ID,
                    3L)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
        .andExpect(status().isOk())
        .andDo(
            document(
                "application-templates/update",
                responsePreprocessor(),
                pathParameters(
                    parameterWithName("workspaceId").description("워크스페이스 ID"),
                    parameterWithName("templateId").description("지원서 템플릿 ID")),
                requestFields(
                    fieldWithPath("name").description("지원서 템플릿 이름"),
                    fieldWithPath("customFields").description("커스텀 필드 목록"),
                    fieldWithPath("customFields[].key").description("필드 키"),
                    fieldWithPath("customFields[].label").description("필드 라벨"),
                    fieldWithPath("customFields[].type").description("필드 타입"),
                    fieldWithPath("used").description("기본 템플릿으로 사용할지 여부").optional()),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data.id").description("지원서 템플릿 ID"),
                    fieldWithPath("data.name").description("지원서 템플릿 이름"),
                    fieldWithPath("data.createdAt").description("생성일"),
                    fieldWithPath("data.updatedAt").description("수정일"),
                    fieldWithPath("data.status").description("사용 상태 라벨"),
                    fieldWithPath("data.used").description("기본 템플릿 사용 여부"),
                    fieldWithPath("data.recruitmentCount").description("연결된 채용 공고 수"),
                    fieldWithPath("data.customFields").description("커스텀 필드 JSON 문자열"),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 지원서_템플릿_상태변경_API_문서() throws Exception {
    ApplicationTemplateEntity template =
        templateEntity(4L, "상태 변경 템플릿", false, "[{\"key\":\"portfolioUrl\",\"type\":\"TEXT\"}]");

    when(applicationTemplateRepository.findActiveById(4L)).thenReturn(Optional.of(template));
    when(recruitmentRepository.countByTemplateIds(anyList(), eq(EntityStatus.ACTIVE)))
        .thenReturn(java.util.Collections.singletonList(new Object[] {4L, 0L}));

    Map<String, Object> request = Map.of("used", true);

    performWithTenant(
            WORKSPACE_ID,
            patch(
                    "/api/v1/workspaces/{workspaceId}/application-templates/{templateId}/status",
                    WORKSPACE_ID,
                    4L)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
        .andExpect(status().isOk())
        .andDo(
            document(
                "application-templates/status",
                responsePreprocessor(),
                pathParameters(
                    parameterWithName("workspaceId").description("워크스페이스 ID"),
                    parameterWithName("templateId").description("지원서 템플릿 ID")),
                requestFields(fieldWithPath("used").description("기본 템플릿으로 사용할지 여부")),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data.id").description("지원서 템플릿 ID"),
                    fieldWithPath("data.name").description("지원서 템플릿 이름"),
                    fieldWithPath("data.createdAt").description("생성일"),
                    fieldWithPath("data.updatedAt").description("수정일"),
                    fieldWithPath("data.status").description("사용 상태 라벨"),
                    fieldWithPath("data.used").description("기본 템플릿 사용 여부"),
                    fieldWithPath("data.recruitmentCount").description("연결된 채용 공고 수"),
                    fieldWithPath("data.customFields").description("커스텀 필드 JSON 문자열"),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 지원서_템플릿_삭제_API_문서() throws Exception {
    ApplicationTemplateEntity template =
        templateEntity(5L, "삭제 가능한 템플릿", false, "[{\"key\":\"resumeUrl\",\"type\":\"TEXT\"}]");

    when(applicationTemplateRepository.findActiveById(5L)).thenReturn(Optional.of(template));
    when(recruitmentRepository.countByTemplateIds(anyList(), eq(EntityStatus.ACTIVE)))
        .thenReturn(java.util.Collections.singletonList(new Object[] {5L, 0L}));

    performWithTenant(
            WORKSPACE_ID,
            delete(
                    "/api/v1/workspaces/{workspaceId}/application-templates/{templateId}",
                    WORKSPACE_ID,
                    5L)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "application-templates/delete",
                responsePreprocessor(),
                pathParameters(
                    parameterWithName("workspaceId").description("워크스페이스 ID"),
                    parameterWithName("templateId").description("지원서 템플릿 ID")),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data").description("응답 데이터").optional(),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  private ResultActions performWithTenant(String tenantId, MockHttpServletRequestBuilder request)
      throws Exception {
    TenantContext.setTenantId(tenantId);
    try {
      return mockMvc.perform(request);
    } finally {
      TenantContext.clear();
    }
  }

  private ApplicationTemplateEntity templateEntity(
      Long id, String name, boolean used, String customFields) {
    ApplicationTemplateEntity entity = ApplicationTemplateEntity.create(name, customFields, used);
    setBaseFields(entity, id);
    return entity;
  }

  private void setBaseFields(ApplicationTemplateEntity entity, Long id) {
    ReflectionTestUtils.setField(entity, "id", id, Long.class);
    ReflectionTestUtils.setField(
        entity, "createdAt", LocalDateTime.of(2026, 3, 16, 10, 0), LocalDateTime.class);
    ReflectionTestUtils.setField(
        entity, "updatedAt", LocalDateTime.of(2026, 3, 16, 11, 0), LocalDateTime.class);
  }
}

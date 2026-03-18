package com.coffiness.calfit.docs.applications;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coffiness.calfit.core.api.controller.v1.ApplicationExcelController;
import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.Gender;
import com.coffiness.calfit.core.enums.RecruitmentStageType;
import com.coffiness.calfit.core.enums.RecruitmentStatus;
import com.coffiness.calfit.docs.RestDocsTest;
import com.coffiness.calfit.storage.db.core.application.ApplicationEntity;
import com.coffiness.calfit.storage.db.core.application.ApplicationRepository;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleApplicantRepository;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleRepository;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentEntity;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentRepository;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentStageEntity;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentStageRepository;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import java.time.LocalDateTime;
import java.util.List;
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

class ApplicationExcelApiDocsTest extends RestDocsTest {

  private static final String EXCEL_CONTENT_TYPE =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
  private static final String WORKSPACE_ID = "workspace-123";

  private final ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
  private final RecruitmentRepository recruitmentRepository = mock(RecruitmentRepository.class);
  private final RecruitmentStageRepository recruitmentStageRepository =
      mock(RecruitmentStageRepository.class);
  private final InterviewScheduleRepository interviewScheduleRepository =
      mock(InterviewScheduleRepository.class);
  private final InterviewScheduleApplicantRepository interviewScheduleApplicantRepository =
      mock(InterviewScheduleApplicantRepository.class);

  private final ApplicationExcelController applicationExcelController =
      new ApplicationExcelController(
          applicationRepository,
          recruitmentRepository,
          recruitmentStageRepository,
          interviewScheduleRepository,
          interviewScheduleApplicantRepository);

  private final SecurityUser mockSecurityUser = new SecurityUser(1L, "hr@test.com", "USER");

  @BeforeEach
  @Override
  public void setUp(RestDocumentationContextProvider restDocumentation) {
    super.setUp(restDocumentation);
    setUpMockMvc(
        applicationExcelController,
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
  void 지원자_엑셀_다운로드_API_문서() throws Exception {
    ApplicationEntity application =
        ApplicationEntity.create(
            10L,
            100L,
            1000L,
            2000L,
            "홍길동",
            Gender.MALE,
            LocalDateTime.of(1999, 1, 1, 0, 0),
            "010-1234-5678",
            "applicant@test.com",
            "{}");
    ReflectionTestUtils.setField(
        application, "createdAt", LocalDateTime.of(2026, 3, 16, 12, 0), LocalDateTime.class);

    RecruitmentEntity recruitment =
        RecruitmentEntity.builder()
            .creatorId(1L)
            .title("백엔드 채용")
            .recruitmentStatus(RecruitmentStatus.OPEN)
            .targetCount(1)
            .startDate(LocalDateTime.now().minusDays(1))
            .endDate(LocalDateTime.now().plusDays(7))
            .applicationTemplateId(2000L)
            .contents("recruitment-contents")
            .careerType(CareerType.NEW)
            .minExperienceYears(0)
            .maxExperienceYears(0)
            .leadGroupId(1L)
            .build();
    ReflectionTestUtils.setField(recruitment, "id", 100L, Long.class);

    RecruitmentStageEntity stage =
        RecruitmentStageEntity.builder()
            .recruitmentId(100L)
            .stageName("서류 전형")
            .stageStep(1)
            .stageType(RecruitmentStageType.DOCUMENT)
            .build();
    ReflectionTestUtils.setField(stage, "id", 1000L, Long.class);

    when(applicationRepository.findByStatus(eq(EntityStatus.ACTIVE)))
        .thenReturn(List.of(application));
    when(recruitmentRepository.findAllById(anyList())).thenReturn(List.of(recruitment));
    when(recruitmentStageRepository.findAllById(anyList())).thenReturn(List.of(stage));
    when(interviewScheduleApplicantRepository.findAllByTenantIdAndApplicantIdIn(
            eq(WORKSPACE_ID), anyList()))
        .thenReturn(List.of());

    performWithTenant(
            WORKSPACE_ID,
            get("/api/v1/applications/excel")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.parseMediaType(EXCEL_CONTENT_TYPE)))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", containsString(EXCEL_CONTENT_TYPE)))
        .andDo(
            document(
                "applications/excel-export",
                queryParameters(
                    parameterWithName("keyword").description("지원자 이름/이메일 검색어").optional(),
                    parameterWithName("search").description("keyword 와 동일한 검색어 alias").optional(),
                    parameterWithName("status").description("진행 상태 필터").optional(),
                    parameterWithName("stageId").description("채용 단계 ID 필터").optional(),
                    parameterWithName("job").description("공고명 또는 공고 ID 필터").optional(),
                    parameterWithName("jobId").description("공고 ID 필터").optional(),
                    parameterWithName("recruitmentId").description("공고 ID 필터").optional()),
                responseHeaders(
                    headerWithName("Content-Disposition").description("다운로드 파일명"),
                    headerWithName("Content-Type").description("엑셀 MIME 타입"))));
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
}

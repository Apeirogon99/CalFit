package com.coffiness.calfit.docs.admin;

import static com.coffiness.calfit.docs.RestDocsUtils.responsePreprocessor;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coffiness.calfit.core.api.controller.v1.AdminDashboardController;
import com.coffiness.calfit.docs.RestDocsTest;
import com.coffiness.calfit.domain.billing.dashboard.DashboardService;
import com.coffiness.calfit.domain.billing.dashboard.DashboardSummary;
import com.coffiness.calfit.domain.billing.dashboard.PlanDistribution;
import com.coffiness.calfit.domain.billing.dashboard.RevenueCostTrend;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;

public class AdminDashboardApiDocs extends RestDocsTest {

  private final DashboardService dashboardService = mock(DashboardService.class);

  private final AdminDashboardController adminDashboardController =
      new AdminDashboardController(dashboardService);

  @BeforeEach
  @Override
  public void setUp(RestDocumentationContextProvider restDocumentation) {
    super.setUp(restDocumentation);
    setUpMockMvc(adminDashboardController, restDocumentation);
  }

  @Test
  void 대시보드_요약_조회_API_문서() throws Exception {
    DashboardSummary summary =
        new DashboardSummary(9900000L, 12.5, 100L, 15L, 2.3, 0.5, 8500000L, 5.2);
    when(dashboardService.getSummary(anyInt(), anyInt())).thenReturn(summary);

    mockMvc
        .perform(
            get("/api/v1/admin/dashboard/summary")
                .param("month", "2026-03")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "admin/dashboard/summary",
                responsePreprocessor(),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data.mrr").description("월간 반복 수익 (MRR)"),
                    fieldWithPath("data.mrrGrowth").description("MRR 성장률 (%)"),
                    fieldWithPath("data.subscribers").description("구독자 수"),
                    fieldWithPath("data.newThisMonth").description("이번 달 신규 구독자 수"),
                    fieldWithPath("data.churnRate").description("이탈률 (%)"),
                    fieldWithPath("data.churnImprove").description("이탈률 개선폭"),
                    fieldWithPath("data.totalCost").description("총 비용"),
                    fieldWithPath("data.costGrowth").description("비용 증가율 (%)"),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 매출_비용_추세_조회_API_문서() throws Exception {
    List<RevenueCostTrend> trend =
        List.of(
            new RevenueCostTrend(2026, 1, 9000000L, 7500000L),
            new RevenueCostTrend(2026, 2, 9500000L, 8000000L),
            new RevenueCostTrend(2026, 3, 9900000L, 8500000L));
    when(dashboardService.getRevenueCostTrend(anyString())).thenReturn(trend);

    mockMvc
        .perform(
            get("/api/v1/admin/dashboard/revenue-cost-trend")
                .param("period", "monthly")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "admin/dashboard/revenue-cost-trend",
                responsePreprocessor(),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data.trend[].year").description("연도"),
                    fieldWithPath("data.trend[].month").description("월"),
                    fieldWithPath("data.trend[].revenue").description("매출"),
                    fieldWithPath("data.trend[].cost").description("비용"),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 플랜_분포_조회_API_문서() throws Exception {
    List<PlanDistribution> distribution =
        List.of(
            new PlanDistribution("ACTIVE", 80),
            new PlanDistribution("TRIAL", 15),
            new PlanDistribution("CANCELLED", 5));
    when(dashboardService.getPlanDistribution()).thenReturn(distribution);

    mockMvc
        .perform(
            get("/api/v1/admin/dashboard/plan-distribution")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "admin/dashboard/plan-distribution",
                responsePreprocessor(),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data.distribution[].status").description("플랜 상태"),
                    fieldWithPath("data.distribution[].count").description("구독자 수"),
                    fieldWithPath("error").description("에러 정보").optional())));
  }
}

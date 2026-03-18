package com.coffiness.calfit.docs.admin;

import static com.coffiness.calfit.docs.RestDocsUtils.responsePreprocessor;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coffiness.calfit.core.api.controller.v1.AdminPnlController;
import com.coffiness.calfit.docs.RestDocsTest;
import com.coffiness.calfit.domain.billing.pnl.PnlReport;
import com.coffiness.calfit.domain.billing.pnl.PnlService;
import com.coffiness.calfit.domain.billing.pnl.PnlSummary;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;

public class AdminPnlApiDocs extends RestDocsTest {

  private final PnlService pnlService = mock(PnlService.class);

  private final AdminPnlController adminPnlController = new AdminPnlController(pnlService);

  @BeforeEach
  @Override
  public void setUp(RestDocumentationContextProvider restDocumentation) {
    super.setUp(restDocumentation);
    setUpMockMvc(adminPnlController, restDocumentation);
  }

  @Test
  void 손익_요약_조회_API_문서() throws Exception {
    PnlSummary summary = new PnlSummary(9900000L, 12.5, 8500000L, 5.2, 1400000L, 14.1);
    when(pnlService.getSummary(anyInt(), anyInt())).thenReturn(summary);

    mockMvc
        .perform(
            get("/api/v1/admin/pnl/summary")
                .param("month", "2026-03")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "admin/pnl/summary",
                responsePreprocessor(),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data.totalRevenue").description("총 매출"),
                    fieldWithPath("data.revenueGrowth").description("매출 성장률 (%)"),
                    fieldWithPath("data.totalCost").description("총 비용"),
                    fieldWithPath("data.costGrowth").description("비용 증가율 (%)"),
                    fieldWithPath("data.operatingProfit").description("영업이익"),
                    fieldWithPath("data.operatingMargin").description("영업이익률 (%)"),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 손익_리포트_조회_API_문서() throws Exception {
    PnlReport report =
        new PnlReport(
            List.of("2026.01", "2026.02", "2026.03"),
            new long[] {9000000L, 9500000L, 9900000L},
            new long[] {5000000L, 5000000L, 5000000L},
            new long[] {1500000L, 1800000L, 2000000L},
            new long[] {500000L, 700000L, 1000000L},
            new long[] {500000L, 500000L, 500000L},
            new long[] {7500000L, 8000000L, 8500000L},
            new long[] {1500000L, 1500000L, 1400000L});
    when(pnlService.getReport(anyInt())).thenReturn(report);

    mockMvc
        .perform(
            get("/api/v1/admin/pnl/report")
                .param("months", "3")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "admin/pnl/report",
                responsePreprocessor(),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data.columns[]").description("기간 컬럼 (예: 2026.01)"),
                    fieldWithPath("data.revenueTotal[]").description("매출 합계"),
                    fieldWithPath("data.laborCost[]").description("인건비"),
                    fieldWithPath("data.infraCost[]").description("인프라 비용"),
                    fieldWithPath("data.marketingCost[]").description("마케팅 비용"),
                    fieldWithPath("data.otherCost[]").description("기타 비용"),
                    fieldWithPath("data.costTotal[]").description("비용 합계"),
                    fieldWithPath("data.operatingProfit[]").description("영업이익"),
                    fieldWithPath("error").description("에러 정보").optional())));
  }
}

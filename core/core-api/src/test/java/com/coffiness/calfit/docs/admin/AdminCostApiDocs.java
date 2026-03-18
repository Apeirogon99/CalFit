package com.coffiness.calfit.docs.admin;

import static com.coffiness.calfit.docs.RestDocsUtils.responsePreprocessor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coffiness.calfit.api.v1.request.CreateCostRequest;
import com.coffiness.calfit.api.v1.request.UpdateCostRequest;
import com.coffiness.calfit.core.api.controller.v1.AdminCostController;
import com.coffiness.calfit.core.enums.CostCategory;
import com.coffiness.calfit.docs.RestDocsTest;
import com.coffiness.calfit.domain.billing.cost.Cost;
import com.coffiness.calfit.domain.billing.cost.CostService;
import com.coffiness.calfit.domain.billing.cost.MonthlyCostTotal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;

public class AdminCostApiDocs extends RestDocsTest {

  private final CostService costService = mock(CostService.class);

  private final AdminCostController adminCostController = new AdminCostController(costService);

  @BeforeEach
  @Override
  public void setUp(RestDocumentationContextProvider restDocumentation) {
    super.setUp(restDocumentation);
    setUpMockMvc(adminCostController, restDocumentation);
  }

  @Test
  void 비용_요약_조회_API_문서() throws Exception {
    Map<CostCategory, Long> summary =
        Map.of(
            CostCategory.LABOR, 5000000L,
            CostCategory.INFRASTRUCTURE, 2000000L,
            CostCategory.MARKETING, 1000000L,
            CostCategory.OTHER, 500000L);
    when(costService.getSummaryByCategory(anyInt(), anyInt())).thenReturn(summary);

    mockMvc
        .perform(
            get("/api/v1/admin/costs/summary")
                .param("month", "2026-03")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "admin/costs/summary",
                responsePreprocessor(),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data.byCategory").description("카테고리별 비용"),
                    fieldWithPath("data.byCategory.LABOR").description("인건비").optional(),
                    fieldWithPath("data.byCategory.INFRASTRUCTURE")
                        .description("인프라 비용")
                        .optional(),
                    fieldWithPath("data.byCategory.MARKETING").description("마케팅 비용").optional(),
                    fieldWithPath("data.byCategory.OTHER").description("기타 비용").optional(),
                    fieldWithPath("data.total").description("총 비용"),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 비용_추세_조회_API_문서() throws Exception {
    List<MonthlyCostTotal> trend =
        List.of(
            new MonthlyCostTotal(2026, 1, 8000000L),
            new MonthlyCostTotal(2026, 2, 8500000L),
            new MonthlyCostTotal(2026, 3, 8500000L));
    when(costService.getMonthlyTrend()).thenReturn(trend);

    mockMvc
        .perform(
            get("/api/v1/admin/costs/trend")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "admin/costs/trend",
                responsePreprocessor(),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data.trend[].year").description("연도"),
                    fieldWithPath("data.trend[].month").description("월"),
                    fieldWithPath("data.trend[].amount").description("월별 총 비용"),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 비용_목록_조회_API_문서() throws Exception {
    Cost cost = new Cost(1L, CostCategory.LABOR, "개발자 인건비", 5000000L, 2026, 3, "정규직");
    when(costService.getAll(any(), any(), any(), anyInt(), anyInt())).thenReturn(List.of(cost));

    mockMvc
        .perform(
            get("/api/v1/admin/costs")
                .param("year", "2026")
                .param("month", "3")
                .param("page", "0")
                .param("size", "50")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "admin/costs/list",
                responsePreprocessor(),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data[].id").description("비용 ID"),
                    fieldWithPath("data[].category")
                        .description("비용 카테고리 (LABOR, INFRASTRUCTURE, MARKETING, OTHER)"),
                    fieldWithPath("data[].name").description("비용명"),
                    fieldWithPath("data[].amount").description("금액"),
                    fieldWithPath("data[].costYear").description("비용 귀속 연도"),
                    fieldWithPath("data[].costMonth").description("비용 귀속 월"),
                    fieldWithPath("data[].memo").description("메모").optional(),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 비용_등록_API_문서() throws Exception {
    Cost cost = new Cost(1L, CostCategory.LABOR, "개발자 인건비", 5000000L, 2026, 3, "정규직");
    when(costService.create(any(), anyString(), anyLong(), anyInt(), anyInt(), any()))
        .thenReturn(cost);

    CreateCostRequest request =
        new CreateCostRequest(CostCategory.LABOR, "개발자 인건비", 5000000L, 2026, 3, "정규직");

    mockMvc
        .perform(
            post("/api/v1/admin/costs")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
        .andExpect(status().isOk())
        .andDo(
            document(
                "admin/costs/create",
                responsePreprocessor(),
                requestFields(
                    fieldWithPath("category")
                        .description("비용 카테고리 (LABOR, INFRASTRUCTURE, MARKETING, OTHER)"),
                    fieldWithPath("name").description("비용명"),
                    fieldWithPath("amount").description("금액"),
                    fieldWithPath("year").description("비용 귀속 연도"),
                    fieldWithPath("month").description("비용 귀속 월"),
                    fieldWithPath("memo").description("메모").optional()),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data.id").description("비용 ID"),
                    fieldWithPath("data.category").description("비용 카테고리"),
                    fieldWithPath("data.name").description("비용명"),
                    fieldWithPath("data.amount").description("금액"),
                    fieldWithPath("data.costYear").description("비용 귀속 연도"),
                    fieldWithPath("data.costMonth").description("비용 귀속 월"),
                    fieldWithPath("data.memo").description("메모").optional(),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 비용_수정_API_문서() throws Exception {
    Cost cost = new Cost(1L, CostCategory.INFRASTRUCTURE, "AWS 서버비", 2500000L, 2026, 3, "월정액");
    when(costService.update(anyLong(), any(), anyString(), anyLong(), anyInt(), anyInt(), any()))
        .thenReturn(cost);

    UpdateCostRequest request =
        new UpdateCostRequest(CostCategory.INFRASTRUCTURE, "AWS 서버비", 2500000L, 2026, 3, "월정액");

    mockMvc
        .perform(
            put("/api/v1/admin/costs/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
        .andExpect(status().isOk())
        .andDo(
            document(
                "admin/costs/update",
                responsePreprocessor(),
                requestFields(
                    fieldWithPath("category").description("비용 카테고리"),
                    fieldWithPath("name").description("비용명"),
                    fieldWithPath("amount").description("금액"),
                    fieldWithPath("year").description("비용 귀속 연도"),
                    fieldWithPath("month").description("비용 귀속 월"),
                    fieldWithPath("memo").description("메모").optional()),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data.id").description("비용 ID"),
                    fieldWithPath("data.category").description("비용 카테고리"),
                    fieldWithPath("data.name").description("비용명"),
                    fieldWithPath("data.amount").description("금액"),
                    fieldWithPath("data.costYear").description("비용 귀속 연도"),
                    fieldWithPath("data.costMonth").description("비용 귀속 월"),
                    fieldWithPath("data.memo").description("메모").optional(),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 비용_삭제_API_문서() throws Exception {
    doNothing().when(costService).delete(anyLong());

    mockMvc
        .perform(
            delete("/api/v1/admin/costs/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "admin/costs/delete",
                responsePreprocessor(),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data").description("응답 데이터").optional(),
                    fieldWithPath("error").description("에러 정보").optional())));
  }
}

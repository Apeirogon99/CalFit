package com.coffiness.calfit.docs.admin;

import static com.coffiness.calfit.docs.RestDocsUtils.responsePreprocessor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coffiness.calfit.core.api.controller.v1.AdminSubscriptionController;
import com.coffiness.calfit.core.enums.PlanType;
import com.coffiness.calfit.core.enums.SubscriptionStatus;
import com.coffiness.calfit.docs.RestDocsTest;
import com.coffiness.calfit.domain.billing.subscription.Subscription;
import com.coffiness.calfit.domain.billing.subscription.SubscriptionService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;

public class AdminSubscriptionApiDocs extends RestDocsTest {

  private final SubscriptionService subscriptionService = mock(SubscriptionService.class);

  private final AdminSubscriptionController adminSubscriptionController =
      new AdminSubscriptionController(subscriptionService);

  @BeforeEach
  @Override
  public void setUp(RestDocumentationContextProvider restDocumentation) {
    super.setUp(restDocumentation);
    setUpMockMvc(adminSubscriptionController, restDocumentation);
  }

  @Test
  void 구독_목록_조회_API_문서() throws Exception {
    Subscription subscription =
        new Subscription(
            1L,
            "workspace-uuid-1234",
            PlanType.ENTERPRISE,
            SubscriptionStatus.ACTIVE,
            99000L,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 12, 31),
            null);
    when(subscriptionService.getAll(any(), any(), anyInt(), anyInt()))
        .thenReturn(List.of(subscription));

    mockMvc
        .perform(
            get("/api/v1/admin/subscriptions")
                .param("status", "ACTIVE")
                .param("page", "0")
                .param("size", "20")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "admin/subscriptions/list",
                responsePreprocessor(),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data[].id").description("구독 ID"),
                    fieldWithPath("data[].workspaceId").description("워크스페이스 ID"),
                    fieldWithPath("data[].planType").description("플랜 유형 (ENTERPRISE, BUSINESS)"),
                    fieldWithPath("data[].subscriptionStatus")
                        .description("구독 상태 (ACTIVE, TRIAL, CANCELLED, SUSPENDED)"),
                    fieldWithPath("data[].monthlyAmount").description("월 결제 금액"),
                    fieldWithPath("data[].startDate").description("구독 시작일"),
                    fieldWithPath("data[].endDate").description("구독 종료일").optional(),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 구독_상세_조회_API_문서() throws Exception {
    Subscription subscription =
        new Subscription(
            1L,
            "workspace-uuid-1234",
            PlanType.ENTERPRISE,
            SubscriptionStatus.ACTIVE,
            99000L,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 12, 31),
            null);
    when(subscriptionService.getById(anyLong())).thenReturn(subscription);

    mockMvc
        .perform(
            get("/api/v1/admin/subscriptions/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "admin/subscriptions/detail",
                responsePreprocessor(),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data.id").description("구독 ID"),
                    fieldWithPath("data.workspaceId").description("워크스페이스 ID"),
                    fieldWithPath("data.planType").description("플랜 유형"),
                    fieldWithPath("data.subscriptionStatus").description("구독 상태"),
                    fieldWithPath("data.monthlyAmount").description("월 결제 금액"),
                    fieldWithPath("data.startDate").description("구독 시작일"),
                    fieldWithPath("data.endDate").description("구독 종료일").optional(),
                    fieldWithPath("error").description("에러 정보").optional())));
  }
}

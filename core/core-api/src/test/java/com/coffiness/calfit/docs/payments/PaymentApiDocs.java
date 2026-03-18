package com.coffiness.calfit.docs.payments;

import static com.coffiness.calfit.docs.RestDocsUtils.responsePreprocessor;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coffiness.calfit.api.v1.request.RegisterBillingKeyRequest;
import com.coffiness.calfit.core.api.controller.v1.PaymentController;
import com.coffiness.calfit.core.enums.PaymentStatus;
import com.coffiness.calfit.core.enums.PlanType;
import com.coffiness.calfit.core.enums.SubscriptionStatus;
import com.coffiness.calfit.docs.RestDocsTest;
import com.coffiness.calfit.domain.billing.subscription.Subscription;
import com.coffiness.calfit.domain.payment.PaymentService;
import com.coffiness.calfit.domain.payment.billingkey.BillingKeyInfo;
import com.coffiness.calfit.domain.payment.payment.Payment;
import com.coffiness.calfit.domain.payment.toss.TossPaymentProperties;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;

public class PaymentApiDocs extends RestDocsTest {

  private final PaymentService paymentService = mock(PaymentService.class);
  private final TossPaymentProperties tossPaymentProperties =
      new TossPaymentProperties(
          "test_ck_client_key", "test_sk_secret_key", "https://api.tosspayments.com");

  private final PaymentController paymentController =
      new PaymentController(paymentService, tossPaymentProperties);

  @BeforeEach
  @Override
  public void setUp(RestDocumentationContextProvider restDocumentation) {
    super.setUp(restDocumentation);
    TenantContext.setTenantId("workspace-uuid-1234");
    setUpMockMvc(paymentController, restDocumentation);
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void 토스_설정_조회_API_문서() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/payments/toss-config")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "payments/toss-config",
                responsePreprocessor(),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data.clientKey").description("토스 클라이언트 키"),
                    fieldWithPath("data.customerKey").description("고객 키 (cust_ + workspaceId)"),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 빌링키_등록_API_문서() throws Exception {
    BillingKeyInfo info =
        new BillingKeyInfo(
            1L,
            "workspace-uuid-1234",
            "billing-key-123",
            "cust_workspace-uuid-1234",
            "신한카드",
            "4321-****-****-1234",
            "신용",
            "개인",
            true);
    when(paymentService.registerBillingKey(anyString(), anyString(), anyString())).thenReturn(info);

    RegisterBillingKeyRequest request =
        new RegisterBillingKeyRequest("cust_workspace-uuid-1234", "auth-key-example");

    mockMvc
        .perform(
            post("/api/v1/payments/billing-key")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
        .andExpect(status().isOk())
        .andDo(
            document(
                "payments/billing-key",
                responsePreprocessor(),
                requestFields(
                    fieldWithPath("customerKey").description("고객 키"),
                    fieldWithPath("authKey").description("인증 키")),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data.cardCompany").description("카드사"),
                    fieldWithPath("data.cardNumber").description("카드 번호 (마스킹)"),
                    fieldWithPath("data.cardType").description("카드 유형 (신용/체크)"),
                    fieldWithPath("data.ownerType").description("소유자 유형 (개인/법인)"),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 엔터프라이즈_업그레이드_API_문서() throws Exception {
    Payment payment =
        new Payment(
            1L,
            "workspace-uuid-1234",
            1L,
            1L,
            "billing-key-123",
            "payment-key-123",
            "order-123",
            99000L,
            PaymentStatus.SUCCESS,
            null,
            LocalDateTime.now(),
            0,
            null);
    when(paymentService.upgradeToEnterprise(anyString())).thenReturn(Optional.of(payment));

    mockMvc
        .perform(
            post("/api/v1/payments/upgrade")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "payments/upgrade",
                responsePreprocessor(),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data.paymentId").description("결제 ID"),
                    fieldWithPath("data.status")
                        .description("결제 상태 (SUCCESS, FAILED, RETRY, CANCELLED)"),
                    fieldWithPath("data.amount").description("결제 금액"),
                    fieldWithPath("data.failReason").description("실패 사유").optional(),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 무료_플랜_다운그레이드_API_문서() throws Exception {
    doNothing().when(paymentService).downgradeToFree(anyString());

    mockMvc
        .perform(
            post("/api/v1/payments/downgrade")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "payments/downgrade",
                responsePreprocessor(),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data").description("응답 데이터").optional(),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 등록_카드_조회_API_문서() throws Exception {
    BillingKeyInfo info =
        new BillingKeyInfo(
            1L,
            "workspace-uuid-1234",
            "billing-key-123",
            "cust_workspace-uuid-1234",
            "신한카드",
            "4321-****-****-1234",
            "신용",
            "개인",
            true);
    when(paymentService.getRegisteredCard(anyString())).thenReturn(info);

    mockMvc
        .perform(
            get("/api/v1/payments/card")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "payments/card",
                responsePreprocessor(),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data.cardCompany").description("카드사"),
                    fieldWithPath("data.cardNumber").description("카드 번호 (마스킹)"),
                    fieldWithPath("data.cardType").description("카드 유형 (신용/체크)"),
                    fieldWithPath("data.ownerType").description("소유자 유형 (개인/법인)"),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 구독_정보_조회_API_문서() throws Exception {
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
    when(paymentService.getSubscription(anyString())).thenReturn(subscription);

    mockMvc
        .perform(
            get("/api/v1/payments/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "payments/subscription",
                responsePreprocessor(),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data.id").description("구독 ID"),
                    fieldWithPath("data.workspaceId").description("워크스페이스 ID"),
                    fieldWithPath("data.planType").description("플랜 유형 (ENTERPRISE, BUSINESS)"),
                    fieldWithPath("data.subscriptionStatus")
                        .description("구독 상태 (ACTIVE, TRIAL, CANCELLED, SUSPENDED)"),
                    fieldWithPath("data.monthlyAmount").description("월 결제 금액"),
                    fieldWithPath("data.startDate").description("구독 시작일"),
                    fieldWithPath("data.endDate").description("구독 종료일").optional(),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 결제_이력_조회_API_문서() throws Exception {
    Payment payment =
        new Payment(
            1L,
            "workspace-uuid-1234",
            1L,
            1L,
            "billing-key-123",
            "payment-key-123",
            "order-202603",
            99000L,
            PaymentStatus.SUCCESS,
            null,
            LocalDateTime.now(),
            0,
            null);
    when(paymentService.getPaymentHistory(anyString(), anyInt(), anyInt()))
        .thenReturn(List.of(payment));

    mockMvc
        .perform(
            get("/api/v1/payments/history")
                .param("page", "0")
                .param("size", "20")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "payments/history",
                responsePreprocessor(),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data[].id").description("결제 ID"),
                    fieldWithPath("data[].amount").description("결제 금액"),
                    fieldWithPath("data[].status").description("결제 상태"),
                    fieldWithPath("data[].paidAt").description("결제 시각"),
                    fieldWithPath("data[].orderId").description("주문 ID"),
                    fieldWithPath("error").description("에러 정보").optional())));
  }
}

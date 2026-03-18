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

import com.coffiness.calfit.core.api.controller.v1.AdminInvoiceController;
import com.coffiness.calfit.core.enums.InvoiceStatus;
import com.coffiness.calfit.core.enums.PlanType;
import com.coffiness.calfit.docs.RestDocsTest;
import com.coffiness.calfit.domain.billing.invoice.Invoice;
import com.coffiness.calfit.domain.billing.invoice.InvoiceService;
import com.coffiness.calfit.domain.billing.invoice.InvoiceSummary;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;

public class AdminInvoiceApiDocs extends RestDocsTest {

  private final InvoiceService invoiceService = mock(InvoiceService.class);

  private final AdminInvoiceController adminInvoiceController =
      new AdminInvoiceController(invoiceService);

  @BeforeEach
  @Override
  public void setUp(RestDocumentationContextProvider restDocumentation) {
    super.setUp(restDocumentation);
    setUpMockMvc(adminInvoiceController, restDocumentation);
  }

  @Test
  void 인보이스_요약_조회_API_문서() throws Exception {
    InvoiceSummary summary = new InvoiceSummary(9900000L, 100L, 500000L, 2L);
    when(invoiceService.getSummary(anyInt(), anyInt())).thenReturn(summary);

    mockMvc
        .perform(
            get("/api/v1/admin/invoices/summary")
                .param("month", "2026-03")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "admin/invoices/summary",
                responsePreprocessor(),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data.confirmedRevenue").description("확정 매출"),
                    fieldWithPath("data.confirmedCount").description("확정 건수"),
                    fieldWithPath("data.outstanding").description("미수금"),
                    fieldWithPath("data.outstandingOverdue").description("미수금 중 연체 건수"),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 인보이스_상세_조회_API_문서() throws Exception {
    Invoice invoice =
        new Invoice(
            1L,
            1L,
            "workspace-uuid-1234",
            PlanType.ENTERPRISE,
            2026,
            3,
            99000L,
            LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 3, 5),
            InvoiceStatus.PAID);
    when(invoiceService.getById(anyLong())).thenReturn(invoice);

    mockMvc
        .perform(
            get("/api/v1/admin/invoices/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "admin/invoices/detail",
                responsePreprocessor(),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data.id").description("인보이스 ID"),
                    fieldWithPath("data.workspaceId").description("워크스페이스 ID"),
                    fieldWithPath("data.planType").description("플랜 유형 (ENTERPRISE, BUSINESS)"),
                    fieldWithPath("data.billingPeriod").description("청구 기간 (예: 2026.03)"),
                    fieldWithPath("data.amount").description("청구 금액"),
                    fieldWithPath("data.issuedAt").description("발행일"),
                    fieldWithPath("data.paidAt").description("결제일").optional(),
                    fieldWithPath("data.invoiceStatus")
                        .description("인보이스 상태 (PAID, UNPAID, OVERDUE)"),
                    fieldWithPath("error").description("에러 정보").optional())));
  }

  @Test
  void 인보이스_목록_조회_API_문서() throws Exception {
    Invoice invoice =
        new Invoice(
            1L,
            1L,
            "workspace-uuid-1234",
            PlanType.ENTERPRISE,
            2026,
            3,
            99000L,
            LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 3, 5),
            InvoiceStatus.PAID);
    when(invoiceService.getAll(any(), anyInt(), anyInt())).thenReturn(List.of(invoice));

    mockMvc
        .perform(
            get("/api/v1/admin/invoices")
                .param("status", "PAID")
                .param("page", "0")
                .param("size", "20")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "admin/invoices/list",
                responsePreprocessor(),
                responseFields(
                    fieldWithPath("result").description("결과 상태"),
                    fieldWithPath("data[].id").description("인보이스 ID"),
                    fieldWithPath("data[].workspaceId").description("워크스페이스 ID"),
                    fieldWithPath("data[].planType").description("플랜 유형"),
                    fieldWithPath("data[].billingPeriod").description("청구 기간"),
                    fieldWithPath("data[].amount").description("청구 금액"),
                    fieldWithPath("data[].issuedAt").description("발행일"),
                    fieldWithPath("data[].paidAt").description("결제일").optional(),
                    fieldWithPath("data[].invoiceStatus").description("인보이스 상태"),
                    fieldWithPath("error").description("에러 정보").optional())));
  }
}

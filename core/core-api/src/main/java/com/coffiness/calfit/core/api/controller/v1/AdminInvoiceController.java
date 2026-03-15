package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.api.v1.response.InvoiceResponse;
import com.coffiness.calfit.api.v1.response.InvoiceSummaryResponse;
import com.coffiness.calfit.core.enums.InvoiceStatus;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.billing.invoice.Invoice;
import com.coffiness.calfit.domain.billing.invoice.InvoiceService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/invoices")
@RequiredArgsConstructor
public class AdminInvoiceController {

  private final InvoiceService invoiceService;

  @GetMapping("/summary")
  public ApiResponse<InvoiceSummaryResponse> getSummary(
      @RequestParam(required = false) String month) {
    LocalDate date = parseMonth(month);
    return ApiResponse.success(
        InvoiceSummaryResponse.from(
            invoiceService.getSummary(date.getYear(), date.getMonthValue())));
  }

  @GetMapping("/{id}")
  public ApiResponse<InvoiceResponse> getInvoice(@PathVariable Long id) {
    return ApiResponse.success(InvoiceResponse.from(invoiceService.getById(id)));
  }

  @GetMapping
  public ApiResponse<List<InvoiceResponse>> getInvoices(
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    InvoiceStatus invoiceStatus = parseStatus(status);
    List<Invoice> invoices = invoiceService.getAll(invoiceStatus, page, size);
    return ApiResponse.success(invoices.stream().map(InvoiceResponse::from).toList());
  }

  private LocalDate parseMonth(String month) {
    if (month == null || month.isBlank()) {
      return LocalDate.now();
    }
    YearMonth ym = YearMonth.parse(month);
    return ym.atDay(1);
  }

  private InvoiceStatus parseStatus(String status) {
    if (status == null || status.isBlank()) return null;
    try {
      return InvoiceStatus.valueOf(status.toUpperCase());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}

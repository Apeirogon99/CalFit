package com.coffiness.calfit.domain.billing.invoice;

import com.coffiness.calfit.core.enums.InvoiceStatus;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvoiceService {

  private final InvoiceReader invoiceReader;
  private final InvoiceStore invoiceStore;

  public Invoice getById(Long id) {
    return invoiceReader.findById(id).orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
  }

  public InvoiceSummary getSummary(int year, int month) {
    return invoiceReader.getSummary(year, month);
  }

  public List<Invoice> getAll(InvoiceStatus status, int page, int size) {
    return invoiceReader.getAll(status, PageRequest.of(page, size));
  }

  public List<MonthlyRevenue> getMonthlyRevenue() {
    return invoiceReader.getMonthlyRevenue();
  }

  @Transactional
  public Invoice markAsPaid(Long invoiceId) {
    return invoiceStore.markAsPaid(invoiceId);
  }

  @Transactional
  public Invoice markAsOverdue(Long invoiceId) {
    return invoiceStore.markAsOverdue(invoiceId);
  }
}

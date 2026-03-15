package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.InvoiceStatus;
import com.coffiness.calfit.domain.billing.invoice.Invoice;
import com.coffiness.calfit.domain.billing.invoice.InvoiceReader;
import com.coffiness.calfit.domain.billing.invoice.InvoiceSummary;
import com.coffiness.calfit.domain.billing.invoice.MonthlyRevenue;
import com.coffiness.calfit.storage.db.core.billing.InvoiceEntity;
import com.coffiness.calfit.storage.db.core.billing.InvoiceRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvoiceReaderImpl implements InvoiceReader {

  private final InvoiceRepository invoiceRepository;

  @Override
  public Optional<Invoice> findById(Long id) {
    return invoiceRepository.findById(id).map(this::toInvoice);
  }

  @Override
  public List<Invoice> getAll(InvoiceStatus status, Pageable pageable) {
    return invoiceRepository.findAllWithFilter(EntityStatus.ACTIVE, status, pageable).stream()
        .map(this::toInvoice)
        .toList();
  }

  @Override
  public InvoiceSummary getSummary(int year, int month) {
    long confirmedRevenue =
        invoiceRepository.sumAmountByStatusAndMonth(
            EntityStatus.ACTIVE, InvoiceStatus.PAID, year, month);
    long confirmedCount =
        invoiceRepository.countByStatusAndMonth(
            EntityStatus.ACTIVE, InvoiceStatus.PAID, year, month);
    long unpaid =
        invoiceRepository.sumAmountByStatusAndMonth(
            EntityStatus.ACTIVE, InvoiceStatus.UNPAID, year, month);
    long overdue =
        invoiceRepository.sumAmountByStatusAndMonth(
            EntityStatus.ACTIVE, InvoiceStatus.OVERDUE, year, month);
    long overdueCount =
        invoiceRepository.countByStatusAndMonth(
            EntityStatus.ACTIVE, InvoiceStatus.OVERDUE, year, month);
    return new InvoiceSummary(confirmedRevenue, confirmedCount, unpaid + overdue, overdueCount);
  }

  @Override
  public List<MonthlyRevenue> getMonthlyRevenue() {
    return invoiceRepository.findMonthlyRevenue(EntityStatus.ACTIVE).stream()
        .map(
            row ->
                new MonthlyRevenue(
                    ((Number) row[0]).intValue(),
                    ((Number) row[1]).intValue(),
                    ((Number) row[2]).longValue()))
        .toList();
  }

  private Invoice toInvoice(InvoiceEntity entity) {
    return new Invoice(
        entity.getId(),
        entity.getSubscriptionId(),
        entity.getWorkspaceId(),
        entity.getPlanType(),
        entity.getBillingYear(),
        entity.getBillingMonth(),
        entity.getAmount(),
        entity.getIssuedAt(),
        entity.getPaidAt(),
        entity.getInvoiceStatus());
  }
}

package com.coffiness.calfit.infra;

import com.coffiness.calfit.domain.billing.invoice.Invoice;
import com.coffiness.calfit.domain.billing.invoice.InvoiceStore;
import com.coffiness.calfit.storage.db.core.billing.InvoiceEntity;
import com.coffiness.calfit.storage.db.core.billing.InvoiceRepository;
import com.coffiness.calfit.storage.db.core.billing.SubscriptionEntity;
import com.coffiness.calfit.storage.db.core.billing.SubscriptionRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvoiceStoreImpl implements InvoiceStore {

  private final InvoiceRepository invoiceRepository;
  private final SubscriptionRepository subscriptionRepository;

  @Override
  public Invoice createPendingInvoice(
      Long subscriptionId, String workspaceId, int year, int month) {
    SubscriptionEntity subscription =
        subscriptionRepository
            .findById(subscriptionId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    InvoiceEntity entity =
        InvoiceEntity.createPending(
            subscriptionId,
            workspaceId,
            subscription.getPlanType(),
            year,
            month,
            subscription.getMonthlyAmount());
    InvoiceEntity saved = invoiceRepository.save(entity);
    return toInvoice(saved);
  }

  @Override
  public Invoice markAsPaid(Long invoiceId) {
    InvoiceEntity entity =
        invoiceRepository
            .findById(invoiceId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    entity.markAsPaid();
    return toInvoice(invoiceRepository.save(entity));
  }

  @Override
  public Invoice markAsOverdue(Long invoiceId) {
    InvoiceEntity entity =
        invoiceRepository
            .findById(invoiceId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    entity.markAsOverdue();
    return toInvoice(invoiceRepository.save(entity));
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

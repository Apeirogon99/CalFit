package com.coffiness.calfit.domain.billing.invoice;

public interface InvoiceStore {

  Invoice createPendingInvoice(Long subscriptionId, String workspaceId, int year, int month);

  Invoice markAsPaid(Long invoiceId);

  Invoice markAsOverdue(Long invoiceId);
}

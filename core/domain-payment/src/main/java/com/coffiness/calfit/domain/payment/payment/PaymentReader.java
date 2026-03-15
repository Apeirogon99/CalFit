package com.coffiness.calfit.domain.payment.payment;

import java.util.List;
import java.util.Optional;

public interface PaymentReader {

  Optional<Payment> findById(Long id);

  List<Payment> findByWorkspaceId(String workspaceId, int page, int size);

  long countByWorkspaceId(String workspaceId);

  List<Payment> findRetryablePayments();

  boolean existsSuccessfulPaymentInMonth(String workspaceId, int year, int month);
}

package com.coffiness.calfit.domain.billing.event;

import com.coffiness.calfit.support.event.DomainEvent;
import java.time.LocalDate;

/**
 * 결제 완료 이벤트 — domain-billing에서 정의, domain-payment에서 발행. core-api의 PaymentBillingEventHandler가 수신하여
 * Invoice를 PAID 처리한다.
 */
public record PaymentCompletedEvent(
    Long invoiceId, Long subscriptionId, Long amount, LocalDate paidAt, long occurredAt)
    implements DomainEvent {

  public static PaymentCompletedEvent of(Long invoiceId, Long subscriptionId, Long amount) {
    return new PaymentCompletedEvent(
        invoiceId, subscriptionId, amount, LocalDate.now(), System.currentTimeMillis());
  }
}

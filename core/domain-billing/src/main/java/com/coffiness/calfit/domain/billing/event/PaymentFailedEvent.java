package com.coffiness.calfit.domain.billing.event;

import com.coffiness.calfit.support.event.DomainEvent;

/**
 * 결제 실패 이벤트 — domain-billing에서 정의, domain-payment에서 발행. core-api의 PaymentBillingEventHandler가 수신하여
 * Invoice를 OVERDUE 처리한다.
 */
public record PaymentFailedEvent(
    Long invoiceId, Long subscriptionId, String reason, long occurredAt) implements DomainEvent {

  public static PaymentFailedEvent of(Long invoiceId, Long subscriptionId, String reason) {
    return new PaymentFailedEvent(invoiceId, subscriptionId, reason, System.currentTimeMillis());
  }
}

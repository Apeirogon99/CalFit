package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.domain.billing.event.PaymentCompletedEvent;
import com.coffiness.calfit.domain.billing.event.PaymentFailedEvent;
import com.coffiness.calfit.domain.billing.invoice.InvoiceService;
import com.coffiness.calfit.domain.billing.subscription.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * domain-payment 구현 시 채워질 cross-domain 이벤트 핸들러. PaymentCompletedEvent / PaymentFailedEvent 를 수신해
 * billing 상태를 업데이트한다.
 */
@Component
@RequiredArgsConstructor
public class PaymentBillingEventHandler {

  private final InvoiceService invoiceService;
  private final SubscriptionService subscriptionService;

  @EventListener
  @Async
  public void onPaymentCompleted(PaymentCompletedEvent event) {
    invoiceService.markAsPaid(event.invoiceId());
    subscriptionService.activate(event.subscriptionId());
  }

  @EventListener
  @Async
  public void onPaymentFailed(PaymentFailedEvent event) {
    invoiceService.markAsOverdue(event.invoiceId());
  }
}

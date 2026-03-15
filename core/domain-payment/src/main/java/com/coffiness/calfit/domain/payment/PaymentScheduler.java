package com.coffiness.calfit.domain.payment;

import com.coffiness.calfit.core.enums.PlanType;
import com.coffiness.calfit.core.enums.SubscriptionStatus;
import com.coffiness.calfit.domain.billing.subscription.Subscription;
import com.coffiness.calfit.domain.billing.subscription.SubscriptionReader;
import com.coffiness.calfit.domain.payment.payment.Payment;
import com.coffiness.calfit.domain.payment.payment.PaymentReader;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentScheduler {

  private final PaymentService paymentService;
  private final SubscriptionReader subscriptionReader;
  private final PaymentReader paymentReader;

  @Scheduled(cron = "0 0 9 1 * *", zone = "Asia/Seoul")
  public void monthlyBilling() {
    log.info("[PaymentScheduler] 월 정기결제 시작");
    List<Subscription> activeEnterprise =
        subscriptionReader.findActiveByPlanType(PlanType.ENTERPRISE);

    for (Subscription subscription : activeEnterprise) {
      if (subscription.subscriptionStatus() != SubscriptionStatus.ACTIVE) {
        continue;
      }
      try {
        paymentService.processMonthlyBilling(subscription.workspaceId());
        log.info("[PaymentScheduler] 결제 성공: workspaceId={}", subscription.workspaceId());
      } catch (Exception e) {
        log.error(
            "[PaymentScheduler] 결제 실패: workspaceId={}, error={}",
            subscription.workspaceId(),
            e.getMessage());
      }
    }
    log.info("[PaymentScheduler] 월 정기결제 완료");
  }

  @Scheduled(cron = "0 0 * * * *")
  public void retryFailedPayments() {
    List<Payment> retryable = paymentReader.findRetryablePayments();
    if (retryable.isEmpty()) {
      return;
    }

    log.info("[PaymentScheduler] 결제 재시도 시작: {}건", retryable.size());
    for (Payment payment : retryable) {
      try {
        paymentService.retryFailedPayment(payment.id());
        log.info("[PaymentScheduler] 재시도 처리: paymentId={}", payment.id());
      } catch (Exception e) {
        log.error(
            "[PaymentScheduler] 재시도 실패: paymentId={}, error={}", payment.id(), e.getMessage());
      }
    }
  }

  @Scheduled(cron = "0 0 10 * * *", zone = "Asia/Seoul")
  public void suspendOverdueSubscriptions() {
    log.info("[PaymentScheduler] 미결제 구독 중단 처리 시작");
    List<Payment> retryable = paymentReader.findRetryablePayments();

    for (Payment payment : retryable) {
      if (payment.retryCount() >= 2 && payment.nextRetryAt() != null) {
        try {
          paymentService.suspendSubscription(payment.workspaceId());
          log.info("[PaymentScheduler] 구독 중단: workspaceId={}", payment.workspaceId());
        } catch (Exception e) {
          log.error(
              "[PaymentScheduler] 구독 중단 실패: workspaceId={}, error={}",
              payment.workspaceId(),
              e.getMessage());
        }
      }
    }
    log.info("[PaymentScheduler] 미결제 구독 중단 처리 완료");
  }
}

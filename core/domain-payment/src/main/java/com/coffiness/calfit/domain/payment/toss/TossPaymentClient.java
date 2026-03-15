package com.coffiness.calfit.domain.payment.toss;

public interface TossPaymentClient {

  BillingKeyResponse issueBillingKey(String customerKey, String authKey);

  PaymentResponse chargeBillingKey(
      String billingKey, String customerKey, String orderId, Long amount, String orderName);
}

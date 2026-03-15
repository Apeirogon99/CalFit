package com.coffiness.calfit.storage.db.core.payment;

import com.coffiness.calfit.core.enums.PaymentStatus;
import com.coffiness.calfit.storage.db.core.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class PaymentEntity extends BaseEntity {

  @Column(nullable = false)
  private String workspaceId;

  @Column private Long subscriptionId;

  @Column private Long invoiceId;

  @Column private String billingKey;

  @Column private String paymentKey;

  @Column(nullable = false, unique = true)
  private String orderId;

  @Column(nullable = false)
  private Long amount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PaymentStatus paymentStatus;

  @Column private String failReason;

  @Column private LocalDateTime paidAt;

  @Column(nullable = false)
  private int retryCount = 0;

  @Column private LocalDateTime nextRetryAt;

  protected PaymentEntity() {}

  private PaymentEntity(
      String workspaceId,
      Long subscriptionId,
      Long invoiceId,
      String billingKey,
      String orderId,
      Long amount,
      PaymentStatus paymentStatus) {
    this.workspaceId = workspaceId;
    this.subscriptionId = subscriptionId;
    this.invoiceId = invoiceId;
    this.billingKey = billingKey;
    this.orderId = orderId;
    this.amount = amount;
    this.paymentStatus = paymentStatus;
  }

  public static PaymentEntity create(
      String workspaceId,
      Long subscriptionId,
      Long invoiceId,
      String billingKey,
      String orderId,
      Long amount) {
    return new PaymentEntity(
        workspaceId, subscriptionId, invoiceId, billingKey, orderId, amount, PaymentStatus.SUCCESS);
  }

  public String getWorkspaceId() {
    return workspaceId;
  }

  public Long getSubscriptionId() {
    return subscriptionId;
  }

  public Long getInvoiceId() {
    return invoiceId;
  }

  public String getBillingKey() {
    return billingKey;
  }

  public String getPaymentKey() {
    return paymentKey;
  }

  public String getOrderId() {
    return orderId;
  }

  public Long getAmount() {
    return amount;
  }

  public PaymentStatus getPaymentStatus() {
    return paymentStatus;
  }

  public String getFailReason() {
    return failReason;
  }

  public LocalDateTime getPaidAt() {
    return paidAt;
  }

  public int getRetryCount() {
    return retryCount;
  }

  public LocalDateTime getNextRetryAt() {
    return nextRetryAt;
  }

  public void markSuccess(String paymentKey) {
    this.paymentKey = paymentKey;
    this.paymentStatus = PaymentStatus.SUCCESS;
    this.paidAt = LocalDateTime.now();
    this.failReason = null;
    this.nextRetryAt = null;
  }

  public void markFailed(String failReason, LocalDateTime nextRetryAt) {
    this.paymentStatus = PaymentStatus.RETRY;
    this.failReason = failReason;
    this.retryCount++;
    this.nextRetryAt = nextRetryAt;
  }

  public void markFinalFailed(String failReason) {
    this.paymentStatus = PaymentStatus.FAILED;
    this.failReason = failReason;
    this.nextRetryAt = null;
  }

  public void markCancelled() {
    this.paymentStatus = PaymentStatus.CANCELLED;
  }
}

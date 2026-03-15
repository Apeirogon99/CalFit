package com.coffiness.calfit.storage.db.core.billing;

import com.coffiness.calfit.core.enums.InvoiceStatus;
import com.coffiness.calfit.core.enums.PlanType;
import com.coffiness.calfit.storage.db.core.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "invoices")
public class InvoiceEntity extends BaseEntity {

  @Column(nullable = false)
  private Long subscriptionId;

  @Column(nullable = false)
  private String workspaceId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PlanType planType;

  @Column(nullable = false)
  private int billingYear;

  @Column(nullable = false)
  private int billingMonth;

  @Column(nullable = false)
  private Long amount;

  @Column(nullable = false)
  private LocalDate issuedAt;

  @Column private LocalDate paidAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private InvoiceStatus invoiceStatus;

  protected InvoiceEntity() {}

  private InvoiceEntity(
      Long subscriptionId,
      String workspaceId,
      PlanType planType,
      int billingYear,
      int billingMonth,
      Long amount,
      LocalDate issuedAt) {
    this.subscriptionId = subscriptionId;
    this.workspaceId = workspaceId;
    this.planType = planType;
    this.billingYear = billingYear;
    this.billingMonth = billingMonth;
    this.amount = amount;
    this.issuedAt = issuedAt;
    this.invoiceStatus = InvoiceStatus.UNPAID;
  }

  public static InvoiceEntity createPending(
      Long subscriptionId,
      String workspaceId,
      PlanType planType,
      int billingYear,
      int billingMonth,
      Long amount) {
    return new InvoiceEntity(
        subscriptionId, workspaceId, planType, billingYear, billingMonth, amount, LocalDate.now());
  }

  public Long getSubscriptionId() {
    return subscriptionId;
  }

  public String getWorkspaceId() {
    return workspaceId;
  }

  public PlanType getPlanType() {
    return planType;
  }

  public int getBillingYear() {
    return billingYear;
  }

  public int getBillingMonth() {
    return billingMonth;
  }

  public Long getAmount() {
    return amount;
  }

  public LocalDate getIssuedAt() {
    return issuedAt;
  }

  public LocalDate getPaidAt() {
    return paidAt;
  }

  public InvoiceStatus getInvoiceStatus() {
    return invoiceStatus;
  }

  public void markAsPaid() {
    this.invoiceStatus = InvoiceStatus.PAID;
    this.paidAt = LocalDate.now();
  }

  public void markAsOverdue() {
    this.invoiceStatus = InvoiceStatus.OVERDUE;
  }
}

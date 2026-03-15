package com.coffiness.calfit.storage.db.core.payment;

import com.coffiness.calfit.storage.db.core.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "billing_keys")
public class BillingKeyEntity extends BaseEntity {

  @Column(nullable = false)
  private String workspaceId;

  @Column(nullable = false)
  private String billingKey;

  @Column(nullable = false)
  private String customerKey;

  @Column private String cardCompany;

  @Column private String cardNumber;

  @Column private String cardType;

  @Column private String ownerType;

  @Column(nullable = false)
  private boolean active = true;

  protected BillingKeyEntity() {}

  private BillingKeyEntity(
      String workspaceId,
      String billingKey,
      String customerKey,
      String cardCompany,
      String cardNumber,
      String cardType,
      String ownerType) {
    this.workspaceId = workspaceId;
    this.billingKey = billingKey;
    this.customerKey = customerKey;
    this.cardCompany = cardCompany;
    this.cardNumber = cardNumber;
    this.cardType = cardType;
    this.ownerType = ownerType;
  }

  public static BillingKeyEntity create(
      String workspaceId,
      String billingKey,
      String customerKey,
      String cardCompany,
      String cardNumber,
      String cardType,
      String ownerType) {
    return new BillingKeyEntity(
        workspaceId, billingKey, customerKey, cardCompany, cardNumber, cardType, ownerType);
  }

  public String getWorkspaceId() {
    return workspaceId;
  }

  public String getBillingKey() {
    return billingKey;
  }

  public String getCustomerKey() {
    return customerKey;
  }

  public String getCardCompany() {
    return cardCompany;
  }

  public String getCardNumber() {
    return cardNumber;
  }

  public String getCardType() {
    return cardType;
  }

  public String getOwnerType() {
    return ownerType;
  }

  public boolean isActive() {
    return active;
  }

  public void deactivate() {
    this.active = false;
  }
}

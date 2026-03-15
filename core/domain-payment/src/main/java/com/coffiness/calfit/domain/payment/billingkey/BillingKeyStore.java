package com.coffiness.calfit.domain.payment.billingkey;

public interface BillingKeyStore {

  BillingKeyInfo save(
      String workspaceId,
      String billingKey,
      String customerKey,
      String cardCompany,
      String cardNumber,
      String cardType,
      String ownerType);

  void deactivateByWorkspaceId(String workspaceId);
}

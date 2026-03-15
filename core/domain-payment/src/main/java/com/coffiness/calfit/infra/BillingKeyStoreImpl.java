package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.domain.payment.billingkey.BillingKeyInfo;
import com.coffiness.calfit.domain.payment.billingkey.BillingKeyStore;
import com.coffiness.calfit.storage.db.core.payment.BillingKeyEntity;
import com.coffiness.calfit.storage.db.core.payment.BillingKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BillingKeyStoreImpl implements BillingKeyStore {

  private final BillingKeyRepository billingKeyRepository;

  @Override
  public BillingKeyInfo save(
      String workspaceId,
      String billingKey,
      String customerKey,
      String cardCompany,
      String cardNumber,
      String cardType,
      String ownerType) {
    deactivateByWorkspaceId(workspaceId);
    BillingKeyEntity entity =
        BillingKeyEntity.create(
            workspaceId, billingKey, customerKey, cardCompany, cardNumber, cardType, ownerType);
    BillingKeyEntity saved = billingKeyRepository.save(entity);
    return new BillingKeyInfo(
        saved.getId(),
        saved.getWorkspaceId(),
        saved.getBillingKey(),
        saved.getCustomerKey(),
        saved.getCardCompany(),
        saved.getCardNumber(),
        saved.getCardType(),
        saved.getOwnerType(),
        saved.isActive());
  }

  @Override
  public void deactivateByWorkspaceId(String workspaceId) {
    billingKeyRepository
        .findByWorkspaceIdAndActiveAndStatus(workspaceId, true, EntityStatus.ACTIVE)
        .ifPresent(
            entity -> {
              entity.deactivate();
              billingKeyRepository.save(entity);
            });
  }
}

package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.domain.payment.billingkey.BillingKeyInfo;
import com.coffiness.calfit.domain.payment.billingkey.BillingKeyReader;
import com.coffiness.calfit.storage.db.core.payment.BillingKeyEntity;
import com.coffiness.calfit.storage.db.core.payment.BillingKeyRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BillingKeyReaderImpl implements BillingKeyReader {

  private final BillingKeyRepository billingKeyRepository;

  @Override
  public Optional<BillingKeyInfo> findActiveByWorkspaceId(String workspaceId) {
    return billingKeyRepository
        .findByWorkspaceIdAndActiveAndStatus(workspaceId, true, EntityStatus.ACTIVE)
        .map(this::toBillingKeyInfo);
  }

  @Override
  public Optional<BillingKeyInfo> findActiveByWorkspaceIdWithLock(String workspaceId) {
    return billingKeyRepository
        .findByWorkspaceIdAndActiveIsTrueAndStatus(workspaceId, EntityStatus.ACTIVE)
        .map(this::toBillingKeyInfo);
  }

  private BillingKeyInfo toBillingKeyInfo(BillingKeyEntity entity) {
    return new BillingKeyInfo(
        entity.getId(),
        entity.getWorkspaceId(),
        entity.getBillingKey(),
        entity.getCustomerKey(),
        entity.getCardCompany(),
        entity.getCardNumber(),
        entity.getCardType(),
        entity.getOwnerType(),
        entity.isActive());
  }
}

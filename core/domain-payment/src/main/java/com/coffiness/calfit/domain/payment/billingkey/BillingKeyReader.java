package com.coffiness.calfit.domain.payment.billingkey;

import java.util.Optional;

public interface BillingKeyReader {

  Optional<BillingKeyInfo> findActiveByWorkspaceId(String workspaceId);

  Optional<BillingKeyInfo> findActiveByWorkspaceIdWithLock(String workspaceId);
}

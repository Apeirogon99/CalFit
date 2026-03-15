package com.coffiness.calfit.storage.db.core.config;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

@Component("tenantResolver")
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {

  @Override
  public String resolveCurrentTenantIdentifier() {
    String tenantId = TenantContext.getTenantId();
    return (tenantId != null) ? tenantId : "default";
  }

  @Override
  public boolean validateExistingCurrentSessions() {
    return true;
  }
}

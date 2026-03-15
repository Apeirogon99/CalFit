package com.coffiness.calfit.storage.db.core;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.TenantId;

@MappedSuperclass
public abstract class TenantBaseEntity extends BaseEntity {

  @TenantId
  @Column(name = "tenant_id", nullable = false, updatable = false)
  private String tenantId;

  protected TenantBaseEntity() {}

  protected TenantBaseEntity(String tenantId) {
    this.tenantId = tenantId;
  }

  public String getTenantId() {
    return tenantId;
  }
}

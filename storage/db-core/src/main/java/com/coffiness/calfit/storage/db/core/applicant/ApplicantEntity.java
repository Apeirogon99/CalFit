package com.coffiness.calfit.storage.db.core.applicant;

import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "applicants",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_applicant_tenant_email",
          columnNames = {"tenant_id", "email"})
    })
@NoArgsConstructor
@Getter
public class ApplicantEntity extends TenantBaseEntity {

  @Column(name = "email", nullable = false)
  private String email;

  @Column(name = "password", nullable = false)
  private String password;

  @Column(name = "name", nullable = false)
  private String name;

  @Builder
  private ApplicantEntity(String email, String password, String name) {
    this.email = email;
    this.password = password;
    this.name = name;
  }

  public static ApplicantEntity create(String email, String password, String name) {
    return new ApplicantEntity(email, password, name);
  }
}

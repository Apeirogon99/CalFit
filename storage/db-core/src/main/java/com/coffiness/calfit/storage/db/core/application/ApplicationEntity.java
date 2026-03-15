package com.coffiness.calfit.storage.db.core.application;

import com.coffiness.calfit.core.enums.Gender;
import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "applications",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_application_recruitment_applicant",
          columnNames = {"tenant_id", "recruitment_id", "applicant_id"})
    })
@NoArgsConstructor
@Getter
public class ApplicationEntity extends TenantBaseEntity {

  @Column(name = "applicant_id", nullable = false)
  private Long applicantId;

  @Column(name = "recruitment_id", nullable = false)
  private Long recruitmentId;

  @Column(name = "recruitment_process_id", nullable = false)
  private Long recruitmentProcessId;

  @Column(name = "template_id", nullable = false)
  private Long templateId;

  @Column(nullable = false, length = 50)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Gender gender;

  @Column(nullable = false)
  private LocalDateTime birthDate;

  @Column(nullable = false, length = 20)
  private String phone;

  @Column(nullable = false, length = 50)
  private String email;

  @Column(name = "form_fields", columnDefinition = "JSON", nullable = false)
  private String formFields;

  @Builder
  private ApplicationEntity(
      Long applicantId,
      Long recruitmentId,
      Long recruitmentProcessId,
      Long templateId,
      String name,
      Gender gender,
      LocalDateTime birthDate,
      String phone,
      String email,
      String formFields) {
    this.applicantId = applicantId;
    this.recruitmentId = recruitmentId;
    this.recruitmentProcessId = recruitmentProcessId;
    this.templateId = templateId;
    this.name = name;
    this.gender = gender;
    this.birthDate = birthDate;
    this.phone = phone;
    this.email = email;
    this.formFields = formFields;
  }

  public static ApplicationEntity create(
      Long applicantId,
      Long recruitmentId,
      Long recruitmentProcessId,
      Long templateId,
      String name,
      Gender gender,
      LocalDateTime birthDate,
      String phone,
      String email,
      String formFields) {
    return new ApplicationEntity(
        applicantId,
        recruitmentId,
        recruitmentProcessId,
        templateId,
        name,
        gender,
        birthDate,
        phone,
        email,
        formFields);
  }

  public void changeRecruitmentProcess(Long recruitmentProcessId) {
    this.recruitmentProcessId = recruitmentProcessId;
  }

  public void updateRecruitmentProcessId(Long recruitmentProcessId) {
    this.recruitmentProcessId = recruitmentProcessId;
  }
}

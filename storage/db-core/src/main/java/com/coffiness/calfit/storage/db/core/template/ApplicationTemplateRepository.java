package com.coffiness.calfit.storage.db.core.template;

import com.coffiness.calfit.core.enums.EntityStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplicationTemplateRepository
    extends JpaRepository<ApplicationTemplateEntity, Long> {

  @Query(
      "SELECT t.formFields FROM ApplicationTemplateEntity t"
          + " WHERE t.id = :templateId"
          + " AND t.status = com.coffiness.calfit.core.enums.EntityStatus.ACTIVE")
  Optional<String> findFormFieldsById(@Param("templateId") Long templateId);

  @Query(
      "SELECT t FROM ApplicationTemplateEntity t"
          + " WHERE t.status = com.coffiness.calfit.core.enums.EntityStatus.ACTIVE"
          + " ORDER BY t.createdAt DESC")
  List<ApplicationTemplateEntity> findActiveTemplates();

  @Query(
      "SELECT t FROM ApplicationTemplateEntity t"
          + " WHERE t.id = :templateId"
          + " AND t.status = com.coffiness.calfit.core.enums.EntityStatus.ACTIVE")
  Optional<ApplicationTemplateEntity> findActiveById(@Param("templateId") Long templateId);

  Optional<ApplicationTemplateEntity> findByIdAndStatus(Long id, EntityStatus status);
}

package com.coffiness.calfit.storage.db.core.application;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.UploadStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplicationFileRepository extends JpaRepository<ApplicationFileEntity, Long> {
  Optional<ApplicationFileEntity> findByApplicationIdAndFieldKey(
      Long applicationId, String fieldKey);

  @Query(
      value =
          "SELECT af.tenant_id FROM application_files af"
              + " WHERE af.id = :fileId"
              + " AND af.status = 'ACTIVE'",
      nativeQuery = true)
  Optional<String> findTenantIdById(@Param("fileId") Long fileId);

  List<ApplicationFileEntity> findByApplicationIdAndStatusAndUploadStatus(
      Long applicationId, EntityStatus status, UploadStatus uploadStatus);
}

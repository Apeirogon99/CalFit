package com.coffiness.calfit.storage.db.core.application;

import com.coffiness.calfit.core.enums.UploadStatus;
import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "application_files",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_object_key",
          columnNames = {"object_key"}),
      // db제약: (application_id, field_key)는 DB에서 status='ACTIVE'인 row에 대해서만 유니크 보장
      //       (generated column active_field_key + unique index uk_app_active_field)
    })
@NoArgsConstructor
@Getter
public class ApplicationFileEntity extends TenantBaseEntity {

  @Column(name = "application_id", nullable = false)
  private Long applicationId;

  // 지원자 ID
  @Column(name = "applicant_id", nullable = false)
  private Long applicantId;

  @Column(name = "object_key", nullable = false)
  private String objectKey;

  @Column(name = "original_filename", nullable = false)
  private String originalFilename;

  @Column(name = "field_key", nullable = false)
  private String fieldKey;

  @Column(name = "content_type", nullable = false)
  private String contentType;

  @Column(name = "size_bytes")
  private Long sizeBytes;

  @Column(name = "upload_status", nullable = false)
  @Enumerated(EnumType.STRING)
  private UploadStatus uploadStatus;

  @Builder
  public ApplicationFileEntity(
      Long applicationId,
      Long applicantId,
      String objectKey,
      String originalFilename,
      String fieldKey,
      String contentType,
      Long sizeBytes,
      UploadStatus uploadStatus) {
    this.applicationId = applicationId;
    this.applicantId = applicantId;
    this.objectKey = objectKey;
    this.originalFilename = originalFilename;
    this.fieldKey = fieldKey;
    this.contentType = contentType;
    this.sizeBytes = sizeBytes;
    this.uploadStatus = uploadStatus;
  }

  public void markUploaded(long sizeBytes) {
    this.uploadStatus = UploadStatus.ACTIVE;
    this.sizeBytes = sizeBytes;
  }
}

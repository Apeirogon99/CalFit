package com.coffiness.calfit.domain.applicationFile;

import com.coffiness.calfit.api.v1.request.CompleteUploadRequest;
import com.coffiness.calfit.api.v1.request.PresignUploadRequest;
import com.coffiness.calfit.api.v1.response.CompleteUploadResponse;
import com.coffiness.calfit.api.v1.response.PresignDownloadResponse;
import com.coffiness.calfit.api.v1.response.PresignUploadResponse;
import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.core.enums.UploadStatus;
import com.coffiness.calfit.storage.db.core.application.ApplicationEntity;
import com.coffiness.calfit.storage.db.core.application.ApplicationFileEntity;
import com.coffiness.calfit.storage.db.core.application.ApplicationFileRepository;
import com.coffiness.calfit.storage.db.core.application.ApplicationRepository;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.member.MemberEntity;
import com.coffiness.calfit.storage.db.core.member.MemberRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class ApplicationFileService {

  private static final String DEFAULT_TENANT = "default";

  private final ApplicationFileRepository applicationFileRepository;
  private final ApplicationRepository applicationRepository;
  private final MemberRepository memberRepository;
  private final S3Presigner presigner; // presigner URL 제공
  private final S3Client s3Client; // s3에 업로드 검증

  @Value("${s3.applicant-bucket}")
  private String bucket;

  @Value("${s3.key-prefix}")
  private String keyPrefix;

  @Value("${s3.presign-expire-minutes}")
  private long expireMinutes; // 유효시간 10분

  @Transactional
  public PresignUploadResponse presignUpload(PresignUploadRequest req, SecurityUser requester) {
    ensureTenantFromApplication(req.applicationId());

    ApplicationEntity application = getActiveApplication(req.applicationId());
    validateApplicant(requester, application.getApplicantId());

    if (req.applicantId() == null || !req.applicantId().equals(application.getApplicantId())) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }

    // 같은 application + fieldKey 기존 파일 있으면 삭제 상태로 변경
    applicationFileRepository
        .findByApplicationIdAndFieldKey(req.applicationId(), req.fieldKey())
        .ifPresent(existing -> existing.deleted());

    String ext = extractExt(req.originalFilename());
    String objectKey =
        String.format(
            "%s/%d/%d/%s/%s%s",
            keyPrefix,
            application.getApplicantId(),
            req.applicationId(),
            sanitizeFieldKey(req.fieldKey()),
            UUID.randomUUID(),
            ext);

    ApplicationFileEntity entity =
        ApplicationFileEntity.builder()
            .applicationId(req.applicationId())
            .applicantId(application.getApplicantId())
            .fieldKey(req.fieldKey())
            .objectKey(objectKey)
            .originalFilename(req.originalFilename())
            .contentType(req.contentType())
            .uploadStatus(UploadStatus.PENDING)
            .build();

    applicationFileRepository.save(entity);

    PutObjectRequest putReq =
        PutObjectRequest.builder()
            .bucket(bucket)
            .key(objectKey)
            .contentType(req.contentType())
            .build();

    PutObjectPresignRequest presignReq =
        PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(expireMinutes))
            .putObjectRequest(putReq)
            .build();

    PresignedPutObjectRequest presigned = presigner.presignPutObject(presignReq);

    return new PresignUploadResponse(
        entity.getId(), presigned.url().toString(), objectKey, expireMinutes);
  }

  @Transactional
  public CompleteUploadResponse completeUpload(CompleteUploadRequest req, SecurityUser requester) {
    ensureTenantFromFile(req.fileId());

    ApplicationFileEntity file =
        applicationFileRepository
            .findById(req.fileId())
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

    validateApplicant(requester, file.getApplicantId());

    if (file.getUploadStatus() != UploadStatus.PENDING) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }

    HeadObjectResponse head =
        s3Client.headObject(
            HeadObjectRequest.builder().bucket(bucket).key(file.getObjectKey()).build());

    file.markUploaded(head.contentLength());

    return new CompleteUploadResponse(
        file.getId(), file.getUploadStatus().name(), file.getSizeBytes());
  }

  @Transactional(readOnly = true)
  public PresignDownloadResponse presignDownload(Long fileId, SecurityUser requester) {
    ensureTenantFromFile(fileId);

    ApplicationFileEntity file =
        applicationFileRepository
            .findById(fileId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

    if (file.getUploadStatus() != UploadStatus.ACTIVE) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }

    validateRecruiterCanAccess(requester, file.getApplicationId());

    String encoded = URLEncoder.encode(file.getOriginalFilename(), StandardCharsets.UTF_8);
    String disposition = "attachment; filename*=UTF-8''" + encoded;

    GetObjectRequest getReq =
        GetObjectRequest.builder()
            .bucket(bucket)
            .key(file.getObjectKey())
            .responseContentDisposition(disposition)
            .build();

    GetObjectPresignRequest presignReq =
        GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(expireMinutes))
            .getObjectRequest(getReq)
            .build();

    PresignedGetObjectRequest presigned = presigner.presignGetObject(presignReq);

    return new PresignDownloadResponse(presigned.url().toString(), expireMinutes);
  }

  private void validateApplicant(SecurityUser requester, Long applicantId) {
    if (requester == null
        || !"APPLICANT".equalsIgnoreCase(requester.role())
        || requester.userId() == null
        || !requester.userId().equals(applicantId)) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
  }

  private void validateRecruiterCanAccess(SecurityUser requester, Long applicationId) {
    if (requester == null
        || requester.userId() == null
        || "APPLICANT".equalsIgnoreCase(requester.role())) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }

    getActiveApplication(applicationId);

    MemberEntity member =
        memberRepository.findByTenantIdAndUserIdAndStatus(
            currentTenantId(), requester.userId(), EntityStatus.ACTIVE);
    if (member == null || member.getMemberType() != MemberType.HR) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
  }

  private ApplicationEntity getActiveApplication(Long applicationId) {
    return applicationRepository
        .findByIdAndStatus(applicationId, EntityStatus.ACTIVE)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
  }

  private void ensureTenantFromApplication(Long applicationId) {
    ensureTenant(
        applicationId,
        id ->
            applicationRepository
                .findTenantIdById(id)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND)));
  }

  private void ensureTenantFromFile(Long fileId) {
    ensureTenant(
        fileId,
        id ->
            applicationFileRepository
                .findTenantIdById(id)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND)));
  }

  private void ensureTenant(Long resourceId, java.util.function.Function<Long, String> resolver) {
    if (resourceId == null) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }

    String tenantId = resolver.apply(resourceId);
    String currentTenantId = TenantContext.getTenantId();
    if (currentTenantId == null
        || currentTenantId.isBlank()
        || DEFAULT_TENANT.equals(currentTenantId)
        || !currentTenantId.equals(tenantId)) {
      TenantContext.setTenantId(tenantId);
    }
  }

  private String currentTenantId() {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank() || DEFAULT_TENANT.equals(tenantId)) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    return tenantId;
  }

  private String extractExt(String filename) {
    if (filename == null) {
      return "";
    }
    int idx = filename.lastIndexOf('.');
    if (idx == -1) {
      return "";
    }
    return filename.substring(idx);
  }

  private String sanitizeFieldKey(String fieldKey) {
    return fieldKey == null ? "unknown" : fieldKey.replaceAll("[^a-zA-Z0-9_-]", "_");
  }
}

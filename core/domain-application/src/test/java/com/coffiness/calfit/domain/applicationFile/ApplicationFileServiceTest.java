package com.coffiness.calfit.domain.applicationFile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coffiness.calfit.api.v1.request.PresignUploadRequest;
import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.core.enums.UploadStatus;
import com.coffiness.calfit.storage.db.core.application.ApplicationEntity;
import com.coffiness.calfit.storage.db.core.application.ApplicationFileEntity;
import com.coffiness.calfit.storage.db.core.application.ApplicationFileRepository;
import com.coffiness.calfit.storage.db.core.application.ApplicationRepository;
import com.coffiness.calfit.storage.db.core.member.MemberEntity;
import com.coffiness.calfit.storage.db.core.member.MemberRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApplicationFileService")
class ApplicationFileServiceTest {

  @Mock private ApplicationFileRepository applicationFileRepository;
  @Mock private ApplicationRepository applicationRepository;
  @Mock private MemberRepository memberRepository;
  @Mock private S3Presigner presigner;
  @Mock private S3Client s3Client;

  private ApplicationFileService service;

  @BeforeEach
  void setUp() {
    service =
        new ApplicationFileService(
            applicationFileRepository,
            applicationRepository,
            memberRepository,
            presigner,
            s3Client);
  }

  @Test
  void 지원서_소유자가_아니면_presign_upload를_요청할_수_없다() {
    ApplicationEntity application = mock(ApplicationEntity.class);
    when(applicationRepository.findTenantIdById(10L)).thenReturn(Optional.of("tenant-a"));
    when(applicationRepository.findByIdAndStatus(10L, EntityStatus.ACTIVE))
        .thenReturn(Optional.of(application));
    when(application.getApplicantId()).thenReturn(100L);

    PresignUploadRequest request =
        new PresignUploadRequest(10L, 100L, "resume", "resume.pdf", "application/pdf");
    SecurityUser requester = new SecurityUser(200L, "applicant@test.com", "APPLICANT");

    assertThatThrownBy(() -> service.presignUpload(request, requester))
        .isInstanceOf(CoreException.class)
        .extracting(ex -> ((CoreException) ex).getErrorType())
        .isEqualTo(ErrorType.UNAUTHORIZED);

    verify(applicationFileRepository, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void HR_멤버가_아니면_presign_download를_요청할_수_없다() {
    ApplicationFileEntity file = mock(ApplicationFileEntity.class);
    ApplicationEntity application = mock(ApplicationEntity.class);
    MemberEntity member = mock(MemberEntity.class);

    when(applicationFileRepository.findTenantIdById(5L)).thenReturn(Optional.of("tenant-a"));
    when(applicationFileRepository.findById(5L)).thenReturn(Optional.of(file));
    when(file.getUploadStatus()).thenReturn(UploadStatus.ACTIVE);
    when(file.getApplicationId()).thenReturn(10L);
    when(applicationRepository.findByIdAndStatus(10L, EntityStatus.ACTIVE))
        .thenReturn(Optional.of(application));
    when(memberRepository.findByTenantIdAndUserIdAndStatus("tenant-a", 7L, EntityStatus.ACTIVE))
        .thenReturn(member);
    when(member.getMemberType()).thenReturn(MemberType.INTERVIEWER);

    SecurityUser requester = new SecurityUser(7L, "user@test.com", "USER");

    assertThatThrownBy(() -> service.presignDownload(5L, requester))
        .isInstanceOf(CoreException.class)
        .extracting(ex -> ((CoreException) ex).getErrorType())
        .isEqualTo(ErrorType.UNAUTHORIZED);

    verify(applicationRepository)
        .findByIdAndStatus(anyLong(), org.mockito.ArgumentMatchers.eq(EntityStatus.ACTIVE));
  }
}

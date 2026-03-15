package com.coffiness.calfit.api.application_files.presign_upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.ApplicationFileFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.v1.request.PresignUploadRequest;
import com.coffiness.calfit.api.v1.response.PresignUploadResponse;
import com.coffiness.calfit.domain.applicationFile.ApplicationFileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;

@CalfitApiTest
@DisplayName("POST /api/v1/application-files/presign-upload")
public class POST_specs {

  @MockBean private ApplicationFileService applicationFileService;

  @Test
  void 올바른_토큰으로_presign_upload를_요청하면_성공한다(
      @Autowired ApplicationFileFixture fixture, @Autowired UserFixture userFixture) {
    String token = userFixture.createUserAndGetToken();
    PresignUploadResponse stub =
        new PresignUploadResponse(1L, "https://example.com/upload", "uploads/1/1/resume.pdf", 10);
    when(applicationFileService.presignUpload(any(PresignUploadRequest.class), any()))
        .thenReturn(stub);

    PresignUploadRequest request =
        new PresignUploadRequest(1L, 1L, "resume", "resume.pdf", "application/pdf");

    ResponseEntity<PresignUploadResponse> response = fixture.presignUpload(token, request);

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().uploadUrl()).isEqualTo("https://example.com/upload");
  }

  @Test
  void 토큰이_없으면_401_Unauthorized를_반환한다(@Autowired ApplicationFileFixture fixture) {
    PresignUploadRequest request =
        new PresignUploadRequest(1L, 1L, "resume", "resume.pdf", "application/pdf");

    ResponseEntity<String> response = fixture.presignUploadWithoutToken(request);

    assertThat(response.getStatusCode().value()).isEqualTo(401);
  }
}

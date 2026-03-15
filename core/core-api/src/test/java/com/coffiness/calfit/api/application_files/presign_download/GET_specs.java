package com.coffiness.calfit.api.application_files.presign_download;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.ApplicationFileFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.v1.response.PresignDownloadResponse;
import com.coffiness.calfit.domain.applicationFile.ApplicationFileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;

@CalfitApiTest
@DisplayName("GET /api/v1/application-files/{fileId}/presign-download")
public class GET_specs {

  @MockBean private ApplicationFileService applicationFileService;

  @Test
  void 올바른_토큰으로_presign_download를_요청하면_성공한다(
      @Autowired ApplicationFileFixture fixture, @Autowired UserFixture userFixture) {
    String token = userFixture.createUserAndGetToken();
    PresignDownloadResponse stub = new PresignDownloadResponse("https://example.com/download", 10);
    when(applicationFileService.presignDownload(eq(1L), any())).thenReturn(stub);

    ResponseEntity<PresignDownloadResponse> response = fixture.presignDownload(token, 1L);

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().downloadUrl()).isEqualTo("https://example.com/download");
  }

  @Test
  void 토큰이_없으면_401_Unauthorized를_반환한다(@Autowired ApplicationFileFixture fixture) {
    ResponseEntity<String> response = fixture.presignDownloadWithoutToken(1L);

    assertThat(response.getStatusCode().value()).isEqualTo(401);
  }
}

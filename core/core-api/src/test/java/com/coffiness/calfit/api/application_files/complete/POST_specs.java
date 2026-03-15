package com.coffiness.calfit.api.application_files.complete;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.ApplicationFileFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.v1.request.CompleteUploadRequest;
import com.coffiness.calfit.api.v1.response.CompleteUploadResponse;
import com.coffiness.calfit.domain.applicationFile.ApplicationFileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;

@CalfitApiTest
@DisplayName("POST /api/v1/application-files/complete")
public class POST_specs {

  @MockBean private ApplicationFileService applicationFileService;

  @Test
  void 올바른_토큰으로_complete를_요청하면_성공한다(
      @Autowired ApplicationFileFixture fixture, @Autowired UserFixture userFixture) {
    String token = userFixture.createUserAndGetToken();
    CompleteUploadResponse stub = new CompleteUploadResponse(1L, "ACTIVE", 1024L);
    when(applicationFileService.completeUpload(any(CompleteUploadRequest.class), any()))
        .thenReturn(stub);

    ResponseEntity<CompleteUploadResponse> response =
        fixture.completeUpload(token, new CompleteUploadRequest(1L));

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().uploadStatus()).isEqualTo("ACTIVE");
  }

  @Test
  void 토큰이_없으면_401_Unauthorized를_반환한다(@Autowired ApplicationFileFixture fixture) {
    ResponseEntity<String> response =
        fixture.completeUploadWithoutToken(new CompleteUploadRequest(1L));

    assertThat(response.getStatusCode().value()).isEqualTo(401);
  }
}

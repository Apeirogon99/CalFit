package com.coffiness.calfit.api.fixture;

import com.coffiness.calfit.api.v1.request.CompleteUploadRequest;
import com.coffiness.calfit.api.v1.request.PresignUploadRequest;
import com.coffiness.calfit.api.v1.response.CompleteUploadResponse;
import com.coffiness.calfit.api.v1.response.PresignDownloadResponse;
import com.coffiness.calfit.api.v1.response.PresignUploadResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.web.client.LocalHostUriTemplateHandler;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public record ApplicationFileFixture(TestRestTemplate client, ObjectMapper objectMapper) {

  public static ApplicationFileFixture create(Environment environment, ObjectMapper objectMapper) {
    TestRestTemplate client = new TestRestTemplate(new RestTemplateBuilder());
    LocalHostUriTemplateHandler uriTemplateHandler = new LocalHostUriTemplateHandler(environment);
    client.setUriTemplateHandler(uriTemplateHandler);
    return new ApplicationFileFixture(client, objectMapper);
  }

  public ResponseEntity<PresignUploadResponse> presignUpload(
      String token, PresignUploadRequest request) {
    return presignUpload(token, null, request);
  }

  public ResponseEntity<PresignUploadResponse> presignUpload(
      String token, String tenantId, PresignUploadRequest request) {
    String url = "/api/v1/application-files/presign-upload";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (token != null && !token.isBlank()) {
      headers.setBearerAuth(token);
    }
    if (tenantId != null && !tenantId.isBlank()) {
      headers.set("X-Tenant-ID", tenantId);
    }
    HttpEntity<PresignUploadRequest> entity = new HttpEntity<>(request, headers);
    return client.postForEntity(url, entity, PresignUploadResponse.class);
  }

  public ResponseEntity<String> presignUploadRaw(String token, PresignUploadRequest request) {
    return presignUploadRaw(token, null, request);
  }

  public ResponseEntity<String> presignUploadRaw(
      String token, String tenantId, PresignUploadRequest request) {
    String url = "/api/v1/application-files/presign-upload";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (token != null && !token.isBlank()) {
      headers.setBearerAuth(token);
    }
    if (tenantId != null && !tenantId.isBlank()) {
      headers.set("X-Tenant-ID", tenantId);
    }
    HttpEntity<PresignUploadRequest> entity = new HttpEntity<>(request, headers);
    return client.postForEntity(url, entity, String.class);
  }

  public ResponseEntity<String> presignUploadWithoutToken(PresignUploadRequest request) {
    String url = "/api/v1/application-files/presign-upload";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<PresignUploadRequest> entity = new HttpEntity<>(request, headers);
    return client.postForEntity(url, entity, String.class);
  }

  public ResponseEntity<CompleteUploadResponse> completeUpload(
      String token, CompleteUploadRequest request) {
    String url = "/api/v1/application-files/complete";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (token != null && !token.isBlank()) {
      headers.setBearerAuth(token);
    }
    HttpEntity<CompleteUploadRequest> entity = new HttpEntity<>(request, headers);
    return client.postForEntity(url, entity, CompleteUploadResponse.class);
  }

  public ResponseEntity<String> completeUploadWithoutToken(CompleteUploadRequest request) {
    String url = "/api/v1/application-files/complete";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<CompleteUploadRequest> entity = new HttpEntity<>(request, headers);
    return client.postForEntity(url, entity, String.class);
  }

  public ResponseEntity<PresignDownloadResponse> presignDownload(String token, Long fileId) {
    String url = String.format("/api/v1/application-files/%d/presign-download", fileId);
    HttpHeaders headers = new HttpHeaders();
    if (token != null && !token.isBlank()) {
      headers.setBearerAuth(token);
    }
    HttpEntity<Void> entity = new HttpEntity<>(headers);
    return client.exchange(url, HttpMethod.GET, entity, PresignDownloadResponse.class);
  }

  public ResponseEntity<String> presignDownloadWithoutToken(Long fileId) {
    String url = String.format("/api/v1/application-files/%d/presign-download", fileId);
    HttpHeaders headers = new HttpHeaders();
    HttpEntity<Void> entity = new HttpEntity<>(headers);
    return client.exchange(url, HttpMethod.GET, entity, String.class);
  }
}

package com.coffiness.calfit.api.fixture;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public record ApplicationExcelFixture(BaseFixture base) {

  public static ApplicationExcelFixture create(Environment environment, ObjectMapper objectMapper) {
    return new ApplicationExcelFixture(BaseFixture.create(environment, objectMapper));
  }

  public ResponseEntity<byte[]> export(String token, String tenantId) {
    return exchange("/api/v1/applications/excel", token, tenantId);
  }

  public ResponseEntity<byte[]> export(String token, String tenantId, String keyword) {
    if (keyword == null || keyword.isBlank()) {
      return export(token, tenantId);
    }

    return exchange("/api/v1/applications/excel?keyword={keyword}", token, tenantId, keyword);
  }

  private ResponseEntity<byte[]> exchange(
      String url, String token, String tenantId, Object... urlVariables) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (token != null && !token.isBlank()) {
      headers.setBearerAuth(token);
    }
    if (tenantId != null && !tenantId.isBlank()) {
      headers.set("X-Tenant-ID", tenantId);
    }

    return base.client()
        .exchange(url, HttpMethod.GET, new HttpEntity<>(headers), byte[].class, urlVariables);
  }
}

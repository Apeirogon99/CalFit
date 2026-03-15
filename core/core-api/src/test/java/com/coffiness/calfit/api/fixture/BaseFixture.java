package com.coffiness.calfit.api.fixture;

import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.web.client.LocalHostUriTemplateHandler;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.env.Environment;
import org.springframework.http.*;

public record BaseFixture(TestRestTemplate client, ObjectMapper objectMapper) {

  public static BaseFixture create(Environment environment, ObjectMapper objectMapper) {
    TestRestTemplate client = new TestRestTemplate(new RestTemplateBuilder());
    LocalHostUriTemplateHandler uriTemplateHandler = new LocalHostUriTemplateHandler(environment);
    client.setUriTemplateHandler(uriTemplateHandler);
    return new BaseFixture(client, objectMapper);
  }

  // ==================== GET ====================

  public <T> ApiResponse<T> get(String url, Class<T> responseType, Object... urlVariables) {
    return get(url, null, responseType, urlVariables);
  }

  public <T> ApiResponse<T> get(
      String url, String token, Class<T> responseType, Object... urlVariables) {
    return exchange(url, HttpMethod.GET, null, token, responseType, urlVariables);
  }

  // ==================== POST ====================

  public <T> ApiResponse<T> post(
      String url, Object request, Class<T> responseType, Object... urlVariables) {
    return post(url, request, null, responseType, urlVariables);
  }

  public <T> ApiResponse<T> post(
      String url, Object request, String token, Class<T> responseType, Object... urlVariables) {
    return exchange(url, HttpMethod.POST, request, token, responseType, urlVariables);
  }

  // ==================== PUT ====================

  public <T> ApiResponse<T> put(
      String url, Object request, Class<T> responseType, Object... urlVariables) {
    return put(url, request, null, responseType, urlVariables);
  }

  public <T> ApiResponse<T> put(
      String url, Object request, String token, Class<T> responseType, Object... urlVariables) {
    return exchange(url, HttpMethod.PUT, request, token, responseType, urlVariables);
  }

  // ==================== PATCH ====================

  public <T> ApiResponse<T> patch(
      String url, Object request, Class<T> responseType, Object... urlVariables) {
    return patch(url, request, null, responseType, urlVariables);
  }

  public <T> ApiResponse<T> patch(
      String url, Object request, String token, Class<T> responseType, Object... urlVariables) {
    return exchange(url, HttpMethod.PATCH, request, token, responseType, urlVariables);
  }

  // ==================== DELETE ====================

  public <T> ApiResponse<T> delete(String url, Class<T> responseType, Object... urlVariables) {
    return delete(url, null, null, responseType, urlVariables);
  }

  public <T> ApiResponse<T> delete(
      String url, Object request, Class<T> responseType, Object... urlVariables) {
    return delete(url, request, null, responseType, urlVariables);
  }

  public <T> ApiResponse<T> delete(
      String url, String token, Class<T> responseType, Object... urlVariables) {
    return delete(url, null, token, responseType, urlVariables);
  }

  public <T> ApiResponse<T> delete(
      String url, Object request, String token, Class<T> responseType, Object... urlVariables) {
    return exchange(url, HttpMethod.DELETE, request, token, responseType, urlVariables);
  }

  // ==================== Core Exchange Method ====================

  @SuppressWarnings("unchecked")
  private <T> ApiResponse<T> exchange(
      String url,
      HttpMethod method,
      Object request,
      String token,
      Class<T> responseType,
      Object... urlVariables) {
    HttpHeaders headers = createHeaders(token);
    HttpEntity<?> entity =
        (request != null) ? new HttpEntity<>(request, headers) : new HttpEntity<>(headers);

    ResponseEntity<ApiResponse> response =
        client.exchange(url, method, entity, ApiResponse.class, urlVariables);

    return convertResponse(response.getBody(), responseType);
  }

  private HttpHeaders createHeaders(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (token != null && !token.isBlank()) {
      headers.setBearerAuth(token);
    }
    return headers;
  }

  @SuppressWarnings("unchecked")
  private <T> ApiResponse<T> convertResponse(ApiResponse<?> response, Class<T> responseType) {
    if (response == null) {
      return null;
    }

    if (response.getResult() == ResultType.ERROR || response.getData() == null) {
      return (ApiResponse<T>) response;
    }

    if (responseType == Void.class) {
      return (ApiResponse<T>) response;
    }

    T convertedData = objectMapper.convertValue(response.getData(), responseType);
    return ApiResponse.success(convertedData);
  }
}

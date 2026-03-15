package com.coffiness.calfit.api.fixture;

import com.coffiness.calfit.api.v1.request.ApplicantLoginRequest;
import com.coffiness.calfit.api.v1.request.ApplicantSignUpRequest;
import com.coffiness.calfit.api.v1.response.ApplicantLoginResponse;
import com.coffiness.calfit.api.v1.response.ApplicantResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.core.env.Environment;

public record ApplicantFixture(BaseFixture base) {

  public static ApplicantFixture create(Environment environment, ObjectMapper objectMapper) {
    return new ApplicantFixture(BaseFixture.create(environment, objectMapper));
  }

  public String randomEmail() {
    return "applicant-" + UUID.randomUUID() + "@test.com";
  }

  public String randomPassword() {
    return "pass1234";
  }

  public String randomName() {
    return "Applicant-" + UUID.randomUUID().toString().substring(0, 8);
  }

  public ApiResponse<ApplicantResponse> signUp(
      String workspaceId, String email, String password, String name) {
    ApplicantSignUpRequest request = new ApplicantSignUpRequest(email, password, name);
    return base.post(
        "/api/v1/workspaces/{workspaceId}/applicants/signup",
        request,
        ApplicantResponse.class,
        workspaceId);
  }

  public ApiResponse<ApplicantLoginResponse> login(
      String workspaceId, String email, String password) {
    ApplicantLoginRequest request = new ApplicantLoginRequest(email, password);
    return base.post(
        "/api/v1/workspaces/{workspaceId}/applicants/login",
        request,
        ApplicantLoginResponse.class,
        workspaceId);
  }
}

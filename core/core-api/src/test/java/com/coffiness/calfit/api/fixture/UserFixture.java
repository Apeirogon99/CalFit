package com.coffiness.calfit.api.fixture;

import com.coffiness.calfit.api.v1.request.LoginRequest;
import com.coffiness.calfit.api.v1.request.SignUpRequest;
import com.coffiness.calfit.api.v1.response.LoginResponse;
import com.coffiness.calfit.api.v1.response.UserResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.core.env.Environment;

public record UserFixture(BaseFixture base) {

  public static UserFixture create(Environment environment, ObjectMapper objectMapper) {
    return new UserFixture(BaseFixture.create(environment, objectMapper));
  }

  // ==================== Random Data Generators ====================

  public String randomEmail() {
    return "user-" + UUID.randomUUID() + "@test.com";
  }

  public String randomPassword() {
    return "password123";
  }

  public String randomName() {
    return "TestUser-" + UUID.randomUUID().toString().substring(0, 8);
  }

  // ==================== API Calls ====================

  public ApiResponse<UserResponse> signUp() {
    return signUp(randomEmail(), randomPassword(), randomName());
  }

  public ApiResponse<UserResponse> signUp(String email, String password, String name) {
    SignUpRequest request = new SignUpRequest(email, password, name);
    return base.post("/api/v1/users/signup", request, UserResponse.class);
  }

  public ApiResponse<LoginResponse> login(String email, String password) {
    LoginRequest request = new LoginRequest(email, password);
    return base.post("/api/v1/users/login", request, LoginResponse.class);
  }

  public ApiResponse<UserResponse> me(String token) {
    return base.get("/api/v1/users/me", token, UserResponse.class);
  }

  public ApiResponse<Void> deleteMe(String token) {
    return base.delete("/api/v1/users/me", token, Void.class);
  }

  // ==================== Convenience Methods ====================

  public String createUserAndGetToken() {
    String email = randomEmail();
    String password = randomPassword();
    signUp(email, password, randomName());
    ApiResponse<LoginResponse> loginResponse = login(email, password);
    return loginResponse.getData().accessToken();
  }

  public String createUserAndGetToken(String email, String password, String name) {
    signUp(email, password, name);
    ApiResponse<LoginResponse> loginResponse = login(email, password);
    return loginResponse.getData().accessToken();
  }
}

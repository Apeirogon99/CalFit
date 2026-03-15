package com.coffiness.calfit.api.fixture;

import com.coffiness.calfit.api.v1.request.CreateWorkspaceRequest;
import com.coffiness.calfit.api.v1.response.WorkspaceResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.env.Environment;

public record WorkspaceFixture(BaseFixture base, UserFixture userFixture) {

  public static WorkspaceFixture create(Environment environment, ObjectMapper objectMapper) {
    BaseFixture base = BaseFixture.create(environment, objectMapper);
    UserFixture userFixture = UserFixture.create(environment, objectMapper);
    return new WorkspaceFixture(base, userFixture);
  }

  // ==================== API Calls ====================

  public ApiResponse<WorkspaceResponse> createWorkspace(
      String token, String name, String contactPhone, String employeeScale) {
    CreateWorkspaceRequest request = new CreateWorkspaceRequest(name, contactPhone, employeeScale);
    return base.post("/api/v1/workspaces", request, token, WorkspaceResponse.class);
  }

  public ApiResponse<WorkspaceResponse> createWorkspace(String token) {
    return createWorkspace(token, "스타트업허브", "010-1234-5678", "1-10");
  }

  // ==================== Convenience Methods ====================

  public String createUserAndGetToken() {
    return userFixture.createUserAndGetToken();
  }
}

package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.domain.workspace.Workspace;

public record WorkspaceResponse(
    String workspaceId, String name, String contactPhone, String employeeScale) {

  public static WorkspaceResponse from(Workspace workspace) {
    return new WorkspaceResponse(
        workspace.workspaceId(),
        workspace.name(),
        workspace.contactPhone(),
        workspace.employeeScale());
  }
}

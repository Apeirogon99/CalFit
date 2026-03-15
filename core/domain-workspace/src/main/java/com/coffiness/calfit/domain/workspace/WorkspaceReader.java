package com.coffiness.calfit.domain.workspace;

import java.util.List;

public interface WorkspaceReader {

  List<Workspace> getAllWorkspaces();

  Workspace getWorkspace(String workspaceId);
}

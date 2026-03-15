package com.coffiness.calfit.storage.db.core.workspace;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<WorkspaceEntity, Long> {

  Optional<WorkspaceEntity> findByWorkspaceId(String workspaceId);
}

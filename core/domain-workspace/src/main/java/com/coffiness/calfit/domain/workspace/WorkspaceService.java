package com.coffiness.calfit.domain.workspace;

import com.coffiness.calfit.domain.workspace.event.WorkspaceCreatedEvent;
import com.coffiness.calfit.storage.db.core.workspace.WorkspaceEntity;
import com.coffiness.calfit.storage.db.core.workspace.WorkspaceRepository;
import com.coffiness.calfit.support.event.DomainEventPublisher;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkspaceService {
  private final WorkspaceRepository workspaceRepository;
  private final DomainEventPublisher eventPublisher;

  @Transactional
  public Workspace createWorkspace(String name, String contactPhone, String employeeScale) {
    String workspaceId = UUID.randomUUID().toString();
    WorkspaceEntity entity = WorkspaceEntity.create(workspaceId, name, contactPhone, employeeScale);
    WorkspaceEntity saved = workspaceRepository.save(entity);
    eventPublisher.publish(WorkspaceCreatedEvent.of(workspaceId));
    return toWorkspace(saved);
  }

  private Workspace toWorkspace(WorkspaceEntity entity) {
    return new Workspace(
        entity.getId(),
        entity.getWorkspaceId(),
        entity.getName(),
        entity.getContactPhone(),
        entity.getEmployeeScale());
  }
}

package com.coffiness.calfit.infra;

import com.coffiness.calfit.domain.workspace.group.Group;
import com.coffiness.calfit.domain.workspace.group.GroupStore;
import com.coffiness.calfit.storage.db.core.user.GroupEntity;
import com.coffiness.calfit.storage.db.core.user.GroupRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GroupStoreImpl implements GroupStore {
  private final GroupRepository groupRepository;

  @Override
  public Group save(String name, String color) {
    GroupEntity entity = GroupEntity.create(name, color);
    GroupEntity saved = groupRepository.save(entity);
    return new Group(saved.getId(), saved.getTenantId(), saved.getName(), saved.getColor());
  }

  @Override
  public void updateInfo(Long groupId, String name, String color) {
    GroupEntity entity =
        groupRepository.findById(groupId).orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    entity.updateInfo(name, color);
  }

  @Override
  public void remove(Long groupId) {
    GroupEntity entity =
        groupRepository.findById(groupId).orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    entity.deleted();
  }
}

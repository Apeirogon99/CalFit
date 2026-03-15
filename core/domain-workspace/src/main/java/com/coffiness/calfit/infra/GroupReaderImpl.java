package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.domain.workspace.group.Group;
import com.coffiness.calfit.domain.workspace.group.GroupReader;
import com.coffiness.calfit.storage.db.core.user.GroupEntity;
import com.coffiness.calfit.storage.db.core.user.GroupRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GroupReaderImpl implements GroupReader {
  private final GroupRepository groupRepository;

  @Override
  public Group getGroup(Long groupId) {
    return groupRepository
        .findById(groupId)
        .map(this::toGroup)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
  }

  @Override
  public List<Group> getGroups() {
    return groupRepository.findByStatus(EntityStatus.ACTIVE).stream().map(this::toGroup).toList();
  }

  @Override
  public Map<Long, String> getGroupNameMap(List<Long> groupIds) {
    return groupRepository.findAllByIdInAndStatus(groupIds, EntityStatus.ACTIVE).stream()
        .collect(Collectors.toMap(GroupEntity::getId, GroupEntity::getName));
  }

  @Override
  public boolean existsByName(String name) {
    return groupRepository.existsByNameAndStatus(name, EntityStatus.ACTIVE);
  }

  private Group toGroup(GroupEntity entity) {
    return new Group(entity.getId(), entity.getTenantId(), entity.getName(), entity.getColor());
  }
}

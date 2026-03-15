package com.coffiness.calfit.domain.workspace.group;

import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupService {
  private final GroupReader groupReader;
  private final GroupStore groupStore;

  @Transactional(readOnly = true)
  public Group getGroup(Long groupId) {
    return groupReader.getGroup(groupId);
  }

  @Transactional(readOnly = true)
  public List<Group> getGroups() {
    return groupReader.getGroups();
  }

  @Transactional
  public GroupInfo createGroup(String name, String color) {
    if (groupReader.existsByName(name)) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    Group group = groupStore.save(name, color);
    return new GroupInfo(group.id(), group.name(), group.color(), 0L);
  }

  @Transactional
  public void updateGroup(Long groupId, String name, String color) {
    groupStore.updateInfo(groupId, name, color);
  }

  @Transactional
  public void removeGroup(Long groupId) {
    groupStore.remove(groupId);
  }

  @Transactional(readOnly = true)
  public Map<Long, String> getGroupNameMap(List<Long> groupIds) {
    return groupReader.getGroupNameMap(groupIds);
  }
}

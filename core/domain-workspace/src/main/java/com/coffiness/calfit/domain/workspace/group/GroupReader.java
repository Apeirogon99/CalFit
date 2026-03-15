package com.coffiness.calfit.domain.workspace.group;

import java.util.List;
import java.util.Map;

public interface GroupReader {

  Group getGroup(Long groupId);

  List<Group> getGroups();

  Map<Long, String> getGroupNameMap(List<Long> groupIds);

  boolean existsByName(String name);
}

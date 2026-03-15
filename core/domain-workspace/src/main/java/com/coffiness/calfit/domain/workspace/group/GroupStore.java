package com.coffiness.calfit.domain.workspace.group;

public interface GroupStore {

  Group save(String name, String color);

  void updateInfo(Long groupId, String name, String color);

  void remove(Long groupId);
}

package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.domain.workspace.group.GroupInfo;

public record GroupResponse(Long id, String name, String color, long memberCount) {

  public static GroupResponse from(GroupInfo info) {
    return new GroupResponse(info.id(), info.name(), info.color(), info.memberCount());
  }
}

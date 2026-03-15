package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.domain.workspace.member.MemberInfo;
import java.time.LocalDateTime;

public record MemberResponse(
    Long id,
    Long userId,
    String name,
    LocalDateTime createdAt,
    LocalDateTime recentAt,
    Long groupId,
    String group,
    MemberType memberType) {

  public static MemberResponse from(MemberInfo info) {
    return new MemberResponse(
        info.id(),
        info.userId(),
        info.name(),
        info.createdAt(),
        info.recentAt(),
        info.groupId(),
        info.group(),
        info.memberType());
  }
}

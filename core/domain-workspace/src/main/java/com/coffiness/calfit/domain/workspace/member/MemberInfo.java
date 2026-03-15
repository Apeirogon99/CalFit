package com.coffiness.calfit.domain.workspace.member;

import com.coffiness.calfit.core.enums.MemberType;
import java.time.LocalDateTime;

public record MemberInfo(
    Long id,
    Long userId,
    String name,
    LocalDateTime createdAt,
    LocalDateTime recentAt,
    Long groupId,
    String group,
    MemberType memberType) {}

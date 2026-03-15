package com.coffiness.calfit.domain.workspace.member;

import com.coffiness.calfit.core.enums.MemberType;

public record Member(
    Long id, String workspaceId, Long userId, MemberType memberType, Long groupId) {}

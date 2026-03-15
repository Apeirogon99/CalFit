package com.coffiness.calfit.storage.db.core.member;

import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "workspace_members")
public class MemberEntity extends TenantBaseEntity {

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MemberType memberType;

  @Column(name = "group_id")
  private Long groupId;

  protected MemberEntity() {}

  private MemberEntity(Long userId, MemberType memberType) {
    this.userId = userId;
    this.memberType = memberType;
  }

  public static MemberEntity create(Long userId, MemberType memberType) {
    return new MemberEntity(userId, memberType);
  }

  public Long getUserId() {
    return userId;
  }

  public MemberType getMemberType() {
    return memberType;
  }

  public Long getGroupId() {
    return groupId;
  }

  public void updateMemberType(MemberType memberType) {
    this.memberType = memberType;
  }

  public void assignGroup(Long groupId) {
    this.groupId = groupId;
  }
}

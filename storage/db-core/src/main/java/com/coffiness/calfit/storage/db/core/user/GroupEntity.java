package com.coffiness.calfit.storage.db.core.user;

import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "user_groups")
public class GroupEntity extends TenantBaseEntity {

  // 이름
  @Column(name = "name", nullable = false)
  private String name;

  // 색상
  @Column(name = "color")
  private String color;

  protected GroupEntity() {
    super();
  }

  private GroupEntity(String name, String color) {
    this.name = name;
    this.color = color;
  }

  public static GroupEntity create(String name, String color) {
    return new GroupEntity(name, color);
  }

  public String getName() {
    return name;
  }

  public String getColor() {
    return color;
  }

  public void updateInfo(String name, String color) {
    this.name = name;
    this.color = color;
  }
}

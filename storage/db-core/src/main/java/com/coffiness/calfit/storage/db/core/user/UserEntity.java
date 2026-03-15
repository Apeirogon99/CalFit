package com.coffiness.calfit.storage.db.core.user;

import com.coffiness.calfit.core.enums.UserRole;
import com.coffiness.calfit.storage.db.core.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class UserEntity extends BaseEntity {

  // 이메일
  @Column(nullable = false, unique = true)
  private String email;

  // 비밀번호
  @Column(nullable = false)
  private String password;

  // 이름
  @Column(name = "name", length = 50, nullable = false)
  private String name;

  // 사용자 역할
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserRole role;

  protected UserEntity() {}

  private UserEntity(String email, String password, String name, UserRole role) {
    this.email = email;
    this.password = password;
    this.name = name;
    this.role = role;
  }

  public static UserEntity createMember(String email, String password, String name) {
    return new UserEntity(email, password, name, UserRole.MEMBER);
  }

  public static UserEntity createAdmin(String email, String password, String name) {
    return new UserEntity(email, password, name, UserRole.ADMIN);
  }

  public String getEmail() {
    return email;
  }

  public String getPassword() {
    return password;
  }

  public String getName() {
    return name;
  }

  public UserRole getRole() {
    return role;
  }
}

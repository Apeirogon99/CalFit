package com.coffiness.calfit.storage.db.core.workspace;

import com.coffiness.calfit.storage.db.core.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "workspaces")
public class WorkspaceEntity extends BaseEntity {

  @Column(nullable = false, unique = true)
  private String workspaceId;

  @Column(nullable = false)
  private String name;

  @Column private String contactPhone;

  @Column private String employeeScale;

  protected WorkspaceEntity() {}

  private WorkspaceEntity(
      String workspaceId, String name, String contactPhone, String employeeScale) {
    this.workspaceId = workspaceId;
    this.name = name;
    this.contactPhone = contactPhone;
    this.employeeScale = employeeScale;
  }

  public static WorkspaceEntity create(
      String workspaceId, String name, String contactPhone, String employeeScale) {
    return new WorkspaceEntity(workspaceId, name, contactPhone, employeeScale);
  }

  public String getWorkspaceId() {
    return workspaceId;
  }

  public String getName() {
    return name;
  }

  public String getContactPhone() {
    return contactPhone;
  }

  public String getEmployeeScale() {
    return employeeScale;
  }
}

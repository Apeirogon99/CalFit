package com.coffiness.calfit.storage.db.core;

import com.coffiness.calfit.core.enums.EntityStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@MappedSuperclass
public abstract class BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  private EntityStatus status = EntityStatus.ACTIVE;

  @CreationTimestamp @Column private LocalDateTime createdAt;

  @UpdateTimestamp @Column private LocalDateTime updatedAt;

  public Long getId() {
    return id;
  }

  public void active() {
    status = EntityStatus.ACTIVE;
  }

  public boolean isActive() {
    return status == EntityStatus.ACTIVE;
  }

  public void deleted() {
    status = EntityStatus.DELETED;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}

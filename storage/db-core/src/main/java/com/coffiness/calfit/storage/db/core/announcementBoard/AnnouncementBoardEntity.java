package com.coffiness.calfit.storage.db.core.announcementBoard;

import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "announcement_boards")
@SQLDelete(
    sql =
        "UPDATE announcement_boards SET status = 'DELETED', updated_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("status = 'ACTIVE'")
@NoArgsConstructor
@Getter
public class AnnouncementBoardEntity extends TenantBaseEntity {

  @Column(name = "title", nullable = false, length = 100)
  private String title;

  @Column(name = "content", nullable = false, length = 2000)
  private String content;

  @Column(name = "is_pinned", nullable = false)
  private Boolean pinned;

  @Builder
  public AnnouncementBoardEntity(String title, String content, Boolean pinned) {
    this.title = title;
    this.content = content;
    this.pinned = pinned;
  }

  public void update(String title, String content, Boolean pinned) {
    this.title = title;
    this.content = content;
    this.pinned = pinned;
  }
}

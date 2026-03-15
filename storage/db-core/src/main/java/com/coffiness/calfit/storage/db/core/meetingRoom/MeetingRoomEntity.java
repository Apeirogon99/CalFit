package com.coffiness.calfit.storage.db.core.meetingRoom;

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
@Table(name = "meeting_rooms")
@SQLDelete(
    sql =
        "UPDATE meeting_rooms SET status = 'DELETED', updated_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("status = 'ACTIVE'")
@NoArgsConstructor
@Getter
public class MeetingRoomEntity extends TenantBaseEntity {

  // 회의실 이름
  @Column(name = "name", nullable = false)
  private String name;

  // 회의실 위치
  @Column(name = "location", nullable = false)
  private Integer location;

  // 수용 인원
  @Column(name = "capacity", nullable = false)
  private Integer capacity;

  @Column(name = "description", length = 1000)
  private String description;

  @Column(name = "facilities", columnDefinition = "TEXT", nullable = false)
  private String facilities;

  @Column(name = "color", length = 20)
  private String color;

  @Builder
  public MeetingRoomEntity(
      String name,
      Integer location,
      Integer capacity,
      String description,
      String facilities,
      String color) {
    this.name = name;
    this.location = location;
    this.capacity = capacity;
    this.description = description;
    this.facilities = facilities;
    this.color = color;
  }

  public void update(
      String name,
      Integer location,
      Integer capacity,
      String description,
      String facilities,
      String color) {
    this.name = name;
    this.location = location;
    this.capacity = capacity;
    this.description = description;
    this.facilities = facilities;
    this.color = color;
  }

  public void archiveForDelete() {
    this.name = this.name + "__deleted__" + System.currentTimeMillis();
    deleted();
  }
}

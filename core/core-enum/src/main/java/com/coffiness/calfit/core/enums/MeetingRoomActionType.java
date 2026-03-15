package com.coffiness.calfit.core.enums;

public enum MeetingRoomActionType {
  RESERVED("예약됨"),
  CANCELED("취소됨"),
  UPDATED_TIME("시간 변경"),
  CHANGED_ROOM("회의실 변경"),
  CREATED_ROOM("회의실 생성"),
  UPDATED_ROOM("회의실 수정"),
  DELETED_ROOM("회의실 삭제");

  private final String description;

  MeetingRoomActionType(String description) {
    this.description = description;
  }
}

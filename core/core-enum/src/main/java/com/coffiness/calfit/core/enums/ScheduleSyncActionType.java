package com.coffiness.calfit.core.enums;

import lombok.Getter;

/*
 * 일정의 구글 동기화 액션 타입
 * */
@Getter
public enum ScheduleSyncActionType {
  CREATE("일정 생성 반영"),
  UPDATE("일정 수정 반영"),
  DELETE("일정 삭제 반영");

  private final String description;

  ScheduleSyncActionType(String description) {
    this.description = description;
  }
}

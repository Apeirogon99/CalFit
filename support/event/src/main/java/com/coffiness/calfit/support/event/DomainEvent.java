package com.coffiness.calfit.support.event;

/** 도메인 이벤트 마커 인터페이스 */
public interface DomainEvent {

  /** 이벤트 발생 시간 (Unix timestamp) */
  long occurredAt();
}

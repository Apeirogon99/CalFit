package com.coffiness.calfit.support.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** 도메인 이벤트 발행 유틸리티 */
@Component
public class DomainEventPublisher {

  private final ApplicationEventPublisher applicationEventPublisher;

  public DomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
    this.applicationEventPublisher = applicationEventPublisher;
  }

  public void publish(DomainEvent event) {
    applicationEventPublisher.publishEvent(event);
  }
}

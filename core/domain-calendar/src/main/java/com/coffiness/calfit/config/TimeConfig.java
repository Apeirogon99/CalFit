package com.coffiness.calfit.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
 * 시간 관련 공통 빈 설정
 * */
@Configuration
public class TimeConfig {

  @Bean
  public Clock systemClock() {
    return Clock.systemDefaultZone();
  }
}

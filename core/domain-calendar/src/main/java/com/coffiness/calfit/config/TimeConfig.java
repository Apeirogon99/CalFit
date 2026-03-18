package com.coffiness.calfit.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
 * 시간 관련 공통 빈 설정
 * */
@Configuration
public class TimeConfig {

  @Bean
  public ZoneId appZoneId(@Value("${app.time-zone:Asia/Seoul}") String timeZone) {
    return ZoneId.of(timeZone);
  }

  @Bean
  public Clock systemClock(ZoneId appZoneId) {
    return Clock.system(appZoneId);
  }
}

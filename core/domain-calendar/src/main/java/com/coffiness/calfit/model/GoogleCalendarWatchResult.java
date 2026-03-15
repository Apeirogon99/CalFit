package com.coffiness.calfit.model;

import java.time.LocalDateTime;

/*
 * 구글 watch 채널 생성 결과를 담는 모델
 * */
public record GoogleCalendarWatchResult(
    String channelId, String resourceId, LocalDateTime channelExpiresAt) {}

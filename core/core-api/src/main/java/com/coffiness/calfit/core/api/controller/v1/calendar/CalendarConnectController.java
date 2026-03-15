package com.coffiness.calfit.core.api.controller.v1.calendar;

import com.coffiness.calfit.core.api.facade.calendar.CalendarConnectFacade;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import com.coffiness.calfit.v1.request.CalendarConnectRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CalendarConnectController {

  private final CalendarConnectFacade calendarConnectFacade;

  // 계정 연동
  @PostMapping("/api/v1/calendars/google/connect")
  public ApiResponse<String> connectGoogleCalendar(
      @Valid @RequestBody CalendarConnectRequest request,
      @AuthenticationPrincipal SecurityUser securityUser) {
    String googleEmail =
        calendarConnectFacade.connectGoogleCalendar(
            securityUser.userId(), request.authCode(), request.redirectUri());

    return ApiResponse.success(googleEmail);
  }

  // 구글 -> CalFit 일정 동기화
  @PostMapping("/api/v1/calendars/google/sync")
  public ApiResponse<Void> syncGoogleCalendar(@AuthenticationPrincipal SecurityUser securityUser) {
    calendarConnectFacade.syncGoogleCalendar(securityUser.userId());

    return ApiResponse.success(null);
  }

  // 구글 푸시 알림(웹훅) 수신
  @PostMapping("/api/v1/calendars/google/notifications")
  public ResponseEntity<Void> handleGoogleNotification(
      @RequestHeader(value = "X-Goog-Channel-ID", required = false) String channelId,
      @RequestHeader(value = "X-Goog-Resource-ID", required = false) String resourceId,
      @RequestHeader(value = "X-Goog-Channel-Token", required = false) String channelToken,
      @RequestHeader(value = "X-Goog-Resource-State", required = false) String resourceState) {
    calendarConnectFacade.handleGoogleCalendarNotification(
        channelId, resourceId, channelToken, resourceState);

    return ResponseEntity.ok().build();
  }
}

package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.api.v1.response.NotificationResponse;
import com.coffiness.calfit.api.v1.response.NotificationUnreadCountResponse;
import com.coffiness.calfit.core.api.facade.notification.NotificationFacade;
import com.coffiness.calfit.core.api.facade.notification.NotificationSseFacade;
import com.coffiness.calfit.core.support.Page;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.notification.NotificationCategory;
import com.coffiness.calfit.domain.notification.NotificationPage;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

  private final NotificationFacade notificationFacade;
  private final NotificationSseFacade notificationSseFacade;

  @CrossOrigin
  @GetMapping("/stream")
  public SseEmitter stream(
      @AuthenticationPrincipal SecurityUser user, HttpServletResponse response) {
    response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
    response.setHeader(HttpHeaders.CONNECTION, "keep-alive");
    response.setHeader("X-Accel-Buffering", "no");
    return notificationSseFacade.subscribe(currentTenantId(), user.userId());
  }

  @GetMapping("/unread-count")
  public ApiResponse<NotificationUnreadCountResponse> unreadCount(
      @AuthenticationPrincipal SecurityUser user) {
    return ApiResponse.success(
        new NotificationUnreadCountResponse(
            notificationFacade.countUnreadNotifications(user.userId())));
  }

  @GetMapping("/unread")
  public ApiResponse<Page<NotificationResponse>> unread(
      @AuthenticationPrincipal SecurityUser user,
      @RequestParam(required = false) String type,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    NotificationCategory category = parseCategory(type);
    NotificationPage notifications =
        category == null
            ? notificationFacade.getUnreadNotifications(user.userId(), page, size)
            : notificationFacade.getUnreadNotifications(user.userId(), category, page, size);
    return ApiResponse.success(
        new Page<>(
            notifications.contents().stream().map(NotificationResponse::from).toList(),
            notifications.hasNext()));
  }

  @GetMapping
  public ApiResponse<Page<NotificationResponse>> list(
      @AuthenticationPrincipal SecurityUser user,
      @RequestParam(required = false) String type,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    NotificationCategory category = parseCategory(type);
    NotificationPage notifications =
        category == null
            ? notificationFacade.getRecentNotifications(user.userId(), page, size)
            : notificationFacade.getRecentNotifications(user.userId(), category, page, size);
    return ApiResponse.success(
        new Page<>(
            notifications.contents().stream().map(NotificationResponse::from).toList(),
            notifications.hasNext()));
  }

  @PatchMapping("/{notificationId}/read")
  public ApiResponse<NotificationResponse> read(
      @AuthenticationPrincipal SecurityUser user, @PathVariable Long notificationId) {
    return ApiResponse.success(
        NotificationResponse.from(
            notificationFacade.readNotification(user.userId(), notificationId)));
  }

  @PatchMapping("/read-all")
  public ApiResponse<?> readAll(@AuthenticationPrincipal SecurityUser user) {
    notificationFacade.readAllNotifications(user.userId());
    return ApiResponse.success();
  }

  @DeleteMapping("/{notificationId}")
  public ApiResponse<?> delete(
      @AuthenticationPrincipal SecurityUser user, @PathVariable Long notificationId) {
    notificationFacade.deleteNotification(user.userId(), notificationId);
    return ApiResponse.success();
  }

  @DeleteMapping
  public ApiResponse<?> deleteAll(@AuthenticationPrincipal SecurityUser user) {
    notificationFacade.deleteAllNotifications(user.userId());
    return ApiResponse.success();
  }

  private NotificationCategory parseCategory(String type) {
    if (type == null || type.isBlank()) {
      return null;
    }
    try {
      return NotificationCategory.valueOf(type.toUpperCase());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private String currentTenantId() {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank()) {
      throw new IllegalArgumentException("Tenant ID is required.");
    }
    return tenantId;
  }
}
